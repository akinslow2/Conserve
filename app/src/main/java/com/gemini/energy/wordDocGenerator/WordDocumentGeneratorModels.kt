package com.gemini.energy.wordDocGenerator

import com.gemini.energy.service.device.EBase
import com.gemini.energy.wordDocGenerator.models.*

//Map<AuditId: Map<EquipmentType: Equipment>>
typealias SortedAudits =
        MutableMap<Long, MutableMap<String, MutableList<EBase>>>

typealias AuditComponents = MutableMap<String, MutableList<EBase>>
//the commented numbers represent the mastersheet id values

data class PreAuditValues(
        val businessname: String, //1
        val auditmonth: String, //2
        val audityear: String, // 3
        val clientaddress: String, // 4
        val startday: String, // 5
        val endday: String, //6
        val operationhours: String, //8
        val bldgarea: Double, // 7
        val bldgtype: String, // 12
        val gasCompany: String,
        val gasStructure: String,
        val electricCompany: String,
        val electricStructure: String,
        val gasApplicableToBuilding: Boolean

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
