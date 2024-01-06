package pictures.reisinger.availability.parser

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.LocalDate
import java.time.Month

class ICalParserTest {

    @Test
    fun `read ical file to object`() {
        val events = loadEventsFromIcs()

        assertThat(events.count()).isEqualTo(70)
    }

    @Test
    fun `filter date range with default duration`() {
        var events = loadEventsFromIcs()
        events = events.filterDateRange(from = LocalDate.of(2022, Month.SEPTEMBER, 20), Duration.ofDays(93))

        assertThat(events.count()).isEqualTo(7)
    }

}

fun loadEventsFromIcs(name: String = "basic.ics"): Sequence<Event> {
    val inputStream = loadIcs(name)

    val events = InputStreamReader(inputStream, StandardCharsets.UTF_8).use {
        ICalParser.parseIcal(it)
    }
    return events
}


fun loadIcs(name: String = "basic.ics"): InputStream {
    val inputStream = ICalParserTest::class.java.classLoader.getResourceAsStream("./$name")
    inputStream ?: throw IllegalStateException("Test file not found")
    return inputStream
}
