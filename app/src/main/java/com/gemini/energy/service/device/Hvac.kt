package com.gemini.energy.service.device

import android.content.Context
import com.gemini.energy.R
import com.gemini.energy.domain.entity.Computable
import com.gemini.energy.presentation.form.FormMapper
import com.gemini.energy.service.DataHolder
import com.gemini.energy.service.IComputable
import com.gemini.energy.service.OutgoingRows
import com.gemini.energy.service.type.UsageHours
import com.gemini.energy.service.type.UsageSimple
import com.gemini.energy.service.type.UtilityRate
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.reactivex.Observable
import io.reactivex.Single
import org.json.JSONObject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class Hvac(computable: Computable<*>, utilityRateGas: UtilityRate, utilityRateElectricity: UtilityRate,
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
         * Conversion Factor from Watts to Kilo Watts
         * */
        private const val KW_CONVERSION = 0.001

        private const val HVAC_EER = "hvac_eer"
        private const val HVAC_COOLING_HOURS = "cooling_hours"
        private const val HVAC_EFFICIENCY = "hvac_efficiency"

        private const val HVAC_DB_BTU = "size_btu_hr"
        private const val HVAC_DB_EER = "eer"

        /**
         * Fetches the EER based on the specific Match Criteria via the Parse API

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

        /**
         * Fetches the Hours based on the City
         * @Anthony - Verify where we are using the Extracted Hours ??
         * This is used to calculate the Energy
         * */
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
         * HVAC - Power Consumed
         * There could be a case where the User will input the value in KW - If that happens we need to convert the KW
         * int kbtu / hr :: 1KW equals 3412.142
         * */
        fun power(kbtu: Double, seer: Double) = if (seer == 0.0) 0.0 else (kbtu / seer)

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

    /**
     * HVAC - Energy Efficiency Ratio
     * If not available - Build a match criteria at queryHVACEer()
     * 1. Primary Match - [year equals (Current Year minus 20)]
     * 2. Secondary Match - [size_btu_per_hr_min > kbtu < size_btu_per_hr_max]
     * */
    private var eer = 0.0
    var seer = 9.0
    private var alternateSeer = 16.0
    private var alternateEer = 0.0
    private var alternateBtu = 0

    /**
     * Financial Calc Variables
     * */
    private var presentvaluefactor = 12.462

    /** year equipment was manufactured **/
    var year = 0


    /**
     * HVAC - British Thermal Unit
     * */
    var kbtu = 0.0
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
    private var peakHours = 0.0
    private var partPeakHours = 0.0
    private var offPeakHours = 0.0

    /**
     * Pre Audit Variables
     */

    var clientname = ""
    var clientaddress = ""
    var businessname = ""
    var auditmonth = ""
    var audityear = ""
    var startday = ""
    var endday = ""
    var operationhours = ""
    var bldgarea = 0.0
    var bldgtype = ""
    var utilitycompany = ""
    var electricstructure = ""
    var gasstructure = ""
    var economizer = ""
    var thermotype = ""
    var uvalue = 0.15 //for details see document "U Value Calculations" in the Building Calculations Folder

    var cddThreshold = 67 //assumes five degrees from 72 desired temp
    var hddThreshold = 63 //assumes five degree from 68 desired temp
    var cddTotal = 0
    var hddTotal = 0
    var thermostatChangeForNonBusiness = ""
    var cddBusinessHours = 0
    var hddBusinessHours = 0
    var cddCloseHours = 0
    var hddCloseHours = 0

    var quantity = 0
    var insulation = ""
    var areaHeight = 0.0
    var areaWidth = 0.0
    var areaLength = 0.0
    var heatEfficiency = 0.9
    var hspf = 8.0

    override fun setup() {
        try {

            clientname = preAudit["General Client Info Name"]!! as String
            businessname = preAudit["General Client Info Business Name"]!! as String
            auditmonth = preAudit["General Client Info Audit Month"]!! as String
            audityear = preAudit["General Client Info Audit Year"]!! as String
            clientaddress = "${preAudit["General Client Info Address Line 1"]} ${preAudit["General Client Info Address Line 2"]} ${preAudit["General Client Info Address City"]}, ${preAudit["General Client Info Address State"]} ${preAudit["General Client Info Address Zip Code"]}"
            startday = preAudit["General Client Info Assessment Start Day"]!! as String
            endday = preAudit["General Client Info Assessment End Day"]!! as String
            operationhours = preAudit["Operation Hours Monday Operating Hours"]!! as String
            bldgarea = preAudit["Area Total (Sq.Ft.)"]!! as Double
            utilitycompany = preAudit["Others Electric Utility Company"]!! as String
            electricstructure = preAudit["Others Electric Rate Structure"]!! as String
            gasstructure = preAudit["Others Gas Rate Structure"]!! as String
            bldgtype = preAudit["General Client Info Facility Type"]!! as String

            eer = featureData["EER"]!! as Double
            val inputSeer = featureData["SEER"]!! as Double
            seer = if (inputSeer > 0.0) inputSeer else seer
            year = featureData["Year"]!! as Int
            kbtu = featureData["Cooling Capacity (kBtu/hr)"]!! as Double
            gasInput = featureData["Heating Input (kBtu/hr)"]!! as Double
            gasOutput = featureData["Heating Output (kBtu/hr)"]!! as Double
            economizer = featureData["Economizer"]!! as String
            thermotype = featureData["Thermostat Type"]!! as String
            quantity = featureData["Quantity"]!! as Int

            val inputCddThreshold = featureData["AC Temperature Threshold - Open"]!! as Int
            cddThreshold = if (inputCddThreshold > 0) inputCddThreshold else cddThreshold

            val inputHddThreshold = featureData["Heat Temperature Threshold - Open"]!! as Int
            hddThreshold = if (inputHddThreshold > 0) inputHddThreshold else hddThreshold

            thermostatChangeForNonBusiness = featureData["How is thermostat changed for non-business?"]!! as String
            cddBusinessHours = featureData["Cooling Degree Days Business Hours"]!! as Int
            hddBusinessHours = featureData["Heating Degree Days Business Hours"]!! as Int
            cddCloseHours = featureData["Cooling Degree Days Closed Hours"]!! as Int
            hddCloseHours = featureData["Heating Degree Days Closed Hours"]!! as Int

            insulation = featureData["Is there Insulation in the area?"]!! as String
            areaLength = featureData["Length of Served Area"]!! as Double
            areaWidth = featureData["Width of Served Area"]!! as Double
            areaHeight = featureData["Height of Served Area"]!! as Double

            peakHours = featureData["Peak Hours"]!! as Double
            partPeakHours = featureData["Part Peak Hours"]!! as Double
            offPeakHours = featureData["Off Peak Hours"]!! as Double

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun overAge(): Int {
        return getAge(year) - 15
    }

    /**
     * * Calculations for annual demand, power, and energy for pre and post in the area that the HVAC system serves
     * */
    fun coolingkbtu(): Double {
        val hours = when (thermostatChangeForNonBusiness) {
            "No Change" -> cddTotal
            "Turned Off" -> cddBusinessHours
            else -> cddBusinessHours + cddCloseHours
        }

        val area = (areaLength * areaWidth) + (2 * (areaLength * areaHeight + areaWidth * areaHeight))
        val areakBtu = 24.0 * uvalue * hours.toDouble() * area / 1000.0

        return areakBtu
    }

    fun heatingkbtu(): Double {
        val hours = when (thermostatChangeForNonBusiness) {
            "No Change" -> hddTotal
            "Turned Off" -> hddBusinessHours
            else -> hddBusinessHours + hddCloseHours
        }

        val area =  (areaLength * areaWidth) + (2 * (areaLength * areaHeight + areaWidth * areaHeight))
        val areakBtu = 24.0 * uvalue * hours.toDouble() * area / 1000.0

        return areakBtu
    }


    fun energyPre(): Double {
        val heat = if (heatEfficiency == 0.0) 0.0 else heatingkbtu() / heatEfficiency
        val cool = if (seer == 0.0) 0.0 else coolingkbtu() / seer
        return heat + cool
    }

    var prePowerUsed = power(kbtu, seer)

    fun energyPost(): Double {
        val heat = if (hspf == 0.0) 0.0 else heatingkbtu() / hspf
        val cool = if (alternateSeer == 0.0) 0.0 else coolingkbtu() / alternateSeer
        return heat + cool
    }

    var postSize = kbtu
    var postSEER = alternateSeer
    val postPowerUsed = power(postSize, postSEER)

    val preUsageHours = UsageSimple(peakHours, partPeakHours, offPeakHours)
    val postUsageHours = UsageSimple(peakHours, partPeakHours, offPeakHours)

    val powerDelta = prePowerUsed - postPowerUsed

    /**
     * Cost - Pre State
     * */
    override fun costPreState(elements: List<JsonElement?>): Double {

        Timber.d("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$")
        Timber.d("!!! COST PRE STATE - HVAC !!!")
        Timber.d("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$")
/*
        Extracting the EER from the Database - Standard EER
        If no value has been inputted by the user
       if (eer == 0.0) {
        eer = extractEER(elements)
       }

        Timber.d("::: PARAM - HVAC :::")
        Timber.d("EER -- $eer")
        Timber.d("AGE -- $age")
        Timber.d("kbtu -- $kbtu")
        Timber.d("YEAR -- ${getYear(age)}")

        Timber.d("::: DATA EXTRACTOR - HVAC :::")
        Timber.d(elements.toString())

        val usageHours = UsageSimple(peakHours, partPeakHours, offPeakHours)
        computable.udf1 = usageHours
        Timber.d(usageHours.toString())

        val powerUsedCurrent = power(kbtu, seer)
        val powerUsedStandard = power(kbtu, eer)

        val powerUsed = if (seer == null) powerUsedStandard else powerUsedCurrent
        Timber.d("HVAC :: Power Used (Current) -- [$powerUsedCurrent]")
        Timber.d("HVAC :: Power Used (Standard) -- [$powerUsedStandard]")

        Timber.d("HVAC :: Pre Power Used -- [$powerUsed]")
  */
        if (offPeakHours > 0.0) return costElectricity(prePowerUsed, preUsageHours, electricityRate)
        return costElectricity(energyPre(), electricityRate) + costElectricityFromPowerAndDemand(prePowerUsed, electricityRate)
    }


    /**
     * Cost - Post State
     * */
    override fun costPostState(element: JsonElement, dataHolder: DataHolder): Double {

        Timber.d("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$")
        Timber.d("!!! COST POST STATE - HVAC !!!")
        Timber.d("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$")


        // try {
        //     postSize = element.asJsonObject.get(HVAC_DB_BTU).asInt
        //    postEER = element.asJsonObject.get(HVAC_DB_EER).asDouble
        //} catch (e: Exception) {
        //    e.printStackTrace()
        //}


        if (offPeakHours > 0.0) return costElectricity(postPowerUsed, postUsageHours, electricityRate) + costElectricityFromPowerAndDemand(postPowerUsed, electricityRate)

        return costElectricity(energyPost(), electricityRate) + costElectricityFromPowerAndDemand(postPowerUsed, electricityRate)
    }

    /**
     * Manually Builds the Post State Response from the Suggested Alternative
     * */
    override fun buildPostState(): Single<JsonObject> {
        val element = JsonObject()
        val data = JsonObject()
        data.addProperty("eer", firstNotNull(alternateSeer, alternateEer))
        data.addProperty("size_btu_hr", alternateBtu)

        element.add("data", data)
        element.addProperty("type", HVAC_EFFICIENCY)

        val response = JsonArray()
        response.add(element)

        val wrapper = JsonObject()
        wrapper.add("results", response)

        return Single.just(wrapper)
    }

    /**
     * HVAC - INCENTIVES | MATERIAL COST
     * */
    override fun incentives(): Double {
        if (utilitycompany == "pge") {
            val powerChangeOverHoursPre = if (usageHoursPre() == 0.0) 0.0 else energyPowerChange() / usageHoursPre()
            return energyPowerChange() * 0.15 + powerChangeOverHoursPre * 150
        } else return 0.0
    }

    //@K2 does kbtu have to be a double to be in the equation below? Right now it is an INT
//    @anthony it will need to be a double
    // anytime you are returning a double, all values in the equation must be doubles
    override fun materialCost(): Double {
        return 4000.0 * (kbtu / 12000.0)
    }

    override fun laborCost(): Double {
        return 0.0
    }

    /*
        Net Present Value
     */
    val netPresentValue = (presentvaluefactor * (energyPre() - energyPost())) - materialCost()

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
    override fun usageHoursPre(): Double {
        return peakHours + partPeakHours + offPeakHours
    }

    override fun usageHoursPost(): Double {
        // TODO: check if hours should change
        return usageHoursPre()
    }


    /**
     * PowerTimeChange >> Energy Efficiency Calculations
     * I need to insert the heating and cooling hours based on set-point temp, operation hours, and thermostat schedule
     * If the sensors don't pick up anything that changes the default then that is okay
     * */
    override fun energyPowerChange(): Double {

        // Step 3 : Get the Delta
        val powerPre = if (seer == 0.0) 0.0 else kbtu / seer
        val powerPost = if (alternateSeer == 0.0) 0.0 else kbtu / alternateSeer
        val pSavings = (powerPre - powerPost)

        val delta = pSavings * usageHoursPre()

        val eSavings = (energyPre() - energyPost())

        Timber.d("HVAC :: Delta -- $delta")
        Timber.d("HVAC :: Delta -- $eSavings")

        return eSavings
    }

    fun totalSavings(): Double {

        return costElectricity(energyPowerChange(), electricityRate) + costElectricityFromPowerAndDemand(powerDelta, electricityRate)
    }


    override fun energyTimeChange(): Double = 0.0
    override fun energyPowerTimeChange(): Double = 0.0


    /**
     * Energy Efficiency Lookup Query Definition
     * */
    override fun efficientLookup() = (firstNotNull(alternateSeer, alternateEer) == 0.0 || alternateBtu == 0)
    override fun queryEfficientFilter() = JSONObject()
            .put("type", HVAC_EFFICIENCY)
            .put("data.size_btu_hr", kbtu)
            .toString()

    /**
     * HVAC specific Query Builders
     * */
    override fun queryHVACEer() = JSONObject()
            .put("type", HVAC_EER)
            .put("data.year", getYear(year))
            .put("data.size_btu_per_hr_min", JSONObject().put("\$lte", kbtu))
            .put("data.size_btu_per_hr_max", JSONObject().put("\$gte", kbtu))
            .toString()

    override fun queryHVACCoolingHours() = JSONObject()
            .put("type", HVAC_COOLING_HOURS)
            .put("data.city", city)
            .put("data.state", state)
            .toString()

    /**
     * State if the Equipment has a Post UsageHours Hours (Specific) ie. A separate set of
     * Weekly UsageHours Hours apart from the PreAudit
     * */
    override fun usageHoursSpecific() = false

    /**
     * Define all the fields here - These would be used to Generate the Outgoing Rows or perform the Energy Calculation
     * */
    override fun preAuditFields() = mutableListOf("General Client Info Name",
            "General Client Info Position", "General Client Info Email",
            "General Client Info Business Name", "General Client Info Audit Month",
            "General Client Info Audit Year", "General Client Info Address",
            "General Client Info Assessment Start Day", "General Client Info Assessment End Day",
            "Operation Hours Monday Operating Hours", "Area Total (Sq.Ft.)",
            "Others Utility Company", "Others Electric Rate Structure", "Others Gas Rate Structure",
            "General Client Info Facility Type")

    override fun featureDataFields() = getGFormElements().map { it.value.param!! }.toMutableList()

    override fun preStateFields() = mutableListOf("")
    override fun postStateFields() = mutableListOf("__life_hours", "__maintenance_savings",
            "__cooling_savings", "__energy_savings", "__energy_at_post_state")

    override fun computedFields() = mutableListOf("")

    private fun getFormMapper() = FormMapper(context, R.raw.hvac)
    private fun getModel() = getFormMapper().decodeJSON()
    private fun getGFormElements() = getFormMapper().mapIdToElements(getModel())

}
