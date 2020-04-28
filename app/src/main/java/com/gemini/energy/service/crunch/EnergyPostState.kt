package com.gemini.energy.service.crunch

import com.gemini.energy.domain.entity.Computable
import com.gemini.energy.service.DataHolder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.reactivex.functions.Function
import timber.log.Timber
import java.util.*

class EnergyPostState {

    class Mapper : Function<JsonArray, DataHolder> {
        lateinit var postStateFields: MutableList<String>
        lateinit var computable: Computable<*>
        lateinit var cost: (JsonElement, DataHolder) -> Double

        override fun apply(response: JsonArray): DataHolder {

            Timber.d("**********************************************************************")

            Timber.d("##### Post-State Energy Calculation - (${thread()}) #####")
            Timber.d("### Efficient Alternate Count - [${response.count()}] - ###")
            Timber.d("$response")

            val dataHolderPostState = initDataHolder()
            val costCollector = mutableListOf<Double>()
            val costToElement: MutableMap<Double, JsonElement> = hashMapOf()
            var combinedElement = JsonObject()

            try {
                val jsonElements = response.map { it.asJsonObject.get("data") }
                combinedElement = merge(jsonElements) ?: JsonObject()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

            val postRow = mutableMapOf<String, String>()
            postStateFields.forEach { key ->
                var value = ""
                if (combinedElement.asJsonObject.has(key)) {
                    value = combinedElement.asJsonObject.get(key).asString
                }
                postRow[key] = value
            }

            val cost = cost(combinedElement, dataHolderPostState)
            postRow["__electric_cost"] = cost.toString()

            costCollector.add(cost)
            costToElement[cost] = combinedElement

            dataHolderPostState.rows?.add(postRow)
            computable.energyPostState?.add(postRow)


            Timber.d("## Data Holder - POST STATE  - (${thread()}) ##")
            Timber.d(dataHolderPostState.toString())

            val costMinimum = costCollector.min()
            val efficientAlternative = dataHolderPostState.rows?.filter {
                it.containsKey("__electric_cost") && it.getValue("__electric_cost").toDouble() == costMinimum
            }

            computable.energyPostStateLeastCost = efficientAlternative ?: mutableListOf()
            computable.efficientAlternative = costToElement[costMinimum]

            Timber.d("Minimum Cost : [$costMinimum]")
            Timber.d("Efficient Alternative : ${computable.energyPostStateLeastCost}")

            return dataHolderPostState
        }

        private fun initDataHolder(): DataHolder {
            val dataHolderPostState = DataHolder()
            dataHolderPostState.header = postStateFields
            dataHolderPostState.header?.add("__electric_cost")

            dataHolderPostState.computable = computable
            dataHolderPostState.fileName = "${Date().time}_post_state.csv"

            return dataHolderPostState
        }

        private fun thread() = Thread.currentThread().name

        private fun merge(jsonObjects: List<JsonElement>): JsonObject? {
            val jsonObject = JsonObject()
            for (each in jsonObjects) {
                val keys = each.asJsonObject.keySet()
                for (key in keys)
                    jsonObject.add(key, each.asJsonObject.get(key))
            }
            return jsonObject
        }
    }

}