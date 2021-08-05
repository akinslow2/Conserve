package com.gemini.energy.wordDocGenerator.models

import com.gemini.energy.service.device.Hvac

data class HvacInstances(
        val quantity: Int, // 21
        val year: Int, // 22
        val age: Int, // 29
        val btu: Int, //23
        val seer: Double, // 24
        val overage: Int, //25
        val economizer: String,
        val thermotype: String,
        val npvalue: Double
) {
    companion object {
        fun createFromHvac(hvac: Hvac): HvacInstances {
            return HvacInstances(
                        hvac.quantity,
                        hvac.year(),
                        hvac.age,
                        hvac.kbtu.toInt(),
                        hvac.seer,
                        hvac.overAge(),
                        hvac.economizer,
                        hvac.thermotype,
                        hvac.netPresentValue
                )
        }
    }
}