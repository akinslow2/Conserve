package com.gemini.energy.service.device.lighting

class Utils {
    companion object Utils {

        /**
         * future value factor
         * assuming 7% compound interest factor
         */
        const val futureValueFactor = 0.7130

        const val electricianHourlyRate = 70.0

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
         *
         * @return number of hours in double
         */
        fun timePerFixture(replacementType: String): Double {
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


        /**
         * Customer wants contractor to direct replace fixtures (for linear fluorescent ONLY)
         *
         * @param fixtureType type of fixture
         * @param numberOfFixtures number of fixtures
         * @param electricianCost use [timePerFixture] to get this value
         *
         * @return direct replacement cost (for linear fluorescent ONLY)
         */
        private fun directReplaceLFInstall(fixtureType: String, numberOfFixtures: Int, electricianCost: Double): Double {
            val fixtureCost = if (fixtureType == LfFixtureType.twoXtwo.type)  60.0 else 120.0

            return (fixtureCost * numberOfFixtures.toDouble()) + electricianCost
        }
    }
}