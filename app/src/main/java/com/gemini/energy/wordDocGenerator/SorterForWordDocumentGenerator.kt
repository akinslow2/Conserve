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
import com.gemini.energy.wordDocGenerator.models.*
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import java.util.*

class SorterForWordDocumentGenerator {
    // use these values for sorting types of equipment that need to be aggregated for the report
    companion object {
        const val hvac = "hvac"
        const val lighting = "lighting"
        const val waterheater = "waterheater"
        const val refrigeration = "refrigeration"
        const val motors = "motors"

        val combinationOven = EApplianceType.CombinationOven.value
        val convectionOven = EApplianceType.ConvectionOven.value
        val conveyorBroiler = EApplianceType.ConveyorBroiler.value
        val conveyorOven = EApplianceType.ConveyorOven.value
        val dishWasher = EApplianceType.DishWasher.value
        val fryer = EApplianceType.Fryer.value
        val griddle = EApplianceType.Griddle.value
        val hotFoodCabinet = EApplianceType.HotFoodCabinet.value
        val iceMaker = EApplianceType.IceMaker.value
        val preRinseSpray = EApplianceType.PreRinseSpray.value
        val rackOven = EApplianceType.RackOven.value
        val steamCooker = EApplianceType.SteamCooker.value

        const val other = "other"
    }

    fun prepareAllValues(values: MutableList<EBase>): List<PreparedForDocument> {
        val sortedAudits = sortEbasesIntoAudits(values)

        val returnAble = mutableListOf<PreparedForDocument>()

        for (audit in sortedAudits) {
            val hvac = HvacValues.tryCreate(audit.value)
            val waterheater = WaterHeaterValues.tryCreate(audit.value)
            val lighting = LightingValues.tryCreate(audit.value)
            val equipment = EquipmentValues.tryCreate(audit.value)
            val refrigeration = RefrigerationValues.tryCreate(audit.value)
            val motor = MotorValues.tryCreate(audit.value)
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
}
