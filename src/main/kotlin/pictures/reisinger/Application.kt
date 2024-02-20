package pictures.reisinger

import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain
import pictures.reisinger.db.configureDatabase
import pictures.reisinger.plugins.configureHTTP
import pictures.reisinger.plugins.configureMonitoring
import pictures.reisinger.plugins.configureRouting
import pictures.reisinger.plugins.configureSecurity
import pictures.reisinger.plugins.configureSerialization

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureDatabase()
    configureSerialization()
    configureRouting()
    configureHTTP()
    configureSecurity()
    configureMonitoring()
}
