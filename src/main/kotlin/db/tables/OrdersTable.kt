package com.example.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.UUID

object OrdersTable : Table("orders") {
    val id = uuid("id")
    val userId = uuid("user_id").references(UsersTable.id)
    val status = varchar("status", 20).default("PENDING")
    val totalPrice = decimal("total_price", 10, 2)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}