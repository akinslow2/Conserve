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


/**
 * @return formatted json string
 */
fun Any.prettyPrint(): String {

    var indentLevel = 0
    val indentWidth = 4

    fun padding() = "".padStart(indentLevel * indentWidth)

    val toString = toString()

    val stringBuilder = StringBuilder(toString.length)

    var i = 0
    while (i < toString.length) {
        when (val char = toString[i]) {
            '(', '[', '{' -> {
                indentLevel++
                stringBuilder.append(char).append("\n").append(padding())
            }
            ')', ']', '}' -> {
                indentLevel--
                stringBuilder.append("\n").append(padding()).append(char)
            }
            ',' -> {
                stringBuilder.append(char).append("\n").append(padding())
                // ignore space after comma as we have added a newline
                val nextChar = toString.getOrElse(i + 1) { char }
                if (nextChar == ' ') i++
            }
            else -> {
                stringBuilder.append(char)
            }
        }
        i++
    }

    return stringBuilder.toString()
}
