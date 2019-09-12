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

class Griddle(private val computable: Computable<*>, utilityRateGas: UtilityRate, utilityRateElectricity: UtilityRate,
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

    private var idleEnergy = 0
    private var preHeatEnergy = 0

    private var numberOfSides = ""
    private var surfaceArea = 0.0
    private var nominalWidth = 0.0
    private var productionCapacity = 0.0
    private var fuelType = ""
    private var age = 0.0

    override fun setup() {

        peakHours = featureData["Peak Hours"]!! as Double
        partPeakHours = featureData["Part Peak Hours"]!! as Double
        offPeakHours = featureData["Off Peak Hours"]!! as Double

        idleEnergy = featureData["Idle Energy"]!! as Int
        preHeatEnergy = featureData["Preheat Energy"]!! as Int

        numberOfSides = featureData["Number of Sides"]!! as String
        surfaceArea = featureData["Surface Area"]!! as Double
        nominalWidth = featureData["Nominal Width"]!! as Double
        productionCapacity = featureData["Production Capacity"]!! as Double

        fuelType = featureData["Fuel Type"]!! as String
        age = featureData["Age"]!! as Double

        usageHours = UsageSimple(peakHours, partPeakHours, offPeakHours)

    }

    /**
     * Cost - Pre State
     * */
    override fun costPreState(elements: List<JsonElement?>): Double {
        val powerUsed = hourlyEnergyUsagePre()[0]
        val cost: Double

        cost = if (isElectric())
            costElectricity(powerUsed, usageHours!!, electricityRate) else
            //ToDo : Covert this power into Therms !!
            costGas(powerUsed)

        return cost
    }

    override fun incentives(): Double {
        return 0.0
    }

    override fun materialCost(): Double {
        return 1000.0
    }
    override fun laborCost(): Double {
        return 0.0
    }
    //@K2 is this correct?
    override fun implementationCost(): Double {
        return (materialCost() + laborCost()) - incentives()
    }
    /**
     * Cost - Post State
     * */
    override fun costPostState(element: JsonElement, dataHolder: DataHolder): Double {
        val powerUsed = hourlyEnergyUsagePost(element)[0]
        val cost: Double

        cost = if (isElectric())
            costElectricity(powerUsed, usageHours!!, electricityRate) else
            costGas(powerUsed)

        return cost
    }

    /**
     * PowerTimeChange >> Hourly Energy Use - Pre
     * */
    override fun hourlyEnergyUsagePre(): List<Double> {
        var annualEnergy = 0.0

        try {
            val annualEnergyUsed = (idleEnergy + preHeatEnergy) / 1000.0
            annualEnergy = annualEnergyUsed
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return listOf(annualEnergy)
    }

    /**
     * PowerTimeChange >> Hourly Energy Use - Post
     * */
    override fun hourlyEnergyUsagePost(element: JsonElement): List<Double> {
        var annualEnergy = 0.0

        try {

            val idleEnergyGas = element.asJsonObject.get("idle_energy_gas").asDouble
            val idleEnergyElectric = element.asJsonObject.get("idle_energy_electric").asDouble

            val preHeatEnergyGas = element.asJsonObject.get("preheat_energy_gas").asDouble
            val preHeatEnergyElectric = element.asJsonObject.get("preheat_energy_electric").asDouble

            val idleEnergyPost= if (isGas()) {idleEnergyGas} else {idleEnergyElectric}
            val preHeatEnergyPost= if (isGas()) {preHeatEnergyGas} else {preHeatEnergyElectric}

            val postAnnualEnergyUsed = (idleEnergyPost + preHeatEnergyPost) / 1000.0
            annualEnergy = postAnnualEnergyUsed

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return listOf(annualEnergy)
    }

    /**
     * PowerTimeChange >> Yearly Usage Hours - [Pre | Post]
     * Pre and Post are the same for Refrigerator - 24 hrs
     * */
    //ToDo - @Johnny Verify this
    override fun usageHoursPre(): Double = usageHours!!.yearly()
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
            .put("data.single_or_double_sided", numberOfSides)
            .put("data.surface_area", surfaceArea)
            .put("data.nominal_width", nominalWidth)
            .put("data.preheat_energy_gas", if (isGas()) {preHeatEnergy} else {"-"})
            .put("data.preheat_energy_electric", if (isElectric()) {preHeatEnergy} else {"-"})
            .put("data.idle_energy_gas", if (isGas()) {idleEnergy} else {"-"})
            .put("data.idle_energy_electric", if (isGas()) {idleEnergy} else {"-"})
            .put("data.production_capacity", productionCapacity)
            .toString()

    override fun isGas() = fuelType == "Gas"
    private fun isElectric() = fuelType == "Electric"

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
    override fun postStateFields() = mutableListOf("company","model_number","single_or_double_sided",
            "fuel_type","nominal_width","surface_area","preheat_energy_gas",
            "preheat_energy_electric","idle_energy_gas","idle_energy_electric",
            "energy_efficiency","production_capacity","rebate","pgne_measure_code",
            "utility_company","purchase_price_per_unit")

    override fun computedFields() = mutableListOf("__daily_operating_hours", "__weekly_operating_hours",
            "__yearly_operating_hours", "__electric_cost")

    private fun getFormMapper() = FormMapper(context, R.raw.griddle)
    private fun getModel() = getFormMapper().decodeJSON()
    private fun getGFormElements() = getFormMapper().mapIdToElements(getModel())

}
