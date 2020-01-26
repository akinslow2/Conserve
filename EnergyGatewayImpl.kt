package com.gemini.energy.service

import com.gemini.energy.domain.entity.Audit
import com.gemini.energy.domain.gateway.AuditGateway
import com.gemini.energy.domain.gateway.EnergyGateway
import io.reactivex.Observable
import io.reactivex.disposables.Disposable

class EnergyGatewayImpl(
        private val service: EnergyService,
        private val uploader: IUploader) : EnergyGateway {

    override fun compute(audit: Audit, auditGateway: AuditGateway) {
        return service.run(callback = {
            // *** Do Nothing *** //
        })
    }

    override fun upload(outgoingRows: List<OutgoingRows>): Observable<Unit> {
        return uploader.upload(outgoingRows)
    }
}