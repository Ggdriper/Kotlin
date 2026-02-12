package com.example.service

import java.util.UUID

class RabbitMQService {
    private var producer: RabbitMQProducer? = null
    private var consumer: RabbitMQConsumer? = null

    init {
        println("🔧 Инициализация RabbitMQService...")
        try {
            producer = RabbitMQProducer()
            println("✅ RabbitMQ Producer создан")
        } catch (e: Exception) {
            println("❌ Не удалось создать RabbitMQ Producer: ${e.message}")
            producer = null
        }
    }

    fun startConsumer() {
        if (producer == null) {
            println("❌ Нельзя запустить Consumer: Producer не инициализирован")
            return
        }

        try {
            consumer = RabbitMQConsumer()
            consumer?.start()
        } catch (e: Exception) {
            println("❌ Не удалось запустить Consumer: ${e.message}")
        }
    }

    fun stopConsumer() {
        consumer?.stop()
        consumer = null
    }

    fun sendOrderCreated(orderId: UUID, userId: UUID, totalPrice: String, itemsCount: Int) {
        producer?.sendOrderCreatedEvent(orderId, userId, totalPrice, itemsCount)
            ?: println("⚠️ RabbitMQ не доступен, событие не отправлено: ORDER_CREATED")
    }

    fun sendOrderCancelled(orderId: UUID, userId: UUID, reason: String = "User cancelled") {
        producer?.sendOrderCancelledEvent(orderId, userId, reason)
            ?: println("⚠️ RabbitMQ не доступен, событие не отправлено: ORDER_CANCELLED")
    }

    fun sendAuditEvent(
        userId: UUID?,
        action: String,
        entityType: String,
        entityId: UUID? = null,
        details: String? = null
    ) {
        producer?.sendAuditEvent(userId, action, entityType, entityId, details)
            ?: println("⚠️ RabbitMQ не доступен, аудит-событие не отправлено: $action")
    }

    fun close() {
        try {
            producer?.close()
            stopConsumer()
            println("🔌 RabbitMQService закрыт")
        } catch (e: Exception) {
            println("❌ Ошибка при закрытии RabbitMQService: ${e.message}")
        }
    }
}