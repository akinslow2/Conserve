package com.gemini.energy.service.type

class UsageMotors : UsageHours() {

    var peakHours = 0.0
    var partPeakHours = 0.0
    var offPeakHours = 0.0

    override fun timeOfUse(): TOU {
        return TOU(peakHours, partPeakHours, offPeakHours)
    }

    override fun nonTimeOfUse(): TOUNone {
        return TOUNone(offPeakHours)
    }

    override fun yearly() = peakHours + partPeakHours + offPeakHours
}