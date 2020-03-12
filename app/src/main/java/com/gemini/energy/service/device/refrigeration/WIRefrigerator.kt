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
import com.gemini.energy.service.type.UsageSimple
import com.gemini.energy.service.type.UtilityRate
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.reactivex.Observable
import io.reactivex.Single
import org.json.JSONObject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class WIRefrigerator(computable: Computable<*>, utilityRateGas: UtilityRate, utilityRateElectricity: UtilityRate,
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
         * Year At - Current minus the Age
         * */
        private val dateFormatter = SimpleDateFormat("yyyy", Locale.ENGLISH)

        fun getYear(age: Int): Int {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.YEAR, "-$age".toInt()) //** Subtracting the Age **
            return dateFormatter.format(calendar.time).toInt()
        }

        fun firstNotNull (valueFirst: Double, valueSecond: Double) =
                if (valueFirst == 0.0) valueSecond else valueFirst

        fun firstNotNull (valueFirst: Int, valueSecond: Int) =
                if (valueFirst == 0) valueSecond else valueFirst

// TODO: @k2interactive make the following queries active.

// This relates to queryCondensingUnit
        /**
        fun extractCondensingUnitcompressorkWh(element: JsonElement): Double {
        if (element.asJsonObject.has("cu_scroll_compressor_kWh")) {
        return element.asJsonObject.get("cu_scroll_compressor_kWh").asDouble
        }
        return 0.0
        }

        fun extractCondensingUnitcompressorkW(element: JsonElement): Double {
        if (element.asJsonObject.has("cu_scroll_compressor_kW")) {
        return element.asJsonObject.get("cu_scroll_compressor_kW").asDouble
        }
        return 0.0
        }

        fun extractCondensingUnitcondensorkWh(element: JsonElement): Double {
        if (element.asJsonObject.has("cu_condenser_fan_kwh")) {
        return element.asJsonObject.get("cu_condenser_fan_kwh").asDouble
        }
        return 0.0
        }

        fun extractCondensingUnitcondensorkW(element: JsonElement): Double {
        if (element.asJsonObject.has("cu_condenser_fan_kw")) {
        return element.asJsonObject.get("cu_condenser_fan_kw").asDouble
        }
        return 0.0
        }

        fun extractCondensingUnitheadkWh(element: JsonElement): Double {
        if (element.asJsonObject.has("cu_floating_head_pressure_controls_kwh")) {
        return element.asJsonObject.get("cu_floating_head_pressure_controls_kwh").asDouble
        }
        return 0.0
        }

        fun extractCondensingUnitheadkW(element: JsonElement): Double {
        if (element.asJsonObject.has("cu_floating_head_pressure_controls_kw")) {
        return element.asJsonObject.get("cu_floating_head_pressure_controls_kw").asDouble
        }
        return 0.0
        }

        fun extractCondensingUnitIncrementalCost(element: JsonElement): Double {
        if (element.asJsonObject.has("cu_incremental_cost")) {
        return element.asJsonObject.get("cu_incremental_cost").asDouble
        }
        return 0.0
        }
         */
//This relates to queryEvaporatorFanMotor
        /**
        fun extractEvapFanMotorkWh(element: JsonElement): Double {
        if (element.asJsonObject.has("ev_energy_savings")) {
        return element.asJsonObject.get("ev_energy_savings").asDouble
        }
        return 0.0
        }

        fun extractEvapFanMotorkW(element: JsonElement): Double {
        if (element.asJsonObject.has("ev_demand_savings")) {
        return element.asJsonObject.get("ev_demand_savings").asDouble
        }
        return 0.0
        }

        fun extractEvapFanMotorIncrementalCost(element: JsonElement): Double {
        if (element.asJsonObject.has("ev_installation_cost")) {
        return element.asJsonObject.get("ev_installation_cost").asDouble
        }
        return 0.0
        }
         */

