package com.gemini.energy.data.gateway

import com.gemini.energy.data.gateway.mapper.SystemMapper
import com.gemini.energy.data.local.model.GraveLocalModel
import com.gemini.energy.data.repository.*
import com.gemini.energy.domain.entity.*
import com.gemini.energy.domain.gateway.AuditGateway
import io.reactivex.Observable

class AuditGatewayImpl(
        private val auditRepository: AuditRepository,
        private val zoneRepository: ZoneRepository,
        private val typeRepository: TypeRepository,
        private val featureRepository: FeatureRepository,
        private val computableRepository: ComputableRepository,
        private val gravesRepository: GravesRepository) : AuditGateway {

    private val mapper = SystemMapper()

    /*Audit*/
    override fun getAudit(auditId: Long): Observable<Audit> = auditRepository.get(auditId).map { mapper.toEntity(it) }
    override fun getAuditList(): Observable<List<Audit>> =
            auditRepository.getAll()
                    .doOnError { println("Audit Get Error") }
                    .map { it.map { mapper.toEntity(it) } }

    override fun saveAudit(audit: Audit): Observable<Unit> = auditRepository.save(audit)
    override fun updateAudit(audit: Audit): Observable<Unit> = auditRepository.update(audit)
    override fun deleteAudit(auditId: Long): Observable<Unit> = auditRepository.delete(auditId)

    /*Zone*/
    override fun getZone(zoneId: Long): Observable<Zone> = zoneRepository.get(zoneId).map { mapper.toEntity(it) }
    override fun getZoneList(auditId: Long): Observable<List<Zone>> =
            zoneRepository.getAllByAudit(auditId)
                    .map { it.map { mapper.toEntity(it) } }

    override fun saveZone(zone: Zone): Observable<Unit> = zoneRepository.save(zone)
    override fun updateZone(zone: Zone): Observable<Unit> = zoneRepository.update(zone)
    override fun deleteZone(zoneId: Long): Observable<Unit> = zoneRepository.delete(zoneId)
    override fun deleteZoneByAuditId(id: Long): Observable<Unit> = zoneRepository.deleteByAuditId(id)

    /*Type*/
    override fun getType(id: Long): Observable<Type> =
            typeRepository.get(id).map { mapper.toEntity(it) }

    override fun getTypeList(zoneId: Long, type: String): Observable<List<Type>> =
            typeRepository.getAllTypeByZone(zoneId, type)
                    .map { it.map { mapper.toEntity(it) } }

    override fun getTypeByAudit(auditId: Long): Observable<List<Type>> =
        typeRepository.getAllTypeByAudit(auditId)
                .map { it.map { mapper.toEntity(it) } }

    override fun saveType(auditScope: Type) = typeRepository.save(auditScope)
    override fun updateType(auditScope: Type): Observable<Unit> = typeRepository.update(auditScope)
    override fun deleteType(id: Long): Observable<Unit> = typeRepository.delete(id)
    override fun deleteTypeByZoneId(id: Long): Observable<Unit> = typeRepository.deleteByZoneId(id)
    override fun deleteTypeByAuditId(id: Long): Observable<Unit> = typeRepository.deleteByAuditId(id)

    /*Feature*/
    override fun getFeatureByAudit(auditId: Long): Observable<List<Feature>> =
            featureRepository.getAllByAudit(auditId)
                    .map { it.map { mapper.toEntity(it) } }

    override fun getFeatureByType(typeId: Long): Observable<List<Feature>> =
            featureRepository.getAllByType(typeId)
                    .map { it.map { mapper.toEntity(it) } }

    override fun saveFeature(feature: List<Feature>): Observable<Unit> = featureRepository.save(feature)
    override fun deleteFeature(feature: List<Feature>): Observable<Unit> =
            featureRepository.delete(feature)
    override fun deleteFeatureByZoneId(id: Long): Observable<Unit> = featureRepository.deleteByZoneId(id)

    /*Computable*/
    override fun getComputable(): Observable<List<Computable<*>>> =
        computableRepository.getAllComputable()
                .map { it.map { mapper.toEntity(it) } }

    /*Graves*/
    override fun saveGraves(grave: GraveLocalModel) = gravesRepository.save(grave)
    override fun updateGraves(oid: Int, usn: Int) = gravesRepository.update(oid, usn)
    override fun deleteGraves(oid: Int) = gravesRepository.delete(oid)

}