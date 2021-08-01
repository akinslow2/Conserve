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
        val paybackMonth: Double
) {
    companion object {

        fun createCombinationOven(combinationOven: List<CombinationOven>): EquipmentInstances {
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

            val costOverSavings =
                    if (savings == 0.0) 0.0
                    else implementationCost / savings

            return EquipmentInstances(
                    "Combination Oven",
                    single.energyPowerChange(),
                    costElectricity,
                    single.age,
                    materialCost,
                    implementationCost,
                    savings,
                    costOverSavings * 12
            )
        }

        fun createConveyorBroiler(conveyorBroiler: List<ConveyorBroiler>): EquipmentInstances {
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

            val costOverSavings =
                    if (savings == 0.0) 0.0
                    else implementationCost / savings

            return EquipmentInstances(
                    "Conveyor Broiler",
                    single.energyPowerChange(),
                    costElectricity,
                    single.age,
                    materialCost,
                    implementationCost,
                    savings,
                    costOverSavings * 12
            )
        }

        fun createDishwasher(dishwasher: List<DishWasher>): EquipmentInstances {
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

            val costOverSavings =
                    if (savings == 0.0) 0.0
                    else implementationCost / savings

            return EquipmentInstances(
                    "Dishwasher",
                    single.energyPowerChange(),
                    costElectricity,
                    single.age,
                    materialCost,
                    implementationCost,
                    savings,
                    costOverSavings * 12
            )
        }

        fun createGriddle(griddle: List<Griddle>): EquipmentInstances {
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

            val costOverSavings =
                    if (savings == 0.0) 0.0
                    else implementationCost / savings

            return EquipmentInstances(
                    "Griddle",
                    single.energyPowerChange(),
                    costElectricity,
                    single.age,
                    materialCost,
                    implementationCost,
                    savings,
                    costOverSavings * 12
            )
        }

        fun createHotFoodCabinet(hotFoodCabinet: List<HotFoodCabinet>): EquipmentInstances {
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

            val costOverSavings =
                    if (savings == 0.0) 0.0
                    else implementationCost / savings

            return EquipmentInstances(
                    "Hot Food Cabinet",
                    single.energyPowerChange(),
                    costElectricity,
                    single.age,
                    materialCost,
                    implementationCost,
                    savings,
                    costOverSavings * 12
            )
        }

        fun createIceMaker(iceMaker: List<IceMaker>): EquipmentInstances {
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

            val costOverSavings =
                    if (savings == 0.0) 0.0
                    else implementationCost / savings

            return EquipmentInstances(
                    "Ice Maker",
                    single.energyPowerChange(),
                    costElectricity,
                    single.age,
                    materialCost,
                    implementationCost,
                    savings,
                    costOverSavings * 12
            )
        }

        fun createPreRinseSpray(preRinseSpray: List<PreRinseSpray>): EquipmentInstances {
            val single = preRinseSpray.first()

            var costElectricity = 0.0
            var materialCost = 0.0
            var implementationCost = 0.0
            var savings = 0.0

            for (spray in preRinseSpray) {
                costElectricity += spray.costPostState
                materialCost += spray.materialCost()
                implementationCost += spray.implementationCost()
                savings += spray.costPreState(listOf(null)) - spray.costPostState
            }

            val costOverSavings =
                    if (savings == 0.0) 0.0
                    else implementationCost / savings

            return EquipmentInstances(
                    "Pre Rinse Spray",
                    single.energyPowerChange(),
                    costElectricity,
                    single.age,
                    materialCost,
                    implementationCost,
                    savings,
                    costOverSavings * 12
            )
        }
    }
}