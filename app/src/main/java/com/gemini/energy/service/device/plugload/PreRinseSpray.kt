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
import com.google.gson.JsonElement
import io.reactivex.Observable
import org.json.JSONObject
import timber.log.Timber

class PreRinseSpray(computable: Computable<*>, utilityRateGas: UtilityRate, utilityRateElectricity: UtilityRate,
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
    private var usageHours: UsageSimple? = null

    private var flowRate = 0.0
    private var annualHours = 0
    private var waterTemperature = 0
    private var efficiency = 0.0
    private var waterHeater = ""
    var age = 0.0

    override fun setup() {
        try {
            peakHours = featureData["Peak Hours"]!! as Double
            partPeakHours = featureData["Part Peak Hours"]!! as Double
            offPeakHours = featureData["Off Peak Hours"]!! as Double
            usageHours = UsageSimple(peakHours, partPeakHours, offPeakHours)

            flowRate = featureData["Flow Rate"]!! as Double
            waterTemperature = featureData["Water Temperature (oF)"]!! as Int
            efficiency = featureData["Efficiency"]!! as Double
            waterHeater = featureData["Water Heater"]!! as String
            age = (featureData["Age"]!! as Int).toDouble()
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
        costElectricity = costElectricity(powerUsedElectric, usageHours!!, electricityRate)

        val costGas: Double
        costGas = costGas(thermsUsedGas)

        val cost = if (isGas()) costGas else costElectricity
        return cost
    }

    override fun incentives(): Double {
        return 0.0
    }

    override fun materialCost(): Double {
        return 100.0
    }

    override fun laborCost(): Double {
        return 0.0
    }

    fun implementationCost(): Double {
        return (materialCost() + laborCost()) - incentives()
    }
    /**
     * Cost - Post State
     * */
    var costPostState = 0.0
    override fun costPostState(element: JsonElement, dataHolder: DataHolder): Double {
        val powerUsedElectric = hourlyEnergyUsagePost(element)[0]
        val thermsUsedGas = hourlyEnergyUsagePost(element)[1]

        val costElectricity: Double
        costElectricity = costElectricity(powerUsedElectric, usageHours!!, electricityRate)

        val costGas: Double
        costGas = costGas(thermsUsedGas)

        val cost = if (isGas()) costGas else costElectricity
        costPostState = cost
        return cost
    }

    /**
     * PowerTimeChange >> Hourly Energy Use - Pre
     * */
    override fun hourlyEnergyUsagePre(): List<Double> {
        var annualEnergyUseElectric = 0.0
        var annualEnergyUseGas = 0.0

        try {
            val alpha = (flowRate * 60 * annualHours * 8.34 * waterTemperature)
            annualEnergyUseElectric = alpha / (3412.14 * efficiency)
            annualEnergyUseGas = alpha / (99976.1 * efficiency)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return listOf(annualEnergyUseElectric, annualEnergyUseGas)
    }

    /**
     * PowerTimeChange >> Hourly Energy Use - Post
     * */
    override fun hourlyEnergyUsagePost(element: JsonElement): List<Double> {
        var annualEnergyUseElectric = 0.0
        var annualEnergyUseGas = 0.0

        val flowRatePost = element.asJsonObject.get("flow_rate").asDouble
        val annualHoursPost = usageHoursPost()
        val waterTemperaturePost = waterTemperature
        val efficiencyPost = efficiency

        try {
            val alpha = flowRatePost * 60 * annualHoursPost * 8.34 * waterTemperaturePost
            annualEnergyUseElectric = alpha / (3412.14 * efficiencyPost)
            annualEnergyUseGas = alpha / (99976.1 * efficiencyPost)

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return listOf(annualEnergyUseElectric, annualEnergyUseGas)
    }

    /**
     * PowerTimeChange >> Yearly Usage Hours - [Pre | Post]
     * Pre and Post are the same for Refrigerator - 24 hrs
     * */
    //ToDo - @Johnny Verify this
    override fun usageHoursPre(): Double = usageHours!!.yearly()
    override fun usageHoursPost(): Double = usageHours!!.yearly()

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
            .put("data.flow_rate", flowRate)
            .toString()

    override fun isGas() = waterHeater == "Gas"

    /**
     * State if the Equipment has a Post UsageHours Hours (Specific) ie. A separate set of
     * Weekly UsageHours Hours apart from the PreAudit
     * */
    override fun usageHoursSpecific() = false

    /**
     * Define all the fields here - These would be used to Generate the Outgoing Rows or perform the Energy Calculation
     * */
    override fun preAuditFields() = mutableListOf("")
    override fun featureDataFields() = getGFormElements().map { it.value.param!! }.toMutableList()

    override fun preStateFields() = mutableListOf("")
    override fun postStateFields() = mutableListOf("company_name","model_number","flow_rate","rebate",
            "pgne_measure_code","utility_company")

    override fun computedFields() = mutableListOf("__daily_operating_hours", "__weekly_operating_hours",
            "__yearly_operating_hours", "__electric_cost")

    private fun getFormMapper() = FormMapper(context, R.raw.pre_rinse_spray)
    private fun getModel() = getFormMapper().decodeJSON()
    private fun getGFormElements() = getFormMapper().mapIdToElements(getModel())

}
