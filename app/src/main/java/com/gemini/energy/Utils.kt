package com.gemini.energy

import java.text.DecimalFormat

fun Double.format(fracDigits: Int): String {
    val df = DecimalFormat()
    df.maximumFractionDigits = fracDigits
    return df.format(this)
}

/**
 * get string representation of value if exists, or [defaultValue]
 *
 *
 * when [this] == 1    return "1"
 *
 * when [this] == 1.0  return "1.0"
 *
 * when [this] == "1"  return "1"
 *
 * when [this] == null return [defaultValue]
 */
fun Any?.toStringOr(defaultValue: String): String {
    if (this == null) return defaultValue
    return this.toString()
}


/**
 * get int representation of value if exists, or [defaultValue]
 * if value is a double, this will return [defaultValue]: use with caution
 *
 *
 * when [this] == 1     return 1
 *
 * when [this] == 1.0   return [defaultValue]
 *
 * when [this] == "1"   return 1
 *
 * when [this] == "1.0" return [defaultValue]
 *
 * when [this] == null  return [defaultValue]
 */
fun Any?.toIntOr(defaultValue: Int): Int {
    try {
        return this?.toString()?.toIntOrNull() ?: defaultValue
    } catch (e: Exception) {
        return defaultValue
    }
}

/**
 * get double representation of value if exists, or [defaultValue]
 * if value is a int, this will return int as double
 *
 *
 * when [this] == 1     return 1.0
 *
 * when [this] == 1.0   return 1.0
 *
 * when [this] == "1"   return 1.0
 *
 * when [this] == "1.0" return 1.0
 *
 * when [this] == null  return [defaultValue]
 */
fun Any?.toDoubleOr(defaultValue: Double): Double {
    try {
        return this?.toString()?.toDoubleOrNull() ?: defaultValue
    } catch (e: Exception) {
        return defaultValue
    }
}
