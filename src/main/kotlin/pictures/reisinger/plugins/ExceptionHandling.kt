package pictures.reisinger.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText

fun Application.configureRouting() {
    install(StatusPages) {

        exception<NotAuthorized401Exception> { call, cause ->
            val isDevMode = call.application.environment.developmentMode
            if (isDevMode)
                call.respondText(text = "Not authorized", status = HttpStatusCode.Unauthorized)
            else
                call.respondText(text = "401: $cause", status = HttpStatusCode.InternalServerError)
        }
        exception<Throwable> { call, cause ->
            val isDevMode = call.application.environment.developmentMode
            if (isDevMode)
                call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
            else
                call.respondText(text = "An internal error occured", status = HttpStatusCode.InternalServerError)
        }
    }
}

data object NotAuthorized401Exception : RuntimeException()
