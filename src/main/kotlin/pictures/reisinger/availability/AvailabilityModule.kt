package pictures.reisinger.availability

import io.ktor.client.HttpClient
import io.ktor.http.CacheControl
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.CachingOptions
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.plugins.cachingheaders.caching
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import pictures.reisinger.SuspendingMemoryCache
import pictures.reisinger.availability.parser.ICalParser
import pictures.reisinger.availability.parser.toAvailability
import pictures.reisinger.plugins.defaultHttpClient
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
        route("rest") {
            get("availability") {

                var now = call.request.queryParameters["now"]?.let { LocalDate.parse(it, DateTimeFormatter.ISO_DATE) }
                    ?: LocalDate.now()
                // Analyze whole month --> start on day one
                now = now.withDayOfMonth(1)


                val availability = calendarCache.getValue()
                    .toAvailability(from = now)

                call.caching = CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 7200 /* 2h */))
                call.respond(HttpStatusCode.OK, availability)
            }
        }
    }
}


