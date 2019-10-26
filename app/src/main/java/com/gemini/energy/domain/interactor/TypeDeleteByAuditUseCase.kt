package com.gemini.energy.domain.interactor

import com.gemini.energy.domain.Schedulers
import com.gemini.energy.domain.UseCase
import com.gemini.energy.domain.gateway.AuditGateway
import io.reactivex.Observable

class TypeDeleteByAuditUseCase(schedulers: Schedulers, private val auditGateway: AuditGateway)
    : UseCase<Long, Unit>(schedulers) {

    override fun buildObservable(params: Long?): Observable<Unit> {
        return auditGateway.deleteTypeByAuditId(params!!)
    }
}