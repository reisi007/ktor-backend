package pictures.reisinger.db

import io.ktor.server.application.Application
import org.jetbrains.exposed.sql.Database
import pictures.reisinger.plugins.get

fun Application.configureDatabase() {
    val config = environment.config.config("db")
    Database.connect(
        url = config["url"].getString(),
        user = config["user"].getString(),
        password = config["password"].getString(),
    )
}
