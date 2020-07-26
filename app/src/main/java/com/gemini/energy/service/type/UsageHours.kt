package com.gemini.energy.service.type

import com.gemini.energy.presentation.util.EDay
import com.gemini.energy.presentation.util.ERateKey
import java.text.SimpleDateFormat
import java.util.*


open class UsageHours {

    /**
     * UsageHours Hours - Could either be from the PreAudit i.e. PRE
     * or could be Device Specific i.e. POST
     * */
    private lateinit var usage: Map<EDay, String?>
    private lateinit var mapper: PeakHourMapper

    /**
     * Average UsageHours - UtilityRate Methods
     * */
    fun daily() = mapper.dailyHours()

    fun weekly() = mapper.weeklyHours()
    open fun yearly() = mapper.yearlyHours()
    open fun postyearly() = mapper.yearlyHours()

    /**
     * Mapped Peak Hour by UtilityRate Rate Structure
     * */
    fun mappedPeakHourDaily() = mapper.mappedHoursDaily()

    fun mappedPeakHourWeekly() = mapper.mappedHoursWeekly()
    fun mappedPeakHourYearly() = mapper.mappedHoursYearly()

    /**
     * Mapped Peak Hour Yearly - Return Time Of Use Data Model
     * */
    open fun timeOfUse(): TOU {
        val usageByPeak = mapper.mappedHoursYearly()
        return TOU(
                usageByPeak[ERateKey.SummerOn]!! * WEIGHT_SUMMER,
                usageByPeak[ERateKey.SummerOff]!! * (WEIGHT_Off / 2),
                usageByPeak[ERateKey.WinterOn]!! * WEIGHT_WINTER,
                usageByPeak[ERateKey.WinterOff]!! * (WEIGHT_Off / 2)
        )
    }

    /**
     * Mapped Peak Hour Yearly - Return Non Time Of Use Data Model
     * */
    open fun nonTimeOfUse(): TOUNone {
        return TOUNone(
                yearly() / 2,
                yearly() / 2
        )
    }

    companion object {
        private const val WEIGHT_SUMMER = .333
        private const val WEIGHT_Off = .334
        private const val WEIGHT_WINTER = .333
    }

    fun initUsage(usage: Map<EDay, String?>): UsageHours {
        this.usage = usage
        return this
    }

    fun build(): UsageHours {
        this.mapper = PeakHourMapper().run(usage)
        return this
    }

    /**
     * Different UtilityRate Company would have their own Specific Peak Hour Mapper
     * */
    class PeakHourMapper {

        private var outgoing = hashMapOf<ERateKey, Double>()
        private var aggregate: Double = 0.0

        init {
            ERateKey.getAllElectric().forEach { outgoing[it] = 0.0 }
        }

        /**
         * 1. Loop through each of the Day
         * 2. Extract the UsageHours Hours for the day
         * 3. Loop until Current (start time) is less than the End (end time)
         * 4. For each delta which is 1 min figure out where it goes in terms of the TOU
         * 5. The Aggregate holds the number in minutes for the entire Weekly UsageHours Hours
         * */

        fun run(usage: Map<EDay, String?>): PeakHourMapper {

            for ((_, hourRange) in usage) {

                hourRange?.split(",")?.forEach { time ->

                    val component = time.split("\\s+".toRegex())
                    if (component.count() == 2) {

                        val t1 = component[0]
                        val t2 = component[1]

                        val start = getTime(t1)
                        val end = getTime(t2)

                        if (end > start) {
                            val delta = 1
                            val calendar = Calendar.getInstance()
                            var current = start

                            while (current < end) {

                                if (isSummerOffPeak(current)) {
                                    val tmp = outgoing[ERateKey.SummerOff]
                                    outgoing[ERateKey.SummerOff] = tmp!! + delta
                                }

                                if (isSummerPeak(current)) {
                                    val tmp = outgoing[ERateKey.SummerOn]
                                    outgoing[ERateKey.SummerOn] = tmp!! + delta
                                }

                                if (isWinterOffPeak(current)) {
                                    val tmp = outgoing[ERateKey.WinterOff]
                                    outgoing[ERateKey.WinterOff] = tmp!! + delta
                                }

                                if (isWinterPeak(current)) {
                                    val tmp = outgoing[ERateKey.WinterOn]
                                    outgoing[ERateKey.WinterOn] = tmp!! + delta
                                }

                                calendar.time = current
                                calendar.add(Calendar.MINUTE, delta)
                                current = calendar.time

                                aggregate += 1
                            }
                        }

                    }
                }
            }


            return this
        }

        /**
         * The Aggregate is in Min divide it by 60 to get the Number of Hours
         * */
        fun weeklyHours() = aggregate / 60

        /**
         * Dividing the Weekly UsageHours Hours by 7
         * */
        fun dailyHours() = weeklyHours() / 7

        /**
         * Multiplying the Daily Hours by 365
         * */
        fun yearlyHours() = dailyHours() * 365

        /**
         * Since the Aggregate is in Minutes we divide it by 60 to get Hours and Since this is the UsageHours Hours is
         * for the Week we divide it by 7 to get the Daily UsageHours Hours Mapped for that TOU
         * */
        fun mappedHoursDaily(): HashMap<ERateKey, Double> {
            val clone = outgoing.clone() as HashMap<ERateKey, Double>
            ERateKey.getAllElectric().forEach {
                val tmp = clone[it]
                clone[it] = tmp!! / (60 * 7)
            }

            return clone
        }

        /**
         * Dividing by 60 to get the Weekly Hours - Cause it's the Aggregate that is in Minutes
         * */
        fun mappedHoursWeekly(): HashMap<ERateKey, Double> {
            val clone = outgoing.clone() as HashMap<ERateKey, Double>
            ERateKey.getAllElectric().forEach {
                val tmp = clone[it]
                clone[it] = tmp!! / 60
            }

            return clone
        }

        /**
         * Dividing Aggregate by 60 gives the Hours
         * Dividing by 7 gives the Daily Hours
         * Multiplying by 365 gives the Hours for the Year
         * */
        fun mappedHoursYearly(): HashMap<ERateKey, Double> {
            val clone = outgoing.clone() as HashMap<ERateKey, Double>
            ERateKey.getAllElectric().forEach {
                val tmp = clone[it]
                clone[it] = (tmp!! / (60 * 7)) * 365
            }

            return clone
        }

        companion object {

            private val dateFormatter = SimpleDateFormat("HH:mm", Locale.ENGLISH)
            private const val TAG = "PeakHourMapper"

            private fun getTime(time: String) = dateFormatter.parse(time)

            private fun inBetween(now: Date, start: Date, end: Date): Boolean {
                val a = now.time
                val b = start.time
                val c = end.time

                return (a >= b) && (a < c)
            }

            private fun isSummerPeak(now: Date) = inBetween(now, getTime("13:00"), getTime("19:00"))

            private fun isSummerOffPeak(now: Date) = inBetween(now, getTime("00:00"), getTime("13:00")) ||
                    inBetween(now, getTime("19:00"), getTime("00:00"))

            private fun isWinterPeak(now: Date) = inBetween(now, getTime("04:00"), getTime("10:00"))


            private fun isWinterOffPeak(now: Date) = inBetween(now, getTime("10:01"), getTime("00:00")) ||
                    inBetween(now, getTime("00:00"), getTime("06:00"))

        }
    }
}
