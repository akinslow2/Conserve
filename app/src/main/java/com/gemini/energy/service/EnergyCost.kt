package com.gemini.energy.service

import com.gemini.energy.service.type.*

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

        val summerTOU = costSummerOn + costSummerPart + costSummerOff
        val summerTOUNone = hours.summerNone() * power * rate.summerNone()

        return summerTOU + summerTOUNone

    }

    /**
     * Calculates the Winter Cost (Proper UsageHours Type Cast done via isTOU() flag check)
     * */
    private fun costWinter(): Double {

        val costWinterPart = hours.winterPart() * power * rate.winterPart()
        val costWinterOff = hours.winterOff() * power * rate.winterOff()

        val winterTOU = costWinterPart + costWinterOff
        val winterTOUNone = hours.winterNone() * power * rate.winterNone()

        return winterTOU + winterTOUNone

    }

    /**
     * Cost where there is no seasonal segregation
     * */
    private fun costBlendedSeason(): Double {
        val costPeak = hours.peak() * power * rate.weightedAverage()
        val costPartPeak = hours.partPeak() * power * rate.weightedAverage()
        val costNoPeak = hours.noPeak() * power * rate.weightedAverage()

        return costPeak + costPartPeak + costNoPeak
    }

    companion object {
        private const val NONE = "none"
        private val regex = "^.*TOU$".toRegex()
        private fun isTOU(rate: String) = rate.matches(regex)
        private fun isNoTOU(rate: String) = !isTOU(rate)
    }

}
