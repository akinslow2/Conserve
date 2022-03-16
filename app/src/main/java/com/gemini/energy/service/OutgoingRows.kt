package com.gemini.energy.service

import com.gemini.energy.domain.entity.Computable
import java.util.*

class DataHolder {

    /**
     * Set from EBase
     * */
    var computable: Computable<*>? = null
    set(value) {
        this.path = StringBuilder()

                .append("${value?.auditName?.toLowerCase(Locale.ROOT)?.replace("\\s+".toRegex(), "_")}/")
                .append("${value?.zoneName?.toLowerCase(Locale.ROOT)?.replace("\\s+".toRegex(), "_")}/")
                .append("${value?.auditScopeType?.value?.toLowerCase(Locale.ROOT)}_")
                .append("${value?.auditScopeSubType?.toString()?.toLowerCase(Locale.ROOT)}_")
                .append("${value?.auditScopeName?.toLowerCase(Locale.ROOT)?.replace("[^a-zA-Z0-9]".toRegex(), "_")}/")

                .toString()
        field = value
    }

    /**
     * These variables are exposed and are supposed to be set by Each Computables - @EBase
     * */
    var header: MutableList<String>? = mutableListOf()
    var rows: MutableList<Map<String, String>>? = mutableListOf()
    var fileName: String = ""

    /**
     * Set during runtime via the computable set method
     * */
    var path: String = ""

    override fun toString(): String {
        return "fileName: [$fileName]\n" +
                "path: [$path]\n" +
                "header: $header\n" +
                "rows count: [${rows?.count()}]"
    }

}

class OutgoingRows {
    lateinit var computable: Computable<*>
    lateinit var dataHolder: MutableList<DataHolder>

    companion object {

        private const val TAG = "OutgoingRows"
        private const val GEMINI = "gemini/"
        const val DEBUG = "${GEMINI}debug/"

        /**
         * Creating a Comma Separated CSV Data String
         * */
        @Synchronized
        private fun data(header: List<String>?, rows: MutableList<Map<String, String>>?): String {
            val buffer = StringBuilder()
            buffer.append(header?.joinToString())
            buffer.append("\r\n")

            for (row in rows!!) {
                val tmp: MutableList<String> = mutableListOf()
                header?.let { hdr ->
                    for (item in hdr) {
                        tmp.add(row[item] ?: "")
                    }
                }
                buffer.append(tmp.joinToString())
                buffer.append("\r\n")
            }

            return buffer.toString()
        }
    }

    private fun thread() = Thread.currentThread().name

}
