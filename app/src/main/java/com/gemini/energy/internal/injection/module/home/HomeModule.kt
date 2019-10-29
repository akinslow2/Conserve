package com.gemini.energy.internal.injection.module.home

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.content.Context
import com.gemini.energy.domain.Schedulers
import com.gemini.energy.domain.gateway.AuditGateway
import com.gemini.energy.domain.interactor.*
import com.gemini.energy.internal.injection.scope.HomeScope
import com.gemini.energy.presentation.audit.detail.preaudit.PreAuditCreateViewModel
import com.gemini.energy.presentation.audit.detail.preaudit.PreAuditDeleteViewModel
import com.gemini.energy.presentation.audit.detail.preaudit.PreAuditFragment
import com.gemini.energy.presentation.audit.detail.preaudit.PreAuditGetViewModel
import com.gemini.energy.presentation.audit.detail.zone.dialog.ZoneCreateViewModel
import com.gemini.energy.presentation.audit.detail.zone.dialog.ZoneDialogFragment
import com.gemini.energy.presentation.audit.detail.zone.list.ZoneListFragment
import com.gemini.energy.presentation.audit.detail.zone.list.ZoneListViewModel
import com.gemini.energy.presentation.audit.dialog.AuditCreateViewModel
import com.gemini.energy.presentation.audit.dialog.AuditDialogFragment
import com.gemini.energy.presentation.audit.list.AuditListFragment
import com.gemini.energy.presentation.audit.list.AuditListViewModel
import com.gemini.energy.presentation.base.BaseFormFragment
import com.gemini.energy.presentation.base.Crossfader
import com.gemini.energy.presentation.base.GmailStyleCrossFadeSlidingPaneLayout
import com.gemini.energy.presentation.util.Navigator
import com.gemini.energy.presentation.type.dialog.TypeCreateViewModel
import com.gemini.energy.presentation.type.feature.FeatureCreateViewModel
import com.gemini.energy.presentation.type.feature.FeatureDataFragment
import com.gemini.energy.presentation.type.feature.FeatureGetViewModel
import com.gemini.energy.presentation.type.list.TypeListFragment
import com.gemini.energy.presentation.type.list.TypeListViewModel
import com.gemini.energy.service.*
import com.gemini.energy.service.type.UsageHours
import com.gemini.energy.service.type.UtilityRate
import com.mobsandgeeks.saripaar.Validator
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector


@Module
internal abstract class HomeModule {


    @ContributesAndroidInjector
    internal abstract fun contributeAuditListFragment(): AuditListFragment

    @ContributesAndroidInjector
    internal abstract fun contributeAuditDialogFragment(): AuditDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributePreAuditFragment(): PreAuditFragment

    @ContributesAndroidInjector
    internal abstract fun contributeZoneListFragment(): ZoneListFragment

    @ContributesAndroidInjector
    internal abstract fun contributeZoneDialogFragment(): ZoneDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeTypeListFragment(): TypeListFragment

    @ContributesAndroidInjector
    internal abstract fun contributeFeatureDataFragment(): FeatureDataFragment

    @ContributesAndroidInjector
    internal abstract fun contributeBaseFormFragment(): BaseFormFragment

