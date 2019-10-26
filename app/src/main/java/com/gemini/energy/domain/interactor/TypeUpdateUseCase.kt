package com.gemini.energy.domain.interactor

import com.gemini.energy.domain.Schedulers
import com.gemini.energy.domain.UseCase
import com.gemini.energy.domain.entity.Type
import com.gemini.energy.domain.gateway.AuditGateway
import io.reactivex.Observable

class TypeUpdateUseCase(schedulers: Schedulers, private val auditGateway: AuditGateway):
        UseCase<Type?, Unit>(schedulers) {

    override fun buildObservable(params: Type?): Observable<Unit> {
        return auditGateway.updateType(params!!)
    }

}