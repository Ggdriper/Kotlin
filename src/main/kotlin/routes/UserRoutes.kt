package com.example.routes

import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.database.User
import com.example.database.Users
import com.example.models.UserRequest
import com.example.models.UserResponse
import com.example.models.UserListResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.userRoutes() {

    authenticate("auth-jwt") {
        get("/users") {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 10

            val principal = call.principal<JWTPrincipal>()
            val userRole = principal?.getClaim("role", String::class)

            if (userRole != "ADMIN") {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Insufficient permissions"))
                return@get
            }

            val users = transaction {
                User.all()
                    .orderBy(Users.id to SortOrder.ASC)
                    .limit(pageSize, ((page - 1) * pageSize).toLong())
                    .map { UserResponse(it.id.value, it.username, it.role) }
            }

            val total = transaction { User.count() }

            call.respond(UserListResponse(users, total, page, pageSize))
        }

        get("/users/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))
                return@get
            }

            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.getClaim("userId", Int::class)
            val userRole = principal?.getClaim("role", String::class)

            if (userRole != "ADMIN" && userId != id) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Insufficient permissions"))
                return@get
            }

            val user = transaction { User.findById(id) }
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                return@get
            }

            call.respond(UserResponse(user.id.value, user.username, user.role))
        }

        post("/users") {
            val principal = call.principal<JWTPrincipal>()
            val userRole = principal?.getClaim("role", String::class)

            if (userRole != "ADMIN") {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Insufficient permissions"))
                return@post
            }

            val request = call.receive<UserRequest>()

            val existingUser = transaction { User.find { Users.username eq request.username }.firstOrNull() }
            if (existingUser != null) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "Username already exists"))
                return@post
            }

            val hashedPassword = BCrypt.withDefaults().hashToString(12, request.password.toCharArray())

            val user = transaction {
                User.new {
                    username = request.username
                    password = hashedPassword
                    role = request.role
                }
            }

            call.respond(HttpStatusCode.Created, UserResponse(user.id.value, user.username, user.role))
        }

        put("/users/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))
                return@put
            }

            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.getClaim("userId", Int::class)
            val userRole = principal?.getClaim("role", String::class)

            if (userRole != "ADMIN" && userId != id) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Insufficient permissions"))
                return@put
            }

            val user = transaction { User.findById(id) }
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                return@put
            }

            val request = call.receive<UserRequest>()

            if (userRole != "ADMIN" && request.role != user.role) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot change role"))
                return@put
            }

            transaction {
                user.username = request.username
                if (request.password.isNotBlank()) {
                    user.password = BCrypt.withDefaults().hashToString(12, request.password.toCharArray())
                }
                user.role = request.role
            }

            call.respond(UserResponse(user.id.value, user.username, user.role))
        }

        delete("/users/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))
                return@delete
            }

            val principal = call.principal<JWTPrincipal>()
            val userRole = principal?.getClaim("role", String::class)

            if (userRole != "ADMIN") {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Insufficient permissions"))
                return@delete
            }

            val user = transaction { User.findById(id) }
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                return@delete
            }

            transaction {
                user.delete()
            }

            call.respond(HttpStatusCode.OK, mapOf("message" to "User deleted successfully"))
        }
    }
}