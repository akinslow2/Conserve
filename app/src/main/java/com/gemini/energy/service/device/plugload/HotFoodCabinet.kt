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
import com.gemini.energy.service.type.UsageSimple
import com.gemini.energy.service.type.UtilityRate
import com.google.gson.JsonElement
import io.reactivex.Observable
import org.json.JSONObject
import timber.log.Timber

class HotFoodCabinet(computable: Computable<*>, utilityRateGas: UtilityRate, utilityRateElectricity: UtilityRate,
                     usageHours: UsageHours, outgoingRows: OutgoingRows, private val context: Context) :
        EBase(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows), IComputable {

    /**
     * Entry Point
     * */
    override fun compute(): Observable<Computable<*>> {
        return super.compute(extra = ({ Timber.d(it) }))
    }

    /**
     * Usage Hours
     * */
    private var peakHours = 0.0
    private var partPeakHours = 0.0
    private var offPeakHours = 0.0
    private var usageHours: UsageSimple? = null

    private var cabinetVolume = 0.0
    private var idleEnergyRate = 0.0
    private var size = ""
    private var age = 0.0


    override fun setup() {
        try {
            peakHours = featureData["Peak Hours"]!! as Double
            partPeakHours = featureData["Part Peak Hours"]!! as Double
            offPeakHours = featureData["Off Peak Hours"]!! as Double
            usageHours = UsageSimple(peakHours, partPeakHours, offPeakHours)

            cabinetVolume = featureData["Cabinet Volume"]!! as Double
            idleEnergyRate = featureData["Idle Energy Rate"]!! as Double
            size = featureData["Size"]!! as String
            age = featureData["Age"]!! as Double

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * Cost - Pre State
     * */
    override fun costPreState(elements: List<JsonElement?>): Double {
        val powerUsed = hourlyEnergyUsagePre()[0]
        val costElectricity: Double
        costElectricity = costElectricity(powerUsed, usageHours!!, electricityRate)
        return costElectricity
    }

    /**
     * Cost - Post State
     * */
    override fun costPostState(element: JsonElement, dataHolder: DataHolder): Double {
        val powerUsed = hourlyEnergyUsagePost(element)[0]
        val costElectricity: Double
        costElectricity = costElectricity(powerUsed, usageHours!!, electricityRate)
        return costElectricity
    }

    /**
     * PowerTimeChange >> Hourly Energy Use - Pre
     * */
    override fun hourlyEnergyUsagePre(): List<Double> {
        var annualEnergy = 0.0

        try {
            val annualEnergyUsed = ((idleEnergyRate * cabinetVolume) / 100) * usageHoursPre()
            annualEnergy = annualEnergyUsed
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return listOf(annualEnergy)
    }

    /**
     * PowerTimeChange >> Hourly Energy Use - Post
     * */
    override fun hourlyEnergyUsagePost(element: JsonElement): List<Double> {
        var annualEnergy = 0.0

        try {

            val cabinetVolumePost = element.asJsonObject.get("cabinet_volume").asDouble
            val idleEnergyRatePost = element.asJsonObject.get("idle_energy_rate").asDouble
            val annualHoursPost = peakHours + partPeakHours + offPeakHours

            val postAnnualEnergyUsed = ((idleEnergyRatePost * cabinetVolumePost) / 100) * annualHoursPost
            annualEnergy = postAnnualEnergyUsed

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return listOf(annualEnergy)
    }

    /**
     * PowerTimeChange >> Yearly Usage Hours - [Pre | Post]
     * Pre and Post are the same for Refrigerator - 24 hrs
     * */
    //ToDo - @Johnny Verify this
    override fun usageHoursPre(): Double = usageHours!!.yearly()
    override fun usageHoursPost(): Double = usageHoursBusiness.yearly()

    override fun incentives(): Double {
        return 0.0
    }

    override fun materialCost(): Double {
        return 2500.0
    }
    override fun laborCost(): Double {
        return 0.0
    }
    //@K2 is this correct?
    override fun implementationCost(): Double {
        return (materialCost() + laborCost()) - incentives()
    }
    /**
     * PowerTimeChange >> Energy Efficiency Calculations
     * */
    override fun energyPowerChange(): Double {
        val prePower = hourlyEnergyUsagePre()[0]
        var postPower: Double
        var delta = 0.0

        computable.efficientAlternative?.let {
            postPower = hourlyEnergyUsagePost(it)[0]
            delta = usageHoursPre() * (prePower - postPower)
        }

        return delta
    }

    override fun energyTimeChange(): Double = 0.0
    override fun energyPowerTimeChange(): Double = 0.0

    /**
     * Energy Efficiency Lookup Query Definition
     * */
    override fun efficientLookup() = true
    override fun queryEfficientFilter() = JSONObject()
            .put("data.cabinet_volume", cabinetVolume)
            .put("data.idle_energy_rate", idleEnergyRate)
            .put("data.size", size)
            .toString()

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
    override fun postStateFields() = mutableListOf("company","model_number","size","cabinet_volume","idle_energy_rate",
            "rebate","pgne_measure_code","utility_company")

    override fun computedFields() = mutableListOf("__daily_operating_hours", "__weekly_operating_hours",
            "__yearly_operating_hours", "__electric_cost")

    private fun getFormMapper() = FormMapper(context, R.raw.hot_food_cabinet)
    private fun getModel() = getFormMapper().decodeJSON()
    private fun getGFormElements() = getFormMapper().mapIdToElements(getModel())

}
