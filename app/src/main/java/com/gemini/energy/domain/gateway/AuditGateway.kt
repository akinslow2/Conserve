package com.gemini.energy.domain.gateway

import com.gemini.energy.domain.entity.*
import io.reactivex.Observable

interface AuditGateway {
    fun getFeatureByAudit(auditId: Long): Observable<List<Feature>>
    fun getFeatureByType(typeId: Long): Observable<List<Feature>>
    fun getComputable(): Observable<List<Computable<*>>>
}
