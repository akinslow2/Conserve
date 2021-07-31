package com.gemini.energy.wordDocGenerator

import com.gemini.energy.presentation.util.EApplianceType
import com.gemini.energy.service.DataHolder
import com.gemini.energy.service.device.EBase
import com.gemini.energy.service.device.Hvac
import com.gemini.energy.service.device.WaterHeater
import com.gemini.energy.service.device.lighting.*
import com.gemini.energy.service.device.plugload.*
import com.gemini.energy.service.device.refrigeration.*
import com.gemini.energy.wordDocGenerator.models.*
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject

class SorterForWordDocumentGenerator {
    // use these values for sorting types of equipment that need to be aggregated for the report
    companion object {
        const val hvac = "hvac"
        const val lighting = "lighting"
        const val waterheater = "waterheater"
        const val refrigeration = "refrigeration"

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
        private val steamCooker = EApplianceType.SteamCooker.value

        private const val other = "other"
    }

    fun prepareAllValues(values: MutableList<EBase>): List<PreparedForDocument> {
        val sortedAudits = sortEbasesIntoAudits(values)

        val returnAble = mutableListOf<PreparedForDocument>()

        for (audit in sortedAudits) {
            val hvac = HvacValues.tryCreate(audit.value)
            val waterheater = WaterHeaterValues.tryCreate(audit.value)
            val lighting = LightingValues.tryCreate(audit.value)
            val equipment = prepareValuesForEquipment(audit.value)
            val refrigeration = RefrigerationValues.tryCreate(audit.value)
            val building = prepareBuildingValuesForEquipment(lighting, equipment, hvac, waterheater, refrigeration)
            val preaudit = prepareValuesForPreAudit(audit.value)

            val zones = aggregateZoneNames(audit.value)
            val zoneString = concatenateZoneString(zones)

            if (preaudit == null || preaudit.businessname.isBlank()) {
                continue
            } else {
                returnAble.add(PreparedForDocument(
                        audit.key,
                        preaudit,
                        zones,
                        zoneString,
                        hvac,
                        lighting,
                        waterheater,
                        refrigeration,
                        equipment,
                        building))
            }
        }

        return returnAble.toList()
    }

    private fun prepareValuesForPreAudit(audit: AuditComponents): PreAuditValues? {
        val allEquipment = audit.flatMap { it.value }
        val distinctPreAuditValues = allEquipment.distinctBy { it.preAudit }

        val equipment = distinctPreAuditValues.firstOrNull()
        if (equipment != null
                && equipment.preAudit.containsKey("General Client Info Business Name")
                && equipment.preAudit.containsKey("General Client Info Audit Month")
                && equipment.preAudit.containsKey("General Client Info Audit Year")
                && equipment.preAudit.containsKey("General Client Info Address Line 1")
                && equipment.preAudit.containsKey("General Client Info Address Line 2")
                && equipment.preAudit.containsKey("General Client Info City")
                && equipment.preAudit.containsKey("General Client Info State")
                && equipment.preAudit.containsKey("General Client Info Zip Code")
                && equipment.preAudit.containsKey("General Client Info Assessment Start Day")
                && equipment.preAudit.containsKey("General Client Info Assessment End Day")
                && equipment.preAudit.containsKey("Operation Hours Monday Operating Hours")
                && equipment.preAudit.containsKey("Area Total (Sq.Ft.)")
                && equipment.preAudit.containsKey("General Client Info Facility Type")
                && equipment.preAudit.containsKey("Others Electric Rate Structure")
                && equipment.preAudit.containsKey("Others Utility Company")
                && equipment.preAudit.containsKey("Others Gas Rate Structure")) {
            val preAudit = equipment.preAudit
            val address = "${preAudit["General Client Info Address Line 1"]} ${preAudit["General Client Info Address Line 2"]} ${preAudit["General Client Info City"]}, ${preAudit["General Client Info State"]} ${preAudit["General Client Info Zip Code"]}"


            return PreAuditValues(
                    preAudit["General Client Info Business Name"]!! as String,
                    preAudit["General Client Info Audit Month"]!! as String,
                    preAudit["General Client Info Audit Year"]!! as String,
                    address,
                    preAudit["General Client Info Assessment Start Day"]!! as String,
                    preAudit["General Client Info Assessment End Day"]!! as String,
                    preAudit["Operation Hours Monday Operating Hours"]!! as String,
                    preAudit["Area Total (Sq.Ft.)"]!! as Double,
                    preAudit["General Client Info Facility Type"]!! as String,
                    preAudit["Others Electric Rate Structure"]!! as String,
                    preAudit["Others Utility Company"]!! as String,
                    preAudit["Others Gas Rate Structure"]!! as String
            )

        }

        return null
    }

