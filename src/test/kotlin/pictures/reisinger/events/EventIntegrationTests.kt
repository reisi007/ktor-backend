package pictures.reisinger.events;

import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.junit.Test
import pictures.reisinger.db.EventAvailabilityDto
import pictures.reisinger.db.EventDto
import pictures.reisinger.db.EventService
import pictures.reisinger.db.EventSlotInformationDto
import pictures.reisinger.plugins.AuthProviders
import pictures.reisinger.plugins.defaultPrincipalOrThrow
import pictures.reisinger.test.*
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
        EventService().persistEvent(sampleEvent())
    }) {
        it.getJson<Map<Long, List<EventSlotInformationDto>>>("/admin/events/1/reservations")
            .isSuccessContent<Map<Long, List<EventSlotInformationDto>>> {
                getBody().hasSize(0) // Get empty response as no reservations have been made....
            }
    }
}

fun sampleEvent(): EventDto {
    return EventDto(
        1,
        "Title",
        LocalDate.now(),
        "Description",
        listOf(EventAvailabilityDto(1, "A", isAvailable = true))
    )
}
