package pictures.reisinger.events

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.*
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

            route("{eventId}/slots/{slotId}/reservations") {
                post {
                    val body = call.receive<EventSlotInformationDto>()
                    val slotId = call.parameters.getOrFail("slotId").toLong()
                    eventService.insertReservation(EventSlotReservationDto(slotId, body))
                    call.response.status(HttpStatusCode.OK)
                }
            }
        }

        authenticate(AuthProviders.JWT_ADMIN) {
            route("admin/events") {
                route("{eventId}") {
                    route("reservations") {
                        get {
                            val eventId = call.parameters.getOrFail("eventId").toLong()
                            call.respond(eventService.getReservationsForEvent(eventId))
                        }

                        route("{reservationId}") {
                            delete {
                                val eventId = call.parameters.getOrFail("eventId").toLong()
                                val reservationId = call.parameters.getOrFail("reservationId").toLong()
                                eventService.deleteReservation(eventId, reservationId)
                                call.response.status(HttpStatusCode.OK)
                            }
                        }
                    }

                    route("slots/{slotId}/booking") {
                        put {
                            val eventId = call.parameters.getOrFail("eventId").toLong()
                            val slotId = call.parameters.getOrFail("slotId").toLong()
                            val body = call.receiveText()
                            eventService.bookSlot(eventId, slotId, body)
                            call.response.status(HttpStatusCode.OK)
                        }

                        delete {
                            val eventId = call.parameters.getOrFail("eventId").toLong()
                            val slotId = call.parameters.getOrFail("slotId").toLong()
                            eventService.deleteBookedSlot(eventId, slotId)
                            call.response.status(HttpStatusCode.OK)
                        }
                    }
                }
            }
        }
    }
}
