package com.gemini.energy.wordDocGenerator.models

import com.gemini.energy.service.DataHolder
import com.gemini.energy.service.device.Hvac
import com.gemini.energy.wordDocGenerator.AuditComponents
import com.gemini.energy.wordDocGenerator.SorterForWordDocumentGenerator
import com.google.gson.JsonArray


data class HvacValues(
        val instances: List<HvacInstances>,

        val costPostState: Double, // 20
        val totalCost: Double,
        val materialCost: Double,
        val laborCost: Double,
        val incentives: Double,
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
            var materialCost = 0.0
            var laborCost = 0.0
            var incentives = 0.0

            var totalSavings = 0.0
            val presentvaluefactor = 12.462
            val instances = mutableListOf<HvacInstances>()

            var currentTotalkW = 0.0
            var currentTotalkWh = 0.0
            var postTotalkW = 0.0
            var postTotalkWh = 0.0

            for (hvac in hvacs) {
                val postState = hvac.buildPostState().blockingGet()
                val element = if (postState.getAsJsonArray("results").any())
                    postState.getAsJsonArray("results")[0].asJsonObject.get("data")
                else JsonArray()

                costPostState += hvac.costPostState(element, DataHolder())

                totalSavings += hvac.totalSavings()

                totalCost += hvac.implementationCost()
                materialCost += hvac.materialCost()
                laborCost += hvac.laborCost()
                incentives += hvac.incentives()

                // @anthony, please verify that these are the correct values
                currentTotalkW = hvac.energyPre()
                currentTotalkWh = hvac.energyPre() * hvac.usageHoursPre()
                postTotalkW = hvac.energyPost()
                postTotalkWh = hvac.energyPost() * hvac.usageHoursPost()

                instances.add(HvacInstances.createFromHvac(hvac))
            }

            val costToSavings =
                    if (totalSavings == 0.0) 0.0
                    else totalCost / totalSavings

            val paybackYear: Double = costToSavings + (4 / 12)
            val netPresentValue: Double = (totalSavings * presentvaluefactor) - totalCost

            return HvacValues(
                    instances = instances.toList(),
                    costPostState = costPostState,
                    totalCost = totalCost,
                    materialCost = materialCost,
                    laborCost = laborCost,
                    incentives = incentives,
                    totalSavings = totalSavings,
                    paybackYear = paybackYear,
                    netPresentValue = netPresentValue,
                    currentTotalkW = currentTotalkW,
                    currentTotalkWh = currentTotalkWh,
                    postTotalkW = postTotalkW,
                    postTotalkWh = postTotalkWh
            )
        }
    }
}