package com.gemini.energy.service

import com.gemini.energy.service.type.*
import timber.log.Timber

interface ICost {
    fun cost(): Double
}

class EmptyRateStructureException(message: String) : Exception(message)

class CostElectric(private val usageHours: UsageHours, private val utilityRate: UtilityRate,
                   private val debug: Boolean = false) : ICost {

    var structure: String = NONE
    var power: Double = 0.0

    private lateinit var hours: IUsageType
    private lateinit var rate: IUsageType

    override fun cost(): Double {
        if (structure == NONE) { throw  EmptyRateStructureException("Rate Structure Empty") }

        initialize()
        logIUsageType()
        return costSummer() + costWinter() + costBlendedSeason()
    }

    /**
     * Setting up the Correct Type of Hours and Rate [TOU | Non TOU]
     * */
    private fun initialize() {
        hours = if (isTOU(structure)) usageHours.timeOfUse() else usageHours.nonTimeOfUse()
        rate = if (isTOU(structure)) utilityRate.timeOfUse() else utilityRate.nonTimeOfUse()
    }

    /**
     * Calculates the Summer Cost (Proper UsageHours Type Cast done via isTOU() flag check)
     * */
    private fun costSummer(): Double {

        val costSummerOn = hours.summerOn() * power * rate.summerOn()
        val costSummerPart = hours.summerPart() * power * rate.summerPart()
        val costSummerOff = hours.summerOff() * power * rate.summerOff()

        if (isTOU(structure) && debug) {
            Timber.d("## TOU Cost - Summer ##")
            Timber.d(">>> Summer On : $costSummerOn")
            Timber.d(">>> Summer Part : $costSummerPart")
            Timber.d(">>> Summer Off : $costSummerOff")
        }

        val summerTOU = costSummerOn + costSummerPart + costSummerOff
        val summerTOUNone = hours.summerNone() * power * rate.summerNone()

        if (isNoTOU(structure) && debug) {
            Timber.d("## Non TOU Cost - Summer ##")
            Timber.d(">>> Cost Summer None : $summerTOUNone")
        }

        return summerTOU + summerTOUNone

    }

    /**
     * Calculates the Winter Cost (Proper UsageHours Type Cast done via isTOU() flag check)
     * */
    private fun costWinter(): Double {

        val costWinterPart = hours.winterPart() * power * rate.winterPart()
        val costWinterOff = hours.winterOff() * power * rate.winterOff()

        if (isTOU(structure) && debug) {
            Timber.d("## TOU Cost - Winter ##")
            Timber.d(">>> Winter Part : $costWinterPart")
            Timber.d(">>> Winter Off : $costWinterOff")
        }

        val winterTOU = costWinterPart + costWinterOff
        val winterTOUNone = hours.winterNone() * power * rate.winterNone()

        if (isNoTOU(structure) && debug) {
            Timber.d("## NON TOU Cost - Winter ##")
            Timber.d(">>> Winter None : $winterTOUNone")
        }

        return winterTOU + winterTOUNone

    }

    /**
     * Cost where there is no seasonal segregation
     * */
    private fun costBlendedSeason(): Double {
        val costPeak = hours.peak() * power * rate.weightedAverage()
        val costPartPeak = hours.partPeak() * power * rate.weightedAverage()
        val costNoPeak = hours.noPeak() * power * rate.weightedAverage()

        if (debug) {
            Timber.d("## Blended Cost - No Season ##")
            Timber.d(">>> Cost Peak : $costPeak")
            Timber.d(">>> Cost Part Peak : $costPartPeak")
            Timber.d(">>> Cost No Peak : $costNoPeak")
        }

        return costPeak + costPartPeak + costNoPeak
    }

    private fun logIUsageType() {

        if (isTOU(structure)) {

            val hours = hours as TOU
            val rate = rate as TOU

            if (debug) {
                Timber.d("## TOU Hours ##")
                Timber.d(hours.toString())
                Timber.d("## TOU Rate ##")
                Timber.d(rate.toString())
            }

        } else {

            val hours = hours as TOUNone
            val rate = rate as TOUNone

            if (debug) {
                Timber.d("## Non TOU Hours ##")
                Timber.d(hours.toString())
                Timber.d("## Non TOU Rate ##")
                Timber.d(rate.toString())
            }

        }

    }

    companion object {
        private const val NONE = "none"
        private val regex = "^.*TOU$".toRegex()
        private fun isTOU(rate: String) = rate.matches(regex)
        private fun isNoTOU(rate: String) = !isTOU(rate)
    }

}