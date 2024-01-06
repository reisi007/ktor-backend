package pictures.reisinger.availability.parser

import kotlinx.serialization.Serializable
import pictures.reisinger.availability.parser.AvailabilityStatus.*
import java.time.Duration
import java.time.LocalDate
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

val FORMATTER_YYYY_MM: DateTimeFormatter = DateTimeFormatter.ofPattern("YYYY-MM")

fun Sequence<Event>.toAvailability(from: LocalDate, duration: Duration = Duration.ofDays(93)): List<Availability> {
    val eventsPerMonth = filterDateRange(from, duration)
        .flatMap { sequenceOf(it.startTime to it, it.endTime to it) }
        .map { (time, event) -> FORMATTER_YYYY_MM.format(time) to event }
        .distinctBy { (time, event) -> time + event.id }
        .groupByTo(mutableMapOf(), { (time) -> time }, { (_, value) -> value })

    desiredMonthKeys(from, duration).forEach { key ->
        eventsPerMonth.putIfAbsent(key, mutableListOf())
    }

    return eventsPerMonth
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

fun desiredMonthKeys(from: LocalDate, duration: Duration): Sequence<String> = sequence {
    val endMonth = (from.atTime(23, 59, 59) + duration)
    val lastDayOfEndMonth = endMonth.withDayOfMonth(endMonth.toLocalDate().lengthOfMonth())

    var curDate = from.atStartOfDay()

    while (curDate < lastDayOfEndMonth) {
        yield(FORMATTER_YYYY_MM.format(curDate))
        curDate = curDate.plusMonths(1)
    }
}

internal fun Sequence<Event>.filterDateRange(from: LocalDate, duration: Duration): Sequence<Event> {
    val fromWithTime = from.atStartOfDay()
    val to = from.atTime(23, 59, 59) + duration
    return filter { it.startTime in fromWithTime..to || it.endTime in fromWithTime..to }
}
