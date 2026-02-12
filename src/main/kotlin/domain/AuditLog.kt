package com.example.domain

import java.time.Instant
import java.util.UUID

data class AuditLog(
    val id: UUID,
    val userId: UUID?,
    val action: String,
    val entityType: String,
    val entityId: UUID?,
    val details: String?,
    val createdAt: Instant
)