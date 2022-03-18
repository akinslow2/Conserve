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
    override fun getFeatureByAudit(auditId: String): Observable<List<Feature>> {
        return api.fetchFeatures(where = JSONObject().put("auditId", auditId).toString()).map { it.results }.toObservable()
    }

    override fun getFeatureByType(typeId: Long): Observable<List<Feature>> {
        return api.fetchFeatures(where = JSONObject().put("typeId", typeId.toString()).toString()).map { it.results }.toObservable()
    }

    override fun getComputable(auditId: String): Observable<List<Computable<*>>> {
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
        val zone = audit.zone[type.zoneId.toString()]!!
        val type1 = EZoneType.get(type.type)
        val subtype1 = type.subtype ?: ""
        val subtype = when (type1) {
            EZoneType.Lighting -> ELightingType.get(subtype1)
            EZoneType.Plugload -> EApplianceType.get(subtype1)
            EZoneType.Refrigeration -> ERefrigerationType.get(subtype1)
            else -> null
        }
        return Computable(audit.auditId, audit.name, type.zoneId, zone.name, type.id, type.name, type1, subtype, null, null, false, null, null, mutableListOf(), null, 0.0, null, null)
    }
}
