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

class Thermostat (computable: Computable<*>, utilityRateGas: UtilityRate, utilityRateElectricity: UtilityRate,
    usageHours: UsageHours, outgoingRows: OutgoingRows, private val context: Context) :
    EBase(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows), IComputable {

        /**
         * Entry Point
         * */
        override fun compute(): Observable<Computable<*>> {
            return super.compute(extra = ({ Timber.d(it) }))
        }

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
            //K2-Database
            fun extractThermostatDeemed(elements: List<JsonElement?>): Double {
                elements.forEach {
                    it?.let {
                        if (it.asJsonObject.has("Total_kWh")) {
                            return it.asJsonObject.get("Total_kWh").asDouble
                        }
                    }
                }
                return 0.0
            }
            fun extractThermostatDeemedCost(elements: List<JsonElement?>): Double {
                elements.forEach {
                    it?.let {
                        if (it.asJsonObject.has("Total_Cost")) {
                            return it.asJsonObject.get("Total_Cost").asDouble
                        }
                    }
                }
                return 0.0
            }
        }
        var utilitycompany = ""
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
                utilitycompany = preAudit["Others Electric Utility Company"]!! as String
                heatingFuel = featureData["Heat Fuel"]!! as String
                coolingFuel = featureData["Cool Fuel"]!! as String

                peakHours = featureData["Peak Hours"]!! as Double
                offPeakHours = featureData["Off Peak Hours"]!! as Double
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /**
         * Cost - Pre State
         * */
        override fun costPreState(elements: List<JsonElement?>): Double = 0.0

        /**
         * Cost - Post State
         * */
        override fun costPostState(element: JsonElement, dataHolder: DataHolder): Double {

            //var presciptive_kW_savings = extractThermostatDeemedkW(elements)
           // var presciptive_kWh_savings = extractThermostatDeemedkWh(elements)
            //var implementationCost = extractThermostatDeemedCost(elements)

            val postRow = mutableMapOf<String, String>()
          //  postRow["__prescriptive_kWh_savings"] = presciptive_kWh_savings.toString()
            //postRow["__prescriptive_kW_savings"] = presciptive_kW_savings.toString()
            //postRow["__implementation_cost"] = implementationCost.toString()


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

//K2-Database
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
        override fun preAuditFields() = mutableListOf("")
        override fun featureDataFields() = getGFormElements().map { it.value.param!! }.toMutableList()

        override fun preStateFields() = mutableListOf("")
        override fun postStateFields() = mutableListOf("__prescriptive_kWh_savings", "__prescriptive_kW_savings",
                "__implementation_cost")

        override fun computedFields() = mutableListOf("")

        private fun getFormMapper() = FormMapper(context, R.raw.thermostat)
        private fun getModel() = getFormMapper().decodeJSON()
        private fun getGFormElements() = getFormMapper().mapIdToElements(getModel())

    }
