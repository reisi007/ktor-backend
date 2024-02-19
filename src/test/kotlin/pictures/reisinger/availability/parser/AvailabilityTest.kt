package pictures.reisinger.availability.parser

import org.junit.Test
import pictures.reisinger.availability.assertAvailabilityStatus
import pictures.reisinger.availability.parser.AvailabilityStatus.FREE
import pictures.reisinger.availability.parser.AvailabilityStatus.RELAXED
import pictures.reisinger.test.assertThis
import java.time.LocalDate
import java.time.Month

class AvailabilityTest {
    @Test
    fun `03_2024 should not be busy, should be RELAXED`() {
        loadEventsFromIcs("bugfix_availability_2024-03.ics")
            .toAvailability(LocalDate.of(2024, Month.JANUARY, 6))
            .assertThis { assertAvailabilityStatus(RELAXED, RELAXED, RELAXED, FREE) }
    }

    @Test
    fun `when an event spans multiple months, the older months should not be visible`() {
        sequence {
            yield(
                Event(
                    "id", "summary", "category",
                    LocalDate.of(2024, Month.JANUARY, 31).atStartOfDay(),
                    LocalDate.of(2024, Month.FEBRUARY, 6).atStartOfDay()
                )
            )
        }.toAvailability(LocalDate.of(2024, Month.FEBRUARY, 5))
            .assertThis { assertAvailabilityStatus(RELAXED, FREE, FREE, FREE) }
    }
}
