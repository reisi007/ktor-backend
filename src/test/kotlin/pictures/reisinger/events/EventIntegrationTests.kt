package pictures.reisinger.events;

import assertk.assertions.hasSize
import org.junit.Test
import pictures.reisinger.db.EventAvailabilityDto
import pictures.reisinger.db.EventDto
import pictures.reisinger.db.EventService
import pictures.reisinger.test.getBody
import pictures.reisinger.test.getJson
import pictures.reisinger.test.isSuccessContent
import pictures.reisinger.test.testEventModule
import java.time.LocalDate

class EventIntegrationTests {

    @Test
    fun `event of the same day is returned, events of the past are ignored`() = testEventModule(
        setupServer = {
            EventService().persistEvent(sampleEvent())
            EventService().persistEvent(sampleEvent().copy(date = LocalDate.now().minusDays(1)))
        }
    ) {
        it.getJson<List<EventDto>>("/rest/events")
            .isSuccessContent<List<EventDto>> { getBody().hasSize(1) }
    }
}


fun sampleEvent(): EventDto {
    return EventDto(
        "Title",
        LocalDate.now(),
        "Description",
        listOf(EventAvailabilityDto("A", isAvailable = true))
    )
}
