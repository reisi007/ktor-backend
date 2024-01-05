package pictures.reisinger.availability

import assertk.Assert
import assertk.assertions.containsExactly
import pictures.reisinger.availability.parser.Availability
import pictures.reisinger.availability.parser.AvailabilityStatus
import pictures.reisinger.test.assertThis

fun Assert<List<Availability>>.assertAvailabilityStatus(vararg stati: AvailabilityStatus) = given {
    it.map { element -> element.status }.assertThis {
        containsExactly(*stati)
    }
}
