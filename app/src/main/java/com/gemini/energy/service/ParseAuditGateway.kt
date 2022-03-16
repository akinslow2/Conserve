package com.gemini.energy.service

import com.gemini.energy.domain.entity.Audit
import com.gemini.energy.domain.entity.Computable
import com.gemini.energy.domain.entity.Feature
import com.gemini.energy.domain.entity.Type
import com.gemini.energy.domain.gateway.AuditGateway
import com.gemini.energy.presentation.util.EApplianceType
import com.gemini.energy.presentation.util.ELightingType
import com.gemini.energy.presentation.util.ERefrigerationType
import com.gemini.energy.presentation.util.EZoneType
import io.reactivex.Observable
import org.json.JSONObject

class ParseAuditGateway(private val api: ParseAPI.ParseAPIService) : AuditGateway {
    override fun getFeatureByAudit(auditId: Long): Observable<List<Feature>> {
        return api.fetchFeatures(where = JSONObject().put("auditId", auditId).toString()).map { it.results }.toObservable()
    }

    override fun getFeatureByType(typeId: Long): Observable<List<Feature>> {
        return api.fetchFeatures(where = JSONObject().put("typeId", typeId).toString()).map { it.results }.toObservable()
    }

    override fun getComputable(auditId: Long): Observable<List<Computable<*>>> {
        return api.fetchAudits(where = JSONObject().put("auditId", auditId).toString()).map {
            it.results.flatMap {
                auditToComputables(it)
            }
        }.toObservable()
    }

    private fun auditToComputables(audit: Audit): List<Computable<*>> {
        return audit.type.values.map { type -> typeToComputable(audit, type) }
    }

    private fun typeToComputable(audit: Audit, type: Type): Computable<*> {
        val zone = audit.zone[type.zoneId]!!
        val type1 = EZoneType.get(type.type)
        val subtype = when (type1) {
            EZoneType.Lighting -> ELightingType.get(type.subType ?: "")
            EZoneType.Plugload -> EApplianceType.get(type.subType ?: "")
            EZoneType.Refrigeration -> ERefrigerationType.get(type.subType ?: "")
            else -> null
        }
        return Computable(audit.id, audit.name, type.zoneId, zone.name, type.id, type.name, type1, subtype, null, null, false, null, null, mutableListOf(), null, 0.0, null, null)
    }
}
