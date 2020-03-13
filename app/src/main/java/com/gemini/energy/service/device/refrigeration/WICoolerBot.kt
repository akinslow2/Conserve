package com.gemini.energy.service.device.refrigeration

import android.content.Context
import com.gemini.energy.R
import com.gemini.energy.domain.entity.Computable
import com.gemini.energy.presentation.form.FormMapper
import com.gemini.energy.service.DataHolder
import com.gemini.energy.service.IComputable
import com.gemini.energy.service.OutgoingRows
import com.gemini.energy.service.device.EBase
import com.gemini.energy.service.type.UsageHours
import com.gemini.energy.service.type.UtilityRate
import com.google.gson.JsonElement
import io.reactivex.Observable
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class WICoolerBot(computable: Computable<*>, utilityRateGas: UtilityRate, utilityRateElectricity: UtilityRate,
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
         * Year At - Current minus the Age
         * */
        private val dateFormatter = SimpleDateFormat("yyyy", Locale.ENGLISH)

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
     * HVAC - Age
     * */
    var age = 0


    /**
     * HVAC - British Thermal Unit
     * */
    var btu = 0


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


    var quantity = 0
    var kW = 0.0
    var capacity = 0

    var condensorTemp = ""
    var condesorCompressor = 0
    var condensorCompressorphase = 0

    var motortype = ""
    var fridgetype = ""

    var fanmotortype = ""
    var temprange = ""

    override fun setup() {
        try {

            quantity = featureData["Quantity"]!! as Int

            age = featureData["Age"]!! as Int
            //   btu = featureData["Cooling Capacity (Btu/hr)"]!! as Int
            //   gasInput = featureData["Heating Input (Btu/hr)"]!! as Int
            //   gasOutput = featureData["Heating Output (Btu/hr)"]!! as Int
           // condesorCompressor = featureData["Condensor Compressor Size (HP)"]!! as Int
           // condensorCompressorphase = featureData["Compressor Phase"]!! as Int
           // condensorTemp = featureData["Temp"]!! as String

           // motortype = featureData["Motor Type"]!! as String
           // fridgetype = featureData["Refrigeration Type"]!! as String

            //fanmotortype = featureData["Fan Motor Type"]!! as String
           // temprange = featureData["Temperature Range"]!! as String

            kW = featureData["Heating Power (kW)"]!! as Double

            //    unittype = featureData["Type of Unit"]!! as String
            capacity = featureData["Capacity (BTU)"]!! as Int
            peakHours = featureData["Peak Hours"]!! as Double
            //   partPeakHours = featureData["Part Peak Hours"]!! as Double
            offPeakHours = featureData["Off Peak Hours"]!! as Double


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

        return 0.0 // TODO: AK2 needs to calculate this.
    }

    /**
     * Cost - Post State
     * */
    override fun costPostState(element: JsonElement, dataHolder: DataHolder): Double {

        Timber.d("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$")
        Timber.d("!!! COST POST STATE - WALK-IN Refrigerator !!!")
        Timber.d("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$")


        return 5.0 // TODO: AK2 needs to calculate this
    }

    fun installCost(): Double {
//        sum of the costs pulled from the PARSE
        return 0.0
    }

    fun grosskwhSavings(): Double {
//        sum of the gross energy savings pulled from the PARSE
        return 0.0
    }

    /**
     * HVAC - INCENTIVES | MATERIAL COST
     * */
    override fun incentives(): Double {
        return 0.0
    }

    override fun materialCost(): Double {
        return 0.0
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
    override fun usageHoursPre(): Double = 0.0
    override fun usageHoursPost(): Double = 0.0

    /**
     * PowerTimeChange >> Energy Efficiency Calculations
     * I need to insert the heating and cooling hours based on set-point temp, operation hours, and thermostat schedule
     * */
    override fun energyPowerChange(): Double {


        return 0.0 // TODO: AK2 needs to calculate
    }

    fun totalSavings(): Double {
        return energyPowerChange() * .15 // TODO: AK2 needs to calculate this
    }

    override fun energyTimeChange(): Double = 0.0
    //Calculating TRM Net Savings for Evaporator Fan Motor (pg. 103)
    val freerider = 0.95
    val spillover = 1.05
    //Values recieved wil be savings per fan
    override fun energyPowerTimeChange(): Double = 0.0


    /**
     * State if the Equipment has a Post UsageHours Hours (Specific) ie. A separate set of
     * Weekly UsageHours Hours apart from the PreAudit
     * */
    override fun usageHoursSpecific() = false

    override fun efficientLookup()= false
    override fun queryEfficientFilter()= ""


    override fun preAuditFields() = mutableListOf("")

    override fun featureDataFields() = getGFormElements().map { it.value.param!! }.toMutableList()

    override fun preStateFields() = mutableListOf("")
    override fun postStateFields() = mutableListOf("")

    override fun computedFields() = mutableListOf("")

    private fun getFormMapper() = FormMapper(context, R.raw.walkin_coolbot)
    private fun getModel() = getFormMapper().decodeJSON()
    private fun getGFormElements() = getFormMapper().mapIdToElements(getModel())

}
