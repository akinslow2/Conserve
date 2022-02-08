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
import com.gemini.energy.toDoubleOr
import com.gemini.energy.toIntOr
import com.gemini.energy.toStringOr
import com.google.gson.JsonElement
import io.reactivex.Observable
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
        fun power(kbtu: Double, seer: Double) = if (seer == 0.0) 0.0 else kbtu / seer

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
    private var alternateSeer = 18.0
    private var alternateEer = 0.0
    private var alternateBtu = 0

    /**
     * Financial Calc Variables
     * */
    private var presentvaluefactor = 12.462

    //kBtu to Therms conversion
    private var thermconv =100

    /** year equipment was manufactured **/
    var year = 0


    /**
     * HVAC - British Thermal Unit
     * */
    var kbtu = 0.0

    var tons = 0.0
    private var gasInput = 1.0
    private var gasOutput = 2.0


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
    var electricutilitycompany = ""
    var gasUtilityCompany = ""
    var electricstructure = ""
    var gasstructure = ""
    var occupancy = 0

    var hvacType = ""
    var economizer = ""
    var thermotype = ""
    var uvalue = 0.15 //for details see document "U Value Calculations" in the Building Calculations Folder

    var cddThreshold = 67 //assumes five degrees from 72 desired temp - assumed values as a backstop are not useful until it is automated
    var hddThreshold = 63 //assumes five degree from 68 desired temp -  assumed values as a backstop are not useful until it is automated
    var thermostatChangeForNonBusiness = ""
    var adjustedHeatSetPoint = 0
    var adjustedCoolSetPoint = 0
    var cddBusinessHours = 0
    var hddBusinessHours = 0
    var cddCloseHours = 0
    var hddCloseHours = 0
    var cddTotal = cddBusinessHours + cddCloseHours
    var hddTotal = hddBusinessHours + hddCloseHours

    var typeOfUnit = ""
    var quantity = 0
    var insulation = ""
    var areaHeight = 0.0
    var areaWidth = 0.0
    var areaLength = 0.0

    var heatEfficiency = 0.9
    var heatElecEfficiency = 5.0
    var hspf = 10.0
    var alternateHspf = 10.0
    var namePlate = ""


    override fun setup() {
        try {
            clientname = preAudit["General Client Info Name"].toStringOr("")
            businessname = preAudit["General Client Info Business Name"].toStringOr("")
            auditmonth = preAudit["General Client Info Audit Month"].toStringOr("")
            audityear = preAudit["General Client Info Audit Year"].toStringOr("")
            clientaddress = "${preAudit["General Client Info Address Line 1"]} ${preAudit["General Client Info Address Line 2"]} ${preAudit["General Client Info Address City"]}, ${preAudit["General Client Info Address State"]} ${preAudit["General Client Info Address Zip Code"]}"
            startday = preAudit["General Client Info Assessment Start Day"].toStringOr("")
            endday = preAudit["General Client Info Assessment End Day"].toStringOr("")
            operationhours = preAudit["Operation Hours Monday Operating Hours"].toStringOr("")
            bldgarea = preAudit["Area Total (Sq.Ft.)"].toDoubleOr(0.0)
            electricutilitycompany = preAudit["Others Electric Utility Company"].toStringOr("")
            electricstructure = preAudit["Others Electric Rate Structure"].toStringOr("")
            gasUtilityCompany = preAudit["Others Gas Utility Company"].toStringOr("")
            gasstructure = preAudit["Others Gas Rate Structure"].toStringOr("")
            bldgtype = preAudit["General Client Info Facility Type"].toStringOr("")

            typeOfUnit = featureData["Type of Package Unit"].toStringOr("")
            eer = featureData["EER"].toDoubleOr(0.0)

            val inputSeer = featureData["SEER"].toDoubleOr(0.0)
            seer = if (inputSeer > 0.0) inputSeer else seer

            val altSeer = featureData["Alternate SEER"].toDoubleOr(18.0)
            alternateSeer = if (altSeer > 0.0) altSeer else alternateSeer
            val inputHeatEffElec = featureData["Heating Electric Efficiency"].toDoubleOr(0.0)
            heatElecEfficiency = if (inputHeatEffElec > 0.0) inputHeatEffElec else heatElecEfficiency

            val inputAltHspf = featureData["Alternate HSPF"].toDoubleOr(10.0)
            alternateHspf = if (inputAltHspf > 0.0) inputAltHspf else alternateHspf

            year = featureData["Year"].toIntOr(0)

            hvacType = featureData["Type of Package Unit"].toStringOr("")
            namePlate = featureData["Nameplate Readable?"].toStringOr("")
            kbtu = featureData["Cooling Capacity (kBtu/hr)"].toDoubleOr(0.0)

            gasInput = featureData["Heating Input (kBtu/hr)"].toDoubleOr(0.0)
            gasOutput = featureData["Heating Output (kBtu/hr)"].toDoubleOr(0.0)
            tons = featureData["Cooling Capacity (tons)"].toDoubleOr(0.0)

            economizer = featureData["Economizer"].toStringOr("")
            thermotype = featureData["Thermostat Type"].toStringOr("")
            quantity = featureData["Quantity"].toIntOr(0)

            val gasConserveInput = featureData["Heating Input (kBtu/hr)"].toDoubleOr(0.0)
            gasInput = if (gasConserveInput > 0.0) gasConserveInput else gasInput
            val gasConserveOutput = featureData["Heating Output (kBtu/hr)"].toDoubleOr(0.0)
            gasOutput = if(gasConserveOutput > 0.0) gasConserveOutput else gasOutput

            val inputHeatEffGas = gasOutput / gasInput
            heatEfficiency = if (inputHeatEffGas > 1.0) heatEfficiency else inputHeatEffGas

            //cddThreshold = featureData["AC Temperature Threshold - Open"].toIntOr(0)
            //hddThreshold = featureData["Heat Temperature Threshold - Open"].toIntOr(0)

            //val inputCddThreshold = featureData["AC Temperature Threshold - Open"].toIntOr(0)
            //cddThreshold = if (inputCddThreshold > 0) inputCddThreshold else cddThreshold

            //val inputHddThreshold = featureData["Heat Temperature Threshold - Open"].toIntOr(0)
            //hddThreshold = if (inputHddThreshold > 0) inputHddThreshold else hddThreshold

            cddBusinessHours = featureData["Cooling Degree Days Business Hours"].toIntOr(0)
            hddBusinessHours = featureData["Heating Degree Days Business Hours"].toIntOr(0)
            cddCloseHours = featureData["Cooling Degree Days Closed Hours"].toIntOr(0)
            hddCloseHours = featureData["Heating Degree Days Closed Hours"].toIntOr(0)

            insulation = featureData["Is there Insulation in the area?"].toStringOr("")
            areaLength = featureData["Length of Served Area"].toDoubleOr(0.0)
            areaWidth = featureData["Width of Served Area"].toDoubleOr(0.0)
            areaHeight = featureData["Height of Served Area"].toDoubleOr(0.0)

            peakHours = featureData["Peak Hours"].toDoubleOr(0.0)
            partPeakHours = featureData["Part Peak Hours"].toDoubleOr(0.0)
            offPeakHours = featureData["Off Peak Hours"].toDoubleOr(0.0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun overAge(): Int {
        return getAge(year) - 15
    }

    /**
     ** Calculations for annual demand, power, and energy for pre and post in the area that the HVAC system serves
     * Providing the internal heat gain and therefore cdd and hdd for business and non-business hours means that it account for the adjust thermostat
     * Thus unless unit is turned off during non-business hours business and non-business cdd/hdd will be combined.
     * The cdd/hdd takes into account lighting, people, and equipment. Adjusting of any of these impacts the demand.
     * */
    fun coolingkbtu(): Double {
        val hours = when (thermostatChangeForNonBusiness) {
            "Turned Off" -> cddBusinessHours
            else -> cddTotal
        }

        val area = (areaLength * areaWidth) + (2 * (areaLength * areaHeight + areaWidth * areaHeight))
        val areakBtu = 24.0 * uvalue * hours.toDouble() * area / 1000.0

        return areakBtu
    }

    fun heatingkbtu(): Double {
        val hours = when (thermostatChangeForNonBusiness) {
            "Turned Off" -> hddBusinessHours
            else -> hddTotal
        }

        val area =  (areaLength * areaWidth) + (2 * (areaLength * areaHeight + areaWidth * areaHeight))
        val areakBtu = 24.0 * uvalue * hours.toDouble() * area / 1000.0

        return areakBtu
    }

    fun energyPreGas(): Double {
            val heat = if (hvacType == "package unit") heatingkbtu() / heatEfficiency / thermconv else 0.0
            return heat
    }

    fun energyPre(): Double {
        val heat = if (hvacType == "package unit") 0.0 else heatingkbtu() / heatElecEfficiency
        val cool = coolingkbtu() / seer
        return heat + cool
    }

    var prePowerUsed = power(kbtu, seer)

    fun energyPost(): Double {
        val heat = if (alternateHspf == 0.0) 0.0 else heatingkbtu() / alternateHspf
        val cool = if (alternateSeer == 0.0) 0.0 else coolingkbtu() / alternateSeer

        return heat + cool
    }

    var postSize = if (kbtu == 0.0) tons else kbtu //This is where we would adjust for right-sizing
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

        if (offPeakHours > 0.0) return costElectricity(prePowerUsed, preUsageHours, electricityRate)
        return costElectricity(energyPre(), electricityRate) + costElectricityFromPowerAndDemand(prePowerUsed, electricityRate) + costGas(energyPreGas())
    }


    /**
     * Cost - Post State
     * */
    override fun costPostState(element: JsonElement, dataHolder: DataHolder): Double {
        Timber.d("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$")
        Timber.d("!!! COST POST STATE - HVAC !!!")
        Timber.d("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$")

        if (offPeakHours > 0.0) return costElectricity(postPowerUsed, postUsageHours, electricityRate) + costElectricityFromPowerAndDemand(postPowerUsed, electricityRate)
        return costElectricity(energyPost(), electricityRate) + costElectricityFromPowerAndDemand(postPowerUsed, electricityRate)
    }

// This is not needed @K2
    /**
     * Manually Builds the Post State Response from the Suggested Alternative

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
    * */
    /**
     * HVAC - INCENTIVES | MATERIAL COST
     * */
    override fun incentives(): Double {
        if (electricutilitycompany == "pge") {
            val powerChangeToHours =
                    if (usageHoursPre() == 0.0) 0.0
                    else energyPowerChange() / usageHoursPre()
            return energyPowerChange() * 0.15 + (powerChangeToHours) * 150
        } else return 0.0
    }


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

    override fun usageHoursPost(): Double = 0.0

    /**
     * PowerTimeChange >> Energy Efficiency Calculations
     * I need to insert the heating and cooling hours based on set-point temp, operation hours, and thermostat schedule
     * If the sensors don't pick up anything that changes the default then that is okay
     * */
    override fun energyPowerChange(): Double {

        // Step 3 : Get the Delta
        /**
        val powerPre = if (seer == 0.0) 0.0 else kbtu / seer
        val powerPost = if (alternateSeer == 0.0) 0.0 else kbtu / alternateSeer
        val pSavings = (powerPre - powerPost)

        val delta = pSavings * usageHoursPre() **/

        val eSavings = (energyPre() - energyPost())

        //Timber.d("HVAC :: Delta -- $delta")
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
