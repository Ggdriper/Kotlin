package com.example.routes

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.database.RefreshToken
import com.example.database.RefreshTokens
import com.example.database.User
import com.example.database.Users
import com.example.models.AuthResponse
import com.example.models.LoginRequest
import com.example.models.RefreshTokenRequest
import com.example.models.TokenConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

fun Route.authRoutes(tokenConfig: TokenConfig) {

    post("/login") {
        try {
            val request = call.receive<LoginRequest>()
            println("Login attempt for user: ${request.username}")

            val user = transaction {
                User.find { Users.username eq request.username }.firstOrNull()
            }

            if (user == null) {
                println("User not found: ${request.username}")
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
                return@post
            }

            println("Found user: ${user.username}, password hash: ${user.password}")

            // Простая проверка пароля для отладки
            val passwordMatches = if (user.password.startsWith("\$2a\$")) {
                // Это BCrypt хеш
                BCrypt.verifyer().verify(request.password.toCharArray(), user.password).verified
            } else {
                // Прямое сравнение для отладки
                request.password == user.password
            }

            if (!passwordMatches) {
                println("Password mismatch for user: ${request.username}")
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
                return@post
            }

            val token = generateJWT(tokenConfig, user.id.value, user.role.name)
            val refreshToken = generateRefreshToken()

            println("Generating tokens for user: ${user.username}")

            // Save refresh token
            transaction {
                try {
                    RefreshToken.new {
                        this.userId = user.id
                        this.token = refreshToken
                        this.expiresAt = Instant.now().plusSeconds(7 * 24 * 3600).epochSecond
                    }
                    println("Refresh token saved successfully")
                } catch (e: ExposedSQLException) {
                    println("Error saving refresh token: ${e.message}")
                    throw e
                }
            }

            call.respond(AuthResponse(token, refreshToken))

        } catch (e: Exception) {
            println("Error in login: ${e.message}")
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Internal server error: ${e.message}"))
        }
    }

    post("/refresh") {
        try {
            val request = call.receive<RefreshTokenRequest>()

            val refreshToken = transaction {
                RefreshToken.find { RefreshTokens.token eq request.refreshToken }.firstOrNull()
            }

            if (refreshToken == null || refreshToken.expiresAt < Instant.now().epochSecond) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid or expired refresh token"))
                return@post
            }

            val user = transaction { User.findById(refreshToken.userId.value) }
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "User not found"))
                return@post
            }

            val newToken = generateJWT(tokenConfig, user.id.value, user.role.name)
            val newRefreshToken = generateRefreshToken()

            // Update refresh token
            transaction {
                refreshToken.token = newRefreshToken
                refreshToken.expiresAt = Instant.now().plusSeconds(7 * 24 * 3600).epochSecond
            }

            call.respond(AuthResponse(newToken, newRefreshToken))

        } catch (e: Exception) {
            println("Error in refresh: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Internal server error"))
        }
    }

    authenticate("auth-jwt") {
        post("/logout") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.getClaim("userId", Int::class)

                if (userId != null) {
                    transaction {
                        RefreshToken.find { RefreshTokens.userId eq userId }.forEach { it.delete() }
                    }
                }

                call.respond(HttpStatusCode.OK, mapOf("message" to "Logged out successfully"))

            } catch (e: Exception) {
                println("Error in logout: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Internal server error"))
            }
        }
    }
}

private fun generateJWT(tokenConfig: TokenConfig, userId: Int, role: String): String {
    return JWT.create()
        .withAudience(tokenConfig.audience)
        .withIssuer(tokenConfig.issuer)
        .withClaim("userId", userId)
        .withClaim("role", role)
        .withExpiresAt(Date(System.currentTimeMillis() + tokenConfig.expiresIn))
        .sign(Algorithm.HMAC256(tokenConfig.secret))
}

private fun generateRefreshToken(): String {
    return UUID.randomUUID().toString()
}