    @Module
    companion object {

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideValidator(context: Context): Validator = Validator(context)

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideNavigator(context: Context): Navigator = Navigator(context)

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideCrossfader(): Crossfader<GmailStyleCrossFadeSlidingPaneLayout> {
            return Crossfader<GmailStyleCrossFadeSlidingPaneLayout>()
        }

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideAuditSaveUseCase(schedulers: Schedulers, auditGateway: AuditGateway):
                AuditSaveUseCase {
            return AuditSaveUseCase(schedulers, auditGateway)
        }

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideAuditGetUseCase(schedulers: Schedulers, auditGateway: AuditGateway):
                AuditGetUseCase {
            return AuditGetUseCase(schedulers, auditGateway)
        }

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideAuditGetAllUseCase(schedulers: Schedulers, auditGateway: AuditGateway):
                AuditGetAllUseCase {
            return AuditGetAllUseCase(schedulers, auditGateway)
        }

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideAuditUpdateUseCase(schedulers: Schedulers, auditGateway: AuditGateway):
                AuditUpdateUseCase {
            return AuditUpdateUseCase(schedulers, auditGateway)
        }

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideAuditDeleteUseCase(schedulers: Schedulers, auditGateway: AuditGateway):
                AuditDeleteUseCase {
            return AuditDeleteUseCase(schedulers, auditGateway)
        }

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideZoneGetUseCase(schedulers: Schedulers, auditGateway: AuditGateway):
                ZoneGetUseCase {
            return ZoneGetUseCase(schedulers, auditGateway)
        }

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideZoneGetAllUseCase(schedulers: Schedulers, auditGateway: AuditGateway):
                ZoneGetAllUseCase {
            return ZoneGetAllUseCase(schedulers, auditGateway)
        }

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideZoneDeleteUseCase(schedulers: Schedulers, auditGateway: AuditGateway):
                ZoneDeleteUseCase {
            return ZoneDeleteUseCase(schedulers, auditGateway)
        }

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideZoneDeleteByAuditUseCase(schedulers: Schedulers, auditGateway: AuditGateway):
                ZoneDeleteByAuditUseCase{
            return ZoneDeleteByAuditUseCase(schedulers, auditGateway)
        }

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideZoneSaveUseCase(schedulers: Schedulers, auditGateway: AuditGateway):
                ZoneSaveUseCase {
            return ZoneSaveUseCase(schedulers, auditGateway)
        }

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideZoneUpdateUseCase(schedulers: Schedulers, auditGateway: AuditGateway):
                ZoneUpdateUseCase {
            return ZoneUpdateUseCase(schedulers, auditGateway)
        }

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideZoneTypeGetUseCase(schedulers: Schedulers, auditGateway: AuditGateway):
                TypeGetUseCase{
            return TypeGetUseCase(schedulers, auditGateway)
        }

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideZoneTypeGetAllUseCase(schedulers: Schedulers, auditGateway: AuditGateway):
                TypeGetAllUseCase {
            return TypeGetAllUseCase(schedulers, auditGateway)
        }

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideZoneTypeGetAllByAuditUseCase(schedulers: Schedulers, auditGateway: AuditGateway):
                TypeGetAllByAuditUseCase {
            return TypeGetAllByAuditUseCase(schedulers, auditGateway)
        }

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideZoneTypeGetAllByZoneUseCase(schedulers: Schedulers, auditGateway: AuditGateway):
                TypeGetAllByZoneUseCase {
            return TypeGetAllByZoneUseCase(schedulers, auditGateway)
        }

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideZoneTypeSaveUseCase(schedulers: Schedulers, auditGateway: AuditGateway):
                TypeSaveUseCase {
            return TypeSaveUseCase(schedulers, auditGateway)
        }

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideZoneTypeUpdateUseCase(schedulers: Schedulers, auditGateway: AuditGateway):
                TypeUpdateUseCase{
            return TypeUpdateUseCase(schedulers, auditGateway)
        }

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideZoneTypeDeleteUseCase(schedulers: Schedulers, auditGateway: AuditGateway):
                TypeDeleteUseCase{
            return TypeDeleteUseCase(schedulers, auditGateway)
        }

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideZoneTypeDeleteByAuditUseCase(schedulers: Schedulers, auditGateway: AuditGateway):
                TypeDeleteByAuditUseCase{
            return TypeDeleteByAuditUseCase(schedulers, auditGateway)
        }

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideZoneTypeDeleteByZoneUseCase(schedulers: Schedulers, auditGateway: AuditGateway):
                TypeDeleteByZoneUseCase {
            return TypeDeleteByZoneUseCase(schedulers, auditGateway)
        }

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideFeatureSaveUseCase(schedulers: Schedulers, auditGateway: AuditGateway):
                FeatureSaveUseCase {
            return FeatureSaveUseCase(schedulers, auditGateway)
        }

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideFeatureGetAllUseCase(schedulers: Schedulers, auditGateway: AuditGateway):
                FeatureGetAllUseCase {
            return FeatureGetAllUseCase(schedulers, auditGateway)
        }

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideFeatureGetAllByTypeUseCase(schedulers: Schedulers, auditGateway: AuditGateway):
                FeatureGetAllByTypeUseCase {
            return FeatureGetAllByTypeUseCase(schedulers, auditGateway)
        }

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideFeatureDeleteUseCase(schedulers: Schedulers, auditGateway: AuditGateway):
                FeatureDeleteUseCase {
            return FeatureDeleteUseCase(schedulers, auditGateway)
        }

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideFeatureDeleteByZoneUseCase(schedulers: Schedulers, auditGateway: AuditGateway):
                FeatureDeleteByZoneUseCase{
            return FeatureDeleteByZoneUseCase(schedulers, auditGateway)
        }

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideGravesSaveUseCase(schedulers: Schedulers, auditGateway: AuditGateway) =
                GravesSaveUseCase(schedulers, auditGateway)

        //**** Energy Services **** //

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideEnergyUtility(context: Context): UtilityRate {
            return UtilityRate(context)
        }

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideEnergyUsage(): UsageHours {
            return UsageHours()
        }

        @Provides
        @JvmStatic
        internal fun provideOutgoingRows(context: Context): OutgoingRows {
            return OutgoingRows(context)
        }

        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideEnergyService(context: Context, schedulers: Schedulers, auditGateway: AuditGateway,
                                          utilityRateElectricity: UtilityRate,
                                          usageHours: UsageHours, outgoingRows: OutgoingRows):
                EnergyService {
            return EnergyService(context, schedulers, auditGateway,
                    utilityRateElectricity, usageHours, outgoingRows)
        }

        //**** End :: Energy Services **** //


        @HomeScope
        @Provides
        @JvmStatic
        internal fun provideViewModelFactory(

                context: Context,

                auditGetUseCase: AuditGetUseCase,
                auditGetAllUseCase: AuditGetAllUseCase,
                auditSaveUseCase: AuditSaveUseCase,
                auditUpdateUseCase: AuditUpdateUseCase,
                auditDeleteUseCase: AuditDeleteUseCase,

                zoneGetUseCase: ZoneGetUseCase,
                zoneGetAllUseCase: ZoneGetAllUseCase,
                zoneSaveUseCase: ZoneSaveUseCase,
                zoneUpdateUseCase: ZoneUpdateUseCase,
                zoneDeleteUseCase: ZoneDeleteUseCase,
                zoneDeleteByAuditUseCase: ZoneDeleteByAuditUseCase,

                typeGetUseCase: TypeGetUseCase,
                typeGetAllUseCase: TypeGetAllUseCase,
                typeGetAllByAuditUseCase: TypeGetAllByAuditUseCase,
                typeGetAllByZoneUseCase: TypeGetAllByZoneUseCase,
                typeSaveUseCase: TypeSaveUseCase,
                typeUpdateUseCase: TypeUpdateUseCase,
                typeDeleteUseCase: TypeDeleteUseCase,
                typeDeleteByAuditUseCase: TypeDeleteByAuditUseCase,
                typeDeleteByZoneUseCase: TypeDeleteByZoneUseCase,

                featureSaveUseCase: FeatureSaveUseCase,
                featureGetAllUseCase: FeatureGetAllUseCase,
                featureGetAllByTypeUseCase: FeatureGetAllByTypeUseCase,
                featureDeleteUseCase: FeatureDeleteUseCase,
                featureDeleteByZoneUseCase: FeatureDeleteByZoneUseCase,

                gravesSaveUseCase: GravesSaveUseCase


        ): ViewModelProvider.Factory {

            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                    return when {

                        modelClass.isAssignableFrom(AuditListViewModel::class.java) ->
                            AuditListViewModel(context, auditGetAllUseCase, typeGetAllByAuditUseCase,
                                    featureGetAllByTypeUseCase, featureGetAllUseCase, featureDeleteUseCase,
                                    typeDeleteByAuditUseCase, zoneDeleteByAuditUseCase, auditDeleteUseCase,
                                    gravesSaveUseCase) as T

                        modelClass.isAssignableFrom(AuditCreateViewModel::class.java) ->
                            AuditCreateViewModel(context, auditSaveUseCase, auditGetUseCase, auditUpdateUseCase) as T

                        modelClass.isAssignableFrom(ZoneListViewModel::class.java) ->
                            ZoneListViewModel(context, zoneGetAllUseCase, typeGetAllByZoneUseCase, zoneDeleteUseCase,
                                    typeDeleteByZoneUseCase, featureDeleteByZoneUseCase,
                                    gravesSaveUseCase) as T

                        modelClass.isAssignableFrom(ZoneCreateViewModel::class.java) ->
                            ZoneCreateViewModel(context, zoneSaveUseCase, zoneGetUseCase, zoneUpdateUseCase) as T

                        modelClass.isAssignableFrom(TypeListViewModel::class.java) ->
                            TypeListViewModel(context, typeGetAllUseCase,
                                    featureGetAllByTypeUseCase, featureDeleteUseCase, typeDeleteUseCase,
                                    gravesSaveUseCase) as T

                        modelClass.isAssignableFrom(TypeCreateViewModel::class.java) ->
                            TypeCreateViewModel(context, typeSaveUseCase,
                                    typeGetUseCase, typeUpdateUseCase) as T

                        modelClass.isAssignableFrom(PreAuditCreateViewModel::class.java) ->
                            PreAuditCreateViewModel(context, featureSaveUseCase,
                                    featureGetAllUseCase, featureDeleteUseCase) as T

                        modelClass.isAssignableFrom(PreAuditGetViewModel::class.java) ->
                            PreAuditGetViewModel(context, featureGetAllUseCase) as T

                        modelClass.isAssignableFrom(PreAuditDeleteViewModel::class.java) ->
                            PreAuditDeleteViewModel(context, featureDeleteUseCase) as T

                        modelClass.isAssignableFrom(FeatureCreateViewModel::class.java) ->
                            FeatureCreateViewModel(context, featureSaveUseCase,
                                    featureGetAllByTypeUseCase, featureDeleteUseCase) as T

                        modelClass.isAssignableFrom(FeatureGetViewModel::class.java) ->
                            FeatureGetViewModel(context, featureGetAllByTypeUseCase) as T

                        else -> throw IllegalArgumentException("Unknown ViewModel class : ${modelClass.name}")
                    }
                }
            }

        }
    }
}