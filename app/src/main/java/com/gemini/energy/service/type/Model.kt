package com.gemini.energy.service.type

/**
 * Common interface methods to access the Rates | Hours
 * */
interface IUsageType {
    fun summerOn(): Double
    fun summerPart(): Double
    fun summerOff(): Double
    fun winterPart(): Double
    fun winterOff(): Double

    fun summerNone(): Double
    fun winterNone(): Double

    fun summerExcess(): Double
    fun winterExcess(): Double

    fun peak(): Double
    fun partPeak(): Double
    fun noPeak(): Double

    fun weightedAverage(): Double
}

/**
 * Time Of Use - Scheme [Rate Plus Hours]
 * */
data class TOU(
        private var summerOn: Double,
        private var summerPart: Double,
        private var summerOff: Double,
        private var winterPart: Double,
        private var winterOff: Double,

        private var peak: Double,
        private var partPeak: Double,
        private var noPeak: Double

) : IUsageType {

    constructor(): this(0.0, 0.0, 0.0)

    constructor(summerOn: Double, summerPart: Double, summerOff: Double, winterPart: Double, winterOff: Double):
            this(summerOn, summerPart, summerOff, winterPart, winterOff, 0.0, 0.0, 0.0)

    constructor(peak: Double, partPeak: Double, noPeak: Double): this(0.0, 0.0,
            0.0, 0.0, 0.0, peak, partPeak, noPeak)

    override fun summerOn() = summerOn
    override fun summerPart() = summerPart
    override fun summerOff() = summerOff
    override fun winterPart() = winterPart
    override fun winterOff() = winterOff

    override fun summerNone() = 0.0
    override fun winterNone() = 0.0

    override fun summerExcess() = 0.0
    override fun winterExcess() = 0.0

    override fun peak() = peak
    override fun partPeak() = partPeak
    override fun noPeak() = noPeak

    override fun weightedAverage() = (((summerOn() + summerPart() + summerOff()) / 3) * 0.504) +
            (((winterPart() + winterOff()) / 2) * 0.496)

    override fun toString(): String {
        return ">>> Summer On : $summerOn \n" +
                ">>> Summer Part : $summerPart \n" +
                ">>> Summer Off : $summerOff \n" +
                ">>> Winter Part : $winterPart \n" +
                ">>> Winter Off : $winterOff \n" +

                ">>> Peak : $peak >>> Part Peak : $partPeak >>> No Peak : $noPeak"
    }

}

/**
 * No Time Of Use - Scheme [Rate Plus Hours]
 * */
data class TOUNone(
        private var summerNone: Double,
        private var summerExcess: Double,
        private var winterNone: Double,
        private var winterExcess: Double,
        private var noPeak: Double

) : IUsageType {

    constructor(summerNone: Double, winterNone: Double): this(summerNone, 0.0, winterNone, 0.0, 0.0)
    constructor(noPeak: Double): this(0.0, 0.0, 0.0, 0.0, noPeak)
    constructor(summerNone: Double, summerExcess: Double, winterNone: Double, winterExcess: Double):
            this(summerNone, summerExcess, winterNone, winterExcess, 0.0)

    override fun summerOn() = 0.0
    override fun summerPart() = 0.0
    override fun summerOff() = 0.0
    override fun winterPart() = 0.0
    override fun winterOff() = 0.0

    override fun summerNone() = summerNone
    override fun winterNone() = winterNone

    override fun summerExcess() = summerExcess
    override fun winterExcess() = winterExcess

    override fun peak() = 0.0
    override fun partPeak() = 0.0
    override fun noPeak() = noPeak

    override fun weightedAverage() = (summerNone() * 0.504 + winterNone() * 0.496)

    override fun toString(): String {
        return ">>> Summer None : $summerNone \n" +
                ">>> Summer Excess : $summerExcess \n" +
                ">>> Winter None : $winterNone \n" +
                ">>> Winter Excess : $winterExcess \n" +
                ">>> No Peak : $noPeak"
    }

}
