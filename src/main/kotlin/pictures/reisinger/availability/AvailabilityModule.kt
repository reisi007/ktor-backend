package pictures.reisinger.availability

import io.ktor.client.HttpClient
import io.ktor.http.content.CachingOptions
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.cachingheaders.CachingHeaders
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import pictures.reisinger.SuspendingMemoryCache
import pictures.reisinger.availability.parser.ICalParser
import pictures.reisinger.availability.parser.toAvailability
import pictures.reisinger.plugins.defaultHttpClient
import pictures.reisinger.plugins.maxAgeOfSeconds
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@JvmOverloads
fun Application.module(client: HttpClient = defaultHttpClient()) {

    val calendarCache = SuspendingMemoryCache(Duration.ofMinutes(30)) {
        val fetchCalendarService = CalendarClientImpl(this@module, client)
        ICalParser.parseIcal(fetchCalendarService.readIcal())
    }

    routing {
        route("rest/availability") {
            install(CachingHeaders) {
                options { _, _ -> CachingOptions(maxAgeOfSeconds(2700 /* 2h */)) }
            }

            get {
                val now = call.request.queryParameters["now"]
                    ?.let { LocalDate.parse(it, DateTimeFormatter.ISO_DATE) }
                    ?: LocalDate.now()

                val availability = calendarCache.getValue()
                    .toAvailability(from = now)

                call.respond(availability)
            }
        }
    }
}


