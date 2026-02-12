package com.example.product

import com.example.repository.ProductRepository
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.productRoutes() {
    val service = ProductService(ProductRepository())

    route("/products") {
        get {
            call.respond(service.getAll())
        }

        get("/{id}") {
            val id = UUID.fromString(call.parameters["id"])
            val product = service.getById(id)
                ?: return@get call.respondText("Not found", status = io.ktor.http.HttpStatusCode.NotFound)
            call.respond(product)
        }

        authenticate("auth-jwt") {
            post {
                val role = call.principal<JWTPrincipal>()!!.payload.getClaim("role").asString()
                if (role != "ADMIN") return@post call.respond(io.ktor.http.HttpStatusCode.Forbidden)

                val req = call.receive<CreateProductRequest>()
                service.create(req)
                call.respond(io.ktor.http.HttpStatusCode.Created)
            }

            put("/{id}") {
                val role = call.principal<JWTPrincipal>()!!.payload.getClaim("role").asString()
                if (role != "ADMIN") return@put call.respond(io.ktor.http.HttpStatusCode.Forbidden)

                val id = UUID.fromString(call.parameters["id"])
                val req = call.receive<UpdateProductRequest>()
                service.update(id, req)
                call.respond(io.ktor.http.HttpStatusCode.OK)
            }

            delete("/{id}") {
                val role = call.principal<JWTPrincipal>()!!.payload.getClaim("role").asString()
                if (role != "ADMIN") return@delete call.respond(io.ktor.http.HttpStatusCode.Forbidden)

                val id = UUID.fromString(call.parameters["id"])
                service.delete(id)
                call.respond(io.ktor.http.HttpStatusCode.NoContent)
            }
        }
    }
}