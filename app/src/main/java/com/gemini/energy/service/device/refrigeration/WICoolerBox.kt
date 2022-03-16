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
import com.gemini.energy.toDoubleOr
import com.gemini.energy.toIntOr
import com.gemini.energy.toStringOr
import com.google.gson.JsonElement
import io.reactivex.Observable
import java.text.SimpleDateFormat
import java.util.*

class WICoolerBox(computable: Computable<*>, utilityRateGas: UtilityRate, utilityRateElectricity: UtilityRate,
                  usageHours: UsageHours, outgoingRows: OutgoingRows, private val context: Context) :
        EBase(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows), IComputable {

    /**
     * Entry Point
     * */
    override fun compute(): Observable<Computable<*>> {
        return super.compute(extra = ({}))
    }

    companion object {

        /**
         * Conversion Factor from Watts to Kilo Watts
         * */
        private const val KW_CONVERSION = 0.001

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
        fun power(gasInput: Double, thermaleff: Double) =
                if (thermaleff == 0.0) 0.0
                else (gasInput / (thermaleff / 100)) * KW_CONVERSION

        fun power2(kW: Double, electriceff: Double) =
                if (electriceff == 0.0) 0.0
                else (kW / (electriceff / 100))

        /**
         * Year At - Current minus the Age
         * */
        private val dateFormatter = SimpleDateFormat("yyyy", Locale.ENGLISH)

        fun getYear(age: Int): Int {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.YEAR, "-$age".toInt()) //** Subtracting the Age **
            return dateFormatter.format(calendar.time).toInt()
        }

        fun firstNotNull(valueFirst: Double, valueSecond: Double) =
                if (valueFirst == 0.0) valueSecond else valueFirst

        fun firstNotNull(valueFirst: Int, valueSecond: Int) =
                if (valueFirst == 0) valueSecond else valueFirst
    }

    /** year equipment was manufactured **/
    var year = 0


    /**
     * HVAC - British Thermal Unit
     * */
    var btu = 0
    private var gasInput = 0.0
    private var gasOutput = 0.0


    /**
     * City | State
     * */
    private var city = ""
    private var state = ""

    /**
     * Usage Hours
     * */
    private var evaporatorPeakHours = 0.0
    private var evaporatorPartPeakHours = 0.0
    private var evaporatorOffPeakHours = 0.0

    private var condensorPeakHours = 0.0
    private var condensorPartPeakHours = 0.0
    private var condensorOffPeakHours = 0.0


    var quantity = 0
    var thermaleff = 0.0
    var electriceff = 0.0
    var kW = 0.0
    var fueltype = ""
    var unittype = ""
    var capacity = 0.0

    override fun setup() {
        try {
            quantity = featureData["Quantity"].toIntOr(0)
            year = featureData["Year"].toIntOr(0)

            kW = featureData["Heating Power (kW)"].toDoubleOr(0.0)

            condensorPeakHours = featureData["Condenser Peak Hours"].toDoubleOr(0.0)
            condensorPartPeakHours = featureData["Condenser Off Peak Hours"].toDoubleOr(0.0)
            condensorOffPeakHours = featureData["Condenser Off Peak Hours"].toDoubleOr(0.0)

            evaporatorPeakHours = featureData["Evaporator Peak Hours"].toDoubleOr(0.0)
            evaporatorPartPeakHours = featureData["Evaporator Off Peak Hours"].toDoubleOr(0.0)
            evaporatorOffPeakHours = featureData["Evaporator Off Peak Hours"].toDoubleOr(0.0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun isGas() = fueltype == "Natural Gas"

    fun overAge(): Int {
        return getAge(year) - 15
    }

    /**
     * Cost - Pre State
     * */
    override fun costPreState(elements: List<JsonElement?>): Double {

        val usageHours = UsageSimple(condensorPeakHours, condensorPartPeakHours, condensorOffPeakHours)
        computable.udf1 = usageHours

        val powerUsedGas = power(gasInput, thermaleff) * quantity
        val powerUsedElectricity = power2(kW, electriceff) * quantity
        val gascost: Double
        val ecost: Double
        val powerUsed = if (isGas()) powerUsedGas else powerUsedElectricity
        ecost = costElectricity(powerUsed, usageHours, electricityRate)

        gascost = costGas(powerUsed)

        return if (isGas()) gascost else ecost
    }

    /**
     * Cost - Post State
     * */
    override fun costPostState(element: JsonElement, dataHolder: DataHolder): Double {

        val postthermeff = 95.0
        val posteleceff = 350.0

        val postUsageHours = computable.udf1 as UsageSimple

        val postpowerUsedGas = power(gasInput, postthermeff) * quantity
        val postpowerUsedElectricity = power2(kW, posteleceff) * quantity
        val postGcost: Double
        val postEcost: Double
        val powerUsed = if (isGas()) postpowerUsedGas else postpowerUsedElectricity
        postEcost = costElectricity(powerUsed, postUsageHours, electricityRate)

        postGcost = costGas(powerUsed)

        return if (isGas()) postGcost else postEcost
    }

    fun installCost(): Double {
//     TODO:   sum of the costs pulled from the PARSE
        return 0.0
    }

    fun grosskwhSavings(): Double {
//     TODO:   sum of the gross energy savings pulled from the PARSE
        return 0.0
    }

    /**
     * HVAC - INCENTIVES | MATERIAL COST
     * */
    override fun incentives(): Double {
        val powerChangeToHours =
                if (usageHoursBusiness.yearly() == 0.0) 0.0
                else energyPowerChange() / usageHoursBusiness.yearly()
        return energyPowerChange() * 0.15 + (powerChangeToHours) * 150
    }

    override fun materialCost(): Double {
        return 1000.0
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

        val powerUsedGas = power(gasInput, thermaleff) * quantity
        val powerUsedElectricity = power2(kW, electriceff) * quantity
        val prepowerUsed = if (isGas()) powerUsedGas else powerUsedElectricity

        val postthermeff = 95.0
        val posteleceff = 350.0

        val postpowerUsedGas = power(gasInput, postthermeff) * quantity
        val postpowerUsedElectricity = power2(kW, posteleceff) * quantity
        val postpowerUsed = if (isGas()) postpowerUsedGas else postpowerUsedElectricity

        // Step 1 : Get the Delta
        val delta = (prepowerUsed - postpowerUsed) * 1100

        //ToDo: Multiply by the Number of Equipment
        return delta
    }

    //fix this ADK2
    fun totalSavings(): Double {
        return energyPowerChange() * .18
    }

    override fun energyTimeChange(): Double = 0.0
    override fun energyPowerTimeChange(): Double = 0.0


    /**
     * State if the Equipment has a Post UsageHours Hours (Specific) ie. A separate set of
     * Weekly UsageHours Hours apart from the PreAudit
     * */
    override fun usageHoursSpecific() = false

    override fun efficientLookup() = false
    override fun queryEfficientFilter() = ""
    override fun preAuditFields() = mutableListOf("General Client Info Name", "General Client Info Position", "General Client Info Email")

    override fun featureDataFields() = getGFormElements().map { it.value.param!! }.toMutableList()

    override fun preStateFields() = mutableListOf("")
    override fun postStateFields() = mutableListOf("__life_hours", "__maintenance_savings",
            "__cooling_savings", "__energy_savings", "__energy_at_post_state")

    override fun computedFields() = mutableListOf("")

    private fun getFormMapper() = FormMapper(context, R.raw.walkin_coolbot)
    private fun getModel() = getFormMapper().decodeJSON()
    private fun getGFormElements() = getFormMapper().mapIdToElements(getModel())

}
