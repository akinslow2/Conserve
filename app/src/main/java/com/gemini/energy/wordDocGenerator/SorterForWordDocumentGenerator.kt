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

        private const val other = "other"
    }

    fun prepareAllValues(values: MutableList<EBase>): List<PreparedForDocument> {
        val sortedAudits = sortEbasesIntoAudits(values)

        val returnAble = mutableListOf<PreparedForDocument>()

        for (audit in sortedAudits) {
            val preaudit = prepareValuesForPreAudit(audit.value)



            val zones = aggregateZoneNames(audit.value)
            val zoneString = concatenateZoneString(zones)

            if (preaudit == null || preaudit.businessname.isBlank()) {
                continue
            } else {

                val waterheater = WaterHeaterValues.tryCreate(audit.value)
                val motors = MotorValues.tryCreate(audit.value)
                val lighting = LightingValues.tryCreate(audit.value)
                val hvac = HvacValues.tryCreate(audit.value, lighting?.hvacAdditionalkWhSavings ?: 0.0)
                val equipment = EquipmentValues.tryCreate(audit.value)
                val refrigeration = RefrigerationValues.tryCreate(audit.value)
                val building = BuildingValues.create(preaudit, lighting, equipment, hvac, waterheater, refrigeration, motors)

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
                        motors,
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
}
