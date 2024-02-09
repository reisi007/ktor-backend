package pictures.reisinger.projects

import assertk.all
import assertk.assertions.containsExactly
import assertk.assertions.extracting
import assertk.assertions.hasSize
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Test
import pictures.reisinger.db.ProjectDto
import pictures.reisinger.test.getBody
import pictures.reisinger.test.getJson
import pictures.reisinger.test.isSuccessContent
import pictures.reisinger.test.testProjectsModule
import pictures.reisinger.db.ProjectService.Project.Companion as Project

class ProjectIntegrationTest {
    @Test
    fun `projects are returned in alphabetical order`() = testProjectsModule(
        setupServer = {
            transaction {
                Project.new {
                    title = "B"
                    text = "Lorem ipsum"
                }
                Project.new {
                    title = "A"
                    text = "Lorem ipsum"
                }
                Project.new {
                    title = "C"
                    text = "Lorem ipsum"
                }
            }
        }
    ) { client ->
        client.getJson<List<ProjectDto>>("/rest/projects/ideas").isSuccessContent<List<ProjectDto>> {
            getBody().all {
                hasSize(3)
                extracting { it.title }.containsExactly("A", "B", "C")
            }
        }
    }
}
