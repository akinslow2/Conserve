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

class Halogen(computable: Computable<*>, utilityRateGas: UtilityRate, utilityRateElectricity: UtilityRate,
              usageHours: UsageHours, outgoingRows: OutgoingRows, private val context: Context) :
        EBase(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows), IComputable {

    /**
     * Entry Point
     * */
    override fun compute(): Observable<Computable<*>> {
        return super.compute(extra = ({ Timber.d(it) }))
    }

    //create variable here if you want to make it global to the class with private
    private var percentPowerReduced = 0.0
    private var actualWatts = 0.0
    var lampsPerFixtures = 0
    var numberOfFixtures = 0
    private var peakHours = 0.0
    private var partPeakHours = 0.0
    var offPeakHours = 0.0

    var energyAtPreState = 0.0
    var energyAtPostState = 0.0
    var currentPower = 0.0
    var postPower = 0.0

    private var bulbcost = 3
    private var seer = 10
    private var cooling = 1.0
    var electricianCost = 400

    private var alternateActualWatts = 0.0
    private var alternateNumberOfFixtures = 0
    private var alternateLampsPerFixture = 0
    var postUsageHours = 0

    fun energySavings(): Double {
        return energyAtPreState * percentPowerReduced
    }

    fun selfinstallcost(): Int {
        return bulbcost * numberOfFixtures * lampsPerFixtures
    }

    fun totalSavings(): Double {
        val lifeHours = lightingConfig(ELightingType.CFL)[ELightingIndex.LifeHours.value] as Double
        val energySavings = energyAtPreState * percentPowerReduced
        val coolingSavings = energySavings * cooling * seer
        val maintenanceSavings = lampsPerFixtures * numberOfFixtures * bulbcost * usageHoursSpecific.yearly() / lifeHours
        return energySavings + coolingSavings + maintenanceSavings
    }


    //Where you extract from user inputs and assign to variables
    override fun setup() {
        try {
            actualWatts = featureData["Actual Watts"]!! as Double
            lampsPerFixtures = featureData["Lamps Per Fixture"]!! as Int
            numberOfFixtures = featureData["Number of Fixtures"]!! as Int
            val config = lightingConfig(ELightingType.Halogen)
            percentPowerReduced = config[ELightingIndex.PercentPowerReduced.value] as Double

            peakHours = (featureData["Peak Hours"]!! as Int).toDouble()
            partPeakHours = (featureData["Part Peak Hours"]!! as Int).toDouble()
            offPeakHours = (featureData["Off Peak Hours"]!! as Int).toDouble()

            alternateActualWatts = featureData["Alternate Actual Watts"]!! as Double
            alternateNumberOfFixtures = featureData["Alternate Number of Fixtures"]!! as Int
            alternateLampsPerFixture = featureData["Alternate Lamps Per Fixture"]!! as Int

            postUsageHours = featureData["Suggested Off Peak Hours"]!! as Int

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Cost - Pre State
     * */
    override fun costPreState(element: List<JsonElement?>): Double {

        // @Anthony - Verify the Platform Implementation
        // peakHours*.504*peakPrice*powerUsed= cost at Peak rate...

        val powerUsed = actualWatts * numberOfFixtures / 1000

        val usageHours = UsageLighting()
        usageHours.peakHours = peakHours
        usageHours.partPeakHours = partPeakHours
        usageHours.offPeakHours = offPeakHours

        energyAtPreState = powerUsed * usageHours.yearly()

        return costElectricity(powerUsed, usageHours, electricityRate)
    }

    /**
     * Cost - Post State
     * */
    override fun costPostState(element: JsonElement, dataHolder: DataHolder): Double {

        Timber.d("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$")
        Timber.d("!!! COST POST STATE - HALOGEN !!!")
        Timber.d("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$")

        val lifeHours = lightingConfig(ELightingType.Halogen)[ELightingIndex.LifeHours.value] as Double

        val maintenanceSavings = lampsPerFixtures * numberOfFixtures * bulbcost * usageHoursSpecific.yearly() / lifeHours
        // Adding new variables for the report
        val selfinstallcost = bulbcost * numberOfFixtures * lampsPerFixtures

        // Delta is going to be Power Used * Percentage Power Reduced
        // Percentage Power Reduced - we get it from the Base - ELighting

        val energySavings = energyAtPreState * percentPowerReduced
        val coolingSavings = energySavings * cooling / seer

        energyAtPostState = energyAtPreState - energySavings
        val paybackmonth = selfinstallcost / energySavings * 12
        val paybackyear = selfinstallcost / energySavings
        val totalsavings = energySavings + coolingSavings + maintenanceSavings

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
     * PowerTimeChange >> Yearly Usage Hours - [Pre | Post]
     * */
    override fun usageHoursPre(): Double = 0.0
    override fun usageHoursPost(): Double = 0.0

    /**
     * PowerTimeChange >> Energy Efficiency Calculations
     * */
    override fun energyPowerChange(): Double {
        val powerUsed = actualWatts * lampsPerFixtures * numberOfFixtures / 1000
        currentPower = powerUsed
        postPower = alternateActualWatts * alternateLampsPerFixture * alternateNumberOfFixtures / 1000
        return powerUsed * percentPowerReduced
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
    override fun preAuditFields() = mutableListOf("General Client Info Name", "General Client Info Position", "General Client Info Email")
    override fun featureDataFields() = getGFormElements().map { it.value.param!! }.toMutableList()

    override fun preStateFields() = mutableListOf("")
    override fun postStateFields() = mutableListOf("__life_hours", "__maintenance_savings",
            "__cooling_savings", "__energy_savings", "__energy_at_post_state", "__selfinstall_cost",
            "__payback_month", "__payback_year", "__total_savings")

    override fun computedFields() = mutableListOf("")

    private fun getFormMapper() = FormMapper(context, R.raw.halogen)
    private fun getModel() = getFormMapper().decodeJSON()
    private fun getGFormElements() = getFormMapper().mapIdToElements(getModel())

}
