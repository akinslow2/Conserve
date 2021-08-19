package com.gemini.energy.wordDocGenerator.models

import com.gemini.energy.service.DataHolder
import com.gemini.energy.service.device.Motors
import com.gemini.energy.wordDocGenerator.AuditComponents
import com.gemini.energy.wordDocGenerator.SorterForWordDocumentGenerator
import com.google.gson.JsonArray

data class MotorValues(
        val netPresentValue: Double,
        val totalSavings: Double,
        val totalCost: Double
) {
    companion object {
        fun tryCreate(audit: AuditComponents): MotorValues? {
            if (!audit[SorterForWordDocumentGenerator.motors]!!.any()) {
                return null
            }

            @Suppress("UNCHECKED_CAST")
            val motors = audit[SorterForWordDocumentGenerator.motors]!! as MutableList<Motors>

            // @anthony, please provide what this value should be
            val presentValueFactor = 1.0

            var totalSavings = 0.0
            var totalCost = 0.0

            var currentTotalkW = 0.0
            var currentTotalkWh = 0.0
            var postTotalkW = 0.0
            var postTotalkWh = 0.0

            for (motor in motors) {
                totalSavings += motor.costPreState(listOf()) - motor.costPostState(JsonArray(), DataHolder())
                totalCost += motor.implementationCost + motor.materialCost()

                // TODO: aggregate kw and kwh
            }

            val netPresentValue = (totalSavings * presentValueFactor) - totalCost

            return MotorValues(netPresentValue, totalSavings, totalCost)
        }
    }
}