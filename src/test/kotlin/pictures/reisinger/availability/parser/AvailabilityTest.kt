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
}
