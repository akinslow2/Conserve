package com.gemini.energy.wordDocGenerator.models

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
        val postTotalkWh: Double
) {
    companion object {
        fun create(lightings: LightingValues?,
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

            return BuildingValues(
                    totalNetPresentValue(lightings, equipments, hvacs, waterHeater, refrigeration, motor),
                    totalSavings(lightings, equipments, hvacs, waterHeater, refrigeration, motor),
                    paybackYears,
                    paybackMonths,
                    totalCost(lightings, equipments, hvacs, waterHeater, refrigeration, motor),
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
                    currentTotalkW(lightings, equipments, hvacs, waterHeater, refrigeration, motor),
                    currentTotalkWh(lightings, equipments, hvacs, waterHeater, refrigeration, motor),
                    postTotalkW(lightings, equipments, hvacs, waterHeater, refrigeration, motor),
                    postTotalkWh(lightings, equipments, hvacs, waterHeater, refrigeration, motor)
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

        // TODO: kw and kwh aggregtion for motors
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
    }
}