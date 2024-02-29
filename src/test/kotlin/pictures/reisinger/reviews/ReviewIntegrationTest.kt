package pictures.reisinger.reviews

import assertk.all
import assertk.assertions.first
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import org.junit.Test
import pictures.reisinger.db.ReviewInputDto
import pictures.reisinger.db.ReviewPrivateOutputDto
import pictures.reisinger.db.ReviewService
import pictures.reisinger.test.getBody
import pictures.reisinger.test.getJson
import pictures.reisinger.test.isSuccessContent
import pictures.reisinger.test.putJson
import pictures.reisinger.test.testAdminReviewModule
import pictures.reisinger.test.testReviewModule
import java.time.LocalDate

class ReviewIntegrationTest {

    @Test
    fun `Review can be put without admin privileges`() = testReviewModule {
        it.putJson("rest/reviews", sampleReview()).isSuccessContent()
    }

    @Test
    fun `Admin user can also see private review data`() = testAdminReviewModule(setupServer = {
        ReviewService().insertReview(sampleReview())
    }) { client ->
        client.getJson<List<ReviewPrivateOutputDto>>("admin/reviews").isSuccessContent {
            getBody().all {
                hasSize(1)

                first().all {
                    transform { it.private }.isEqualTo("private")
                    transform { it.public }.isEqualTo("public")
                    transform { it.date }.isEqualTo(LocalDate.now())
                }
            }
        }
    }
}

private fun sampleReview(): ReviewInputDto = ReviewInputDto("name", "private", "public")