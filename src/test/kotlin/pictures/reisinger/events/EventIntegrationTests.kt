package pictures.reisinger.events;

import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.junit.Test
import pictures.reisinger.db.*
import pictures.reisinger.plugins.AuthProviders
import pictures.reisinger.plugins.defaultPrincipalOrThrow
import pictures.reisinger.test.*
import java.time.LocalDate

class EventIntegrationTests {

    @Test
    fun `event of the same day is returned, events of the past are ignored`() = testEventModule(
        setupServer = {
            val eventService = EventService()
            eventService.persistEvent(sampleEvent())
            eventService.persistEvent(sampleEvent().copy(date = LocalDate.now().minusDays(1)))
        }
    ) {
        it.getJson<List<EventDto<EventAvailabilityDto>>>("/rest/events")
            .isSuccessContent { getBody().hasSize(1) }
    }

    @Test
    fun `reservate an event`() = testEventModule(setupServer = {
        val eventService = EventService()
        eventService.persistEvent(sampleEvent())
    }) {
        it.post("rest/events/1/slots/1/reservations") {
            setBody(sampleEventSlotInformation())
            contentType(ContentType.Application.Json)
        }.status.assertThis { isEqualTo(HttpStatusCode.OK) }
    }

    @Test
    fun `admin bearer token works`() = testAdminEventModule(setupServer = {
        routing {
            authenticate(AuthProviders.JWT_ADMIN) {
                route("admin/test") {
                    get {
                        val principal = call.defaultPrincipalOrThrow()
                        if (!principal.roles.contains("admin"))
                            call.response.status(HttpStatusCode.BadRequest)
                        else
                            call.response.status(HttpStatusCode.OK)
                    }
                }
            }
        }
    }) {
        it.get("admin/test").assertThis {
            transform { response -> response.status }.isEqualTo(HttpStatusCode.OK)
        }
    }

    @Test
    fun `get reservations as admin`() = testAdminEventModule(setupServer = {
        val eventService = EventService()
        val eventDto = sampleEvent()
        eventService.persistEvent(eventDto)
        eventService.insertReservation(EventSlotReservationDto(1, sampleEventSlotInformation()))
    }) {
        it.getJson<Map<Long, List<EventSlotInformationDto>>>("/admin/events/1/reservations")
            .isSuccessContent { getBody().hasSize(1) }
    }

    @Test
    fun `booking an event works`() = testAdminEventModule(setupServer = {
        val eventService = EventService()
        val eventDto = sampleEvent()
        eventService.persistEvent(eventDto)
    }) { client ->
        client.put("admin/events/1/slots/1/booking").status.assertThis {
            isEqualTo(HttpStatusCode.OK)
        }
    }

    @Test
    fun `deleting an event booking works`() = testAdminEventModule(setupServer = {
        val eventService = EventService()
        val eventDto = sampleEvent()
        eventService.persistEvent(eventDto)
        eventService.bookSlot(1, 1, "Contact info")
    }) { client ->
        client.delete("admin/events/1/slots/1/booking").status.assertThis {
            isEqualTo(HttpStatusCode.OK)
        }
    }

    @Test
    fun `deleting an event reservation works`() = testAdminEventModule(setupServer = {
        val eventService = EventService()
        val eventDto = sampleEvent()
        eventService.persistEvent(eventDto)
        eventService.insertReservation(EventSlotReservationDto(1, sampleEventSlotInformation()))

    }) { client ->
        client.delete("admin/events/1/reservations/1").status.assertThis {
            isEqualTo(HttpStatusCode.OK)
        }
    }


}

fun sampleEventSlotInformation() = EventSlotInformationDto("Name", "e@mail.com", "+43126", "Text")

fun sampleEvent(): EventDto<EventAvailabilityDto> {
    return EventDto(
        "Title",
        LocalDate.now(),
        "Description",
        listOf(EventAvailabilityDto(1, "A", isAvailable = true))
    )
}
