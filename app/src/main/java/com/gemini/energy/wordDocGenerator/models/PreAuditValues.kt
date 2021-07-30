package com.gemini.energy.wordDocGenerator.models

import com.gemini.energy.service.device.EBase
import com.gemini.energy.wordDocGenerator.AuditComponents

/**
 * PreAudit values for word doc generation
 */
data class PreAuditValues(
        val businessname: String, //1
        val auditmonth: String, //2
        val audityear: String, // 3
        val clientaddress: String, // 4
        val startday: String, // 5
        val endday: String, //6
        val operationhours: String, //8
        val bldgarea: Double, // 7
        val bldgtype: String, // 12
        val electricstructure: String, //10
        val utilitycompany: String, //9
        val gasstructure: String, // 11
        val electrificationSavings: Double
) {
    companion object {

        /**
         * attempt to create preAudit values from audit components
         *
         * @param audit sorted audit components
         *
         * @return [PreAuditValues] if successful
         * @return null if data will not successfully create [PreAuditValues]
         */
        fun tryCreate(audit: AuditComponents): PreAuditValues? {
            val allEquipment = audit.flatMap { it.value }
            val distinctPreAuditValues = allEquipment.distinctBy { it.preAudit }

            val equipment = distinctPreAuditValues.firstOrNull()
            if (!isPreAuditValidFromEbase(equipment)) return null

            val preAudit = equipment!!.preAudit

            val address = concatenateAddress(preAudit)
            val electrificationSavings = calculateCustomerCharge(equipment)

            return PreAuditValues(
                    preAudit["General Client Info Business Name"]!! as String,
                    preAudit["General Client Info Audit Month"]!! as String,
                    preAudit["General Client Info Audit Year"]!! as String,
                    address,
                    preAudit["General Client Info Assessment Start Day"]!! as String,
                    preAudit["General Client Info Assessment End Day"]!! as String,
                    preAudit["Operation Hours Monday Operating Hours"]!! as String,
                    preAudit["Area Total (Sq.Ft.)"]!! as Double,
                    preAudit["General Client Info Facility Type"]!! as String,
                    preAudit["Others Electric Rate Structure"]!! as String,
                    preAudit["Others Utility Company"]!! as String,
                    preAudit["Others Gas Rate Structure"]!! as String,
                    electrificationSavings
            )
        }


        /**
         * check if the equipment has valid preaudit data
         *
         * @param equipment equipment instance that holds preaudit data
         *
         * @return true if data is valid to create a [PreAuditValues]
         */
        private fun isPreAuditValidFromEbase(equipment: EBase?): Boolean {
            if (equipment == null) return false

            val preAudit = equipment.preAudit

            return preAudit.containsKey("General Client Info Business Name")
                    && preAudit.containsKey("General Client Info Audit Month")
                    && preAudit.containsKey("General Client Info Audit Year")
                    && preAudit.containsKey("General Client Info Address Line 1")
                    && preAudit.containsKey("General Client Info Address Line 2")
                    && preAudit.containsKey("General Client Info City")
                    && preAudit.containsKey("General Client Info State")
                    && preAudit.containsKey("General Client Info Zip Code")
                    && preAudit.containsKey("General Client Info Assessment Start Day")
                    && preAudit.containsKey("General Client Info Assessment End Day")
                    && preAudit.containsKey("Operation Hours Monday Operating Hours")
                    && preAudit.containsKey("Area Total (Sq.Ft.)")
                    && preAudit.containsKey("General Client Info Facility Type")
                    && preAudit.containsKey("Others Electric Rate Structure")
                    && preAudit.containsKey("Others Utility Company")
                    && preAudit.containsKey("Others Gas Rate Structure")
        }


        /**
         * concatenate address from preaudit
         *
         * @param preAudit preaudit values
         *
         * @return formatted address
         */
        private fun concatenateAddress(preAudit: Map<String, Any>): String {
            val addressLine1 = preAudit["General Client Info Address Line 1"]
            val addressLine2 = preAudit["General Client Info Address Line 2"]
            val city = preAudit["General Client Info City"]
            val state = preAudit["General Client Info State"]
            val zipCode = preAudit["General Client Info Zip Code"]
            return "$addressLine1 $addressLine2 $city, $state $zipCode"
        }


        /**
         * get inputs for calculating customer charge
         *
         * @param preAudit preAudit values
         *
         * @return list of slab rates for customer charge calculation
         */
        private fun getSlabValuesFromPreAudit(preAudit: Map<String, Any>): List<String> {
            val jan = preAudit["Gas Slab Rate January"] as String
            val feb = preAudit["Gas Slab Rate February"] as String
            val mar = preAudit["Gas Slab Rate March"] as String
            val apr = preAudit["Gas Slab Rate April"] as String
            val may = preAudit["Gas Slab Rate May"] as String
            val jun = preAudit["Gas Slab Rate June"] as String
            val jul = preAudit["Gas Slab Rate July"] as String
            val aug = preAudit["Gas Slab Rate August"] as String
            val sep = preAudit["Gas Slab Rate September"] as String
            val oct = preAudit["Gas Slab Rate October"] as String
            val nov = preAudit["Gas Slab Rate November"] as String
            val dec = preAudit["Gas Slab Rate December"] as String

            return listOf(
                    jan, feb, mar, apr,
                    may, jun, jul, aug,
                    sep, oct, nov, dec
            )
        }


        /**
         * get number of days in a month
         * [month] is 0 inclusive so 0 = january
         *
         * @param month month to get number of days in
         *
         * @return number of days in [month]
         */
        private fun getDaysForMonth(month: Int): Double {
            return when (month) {
                0, 2, 4, 6, 7, 9, 11 -> 31.0
                1 -> 28.0
                3, 5, 8, 10 -> 30.0
                else -> 0.0
            }
        }


        /**
         * calculate customer charge to access gas utility
         *
         * @param equipment equipment with access to gas utility and
         *          pre audit information
         *
         * @return yearly charge to access gas utility
         */
        private fun calculateCustomerCharge(equipment: EBase): Double {
            val electricityRetroFit = equipment.preAudit["Additions Will all gas be removed with retrofit?"] == "yes"
            if (!electricityRetroFit) return 0.0

            val utility = equipment.gasRate
            var totalCustomerCharge = 0.0

            val monthSlabs = getSlabValuesFromPreAudit(equipment.preAudit)

            for ((month, rate) in monthSlabs.withIndex()) {
                val slabRate = utility.getSlab(rate)
                val days = getDaysForMonth(month)
                val monthCharge = slabRate * days
                totalCustomerCharge += monthCharge
            }

            return totalCustomerCharge
        }
    }
}