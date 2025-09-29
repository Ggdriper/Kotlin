package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class AuthResponse(val token: String, val refreshToken: String)

@Serializable
data class RefreshTokenRequest(val refreshToken: String)

@Serializable
data class TokenConfig(
    val issuer: String,
    val audience: String,
    val expiresIn: Long,
    val secret: String,
    val refreshExpiresIn: Long
)