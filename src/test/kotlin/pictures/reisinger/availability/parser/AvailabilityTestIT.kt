package pictures.reisinger.availability.parser;

import io.ktor.client.request.get
import org.junit.Test
import pictures.reisinger.availability.assertAvailabilityStatus
import pictures.reisinger.availability.parser.AvailabilityStatus.*
import pictures.reisinger.test.getBody
import pictures.reisinger.test.isSuccessContent
import pictures.reisinger.test.testAvailabilityModule
import pictures.reisinger.test.toHttpReturn


class AvailabilityTestIT {

    @Test
    fun `availability is working with sample data`() = testAvailabilityModule { client ->
        client.get("/rest/availability?now=2022-09-10").toHttpReturn<List<Availability>>()
            .isSuccessContent { getBody().assertAvailabilityStatus(BUSY, BUSY, RELAXED, RELAXED) }
    }
}


