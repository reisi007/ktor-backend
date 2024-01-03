package pictures.reisinger.plugins

import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.ApplicationConfigValue

operator fun ApplicationConfig.get(key: String): ApplicationConfigValue = property(key)
