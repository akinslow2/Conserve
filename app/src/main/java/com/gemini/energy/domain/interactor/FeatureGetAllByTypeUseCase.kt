package com.gemini.energy.domain.interactor

import com.gemini.energy.domain.Schedulers
import com.gemini.energy.domain.UseCase
import com.gemini.energy.domain.entity.Feature
import com.gemini.energy.domain.gateway.AuditGateway
import io.reactivex.Observable

class FeatureGetAllByTypeUseCase(schedulers: Schedulers, private val auditGateway: AuditGateway) :
        UseCase<Long, List<Feature>>(schedulers) {

    override fun buildObservable(params: Long?): Observable<List<Feature>> {
        return auditGateway.getFeatureByType(params!!)
    }

}