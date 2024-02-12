package pictures.reisinger.db

import io.ktor.server.plugins.NotFoundException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class EventService {

    init {
        transaction {
            SchemaUtils.create(Events)
            SchemaUtils.create(EventSlots)
        }
    }

    object Events : LongIdTable() {
        val title = varchar("title", length = 128)
        val date = date("date")
        val description = varchar("description", length = 2048)
    }

    object EventSlots : IdTable<String>() {
        override val id = varchar("slot", length = 128).entityId()
        val event = reference("event", Events)
        val isAvailable = bool("availability").default(true)
    }

    class EventSlot(id: EntityID<String>) : Entity<String>(id) {
        companion object : EntityClass<String, EventSlot>(EventSlots)

        var event by Event referencedOn EventSlots.event
        var slot by EventSlots.id
        var isAvailable by EventSlots.isAvailable
    }

    class Event(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<Event>(Events)

        var title by Events.title
        var date by Events.date
        var description by Events.description
        val slots by EventSlot referrersOn EventSlots.event

    }

    @Serializable
    enum class Availability {
        FREE, BOOKED
    }


    fun findAllInFuture(): List<EventDto> = transaction {
        Event.find { Events.date greaterEq LocalDate.now() }
            .map { it.toDao() }
    }

    fun persistEvent(eventDto: EventDto) = transaction {
        val eventEntity = Event.new {
            title = eventDto.title
            date = eventDto.date
            description = eventDto.description
        }

        eventDto.availability.forEach { (slot) ->
            EventSlot.new(id = slot) { event = eventEntity }
        }
    }

    fun bookSlot(eventId: Long, eventAvailabilityDto: EventAvailabilityDto) {
        val result = EventSlot.find {
            (EventSlots.event eq eventId) and (EventSlots.id eq eventAvailabilityDto.slot)
        }.firstOrNull()

        if (result == null) {
            throw NotFoundException()
        }

        result.isAvailable = false
    }

    fun deleteBooking(eventId: Long, eventAvailabilityDto: EventAvailabilityDto) = transaction {
        val result = EventSlot.find {
            (EventSlots.event eq eventId) and (EventSlots.id eq eventAvailabilityDto.slot)
        }.firstOrNull()

        if (result == null) {
            throw NotFoundException()
        }

        result.isAvailable = true
    }
}

@Serializable
data class EventDto(
    val title: String,
    val date: LocalDateAsString,
    val description: String,
    val availability: List<EventAvailabilityDto>
)

@Serializable
data class EventAvailabilityDto(val slot: String, val isAvailable: Boolean)


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


fun EventService.Event.toDao(): EventDto {
    return EventDto(
        title,
        date,
        description,
        slots.map {
            EventAvailabilityDto(
                it.slot.value,
                it.isAvailable
            )
        }
    )
}
