package com.gemini.energy

import com.gemini.energy.presentation.util.EDay
import com.gemini.energy.presentation.util.ERateKey
import com.gemini.energy.service.type.UsageHours
import org.junit.Assert.assertEquals
import org.junit.Test


class UsageHoursTest {

    @Test
    fun testPeakHourMapper() {

        val usage1 = hashMapOf(EDay.Mon to "8:30 12:30")
        val usage2 = hashMapOf(
                EDay.Mon to "8:30 12:30,14:50 20:00",
                EDay.Tue to "12:00 1:00,8:00 12:30",
                EDay.Fri to "7:30 11:40")

        // *** Test Case 1 *** //

        val energyUsageOne = UsageHours()
                .initUsage(usage1)
                .build()

        assertEquals(0.5714, energyUsageOne.daily(), 0.001)
        assertEquals(4.0, energyUsageOne.weekly(), 0.01)
        assertEquals(208.5714, energyUsageOne.yearly(), 0.01)

        val mappedDaily = energyUsageOne.mappedPeakHourDaily()
        assertEquals(0.0714, mappedDaily[ERateKey.SummerOn].toString().toDouble(), 0.01)
        assertEquals(0.5, mappedDaily[ERateKey.SummerPart].toString().toDouble(), 0.01)
        assertEquals(0.571, mappedDaily[ERateKey.WinterPart].toString().toDouble(), 0.01)

        val mappedWeeklyOne = energyUsageOne.mappedPeakHourWeekly()
        assertEquals(0.5, mappedWeeklyOne[ERateKey.SummerOn].toString().toDouble(), 0.01)
        assertEquals(3.5, mappedWeeklyOne[ERateKey.SummerPart].toString().toDouble(), 0.01)
        assertEquals(4.0, mappedWeeklyOne[ERateKey.WinterPart].toString().toDouble(), 0.01)

        val mappedYearly = energyUsageOne.mappedPeakHourYearly()
        assertEquals(26.07, mappedYearly[ERateKey.SummerOn].toString().toDouble(), 0.01)
        assertEquals(182.5, mappedYearly[ERateKey.SummerPart].toString().toDouble(), 0.01)
        assertEquals(208.57, mappedYearly[ERateKey.WinterPart].toString().toDouble(), 0.01)

        // *** Test Case 2 *** //

        val energyUsageTwo = UsageHours()
                .initUsage(usage2)
                .build()

        /**
         * These are used during non TOU
         * */
        assertEquals(2.548, energyUsageTwo.daily(), 0.02)
        assertEquals(17.833, energyUsageTwo.weekly(), 0.15)
        assertEquals(929.880, energyUsageTwo.yearly(), 0.01)

        /**
         * Mapped TOU by Daily
         * */
        val mappedDailyTwo = energyUsageTwo.mappedPeakHourDaily()
        assertEquals(0.5952, mappedDailyTwo[ERateKey.SummerOn].toString().toDouble(), 0.01)
        assertEquals(0.2142, mappedDailyTwo[ERateKey.SummerOff].toString().toDouble(), 0.01)
        assertEquals(1.7381, mappedDailyTwo[ERateKey.SummerPart].toString().toDouble(), 0.01)

        assertEquals(0.21428, mappedDailyTwo[ERateKey.WinterOff].toString().toDouble(), 0.01)
        assertEquals(2.3333, mappedDailyTwo[ERateKey.WinterPart].toString().toDouble(), 0.01)

        /**
         * Mapped TOU by Week
         * */
        val mappedWeeklyTwo = energyUsageTwo.mappedPeakHourWeekly()
        assertEquals(4.166, mappedWeeklyTwo[ERateKey.SummerOn].toString().toDouble(), 0.01)
        assertEquals(1.5, mappedWeeklyTwo[ERateKey.SummerOff].toString().toDouble(), 0.01)
        assertEquals(12.166, mappedWeeklyTwo[ERateKey.SummerPart].toString().toDouble(), 0.01)

        assertEquals(1.5, mappedWeeklyTwo[ERateKey.WinterOff].toString().toDouble(), 0.01)
        assertEquals(16.333, mappedWeeklyTwo[ERateKey.WinterPart].toString().toDouble(), 0.01)

        /**
         * Mapped TOU by Year
         * */
        val mappedYearlyTwo = energyUsageTwo.mappedPeakHourYearly()
        assertEquals(217.2619, mappedYearlyTwo[ERateKey.SummerOn].toString().toDouble(), 0.01)
        assertEquals(78.2142, mappedYearlyTwo[ERateKey.SummerOff].toString().toDouble(), 0.01)
        assertEquals(634.4047, mappedYearlyTwo[ERateKey.SummerPart].toString().toDouble(), 0.01)

        assertEquals(78.2142, mappedYearlyTwo[ERateKey.WinterOff].toString().toDouble(), 0.01)
        assertEquals(851.667, mappedYearlyTwo[ERateKey.WinterPart].toString().toDouble(), 0.01)

    }


}