    private fun aggregateZoneNames(audit: AuditComponents): MutableList<String> {
        val zones = mutableListOf<String>()

        for (component in audit) {
            for (equipment in component.value) {
                if (zones.any { i -> i == equipment.computable.zoneName }) {
                } else {
                    zones.add(equipment.computable.zoneName)
                }
            }
        }

        return zones
    }

    private fun concatenateZoneString(zones: MutableList<String>): String {
        if (zones.count() == 0) {
            return ""
        }
        if (zones.count() == 1) {
            return zones[0]
        }

        var zoneString = ""

        for ((index, zone) in zones.iterator().withIndex()) {
            if (index == zones.count() - 1) {
                zoneString += "and an $zone"
            } else {
                zoneString += "$zone, "
            }
        }

        return zoneString
    }

    private fun sortEbasesIntoAudits(values: MutableList<EBase>): SortedAudits {
        val sorted = mutableMapOf<Long, MutableMap<String, MutableList<EBase>>>()

        for (value in values) {
            if (sorted.keys.contains(value.computable.auditId)) {
            } else {
                sorted[value.computable.auditId] = mutableMapOf(
                        hvac to mutableListOf(),
                        waterheater to mutableListOf(),
                        lighting to mutableListOf(),
                        refrigeration to mutableListOf(),

                        combinationOven to mutableListOf(),
                        convectionOven to mutableListOf(),
                        conveyorBroiler to mutableListOf(),
                        conveyorOven to mutableListOf(),
                        dishWasher to mutableListOf(),
                        fryer to mutableListOf(),
                        griddle to mutableListOf(),
                        hotFoodCabinet to mutableListOf(),
                        iceMaker to mutableListOf(),
                        preRinseSpray to mutableListOf(),
                        rackOven to mutableListOf(),
                        steamCooker to mutableListOf(),

                        other to mutableListOf())
            }

            when (value) {
                // Hvac
                is Hvac -> sorted[value.computable.auditId]!![hvac]!!.add(value)

                // Water Heater
                is WaterHeater -> sorted[value.computable.auditId]!![waterheater]!!.add(value)

                // Lighting
                is Cfl -> sorted[value.computable.auditId]!![lighting]!!.add(value)
                is Halogen -> sorted[value.computable.auditId]!![lighting]!!.add(value)
                is HPSodium -> sorted[value.computable.auditId]!![lighting]!!.add(value)
                is Incandescent -> sorted[value.computable.auditId]!![lighting]!!.add(value)
                is LinearFluorescent -> sorted[value.computable.auditId]!![lighting]!!.add(value)
                is LPSodium -> sorted[value.computable.auditId]!![lighting]!!.add(value)

                // Refrigeration
                is Refrigerator -> sorted[value.computable.auditId]!![refrigeration]!!.add(value)
                is Freezer -> sorted[value.computable.auditId]!![refrigeration]!!.add(value)
                is WIFreezer -> sorted[value.computable.auditId]!![refrigeration]!!.add(value)
                is WIRefrigerator -> sorted[value.computable.auditId]!![refrigeration]!!.add(value)
                is WICoolerBox -> sorted[value.computable.auditId]!![refrigeration]!!.add(value)

                // Appliances
                // TODO: comment in appliances once they have finished being implemented
                is CombinationOven -> sorted[value.computable.auditId]!![combinationOven]!!.add(value)
//                is ConvectionOven -> sorted[value.computable.auditId]!![convectionOven]!!.add(value)
                is ConveyorBroiler -> sorted[value.computable.auditId]!![conveyorBroiler]!!.add(value)
//                is ConveyorOven -> sorted[value.computable.auditId]!![convectionOven]!!.add(value)
                is DishWasher -> sorted[value.computable.auditId]!![dishWasher]!!.add(value)
//                is Fryer -> sorted[value.computable.auditId]!![fryer]!!.add(value)
                is Griddle -> sorted[value.computable.auditId]!![griddle]!!.add(value)
                is HotFoodCabinet -> sorted[value.computable.auditId]!![hotFoodCabinet]!!.add(value)
                is IceMaker -> sorted[value.computable.auditId]!![iceMaker]!!.add(value)
                is PreRinseSpray -> sorted[value.computable.auditId]!![preRinseSpray]!!.add(value)
//                is RackOven -> sorted[value.computable.auditId]!![rackOven]!!.add(value)
//                is SteamCooker -> sorted[value.computable.auditId]!![steamCooker]!!.add(value)


                // Other
                else -> sorted[value.computable.auditId]!![other]!!.add(value)
            }
        }

        return sorted
    }

