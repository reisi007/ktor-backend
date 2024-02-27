package pictures.reisinger.reviews

import io.ktor.http.content.CachingOptions
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.cachingheaders.CachingHeaders
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import pictures.reisinger.db.ReviewInputDto
import pictures.reisinger.db.ReviewService
import pictures.reisinger.plugins.AuthProviders
import pictures.reisinger.plugins.maxAgeOfSeconds

fun Application.module() {
    val reviewService = ReviewService()

    routing {
        route("rest/reviews") {
            put {
                val body = call.receive<ReviewInputDto>()
                reviewService.insertReview(body)
            }
        }

        authenticate(AuthProviders.JWT_ADMIN) {
            route("admin/reviews") {
                install(CachingHeaders) {
                    options { _, _ -> CachingOptions(maxAgeOfSeconds(300 /* 5min */)) }
                }
                get {
                    call.respond(reviewService.findAll())
                }
            }
        }
    }

}
