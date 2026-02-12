package com.example.order

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.util.UUID

@Serializable
data class CreateOrderRequest(
    val items: List<OrderItemRequest>
)

@Serializable
data class OrderItemRequest(
    @Contextual
    val productId: UUID,
    val quantity: Int
)

@Serializable
data class OrderResponse(
    @Contextual
    val id: UUID,
    @Contextual
    val userId: UUID,
    val status: String,
    @Contextual
    val totalPrice: BigDecimal,
    val createdAt: String,
    val items: List<OrderItemResponse>
)

@Serializable
data class OrderItemResponse(
    @Contextual
    val productId: UUID,
    val productName: String,
    val quantity: Int,
    @Contextual
    val price: BigDecimal
)

@Serializable
data class OrdersStatsResponse(
    val totalOrders: Int,
    val totalAmount: Double,
    val averageOrderValue: Double
)