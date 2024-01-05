package pictures.reisinger.availability.parser

import kotlinx.serialization.Serializable
import pictures.reisinger.availability.parser.AvailabilityStatus.*
import java.time.format.DateTimeFormatter

@Serializable
data class Availability(
    val month: String,
    val status: AvailabilityStatus
) : Comparable<Availability> {
    override fun compareTo(other: Availability): Int {
        return compareValuesBy(this, other) { it.month }
    }
}

@Serializable
enum class AvailabilityStatus {
    BUSY,
    RELAXED,
    FREE
}

val FORMATTER_YYYY_MM = DateTimeFormatter.ofPattern("YYYY-MM")

fun Sequence<Event>.toAvailability(): List<Availability> {
    return flatMap {
        sequenceOf(it.startTime to it, it.endTime to it)
    }
        .map { (time, event) -> FORMATTER_YYYY_MM.format(time) to event }
        .distinctBy { (time, event) -> time + event.id }
        .groupBy({ (time) -> time }, { (_, value) -> value })
        .map { (month, entries) ->
            val status: AvailabilityStatus = when {
                entries.size >= 2 -> BUSY
                entries.size == 1 -> RELAXED
                else -> FREE
            }

            Availability(month, status)
        }
        .sorted()
        .toList()
}
