package pictures.reisinger.db

import io.ktor.server.application.Application
import org.jetbrains.exposed.sql.Database

fun Application.configureDatabase(): Database {
    val config = environment.config
    return Database.connect(
        url = config.property("db.url").getString(),
        user = config.property("db.user").getString(),
        password = config.property("db.password").getString(),
    )
}
