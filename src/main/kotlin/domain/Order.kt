package com.example.domain

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class Order(
    val id: UUID,
    val userId: UUID,
    val status: String,
    val totalPrice: BigDecimal,
    val createdAt: Instant
)