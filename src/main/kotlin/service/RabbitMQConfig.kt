package com.example.service

import com.rabbitmq.client.ConnectionFactory

object RabbitMQConfig {
    private const val HOST = "localhost"
    private const val PORT = 5672
    private const val USERNAME = "guest"
    private const val PASSWORD = "guest"

    // Названия очередей
    const val ORDER_QUEUE = "order.events"
    const val AUDIT_QUEUE = "audit.events"
    const val EMAIL_QUEUE = "email.notifications"

    // Exchange
    const val ORDER_EXCHANGE = "order.exchange"
    const val AUDIT_EXCHANGE = "audit.exchange"

    // Routing keys
    const val ORDER_CREATED_KEY = "order.created"
    const val ORDER_CANCELLED_KEY = "order.cancelled"
    const val AUDIT_LOG_KEY = "audit.log"

    val factory: ConnectionFactory = ConnectionFactory().apply {
        host = System.getenv("RABBITMQ_HOST") ?: HOST
        port = (System.getenv("RABBITMQ_PORT") ?: PORT.toString()).toInt()
        username = System.getenv("RABBITMQ_USERNAME") ?: USERNAME
        password = System.getenv("RABBITMQ_PASSWORD") ?: PASSWORD
    }
}