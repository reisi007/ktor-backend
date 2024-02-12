package pictures.reisinger.events

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import pictures.reisinger.db.EventService

fun Application.module() {

    val eventService = EventService()

    routing {
        route("rest") {
            route("events") {

                get {
                    call.respond(eventService.findAllInFuture())
                }
            }
        }
    }
}
