package com.example

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class Item(val id: String, val name: String, val description: String? = null)

@Serializable
data class CreateItemRequest(
    val name: String,
    val description: String? = null,
    val id: String? = null
)

@Serializable
data class ErrorResponse(val error: String, val details: String? = null)