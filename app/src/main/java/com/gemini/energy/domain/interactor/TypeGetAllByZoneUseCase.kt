package com.gemini.energy.domain.interactor

import com.gemini.energy.domain.Schedulers
import com.gemini.energy.domain.UseCase
import com.gemini.energy.domain.gateway.AuditGateway
import com.gemini.energy.domain.entity.Type
import io.reactivex.Observable

class TypeGetAllByZoneUseCase(schedulers: Schedulers, private val auditGateway: AuditGateway):
        UseCase<Any, List<Type>>(schedulers) {

    override fun buildObservable(params: Any?): Observable<List<Type>> {
        return auditGateway.getTypeListByZone(params as Long)
    }

}