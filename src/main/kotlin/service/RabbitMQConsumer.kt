package com.example.service

import com.example.repository.AuditRepository
import com.rabbitmq.client.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.UUID

class RabbitMQConsumer(
    private val auditRepository: AuditRepository = AuditRepository()
) {
    private val connection = RabbitMQConfig.factory.newConnection()
    private val channel = connection.createChannel()
    private val json = Json { ignoreUnknownKeys = true }

    fun start() {
        println("🚀 Запуск RabbitMQ Consumer...")

        // Настраиваем обработчик для очереди заказов
        val orderConsumer = OrderConsumer()
        channel.basicConsume(RabbitMQConfig.ORDER_QUEUE, true, orderConsumer)

        // Настраиваем обработчик для очереди аудита
        val auditConsumer = AuditConsumer(auditRepository)
        channel.basicConsume(RabbitMQConfig.AUDIT_QUEUE, true, auditConsumer)

        // Настраиваем обработчик для email уведомлений
        val emailConsumer = EmailConsumer()
        channel.basicConsume(RabbitMQConfig.EMAIL_QUEUE, true, emailConsumer)

        println("✅ RabbitMQ Consumer запущен и слушает очереди")
    }

    fun stop() {
        channel.close()
        connection.close()
        println("🛑 RabbitMQ Consumer остановлен")
    }

    // Consumer для заказов
    private class OrderConsumer : DefaultConsumer(null) {
        override fun handleDelivery(
            consumerTag: String,
            envelope: Envelope,
            properties: AMQP.BasicProperties,
            body: ByteArray
        ) {
            try {
                val message = String(body, Charsets.UTF_8)
                println("\n📥 Получено сообщение из очереди заказов:")
                println("Routing Key: ${envelope.routingKey}")
                println("Сообщение: $message")

                when (envelope.routingKey) {
                    RabbitMQConfig.ORDER_CREATED_KEY -> {
                        val event = Json.decodeFromString<OrderCreatedEvent>(message)
                        handleOrderCreated(event)
                    }
                    RabbitMQConfig.ORDER_CANCELLED_KEY -> {
                        val event = Json.decodeFromString<OrderCancelledEvent>(message)
                        handleOrderCancelled(event)
                    }
                }

                // Здесь можно добавить логику для отправки email
                sendEmailNotification(envelope.routingKey, message)

            } catch (e: Exception) {
                println("❌ Ошибка обработки сообщения: ${e.message}")
                e.printStackTrace()
            }
        }

        private fun handleOrderCreated(event: OrderCreatedEvent) {
            println("🎉 Обработка созданного заказа:")
            println("   ID заказа: ${event.orderId}")
            println("   ID пользователя: ${event.userId}")
            println("   Сумма: ${event.totalPrice}")
            println("   Количество товаров: ${event.itemsCount}")

            // Здесь можно добавить дополнительную бизнес-логику
            // Например: обновление аналитики, отправка в ERP систему и т.д.
        }

        private fun handleOrderCancelled(event: OrderCancelledEvent) {
            println("❌ Обработка отмененного заказа:")
            println("   ID заказа: ${event.orderId}")
            println("   Причина: ${event.reason}")

            // Дополнительная логика обработки отмены
        }

        private fun sendEmailNotification(routingKey: String, eventData: String) {
            println("📧 [ФЕЙКОВЫЙ EMAIL] Отправка уведомления для события: $routingKey")
            println("   Данные: $eventData")
            // В реальном приложении здесь была бы интеграция с email сервисом
        }
    }

    // Consumer для аудита
    private class AuditConsumer(
        private val auditRepository: AuditRepository
    ) : DefaultConsumer(null) {
        override fun handleDelivery(
            consumerTag: String,
            envelope: Envelope,
            properties: AMQP.BasicProperties,
            body: ByteArray
        ) {
            try {
                val message = String(body, Charsets.UTF_8)
                println("\n📥 Получено аудит-событие:")

                val event = Json.decodeFromString<AuditEvent>(message)

                // Сохраняем в БД через репозиторий
                auditRepository.logEvent(
                    userId = event.userId,
                    action = event.action,
                    entityType = event.entityType,
                    entityId = event.entityId,
                    details = event.details
                )

                println("✅ Аудит-событие сохранено в БД: ${event.action}")

            } catch (e: Exception) {
                println("❌ Ошибка обработки аудит-события: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // Consumer для email (заглушка)
    private class EmailConsumer : DefaultConsumer(null) {
        override fun handleDelivery(
            consumerTag: String,
            envelope: Envelope,
            properties: AMQP.BasicProperties,
            body: ByteArray
        ) {
            val message = String(body, Charsets.UTF_8)
            println("\n📧 [EMAIL WORKER] Получено сообщение для отправки email:")
            println("   Содержимое: $message")

            // Фейковая отправка email
            println("   [ИМИТАЦИЯ] Email отправлен успешно!")
        }
    }
}