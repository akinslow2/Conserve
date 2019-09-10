package com.gemini.energy

import android.util.Log
import com.gemini.energy.presentation.util.EApplianceType
import com.gemini.energy.service.DataHolder
import com.gemini.energy.service.device.EBase
import com.gemini.energy.service.device.Hvac
import com.gemini.energy.service.device.lighting.*
import com.gemini.energy.service.device.plugload.*
import com.google.gson.JsonNull
import java.text.DecimalFormat

class SorterForWordDocumentGenerator {
    // use these values for sorting types of equipment that need to be aggregated for the report
    companion object {
        private val hvac = "hvac"
        private val lighting = "lighting"

        private val combinationOven = EApplianceType.CombinationOven.value
        private val convectionOven = EApplianceType.ConvectionOven.value
        private val conveyorBroiler = EApplianceType.ConveyorBroiler.value
        private val conveyorOven = EApplianceType.ConveyorOven.value
        private val dishWasher = EApplianceType.DishWasher.value
        private val fryer = EApplianceType.Fryer.value
        private val griddle = EApplianceType.Griddle.value
        private val hotFoodCabinet = EApplianceType.HotFoodCabinet.value
        private val iceMaker = EApplianceType.IceMaker.value
        private val preRinseSpray = EApplianceType.PreRinseSpray.value
        private val rackOven = EApplianceType.RackOven.value
        private val refrigerator = EApplianceType.Refrigerator.value
        private val steamCooker = EApplianceType.SteamCooker.value

        private val other = "other"
    }


    fun prepareAllValues(values: MutableList<EBase>): List<PreparedForDocument> {
        val sortedAudits = sortEbasesIntoAudits(values)


        val hvacPrepared = mutableListOf<HvacValues>()
        for (audit in sortedAudits) {
            val value = prepareValuesFromHvac(audit.value)
            if (value != null) {
                hvacPrepared.add(value)
            } else {
                Log.e("-----moo", "ERROR: need hvac to generatre report!!!!!")
            }
        }

        val lightingPrepared = mutableListOf<LightingValues>()
        for (audit in sortedAudits) {
            val value = prepareValuesFromLighting(audit.value)
            if (value != null) {
                lightingPrepared.add(value)
            } else {
                Log.i("-----moo", "no lighting to add for this audit")
            }
        }

        val eqipPrepared = mutableListOf<EquipmentValues>()
        for (audit in sortedAudits) {
            val value = prepareValuesForEquipment(audit.value)
            if (value != null) {
                eqipPrepared.add(value)
            } else {
                Log.i("-----moo", "no equipment to add for this audit")
            }
        }

        Log.i("-----moo", "DONE")
    }


    private fun sortEbasesIntoAudits(values: MutableList<EBase>): SortedAudits {
        val sorted = mutableMapOf<Long, MutableMap<String, MutableList<EBase>>>()

        for (value in values) {
            if (sorted.keys.contains(value.computable.auditId)) {

            } else {
                sorted[value.computable.auditId] = mutableMapOf(
                        hvac to mutableListOf(),
                        lighting to mutableListOf(),

                        combinationOven to mutableListOf(),
                        convectionOven to mutableListOf(),
                        conveyorOven to mutableListOf(),
                        fryer to mutableListOf(),
                        iceMaker to mutableListOf(),
                        rackOven to mutableListOf(),
                        refrigerator to mutableListOf(),
                        steamCooker to mutableListOf(),
                        griddle to mutableListOf(),
                        hotFoodCabinet to mutableListOf(),
                        conveyorBroiler to mutableListOf(),
                        dishWasher to mutableListOf(),
                        preRinseSpray to mutableListOf(),

                        other to mutableListOf())
            }

            when (value) {
                // Hvac
                is Hvac -> sorted[value.computable.auditId]!![hvac]!!.add(value)

                // Lighting
                is Cfl -> sorted[value.computable.auditId]!![lighting]!!.add(value)
                is Halogen -> sorted[value.computable.auditId]!![lighting]!!.add(value)
                is HighPressureSodium -> sorted[value.computable.auditId]!![lighting]!!.add(value)
                is Incandescent -> sorted[value.computable.auditId]!![lighting]!!.add(value)
                is LinearFluorescent -> sorted[value.computable.auditId]!![lighting]!!.add(value)
                is LowPressureSodium -> sorted[value.computable.auditId]!![lighting]!!.add(value)

                // Appliances
                // TODO: comment in appliances once they have finished being implemented
                is CombinationOven -> sorted[value.computable.auditId]!![combinationOven]!!.add(value)
//                is ConvectionOven -> sorted[value.computable.auditId]!![convectionOven]!!.add(value)
//                is ConveyorOven -> sorted[value.computable.auditId]!![convectionOven]!!.add(value)
//                is Fryer -> sorted[value.computable.auditId]!![fryer]!!.add(value)
                is IceMaker -> sorted[value.computable.auditId]!![iceMaker]!!.add(value)
//                is RackOven -> sorted[value.computable.auditId]!![rackOven]!!.add(value)
                is Refrigerator -> sorted[value.computable.auditId]!![refrigerator]!!.add(value)
//                is SteamCooker -> sorted[value.computable.auditId]!![steamCooker]!!.add(value)
                is Griddle -> sorted[value.computable.auditId]!![griddle]!!.add(value)
                is HotFoodCabinet -> sorted[value.computable.auditId]!![hotFoodCabinet]!!.add(value)
                is ConveyorBroiler -> sorted[value.computable.auditId]!![conveyorBroiler]!!.add(value)
//                is Dishwasher -> sorted[value.computable.auditId]!![dishWasher]!!.add(value)
                is PreRinseSpray -> sorted[value.computable.auditId]!![preRinseSpray]!!.add(value)


                // Other
                else -> sorted[value.computable.auditId]!![other]!!.add(value)
            }
        }

        return sorted
    }

