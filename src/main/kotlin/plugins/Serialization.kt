package com.example.plugins

import com.example.auth.BigDecimalSerializer
import com.example.order.UUIDSerializer
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.Contextual
import java.math.BigDecimal
import java.util.UUID

fun Application.configureSerialization() {
    val module = SerializersModule {
        contextual(UUID::class, UUIDSerializer)
        contextual(BigDecimal::class, BigDecimalSerializer)
    }

    install(ContentNegotiation) {
        json(
            Json {
                serializersModule = module
                prettyPrint = true
                isLenient = true
            }
        )
    }
}