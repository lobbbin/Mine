package com.example.ui

import android.app.Application
import android.util.Log
import com.example.data.SettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AgenicActionHandler(
    private val parentViewModel: SimulationViewModel,
    private val application: Application,
    private val settingsDataStore: SettingsDataStore
) {
    private val _agenicInterventions = MutableStateFlow<List<String>>(listOf(
        "[Backstory] Coordinated a backroom health caucus across progressive and independent legislative delegates to increase chronic drug subsidies.",
        "[Backstory] Approved emergency deployment of sanitary vectors to Municipal Precinct B, halting localized outbreak transmission curves."
    ))
    val agenicInterventions: StateFlow<List<String>> = _agenicInterventions.asStateFlow()

    private fun logAction(action: String) {
        val timeStamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _agenicInterventions.value = _agenicInterventions.value + "[$timeStamp] $action"
    }

    fun publishExternalActionLog(action: String) {
        logAction(action)
    }

    /**
     * Bribes or influences politicians within a target faction.
     * Alters faction bias/polling and deducts from campaign or clinical reserves.
     */
    suspend fun bribeOrInfluencePolitician(
        factionName: String,
        amount: Double,
        purpose: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val politics = parentViewModel.politicsHandler
            val currentFunds = politics.campaignFunds.value
            
            if (currentFunds >= amount) {
                politics.deductCampaignFunds(amount)
            } else {
                val currentClinicBal = settingsDataStore.clinicBalanceFlow.first()
                if (currentClinicBal >= amount) {
                    val rem = currentClinicBal - amount
                    settingsDataStore.updateClinicStats(rem, settingsDataStore.reputationStarsFlow.first())
                } else {
                    return@withContext "FAILED: Insufficient financial reserves (Campaign or Clinical) to execute influence operation."
                }
            }

            // Programmatically alter voter polling or political prestige based on amount of bribe
            val influencePoints = (amount / 1000.0).toInt().coerceIn(1, 10)
            politics.boostVoterPolling(influencePoints)
            politics.boostPrestige(influencePoints / 2)

            val summary = "Executed Backroom Lobbying for $factionName: spent R$amount to leverage support for: '$purpose'. Polling boosted by +$influencePoints%."
            logAction(summary)
            Log.d("AgenicActionHandler", summary)
            
            summary
        } catch (e: Exception) {
            "Error in bribeOrInfluencePolitician: ${e.message}"
        }
    }

    /**
     * Deploys a municipal Sanitary Outbreak Patrol Squad to sanitize districts.
     * Increases inventory items, logs local health improvements, and reduces clinical funds.
     */
    suspend fun deployOutbreakSanitarySquad(
        municipalRegion: String,
        fundsAllocated: Double,
        urgency: Int
    ): String = withContext(Dispatchers.IO) {
        try {
            val currentClinicBal = settingsDataStore.clinicBalanceFlow.first()
            if (currentClinicBal < fundsAllocated) {
                return@withContext "FAILED: Outbreak dispatch failed due to insufficient clinical balance."
            }

            // Deduct funds
            val newBal = currentClinicBal - fundsAllocated
            settingsDataStore.updateClinicStats(newBal, settingsDataStore.reputationStarsFlow.first())

            // Award sanitizing reagents to the dispensary as a logical reward
            val stockAdded = (fundsAllocated / 250.0).toInt().coerceAtLeast(3)
            OrchidDeepStateManager.forceRestockItemDirectly("reagents", stockAdded)

            // Dynamic community reputation boost
            val repDelta = (urgency * 5).coerceIn(5, 15)
            val oldRep = settingsDataStore.reputationStarsFlow.first()
            val newRep = (oldRep + repDelta / 50f).coerceIn(1f, 5f)
            settingsDataStore.updateClinicStats(newBal, newRep)

            val summary = "Dispatched Outbreak Sanitary Squad to '$municipalRegion' with R$fundsAllocated funding (Urgency Level: $urgency). Disinfected vectors; clinical inventory received +$stockAdded sanitizing reagents; reputation improved by ${repDelta / 50f} stars!"
            logAction(summary)
            
            summary
        } catch (e: Exception) {
            "Error in deployOutbreakSanitarySquad: ${e.message}"
        }
    }

    /**
     * Instigates union/industrial strikes in rival hospitals to redirect clinical volume.
     * Boosts local patient intake rates or prestige, shifts public mood, deducts funds.
     */
    suspend fun instigateIndustrialStrike(
        targetHospital: String,
        incitementFund: Double,
        durationDays: Int
    ): String = withContext(Dispatchers.IO) {
        try {
            val politics = parentViewModel.politicsHandler
            val currentFunds = politics.campaignFunds.value
            
            if (currentFunds < incitementFund) {
                return@withContext "FAILED: Insufficient campaign funds to bankroll strike operations."
            }

            politics.deductCampaignFunds(incitementFund)
            
            // Boost clinical reputation and patient count because patients are forced to visit our clinic
            val oldRep = settingsDataStore.reputationStarsFlow.first()
            val newRep = (oldRep + 0.35f).coerceIn(1f, 5f)
            val currentClinicBal = settingsDataStore.clinicBalanceFlow.first()
            settingsDataStore.updateClinicStats(currentClinicBal, newRep)
            
            // Shift public approval rating
            politics.modifyApprovalRating(-4) // Distrust in public sector increases

            val summary = "Bankrolled Syndicate Lockout at '$targetHospital' for $durationDays days using R$incitementFund in incitement funding. Rival patient intakes collapsed; local clinic reputation and traffic spiked!"
            logAction(summary)
            
            summary
        } catch (e: Exception) {
            "Error in instigateIndustrialStrike: ${e.message}"
        }
    }

    /**
     * Declares active quarantine zones to raise client priority and alter supply prices.
     */
    suspend fun triggerNationalQuarantineLevel(
        quarantineZone: String,
        lockdownSeverity: Int,
        scientificJustification: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val politics = parentViewModel.politicsHandler
            
            // Adjust financial modifiers or national compliance based on lockdown
            val pre = parentViewModel.politicalPrestige.value
            val prestigeShift = if (lockdownSeverity > 3) -8 else 5
            politics.boostPrestige(prestigeShift)

            // Scarcity simulation: stock in the dispensary gets slightly depleted due to logistics lockdowns
            OrchidDeepStateManager.forceRestockItemDirectly("saline", -5)
            OrchidDeepStateManager.forceRestockItemDirectly("meds", -3)

            val summary = "Sovereign Decree: Triggered Level-$lockdownSeverity Quarantine Zone at '$quarantineZone'. Reason: $scientificJustification. Prestige shifted by $prestigeShift%; logistics lockdown depleted some saline and meds."
            logAction(summary)
            
            summary
        } catch (e: Exception) {
            "Error in triggerNationalQuarantineLevel: ${e.message}"
        }
    }

    /**
     * Sponsors televised healthcare caucuses. Boosts prestige and XP.
     */
    suspend fun sponsorMedicalCaucus(
        caucusTheme: String,
        marketingCost: Double,
        tvBroadcastTimeMinutes: Int
    ): String = withContext(Dispatchers.IO) {
        try {
            val politics = parentViewModel.politicsHandler
            val currentFunds = politics.campaignFunds.value
            
            if (currentFunds < marketingCost) {
                return@withContext "FAILED: Sponsoring failed due to insufficient campaign cash."
            }

            politics.deductCampaignFunds(marketingCost)
            
            // Boost reputation and award career experience
            val oldRep = settingsDataStore.reputationStarsFlow.first()
            val newRep = (oldRep + 0.5f).coerceIn(1f, 5f)
            val curBal = settingsDataStore.clinicBalanceFlow.first()
            settingsDataStore.updateClinicStats(curBal, newRep)
            
            val xpGain = (tvBroadcastTimeMinutes * 5L).coerceIn(50L, 500L)
            settingsDataStore.addXp(xpGain)

            val summary = "Sponsored Presidential Health Caucus on '$caucusTheme' for $tvBroadcastTimeMinutes min of airtime. Prestige rose; awarded over +$xpGain clinical career XP, clinic rep is at $newRep stars."
            logAction(summary)
            
            summary
        } catch (e: Exception) {
            "Error in sponsorMedicalCaucus: ${e.message}"
        }
    }

    /**
     * Nationalizes pharmaceutical labs to synthesize essential vaccine compounds.
     */
    suspend fun nationalizeVaccineLaboratory(
        labName: String,
        patentControlAction: String,
        compensationFund: Double
    ): String = withContext(Dispatchers.IO) {
        try {
            val curClinicBal = settingsDataStore.clinicBalanceFlow.first()
            if (curClinicBal < compensationFund) {
                return@withContext "FAILED: Nationalization blocked by fiscal treasury audit."
            }

            // Deduct compensation
            val rem = curClinicBal - compensationFund
            settingsDataStore.updateClinicStats(rem, settingsDataStore.reputationStarsFlow.first())

            // Re-stock high value medications
            OrchidDeepStateManager.forceRestockItemDirectly("vaccines", 15)
            OrchidDeepStateManager.forceRestockItemDirectly("adrenaline", 10)

            val summary = "Sovereign Executive Cmd: Nationalized pharmaceutical complex '$labName' under patent order '$patentControlAction'. Compensation paid: R$compensationFund. Received emergency stockpile: 15 vaccines & 10 adrenaline."
            logAction(summary)
            
            summary
        } catch (e: Exception) {
            "Error in nationalizeVaccineLaboratory: ${e.message}"
        }
    }
}
