package com.gemini.energy.service.device

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

class WaterHeater(computable: Computable<*>, utilityRateGas: UtilityRate, utilityRateElectricity: UtilityRate,
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
         * Conversion Factor from Watts to Kilo Watts
         * */
        private const val KW_CONVERSION = 0.001

        private const val HVAC_EER = "hvac_eer"
        private const val HVAC_COOLING_HOURS = "cooling_hours"
        private const val HVAC_EFFICIENCY = "hvac_efficiency"

        private const val HVAC_DB_BTU = "size_btu_hr"
        private const val HVAC_DB_EER = "eer"


        /**
         * Fetches the EER based on the specific Match Criteria via the Parse API
         * Not needed right now

        fun extractEER(elements: List<JsonElement?>): Double {
            elements.forEach {
                it?.let {
                    if (it.asJsonObject.has("eer")) {
                        return it.asJsonObject.get("eer").asDouble
                    }
                }
            }
            return 0.0
        }
         * */
        /**
         * Fetches the Hours based on the City
         * @Anthony - Verify where we are using the Extracted Hours ??
         * This is used to calculate the Energy but needs to be more robust before using

        fun extractHours(elements: List<JsonElement?>): Int {
            elements.forEach {
                it?.let {
                    if (it.asJsonObject.has("hours")) {
                        return it.asJsonObject.get("hours").asInt
                    }
                }
            }
            return 0
        }
         * */
        /**
         * WaterHeater - Power Consumed
         * There could be a case where the User will input the value in KW - If that happens we need to convert the KW
         * int BTU / hr :: 1KW equals 3412.142
         * */
        fun power(gasInput: Int, thermaleff: Int): Double  {
            if (thermaleff == 0) return 0.0
            return (gasInput / (thermaleff.toDouble() / 100.0)) * KW_CONVERSION
        }

        fun power2(kW: Double, electriceff: Double): Double {
            if (electriceff == 0.0) return 0.0
            return (kW / (electriceff/100.0))
        }
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
    }

    /**
     * HVAC - Age
     * */
    var age = 0


    /**
     * HVAC - British Thermal Unit
     * */
    var btu = 0
    private var gasInput = 0
    private var gasOutput = 0


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
    var thermaleff = 0
    var electriceff = 0.0
    var kW = 0.0
    var fueltype = ""
    var unittype = ""
    var capacity = 0.0
    var postthermeff = 95
    var posteleceff = 350.0

    override fun setup() {
        try {

            quantity = featureData["Quantity"]!! as Int

            age = featureData["Age"]!! as Int
//            btu = featureData["Cooling Capacity (Btu/hr)"]!! as Int
            gasInput = featureData["Heating Input (Btu/hr)"]!! as Int
            gasOutput = featureData["Heating Output (Btu/hr)"]!! as Int
            thermaleff = featureData["Thermal Efficiency"]!! as Int
            electriceff = featureData["Heating Electirc Efficiency"]!! as Double
            kW = featureData["Heating Power (kW)"]!! as Double
            fueltype = featureData["Fuel Type"]!! as String
            unittype = featureData["Type of Unit"]!! as String
            capacity = featureData["Capacity (U.S. Gal)"]!! as Double
            peakHours = featureData["Peak Hours"]!! as Double
            partPeakHours = featureData["Part Peak Hours"]!! as Double
            offPeakHours = featureData["Off Peak Hours"]!! as Double


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
     override fun isGas() = fueltype == "Natural Gas"

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
//NoteChange
        val usageHours = (3 * 365) as UsageSimple//https://energyusecalculator.com/electricity_waterheater.htm - but need to explore actual times
        computable.udf1 = usageHours
        Timber.d(usageHours.toString())

        val powerUsedGas = power(gasInput, thermaleff) * quantity
        val powerUsedElectricity = power2(kW, electriceff) * quantity
        preElectricPower = powerUsedElectricity
        val gascost: Double
        val ecost: Double
        val powerUsed = if (isGas()) powerUsedGas else powerUsedElectricity
        Timber.d("HotWater :: Power Used (Electricity) -- [$powerUsedElectricity]")
        Timber.d("HotWater :: Power Used (Gas) -- [$powerUsedGas]")


        Timber.d("HotWater :: Pre Power Used -- [$powerUsed]")
        ecost = costElectricity(powerUsed, usageHours, electricityRate)

        gascost = costGas(powerUsed)

        return if (isGas()) gascost else ecost
    }

    var preElectricPower = power2(kW, electriceff) * quantity
    var postElectricPower = power2(kW, posteleceff) * quantity
    var ElectricPowerSavings = preElectricPower -postElectricPower

            /**
     * Cost - Post State
     * */
    override fun costPostState(element: JsonElement, dataHolder: DataHolder): Double {

        Timber.d("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$")
        Timber.d("!!! COST POST STATE - HVAC !!!")
        Timber.d("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$")

//Note Change - Newer units run's 50% less often
         val postUsageHours = (1.5 * 365) as UsageSimple
         //val postUsageHours = computable.udf1 as UsageSimple

            val postpowerUsedGas = power(gasInput, postthermeff) * quantity
            val postpowerUsedElectricity = power2(kW, posteleceff) * quantity
         postElectricPower = postpowerUsedElectricity
            val postGcost: Double
            val postEcost: Double
            val powerUsed = if (isGas()) postpowerUsedGas else postpowerUsedElectricity
        Timber.d("HotWater :: Power Used (Electricity) -- [$postpowerUsedElectricity]")
            Timber.d("HotWater :: Power Used (Gas) -- [$postpowerUsedGas]")


            Timber.d("HotWater :: Post Power Used -- [$powerUsed]")
            postEcost = costElectricity(powerUsed, postUsageHours, electricityRate)

            postGcost = costGas(powerUsed)

             return if (isGas()) postGcost else postEcost
    }

      /**
     * HVAC - INCENTIVES | MATERIAL COST
     * */
    override fun incentives(): Double {
        return 0.0
    }

    override fun materialCost(): Double {
        return 1000.0
    }

    override fun laborCost(): Double {
        return 700.0
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
    override fun usageHoursPre(): Double = 3.0 * 365
    override fun usageHoursPost(): Double = 1.5 * 365

    /**
     * PowerTimeChange >> Energy Efficiency Calculations
     * I need to insert the heating and cooling hours based on set-point temp, operation hours, and thermostat schedule
     * */
    override fun energyPowerChange(): Double {

        val powerUsedGas = power(gasInput, thermaleff) * quantity
        val powerUsedElectricity = power2(kW, electriceff) * quantity
        val prepowerUsed = if (isGas()) powerUsedGas else powerUsedElectricity

        var postthermeff = 95
        var posteleceff = 350.0

        val postpowerUsedGas = power(gasInput, postthermeff) * quantity
        val postpowerUsedElectricity = power2(kW, posteleceff) * quantity
        val postpowerUsed = if (isGas()) postpowerUsedGas else postpowerUsedElectricity

        // Step 1 : Get the Delta
        val delta = (prepowerUsed - postpowerUsed) * (1.5 * 365)

        Timber.d("HVAC :: Delta -- $delta")
        //ToDo: Multiply by the Number of Equipment
        return delta
    }
    //fix this ADK2
    fun totalSavings(): Double {
        return costElectricity(energyPowerChange(),electricityRate) // + costElectricity(ElectricPowerSavings,demandRate)
    }

    override fun energyTimeChange(): Double = 0.0
    override fun energyPowerTimeChange(): Double = 0.0


    /**
     * State if the Equipment has a Post UsageHours Hours (Specific) ie. A separate set of
     * Weekly UsageHours Hours apart from the PreAudit
     * */
    override fun usageHoursSpecific() = false

    override fun efficientLookup()= false
    override fun queryEfficientFilter()= ""
    override fun preAuditFields() = mutableListOf("General Client Info Name", "General Client Info Position", "General Client Info Email")

    override fun featureDataFields() = getGFormElements().map { it.value.param!! }.toMutableList()

    override fun preStateFields() = mutableListOf("")
    override fun postStateFields() = mutableListOf("__life_hours", "__maintenance_savings",
            "__cooling_savings", "__energy_savings", "__energy_at_post_state")

    override fun computedFields() = mutableListOf("")

    private fun getFormMapper() = FormMapper(context, R.raw.hotwater)
    private fun getModel() = getFormMapper().decodeJSON()
    private fun getGFormElements() = getFormMapper().mapIdToElements(getModel())

}
