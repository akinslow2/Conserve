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
         * Getting age of device and how much over life it is
         */
        fun getAge(year: Int): Int {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            return currentYear - year
        }

        fun overAge(year: Int) = getAge(year) - 15

        fun firstNotNull(valueFirst: Double, valueSecond: Double) =
                if (valueFirst == 0.0) valueSecond else valueFirst

        fun firstNotNull(valueFirst: Int, valueSecond: Int) =
                if (valueFirst == 0) valueSecond else valueFirst

    }

    /**
     * HVAC - Age
     * */
    var year = 0

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
    private var condensorPeakHours = 0.0
    private var condensorOffPeakHours = 0.0

    private var evaporatorPeakHours = 0.0
    private var evaporatorOffPeakHours = 0.0

    var quantity = 0
    var kWh = 0.0
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
            year = featureData["Year"]!! as Int

            condensorPeakHours = featureData["Condenser Peak Hours"]!! as Double
            condensorOffPeakHours = featureData["Condenser Off Peak Hours"]!! as Double
            evaporatorPeakHours = featureData["Evaporator Peak Hours"]!! as Double
            evaporatorOffPeakHours = featureData["Evaporator Off Peak Hours"]!! as Double
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

        val postRow = mutableMapOf<String, String>()

        dataHolder.header = postStateFields()
        dataHolder.computable = computable
        dataHolder.fileName = "${computable.zoneName}_${computable.auditScopeName}_WalkInCoolerBot_post_state_${Date().time}.csv"
        dataHolder.rows?.add(postRow)

        return 5.0 // TODO: AK2 needs to calculate this
    }

    fun installCost(): Double {
//     TODO:   sum of the costs pulled from the PARSE
        return 0.0
    }

    fun grosskwhSavings(): Double {
//     TODO:   sum of the gross energy savings pulled from the PARSE
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

    override fun efficientLookup() = false
    override fun queryEfficientFilter() = ""


    override fun preAuditFields() = mutableListOf<String>()

    override fun featureDataFields() = getGFormElements().map { it.value.param!! }.toMutableList()

    override fun preStateFields() = mutableListOf<String>()
    override fun postStateFields() = mutableListOf<String>()

    override fun computedFields() = mutableListOf<String>()

    private fun getFormMapper() = FormMapper(context, R.raw.walkin_coolbot)
    private fun getModel() = getFormMapper().decodeJSON()
    private fun getGFormElements() = getFormMapper().mapIdToElements(getModel())
}
