package com.example.health

import com.example.cache.CacheService
import com.example.cache.RedisClientProvider
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class RedisHealthResponse(
    val healthy: Boolean,
    val keys_count: Int,
    val keys: List<String>
)

@Serializable
data class HealthResponse(
    val status: String,
    val redis: Boolean,
    val timestamp: Long
)

fun Route.healthRoutes() {
    val cacheService = CacheService(RedisClientProvider.connection)

    get("/health") {
        val redisHealth = cacheService.healthCheck()
        val status = if (redisHealth) "healthy" else "degraded"

        call.respond(
            HealthResponse(
                status = status,
                redis = redisHealth,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    get("/health/redis") {
        val isHealthy = cacheService.healthCheck()
        val keys = cacheService.getKeys()

        call.respond(
            RedisHealthResponse(
                healthy = isHealthy,
                keys_count = keys.size,
                keys = keys.take(10) // показываем первые 10 ключей
            )
        )
    }

    get("/health/redis/clear") {
        cacheService.clearAll()
        call.respond(mapOf("message" to "Redis cache cleared"))
    }

    get("/health/redis/info") {
        val memoryInfo = cacheService.getMemoryInfo()
        call.respond(memoryInfo)
    }
}