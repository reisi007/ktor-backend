package pictures.reisinger.db

import io.ktor.server.application.Application
import org.jetbrains.exposed.sql.Database
import pictures.reisinger.plugins.get

fun Application.configureDatabase() {
    val config = environment.config
     Database.connect(
        url = config["db.url"].getString(),
        user = config["db.user"].getString(),
        password = config["db.password"].getString(),
    )
}
