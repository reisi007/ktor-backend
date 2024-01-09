package pictures.reisinger.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText

fun Application.configureRouting() {
    install(StatusPages) {

        exception<StatusException> { call, cause ->
            call.respondError(cause.statusCode, cause)
        }

        exception<Throwable> { call, cause ->
            call.respondError(HttpStatusCode.InternalServerError, cause)
        }
    }
}

private suspend fun ApplicationCall.respondError(
    statusCode: HttpStatusCode,
    cause: Throwable
) {
    val isDevMode = application.environment.developmentMode
    if (isDevMode) respondText(text = statusCode.description, status = statusCode)
    else respondText(text = "${statusCode.value}: $cause", status = statusCode)
}

data object NotAuthorized401Exception : StatusException(HttpStatusCode.Unauthorized)

abstract class StatusException(val statusCode: HttpStatusCode, cause: Throwable? = null) :
    Exception(statusCode.description, cause)
