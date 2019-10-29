package com.gemini.energy.data.local.dao

import android.arch.persistence.room.*
import com.gemini.energy.data.local.model.TypeLocalModel
import io.reactivex.Maybe
import java.lang.reflect.Type

@Dao
interface TypeDao {

    @Query("SELECT * FROM AuditZoneType WHERE id = :id")
    fun get(id: Long): Maybe<TypeLocalModel>

    @Query("SELECT * FROM AuditZoneType WHERE zone_id = :id AND type = :type")
    fun getAllTypeByZone(id: Long, type: String): Maybe<List<TypeLocalModel>>

    @Query("SELECT * FROM AuditZoneType WHERE zone_id = :id")
    fun getAllTypeByZone(id: Long): Maybe<List<TypeLocalModel>>

    @Query("SELECT * FROM AuditZoneType WHERE audit_id = :id")
    fun getAllTypeByAudit(id: Long): Maybe<List<TypeLocalModel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(auditScope: TypeLocalModel)

    @Update
    fun update(type: TypeLocalModel)

    @Query("DELETE FROM AuditZoneType WHERE id = :id")
    fun delete(id: Long)

    @Query("DELETE FROM AuditZoneType WHERE zone_id = :id")
    fun deleteByZoneId(id: Long)

    @Query("DELETE FROM AuditZoneType WHERE audit_id = :id")
    fun deleteByAuditId(id: Long)

}