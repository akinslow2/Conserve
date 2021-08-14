package com.gemini.energy.wordDocGenerator.models

import com.gemini.energy.service.device.plugload.*
import com.gemini.energy.wordDocGenerator.AuditComponents
import com.gemini.energy.wordDocGenerator.SorterForWordDocumentGenerator

data class EquipmentValues(
        val instances: List<EquipmentInstances>,
        val totalSavings: Double, // 31
        val totalCost: Double,
        val netPresentValue: Double,
        val currentTotalkW: Double,
        val currentTotalkWh: Double,
        val postTotalkW: Double,
        val postTotalkWh: Double
) {
    companion object {
        fun tryCreate(audit: AuditComponents): EquipmentValues? {
            val instances = mutableListOf<EquipmentInstances>()

            @Suppress("UNCHECKED_CAST")
            val combinationOven = audit[SorterForWordDocumentGenerator.combinationOven]!! as List<CombinationOven>

            @Suppress("UNCHECKED_CAST")
            val convectionOven = audit[SorterForWordDocumentGenerator.convectionOven]!! as List<ConvectionOven>

            @Suppress("UNCHECKED_CAST")
            val conveyorOven = audit[SorterForWordDocumentGenerator.conveyorOven]!! as List<ConveyorOven>

            @Suppress("UNCHECKED_CAST")
            val fryer = audit[SorterForWordDocumentGenerator.fryer]!! as List<Fryer>

            @Suppress("UNCHECKED_CAST")
            val iceMaker = audit[SorterForWordDocumentGenerator.iceMaker]!! as List<IceMaker>

            @Suppress("UNCHECKED_CAST")
            val steamCooker = audit[SorterForWordDocumentGenerator.steamCooker]!! as List<SteamCooker>

            @Suppress("UNCHECKED_CAST")
            val griddle = audit[SorterForWordDocumentGenerator.griddle]!! as List<Griddle>

            @Suppress("UNCHECKED_CAST")
            val hotFoodCabinet = audit[SorterForWordDocumentGenerator.hotFoodCabinet]!! as List<HotFoodCabinet>

            @Suppress("UNCHECKED_CAST")
            val conveyorBroiler = audit[SorterForWordDocumentGenerator.conveyorBroiler]!! as List<ConveyorBroiler>

            @Suppress("UNCHECKED_CAST")
            val dishwasher = audit[SorterForWordDocumentGenerator.dishWasher]!! as List<DishWasher>

            @Suppress("UNCHECKED_CAST")
            val preRinseSpray = audit[SorterForWordDocumentGenerator.preRinseSpray]!! as List<PreRinseSpray>

            if (combinationOven.any()) {
                instances.add(EquipmentInstances.createCombinationOven(combinationOven))
            }

            // TODO: add convection oven when implemented
            if (convectionOven.any()) {
            }

            if (conveyorBroiler.any()) {
                instances.add(EquipmentInstances.createConveyorBroiler(conveyorBroiler))
            }

            // TODO: add conveyor oven when implemented
            if (conveyorOven.any()) {
            }

            if (dishwasher.any()) {
                instances.add(EquipmentInstances.createDishwasher(dishwasher))
            }

            // TODO: add fryer when implemented
            if (fryer.any()) {
            }

            if (griddle.any()) {
                instances.add(EquipmentInstances.createGriddle(griddle))
            }

            if (hotFoodCabinet.any()) {
                instances.add(EquipmentInstances.createHotFoodCabinet(hotFoodCabinet))
            }

            if (iceMaker.any()) {
                instances.add(EquipmentInstances.createIceMaker(iceMaker))
            }

            if (preRinseSpray.any()) {
                instances.add(EquipmentInstances.createPreRinseSpray(preRinseSpray))
            }

            // TODO add SteamCooker when implemented
            if (steamCooker.any()) {
            }

            var totalSavings = 0.0
            var totalCost = 0.0
            var netPresentValue = 0.0

            var currentTotalkW = 0.0
            var currentTotalkWh = 0.0
            var postTotalkW = 0.0
            var postTotalkWh = 0.0

            for (instance in instances) {
                totalSavings += instance.costElectricity
                totalCost += instance.implementationCost

                currentTotalkW += instance.currentTotalkW
                currentTotalkWh += instance.currentTotalkWh

                postTotalkW += instance.postTotalkW
                postTotalkWh += instance.postTotalkWh

                netPresentValue += instance.netPresentValue
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
    }
}