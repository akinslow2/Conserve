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
        private const val TYPE1 = "hvac_vfdprescriptive"

        /**
         * Fetches the Motor Efficiency (NEMA-Premium) based on the specific Match Criteria
         * via the Parse API
         * */
        fun extractNemaPremium(elements: List<JsonElement?>): Double {
            elements.forEach {
                it?.let {
                    if (it.asJsonObject.has("cee_specification_nema_premium"))
                        return it.asJsonObject.get("cee_specification_nema_premium").asDouble
                }
            }
            return 0.0
        }

        fun extractDemandSavings(element: JsonElement): Double {
            if (element.asJsonObject.has("demand_savings"))
                return element.asJsonObject.get("demand_savings").asDouble
            return 0.0
        }

        fun extractEnergySavings(element: JsonElement): Double {
            if (element.asJsonObject.has("energy_savings"))
                return element.asJsonObject.get("energy_savings").asDouble
            return 0.0
        }

        fun extractInstallCost(element: JsonElement): Double {
            if (element.asJsonObject.has("total_installed_cost"))
                return element.asJsonObject.get("total_installed_cost").asDouble
            return 0.0
        }

        fun extractOffRpf(element: JsonElement): Double {
            if (element.asJsonObject.has("off"))
                return element.asJsonObject.get("off").asDouble
            return 0.0
        }

        fun extractOnRpf(element: JsonElement): Double {
            if (element.asJsonObject.has("on"))
                return element.asJsonObject.get("on").asDouble
            return 0.0
        }

        fun extractSummerCF(element: JsonElement): Double {
            if (element.asJsonObject.has("summer"))
                return element.asJsonObject.get("summer").asDouble
            return 0.0
        }

        fun extractWinterCF(element: JsonElement): Double {
            if (element.asJsonObject.has("winter"))
                return element.asJsonObject.get("winter").asDouble
            return 0.0
        }

    }

    var utilitycompany = ""
    private var srs = 0
    private var mrs = 0
    private var nrs = 0
    private var hp = 0.0
    private var efficiency = 0.0
    private var quantity = 0

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
            utilitycompany = preAudit["Others Electric Utility Company"]!! as String
            motortype = featureData["Purpose"]!! as String
            srs = (featureData["Synchronous Rotational Speed (SRS)"]!! as String).toIntOrNull() ?: 0
            mrs = featureData["Measured Rotational Speed (MRS)"]!! as Int
            nrs = featureData["Nameplate Rotational Speed (NRS)"]!! as Int
            hp = featureData["Horsepower (HP)"]!! as Double
            efficiency = featureData["Efficiency"]!! as Double
            quantity = featureData["Quantity"]!! as Int

            OTF = featureData["Operational Testing Will Be Conducted"]!! as String
            controls = featureData["Controls"]!! as String
            alternateHp = featureData["Alternate Horsepower (HP)"]!! as Double
            alternateEfficiency = featureData["Alternate Efficiency"]!! as Double

            peakHours = featureData["Peak Hours"]!! as Double
            offPeakHours = featureData["Off Peak Hours"]!! as Double
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Cost - Pre State
     * */
    override fun costPreState(elements: List<JsonElement?>): Double {

        val percentageLoad =
                if (srs - nrs == 0) 0
                else (srs - mrs) / (srs - nrs)
        val powerUsed =
                if (efficiency == 0.0) 0.0
                else hp * KW_CONVERSION * percentageLoad / efficiency
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
        var grossBLPMkwhsavings = 0.0
        if (controls == "yes") {
            grossBLPMkwhsavings = kwBase - kwEff * (1 - 0.73) * hours // //according to Vermont TRM pg. 80
        } else {
            grossBLPMkwhsavings = kwBase - kwEff * hours ////according to Vermont TRM pg. 80
        }
        var grossBLPMkwsavings = (kwBase - kwEff)

        //1b. BED Prescriptive Net Energy Savings (kWh) and Net Demand Savings (kW) for BLPM circulator pump
        val freerider = 0.95
        val spillover = 1.0
        val winterRPF = 0.613 // RPF = rating period factor
        val summerRPF = 0.387 //see pg. 80 of Vermont TRM
        val offpeakLLF = .121 // LLF = line lost factor
        val onpeakLLF = .149
        var netBLPMkwhsavings = (grossBLPMkwsavings * (1 + offpeakLLF) * (freerider + spillover - 1) * winterRPF) +
                (grossBLPMkwhsavings * (1 + onpeakLLF) * (freerider + spillover - 1) * summerRPF)

        var netBLPMkwsavings = (grossBLPMkwsavings * (1 + .113) * (freerider + spillover - 1) * .57) +
                (grossBLPMkwsavings * (1 + .112) * (freerider + spillover - 1) * .003)

        //1c. BED Prescriptive Savings for VFD - Based on table on pg. 70 of Vermont TRM
        var VFDeSavings =
                if (OTF == "yes") {
                    extractEnergySavings(element) / 0.9
                } else {
                    extractEnergySavings(element)
                }
        val demandSavings = energyPowerChange() / usageHoursPre()

        //2a. BED Prescriptive Savings for VFD - Based on pg. 70 of Vermont TRM
        var VFDdSavings = extractDemandSavings(element)

        val offRPF = extractOffRpf(element)
        val onRPF = extractOnRpf(element)
        val netkwhSavings = VFDeSavings * (1 + 0.121) * (0.95 + 1 - 1) * offRPF + VFDeSavings * (1 + 0.149) * (0.95 + 1 - 1) * onRPF

        val summerCF = extractSummerCF(element)
        val winterCF = extractWinterCF(element)
        val netkwSavings = VFDeSavings * (1 + 0.112) * (0.95 + 1 - 1) * summerCF + VFDdSavings * (1 + 0.113) * (0.95 + 1 - 1) * winterCF

        //3. Implementation Cost
        // ToDo - AK2 The below cost should be a look up based on HP for personal calcs
        val costPremiumMotor = 697
        val costStandardMotor = 555
        val implementationCost = (costPremiumMotor - costStandardMotor)
        //3a. VFD Costs based on pg. 70 of Vermont TRM
        var VFDinstallCost = extractInstallCost(element)

        val postRow = mutableMapOf<String, String>()
        postRow["__energy_savings"] = energySavings.toString()
        postRow["__demand_savings"] = demandSavings.toString()
        postRow["__implementation_cost"] = implementationCost.toString()
        postRow["__Gross_Energy_Savings_BLPM_circulator_pump"] = grossBLPMkwhsavings.toString()
        postRow["__Net_Energy_Savings__BLPM_circulator_pump"] = netBLPMkwhsavings.toString()
        postRow["__Gross_Demand_Savings_BLPM_circulator_pump"] = grossBLPMkwsavings.toString()
        postRow["__Net_Demand_Savings__BLPM_circulator_pump"] = netBLPMkwsavings.toString()
        postRow["__VFD_Gross_Prescriptive_Energy"] = VFDeSavings.toString()
        postRow["__VFD_Gross_Prescriptive_Demand"] = VFDdSavings.toString()
        postRow["__VFD_Prescriptive_Install_Cost"] = VFDinstallCost.toString()
        postRow["__VFD_Net_Prescriptive_Energy"] = netkwhSavings.toString()
        postRow["__VFD_Net_Prescriptive_Demand"] = netkwSavings.toString()

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

        val delta = deltaHorsePower * usageHoursPre() * quantity
        return delta
    }

    override fun queryMotorVFDprescriptive() = JSONObject()
            .put("type", TYPE1)
            .put("data.hp", hp)
            .put("data.purpose", motortype)
            .toString()


    override fun energyTimeChange(): Double = 0.0
    override fun energyPowerTimeChange(): Double = 0.0

    /**
     * Energy Efficiency Lookup Query Definition
     * */
    override fun efficientLookup() = false

    override fun queryEfficientFilter() = queryMotorVFDprescriptive()

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
    override fun preAuditFields() = mutableListOf<String>()

    override fun featureDataFields() = getGFormElements().map { it.value.param!! }.toMutableList()

    override fun preStateFields() = mutableListOf<String>()
    override fun postStateFields() = mutableListOf(
            "__life_hours",
            "__maintenance_savings",
            "__cooling_savings",
            "__energy_savings",
            "__demand_savings",
            "__implementation_cost",
            "__Gross_Energy_Savings_BLPM_circulator_pump",
            "__Gross_Demand_Savings__BLPM_circulator_pump",
            "__Net_Energy_Savings__BLPM_circulator_pump",
            "__Net_Demand_Savings__BLPM_circulator_pump",
            "__VFD_Gross_Prescriptive_Energy",
            "__VFD_Gross_Prescriptive_Demand",
            "__VFD_Prescriptive_Install_Cost",
            "__VFD_Net_Prescriptive_Energy",
            "__VFD_Net_Prescriptive_Demand")

    override fun computedFields() = mutableListOf<String>()

    private fun getFormMapper() = FormMapper(context, R.raw.motors)
    private fun getModel() = getFormMapper().decodeJSON()
    private fun getGFormElements() = getFormMapper().mapIdToElements(getModel())

}

