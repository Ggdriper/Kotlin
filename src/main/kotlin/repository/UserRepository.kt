package com.example.repository

import com.example.db.tables.UsersTable
import com.example.domain.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

class UserRepository {

    fun findByEmail(email: String): User? = transaction {
        UsersTable
            .select { UsersTable.email eq email }
            .map {
                User(
                    id = it[UsersTable.id],
                    email = it[UsersTable.email],
                    passwordHash = it[UsersTable.passwordHash],
                    role = it[UsersTable.role],
                    createdAt = it[UsersTable.createdAt]
                )
            }
            .singleOrNull()
    }

    fun create(
        email: String,
        passwordHash: String,
        role: String
    ): UUID = transaction {
        UsersTable.insert {
            it[id] = UUID.randomUUID()
            it[UsersTable.email] = email
            it[UsersTable.passwordHash] = passwordHash
            it[UsersTable.role] = role
            it[UsersTable.createdAt] = Instant.now()
        }[UsersTable.id]
    }
}