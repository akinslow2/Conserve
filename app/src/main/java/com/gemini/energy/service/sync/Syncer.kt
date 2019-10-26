@file:Suppress("ControlFlowWithEmptyBody")

package com.gemini.energy.service.sync

import android.annotation.SuppressLint
import com.gemini.energy.data.local.model.AuditLocalModel
import com.gemini.energy.data.local.model.FeatureLocalModel
import com.gemini.energy.data.local.model.TypeLocalModel
import com.gemini.energy.data.local.model.ZoneLocalModel
import com.gemini.energy.service.ParseAPI
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.merge
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import timber.log.Timber
import java.lang.StringBuilder
import java.util.*

@SuppressLint("CheckResult")
class Syncer(private val parseAPIService: ParseAPI.ParseAPIService,
             private var col: Collection,
             private val mListener: Listener? = null) {

    private val auditTaskHolder: MutableList<Observable<JsonObject>> = mutableListOf()
    private val featureTaskHolder: MutableList<Observable<JsonObject>> = mutableListOf()

    private val auditList: MutableList<AuditLocalModel> = mutableListOf()
    private val zoneList: MutableList<ZoneLocalModel> = mutableListOf()
    private val typeList: MutableList<TypeLocalModel> = mutableListOf()

    fun refreshCollection(col: Collection) {
        this.col = col
    }

    private fun prepare() {
        val audit = col.audit
        audit.forEach {
            auditList.add(it)
            buildFeature(it.auditId).forEach {
                featureTaskHolder.add(parseAPIService.saveFeature(it).toObservable())
            }

            auditTaskHolder.add(parseAPIService.saveAudit(buildAudit(it)).toObservable())
        }
    }

    private fun updateGraves() {
        //ToDo - Update the Graves Table so that all the items have +1
    }

    /**
     * DOWNLOAD - MAIN
     * */
    fun gDownload() {
        mListener?.onPreExecute()
        download()
    }

    /**
     * UPLOAD - MAIN
     * */
    fun gUpload() {
        prepare()
        uploadAudit()
    }

    private fun download() {

        val toDeleteAudit: List<Long> = col.graveAudit.map { it.oid }
        val toDeleteZone: List<Long> = col.graveZone.map { it.oid }
        val toDeleteType: List<Long> = col.graveType.map { it.oid }

        fun splitFields(fields: String): List<String> {
            return fields.split("\\x1f".toRegex())
        }

        fun taskFeature() {

            parseAPIService.fetchFeature().toObservable()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        val rFeature = it.getAsJsonArray("results")
                        val models: MutableList<FeatureLocalModel> = mutableListOf()

                        rFeature.forEach {

                            val auditId = it.asJsonObject.get("auditId").asString
                            val zoneId = it.asJsonObject.get("zoneId").asString
                            val typeId = it.asJsonObject.get("typeId").asString
                            val belongsTo = it.asJsonObject.get("belongsTo").asString

                            val usn = it.asJsonObject.get("usn").asInt
                            val mod = it.asJsonObject.get("mod").asString

                            val values = splitFields(it.asJsonObject.get("values").asString)
                            val dataTypes = splitFields(it.asJsonObject.get("dataType").asString)
                            val fields = splitFields(it.asJsonObject.get("fields").asString)
                            val formIds = splitFields(it.asJsonObject.get("formId").asString)
                            val id = splitFields(it.asJsonObject.get("id").asString)

                            val _auditId = auditId.toLong()

                            // *** Load Feature by Type to the Local DB
                            if (typeId != "null") {
                                if (col.featureType.containsKey(typeId.toLong())) {
                                    val localFeature = col.featureType[typeId.toLong()]
                                    val lMod = localFeature?.map { it.updatedAt.time }?.max()

                                    Timber.d("Local Updated At (Feature - Type)")
                                    Timber.d(lMod.toString())

                                    Timber.d("Remote Last Modified At (Feature - Type)")
                                    Timber.d(mod)

                                    lMod?.let {
                                        if (lMod > mod.toLong()) { /*DO NOTHING*/ }
                                        else {
                                            for (i in 0 until formIds.count() - 1) {
                                                val feature = FeatureLocalModel(id[i].toInt(), formIds[i].toInt(), belongsTo,
                                                        dataTypes[i], usn, null, zoneId.toLong(), typeId.toLong(), fields[i],
                                                        values[i], null, null, Date(), Date())
                                                models.add(feature)
                                            }
                                        }
                                    }

                                } else {
                                    Timber.d("---------- Feature Type Fresh Entry -----------")
                                    if (toDeleteType.contains(typeId.toLong())) { /*DO NOTHING*/ }
                                    else {
                                        for (i in 0 until formIds.count() - 1) {
                                            val feature = FeatureLocalModel(id[i].toInt(), formIds[i].toInt(), belongsTo,
                                                    dataTypes[i], usn, null, zoneId.toLong(), typeId.toLong(), fields[i],
                                                    values[i], null, null, Date(), Date())
                                            models.add(feature)
                                        }
                                    }
                                }
                            }

                            if (typeId == "null") {
                                if (col.featureAudit.containsKey(_auditId)) {
                                    val localFeature = col.featureAudit[_auditId]
                                    val lMod = localFeature?.map { it.updatedAt.time }?.max()

                                    Timber.d("Local Updated At (Feature - Audit)")
                                    Timber.d(lMod.toString())

                                    Timber.d("Remote Last Modified At (Feature - Audit)")
                                    Timber.d(mod)

                                    lMod?.let {
                                        if (lMod > mod.toLong()) { /*DO NOTHING*/ }
                                        else {
                                            for (i in 0 until formIds.count() - 1) {
                                                val feature = FeatureLocalModel(id[i].toInt(), formIds[i].toInt(), belongsTo,
                                                        dataTypes[i], usn, _auditId, null, null, fields[i],
                                                        values[i], null, null, Date(), Date())
                                                models.add(feature)
                                            }
                                        }
                                    }

                                } else {
                                    Timber.d("---------- Feature Audit Fresh Entry -----------")
                                    if (toDeleteAudit.contains(_auditId)) { /*DO NOTHING*/ }
                                    else {
                                        for (i in 0 until formIds.count() - 1) {
                                            val feature = FeatureLocalModel(id[i].toInt(), formIds[i].toInt(), belongsTo,
                                                    dataTypes[i], usn, _auditId, null, null, fields[i],
                                                    values[i], null, null, Date(), Date())
                                            models.add(feature)
                                        }
                                    }
                                }
                            }
                        }

                        col.db?.featureDao()?.insert(models)

                    }, { it.printStackTrace() }, {

                        Timber.d("-- Feature Fetch Complete --")
                        mListener?.onPostDownload(this)

                    })
        }

        fun taskAudit() {

            parseAPIService.fetchAudit().toObservable()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({

                        val rAudit = it.getAsJsonArray("results")
                        rAudit.forEach {

                            var auditId = ""
                            var name = ""
                            var usn = -99
                            var objectId = ""
                            var zone: JsonObject? = null
                            var type: JsonObject? = null
                            var mod = ""

                            try {
                                auditId = it.asJsonObject.get("auditId").asString
                                name = it.asJsonObject.get("name").asString
                                usn = it.asJsonObject.get("usn").asInt
                                mod = it.asJsonObject.get("mod").asString
                                objectId = it.asJsonObject.get("objectId").asString

                                zone = it.asJsonObject.get("zone").asJsonObject
                                type = it.asJsonObject.get("type").asJsonObject
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            Timber.d(zone.toString())
                            Timber.d(type.toString())

                            val _auditId = auditId.toLong()

                            // 1. Audit Entry - To local DB
                            Timber.d("<<<< AUDIT >>>>")
                            val localAuditId = col.audit.associateBy { it.auditId }
                            if (!localAuditId.containsKey(_auditId)) {
                                val model = AuditLocalModel(_auditId, name, usn, objectId, Date(), Date())
                                if (toDeleteAudit.contains(_auditId)) { /*DO NOTHING*/ }
                                else { col.db?.auditDao()?.insert(model) }
                            } else {
                                val local = localAuditId[_auditId]
                                Timber.d("Local Updated At")
                                Timber.d(local?.updatedAt?.time.toString())

                                Timber.d("Remote Updated At")
                                Timber.d(mod.toLong().toString())

                                local?.let {
                                    if (it.updatedAt.time > mod.toLong()) { /*DO NOTHING*/ }
                                    else {
                                        val model = AuditLocalModel(_auditId, name, usn, objectId, Date(), Date())
                                        col.db?.auditDao()?.insert(model)
                                    }
                                }
                            }

                            // 2. Zone Entry - To local DB
                            Timber.d("<<<< ZONE >>>>")
                            zone?.keySet()?.forEach {
                                val inner = zone.get(it).asJsonObject
                                val iUsn = inner.get("usn").asInt
                                val iName = inner.get("name").asString
                                val iMod = inner.get("mod").asString
                                val iId = inner.get("id").asLong

                                if (col.zone.containsKey(_auditId)) {
                                    val zones = col.zone[_auditId]
                                    zones?.let {
                                        val ids = it.map { it.zoneId }
                                        if (ids.contains(iId)) {
                                            val localZone = it[ids.indexOf(iId)]

                                            Timber.d("Local Updated At")
                                            Timber.d(localZone.updatedAt.time.toString())

                                            Timber.d("Remote Last Modified At")
                                            Timber.d(iMod)

                                            if (localZone.updatedAt.time > iMod.toLong()) { /*DO NOTHING*/ }
                                            else {
                                                Timber.d("-------- Zone Fresh Entry ----------")
                                                val model = ZoneLocalModel(iId, iName, "Sample Zone", iUsn, _auditId, Date(), Date())
                                                if (toDeleteZone.contains(iId)) { /*DO NOTHING*/ }
                                                else {col.db?.zoneDao()?.insert(model) }
                                            }

                                        } else {
                                            Timber.d("-------- Zone Fresh Entry ----------")
                                            val model = ZoneLocalModel(iId, iName, "Sample Zone", iUsn, _auditId, Date(), Date())
                                            if (toDeleteZone.contains(iId)) { /*DO NOTHING*/ }
                                            else {col.db?.zoneDao()?.insert(model) }
                                        }
                                    }
                                } else {
                                    Timber.d("-------- Zone Fresh Entry ----------")
                                    val model = ZoneLocalModel(iId, iName, "Sample Zone", iUsn, _auditId, Date(), Date())
                                    if (toDeleteZone.contains(iId)) { /*DO NOTHING*/ }
                                    else {col.db?.zoneDao()?.insert(model) }
                                }
                            }

                            // 3. Type Entry - To Local DB
                            Timber.d("<<<< TYPE >>>>")
                            type?.keySet()?.forEach {
                                val inner = type.get(it).asJsonObject
                                val iUsn = inner.get("usn").asInt
                                val iName = inner.get("name").asString
                                val iType = inner.get("type").asString
                                val iSubType = inner.get("subtype").asString
                                val iMod = inner.get("mod").asString
                                val iId = inner.get("id").asLong
                                val iZoneId = inner.get("zoneId").asLong

                                if (col.type.containsKey(_auditId)) {
                                    val types = col.type[_auditId]
                                    types?.let {
                                        val ids = it.map { it.auditParentId }
                                        if (ids.contains(iId)) {
                                            val localType = it[ids.indexOf(iId)]

                                            Timber.d("Local Updated At")
                                            Timber.d(localType.updatedAt?.time.toString())

                                            Timber.d("Remote Last Modified At")
                                            Timber.d(iMod)

                                            localType.updatedAt?.let {
                                                if (it.time > iMod.toLong()) { /*DO NOTHING*/}
                                                else {
                                                    Timber.d("------------ Type Fresh Entry --------------------")
                                                    val model = TypeLocalModel(iId, iName, iType, iSubType, iUsn, iZoneId, _auditId, Date(), Date())
                                                    col.db?.auditScopeDao()?.insert(model)
                                                }
                                            }

                                        } else {
                                            Timber.d("------------ Type Fresh Entry --------------------")
                                            val model = TypeLocalModel(iId, iName, iType, iSubType, iUsn, iZoneId, _auditId, Date(), Date())
                                            if (toDeleteType.contains(iId)) { /*DO NOTHING*/ }
                                            else {col.db?.auditScopeDao()?.insert(model)}
                                        }
                                    }
                                } else {
                                    Timber.d("------------ Type Fresh Entry --------------------")
                                    val model = TypeLocalModel(iId, iName, iType, iSubType, iUsn, iZoneId, _auditId, Date(), Date())
                                    if (toDeleteType.contains(iId)) { /*DO NOTHING*/ }
                                    else {col.db?.auditScopeDao()?.insert(model)}
                                }
                            }

                        }

                    }, { it.printStackTrace() }, {

                        Timber.d("-- AUDIT DOWNLOAD COMPLETE --")
                        taskFeature()

                    })
        }

        taskAudit()

    }

    private fun uploadAudit() {
        auditTaskHolder.merge()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    val objectId = it.get("objectId").asString
                    val query = JSONObject().put("objectId", objectId)
                    parseAPIService.fetchAudit(query.toString()).toObservable()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({

                                val rAudit = it.getAsJsonArray("results")
                                rAudit.forEach {
                                    val auditId = it.asJsonObject.get("auditId").asString
                                    val usn = it.asJsonObject.get("usn").asInt

                                    Timber.d("OBJECT ID -- $objectId")
                                    Timber.d("AUDIT ID -- $auditId")
                                    Timber.d("USN -- $usn")

                                    auditList.forEach {
                                        if (it.auditId == auditId.toLong()) {
                                            it.objectId = objectId
                                            it.usn = usn
                                            col.db?.auditDao()?.update(it)
                                        }
                                    }
                                }

                            }, { it.printStackTrace() }, {})


                }, { it.printStackTrace() }, {

                    Timber.d("Complete - Audit Upload"); uploadFeature()
                    auditList.forEach {
                        val query = JSONObject().put("auditId", it.auditId.toString())
                        parseAPIService.fetchAudit(query.toString()).toObservable()
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({

                                    val rAudit = it.getAsJsonArray("results")

                                    Timber.d("<<< Clean Up - Audit [${rAudit.count()}]>>")
                                    Timber.d(rAudit.toString())

                                    if (rAudit.count() > 1) {

                                        val maxUSN = rAudit.map {
                                            it.asJsonObject.get("usn").asInt
                                        }.max()

                                        val clean = rAudit.filter { maxUSN != it.asJsonObject.get("usn").asInt }
                                        clean.forEach {
                                            val objectId = it.asJsonObject.get("objectId").asString
                                            Timber.d("Clean - [$objectId]")
                                            parseAPIService.deleteAudit(objectId).toObservable()
                                                    .subscribeOn(Schedulers.io())
                                                    .observeOn(AndroidSchedulers.mainThread())
                                                    .subscribe({ Timber.d(it.toString()) }, { it.printStackTrace() }, {})
                                        }

                                    }

                                }, { it.printStackTrace() }, {})
                    }
                })
    }

    private fun uploadFeature() {

        val objectIds: MutableList<String> = mutableListOf()
        fun extract(item: JsonObject) {
            val rFeature = item.getAsJsonArray("results")
            rFeature.forEach {
                objectIds.add(it.asJsonObject.get("objectId").asString)
            }
        }

        val collectObjectIds: MutableList<Observable<JsonObject>> = mutableListOf()
        val deletions: MutableList<Observable<JsonObject>> = mutableListOf()

        fun upload() {
            featureTaskHolder.merge()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ Timber.d(it.toString()) }, { it.printStackTrace() },
                        { Timber.d("Complete - Feature Upload"); mListener?.onPostExecute(); updateGraves() })

        }

        fun clean() {
            deletions.merge()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ Timber.d(it.toString()) }, { it.printStackTrace() }, {
                        Timber.d("Complete - Feature CleanUp ")
                        upload()
                    })
        }

        auditList.forEach {
            val query = JSONObject().put("auditId", it.auditId.toString())
            collectObjectIds.add(parseAPIService.fetchFeature(query.toString()).toObservable())
        }

        collectObjectIds.merge()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ extract(it) }, { it.printStackTrace() }, {
                    objectIds.forEach { deletions.add(parseAPIService.deleteFeature(it).toObservable()) }
                    clean()
                })
    }

    /**
     * 1. Audit
     * */
    private fun buildAudit(local: AuditLocalModel): JsonObject {
        val outgoing = JsonObject()
        outgoing.addProperty("auditId", local.auditId.toString())
        outgoing.addProperty("name", local.name)
        outgoing.addProperty("usn", ++local.usn)
        outgoing.addProperty("mod", Date().time.toString())

        outgoing.add("zone", buildZone(local.auditId))
        outgoing.add("type", buildType(local.auditId))

        Timber.d(outgoing.toString())

        return outgoing
    }

    /**
     * 2. Zone
     * */
    private fun buildZone(auditId: Long): JsonObject {

        fun associatedTypes(zoneId: Long?): JsonArray {
            val outgoing = JsonArray()
            if (col.type.containsKey(auditId)) {
                val type = col.type[auditId]
                type?.forEach {
                    if (it.zoneId == zoneId) {
                        outgoing.add(it.auditParentId)
                    }
                }
            }
            return outgoing
        }

        val outgoing = JsonObject()
        if (col.zone.containsKey(auditId)) {
            val zone = col.zone[auditId]
            zone?.let {
                it.forEach {

                    zoneList.add(it)

                    val inner = JsonObject()
                    inner.addProperty("usn", ++it.usn)
                    inner.addProperty("name", it.name)
                    inner.add("typeId", associatedTypes(it.zoneId))
                    inner.addProperty("mod", Date().time)
                    inner.addProperty("id", it.zoneId)

                    outgoing.add(it.zoneId.toString(), inner)
                }
            }
        }

        return outgoing
    }

    /**
     * 3. Type
     * */
    private fun buildType(auditId: Long): JsonObject {
        val outgoing = JsonObject()
        if (col.type.containsKey(auditId)) {
            val type = col.type[auditId]
            type?.let {
                it.forEach {

                    typeList.add(it)

                    val inner = JsonObject()
                    inner.addProperty("usn", ++it.usn)
                    inner.addProperty("name", it.name)
                    inner.addProperty("type", it.type)
                    inner.addProperty("subtype", it.subType)
                    inner.addProperty("mod", Date().time)
                    inner.addProperty("id", it.auditParentId)
                    inner.addProperty("zoneId", it.zoneId)

                    outgoing.add(it.auditParentId.toString(), inner)
                }
            }
        }

        return outgoing
    }

    /**
     * 4. Feature
     * */
    private fun buildFeature(auditId: Long): List<JsonObject> {

        fun joinFields(fields: List<String?>): String {
            val result = StringBuilder(128)
            fields.forEach {
                it?.let {
                    result.append(it).append("\u001f")
                }
            }

            return result.toString()
        }

        fun create(auditId: Long, model: List<FeatureLocalModel>): JsonObject {
            val inner = JsonObject()
            inner.addProperty("usn", 0)
            inner.addProperty("auditId", auditId.toString())
            inner.addProperty("zoneId", model[0].zoneId.toString())
            inner.addProperty("typeId", model[0].typeId.toString())
            inner.addProperty("mod", Date().time.toString())
            inner.addProperty("belongsTo", model[0].belongsTo)

            inner.addProperty("id", joinFields(model.map { it.featureId.toString() }))
            inner.addProperty("dataType", joinFields(model.map { it.dataType }))
            inner.addProperty("formId", joinFields(model.map { it.formId.toString() }))
            inner.addProperty("fields", joinFields(model.map { it.key }))
            inner.addProperty("values", joinFields(model.map { it.valueString }))

            return inner
        }

        val outgoing: MutableList<JsonObject> = mutableListOf()

        if (col.typeIdsByAudit.containsKey(auditId)) {
            val typeIds = col.typeIdsByAudit[auditId]
            typeIds?.let {
                it.forEach {
                    it?.let {
                        if (col.featureType.containsKey(it)) {
                            val feature = col.featureType[it]
                            feature?.let {
                                if (it.isNotEmpty()) { outgoing.add(create(auditId, it)) }
                            }
                        }
                    }
                }
            }
        }

        if (col.featureAudit.containsKey(auditId)) {
            val feature = col.featureAudit[auditId]
            feature?.let {
                if (it.isNotEmpty()) { outgoing.add(create(auditId, it)) }
            }

        }

        return outgoing
    }

    interface Listener {
        fun onPreExecute()
        fun onPostExecute()
        fun onPostDownload(sync: Syncer)
    }

}
