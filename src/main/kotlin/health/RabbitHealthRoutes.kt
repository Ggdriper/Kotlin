package com.example.health

import com.example.service.RabbitMQConfig
import com.example.service.RabbitMQService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class RabbitMQHealthResponse(
    val connected: Boolean,
    val queues: List<String>,
    val messageCount: Map<String, Long>
)

fun Route.rabbitHealthRoutes() {
    route("/health/rabbitmq") {
        get {
            try {
                val rabbitMQService = RabbitMQService()


                call.respond(
                    RabbitMQHealthResponse(
                        connected = true,
                        queues = listOf(
                            RabbitMQConfig.ORDER_QUEUE,
                            RabbitMQConfig.AUDIT_QUEUE,
                            RabbitMQConfig.EMAIL_QUEUE
                        ),
                        messageCount = mapOf(
                            RabbitMQConfig.ORDER_QUEUE to 0L,
                            RabbitMQConfig.AUDIT_QUEUE to 0L,
                            RabbitMQConfig.EMAIL_QUEUE to 0L
                        )
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    RabbitMQHealthResponse(
                        connected = false,
                        queues = emptyList(),
                        messageCount = emptyMap()
                    )
                )
            }
        }

        get("/test") {
            val rabbitMQService = RabbitMQService()

            // Отправляем тестовое сообщение
            rabbitMQService.sendAuditEvent(
                userId = null,
                action = "TEST_EVENT",
                entityType = "SYSTEM",
                details = "Test message from health check"
            )

            call.respond(mapOf("message" to "Test message sent to RabbitMQ"))
        }
    }
}