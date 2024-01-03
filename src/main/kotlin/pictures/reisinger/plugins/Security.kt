package pictures.reisinger.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import pictures.reisinger.db.LoginUserService
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


fun Application.configureSecurity() {
    val jwtAudience = "jwt-audience"
    val jwtDomain = "https://backend.reisinger.pictures/"
    val jwtRealm = "reisinger-backend"
    val jwtSecret = environment.config["jwt.secret"].getString()
    val algorithm = Algorithm.HMAC512(jwtSecret)
    val loginUserService = LoginUserService()
    val jwtVerifier = JWT
        .require(algorithm)
        .withIssuer(jwtDomain)
        .withAudience(jwtAudience)
        .build()
    authentication {
        jwt(AuthProviders.JWT) {
            realm = jwtRealm
            verifier(jwtVerifier)
            validate { credential ->
                val isValid =
                    credential.payload.audience.contains(jwtAudience) && credential.payload.claims["roles"].toString()
                        .contains("user")

                if (isValid) JWTPrincipal(credential.payload) else null
            }
        }
        jwt(AuthProviders.JWT_ADMIN) {
            realm = jwtRealm
            verifier(jwtVerifier)
            validate { credential ->
                val isValid =
                    credential.payload.audience.contains(jwtAudience) && credential.payload.claims["roles"].toString()
                        .contains("admin")

                if (isValid) JWTPrincipal(credential.payload) else null
            }
        }
    }

    authentication {
        basic(AuthProviders.BASIC_JWT_LOGIN) {
            validate { credentials ->
                val roles = loginUserService.findRoles(credentials)
                if (roles.isNullOrEmpty()) {
                    return@validate null
                } else {
                    UserIdRolesPrincipal(credentials.name, roles)

                }
            }
        }
    }

    routing {
        put("register") {
            val userInformation: RegisterUserInformation = call.receive()
            loginUserService.create(userInformation.toUserPasswordCredentials())
            call.response.status(HttpStatusCode.OK)
        }


        authenticate(AuthProviders.BASIC_JWT_LOGIN) {
            post("/login") {
                val principal = call.defaultPrincipalOrThrow()

                val jwt = JWT.create()
                    .withIssuer(jwtDomain)
                    .withSubject(principal.name)
                    .withClaim("roles", principal.roles)
                    .withIssuedAt(Date())
                    .withExpiresAt(LocalDateTime.now().plusHours(12).toInstant(ZoneOffset.ofHours(2)))
                    .withJWTId(UUID.randomUUID().toString())
                    .withNotBefore(Date())
                    .sign(algorithm)

                call.respondText(jwt)
            }
        }
    }
}

fun ApplicationCall.defaultPrincipalOrThrow(): UserIdRolesPrincipal {
    return principalOrThrow()
}

inline fun <reified P : Principal> ApplicationCall.principalOrThrow(): P {
    return principal<P>() ?: throw NotAuthorized401Exception
}

data class UserIdRolesPrincipal(val name: String, val roles: String) : Principal
