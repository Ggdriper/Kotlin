package com.example.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import com.example.models.TokenConfig

fun Application.configureAuthentication() {
    val tokenConfig = TokenConfig(
        issuer = "ktor-server",
        audience = "ktor-client",
        expiresIn = 3600L * 1000L,
        secret = "your-secret-key-here-change-in-production",
        refreshExpiresIn = 7L * 24L * 3600L * 1000L
    )

    install(Authentication) {
        jwt("auth-jwt") {
            realm = "ktor server"
            verifier(
                JWT
                    .require(Algorithm.HMAC256(tokenConfig.secret))
                    .withAudience(tokenConfig.audience)
                    .withIssuer(tokenConfig.issuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("userId").asInt() != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}