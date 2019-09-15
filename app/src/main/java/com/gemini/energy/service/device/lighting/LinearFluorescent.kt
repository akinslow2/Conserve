package com.gemini.energy.service.device.lighting

import android.content.Context
import com.gemini.energy.R
import com.gemini.energy.domain.entity.Computable
import com.gemini.energy.presentation.form.FormMapper
import com.gemini.energy.presentation.util.ELightingType
import com.gemini.energy.service.DataHolder
import com.gemini.energy.service.IComputable
import com.gemini.energy.service.OutgoingRows
import com.gemini.energy.service.device.EBase
import com.gemini.energy.service.type.UsageHours
import com.gemini.energy.service.type.UsageLighting
import com.gemini.energy.service.type.UtilityRate
import com.google.gson.JsonElement
import io.reactivex.Observable
import timber.log.Timber
import java.util.*

class LinearFluorescent(computable: Computable<*>, utilityRateGas: UtilityRate, utilityRateElectricity: UtilityRate,
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
         * Hypothetical Cost of Replacement for Linear Fluorescent
         * */
        private const val ledbulbcost = 12.0
        private const val bulbcost = 3.0

        /**
         * Conversion Factor from Watts to Kilo Watts
         * */
        private const val KW_CONVERSION = 0.001
    }

    private var percentPowerReduced = 0.0
    private var actualWatts = 0.0

    private var peakHours = 0.0
    private var partPeakHours = 0.0
    var offPeakHours = 0.0
    var postUsageHours = 0

    var lampsPerFixtures = 0
    var ballastsPerFixtures = 0
    var numberOfFixtures = 0
    private val LEDlifeHours = 30000

    var energyAtPreState = 0.0
    var currentPower = 0.0
    var postPower = 0.0

    private var seer = 13
    private var cooling = 1.0
    var electricianCost = 400

    /**
     * Suggested Alternative
     * */
    private var alternateActualWatts = 0.0
    private var alternateNumberOfFixtures = 0
    private var alternateLampsPerFixture = 0
    private var alternateLifeHours = 0

    fun energySavings(): Double {
        val totalUnitsPost = alternateLampsPerFixture * alternateNumberOfFixtures
        val powerUsedPost = alternateActualWatts * totalUnitsPost * KW_CONVERSION * postUsageHours

        return energyAtPreState - powerUsedPost
    }

    fun selfinstallcost(): Double {
        return ledbulbcost * numberOfFixtures * lampsPerFixtures
    }

    fun totalSavings(): Double {
        val energySavings = energyAtPreState * percentPowerReduced
        val coolingSavings = energyAtPreState * cooling / seer
        val maintenanceSavings = alternateLampsPerFixture * alternateNumberOfFixtures * ledbulbcost * postUsageHours / LEDlifeHours

        return energySavings + coolingSavings + maintenanceSavings
    }


    override fun setup() {
        try {
            actualWatts = featureData["Actual Watts"]!! as Double
            lampsPerFixtures = featureData["Lamps Per Fixture"]!! as Int
            ballastsPerFixtures = featureData["Ballasts Per Fixture"]!! as Int
            numberOfFixtures = featureData["Number of Fixtures"]!! as Int

            peakHours = (featureData["Peak Hours"]!! as Int).toDouble()
            partPeakHours = (featureData["Part Peak Hours"]!! as Int).toDouble()
            offPeakHours = (featureData["Off Peak Hours"]!! as Int).toDouble()

            alternateActualWatts = featureData["Alternate Actual Watts"]!! as Double
            alternateNumberOfFixtures = featureData["Alternate Number of Fixtures"]!! as Int
            alternateLampsPerFixture = featureData["Alternate Lamps Per Fixture"]!! as Int
            alternateLifeHours = featureData["Alternate Life Hours"]!! as Int

            postUsageHours = featureData["Suggested Off Peak Hours"]!! as Int

            val config = lightingConfig(ELightingType.LinearFluorescent)
            percentPowerReduced = config[ELightingIndex.PercentPowerReduced.value] as Double

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Cost - Pre State
     * */
    override fun costPreState(elements: List<JsonElement?>): Double {
        val totalUnits = lampsPerFixtures * numberOfFixtures
        val powerUsed = actualWatts * totalUnits * KW_CONVERSION

        val usageHours = UsageLighting()
        usageHours.peakHours = peakHours
        usageHours.partPeakHours = partPeakHours
        usageHours.offPeakHours = offPeakHours

        energyAtPreState = powerUsed * usageHours.yearly()
        Timber.d("******* Power Used :: ($powerUsed) *******")
        Timber.d("******* Energy At Pre State :: ($energyAtPreState) *******")

        return costElectricity(powerUsed, usageHoursSpecific, electricityRate)
    }

    /**
     * Cost - Post State
     * */
    override fun costPostState(element: JsonElement, dataHolder: DataHolder): Double {
        Timber.d("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$")
        Timber.d("!!! COST POST STATE - LINEAR FLUORESCENT !!!")
        Timber.d("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$")

        val config = lightingConfig(ELightingType.LinearFluorescent)
        val cooling = config[ELightingIndex.Cooling.value] as Double
        val lifeHours = config[ELightingIndex.LifeHours.value] as Double


        //1. Maintenance Savings
        val totalUnits = lampsPerFixtures * numberOfFixtures
        val selfinstallcost = ledbulbcost * numberOfFixtures * lampsPerFixtures
        val replacementIndex = LEDlifeHours / lifeHours
        val expectedLife = LEDlifeHours / usageHoursSpecific.yearly()
        val maintenanceSavings = totalUnits * bulbcost * replacementIndex / expectedLife

        //2. Cooling Savings
        val coolingSavings = energyAtPreState * cooling / seer

        //3. Energy Savings
        val energySavings = energyPowerChange()
        val energyAtPostState = energyAtPreState - energySavings

        val paybackmonth = selfinstallcost / energySavings * 12
        val paybackyear = selfinstallcost / energySavings
        val totalsavings = energySavings + coolingSavings + maintenanceSavings

        val postRow = mutableMapOf<String, String>()
        postRow["__life_hours"] = alternateLifeHours.toString()
        postRow["__maintenance_savings"] = maintenanceSavings.toString()
        postRow["__cooling_savings"] = coolingSavings.toString()
        postRow["__energy_savings"] = energySavings.toString()
        postRow["__energy_at_post_state"] = energyAtPostState.toString()
        postRow["__selfinstall_cost"] = selfinstallcost.toString()
        postRow["__payback_month"] = paybackmonth.toString()
        postRow["__payback_year"] = paybackyear.toString()
        postRow["__total_savings"] = totalsavings.toString()

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
     * Pre and Post are the same for Refrigerator - 24 hrs
     * */
    override fun usageHoursPre(): Double = usageHoursSpecific.yearly()
    override fun usageHoursPost(): Double = usageHoursSpecific.yearly()

    /**
     * PowerTimeChange >> Energy Efficiency Calculations
     * */
    override fun energyPowerChange(): Double {
        val totalUnitsPre = lampsPerFixtures * numberOfFixtures
        val totalUnitsPost = alternateLampsPerFixture * alternateNumberOfFixtures

        val powerUsedPre = actualWatts * totalUnitsPre * KW_CONVERSION
        currentPower = powerUsedPre
        val powerUsedPost = alternateActualWatts * totalUnitsPost * KW_CONVERSION
        postPower = powerUsedPost
        val delta = powerUsedPre - powerUsedPost

        return delta * usageHoursPre()
    }

    override fun energyTimeChange(): Double = 0.0
    override fun energyPowerTimeChange(): Double = 0.0

    /**
     * Energy Efficiency Lookup Query Definition
     * */
    override fun efficientLookup() = false
    override fun queryEfficientFilter() = ""

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
            "__cooling_savings", "__energy_savings", "__energy_at_post_state", "__selfinstall_cost",
            "__payback_month", "__payback_year", "__total_savings")

    override fun computedFields() = mutableListOf("")

    private fun getFormMapper() = FormMapper(context, R.raw.linear_fluorescent)
    private fun getModel() = getFormMapper().decodeJSON()
    private fun getGFormElements() = getFormMapper().mapIdToElements(getModel())

}