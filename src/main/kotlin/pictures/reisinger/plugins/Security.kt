package pictures.reisinger.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.BasicAuthenticationProvider
import io.ktor.server.auth.Principal
import io.ktor.server.auth.UserPasswordCredential
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.auth.basic
import io.ktor.server.auth.jwt.JWTAuthenticationProvider
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import pictures.reisinger.db.LoginUserService
import pictures.reisinger.db.asRolesList
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@Serializable
data class RegisterUserInformation(val email: String, val password: String) {
    fun toUserPasswordCredentials(): UserPasswordCredential {
        return UserPasswordCredential(email, password)
    }
}

object AuthProviders {
    const val JWT = "jwt"
    const val JWT_ADMIN = "jwtAdmin"
    const val BASIC_JWT_LOGIN = "basicJwtLogin"
}

private const val jwtAudience = "jwt-audience"
private const val jwtDomain = "https://backend.reisinger.pictures/"
private const val jwtRealm = "reisinger-backend"

fun Application.configureSecurity() {
    val jwtSecret = environment.config["jwt.secret"].getString()
    val algorithm = Algorithm.HMAC512(jwtSecret)

    val jwtVerifier = JWT
        .require(algorithm)
        .withIssuer(jwtDomain)
        .withAudience(jwtAudience)
        .acceptLeeway(1) // Accept a leeway of 1 second
        .build()
    authentication {
        jwt(AuthProviders.JWT) { configureAuth(jwtVerifier, "user") }
        jwt(AuthProviders.JWT_ADMIN) { configureAuth(jwtVerifier, "admin") }
    }

    authentication {
        basic(AuthProviders.BASIC_JWT_LOGIN) { configureAuth() }
    }

    routing {
        put("register") {
            val loginUserService = LoginUserService()
            val userInformation: RegisterUserInformation = call.receive()
            loginUserService.create(userInformation.toUserPasswordCredentials())
            call.response.status(HttpStatusCode.OK)
        }

        authenticate(AuthProviders.BASIC_JWT_LOGIN) {
            post("/login") {
                val principal = call.defaultPrincipalOrThrow()

                call.respondText(principal.buildJwt(algorithm))
            }
        }
    }
}

private fun UserIdRolesPrincipal.buildJwt(algorithm: Algorithm): String = JWT.create()
    .withIssuer(jwtDomain)
    .withSubject(name)
    .withClaim("roles", roles)
    .withIssuedAt(Date())
    .withAudience(jwtAudience)
    .withExpiresAt(LocalDateTime.now().plusHours(12).toInstant(ZoneOffset.ofHours(2)))
    .withJWTId(UUID.randomUUID().toString())
    .withNotBefore(Date())
    .sign(algorithm)

@Serializable
data class LoginInfo(val user: String, val password: String)


private fun BasicAuthenticationProvider.Config.configureAuth() {
    validate { credentials ->
        val roles = LoginUserService().findRoles(credentials)?.asRolesList()
        if (roles.isNullOrEmpty()) return@validate null
        else UserIdRolesPrincipal(credentials.name, roles)
    }
}

private fun JWTAuthenticationProvider.Config.configureAuth(jwtVerifier: JWTVerifier, requiredRole: String) {
    realm = jwtRealm
    verifier(jwtVerifier)

    validate { credentials ->
        val roles = credentials.getListClaim("roles", String::class)
        val isValid = credentials.payload.audience.contains(jwtAudience) && roles.contains(requiredRole)
        if (isValid) UserIdRolesPrincipal(credentials.subject.toString(), roles) else null
    }
}

fun ApplicationCall.defaultPrincipalOrThrow(): UserIdRolesPrincipal {
    return principalOrThrow()
}

inline fun <reified P : Principal> ApplicationCall.principalOrThrow(): P {
    return principal<P>() ?: throw NotAuthorized401Exception
}

data class UserIdRolesPrincipal(val name: String, val roles: List<String>) : Principal
