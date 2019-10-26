package com.gemini.energy.presentation.type.feature

import android.app.Application
import android.content.Context
import android.databinding.ObservableBoolean
import android.databinding.ObservableField
import android.util.Log
import com.gemini.energy.R
import com.gemini.energy.domain.entity.Feature
import com.gemini.energy.domain.interactor.FeatureGetAllByTypeUseCase
import com.gemini.energy.internal.util.BaseAndroidViewModel
import com.gemini.energy.internal.util.SingleLiveData
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver

class FeatureGetViewModel(context: Context, private val featureGetAllUseCase: FeatureGetAllByTypeUseCase) :
        BaseAndroidViewModel(context.applicationContext as Application) {

    val loading = ObservableBoolean()
    val result = SingleLiveData<List<Feature>>()
    val empty = ObservableBoolean()
    val error = ObservableField<String>()

    fun loadFeature(typeId: Long) = addDisposable(getAll(typeId))

    private fun getAll(typeId: Long): Disposable {
        return featureGetAllUseCase.execute(typeId)
                .subscribeWith(object : DisposableObserver<List<Feature>>() {
                    override fun onStart() {
                        loading.set(true)
                    }

                    override fun onNext(t: List<Feature>) {
                        loading.set(false)
                        result.call()
                        result.value = t
                        empty.set(t.isEmpty())

                        Log.d(TAG, t.toString())
                        Log.d(TAG, "ON-NEXT !!! \\m/")
                    }

                    override fun onError(e: Throwable) {
                        loading.set(false)
                        error.set(e.localizedMessage ?: e.message ?: context.getString(R.string.unknown_error))
                    }

                    override fun onComplete() {}
                })
    }

    companion object {
        private const val TAG = "FeatureGetViewModel"
    }

}