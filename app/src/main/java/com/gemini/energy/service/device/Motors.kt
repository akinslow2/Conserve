package com.gemini.energy.service.device

import android.content.Context
import com.gemini.energy.R
import com.gemini.energy.domain.entity.Computable
import com.gemini.energy.presentation.form.FormMapper
import com.gemini.energy.service.DataHolder
import com.gemini.energy.service.IComputable
import com.gemini.energy.service.OutgoingRows
import com.gemini.energy.service.type.UsageHours
import com.gemini.energy.service.type.UsageMotors
import com.gemini.energy.service.type.UtilityRate
import com.google.gson.JsonElement
import io.reactivex.Observable
import org.json.JSONObject
import timber.log.Timber
import java.util.*

class Motors(computable: Computable<*>, utilityRateGas: UtilityRate, utilityRateElectricity: UtilityRate,
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
        private const val MOTOR_EFFICIENCY = "motor_efficiency"
        private const val TYPE1 = "BED_VFD_Prescriptive_kWh"
        private const val TYPE2 = "BED_VFD_Prescriptive_kW"
        /**
         * Fetches the Motor Efficiency (NEMA-Premium) based on the specific Match Criteria
         * via the Parse API
         * */
        fun extractNemaPremium(elements: List<JsonElement?>): Double {
            elements.forEach {
                it?.let {
                    if (it.asJsonObject.has("cee_specification_nema_premium")) {
                        return it.asJsonObject.get("cee_specification_nema_premium").asDouble
                    }
                }
            }
            return 0.0
        }
        fun extractDemandSavings(elements: List<JsonElement?>): Double {
            elements.forEach {
                it?.let {
                    if (it.asJsonObject.has("Demand_Savings")) {
                        return it.asJsonObject.get("Demand_Savings").asDouble
                    }
                }
            }
            return 0.0
        }
        fun extractEnergySavings(elements: List<JsonElement?>): Double {
            elements.forEach {
                it?.let {
                    if (it.asJsonObject.has("Energy_Savings")) {
                        return it.asJsonObject.get("Energy_Savings").asDouble
                    }
                }
            }
            return 0.0
        }
    }
    var utilitycompany = ""
    private var srs = 0
    private var mrs = 0
    private var nrs = 0
    private var hp = 0.0
    private var efficiency = 0.0

    //Vermont Prescriptive Savings Inputs
    private var motortype = ""
    private var OTF = "" //yes equal 1 and no or null equals 0.9
    private var controls = "" //yes equals .27 no or null equals 0

    /**
     * Suggested Alternative
     * */
    private var alternateHp = 0.0
    private var alternateEfficiency = 0.0

    private var peakHours = 0.0
    private var partPeakHours = 0.0
    private var offPeakHours = 0.0

    override fun setup() {
        try {
            utilitycompany = preAudit["Others Utility Company"]!! as String
            motortype = featureData["Purpose"]!! as String
            srs = featureData["Synchronous Rotational Speed (SRS)"]!! as Int
            mrs = featureData["Measured Rotational Speed (MRS)"]!! as Int
            nrs = featureData["Nameplate Rotational Speed (NRS)"]!! as Int
            hp = featureData["Horsepower (HP)"]!! as Double
            efficiency = featureData["Efficiency"]!! as Double

            OTF = featureData["Operational Testing Will Be Conducted"]!! as String
            controls = featureData["Controls"]!! as String
            alternateHp = featureData["Alternate Horsepower (HP)"]!! as Double
            alternateEfficiency = featureData["Alternate Efficiency"]!! as Double

            peakHours = (featureData["Peak Hours"]!! as String).toDoubleOrNull() ?: 0.0
            peakHours = (featureData["Peak Hours"]!! as String).toDoubleOrNull() ?: 0.0
            partPeakHours = (featureData["Part Peak Hours"]!! as String).toDoubleOrNull() ?: 0.0
            offPeakHours = (featureData["Off Peak Hours"]!! as String).toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Cost - Pre State
     * */
    override fun costPreState(elements: List<JsonElement?>): Double {

        val percentageLoad = (srs - mrs) / (srs - nrs)
        val powerUsed = hp * KW_CONVERSION * percentageLoad / efficiency
//        val nemaPremium = extractNemaPremium(elements)
        val nemaPremium = 0.0

        Timber.d("*** Nema Premium :: ($nemaPremium) ***")

        var usageHours = usageHoursSpecific
        if (peakHours != 0.0 || partPeakHours != 0.0 || offPeakHours != 0.0) {
            usageHours = UsageMotors()
            usageHours.peakHours = peakHours
            usageHours.partPeakHours = partPeakHours
            usageHours.offPeakHours = offPeakHours
        }

        return costElectricity(powerUsed, usageHours, electricityRate)
    }

    /**
     * Cost - Post State
     * */
    override fun costPostState(element: JsonElement, dataHolder: DataHolder): Double {

        // @Anthony - Post State Implementation ?? Yet to determine.
        Timber.d("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$")
        Timber.d("!!! COST POST STATE - Motors !!!")
        Timber.d("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$")

        //1. Energy Savings
        val energySavings = energyPowerChange()

        //1a. BED Prescriptive Gross Energy Savings (kWh) for BLPM circulator pump
        val kwBase = hp * KW_CONVERSION
        val kwEff = alternateHp * KW_CONVERSION
        val hours = 4592 //according to TRM pg. 80
        var grossBEDkwhsavings = 0.0
        if (utilitycompany == "Burlington Electric Department" && controls == "yes") {
            grossBEDkwhsavings = kwBase - kwEff * (1 - 0.73) * hours // //according to Vermont TRM pg. 80
        } else  {
            grossBEDkwhsavings = kwBase - kwEff * hours ////according to Vermont TRM pg. 80
        }
        //1b. BED Prescriptive Net Energy Savings (kWh) for BLPM circulator pump
        val freerider = 0.95
        val spillover = 1.0
        val winterRPF = 0.99 // RPF = rating period factor
        val summerRPF = 0.01 //see pg. 80 of Vermont TRM
        val winterLLF = 1.3 // LLF = line lost factor
        val summerLLF = 11.2
        var netBEDkwhsavings = (grossBEDkwhsavings * (1 + winterLLF) * (freerider + spillover - 1) * winterRPF) +
                    (grossBEDkwhsavings * (1 + summerLLF) * (freerider + spillover - 1) * summerRPF)

        //1c. BED Prescriptive Savings for VFD - Based on table on pg. 70 of Vermont TRM
        var VFDeSavings = if (OTF == "yes") {
            extractEnergySavings(elements) / 0.9
        } else {
            extractEnergySavings(elements)
        }
        //2. Demand Saving
        val demandSavings = energyPowerChange() / usageHoursPre()

        //2a. BED Prescriptive Savings for VFD - Based on pg. 70 of Vermont TRM
        var VFDdSavings = extractDemandSavings(elements) * hp * KW_CONVERSION


        //3. Implementation Cost
        // ToDo - The below cost would be a look up in the future.
        val costPremiumMotor = 697
        val costStandardMotor = 555
        val implementationCost = (costPremiumMotor - costStandardMotor)

        val postRow = mutableMapOf<String, String>()
        postRow["__energy_savings"] = energySavings.toString()
        postRow["__demand_savings"] = demandSavings.toString()
        postRow["__implementation_cost"] = implementationCost.toString()
        postRow["__Gross_Savings_BLPM_circulator_pump"] = grossBEDkwhsavings.toString()
        postRow["__Net_Savings__BLPM_circulator_pump"] = netBEDkwhsavings.toString()
        postRow["__VFD_Prescriptive_Energy_Savings"] = VFDeSavings.toString()
        postRow["__VFD_Prescriptive_Demand_Savings"] = VFDdSavings.toString()

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
    override fun energyPowerChange(): Double {
        val percentageLoadPre = (srs - mrs) / (srs - nrs).toString().toDouble()
        val percentageLoadPost = if (percentageLoadPre > 1.0) (hp / alternateHp) * percentageLoadPre else percentageLoadPre

        val horsePowerPre = (hp / efficiency) * percentageLoadPre
        val horsePowerPost = (alternateHp / alternateEfficiency) * percentageLoadPost
        val deltaHorsePower = (horsePowerPre - horsePowerPost) * KW_CONVERSION

        // ** Note :: Load shouldn't be 1 - Ideally increase the Power of the Motor and lower the load to less than 1
        // ** If the load is above 1 - Notify the User above 100 % - Replace a Higher Size
        // ** The better Rating would be a Motor of HP 7

        // ** This is what we recommended
        // Change Motor with the same Horse Power - Overload it
        // Get a new Motor which is bigger in size and of a Premium Efficient
        // Less Load and more efficiency
        Timber.d("*** Percentage Load Pre :: ($percentageLoadPre)")
        Timber.d("*** Percentage Load Post :: ($percentageLoadPost)")

        val delta = deltaHorsePower * usageHoursPre()
        return delta
    }

    override fun queryBEDMotorVFDprescriptivekwh() = JSONObject()
    .put("type", TYPE1)
    .put("data.hp", hp)
    .put("data.purpose", motortype)
    .toString()    //check to make sure it is correct


    override fun queryBEDMotorVFDprescriptivekw() = JSONObject()
            .put("type", TYPE2)
            .put("data.hp", hp)
            .put("data.purpose", motortype)
            .toString()    //check to make sure it is correct
    override fun energyTimeChange(): Double = 0.0
    override fun energyPowerTimeChange(): Double = 0.0

    /**
     * Energy Efficiency Lookup Query Definition
     * */
    override fun efficientLookup() = false
    override fun queryEfficientFilter() = ""

    override fun queryMotorEfficiency() = JSONObject()
            .put("type", MOTOR_EFFICIENCY)
            .put("data.hp", hp)
            .put("data.rpm_start_range", JSONObject().put("\$lte", nrs))
            .put("data.rpm_end_range", JSONObject().put("\$gte", nrs))
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
    override fun postStateFields() = mutableListOf("__life_hours", "__maintenance_savings",
            "__cooling_savings", "__energy_savings","__Gross_Savings_BLPM_circulator_pump","__Net_Savings__BLPM_circulator_pump",
            "__VFD_Prescriptive_Energy_Savings", "__VFD_Prescriptive_Demand_Savings")

    override fun computedFields() = mutableListOf("")

    private fun getFormMapper() = FormMapper(context, R.raw.motors)
    private fun getModel() = getFormMapper().decodeJSON()
    private fun getGFormElements() = getFormMapper().mapIdToElements(getModel())

}

