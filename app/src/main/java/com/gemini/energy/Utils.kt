package com.gemini.energy

import java.text.DecimalFormat

fun Double.format(fracDigits: Int): String {
    val df = DecimalFormat()
    df.maximumFractionDigits = fracDigits
    return df.format(this)
}