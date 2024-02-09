package pictures.reisinger

import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain
import pictures.reisinger.db.configureDatabase
import pictures.reisinger.plugins.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureDatabase()
    configureSerialization()
    configureRouting()
    configureHTTP()
    configureMonitoring()
    configureSecurity()
}
