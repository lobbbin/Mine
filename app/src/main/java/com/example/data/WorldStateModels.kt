package com.example.data

import androidx.room.*

enum class LicenseStatus { ACTIVE, PROBATION, SUSPENDED, REVOKED }

@Entity(tableName = "world_state")
data class WorldStateEntity(
    @PrimaryKey val id: Int = 0,
    val clinicName: String,
    val cashBalance: Double,
    val reputationScore: Int, // 0 to 100
    val medicalLicenseStatus: LicenseStatus
)

@Entity(tableName = "laws")
data class Law(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val violationPenalty: String
)

@Entity(tableName = "fines")
data class Fine(
    @PrimaryKey val id: String,
    val amount: Double,
    val reason: String,
    val isPaid: Boolean = false
)

data class WorldSnapshot(
    val clinicName: String,
    val cashBalance: Double,
    val reputationScore: Int,
    val licenseStatus: LicenseStatus,
    val activeLaws: List<Law>,
    val activeFines: List<Fine>
)
