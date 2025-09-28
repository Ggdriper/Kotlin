package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.itemRoutes(repo: ItemRepository) {

    route("/items") {

        // GET /items?limit=5&name=foo
        get {
            val limitParam = call.request.queryParameters["limit"]
            val nameParam = call.request.queryParameters["name"]

            val limit = limitParam?.toIntOrNull()
            if (limitParam != null && limit == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(error = "invalid_query_param", details = "limit must be integer")
                )
                return@get
            }

            val items = repo.list(limit = limit, nameFilter = nameParam)
            call.respond(items)
        }

        // POST /items
        post {
            val payload = try {
                call.receive<CreateItemRequest>()
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(error = "invalid_json", details = e.message)
                )
                return@post
            }

            if (payload.name.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(error = "validation_error", details = "name must not be blank")
                )
                return@post
            }

            // Если id не указан, генерируем автоматически
            val itemId = payload.id ?: java.util.UUID.randomUUID().toString()
            val created = Item(id = itemId, name = payload.name, description = payload.description)


            repo.add(created)


            call.response.headers.append(HttpHeaders.Location, "/items/${created.id}")
            call.respond(HttpStatusCode.Created, created)
        }

        // DELETE /items/{id}
        delete("{id}") {
            val id = call.parameters["id"] ?: run {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(error = "missing_id"))
                return@delete
            }

            val deleted = repo.delete(id)
            if (deleted) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, ErrorResponse(error = "not_found", details = "Item $id not found"))
            }
        }
    }
}