package com.gemini.energy.data.local.dao

import android.arch.persistence.room.*
import com.gemini.energy.data.local.model.FeatureLocalModel
import com.gemini.energy.domain.entity.Feature
import io.reactivex.Maybe

@Dao
interface FeatureDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(feature: List<FeatureLocalModel>)

    @Query("SELECT * FROM feature WHERE audit_id = :id")
    fun getAllByAudit(id: Long): Maybe<List<FeatureLocalModel>>

    @Query("SELECT * FROM feature WHERE type_id = :id ORDER BY form_id")
    fun getAllByType(id: Long): Maybe<List<FeatureLocalModel>>

    @Delete
    fun deleteByType(feature: List<FeatureLocalModel>)

    @Query("DELETE FROM feature WHERE type_id = :id")
    fun deleteByTypeId(id: Long)

    @Query("DELETE FROM feature WHERE audit_id = :id")
    fun deleteByAuditId(id: Long)

    @Query("DELETE FROM feature WHERE zone_id = :id")
    fun deleteByZoneId(id: Long)

}