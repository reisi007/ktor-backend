package pictures.reisinger.test

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import pictures.reisinger.availability.parser.loadIcs
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import pictures.reisinger.availability.module as availabilityModule
import pictures.reisinger.events.module as eventsModule
import pictures.reisinger.module as baseModule
import pictures.reisinger.projects.module as projectsModule

fun ApplicationTestBuilder.setupTestHttpClient() = createClient {
    install(ContentNegotiation) {
        json()
    }
}

fun ApplicationTestBuilder.withTestConfig() {
    environment {
        developmentMode = false
        config = ApplicationConfig("application-test.conf")
    }
}

private fun ApplicationTestBuilder.setupAvailabilityModuleIntegrationTest(): HttpClient {
    withTestConfig()

    externalServices {
        hosts("https://external.service") {
            routing {
                get("basic.ics") {
                    call.respondText {
                        InputStreamReader(loadIcs(), StandardCharsets.UTF_8).use {
                            it.readText()
                        }
                    }
                }
            }
        }
    }

    val client = setupTestHttpClient()

    application {
        baseModule()
        availabilityModule(client)
    }

    return client
}

fun testAvailabilityModule(block: suspend (HttpClient) -> Unit) = testApplication {
    val client = setupAvailabilityModuleIntegrationTest()
    block(client)
}

fun testProjectsModule(setupServer: Application.() -> Unit = {}, block: suspend (HttpClient) -> Unit) =
    testApplication {
        withTestConfig()

        application {
            baseModule()
            projectsModule()
            setupServer()
        }
        block(setupTestHttpClient())
    }

fun testEventModule(setupServer: Application.() -> Unit = {}, block: suspend (HttpClient) -> Unit) =
    testApplication {
        withTestConfig()

        application {
            baseModule()
            eventsModule()
            setupServer()
        }
        block(setupTestHttpClient())
    }

