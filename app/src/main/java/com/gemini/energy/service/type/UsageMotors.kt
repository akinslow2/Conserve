package com.gemini.energy.service.type

class UsageMotors : UsageHours() {

    var peakHours = 0.0
    var partPeakHours = 0.0
    var offPeakHours = 0.0

    override fun timeOfUse() = TOU(peakHours, offPeakHours)

    override fun nonTimeOfUse() = TOUNone(offPeakHours)

    override fun yearly() = peakHours + partPeakHours + offPeakHours
}