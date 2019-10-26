package com.gemini.energy.presentation.audit.detail.zone.list.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ZoneModel(
        val id: Long,
        val name: String,
        val auditId: Long
) : Parcelable
