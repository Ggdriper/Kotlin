package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class ProductRequest(
    val name: String,
    val description: String,
    val price: Double,
    val stock: Int
)

@Serializable
data class ProductResponse(
    val id: Int,
    val name: String,
    val description: String,
    val price: Double,
    val stock: Int
)

@Serializable
data class ProductListResponse(
    val products: List<ProductResponse>,
    val total: Int,
    val page: Int,
    val pageSize: Int
)