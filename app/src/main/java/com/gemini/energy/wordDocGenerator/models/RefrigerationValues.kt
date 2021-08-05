package com.gemini.energy.wordDocGenerator.models

import com.gemini.energy.service.device.refrigeration.*
import com.gemini.energy.wordDocGenerator.*

data class RefrigerationValues(
        val totalCost: Double,
        val totalSavings: Double,
        val paybackMonth: Double,
        val netPresentValue: Double,
        val currentTotalkW: Double,
        val currentTotalkWh: Double,
        val postTotalkW: Double,
        val postTotalkWh: Double
) {
    companion object {
        fun tryCreate(audit: AuditComponents): RefrigerationValues? {
            if (!audit[SorterForWordDocumentGenerator.refrigeration]!!.any()) {
                return null
            }

            val wiRefigerators = mutableListOf<WIRefrigerator>()
            val wiFreezers = mutableListOf<WIFreezer>()
            val refrigerators = mutableListOf<Refrigerator>()
            val freezers = mutableListOf<Freezer>()
            val wiCoolerBot = mutableListOf<WICoolerBox>()
            val presentvaluefactor = 10.380

            var currentTotalkW = 0.0
            var currentTotalkWh = 0.0
            var postTotalkW = 0.0
            var postTotalkWh = 0.0

            for (type in audit[SorterForWordDocumentGenerator.refrigeration]!!) {
                when (type) {
                    is WIRefrigerator -> wiRefigerators.add(type)
                    is WIFreezer -> wiFreezers.add(type)
                    is Refrigerator -> refrigerators.add(type)
                    is Freezer -> freezers.add(type)
                    is WICoolerBox -> wiCoolerBot.add(type)
                }
            }

            var totalCost = 0.0
            var totalSavings = 0.0

            for (fridge in wiRefigerators) {
                totalCost += fridge.installCost
                totalSavings += fridge.grosskwhSavings

                // @anthony, please verify that these are the correct values
                if (!fridge.isGas()) {
                    currentTotalkW += fridge.kW
                    currentTotalkWh += fridge.kW * fridge.usageHoursPre()
                    postTotalkW += fridge.kW
                    postTotalkWh += fridge.kW * fridge.usageHoursPost()
                }
            }

            for (fridge in wiFreezers) {
                totalCost += fridge.installCost
                totalSavings += fridge.grosskwhSavings

                // @anthony, please verify that these are the correct values
                if (!fridge.isGas()) {
                    currentTotalkW += fridge.kW
                    currentTotalkWh += fridge.kW * fridge.usageHoursPre()
                    postTotalkW += fridge.kW
                    postTotalkWh += fridge.kW * fridge.usageHoursPost()
                }
            }

            for (fridge in refrigerators) {
                totalCost += fridge.installCost()
                totalSavings += fridge.grosskwhSavings()

                // @anthony, please verify that these are the correct values
                if (!fridge.isGas()) {
                    // TODO: add kw to refrigerator
//                currentTotalkW += fridge.kW
//                currentTotalkWh += fridge.kW * fridge.usageHoursPre()
//                postTotalkW += fridge.kW
//                postTotalkWh += fridge.kW * fridge.usageHoursPost()
                }
            }

            for (fridge in freezers) {
                totalCost += fridge.installCost()
                totalSavings += fridge.grosskwhSavings()

                // @anthony, please verify that these are the correct values
                if (!fridge.isGas()) {
                    // TODO: add kw to freezer
//                currentTotalkW += fridge.kW
//                currentTotalkWh += fridge.kW * fridge.usageHoursPre()
//                postTotalkW += fridge.kW
//                postTotalkWh += fridge.kW * fridge.usageHoursPost()
                }
            }

            for (fridge in wiCoolerBot) {
                totalCost += fridge.installCost()
                totalSavings += fridge.grosskwhSavings()

                // @anthony, please verify that these are the correct values
                if (!fridge.isGas()) {
                    currentTotalkW += fridge.kW
                    currentTotalkWh += fridge.kW * fridge.usageHoursPre()
                    postTotalkW += fridge.kW
                    postTotalkWh += fridge.kW * fridge.usageHoursPost()
                }
            }

            val paybackMonth =
                    if (totalSavings == 0.0) 0.0
                    else totalCost / totalSavings * 12
            val netPresentValue: Double = (totalSavings * presentvaluefactor) - totalCost
            return RefrigerationValues(
                    totalCost,
                    totalSavings,
                    paybackMonth,
                    netPresentValue,
                    currentTotalkW,
                    currentTotalkWh,
                    postTotalkW,
                    postTotalkWh
            )
        }
    }
}