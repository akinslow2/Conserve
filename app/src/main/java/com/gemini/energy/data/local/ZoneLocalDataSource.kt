package com.gemini.energy.data.local

import com.gemini.energy.data.local.dao.ZoneDao
import com.gemini.energy.data.local.model.ZoneLocalModel
import io.reactivex.Observable

class ZoneLocalDataSource(private val zoneDao: ZoneDao) {

    fun get(id: Long): Observable<ZoneLocalModel> = zoneDao.get(id).toObservable()
    fun getAllByAudit(id: Long): Observable<List<ZoneLocalModel>> = zoneDao.getAllByAudit(id).toObservable()

    fun save(zone: ZoneLocalModel): Observable<Unit> = Observable.fromCallable {
        zoneDao.insert(zone)
    }

    fun update(zone: ZoneLocalModel): Observable<Unit> = Observable.fromCallable { zoneDao.update(zone) }
    fun delete(id: Long): Observable<Unit> = Observable.fromCallable { zoneDao.delete(id) }

    fun deleteByAuditId(id: Long): Observable<Unit> = Observable.fromCallable {
        zoneDao.deleteByAuditId(id)
    }

}