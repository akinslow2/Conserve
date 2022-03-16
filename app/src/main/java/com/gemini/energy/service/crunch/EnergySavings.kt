package com.gemini.energy.service.crunch

import com.gemini.energy.domain.entity.Computable
import com.gemini.energy.service.DataHolder
import io.reactivex.functions.Function
import java.util.*

class EnergySavings {

    class Mapper : Function<Unit, DataHolder> {

        override fun apply(unit: Unit): DataHolder {

            /**
             * The Power Time Change class takes in the computable and calculates the power delta
             * */
            val ptc = powerTimeChange.delta()

            /**
             * Final Energy Saving
             * @Johnny - The savings here can be in terms of Power Change - Time Change - PowerTime Change
             * Also the method name energySaving is misleading
             * */
            val energySaving = ptc.energySaving()

            val energySavingHeader = listOf("__check_power_change", "__check_time_change",
                    "__check_power_time_change", "__energy_power_change", "__energy_time_change",
                    "__energy_power_time_change", "__energy_saving")

            fun initDataHolder(): DataHolder {
                val dataHolderPostState = DataHolder()
                dataHolderPostState.header?.addAll(energySavingHeader)

                dataHolderPostState.computable = computable
                dataHolderPostState.fileName = "${Date().time}_energy_savings.csv"

                return dataHolderPostState
            }

            /**
             * Prepare the Outgoing Rows
             * */
            val dataHolder = initDataHolder()
            dataHolder.rows?.add(mapOf(
                    "__check_power_change" to ptc.checkPowerChange.toString(),
                    "__check_time_change" to ptc.checkTimeChange.toString(),
                    "__check_power_time_change" to ptc.checkPowerTimeChange.toString(),
                    "__energy_power_change" to ptc.energyPowerChange.toString(),
                    "__energy_time_change" to ptc.energyTimeChange.toString(),
                    "__energy_power_time_change" to ptc.energyPowerTimeChange.toString(),
                    "__energy_saving" to energySaving.toString()
            ))

            return dataHolder

        }

        private fun thread() = Thread.currentThread().name

        lateinit var powerTimeChange: PowerTimeChange
        lateinit var computable: Computable<*>
    }

}
