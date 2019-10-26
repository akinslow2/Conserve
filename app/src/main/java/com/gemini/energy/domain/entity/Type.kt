package com.gemini.energy.domain.entity

import java.util.*

data class Type(
    val id: Long,
    var name: String?,
    val type: String?,
    var subType: String?,
    var usn: Int,

    val zoneId: Long?,
    val auditId: Long?,

    val createdAt: Date?,
    var updatedAt: Date?
)
