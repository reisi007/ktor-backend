package pictures.reisinger.selection

import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import org.junit.Test
import pictures.reisinger.test.assertThis
import pictures.reisinger.test.getBody
import pictures.reisinger.test.getJson
import pictures.reisinger.test.isSuccessContent
import pictures.reisinger.test.testSelectionsModule

class SelectionIntegrationTest {

    @Test
    fun `list all images works`() = testSelectionsModule {
        it.getJson<List<String>>("rest/images/secret").isSuccessContent {
            getBody().containsExactly("01.jpg", "02.jpg")
        }
    }

    @Test
    fun `getting a specific image works`() = testSelectionsModule {
        it.get("/rest/images/secret/02.jpg").status.assertThis {
            isEqualTo(HttpStatusCode.OK)
        }
    }
}