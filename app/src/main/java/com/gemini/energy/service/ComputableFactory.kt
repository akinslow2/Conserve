package com.gemini.energy.service

import com.gemini.energy.domain.entity.Computable
import com.gemini.energy.presentation.util.EApplianceType
import com.gemini.energy.presentation.util.ERefrigerationType
import com.gemini.energy.presentation.util.ELightingType
import com.gemini.energy.presentation.util.EZoneType
import com.gemini.energy.service.device.*
import com.gemini.energy.service.device.lighting.Cfl
import com.gemini.energy.service.device.lighting.Halogen
import com.gemini.energy.service.device.lighting.Incandescent
import com.gemini.energy.service.device.lighting.LinearFluorescent
import com.gemini.energy.service.device.lighting.LPSodium
import com.gemini.energy.service.device.lighting.HPSodium
import com.gemini.energy.service.device.plugload.*
import com.gemini.energy.service.device.refrigeration.*
import com.gemini.energy.service.type.UsageHours
import com.gemini.energy.service.type.UtilityRate


abstract class ComputableFactory {
    abstract fun build(): IComputable

    companion object {
        lateinit var computable: Computable<*>
        inline fun createFactory(computable: Computable<*>, utilityRateGas: UtilityRate,
                                 utilityRateElectricity: UtilityRate,
                                 usageHours: UsageHours, outgoingRows: OutgoingRows,
                                 ): ComputableFactory {
            this.computable = computable

            // This is to ensure every computable gets it's own copy of Utility Rate
            // Not sure how to create these instances from Dagger
            val _utilityRateGas = UtilityRate()
            val _utilityRateElectricity = UtilityRate()

            return when (computable.auditScopeType as EZoneType) {

                EZoneType.Plugload                  -> PlugloadFactory(_utilityRateGas,
                        _utilityRateElectricity, usageHours, outgoingRows)

                EZoneType.HVAC                      -> HvacFactory(_utilityRateGas,
                        _utilityRateElectricity, usageHours, outgoingRows)

                EZoneType.Refrigeration       -> RefrigerationFactory(_utilityRateGas,
                        _utilityRateElectricity, usageHours, outgoingRows)

                EZoneType.WaterHeater                      -> WaterHeaterFactory(_utilityRateGas,
                        _utilityRateElectricity, usageHours, outgoingRows)

                EZoneType.Lighting                  -> LightingFactory(_utilityRateGas,
                        _utilityRateElectricity, usageHours, outgoingRows)

                EZoneType.Motors                    -> MotorFactory(_utilityRateGas,
                        _utilityRateElectricity, usageHours, outgoingRows)

                EZoneType.Others                   -> GeneralFactory()
            }
       }
    }
}

class PlugloadFactory(private val utilityRateGas: UtilityRate,
                      private val utilityRateElectricity: UtilityRate,
                      private val usageHours: UsageHours,
                      private val outgoingRows: OutgoingRows,
                      ) : ComputableFactory() {

    override fun build(): IComputable {
        return when(computable.auditScopeSubType as EApplianceType) {

            EApplianceType.CombinationOven          -> CombinationOven(computable,
                    utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)

            EApplianceType.ConvectionOven           -> ConvectionOven()
            EApplianceType.ConveyorOven             -> ConveyorOven()
            EApplianceType.Fryer                    -> Fryer()

            EApplianceType.IceMaker                 -> IceMaker(computable,
                    utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)

            EApplianceType.RackOven                 -> RackOven()

            EApplianceType.SteamCooker              -> SteamCooker()

            EApplianceType.Griddle                  -> Griddle(computable,
                    utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)

            EApplianceType.HotFoodCabinet           -> HotFoodCabinet(computable,
                    utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)

            EApplianceType.ConveyorBroiler          -> ConveyorBroiler(computable,
                    utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)

            EApplianceType.DishWasher               -> DishWasher(computable,
                    utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)

            EApplianceType.PreRinseSpray            -> PreRinseSpray(computable,
                    utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)

            //**Add this inside class PlugloadFactory**
            EApplianceType.SampleAppliance          -> SampleAppliance(computable,
                    utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)

        }
    }

}

class RefrigerationFactory(private val utilityRateGas: UtilityRate,
                      private val utilityRateElectricity: UtilityRate,
                      private val usageHours: UsageHours,
                      private val outgoingRows: OutgoingRows,
                      ) : ComputableFactory() {

    override fun build(): IComputable {
        return when(computable.auditScopeSubType as ERefrigerationType) {

            ERefrigerationType.WIRefrigerator          -> WIRefrigerator(computable,
                    utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)

            ERefrigerationType.WIFreezer             -> WIFreezer(computable,
                    utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)

            ERefrigerationType.WICoolerBox                -> WICoolerBox(computable,
                    utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)

            ERefrigerationType.Refrigerator             -> Refrigerator(computable,
                    utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)

            ERefrigerationType.Freezer             -> Freezer(computable,
                    utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)
        }
    }

}

class LightingFactory(private val utilityRateGas: UtilityRate,
                      private val utilityRateElectricity: UtilityRate,
                      private val usageHours: UsageHours,
                      private val outgoingRows: OutgoingRows,
                      ) : ComputableFactory() {
    override fun build(): IComputable {
        return when(computable.auditScopeSubType as ELightingType) {

            ELightingType.CFL                       -> Cfl(computable,
                    utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)

            ELightingType.Halogen                   -> Halogen(computable,
                    utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)

            ELightingType.Incandescent              -> Incandescent(computable,
                    utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)

            ELightingType.LinearFluorescent         -> LinearFluorescent(computable,
                    utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)

            ELightingType.LPSodium         -> LPSodium(computable,
                    utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)

            ELightingType.HPSodium         -> HPSodium(computable,
                    utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)
        }
    }
}

class HvacFactory(private val utilityRateGas: UtilityRate,
                  private val utilityRateElectricity: UtilityRate,
                  private val usageHours: UsageHours,
                  private val outgoingRows: OutgoingRows,
                  ) : ComputableFactory() {

    override fun build(): IComputable = Hvac(computable,
            utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)

}

class WaterHeaterFactory(private val utilityRateGas: UtilityRate,
                  private val utilityRateElectricity: UtilityRate,
                  private val usageHours: UsageHours,
                  private val outgoingRows: OutgoingRows,
                  ) : ComputableFactory() {

    override fun build(): IComputable = WaterHeater(computable,
            utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)

}

class MotorFactory(private val utilityRateGas: UtilityRate,
                   private val utilityRateElectricity: UtilityRate,
                   private val usageHours: UsageHours,
                   private val outgoingRows: OutgoingRows,
                   ) : ComputableFactory() {

    override fun build() = Motors(computable,
            utilityRateGas, utilityRateElectricity, usageHours, outgoingRows)

}

class GeneralFactory : ComputableFactory() {
    override fun build() = General()
}
