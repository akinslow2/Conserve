package com.gemini.energy

import java.text.DecimalFormat

fun Double.format(fracDigits: Int): String {
    val df = DecimalFormat()
    df.maximumFractionDigits = fracDigits
    return df.format(this)
}

/// Determines which filepath to save exported files to
val branch = Branches.PGE.string

enum class Branches(val string: String) {
    BED("BED"),
    NES("NES"),
    PGE("PGE")
}