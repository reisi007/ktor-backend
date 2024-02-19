package pictures.reisinger.test

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.UserPasswordCredential
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import pictures.reisinger.availability.parser.loadIcs
import pictures.reisinger.db.LoginUserService
import pictures.reisinger.plugins.configureClient
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.*
import pictures.reisinger.availability.module as availabilityModule
import pictures.reisinger.events.module as eventsModule
import pictures.reisinger.module as baseModule
import pictures.reisinger.projects.module as projectsModule
import pictures.reisinger.selection.module as selectionModule

fun testAvailabilityModule(block: IntegrationTestBuilder) = testWithDefaultConfig {

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

    block(client)
}

fun testProjectsModule(setupServer: IntegrationTestServerSetup = {}, block: suspend (HttpClient) -> Unit) =
    testWithDefaultConfig {

        application {
            baseModule()
            projectsModule()
            setupServer()
        }

        block(setupTestHttpClient())
    }

fun testSelectionsModule(setupServer: IntegrationTestServerSetup = {}, block: suspend (HttpClient) -> Unit) =
    testWithDefaultConfig {

        application {
            baseModule()
            selectionModule()
            setupServer()
        }

        block(setupTestHttpClient())
    }

fun testEventModule(setupServer: IntegrationTestServerSetup = {}, block: IntegrationTestBuilder) =
    testWithDefaultConfig {

        application {
            baseModule()
            eventsModule()
            setupServer()
        }

        block(setupTestHttpClient())
    }

fun testAdminEventModule(setupServer: IntegrationTestServerSetup = {}, block: IntegrationTestBuilder) =
    testEventModule(setupServer = {
        setupServer()
        createAdminUserInDb()
    }) { block(createTestAdminHttpClient()) }

private suspend fun ApplicationTestBuilder.createTestAdminHttpClient(): HttpClient {
    val basicAuthClient = setupTestHttpClient {
        install(Auth) {
            basic {
                credentials {
                    BasicAuthCredentials("admin", "admin")
                }
            }
        }
    }

    val loginResponse = basicAuthClient.post("login")
    if (!loginResponse.status.isSuccess()) throw IllegalStateException("Login failed")
    val bearerToken = loginResponse.bodyAsText()

    return setupTestHttpClient {
        install(Auth) {
            bearer {
                loadTokens {
                    BearerTokens(bearerToken, "unused")
                }
            }
        }
    }
}

private fun ApplicationTestBuilder.setupTestHttpClient(config: HttpClientConfig<out HttpClientEngineConfig>.() -> Unit = {}) =
    createClient {
        configureClient()
        config()
    }

private suspend fun ApplicationTestBuilder.withTestConfig(block: suspend ApplicationTestBuilder.() -> Unit) {
    val jdbcUrl = "jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1"
    environment {
        developmentMode = false
        config = MapApplicationConfig().apply {
            put("db.url", jdbcUrl)
            put("db.user", "root")
            put("db.password", "")

            put("app.availability.ical", "https://external.service/basic.ics")

            put("jwt.secret", "test")

            put("images.selection.folder", "selectionTest")
        }
    }

    block()
}

private fun testWithDefaultConfig(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
    withTestConfig { block() }
}

private fun createAdminUserInDb() = LoginUserService().createAdmin(UserPasswordCredential("admin", "admin"))

typealias IntegrationTestBuilder = suspend ApplicationTestBuilder.(HttpClient) -> Unit
typealias IntegrationTestServerSetup = Application.() -> Unit
