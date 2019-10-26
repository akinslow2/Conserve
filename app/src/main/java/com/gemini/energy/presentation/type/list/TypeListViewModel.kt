package com.gemini.energy.presentation.type.list

import android.app.Application
import android.content.Context
import android.databinding.ObservableArrayList
import android.databinding.ObservableBoolean
import android.databinding.ObservableField
import android.util.Log
import com.gemini.energy.R
import com.gemini.energy.data.local.model.GraveLocalModel
import com.gemini.energy.domain.entity.Type
import com.gemini.energy.domain.interactor.*
import com.gemini.energy.internal.util.BaseAndroidViewModel
import com.gemini.energy.presentation.type.list.mapper.TypeMapper
import com.gemini.energy.presentation.type.list.model.TypeModel
import com.gemini.energy.presentation.util.Utils
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import timber.log.Timber

class TypeListViewModel(context: Context,

                        private val zoneTypeGetAllUseCase: TypeGetAllUseCase,
                        private val featureGetAllByTypeUseCase: FeatureGetAllByTypeUseCase,
                        private val featureDeleteUseCase: FeatureDeleteUseCase,
                        private val typeDeleteUseCase: TypeDeleteUseCase,
                        private val gravesSaveUseCase: GravesSaveUseCase) :

        BaseAndroidViewModel(context.applicationContext as Application) {

    private val mapper = TypeMapper(context)

    val loading = ObservableBoolean()
    val result = ObservableArrayList<TypeModel>()
    val empty = ObservableBoolean()
    val error = ObservableField<String>()

    fun loadZoneTypeList(zoneId: Long, zoneType: String) = addDisposable(getAll(zoneId, zoneType))
    fun deleteZoneType(type: TypeModel) = addDisposable(
            featureGetAllByTypeUseCase.execute(type.id)
                    .subscribe{
                        featureDeleteUseCase.execute(it)
                                .subscribe { delete(type) }
                    })

    private fun getAll(zoneId: Long, zoneType: String): Disposable {
        return zoneTypeGetAllUseCase.execute(listOf(zoneId, zoneType))
                .subscribeWith(object : DisposableObserver<List<Type>>() {

                    override fun onStart() {
                        loading.set(true)
                    }

                    override fun onNext(t: List<Type>) {
                        loading.set(false)
                        result.clear()
                        result.addAll(mapper.toModel(t))
                        empty.set(t.isEmpty())

                        Log.d(TAG, t.toString())
                    }

                    override fun onError(t: Throwable) {
                        loading.set(false)
                        error.set(t.localizedMessage ?: t.message
                        ?: context.getString(R.string.unknown_error))
                    }

                    override fun onComplete() {}
                })

    }

    private fun delete(type: TypeModel): Disposable? {
        return typeDeleteUseCase.execute(type.id!!)
                .subscribeWith(object : DisposableObserver<Unit>() {
                    override fun onComplete() {
                        Timber.d("!! ON COMPLETE !!")
                        gravesSaveUseCase.execute(GraveLocalModel(Utils.intNow(),-1, type.id.toLong(),2))
                                .subscribe { Timber.d("Type to Graves") }
                    }

                    override fun onNext(t: Unit) {
                        Timber.d("[[ ON NEXT :: TYPE DELETE ]]")
                        Timber.d("<< REFRESHING LIST -- NOW >>")
                        getAll(type.zoneId!!, type.type!!)
                    }

                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                    }
                })
    }

    companion object {
        private const val TAG = "TypeListViewModel"
    }

}