package com.gemini.energy.wordDocGenerator.models

import com.gemini.energy.service.DataHolder
import com.gemini.energy.service.device.Hvac
import com.gemini.energy.wordDocGenerator.AuditComponents
import com.gemini.energy.wordDocGenerator.SorterForWordDocumentGenerator

data class HvacValues(
        val instances: List<HvacInstances>,

        val costPostState: Double, // 20
        val totalCost: Double,
        val totalSavings: Double,
        val paybackYear: Double,
        val netPresentValue: Double,
        val currentTotalkW: Double,
        val currentTotalkWh: Double,
        val postTotalkW: Double,
        val postTotalkWh: Double
) {
    companion object {
        fun tryCreate(audit: AuditComponents): HvacValues? {
            if (!audit[SorterForWordDocumentGenerator.hvac]!!.any()) {
                return null
            }

            @Suppress("UNCHECKED_CAST")
            val hvacs = audit[SorterForWordDocumentGenerator.hvac]!! as MutableList<Hvac>

            var costPostState = 0.0
            var totalCost = 0.0
            var totalSavings = 0.0
            val presentvaluefactor = 12.462
            val instances = mutableListOf<HvacInstances>()

            var currentTotalkW = 0.0
            var currentTotalkWh = 0.0
            var postTotalkW = 0.0
            var postTotalkWh = 0.0

            for (hvac in hvacs) {
                val postState = hvac.buildPostState().blockingGet()
                val element = postState.getAsJsonArray("results")[0].asJsonObject.get("data")

                costPostState += hvac.costPostState(element, DataHolder())

                totalSavings += hvac.totalSavings()
                totalCost += hvac.implementationCost()

                // @anthony, please verify that these are the correct values
                currentTotalkW += hvac.energyPre()
                currentTotalkWh += hvac.energyPre() * hvac.usageHoursPre()
                postTotalkW += hvac.energyPost()
                postTotalkWh += hvac.energyPost() * hvac.usageHoursPost()

                instances.add(HvacInstances.createFromHvac(hvac))
            }
            val costOverSavings =
                    if (totalSavings == 0.0) 0.0
                    else totalCost / totalSavings
            val paybackYear: Double = costOverSavings + (4.0 / 12.0)
            val netPresentValue: Double = (totalSavings * presentvaluefactor) - totalCost

            return HvacValues(
                    instances.toList(),

                    costPostState,
                    totalCost,
                    totalSavings,
                    paybackYear,
                    netPresentValue,
                    currentTotalkW,
                    currentTotalkWh,
                    postTotalkW,
                    postTotalkWh
            )
        }
    }
}