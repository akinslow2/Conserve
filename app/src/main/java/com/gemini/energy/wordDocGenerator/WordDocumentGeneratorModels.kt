package com.gemini.energy.wordDocGenerator

import com.gemini.energy.service.device.EBase
import com.gemini.energy.wordDocGenerator.models.*

//Map<AuditId: Map<EquipmentType: Equipment>>
typealias SortedAudits =
        MutableMap<Long, MutableMap<String, MutableList<EBase>>>

typealias AuditComponents = MutableMap<String, MutableList<EBase>>
//the commented numbers represent the mastersheet id values

data class BuildingValues(
        val buildingNetPresentValue: Double,
        val buildingTotalSavings: Double, // 19
        val buildingPayback: Double, // 33
        val buildingPaybackMonth: Double, // 41
        val buildingTotalCost: Double, // 34
        val hvacTotalCost: Double, // 35
        val hvacPaybackYear: Double, // 37
        val hvacPaybackMonth: Double, // 38
        val equipmentTotalCost: Double, // 36
        val equipmentPaybackYear: Double, // 39
        val equipmentPaybackMonth: Double, // 40
        val refrigerationTotalCost: Double,
        val refrigerationPaybackYear: Double,
        val refrigerationPaybackMonth: Double,
        val waterHeaterTotalCost: Double,
        val waterHeaterPaybackYear: Double,
        val waterHeaterPaybackMonth: Double,
        val currentTotalkW: Double,
        val currentTotalkWh: Double,
        val postTotalkW: Double,
        val postTotalkWh: Double
)

data class PreparedForDocument(
        val auditId: Long,
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
