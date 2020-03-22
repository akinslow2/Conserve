package com.gemini.energy.service.crunch

import com.gemini.energy.domain.entity.Computable
import com.gemini.energy.presentation.util.ERateKey
import com.gemini.energy.service.CostElectric
import com.gemini.energy.service.DataHolder
import com.gemini.energy.service.type.Gas
import com.gemini.energy.service.type.UsageHours
import com.gemini.energy.service.type.UtilityRate
import io.reactivex.functions.Function
import timber.log.Timber
import java.util.*

class CostSavings {

    companion object {
        /**
         * TOU Validator
         * */
        private val regex = "^.*TOU$".toRegex()
        fun isTOU(rate: String) = rate.matches(regex)
        fun isNoTOU(rate: String) = !isTOU(rate)

        /**
         * Gives the first that is Not Zero
         * */
        fun firstNotNull(specific: UsageHours, business: UsageHours) =
                if (specific.yearly() == 0.0) business else specific

        fun firstNotNull (valueFirst: Double, valueSecond: Double) =
                if (valueFirst == 0.0) valueSecond else valueFirst

        /**
         * Accessing Values for the Best Alternative - Post State
         * */
        fun getValue(computable: Computable<*>, key: String) =
                computable.energyPostStateLeastCost[0].getValue(key).toDouble()

        fun resolve(computable: Computable<*>, key: String): Double {
            fun checker(computable: Computable<*>, key: String) =
                    (computable.energyPostStateLeastCost.count() > 0) &&
                            computable.energyPostStateLeastCost[0].containsKey(key)

            if (checker(computable, key)) {
                try {
                    return getValue(computable, key)
                } catch(exception: Exception) {
                    Timber.d("Exception @ $key - toDouble")
                }
            }
            return 0.0
        }

        /**
         * Compute Cost Electric
         * */
        fun costElectricity(power: Double, usage: UsageHours, rate: UtilityRate, structure: String): Double {
            val costElectric = CostElectric(usage, rate)
            costElectric.structure = structure
            costElectric.power = power

            return costElectric.cost()
        }

        /**
         * Compute Cost Gas
         * */
        fun costGas(energyUsed: Double, rate: UtilityRate): Double {
            val gas= rate.nonTimeOfUse()
            val rateFirst = (gas.summerNone() + gas.winterNone()) / 2
            val rateExcess = (gas.summerExcess() + gas.winterExcess()) / 2
            return (energyUsed) * (if (energyUsed > Gas.FIRST_SLAB) rateExcess else rateFirst)
        }

        /**
         * Key - To access Post State - Best Alternative
         * */
        private const val PURCHASE_PRICE_PER_UNIT = "purchase_price_per_unit"
        private const val REBATE = "rebate"
        private const val DAILY_ENERGY_USE = "daily_energy_use"

        private const val EMPTY = ""

    }

    class Mapper : Function<Unit, DataHolder> {

        lateinit var computable: Computable<*>
        lateinit var usageHoursSpecific: UsageHours
        lateinit var usageHoursBusiness: UsageHours
        lateinit var schedule: String
        lateinit var rateElectric: UtilityRate
        lateinit var rateGas: UtilityRate
        lateinit var featureData: Map<String, Any>
        lateinit var powerTimeChange: PowerTimeChange

        lateinit var dailyEnergyUsagePre: () -> Double
        lateinit var materialCost: () -> Double
        lateinit var laborCost: () -> Double
        lateinit var incentives: () -> Double

        lateinit var isGas: () -> Boolean

