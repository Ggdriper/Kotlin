package com.example.domain

import java.math.BigDecimal
import java.util.UUID

data class OrderItem(
    val id: UUID,
    val orderId: UUID,
    val productId: UUID,
    val quantity: Int,
    val price: BigDecimal
)