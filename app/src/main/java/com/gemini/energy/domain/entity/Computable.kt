package com.gemini.energy.domain.entity

import com.gemini.energy.presentation.util.BaseRowType
import com.gemini.energy.presentation.util.EZoneType
import com.gemini.energy.service.OutgoingRows
import com.google.gson.JsonElement

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
        var outgoingRows: OutgoingRows?,
) {

    fun mappedFeatureAuditScope(): HashMap<String, Any> = featureMapper(featureAuditScope)
    fun mappedFeaturePreAudit(): HashMap<String, Any> = featureMapper(featurePreAudit)

    private fun featureMapper(features: List<Feature>?): HashMap<String, Any> {
        val outgoing = hashMapOf<String, Any>()
        if (features.isNullOrEmpty()) {
            return outgoing
        }

        val separator = '\u001F'
        for (feature in features) {
            val dataTypes = feature.dataType.split(separator)
            val values = feature.values.split(separator)
            // TODO all the devices use the human-readable name, but we want to transition to IDs
            val keys = feature.fields.split(separator)
            val count = minOf(dataTypes.size, values.size, keys.size)
            for (index in 0 until count) {
                val strValue = values[index]
                outgoing[keys[index]] = when (dataTypes[index]) {
                    BaseRowType.IntRow.value -> strValue.toInt()
                    BaseRowType.DecimalRow.value -> strValue.toDouble()
                    else -> strValue
                }
            }
        }

        return outgoing
    }

    override fun toString(): String {
        return  "audit: [$auditId - $auditName] | zone: [$zoneId - $zoneName]\n" +
                "scope: [$auditScopeId - $auditScopeName] | type: [${auditScopeType?.value} - ${auditScopeSubType?.toString()}]\n" +
                "featurePreAudit: COUNT [${featurePreAudit?.count()}]\n" +
                "featureData: COUNT [${featureAuditScope?.count()}]"
    }

}
