package com.example.order

import com.example.cache.CacheService
import com.example.cache.RedisClientProvider
import com.example.db.tables.OrderItemsTable
import com.example.db.tables.OrdersTable
import com.example.db.tables.ProductsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.Instant
import com.example.db.tables.*
import com.example.repository.AuditRepository
import com.example.service.RabbitMQService
import kotlinx.serialization.json.Json
import java.math.BigDecimal


class OrderRepository(
    private val auditRepository: AuditRepository = AuditRepository(),
    private val cacheService: CacheService = CacheService(RedisClientProvider.connection),
    private val rabbitMQService: RabbitMQService = RabbitMQService()  // Добавлено
) {

    fun createOrder(userId: UUID, items: List<OrderItemRequest>): OrderResponse = transaction {
        // Проверяем наличие всех товаров и достаточность stock
        items.forEach { item ->
            val product = ProductsTable.select { ProductsTable.id eq item.productId }
                .singleOrNull()
                ?: throw IllegalArgumentException("Product ${item.productId} not found")

            val currentStock = product[ProductsTable.stock]
            if (currentStock < item.quantity) {
                throw IllegalArgumentException(
                    "Insufficient stock for product ${item.productId}. " +
                            "Available: $currentStock, Requested: ${item.quantity}"
                )
            }
        }

        // Вычисляем общую стоимость
        var totalPrice = BigDecimal.ZERO
        val itemsWithPrices = mutableListOf<OrderItemWithPrice>()
        var totalItemsCount = 0

        items.forEach { item ->
            val product = ProductsTable.select { ProductsTable.id eq item.productId }.single()
            val price = product[ProductsTable.price]
            val itemTotal = price.multiply(BigDecimal(item.quantity))
            totalPrice = totalPrice.add(itemTotal)
            totalItemsCount += item.quantity

            itemsWithPrices.add(OrderItemWithPrice(
                productId = item.productId,
                productName = product[ProductsTable.name],
                quantity = item.quantity,
                price = price
            ))

            // Уменьшаем stock
            ProductsTable.update({ ProductsTable.id eq item.productId }) {
                with(SqlExpressionBuilder) {
                    it[stock] = stock - item.quantity
                }
            }

            // Инвалидируем кэш товара
            cacheService.invalidateProduct(item.productId.toString())
        }

        // Создаем заказ
        val orderId = UUID.randomUUID()
        val now = Instant.now()

        OrdersTable.insert {
            it[id] = orderId
            it[this.userId] = userId
            it[OrdersTable.status] = "PENDING"
            it[OrdersTable.totalPrice] = totalPrice
            it[OrdersTable.createdAt] = now
        }

        // Добавляем позиции заказа
        itemsWithPrices.forEach { item ->
            OrderItemsTable.insert {
                it[id] = UUID.randomUUID()
                it[OrderItemsTable.orderId] = orderId
                it[OrderItemsTable.productId] = item.productId
                it[OrderItemsTable.quantity] = item.quantity
                it[OrderItemsTable.price] = item.price
            }
        }

        // 1. Пишем в аудит-логи (синхронно, для надежности)
        auditRepository.logEvent(
            userId = userId,
            action = "ORDER_CREATED",
            entityType = "ORDER",
            entityId = orderId,
            details = "Created order with ${items.size} items, total: $totalPrice"
        )

        // 2. Отправляем событие в RabbitMQ (асинхронно)
        rabbitMQService.sendOrderCreated(
            orderId = orderId,
            userId = userId,
            totalPrice = totalPrice.toString(),
            itemsCount = totalItemsCount
        )

        // 3. Отправляем аудит-событие в RabbitMQ
        rabbitMQService.sendAuditEvent(
            userId = userId,
            action = "ORDER_CREATED",
            entityType = "ORDER",
            entityId = orderId,
            details = "Order created via RabbitMQ, total items: ${items.size}"
        )

        val orderResponse = OrderResponse(
            id = orderId,
            userId = userId,
            status = "PENDING",
            totalPrice = totalPrice,
            createdAt = now.toString(),
            items = itemsWithPrices.map {
                OrderItemResponse(
                    productId = it.productId,
                    productName = it.productName,
                    quantity = it.quantity,
                    price = it.price
                )
            }
        )

        // Кэшируем заказ
        val orderJson = Json.encodeToString(orderResponse)
        cacheService.cacheOrder(orderId.toString(), orderJson)

        // Инвалидируем кэш заказов пользователя и статистики
        cacheService.invalidateUserOrders(userId.toString())
        cacheService.invalidateStats()
        cacheService.invalidateAllProducts()

        orderResponse
    }

    private data class OrderItemWithPrice(
        val productId: UUID,
        val productName: String,
        val quantity: Int,
        val price: BigDecimal
    )

    fun getOrdersByUser(userId: UUID): List<OrderResponse> = transaction {
        // Пытаемся получить из кэша
        val cachedJson = cacheService.getUserOrders(userId.toString())
        if (cachedJson != null) {
            return@transaction Json.decodeFromString(cachedJson)
        }

        // Если нет в кэше, получаем из БД
        val orders = OrdersTable
            .select { OrdersTable.userId eq userId }
            .orderBy(OrdersTable.createdAt to SortOrder.DESC)
            .map { orderRow ->
                val orderId = orderRow[OrdersTable.id]

                val items = OrderItemsTable
                    .innerJoin(ProductsTable, { OrderItemsTable.productId }, { ProductsTable.id })
                    .select { OrderItemsTable.orderId eq orderId }
                    .map { itemRow ->
                        OrderItemResponse(
                            productId = itemRow[OrderItemsTable.productId],
                            productName = itemRow[ProductsTable.name],
                            quantity = itemRow[OrderItemsTable.quantity],
                            price = itemRow[OrderItemsTable.price]
                        )
                    }

                OrderResponse(
                    id = orderId,
                    userId = orderRow[OrdersTable.userId],
                    status = orderRow[OrdersTable.status],
                    totalPrice = orderRow[OrdersTable.totalPrice],
                    createdAt = orderRow[OrdersTable.createdAt].toString(),
                    items = items
                )
            }

        // Кэшируем результат
        val ordersJson = Json.encodeToString(orders)
        cacheService.cacheUserOrders(userId.toString(), ordersJson)

        orders
    }

    fun getOrderById(orderId: UUID): OrderResponse? = transaction {
        // Пытаемся получить из кэша
        val cachedJson = cacheService.getOrder(orderId.toString())
        if (cachedJson != null) {
            return@transaction Json.decodeFromString<OrderResponse>(cachedJson)
        }

        // Если нет в кэше, получаем из БД
        val order = OrdersTable
            .select { OrdersTable.id eq orderId }
            .map { orderRow ->
                val items = OrderItemsTable
                    .innerJoin(ProductsTable, { OrderItemsTable.productId }, { ProductsTable.id })
                    .select { OrderItemsTable.orderId eq orderId }
                    .map { itemRow ->
                        OrderItemResponse(
                            productId = itemRow[OrderItemsTable.productId],
                            productName = itemRow[ProductsTable.name],
                            quantity = itemRow[OrderItemsTable.quantity],
                            price = itemRow[OrderItemsTable.price]
                        )
                    }

                OrderResponse(
                    id = orderRow[OrdersTable.id],
                    userId = orderRow[OrdersTable.userId],
                    status = orderRow[OrdersTable.status],
                    totalPrice = orderRow[OrdersTable.totalPrice],
                    createdAt = orderRow[OrdersTable.createdAt].toString(),
                    items = items
                )
            }
            .singleOrNull()

        // Кэшируем найденный заказ
        order?.let {
            val orderJson = Json.encodeToString(it)
            cacheService.cacheOrder(orderId.toString(), orderJson)
        }

        order
    }

    fun deleteOrder(userId: UUID, orderId: UUID): Boolean = transaction {
        val order = OrdersTable.select {
            OrdersTable.id eq orderId and (OrdersTable.userId eq userId)
        }.singleOrNull()
            ?: return@transaction false

        // Получаем информацию о заказе для RabbitMQ
        val orderTotal = order[OrdersTable.totalPrice]
        val items = OrderItemsTable.select { OrderItemsTable.orderId eq orderId }
        var itemsCount = 0

        // Возвращаем stock товаров
        items.forEach { itemRow ->
            val productId = itemRow[OrderItemsTable.productId]
            val quantity = itemRow[OrderItemsTable.quantity]
            itemsCount += quantity

            ProductsTable.update({ ProductsTable.id eq productId }) {
                with(SqlExpressionBuilder) {
                    it[stock] = stock + quantity
                }
            }

            // Инвалидируем кэш товара
            cacheService.invalidateProduct(productId.toString())
        }

        // Удаляем позиции заказа
        OrderItemsTable.deleteWhere { OrderItemsTable.orderId eq orderId }

        // Удаляем заказ
        OrdersTable.deleteWhere { OrdersTable.id eq orderId }

        // 1. Синхронный аудит
        auditRepository.logEvent(
            userId = userId,
            action = "ORDER_CANCELLED",
            entityType = "ORDER",
            entityId = orderId,
            details = "Order cancelled, stock returned for $itemsCount items"
        )

        // 2. Отправляем событие отмены в RabbitMQ
        rabbitMQService.sendOrderCancelled(
            orderId = orderId,
            userId = userId,
            reason = "User requested cancellation"
        )

        // 3. Отправляем аудит-событие
        rabbitMQService.sendAuditEvent(
            userId = userId,
            action = "ORDER_CANCELLED",
            entityType = "ORDER",
            entityId = orderId,
            details = "Order cancelled via RabbitMQ, total: $orderTotal"
        )

        // Инвалидируем кэш
        cacheService.invalidateOrder(orderId.toString())
        cacheService.invalidateUserOrders(userId.toString())
        cacheService.invalidateStats()
        cacheService.invalidateAllProducts()

        true
    }

    fun getOrdersStats(): OrdersStatsResponse = transaction {
        // Пытаемся получить из кэша
        val cachedJson = cacheService.getStats()
        if (cachedJson != null) {
            return@transaction Json.decodeFromString(cachedJson)
        }

        // Если нет в кэше, вычисляем
        val orders = OrdersTable.selectAll().toList()
        val totalOrders = orders.size
        val totalAmount = orders.sumOf { it[OrdersTable.totalPrice].toDouble() }
        val averageOrderValue = if (totalOrders > 0) totalAmount / totalOrders else 0.0

        val stats = OrdersStatsResponse(
            totalOrders = totalOrders,
            totalAmount = totalAmount,
            averageOrderValue = averageOrderValue
        )

        // Кэшируем статистику
        val statsJson = Json.encodeToString(stats)
        cacheService.cacheStats(statsJson)

        stats
    }

    fun getOrderCountByUser(userId: UUID): Int = transaction {
        OrdersTable
            .select { OrdersTable.userId eq userId }
            .count()
            .toInt()
    }
}