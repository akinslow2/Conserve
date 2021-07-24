package com.gemini.energy.service.device.lighting

class Utils {
    companion object Utils {


        val electricianHourlyRate = 75.0



        /**
         * get time to replace a fixture
         *
         * @param replacementType
         * @param ceilingType
         *
         * @return number of hours in double
         */
        fun timePerFixture(replacementType: String, ceilingType: String): Double {
            if (replacementType == ReplacementType.Retrofit.type) {
                if (ceilingType == CeilingType.DryWall.type) { return 1.0 }
                return 0.0
            }

            if (replacementType == ReplacementType.Retrofit.type) {
                if (ceilingType == CeilingType.DryWall.type) { return 1.0 }
                return 0.0
            }

            // return lamp replacement as default
            return 0.5
        }


        /**
         * get cost of electrician based off of time to replace fixture and number
         * of fixtures
         *
         * @param timePerFixture use [timePerFixture] to get this value
         * @param numberOfFixtures
         *
         * @return estimated cost for electrician for replacement
         */
        fun electricianCost(timePerFixture: Double, numberOfFixtures: Int): Double {
            return timePerFixture * numberOfFixtures.toDouble() * electricianHourlyRate
        }
    }
}