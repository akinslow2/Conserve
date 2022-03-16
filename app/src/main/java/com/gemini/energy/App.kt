package com.gemini.energy

import com.gemini.energy.service.EnergyService
import com.gemini.energy.service.OutgoingRows
import com.gemini.energy.service.ParseAPI
import com.gemini.energy.service.ParseAuditGateway
import com.gemini.energy.service.type.UsageHours
import com.gemini.energy.service.type.UtilityRate
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get

fun main() {
    val app = Javalin.create().start(7000)

    app.routes {
        get("/audits/{auditId}/report") { ctx ->
            val auditId = ctx.pathParam("auditId")
            val rows = OutgoingRows()
            val service = EnergyService(ParseAuditGateway(ParseAPI.create()), UtilityRate(), UsageHours(), rows)
            service.run {} // TODO return the document(s?)
        }
    }

}
