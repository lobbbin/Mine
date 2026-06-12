package com.example.data

import kotlinx.coroutines.flow.Flow

class EncounterRepository(private val encounterDao: EncounterDao) {
    val allEncountersFlow: Flow<List<EncounterEntity>> = encounterDao.getAllEncountersFlow()

    suspend fun getAllEncounters(): List<EncounterEntity> = encounterDao.getAllEncounters()

    suspend fun getLatestEncounter(): EncounterEntity? = encounterDao.getLatestEncounter()

    suspend fun insertOrUpdate(encounter: EncounterEntity): Long = encounterDao.insertOrUpdate(encounter)

    suspend fun deleteAll() = encounterDao.deleteAll()

    suspend fun getTotalRevenue(): Double = encounterDao.getTotalRevenue() ?: 0.0

    suspend fun getCompletedCount(): Int = encounterDao.getCompletedCount() ?: 0

    suspend fun deleteEncounterById(id: Long) = encounterDao.deleteEncounterById(id)

    suspend fun deleteEncountersByDemographics(demographics: String) = encounterDao.deleteEncountersByDemographics(demographics)
}
