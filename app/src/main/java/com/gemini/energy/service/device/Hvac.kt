package com.gemini.energy.service.device

import android.content.Context
import com.gemini.energy.R
import com.gemini.energy.domain.entity.Computable
import com.gemini.energy.presentation.form.FormMapper
import com.gemini.energy.service.DataHolder
import com.gemini.energy.service.IComputable
import com.gemini.energy.service.OutgoingRows
import com.gemini.energy.service.type.UsageHours
import com.gemini.energy.service.type.UsageLighting
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

        fun extractThermostatreplacementkWh(element: JsonElement): Double {
            if (element.asJsonObject.has("total_kwh"))
                return element.asJsonObject.get("total_kwh").asDouble
            return 0.0
        }

        fun extractThermostatreplacementkW(element: JsonElement): Double {
            if (element.asJsonObject.has("kw"))
                return element.asJsonObject.get("kw").asDouble
            return 0.0
        }

        fun extractThermostatreplacementCost(element: JsonElement): Double {
            if (element.asJsonObject.has("total_cost"))
                return element.asJsonObject.get("total_cost").asDouble
            return 0.0
        }

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
         * int BTU / hr :: 1KW equals 3412.142
         * */
        fun power(btu: Int, seer: Double) = (btu / seer)

        /**
         * Year At - Current minus the Age
         * */
        private val dateFormatter = SimpleDateFormat("yyyy", Locale.ENGLISH)
