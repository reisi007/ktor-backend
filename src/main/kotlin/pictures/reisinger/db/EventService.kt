package pictures.reisinger.db

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.transactions.transaction
import pictures.reisinger.LocalDateAsString
import pictures.reisinger.plugins.BadRequest400Exception
import pictures.reisinger.plugins.NotFound404Exception
import java.time.LocalDate

class EventService {

    init {
        transaction {
            SchemaUtils.create(Events)
            SchemaUtils.create(EventSlots)
            SchemaUtils.create(EventSlotReservations)
        }
    }

    object Events : LongIdTable() {
        val title = varchar("title", length = 128)
        val date = date("_date")
        val description = varchar("description", length = 2048)
    }

    object EventSlots : LongIdTable() {
        val name = varchar("slot", length = 128)
        val event = reference("event", Events)
        val contact = text("contact").nullable()

        init {
            uniqueIndex("eventSlot", event, name)
        }
    }

    object EventSlotReservations : LongIdTable() {
        val eventSlot = reference("slot", EventSlots)
        val email = varchar("email", length = 128)
        val tel = varchar("tel", length = 128)
        val name = varchar("name", length = 256)
        val text = varchar("text", length = 2048).nullable()

        init {
            uniqueIndex("eventReservations", eventSlot, email)
        }
    }

    class EventSlotReservation(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<EventSlotReservation>(EventSlotReservations)

        var slot by EventSlot referencedOn EventSlotReservations.eventSlot
        var email by EventSlotReservations.email
        var name by EventSlotReservations.name
        var tel by EventSlotReservations.tel
        var text by EventSlotReservations.text
    }

    class EventSlot(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<EventSlot>(EventSlots)

        var event by Event referencedOn EventSlots.event
        var name by EventSlots.name
        var contact by EventSlots.contact
    }

    class Event(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<Event>(Events)

        var title by Events.title
        var date by Events.date
        var description by Events.description
        val slots by EventSlot referrersOn EventSlots.event

    }


    fun findAllInFuture(): List<EventDto<EventAvailabilityDto>> = transaction {
        findEventsInFuture().map { it.toPublicDto() }
    }

    fun extendedFindAllInFuture(): List<EventDto<AdminEventAvailabilityDto>> = transaction {
        findEventsInFuture().map { it.toAdminDto() }
    }

    private fun findEventsInFuture(): SizedIterable<Event> {
        return Event.find { Events.date greaterEq LocalDate.now() }
            .orderBy(Events.date to SortOrder.ASC)
    }

    fun persistEvent(eventDto: EventDto<EventAvailabilityDto>) = transaction {
        val eventEntity = Event.new {
            title = eventDto.title
            date = eventDto.date
            description = eventDto.description
        }

        eventDto.availability.forEach { (_, slot) ->
            EventSlot.new {
                this.name = slot
                event = eventEntity
            }
        }
    }

    fun bookSlot(eventId: Long, slotId: Long, contactInfo: String) = transaction {
        val result = EventSlot.find {
            (EventSlots.event eq eventId) and (EventSlots.id eq slotId)
        }.firstOrNull()

        if (result == null) throw NotFound404Exception

        result.contact = contactInfo
    }

    fun deleteBookedSlot(eventId: Long, slotId: Long) = transaction {
        val result = EventSlot.find {
            (EventSlots.event eq eventId) and (EventSlots.id eq slotId)
        }.firstOrNull()

        if (result == null) throw NotFound404Exception

        result.contact = null
    }

    fun insertReservation(reservation: EventSlotReservationDto): Unit = transaction {
        EventSlotReservation.new {
            slot = EventSlot.findById(reservation.slotId) ?: throw NotFound404Exception
            val info = reservation.info
            email = info.email
            tel = info.tel
            name = info.name
            text = info.text?.ifBlank { null }
        }
    }

    fun deleteReservation(eventId: Long, reservationId: Long) = transaction {
        val reservation = EventSlotReservation.findById(reservationId) ?: return@transaction
        if (eventId != reservation.slot.event.id.value) throw BadRequest400Exception
        reservation.delete()
    }

    fun getReservationsForEvent(eventId: Long): Map<Long, List<EventSlotInformationDto>> = transaction {
        EventSlotReservations.join(EventSlots, JoinType.LEFT, EventSlotReservations.eventSlot, EventSlots.id)
            .selectAll()
            .where { EventSlots.event.eq(eventId) }
            .orderBy(EventSlots.name to SortOrder.DESC, EventSlotReservations.id to SortOrder.DESC)
            .asSequence()
            .map { EventSlotReservation.wrapRow(it) }
            .groupBy { it.slot.id.value }
            .mapValues { (_, reservations) -> reservations.map { it.toDto().info } }
    }
}


@Serializable
data class EventDto<E : IEventAvailabilityDto>(
    val title: String,
    val date: LocalDateAsString,
    val description: String,
    val availability: List<E>,
    val id: Long? = null,
)

interface IEventAvailabilityDto {
    val id: Long?
    val slot: String
    val isAvailable: Boolean
}

@Serializable
data class EventAvailabilityDto(
    override val id: Long?,
    override val slot: String,
    override val isAvailable: Boolean
) : IEventAvailabilityDto

@Serializable
data class AdminEventAvailabilityDto(
    override val id: Long?,
    override val slot: String,
    override val isAvailable: Boolean,
    val contactInfo: String?
) : IEventAvailabilityDto

@Serializable
data class EventSlotReservationDto(val slotId: Long, val info: EventSlotInformationDto)

@Serializable
data class EventSlotInformationDto(
    val name: String,
    val email: String,
    val tel: String,
    val text: String?,
    val id: Long? = null,
)

private fun <E : IEventAvailabilityDto> EventService.Event.toDto(mapSlots: EventService.EventSlot.() -> E): EventDto<E> {
    return EventDto(
        title,
        date,
        description,
        slots.asSequence()
            .map(mapSlots)
            .sortedBy { it.slot }
            .toList(),
        id.value,
    )
}

fun EventService.Event.toPublicDto(): EventDto<EventAvailabilityDto> {
    return toDto {
        EventAvailabilityDto(
            id.value,
            name,
            contact == null
        )
    }
}

fun EventService.Event.toAdminDto(): EventDto<AdminEventAvailabilityDto> {
    return toDto {
        AdminEventAvailabilityDto(
            id.value,
            name,
            contact == null,
            contact
        )
    }
}

private fun EventService.EventSlotReservation.toInformationDto(): EventSlotInformationDto {
    return EventSlotInformationDto(
        name,
        email,
        tel,
        text?.ifBlank { null },
        id.value,
    )
}

fun EventService.EventSlotReservation.toDto(): EventSlotReservationDto {
    return EventSlotReservationDto(id.value, toInformationDto())
}
