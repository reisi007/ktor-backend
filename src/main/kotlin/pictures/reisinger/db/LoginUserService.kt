package pictures.reisinger.db

import at.favre.lib.crypto.bcrypt.BCrypt
import io.ktor.server.auth.UserPasswordCredential
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class LoginUserService {
    object Users : UUIDTable() {
        val email = varchar("name", length = 50).uniqueIndex("uniqueEmail")
        val roles = varchar("roles", length = 100).default("user")
        val pwd = varchar("pwd", length = 60)
    }

    init {
        transaction {
            SchemaUtils.create(Users)
        }
    }

    class User(id: EntityID<UUID>) : UUIDEntity(id) {
        companion object : UUIDEntityClass<User>(Users)

        var email by Users.email
        var pwdHash by Users.pwd
        var roles by Users.roles
    }

    fun create(user: UserPasswordCredential) = transaction {
        User.new {
            email = user.name
            pwdHash = passwordHash(user.password)
        }
    }

    fun createAdmin(user: UserPasswordCredential) = transaction {
        User.new {
            email = user.name
            roles = listOf("user", "admin").toRolesString()
            pwdHash = passwordHash(user.password)
        }
    }

    fun findRoles(user: UserPasswordCredential): String? = transaction {
        val dbUser = User.find { Users.email.eq(user.name) }
            .firstOrNull()
        if (dbUser == null) return@transaction null
        val isPwdValid = passwordVerify(user.password, dbUser.pwdHash)
        if (!isPwdValid) return@transaction null
        return@transaction dbUser.roles
    }
}


private val pwdHasher = BCrypt.withDefaults()
private val pwdVerifier = BCrypt.verifyer()
private val bcryptCost = 12

fun passwordHash(pwd: String): String {
    return pwdHasher.hashToString(bcryptCost, pwd.toCharArray())
}

fun passwordVerify(pwd: String, hashedPwd: String): Boolean {
    return pwdVerifier.verify(pwd.toCharArray(), hashedPwd).verified
}

typealias RolesString = String

fun RolesString.asRolesList(): List<String> = split(",")
fun Iterable<String>.toRolesString(): RolesString = joinToString(",")
