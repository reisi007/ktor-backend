package pictures.reisinger.availability.parser

import io.ktor.server.util.toLocalDateTime
import io.ktor.util.InternalAPI
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.component.VEvent
import java.io.Reader
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

object ICalParser {

    @OptIn(InternalAPI::class)
    fun parseIcal(reader: Reader): Sequence<Event> {
        val calendar = with(CalendarBuilder()) {
            build(reader)
        }
        return calendar.components.asSequence()
            .map { it as? VEvent }
            .filterNotNull()
            .map {
                val summary = it.summary.value

                Event(
                    it.uid.value,
                    summary.substringBefore('|'),
                    summary.substringAfter('|', "???"),
                    it.startDate.date.toLocalDateTime(),
                    it.endDate.date.toLocalDateTime()
                )
            }
    }
}

data class Event(
    val id: String,
    val summary: String,
    val category: String?,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime
)
