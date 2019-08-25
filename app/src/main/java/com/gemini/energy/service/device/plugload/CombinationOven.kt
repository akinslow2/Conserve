package com.gemini.energy.service.device.plugload

import com.gemini.energy.domain.entity.Computable
import com.gemini.energy.service.DataHolder
import com.gemini.energy.service.IComputable
import com.gemini.energy.service.OutgoingRows
import com.gemini.energy.service.device.EBase
import com.gemini.energy.service.type.UsageHours
import com.gemini.energy.service.type.UtilityRate
import com.google.gson.JsonElement
import io.reactivex.Observable
import org.json.JSONObject
import timber.log.Timber

class CombinationOven(private val computable: Computable<*>, utilityRateGas: UtilityRate, utilityRateElectricity: UtilityRate,
                      usageHours: UsageHours, outgoingRows: OutgoingRows) :
        EBase(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows), IComputable {

    /**
     * Entry Point
     * */
    override fun compute(): Observable<Computable<*>> {
        return super.compute(extra = ({ Timber.d(it) }))
    }

    companion object {

        /**
         * This changes Kilowatts to BTU
         * */
        private const val ADJUSTMENT_GAS = 3412

        /**
         * The water charge is .015 only for San Francisco area.
         * Later we will need to make a sheet that will be used to determine the water charge based on city
         * */
        private const val WATER_CHARGE = 0.015

        /**
         * It takes 15 min to Pre Heat the Oven but the Energy Value Provided is for an Hour
         * Thus need to multiply by 1/4
         * */
        private const val ADJUSTMENT_PRE_HEAT = 0.25
        fun adjustPreHeat(value: Double) = value * ADJUSTMENT_PRE_HEAT

    }

    private fun isElectric() = fuelType == "Electric"
    override fun isGas() = fuelType == "Gas"

    /**
     * This is going to be the Business Hours (Pre Audit Operation Hours)
     * - the number of hours the client is open for business over a year
     * - each client could have a unique value for pre run hours as they work different hours
     * */
    private var daysInOperation = 0.0

    private var preIdleEnergyRateConvection = 0.0
    private var preIdleEnergyRateSteam = 0.0

    /**
     * The Adjustment Factor is taken care within the setup()
     * */
    private var preHeatEnergy = 0.0

    private var preFanEnergyRate = 0.0
    private var postFanEnergyRate = 0.0

    private var waterUseConvection = 0.0
    private var waterUseSteam = 0.0

    private var fuelType = ""

    /**
     * To be used for Efficient Query Filter
     * */
    private var steamPanSize = -99

    /**
     * Computes the Combination Oven Cost
     * 1. Electric
     * 2. Gas
     * 3. Water
     * */
    class Cost {

        var preHeatEnergy = 0.0
        var preFanEnergyRate = 0.0
        var postFanEnergyRate = 0.0

        var idlePowerRateConvection = 0.0
        var idlePowerRateSteam = 0.0

        var waterUseConvection = 0.0
        var waterUseSteam = 0.0

        lateinit var electricityRate: UtilityRate

        var isGas = false
        var isElectric = false
        var daysInOperation = 0.0

        lateinit var usageHours: UsageHours

        lateinit var isTOU: () -> Boolean
        lateinit var usageHoursPre: () -> Double
        lateinit var getCostElectric: (powerUsed: Double, usageHours: UsageHours, utilityRate: UtilityRate) -> Double
        lateinit var getCostGas: (energyUsed: Double) -> Double

        fun calculate(): Double {

            val adjustment = if (isGas) ADJUSTMENT_GAS else 1
            val averageIdleRate = (idlePowerRateConvection + idlePowerRateSteam) / 2

            val yearlyIdleEnergy = averageIdleRate * usageHoursPre()
            val yearlyPreHeatEnergy = preHeatEnergy * daysInOperation // Adjusted preheat energy is per day over here
            val yearlyFanEnergy = (preFanEnergyRate - postFanEnergyRate) * usageHoursPre() * adjustment

            val energyUsed= yearlyIdleEnergy + yearlyPreHeatEnergy + yearlyFanEnergy
            val powerUsed = averageIdleRate + preFanEnergyRate // Needed to identify PreEnergy Usage thus PreFanEnergy

            var costElectric = 0.0
            var costGas = 0.0
            val costWater: Double

            // >>> 1. Cost Electricity
            if (isElectric) {

                //@Anthony - What happens when the Audit Rate Structure is TOU ??
                // - Now costElectricity is using A1-TOU
                // - costToPreHeat should be using A1 | A10 .. ??
                // - Should we just do an average of whatever the rate structure is TOU or Non TOU ??
                val rate = if (isTOU()) electricityRate.timeOfUse() else electricityRate.nonTimeOfUse()

                //@Anthony - Do you think we need to multiply by 365 as we have already multiplied by yearly daysInOperation
                val costToPreHeat = yearlyPreHeatEnergy * rate.weightedAverage()
                Timber.d("Cost To Pre Heat :: $costToPreHeat")
                costElectric = getCostElectric(powerUsed, usageHours, electricityRate)
                costElectric += costToPreHeat

            }

            // >>> 2. Cost Gas
            if (isGas) {
                costGas = getCostGas(energyUsed)
            }

            // >>> 3. Cost Water
            val averageWaterUsed = (waterUseConvection + waterUseSteam) / 2
            costWater = usageHoursPre() * WATER_CHARGE * averageWaterUsed

            Timber.d("Electricity Cost :: $costElectric")
            Timber.d("Gas Cost :: $costGas")
            Timber.d("Water Cost :: $costWater")

            val totalCost = costElectric + costGas + costWater
            Timber.d("Total Cost :: $totalCost")

            return totalCost
        }

    }

    /**
     * Will be called once before anything to initialize the commonly used parameters
     * */
    override fun setup() {

        try {
            fuelType = featureData["Fuel Type"]!! as String

            //ToDo: DaysInOperation
            //This does not work. See bottom of yesterdays detailed email for clarification.
            //Also Days in operation does not change between pre and post so you can just call the variable DaysInOperation...
            daysInOperation = usageHoursPre() / 24

            preIdleEnergyRateConvection = featureData["Convection Idle Rate"]!! as Double
            preIdleEnergyRateSteam = featureData["Steam Idle Rate"]!! as Double

            val rawPreHeatEnergy = featureData["Preheat Energy"]!! as Double
            preHeatEnergy = adjustPreHeat(rawPreHeatEnergy)

            preFanEnergyRate = featureData["Pre Fan Energy Rate"]!! as Double
            postFanEnergyRate = featureData["Post Fan Energy Rate"]!! as Double

            waterUseConvection = featureData["Convection Water Usage Rate"]!! as Double
            waterUseSteam = featureData["Steam Water Usage Rate"]!! as Double

            steamPanSize = featureData["Size (Steam Pans)"]!! as Int
            age = featureData["Age"]!! as Double

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * Cost - Pre State
     * The main Cost class does the computing - This way we can use the same Cost class on the Post State
     * */
    override fun costPreState(elements: List<JsonElement?>): Double {
        val cost = Cost()

        cost.preHeatEnergy = preHeatEnergy
        cost.preFanEnergyRate = preFanEnergyRate
        cost.postFanEnergyRate = postFanEnergyRate

        cost.idlePowerRateConvection = preIdleEnergyRateConvection
        cost.idlePowerRateSteam = preIdleEnergyRateSteam

        cost.waterUseConvection = waterUseConvection
        cost.waterUseSteam = waterUseSteam

        cost.electricityRate = electricityRate
        cost.isGas = isGas()
        cost.isElectric = isElectric()

        cost.daysInOperation = daysInOperation
        cost.usageHours = usageHoursSpecific

        cost.isTOU = { isTOU(electricRateStructure) }
        cost.usageHoursPre = { usageHoursPre() }

        cost.getCostGas = { costGas(it) }
        cost.getCostElectric = { powerUsed, usageHours, utilityRate ->
            costElectricity(powerUsed, usageHours, utilityRate)
        }

        return cost.calculate()

    }
    override fun incentives(): Double {
        return 0.0
    }

    override fun materialCost(): Double {
        return 8500.0
    }
    override fun laborCost(): Double {
        return 0.0
    }
    //@K2 is this correct?
    override fun implementationCost(): Double {
        return (materialCost() + laborCost()) - incentives()
    }
    /**
     * Cost - Post State
     * */
    // The post state cost or energy calculation?? for energy I provided the location for that in the last email.
    // Cost is the same process as the pre
    override fun costPostState(element: JsonElement, dataHolder: DataHolder): Double {
        var preHeatEnergyPS = 0.0
        val preFanEnergyRatePS = 0.0
        val postFanEnergyRatePS = 0.0

        var idlePowerRateConvectionPS = 0.0
        var idlePowerRateSteamPS = 0.0

        var waterUseConvectionPS = 0.0
        var waterUseSteamPS = 0.0

        try {
            preHeatEnergyPS = element.asJsonObject.get("preheat_energy").asDouble
            idlePowerRateConvectionPS = element.asJsonObject.get("convection_idle_rate").asDouble
            idlePowerRateSteamPS = element.asJsonObject.get("steam_idle_rate").asDouble
            waterUseConvectionPS = element.asJsonObject.get("convection_cooking_water_use").asDouble
            waterUseSteamPS = element.asJsonObject.get("steam_cooking_water_use").asDouble
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val cost = Cost()

        cost.preHeatEnergy = preHeatEnergyPS
        cost.preFanEnergyRate = preFanEnergyRatePS
        cost.postFanEnergyRate = postFanEnergyRatePS

        cost.idlePowerRateConvection = idlePowerRateConvectionPS
        cost.idlePowerRateSteam = idlePowerRateSteamPS

        cost.waterUseConvection = waterUseConvectionPS
        cost.waterUseSteam = waterUseSteamPS

        cost.electricityRate = electricityRate
        cost.isGas = isGas()
        cost.isElectric = isElectric()

        cost.daysInOperation = daysInOperation
        cost.usageHours = usageHoursSpecific

        cost.isTOU = { isTOU(electricRateStructure) }
        cost.usageHoursPre = { usageHoursPre() }

        cost.getCostGas = { costGas(it) }
        cost.getCostElectric = { powerUsed, usageHours, utilityRate ->
            costElectricity(powerUsed, usageHours, utilityRate)
        }

        return cost.calculate()

    }

    /**
     * PowerTimeChange >> Hourly Energy Use - Pre
     * */
    override fun hourlyEnergyUsagePre(): List<Double> = listOf(0.0)

    /**
     * PowerTimeChange >> Hourly Energy Use - Post
     * */
    override fun hourlyEnergyUsagePost(element: JsonElement): List<Double> = listOf(0.0)

    /**
     * PowerTimeChange >> Yearly Usage Hours - [Pre | Post]
     * */
    override fun usageHoursPre(): Double = usageHoursSpecific.yearly()
    override fun usageHoursPost(): Double = usageHoursSpecific.yearly()

    /**
     * PowerTimeChange >> Energy Efficiency Calculations
     * For Ovens there is no Time Change nor Power Time Change
     * */
    override fun energyPowerChange(): Double {
        var rawPreHeatEnergyPS = 0.0
        val preFanEnergyRatePS = 0.0
        val postFanEnergyRatePS = 0.0

        var idlePowerRateConvectionPS = 0.0
        var idlePowerRateSteamPS = 0.0

        val element = computable.efficientAlternative
        element?.let {
            try {
                rawPreHeatEnergyPS = it.asJsonObject.get("preheat_energy").asDouble
                idlePowerRateConvectionPS = it.asJsonObject.get("convection_idle_rate").asDouble
                idlePowerRateSteamPS = it.asJsonObject.get("steam_idle_rate").asDouble
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val adjustment = if (isGas()) ADJUSTMENT_GAS else 1

        val deltaPreHeatEnergy = preHeatEnergy - adjustPreHeat(rawPreHeatEnergyPS)
        val deltaFanEnergyRate = preFanEnergyRatePS - postFanEnergyRatePS

        val preIdlePowerRate = (preIdleEnergyRateConvection + preIdleEnergyRateSteam) / 2
        val postIdlePowerRate = (idlePowerRateConvectionPS + idlePowerRateSteamPS) / 2
        val deltaIdlePowerRate = preIdlePowerRate - postIdlePowerRate

        val deltaYearlyPreHeatEnergy = deltaPreHeatEnergy * daysInOperation
        val deltaYearlyIdleEnergy = deltaIdlePowerRate * usageHoursPre()
        val deltaYearlyFanEnergy = deltaFanEnergyRate * usageHoursPre() * adjustment

        val delta = deltaYearlyPreHeatEnergy + deltaYearlyIdleEnergy + deltaYearlyFanEnergy

        Timber.d("Delta Yearly Pre Heat Energy :: $deltaYearlyPreHeatEnergy")
        Timber.d("Delta Yearly Idle Energy :: $deltaYearlyIdleEnergy")
        Timber.d("Delta Yearly Fan Energy :: $deltaYearlyFanEnergy")
        Timber.d("Delta :: $delta")

        return delta
    }

    override fun energyTimeChange(): Double = 0.0
    override fun energyPowerTimeChange(): Double = 0.0

    /**
     * Energy Efficiency Lookup Query Definition
     * */
    override fun efficientLookup() = true
    override fun queryEfficientFilter() = JSONObject()
            .put("data.size", steamPanSize)
            .toString()

    /**
     * State if the Equipment has a Post UsageHours Hours (Specific) ie. A separate set of
     * Weekly UsageHours Hours apart from the PreAudit
     * */
    override fun usageHoursSpecific() = true

    /**
     * Define all the fields here - These would be used to Generate the Outgoing Rows or perform the Energy Calculation
     * */
    override fun preAuditFields() = mutableListOf("Number of Vacation days")
    override fun featureDataFields() = mutableListOf("Size (Steam Pans)", "Fuel Type", "Model Number",
            "Company", "Used During Peak", "Age", "Control", "Preheat Energy",
            "Convection Idle Energy", "Steam Idle Rate")

    override fun preStateFields() = mutableListOf<String>()
    override fun postStateFields() = mutableListOf("company", "model_number", "size",
            "fuel_type", "preheat_energy", "convection_idle_rate", "convection_energy_efficiency", "convection_production_capacity",
            "convection_cooking_water_use", "steam_idle_rate", "steam_energy_efficiency", "steam_production_capacity", "steam_cooking_water_use",
            "rebate", "pgne_measure_code", "purchase_price_per_unit", "vendor")

    override fun computedFields() = mutableListOf("__daily_operating_hours", "__weekly_operating_hours",
            "__yearly_operating_hours", "__electric_cost")

}



