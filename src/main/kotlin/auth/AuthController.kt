package com.example.auth

import com.example.repository.UserRepository
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes() {

    val authService = AuthService(UserRepository())

    route("/auth") {

        post("/register") {
            val req = call.receive<RegisterRequest>()
            val token = authService.register(req.email, req.password)
            call.respond(AuthResponse(token))
        }

        post("/login") {
            val req = call.receive<LoginRequest>()
            val token = authService.login(req.email, req.password)
            call.respond(AuthResponse(token))
        }
        post("/token-by-email") {
            val req = call.receive<TokenByEmailRequest>()
            val token = authService.getTestToken(req.email)
            call.respond(AuthResponse(token))
        }
    }
}