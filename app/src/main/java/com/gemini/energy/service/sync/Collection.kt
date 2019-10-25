package com.gemini.energy.service.sync

import android.annotation.SuppressLint
import com.gemini.energy.App
import com.gemini.energy.data.local.dao.*
import com.gemini.energy.data.local.model.*
import com.gemini.energy.data.local.system.AuditDatabase
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.merge
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

/**
 * Read all the Data from the Local DB and Build the Collection
 * -- This happens only once during the start
 * -- This class is to be shared amongst all the others
 * -- At any instance this class shows the State of the Local Database
 * */
@SuppressLint("CheckResult")
class Collection(private val mListener: Listener?) {

    var db: AuditDatabase? = null

    var audit: List<AuditLocalModel> = listOf()
    var zone: HashMap<Long, List<ZoneLocalModel>> = hashMapOf()
    var type: HashMap<Long, List<TypeLocalModel>> = hashMapOf()

    var featureAudit: HashMap<Long, List<FeatureLocalModel>> = hashMapOf()
    var featureType: HashMap<Int, List<FeatureLocalModel>> = hashMapOf()

    private var typeIds: MutableList<Int?> = mutableListOf()
    var typeIdsByAudit: MutableMap<Long, List<Int?>> = hashMapOf()

    var graveAudit: MutableList<GraveLocalModel> = mutableListOf()
    var graveZone: MutableList<GraveLocalModel> = mutableListOf()
    var graveType: MutableList<GraveLocalModel> = mutableListOf()

    private var auditDAO: AuditDao? = null
    private var zoneDao: ZoneDao? = null
    private var typeDao: TypeDao? = null
    private var featureDao: FeatureDao? = null
    private var gravesDao: GravesDao? = null

    private fun load() = audit()?.subscribe({ audit = it }, { it.printStackTrace() }, { zone() })

    private fun audit() = auditDAO?.getAll()?.toObservable()
    private fun zone() {
        val taskHolder: MutableList<Observable<List<ZoneLocalModel>>> = mutableListOf()
        audit.forEach {
            zoneDao?.getAllByAudit(it.auditId)?.let {
                taskHolder.add(it.toObservable())
            }
        }

        taskHolder.merge()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it.isNotEmpty()) {
                        val auditId = it[0].auditId
                        zone[auditId] = it
                    }
                }, { it.printStackTrace() }, { type() })

    }

    private fun type() {
        val taskHolder: MutableList<Observable<List<TypeLocalModel>>> = mutableListOf()
        audit.forEach {
            typeDao?.getAllTypeByAudit(it.auditId)?.let {
                taskHolder.add(it.toObservable())
            }
        }

        taskHolder.merge()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it.isNotEmpty()) {
                        val auditId = it[0].auditId!!
                        type[auditId] = it
                    }

                    it.forEach { typeIds.add(it.auditParentId) }
                    for ((auditId, type) in it.groupBy { it.auditId }) {
                        auditId?.let { typeIdsByAudit[it] = type.map { it.auditParentId } }
                    }

                }, { it.printStackTrace() }, { feature() })
    }

    private fun feature() {
        val taskHolderTypeFeature: MutableList<Observable<List<FeatureLocalModel>>> = mutableListOf()
        typeIds.forEach {
            if (it != null) {
                featureDao?.getAllByType(it)?.let {
                    taskHolderTypeFeature.add(it.toObservable())
                }
            }
        }

        val taskHolderAuditFeature: MutableList<Observable<List<FeatureLocalModel>>> = mutableListOf()
        audit.forEach {
            featureDao?.getAllByAudit(it.auditId)?.let {
                taskHolderAuditFeature.add(it.toObservable())
            }
        }

        fun auditFeatureExec() {
            taskHolderAuditFeature.merge()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({

                        if (it.isNotEmpty()) {
                            it[0].auditId?.let { id ->
                                featureAudit[id] = it
                            }
                        }

                    }, { it.printStackTrace() }, { complete() })
        }

        fun typeFeatureExec() {
            taskHolderTypeFeature.merge()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({

                        if (it.isNotEmpty()) {
                            it[0].typeId?.let { id ->
                                featureType[id] = it
                            }
                        }

                    }, { it.printStackTrace() }, { auditFeatureExec() })
        }

        typeFeatureExec()

    }

    private fun complete() {

        val grave = gravesDao?.getAll()?.toObservable()
        grave?.subscribe({

            it.forEach {
                when {
                    it.type == 0 -> graveAudit.add(it)
                    it.type == 1 -> graveZone.add(it)
                    it.type == 2 -> graveType.add(it)
                }
            }

        }, { it.printStackTrace() }, {

            mListener?.onPostExecute(this)

        })


    }

    override fun toString(): String {
        return "Audit -- $audit" +
                "\nZone -- $zone" +
                "\nType -- $type" +
                "\nFeatureType -- $featureType"
    }

    init {

        Timber.d("Collection :: INIT")
        db = AuditDatabase.newInstance(App.instance)
        auditDAO = db?.auditDao()
        zoneDao = db?.zoneDao()
        typeDao = db?.auditScopeDao()
        featureDao = db?.featureDao()
        gravesDao = db?.gravesDao()

        load()
    }

    companion object {
        fun create(mListener: Listener? = null): Collection {
            Timber.d("Collection :: CREATE")
            return Collection(mListener)
        }
    }

    interface Listener {
        fun onPreExecute()
        fun onPostExecute(col: Collection?)
    }

}
