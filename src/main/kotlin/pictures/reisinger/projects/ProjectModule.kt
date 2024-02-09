package pictures.reisinger.projects

import io.ktor.http.HttpStatusCode
import io.ktor.http.content.CachingOptions
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.cachingheaders.CachingHeaders
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import pictures.reisinger.db.ProjectService
import pictures.reisinger.db.toDto
import pictures.reisinger.plugins.maxAgeOfSeconds

fun Application.module() {

    val projectService = ProjectService()

    routing {
        route("rest") {
            route("projects") {
                route("ideas") {
                    install(CachingHeaders) {
                        options { _, _ -> CachingOptions(maxAgeOfSeconds(2700 /* 2h */)) }
                    }

                    get {
                        val projects = projectService.findAll()
                            .map { it.toDto() }

                        call.respond(HttpStatusCode.OK, projects)
                    }
                }
            }
        }
    }
}
