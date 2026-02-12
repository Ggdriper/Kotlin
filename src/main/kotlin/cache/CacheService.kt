package com.example.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit

class CacheService(
    private val connection: StatefulRedisConnection<String, String>
) {
    private val redis = connection.sync()

    companion object {
        private const val PRODUCT_KEY_PREFIX = "product:"
        private const val PRODUCTS_LIST_KEY = "products:all"
        private const val ORDER_KEY_PREFIX = "order:"
        private const val USER_ORDERS_KEY_PREFIX = "user:orders:"
        private const val STATS_KEY = "stats:orders"

        // TTL в секундах
        private const val PRODUCT_TTL = 300L    // 5 минут
        private const val ORDER_TTL = 600L      // 10 минут
        private const val STATS_TTL = 120L      // 2 минуты
    }

    // ========== Product Cache ==========
    fun cacheProduct(id: String, json: String) {
        val key = "$PRODUCT_KEY_PREFIX$id"
        redis.setex(key, PRODUCT_TTL, json)
    }

    fun getProduct(id: String): String? {
        val key = "$PRODUCT_KEY_PREFIX$id"
        return redis.get(key)
    }

    fun cacheAllProducts(json: String) {
        redis.setex(PRODUCTS_LIST_KEY, PRODUCT_TTL, json)
    }

    fun getAllProducts(): String? {
        return redis.get(PRODUCTS_LIST_KEY)
    }

    fun invalidateProduct(id: String) {
        val key = "$PRODUCT_KEY_PREFIX$id"
        redis.del(key)
        redis.del(PRODUCTS_LIST_KEY)
    }

    fun invalidateAllProducts() {
        redis.del(PRODUCTS_LIST_KEY)
        val keys = redis.keys("$PRODUCT_KEY_PREFIX*")
        if (keys.isNotEmpty()) {
            redis.del(*keys.toTypedArray())
        }
    }

    // ========== Order Cache ==========
    fun cacheOrder(id: String, json: String) {
        val key = "$ORDER_KEY_PREFIX$id"
        redis.setex(key, ORDER_TTL, json)
    }

    fun getOrder(id: String): String? {
        val key = "$ORDER_KEY_PREFIX$id"
        return redis.get(key)
    }

    fun cacheUserOrders(userId: String, json: String) {
        val key = "$USER_ORDERS_KEY_PREFIX$userId"
        redis.setex(key, ORDER_TTL, json)
    }

    fun getUserOrders(userId: String): String? {
        val key = "$USER_ORDERS_KEY_PREFIX$userId"
        return redis.get(key)
    }

    fun invalidateOrder(id: String) {
        val key = "$ORDER_KEY_PREFIX$id"
        redis.del(key)
        invalidateStats()
    }

    fun invalidateUserOrders(userId: String) {
        val key = "$USER_ORDERS_KEY_PREFIX$userId"
        redis.del(key)
        invalidateStats()
    }

    // ========== Stats Cache ==========
    fun cacheStats(json: String) {
        redis.setex(STATS_KEY, STATS_TTL, json)
    }

    fun getStats(): String? {
        return redis.get(STATS_KEY)
    }

    fun invalidateStats() {
        redis.del(STATS_KEY)
    }

    // ========== General Methods ==========
    fun clearAll() {
        redis.flushdb()
    }

    fun getKeys(pattern: String = "*"): List<String> {
        return redis.keys(pattern)
    }

    fun healthCheck(): Boolean {
        return try {
            redis.ping() == "PONG"
        } catch (e: Exception) {
            false
        }
    }

    fun getMemoryInfo(): Map<String, Any> {
        return try {
            val info = redis.info("memory")
            val usedMemory = info.split("\n")
                .find { it.startsWith("used_memory:") }
                ?.split(":")?.get(1)?.trim() ?: "unknown"

            mapOf(
                "used_memory" to usedMemory,
                "total_keys" to redis.dbsize()
            )
        } catch (e: Exception) {
            mapOf("error" to (e.message ?: "Unknown error"))
        }
    }
}