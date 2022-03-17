package com.gemini.energy.service.device.refrigeration

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
import com.google.gson.JsonObject
import io.reactivex.Observable
import org.json.JSONObject

class Refrigerator(computable: Computable<*>, utilityRateGas: UtilityRate, utilityRateElectricity: UtilityRate,
                   usageHours: UsageHours, outgoingRows: OutgoingRows) :
        EBase(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows), IComputable {

    /** year equipment was installed **/
    var year = 0
    var doorType = ""
    var styleType = ""
    var totalVolume = 0.0
    var numberOfDoors = 0
    var compressorTons = 0.0

    /**
     * Entry Point
     * */
    override fun compute(): Observable<Computable<*>> {
        return super.compute(extra = ({}))
    }

    override fun setup() {
        try {
            year = featureData["Year installed"].toIntOr(0)
            doorType = featureData["Door Type"].toStringOr("")
            styleType = featureData["Style Type"].toStringOr("")
            totalVolume = featureData["Total Volume (cu.ft.)"].toDoubleOr(0.0)
            numberOfDoors = featureData["Number of Doors"].toIntOr(0)
            compressorTons = featureData["Condensor Compressor Size (HP)"].toDoubleOr(0.0)
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
        var total = 0.0
        //CA Energy Wise Foodservice Instant Rebates for ENERGY STAR: https://www.caenergywise.com/instant-rebates/

        // door type
        if (doorType == "Glass" || doorType == "Glass Chest") {
            if (totalVolume >= 30.0) {
                total += 100.0
            } else {
                total += 70.0
            }
        } else if (doorType == "Solid" || doorType == "Solid Chest") {
            if (totalVolume >= 50.0) {
                total += 100.0
            } else {
                total += 70.0
            }
        }

        return total
    }

    override fun materialCost(): Double {
        if (totalVolume >= 50.0) {
            return 7500.0
        } else if (totalVolume >= 30.0) {
            return 5000.0
        } else {
            return 2500.0
        }
    }

    override fun laborCost(): Double {
        return 1500.0
    }

    /**
     * Cost - Post State
     * */
    var costPostState = 0.0
    var replacementIncrementalcost = 0.0

    override fun costPostState(element: JsonElement, dataHolder: DataHolder): Double {
        val powerUsed = hourlyEnergyUsagePost(element)[0]
        val costElectricity: Double
        costElectricity = costElectricity(powerUsed, super.usageHoursBusiness, super.electricityRate)
        costPostState = costElectricity
        return costElectricity
    }

    fun installCost(): Double {
        val increCost = replacementIncrementalcost
        val totalCost = increCost * 4 //@AK2 fill
        return totalCost
    }

    fun grosskwhSavings(): Double {
//        TODO: sum of the gross energy savings pulled from the PARSE
        return 0.0
    }

    /**
     * PowerTimeChange >> Hourly Energy Use - Pre
     * */
    override fun hourlyEnergyUsagePre(): List<Double> {
        var hourlyEnergy = 0.0

        try {
            val dailyEnergyUsed = featureData["Daily Energy Used"].toDoubleOr(3.5)
            //set to default typical daily energy used of 3.5 kWh/day, using Beverage Air HR2-1S: https://www.energystar.gov/ia/products/prod_lists/commer_refrig_prod_list.pdf
            hourlyEnergy = dailyEnergyUsed / 24
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return listOf(hourlyEnergy)
    }

    /**
     * PowerTimeChange >> Hourly Energy Use - Post
     * */
    override fun hourlyEnergyUsagePost(element: JsonElement): List<Double> {
        var hourlyEnergy = 0.0

        try {
            //ToDo: Check how this is being impacted within the current code base !!
            val postDailyEnergyUsed = element.asJsonObject.get("daily_energy_use").asDouble
            hourlyEnergy = postDailyEnergyUsed / 24
        } catch (e: Exception) {
            e.printStackTrace()
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

    override fun preStateFields() = mutableListOf("Daily Energy Used (kWh)")
    override fun postStateFields() = mutableListOf("company", "model_number", "style_type",
            "total_volume", "daily_energy_use", "rebate", "pgne_measure_code", "purchase_price_per_unit", "vendor")

    override fun computedFields() = mutableListOf("__daily_operating_hours", "__weekly_operating_hours",
            "__yearly_operating_hours", "__electric_cost")
}
