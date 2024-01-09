package pictures.reisinger.availability

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.server.application.Application
import pictures.reisinger.plugins.get
import java.io.Reader
import java.io.StringReader

interface CalendarClient {

    suspend fun readIcal(): Reader
}

class CalendarClientImpl(private val iCalUrl: String, private val client: HttpClient) : CalendarClient {

    constructor(application: Application, client: HttpClient) : this(
        application.environment.config["app.availability.ical"].getString(),
        client
    )

    override suspend fun readIcal(): Reader {
        val response = client.get(iCalUrl)
        if (!response.status.isSuccess()) {
            throw IllegalStateException("iCal URL is not valid")
        }
        val body = response.bodyAsText()

        return StringReader(body)
    }

}
