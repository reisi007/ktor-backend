package pictures.reisinger.db;

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

class ProjectService {

    object Projects : LongIdTable() {
        val title = varchar("title", length = 50).uniqueIndex("uniqueTitle")
        val text = varchar("text", length = 512)
    }

    init {
        transaction {
            SchemaUtils.create(Projects)
        }
    }

    class Project(id: EntityID<Long>) : LongEntity(id) {

        companion object : LongEntityClass<Project>(Projects)

        var title by Projects.title
        var text by Projects.text
    }

    fun findAll(): List<Project> = transaction {
        Project.all().toList()
    }
}

@Serializable
data class ProjectDto(val title: String, val text: String)

fun ProjectService.Project.toDto() = ProjectDto(title, text)