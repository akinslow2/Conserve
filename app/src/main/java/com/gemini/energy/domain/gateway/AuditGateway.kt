package com.gemini.energy.domain.gateway

import com.gemini.energy.data.local.model.GraveLocalModel
import com.gemini.energy.domain.entity.*
import io.reactivex.Observable

interface AuditGateway {
    fun getAudit(auditId: Long): Observable<Audit>
    fun getAuditList(): Observable<List<Audit>>
    fun saveAudit(audit: Audit): Observable<Unit>
    fun updateAudit(audit: Audit): Observable<Unit>
    fun deleteAudit(auditId: Long): Observable<Unit>

    fun getZone(zoneId: Long): Observable<Zone>
    fun getZoneList(auditId: Long): Observable<List<Zone>>
    fun saveZone(zone: Zone): Observable<Unit>
    fun updateZone(zone: Zone): Observable<Unit>
    fun deleteZone(zoneId: Long): Observable<Unit>
    fun deleteZoneByAuditId(id: Long): Observable<Unit>

    fun getType(id: Long): Observable<Type>
    fun getTypeList(zoneId: Long, type: String): Observable<List<Type>>
    fun getTypeListByZone(zoneId:Long): Observable<List<Type>>
    fun getTypeByAudit(auditId: Long): Observable<List<Type>>
    fun saveType(auditScope: Type): Observable<Unit>
    fun updateType(auditScope: Type): Observable<Unit>
    fun deleteType(id: Long): Observable<Unit>
    fun deleteTypeByZoneId(id: Long): Observable<Unit>
    fun deleteTypeByAuditId(id: Long): Observable<Unit>

    fun getFeatureByAudit(auditId: Long): Observable<List<Feature>>
    fun getFeatureByType(typeId: Long): Observable<List<Feature>>
    fun saveFeature(feature: List<Feature>): Observable<Unit>
    fun deleteFeature(feature: List<Feature>): Observable<Unit>
    fun deleteFeatureByZoneId(id: Long): Observable<Unit>

    fun getComputable(): Observable<List<Computable<*>>>

    fun saveGraves(grave: GraveLocalModel): Observable<Unit>
    fun updateGraves(oid: Int, usn: Int): Observable<Unit>
    fun deleteGraves(oid: Int): Observable<Unit>

}