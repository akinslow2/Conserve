package com.gemini.energy.domain.entity
import java.util.*

data class Audit(
        val auditId: String,
        var name: String,
        var objectId: String,
        var createdAt: Date,
        var updatedAt: Date,
        val zone: Map<String, Zone>,
        val type: Map<String, Type>,
)