    private fun prepareValuesForEquipment(audit: AuditComponents): EquipmentValues? {
        var totalSavings = 0.0
        var totalCost = 0.0
        val netPresentValue = 0.0
        val instances = mutableListOf<EquipmentInstances>()

        var currentTotalkW = 0.0
        var currentTotalkWh = 0.0
        var postTotalkW = 0.0
        var postTotalkWh = 0.0

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
        val steamCooker = audit[steamCooker]!! as List<SteamCooker>

        @Suppress("UNCHECKED_CAST")
        val griddle = audit[griddle]!! as List<Griddle>

        @Suppress("UNCHECKED_CAST")
        val hotFoodCabinet = audit[hotFoodCabinet]!! as List<HotFoodCabinet>

        @Suppress("UNCHECKED_CAST")
        val conveyorBroiler = audit[conveyorBroiler]!! as List<ConveyorBroiler>

        @Suppress("UNCHECKED_CAST")
        val dishwasher = audit[dishWasher]!! as List<DishWasher>

        @Suppress("UNCHECKED_CAST")
        val preRinseSpray = audit[preRinseSpray]!! as List<PreRinseSpray>

        if (combinationOven.any()) {
            var costElectricity = 0.0
            var materialCost = 0.0
            var implementationCost = 0.0
            var savings = 0.0

            val single = combinationOven.first()

            for (item in combinationOven) {
                costElectricity += item.costPostState
                materialCost += item.materialCost()
                implementationCost += item.implementationCost()
                savings += item.costPreState(listOf()) - item.costPostState

                // @anthony, please verify that these are the correct values
                if (!item.isGas()) {
//                    currentTotalkW +=
//                    currentTotalkWh +=
//                    postTotalkW +=
//                    postTotalkWh +=
                }
            }

            val costOverSavings =
                    if (savings == 0.0) 0.0
                    else implementationCost / savings

            instances.add(
                    EquipmentInstances(
                            "Combination Oven",
                            single.energyPowerChange(),
                            costElectricity,
                            single.age,
                            materialCost,
                            implementationCost,
                            savings,
                            costOverSavings * 12
                    )
            )
        }

        // TODO: add convection oven when implemented
        if (convectionOven.any()) {
        }

        if (conveyorBroiler.any()) {
            var costElectricity = 0.0
            var materialCost = 0.0
            var implementationCost = 0.0
            var savings = 0.0

            val single = conveyorBroiler.first()

            for (broiler in conveyorBroiler) {
                costElectricity += broiler.costPostState
                materialCost += broiler.materialCost()
                implementationCost += broiler.implementationCost()
                savings += broiler.costPreState(listOf()) - broiler.costPostState

                // @anthony, please verify that these are the correct values
                if (!broiler.isGas()) {
//                    currentTotalkW +=
//                    currentTotalkWh +=
//                    postTotalkW +=
//                    postTotalkWh +=
                }
            }

            val costOverSavings =
                    if (savings == 0.0) 0.0
                    else implementationCost / savings

            instances.add(
                    EquipmentInstances(
                            "Conveyor Broiler",
                            single.energyPowerChange(),
                            costElectricity,
                            single.age,
                            materialCost,
                            implementationCost,
                            savings,
                            costOverSavings * 12
                    )
            )
        }

        // TODO: add conveyor oven when implemented
        if (conveyorOven.any()) {
        }

        if (dishwasher.any()) {
            var costElectricity = 0.0
            var materialCost = 0.0
            var implementationCost = 0.0
            var savings = 0.0

            val single = dishwasher.first()

            for (washer in dishwasher) {
                val postState = washer.buildPostState().blockingGet()
                val element = postState.getAsJsonArray("results")[0].asJsonObject.get("data")

                costElectricity += washer.costPostState(element, DataHolder())
                materialCost += washer.materialCost()
                implementationCost += washer.implementationCost()
                savings += washer.costPreState(listOf(null)) - washer.costPostState

                // @anthony, please verify that these are the correct values
                if (!washer.isGas()) {
//                    currentTotalkW +=
//                    currentTotalkWh +=
//                    postTotalkW +=
//                    postTotalkWh +=
                }
            }

            val costOverSavings =
                    if (savings == 0.0) 0.0
                    else implementationCost / savings

            instances.add(
                    EquipmentInstances(
                            "Dishwasher",
                            single.energyPowerChange(),
                            costElectricity,
                            single.age,
                            materialCost,
                            implementationCost,
                            savings,
                            costOverSavings * 12
                    )
            )
        }

        // TODO: add fryer when implemented
        if (fryer.any()) {
        }

        if (griddle.any()) {
            var costElectricity = 0.0
            var materialCost = 0.0
            var implementationCost = 0.0
            var savings = 0.0

            val single = griddle.first()

            for (item in griddle) {
                costElectricity += item.costPostState
                materialCost += item.materialCost()
                implementationCost += item.implementationCost()
                savings += item.costPreState(listOf(null)) - item.costPostState

                // @anthony, please verify that these are the correct values
                if (!item.isGas()) {
//                    currentTotalkW +=
//                    currentTotalkWh +=
//                    postTotalkW +=
//                    postTotalkWh +=
                }
            }

            val costOverSavings =
                    if (savings == 0.0) 0.0
                    else implementationCost / savings

            instances.add(
                    EquipmentInstances(
                            "Griddle",
                            single.energyPowerChange(),
                            costElectricity,
                            single.age,
                            materialCost,
                            implementationCost,
                            savings,
                            costOverSavings * 12
                    )
            )
        }

        if (hotFoodCabinet.any()) {
            var costElectricity = 0.0
            var materialCost = 0.0
            var implementationCost = 0.0
            var savings = 0.0

            val single = hotFoodCabinet.first()

            for (cabinet in hotFoodCabinet) {
                costElectricity += cabinet.costPostState
                materialCost += cabinet.materialCost()
                implementationCost += cabinet.implementationCost()
                savings += cabinet.costPreState(listOf(null)) - cabinet.costPostState

                // @anthony, please verify that these are the correct values
//                        currentTotalkW +=
//                        currentTotalkWh +=
//                        postTotalkW +=
//                        postTotalkWh +=
            }

            val costOverSavings =
                    if (savings == 0.0) 0.0
                    else implementationCost / savings

            instances.add(
                    EquipmentInstances(
                            "Hot Food Cabinet",
                            single.energyPowerChange(),
                            costElectricity,
                            single.age,
                            materialCost,
                            implementationCost,
                            savings,
                            costOverSavings * 12
                    )
            )
        }

        if (iceMaker.any()) {
            var costElectricity = 0.0
            var materialCost = 0.0
            var implementationCost = 0.0
            var savings = 0.0

            val single = iceMaker.first()

            for (item in iceMaker) {
                costElectricity += item.costPostState
                materialCost += item.materialCost()
                implementationCost += item.implementationCost()
                savings += item.costPreState(listOf()) - item.costPostState

                // @anthony, please verify that these are the correct values
                if (!item.isGas()) {
//                    currentTotalkW +=
//                    currentTotalkWh +=
//                    postTotalkW +=
//                    postTotalkWh +=
                }
            }

            val costOverSavings =
                    if (savings == 0.0) 0.0
                    else implementationCost / savings

            instances.add(
                    EquipmentInstances(
                            "Ice Maker",
                            single.energyPowerChange(),
                            costElectricity,
                            single.age,
                            materialCost,
                            implementationCost,
                            savings,
                            costOverSavings * 12
                    )
            )
        }

        if (preRinseSpray.any()) {
            var costElectricity = 0.0
            var materialCost = 0.0
            var implementationCost = 0.0
            var savings = 0.0

            val single = preRinseSpray.first()

            for (spray in preRinseSpray) {
                costElectricity += spray.costPostState
                materialCost += spray.materialCost()
                implementationCost += spray.implementationCost()
                savings += spray.costPreState(listOf(null)) - spray.costPostState

                // @anthony, please verify that these are the correct values
                if (!spray.isGas()) {
                    currentTotalkW += spray.preElectricPower
                    currentTotalkWh += spray.preElectricPower * spray.usageHoursPre()
                    postTotalkW += spray.postElectricPower
                    postTotalkWh += spray.postElectricPower * spray.usageHoursPost()
                }
            }

            val costOverSavings =
                    if (savings == 0.0) 0.0
                    else implementationCost / savings

            instances.add(
                    EquipmentInstances(
                            "Pre Rinse Spray",
                            single.energyPowerChange(),
                            costElectricity,
                            single.age,
                            materialCost,
                            implementationCost,
                            savings,
                            costOverSavings * 12
                    )
            )
        }

        // TODO add SteamCooker when implemented
        if (steamCooker.any()) {
        }

        for (instance in instances) {
            totalSavings += instance.costElectricity
            totalCost += instance.implementationCost
        }

        return EquipmentValues(
                instances.toList(),
                totalSavings,
                totalCost,
                netPresentValue,
                currentTotalkW,
                currentTotalkWh,
                postTotalkW,
                postTotalkWh
        )
    }

