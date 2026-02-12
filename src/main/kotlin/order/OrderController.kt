package com.example.order

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID



fun Route.orderRoutes(orderRepository: OrderRepository) {

    authenticate("auth-jwt") {
        route("/orders") {

            // Создать заказ (только для аутентифицированных пользователей)
            post {
                try {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = UUID.fromString(principal.payload.getClaim("userId").asString())

                    val request = call.receive<CreateOrderRequest>()
                    val order = orderRepository.createOrder(userId, request.items)

                    call.respond(order)
                } catch (e: IllegalArgumentException) {
                    call.respond(io.ktor.http.HttpStatusCode.BadRequest, mapOf("error" to e.message))
                } catch (e: Exception) {
                    call.respond(io.ktor.http.HttpStatusCode.InternalServerError, mapOf("error" to "Internal server error"))
                }
            }

            // Получить историю заказов пользователя
            get {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = UUID.fromString(principal.payload.getClaim("userId").asString())

                val orders = orderRepository.getOrdersByUser(userId)
                call.respond(orders)
            }

            // Получить заказ по ID (только свой заказ)
            get("/{id}") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = UUID.fromString(principal.payload.getClaim("userId").asString())

                val orderId = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: return@get call.respond(
                        io.ktor.http.HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid order id")
                    )

                val order = orderRepository.getOrderById(orderId)
                    ?: return@get call.respond(
                        io.ktor.http.HttpStatusCode.NotFound,
                        mapOf("error" to "Order not found")
                    )

                // Проверяем, что пользователь может просматривать только свои заказы
                if (order.userId != userId) {
                    val role = principal.payload.getClaim("role").asString()
                    if (role != "ADMIN") {
                        return@get call.respond(
                            io.ktor.http.HttpStatusCode.Forbidden,
                            mapOf("error" to "Access denied")
                        )
                    }
                }

                call.respond(order)
            }

            // Отменить заказ
            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = UUID.fromString(principal.payload.getClaim("userId").asString())

                val orderId = call.parameters["id"]?.let { UUID.fromString(it) }
                    ?: return@delete call.respond(
                        io.ktor.http.HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid order id")
                    )

                val deleted = orderRepository.deleteOrder(userId, orderId)
                if (!deleted) {
                    return@delete call.respond(
                        io.ktor.http.HttpStatusCode.NotFound,
                        mapOf("error" to "Order not found or you don't have permission")
                    )
                }

                call.respond(mapOf("message" to "Order cancelled successfully"))
            }
        }
    }

    // Эндпоинт для админа — статистика заказов
    authenticate("auth-jwt") {
        route("/stats") {
            get("/orders") {
                val principal = call.principal<JWTPrincipal>()!!
                val role = principal.payload.getClaim("role").asString()

                if (role != "ADMIN") {
                    return@get call.respond(
                        io.ktor.http.HttpStatusCode.Forbidden,
                        mapOf("error" to "Admin access required")
                    )
                }

                val stats = orderRepository.getOrdersStats()
                call.respond(stats)
            }
        }
    }
}