package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Vitals(
    val bp: String,
    val hr: String,
    val tempC: Double,
    val rr: String,
    val spo2: String
)

@JsonClass(generateAdapter = true)
data class ChatMessage(
    val role: String, // "patient" or "doctor" or "system"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val virtualTimestampStr: String? = null
)

@JsonClass(generateAdapter = true)
data class SimulationState(
    val currentPhase: String = "Phase 1 - Presentation",
    val vitals: Vitals? = null,
    val chatHistory: List<ChatMessage> = emptyList(),
    val labResults: String? = null,
    val physicalExamResults: String? = null,
    val billingReceipt: String? = null,
    val evaluation: String? = null,
    val isEncounterComplete: Boolean = false,
    val dailyRevenue: Double = 0.0,
    val patientsSeen: Int = 0,
    val expensesIncurred: Double = 0.0,
    val patientDemographics: String = "Unknown Patient",
    val virtualTimeElapsed: Int = 0,
    val patientMood: String = "Neutral",
    val patientStability: String = "Stable",
    val ddxNotes: String = "",
    val prescriptionString: String? = null,
    val referralLetterString: String? = null,
    val sickNoteString: String? = null,
    val paymentCollected: Boolean = false,
    val billingApprovedByHuman: Boolean = false,
    val patientOutcome: String = "Recovered",
    val submittedDiagnosis: String = "",
    val submittedTreatmentPlan: String = ""
)

@JsonClass(generateAdapter = true)
data class HiddenCaseProfile(
    val specialty: String,
    val chiefComplaint: String,
    val trueDiagnosis: String,
    val pathophysiology: String,
    val expectedLabs: String,
    val severity: String, // "Routine" or "Severe"
    val insuranceStatus: String = "Private Medical Aid", // "Uninsured", "State Funded", "Private Medical Aid"
    val patientDemographics: String = "Unknown Patient"
)

@JsonClass(generateAdapter = true)
data class GeneratedCaseWrapper(
    val specialty: String,
    val chiefComplaint: String,
    val trueDiagnosis: String,
    val pathophysiology: String,
    val expectedLabs: String,
    val severity: String,
    val initialVitals: Vitals,
    val insuranceStatus: String = "Private Medical Aid",
    val patientDemographics: String = "Unknown Patient"
)

@JsonClass(generateAdapter = true)
data class AIResponseStateUpdate(
    val dialogueResponse: String?,
    val vitals: Vitals? = null,
    val currentPhase: String? = null,
    val labResults: String? = null,
    val physicalExamResults: String? = null,
    val billingReceipt: String? = null,
    val evaluation: String? = null,
    val isEncounterComplete: Boolean? = null,
    val additionalExpenses: Double? = null,
    val clinicalScore: Double? = null,
    val patientMood: String? = null,
    val patientStability: String? = null,
    val prescriptionString: String? = null,
    val referralLetterString: String? = null,
    val sickNoteString: String? = null
)

@JsonClass(generateAdapter = true)
data class LawsuitResponse(
    val courtDialogue: String,
    val tensionAdjustment: Int,
    val aggressionAdjustment: Int,
    val judgmentStageReached: Boolean,
    val verdictType: String, // "Exonerated", "Warning", "Suspension", "Fined"
    val fineAmount: Double,
    val suspensionWeeks: Int,
    val finalVerdictText: String
)

@JsonClass(generateAdapter = true)
data class HealthPolicy(
    val id: String,
    val title: String,
    val summary: String,
    val extendedClauses: List<String> = emptyList(),
    val economicImpact: String,
    val clinicalRule: String,
    val status: String, // "Draft", "Voting", "PresidentDesk", "Approved", "Vetoed", "Defeated"
    val yesVotes: Int = 0,
    val noVotes: Int = 0,
    val abstainVotes: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)



