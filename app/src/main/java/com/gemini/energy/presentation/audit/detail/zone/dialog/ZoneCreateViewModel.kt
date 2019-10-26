package com.gemini.energy.presentation.audit.detail.zone.dialog

import android.app.Application
import android.content.Context
import com.gemini.energy.R
import com.gemini.energy.domain.entity.Zone
import com.gemini.energy.domain.interactor.ZoneGetUseCase
import com.gemini.energy.domain.interactor.ZoneSaveUseCase
import com.gemini.energy.domain.interactor.ZoneUpdateUseCase
import com.gemini.energy.internal.util.BaseAndroidViewModel
import com.gemini.energy.internal.util.SingleLiveData
import com.gemini.energy.presentation.audit.detail.zone.list.model.ZoneModel
import com.gemini.energy.presentation.util.Utils
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import timber.log.Timber
import java.util.*

class ZoneCreateViewModel(context: Context,
                          private val zoneCreateUseCase: ZoneSaveUseCase,
                          private val zoneGetUseCase: ZoneGetUseCase,
                          private val zoneUpdateUseCase: ZoneUpdateUseCase) :
        BaseAndroidViewModel(context.applicationContext as Application) {

    private val _result = SingleLiveData<Boolean>()
    val result = _result

    private val _error = SingleLiveData<String>()
    val error = _error

    fun createZone(auditId: Long, zoneTag: String) {
        val date = Date()
        addDisposable(save(Zone(Utils.intNow(), zoneTag, "Sample Zone", -1, auditId, date, date)))
    }

    fun updateZone(zoneModel: ZoneModel, zoneTag: String) = addDisposable(
        zoneGetUseCase.execute(zoneModel.id)
                .subscribe {
                    it.name = zoneTag
                    it.usn = -1
                    it.updatedAt = Date()
                    update(it)
                })

    private fun save(zone: Zone): Disposable {
        return zoneCreateUseCase.execute(zone)
                .subscribeWith(object : DisposableObserver<Unit>() {

                    override fun onNext(t: Unit) {
                        Timber.d("ON-NEXT !!! \\m/ -- [[ Zone Save ]]")
                        result.value = true
                    }

                    override fun onError(e: Throwable) {
                        error.value = e.localizedMessage ?: e.message ?:
                                context.getString(R.string.unknown_error)
                    }

                    override fun onComplete() {}
                })
    }

    private fun update(zone: Zone): Disposable {
        return zoneUpdateUseCase.execute(zone)
                .subscribeWith(object : DisposableObserver<Unit>() {
                    override fun onNext(t: Unit) {
                        Timber.d("ON-NEXT !!! \\m/ -- [[ Zone Update ]]")
                        result.value = true
                    }
                    override fun onError(e: Throwable) { e.printStackTrace() }
                    override fun onComplete() {}
                })
    }
}