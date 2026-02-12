package com.example.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

object JwtConfig {

    private const val secret = "very_secret_key"
    private const val issuer = "ktor-shop"
    private const val audience = "ktor-users"
    private const val validityMs = 36_000_00 // 1 час

    private val algorithm = Algorithm.HMAC256(secret)

    fun generateToken(userId: UUID, role: String): String =
        JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("userId", userId.toString())
            .withClaim("role", role)
            .withExpiresAt(Date(System.currentTimeMillis() + validityMs))
            .sign(algorithm)

    fun verifier() =
        JWT.require(algorithm)
            .withIssuer(issuer)
            .withAudience(audience)
            .build()
}