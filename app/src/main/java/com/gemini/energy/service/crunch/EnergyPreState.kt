package com.gemini.energy.service.crunch

import com.gemini.energy.domain.entity.Computable
import com.gemini.energy.service.DataHolder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import io.reactivex.Observable
import timber.log.Timber
import java.util.*

class EnergyPreState {

    lateinit var featureDataFields: MutableList<String>
    lateinit var preAuditDataFields: MutableList<String>
    lateinit var featureData: Map<String, Any>
    lateinit var preAuditData: Map<String, Any>
    lateinit var computable: Computable<*>

    private fun initDataHolder(): DataHolder {
        val dataHolderPreState = DataHolder()

        dataHolderPreState.header?.addAll(featureDataFields)
        dataHolderPreState.header?.addAll(preAuditDataFields)
        dataHolderPreState.computable = computable
        dataHolderPreState.fileName = "${Date().time}_pre_state.csv"

        return dataHolderPreState
    }

    fun getObservable(remoteExtract: List<Observable<JsonArray>>, cost: (List<JsonElement?>) -> Double): Observable<DataHolder> {
        return Observable.zip(remoteExtract) { responses ->
            Timber.d("##### Pre-State Energy Calculation - (${thread()}) #####")
            val jsonElement: MutableList<JsonElement?> = mutableListOf()
            responses.forEach { response ->
                if (response is JsonArray) {
                    // ** 1. Create a list of each response as JSON Element
                    // ** 2. If there are multiple rows returned - send one of the rows - Need to improve on this
                    // ** 3. The Equipment Class knows what to expect and extracts the required data as applicable
                    try {
                        val jsonElements = response.map { it.asJsonObject.get("data") }
                        Timber.d(jsonElements.toString())
                        if (jsonElements.count() > 0) {
                            jsonElement.add(jsonElements[0])
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val dataHolderPreState = initDataHolder()
            val preRow = mutableMapOf<String, String>()
            featureDataFields.forEach { field ->
                preRow[field] = if (featureData.containsKey(field)) featureData[field].toString() else ""
            }
            preAuditDataFields.forEach { field ->
                preRow[field] = if (preAuditData.containsKey(field)) preAuditData[field].toString() else ""
            }

            val costValue = cost(jsonElement)
            dataHolderPreState.header?.add("__electric_cost")
            preRow["__electric_cost"] = costValue.toString()

            dataHolderPreState.rows?.add(preRow)
            computable.energyPreState = preRow

            Timber.d("## Data Holder - PRE STATE - (${thread()}) ##")
            Timber.d(dataHolderPreState.toString())

            dataHolderPreState
        }
    }

    private fun thread() = Thread.currentThread().name
}