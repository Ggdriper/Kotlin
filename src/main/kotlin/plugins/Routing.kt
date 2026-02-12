package com.example.plugins


import com.example.auth.authRoutes
import com.example.health.healthRoutes
import com.example.health.rabbitHealthRoutes
import com.example.order.OrderRepository
import com.example.order.orderRoutes
import com.example.product.productRoutes

import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {

    val orderRepository = OrderRepository()

    routing {
        healthRoutes()
        rabbitHealthRoutes()
        authRoutes()
        productRoutes()
        orderRoutes(orderRepository)
    }
}


