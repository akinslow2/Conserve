package com.gemini.energy.wordDocGenerator

import com.gemini.energy.service.device.EBase
import com.gemini.energy.wordDocGenerator.models.*

//Map<AuditId: Map<EquipmentType: Equipment>>
typealias SortedAudits = MutableMap<String, MutableMap<String, MutableList<EBase>>>

typealias AuditComponents = MutableMap<String, MutableList<EBase>>
//the commented numbers represent the mastersheet id values

data class PreparedForDocument(
        val auditId: String,
        val preAudit: PreAuditValues,
        val zoneNames: MutableList<String>,
        val zoneString: String,
        val hvac: HvacValues?,
        val lighting: LightingValues?,
        val waterHeater: WaterHeaterValues?,
        val refrigeration: RefrigerationValues?,
        val equipment: EquipmentValues?,
        val motor: MotorValues?,
        val building: BuildingValues
)
