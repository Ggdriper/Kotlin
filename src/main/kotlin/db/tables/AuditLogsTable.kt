package com.example.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.*

object AuditLogsTable : Table("audit_logs") {
    val id = uuid("id")
    val action = varchar("action", 255)
    val entityType = varchar("entity_type", 100).nullable()
    val entityId = uuid("entity_id").nullable()
    val userId = uuid("user_id").nullable()
    val details = text("details").nullable()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}
