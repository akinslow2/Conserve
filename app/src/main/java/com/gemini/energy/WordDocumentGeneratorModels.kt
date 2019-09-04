package com.gemini.energy

import com.gemini.energy.service.device.EBase

//Map<AuditId: Map<EqupmentType: Equipment>>
typealias SortedAudits =
        MutableMap<Long, MutableMap<String, MutableList<EBase>>>

typealias AuditComponents = MutableMap<String, MutableList<EBase>>

data class HvacInstances(
        val quantity: Int, // 21
        val year: Int, // 22
        val age: Int, // 29
        val tons: Int, //23
        val seer: Double, // 24
        val overage: Int //25
)

data class HvacValues(
        val instances: List<HvacInstances>,

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
        val gasstructure: String, // 11

        val costPostState: Double, // 20
        val totalCost: Double,
        val totalsavings: Double
)

val hvacTotalSavings = 200


data class LightingValues(
        val totalsavings: Double, // 13
        val selfinstallcost: Int, // 14
        val totalCost: Double, // 15
        val paybackMonth: Double, // 16
        val geminiPayback: Double, // 17
        val paybackYear: Double // 18
)

data class EquipmentInstances(
        val name: String, // 27
        val delta: Double, // 26
        val costElectricity: Double, // 28
        val age: Double, // 29
        val hfcCost: Int, // 30
        val materialCost: Double, // 32
        val implementationCost: Double,
        val totalsavings: Double
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
        val equipmentPaybackMonth: Double // 40
)

data class PreparedForDocument(
        val auditId: Long,
        val hvac: HvacValues,
        val lighting: LightingValues?,
        val equipment: EquipmentValues?,
        val building: BuildingValues
)