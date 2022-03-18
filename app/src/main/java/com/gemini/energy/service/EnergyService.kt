
package com.gemini.energy.service

import com.gemini.energy.domain.entity.Computable
import com.gemini.energy.domain.entity.Feature
import com.gemini.energy.domain.gateway.AuditGateway
import com.gemini.energy.service.device.EBase
import com.gemini.energy.service.type.UsageHours
import com.gemini.energy.service.type.UtilityRate
import com.gemini.energy.wordDocGenerator.WordDocumentGenerator
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.merge
import org.apache.poi.xwpf.usermodel.XWPFDocument


class EnergyService(
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
    private val energyUtilityGas = UtilityRate()


    private var ebases = mutableListOf<EBase>()

    /**
     * Energy Calculation - Main Entry Point
     * */
    fun run(auditId: String, callback: (result: XWPFDocument?) -> Unit) {

        cleanup()

        // TODO this entire class is completely weird. What's with all the nested subscribes and Observables?
        //  All the class variables could be removed as well by refactoring this into a clean method.
        //  Probably way easier with coroutines.
        disposables.add(auditGateway.getComputable(auditId)
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
    private fun doWork(callback: (result: XWPFDocument?) -> Unit) {
        disposables.add(taskHolder.merge()
                .subscribe({
                }, { exception ->
                    exception.printStackTrace()
                    callback(null)
                }, {
                    val docs = WordDocumentGenerator().triggerGeneration(ebases)
                    // TODO the documents are grouped by audit.
                    //  Only one audit is handled at a time so we might as well get rid of the lists.
                    callback(docs.getOrNull(0))
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

            val iComputable = ComputableFactory.createFactory(computable, usageHours, outgoingRows).build()

            if (iComputable is EBase) {
                ebases.add(iComputable)
            }

            return iComputable.compute()
        }


        return Observable.zip(
                auditGateway.getFeatureByAudit(computable.auditId),
                auditGateway.getFeatureByType(computable.auditScopeId)
        ) { featurePreAudit, featureAuditScope ->
            build(computable, featureAuditScope, featurePreAudit)
        }
    }
}
