package com.gemini.energy.data.local.model

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import java.util.*

@Entity(tableName = "Audit")
data class AuditLocalModel(
        @PrimaryKey
        @ColumnInfo(name = "id")
        var auditId: Int,
        var name: String,

        @ColumnInfo(name = "created_at")
        var createdAt: Date,

        @ColumnInfo(name = "updated_at")
        var updatedAt: Date
)