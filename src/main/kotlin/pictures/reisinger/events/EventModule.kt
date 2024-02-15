package pictures.reisinger.events

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.util.getOrFail
import pictures.reisinger.db.EventService
import pictures.reisinger.db.EventSlotInformationDto
import pictures.reisinger.db.EventSlotReservationDto
import pictures.reisinger.plugins.AuthProviders

fun Application.module() {

    val eventService = EventService()

    routing {
        route("rest/events") {

            get {
                call.respond(eventService.findAllInFuture())
            }

            route(":eventId/slots/:slotId/reservations") {

                put {
                    val body = call.receive<EventSlotInformationDto>()
                    val slotId = call.parameters.getOrFail("slotId").toLong()
                    eventService.insertReservation(EventSlotReservationDto(slotId, body))
                }
            }
        }

        route("admin/events") {
            authenticate(AuthProviders.JWT_ADMIN) {
                route(":eventId") {

                    route("reservations") {
                        get {
                            val eventId = context.parameters.getOrFail("eventId").toLong()
                            context.respond(eventService.getReservationsForEvent(eventId))
                        }

                        route(":reservationId") {
                            delete {
                                val eventId = context.parameters.getOrFail("eventId").toLong()
                                val reservationId = context.parameters.getOrFail("reservationId").toLong()
                                eventService.deleteReservation(eventId, reservationId)
                            }
                        }
                    }

                    route("slots/:slotId/booking") {
                        post {
                            val eventId = context.parameters.getOrFail("eventId").toLong()
                            val slotId = context.parameters.getOrFail("slotId").toLong()
                            eventService.bookSlot(eventId, slotId)
                        }
                        delete {
                            val eventId = context.parameters.getOrFail("eventId").toLong()
                            val slotId = context.parameters.getOrFail("slotId").toLong()
                            eventService.deleteBookedSlot(eventId, slotId)
                        }
                    }
                }
            }
        }
    }
}
