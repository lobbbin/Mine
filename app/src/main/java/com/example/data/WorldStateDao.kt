package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorldStateDao {
    @Query("SELECT * FROM world_state WHERE id = 0")
    fun getWorldState(): Flow<WorldStateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateWorldState(state: WorldStateEntity)

    @Query("SELECT * FROM laws")
    fun getLaws(): Flow<List<Law>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLaw(law: Law)

    @Delete
    suspend fun removeLaw(law: Law)

    @Query("SELECT * FROM fines WHERE isPaid = 0")
    fun getActiveFines(): Flow<List<Fine>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFine(fine: Fine)

    @Update
    suspend fun updateFine(fine: Fine)
}
