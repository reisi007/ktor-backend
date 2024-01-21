package pictures.reisinger.availability.parser

import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.component.VEvent
import java.io.Reader
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*

object ICalParser {

    init {
        val properties = Properties()
        properties.load(ICalParser::class.java.classLoader.getResourceAsStream("ical4j.properties"))

        // Set the properties for iCal4j
        System.getProperties().putAll(properties)
    }

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
                    // end date is excluding in ical, including here...
                    it.endDate.date.toLocalDateTime().minusNanos(1)
                )
            }
    }


}

fun Date.toLocalDateTime(): LocalDateTime {
    return toInstant().atZone(ZoneId.ofOffset("UTC", ZoneOffset.UTC)).toLocalDateTime()
}

data class Event(
    val id: String,
    val summary: String,
    val category: String?,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime
)
