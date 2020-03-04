package com.gemini.energy.service.type

class UsageHVAC(usageHours: UsageHours, isTOU: Boolean,
                private val hoursExtract: Int) : UsageHours() {

    /**
     * Total Hours could be Business | Specific
     * */
    private val total = usageHours.yearly()

    /**
     * Mapped Hours differs with the TOU | No TOU
     * */
    private val mappedHours = if (isTOU) usageHours.timeOfUse() else
        usageHours.nonTimeOfUse()

    /**
     * Ratio for each of the Peaks | Part Peak | No Peak
     * Get the Specific Mapped Hours for each of the above cases and dividing by the Total Platform Hours
     * */
    private fun getPeakRatio() = mappedHours.summerOn() / total
    private fun getPartPeakRatio() = (mappedHours.summerPart() + mappedHours.winterPart()) / total
    private fun getOffPeakRatio() = (mappedHours.summerOff() + mappedHours.winterOff() +
            mappedHours.summerNone() + mappedHours.winterNone()) / total

    /**
     * Used to Define TOU | TOUNone
     * */
    private var peakHours = 0.0
    private var partPeakHours = 0.0
    private var offPeakHours = 0.0

    init {
        peakHours = hoursExtract * getPeakRatio()
        partPeakHours = hoursExtract * getPartPeakRatio()
        offPeakHours = hoursExtract * getOffPeakRatio()
    }

    override fun timeOfUse(): TOU {
        return TOU(peakHours, partPeakHours, offPeakHours)
    }

    override fun nonTimeOfUse(): TOUNone {
        return TOUNone(offPeakHours)
    }

    override fun yearly() = peakHours + partPeakHours + offPeakHours

    override fun toString(): String {
        return ">>> Extracted Total Hours : $hoursExtract \n" +
                ">>> Internal Total Hours : $total \n" +
                ">>> Peak Hours : $peakHours \n" +
                ">>> Part Peak Hours : $partPeakHours \n" +
                ">>> Off Peak Hours : $offPeakHours \n" +
                ">>> Ratio Peak : ${getPeakRatio()} \n" +
                ">>> Ratio Part Peak : ${getPartPeakRatio()} \n" +
                ">>> Ratio Off Peak : ${getOffPeakRatio()}"
    }
}