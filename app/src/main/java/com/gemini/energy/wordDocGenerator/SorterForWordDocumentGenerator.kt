package com.gemini.energy.wordDocGenerator

import com.gemini.energy.presentation.util.EApplianceType
import com.gemini.energy.service.DataHolder
import com.gemini.energy.service.device.EBase
import com.gemini.energy.service.device.Hvac
import com.gemini.energy.service.device.lighting.*
import com.gemini.energy.service.device.plugload.*
import com.google.gson.JsonNull

class SorterForWordDocumentGenerator {
    // use these values for sorting types of equipment that need to be aggregated for the report
    companion object {
        private const val hvac = "hvac"
        private const val lighting = "lighting"

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

        private const val other = "other"
    }

    fun prepareAllValues(values: MutableList<EBase>): List<PreparedForDocument> {
        val sortedAudits = sortEbasesIntoAudits(values)

        val returnAble = mutableListOf<PreparedForDocument>()

        for (audit in sortedAudits) {
            val hvac = prepareValuesFromHvac(audit.value)
            val lighting = prepareValuesFromLighting(audit.value)
            val equipment = prepareValuesForEquipment(audit.value)
            val building = prepareBuildingValuesForEquipment(lighting, equipment, hvac)

            val zones = aggregateZoneNames(audit.value)
            val zoneString = concatenateZoneString(zones)

            if (hvac == null) {
                continue
            } else {
                returnAble.add(PreparedForDocument(audit.key, zones, zoneString, hvac, lighting, equipment, building))
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
                        lighting to mutableListOf(),

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
                        refrigerator to mutableListOf(),
                        steamCooker to mutableListOf(),

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
                is ConveyorBroiler -> sorted[value.computable.auditId]!![conveyorBroiler]!!.add(value)
//                is ConveyorOven -> sorted[value.computable.auditId]!![convectionOven]!!.add(value)
                is DishWasher -> sorted[value.computable.auditId]!![dishWasher]!!.add(value)
//                is Fryer -> sorted[value.computable.auditId]!![fryer]!!.add(value)
                is Griddle -> sorted[value.computable.auditId]!![griddle]!!.add(value)
                is HotFoodCabinet -> sorted[value.computable.auditId]!![hotFoodCabinet]!!.add(value)
                is IceMaker -> sorted[value.computable.auditId]!![iceMaker]!!.add(value)
                is PreRinseSpray -> sorted[value.computable.auditId]!![preRinseSpray]!!.add(value)
//                is RackOven -> sorted[value.computable.auditId]!![rackOven]!!.add(value)
                is Refrigerator -> sorted[value.computable.auditId]!![refrigerator]!!.add(value)
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

            totalSavings += hvac.energyPowerChange()
            totalCost += hvac.implementationCost()

            instances.add(HvacInstances(
                    hvac.quantity,
                    hvac.year(),
                    hvac.age,
                    hvac.tons,
                    hvac.seer,
                    hvac.overAge()
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
            type.costPreState(listOf(JsonNull.INSTANCE))
            when (type) {
                is Cfl -> cfls.add(type)
                is Halogen -> halogens.add(type)
                is HighPressureSodium -> highPressureSodiums.add(type)
                is Incandescent -> incandescents.add(type)
                is LinearFluorescent -> linearFluorescents.add(type)
                is LowPressureSodium -> lowPressureSodium.add(type)
            }
        }

        var totalSavings = 0.0
        var selfinstallcost = 0.0
        var electricianCost = 0
        val lightingRows = prepareLightingTableRows(audit[lighting]!!)

        for (light in cfls) {
            totalSavings += light.totalSavings()
            selfinstallcost += light.selfinstallcost()
            electricianCost = light.electricianCost
        }

        for (light in halogens) {
            totalSavings += light.totalSavings()
            selfinstallcost += light.selfinstallcost()
            electricianCost = light.electricianCost
        }

        for (light in highPressureSodiums) {
            totalSavings += light.totalSavings()
            selfinstallcost += light.selfinstallcost()
            electricianCost = light.electricianCost
        }

        for (light in incandescents) {
            totalSavings += light.totalSavings()
            selfinstallcost += light.selfinstallcost()
            electricianCost = light.electricianCost
        }

        for (light in linearFluorescents) {
            totalSavings += light.totalSavings()
            selfinstallcost += light.selfinstallcost()
            electricianCost = light.electricianCost
        }

        for (light in lowPressureSodium) {
            totalSavings += light.totalSavings()
            selfinstallcost += light.selfinstallcost()
            electricianCost = light.electricianCost
        }

        val installCost = electricianCost + selfinstallcost
        val paybackMonth = selfinstallcost / totalSavings * 12
        val geminiPayback = paybackMonth + 4
        val paybackYear: Double = selfinstallcost / totalSavings

        return LightingValues(
                totalSavings,
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
                            light.preEnergy(),
                            light.offPeakHours,
                            light.postEnergy(),
                            light.postUsageHours.toDouble(),
                            light.energySavings(),
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
                            light.postUsageHours.toDouble(),
                            light.energySavings(),
                            light.totalSavings(),
                            light.selfinstallcost().toDouble(),
                            light.selfinstallcost() / light.totalSavings() * 12,
                            light.selfinstallcost() / light.totalSavings()
                    ))
                }
                is HighPressureSodium -> {
                    rows.add(LightingDataRow(
                            "$measure",
                            light.computable.zoneName,
                            light.computable.auditScopeName,
                            light.lampsPerFixtures.toDouble() * light.numberOfFixtures.toDouble(),
                            light.preEnergy(),
                            light.offPeakHours,
                            light.postEnergy(),
                            light.postUsageHours.toDouble(),
                            light.energySavings(),
                            light.totalSavings(),
                            light.selfinstallcost().toDouble(),
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
                            light.postUsageHours.toDouble(),
                            light.energySavings(),
                            light.totalSavings(),
                            light.selfinstallcost().toDouble(),
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
                            light.postUsageHours.toDouble(),
                            light.energySavings(),
                            light.totalSavings(),
                            light.selfinstallcost(),
                            light.selfinstallcost() / light.totalSavings() * 12,
                            light.selfinstallcost() / light.totalSavings()
                    ))
                }
                is LowPressureSodium -> {
                    rows.add(LightingDataRow(
                            "$measure",
                            light.computable.zoneName,
                            light.computable.auditScopeName,
                            light.lampsPerFixtures.toDouble() * light.numberOfFixtures.toDouble(),
                            light.preEnergy(),
                            light.offPeakHours,
                            light.postEnergy(),
                            light.postUsageHours.toDouble(),
                            light.energySavings(),
                            light.totalSavings(),
                            light.selfinstallcost().toDouble(),
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
                            single.age,
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
                            single.age,
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
                            single.age,
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
                            single.age,
                            materialCost,
                            implementationCost,
                            savings,
                            implementationCost / savings * 12
                    )
            )
        }

        if (refrigerator.any()) {
            var costElectricity = 0.0
            var materialCost = 0.0
            var implementationCost = 0.0
            var savings = 0.0

            val single = refrigerator.first()

            for (item in refrigerator) {
                costElectricity += item.costPostState
                materialCost += item.materialCost()
                implementationCost += item.implementationCost()
                savings += item.costPreState(listOf(null)) - item.costPostState
            }

            instances.add(
                    EquipmentInstances(
                            "Refrigerator",
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

    private fun prepareBuildingValuesForEquipment(lightings: LightingValues?, equpments: EquipmentValues?, hvacs: HvacValues?): BuildingValues {

        val buildingTotalSavings = (lightings?.totalsavings ?: 0.0) + (equpments?.totalSavings
                ?: 0.0) + (hvacs?.totalsavings ?: 0.0)

        val buildingTotalCost = (lightings?.totalCost ?: 0.0) + (hvacs?.totalCost
                ?: 0.0) + (equpments?.totalCost ?: 0.0)

        val buildingPayback = buildingTotalCost / buildingTotalSavings

        val buildingPaybackMonth = buildingTotalCost / buildingTotalSavings * 12

        return BuildingValues(
                buildingTotalSavings,
                buildingPayback,
                buildingPaybackMonth,
                buildingTotalCost,
                (hvacs?.totalCost ?: 0.0),
                (hvacs?.totalCost ?: 0.0) / (hvacs?.totalsavings ?: 0.0),
                (hvacs?.totalCost ?: 0.0) / (hvacs?.totalsavings ?: 0.0) * 12,
                equpments?.totalCost ?: 0.0,
                (equpments?.totalCost ?: 0.0) / (equpments?.totalSavings ?: 0.0),
                (equpments?.totalCost ?: 0.0) / (equpments?.totalSavings ?: 0.0) * 12
        )
    }
}
