
package com.gemini.energy.service

import android.content.Context
import com.gemini.energy.domain.Schedulers
import com.gemini.energy.domain.entity.Computable
import com.gemini.energy.domain.entity.Feature
import com.gemini.energy.domain.gateway.AuditGateway
import com.gemini.energy.service.device.EBase
import com.gemini.energy.service.type.UsageHours
import com.gemini.energy.service.type.UtilityRate
import com.gemini.energy.wordDocGenerator.WordDocumentGenerator
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.merge


class EnergyService(
        private val context: Context,
        private val schedulers: Schedulers,
        private val auditGateway: AuditGateway,
        private val utilityRateElectricity: UtilityRate,
        private val usageHours: UsageHours,
        private val outgoingRows: OutgoingRows) {

    /**
     * Holds the Unit of Work i.e the IComputables
     * */
    private var taskHolder: MutableList<Observable<Computable<*>>> = mutableListOf()


    /**
     * Holds the reference to the Observed Stream
     * */
    private var disposables: MutableList<Disposable> = mutableListOf()


    /**
     * Energy UtilityRate Gas - Electricity
     * */
    private val energyUtilityGas = UtilityRate(context)


    private var ebases = mutableListOf<EBase>()

    /**
     * Energy Calculation - Main Entry Point
     * */
    fun run(callback: (status: Boolean) -> Unit) {

        cleanup()

        disposables.add(auditGateway.getComputable()
                .subscribeOn(schedulers.subscribeOn)
                .subscribe { computables ->
                    Observable.fromIterable(computables)
                            .subscribe({ eachComputable ->
                                getComputable(eachComputable)
                                        .subscribe({
                                            synchronized(taskHolder) {
                                                taskHolder.add(it)
                                            }
                                        }, { exception ->
                                            exception.printStackTrace()
                                        }, {})
                            }, {}, {
                                doWork(callback) // << ** Executed Only One Time ** >> //
                            })
                })

    }

    /**
     * Required to Clean Up Existing Disposables before a new CRUNCH begins
     * */
    private fun cleanup() {
        if (disposables.count() > 0) {
            disposables.forEach {
                it.dispose()
            }
        }

        disposables.clear()
        taskHolder.clear()
    }

    /**
     * Holds the Main Work - i.e Running each Energy Calculations for each of the IComputables
     * */
    private fun doWork(callback: (status: Boolean) -> Unit) {
        disposables.add(taskHolder.merge()
                .observeOn(schedulers.observeOn)
                .subscribe({
                }, { exception ->
                    exception.printStackTrace()
                    callback(false)
                }, {
                    WordDocumentGenerator().triggerGeneration(ebases)

                    callback(true) // << ** The final Exit Point ** >> //
                }))
    }

    /**
     * Takes in two Observables
     * 1. To get the Feature Pre Audit (pre-audit)
     * 2. To get the Feature Audit Scope (feature data)
     *
     * The build() method returns the fully packaged IComputable as a Flowable - Wrapped by an Observable
     * */
    private fun getComputable(computable: Computable<*>): Observable<Observable<Computable<*>>> {

        fun build(computable: Computable<*>, featureAuditScope: List<Feature>,
                          featurePreAudit: List<Feature>): Observable<Computable<*>>{


            computable.featureAuditScope = featureAuditScope
            computable.featurePreAudit = featurePreAudit

            val iComputable = ComputableFactory.createFactory(computable, energyUtilityGas,
                    utilityRateElectricity, usageHours,
                    outgoingRows, context).build()

            iComputable.compute().blockingLast()

            if (iComputable is EBase) {
                ebases.add(iComputable)
            }

            return iComputable.compute()
        }


        return Observable.zip(
                auditGateway.getFeatureByAudit(computable.auditId),
                auditGateway.getFeatureByType(computable.auditScopeId),
                BiFunction<List<Feature>, List<Feature>, Observable<Computable<*>>> { featurePreAudit, featureAuditScope ->
                    build(computable, featureAuditScope, featurePreAudit)
                })
    }

    private fun thread() = Thread.currentThread().name

}
