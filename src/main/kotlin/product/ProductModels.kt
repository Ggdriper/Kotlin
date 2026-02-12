package com.example.product

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal


@Serializable
data class ProductResponse(
    val id: String,
    val name: String,
    val description: String?,
    @Contextual
    val price: Double,
    val stock: Int
)

@Serializable
data class CreateProductRequest(
    val name: String,
    val description: String?,
    @Contextual
    val price: BigDecimal,
    val stock: Int
)

@Serializable
data class UpdateProductRequest(
    val name: String?,
    val description: String?,
    @Contextual
    val price: BigDecimal?,
    val stock: Int?
)