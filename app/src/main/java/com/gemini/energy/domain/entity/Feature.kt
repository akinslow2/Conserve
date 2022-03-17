package com.gemini.energy.domain.entity

import java.util.*

data class Feature (
        var usn: Int,
        val createdAt: Date,
        val updatedAt: Date,

        val belongsTo: String, // preaudit or type
        val auditId: String,
        val zoneId: String?,
        val typeId: String?,

        val dataType: String,
        val fields: String, // display name
        val formId: String, // id
        val values: String,
)
