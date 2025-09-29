package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class OrderItemRequest(
    val productId: Int,
    val quantity: Int
)

@Serializable
data class OrderRequest(
    val items: List<OrderItemRequest>
)

@Serializable
data class OrderResponse(
    val id: Int,
    val userId: Int,
    val total: Double,
    val status: OrderStatus,
    val createdAt: String,
    val items: List<OrderItemResponse>
)

@Serializable
data class OrderItemResponse(
    val productId: Int,
    val productName: String,
    val quantity: Int,
    val price: Double
)

@Serializable
data class OrderListResponse(
    val orders: List<OrderResponse>,
    val total: Int,
    val page: Int,
    val pageSize: Int
)

@Serializable
enum class OrderStatus {
    PENDING, PROCESSING, COMPLETED, CANCELLED
}