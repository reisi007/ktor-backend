package pictures.reisinger.selection

import io.ktor.http.HttpStatusCode
import io.ktor.http.content.CachingOptions
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.cachingheaders.CachingHeaders
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import pictures.reisinger.plugins.maxAgeOfSeconds
import java.nio.file.Paths
import kotlin.io.path.listDirectoryEntries

fun Application.module() {

    routing {
        route("rest/images") {
            route("{secret}") {
                install(CachingHeaders) {
                    options { _, _ -> CachingOptions(maxAgeOfSeconds(600 /* 10 min */)) }
                }
                get { call.listImages() }

                route("{filename}") {
                    install(CachingHeaders) {
                        options { _, _ -> CachingOptions(maxAgeOfSeconds(1800 /* 30 min */)) }
                    }
                    get { call.fetchImage() }
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
        response.status(HttpStatusCode.Unauthorized)
    }

    val files = Paths.get(".", "selection", secret)
        .listDirectoryEntries()
        .asSequence()
        .map { it.fileName.toString() }
        .sorted()
        .toList()

    respond(files)
}

suspend fun ApplicationCall.fetchImage() {
    val secret = parameters["secret"]
    val filename = parameters["filename"]
    if (
        secret == null
        || filename == null
        || secret.contains(".")
        || filename.contains("..")
    ) {
        response.status(HttpStatusCode.Unauthorized)
    }

    respondFile(Paths.get(".", "selection", secret, filename).toFile())
}
