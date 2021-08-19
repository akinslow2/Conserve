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
            srs = (featureData["Synchronous Rotational Speed (SRS)"]!! as Int).toDouble()
            mrs = (featureData["Measured Rotational Speed (MRS)"]!! as Int).toDouble()
            nrs = (featureData["Nameplate Rotational Speed (NRS)"]!! as Int).toDouble()
            hp = featureData["Horsepower (HP)"]!! as Double
            efficiency = featureData["Efficiency"]!! as Double

            peakHours = featureData["Peak Hours"]!! as Double
            partPeakHours = featureData["Part Peak Hours"]!! as Double
            offPeakHours = featureData["Off Peak Hours"]!! as Double
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

    private val costPremiumMotor = 697
    private val costStandardMotor = 555
    val implementationCost = (costPremiumMotor - costStandardMotor)

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
        Timber.d("*** Percentage Load Pre :: ($percentageLoadPre)")
        Timber.d("*** Percentage Load Post :: ($percentageLoadPost)")

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

    /**
     * Define all the fields here - These would be used to Generate the Outgoing Rows or perform the Energy Calculation
     * */
    override fun preAuditFields() = mutableListOf("")
    override fun featureDataFields() = getGFormElements().map { it.value.param!! }.toMutableList()

    override fun preStateFields() = mutableListOf("")
    override fun postStateFields() = mutableListOf("__life_hours", "__maintenance_savings",
            "__cooling_savings", "__energy_savings")

    override fun computedFields() = mutableListOf("")

    private fun getFormMapper() = FormMapper(context, R.raw.motors)
    private fun getModel() = getFormMapper().decodeJSON()
    private fun getGFormElements() = getFormMapper().mapIdToElements(getModel())

}

