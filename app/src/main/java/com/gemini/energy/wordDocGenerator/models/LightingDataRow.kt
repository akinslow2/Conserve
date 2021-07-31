package com.gemini.energy.wordDocGenerator.models

import com.gemini.energy.service.device.lighting.*

/**
 * aggregated values for lighting tables per lighting instance
 */
data class LightingDataRow(
        val measure: String,
        val location: String,
        val measureDescription: String,
        val quantity: Double,
        val currentPowerkW: Double,
        val usageHours: Double,
        val currentEnergykWh: Double,
        val postPowerkW: Double,
        val postUsageHours: Double,
        val postEnergykWh: Double,
        val energySavingskWh: Double,
        val costSavings: Double,
        val implementationCost: Double,
        val paybackPeriodMonths: Double,
        val paybackPeriodYears: Double,
        val netPresentValue: Double
) {
    companion object {
        /**
         * create row from [Cfl] instance
         *
         * @param light Cfl
         * @param measure measure
         */
        fun createCflRow(light: Cfl, measure: Int): LightingDataRow {
            val quantity = light.lampsPerFixtures.toDouble() * light.numberOfFixtures.toDouble()

            return LightingDataRow(
                    measure.toString(),
                    light.computable.zoneName,
                    light.computable.auditScopeName,
                    quantity,
                    light.prePower(),
                    light.usageHoursPre(),
                    light.preEnergy(),
                    light.postPower(),
                    light.usageHoursPost(),
                    light.postEnergy(),
                    light.totalEnergySavings(),
                    light.totalSavings(),
                    light.installCost(),
                    installOverSavings(light.installCost(), light.totalSavings()) * 12,
                    installOverSavings(light.installCost(), light.totalSavings()),
                    light.netPresentValue()
            )
        }


        /**
         * create row from [Halogen] instance
         *
         * @param light Halogen
         * @param measure measure
         */
        fun createHalogenRow(light: Halogen, measure: Int): LightingDataRow {
            val quantity = light.lampsPerFixtures.toDouble() * light.numberOfFixtures.toDouble()

            return LightingDataRow(
                    measure.toString(),
                    light.computable.zoneName,
                    light.computable.auditScopeName,
                    quantity,
                    light.prePower(),
                    light.usageHoursPre(),
                    light.preEnergy(),
                    light.postPower(),
                    light.usageHoursPost(),
                    light.postEnergy(),
                    light.totalEnergySavings(),
                    light.totalSavings(),
                    light.installCost(),
                    installOverSavings(light.installCost(), light.totalSavings()) * 12,
                    installOverSavings(light.installCost(), light.totalSavings()),
                    light.netPresentValue()
            )
        }


        /**
         * create row from [HPSodium] instance
         *
         * @param light HPSodium
         * @param measure measure
         */
        fun createHPSodiumRow(light: HPSodium, measure: Int): LightingDataRow {
            val quantity = light.lampsPerFixtures.toDouble() * light.numberOfFixtures.toDouble()

            return LightingDataRow(
                    measure.toString(),
                    light.computable.zoneName,
                    light.computable.auditScopeName,
                    quantity,
                    light.prePower(),
                    light.usageHoursPre(),
                    light.preEnergy(),
                    light.postPower(),
                    light.usageHoursPost(),
                    light.postEnergy(),
                    light.totalEnergySavings(),
                    light.totalSavings(),
                    light.installCost(),
                    installOverSavings(light.installCost(), light.totalSavings()) * 12,
                    installOverSavings(light.installCost(), light.totalSavings()),
                    light.netPresentValue()
            )
        }


        /**
         * create row from [Incandescent] instance
         *
         * @param light Incandescent
         * @param measure measure
         */
        fun createIncandescentRow(light: Incandescent, measure: Int): LightingDataRow {
            val quantity = light.lampsPerFixtures.toDouble() * light.numberOfFixtures.toDouble()

            return LightingDataRow(
                    measure.toString(),
                    light.computable.zoneName,
                    light.computable.auditScopeName,
                    quantity,
                    light.prePower(),
                    light.usageHoursPre(),
                    light.preEnergy(),
                    light.postPower(),
                    light.usageHoursPost(),
                    light.postEnergy(),
                    light.totalEnergySavings(),
                    light.totalSavings(),
                    light.installCost(),
                    installOverSavings(light.installCost(), light.totalSavings()) * 12,
                    installOverSavings(light.installCost(), light.totalSavings()),
                    light.netPresentValue()
            )
        }


        /**
         * create row from [LinearFluroescent] instance
         *
         * @param light LinearFluroescent
         * @param measure measure
         */
        fun createLinearFluorescentRow(light: LinearFluorescent, measure: Int): LightingDataRow {
            val quantity = light.lampsPerFixtures.toDouble() * light.numberOfFixtures.toDouble()

            return LightingDataRow(
                    measure.toString(),
                    light.computable.zoneName,
                    light.computable.auditScopeName,
                    quantity,
                    light.prePower(),
                    light.usageHoursPre(),
                    light.preEnergy(),
                    light.postPower(),
                    light.usageHoursPost(),
                    light.postEnergy(),
                    light.totalEnergySavings(),
                    light.totalSavings(),
                    light.installCost(),
                    installOverSavings(light.installCost(), light.totalSavings()) * 12,
                    installOverSavings(light.installCost(), light.totalSavings()),
                    light.netPresentValue()
            )
        }


        /**
         * create row from [LPSodium] instance
         *
         * @param light LPSodium
         * @param measure measure
         */
        fun createLPSodium(light: LPSodium, measure: Int): LightingDataRow {
            val quantity = light.lampsPerFixtures.toDouble() * light.numberOfFixtures.toDouble()

            return LightingDataRow(
                    measure.toString(),
                    light.computable.zoneName,
                    light.computable.auditScopeName,
                    quantity,
                    light.prePower(),
                    light.usageHoursPre(),
                    light.preEnergy(),
                    light.postPower(),
                    light.usageHoursPost(),
                    light.postEnergy(),
                    light.totalEnergySavings(),
                    light.totalSavings(),
                    light.installCost(),
                    installOverSavings(light.installCost(), light.totalSavings()) * 12,
                    installOverSavings(light.installCost(), light.totalSavings()),
                    light.netPresentValue()
            )
        }


        /**
         * safely divide [installCost] / [totalSavings]
         * will return 0 if total savings == 0 instead of NaN
         */
        private fun installOverSavings(installCost: Double, totalSavings: Double): Double {
            return if (totalSavings == 0.0) 0.0
            else installCost / totalSavings
        }
    }
}