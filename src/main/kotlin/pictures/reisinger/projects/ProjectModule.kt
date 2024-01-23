package pictures.reisinger.projects

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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