package com.gemini.energy.service.device.plugload

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

class IceMaker(computable: Computable<*>, utilityRateGas: UtilityRate, utilityRateElectricity: UtilityRate,
                   usageHours: UsageHours, outgoingRows: OutgoingRows, private val context: Context) :
        EBase(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows), IComputable {

    /**
     * Entry Point
     * */
    override fun compute(): Observable<Computable<*>> {
        return super.compute(extra = ({ Timber.d(it) }))
    }

    private var iceHarvestRate = 0.0
    private var energyUseRate = 0.0
    private var alternateIceHarvestRate = 0.0
    private var alternateEnergyUseRate = 0.0
    var age = 0.0

    override fun setup() {
        try {
            iceHarvestRate = featureData["Ice Harvest Rate"]!! as Double
            energyUseRate = featureData["Energy Use Rate"]!! as Double
            age = (featureData["Age"]!! as Int).toDouble()
            alternateIceHarvestRate = featureData["Alternate Ice Harvest Rate"]!! as Double
            alternateEnergyUseRate = featureData["Alternate Energy Use Rate"]!! as Double
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Cost - Pre State
     * IceHarvestRate is by day & energyUseRate is per 100lbs so we are normalizing to
     * powerUsed per hour (kWh)
     * */
    override fun costPreState(elements: List<JsonElement?>): Double {
        val powerUsed = (iceHarvestRate / 24) * (energyUseRate / 100)
        val usageHours = usageHoursBusiness
        return costElectricity(powerUsed, usageHours, electricityRate)
    }

    /**
     * Cost - Post State
     * */
    var costPostState = 0.0
    override fun costPostState(element: JsonElement, dataHolder: DataHolder): Double {
        return 0.0
    }

    /**
     * Ice Maker - INCENTIVES | MATERIAL COST
     * */
    override fun incentives(): Double {
        return 200.00
    }

    override fun materialCost(): Double {
        return 3455.00
    }

    override fun laborCost(): Double {
        return 0.0
    }

    fun implementationCost(): Double {
        return (materialCost() + laborCost()) - incentives()
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
    override fun usageHoursPre(): Double = usageHoursBusiness.yearly()
    override fun usageHoursPost(): Double = usageHoursBusiness.yearly()

    /**
     * PowerTimeChange >> Energy Efficiency Calculations
     * */
    override fun energyPowerChange(): Double {
        val powerPre = (iceHarvestRate / 24) * (energyUseRate / 100)
        val powerPost = (alternateIceHarvestRate / 24) * (alternateEnergyUseRate / 100)
        val deltaPower = powerPre - powerPost

        val delta = deltaPower * usageHoursPre()
        return delta
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
    override fun postStateFields() = mutableListOf("company", "model_number", "style_type",
            "total_volume", "daily_energy_use", "rebate", "pgne_measure_code", "purchase_price_per_unit", "vendor")

    override fun computedFields() = mutableListOf("")

    private fun getFormMapper() = FormMapper(context, R.raw.icemaker)
    private fun getModel() = getFormMapper().decodeJSON()
    private fun getGFormElements() = getFormMapper().mapIdToElements(getModel())

}
