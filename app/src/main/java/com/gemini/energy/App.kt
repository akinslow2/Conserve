package com.gemini.energy

import com.gemini.energy.service.EnergyService
import com.gemini.energy.service.OutgoingRows
import com.gemini.energy.service.ParseAPI
import com.gemini.energy.service.ParseAuditGateway
import com.gemini.energy.service.type.UsageHours
import com.gemini.energy.service.type.UtilityRate
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.http.HttpCode

fun main() {
    val app = Javalin.create().start(Integer.getInteger("server.port", 57269))

    app.routes {
        get("/api/v1/audits/{auditId}/report") { ctx ->
            val auditId = ctx.pathParam("auditId")
            val rows = OutgoingRows()
            val service = EnergyService(ParseAuditGateway(ParseAPI.create()), UtilityRate(), UsageHours(), rows)
            service.run(auditId) { doc ->
                if (doc != null) {
                    ctx.status(HttpCode.OK)
                    ctx.contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                    doc.write(ctx.res.outputStream)
                } else {
                    ctx.status(HttpCode.NO_CONTENT)
                    ctx.result("No report was generated")
                }
            }
        }
    }

}
