package com.gemini.energy.presentation.audit.list

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.databinding.ObservableArrayList
import android.databinding.ObservableBoolean
import android.databinding.ObservableField
import android.widget.Toast
import com.gemini.energy.R
import com.gemini.energy.data.local.model.GraveLocalModel
import com.gemini.energy.domain.entity.Audit
import com.gemini.energy.domain.entity.Feature
import com.gemini.energy.domain.interactor.*
import com.gemini.energy.internal.util.BaseAndroidViewModel
import com.gemini.energy.presentation.audit.list.mapper.AuditMapper
import com.gemini.energy.presentation.audit.list.model.AuditModel
import com.gemini.energy.presentation.util.Utils
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.rxkotlin.merge
import timber.log.Timber

class AuditListViewModel(context: Context,
                         private val auditGetAllUseCase: AuditGetAllUseCase,
                         private val zoneTypeGetAllByAuditUseCase: TypeGetAllByAuditUseCase,
                         private val featureGetAllByTypeUseCase: FeatureGetAllByTypeUseCase,
                         private val featureGetAllUseCase: FeatureGetAllUseCase,
                         private val featureDeleteUseCase: FeatureDeleteUseCase,
                         private val typeDeleteByAuditUseCase: TypeDeleteByAuditUseCase,
                         private val zoneDeleteByAuditUseCase: ZoneDeleteByAuditUseCase,
                         private val auditDeleteUseCase: AuditDeleteUseCase,
                         private val gravesSaveUseCase: GravesSaveUseCase) :

        BaseAndroidViewModel(context.applicationContext as Application) {

    private val mapper = AuditMapper(context)

    val loading = ObservableBoolean()
    val result = ObservableArrayList<AuditModel>()
    val empty = ObservableBoolean()
    val error = ObservableField<String>()

    fun loadAuditList() = addDisposable(getAll())
    fun deleteAudit(audit: AuditModel) = addDisposable(
            zoneTypeGetAllByAuditUseCase.execute(audit.id)
                    .subscribe {

                        Timber.d("Audit Id :: ${audit.id}")
                        Timber.d("Zone Type Get All By Audit :: [${it.count()}]")

                        val getFeatureObservable: MutableList<Observable<List<Feature>>> = mutableListOf()
                        val feature: MutableList<Feature> = mutableListOf()

                        getFeatureObservable.add(featureGetAllUseCase.execute(audit.id))
                        it.forEach { getFeatureObservable.add(featureGetAllByTypeUseCase.execute(it.id)) }

                        getFeatureObservable.merge().subscribe({ it.forEach { feature.add(it) } }, {}, {
                            Timber.d("::: Feature Delete - Count [${feature.count()}] :::")
                            featureDeleteUseCase.execute(feature)
                                    .subscribe {
                                        typeDeleteByAuditUseCase.execute(audit.id)
                                                .subscribe {
                                                    zoneDeleteByAuditUseCase.execute(audit.id)
                                                            .subscribe { delete(audit) }
                                                }
                                    }

                        })

                    }
    )

    private fun getAll(): Disposable {
        return auditGetAllUseCase.execute()
                .subscribeWith(object : DisposableObserver<List<Audit>>() {

                    override fun onStart() {
                        loading.set(true)
                    }

                    override fun onNext(t: List<Audit>) {
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

    private fun delete(audit: AuditModel): Disposable? {
        return auditDeleteUseCase.execute(audit.id)
                .subscribeWith(object : DisposableObserver<Unit>() {
                    @SuppressLint("CheckResult")
                    override fun onComplete() {
                        Timber.d("!! ON COMPLETE !!")
                        Toast.makeText(context, "Audit Delete Completed.", Toast.LENGTH_SHORT).show()
                        gravesSaveUseCase.execute(GraveLocalModel(Utils.intNow(),-1, audit.id, 0))
                                .subscribe { Timber.d("Audit to Graves") }
                    }

                    override fun onNext(t: Unit) {
                        Timber.d("[[ ON NEXT :: AUDIT DELETE ]]")
                        Timber.d("<< REFRESHING LIST -- NOW >>")
                        getAll()
                    }

                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                    }
                })
    }
}