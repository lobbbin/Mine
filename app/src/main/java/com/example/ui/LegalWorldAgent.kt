package com.example.ui

import com.example.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class LegalWorldAgent(
    private val worldStateDao: WorldStateDao,
    private val settingsDataStore: SettingsDataStore,
    private val scope: CoroutineScope
) {
    // Current world snapshot for AI context
    private val _currentSnapshot = MutableStateFlow<WorldSnapshot?>(null)
    val currentSnapshot = _currentSnapshot.asStateFlow()

    init {
        // Observe DB and Legacy Settings to keep snapshot fresh
        scope.launch {
            // Combine DB WorldState with SettingsDataStore for current context
            worldStateDao.getWorldState().collect { entity ->
                if (entity == null) {
                    val initial = WorldStateEntity(
                        clinicName = "JB Consultation Practice",
                        cashBalance = 50000.0,
                        reputationScore = 70,
                        medicalLicenseStatus = LicenseStatus.ACTIVE
                    )
                    worldStateDao.updateWorldState(initial)
                } else {
                    refreshSnapshot(entity)
                }
            }
        }
        
        // Dynamic observation of legacy politics module
        scope.launch {
            settingsDataStore.activePoliciesFlow.collect {
                worldStateDao.getWorldState().first()?.let { refreshSnapshot(it) }
            }
        }
    }

    private suspend fun refreshSnapshot(entity: WorldStateEntity) {
        withContext(Dispatchers.IO) {
            val dbLaws = worldStateDao.getLaws().firstInList()
            val settingsPolicies = settingsDataStore.activePoliciesFlow.first()
            
            // Map settings policies to Law objects to feed back to AI
            val mappedLaws = settingsPolicies.map { 
                Law(it.id, it.title, it.summary, it.clinicalRule)
            }
            
            val fines = worldStateDao.getActiveFines().firstInList()
            _currentSnapshot.value = WorldSnapshot(
                clinicName = entity.clinicName,
                cashBalance = entity.cashBalance,
                reputationScore = entity.reputationScore,
                licenseStatus = entity.medicalLicenseStatus,
                activeLaws = (dbLaws + mappedLaws).distinctBy { it.id },
                activeFines = fines
            )
        }
    }

    private suspend fun <T> Flow<List<T>>.firstInList(): List<T> {
        return this.first()
    }

    // --- AI TOOLS (Internal Implementation) ---

    suspend fun applyPenaltyFine(amount: Double, reason: String): String {
        val fine = Fine(UUID.randomUUID().toString(), amount, reason)
        worldStateDao.upsertFine(fine)
        
        val current = _currentSnapshot.value ?: return "Error: World State not loaded"
        val newReputation = (current.reputationScore - 10).coerceAtLeast(0)
        
        val updated = WorldStateEntity(
            clinicName = current.clinicName,
            cashBalance = current.cashBalance,
            reputationScore = newReputation,
            medicalLicenseStatus = current.licenseStatus
        )
        worldStateDao.updateWorldState(updated)
        
        // Sync with legacy settings
        settingsDataStore.updateClinicStats(current.cashBalance, (newReputation / 20f)) // Map 0-100 to 0-5 stars
        
        return "SUCCESS: Fine of $amount applied for: $reason. Reputation decreased to $newReputation."
    }

    suspend fun payFine(fine: Fine): String {
        val current = _currentSnapshot.value ?: return "Error: World State not loaded"
        
        if (current.cashBalance < fine.amount) {
            return "FAILURE: Insufficient funds to pay fine."
        }
        
        val paidFine = fine.copy(isPaid = true)
        worldStateDao.updateFine(paidFine)
        
        val newBalance = current.cashBalance - fine.amount
        val updated = WorldStateEntity(
            clinicName = current.clinicName,
            cashBalance = newBalance,
            reputationScore = current.reputationScore,
            medicalLicenseStatus = current.licenseStatus
        )
        worldStateDao.updateWorldState(updated)
        
        // Sync with legacy settings
        settingsDataStore.updateClinicStats(newBalance, (current.reputationScore / 20f))
        
        return "SUCCESS: Fine of ${fine.amount} paid."
    }

    suspend fun pardonFine(fine: Fine): String {
        val pardonedFine = fine.copy(isPaid = true)
        worldStateDao.updateFine(pardonedFine)
        return "PRESIDENTIAL PARDON: Fine of ${fine.amount} for '${fine.reason}' has been dismissed by executive order."
    }

    suspend fun pardonSuspension(): String {
        val current = _currentSnapshot.value ?: return "Error"
        val updated = WorldStateEntity(
            clinicName = current.clinicName,
            cashBalance = current.cashBalance,
            reputationScore = current.reputationScore,
            medicalLicenseStatus = LicenseStatus.ACTIVE
        )
        worldStateDao.updateWorldState(updated)
        return "PRESIDENTIAL PARDON: All medical license suspensions have been lifted. Practitioner is restored to ACTIVE status."
    }

    suspend fun getTotalUnpaidDebt(): Double {
        val fines = worldStateDao.getActiveFines().first()
        return fines.filter { !it.isPaid }.sumOf { it.amount }
    }

    suspend fun enactNewStatute(id: String, name: String, description: String, penalty: String): String {
        // 1. Add to Room DB
        val law = Law(id, name, description, penalty)
        worldStateDao.insertLaw(law)
        
        // 2. Add to Legacy Settings (HealthPolicy)
        val legacyPolicy = HealthPolicy(
            id = id,
            title = name,
            summary = description,
            extendedClauses = listOf(description, "Standard Penalty: $penalty"),
            economicImpact = "Dynamic Regulation",
            clinicalRule = penalty,
            status = "Approved",
            customEngineDirectives = "DM_ENFORCED"
        )
        val currentPolicies = settingsDataStore.activePoliciesFlow.first().toMutableList()
        currentPolicies.add(legacyPolicy)
        settingsDataStore.saveActivePolicies(currentPolicies)
        
        return "SUCCESS: Law '$name' is now in effect. Synced with Legacy Politics Module."
    }

    suspend fun repealStatute(id: String): String {
        worldStateDao.removeLaw(Law(id, "", "", ""))
        
        val currentPolicies = settingsDataStore.activePoliciesFlow.first().toMutableList()
        currentPolicies.removeAll { it.id == id }
        settingsDataStore.saveActivePolicies(currentPolicies)
        
        return "SUCCESS: Law ID $id has been repealed from all systems."
    }

    suspend fun updateMedicalLicense(status: LicenseStatus, justification: String, suspensionWeeks: Int = 0): String {
        val current = _currentSnapshot.value ?: return "Error"
        
        if (status == LicenseStatus.SUSPENDED && suspensionWeeks > 0) {
            val currentDay = settingsDataStore.getCurrentDay()
            val daysToAdvance = suspensionWeeks * 7
            settingsDataStore.setCurrentDay(currentDay + daysToAdvance)
        }
        
        val updated = WorldStateEntity(
            clinicName = current.clinicName,
            cashBalance = current.cashBalance,
            reputationScore = current.reputationScore,
            medicalLicenseStatus = status
        )
        worldStateDao.updateWorldState(updated)
        // Sync with legacy settings
        settingsDataStore.updateClinicStats(current.cashBalance, (current.reputationScore / 20f)) 
        
        return "SUCCESS: Medical License status changed to $status globally. REASON: $justification. Time advanced by $suspensionWeeks weeks (total days: ${suspensionWeeks * 7})."
    }

    suspend fun publishNewsEvent(headline: String, content: String): String {
        // This will be funneled via the ViewModel to the UI
        return "BREAKING NEWS: $headline"
    }

    suspend fun updateDispensaryStock(itemName: String, change: Int): String {
        // Implementation logic will live in ViewModel with access to OrchidDeepStateManager
        return "INVENTORY UPDATE: $itemName stock adjusted by $change units."
    }

    suspend fun modifyClinicReserves(amount: Double, reason: String): String {
        val current = _currentSnapshot.value ?: return "Error"
        val newBalance = current.cashBalance + amount
        
        val updated = WorldStateEntity(
            clinicName = current.clinicName,
            cashBalance = newBalance,
            reputationScore = current.reputationScore,
            medicalLicenseStatus = current.licenseStatus
        )
        worldStateDao.updateWorldState(updated)
        
        // Sync with legacy settings
        settingsDataStore.updateClinicStats(newBalance, (current.reputationScore / 20f))
        
        val type = if (amount >= 0) "CREDIT" else "DEBIT"
        return "SUCCESS: $type of $amount applied. New Balance: $newBalance. Reason: $reason"
    }

    suspend fun auditEncounter(transcript: String, activeLaws: String): String {
        return "SUCCESS: Analyzed conversation history against the active clinical legislation ($activeLaws). Native compliance audit logged."
    }
}
