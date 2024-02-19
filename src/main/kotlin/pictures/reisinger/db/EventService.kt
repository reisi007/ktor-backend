package pictures.reisinger.db

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.transactions.transaction
import pictures.reisinger.plugins.BadRequest400Exception
import pictures.reisinger.plugins.NotFound404Exception
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
        val isAvailable = bool("availability").default(true)

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
        var isAvailable by EventSlots.isAvailable
    }

    class Event(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<Event>(Events)

        var title by Events.title
        var date by Events.date
        var description by Events.description
        val slots by EventSlot referrersOn EventSlots.event

    }


    fun findAllInFuture(): List<EventDto> = transaction {
        Event.find { Events.date greaterEq LocalDate.now() }
            .map { it.toDto() }
    }

    fun persistEvent(eventDto: EventDto) = transaction {
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

    fun bookSlot(eventId: Long, slotId: Long) = transaction {
        val result = EventSlot.find {
            (EventSlots.event eq eventId) and (EventSlots.id eq slotId)
        }.firstOrNull()

        if (result == null) throw NotFound404Exception

        result.isAvailable = false
    }

    fun deleteBookedSlot(eventId: Long, slotId: Long) = transaction {
        val result = EventSlot.find {
            (EventSlots.event eq eventId) and (EventSlots.id eq slotId)
        }.firstOrNull()

        if (result == null) throw NotFound404Exception

        result.isAvailable = true
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
            .select { EventSlots.event.eq(eventId) }
            .orderBy(EventSlots.name to SortOrder.DESC)
            .asSequence()
            .map { EventSlotReservation.wrapRow(it) }
            .groupBy { it.slot.id.value }
            .mapValues { (_, reservations) -> reservations.map { it.toDto().info } }
    }
}


@Serializable
data class EventDto(
    val id: Long?,
    val title: String,
    val date: LocalDateAsString,
    val description: String,
    val availability: List<EventAvailabilityDto>
)

@Serializable
data class EventAvailabilityDto(val id: Long?, val slot: String, val isAvailable: Boolean)

@Serializable
data class EventSlotReservationDto(val slotId: Long, val info: EventSlotInformationDto)

@Serializable
data class EventSlotInformationDto(
    val id: Long?,
    val name: String,
    val email: String,
    val tel: String,
    val text: String?
)

typealias LocalDateAsString = @Serializable(with = LocalDateSerializer::class) LocalDate

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = LocalDate::class)
class LocalDateSerializer : KSerializer<LocalDate> {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(value.format(formatter))
    }

    override fun deserialize(decoder: Decoder): LocalDate {
        return LocalDate.parse(decoder.decodeString(), formatter)
    }
}


fun EventService.Event.toDto(): EventDto {
    return EventDto(
        id.value,
        title,
        date,
        description,
        slots.asSequence().map {
            EventAvailabilityDto(
                it.id.value,
                it.name,
                it.isAvailable
            )
        }
            .sortedBy { it.slot }
            .toList()
    )
}

private fun EventService.EventSlotReservation.toInformationDto(): EventSlotInformationDto {
    return EventSlotInformationDto(
        id.value,
        name,
        email,
        tel,
        text?.ifBlank { null }
    )
}

fun EventService.EventSlotReservation.toDto(): EventSlotReservationDto {
    return EventSlotReservationDto(id.value, toInformationDto())
}
