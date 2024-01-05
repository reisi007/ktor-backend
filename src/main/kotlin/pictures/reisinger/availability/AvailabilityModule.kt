package pictures.reisinger.availability

import io.ktor.client.HttpClient
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import pictures.reisinger.availability.parser.ICalParser
import pictures.reisinger.availability.parser.filterDateRange
import pictures.reisinger.availability.parser.toAvailability
import pictures.reisinger.plugins.defaultHttpClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@JvmOverloads
fun Application.module(client: HttpClient = defaultHttpClient()) {

    routing {
        route("rest") {
            get("availability") {
                val now = call.request.queryParameters["now"]?.let { LocalDate.parse(it, DateTimeFormatter.ISO_DATE) }
                    ?: LocalDate.now()

                val fetchCalendarService = CalendarClientImpl(this@module, client)
                val availability = ICalParser.parseIcal(fetchCalendarService.readIcal())
                    .filterDateRange(from = now)
                    .toAvailability()

                call.respond(HttpStatusCode.OK, availability)
            }
        }
    }
}