    // TODO: implment me!!!
    private fun prepareValuesFromHvac(audit: AuditComponents): HvacValues? {
        if (!audit[hvac]!!.any()) {
            return null
        }

        // TODO: aggregate hvac values as needed
        @Suppress("UNCHECKED_CAST")
        val hvacs = audit[hvac]!! as MutableList<Hvac>

        var costPostState = 0.0 // aggregation value
        var totalCost = 0
        val instances = mutableListOf<HvacInstances>() // aggregation value

        for (hvac in hvacs) {
            // TODO: figure out how below should be called
//                hvac.costPostState(element, dataHolder)
            costPostState += hvac.costPostState(dataHolder = DataHolder(), element = JsonNull())

            // TODO: comment in once merged
//            totalCost += hvac.totalCost()

            instances.add(HvacInstances(
                    hvac.quantity,
                    hvac.year,
                    // TODO: implement me
//                    hvac.age,
                    20,
                    hvac.tons,
                    hvac.seer,
                    hvac.overage
            ))
        }

        val hvac = audit[hvac]!!.first() as Hvac


        return HvacValues(
                instances.toList(),

                hvac.businessname,
                hvac.clientaddress,
                hvac.auditmonth,
                hvac.audityear,
                hvac.startday,
                hvac.endday,
                hvac.operationhours,
                hvac.bldgarea,
                hvac.bldgtype,
                hvac.electricstructure,
                hvac.utilitycompany,
                hvac.gasstructure,

                costPostState,
                totalCost
        )
    }

    // probably done
    private fun prepareValuesFromLighting(audit: AuditComponents): LightingValues? {
        if (!audit[lighting]!!.any()) {
            return null
        }

        val cfls = mutableListOf<Cfl>()
        val halogens = mutableListOf<Halogen>()
        val highPressureSodiums = mutableListOf<HighPressureSodium>()
        val incandescents = mutableListOf<Incandescent>()
        val linearFluorescents = mutableListOf<LinearFluorescent>()
        val lowPressureSodium = mutableListOf<LowPressureSodium>()

        for (type in audit[lighting]!!) {
            when (type) {
                is Cfl -> cfls.add(type)
                is Halogen -> halogens.add(type)
                is HighPressureSodium -> highPressureSodiums.add(type)
                is Incandescent -> incandescents.add(type)
                is LinearFluorescent -> linearFluorescents.add(type)
                is LowPressureSodium -> lowPressureSodium.add(type)
            }
        }

        val totalSavings = 0.0
        val selfinstallcost = 0
        var electricianCost = 0

        for (light in cfls) {
//            NEED TOTAL SAVINGS FROM CFLS
//            NEED SELF INSTALL COST FROM CFLS
            electricianCost += light.electricianCost
        }

        for (light in halogens) {
//            NEED TOTAL SAVINGS FROM HALOGENS
//            NEED SELF INSTALL COST FROM HALOGENS
            electricianCost += light.electricianCost
        }

        for (light in highPressureSodiums) {
//            NEED TOTAL SAVINGS FROM HIGH PRESSURE SODIUM
//            NEED SELF INSTALL COST FROM HIGH PRESSURE SODIUM
            light.electricianCost
        }

        for (light in incandescents) {
//            NEED TOTAL SAVINGS FROM INCANDESCENTS
//            NEED SELF INSTALL COST FROM INCANDESCENTS
            electricianCost += light.electricianCost
        }

        for (light in linearFluorescents) {
//            NEED TOTAL SAVINGS FROM LINEAR FLUORESCENTS
//            NEED SELF INSTALL COST FROM LINEAR FLUORESCENTS
//            NEED ELECTRICIAN COST FROM LINEAR FLUORESCENTS
        }

        for (light in lowPressureSodium) {
//            NEED TOTAL SAVINGS FROM LOW PRESSURE SODIUM
//            NEED SELF INSTALL COST FROM LOW PRESSURE SODIUM
            electricianCost += light.electricianCost
        }

        val installCost = electricianCost + selfinstallcost
        val paybackMonth = selfinstallcost / totalSavings * 12
        val geminiPayback = selfinstallcost / (totalSavings * 0.7)
        val paybackYear: Double = selfinstallcost.toDouble() / totalSavings

        return LightingValues(
                totalSavings,
                selfinstallcost,
                installCost,
                paybackMonth,
                geminiPayback,
                paybackYear)
    }

