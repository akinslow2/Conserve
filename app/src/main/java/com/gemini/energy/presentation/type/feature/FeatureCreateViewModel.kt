package com.gemini.energy.presentation.type.feature

import android.app.Application
import android.content.Context
import android.util.Log
import com.gemini.energy.R
import com.gemini.energy.domain.entity.Feature
import com.gemini.energy.domain.interactor.FeatureDeleteUseCase
import com.gemini.energy.domain.interactor.FeatureGetAllByTypeUseCase
import com.gemini.energy.domain.interactor.FeatureSaveUseCase
import com.gemini.energy.internal.util.BaseAndroidViewModel
import com.gemini.energy.internal.util.SingleLiveData
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver

class FeatureCreateViewModel(context: Context,
                             private val featureCreateUseCase: FeatureSaveUseCase,
                             private val featureListUseCase: FeatureGetAllByTypeUseCase,
                             private val featureDeleteUseCase: FeatureDeleteUseCase) :
        BaseAndroidViewModel(context.applicationContext as Application) {

    private val _result = SingleLiveData<Boolean>()
    val result = _result

    private val _error = SingleLiveData<String>()
    val error = _error

    fun createFeature(feature: List<Feature>, typeId: Long) {
        addDisposable(
                featureListUseCase.execute(typeId)
                        .subscribe {
                            featureDeleteUseCase.execute(it)
                                    .subscribe { save(feature) }
                        })
    }


    private fun save(feature: List<Feature>): Disposable {
        return featureCreateUseCase.execute(feature)
                .subscribeWith(object : DisposableObserver<Unit>() {

                    override fun onNext(t: Unit) {
                        Log.d(TAG, "ON-NEXT !!! \\m/")
                        result.value = true
                    }

                    override fun onError(e: Throwable) {
                        error.value = e.localizedMessage ?: e.message ?: context.getString(R.string.unknown_error)
                    }

                    override fun onComplete() {}
                })
    }

    companion object {
        private const val TAG = "FeatureCreateViewModel"
    }

}