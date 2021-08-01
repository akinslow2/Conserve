package com.gemini.energy.wordDocGenerator.models

import com.gemini.energy.service.device.plugload.*
import com.gemini.energy.wordDocGenerator.*


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
            var totalSavings = 0.0
            var totalCost = 0.0
            val netPresentValue = 0.0
            val instances = mutableListOf<EquipmentInstances>()

            var currentTotalkW = 0.0
            var currentTotalkWh = 0.0
            var postTotalkW = 0.0
            var postTotalkWh = 0.0

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
                for (item in combinationOven) {
                    if (!item.isGas()) {
//                    currentTotalkW +=
//                    currentTotalkWh +=
//                    postTotalkW +=
//                    postTotalkWh +=
                    }
                }

                instances.add(EquipmentInstances.createCombinationOven(combinationOven))
            }

            // TODO: add convection oven when implemented
            if (convectionOven.any()) { }

            if (conveyorBroiler.any()) {
                for (broiler in conveyorBroiler) {
                    // @anthony, please verify that these are the correct values
                    if (!broiler.isGas()) {
//                    currentTotalkW +=
//                    currentTotalkWh +=
//                    postTotalkW +=
//                    postTotalkWh +=
                    }
                }

                instances.add(EquipmentInstances.createConveyorBroiler(conveyorBroiler))
            }

            // TODO: add conveyor oven when implemented
            if (conveyorOven.any()) { }

            if (dishwasher.any()) {
                for (washer in dishwasher) {
                    // @anthony, please verify that these are the correct values
                    if (!washer.isGas()) {
//                    currentTotalkW +=
//                    currentTotalkWh +=
//                    postTotalkW +=
//                    postTotalkWh +=
                    }
                }

                instances.add(EquipmentInstances.createDishwasher(dishwasher))
            }

            // TODO: add fryer when implemented
            if (fryer.any()) { }

            if (griddle.any()) {
                for (item in griddle) {
                    // @anthony, please verify that these are the correct values
                    if (!item.isGas()) {
//                    currentTotalkW +=
//                    currentTotalkWh +=
//                    postTotalkW +=
//                    postTotalkWh +=
                    }
                }

                instances.add(EquipmentInstances.createGriddle(griddle))
            }

            if (hotFoodCabinet.any()) {
                for (cabinet in hotFoodCabinet) {
                    // @anthony, please verify that these are the correct values
//                        currentTotalkW +=
//                        currentTotalkWh +=
//                        postTotalkW +=
//                        postTotalkWh +=
                }

                instances.add(EquipmentInstances.createHotFoodCabinet(hotFoodCabinet))
            }

            if (iceMaker.any()) {
                for (item in iceMaker) {
                    // @anthony, please verify that these are the correct values
                    if (!item.isGas()) {
//                    currentTotalkW +=
//                    currentTotalkWh +=
//                    postTotalkW +=
//                    postTotalkWh +=
                    }
                }
                instances.add(EquipmentInstances.createIceMaker(iceMaker))
            }

            if (preRinseSpray.any()) {
                for (spray in preRinseSpray) {
                    // @anthony, please verify that these are the correct values
                    if (!spray.isGas()) {
                        currentTotalkW += spray.preElectricPower
                        currentTotalkWh += spray.preElectricPower * spray.usageHoursPre()
                        postTotalkW += spray.postElectricPower
                        postTotalkWh += spray.postElectricPower * spray.usageHoursPost()
                    }
                }

                instances.add(EquipmentInstances.createPreRinseSpray(preRinseSpray))
            }

            // TODO add SteamCooker when implemented
            if (steamCooker.any()) { }

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
    }
}