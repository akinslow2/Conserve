package com.gemini.energy.wordDocGenerator.models

import com.gemini.energy.service.device.Hvac

data class HvacInstances(
        val quantity: Int, // 21
        val type: String,
        val location: String,
        val year: Int, // 22
        val age: Int, // 29
        val btu: Int, //23
        val seer: Double, // 24
        val overage: Int, //25
        val economizer: String,
        val thermotype: String,
        val npvalue: Double,
        val suggestedIntervention: String
) {
    companion object {
        fun createFromHvac(hvac: Hvac): HvacInstances {

            return HvacInstances(
                    quantity = hvac.quantity,
                    type = hvac.typeOfUnit,
                    location = hvac.computable.zoneName,
                    year = hvac.year,
                    age = hvac.getAge(hvac.year),
                    btu = hvac.kbtu.toInt(),
                    seer = hvac.seer,
                    overage = hvac.overAge(),
                    economizer = hvac.economizer,
                    thermotype = hvac.thermotype,
                    npvalue = hvac.netPresentValue,
                    suggestedIntervention = if (hvac.overAge() > 0) "Replace" else "Add Controls"
                )
        }
    }
}