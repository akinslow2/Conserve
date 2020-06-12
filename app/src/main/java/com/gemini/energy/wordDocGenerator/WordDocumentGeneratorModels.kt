package com.gemini.energy.wordDocGenerator

import com.gemini.energy.service.device.EBase

//Map<AuditId: Map<EquipmentType: Equipment>>
typealias SortedAudits =
        MutableMap<Long, MutableMap<String, MutableList<EBase>>>

typealias AuditComponents = MutableMap<String, MutableList<EBase>>
//the commented numbers represent the mastersheet id values

data class HvacInstances(
        val quantity: Int, // 21
        val year: Int, // 22
        val age: Int, // 29
        val btu: Int, //23
        val seer: Double, // 24
        val overage: Int, //25
        val economizer: String,
        val thermotype: String
)

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
        val electricstructure: String, //10
        val utilitycompany: String, //9
        val gasstructure: String // 11
)

data class HvacValues(
        val instances: List<HvacInstances>,

        val costPostState: Double, // 20
        val totalCost: Double,
        val totalSavings: Double
)

data class WaterHeaterValues(
        val totalSavings: Double,
        val totalCost: Double,
        val paybackMonth: Double,
        val paybackYear: Double,
        val quantity: Int,
        val year: Int,
        val age: Int,
        val thermaleff: Int,
        val fueltype: String,
        val unittype: String,
        val capacity: Double
)

data class LightingValues(
        val totalcostsavings: Double, // 13
        val totalenergysavings: Double,
        val selfinstallcost: Int, // 14
        val totalCost: Double, // 15
        val paybackMonth: Double, // 16
        val geminiPayback: Double, // 17
        val paybackYear: Double, // 18
        val lightingRows: List<LightingDataRow>
)

data class LightingDataRow(
        val measure: String,
        val location: String,
        val measureDescription: String,
        val quantity: Double,
        val currentPowerkW: Double,
        val usageHours: Double,
        val postPowerkW: Double,
        val postUsageHours: Double,
        val energySavingskWh: Double,
        val costSavings: Double,
        val implementationCost: Double,
        val paybackPeriodMonths: Double,
        val paybackPeriodYears: Double
)

data class RefrigerationValues(
        val totalCost: Double,
        val totalSavings: Double,
        val paybackMonth: Double
)

data class EquipmentInstances(
        val name: String, // 27
        val delta: Double, // 26
        val costElectricity: Double, // 28
        val age: Double, // 29
        val materialCost: Double, // 32
        val implementationCost: Double,
        val totalsavings: Double,
        val paybackMonth: Double
)

data class EquipmentValues(
        val instances: List<EquipmentInstances>,

        val totalSavings: Double, // 31
        val totalCost: Double
)

data class BuildingValues(
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
        val waterHeaterPaybackMonth: Double
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
        val building: BuildingValues
)