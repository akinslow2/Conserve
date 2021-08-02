package com.gemini.energy.wordDocGenerator.models

import com.gemini.energy.wordDocGenerator.PreAuditValues

data class BuildingValues(
        val buildingNetPresentValue: Double,
        val buildingTotalSavings: Double, // 19
        val buildingPayback: Double, // 33
        val buildingPaybackMonth: Double, // 41
        val buildingTotalCost: Double, // 34
        val hvacTotalCost: Double, // 35
        val hvacPaybackYear: Double, // 37
        val hvacPaybackMonth: Double, // 38
        val equipmentTotalCost: Double, // 36
        val equipmentPaybackYear: Double, // 39
        val equipmentPaybackMonth: Double, // 40
        val refrigerationTotalCost: Double,
        val refrigerationPaybackYear: Double,
        val refrigerationPaybackMonth: Double,
        val waterHeaterTotalCost: Double,
        val waterHeaterPaybackYear: Double,
        val waterHeaterPaybackMonth: Double,
        val currentTotalkW: Double,
        val currentTotalkWh: Double,
        val postTotalkW: Double,
        val postTotalkWh: Double,
        val currentElectricityCharge: Double,
        val postElectricityCharge: Double,
        val customerAnnualSavingsDuringPaybackPeriod: Double
) {
    companion object {
        fun create(preAudit: PreAuditValues,
                   lightings: LightingValues?,
                   equipments: EquipmentValues?,
                   hvacs: HvacValues?,
                   waterHeater: WaterHeaterValues?,
                   refrigeration: RefrigerationValues?,
                   motor: MotorValues?
        ): BuildingValues {

            val totalSavings = totalSavings(lightings, equipments, hvacs, waterHeater, refrigeration, motor)
            val totalCost = totalCost(lightings, equipments, hvacs, waterHeater, refrigeration, motor)
            val paybackYears = calculatePaybackYear(totalCost, totalSavings)
            val paybackMonths = calculatePaybackMonth(totalCost, totalSavings)

            val currentkW = currentTotalkW(lightings, equipments, hvacs, waterHeater, refrigeration, motor)
            val currentkWh = currentTotalkWh(lightings, equipments, hvacs, waterHeater, refrigeration, motor)
            val postkW = postTotalkW(lightings, equipments, hvacs, waterHeater, refrigeration, motor)
            val postkWh = postTotalkWh(lightings, equipments, hvacs, waterHeater, refrigeration, motor)

            val currentElectricityCharge = electrictyCharge(preAudit.electricstructure, currentkW, currentkWh)
            val postElectricityCharge = electrictyCharge(preAudit.electricstructure, postkW, postkWh)

            return BuildingValues(
                    totalNetPresentValue(lightings, equipments, hvacs, waterHeater, refrigeration, motor),
                    totalSavings,
                    paybackYears,
                    paybackMonths,
                    totalCost,
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
                    currentkW,
                    currentkWh,
                    postkW,
                    postkWh,
                    currentElectricityCharge,
                    postElectricityCharge,
                    calculateAnnualSavingsDuringPaybackPeriod(totalCost, totalSavings)
            )
        }

        private fun totalSavings(lightings: LightingValues?,
                                 equipments: EquipmentValues?,
                                 hvacs: HvacValues?,
                                 waterHeater: WaterHeaterValues?,
                                 refrigeration: RefrigerationValues?,
                                 motor: MotorValues?): Double =
                (lightings?.totalcostsavings ?: 0.0) +
                        (equipments?.totalSavings ?: 0.0) +
                        (hvacs?.totalSavings ?: 0.0) +
                        (waterHeater?.totalSavings ?: 0.0) +
                        (refrigeration?.totalSavings ?: 0.0) +
                        (motor?.totalSavings ?: 0.0)

        private fun totalCost(lightings: LightingValues?,
                              equipments: EquipmentValues?,
                              hvacs: HvacValues?,
                              waterHeater: WaterHeaterValues?,
                              refrigeration: RefrigerationValues?,
                              motor: MotorValues?) =
                (lightings?.totalCost ?: 0.0) +
                        (hvacs?.totalCost ?: 0.0) +
                        (equipments?.totalCost ?: 0.0) +
                        (waterHeater?.totalCost ?: 0.0) +
                        (refrigeration?.totalCost ?: 0.0) +
                        (motor?.totalCost ?: 0.0)

        private fun calculatePaybackYear(cost: Double?, savings: Double?) =
                if (cost != null && savings != null && savings > 0.0)
                    cost / savings
                else 0.0

        private fun calculatePaybackMonth(cost: Double?, savings: Double?) =
                if (cost != null && savings != null && savings > 0.0)
                    cost / savings * 12
                else 0.0

        private fun totalNetPresentValue(lightings: LightingValues?,
                                         equipments: EquipmentValues?,
                                         hvacs: HvacValues?,
                                         waterHeater: WaterHeaterValues?,
                                         refrigeration: RefrigerationValues?,
                                         motor: MotorValues?) =
                (lightings?.netPresentValue ?: 0.0) +
                        (hvacs?.netPresentValue ?: 0.0) +
                        (equipments?.netPresentValue ?: 0.0) +
                        (waterHeater?.netPresentValue ?: 0.0) +
                        (refrigeration?.netPresentValue ?: 0.0) +
                        (motor?.netPresentValue ?: 0.0)

        private fun currentTotalkW(lightings: LightingValues?,
                                   equipments: EquipmentValues?,
                                   hvacs: HvacValues?,
                                   waterHeater: WaterHeaterValues?,
                                   refrigeration: RefrigerationValues?,
                                   motor: MotorValues?) =
                (lightings?.currentTotalkW ?: 0.0) +
                        (hvacs?.currentTotalkW ?: 0.0) +
                        (equipments?.currentTotalkW ?: 0.0) +
                        (waterHeater?.currentTotalkW ?: 0.0) +
                        (refrigeration?.currentTotalkW ?: 0.0) +
                        (motor?.currentTotalkW ?: 0.0)

        private fun currentTotalkWh(lightings: LightingValues?,
                                    equipments: EquipmentValues?,
                                    hvacs: HvacValues?,
                                    waterHeater: WaterHeaterValues?,
                                    refrigeration: RefrigerationValues?,
                                    motor: MotorValues?) =
                (lightings?.currentTotalkWh ?: 0.0) +
                        (hvacs?.currentTotalkWh ?: 0.0) +
                        (equipments?.currentTotalkWh ?: 0.0) +
                        (waterHeater?.currentTotalkWh ?: 0.0) +
                        (refrigeration?.currentTotalkWh ?: 0.0) +
                        (motor?.currentTotalkWh ?: 0.0)


        private fun postTotalkW(lightings: LightingValues?,
                                equipments: EquipmentValues?,
                                hvacs: HvacValues?,
                                waterHeater: WaterHeaterValues?,
                                refrigeration: RefrigerationValues?,
                                motor: MotorValues?) =
                (lightings?.postTotalkW ?: 0.0) +
                        (hvacs?.postTotalkW ?: 0.0) +
                        (equipments?.postTotalkW ?: 0.0) +
                        (waterHeater?.postTotalkW ?: 0.0) +
                        (refrigeration?.postTotalkW ?: 0.0) +
                        (motor?.postTotalkW ?: 0.0)

        private fun postTotalkWh(lightings: LightingValues?,
                                 equipments: EquipmentValues?,
                                 hvacs: HvacValues?,
                                 waterHeater: WaterHeaterValues?,
                                 refrigeration: RefrigerationValues?,
                                 motor: MotorValues?) =
                (lightings?.postTotalkWh ?: 0.0) +
                        (hvacs?.postTotalkWh ?: 0.0) +
                        (equipments?.postTotalkWh ?: 0.0) +
                        (waterHeater?.postTotalkWh ?: 0.0) +
                        (refrigeration?.postTotalkWh ?: 0.0) +
                        (motor?.postTotalkWh ?: 0.0)

        private fun electrictyCharge(structure: String, kW: Double, kWh: Double): Double {
            return when (structure) {
                "F" -> calculateScheduleF(kW, kWh)
                "F TOD" -> calculateScheduleFTOD(kW, kWh)
                "H" -> calculateScheduleH(kW, kWh)
                else -> 0.0
            }
        }

        private fun calculateScheduleF(kW: Double, kWh: Double): Double {
            println("calculate schedule F")

            val totalDemand = calculateDemandCharegeForScheduleF(kW)
            val totalEnergy = calculateEnergyChargeForScheduleF(kWh)

            val totalCost = totalDemand + totalEnergy
            println("cost 1 is ${totalCost}")

            if (kW >= 100) {
                val otherPotentialCost = kWh * 0.1697
                println("cost 2 is ${otherPotentialCost}")
                if (totalCost > otherPotentialCost) {
                    return otherPotentialCost
                }
            }
            return totalCost
        }

        private fun calculateScheduleFTOD(kW: Double, kWh: Double): Double {
            println("calculate schedule F TOD")

            // calculating based on assumption that all hours are off peak
            val totalDemand = kW * 4.95
            println("totalDemand $totalDemand")
            val totalEnergy = kWh * 0.05
            println("totalEnergy $totalEnergy")
            val totalCost = totalDemand + totalEnergy
            println("totalCost $totalCost")
            return totalCost
        }

        private fun calculateScheduleH(kW: Double, kWh: Double): Double {
            println("calculate schedule H")

            // calculating based on assumption that all hours are off peak
            val totalDemand = kW * 5.20
            println("totalDemand $totalDemand")
            val totalEnergy = kWh * 0.0449
            println("totalEnergy $totalEnergy")
            val totalCost = totalDemand + totalEnergy
            println("totalCost $totalCost")
            return totalCost
        }

        private fun calculateEnergyChargeForScheduleF(kWh: Double): Double {
            val energyChargeFirst2500kWh = 0.1288
            val energyChargeNext7500kWh = 0.0918
            val energyChargeNext40000kWh = 0.0776
            val energyChargeOver50000kWh = 0.0660

            var totalEnergyCharge = 0.0
            if (kWh >= 2500.0) {
                totalEnergyCharge += 2500.0 * energyChargeFirst2500kWh
            } else {
                totalEnergyCharge += kWh * energyChargeFirst2500kWh
                return totalEnergyCharge
            }
            println("energy cost first 2500 $totalEnergyCharge")

            var remainingkWh = kWh - 2500.0
            if (remainingkWh >= 7500.0) {
                totalEnergyCharge += 7500.0 * energyChargeNext7500kWh
            } else {
                totalEnergyCharge += remainingkWh * energyChargeNext7500kWh
                return totalEnergyCharge
            }
            println("energy cost next 7500 $totalEnergyCharge")

            remainingkWh -= 7500.0
            if (remainingkWh >= 40000.0) {
                totalEnergyCharge += 40000.0 * energyChargeNext40000kWh
            } else {
                totalEnergyCharge += remainingkWh * energyChargeNext40000kWh
                return totalEnergyCharge
            }
            println("energy cost next 40000 $totalEnergyCharge")

            remainingkWh -= 40000.0
            totalEnergyCharge += remainingkWh * energyChargeOver50000kWh

            println("total energy is ${totalEnergyCharge}")
            return totalEnergyCharge
        }

        private fun calculateDemandCharegeForScheduleF(kW: Double): Double {
            val demandChargeOver15kW = 9.5

            // no cost 15 and under
            val demandKW = kW - 15.0

            return if (kW <= 15) 0.0
            else demandKW * demandChargeOver15kW
        }

        private fun calculateAnnualSavingsDuringPaybackPeriod(totalCost: Double, totalCostSavings: Double): Double {
            val utilityMonthlyCharge = totalCost / 12.0 / 12.0
            return totalCostSavings - (utilityMonthlyCharge * 12)
        }
    }
}