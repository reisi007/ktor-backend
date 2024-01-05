package pictures.reisinger.availability.parser;

import assertk.all
import assertk.assertions.hasSize
import assertk.assertions.support.expected
import assertk.assertions.support.show
import io.ktor.client.request.get
import org.junit.Test
import pictures.reisinger.test.getData
import pictures.reisinger.test.isSuccessContent
import pictures.reisinger.test.testAvailabilityModule
import pictures.reisinger.test.toHttpReturn


class AvailabilityTestIT {

    @Test
    fun `availability is working with sample data`() = testAvailabilityModule { client ->
        client.get("/rest/availability?now=2022-09-10").toHttpReturn<List<Availability>>()
            .isSuccessContent<List<Availability>> {
                getData().all {
                    hasSize(3)

                    given {
                        if (it.all { elem -> elem.status == AvailabilityStatus.BUSY }) return@given
                        expected(
                            "Expected all elements to be of status ${show(AvailabilityStatus.BUSY)}, but was ${show(it)}"
                        )

                    }
                }
            }

    }


}
