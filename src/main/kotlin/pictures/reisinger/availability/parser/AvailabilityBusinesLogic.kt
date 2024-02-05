package pictures.reisinger.availability.parser

import kotlinx.serialization.Serializable
import pictures.reisinger.availability.parser.AvailabilityStatus.*
import pictures.reisinger.availability.parser.ShootingDurationType.LONG
import pictures.reisinger.availability.parser.ShootingDurationType.SHORT
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
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

fun Sequence<Event>.toAvailability(from: LocalDate): List<Availability> {
    val fromInternal = from.withDayOfMonth(1)
    val toInternal = fromInternal.calculateEndDate(4)

    val eventsPerMonth: Map<String, List<Event>> = filterDateRange(fromInternal, toInternal)
        .flatMap { sequenceOf(it.startTime to it, it.endTime to it) }
        .map { (time, event) -> FORMATTER_YYYY_MM.format(time) to event }
        .distinctBy { (time, event) -> time + event.id }
        .groupByTo(mutableMapOf(), { (time) -> time }, { (_, value) -> value })
        .apply {
            desiredMonthKeys(fromInternal, toInternal).forEach { key -> putIfAbsent(key, mutableListOf()) }
        }

    return eventsPerMonth
        .map { (month, entries) -> Availability(month, computeAvailabilityStatus(entries)) }
        .sorted()
        .toList()
}


fun desiredMonthKeys(from: LocalDate, endMonth: LocalDateTime): Sequence<String> = sequence {
    val lastDayOfEndMonth = endMonth.withDayOfMonth(endMonth.toLocalDate().lengthOfMonth())

    var curDate = from.atStartOfDay()

    while (curDate < lastDayOfEndMonth) {
        yield(FORMATTER_YYYY_MM.format(curDate))
        curDate = curDate.plusMonths(1)
    }
}

internal fun Sequence<Event>.filterDateRange(from: LocalDate, to: LocalDateTime): Sequence<Event> {
    val fromWithTime = from.atStartOfDay()
    return filter { it.startTime in fromWithTime..to || it.endTime in fromWithTime..to }
}

private fun LocalDate.calculateEndDate(nrOfMonths: Long): LocalDateTime = plusMonths(nrOfMonths)
    .minusDays(1)
    .atTime(23, 59, 59)

private fun computeAvailabilityStatus(entries: List<Event>): AvailabilityStatus {
    val count = entries.asSequence()
        .filter { it.category != null } // This is a shooting
        .map { it.calculateDuration() }
        .sumOf { it.weight }

    return when {
        count >= 2 * LONG.weight -> BUSY
        count >= LONG.weight -> RELAXED
        else -> FREE
    }
}

enum class ShootingDurationType(val weight: Int) {
    SHORT(1),
    LONG(4)
}

fun Event.calculateDuration(): ShootingDurationType {
    val endTimeExclusive = endTime.plusNanos(1)
    val isShootingLong = Duration.between(startTime, endTimeExclusive) > Duration.ofHours(2)
    return if (isShootingLong) LONG
    else SHORT
}
