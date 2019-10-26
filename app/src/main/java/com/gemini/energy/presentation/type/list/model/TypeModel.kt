package com.gemini.energy.presentation.type.list.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class TypeModel(
        val id: Long?,
        var name: String?,
        val type: String?,
        val subType: String?,

        val zoneId: Long?,
        val auditId: Long?
) : Parcelable

