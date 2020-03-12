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
        // TODO: @k2interactive please make sure that this query is moved to the HVAC class
        private const val ThermostatDeemed = "thermostat_thermostatdeemed"

        /**
         * Fetches the Deemed Criteria at once
         * via the Parse API
         * */
        // Both total_kwh, kw, and total_cost can be found from the call to
        // dataExtractThermostat with the string from queryThermostatDeemed
        // that data should be sent to the costPostState function
        // TODO: @k2interactive please make sure that this query is moved to the HVAC class
        fun extractThermostatreplacementkWh(elements: List<JsonElement?>): Double {
            elements.forEach {
                it?.let {
                    if (it.asJsonObject.has("total_kwh")) {
                        return it.asJsonObject.get("total_kwh").asDouble
                    }
                }
            }
            return 0.0
        }
        // TODO: @k2interactive please make sure that this query is moved to the HVAC class
        // TODO: Test me
        fun extractThermostatreplacementkWh(element: JsonElement): Double {
            if (element.asJsonObject.has("total_kwh")) {
                return element.asJsonObject.get("total_kwh").asDouble
            }
            return 0.0
        }

        fun extractThermostatreplacementkW(elements: List<JsonElement?>): Double {
            elements.forEach {
                it?.let {
                    if (it.asJsonObject.has("kw")) {
                        return it.asJsonObject.get("kw").asDouble
                    }
                }
            }
            return 0.0
        }

        // TODO: @k2interactive please make sure that this query is moved to the HVAC class
        // TODO: Test me
        fun extractThermostatreplacementkW(element: JsonElement): Double {
            if (element.asJsonObject.has("kw")) {
                return element.asJsonObject.get("kw").asDouble
            }
            return 0.0
        }

        fun extractThermostatreplacementCost(elements: List<JsonElement?>): Double {
            elements.forEach {
                it?.let {
                    if (it.asJsonObject.has("total_cost")) {
                        return it.asJsonObject.get("total_cost").asDouble
                    }
                }
            }
            return 0.0
        }
        // TODO: @k2interactive please make sure that this query is moved to the HVAC class
        // TODO: Test me
        fun extractThermostatreplacementCost(element: JsonElement): Double {
            if (element.asJsonObject.has("total_cost")) {
                return element.asJsonObject.get("total_cost").asDouble
            }
            return 0.0
        }
    }
    // TODO: @k2interactive heatingFuel and coolingFuel must be in the HVAC class for the query
    //  let me know if these type of comments are unneccessary
    private var heatingFuel = ""
    private var coolingFuel = ""


    /**
     * Suggested Alternative
     * */


    private var peakHours = 0.0
    private var offPeakHours = 0.0

    override fun setup() {
        try {
            // TODO: @k2interactive heatingFuel and coolingFuel must to be in the HVAC class for the query
            //  I added them into the HVAC input parameters already
//            heatingFuel = featureData["Heat Fuel"]!! as String
//            coolingFuel = featureData["Cool Fuel"]!! as String

//            peakHours = featureData["Peak Hours"]!! as Double
//            offPeakHours = featureData["Off Peak Hours"]!! as Double
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
        // TODO: @k2interactive please move these query related items to HVAC
//@k2interactive please make these active so that the they also spit out the values in the CSV
        val presciptive_kW_savings = extractThermostatreplacementkW(element)
        val presciptive_kWh_savings = extractThermostatreplacementkWh(element)
        val implementationCost = extractThermostatreplacementCost(element)

        val postRow = mutableMapOf<String, String>()
        //@k2interactive
        postRow["__prescriptive_kWh_savings"] = presciptive_kWh_savings.toString()
        postRow["__prescriptive_kW_savings"] = presciptive_kW_savings.toString()
        postRow["__prescriptive_implementation_cost"] = implementationCost.toString()


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
    // TODO: @k2interactive please make sure that this query filter is moved to the HVAC class
    override fun queryThermostatDeemed(): String {
        return JSONObject()
                .put("type", ThermostatDeemed)
                .put("data.heating_fuel", heatingFuel)
                .put("data.cooling_fuel", coolingFuel)
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
    override fun preAuditFields() = mutableListOf("")

    override fun featureDataFields() = getGFormElements().map { it.value.param!! }.toMutableList()

    // TODO: @k2interactive please make sure that these two prestatefields and three poststatefields are moved to the HVAC
    //  thermostat should not be producing these values anymore.
    override fun preStateFields() = mutableListOf("heatingfuel", "coolingfuel")

    override fun postStateFields() = mutableListOf("__prescriptive_kWh_savings", "__prescriptive_kW_savings",
            "__prescriptive_implementation_cost")

    override fun computedFields() = mutableListOf("")

    private fun getFormMapper() = FormMapper(context, R.raw.thermostat)
    private fun getModel() = getFormMapper().decodeJSON()
    private fun getGFormElements() = getFormMapper().mapIdToElements(getModel())

}
