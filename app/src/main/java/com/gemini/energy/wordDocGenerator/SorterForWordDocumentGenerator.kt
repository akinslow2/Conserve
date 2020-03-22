package com.gemini.energy.wordDocGenerator

import com.gemini.energy.presentation.util.EApplianceType
import com.gemini.energy.service.DataHolder
import com.gemini.energy.service.device.EBase
import com.gemini.energy.service.device.Hvac
import com.gemini.energy.service.device.WaterHeater
import com.gemini.energy.service.device.lighting.*
import com.gemini.energy.service.device.plugload.*
import com.gemini.energy.service.device.refrigeration.*
import com.google.gson.JsonNull


class SorterForWordDocumentGenerator {
    // use these values for sorting types of equipment that need to be aggregated for the report
    companion object {
        private const val hvac = "hvac"
        private const val lighting = "lighting"
        private const val waterheater = "waterheater"
        private const val refrigeration = "refrigeration"

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
            val hvac = prepareValuesFromHvac(audit.value)
            val waterheater = prepareValuesFromWaterHeater(audit.value)
            val lighting = prepareValuesFromLighting(audit.value)
            val equipment = prepareValuesForEquipment(audit.value)
            val refrigeration = prepareValuesFromRefrigeration(audit.value)
            val building = prepareBuildingValuesForEquipment(lighting, equipment, hvac, waterheater, refrigeration)

            val zones = aggregateZoneNames(audit.value)
            val zoneString = concatenateZoneString(zones)

            if (hvac == null || hvac.businessname.isBlank()) {
                continue
            } else {
                returnAble.add(PreparedForDocument(
                        audit.key,
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
                is WICoolerBot -> sorted[value.computable.auditId]!![refrigeration]!!.add(value)

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

    private fun prepareValuesFromHvac(audit: AuditComponents): HvacValues? {
        if (!audit[hvac]!!.any()) {
            return null
        }

        @Suppress("UNCHECKED_CAST")
        val hvacs = audit[hvac]!! as MutableList<Hvac>

        var costPostState = 0.0
        var totalCost = 0.0
        var totalSavings = 0.0
        val instances = mutableListOf<HvacInstances>()

        for (hvac in hvacs) {
            val postState = hvac.buildPostState().blockingGet()
            val element = postState.getAsJsonArray("results")[0].asJsonObject.get("data")

            costPostState += hvac.costPostState(element, DataHolder())

            totalSavings += hvac.totalSavings()
            totalCost = hvac.implementationCost()

            instances.add(HvacInstances(
                    hvac.quantity,
                    hvac.year,
                    hvac.age(),
                    hvac.btu,
                    hvac.seer,
                    hvac.overAge(),
                    hvac.economizer,
                    hvac.thermotype
            ))
        }

        val hvac = audit[hvac]!!.first() as Hvac

        return HvacValues(
                instances.toList(),

                hvac.businessname,
                hvac.auditmonth,
                hvac.audityear,
                hvac.clientaddress,
                hvac.startday,
                hvac.endday,
                hvac.operationhours,
                hvac.bldgarea,
                hvac.bldgtype,
                hvac.electricstructure,
                hvac.utilitycompany,
                hvac.gasstructure,

                costPostState,
                totalCost,
                totalSavings
        )
    }

    private fun prepareValuesFromWaterHeater(audit: AuditComponents): WaterHeaterValues? {
        if (!audit[waterheater]!!.any()) {
            return null
        }

        @Suppress("UNCHECKED_CAST")
        val waterheaters = audit[waterheater]!! as MutableList<WaterHeater>

        var costPostState = 0.0
        var totalCost = 0.0
        var totalSavings = 0.0

        for (waterheater in waterheaters) {
            val postState = waterheater.buildPostState().blockingGet()
            val element = postState.getAsJsonArray("results")[0].asJsonObject.get("data")

            costPostState += waterheater.costPostState(element, DataHolder())

            totalSavings += waterheater.totalSavings()
            totalCost += waterheater.implementationCost()
        }

        val waterheater = audit[waterheater]!!.first() as WaterHeater
        val paybackMonth = (totalCost / totalSavings * 12) + 4
        val paybackYear: Double = (totalCost / totalSavings) + (4 / 12)

        return WaterHeaterValues(
                totalSavings,
                totalCost,
                paybackMonth,
                paybackYear,
                waterheater.quantity,
                waterheater.year,
                waterheater.age(),
                waterheater.thermaleff,
                waterheater.fueltype,
                waterheater.unittype,
                waterheater.capacity)
    }

    private fun prepareValuesFromRefrigeration(audit: AuditComponents): RefrigerationValues? {
        if (!audit[refrigeration]!!.any()) {
            return null
        }

        val wiRefigerators = mutableListOf<WIRefrigerator>()
        val wiFreezers = mutableListOf<WIFreezer>()
        val refrigerators = mutableListOf<Refrigerator>()
        val freezers = mutableListOf<Freezer>()
        val wiCoolerBot = mutableListOf<WICoolerBot>()

        for (type in audit[refrigeration]!!) {
            when (type) {
                is WIRefrigerator -> wiRefigerators.add(type)
                is WIFreezer -> wiFreezers.add(type)
                is Refrigerator -> refrigerators.add(type)
                is Freezer -> freezers.add(type)
                is WICoolerBot -> wiCoolerBot.add(type)
            }
        }

        var totalCost = 0.0
        var totalSavings = 0.0

        for (fridge in wiRefigerators) {
            totalCost += fridge.installCost
            totalSavings += fridge.grosskwhSavings
        }

        for (fridge in wiFreezers) {
            totalCost += fridge.installCost
            totalSavings += fridge.grosskwhSavings
        }

        for (fridge in refrigerators) {
            totalCost += fridge.installCost()
            totalSavings += fridge.grosskwhSavings()
        }

        for (fridge in freezers) {
            totalCost += fridge.installCost()
            totalSavings += fridge.grosskwhSavings()
        }

        for (fridge in wiCoolerBot) {
            totalCost += fridge.installCost()
            totalSavings += fridge.grosskwhSavings()
        }

        val paybackMonth = if (totalSavings == 0.0) 0.0 else totalCost / totalSavings * 12

        return RefrigerationValues(
                totalCost,
                totalSavings,
                paybackMonth
        )
    }

    private fun prepareValuesFromLighting(audit: AuditComponents): LightingValues? {
        if (!audit[lighting]!!.any()) {
            return null
        }

        val cfls = mutableListOf<Cfl>()
        val halogens = mutableListOf<Halogen>()
        val highPressureSodiums = mutableListOf<HPSodium>()
        val incandescents = mutableListOf<Incandescent>()
        val linearFluorescents = mutableListOf<LinearFluorescent>()
        val lowPressureSodium = mutableListOf<LPSodium>()

        for (type in audit[lighting]!!) {
            type.costPreState(listOf(JsonNull.INSTANCE))
            when (type) {
                is Cfl -> cfls.add(type)
                is Halogen -> halogens.add(type)
                is HPSodium -> highPressureSodiums.add(type)
                is Incandescent -> incandescents.add(type)
                is LinearFluorescent -> linearFluorescents.add(type)
                is LPSodium -> lowPressureSodium.add(type)
            }
        }

        var totalCostSavings = 0.0
        var selfinstallcost = 0.0
        var electricianCost = 0.0
        var totalEnergySavings = 0.0
        val lightingRows = prepareLightingTableRows(audit[lighting]!!)

        for (light in cfls) {
            totalCostSavings += light.totalSavings()
            selfinstallcost += light.selfinstallcost()
            electricianCost += light.electricianCost
            if (electricianCost < 100.0) {
                electricianCost = 100.0
            }
            totalEnergySavings += light.totalEnergySavings()
        }

        for (light in halogens) {
            totalCostSavings += light.totalSavings()
            selfinstallcost += light.selfinstallcost()
            electricianCost += light.electricianCost
            if (electricianCost < 100.0) {
                electricianCost = 100.0
            }
            totalEnergySavings += light.totalEnergySavings()
        }

        for (light in highPressureSodiums) {
            totalCostSavings += light.totalSavings()
            selfinstallcost += light.selfinstallcost()
            electricianCost += light.electricianCost
            if (electricianCost < 100.0) {
                electricianCost = 100.0
            }
            totalEnergySavings += light.totalEnergySavings()
        }

        for (light in incandescents) {
            totalCostSavings += light.totalSavings()
            selfinstallcost += light.selfinstallcost()
            electricianCost += light.electricianCost
            if (electricianCost < 100.0) {
                electricianCost = 100.0
            }
            totalEnergySavings += light.totalEnergySavings()
        }

        for (light in linearFluorescents) {
            totalCostSavings += light.totalSavings()
            selfinstallcost += light.selfinstallcost()
            electricianCost += light.electricianCost
            if (electricianCost < 100.0) {
                electricianCost = 100.0
            }
            totalEnergySavings += light.totalEnergySavings()
        }

        for (light in lowPressureSodium) {
            totalCostSavings += light.totalSavings()
            selfinstallcost += light.selfinstallcost()
            electricianCost += light.electricianCost
            if (electricianCost < 100.0) {
                electricianCost = 100.0
            }
            totalEnergySavings += light.totalEnergySavings()
        }

        val installCost = electricianCost + selfinstallcost
        val paybackMonth = selfinstallcost / totalCostSavings * 12
        val geminiPayback = paybackMonth + 4
        val paybackYear: Double = selfinstallcost / totalCostSavings

        return LightingValues(
                totalCostSavings,
                totalEnergySavings,
                selfinstallcost.toInt(),
                installCost,
                paybackMonth,
                geminiPayback,
                paybackYear,
                lightingRows)
    }

    private fun prepareLightingTableRows(lights: List<EBase>): List<LightingDataRow> {
        val rows = mutableListOf<LightingDataRow>()

        var measure = 1
        for (light in lights) {
            when (light) {
                is Cfl -> {
                    rows.add(LightingDataRow(
                            "$measure",
                            light.computable.zoneName,
                            light.computable.auditScopeName,
                            light.lampsPerFixtures.toDouble() * light.numberOfFixtures.toDouble(),
                            light.prePower(),
                            light.usageHoursPre(),
                            light.postPower(),
                            light.usageHoursPost(),
                            light.totalEnergySavings(),
                            light.totalSavings(),
                            light.selfinstallcost(),
                            light.selfinstallcost() / light.totalSavings() * 12,
                            light.selfinstallcost() / light.totalSavings()
                    ))
                }
                is Halogen -> {
                    rows.add(LightingDataRow(
                            "$measure",
                            light.computable.zoneName,
                            light.computable.auditScopeName,
                            light.lampsPerFixtures.toDouble() * light.numberOfFixtures.toDouble(),
                            light.preEnergy(),
                            light.offPeakHours,
                            light.postEnergy(),
                            light.usageHoursPost(),
                            light.energySavings(),
                            light.totalSavings(),
                            light.selfinstallcost(),
                            light.selfinstallcost() / light.totalSavings() * 12,
                            light.selfinstallcost() / light.totalSavings()
                    ))
                }
                is HPSodium -> {
                    rows.add(LightingDataRow(
                            "$measure",
                            light.computable.zoneName,
                            light.computable.auditScopeName,
                            light.lampsPerFixtures.toDouble() * light.numberOfFixtures.toDouble(),
                            light.preEnergy(),
                            light.offPeakHours,
                            light.postEnergy(),
                            light.usageHoursPost(),
                            light.energySavings(),
                            light.totalSavings(),
                            light.selfinstallcost(),
                            light.selfinstallcost() / light.totalSavings() * 12,
                            light.selfinstallcost() / light.totalSavings()
                    ))
                }
                is Incandescent -> {
                    rows.add(LightingDataRow(
                            "$measure",
                            light.computable.zoneName,
                            light.computable.auditScopeName,
                            light.lampsPerFixtures.toDouble() * light.numberOfFixtures.toDouble(),
                            light.preEnergy(),
                            light.offPeakHours,
                            light.postEnergy(),
                            light.usageHoursPost(),
                            light.energySavings(),
                            light.totalSavings(),
                            light.selfinstallcost(),
                            light.selfinstallcost() / light.totalSavings() * 12,
                            light.selfinstallcost() / light.totalSavings()
                    ))
                }
                is LinearFluorescent -> {
                    rows.add(LightingDataRow(
                            "$measure",
                            light.computable.zoneName,
                            light.computable.auditScopeName,
                            light.lampsPerFixtures.toDouble() * light.numberOfFixtures.toDouble(),
                            light.preEnergy(),
                            light.offPeakHours,
                            light.postEnergy(),
                            light.usageHoursPost(),
                            light.energySavings(),
                            light.totalSavings(),
                            light.selfinstallcost(),
                            light.selfinstallcost() / light.totalSavings() * 12,
                            light.selfinstallcost() / light.totalSavings()
                    ))
                }
                is LPSodium -> {
                    rows.add(LightingDataRow(
                            "$measure",
                            light.computable.zoneName,
                            light.computable.auditScopeName,
                            light.lampsPerFixtures.toDouble() * light.numberOfFixtures.toDouble(),
                            light.preEnergy(),
                            light.offPeakHours,
                            light.postEnergy(),
                            light.usageHoursPost(),
                            light.energySavings(),
                            light.totalSavings(),
                            light.selfinstallcost(),
                            light.selfinstallcost() / light.totalSavings() * 12,
                            light.selfinstallcost() / light.totalSavings()
                    ))
                }
            }
            measure += 1
        }

        return rows.toList()
    }

    private fun prepareValuesForEquipment(audit: AuditComponents): EquipmentValues? {
        var totalSavings = 0.0
        var totalCost = 0.0
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
            }

            instances.add(
                    EquipmentInstances(
                            "Combination Oven",
                            single.energyPowerChange(),
                            costElectricity,
                            single.age,
                            materialCost,
                            implementationCost,
                            savings,
                            implementationCost / savings * 12
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
            }

            instances.add(
                    EquipmentInstances(
                            "Conveyor Broiler",
                            single.energyPowerChange(),
                            costElectricity,
                            single.age,
                            materialCost,
                            implementationCost,
                            savings,
                            implementationCost / savings * 12
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
            }

            instances.add(
                    EquipmentInstances(
                            "Dishwasher",
                            single.energyPowerChange(),
                            costElectricity,
                            single.age.toDouble(),
                            materialCost,
                            implementationCost,
                            savings,
                            implementationCost / savings * 12
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
            }

            instances.add(
                    EquipmentInstances(
                            "Griddle",
                            single.energyPowerChange(),
                            costElectricity,
                            single.age.toDouble(),
                            materialCost,
                            implementationCost,
                            savings,
                            implementationCost / savings * 12
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
            }

            instances.add(
                    EquipmentInstances(
                            "Hot Food Cabinet",
                            single.energyPowerChange(),
                            costElectricity,
                            single.age.toDouble(),
                            materialCost,
                            implementationCost,
                            savings,
                            implementationCost / savings * 12
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
            }

            instances.add(
                    EquipmentInstances(
                            "Ice Maker",
                            single.energyPowerChange(),
                            costElectricity,
                            single.age,
                            materialCost,
                            implementationCost,
                            savings,
                            implementationCost / savings * 12
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
            }

            instances.add(
                    EquipmentInstances(
                            "Pre Rinse Spray",
                            single.energyPowerChange(),
                            costElectricity,
                            single.age.toDouble(),
                            materialCost,
                            implementationCost,
                            savings,
                            implementationCost / savings * 12
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
                totalCost
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

        return BuildingValues(
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
                calculatePaybackMonth(waterHeater?.totalCost, waterHeater?.totalSavings)
        )
    }
}
