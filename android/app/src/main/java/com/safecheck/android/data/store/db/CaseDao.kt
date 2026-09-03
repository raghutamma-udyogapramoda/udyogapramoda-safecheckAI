package com.safecheck.android.data.store.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CaseEntity)

    @Query("SELECT * FROM cases WHERE caseId = :caseId LIMIT 1")
    suspend fun get(caseId: String): CaseEntity?

    @Query("SELECT * FROM cases ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<CaseEntity>>

    @Query("DELETE FROM cases")
    suspend fun deleteAll()

    @Query("DELETE FROM cases WHERE caseId = :caseId")
    suspend fun delete(caseId: String)
}
