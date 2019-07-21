package com.gemini.energy.service.type

import android.content.Context
import com.gemini.energy.presentation.util.ERateKey
import timber.log.Timber

open class UtilityRate(private val context: Context) {

    /**
     * The Rate Structure Gets Setup from the PreAudit
     * */
    lateinit var content: String
    lateinit var structure: HashMap<String, List<String>>
    lateinit var utility: IUtility

    /**
     * To be set by the Child Class later on
     * */
    private fun getResourcePath() = utility.getResourcePath()

    private fun getSeparator() = utility.getSeparator()
    private fun getRowIdentifier() = utility.getRowIdentifier()

    fun initUtility(utility: IUtility): UtilityRate {
        this.utility = utility
        return this
    }

    fun build(): UtilityRate {
        val inputStream = context.resources.assets.open(getResourcePath())
        val text = inputStream.bufferedReader().use { it.readText() }
        val collection = text.lines()
        val outgoing: HashMap<String, List<String>> = hashMapOf()

        if (collection.count() > 0) {
            val header = collection[0]
            collection.drop(0)
            collection.forEach {
                if (it.matches(getRowIdentifier())) {
                    val result = parseLine(it, getSeparator())
                    utility.getKey(result)
                            .forEachIndexed { index, key ->
                                outgoing[key] = utility.getValue(result, header)[index]
                            }
                }
            }
        }

        this.structure = outgoing
        return this
    }

    /**
     * Mapped Peak UtilityRate Rate Structure - Return Time Of Use Data Model
     * */
    fun timeOfUse() = utility.getTOU(structure)

    /**
     * Mapped Peak UtilityRate Rate Structure - Return Non Time Of Use Data Model
     * This could either return Gas or Electricity - how do i achieve this dynamically ??
     * */
    fun nonTimeOfUse() = utility.getNoneTOU(structure)

    companion object {

        private fun parseLine(line: String, separator: Char): List<String> {
            val result = mutableListOf<String>()
            val builder = StringBuilder()
            var quotes = 0

            for (ch in line) {
                when {
                    ch == '\"' -> {
                        quotes++
                        builder.append(ch)
                    }
                    (ch == '\n') || (ch == '\r') -> {
                    }
                    (ch == separator) && (quotes % 2 == 0) -> {
                        val tmp = quotes % 2
                        result.add(builder.toString().trim())
                        builder.setLength(0)
                    }
                    else -> builder.append(ch)
                }
            }

            return result
        }

    }

}

interface IUtility {
    fun getKey(columns: List<String>): List<String>
    fun getValue(columns: List<String>, header: String): List<List<String>>

    fun getResourcePath(): String
    fun getSeparator(): Char
    fun getRowIdentifier(): Regex
    fun getRate(): String

    fun getTOU(structure: HashMap<String, List<String>>): TOU
    fun getNoneTOU(structure: HashMap<String, List<String>>): TOUNone
}

class Electricity(private val rateStructure: String, private val companyCode: String) : IUtility {

    enum class EKey(val index: Int) { Season(1), Peak(4) }
    enum class EValue(val index: Int) { EnergyCharge(5), Average(6), Demand(3) }

    override fun getKey(columns: List<String>) = listOf(columns[EKey.Season.index] + "-" + columns[EKey.Peak.index])
    override fun getValue(columns: List<String>, header: String) = listOf(listOf(columns[EValue.EnergyCharge.index],
            columns[EValue.Average.index], columns[EValue.Demand.index]))

    override fun getResourcePath() = "utility/${companyCode}_electric.csv"
    override fun getSeparator(): Char = ','
    override fun getRate() = rateStructure
    override fun getRowIdentifier() = "^${getRate()}${getSeparator()}.*".toRegex()

    override fun getTOU(structure: HashMap<String, List<String>>) = TOU(
            structure[ERateKey.SummerOn.value]!![0].toDouble(),
            structure[ERateKey.SummerPart.value]!![0].toDouble(),
            structure[ERateKey.SummerOff.value]!![0].toDouble(),
            structure[ERateKey.WinterPart.value]!![0].toDouble(),
            structure[ERateKey.WinterOff.value]!![0].toDouble())

    override fun getNoneTOU(structure: HashMap<String, List<String>>) = TOUNone(
            structure[ERateKey.SummerNone.value]!![0].toDouble(),
            structure[ERateKey.WinterNone.value]!![0].toDouble())
}

class Gas(private val rateStructure: String = "") : IUtility {

    override fun getKey(columns: List<String>) = keys
    override fun getValue(columns: List<String>, header: String): List<List<String>> {
        val outgoing: MutableList<List<String>> = mutableListOf()
        val lookup = header.split(getSeparator())
        keys.forEach {
            outgoing.add(listOf(columns[lookup.indexOf(it)]))
        }

        return outgoing
    }

    override fun getResourcePath() = "utility/pge_gas.csv"
    override fun getSeparator() = ','
    override fun getRowIdentifier(): Regex {
        return ".*".toRegex()
    }

    override fun getRate() = rateStructure

    override fun getTOU(structure: HashMap<String, List<String>>) = TOU()

    override fun getNoneTOU(structure: HashMap<String, List<String>>) = TOUNone(
            structure[ERateKey.GasSummer.value]!![0].toDouble(),
            structure[ERateKey.SummerExcess.value]!![0].toDouble(),
            structure[ERateKey.GasWinter.value]!![0].toDouble(),
            structure[ERateKey.WinterExcess.value]!![0].toDouble())

    companion object {
        private val keys = listOf(
                ERateKey.Slab1.value, ERateKey.Slab2.value, ERateKey.Slab3.value,
                ERateKey.Slab4.value, ERateKey.Slab5.value, ERateKey.Surcharge.value,
                ERateKey.SummerTransport.value, ERateKey.WinterTransport.value,
                ERateKey.SummerExcess.value, ERateKey.WinterExcess.value)

        const val FIRST_SLAB = 4000.0

    }

}