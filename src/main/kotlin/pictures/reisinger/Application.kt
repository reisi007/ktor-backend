package pictures.reisinger

import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain
import pictures.reisinger.db.configureDatabase
import pictures.reisinger.plugins.*
import pictures.reisinger.selection.module
import pictures.reisinger.availability.module as availabilityModule
import pictures.reisinger.events.module as eventsModule
import pictures.reisinger.module as baseModule
import pictures.reisinger.projects.module as projectsModule
import pictures.reisinger.reviews.module as reviewModule
import pictures.reisinger.selection.module as selectionModule

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

fun Application.allFeatures() {
    module()

    availabilityModule()
    baseModule()
    eventsModule()
    projectsModule()
    reviewModule()
    selectionModule()
}
