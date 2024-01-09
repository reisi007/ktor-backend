package pictures.reisinger.plugins

import io.ktor.http.CacheControl
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.deflate
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.compression.minimumSize
import io.ktor.server.plugins.cors.routing.CORS

fun Application.configureHTTP() {

    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowOrigins {
            it.endsWith("reisinger.pictures") || it.contains("//localhost")
        }
    }
    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024) // condition
        }
    }
}

fun maxAgeOfSeconds(maxAgeSeconds: Int, proxyMaxAgeSeconds: Int = maxAgeSeconds) =
    CacheControl.MaxAge(maxAgeSeconds = maxAgeSeconds, proxyMaxAgeSeconds = maxAgeSeconds)
