package com.gemini.energy.domain.entity

import java.util.*

data class Feature (
        val id: Int?,
        val formId: Int?,
        val belongsTo: String?,
        val dataType: String?,
        var usn: Int,

        val auditId: Long?,
        val zoneId: Long?,
        val typeId: Long?,

        var key: String?,
        val valueString: String?,
        val valueInt: Int?,
        val valueDouble: Double?,

        val createdAt: Date,
        val updatedAt: Date
)
