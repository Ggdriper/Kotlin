package com.example.repository

import com.example.db.tables.AuditLogsTable
import com.example.domain.AuditLog
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

class AuditRepository {

    fun logEvent(
        userId: UUID?,
        action: String,
        entityType: String,
        entityId: UUID? = null,
        details: String? = null
    ) = transaction {
        AuditLogsTable.insert {
            it[id] = UUID.randomUUID()
            it[this.userId] = userId
            it[AuditLogsTable.action] = action
            it[this.entityType] = entityType
            it[this.entityId] = entityId
            it[this.details] = details
            it[createdAt] = Instant.now()
        }
    }

    fun getLogsByUser(userId: UUID): List<AuditLog> = transaction {
        AuditLogsTable
            .select { AuditLogsTable.userId eq userId }
            .orderBy(AuditLogsTable.createdAt to SortOrder.DESC)
            .map { row ->
                AuditLog(
                    id = row[AuditLogsTable.id],
                    userId = row[AuditLogsTable.userId],
                    action = row[AuditLogsTable.action],
                    entityType = row[AuditLogsTable.entityType] ?: "",
                    entityId = row[AuditLogsTable.entityId],
                    details = row[AuditLogsTable.details],
                    createdAt = row[AuditLogsTable.createdAt]
                )
            }
    }
}