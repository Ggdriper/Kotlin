package com.example.product



import com.example.cache.CacheService
import com.example.cache.RedisClientProvider
import com.example.repository.ProductRepository
import java.util.UUID


class ProductService(
    private val repository: ProductRepository,
    private val cacheService: CacheService = CacheService(RedisClientProvider.connection)
) {

    fun getAll(): List<ProductResponse> = repository.findAll()

    fun getById(id: UUID): ProductResponse? = repository.findById(id)

    fun create(req: CreateProductRequest) {
        repository.create(req.name, req.description, req.price, req.stock)
        // Кэш инвалидируется внутри repository
    }

    fun update(id: UUID, req: UpdateProductRequest) {
        repository.update(id, req)
        // Кэш инвалидируется внутри repository
    }

    fun delete(id: UUID) {
        repository.delete(id)
        // Кэш инвалидируется внутри repository
    }

    // Метод для принудительной очистки кэша продуктов (можно использовать для админки)
    fun clearProductCache() {
        cacheService.invalidateAllProducts()
    }

    fun getCacheStats(): Map<String, Any> {
        val productKeys = cacheService.getKeys("product:*").size
        val allProductsCached = cacheService.getAllProducts() != null

        return mapOf(
            "product_keys" to productKeys,
            "all_products_cached" to allProductsCached,
            "total_redis_keys" to cacheService.getKeys().size
        )
    }
}