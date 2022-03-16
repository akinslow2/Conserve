package com.gemini.energy.domain.entity
import java.util.*

data class Audit(
        val id: Long,
        var name: String,
        var usn: Int,
        var objectId: String,
        var createdAt: Date,
        var updatedAt: Date,
        val zone: Map<Long, Zone>,
        val type: Map<Long, Type>,
)