//This relates to queryEvaporatorFanMotorControls
        /**
        fun extractEvapFanMotorControlskWh(element: JsonElement): Double {
        if (element.asJsonObject.has("evf_energy_savings")) {
        return element.asJsonObject.get("evf_energy_savings").asDouble
        }
        return 0.0
        }

        fun extractEvapFanMotorControlskW(element: JsonElement): Double {
        if (element.asJsonObject.has("evf_demand_savings")) {
        return element.asJsonObject.get("evf_demand_savings").asDouble
        }
        return 0.0
        }

        fun extractEvapFanMotorControlsCost(element: JsonElement): Double {
        if (element.asJsonObject.has("evf_cost")) {
        return element.asJsonObject.get("evf_cost").asDouble
        }
        return 0.0
        }
         */
    }

    /**
     * HVAC - Age
     * */
    var age = 0


    /**
     * HVAC - British Thermal Unit
     * */
    var btu = 0


    /**
     * City | State
     * */
    private var city = ""
    private var state = ""

    /**
     * Usage Hours
     * */
    private var peakHours = 0.0
    private var partPeakHours = 0.0
    private var offPeakHours = 0.0


    var quantity = 0
    var kW = 0.0
    var capacity = 0

    var condensorTemp = ""
    var condesorCompressor = 0
    var condensorCompressorphase = 0

    var motortype = ""
    var fridgetype = ""

    var fanmotortype = ""
    var temprange = ""

    override fun setup() {
        try {

            quantity = featureData["Quantity"]!! as Int

            age = featureData["Age"]!! as Int
         //   btu = featureData["Cooling Capacity (Btu/hr)"]!! as Int
         //   gasInput = featureData["Heating Input (Btu/hr)"]!! as Int
         //   gasOutput = featureData["Heating Output (Btu/hr)"]!! as Int
            condesorCompressor = featureData["Condensor Compressor Size (HP)"]!! as Int
            condensorCompressorphase = featureData["Compressor Phase"]!! as Int
            condensorTemp = featureData["Temp"]!! as String

            motortype = featureData["Motor Type"]!! as String
            fridgetype = featureData["Refrigeration Type"]!! as String

            fanmotortype = featureData["Fan Motor Type"]!! as String
            temprange = featureData["Temperature Range"]!! as String

            kW = featureData["Heating Power (kW)"]!! as Double

        //    unittype = featureData["Type of Unit"]!! as String
            capacity = featureData["Capacity (BTU)"]!! as Int
            peakHours = featureData["Peak Hours"]!! as Double
         //   partPeakHours = featureData["Part Peak Hours"]!! as Double
            offPeakHours = featureData["Off Peak Hours"]!! as Double


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Getting year of device and how much over life it is
     */
    fun year(): Int {
        return getYear(age)
    }

    fun overAge(): Int {
        return age - 15
    }

    /**
     * Cost - Pre State
     * */
    override fun costPreState(elements: List<JsonElement?>): Double {

        return 0.0 // TODO: AK2 needs to calculate this.
    }
// TODO: @k2interactive enter walkin equations below.
//  The energy savings (kwh) and demand savings (kw) pulled from the PARSE dashboard are the gross values.
//  The net values are created using the equations below.
//  The gross & net savings for both kwh and kw as well as the cost (pulled from PARSE) should be crunched to the CSV.
//  I also want the sum of the gross energy savings pulled from the PARSE combined with a fun grosskwhsavings() &
//  the sum of the costs pulled from the PARSE combined with a fun installcost() and all pushed to the report.
//  I added a comment in SorterForWordDocumentGenerator.kt line 14 to match to this.

    /** Condensing unit gross and net savings: energy (kwh) and demand (kw)
    CU_gross_energy_savings = extractCondensingUnitcompressorkWh(element) +
    extractCondensingUnitcondensorkWh(element) + extractCondensingUnitheadkWh(element)

    // CSV header should be titled __HE_Condensing_Unit_Gross_kwh

    CU_gross_demand_savings = extractCondensingUnitcompressorkW(element) +
    extractCondensingUnitcondensorkW(element) + extractCondensingUnitheadkW(element)

    // CSV header should be titled __HE_Condensing_Unit_Gross_kw

    CU_net_energy_savings = (extractCondensingUnitcompressorkWh(element) * (1 + 0.121) * (1 + 1 – 1) * 0.5) +
    (extractCondensingUnitcompressorkWh(element) * (1 + 0.149) * (1 + 1 – 1) * 0.5) +
    (extractCondensingUnitcondensorkWh(element) * (1 + 0.121) * (1 + 1 – 1) * 0.5583) +
    (extractCondensingUnitcondensorkWh(element) * (1 + 0.149) * (1 + 1 – 1) * 0.4018) +
    (extractCondensingUnitheadkWh(element) * (1 + 0.121) * (1 + 1 – 1) * 0.539) +
    (extractCondensingUnitheadkWh(element) * (1 + 0.149) * (1 + 1 – 1) * 0.462)

    // CSV header should be titled __HE_Condensing_Unit_Net_kwh

    CU_net_demand_savings = (extractCondensingUnitcompressorkW(element) * (1 + 0.113) * (1 + 1 – 1) * 0.69) +
    (extractCondensingUnitcompressorkW(element) * (1 + 0.112) * (1 + 1 – 1) * 0.772) +
    (extractCondensingUnitcondensorkW(element) * (1 + 0.113) * (1 + 1 – 1) * 1) +
    (extractCondensingUnitcondensorkW(element) * (1 + 0.112) * (1 + 1 – 1) * 0.0115) +
    (extractCondensingUnitheadkW(element) * (1 + 0.113) * (1 + 1 – 1) * 1) +
    (extractCondensingUnitheadkW(element) * (1 + 0.112) * (1 + 1 – 1) * 0.0)

    // CSV header should be titled __HE_Condensing_Unit_Net_kw


*/
     /**
     * Cost - Post State
     * */
    override fun costPostState(element: JsonElement, dataHolder: DataHolder): Double {

        Timber.d("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$")
        Timber.d("!!! COST POST STATE - WALK-IN Refrigerator !!!")
        Timber.d("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$")


             return 5.0 // TODO: AK2 needs to calculate this
    }

      /**
     * HVAC - INCENTIVES | MATERIAL COST
     * */
    override fun incentives(): Double {
        return 0.0
    }

    override fun materialCost(): Double {
        return 0.0
    }

    override fun laborCost(): Double {
        return 0.0
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
    override fun usageHoursPre(): Double = 0.0
    override fun usageHoursPost(): Double = 0.0

    /**
     * PowerTimeChange >> Energy Efficiency Calculations
     * I need to insert the heating and cooling hours based on set-point temp, operation hours, and thermostat schedule
     * */
    override fun energyPowerChange(): Double {


        return 0.0 // TODO: AK2 needs to calculate
    }

    fun totalSavings(): Double {
        return energyPowerChange() * .15 // TODO: AK2 needs to calculate this
    }

    override fun energyTimeChange(): Double = 0.0
    //Calculating TRM Net Savings for Evaporator Fan Motor (pg. 103)
    val freerider = 0.95
    val spillover = 1.05
    //Values recieved wil be savings per fan
    override fun energyPowerTimeChange(): Double = 0.0


    /**
     * State if the Equipment has a Post UsageHours Hours (Specific) ie. A separate set of
     * Weekly UsageHours Hours apart from the PreAudit
     * */
    override fun usageHoursSpecific() = false

    override fun efficientLookup()= false
    override fun queryEfficientFilter()= ""

//TODO: @k2interactive make the below query filters active - (the functions will need to be created in EBase - see TODOs at EBase line 389)
/**override fun queryEvaporatorFanMotor(): String {
    return JSONObject()
            .put("type", refrigeration_evaporatorfanmotor)
            .put("data.motor_type", motortype)
            .put("data.refrigeration_type", fridgetype)
            .toString()
}*/

    /**override fun queryCondensingUnit(): String {
    return JSONObject()
    .put("type", refrigeration_condensingunit)
    .put("data.phase", condensorCompressorphase)
    .put("data.temp", condensorTemp)
    .put("data.hp", condesorCompressor)
    .toString()
    }*/

    /**override fun queryEvaporatorFanMotorControls(): String {
    return JSONObject()
    .put("type", refrigeration_evaporatorfanmotorcontrols)
    .put("data.motor_type", fanmotortype)
    .put("data.temperature_range", temprange)
    .toString()
    }*/

    /**override fun queryEvaporatorFanMotorControls(): String {
    return JSONObject()
    .put("type", refrigeration_evaporatorfanmotorcontrols)
    .put("data.motor_type", fanmotortype)
    .put("data.temperature_range", temprange)
    .toString()
    }*/

    override fun preAuditFields() = mutableListOf("")

    override fun featureDataFields() = getGFormElements().map { it.value.param!! }.toMutableList()

    override fun preStateFields() = mutableListOf("")
    override fun postStateFields() = mutableListOf("")

    override fun computedFields() = mutableListOf("")

    private fun getFormMapper() = FormMapper(context, R.raw.walkin_refrigerator)
    private fun getModel() = getFormMapper().decodeJSON()
    private fun getGFormElements() = getFormMapper().mapIdToElements(getModel())

}
