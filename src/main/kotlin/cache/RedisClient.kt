package com.example.cache

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection

object RedisClientProvider {

    private val redisUrl =
        System.getenv("REDIS_URL") ?: "redis://localhost:6379"

    val client: RedisClient = RedisClient.create(redisUrl)

    val connection: StatefulRedisConnection<String, String> by lazy {
        client.connect()
    }

    fun close() {
        connection.close()
        client.shutdown()
    }
}