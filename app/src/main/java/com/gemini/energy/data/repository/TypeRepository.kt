package com.gemini.energy.data.repository

import com.gemini.energy.data.local.TypeLocalDataSource
import com.gemini.energy.data.local.model.TypeLocalModel
import com.gemini.energy.data.remote.TypeRemoteDataSource
import com.gemini.energy.data.repository.mapper.TypeMapper
import com.gemini.energy.domain.entity.Type
import io.reactivex.Observable

class TypeRepository(
        private val typeLocalDataSource: TypeLocalDataSource,
        private val typeRemoteDataSource: TypeRemoteDataSource,
        private val auditScopeMapper: TypeMapper) {

    fun get(id: Long): Observable<TypeLocalModel> = typeLocalDataSource.get(id)
    fun getAllTypeByZone(id: Long, type: String): Observable<List<TypeLocalModel>> =
            typeLocalDataSource.getAllTypeByZone(id, type)
    fun getAllTypeByAudit(id: Long): Observable<List<TypeLocalModel>> =
            typeLocalDataSource.getAllTypeByAudit(id)

    fun save(auditScope: Type): Observable<Unit> =
            typeLocalDataSource.save(auditScopeMapper.toLocal(auditScope))

    fun update(type: Type): Observable<Unit> = typeLocalDataSource.update(auditScopeMapper.toLocal(type))
    fun delete(id: Long): Observable<Unit> = typeLocalDataSource.delete(id)
    fun deleteByZoneId(id: Long): Observable<Unit> = typeLocalDataSource.deleteByZoneId(id)
    fun deleteByAuditId(id: Long): Observable<Unit> = typeLocalDataSource.deleteByAuditId(id)

}