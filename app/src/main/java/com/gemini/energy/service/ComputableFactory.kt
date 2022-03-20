package com.gemini.energy.service

import com.gemini.energy.domain.entity.Computable
import com.gemini.energy.presentation.util.EApplianceType
import com.gemini.energy.presentation.util.ELightingType
import com.gemini.energy.presentation.util.ERefrigerationType
import com.gemini.energy.presentation.util.EZoneType
import com.gemini.energy.service.device.General
import com.gemini.energy.service.device.Hvac
import com.gemini.energy.service.device.Motors
import com.gemini.energy.service.device.WaterHeater
import com.gemini.energy.service.device.lighting.*
import com.gemini.energy.service.device.plugload.*
import com.gemini.energy.service.device.refrigeration.*
import com.gemini.energy.service.type.UsageHours
import com.gemini.energy.service.type.UtilityRate


abstract class ComputableFactory {
    abstract fun build(): IComputable

    companion object {
        fun createFactory(
                computable: Computable<*>,
                usageHours: UsageHours,
                outgoingRows: OutgoingRows,
        ): ComputableFactory {
            // This is to ensure every computable gets it's own copy of Utility Rate
            // Not sure how to create these instances from Dagger
            val gas = UtilityRate()
            val electric = UtilityRate()

            return when (computable.auditScopeType) {
                EZoneType.Plugload -> PlugloadFactory(computable, gas, electric, usageHours, outgoingRows)
                EZoneType.HVAC -> HvacFactory(computable, gas, electric, usageHours, outgoingRows)
                EZoneType.Refrigeration -> RefrigerationFactory(computable, gas, electric, usageHours, outgoingRows)
                EZoneType.WaterHeater -> WaterHeaterFactory(computable, gas, electric, usageHours, outgoingRows)
                EZoneType.Lighting -> LightingFactory(computable, gas, electric, usageHours, outgoingRows)
                EZoneType.Motors -> MotorFactory(computable, gas, electric, usageHours, outgoingRows)
                else -> GeneralFactory()
            }
        }
    }
}

class PlugloadFactory(
        private val computable: Computable<*>,
        private val utilityRateGas: UtilityRate,
        private val utilityRateElectricity: UtilityRate,
        private val usageHours: UsageHours,
        private val outgoingRows: OutgoingRows,
) : ComputableFactory() {

    override fun build(): IComputable {
        return when (computable.auditScopeSubType) {
            EApplianceType.CombinationOven -> CombinationOven(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)
            EApplianceType.ConvectionOven -> ConvectionOven()
            EApplianceType.ConveyorOven -> ConveyorOven()
            EApplianceType.Fryer -> Fryer()
            EApplianceType.IceMaker -> IceMaker(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)
            EApplianceType.RackOven -> RackOven()
            EApplianceType.SteamCooker -> SteamCooker()
            EApplianceType.Griddle -> Griddle(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)
            EApplianceType.HotFoodCabinet -> HotFoodCabinet(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)
            EApplianceType.ConveyorBroiler -> ConveyorBroiler(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)
            EApplianceType.DishWasher -> DishWasher(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)
            EApplianceType.PreRinseSpray -> PreRinseSpray(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)
            //**Add this inside class PlugloadFactory**
            EApplianceType.SampleAppliance -> SampleAppliance(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)
            else -> General()
        }
    }

}

class RefrigerationFactory(
        private val computable: Computable<*>,
        private val utilityRateGas: UtilityRate,
        private val utilityRateElectricity: UtilityRate,
        private val usageHours: UsageHours,
        private val outgoingRows: OutgoingRows,
) : ComputableFactory() {

    override fun build(): IComputable {
        return when (computable.auditScopeSubType) {
            ERefrigerationType.WIRefrigerator -> WIRefrigerator(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)
            ERefrigerationType.WIFreezer -> WIFreezer(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)
            ERefrigerationType.WICoolerBox -> WICoolerBox(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)
            ERefrigerationType.Refrigerator -> Refrigerator(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)
            ERefrigerationType.Freezer -> Freezer(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)
            else -> General()
        }
    }

}

class LightingFactory(
        private val computable: Computable<*>,
        private val utilityRateGas: UtilityRate,
        private val utilityRateElectricity: UtilityRate,
        private val usageHours: UsageHours,
        private val outgoingRows: OutgoingRows,
) : ComputableFactory() {
    override fun build(): IComputable {
        return when (computable.auditScopeSubType) {
            ELightingType.CFL -> Cfl(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)
            ELightingType.Halogen -> Halogen(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)
            ELightingType.Incandescent -> Incandescent(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)
            ELightingType.LinearFluorescent -> LinearFluorescent(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)
            ELightingType.LPSodium -> LPSodium(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)
            ELightingType.HPSodium -> HPSodium(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)
            else -> General()
        }
    }
}

class HvacFactory(
        private val computable: Computable<*>,
        private val utilityRateGas: UtilityRate,
        private val utilityRateElectricity: UtilityRate,
        private val usageHours: UsageHours,
        private val outgoingRows: OutgoingRows,
) : ComputableFactory() {

    override fun build(): IComputable = Hvac(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)
}

class WaterHeaterFactory(
        private val computable: Computable<*>,
        private val utilityRateGas: UtilityRate,
        private val utilityRateElectricity: UtilityRate,
        private val usageHours: UsageHours,
        private val outgoingRows: OutgoingRows,
) : ComputableFactory() {

    override fun build(): IComputable = WaterHeater(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)
}

class MotorFactory(
        private val computable: Computable<*>,
        private val utilityRateGas: UtilityRate,
        private val utilityRateElectricity: UtilityRate,
        private val usageHours: UsageHours,
        private val outgoingRows: OutgoingRows,
) : ComputableFactory() {

    override fun build() = Motors(computable, utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)
}

class GeneralFactory : ComputableFactory() {
    override fun build() = General()
}
