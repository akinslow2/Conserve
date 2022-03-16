package com.gemini.energy.service.device

import com.gemini.energy.domain.entity.Computable
import com.gemini.energy.service.DataHolder
import com.gemini.energy.service.IComputable
import com.gemini.energy.service.OutgoingRows
import com.gemini.energy.service.type.UsageHours
import com.gemini.energy.service.type.UsageMotors
import com.gemini.energy.service.type.UtilityRate
import com.gemini.energy.toDoubleOr
import com.gemini.energy.toIntOr
import com.gemini.energy.toStringOr
import com.google.gson.JsonElement
import io.reactivex.Observable
import org.json.JSONObject
import java.util.*

class Motors(computable: Computable<*>, utilityRateGas: UtilityRate, utilityRateElectricity: UtilityRate,
             usageHours: UsageHours, outgoingRows: OutgoingRows) :
        EBase(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows), IComputable {

    /**
     * Entry Point
     * */
    override fun compute(): Observable<Computable<*>> {
        return super.compute(extra = ({}))
    }

    companion object {

        /**
         * Conversion Factor from Horse Power to Kilo Watts
         * */
        private const val KW_CONVERSION = 0.746
        private const val MOTOR_EFFICIENCY = "motor_efficiency"

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

    }

    private var srs = 0.0
    private var mrs = 0.0
    private var nrs = 0.0
    private var hp = 0.0
    private var efficiency = 0.0

    /**
     * Suggested Alternative
     * */
//    @anthony: alternate needed for HP and Efficiency
    private var alternateHp = 100.0
    private var alternateEfficiency = 100.0

    private var peakHours = 0.0
    private var partPeakHours = 0.0
    private var offPeakHours = 0.0

    override fun setup() {
        try {
            srs = (featureData["Synchronous Rotational Speed (SRS)"].toIntOr(0)).toDouble()
            mrs = (featureData["Measured Rotational Speed (MRS)"].toIntOr(0)).toDouble()
            nrs = (featureData["Nameplate Rotational Speed (NRS)"].toIntOr(0)).toDouble()
            hp = featureData["Horsepower (HP)"].toDoubleOr(0.0)
            efficiency = featureData["Efficiency"].toDoubleOr(0.0)

            peakHours = featureData["Peak Hours"].toDoubleOr(0.0)
            partPeakHours = featureData["Part Peak Hours"].toDoubleOr(0.0)
            offPeakHours = featureData["Off Peak Hours"].toDoubleOr(0.0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Cost - Pre State
     * */
    override fun costPreState(elements: List<JsonElement?>): Double {

        val percentageLoad = if (srs - nrs == 0.0) 0.0 else (srs - mrs) / (srs - nrs)
        val powerUsed = if (efficiency == 0.0) 0.0 else hp * KW_CONVERSION * percentageLoad / efficiency
        val nemaPremium = 0.0

        var usageHours = usageHoursSpecific
        if (peakHours != 0.0 || partPeakHours != 0.0 || offPeakHours != 0.0) {
            usageHours = UsageMotors()
            usageHours.peakHours = peakHours
            usageHours.partPeakHours = partPeakHours
            usageHours.offPeakHours = offPeakHours
        }

        return costElectricity(powerUsed, usageHours, electricityRate)
    }

    private val costPremiumMotor = 697
    private val costStandardMotor = 555
    val implementationCost = (costPremiumMotor - costStandardMotor)

    /**
     * Cost - Post State
     * */
    override fun costPostState(element: JsonElement, dataHolder: DataHolder): Double {

        // @Anthony - Post State Implementation ?? Yet to determine.

        //1. Energy Savings
        val energySavings = energyPowerChange()

        //2. Demand Saving
        val demandSavings = if (usageHoursPre() == 0.0) 0.0 else energyPowerChange() / usageHoursPre()

        //3. Implementation Cost
        // ToDo - Implementation Cost would be a look up in the future.\

        val postRow = mutableMapOf<String, String>()
        postRow["__energy_savings"] = energySavings.toString()
        postRow["__demand_savings"] = demandSavings.toString()
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
    override fun energyPowerChange(): Double {
        val percentageLoadPre = if (srs - nrs == 0.0) 0.0 else (srs - mrs) / (srs - nrs)
        val percentageLoadPost =
                if (percentageLoadPre > 1.0) {
                    if (alternateHp == 0.0) 0.0 else (hp / alternateHp) * percentageLoadPre
                } else percentageLoadPre

        val horsePowerPre = if (efficiency == 0.0) 0.0 else (hp / efficiency) * percentageLoadPre
        val horsePowerPost = if (alternateEfficiency == 0.0) 0.0 else (alternateHp / alternateEfficiency) * percentageLoadPost
        val deltaHorsePower = (horsePowerPre - horsePowerPost) * KW_CONVERSION

        // ** Note :: Load shouldn't be 1 - Ideally increase the Power of the Motor and lower the load to less than 1
        // ** If the load is above 1 - Notify the User above 100 % - Replace a Higher Size
        // ** The better Rating would be a Motor of HP 7

        // ** This is what we recommended
        // Change Motor with the same Horse Power - Overload it
        // Get a new Motor which is bigger in size and of a Premium Efficient
        // Less Load and more efficiency

        val delta = deltaHorsePower * usageHoursPre()
        return delta
    }

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

    override fun preStateFields() = mutableListOf("")
    override fun postStateFields() = mutableListOf("__life_hours", "__maintenance_savings",
            "__cooling_savings", "__energy_savings")

    override fun computedFields() = mutableListOf("")

}

