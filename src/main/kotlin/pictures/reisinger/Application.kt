package pictures.reisinger

import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.content.CachingOptions
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.cachingheaders.CachingHeaders
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
