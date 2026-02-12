package com.example.domain

import java.math.BigDecimal
import java.time.Instant
import java.util.*

data class Product(
    val id: UUID,
    val name: String,
    val description: String?,
    val price: BigDecimal,
    val stock: Int,
    val createdAt: Instant
)