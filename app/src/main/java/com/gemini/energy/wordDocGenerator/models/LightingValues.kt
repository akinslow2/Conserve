package com.gemini.energy.wordDocGenerator.models

import com.gemini.energy.service.device.EBase
import com.gemini.energy.service.device.lighting.*
import com.gemini.energy.wordDocGenerator.*
import com.google.gson.JsonNull
import java.util.*


/**
 * aggregated lighting values for word doc generation
 */
data class LightingValues(
        val totalcostsavings: Double, // 13
        val totalenergysavings: Double,
        val totalCost: Double, // 15
        val paybackMonth: Double, // 16
        val geminiPayback: Double, // 17
        val paybackYear: Double, // 18
        val netPresentValue: Double,
        val lightingRows: List<LightingDataRow>,
        val currentTotalkW: Double,
        val currentTotalkWh: Double,
        val postTotalkW: Double,
        val postTotalkWh: Double,
        val kWSavings: Double,
        val kWhSavings: Double,
        val hvacAdditionalkWhSavings: Double
) {
    companion object {

        private val presentvaluefactor = 7.722 //Assume 10 years


        /**
         * attempt creation of lighting values for word doc generation
         *
         * @param audit sorted audit components
         *
         * @return aggregated values for word doc if [audit] contains lighting instances
         */
        fun tryCreate(audit: AuditComponents): LightingValues? {
            if (!audit[SorterForWordDocumentGenerator.lighting]!!.any()) {
                return null
            }

            val lightingRows = prepareLightingTableRows(audit[SorterForWordDocumentGenerator.lighting]!!)

            val cfls = mutableListOf<Cfl>()
            val halogens = mutableListOf<Halogen>()
            val highPressureSodiums = mutableListOf<HPSodium>()
            val incandescents = mutableListOf<Incandescent>()
            val linearFluorescents = mutableListOf<LinearFluorescent>()
            val lowPressureSodiums = mutableListOf<LPSodium>()

            for (type in audit[SorterForWordDocumentGenerator.lighting]!!) {
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

            val netPresentValue: Double = (totalSavings * presentvaluefactor) - totalInstallCost

            val totalkWhSavings = currentTotalkWh - postTotalkWh
            val totalkWSavings = currentTotalkW - postTotalkW

            val additionalHvacSavings = totalkWhSavings * 3410.0 / 17.0 / 1000.0

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
                    postTotalkWh,
                    totalkWSavings,
                    totalkWhSavings,
                    additionalHvacSavings)
        }


        /**
         * prepare table lighting rows
         *
         * @param lights all lighting instances
         *
         * @return list of lighting rows
         */
        private fun prepareLightingTableRows(lights: List<EBase>): List<LightingDataRow> {
            val rows = mutableListOf<LightingDataRow>()

            var measure = 1
            for (light in lights) {
                when (light) {
                    is Cfl -> rows.add(LightingDataRow.createCflRow(light, measure))
                    is Halogen -> rows.add(LightingDataRow.createHalogenRow(light, measure))
                    is HPSodium -> rows.add(LightingDataRow.createHPSodiumRow(light, measure))
                    is Incandescent -> rows.add(LightingDataRow.createIncandescentRow(light, measure))
                    is LinearFluorescent -> rows.add(LightingDataRow.createLinearFluorescentRow(light, measure))
                    is LPSodium -> rows.add(LightingDataRow.createLPSodium(light, measure))
                }

                measure += 1
            }

            return rows.toList()
        }
    }
}