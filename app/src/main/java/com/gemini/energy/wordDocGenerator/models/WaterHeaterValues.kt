package com.gemini.energy.wordDocGenerator.models

import com.gemini.energy.service.DataHolder
import com.gemini.energy.service.device.WaterHeater
import com.gemini.energy.wordDocGenerator.*
import com.google.gson.JsonElement
import com.google.gson.JsonObject

data class WaterHeaterValues(
        val totalSavings: Double, //1
        val totalCost: Double, //2
        val paybackMonth: Double, //3
        val paybackYear: Double, //4
        val netPresentValue: Double, //5
        val quantity: Int,
        val year: Int,
        val age: Int,
        val thermaleff: Int,
        val fueltype: String,
        val unittype: String,
        val capacity: Double,
        val currentTotalkW: Double,
        val currentTotalkWh: Double,
        val postTotalkW: Double,
        val postTotalkWh: Double
) {
    companion object {
        fun tryCreate(audit: AuditComponents): WaterHeaterValues? {
            if (!audit[SorterForWordDocumentGenerator.waterheater]!!.any()) {
                return null
            }

            @Suppress("UNCHECKED_CAST")
            val waterheaters = audit[SorterForWordDocumentGenerator.waterheater]!! as MutableList<WaterHeater>

            var costPostState = 0.0
            var totalCost = 0.0
            var totalSavings = 0.0
            val presentvaluefactor = 10.38

            var currentTotalkW = 0.0
            var currentTotalkWh = 0.0
            var postTotalkW = 0.0
            var postTotalkWh = 0.0

            for (waterheater in waterheaters) {
                val postState = waterheater.buildPostState().blockingGet()
                var element: JsonElement = JsonObject()
                if (postState.getAsJsonArray("results").size() > 0)
                    element = postState.getAsJsonArray("results")[0].asJsonObject.get("data")

                costPostState += waterheater.costPostState(element, DataHolder())

                totalSavings += waterheater.totalSavings()
                totalCost += waterheater.implementationCost()

                // @anthony, please verify that these are the correct values
                if (!waterheater.isGas()) {
                    currentTotalkW += waterheater.prepowerUsedElectricity
                    currentTotalkWh += waterheater.prepowerUsedElectricity * waterheater.usageHoursPre()
                    postTotalkW += waterheater.postpowerUsedElectricity
                    postTotalkWh += waterheater.postpowerUsed * waterheater.usageHoursPost()
                }
            }

            val waterheater = audit[SorterForWordDocumentGenerator.waterheater]!!.first() as WaterHeater

            val costOverSavings =
                    if (totalSavings == 0.0) 0.0
                    else totalCost / totalSavings
            val paybackMonth = (costOverSavings * 12.0) + 4.0
            val paybackYear = costOverSavings + (4.0 / 12.0)
            val netPresentValue = (totalSavings * presentvaluefactor) - totalCost

            return WaterHeaterValues(
                    totalSavings,
                    totalCost,
                    paybackMonth,
                    paybackYear,
                    netPresentValue,
                    waterheater.quantity,
                    waterheater.year(),
                    waterheater.age,
                    waterheater.thermaleff,
                    waterheater.fueltype,
                    waterheater.unittype,
                    waterheater.capacity,
                    currentTotalkW,
                    currentTotalkWh,
                    postTotalkW,
                    postTotalkWh)
        }
    }
}