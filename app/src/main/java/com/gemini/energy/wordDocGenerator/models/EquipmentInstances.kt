package com.gemini.energy.wordDocGenerator.models

import com.gemini.energy.service.DataHolder
import com.gemini.energy.service.device.plugload.*

data class EquipmentInstances(
        val name: String, // 27
        val delta: Double, // 26
        val costElectricity: Double, // 28
        val age: Double, // 29
        val materialCost: Double, // 32
        val implementationCost: Double,
        val totalsavings: Double,
        val paybackMonth: Double,
        val netPresentValue: Double,
        val currentTotalkW: Double,
        val currentTotalkWh: Double,
        val postTotalkW: Double,
        val postTotalkWh: Double
) {
    companion object {

        /**
         * create [EquipmentInstances] from [combinationOven]
         */
        fun createCombinationOven(combinationOven: List<CombinationOven>): EquipmentInstances {
            var costElectricity = 0.0
            var materialCost = 0.0
            var implementationCost = 0.0
            var savings = 0.0

            var currentTotalkW = 0.0
            var currentTotalkWh = 0.0
            var postTotalkW = 0.0
            var postTotalkWh = 0.0

            val single = combinationOven.first()
            val presentValueFactor = single.presentValueFactor

            for (item in combinationOven) {
                costElectricity += item.costPostState
                materialCost += item.materialCost()
                implementationCost += item.implementationCost()
                savings += item.costPreState(listOf()) - item.costPostState

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

            val netPresentValue = (savings * presentValueFactor) - (implementationCost + materialCost)

            return EquipmentInstances(
                    "Combination Oven",
                    single.energyPowerChange(),
                    costElectricity,
                    single.getAge(single.year).toDouble(),
                    materialCost,
                    implementationCost,
                    savings,
                    costOverSavings * 12,
                    netPresentValue,
                    currentTotalkW,
                    currentTotalkWh,
                    postTotalkW,
                    postTotalkWh
            )
        }


        /**
         * create [EquipmentInstances] from [conveyorBroiler]
         */
        fun createConveyorBroiler(conveyorBroiler: List<ConveyorBroiler>): EquipmentInstances {
            var costElectricity = 0.0
            var materialCost = 0.0
            var implementationCost = 0.0
            var savings = 0.0

            var currentTotalkW = 0.0
            var currentTotalkWh = 0.0
            var postTotalkW = 0.0
            var postTotalkWh = 0.0

            val single = conveyorBroiler.first()
            val presentValueFactor = single.presentValueFactor

            for (broiler in conveyorBroiler) {
                costElectricity += broiler.costPostState
                materialCost += broiler.materialCost()
                implementationCost += broiler.implementationCost()
                savings += broiler.costPreState(listOf()) - broiler.costPostState

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
            val netPresentValue = (savings * presentValueFactor) - (implementationCost + materialCost)

            return EquipmentInstances(
                    "Conveyor Broiler",
                    single.energyPowerChange(),
                    costElectricity,
                    single.getAge(single.year).toDouble(),
                    materialCost,
                    implementationCost,
                    savings,
                    costOverSavings * 12,
                    netPresentValue,
                    currentTotalkW,
                    currentTotalkWh,
                    postTotalkW,
                    postTotalkWh
            )
        }


        /**
         * create [EquipmentInstances] from [dishwasher]
         */
        fun createDishwasher(dishwasher: List<DishWasher>): EquipmentInstances {
            var costElectricity = 0.0
            var materialCost = 0.0
            var implementationCost = 0.0
            var savings = 0.0

            var currentTotalkW = 0.0
            var currentTotalkWh = 0.0
            var postTotalkW = 0.0
            var postTotalkWh = 0.0

            val single = dishwasher.first()
            val presentValueFactor = single.presentValueFactor

            for (washer in dishwasher) {
                val postState = washer.buildPostState().blockingGet()
                val element = postState.getAsJsonArray("results")[0].asJsonObject.get("data")

                costElectricity += washer.costPostState(element, DataHolder())
                materialCost += washer.materialCost()
                implementationCost += washer.implementationCost()
                savings += washer.costPreState(listOf(null)) - washer.costPostState

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
            val netPresentValue = (savings * presentValueFactor) - (implementationCost + materialCost)

            return EquipmentInstances(
                    "Dishwasher",
                    single.energyPowerChange(),
                    costElectricity,
                    single.getAge(single.year).toDouble(),
                    materialCost,
                    implementationCost,
                    savings,
                    costOverSavings * 12,
                    netPresentValue,
                    currentTotalkW,
                    currentTotalkWh,
                    postTotalkW,
                    postTotalkWh
            )
        }


        /**
         * create [EquipmentInstances] from [griddle]
         */
        fun createGriddle(griddle: List<Griddle>): EquipmentInstances {
            var costElectricity = 0.0
            var materialCost = 0.0
            var implementationCost = 0.0
            var savings = 0.0

            var currentTotalkW = 0.0
            var currentTotalkWh = 0.0
            var postTotalkW = 0.0
            var postTotalkWh = 0.0

            val single = griddle.first()
            val presentValueFactor = single.presentValueFactor

            for (item in griddle) {
                costElectricity += item.costPostState
                materialCost += item.materialCost()
                implementationCost += item.implementationCost()
                savings += item.costPreState(listOf(null)) - item.costPostState

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
            val netPresentValue = (savings * presentValueFactor) - (implementationCost + materialCost)

            return EquipmentInstances(
                    "Griddle",
                    single.energyPowerChange(),
                    costElectricity,
                    single.getAge(single.year).toDouble(),
                    materialCost,
                    implementationCost,
                    savings,
                    costOverSavings * 12,
                    netPresentValue,
                    currentTotalkW,
                    currentTotalkWh,
                    postTotalkW,
                    postTotalkWh
            )
        }


        /**
         * create [EquipmentInstances] from [hotFoodCabinet]
         */
        fun createHotFoodCabinet(hotFoodCabinet: List<HotFoodCabinet>): EquipmentInstances {
            var costElectricity = 0.0
            var materialCost = 0.0
            var implementationCost = 0.0
            var savings = 0.0

            var currentTotalkW = 0.0
            var currentTotalkWh = 0.0
            var postTotalkW = 0.0
            var postTotalkWh = 0.0

            val single = hotFoodCabinet.first()
            val presentValueFactor = single.presentValueFactor

            for (cabinet in hotFoodCabinet) {
                costElectricity += cabinet.costPostState
                materialCost += cabinet.materialCost()
                implementationCost += cabinet.implementationCost()
                savings += cabinet.costPreState(listOf(null)) - cabinet.costPostState

                if (!cabinet.isGas()) {
//                    currentTotalkW +=
//                    currentTotalkWh +=
//                    postTotalkW +=
//                    postTotalkWh +=
                }
            }

            val costOverSavings =
                    if (savings == 0.0) 0.0
                    else implementationCost / savings
            val netPresentValue = (savings * presentValueFactor) - (implementationCost + materialCost)

            return EquipmentInstances(
                    "Hot Food Cabinet",
                    single.energyPowerChange(),
                    costElectricity,
                    single.getAge(single.year).toDouble(),
                    materialCost,
                    implementationCost,
                    savings,
                    costOverSavings * 12,
                    netPresentValue,
                    currentTotalkW,
                    currentTotalkWh,
                    postTotalkW,
                    postTotalkWh
            )
        }


        /**
         * create [EquipmentInstances] from [iceMaker]
         */
        fun createIceMaker(iceMaker: List<IceMaker>): EquipmentInstances {
            var costElectricity = 0.0
            var materialCost = 0.0
            var implementationCost = 0.0
            var savings = 0.0

            var currentTotalkW = 0.0
            var currentTotalkWh = 0.0
            var postTotalkW = 0.0
            var postTotalkWh = 0.0

            val single = iceMaker.first()
            val presentValueFactor = single.presentValueFactor

            for (item in iceMaker) {
                costElectricity += item.costPostState
                materialCost += item.materialCost()
                implementationCost += item.implementationCost()
                savings += item.costPreState(listOf()) - item.costPostState

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
            val netPresentValue = (savings * presentValueFactor) - (implementationCost + materialCost)

            return EquipmentInstances(
                    "Ice Maker",
                    single.energyPowerChange(),
                    costElectricity,
                    single.getAge(single.year).toDouble(),
                    materialCost,
                    implementationCost,
                    savings,
                    costOverSavings * 12,
                    netPresentValue,
                    currentTotalkW,
                    currentTotalkWh,
                    postTotalkW,
                    postTotalkWh
            )
        }


        /**
         * create [EquipmentInstances] from [preRinseSpray]
         */
        fun createPreRinseSpray(preRinseSpray: List<PreRinseSpray>): EquipmentInstances {
            val single = preRinseSpray.first()
            val presentValueFactor = single.presentValueFactor

            var costElectricity = 0.0
            var materialCost = 0.0
            var implementationCost = 0.0
            var savings = 0.0

            var currentTotalkW = 0.0
            var currentTotalkWh = 0.0
            var postTotalkW = 0.0
            var postTotalkWh = 0.0

            for (spray in preRinseSpray) {
                costElectricity += spray.costPostState
                materialCost += spray.materialCost()
                implementationCost += spray.implementationCost()
                savings += spray.costPreState(listOf(null)) - spray.costPostState

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
            val netPresentValue = (savings * presentValueFactor) - (implementationCost + materialCost)

            return EquipmentInstances(
                    "Pre Rinse Spray",
                    single.energyPowerChange(),
                    costElectricity,
                    single.getAge(single.year).toDouble(),
                    materialCost,
                    implementationCost,
                    savings,
                    costOverSavings * 12,
                    netPresentValue,
                    currentTotalkW,
                    currentTotalkWh,
                    postTotalkW,
                    postTotalkWh
            )
        }
    }
}