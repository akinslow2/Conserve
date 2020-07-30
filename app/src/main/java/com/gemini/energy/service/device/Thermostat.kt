package com.gemini.energy.service.device

import android.content.Context
import com.gemini.energy.R
import com.gemini.energy.domain.entity.Computable
import com.gemini.energy.presentation.form.FormMapper
import com.gemini.energy.service.DataHolder
import com.gemini.energy.service.IComputable
import com.gemini.energy.service.OutgoingRows
import com.gemini.energy.service.type.UsageHours
import com.gemini.energy.service.type.UtilityRate
import com.google.gson.JsonElement
import io.reactivex.Observable
import org.json.JSONObject
import timber.log.Timber
import java.util.*

class Thermostat(computable: Computable<*>, utilityRateGas: UtilityRate, utilityRateElectricity: UtilityRate,
                 usageHours: UsageHours, outgoingRows: OutgoingRows, private val context: Context) :
        EBase(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows), IComputable {

    companion object {
        /**
         * Conversion Factor from Horse Power to Kilo Watts
         * */
        private const val KW_CONVERSION = 0.746
        private const val ThermostatDeemed = "Thermostat_Deemed"


        /**
         * Fetches the Deemed Criteria at once
         * via the Parse API
         * */
        //Need to pull multiple at once if feasible otherwise it is a lot of code
        fun extractThermostatDeemedkWh(elements: List<JsonElement?>): Double {
            elements.forEach {
                it?.let {
                    if (it.asJsonObject.has("Total_kWh")) {
                        return it.asJsonObject.get("Total_kWh").asDouble

                    }
                }
            }
            return 0.0
        }

        // TODO: Test me
        fun extractThermostatDeemedkWh(element: JsonElement): Double {
            if (element.asJsonObject.has("Total_kWh")) {
                return element.asJsonObject.get("Total_kWh").asDouble
            }
            return 0.0
        }

        fun extractThermostatDeemedkW(elements: List<JsonElement?>): Double {
            elements.forEach {
                it?.let {
                    if (it.asJsonObject.has("kW")) {
                        return it.asJsonObject.get("kW").asDouble
                    }
                }

            }
            return 0.0

        }

        // TODO: Test me
        fun extractThermostatDeemedkW(element: JsonElement): Double {
            if (element.asJsonObject.has("kW")) {
                return element.asJsonObject.get("kW").asDouble
            }
            return 0.0
        }

        /**
         * Cost - Pre State
         * */
        override fun costPreState(elements: List<JsonElement?>): Double = 0.0

        /**
         * Cost - Post State
         * */
        override fun costPostState(element: JsonElement, dataHolder: DataHolder): Double {

            val implementationCost = 44

            val postRow = mutableMapOf<String, String>()

            postRow["__implementation_cost"] = implementationCost.toString()


            dataHolder.header = postStateFields()
            dataHolder.computable = computable
            dataHolder.fileName = "${Date().time}_post_state.csv"
            dataHolder.rows?.add(postRow)

            return -99.99


        }

        // TODO: Test me
        fun extractThermostatDeemedCost(element: JsonElement): Double {
            if (element.asJsonObject.has("Total_Cost")) {
                return element.asJsonObject.get("Total_Cost").asDouble
            }
            return 0.0
        }
    }

    var electricUtilityCompany = ""
    var gasUtilityCompany = ""
    private var heatingFuel = ""
    private var coolingFuel = ""
    private var efficiency = 0.0

    /**
     * Suggested Alternative
     * */
    private var peakHours = 0.0
    private var partPeakHours = 0.0
    private var offPeakHours = 0.0


    override fun setup() {
        try {
            electricUtilityCompany = preAudit["Others Electric Utility Company"]!! as String
            gasUtilityCompany = preAudit["Others Gas Utility Company"]!! as String
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    /**
     * Entry Point
     * */
    override fun compute(): Observable<Computable<*>> {
        return super.compute(extra = ({ Timber.d(it) }))
    }


    /**
     * Cost - Pre State
     * */
    override fun costPreState(elements: List<JsonElement?>): Double = 0.0

    /**
     * Cost - Post State
     * */
    override fun costPostState(element: JsonElement, dataHolder: DataHolder): Double {

        val presciptive_kW_savings = extractThermostatDeemedkW(element)
        val presciptive_kWh_savings = extractThermostatDeemedkWh(element)
        val implementationCost = extractThermostatDeemedCost(element)

        val postRow = mutableMapOf<String, String>()
        postRow["__prescriptive_kWh_savings"] = presciptive_kWh_savings.toString()
        postRow["__prescriptive_kW_savings"] = presciptive_kW_savings.toString()
        postRow["__implementation_cost"] = implementationCost.toString()

        dataHolder.header = postStateFields()
        dataHolder.computable = computable
        dataHolder.fileName = "${Date().time}_post_state.csv"
        dataHolder.rows?.add(postRow)

        return -99.99
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
    override fun usageHoursPre(): Double = usageHoursSpecific.yearly()
    override fun usageHoursPost(): Double = usageHoursSpecific.yearly()

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

    override fun queryThermostatDeemed() = JSONObject()
            .put("type", ThermostatDeemed)
            .put("data.Heating_Fuel", heatingFuel)
            .put("data.Cooling_Fuel", coolingFuel)
            .toString()

    /**
     * State if the Equipment has a Post UsageHours Hours (Specific) ie. A separate set of
     * Weekly UsageHours Hours apart from the PreAudit
     * */
    override fun usageHoursSpecific() = false

    /**
     * Define all the fields here - These would be used to Generate the Outgoing Rows or perform the Energy Calculation
     * */
    override fun preAuditFields() = mutableListOf<String>()
    override fun featureDataFields() = getGFormElements().map { it.value.param!! }.toMutableList()

    override fun preStateFields() = mutableListOf<String>()
    override fun postStateFields() = mutableListOf(
            "__prescriptive_kWh_savings",
            "__prescriptive_kW_savings",
            "__implementation_cost")

    override fun computedFields() = mutableListOf<String>()

    private fun getFormMapper() = FormMapper(context, R.raw.thermostat)
    private fun getModel() = getFormMapper().decodeJSON()
    private fun getGFormElements() = getFormMapper().mapIdToElements(getModel())

}
