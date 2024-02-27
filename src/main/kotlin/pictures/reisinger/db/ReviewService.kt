package pictures.reisinger.db

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

class ReviewService {

    init {
        transaction {
            SchemaUtils.create(Reviews)
        }
    }

    object Reviews : LongIdTable() {
        val name = varchar("name", length = 256)
        val public = text("public", eagerLoading = true)
        val private = text("public", eagerLoading = true).nullable()
        val date = date("reviewDate")
    }

    class Review(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<Review>(Reviews)

        var name by Reviews.name
        var public by Reviews.public
        var private by Reviews.private
        var date by Reviews.date
    }

    fun findAll() = transaction {
        Review.all()
            .map { it.toDto() }
    }

    fun insertReview(review: ReviewInputDto) = transaction {
        Review.new {
            name = review.name
            public = review.public
            private = review.private
            date = LocalDate.now()
        }
    }
}

@Serializable
data class ReviewPublicOutputDto(
    val id: Long,
    val name: String,
    val public: String,
    val date: LocalDateAsString,
)

@Serializable
data class ReviewPrivateOutputDto(
    val id: Long,
    val name: String,
    val public: String,
    val private: String?,
    val date: LocalDateAsString,
)

@Serializable
data class ReviewInputDto(
    val name: String,
    val private: String,
    val public: String
)

fun ReviewService.Review.toDto() = ReviewPrivateOutputDto(
    id = id.value,
    name = name,
    public = public,
    private = private,
    date = date
)

fun ReviewPrivateOutputDto.asPublic() = ReviewPublicOutputDto(
    id,
    name,
    public,
    date
)
