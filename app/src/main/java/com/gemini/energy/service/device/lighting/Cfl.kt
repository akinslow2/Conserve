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
import org.json.JSONObject
import timber.log.Timber
import java.util.*

class Cfl(computable: Computable<*>, utilityRateGas: UtilityRate, utilityRateElectricity: UtilityRate,
          usageHours: UsageHours, outgoingRows: OutgoingRows, private val context: Context) :
        EBase(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows), IComputable {

    /**
     * Entry Point
     * */
    override fun compute(): Observable<Computable<*>> {
        return super.compute(extra = ({ Timber.d(it) }))
    }

    companion object {
        private const val LightControls = "lighting_lightingcontrols"
        private const val ControlHours = "lighting_lightingcontrolhours"

        /**
         * Fetches the Deemed Criteria at once
         * via the Parse API
         * */
        fun extractControlPercentSaved(element: JsonElement): Double {
            if (element.asJsonObject.has("percent_savings")) {
                return element.asJsonObject.get("percent_savings").asDouble
            }
            return 0.0
        }

        fun extractEquipmentCost(element: JsonElement): Double {
            if (element.asJsonObject.has("equipment_cost")) {
                return element.asJsonObject.get("equipment_cost").asDouble
            }
            return 0.0
        }

        fun extractMeasureCode(element: JsonElement): String {
            if (element.asJsonObject.has("measure_code")) {
                return element.asJsonObject.get("measure_code").asString
            }
            return ""
        }

        fun extractAssumedHours(element: JsonElement): Double {
            if (element.asJsonObject.has("hours")) {
                return element.asJsonObject.get("hours").asDouble
            }
            return 0.0
        }
    }

    //create variable here if you want to make it global to the class with private
    private var percentPowerReduced = 0.0
    private var actualWatts = 0.0
    var lampsPerFixtures = 0
    var numberOfFixtures = 0
    private var peakHours = 0.0
    private var partPeakHours = 0.0
    var offPeakHours = 0.0

    private var ControlType1 = ""
    private var ControlType2 = ""
    private var bType = ""

    var postPower = 0.0
    var postUsageHours = 0

    private val bulbcost = 1.5
    private val ledbulbcost = 3.0

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
    private var alternateActualWatts = 0.0
    private var alternateNumberOfFixtures = 0
    private var alternateLampsPerFixture = 0


    //Where you extract from user inputs and assign to variables
    override fun setup() {
        try {
            actualWatts = featureData["Actual Watts"]!! as Double
            lampsPerFixtures = featureData["Lamps Per Fixture"]!! as Int
            numberOfFixtures = featureData["Number of Fixtures"]!! as Int

            val config = lightingConfig(ELightingType.CFL)
            percentPowerReduced = config[ELightingIndex.PercentPowerReduced.value] as Double

            ControlType1 = featureData["Suggested Control Type1"]!! as String
            ControlType2 = featureData["Suggested Control Type2"]!! as String
            bType = featureData["Building Type"]!! as String

            peakHours = featureData["Peak Hours"]!! as Double
            offPeakHours = featureData["Off Peak Hours"]!! as Double

            alternateActualWatts = featureData["Alternate Actual Watts"]!! as Double
            alternateNumberOfFixtures = featureData["Alternate Number of Fixtures"]!! as Int
            alternateLampsPerFixture = featureData["Alternate Lamps Per Fixture"]!! as Int

            controls = featureData["Type of Control"]!! as String

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Time | Energy | Power - Pre State
     * */
    override fun usageHoursPre(): Double {
        val usageHours = UsageLighting()
        val preauditHours = UsageLighting()
        usageHours.peakHours = peakHours
        usageHours.partPeakHours = partPeakHours
        usageHours.offPeakHours = offPeakHours
        if (usageHours.yearly() < 1.0) {
            return preauditHours.yearly()
        } else {
            return usageHours.yearly()
        }
    }

    fun preEnergy(): Double {
        val totalUnitsPre = lampsPerFixtures * numberOfFixtures
        return actualWatts * totalUnitsPre * 0.001 * usageHoursPre()
    }

    fun prePower() = actualWatts * numberOfFixtures * lampsPerFixtures / 1000

    /**
     * Cost - Pre State
     * */
    override fun costPreState(element: List<JsonElement?>): Double {
        val usageHours = UsageLighting()
        usageHours.peakHours = peakHours
        usageHours.partPeakHours = partPeakHours
        usageHours.offPeakHours = offPeakHours

        return costElectricity(prePower(), usageHours, electricityRate)
    }

    /**
     * Cost - Post State
     * */
    override fun costPostState(element: JsonElement, dataHolder: DataHolder): Double {

        Timber.d("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$")
        Timber.d("!!! COST POST STATE - CFL !!!")
        Timber.d("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$")

        val lifeHours = lightingConfig(ELightingType.CFL)[ELightingIndex.LifeHours.value] as Double

        val totalUnits = lampsPerFixtures * numberOfFixtures

        val replacementIndex = LEDlifeHours / lifeHours
        val expectedLife = LEDlifeHours / usageHoursSpecific.yearly()
        val maintenanceSavings = totalUnits * bulbcost * replacementIndex / expectedLife

        val selfinstallcost = this.selfinstallcost()

        // Delta is going to be Power Used * Percentage Power Reduced
        // Percentage Power Reduced - we get it from the Base - ELighting

        val energySavings = preEnergy() * percentPowerReduced
        val coolingSavings = energySavings * cooling / seer

        val energyAtPostState = preEnergy() - energySavings
        val paybackmonth = selfinstallcost / energySavings * 12
        val paybackyear = selfinstallcost / energySavings
        val totalsavings = energySavings + coolingSavings + maintenanceSavings
        val controlCost = extractEquipmentCost(element)
        val measureCode = extractMeasureCode(element)
        val prescriptiveHours = extractAssumedHours(element)
        val percentSaved = extractControlPercentSaved(element)
        val prescriptiveSaved = preEnergy() * percentSaved

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
        postRow["__lighting_control_prescriptive_cost"] = controlCost.toString()
        postRow["__lighting_control_measure_code"] = measureCode
        postRow["__lighting_control_prescriptive_hours"] = prescriptiveHours.toString()
        postRow["__lighting_control_prescriptive_savings"] = prescriptiveSaved.toString()
        postRow["__lighting_control_prescriptive_percent"] = percentSaved.toString()

        dataHolder.header = postStateFields()
        dataHolder.computable = computable
        dataHolder.fileName = "${computable.zoneName}_${computable.auditScopeName}_CFL_post_state_${Date().time}.csv"
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

        if (postusageHours.yearly() > 0.0)
            return postusageHours.yearly()

        if (usageHoursPre() > 0)
            return usageHoursPre()

        return usageHoursBusiness.yearly()
    }

    /**
     * PowerTimeChange >> Energy Efficiency Calculations
     * */
    override fun energyPowerChange() = preEnergy() * (1 - percentPowerReduced)

    override fun energyTimeChange() =
            actualWatts * numberOfFixtures * lampsPerFixtures / 1000 * usageHoursPost()


    override fun energyPowerTimeChange() = prePower() * percentPowerReduced * usageHoursPost()

    fun postPower() = prePower() * (1 - percentPowerReduced)

    fun postEnergy() = preEnergy() - energySavings()

    fun energySavings() = preEnergy() * percentPowerReduced

    fun selfinstallcost() = ledbulbcost * alternateNumberOfFixtures * alternateLampsPerFixture

    fun totalEnergySavings(): Double {
        if (controls == "yes") {
            val coolingSavings = (preEnergy() - energyPowerChange()) * cooling / seer
            return (preEnergy() - energyPowerChange()) + coolingSavings
        } else if (ControlType1 != null || ControlType2 != null) {
            val coolingSavings = (preEnergy() - energyTimeChange()) * cooling / seer
            return (preEnergy() - energyTimeChange()) + coolingSavings
        } else {
            val coolingSavings = (preEnergy() - energyPowerTimeChange()) * cooling / seer
            return (preEnergy() - energyPowerTimeChange()) + coolingSavings
        }
    }

    fun totalSavings(): Double {
        if (controls == null && usageHoursPost() != null) {
            val postPower = energyPowerTimeChange() / usageHoursPost()
            val postusageHours = UsageLighting()
            postusageHours.postpeakHours = postpeakHours
            postusageHours.postpartPeakHours = postpartPeakHours
            postusageHours.postoffPeakHours = postoffPeakHours
            return costElectricity(postPower, postusageHours, electricityRate)
        } else {
            val postPower = energyPowerChange() / usageHoursPre()
            val usageHours = UsageLighting()
            usageHours.peakHours = peakHours
            usageHours.partPeakHours = partPeakHours
            usageHours.offPeakHours = offPeakHours
            return costElectricity(postPower, usageHours, electricityRate)
        }
    }

    /**
     * Energy Efficiency Lookup Query Definition
     * */
    override fun efficientLookup() = false

    override fun queryEfficientFilter() =
            "{\"\$or\":[" + queryControlPercentSaved() + "," + queryAssumedHours() + "]}"

    override fun queryControlPercentSaved() = JSONObject()
            .put("type", LightControls)
            .put("data.type", ControlType1)
            .toString()

    override fun queryControlPercentSaved2() = JSONObject()
            .put("type", LightControls)
            .put("data.type", ControlType2)
            .toString()

    override fun queryAssumedHours() = JSONObject()
            .put("type", ControlHours)
            .put("data.location_type", bType)
            .toString()

    /**
     * State if the Equipment has a Post UsageHours Hours (Specific) ie. A separate set of
     * Weekly UsageHours Hours apart from the PreAudit
     * */
    override fun usageHoursSpecific() = false

    /**
     * Define all the fields here - These would be used to Generate the Outgoing Rows or perform the Energy Calculation
     * */
    override fun preAuditFields() = mutableListOf(
            "General Client Info Name",
            "General Client Info Position",
            "General Client Info Email")

    override fun featureDataFields() = getGFormElements().map { it.value.param!! }.toMutableList()

    override fun preStateFields() = mutableListOf<String>()

    override fun postStateFields() = mutableListOf(
            "__lighting_control_measure_code",
            "__lighting_control_prescriptive_hours",
            "__lighting_control_prescriptive_cost",
            "__lighting_control_prescriptive_savings",
            "__lighting_control_prescriptive_percent",
            "__life_hours",
            "__maintenance_savings",
            "__cooling_savings",
            "__energy_savings",
            "__energy_at_post_state",
            "__selfinstall_cost",
            "__payback_month",
            "__payback_year",
            "__total_savings")

    override fun computedFields() = mutableListOf<String>()
    private fun getFormMapper() = FormMapper(context, R.raw.cfl)
    private fun getModel() = getFormMapper().decodeJSON()
    private fun getGFormElements() = getFormMapper().mapIdToElements(getModel())
}
