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
        private const val bulbcost = 3.0

        /**
         * Conversion Factor from Watts to Kilo Watts
         * */
        private const val KW_CONVERSION = 0.001
    }

    var electricianCost = 400
    private var actualWatts = 0.0
    private var ballastsPerFixtures = 0
    private var numberOfFixtures = 0

    private var energyAtPreState = 0.0
    private var seer = 13

    /**
     * Suggested Alternative
     * */
    private var alternateActualWatts = 0.0
    private var alternateNumberOfFixtures = 0
    private var alternateLampsPerFixture = 0
    private var alternateLifeHours = 0

    override fun setup() {
        try {
            actualWatts = featureData["Actual Watts"]!! as Double
            ballastsPerFixtures = featureData["Ballasts Per Fixture"]!! as Int
            numberOfFixtures = featureData["Number of Fixtures"]!! as Int

            alternateActualWatts = featureData["Alternate Actual Watts"]!! as Double
            alternateNumberOfFixtures = featureData["Alternate Number of Fixtures"]!! as Int
            alternateLampsPerFixture = featureData["Alternate Lamps Per Fixture"]!! as Int
            alternateLifeHours = featureData["Alternate Life Hours"]!! as Int

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Cost - Pre State
     * */
    override fun costPreState(elements: List<JsonElement?>): Double {
        val totalUnits = ballastsPerFixtures * numberOfFixtures
        val powerUsed = actualWatts * totalUnits * KW_CONVERSION

        energyAtPreState = powerUsed * usageHoursSpecific.yearly()
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
        val totalUnits= ballastsPerFixtures * numberOfFixtures
        val selfinstallcost = bulbcost * numberOfFixtures * ballastsPerFixtures
        val replacementIndex = usageHoursSpecific.yearly() / alternateLifeHours
        val maintenanceSavings = totalUnits * bulbcost * replacementIndex

        //2. Cooling Savings
        val coolingSavings = energyAtPreState * cooling * seer

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
        val totalUnitsPre = ballastsPerFixtures * numberOfFixtures
        val totalUnitsPost = alternateLampsPerFixture * alternateNumberOfFixtures

        val powerUsedPre = actualWatts *  totalUnitsPre * KW_CONVERSION
        val powerUsedPost = alternateActualWatts * totalUnitsPost * KW_CONVERSION
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
            "__cooling_savings", "__energy_savings")

    override fun computedFields() = mutableListOf("")

    private fun getFormMapper() = FormMapper(context, R.raw.linear_fluorescent)
    private fun getModel() = getFormMapper().decodeJSON()
    private fun getGFormElements() = getFormMapper().mapIdToElements(getModel())

}