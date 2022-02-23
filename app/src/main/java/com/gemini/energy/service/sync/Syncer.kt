@file:Suppress("ControlFlowWithEmptyBody")

package com.gemini.energy.service.sync

import android.annotation.SuppressLint
import com.gemini.energy.data.local.model.*
import com.gemini.energy.prettyPrint
import com.gemini.energy.service.ParseAPI
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.merge
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import timber.log.Timber
import java.util.*

@SuppressLint("CheckResult")
class Syncer(private val parseAPIService: ParseAPI.ParseAPIService,
             private var col: Collection,
             private val mListener: Listener? = null) {

    private val auditTaskHolder: MutableList<Observable<JsonObject>> = mutableListOf()
    private val featureTaskHolder: MutableList<Observable<JsonObject>> = mutableListOf()
    private val gravesTaskHolder: MutableList<Observable<JsonObject>> = mutableListOf()

    private val auditList: MutableList<AuditLocalModel> = mutableListOf()
    private val zoneList: MutableList<ZoneLocalModel> = mutableListOf()
    private val typeList: MutableList<TypeLocalModel> = mutableListOf()

    private val rGraveIds: MutableList<Long> = mutableListOf()
    private val rGraveAuditIds: MutableList<Long> = mutableListOf()
    private val rGraveZoneIds: MutableList<Long> = mutableListOf()
    private val rGraveTypeIds: MutableList<Long> = mutableListOf()

    fun refreshCollection(col: Collection) {
        Timber.i("~~~~~ Syncer: refreshCollection(${col.audit.count()} audits in collection) ")
        this.col = col
    }

    private fun prepare() {
        Timber.i("~~~~~ Syncer: prepare()")

        val audit = col.audit

        Timber.i("~~~~~ Syncer: iterating through ${audit.count()} audits")
        audit.forEach { it ->
            Timber.i("~~~~~ Syncer: checking on audit ${it.auditId} ${it.name}")
            if (rGraveIds.contains(it.auditId) || col.graveIds.contains(it.auditId)) {
                Timber.i("~~~~~ Syncer: deleting audit that belongs in the grave")
                deleteLocalAudit(it.auditId)
            } else {
                Timber.i("~~~~~ Syncer: adding audit to auditList and audit task holder")
                auditList.add(it)
                buildFeature(it.auditId).forEach {
                    Timber.i("~~~~~ Syncer: built feature ${it.toString()}")
                    Timber.i("~~~~~ Syncer: adding saving feature to parse api to feature task holder")
                    featureTaskHolder.add(parseAPIService.saveFeature(it).toObservable())
                }

                Timber.i("~~~~~ Syncer: adding build and save audit to parse to audit task holder for ${it.auditId} ${it.name}")
                auditTaskHolder.add(parseAPIService.saveAudit(buildAudit(it)).toObservable())
            }
        }
    }

    private fun deleteLocalType(id: Long) {
        Timber.i("~~~~~ Syncer: deleteLocalType($id)")
        col.featureDao?.deleteByTypeId(id)
        col.typeDao?.delete(id)
    }

    private fun deleteLocalZone(id: Long) {
        Timber.i("~~~~~ Syncer: deleteLocalZone($id)")
        col.featureDao?.deleteByZoneId(id)
        col.typeDao?.deleteByZoneId(id)
        col.zoneDao?.delete(id)
    }

    private fun deleteLocalAudit(id: Long) {
        Timber.i("~~~~~ Syncer: deleteLocalAudit($id)")
        col.featureDao?.deleteByAuditId(id)
        col.typeDao?.deleteByAuditId(id)
        col.zoneDao?.deleteByAuditId(id)
        col.auditDAO?.delete(id)
    }

    private fun updateGraves() {
        Timber.i("~~~~~ Syncer: updateGraves()")

        fun buildGraves(grave: GraveLocalModel): JsonObject {
            Timber.i("~~~~~ Syncer: updateGraves().buildGraves($grave)")
            val outgoing = JsonObject()
            outgoing.addProperty("usn", ++grave.usn)
            outgoing.addProperty("oid", grave.oid)
            outgoing.addProperty("type", grave.type)

            return outgoing
        }

        col.grave.forEach {
            gravesTaskHolder.add(parseAPIService.saveGraves(buildGraves(it)).toObservable())
        }

        gravesTaskHolder.merge()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { Timber.d(it.toString()) },
                        { it.printStackTrace() },
                        {
                            col.grave.forEach {
                                col.gravesDao?.update(it.oid, it.usn)
                            }
                            mListener?.onPostExecute()
                        })
    }

    /**
     * DOWNLOAD - MAIN
     * */
    fun gDownload() {
        Timber.i("~~~~~ Syncer: gDownload()")
        mListener?.onPreExecute()
        download()
    }

    /**
     * UPLOAD - MAIN
     * */
    fun gUpload() {
        Timber.i("~~~~~ Syncer: gUpload()")
        mListener?.onPostExecute()
        // comment back in the following lines and remove the above line to upload data back to server
//        prepare()
//        uploadAudit()
    }

    private fun download() {
        Timber.i("~~~~~Syncer:  download()")

        val toDeleteAudit: List<Long> = col.graveAudit.map { it.oid }
        val toDeleteZone: List<Long> = col.graveZone.map { it.oid }
        val toDeleteType: List<Long> = col.graveType.map { it.oid }

        fun splitFields(fields: String): List<String> {
            return fields.split("\\x1f".toRegex())
        }

        fun taskFeature() {
            Timber.i("~~~~~Syncer:  download().taskFeature()")

            parseAPIService.fetchFeature().toObservable()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ allFeatures ->
                        val rFeature = allFeatures.getAsJsonArray("results")
                        val models: MutableList<FeatureLocalModel> = mutableListOf()

                        Timber.i("~~~~~Syncer:  download().taskFeature() got ${rFeature.count()} features from server")
                        rFeature.forEach {

                            val auditId = it.asJsonObject.get("auditId").asString
                            var zoneId: String? = null
                            if (!it.asJsonObject.get("zoneId").isJsonNull) {
                                zoneId = it.asJsonObject.get("zoneId").asString
                            }

                            var typeId: String? = null
                            if (!it.asJsonObject.get("typeId").isJsonNull) {
                                typeId = it.asJsonObject.get("typeId").asString
                            }

                            val belongsTo = it.asJsonObject.get("belongsTo").asString

                            val usn = it.asJsonObject.get("usn").asInt
                            val mod = it.asJsonObject.get("mod").asString

                            val values = splitFields(it.asJsonObject.get("values").asString)
                            val dataTypes = splitFields(it.asJsonObject.get("dataType").asString)
                            val fields = splitFields(it.asJsonObject.get("fields").asString)
                            val formIds = splitFields(it.asJsonObject.get("formId").asString)

                            var id: List<String>? = null
                            if (it.asJsonObject.has("id") && !it.asJsonObject.get("id").isJsonNull) {
                                id = splitFields(it.asJsonObject.get("id").asString)
                            }

                            val _auditId = auditId.toLong()

                            fun addFeatureToModel(formIds: List<String>) {
                                if (formIds.contains("lamps_fixture")) {
                                    Timber.e("Can't add feature to model")
                                    Timber.i("audit id $auditId")
                                    // HACK: don't add feature to model if
                                    // form id is not an underlying int

                                    // lamps_fixture is the first string in broken equipment
                                    return
                                }
                                for (i in 0 until formIds.count() - 1) {
                                    val feature = FeatureLocalModel(
                                            featureId = id?.get(i)?.toInt(),
                                            formId = formIds[i].toIntOrNull(),
                                            belongsTo = belongsTo,
                                            dataType = dataTypes[i],
                                            usn = usn,
                                            auditId = null,
                                            zoneId = zoneId?.toLongOrNull(),
                                            typeId = typeId?.toLongOrNull(),
                                            key = fields[i],
                                            valueString = values[i],
                                            valueInt = null,
                                            valueDouble = null,
                                            createdAt = Date(),
                                            updatedAt = Date())
                                    models.add(feature)
                                }
                            }

                            fun addPreAuditToModel(formIds: List<String>) {
                                for (i in 0 until formIds.count() - 1) {
                                    val feature = FeatureLocalModel(
                                            id?.get(i)?.toInt(), formIds[i].toInt(),
                                            belongsTo, dataTypes[i],
                                            usn, _auditId,
                                            null, null,
                                            fields[i], values[i],
                                            null, null,
                                            Date(), Date())
                                    models.add(feature)
                                }
                            }

                            // non preaudit data
                            // *** Load Feature by Type to the Local DB
                            if (typeId != "null") {
                                if (col.featureType.containsKey(typeId?.toLong())) {
                                    // make sure data from server always overrides local data
                                    addFeatureToModel(formIds)
                                } else {
                                    if (toDeleteType.contains(typeId?.toLong())) { /*DO NOTHING*/
                                    } else {
                                        addFeatureToModel(formIds)
                                    }
                                }
                            }

                            // preaudit data
                            if (typeId == "null") {
                                if (col.featureAudit.containsKey(_auditId)) {
                                    addPreAuditToModel(formIds)
                                } else {
                                    if (toDeleteAudit.contains(_auditId)) { /*DO NOTHING*/
                                    } else {
                                        addPreAuditToModel(formIds)
                                    }
                                }
                            }
                        }

                        col.db?.featureDao()?.insert(models)


                    }, { it.printStackTrace() }, { mListener?.onPostDownload(this) })
        } // end taskFeature()

        fun taskAudit() {
            Timber.i("~~~~~Syncer:  download().taskAudit()")
            parseAPIService.fetchAudit().toObservable()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        val rAudit = it.getAsJsonArray("results")
                        Timber.i("~~~~~ Syncer: iterating through ${rAudit.count()} audits from parse")

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

                            val _auditId = auditId.toLong()

                            // 1. Audit Entry - To local DB
                            val localAuditId = col.audit.associateBy { it.auditId }
                            val auditLocalModel = AuditLocalModel(_auditId, name, usn, objectId, Date(), Date())
                            if (!localAuditId.containsKey(_auditId)) {
                                if (toDeleteAudit.contains(_auditId)) {
                                    //DELETING LOCAL AUDIT//
                                    deleteLocalAudit(_auditId)
                                } else {
                                    col.db?.auditDao()?.insert(auditLocalModel)
                                }
                            } else {
                                col.db?.auditDao()?.insert(auditLocalModel)
                            }

                            // 2. Zone Entry - To local DB
                            zone?.keySet()?.forEach {
                                val inner = zone.get(it).asJsonObject
                                val iUsn = inner.get("usn").asInt
                                val iName = inner.get("name").asString
                                val iMod = inner.get("mod").asString
                                val iId = inner.get("id").asLong

                                if (col.zone.containsKey(_auditId)) {
                                    val localZones = col.zone[_auditId]
                                    localZones?.let {
                                        val localZoneIds = it.map { it.zoneId }
                                        if (localZoneIds.contains(iId)) {
                                            if (toDeleteZone.contains(iId)) {
                                                deleteLocalZone(iId)
                                            } else {
                                                val model = ZoneLocalModel(iId, iName, "Sample Zone", iUsn, _auditId, Date(), Date())
                                                col.db?.zoneDao()?.insert(model)
                                            }
                                        } else {
                                            val model = ZoneLocalModel(iId, iName, "Sample Zone", iUsn, _auditId, Date(), Date())
                                            if (toDeleteZone.contains(iId)) {
                                                deleteLocalZone(iId)
                                            } else {
                                                col.db?.zoneDao()?.insert(model)
                                            }
                                        }
                                    }
                                } else {
                                    val model = ZoneLocalModel(iId, iName, "Sample Zone", iUsn, _auditId, Date(), Date())
                                    if (toDeleteZone.contains(iId)) {
                                        deleteLocalZone(iId)
                                    } else {
                                        col.db?.zoneDao()?.insert(model)
                                    }
                                }
                            }

                            // 3. Type Entry - To Local DB
                            type?.keySet()?.forEach {
                                val inner = type.get(it).asJsonObject

                                val iUsn = inner.get("usn").asInt
                                val iName = inner.get("name").asString
                                val iType = inner.get("type").asString
                                var iSubType: String? = null
                                if (!inner.get("subtype").isJsonNull) {
                                    iSubType = inner.get("subtype").asString
                                }
                                val iMod = inner.get("mod").asString
                                val iId = inner.get("id").asLong
                                val iZoneId = inner.get("zoneId").asLong

                                val typeModel = TypeLocalModel(iId, iName, iType, iSubType, iUsn, iZoneId, _auditId, Date(), Date())
                                if (col.type.containsKey(_auditId)) {
                                    val types = col.type[_auditId]
                                    types?.let {
                                        val ids = it.map { it.auditParentId }
                                        if (ids.contains(iId)) {
                                            col.db?.auditScopeDao()?.insert(typeModel)
                                        } else {
                                            if (toDeleteType.contains(iId)) {
                                                deleteLocalType(iId)
                                            } else {
                                                col.db?.auditScopeDao()?.insert(typeModel)
                                            }
                                        }
                                    }
                                } else {
                                    if (toDeleteType.contains(iId)) {
                                        deleteLocalType(iId)
                                    } else {
                                        col.db?.auditScopeDao()?.insert(typeModel)
                                    }
                                }
                            }
                        }
                    }, { it.printStackTrace() }, { taskFeature() })
        } // end taskAudit()

        Timber.i("~~~~~ Syncer: downloading graves from parse")
        parseAPIService.fetchGraves().toObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ it ->
                    val grave = it.getAsJsonArray("results")
                    grave.forEach {
                        try {
                            val oid = it.asJsonObject.get("oid").asLong
                            val type = it.asJsonObject.get("type").asInt
                            rGraveIds.add(oid)
                            when (type) {
                                0 -> rGraveAuditIds.add(oid)
                                1 -> rGraveZoneIds.add(oid)
                                2 -> rGraveTypeIds.add(oid)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }, { it.printStackTrace() }, { taskAudit() })

    } // end download()

    private fun uploadAudit() {
        Timber.i("~~~~~Syncer:  uploadAudit()")

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

                                    auditList.forEach {
                                        if (it.auditId == auditId.toLong()) {
                                            it.objectId = objectId
                                            it.usn = usn
                                            col.db?.auditDao()?.update(it)
                                        }
                                    }
                                }
                            }, { it.printStackTrace() }, {})
                }, { it.printStackTrace() }
                        , {
                    uploadFeature()
                    val updatedAudit = auditList.toMutableList()
                    val toDeleteAudit: MutableList<Long> = col.graveAudit.map { it.oid }.toMutableList()
                    toDeleteAudit.addAll(rGraveAuditIds)

                    /**
                     * We are appending Grave Audit from the Parse Database too, Imp: the
                     * local grave collection only queries for Graves where USN is -1
                     * As of now getting the Audit Ids from Remote works maybe will fix this later
                     */

                    toDeleteAudit.distinct().forEach {
                        updatedAudit.add(AuditLocalModel(it, "", 0, "", Date(), Date()))
                    }

                    updatedAudit.forEach { _aId ->
                        val query = JSONObject().put("auditId", _aId.auditId.toString())
                        parseAPIService.fetchAudit(query.toString()).toObservable()
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({ it ->
                                    val rAudit = it.getAsJsonArray("results")

                                    if (rAudit.count() > 1 || rGraveIds.contains(_aId.auditId) ||
                                            col.graveIds.contains(_aId.auditId)) {

                                        var maxUSN = rAudit
                                                .map { it.asJsonObject.get("usn").asInt }
                                                .max()

                                        if (rGraveIds.contains(_aId.auditId) ||
                                                col.graveIds.contains(_aId.auditId)) {
                                            maxUSN = -99
                                        }
                                        val clean = rAudit.filter { maxUSN != it.asJsonObject.get("usn").asInt }
                                        clean.forEach {
                                            val objectId = it.asJsonObject.get("objectId").asString
                                            parseAPIService.deleteAudit(objectId).toObservable()
                                                    .subscribeOn(Schedulers.io())
                                                    .observeOn(AndroidSchedulers.mainThread())
                                                    .subscribe(
                                                            {
                                                                Timber.d(it.toString())
                                                            },
                                                            { it.printStackTrace() },
                                                            {})
                                        }
                                    }
                                }, { it.printStackTrace() }, {})
                    }
                })
    } // end uploadAudit()

    private fun uploadFeature() {
        Timber.i("~~~~~ Syncer: uploadFeature()")

        val objectIds: MutableList<String> = mutableListOf()
        fun extract(item: JsonObject) {
            Timber.i("~~~~~ Syncer: uploadFeature().extract(${item.prettyPrint()})")
            val rFeature = item.getAsJsonArray("results")
            rFeature.forEach {
                objectIds.add(it.asJsonObject.get("objectId").asString)
            }
        }

        val collectObjectIds: MutableList<Observable<JsonObject>> = mutableListOf()
        val deletions: MutableList<Observable<JsonObject>> = mutableListOf()

        fun upload() {
            Timber.i("~~~~~ Syncer: uploadFeature().upload")
            featureTaskHolder.merge()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            {
                                Timber.d(it.toString())
                            },
                            {
                                it.printStackTrace()
                            },
                            {
                                Timber.d("Complete - Feature Upload")
                                updateGraves()
                            })

        }

        fun clean() {
            Timber.i("~~~~~ Syncer: uploadFeature().clean()")
            deletions.merge()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            {
                                Timber.d(it.toString())
                            },
                            { it.printStackTrace() },
                            {
                                Timber.d("Complete - Feature CleanUp ")
                                upload()
                            })
        }

        val updatedAudit = auditList.toMutableList()
        val toDeleteAudit: MutableList<Long> = col.graveAudit.map { it.oid }.toMutableList()
        toDeleteAudit.addAll(rGraveAuditIds)
        toDeleteAudit.distinct().forEach {
            updatedAudit.add(AuditLocalModel(it, "", 0, "", Date(), Date()))
        }

        updatedAudit.forEach {
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
    } // end uploadFeature()

    /**
     * 1. Audit
     * */
    private fun buildAudit(local: AuditLocalModel): JsonObject {
        Timber.i("~~~~~ Syncer: buildAudit(${local})")

        val outgoing = JsonObject()
        outgoing.addProperty("auditId", local.auditId.toString())
        outgoing.addProperty("name", local.name)
        outgoing.addProperty("usn", ++local.usn)
        outgoing.addProperty("mod", Date().time.toString())

        outgoing.add("zone", buildZone(local.auditId))
        outgoing.add("type", buildType(local.auditId))

        return outgoing
    }

    /**
     * 2. Zone
     * */
    private fun buildZone(auditId: Long): JsonObject {
        Timber.i("~~~~~ Syncer: buildZone($auditId)")

        fun associatedTypes(zoneId: Long?): JsonArray {
            Timber.i("~~~~~ Syncer: buildZone($auditId).associatedTypes($zoneId)")
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
            zone?.let { it ->
                it.forEach {
                    zoneList.add(it)

                    val inner = JsonObject()
                    inner.addProperty("usn", ++it.usn)
                    inner.addProperty("name", it.name)
                    inner.add("typeId", associatedTypes(it.zoneId))
                    inner.addProperty("mod", Date().time)
                    inner.addProperty("id", it.zoneId)

                    if (rGraveIds.contains(it.zoneId) ||
                            col.graveIds.contains(it.zoneId)) {
                        deleteLocalZone(it.zoneId)
                    } else {
                        outgoing.add(it.zoneId.toString(), inner)
                    }
                }
            }
        }
        return outgoing
    }

    /**
     * 3. Type
     * */
    private fun buildType(auditId: Long): JsonObject {
        Timber.i("~~~~~ Syncer: buildType($auditId)")

        val outgoing = JsonObject()
        if (col.type.containsKey(auditId)) {
            val type = col.type[auditId]
            type?.let { it ->
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

                    if (rGraveIds.contains(it.auditParentId) ||
                            col.graveIds.contains(it.auditParentId)) {
                        deleteLocalType(it.auditParentId)
                    } else {
                        outgoing.add(it.auditParentId.toString(), inner)
                    }
                }
            }
        }
        return outgoing
    }

    /**
     * 4. Feature
     * */
    private fun buildFeature(auditId: Long): List<JsonObject> {
        Timber.i("~~~~~ Syncer: buildFeature($auditId)")

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
            Timber.i("~~~~~ Syncer: buildFeature($auditId).create($auditId, ${model.count()} features)")
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
                                if (it.isNotEmpty()) {
                                    outgoing.add(create(auditId, it))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (col.featureAudit.containsKey(auditId)) {
            val feature = col.featureAudit[auditId]
            feature?.let {
                if (it.isNotEmpty()) {
                    outgoing.add(create(auditId, it))
                }
            }

        }

        var stringRepresentation = ""
        outgoing.forEach { stringRepresentation += it.toString() }
        return outgoing
    } // end build feature

    interface Listener {
        fun onPreExecute()
        fun onPostExecute()
        fun onPostDownload(sync: Syncer)
    }

}
