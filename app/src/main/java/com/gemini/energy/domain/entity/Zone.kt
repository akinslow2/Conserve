package com.gemini.energy.domain.entity

data class Zone(
        val id: Long,
        var name: String,
        val typeId: List<String>,
)
