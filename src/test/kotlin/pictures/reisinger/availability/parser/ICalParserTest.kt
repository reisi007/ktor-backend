package pictures.reisinger.availability.parser

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.Month

class ICalParserTest {

    @Test
    fun `read ical file to object`() {
        val events = loadEventsFromBasicIcs()

        assertThat(events.count()).isEqualTo(70)
    }

    @Test
    fun `filter date range with default duration`() {
        var events = loadEventsFromBasicIcs()
        events = events.filterDateRange(from = LocalDate.of(2022, Month.SEPTEMBER, 20))

        assertThat(events.count()).isEqualTo(5)
    }

    private fun loadEventsFromBasicIcs(): Sequence<Event> {
        val inputStream = ICalParserTest::class.java.classLoader.getResourceAsStream("basic.ics")
        inputStream ?: throw IllegalStateException("Test file not found")

        val events = InputStreamReader(inputStream, StandardCharsets.UTF_8).use {
            ICalParser.parseIcal(it)
        }
        return events
    }
}
