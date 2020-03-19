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
import java.util.*

class Refrigerator(computable: Computable<*>, utilityRateGas: UtilityRate, utilityRateElectricity: UtilityRate,
                   usageHours: UsageHours, outgoingRows: OutgoingRows, private val context: Context) :
        EBase(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows), IComputable {

    /**
     * Entry Point
     * */
    override fun compute(): Observable<Computable<*>> {
        return super.compute(extra = ({ Timber.d(it) }))
    }

    companion object {
        fun extractDeemedfridgeReplacementkwh(element: JsonElement) =
                if (element.asJsonObject.has("annual_energy_savings"))
                    element.asJsonObject.get("annual_energy_savings").asDouble
                else 0.0

        fun extractDeemedfridgeReplacementkw(element: JsonElement) =
                if (element.asJsonObject.has("demand_savings"))
                    element.asJsonObject.get("demand_savings").asDouble
                else 0.0

        fun extractDeemedfridgeReplacementcost(element: JsonElement) =
                if (element.asJsonObject.has("incremental_cost"))
                    element.asJsonObject.get("incremental_cost").asDouble
                else 0.0
    }


    var age = 0
    var fridgeVolume = 0.0
    var doorType = ""
    var styleType = ""
    var dailyEnergyUsed = 0.0


    override fun setup() {
        try {
            age = featureData["Age"]!! as Int
            doorType = featureData["Door Type"]!! as String
            fridgeVolume = featureData["Total Volume"]!! as Double
            styleType = featureData["Style Type"]!! as String
            dailyEnergyUsed = featureData["Daily Energy Used"]!! as Double

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
        val grossDeemedReplacementkwh = extractDeemedfridgeReplacementkwh(element)
        val grossDeemedReplacementkw = extractDeemedfridgeReplacementkw(element)
        val replacementIncrementalcost = extractDeemedfridgeReplacementcost(element)

        val netDeemedReplacementkWh =
                (grossDeemedReplacementkwh * (1 + 0.121) * (1 + 1 - 1) * 0.503) +
                        (grossDeemedReplacementkwh * (1 + 0.149) * (1 + 1 - 1) * 0.496)

        val netDeemedReplacementkW =
                (grossDeemedReplacementkw * (1 + 0.113) * (1 + 1 - 1) * 0.979) +
                        (grossDeemedReplacementkw * (1 + 0.112) * (1 + 1 - 1) * 1.186)


        val postRow = mutableMapOf<String, String>()
        postRow["grossDeemedReplacementkwh"] = grossDeemedReplacementkwh.toString()
        postRow["grossDeemedReplacementkw"] = grossDeemedReplacementkw.toString()
        postRow["replacementIncrementalcost"] = replacementIncrementalcost.toString()
        postRow["netDeemedReplacementkWh"] = netDeemedReplacementkWh.toString()
        postRow["netDeemedReplacementkW"] = netDeemedReplacementkW.toString()

        dataHolder.header = postStateFields()
        dataHolder.computable = computable
        dataHolder.fileName = "${Date().time}_post_state.csv"
        dataHolder.rows?.add(postRow)


        val powerUsed = hourlyEnergyUsagePost(element)[0]
        val costElectricity: Double
        costElectricity = costElectricity(powerUsed, super.usageHoursBusiness, super.electricityRate)
        costPostState = costElectricity

        return costElectricity
    }


    // TODO: @k2interactive added this here, please correct
    fun installCost(): Double {
//        val increCost = extractDeemedfridgeReplacementcost(element)
//        val totalCost = increCost * 4 //@AK2 fill
//        return totalCost
        return 0.0
    }


    fun grosskwhSavings(): Double {
//        sum of the gross energy savings pulled from the PARSE
        return 0.0
    }

    /**
     * PowerTimeChange >> Hourly Energy Use - Pre
     * */
    override fun hourlyEnergyUsagePre(): List<Double> {
        val hourlyEnergy = dailyEnergyUsed / 24
        return listOf(hourlyEnergy)
    }

    /**
     * PowerTimeChange >> Hourly Energy Use - Post
     * */
    override fun hourlyEnergyUsagePost(element: JsonElement): List<Double> {
        var hourlyEnergy = 0.0

        if (element.asJsonObject.has("daily_energy_use")) {
//            //ToDo: Check how this is being impacted within the current code base !!
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

    // there is no database entry with style_type or total_volume
    override fun queryEfficientFilter() = JSONObject()
            .put("data.style_type", styleType)
            .put("data.total_volume", JSONObject()
                    .put("\$gte", fridgeVolume - 2)
                    .put("\$lte", fridgeVolume + 2))

            .toString()

    override fun queryReachIn(): String {
        return JSONObject()
                .put("type", "refrigeration_reachinfreezerrefrigerator")
                .put("data.reach_in_type", "Refrigerator")
                .put("data.door_type", doorType)
                .put("data.low_cu_ft", JSONObject()
                        .put("\$lte", fridgeVolume - 2))
                .put("data.high_cu_ft", JSONObject()
                        .put("\$gte", fridgeVolume - 2))
                .toString()
    }

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

    // TODO: @k2interactive please add the incremental cost, as well as
//  the gross and net kwh and kw to the postStateFields
    override fun postStateFields() = mutableListOf(
            "company",
            "model_number",
            "style_type",
            "total_volume",
            "daily_energy_use",
            "rebate",
            "pgne_measure_code",
            "purchase_price_per_unit",
            "vendor",
            "grossDeemedReplacementkwh",
            "grossDeemedReplacementkw",
            "replacementIncrementalcost",
            "netDeemedReplacementkWh",
            "netDeemedReplacementkW")

    override fun computedFields() = mutableListOf(
            "__daily_operating_hours",
            "__weekly_operating_hours",
            "__yearly_operating_hours",
            "__electric_cost")

    private fun getFormMapper() = FormMapper(context, R.raw.refrigerator)
    private fun getModel() = getFormMapper().decodeJSON()
    private fun getGFormElements() = getFormMapper().mapIdToElements(getModel())

}
