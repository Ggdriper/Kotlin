package com.example.repository

import com.example.cache.CacheService
import com.example.cache.RedisClientProvider
import com.example.db.tables.ProductsTable
import com.example.product.ProductResponse
import com.example.product.UpdateProductRequest
import com.example.service.RabbitMQService
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

class ProductRepository(
    private val cacheService: CacheService = CacheService(RedisClientProvider.connection),
    private val rabbitMQService: RabbitMQService = RabbitMQService()  // Добавлено
) {

    fun findAll(): List<ProductResponse> = transaction {
        // Пытаемся получить из кэша
        val cachedJson = cacheService.getAllProducts()
        if (cachedJson != null) {
            return@transaction Json.decodeFromString(cachedJson)
        }

        // Если нет в кэше, получаем из БД и кэшируем
        val products = ProductsTable.selectAll().map { it.toProductEntity() }
        val productsJson = Json.encodeToString(products)
        cacheService.cacheAllProducts(productsJson)
        products
    }

    fun findById(id: UUID): ProductResponse? = transaction {
        // Пытаемся получить из кэша
        val cachedJson = cacheService.getProduct(id.toString())
        if (cachedJson != null) {
            return@transaction Json.decodeFromString<ProductResponse>(cachedJson)
        }

        // Если нет в кэше, получаем из БД и кэшируем
        val product = ProductsTable.select { ProductsTable.id eq id }
            .map { it.toProductEntity() }
            .firstOrNull()

        product?.let {
            val productJson = Json.encodeToString(it)
            cacheService.cacheProduct(id.toString(), productJson)
        }

        product
    }

    fun create(name: String, description: String?, price: BigDecimal, stock: Int) = transaction {
        val productId = UUID.randomUUID()

        ProductsTable.insert {
            it[id] = productId
            it[ProductsTable.name] = name
            it[ProductsTable.description] = description
            it[ProductsTable.price] = price
            it[ProductsTable.stock] = stock
            it[createdAt] = LocalDateTime.now()
        }

        // Отправляем событие в RabbitMQ
        rabbitMQService.sendAuditEvent(
            userId = null, // Системное событие
            action = "PRODUCT_CREATED",
            entityType = "PRODUCT",
            entityId = productId,
            details = "Created product: $name, price: $price, stock: $stock"
        )

        // Инвалидируем кэш списка продуктов
        cacheService.invalidateAllProducts()

        println("✅ Product created with ID: $productId, RabbitMQ event sent")
    }

    fun update(id: UUID, req: UpdateProductRequest) = transaction {
        // Получаем старые данные для аудита
        val oldProduct = ProductsTable.select { ProductsTable.id eq id }
            .map { it.toProductEntity() }
            .firstOrNull()

        // Сохраняем старые значения
        val oldName = oldProduct?.name
        val oldPrice = oldProduct?.price
        val oldStock = oldProduct?.stock

        ProductsTable.update({ ProductsTable.id eq id }) {
            req.name?.let { v -> it[name] = v }
            req.description?.let { v -> it[description] = v }
            req.price?.let { v -> it[price] = v }
            req.stock?.let { v -> it[stock] = v }
            it[updatedAt] = LocalDateTime.now()
        }

        // Получаем новые данные
        val newProduct = ProductsTable.select { ProductsTable.id eq id }
            .map { it.toProductEntity() }
            .firstOrNull()

        // Формируем детали для аудита
        val details = buildString {
            append("Product updated: ")
            if (req.name != null && req.name != oldName) {
                append("name: $oldName -> ${req.name}, ")
            }
            if (req.price != null && req.price != BigDecimal(oldPrice ?: 0.0)) {
                append("price: $oldPrice -> ${req.price}, ")
            }
            if (req.stock != null && req.stock != oldStock) {
                append("stock: $oldStock -> ${req.stock}, ")
            }
            if (req.description != null) {
                append("description updated")
            }
        }.trimEnd(',', ' ')

        // Отправляем событие обновления
        rabbitMQService.sendAuditEvent(
            userId = null,
            action = "PRODUCT_UPDATED",
            entityType = "PRODUCT",
            entityId = id,
            details = if (details.isNotEmpty()) details else "Product fields updated"
        )

        // Инвалидируем кэш этого продукта и списка
        cacheService.invalidateProduct(id.toString())
        cacheService.invalidateAllProducts()

        println("✅ Product $id updated, RabbitMQ event sent")
    }

    fun delete(id: UUID) = transaction {
        // Получаем данные перед удалением для аудита
        val product = ProductsTable.select { ProductsTable.id eq id }
            .map { it.toProductEntity() }
            .firstOrNull()

        ProductsTable.deleteWhere { ProductsTable.id eq id }

        // Отправляем событие удаления
        rabbitMQService.sendAuditEvent(
            userId = null,
            action = "PRODUCT_DELETED",
            entityType = "PRODUCT",
            entityId = id,
            details = "Deleted product: ${product?.name ?: "Unknown"}"
        )

        // Инвалидируем кэш
        cacheService.invalidateProduct(id.toString())
        cacheService.invalidateAllProducts()

        println("✅ Product $id deleted, RabbitMQ event sent")
    }

    private fun ResultRow.toProductEntity(): ProductResponse =
        ProductResponse(
            id = this[ProductsTable.id].toString(),
            name = this[ProductsTable.name],
            description = this[ProductsTable.description],
            price = this[ProductsTable.price].toDouble(),
            stock = this[ProductsTable.stock]
        )
}