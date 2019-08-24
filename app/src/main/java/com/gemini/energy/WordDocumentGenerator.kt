package com.gemini.energy

import android.util.Log
import com.gemini.energy.service.device.EBase

typealias SortedAudits =
        MutableMap<Long, MutableMap<String, MutableList<EBase>>>

typealias AuditComponents = MutableMap<String, MutableList<EBase>>


//interface DocumentSubtype {}
//
//data class HvacValues
//(val businessname: String, //1
// val clientaddress: String, // 4
// val auditmonth: String, //2
// val audityear: String, // 3
// val startday: String, // 5
// val endday: String, //6
// val bldgarea: Double, // 7
// val bldgtype: String, // 12
// val operationhours: String, //8
// val electricstructure: String, //10
// val utilitycompany: String, //9
// val gasstructure: String, // 11
//
// val costPostState: Double, // 20
// val quantity: Int, // 21
// val year: Int, // 22
// val tons: Int, // 23
// val seer: Double, // 24
// val overage: Int // 25
//): DocumentSubtype
//
//data class LightingValues
//(val totalsavings: Double, // 13
// val selfinstallcost: Int, //14
// val installcost: Int, // 15
// val paybackMonth: Double, //16
// val geminiPayback: Double, // 17
// val paybackYear: Double // 18
//): DocumentSubtype
//
//data class EquipmentValues(
//        val equipmentName: String,
//        val age: Int,
//        val delta: Int,
//        val electricityCost: Int,
//        val materialCost: Double,
//        val hfcCost: Int
//)


data class HvacInstances(
        val quantity: Int, // 21
        val year: Int, // 22
        val age: Int, // TODO: implement me???
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
        val totalCost: Int
//        val totalCost: Int
)

val hvacTotalSavings = 200


data class LightingValues(
        val totalsavings: Double, // 13
        val selfinstallcost: Int, // 14
        val totalCost: Int, // 15
        val paybackMonth: Double, // 16
        val geminiPayback: Double, // 17
        val paybackYear: Double // 18
)

data class EquipmentInstances(
        val name: String, // 27
        val delta: Int, // 26
        val costElectricity: Int, // 28
        val age: Int, // 29
        val hfcCost: Int, // 30
        val materialCost: Int // 32
)


data class EquipmentValues(
        val instances: List<EquipmentInstances>,

        val totalSavings: Int, // 31
        val totalCost: Int
)

data class BuildingValues(
        val buildingTotalSavings: Double, // 19
        val buildingPayback: Double, // 33
        val buildingPaybackMonth: Double, // 41
        val buildingTotalCost: Int, // 34
        val hvacTotalCost: Int, // 35
        val hvacPaybackYear: Int, // 37
        val hvacPaybackMonth: Int, // 38
        val equipmentTotalCost: Int, // 36
        val equipmentPaybackYear: Int, // 39
        val equipmentPaybackMonth: Int // 40
)


class WordDocumentGenerator {

    fun generateDocument(values: MutableList<EBase>) {
        // organization & aggregation
        val sorter = SorterForWordDocumentGenerator()

        sorter.prepareAllValues(values)
//        val sortedAudits = sorter.sortEbasesIntoAudits(values)


        Log.i("-----moo", "SORTED")

//        // generation
//        generateFirstPage()
//        generateHvac()
//
//        // lighting
//        if (true) {
//            generateLighting()
//        }
//
//        generateClosingPage()
    }


    // generation
    // TODO: implement generation once ApachePoi is integrated

    private fun generateFirstPage(hvac: HvacValues) {}

    private fun generateEnergySavingPotential() {}

    private fun generateHvacSavings(hvac: HvacValues) {}

    private fun generateLightingSavings(lights: LightingValues) {}

    private fun generateEquipmentSavings() {}

    private fun generateFacilityInformation(hvac: HvacValues) {}


    // utils
    // TODO: implement utils once ApachePoi is integrated

    //    private fun createBullets(items: Array<String>, size: Int, color: String, isBold: Boolean, isItalic: Boolean)
    private fun createBullets() {}

    //    private fun mergeCellHorizontally(table: XWPFTable, row: Int, fromCol: Int, toCol: Int)
    private fun mergeCellHorizontally() {}

    private fun addPageNumbers() {}

    //    private fun centerTable(table: XWPFTable)
    private fun centerTable() {}

    //    private fun fitTable(table: XWPFTable, width: Long)
    private fun fitTable() {}
}

