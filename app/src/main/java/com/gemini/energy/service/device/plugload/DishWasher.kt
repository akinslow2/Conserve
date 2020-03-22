package com.gemini.energy.service.device.plugload

import android.content.Context
import com.gemini.energy.R
import com.gemini.energy.domain.entity.Computable
import com.gemini.energy.presentation.form.FormMapper
import com.gemini.energy.service.DataHolder
import com.gemini.energy.service.IComputable
import com.gemini.energy.service.OutgoingRows
import com.gemini.energy.service.device.EBase
import com.gemini.energy.service.type.UsageHours
import com.gemini.energy.service.type.UsageSimple
import com.gemini.energy.service.type.UtilityRate
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.reactivex.Observable
import io.reactivex.Single
import org.json.JSONObject
import timber.log.Timber

class DishWasher(computable: Computable<*>, utilityRateGas: UtilityRate, utilityRateElectricity: UtilityRate,
                 usageHours: UsageHours, outgoingRows: OutgoingRows, private val context: Context) :
        EBase(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows), IComputable {

    /**
     * Entry Point
     * */
    override fun compute(): Observable<Computable<*>> {
        return super.compute(extra = ({ Timber.d(it) }))
    }

    /**
     * Usage Hours
     * */
    private var peakHours = 0.0
    private var partPeakHours = 0.0
    private var offPeakHours = 0.0
    private var usageHoursPre: UsageSimple? = null

    private var suggestedPeakHours = 0.0
    private var suggestedPartPeakHours = 0.0
    private var suggestedOffPeakHours = 0.0
    private var usageHoursPost: UsageSimple? = null

    private var waterConsumption = 0.0
    private var numberOfRacks = 0
    private var cyclesPerDay = 0
    private var daysUsed = 0
    private var waterTemperature = 0
    private var efficiency = 0.0
    private var idleEnergyRate = 0.0
    private var waterHeater = ""

    var age = 0

    override fun setup() {
        try {
            peakHours = featureData["Peak Hours"]!! as Double
            offPeakHours = featureData["Off Peak Hours"]!! as Double
            usageHoursPre = UsageSimple(peakHours, partPeakHours, offPeakHours)

            waterConsumption = featureData["Water Consumption"]!! as Double
            numberOfRacks = featureData["Number of Racks"]!! as Int
            cyclesPerDay = featureData["Cycles per Day"]!! as Int
            daysUsed = featureData["Days Used"]!! as Int
            waterTemperature = featureData["Water Temperature (oF)"]!! as Int
            efficiency = featureData["Efficiency"]!! as Double
            idleEnergyRate = featureData["Idle Energy Rate"]!! as Double
            age = featureData["Age"]!! as Int
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * Cost - Pre State
     * */
    override fun costPreState(elements: List<JsonElement?>): Double {
        val powerUsedElectric = hourlyEnergyUsagePre()[0]
        val thermsUsedGas = hourlyEnergyUsagePre()[1]

        val costElectricity: Double
        costElectricity = costElectricity(powerUsedElectric, usageHoursPre!!, electricityRate)

        val costGas: Double
        costGas = costGas(thermsUsedGas)

        Timber.d("---- THERMS USED :: $thermsUsedGas ----")
        Timber.d("---- COST GAS :: $costGas ----")

        return if (isGas()) costGas else costElectricity
    }


    /**
     * Cost - Post State
     * */
    var costPostState = 0.0

    override fun costPostState(element: JsonElement, dataHolder: DataHolder): Double {
        val powerUsedElectric = hourlyEnergyUsagePost(element)[0]
        val thermsUsedGas = hourlyEnergyUsagePost(element)[1]

        val costElectricity: Double
        costElectricity = costElectricity(powerUsedElectric, usageHoursPre!!, electricityRate)

        val costGas: Double
        costGas = costGas(thermsUsedGas)

        costPostState = if (isGas()) costGas else costElectricity

        return costPostState
    }

    /**
     * Manually Builds the Post State Response from the Suggested Alternative
     * */
    override fun buildPostState(): Single<JsonObject> {
        val element = JsonObject()
        val data = JsonObject()
        data.addProperty("water_consumption", waterConsumption)
        data.addProperty("idle_energy_rate", idleEnergyRate)

        element.add("data", data)
        element.addProperty("type", "dishwashers")

        val response = JsonArray()
        response.add(element)

        val wrapper = JsonObject()
        wrapper.add("results", response)

        return Single.just(wrapper)
    }

    /**
     * PowerTimeChange >> Hourly Energy Use - Pre
     * */
    override fun hourlyEnergyUsagePre(): List<Double> {
        var annualEnergyGas = 0.0
        var annualEnergyElectric = 0.0

        try {
            val annualHours = usageHoursPre()
            val alpha = (waterConsumption * numberOfRacks * cyclesPerDay * daysUsed * annualHours * 8.34 * waterTemperature)
            annualEnergyGas = ((alpha / efficiency) + (idleEnergyRate * 3412.14 * annualHours)) / 99976.1
            annualEnergyElectric = (alpha / (efficiency * 3412.14)) + (idleEnergyRate * annualHours)

        } catch (e: Exception) {
            e.printStackTrace()
        }
        val hoursInyear: Int = 8760

        var hourlyEnergyGas = annualEnergyGas / hoursInyear
        var hourlyEnergyElectric = annualEnergyElectric / hoursInyear

        return listOf(hourlyEnergyElectric, hourlyEnergyGas)
    }

    /**
     * PowerTimeChange >> Hourly Energy Use - Post
     * */
    override fun hourlyEnergyUsagePost(element: JsonElement): List<Double> {
        var annualEnergyGas = 0.0
        var annualEnergyElectric = 0.0

        try {
            val waterConsumptionPost = element.asJsonObject.get("water_consumption").asDouble
            val numberOfRacksPost = numberOfRacks
            val cyclesPerDayPost = cyclesPerDay
            val daysUsedPost = daysUsed
            val waterTemperaturePost = waterTemperature
            val efficiencyPost = efficiency
            val annualHoursPost = usageHoursPre()
            val idleEnergyRatePost = element.asJsonObject.get("idle_energy_rate").asDouble

            val alpha = (waterConsumptionPost * numberOfRacksPost * cyclesPerDayPost * daysUsedPost *
                    annualHoursPost * 8.34 * waterTemperaturePost)
            annualEnergyGas = ((alpha / efficiencyPost) + (idleEnergyRatePost * 3412.14 * annualHoursPost)) / 99976.1
            annualEnergyElectric = (alpha / (efficiencyPost * 3412.14)) + (idleEnergyRatePost * annualHoursPost)

        } catch (e: Exception) {
            e.printStackTrace()
        }
        val hoursInyear: Int = 8760
        var hourlyEnergyGas = annualEnergyGas / hoursInyear
        var hourlyEnergyElectric = annualEnergyElectric / hoursInyear

        return listOf(hourlyEnergyElectric, hourlyEnergyGas)
    }

    /**
     * PowerTimeChange >> Yearly Usage Hours - [Pre | Post]
     * Pre - TOU Section
     * Post - Suggested Time
     * */
    override fun usageHoursPre(): Double = usageHoursPre!!.yearly()

    override fun usageHoursPost(): Double = usageHoursPre!!.yearly()

    /**
     * PowerTimeChange >> Energy Efficiency Calculations
     * ToDo -- Change this into Energy - It is not power !!
     * */
    override fun energyPowerChange(): Double {
        val usageHours = UsageSimple(peakHours, partPeakHours, offPeakHours)
        //Need to give a more accurate determeination of hours
        val preEnergy = if (isGas()) hourlyEnergyUsagePre()[1] else hourlyEnergyUsagePre()[0]
        var postEnergy: Double
        var savings = 0.0

        computable.efficientAlternative?.let {
            postEnergy = if (isGas()) hourlyEnergyUsagePost(it)[1] else hourlyEnergyUsagePost(it)[0]
            savings = preEnergy - postEnergy
        }

        val delta = costElectricity(savings, usageHours, electricityRate)
        Timber.d("HVAC :: Delta -- $delta")

        return delta
    }

    override fun energyTimeChange(): Double = 0.0
    override fun energyPowerTimeChange(): Double = 0.0

    /**
     * Energy Efficiency Lookup Query Definition
     * */
    override fun efficientLookup() = true

    override fun queryEfficientFilter() = JSONObject()
            .put("data.idle_energy_rate", queryAdjustment(idleEnergyRate))
            .put("data.water_consumption", queryAdjustment(waterConsumption))
            .toString()

    override fun isGas() = waterHeater == "Gas"

    /**
     * Adjusting the Query Filter Range by 10%
     * ToDo - Move this to the Base Class
     * */
    private fun queryAdjustment(value: Double) = JSONObject()
            .put("\$gte", value * 0.9)
            .put("\$lte", value * 1.1)

    /**
     * State if the Equipment has a Post UsageHours Hours (Specific) ie. A separate set of
     * Weekly UsageHours Hours apart from the PreAudit
     * */
    override fun usageHoursSpecific() = false

    /**
     * Additional Costs
     * */
    override fun materialCost(): Double = 4500.0

    override fun laborCost(): Double = 0.0
    override fun incentives(): Double = 600.0

    /**
     * Define all the fields here - These would be used to Generate the Outgoing Rows or perform the Energy Calculation
     * */
    override fun preAuditFields() = mutableListOf("")

    override fun featureDataFields() = getGFormElements().map { it.value.param!! }.toMutableList()

    override fun preStateFields() = mutableListOf("")
    override fun postStateFields() = mutableListOf("company", "model_number", "type", "sanitation_method",
            "idle_energy_rate", "water_consumption", "rebate", "pgne_measure_code", "utility_company", "purchase_price_per_unit")

    override fun computedFields() = mutableListOf("__daily_operating_hours", "__weekly_operating_hours",
            "__yearly_operating_hours", "__electric_cost")

    private fun getFormMapper() = FormMapper(context, R.raw.dishwasher)
    private fun getModel() = getFormMapper().decodeJSON()
    private fun getGFormElements() = getFormMapper().mapIdToElements(getModel())

}
