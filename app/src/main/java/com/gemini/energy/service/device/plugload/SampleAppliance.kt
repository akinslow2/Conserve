package com.gemini.energy.service.device.plugload

import com.gemini.energy.domain.entity.Computable
import com.gemini.energy.service.DataHolder
import com.gemini.energy.service.IComputable
import com.gemini.energy.service.OutgoingRows
import com.gemini.energy.service.device.EBase
import com.gemini.energy.service.type.UsageHours
import com.gemini.energy.service.type.UtilityRate
import com.gemini.energy.toDoubleOr
import com.gemini.energy.toIntOr
import com.gemini.energy.toStringOr
import com.google.gson.JsonElement
import io.reactivex.Observable

class SampleAppliance(computable: Computable<*>, utilityRateGas: UtilityRate, utilityRateElectricity: UtilityRate,
                      usageHours: UsageHours, outgoingRows: OutgoingRows) :
        EBase(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows), IComputable {

    /**
     * Entry Point
     * */
    override fun compute(): Observable<Computable<*>> {
        return super.compute(extra = ({}))
    }

    private var sectionOneParameterOne = 0
    private var sectionOneParameterTwo = 0.0
    private var sectionOneParameterThree = ""

    private var sectionTwoParameterOne = ""
    private var sectionTwoParameterTwo = ""

    override fun setup() {
        try {
            sectionOneParameterOne = featureData["Section One Parameter One"].toIntOr(0)
            sectionOneParameterTwo = featureData["Section One Parameter Two"].toDoubleOr(0.0)
            sectionOneParameterThree = featureData["Section One Parameter Three"].toStringOr("")
            sectionTwoParameterOne = featureData["Section Two Parameter One"].toStringOr("")
            sectionTwoParameterTwo = featureData["Section Two Parameter Two"].toStringOr("")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Cost - Pre State
     * */
    override fun costPreState(elements: List<JsonElement?>): Double {
        return 0.0
    }

    /**
     * Cost - Post State
     * */
    override fun costPostState(element: JsonElement, dataHolder: DataHolder): Double {
        return 0.0
    }

    /**
     * INCENTIVES | MATERIAL COST - If Applicable
     * */
    override fun incentives(): Double {
        return 200.00
    }

    override fun materialCost(): Double {
        return 3455.00
    }

    /**
     * PowerTimeChange >> Hourly Energy Use - Pre
     * */
    override fun hourlyEnergyUsagePre(): List<Double> = listOf(0.0, 0.0)

    /**
     * PowerTimeChange >> Hourly Energy Use - Post
     * */
    override fun hourlyEnergyUsagePost(element: JsonElement): List<Double> = listOf(0.0, 0.0)

    /**
     * PowerTimeChange >> Yearly Usage Hours - [Pre | Post]
     * */
    override fun usageHoursPre(): Double = usageHoursBusiness.yearly()
    override fun usageHoursPost(): Double = usageHoursBusiness.yearly()

    /**
     * PowerTimeChange >> Energy Efficiency Calculations
     * */
    override fun energyPowerChange(): Double = 0.0
    override fun energyTimeChange(): Double = 0.0
    override fun energyPowerTimeChange(): Double = 0.0

    /**
     * Energy Efficiency Lookup Query Definition
     * */
    override fun efficientLookup() = false
    override fun queryEfficientFilter() = ""

    /**
     * State if the Equipment has a Post UsageHours Hours (Specific) ie. A separate set of
     * Weekly UsageHours Hours apart from the PreAudit
     * */
    override fun usageHoursSpecific() = false

    override fun preStateFields() = mutableListOf("Section One Parameter One", "Section One Parameter Two",
            "Section One Parameter Three", "Section Two Parameter One", "Section Two Parameter Two")

    override fun postStateFields() = mutableListOf("")
    override fun computedFields() = mutableListOf("")
}

