package pictures.reisinger

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import pictures.reisinger.db.configureDatabase
import pictures.reisinger.plugins.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val db = configureDatabase()
    configureSerialization()
    configureRouting()
    configureHTTP()
    configureMonitoring()
    configureSecurity(db)

}
