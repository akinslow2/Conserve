package com.gemini.energy.data.gateway.mapper

import com.gemini.energy.data.local.model.*
import com.gemini.energy.domain.entity.*
import com.gemini.energy.presentation.util.EApplianceType
import com.gemini.energy.presentation.util.ERefrigerationType
import com.gemini.energy.presentation.util.ELightingType
import com.gemini.energy.presentation.util.EZoneType

class SystemMapper {

    fun toEntity(audit: AuditLocalModel) = Audit(
            audit.auditId,
            audit.name,
            audit.usn,
            audit.objectId,
            audit.createdAt,
            audit.updatedAt
    )

    fun toEntity(zone: ZoneLocalModel) = Zone(
            zone.zoneId,
            zone.name,
            zone.type,
            zone.usn,

            zone.auditId,

            zone.createdAt,
            zone.updatedAt
    )

    fun toEntity(type: TypeLocalModel) = Type(
            type.auditParentId,
            type.name,
            type.type,
            type.subType,
            type.usn,

            type.zoneId,
            type.auditId,

            type.createdAt,
            type.updatedAt
    )

    fun toEntity(feature: FeatureLocalModel): Feature {

        return Feature(
                feature.featureId,
                feature.formId,
                feature.belongsTo,
                feature.dataType,
                feature.usn,

                feature.auditId,
                feature.zoneId,
                feature.typeId,

                feature.key,
                feature.valueString,
                feature.valueInt,
                feature.valueDouble,

                feature.createdAt,
                feature.updatedAt
        )
    }

    fun toEntity(computable: ComputableLocalModel): Computable<*> {

        val eZoneType = EZoneType.get(computable.auditScopeType)
        val entity = when (eZoneType) {
            EZoneType.Plugload      -> Computable<EApplianceType>(EApplianceType.get(computable.auditScopeSubType)!!)
            EZoneType.Refrigeration -> Computable(ERefrigerationType.get(computable.auditScopeSubType)!!)
            EZoneType.Lighting      -> Computable<ELightingType>(ELightingType.get(computable.auditScopeSubType)!!)
            else                    -> Computable()
        }

        entity.auditId = computable.auditId
        entity.auditName = computable.auditName
        entity.zoneId = computable.zoneId
        entity.zoneName = computable.zoneName
        entity.auditScopeId = computable.auditScopeId
        entity.auditScopeName = computable.auditScopeName
        entity.auditScopeType = eZoneType

        return entity
    }

    companion object {
        private const val TAG = "SystemMapper"
    }

}