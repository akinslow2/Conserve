package com.gemini.energy.domain.entity

import android.util.Log
import com.gemini.energy.presentation.util.BaseRowType
import com.gemini.energy.presentation.util.EZoneType
import com.gemini.energy.service.OutgoingRows
import com.google.gson.JsonElement
import timber.log.Timber

data class Computable<SubType>(

        /**
         * Audit Section
         * */
        var auditId: Long,
        var auditName: String,

        /**
         * Zone Section
         * */
        var zoneId: Long,
        var zoneName: String,

        /**
         * Categorize Device by Type - Sub Type
         * */
        var auditScopeId: Long,
        var auditScopeName: String,
        var auditScopeType: EZoneType?,
        var auditScopeSubType: SubType?,

        /**
         * These are the Form Data Being Collected from the User
         * */
        var featurePreAudit: List<Feature>?,
        var featureAuditScope: List<Feature>?,

        /**
         * The following parameters are used for Energy Efficient Equivalent
         * */
        var isEnergyStar: Boolean,
        var energyPreState: Map<String, String>?,
        var energyPostState: MutableList<Map<String, String>>?,
        var energyPostStateLeastCost: List<Map<String, String>>,
        var efficientAlternative: JsonElement?,
        var laborCost: Double,

        /**
         * Holds the Data Extracted from Parse during the Pre State*/
        var udf1: Any?,

        /**
         * These are the Outgoing Rows to be written to a file
         * */
        var outgoingRows: OutgoingRows?) {

    constructor(): this(
            NONE.toLong(), EMPTY, NONE.toLong(), EMPTY, NONE.toLong(), EMPTY, null,
            null, null, null, false,
            null, mutableListOf(), mutableListOf(), null,
            0.0, null, null)

    constructor(auditScopeSubType: SubType) : this(NONE.toLong(), EMPTY, NONE.toLong(), EMPTY, NONE.toLong(), EMPTY,
            null, auditScopeSubType, null, null,
            false, null, mutableListOf(), mutableListOf(),
            null, 0.0, null, null)

    fun mappedFeatureAuditScope(): HashMap<String, Any> = featureMapper(featureAuditScope)
    fun mappedFeaturePreAudit(): HashMap<String, Any> = featureMapper(featurePreAudit)

    private fun featureMapper(features: List<Feature>?): HashMap<String, Any> {
        val outgoing = hashMapOf<String, Any>()
        features?.let { featureList ->
            featureList
                    .forEach { feature ->
                        val key = feature.key!!
                        var value: Any = EMPTY

                        try {
                            value = typeMapper(feature.valueString, feature.dataType)
                        } catch (e: Exception) {
                            Timber.e("Type Mapper - Failed :: $key")
                            e.printStackTrace()
                        }

                        outgoing[key] = value
                    }
        }

        Log.d(this.javaClass.simpleName, outgoing.toString())
        return outgoing
    }

    private fun typeMapper(value: String?, type: String?) = cleanup(value, type)

    companion object {
        private const val EMPTY = ""
        private const val NONE = -1
        private fun cleanup(value: String?, type: String?) =
                when (type) {
                    BaseRowType.IntRow.value            -> cleanInt(value)
                    BaseRowType.DecimalRow.value        -> cleanDecimal(value)
                    else                                -> cleanString(value)
                }

        private fun cleanInt(value: String?): Int {
            val default = 0
            if (!value.isNullOrEmpty())
                return value.toString().toInt()
            return default
        }

        private fun cleanDecimal(value: String?): Double {
            val default = 0.0
            if (!value.isNullOrEmpty())
                return value.toString().toDouble()
            return default
        }

        private fun cleanString(value: String?): String {
            val default = EMPTY
            if (!value.isNullOrEmpty())
                return value.toString()
            return default
        }
    }

    override fun toString(): String {
        return  "audit: [$auditId - $auditName] | zone: [$zoneId - $zoneName]\n" +
                "scope: [$auditScopeId - $auditScopeName] | type: [${auditScopeType?.value} - ${auditScopeSubType?.toString()}]\n" +
                "featurePreAudit: COUNT [${featurePreAudit?.count()}]\n" +
                "featureData: COUNT [${featureAuditScope?.count()}]"
    }

}
