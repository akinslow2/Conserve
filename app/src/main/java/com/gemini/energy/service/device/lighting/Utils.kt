package com.gemini.energy.service.device.lighting

class Utils {
    companion object Utils {
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
    }
}