        override fun apply(unit: Unit): DataHolder {

            /**
             * Usage Pre     - Mapped Peak Hours (Specific)
             * Usage Post    - Mapped Peak Hours (Business)
             * */
            val usageHoursPre = firstNotNull(usageHoursSpecific, usageHoursBusiness)
            val usageHoursPost= usageHoursBusiness

            /**
             * Power, Time Change or Both
             * */
            val ptc = powerTimeChange.delta()
            val powerChangeCheck = ptc.checkPowerChange && !isGas()
            val timeChangeCheck = ptc.checkTimeChange
            val powerTimeChangeCheck = ptc.checkPowerTimeChange

            Timber.d("----::::---- Power Change Check ($powerChangeCheck) ----::::----")
            Timber.d("----::::---- Time Change Check ($timeChangeCheck) ----::::----")
            Timber.d("----::::---- Power Time Change Check ($powerTimeChangeCheck) ----::::----")

            val multiplePowerCheck = false
            val multipleTimeCheck = false
            val multiplePowerTimeCheck = false

            Timber.d("----::::---- Multiple Power Check ($multiplePowerCheck) ----::::----")
            Timber.d("----::::---- Multiple Time Check ($multipleTimeCheck) ----::::----")
            Timber.d("----::::---- Multiple Power Time Check ($multiplePowerTimeCheck) ----::::----")

            /**
             * Energy UsageHours - Efficient Alternative (Post State)
             * Either resolve the Energy Use or use the PTC value
             * */
            fun energyUse() = resolve(computable, DAILY_ENERGY_USE)
            val energyUse = if (energyUse() == 0.0) ptc.energySaving() else energyUse()
            Timber.d("----::::---- Energy Use ($energyUse) ----::::----")

            /**
             * Power Value
             * Case 1 : Multiple Power | Time Check -- @powerValues
             * Case 2 : Single Power | Time -- @powerValue
             * ToDo - Need to Populate these from the Post State - idle and preheat (2 Energy State)
             * */
            val powerValues = listOf<Double>()

            /**
             * Single Power Value
             * */
            val dailyEnergyUsePre = dailyEnergyUsagePre()
            val dailyEnergyUsePost = energyUse()
            var powerValue = (dailyEnergyUsePre - dailyEnergyUsePost)

            // *** This has been done for Lightening Specific - May be applicable to other Equipments as well ***
            val usageHours = firstNotNull(usageHoursSpecific, usageHoursBusiness).yearly()
            if (dailyEnergyUsePre == 0.0) {
                powerValue = ptc.energySaving() / usageHours
            }

            Timber.d("----::::---- Power Value ($powerValue) ----::::----")
            Timber.d("----::::---- Power Values ($powerValues) ----::::----")

            /**
             * <<< Energy Cost Saving >>>
             * */
            fun energyCostSaving(): HashMap<String, Double> {

                /**
                 * Energy Cost Savings Calculations - [TOU Based Savings]
                 *
                 * Multiple Power - Time
                 * Single Power - Time
                 * */
                fun electricityCostsCalcMultiplePowerChange(powerValues: List<Double>): Double {
                    var cost = 0.0
                    val rate = rateElectric
                    powerValues.forEach { power ->
                        cost += costElectricity(power, usageHoursPre, rate, schedule)
                    }
                    return cost
                }

                fun electricityCostsCalcMultipleTimeChange(powerValues: List<Double>): Double {
                    var cost = 0.0
                    val rate = rateElectric
                    powerValues.forEach { power ->
                        val costPre = costElectricity(power, usageHoursPre, rate, schedule)
                        val costPost = costElectricity(power, usageHoursPost, rate, schedule)
                        cost += (costPre - costPost)
                    }
                    return cost
                }

                fun electricityCostsCalcPowerChange(powerValue: Double) =
                        electricityCostsCalcMultiplePowerChange(listOf(powerValue))

                fun electricityCostsCalcTimeChange(powerValue: Double) =
                        electricityCostsCalcMultipleTimeChange(listOf(powerValue))

                /**
                 * Energy Cost Savings - Case 1 : TOU Based
                 * */
                fun findTimeOfUseCostSavings(outgoing: HashMap<String, Double>): HashMap<String, Double> {

                    if (multiplePowerCheck) {
                        outgoing["CostSavingMultiplePowerCheck"] = electricityCostsCalcMultiplePowerChange(powerValues)
                    }

                    if (multipleTimeCheck || multiplePowerTimeCheck) {
                        outgoing["CostSavingMultipleTimeOrPowerTimeCheck"] = electricityCostsCalcMultipleTimeChange(powerValues)
                    }

                    if (powerChangeCheck) {
                        outgoing["CostSavingPowerChangeCheck"] = electricityCostsCalcPowerChange(powerValue)
                    }

                    if (timeChangeCheck) {
                        outgoing["CostSavingTimeChangeCheck"] = electricityCostsCalcTimeChange(powerValue)
                    }

                    return outgoing
                }

                /**
                 * Energy Cost Savings - Case 2 : Non TOU Based
                 * */
                fun costNonTOU() = energyUse * rateElectric.nonTimeOfUse().weightedAverage()
                fun findNonTimeOfUseCostSavings(outgoing: HashMap<String, Double>): HashMap<String, Double> {
                    outgoing["CostSavingNonTimeOfUse"] = costNonTOU()
                    return outgoing
                }

                /**
                 * Energy Cost Savings - Case 3 : Gas Based
                 * */
                fun findGasCostSavings(outgoing: HashMap<String, Double>): HashMap<String, Double> {
                    outgoing["CostSavingGas"] = costGas(energyUse, rateGas)
                    return outgoing
                }

                /**
                 * Flag to denote a gas based equipment
                 * */
                val checkForGas = isGas()
                Timber.d("----::::---- Check For Gas ($checkForGas) ----::::----")

                /**
                 * Main Block
                 * */
                val energyCostSavings: HashMap<String, Double> = hashMapOf()
                if (isTOU(schedule)) { findTimeOfUseCostSavings(energyCostSavings) }
                if (isNoTOU(schedule)) { findNonTimeOfUseCostSavings(energyCostSavings) }
                if (checkForGas) { findGasCostSavings(energyCostSavings) }

                return energyCostSavings

            }

            /**
             * <<< Demand Cost Saving >>>
             * */
            fun demandCostSaving(): Double {

                fun demandCostSavingsYearCalc(power: Double): Double {

                    val structure = rateElectric.structure
                    val rateSummer: Double
                    val rateWinter: Double

                    if (isNoTOU(schedule)) {
                        rateSummer = structure[ERateKey.SummerNone.value]?.get(2)?.toDouble() ?: 0.0
                        rateWinter = structure[ERateKey.WinterNone.value]?.get(2)?.toDouble() ?: 0.0
                    } else {
                        rateSummer = structure[ERateKey.SummerOff.value]!![2].toDouble()
                        rateWinter = structure[ERateKey.WinterOff.value]!![2].toDouble()
                    }

                    return ((powerValue / 2) * 6 * (rateWinter + rateSummer))

                }

                val demandCostSavings: Double
                demandCostSavings = when {
                    powerValue > 0 && powerValues.count() == 0 -> demandCostSavingsYearCalc(powerValue)
                    powerValue <= 0 && powerValues.count() > 0 -> demandCostSavingsYearCalc(powerValues[0])
                    else -> 0.0
                }

                return if (isGas()) 0.0 else demandCostSavings

            }

            /**
             * Parse API Energy Efficient Database - materialCost
             * Parse API Labor Cost - laborCost
             * */
            fun purchasePricePerUnit() = resolve(computable, PURCHASE_PRICE_PER_UNIT)
            val materialCost = firstNotNull(purchasePricePerUnit(), materialCost())
            val laborCost = firstNotNull(computable.laborCost, laborCost())

            Timber.d("----::::---- Material Cost ($materialCost) ----::::----")
            Timber.d("----::::---- Labor Cost ($laborCost) ----::::----")

            /**
             * Note: Looks like this is going to be Equipment Specific
             * */
            val maintenanceCostSavings = 0.0
            val otherEquipmentSavings = 0.0

            Timber.d( "----::::---- Maintenance Cost Savings ($maintenanceCostSavings) ----::::----")
            Timber.d( "----::::---- Other Equipment Savings ($otherEquipmentSavings) ----::::----")

            /**
             * Parse API Energy Efficient Database - Rebate
             * */
            fun rebate() = resolve(computable, REBATE)
            val incentives = firstNotNull(rebate(), incentives())
            Timber.d("----::::---- Incentive ($incentives) ----::::----")

            /**
             * <<< Implementation Cost >>>
             * */
            //ToDo - @Verify Johnny : How does Quantity Impact this ??

            fun implementationCost() = (materialCost() + laborCost()) - incentives()
            /**
             * <<< Total Cost Saved >>>
             * */
            fun aggregateEnergyCostSavings(): Double {

                val savings = energyCostSaving()
                fun checker(key: String) = if (savings.containsKey(key)) {
                    savings[key]!!
                } else 0.0

                /**
                 * TOU Based Savings
                 * */
                var aggregate = 0.0
                aggregate += checker("CostSavingMultiplePowerCheck")
                aggregate += checker("CostSavingMultipleTimeOrPowerTimeCheck")
                aggregate += checker("CostSavingPowerChangeCheck")
                aggregate += checker("CostSavingTimeChangeCheck")

                /**
                 * Non TOU Based Saving*/
                aggregate += checker("CostSavingNonTimeOfUse")

                /**
                 * Gas Based Savings
                 * */
                aggregate += checker("CostSavingGas")

                return aggregate
            }

            //ToDo - @Verify Johnny : How does Quantity Impact this ??
            fun totalCostSaved() = aggregateEnergyCostSavings() + maintenanceCostSavings + otherEquipmentSavings + demandCostSaving()

            /**
             * <<< Payback Period - Months >>>
             * */
            fun paybackPeriodMonths() = (implementationCost() / totalCostSaved()) * 12


            /**
             * <<< Payback Period - Years >>>
             * */
            fun paybackPeriodYears() = (implementationCost() / totalCostSaved())

            /**
             * Preparing the Data-Holder to Store the Outgoing Data
             * */
            val energyCostSavingHeader = listOf("__costSavingMultiplePowerCheck",

                    "__costSavingMultipleTimeOrPowerTimeCheck", "__costSavingPowerChangeCheck",
                    "__costSavingTimeChangeCheck", "__costSavingNonTimeOfUse", "__costSavingGas",

                    "__demand_cost_saving", "__implementation_cost", "__total_cost_saved",
                    "__payback_period_months", "__payback_period_years")

            fun initDataHolder(): DataHolder {
                val dataHolderPostState = DataHolder()
                dataHolderPostState.header?.addAll(energyCostSavingHeader)

                dataHolderPostState.computable = computable
                dataHolderPostState.fileName = "${Date().time}_energy_cost_savings.csv"

                return dataHolderPostState
            }

            val dataHolder = initDataHolder()

            /**
             * Prepare the Outgoing Rows
             * */
            fun checker(key: String) = if (energyCostSaving().containsKey(key)) {
                energyCostSaving()[key]!!.toString()
            } else EMPTY
            dataHolder.rows?.add(mapOf(
                    "__costSavingMultiplePowerCheck" to checker("CostSavingMultiplePowerCheck"),
                    "__costSavingMultipleTimeOrPowerTimeCheck" to checker("CostSavingMultipleTimeOrPowerTimeCheck"),
                    "__costSavingPowerChangeCheck" to checker("CostSavingPowerChangeCheck"),
                    "__costSavingTimeChangeCheck" to checker("CostSavingTimeChangeCheck"),
                    "__costSavingNonTimeOfUse" to checker("CostSavingNonTimeOfUse"),
                    "__costSavingGas" to checker("CostSavingGas"),
                    "__demand_cost_saving" to demandCostSaving().toString(),
                    "__implementation_cost" to implementationCost().toString(),
                    "__total_cost_saved" to totalCostSaved().toString(),
                    "__payback_period_months" to paybackPeriodMonths().toString(),
                    "__payback_period_years" to paybackPeriodYears().toString()
            ))

            return dataHolder
        }

    }
}
