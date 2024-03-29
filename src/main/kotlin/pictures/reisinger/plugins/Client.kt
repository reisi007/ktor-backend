package pictures.reisinger.plugins

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json

fun defaultHttpClient(): HttpClient = HttpClient(CIO) {
    configureClient()
}

fun <T : HttpClientEngineConfig> HttpClientConfig<T>.configureClient() {
    install(ContentNegotiation) {
        json()
    }
}
