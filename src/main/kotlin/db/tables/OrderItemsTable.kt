package com.example.db.tables


import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import java.util.UUID

object OrderItemsTable : Table("order_items") {
    val id = uuid("id")
    val orderId = uuid("order_id").references(OrdersTable.id)
    val productId = uuid("product_id").references(ProductsTable.id)
    val quantity = integer("quantity")
    val price = decimal("price", 10, 2)

    override val primaryKey = PrimaryKey(id)
}

