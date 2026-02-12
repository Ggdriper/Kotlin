package com.example.service

import com.rabbitmq.client.BuiltinExchangeType
import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import kotlinx.serialization.Contextual
import kotlinx.serialization.json.Json
import java.util.UUID

class RabbitMQProducer {
    private var connection: com.rabbitmq.client.Connection? = null
    private var channel: com.rabbitmq.client.Channel? = null

    init {
        try {
            connection = RabbitMQConfig.factory.newConnection()
            channel = connection?.createChannel()

            // Создаем exchange для заказов
            channel?.exchangeDeclare(RabbitMQConfig.ORDER_EXCHANGE, BuiltinExchangeType.TOPIC, true)

            // Создаем exchange для аудита
            channel?.exchangeDeclare(RabbitMQConfig.AUDIT_EXCHANGE, BuiltinExchangeType.TOPIC, true)

            // Создаем очереди
            channel?.queueDeclare(RabbitMQConfig.ORDER_QUEUE, true, false, false, null)
            channel?.queueDeclare(RabbitMQConfig.AUDIT_QUEUE, true, false, false, null)
            channel?.queueDeclare(RabbitMQConfig.EMAIL_QUEUE, true, false, false, null)

            // Биндим очереди к exchange с routing keys
            channel?.queueBind(
                RabbitMQConfig.ORDER_QUEUE,
                RabbitMQConfig.ORDER_EXCHANGE,
                "#"  // Получаем все сообщения
            )

            channel?.queueBind(
                RabbitMQConfig.AUDIT_QUEUE,
                RabbitMQConfig.AUDIT_EXCHANGE,
                RabbitMQConfig.AUDIT_LOG_KEY
            )

        } catch (e: Exception) {
            println("❌ Ошибка при инициализации RabbitMQ Producer: ${e.message}")
            throw e // Пробрасываем исключение дальше
        }
    }

    fun sendOrderCreatedEvent(orderId: UUID, userId: UUID, totalPrice: String, itemsCount: Int) {
        try {
            val event = OrderCreatedEvent(
                eventId = UUID.randomUUID(),
                orderId = orderId,
                userId = userId,
                totalPrice = totalPrice,
                itemsCount = itemsCount,
                timestamp = System.currentTimeMillis(),
                eventType = "ORDER_CREATED"
            )

            val message = Json.encodeToString(event)

            channel?.basicPublish(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CREATED_KEY,
                null,
                message.toByteArray()
            )

            println("📤 Отправлено событие: ORDER_CREATED для заказа $orderId")
        } catch (e: Exception) {
            println("❌ Ошибка отправки события ORDER_CREATED: ${e.message}")
        }
    }

    fun sendOrderCancelledEvent(orderId: UUID, userId: UUID, reason: String = "User cancelled") {
        try {
            val event = OrderCancelledEvent(
                eventId = UUID.randomUUID(),
                orderId = orderId,
                userId = userId,
                reason = reason,
                timestamp = System.currentTimeMillis(),
                eventType = "ORDER_CANCELLED"
            )

            val message = Json.encodeToString(event)

            channel?.basicPublish(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CANCELLED_KEY,
                null,
                message.toByteArray()
            )

            println("📤 Отправлено событие: ORDER_CANCELLED для заказа $orderId")
        } catch (e: Exception) {
            println("❌ Ошибка отправки события ORDER_CANCELLED: ${e.message}")
        }
    }

    fun sendAuditEvent(
        userId: UUID?,
        action: String,
        entityType: String,
        entityId: UUID?,
        details: String?
    ) {
        try {
            val event = AuditEvent(
                eventId = UUID.randomUUID(),
                userId = userId,
                action = action,
                entityType = entityType,
                entityId = entityId,
                details = details,
                timestamp = System.currentTimeMillis()
            )

            val message = Json.encodeToString(event)

            channel?.basicPublish(
                RabbitMQConfig.AUDIT_EXCHANGE,
                RabbitMQConfig.AUDIT_LOG_KEY,
                null,
                message.toByteArray()
            )

            println("📤 Отправлено аудит-событие: $action")
        } catch (e: Exception) {
            println("❌ Ошибка отправки аудит-события: ${e.message}")
        }
    }

    fun close() {
        try {
            channel?.close()
            connection?.close()
        } catch (e: Exception) {
            // Игнорируем ошибки при закрытии
        }
    }
}

// DTO для событий (остаются без изменений)
@kotlinx.serialization.Serializable
data class OrderCreatedEvent(
    @Contextual
    val eventId: UUID,
    @Contextual
    val orderId: UUID,
    @Contextual
    val userId: UUID,
    val totalPrice: String,
    val itemsCount: Int,
    val timestamp: Long,
    val eventType: String
)

@kotlinx.serialization.Serializable
data class OrderCancelledEvent(
    @Contextual
    val eventId: UUID,
    @Contextual
    val orderId: UUID,
    @Contextual
    val userId: UUID,
    val reason: String,
    val timestamp: Long,
    val eventType: String
)

@kotlinx.serialization.Serializable
data class AuditEvent(
    @Contextual
    val eventId: UUID,
    @Contextual
    val userId: UUID?,
    val action: String,
    val entityType: String,
    @Contextual
    val entityId: UUID?,
    val details: String?,
    val timestamp: Long
)