// TODO: @k2interactive Not sure if this is doing this but I need this to take a year and provide me with the age. So if I insert 1990 as the input variable for age it will tell me it is 30 years old.
        fun getYear(age: Int): Int {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.YEAR, "-$age".toInt()) //** Subtracting the Age **
            return dateFormatter.format(calendar.time).toInt()
        }

        fun firstNotNull (valueFirst: Double, valueSecond: Double) =
                if (valueFirst == 0.0) valueSecond else valueFirst

        fun firstNotNull (valueFirst: Int, valueSecond: Int) =
                if (valueFirst == 0) valueSecond else valueFirst
    }

    /**
     * HVAC - Energy Efficiency Ratio
     * If not available - Build a match criteria at queryHVACEer()
     * 1. Primary Match - [year equals (Current Year minus 20)]
     * 2. Secondary Match - [size_btu_per_hr_min > BTU < size_btu_per_hr_max]
     * */
    private var eer = 0.0
    var seer = 11.0
    private var alternateSeer = 17.0
    private var alternateEer = 0.0
    private var alternateBtu = 0

    /**
     * HVAC - Age
     * */
    var age = 0


    /**
     * HVAC - British Thermal Unit
     * */
    var btu = 0
    private var gasInput = 0
    private var gasOutput = 0


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
    var mondayOperationHours = ""
    var tuesdayOperationHours = ""
    var wednesdayOperationHours = ""
    var thursdayOperationHours = ""
    var fridayOperationHours = ""
    var saturdayOperationHours = ""
    var sundayOperationHours = ""
    var bldgarea = 0.0
    var bldgtype = ""
    var utilitycompany = ""
    var electricstructure = ""
    var gasstructure = ""
    var economizer = ""
    var thermotype = ""

    private var heatingFuel = ""
    private var coolingFuel = ""

    var quantity = 0

    override fun setup() {
        try {
            clientname = preAudit["General Client Info Name"]!! as String
            businessname = preAudit["General Client Info Business Name"]!! as String
            auditmonth = preAudit["General Client Info Audit Month"]!! as String
            audityear = preAudit["General Client Info Audit Year"]!! as String
            clientaddress = preAudit["General Client Info Address"]!! as String
            startday = preAudit["General Client Info Assessment Start Day"]!! as String
            endday = preAudit["General Client Info Assessment End Day"]!! as String
            operationhours = preAudit["Operation Hours Monday Operating Hours"]!! as String

            mondayOperationHours = preAudit["Operation Hours Monday Operating Hours"]!! as String
            tuesdayOperationHours = preAudit["Operation Hours Tuesday Operating Hours"]!! as String
            wednesdayOperationHours = preAudit["Operation Hours Wednesday Operating Hours"]!! as String
            thursdayOperationHours = preAudit["Operation Hours Thursday Operating Hours"]!! as String
            fridayOperationHours = preAudit["Operation Hours Friday Operating Hours"]!! as String
            saturdayOperationHours = preAudit["Operation Hours Saturday Operating Hours"]!! as String
            sundayOperationHours = preAudit["Operation Hours Sunday Operating Hours"]!! as String

            bldgarea = preAudit["Area Total (Sq.Ft.)"]!! as Double
            utilitycompany = preAudit["Others Electric Utility Company"]!! as String
            electricstructure = preAudit["Others Electric Rate Structure"]!! as String
            gasstructure = preAudit["Others Gas Rate Structure"]!! as String
            bldgtype = preAudit["General Client Info Facility Type"]!! as String

            heatingFuel = featureData["Heat Fuel"]!! as String
            coolingFuel = featureData["Cool Fuel"]!! as String

            eer = featureData["EER"]!! as Double
            seer = featureData["SEER"]!! as Double
            age = featureData["Age installed (years)"]!! as Int
            btu = featureData["Cooling Capacity (Btu/hr)"]!! as Int
            gasInput = featureData["Heating Input (Btu/hr)"]!! as Int
            gasOutput = featureData["Heating Output (Btu/hr)"]!! as Int
           economizer = featureData["Economizer"]!! as String
//            thermotype = featureData["Thermostat Type"]!! as String
            quantity = featureData["Quantity"]!! as Int

//            city = featureData["City"]!! as String
//            state = featureData["State"]!! as String

            peakHours = featureData["Peak Hours"]!! as Double
            offPeakHours = featureData["Off Peak Hours"]!! as Double

            alternateSeer = featureData["Alternate SEER"]!! as Double
//            alternateEer = featureData["Alternate EER"]!! as Double
            alternateBtu = featureData["Alternate Cooling Capacity (Btu/hr)"]!! as Int
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Getting year of device and how much over life it is
     */
    fun year(): Int {
        return getYear(age)
    }

    fun overAge(): Int {
        return age - 15
    }

    /**
     * Cost - Pre State
     * */
    override fun costPreState(elements: List<JsonElement?>): Double {

        // Extracting the EER from the Database - Standard EER
        // If no value has been inputted by the user
       // if (eer == 0.0) {
        //    eer = extractEER(elements)
       // }

        Timber.d("::: PARAM - HVAC :::")
        Timber.d("EER -- $eer")
        Timber.d("AGE -- $age")
        Timber.d("BTU -- $btu")
        Timber.d("YEAR -- ${getYear(age)}")

        Timber.d("::: DATA EXTRACTOR - HVAC :::")
        Timber.d(elements.toString())

        val usageHours = UsageSimple(peakHours, partPeakHours, offPeakHours)
        computable.udf1 = usageHours
        Timber.d(usageHours.toString())

        val powerUsedCurrent = power(btu, seer)
        val powerUsedStandard = power(btu, eer)

        val powerUsed = if (seer == null) powerUsedStandard else powerUsedCurrent
        Timber.d("HVAC :: Power Used (Current) -- [$powerUsedCurrent]")
        Timber.d("HVAC :: Power Used (Standard) -- [$powerUsedStandard]")

        Timber.d("HVAC :: Pre Power Used -- [$powerUsed]")


        return costElectricity(powerUsed, usageHours, electricityRate)
    }


    /**
     * Cost - Post State
     * */
    override fun costPostState(element: JsonElement, dataHolder: DataHolder): Double {

        Timber.d("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$")
        Timber.d("!!! COST POST STATE - HVAC !!!")
        Timber.d("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$")

        val postSize = btu
        val postSEER = 17.0

        val presciptive_kW_savings = extractThermostatreplacementkW(element)
        val presciptive_kWh_savings = extractThermostatreplacementkWh(element)
        val implementationCost = extractThermostatreplacementCost(element)

        val postRow = mutableMapOf<String, String>()
        postRow["__prescriptive_kWh_savings"] = presciptive_kWh_savings.toString()
        postRow["__prescriptive_kW_savings"] = presciptive_kW_savings.toString()
        postRow["__prescriptive_implementation_cost"] = implementationCost.toString()

        dataHolder.header = postStateFields()
        dataHolder.computable = computable
        dataHolder.fileName = "${Date().time}_post_state.csv"
        dataHolder.rows?.add(postRow)

       // try {
       //     postSize = element.asJsonObject.get(HVAC_DB_BTU).asInt
        //    postEER = element.asJsonObject.get(HVAC_DB_EER).asDouble
        //} catch (e: Exception) {
        //    e.printStackTrace()
        //}

        val postPowerUsed = power(postSize, postSEER)

        val postUsageHours = UsageSimple(peakHours, partPeakHours, offPeakHours)


        return costElectricity(postPowerUsed, postUsageHours, electricityRate)
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
        return 0.0
    }

    override fun materialCost(): Double {
        return 7000.0
    }

    override fun laborCost(): Double {
        return 0.0
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
    override fun usageHoursPre(): Double {
        return peakHours + partPeakHours + offPeakHours
    }
    override fun usageHoursPost(): Double = 0.0

    /**
     * PowerTimeChange >> Energy Efficiency Calculations
     * I need to insert the heating and cooling hours based on set-point temp, operation hours, and thermostat schedule
     * */
    override fun energyPowerChange(): Double {

          // Step 3 : Get the Delta
        val powerPre = btu / seer / 1000
        val powerPost = btu / alternateSeer / 1000
        val eSavings = (powerPre - powerPost)

        val delta = eSavings * usageHoursPre()
        Timber.d("HVAC :: Delta -- $delta")

        return delta
    }

    fun totalSavings(): Double {
        val powerPre = btu / seer / 1000
        val powerPost = btu / alternateSeer / 1000
        var eSavings = (powerPre - powerPost)
            val usageHours = UsageLighting()
            usageHours.peakHours = peakHours
            usageHours.partPeakHours = partPeakHours
            usageHours.offPeakHours = offPeakHours
            return costElectricity(eSavings, usageHours, electricityRate)
            //return 296.0 just for testing
    }



    override fun energyTimeChange(): Double = 0.0
    override fun energyPowerTimeChange(): Double = 0.0

    /**
     * Energy Efficiency Lookup Query Definition
     * */
    override fun efficientLookup() =
            (firstNotNull(alternateSeer, alternateEer) == 0.0 || alternateBtu == 0)
    override fun queryEfficientFilter() = JSONObject()
            .put("type", HVAC_EFFICIENCY)
            .put("data.size_btu_hr", btu)
            .toString()

    /**
     * HVAC specific Query Builders
     * */
    override fun queryHVACEer() = JSONObject()
            .put("type", HVAC_EER)
            .put("data.year", getYear(age))
            .put("data.size_btu_per_hr_min", JSONObject().put("\$lte", btu))
            .put("data.size_btu_per_hr_max", JSONObject().put("\$gte", btu))
            .toString()

    override fun queryHVACCoolingHours() = JSONObject()
            .put("type", HVAC_COOLING_HOURS)
            .put("data.city", city)
            .put("data.state", state)
            .toString()

    override fun queryThermostatDeemed(): String {
        return JSONObject()
                .put("type", "thermostat_thermostatdeemed")
                .put("data.heating_fuel", heatingFuel)
                .put("data.cooling_fuel", coolingFuel)
                .toString()
    }

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
            "General Client Info Email",
            "General Client Info Business Name",
            "General Client Info Audit Month",
            "General Client Info Audit Year",
            "General Client Info Address",
            "General Client Info Assessment Start Day",
            "General Client Info Assessment End Day",
            "Area Total (Sq.Ft.)",
            "Electric Utility Company",
            "Others Electric Rate Structure",
            "Others Gas Rate Structure",
            "General Client Info Facility Type",
            "Operation Hours Monday Operating Hours",
            "Operation Hours Tuesday Operating Hours",
            "Operation Hours Wednesday Operating Hours",
            "Operation Hours Thursday Operating Hours",
            "Operation Hours Friday Operating Hours",
            "Operation Hours Saturday Operating Hours",
            "Operation Hours Sunday Operating Hours")

    override fun featureDataFields() = getGFormElements().map { it.value.param!! }.toMutableList()

    override fun preStateFields() = mutableListOf("heatingfuel", "coolingfuel")
    override fun postStateFields() = mutableListOf(
            "__life_hours",
            "__maintenance_savings",
            "__cooling_savings",
            "__energy_savings",
            "__energy_at_post_state",
            "__prescriptive_kWh_savings",
            "__prescriptive_kW_savings",
            "__prescriptive_implementation_cost")

    override fun computedFields() = mutableListOf<String>()

    private fun getFormMapper() = FormMapper(context, R.raw.hvac)
    private fun getModel() = getFormMapper().decodeJSON()
    private fun getGFormElements() = getFormMapper().mapIdToElements(getModel())

}
