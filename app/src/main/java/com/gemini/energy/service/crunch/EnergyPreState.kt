package com.gemini.energy.service.crunch

import com.gemini.energy.domain.entity.Computable
import com.gemini.energy.service.DataHolder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import io.reactivex.Observable
import java.util.*

class EnergyPreState {

    lateinit var featureData: Map<String, Any>
    lateinit var preAuditData: Map<String, Any>
    lateinit var computable: Computable<*>

    private fun initDataHolder(): DataHolder {
        val dataHolderPreState = DataHolder()

        // TODO review whether this is equivalent to the previous implementation,
        //      which let the feature define the relevant keys and only added those
        dataHolderPreState.header?.addAll(featureData.keys)
        dataHolderPreState.header?.addAll(preAuditData.keys)
        dataHolderPreState.computable = computable
        dataHolderPreState.fileName = "${Date().time}_pre_state.csv"

        return dataHolderPreState
    }

    fun getObservable(remoteExtract: List<Observable<JsonArray>>, cost: (List<JsonElement?>) -> Double): Observable<DataHolder> {
        return Observable.zip(remoteExtract, { responses ->
            val jsonElement: MutableList<JsonElement?> = mutableListOf()
            responses.forEach { response ->
                if (response is JsonArray) {
                    // ** 1. Create a list of each response as JSON Element
                    // ** 2. If there are multiple rows returned - send one of the rows - Need to improve on this
                    // ** 3. The Equipment Class knows what to expect and extracts the required data as applicable
                    try {
                        val jsonElements = response.map { it.asJsonObject.get("data") }
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
            // TODO review whether this is equivalent to the previous implementation,
            //      which let the features define the keys and copied only the corresponding values
            for (entry in featureData) {
                preRow[entry.key] = entry.value.toString()
            }
            for (entry in preAuditData) {
                preRow[entry.key] = entry.value.toString()
            }

            val costValue = cost(jsonElement)
            dataHolderPreState.header?.add("__electric_cost")
            preRow["__electric_cost"] = costValue.toString()

            dataHolderPreState.rows?.add(preRow)
            computable.energyPreState = preRow

            dataHolderPreState
        })

    }

    private fun thread() = Thread.currentThread().name
}
