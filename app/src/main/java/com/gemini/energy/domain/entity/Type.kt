package com.gemini.energy.domain.entity

data class Type(
        val id: Long,
        val zoneId: Long,
        var name: String,
        val type: String,
        var subtype: String?,
)
