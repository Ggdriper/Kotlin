package com.example.auth

import com.example.repository.UserRepository
import org.mindrot.jbcrypt.BCrypt
import java.util.*

class AuthService(
    private val userRepository: UserRepository
) {

    fun register(email: String, password: String): String {
        val existing = userRepository.findByEmail(email)
        require(existing == null) { "User already exists" }

        val hash = BCrypt.hashpw(password, BCrypt.gensalt())
        val userId = userRepository.create(email, hash, "USER")

        return JwtConfig.generateToken(userId, "USER")
    }

    fun login(email: String, password: String): String {
        val user = userRepository.findByEmail(email)
            ?: throw IllegalArgumentException("Invalid credentials")

        if (!BCrypt.checkpw(password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid credentials")
        }

        return JwtConfig.generateToken(user.id, user.role)
    }

    fun getTestToken(email: String): String {
        val user = userRepository.findByEmail(email)
            ?: throw IllegalArgumentException("User not found")

        return JwtConfig.generateToken(user.id, user.role)
    }
}