package pictures.reisinger.selection

import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.Test
import pictures.reisinger.test.*

class SelectionIntegrationTest {

    @Test
    fun `list all images works`() = testSelectionsModule {
        it.getJson<List<String>>("rest/images/secret").isSuccessContent {
            getBody().containsExactly("01.txt", "02.txt")
        }
    }

    @Test
    fun `getting a specific image works`() = testSelectionsModule { client ->
        client.get("rest/images/secret/02.txt").assertThis {
            transform { it.status }.isEqualTo(HttpStatusCode.OK)
            transform { runBlocking { it.bodyAsText() }.trim() }.isEqualTo("file 2 content")
        }
    }
}
