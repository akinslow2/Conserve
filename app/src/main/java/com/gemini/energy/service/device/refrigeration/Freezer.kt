package com.gemini.energy.service.device.refrigeration

import android.content.Context
import com.gemini.energy.R
import com.gemini.energy.domain.entity.Computable
import com.gemini.energy.presentation.form.FormMapper
import com.gemini.energy.service.DataHolder
import com.gemini.energy.service.IComputable
import com.gemini.energy.service.OutgoingRows
import com.gemini.energy.service.device.EBase
import com.gemini.energy.service.type.UsageHours
import com.gemini.energy.service.type.UtilityRate
import com.google.gson.JsonElement
import io.reactivex.Observable
import org.json.JSONObject
import timber.log.Timber

class Freezer(computable: Computable<*>, utilityRateGas: UtilityRate, utilityRateElectricity: UtilityRate,
              usageHours: UsageHours, outgoingRows: OutgoingRows, private val context: Context) :
        EBase(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows), IComputable {

    var age = 0
    var estimatedAge = ""
    var dailyEnergyUse = 0.0
    var totalVolume = 0.0
    /**
     * Entry Point
     * */
    override fun compute(): Observable<Computable<*>> {
        return super.compute(extra = ({ Timber.d(it) }))
    }

    override fun setup() {
        try {
            estimatedAge = featureData["Estimated Age"]!! as String
            dailyEnergyUse = featureData["Daily Energy Used"]!! as Double
            totalVolume = featureData["Total Volume (cu.ft.)"] as Double
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Cost - Pre State
     * */
    override fun costPreState(elements: List<JsonElement?>): Double {
        val powerUsed = hourlyEnergyUsagePre()[0]
        val costElectricity: Double
        costElectricity = costElectricity(powerUsed, super.usageHoursBusiness, super.electricityRate)
        return costElectricity
    }

    override fun incentives(): Double {
        return 0.0
    }

    override fun materialCost(): Double {
        return 2500.0
    }

    override fun laborCost(): Double {
        return 0.0
    }

     /**
     * Cost - Post State
     * */
    var costPostState = 0.0
    override fun costPostState(element: JsonElement, dataHolder: DataHolder): Double {
        val powerUsed = hourlyEnergyUsagePost(element)[0]
        val costElectricity: Double
        costElectricity = costElectricity(powerUsed, super.usageHoursBusiness, super.electricityRate)
        costPostState = costElectricity
        return costElectricity
    }

    /**
     * PowerTimeChange >> Hourly Energy Use - Pre
     * */
    override fun hourlyEnergyUsagePre(): List<Double> {
        var hourlyEnergy = dailyEnergyUse / 24
        return listOf(hourlyEnergy)
    }

    /**
     * PowerTimeChange >> Hourly Energy Use - Post
     * */
    override fun hourlyEnergyUsagePost(element: JsonElement): List<Double> {
        var hourlyEnergy = 0.0

        if (element.asJsonObject.has("daily_energy_use")) {
            val postDailyEnergyUsed = element.asJsonObject.get("daily_energy_use").asDouble
            hourlyEnergy = postDailyEnergyUsed / 24
        }

        return listOf(hourlyEnergy)
    }

    /**
     * PowerTimeChange >> Yearly Usage Hours - [Pre | Post]
     * Pre and Post are the same for Refrigerator - 24 hrs
     * */
    override fun usageHoursPre(): Double = usageHoursBusiness.yearly()
    override fun usageHoursPost(): Double = usageHoursBusiness.yearly()

    /**
     * PowerTimeChange >> Energy Efficiency Calculations
     * */
    override fun energyPowerChange(): Double {
        val prePower = hourlyEnergyUsagePre()[0]
        var postPower: Double
        var delta = 0.0

        computable.efficientAlternative?.let {
            postPower = hourlyEnergyUsagePost(it)[0]
            delta = usageHoursPre() * (prePower - postPower)
        }

        return delta
    }

    override fun energyTimeChange(): Double = 0.0
    override fun energyPowerTimeChange(): Double = 0.0

    /**
     * Energy Efficiency Lookup Query Definition
     * */
    override fun efficientLookup() = true
    override fun queryEfficientFilter() = JSONObject()
            .put("data.style_type", featureData["Product Type"])
            .put("data.total_volume", JSONObject()
                    .put("\$gte", totalVolume - 2)
                    .put("\$lte", totalVolume + 2))
            .toString()

    /**
     * State if the Equipment has a Post UsageHours Hours (Specific) ie. A separate set of
     * Weekly UsageHours Hours apart from the PreAudit
     * */
    override fun usageHoursSpecific() = false

    /**
     * Define all the fields here - These would be used to Generate the Outgoing Rows or perform the Energy Calculation
     * */
    override fun preAuditFields() = mutableListOf("Number of Vacation days")

    override fun featureDataFields() = mutableListOf(
            "Company",
            "Model Number",
            "Fridge Capacity",
            "Age",
            "Control",
            "Daily Energy Used",
            "Product Type",
            "Total Volume")

    override fun preStateFields() = mutableListOf("Daily Energy Used (kWh)")

    override fun postStateFields() = mutableListOf(
            "company",
            "model_number",
            "style_type",
            "total_volume",
            "daily_energy_use",
            "rebate",
            "pgne_measure_code",
            "purchase_price_per_unit",
            "vendor")

    override fun computedFields() = mutableListOf(
            "__daily_operating_hours",
            "__weekly_operating_hours",
            "__yearly_operating_hours",
            "__electric_cost")

    private fun getFormMapper() = FormMapper(context, R.raw.freezer)
    private fun getModel() = getFormMapper().decodeJSON()
    private fun getGFormElements() = getFormMapper().mapIdToElements(getModel())
}
