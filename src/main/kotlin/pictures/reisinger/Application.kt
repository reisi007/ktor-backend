package pictures.reisinger

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.netty.EngineMain
import io.ktor.server.routing.routing
import pictures.reisinger.db.configureDatabase
import pictures.reisinger.plugins.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val db = configureDatabase()
    configureSerialization()
    configureRouting()
    configureHTTP()
    configureMonitoring()
    configureSecurity(db)

    setupBusinessRoutes()
}

fun Application.setupBusinessRoutes() {
    routing {
        authenticate("jwt") {
            userRoutes()
        }

        authenticate("adminJwt") {
            adminRoutes()
        }
    }
}
