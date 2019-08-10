package com.gemini.energy.presentation.type.feature

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import com.gemini.energy.R
import com.gemini.energy.domain.entity.Feature
import com.gemini.energy.internal.util.lazyThreadSafetyNone
import com.gemini.energy.presentation.base.BaseFormFragment
import com.gemini.energy.presentation.form.model.GElements
import com.gemini.energy.presentation.type.SharedViewModel
import com.gemini.energy.presentation.type.list.model.TypeModel
import com.gemini.energy.presentation.util.EApplianceType
import com.gemini.energy.presentation.util.ELightingType
import com.gemini.energy.presentation.util.EZoneType
import com.thejuki.kformmaster.model.BaseFormElement
import java.util.*
import javax.inject.Inject

class FeatureDataFragment : BaseFormFragment() {

    private lateinit var sharedViewModel: SharedViewModel
    private var typeModel: TypeModel? = null

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val featureSaveViewModel by lazyThreadSafetyNone {
        ViewModelProviders.of(this, viewModelFactory).get(FeatureCreateViewModel::class.java)
    }

    private val featureListViewModel by lazyThreadSafetyNone {
        ViewModelProviders.of(this, viewModelFactory).get(FeatureGetViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedViewModel = ViewModelProviders.of(activity!!).get(SharedViewModel::class.java)
        sharedViewModel.getType().observe(this,
                Observer<TypeModel> {
                    this.typeModel = it
                    super.loadForm()
                })
    }

    override fun resourceId(): Int? {

        var rawId: Int = -1

        typeModel?.let { model ->
            if (model.type == EZoneType.Plugload.value) {
                if (model.subType == EApplianceType.CombinationOven.value) {rawId = R.raw.combination_oven}
                if (model.subType == EApplianceType.ConvectionOven.value) {rawId = R.raw.convection_oven}
                if (model.subType == EApplianceType.ConveyorOven.value) {rawId = R.raw.conveyor_oven}
                if (model.subType == EApplianceType.Refrigerator.value) {rawId = R.raw.refrigerator_freezer}
                if (model.subType == EApplianceType.Fryer.value) {rawId = R.raw.fryer}
                if (model.subType == EApplianceType.IceMaker.value) {rawId = R.raw.icemaker}
                if (model.subType == EApplianceType.RackOven.value) {rawId = R.raw.rack_oven}
                if (model.subType == EApplianceType.SteamCooker.value) {rawId = R.raw.steam_cooker}
                if (model.subType == EApplianceType.Griddle.value) {rawId = R.raw.griddle }
                if (model.subType == EApplianceType.HotFoodCabinet.value) {rawId = R.raw.hot_food_cabinet }
                if (model.subType == EApplianceType.ConveyorBroiler.value) {rawId = R.raw.conveyor_broiler }
                if (model.subType == EApplianceType.DishWasher.value) {rawId = R.raw.dishwasher }
                if (model.subType == EApplianceType.PreRinseSpray.value) {rawId = R.raw.pre_rinse_spray }

                //**Add this line under EZoneType.Plugload.value**
                if (model.subType == EApplianceType.SampleAppliance.value) {rawId = R.raw.sample_appliance }
            }
            else if (model.type == EZoneType.HVAC.value) {rawId = R.raw.hvac}
            else if (model.type == EZoneType.Motors.value) {rawId = R.raw.motors}
            else if (model.type == EZoneType.Lighting.value) {
                if (model.subType == ELightingType.CFL.value) {rawId = R.raw.cfl}
                if (model.subType == ELightingType.Halogen.value) {rawId = R.raw.halogen}
                if (model.subType == ELightingType.Incandescent.value) {rawId = R.raw.incandescent}
                if (model.subType == ELightingType.LinearFluorescent.value) {rawId = R.raw.linear_fluorescent}
            }
        }

        return rawId
    }

    companion object {
        fun newInstance(): FeatureDataFragment {
            return FeatureDataFragment()
        }

        private const val belongsTo = "type"
    }

    override fun getAuditId(): Long? {
        return null
    }

    override fun getZoneId(): Int? {
        return null
    }

    override fun executeListeners() {
        featureListViewModel.result
                .observe(this, Observer {
                    super.refreshFormData(it)
                })
    }

    override fun loadFeatureData() {
        typeModel?.let {
            featureListViewModel.loadFeature(it.id!!)
        }
    }

    override fun createFeatureData(formData: MutableList<Feature>) {
        typeModel?.let {
            featureSaveViewModel.createFeature(formData, it.id!!)
        }
    }

    override fun buildFeature(gElement: GElements, gFormElement: BaseFormElement<*>): Feature? {
        var feature: Feature? = null
        val date = Date()
        typeModel?.let {
            feature = Feature(null, gElement.id, belongsTo, gElement.dataType, -1,
                    null, it.zoneId, it.id, gElement.param, gFormElement.valueAsString,
                    null, null, date, date)
        }

        return feature
    }
}