package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EncounterDao {
    @Query("SELECT * FROM encounters ORDER BY timestamp DESC")
    fun getAllEncountersFlow(): Flow<List<EncounterEntity>>

    @Query("SELECT * FROM encounters ORDER BY timestamp DESC")
    suspend fun getAllEncounters(): List<EncounterEntity>

    @Query("SELECT * FROM encounters ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestEncounter(): EncounterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(encounter: EncounterEntity): Long

    @Query("DELETE FROM encounters")
    suspend fun deleteAll()

    @Query("SELECT SUM(revenueEarned) FROM encounters WHERE isEncounterComplete = 1")
    suspend fun getTotalRevenue(): Double?

    @Query("SELECT COUNT(*) FROM encounters WHERE isEncounterComplete = 1")
    suspend fun getCompletedCount(): Int?

    @Query("DELETE FROM encounters WHERE id = :id")
    suspend fun deleteEncounterById(id: Long)

    @Query("DELETE FROM encounters WHERE patientDemographics = :demographics")
    suspend fun deleteEncountersByDemographics(demographics: String)
}
