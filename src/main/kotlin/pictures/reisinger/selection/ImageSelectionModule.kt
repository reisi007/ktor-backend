package pictures.reisinger.selection

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.nio.file.Paths
import kotlin.io.path.listDirectoryEntries

fun Application.module() {

    routing {
        route("rest") {
            route("images") {
                route("{secret}") {
                    get {
                        call.listImages()
                    }
                }
            }
        }
    }
}


suspend fun ApplicationCall.listImages() {
    val secret = parameters["secret"]
    if (
        secret == null
        || secret.contains(".")
    ) {
        response.status(HttpStatusCode.NotFound)
    }

    val files = Paths.get(".", "selection", secret)
        .listDirectoryEntries()
        .asSequence()
        .map { it.fileName.toString() }
        .sorted()
        .toList()

    respond(files)
}
