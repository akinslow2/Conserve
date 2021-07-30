package com.gemini.energy.wordDocGenerator

import com.gemini.energy.presentation.util.EApplianceType
import com.gemini.energy.service.DataHolder
import com.gemini.energy.service.device.EBase
import com.gemini.energy.service.device.Hvac
import com.gemini.energy.service.device.Motors
import com.gemini.energy.service.device.WaterHeater
import com.gemini.energy.service.device.lighting.*
import com.gemini.energy.service.device.plugload.*
import com.gemini.energy.service.device.refrigeration.*
import com.gemini.energy.wordDocGenerator.models.HvacValues
import com.gemini.energy.wordDocGenerator.models.PreAuditValues
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import java.util.*

class SorterForWordDocumentGenerator {
    // use these values for sorting types of equipment that need to be aggregated for the report
    companion object {
        const val hvac = "hvac"
        private const val lighting = "lighting"
        private const val waterheater = "waterheater"
        private const val refrigeration = "refrigeration"
        private const val motors = "motors"

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
            val waterheater = prepareValuesFromWaterHeater(audit.value)
            val lighting = prepareValuesFromLighting(audit.value)
            val equipment = prepareValuesForEquipment(audit.value)
            val refrigeration = prepareValuesFromRefrigeration(audit.value)
            val motor = prepareMotorValues(audit.value)
            val building = prepareBuildingValuesForEquipment(lighting, equipment, hvac, waterheater, refrigeration, motor)
            val preaudit = PreAuditValues.tryCreate(audit.value)

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
                        motor,
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
                        motors to mutableListOf(),

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

                // Motors
                is Motors -> sorted[value.computable.auditId]!![motors]!!.add(value)

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

    private fun prepareValuesFromWaterHeater(audit: AuditComponents): WaterHeaterValues? {
        if (!audit[waterheater]!!.any()) {
            return null
        }

        @Suppress("UNCHECKED_CAST")
        val waterheaters = audit[waterheater]!! as MutableList<WaterHeater>

        var costPostState = 0.0
        var totalCost = 0.0
        var totalSavings = 0.0
        val presentvaluefactor = 10.38

        var currentTotalkW = 0.0
        var currentTotalkWh = 0.0
        var postTotalkW = 0.0
        var postTotalkWh = 0.0

        for (waterheater in waterheaters) {
            val postState = waterheater.buildPostState().blockingGet()
            var element: JsonElement = JsonObject()
            if (postState.getAsJsonArray("results").size() > 0)
                element = postState.getAsJsonArray("results")[0].asJsonObject.get("data")

            costPostState += waterheater.costPostState(element, DataHolder())

            totalSavings += waterheater.totalSavings()
            totalCost += waterheater.implementationCost()

            // @anthony, please verify that these are the correct values
            if (!waterheater.isGas()) {
                currentTotalkW += waterheater.prepowerUsedElectricity
                currentTotalkWh += waterheater.prepowerUsedElectricity * waterheater.usageHoursPre()
                postTotalkW += waterheater.postpowerUsedElectricity
                postTotalkWh += waterheater.postpowerUsedElectricity * waterheater.usageHoursPost()
            }
         }

        val waterheater = audit[waterheater]!!.first() as WaterHeater

        val costToSavings =
                if (totalSavings == 0.0) 0.0
                else totalCost / totalSavings
        val paybackMonth = (costToSavings * 12) + 4
        val paybackYear: Double = (costToSavings) + (4 / 12)
        val netPresentValue: Double = (totalSavings * presentvaluefactor) -  totalCost

        return WaterHeaterValues(
                totalSavings,
                totalCost,
                paybackMonth,
                paybackYear,
                netPresentValue,
                waterheater.quantity,
                waterheater.year(),
                waterheater.age,
                waterheater.thermaleff.toInt(),
                waterheater.fueltype,
                waterheater.unittype,
                waterheater.capacity,
                currentTotalkW,
                currentTotalkWh,
                postTotalkW,
                postTotalkWh)
    }

    private fun prepareValuesFromRefrigeration(audit: AuditComponents): RefrigerationValues? {
        if (!audit[refrigeration]!!.any()) {
            return null
        }

        val wiRefigerators = mutableListOf<WIRefrigerator>()
        val wiFreezers = mutableListOf<WIFreezer>()
        val refrigerators = mutableListOf<Refrigerator>()
        val freezers = mutableListOf<Freezer>()
        val wiCoolerBot = mutableListOf<WICoolerBox>()
        val presentvaluefactor = 10.380

        var currentTotalkW = 0.0
        var currentTotalkWh = 0.0
        var postTotalkW = 0.0
        var postTotalkWh = 0.0

        for (type in audit[refrigeration]!!) {
            when (type) {
                is WIRefrigerator -> wiRefigerators.add(type)
                is WIFreezer -> wiFreezers.add(type)
                is Refrigerator -> refrigerators.add(type)
                is Freezer -> freezers.add(type)
                is WICoolerBox -> wiCoolerBot.add(type)
            }
        }

        var totalCost = 0.0
        var totalSavings = 0.0

        for (fridge in wiRefigerators) {
            totalCost += fridge.installCost
            totalSavings += fridge.grosskwhSavings

            // @anthony, please verify that these are the correct values
            if (!fridge.isGas()) {
                currentTotalkW += fridge.kW
                currentTotalkWh += fridge.kW * fridge.usageHoursPre()
                postTotalkW += fridge.kW
                postTotalkWh += fridge.kW * fridge.usageHoursPost()
            }
        }

        for (fridge in wiFreezers) {
            totalCost += fridge.installCost
            totalSavings += fridge.grosskwhSavings

            // @anthony, please verify that these are the correct values
            if (!fridge.isGas()) {
                currentTotalkW += fridge.kW
                currentTotalkWh += fridge.kW * fridge.usageHoursPre()
                postTotalkW += fridge.kW
                postTotalkWh += fridge.kW * fridge.usageHoursPost()
            }
        }

        for (fridge in refrigerators) {
            totalCost += fridge.installCost()
            totalSavings += fridge.grosskwhSavings()

            // @anthony, please verify that these are the correct values
            if (!fridge.isGas()) {
                // TODO: add kw to refrigerator
//                currentTotalkW += fridge.kW
//                currentTotalkWh += fridge.kW * fridge.usageHoursPre()
//                postTotalkW += fridge.kW
//                postTotalkWh += fridge.kW * fridge.usageHoursPost()
            }
        }

        for (fridge in freezers) {
            totalCost += fridge.installCost()
            totalSavings += fridge.grosskwhSavings()

            // @anthony, please verify that these are the correct values
            if (!fridge.isGas()) {
                // TODO: add kw to freezer
//                currentTotalkW += fridge.kW
//                currentTotalkWh += fridge.kW * fridge.usageHoursPre()
//                postTotalkW += fridge.kW
//                postTotalkWh += fridge.kW * fridge.usageHoursPost()
            }
        }

        for (fridge in wiCoolerBot) {
            totalCost += fridge.installCost()
            totalSavings += fridge.grosskwhSavings()

            // @anthony, please verify that these are the correct values
            if (!fridge.isGas()) {
                currentTotalkW += fridge.kW
                currentTotalkWh += fridge.kW * fridge.usageHoursPre()
                postTotalkW += fridge.kW
                postTotalkWh += fridge.kW * fridge.usageHoursPost()
            }
        }

        val paybackMonth =
                if (totalSavings == 0.0) 0.0
                else totalCost / totalSavings * 12
        val netPresentValue: Double = (totalSavings * presentvaluefactor) -  totalCost
        return RefrigerationValues(
                totalCost,
                totalSavings,
                paybackMonth,
                netPresentValue,
                currentTotalkW,
                currentTotalkWh,
                postTotalkW,
                postTotalkWh
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
        val lowPressureSodiums = mutableListOf<LPSodium>()

        for (type in audit[lighting]!!) {
            type.costPreState(listOf(JsonNull.INSTANCE))
            when (type) {
                is Cfl -> cfls.add(type)
                is Halogen -> halogens.add(type)
                is HPSodium -> highPressureSodiums.add(type)
                is Incandescent -> incandescents.add(type)
                is LinearFluorescent -> linearFluorescents.add(type)
                is LPSodium -> lowPressureSodiums.add(type)
            }
        }

        var totalSavings = 0.0
        var totalInstallCost = 0.0
        var totalEnergySavings = 0.0
        val presentvaluefactor = 7.722 //Assume 10 years
        val lightingRows = prepareLightingTableRows(audit[lighting]!!)

        var currentTotalkW = 0.0
        var currentTotalkWh = 0.0
        var postTotalkW = 0.0
        var postTotalkWh = 0.0

        for (light in cfls) {
            totalSavings += light.totalSavings()
            totalInstallCost += light.installCost()
            totalEnergySavings += light.totalEnergySavings()

            // @anthony, please verify that these are the correct values
            currentTotalkW += light.prePower()
            currentTotalkWh += light.preEnergy()
            postTotalkW += light.postPower()
            postTotalkWh += light.postEnergy()
        }

        for (light in halogens) {
            totalSavings += light.totalSavings()
            totalInstallCost += light.installCost()
            totalEnergySavings += light.totalEnergySavings()

            // @anthony, please verify that these are the correct values
            currentTotalkW += light.prePower()
            currentTotalkWh += light.preEnergy()
            postTotalkW += light.postPower()
            postTotalkWh += light.postEnergy()
        }

        for (light in highPressureSodiums) {
            totalSavings += light.totalSavings()
            totalInstallCost += light.installCost()
            totalEnergySavings += light.totalEnergySavings()

            // @anthony, please verify that these are the correct values
            currentTotalkW += light.prePower()
            currentTotalkWh += light.preEnergy()
            postTotalkW += light.postPower()
            postTotalkWh += light.postEnergy()
        }

        for (light in incandescents) {
            totalSavings += light.totalSavings()
            totalInstallCost += light.installCost()
            totalEnergySavings += light.totalEnergySavings()

            // @anthony, please verify that these are the correct values
            currentTotalkW += light.prePower()
            currentTotalkWh += light.preEnergy()
            postTotalkW += light.postPower()
            postTotalkWh += light.postEnergy()
        }

        for (light in linearFluorescents) {
            totalSavings += light.totalSavings()
            totalInstallCost += light.installCost()
            totalEnergySavings += light.totalEnergySavings()

            // @anthony, please verify that these are the correct values
            currentTotalkW += light.prePower()
            currentTotalkWh += light.preEnergy()
            postTotalkW += light.postPower()
            postTotalkWh += light.postEnergy()
        }

        for (light in lowPressureSodiums) {
            totalSavings += light.totalSavings()
            totalInstallCost += light.installCost()
            totalEnergySavings += light.totalEnergySavings()

            // @anthony, please verify that these are the correct values
            currentTotalkW += light.prePower()
            currentTotalkWh += light.preEnergy()
            postTotalkW += light.postPower()
            postTotalkWh += light.postEnergy()
        }

        if (lightingRows.any { it.isCeilingOver10Feet.toLowerCase(Locale.ROOT) == "yes" }) {
            totalInstallCost += 500
        }

        val paybackYear: Double =
                if (totalSavings == 0.0) 0.0
                else totalInstallCost / totalSavings
        val paybackMonth = paybackYear * 12
        val geminiPayback = paybackMonth + 4

        val netPresentValue: Double = (totalSavings * presentvaluefactor) -  totalInstallCost

        return LightingValues(
                totalSavings,
                totalEnergySavings,
                totalInstallCost,
                paybackMonth,
                geminiPayback,
                paybackYear,
                netPresentValue,
                lightingRows,
                currentTotalkW,
                currentTotalkWh,
                postTotalkW,
                postTotalkWh)
    }

    private fun prepareLightingTableRows(lights: List<EBase>): List<LightingDataRow> {
        val rows = mutableListOf<LightingDataRow>()

        var measure = 1
        for (light in lights) {
            when (light) {
                is Cfl -> {
                    val paybackYears =
                            if (light.totalSavings() == 0.0) 0.0
                            else light.installCost() / light.totalSavings()

                    rows.add(LightingDataRow(
                            "$measure",
                            light.computable.zoneName,
                            light.computable.auditScopeName,
                            light.lampsPerFixtures,
                            light.numberOfFixtures,
                            light.lampsPerFixtures.toDouble() * light.numberOfFixtures.toDouble(),
                            light.ceilingType,
                            light.replacementType,
                            light.isCeilingOver10Feet,
                            light.prePower(),
                            light.usageHoursPre(),
                            light.preEnergy(),
                            light.postPower(),
                            light.usageHoursPost(),
                            light.postEnergy(),
                            light.totalEnergySavings(),
                            light.totalSavings(),
                            light.installCost(),
                            paybackYears * 12,
                            paybackYears,
                            light.netPresentValue()
                    ))
                }
                is Halogen -> {
                    val paybackYears =
                            if (light.totalSavings() == 0.0) 0.0
                            else light.installCost() / light.totalSavings()

                    rows.add(LightingDataRow(
                            "$measure",
                            light.computable.zoneName,
                            light.computable.auditScopeName,
                             light.lampsPerFixtures,
                            light.numberOfFixtures,
                            light.lampsPerFixtures.toDouble() * light.numberOfFixtures.toDouble(),
                            light.ceilingType,
                            light.replacementType,
                            light.isCeilingOver10Feet,
                            light.prePower(),
                            light.usageHoursPre(),
                            light.preEnergy(),
                            light.postPower(),
                            light.usageHoursPost(),
                            light.postEnergy(),
                            light.totalEnergySavings(),
                            light.totalSavings(),
                            light.installCost(),
                            paybackYears * 12,
                            paybackYears,
                            light.netPresentValue()
                    ))
                }
                is HPSodium -> {
                    val paybackYears =
                            if (light.totalSavings() == 0.0) 0.0
                            else light.installCost() / light.totalSavings()

                    rows.add(LightingDataRow(
                             "$measure",
                            light.computable.zoneName,
                            light.computable.auditScopeName,
                             light.lampsPerFixtures,
                            light.numberOfFixtures,
                            light.lampsPerFixtures.toDouble() * light.numberOfFixtures.toDouble(),
                            light.ceilingType,
                            light.replacementType,
                            light.isCeilingOver10Feet,
                            light.prePower(),
                            light.usageHoursPre(),
                            light.preEnergy(),
                            light.postPower(),
                            light.usageHoursPost(),
                            light.postEnergy(),
                            light.totalEnergySavings(),
                            light.totalSavings(),
                            light.installCost(),
                            paybackYears * 12,
                            paybackYears,
                            light.netPresentValue()
                    ))
                }
                is Incandescent -> {
                    val paybackYears =
                            if (light.totalSavings() == 0.0) 0.0
                            else light.installCost() / light.totalSavings()

                    rows.add(LightingDataRow(
                            "$measure",
                            light.computable.zoneName,
                            light.computable.auditScopeName,
                             light.lampsPerFixtures,
                            light.numberOfFixtures,
                            light.lampsPerFixtures.toDouble() * light.numberOfFixtures.toDouble(),
                            light.ceilingType,
                            light.replacementType,
                            light.isCeilingOver10Feet,
                            light.prePower(),
                            light.usageHoursPre(),
                            light.preEnergy(),
                            light.postPower(),
                            light.usageHoursPost(),
                            light.postEnergy(),
                            light.totalEnergySavings(),
                            light.totalSavings(),
                            light.installCost(),
                            paybackYears * 12,
                            paybackYears,
                            light.netPresentValue()
                    ))
                }
                is LinearFluorescent -> {
                    val paybackYears =
                            if (light.totalSavings() == 0.0) 0.0
                            else light.installCost() / light.totalSavings()

                    rows.add(LightingDataRow(
                           "$measure",
                            light.computable.zoneName,
                            light.computable.auditScopeName,
                            light.lampsPerFixtures,
                            light.numberOfFixtures,
                            light.lampsPerFixtures.toDouble() * light.numberOfFixtures.toDouble(),
                            light.ceilingType,
                            light.replacementType,
                            light.isCeilingOver10Feet,
                            light.prePower(),
                            light.usageHoursPre(),
                            light.preEnergy(),
                            light.postPower(),
                            light.usageHoursPost(),
                            light.postEnergy(),
                            light.totalEnergySavings(),
                            light.totalSavings(),
                            light.installCost(),
                            paybackYears * 12,
                            paybackYears,
                            light.netPresentValue()
                    ))
                }
                is LPSodium -> {
                    val paybackYears =
                            if (light.totalSavings() == 0.0) 0.0
                            else light.installCost() / light.totalSavings()

                    rows.add(LightingDataRow(
                            "$measure",
                            light.computable.zoneName,
                            light.computable.auditScopeName,
                             light.lampsPerFixtures,
                            light.numberOfFixtures,
                            light.lampsPerFixtures.toDouble() * light.numberOfFixtures.toDouble(),
                            light.ceilingType,
                            light.replacementType,
                            light.isCeilingOver10Feet,
                            light.prePower(),
                            light.usageHoursPre(),
                            light.preEnergy(),
                            light.postPower(),
                            light.usageHoursPost(),
                            light.postEnergy(),
                            light.totalEnergySavings(),
                            light.totalSavings(),
                            light.installCost(),
                            paybackYears * 12,
                            paybackYears,
                            light.netPresentValue()
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
        var totalNetPresentValue = 0.0
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
            var netPresentValue = 0.0

            val single = combinationOven.first()
            val presentValueFactor = single.presentValueFactor

            for (item in combinationOven) {
                costElectricity += item.costPostState
                materialCost += item.materialCost()
                implementationCost += item.implementationCost()
                savings += item.costPreState(listOf()) - item.costPostState

                netPresentValue += (savings * presentValueFactor) - implementationCost

                // @anthony, please verify that these are the correct values
                if (!item.isGas()) {
//                    currentTotalkW +=
//                    currentTotalkWh +=
//                    postTotalkW +=
//                    postTotalkWh +=
                }
            }

            val costToSavings =
                    if (savings == 0.0) 0.0
                    else  implementationCost / savings

            instances.add(
                    EquipmentInstances(
                            "Combination Oven",
                            single.energyPowerChange(),
                            costElectricity,
                            single.age,
                            materialCost,
                            implementationCost,
                            savings,
                            costToSavings * 12,
                            netPresentValue
                    )
            )
        }

        // TODO: add convection oven when implemented
        if (convectionOven.any()) { }

        if (conveyorBroiler.any()) {
            var costElectricity = 0.0
            var materialCost = 0.0
            var implementationCost = 0.0
            var savings = 0.0
            var netPresentValue = 0.0

            val single = conveyorBroiler.first()
            val presentValueFactor = single.presentValueFactor

            for (broiler in conveyorBroiler) {
                costElectricity += broiler.costPostState
                materialCost += broiler.materialCost()
                implementationCost += broiler.implementationCost()
                savings += broiler.costPreState(listOf()) - broiler.costPostState

                netPresentValue += (savings * presentValueFactor) - implementationCost

                // @anthony, please verify that these are the correct values
                if (!broiler.isGas()) {
//                    currentTotalkW +=
//                    currentTotalkWh +=
//                    postTotalkW +=
//                    postTotalkWh +=
                }
            }

            val costToSavings =
                    if (savings == 0.0) 0.0
                    else  implementationCost / savings

            instances.add(
                    EquipmentInstances(
                            "Conveyor Broiler",
                            single.energyPowerChange(),
                            costElectricity,
                            single.age,
                            materialCost,
                            implementationCost,
                            savings,
                            costToSavings * 12,
                            netPresentValue
                    )
            )
        }

        // TODO: add conveyor oven when implemented
        if (conveyorOven.any()) { }

        if (dishwasher.any()) {
            var costElectricity = 0.0
            var materialCost = 0.0
            var implementationCost = 0.0
            var savings = 0.0

            var netPresentValue = 0.0

            val single = dishwasher.first()
            val presentValueFactor = single.presentValueFactor

            for (washer in dishwasher) {
                val postState = washer.buildPostState().blockingGet()
                val element = postState.getAsJsonArray("results")[0].asJsonObject.get("data")

                costElectricity += washer.costPostState(element, DataHolder())
                materialCost += washer.materialCost()
                implementationCost += washer.implementationCost()
                savings += washer.costPreState(listOf(null)) - washer.costPostState
                netPresentValue += (savings * presentValueFactor) - implementationCost

                // @anthony, please verify that these are the correct values
                if (!washer.isGas()) {
//                    currentTotalkW +=
//                    currentTotalkWh +=
//                    postTotalkW +=
//                    postTotalkWh +=
                }
            }

            val costToSavings =
                    if (savings == 0.0) 0.0
                    else  implementationCost / savings

            instances.add(
                    EquipmentInstances(
                            "Dishwasher",
                            single.energyPowerChange(),
                            costElectricity,
                            single.age,
                            materialCost,
                            implementationCost,
                            savings,
                            costToSavings * 12,
                            netPresentValue
                    )
            )
        }

        // TODO: add fryer when implemented
        if (fryer.any()) { }

        if (griddle.any()) {
            var costElectricity = 0.0
            var materialCost = 0.0
            var implementationCost = 0.0
            var savings = 0.0
            var netPresentValue = 0.0

            val single = griddle.first()
            val presentValueFactor = single.presentValueFactor

            for (item in griddle) {
                costElectricity += item.costPostState
                materialCost += item.materialCost()
                implementationCost += item.implementationCost()
                savings += item.costPreState(listOf(null)) - item.costPostState
                netPresentValue += (savings * presentValueFactor) - implementationCost

                // @anthony, please verify that these are the correct values
                if (!item.isGas()) {
//                    currentTotalkW +=
//                    currentTotalkWh +=
//                    postTotalkW +=
//                    postTotalkWh +=
                }
            }

            val costToSavings =
                    if (savings == 0.0) 0.0
                    else  implementationCost / savings

            instances.add(
                    EquipmentInstances(
                            "Griddle",
                            single.energyPowerChange(),
                            costElectricity,
                            single.age,
                            materialCost,
                            implementationCost,
                            savings,
                            costToSavings * 12,
                            netPresentValue
                    )
            )
        }

        if (hotFoodCabinet.any()) {
            var costElectricity = 0.0
            var materialCost = 0.0
            var implementationCost = 0.0
            var savings = 0.0
            var netPresentValue = 0.0

            val single = hotFoodCabinet.first()
            val presentValueFactor = single.presentValueFactor

            for (cabinet in hotFoodCabinet) {
                costElectricity += cabinet.costPostState
                materialCost += cabinet.materialCost()
                implementationCost += cabinet.implementationCost()
                savings += cabinet.costPreState(listOf(null)) - cabinet.costPostState
                netPresentValue += (savings * presentValueFactor) - implementationCost

                // @anthony, please verify that these are the correct values
//                        currentTotalkW +=
//                        currentTotalkWh +=
//                        postTotalkW +=
//                        postTotalkWh +=
            }

            val costToSavings =
                    if (savings == 0.0) 0.0
                    else  implementationCost / savings

            instances.add(
                    EquipmentInstances(
                            "Hot Food Cabinet",
                            single.energyPowerChange(),
                            costElectricity,
                            single.age,
                            materialCost,
                            implementationCost,
                            savings,
                            costToSavings * 12,
                            netPresentValue
                    )
            )
        }

        if (iceMaker.any()) {
            var costElectricity = 0.0
            var materialCost = 0.0
            var implementationCost = 0.0
            var savings = 0.0
            var netPresentValue = 0.0

            val single = iceMaker.first()
            val presentValueFactor = single.presentValueFactor

            for (item in iceMaker) {
                costElectricity += item.costPostState
                materialCost += item.materialCost()
                implementationCost += item.implementationCost()
                savings += item.costPreState(listOf()) - item.costPostState
                netPresentValue += (savings * presentValueFactor) - implementationCost

                // @anthony, please verify that these are the correct values
                if (!item.isGas()) {
//                    currentTotalkW +=
//                    currentTotalkWh +=
//                    postTotalkW +=
//                    postTotalkWh +=
                }
            }

            val costToSavings =
                    if (savings == 0.0) 0.0
                    else  implementationCost / savings

            instances.add(
                    EquipmentInstances(
                            "Ice Maker",
                            single.energyPowerChange(),
                            costElectricity,
                            single.age,
                            materialCost,
                            implementationCost,
                            savings,
                            costToSavings * 12,
                            netPresentValue
                    )
            )
        }

        if (preRinseSpray.any()) {
            var costElectricity = 0.0
            var materialCost = 0.0
            var implementationCost = 0.0
            var savings = 0.0
            var netPresentValue = 0.0

            val single = preRinseSpray.first()
            val presentValueFactor = single.presentValueFactor

            for (spray in preRinseSpray) {
                costElectricity += spray.costPostState
                materialCost += spray.materialCost()
                implementationCost += spray.implementationCost()
                savings += spray.costPreState(listOf(null)) - spray.costPostState
                netPresentValue += (savings * presentValueFactor) - implementationCost

                // @anthony, please verify that these are the correct values
                if (!spray.isGas()) {
                    currentTotalkW += spray.preElectricPower
                    currentTotalkWh += spray.preElectricPower * spray.usageHoursPre()
                    postTotalkW += spray.postElectricPower
                    postTotalkWh += spray.postElectricPower * spray.usageHoursPost()
                }
            }

            val costToSavings =
                    if (savings == 0.0) 0.0
                    else  implementationCost / savings

            instances.add(
                    EquipmentInstances(
                            "Pre Rinse Spray",
                            single.energyPowerChange(),
                            costElectricity,
                            single.age,
                            materialCost,
                            implementationCost,
                            savings,
                            costToSavings * 12,
                            netPresentValue
                    )
            )
        }

        // TODO add SteamCooker when implemented
        if (steamCooker.any()) { }

        for (instance in instances) {
            totalSavings += instance.costElectricity
            totalCost += instance.implementationCost
            totalNetPresentValue += instance.netPresentValue
        }

        return EquipmentValues(
                instances.toList(),
                totalSavings,
                totalCost,
                totalNetPresentValue,
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
            refrigeration: RefrigerationValues?,
            motors: MotorValues?): BuildingValues {

        fun calculatePaybackYear(cost: Double?, savings: Double?) =
                if (cost == null || savings == null || savings == 0.0) 0.0
                else cost / savings


        fun calculatePaybackMonth(cost: Double?, savings: Double?) =
                if (cost == null || savings == null || savings == 0.0) 0.0
                else cost / savings * 12

        val buildingTotalSavings = (lightings?.totalcostsavings ?: 0.0) +
                (equipments?.totalSavings ?: 0.0) +
                (hvacs?.totalSavings ?: 0.0) +
                (waterHeater?.totalSavings ?: 0.0) +
                (refrigeration?.totalSavings ?: 0.0) +
                (motors?.totalSavings ?: 0.0)

        val buildingTotalCost = (lightings?.totalCost ?: 0.0) +
                (hvacs?.totalCost ?: 0.0) +
                (equipments?.totalCost ?: 0.0) +
                (waterHeater?.totalCost ?: 0.0) +
                (refrigeration?.totalCost ?: 0.0) +
                (motors?.totalSavings ?: 0.0)

        val buildingPayback =
                if (buildingTotalSavings == 0.0) 0.0
                else buildingTotalCost / buildingTotalSavings

        val buildingPaybackMonth =
                if (buildingTotalSavings == 0.0) 0.0
                else buildingTotalCost / buildingTotalSavings * 12

        val buildingNetPresentValue =(lightings?.netPresentValue ?: 0.0) +
                        (hvacs?.netPresentValue ?: 0.0) +
                        (equipments?.totalNetPresentValue ?: 0.0) +
                        (waterHeater?.netPresentValue ?: 0.0) +
                        (refrigeration?.netPresentValue ?: 0.0) +
                        (motors?.netPresentValue ?: 0.0)

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

    private fun prepareMotorValues(audit: AuditComponents): MotorValues? {
        if (!audit[motors]!!.any()) {
            return null
        }

        @Suppress("UNCHECKED_CAST")
        val motors = audit[motors]!! as MutableList<Motors>

        // @anthony, please provide what this value should be
        val presentValueFactor = 1.0

        var totalSavings = 0.0
        var totalCost = 0.0

        for (motor in motors) {
            totalSavings += motor.costPreState(listOf()) - motor.costPostState(JsonArray(), DataHolder())
            totalCost += motor.implementationCost
        }

        val netPresentValue = (totalSavings * presentValueFactor) - totalCost

        return MotorValues(
                totalCost,
                totalSavings,
                netPresentValue)
    }
}
