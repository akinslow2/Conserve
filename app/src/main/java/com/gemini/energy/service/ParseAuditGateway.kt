package com.gemini.energy.service

import com.gemini.energy.domain.entity.Computable
import com.gemini.energy.domain.entity.Feature
import com.gemini.energy.domain.gateway.AuditGateway
import io.reactivex.Observable
import org.json.JSONObject

class ParseAuditGateway(private val api: ParseAPI.ParseAPIService) : AuditGateway {
    override fun getFeatureByAudit(auditId: Long): Observable<List<Feature>> {
        return api.fetchFeatures(where = JSONObject().put("auditId", auditId).toString()).map { it.result }.toObservable()
    }

    override fun getFeatureByType(typeId: Long): Observable<List<Feature>> {
        return api.fetchFeatures(where = JSONObject().put("typeId", typeId).toString()).map { it.result }.toObservable()
    }

    override fun getComputable(): Observable<List<Computable<*>>> {
        TODO("Not yet implemented")
    }
}
