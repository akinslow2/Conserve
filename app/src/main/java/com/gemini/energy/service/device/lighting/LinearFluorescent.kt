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
        private const val bulbcost = 4.0

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


    var energyAtPreState = 0.0
    var currentPower = 0.0
    var postPower = 0.0

    private val bulbcost = 4.0
    private val ledbulbcost = 7.0

    private val LEDlifeHours = 30000
    private var seer = 10
    private var cooling = 3.142

    private var timeperfixture = 0.33
    private var electricanHourlyRate = 25

    var electricianCost = timeperfixture * numberOfFixtures * electricanHourlyRate

    private var controls = ""
    var postpeakHours = 0.0
    var postpartPeakHours = 0.0
    var postoffPeakHours = 0.0

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
            lampsPerFixtures = featureData["Lamps Per Fixture"]!! as Int
            ballastsPerFixtures = featureData["Ballasts Per Fixture"]!! as Int
            numberOfFixtures = featureData["Number of Fixtures"]!! as Int

            alternateActualWatts = featureData["Alternate Actual Watts"]!! as Double
            alternateNumberOfFixtures = featureData["Alternate Number of Fixtures"]!! as Int
            alternateLampsPerFixture = featureData["Alternate Lamps Per Fixture"]!! as Int
            alternateLifeHours = featureData["Alternate Life Hours"]!! as Int

            val config = lightingConfig(ELightingType.LinearFluorescent)
            percentPowerReduced = config[ELightingIndex.PercentPowerReduced.value] as Double
            postpeakHours = featureData["Suggested Peak Hours"]!! as Double
            postpartPeakHours = featureData["Suggested Part Peak Hours"]!! as Double
            postoffPeakHours = featureData["Suggested Off Peak Hours"]!! as Double

            controls = featureData["Type of Control"]!! as String

            peakHours = featureData["Peak Hours"]!! as Double
            partPeakHours = featureData["Part Peak Hours"]!! as Double
            offPeakHours = featureData["Off Peak Hours"]!! as Double

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    /**
     * Time | Energy | Power - Pre State
     * */
    override fun usageHoursPre(): Double {
        val usageHours = UsageLighting()
        val preauditHours = UsageHours()
        usageHours.peakHours = peakHours
        usageHours.partPeakHours = partPeakHours
        usageHours.offPeakHours = offPeakHours
        usageHours.build()
        preauditHours.build()
        if (usageHours.yearly() < 1.0){
            return  preauditHours.yearly()}
        else { return usageHours.yearly()}
    }

    fun preEnergy(): Double {
        val totalUnitsPre = lampsPerFixtures * numberOfFixtures
        return actualWatts * totalUnitsPre * 0.001 * usageHoursPre()
    }

    fun prePower(): Double {
        return actualWatts * numberOfFixtures * lampsPerFixtures / 1000
    }
    /**
     * Cost - Pre State
     * */
    override fun costPreState(element: List<JsonElement?>): Double {

        val usageHours = UsageLighting()
        usageHours.peakHours = peakHours
        usageHours.partPeakHours = partPeakHours
        usageHours.offPeakHours = offPeakHours
        usageHours.build()

        return costElectricity(prePower(), usageHours, electricityRate)
    }




    /**
     * Cost - Post State
     * */
    override fun costPostState(element: JsonElement, dataHolder: DataHolder): Double {

        Timber.d("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$")
        Timber.d("!!! COST POST STATE - Linear Fluorescent !!!")
        Timber.d("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$")

        val lifeHours = lightingConfig(ELightingType.LinearFluorescent)[ELightingIndex.LifeHours.value] as Double

        val totalUnits = lampsPerFixtures * numberOfFixtures

        val replacementIndex = LEDlifeHours / lifeHours
        val expectedLife = LEDlifeHours / usageHoursSpecific.yearly()
        val maintenanceSavings = totalUnits * bulbcost * replacementIndex / expectedLife

        // Adding new variables for the report
        val selfinstallcost = this.selfinstallcost()

        // Delta is going to be Power Used * Percentage Power Reduced
        // Percentage Power Reduced - we get it from the Base - ELighting

        val energySavings = prePower() * percentPowerReduced
        val coolingSavings = energySavings * cooling / seer

        val energyAtPostState = preEnergy() - energySavings
        val paybackmonth = selfinstallcost / energySavings * 12
        val paybackyear = selfinstallcost / energySavings
        val totalsavings =  totalSavings()

        val postRow = mutableMapOf<String, String>()
        postRow["__life_hours"] = lifeHours.toString()
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
     * Post Yearly Usage Hours
     * */


    override fun usageHoursPost(): Double {
        val postusageHours = UsageLighting()
        postusageHours.postpeakHours = postpeakHours
        postusageHours.postpartPeakHours = postpartPeakHours
        postusageHours.postoffPeakHours = postoffPeakHours
        postusageHours.build()

        if (postusageHours.yearly() > 0.0)
            return postusageHours.yearly()

        if (usageHoursPre() > 0)
            return usageHoursPre()

        return usageHoursBusiness.yearly()
    }

    /**
     * PowerTimeChange >> Energy Efficiency Calculations
     * */
    override fun energyPowerChange(): Double {
        return preEnergy() * (1 - percentPowerReduced)
    }

    override fun energyTimeChange(): Double {
        return  actualWatts * numberOfFixtures * lampsPerFixtures / 1000 * usageHoursPost()

    }
    override fun energyPowerTimeChange(): Double {
        return prePower() * percentPowerReduced * usageHoursPost()
    }

    fun postPower(): Double {
        return prePower() * (1 - percentPowerReduced)
    }

    fun postEnergy(): Double {
        return preEnergy() - energySavings()
    }
    fun energySavings(): Double {
        return preEnergy() * percentPowerReduced
    }

    fun selfinstallcost(): Double {
        return ledbulbcost * alternateNumberOfFixtures * alternateLampsPerFixture
    }
    fun totalEnergySavings(): Double {
        if (controls != null) {
            val coolingSavings = (preEnergy() - energyPowerChange()) * cooling / seer
            return (preEnergy() - energyPowerChange()) + coolingSavings
        } else {
            val coolingSavings = (preEnergy() - energyPowerTimeChange()) * cooling / seer
            return (preEnergy() - energyPowerTimeChange()) + coolingSavings
        }

    }

    fun totalSavings(): Double {
        val lifeHours = lightingConfig(ELightingType.LinearFluorescent)[ELightingIndex.LifeHours.value] as Double
        val totalUnits = lampsPerFixtures * numberOfFixtures
        val replacementIndex = LEDlifeHours / lifeHours
        val expectedLife = LEDlifeHours / usageHoursSpecific.yearly()
        val maintenanceSavings = totalUnits * bulbcost * replacementIndex / expectedLife
        val energySavings = prePower() * percentPowerReduced
        val coolingSavings = energySavings * cooling / seer

        val usageHours = UsageLighting()
        usageHours.peakHours = peakHours
        usageHours.partPeakHours = partPeakHours
        usageHours.offPeakHours = offPeakHours
        usageHours.build()
        val totalenergySavings = (energySavings + coolingSavings)
        val energycostSavings = costElectricity(totalenergySavings, usageHours, electricityRate)
        return energycostSavings * 8 + maintenanceSavings;
    }

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

    private fun getFormMapper() = FormMapper(context, R.raw.linearfluorescent)
    private fun getModel() = getFormMapper().decodeJSON()
    private fun getGFormElements() = getFormMapper().mapIdToElements(getModel())

}