package com.gemini.energy.domain.gateway

import com.gemini.energy.domain.entity.*
import io.reactivex.Observable

interface AuditGateway {
    fun getFeatureByAudit(auditId: String): Observable<List<Feature>>
    fun getFeatureByType(typeId: Long): Observable<List<Feature>>
    fun getComputable(auditId: String): Observable<List<Computable<*>>>
}
