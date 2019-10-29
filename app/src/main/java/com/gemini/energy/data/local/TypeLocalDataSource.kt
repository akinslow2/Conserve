package com.gemini.energy.data.local

import com.gemini.energy.data.local.dao.TypeDao
import com.gemini.energy.data.local.model.TypeLocalModel
import io.reactivex.Observable

class TypeLocalDataSource(
        private val auditZoneTypeDao: TypeDao) {

    fun get(id: Long): Observable<TypeLocalModel> =
            auditZoneTypeDao.get(id).toObservable()

    fun getAllTypeByZone(id: Long, type: String): Observable<List<TypeLocalModel>> =
            auditZoneTypeDao.getAllTypeByZone(id, type).toObservable()

    fun getAllTypeByZone(id: Long): Observable<List<TypeLocalModel>> =
            auditZoneTypeDao.getAllTypeByZone(id).toObservable()

    fun getAllTypeByAudit(id: Long): Observable<List<TypeLocalModel>> =
            auditZoneTypeDao.getAllTypeByAudit(id).toObservable()

    fun save(auditZoneType: TypeLocalModel): Observable<Unit> = Observable.fromCallable {
        auditZoneTypeDao.insert(auditZoneType)
    }

    fun update(type: TypeLocalModel): Observable<Unit> = Observable.fromCallable {
        auditZoneTypeDao.update(type)
    }

    fun delete(id: Long): Observable<Unit> = Observable.fromCallable {
        auditZoneTypeDao.delete(id)
    }

    fun deleteByZoneId(id: Long): Observable<Unit> = Observable.fromCallable {
        auditZoneTypeDao.deleteByZoneId(id)
    }

    fun deleteByAuditId(id: Long): Observable<Unit> = Observable.fromCallable {
        auditZoneTypeDao.deleteByAuditId(id)
    }

}