    private fun prepareBuildingValuesForEquipment(
            lightings: LightingValues?,
            equipments: EquipmentValues?,
            hvacs: HvacValues?,
            waterHeater: WaterHeaterValues?,
            refrigeration: RefrigerationValues?): BuildingValues {

        fun calculatePaybackYear(cost: Double?, savings: Double?) =
                if (cost != null && savings != null && savings > 0.0)
                    cost / savings
                else 0.0

        fun calculatePaybackMonth(cost: Double?, savings: Double?) =
                if (cost != null && savings != null && savings > 0.0)
                    cost / savings * 12
                else 0.0

        val buildingTotalSavings = (lightings?.totalcostsavings ?: 0.0) +
                (equipments?.totalSavings ?: 0.0) +
                (hvacs?.totalSavings ?: 0.0) +
                (waterHeater?.totalSavings ?: 0.0) +
                (refrigeration?.totalSavings ?: 0.0)

        val buildingTotalCost = (lightings?.totalCost ?: 0.0) +
                (hvacs?.totalCost ?: 0.0) +
                (equipments?.totalCost ?: 0.0) +
                (waterHeater?.totalCost ?: 0.0) +
                (refrigeration?.totalCost ?: 0.0)

        val buildingPayback =
                if (buildingTotalSavings == 0.0) 0.0
                else buildingTotalCost / buildingTotalSavings

        val buildingPaybackMonth =
                if (buildingTotalSavings == 0.0) 0.0
                else buildingTotalCost / buildingTotalSavings * 12

        val buildingNetPresentValue =(lightings?.netPresentValue ?: 0.0) +
                        (hvacs?.netPresentValue ?: 0.0) +
                        (equipments?.netPresentValue ?: 0.0) +
                        (waterHeater?.netPresentValue ?: 0.0) +
                        (refrigeration?.netPresentValue ?: 0.0)

        val currentTotalkW = (lightings?.currentTotalkW ?: 0.0) +
                (hvacs?.currentTotalkW ?: 0.0) +
                (equipments?.currentTotalkW ?: 0.0) +
                (waterHeater?.currentTotalkW ?: 0.0) +
                (refrigeration?.currentTotalkW ?: 0.0)

        val currentTotalkWh = (lightings?.currentTotalkWh ?: 0.0) +
                (hvacs?.currentTotalkWh ?: 0.0) +
                (equipments?.currentTotalkWh ?: 0.0) +
                (waterHeater?.currentTotalkWh ?: 0.0) +
                (refrigeration?.currentTotalkWh ?: 0.0)

        val postTotalkW = (lightings?.postTotalkW ?: 0.0) +
                (hvacs?.postTotalkW ?: 0.0) +
                (equipments?.postTotalkW ?: 0.0) +
                (waterHeater?.postTotalkW ?: 0.0) +
                (refrigeration?.postTotalkW ?: 0.0)

        val postTotalkWh = (lightings?.postTotalkWh ?: 0.0) +
                (hvacs?.postTotalkWh ?: 0.0) +
                (equipments?.postTotalkWh ?: 0.0) +
                (waterHeater?.postTotalkWh ?: 0.0) +
                (refrigeration?.postTotalkWh ?: 0.0)

        return BuildingValues(
                buildingNetPresentValue,
                buildingTotalSavings,
                buildingPayback,
                buildingPaybackMonth,
                buildingTotalCost,
                hvacs?.totalCost ?: 0.0,
                calculatePaybackYear(hvacs?.totalCost, hvacs?.totalSavings),
                calculatePaybackMonth(hvacs?.totalCost, hvacs?.totalSavings),
                equipments?.totalCost ?: 0.0,
                calculatePaybackYear(equipments?.totalCost, equipments?.totalSavings),
                calculatePaybackMonth(equipments?.totalCost, equipments?.totalSavings),
                refrigeration?.totalCost ?: 0.0,
                calculatePaybackYear(refrigeration?.totalCost, refrigeration?.totalSavings),
                calculatePaybackMonth(refrigeration?.totalCost, refrigeration?.totalSavings),
                waterHeater?.totalCost ?: 0.0,
                calculatePaybackYear(waterHeater?.totalCost, waterHeater?.totalSavings),
                calculatePaybackMonth(waterHeater?.totalCost, waterHeater?.totalSavings),
                currentTotalkW,
                currentTotalkWh,
                postTotalkW,
                postTotalkWh
        )
    }
}
