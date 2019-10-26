package com.gemini.energy.presentation.audit.detail.zone.list

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.databinding.ObservableArrayList
import android.databinding.ObservableBoolean
import android.databinding.ObservableField
import com.gemini.energy.R
import com.gemini.energy.data.local.model.GraveLocalModel
import com.gemini.energy.domain.entity.Zone
import com.gemini.energy.domain.interactor.*
import com.gemini.energy.internal.util.BaseAndroidViewModel
import com.gemini.energy.presentation.audit.detail.zone.list.mapper.ZoneMapper
import com.gemini.energy.presentation.audit.detail.zone.list.model.ZoneModel
import com.gemini.energy.presentation.util.Utils
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import timber.log.Timber

class ZoneListViewModel(context: Context,
                        private val zoneGetAllUseCase: ZoneGetAllUseCase,
                        private val zoneDeleteUseCase: ZoneDeleteUseCase,
                        private val typeDeleteByZoneUseCase: TypeDeleteByZoneUseCase,
                        private val featureDeleteByZoneUseCase: FeatureDeleteByZoneUseCase,
                        private val gravesSaveUseCase: GravesSaveUseCase) :
        BaseAndroidViewModel(context.applicationContext as Application) {

    private val mapper = ZoneMapper(context)

    val loading = ObservableBoolean()
    val result = ObservableArrayList<ZoneModel>()
    val empty = ObservableBoolean()
    val error = ObservableField<String>()

    fun loadZoneList(auditId: Long) = addDisposable(getAll(auditId))
    fun deleteZone(zone: ZoneModel) = addDisposable(
            featureDeleteByZoneUseCase.execute(zone.id)
                    .subscribe {
                        typeDeleteByZoneUseCase.execute(zone.id)
                                .subscribe { delete(zone) }
                    })

    private fun getAll(auditId: Long): Disposable {
        return zoneGetAllUseCase.execute(auditId)
                .subscribeWith(object : DisposableObserver<List<Zone>>() {

                    override fun onStart() {
                        loading.set(true)
                    }

                    override fun onNext(t: List<Zone>) {
                        loading.set(false)
                        result.clear()
                        result.addAll(mapper.toModel(t))
                        empty.set(t.isEmpty())
                        Timber.d(t.toString())
                    }

                    override fun onError(t: Throwable) {
                        loading.set(false)
                        error.set(t.localizedMessage ?: t.message
                        ?: context.getString(R.string.unknown_error))
                    }

                    override fun onComplete() {}

                })
    }

    private fun delete(zone: ZoneModel): Disposable {
        return zoneDeleteUseCase.execute(zone.id!!)
                .subscribeWith(object : DisposableObserver<Unit>() {
                    @SuppressLint("CheckResult")
                    override fun onComplete() {
                        Timber.d("!! ON COMPLETE !!")
                        gravesSaveUseCase.execute(GraveLocalModel(Utils.intNow(), -1, zone.id.toLong(),1))
                                .subscribe { Timber.d("Zone to Graves") }
                    }

                    override fun onNext(t: Unit) {
                        Timber.d("[[ ON NEXT :: ZONE DELETE ]]")
                        Timber.d("<< REFRESHING LIST -- NOW >>")
                        getAll(zone.auditId)
                    }

                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                    }
                })
    }
}