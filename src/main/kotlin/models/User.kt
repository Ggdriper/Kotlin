package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class UserRequest(
    val username: String,
    val password: String,
    val role: UserRole = UserRole.USER
)

@Serializable
data class UserResponse(
    val id: Int,
    val username: String,
    val role: UserRole
)

@Serializable
enum class UserRole {
    USER, ADMIN
}

@Serializable
data class UserListResponse(
    val users: List<UserResponse>,
    val total: Long,
    val page: Int,
    val pageSize: Int
)