    // TODO: implment me!!!
    private fun prepareValuesForEquipment(audit: AuditComponents): EquipmentValues? {
        val totalSavings = 5
        // TODO: will be in updates once merged
        //            if there is an equipment with no implementation cost
//                    skip it in aggregation
        val totalCost = 0
        val instances = mutableListOf<EquipmentInstances>()

        @Suppress("UNCHECKED_CAST")
        val combinationOven = audit[combinationOven]!! as List<CombinationOven>
        @Suppress("UNCHECKED_CAST")
        val convectionOven = audit[convectionOven]!! as List<ConvectionOven>
        @Suppress("UNCHECKED_CAST")
        val conveyorOven = audit[conveyorOven]!! as List<ConveyorOven>
        @Suppress("UNCHECKED_CAST")
        val fryer = audit[fryer]!! as List<Fryer>
        @Suppress("UNCHECKED_CAST")
        val iceMaker = audit[iceMaker]!! as List<IceMaker>
        @Suppress("UNCHECKED_CAST")
        val refrigerator = audit[refrigerator]!! as List<Refrigerator>
        @Suppress("UNCHECKED_CAST")
        val steamCooker = audit[steamCooker]!! as List<SteamCooker>
        @Suppress("UNCHECKED_CAST")
        val griddle = audit[griddle]!! as List<Griddle>
        @Suppress("UNCHECKED_CAST")
        val hotFoodCabinet = audit[hotFoodCabinet]!! as List<HotFoodCabinet>
        @Suppress("UNCHECKED_CAST")
        val conveyorBroiler = audit[conveyorBroiler]!! as List<ConveyorBroiler>
        @Suppress("UNCHECKED_CAST")
        val diswaher = audit[dishWasher]!! as List<DishWasher>
        @Suppress("UNCHECKED_CAST")
        val preRinseSpray = audit[preRinseSpray]!! as List<PreRinseSpray>

//        if ((audit[combinationOven]!!).any()) {
//
//        }

//        if (combinationOven.any()) {
//            val ovens =
//            val first = combinationOven.first()
//
////            val equip = EquipmentValues(
////                    "Combination Oven",
////                    12,
////                    12,
////                    12,
////                    first.materialCost(),
////                    first.materialCost() / (first.pre)
////            )
//
//        }


        return EquipmentValues(
                instances.toList(),
                totalSavings,
                totalCost
        )
    }

    // probably done
    private fun prepareBuildingValuesForEquipment(audit: AuditComponents, lightings: LightingValues, equpments: EquipmentValues, hvacs: HvacValues): BuildingValues? {

//        val hvacTotalCost: Int, // 35
//            implementationCost + implementationCost + …
        var hvacTotalCost = hvacs.totalCost


//        val hvacPaybackYear: Double, // 37
//            ("HtotalCost") / ("HVACtotalSavings")
        val hvacPaybackYear = hvacTotalCost / hvacTotalSavings


//        val hvacPaybackMonth: Int, // 38
//            ("HtotalCost") / ("HVACtotalSavings")* 12
        val hvacPaybackMonth = hvacTotalCost / hvacTotalSavings * 12


//        val equipmentTotalCost: Int, // 36
//            ∑implementationCost
//            if there is an equipment with no implementation cost
//                    skip it in aggregation
        val equipmentTotalCost = equpments.totalCost


//        val equipmentPaybackYear: Double, // 39
//             ("EtotalCost") / ("EquipmenttotalSavings")
        val equipmentPaybackYear = equipmentTotalCost / equpments.totalSavings


//        val equipmentPaybackMonth: Int // 40
//            ("EtotalCost") / ("EquipmenttotalSavings") * 12
        val equipmentPaybackMonth = equipmentTotalCost / equpments.totalSavings * 12


//        val buildingTotalSavings: Int, // 19
//             "LightingtotalSavings" + "EquipmenttotalSavings" + "HVACtotalSavings"
        val buildingTotalSavings = lightings.totalsavings + equpments.totalSavings + hvacTotalSavings


//        val buildingTotalCost: Int, // 34
//            LtotalCost + HtotalCost + EtotalCost
        val buildingTotalCost = lightings.totalCost + hvacTotalCost + equipmentTotalCost

//        val buildingPayback: Double, // 33
//            buildingTotalCost / buildingTotalSavings * 12
        val buildingPayback = buildingTotalCost / buildingTotalSavings * 12


//        val buildingPaybackMonth: Int, // 41
//            buildingTotalCost / buildingTotalSavings
        val buildingPaybackMonth = buildingTotalCost / buildingTotalSavings


        return BuildingValues(
                buildingTotalSavings,
                buildingPayback,
                buildingPaybackMonth,
                buildingTotalCost,
                hvacTotalCost,
                hvacPaybackYear,
                hvacPaybackMonth,
                equipmentTotalCost,
                equipmentPaybackYear,
                equipmentPaybackMonth
        )
    }
}