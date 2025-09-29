package com.example.database

import com.example.models.OrderStatus
import com.example.models.UserRole
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object Users : IntIdTable() {
    val username = varchar("username", 50).uniqueIndex()
    val password = varchar("password", 100)
    val role = enumerationByName("role", 10, UserRole::class)
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
}

class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(Users)

    var username by Users.username
    var password by Users.password
    var role by Users.role
    var createdAt by Users.createdAt
}

object Products : IntIdTable() {
    val name = varchar("name", 100)
    val description = text("description")
    val price = double("price")
    val stock = integer("stock")
    val createdAt = long("created_at").clientDefault { Instant.now().epochSecond }
}

class Product(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Product>(Products)

    var name by Products.name
    var description by Products.description
    var price by Products.price
    var stock by Products.stock
    var createdAt by Products.createdAt
}

object Orders : IntIdTable() {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val total = double("total")
    val status = enumerationByName("status", 20, OrderStatus::class).default(OrderStatus.PENDING)
    val createdAt = long("created_at").clientDefault { Instant.now().epochSecond }
}

class Order(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Order>(Orders)

    var userId by Orders.userId
    var total by Orders.total
    var status by Orders.status
    var createdAt by Orders.createdAt
}

object OrderItems : IntIdTable() {
    val orderId = reference("order_id", Orders, onDelete = ReferenceOption.CASCADE)
    val productId = reference("product_id", Products, onDelete = ReferenceOption.RESTRICT)
    val quantity = integer("quantity")
    val price = double("price")
}

class OrderItem(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<OrderItem>(OrderItems)

    var orderId by OrderItems.orderId
    var productId by OrderItems.productId
    var quantity by OrderItems.quantity
    var price by OrderItems.price
}

object RefreshTokens : IntIdTable() {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val token = varchar("token", 500).uniqueIndex()
    val expiresAt = long("expires_at")
    val createdAt = long("created_at").clientDefault { Instant.now().epochSecond }
}

class RefreshToken(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<RefreshToken>(RefreshTokens)

    var userId by RefreshTokens.userId
    var token by RefreshTokens.token
    var expiresAt by RefreshTokens.expiresAt
    var createdAt by RefreshTokens.createdAt
}