package com.gemini.energy.service.type

class UsageLighting : UsageHours() {

    var peakHours = 0.0
    var partPeakHours = 0.0
    var offPeakHours = 0.0
    var postpeakHours = 0.0
    var postpartPeakHours = 0.0
    var postoffPeakHours = 0.0

    override fun timeOfUse(): TOU {
        return TOU(peakHours, offPeakHours)
    }

    override fun nonTimeOfUse(): TOUNone {
        return TOUNone(offPeakHours)
    }

    override fun yearly() = peakHours + partPeakHours + offPeakHours
    override fun postyearly() = postpeakHours + postpartPeakHours + postoffPeakHours
}