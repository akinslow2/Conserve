package com.gemini.energy.service.device.lighting

class Utils {
    companion object Utils {

        private const val electricianHourlyRate = 70.0

        /**
         * gets fixture cost for linear fluorescent
         *
         * @param fixtureType
         *
         * @return fixture cost based on fixture type
         */
        fun fixtureCost(fixtureType: String): Double {
            if (fixtureType == LfFixtureType.twoXfour.type)
                return 120.0
            return 60.0
        }

        /**
         * get time to replace a fixture
         *
         * @param replacementType
         * @param ceilingType
         *
         * @return number of hours in double
         * @k2, I removed the ceiling type because it currently requires the data collector to know that retrofit goes with drywall. If they mess up then the cost is impacted changing from 1 to 0.5/
         * Since both retrofit and DirectReplacement Fixture are 1 hour, the ceiling type doesn't matter in this particular decision making. Please check for correct coding.
         */
        fun timePerFixture(replacementType: String, ceilingType: String): Double {
            if (replacementType == ReplacementType.Retrofit.type) {
                 return 1.0
            }

            if (replacementType == ReplacementType.DirectReplacementFixture.type) {
                 return 1.0
            }

            // return lamp replacement as default
            return 0.5
        }


        /**
         * get cost of electrician based off of time to replace fixture and number
         * of fixtures
         *
         * @param timePerFixture use [timePerFixture] to get this value
         * @param numberOfFixtures number of fixtures
         *
         * @return estimated cost for electrician for replacement
         */
        fun electricianCost(timePerFixture: Double, numberOfFixtures: Int): Double {
            return timePerFixture * numberOfFixtures.toDouble() * electricianHourlyRate
        }


        /**
         * get the installation cost
         *
         * @param replacementType
         * @param ledBulbCost cost of replacement bulb
         * @param lampsPerFixture number of lamps in each fixture
         * @param numberOfFixtures number of fixtures
         * @param electricianCost use [timePerFixture] to get this value
         *
         * @return installation cost
         * @k2 missing directReplaceLFInstall in the install cost...
         * @Anthony the implementation of installCost() in LinearFluorescent.kt checks if the replacement type is direct replacement
         * and if it is it calls [directReplacementInstallCostLinearFluorescent]
         * the unused (and now removed) function directReplaceLFInstall returned the same value as [directReplacementInstallCostLinearFluorescent]
         */
        fun installCost(
                replacementType: String,
                ledBulbCost: Double,
                lampsPerFixture: Int,
                numberOfFixtures: Int,
                electricianCost: Double): Double
        {
            if (replacementType == ReplacementType.Retrofit.type)
                return retrofitInstall(ledBulbCost, lampsPerFixture, numberOfFixtures, electricianCost)

            if (replacementType == ReplacementType.DirectReplacementFixture.type)
                return directReplaceInstall(ledBulbCost, lampsPerFixture, numberOfFixtures, electricianCost)

            // default to lamp replacement
            return replacementInstall(ledBulbCost, lampsPerFixture, numberOfFixtures, electricianCost)
        }


        /**
         * get the installation cost for direct replacement on a linear fluorescent instance
         *      for linear fluorescent ONLY
         *
         * @param fixtureCost use [fixtureCost] to get the value for this parameter
         * @param numberOfFixtures number of fixtures to replace
         * @param electricianCost use [electricianCost] to get the value for this parameter
         *
         * @return installation cost for direct replacement of linear fluorescent
         */
        fun directReplacementInstallCostLinearFluorescent(fixtureCost: Double, numberOfFixtures: Int, electricianCost: Double): Double {
            return (fixtureCost * numberOfFixtures) + electricianCost
        }


        // MARK: private methods

        /**
         * Customer wants contractor to replace lamps
         *
         * @param ledBulbCost cost of replacement bulb
         * @param lampsPerFixture number of lamps in each fixture
         * @param numberOfFixtures number of fixtures
         * @param electricianCost use [timePerFixture] to get this value
         *
         * @return replacement cost
         */
        private fun replacementInstall(ledBulbCost: Double, lampsPerFixture: Int, numberOfFixtures: Int, electricianCost: Double): Double {
            return (ledBulbCost * lampsPerFixture * numberOfFixtures) + electricianCost
        }


        /**
         * Customer wants contractor to retrofit fixtures
         *
         * @param ledBulbCost cost of replacement bulb
         * @param lampsPerFixture number of lamps in each fixture
         * @param numberOfFixtures number of fixtures
         * @param electricianCost use [timePerFixture] to get this value
         *
         * @return retrofit install cost
         */
        private fun retrofitInstall(ledBulbCost: Double, lampsPerFixture: Int, numberOfFixtures: Int, electricianCost: Double): Double {
            return (ledBulbCost * lampsPerFixture * numberOfFixtures) + electricianCost
        }


        /**
         * Customer wants contractor to direct replace fixtures
         *
         * @param ledBulbCost cost of replacement bulb
         * @param lampsPerFixture number of lamps in each fixture
         * @param numberOfFixtures number of fixtures
         * @param electricianCost use [timePerFixture] to get this value
         *
         * @return direct replacement cost
         */
        private fun directReplaceInstall(ledBulbCost: Double, lampsPerFixture: Int, numberOfFixtures: Int, electricianCost: Double): Double {
            return (ledBulbCost * lampsPerFixture * numberOfFixtures) + electricianCost
        }
    }
}