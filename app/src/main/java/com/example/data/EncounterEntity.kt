package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "encounters")
data class EncounterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val specialty: String,
    val chiefComplaint: String,
    val trueDiagnosis: String,
    val pathophysiology: String,
    val expectedLabs: String,
    val severity: String,
    val insuranceStatus: String = "Private Medical Aid",
    
    // Live fields of simulation state
    val currentPhase: String,
    val vitals: Vitals? = null,
    val chatHistory: List<ChatMessage> = emptyList(),
    val labResults: String? = null,
    val physicalExamResults: String? = null,
    val billingReceipt: String? = null,
    val evaluation: String? = null,
    val isEncounterComplete: Boolean = false,
    val revenueEarned: Double = 0.0,
    val expensesIncurred: Double = 0.0,
    val virtualTimeElapsed: Int = 0,
    val patientMood: String = "Neutral",
    val patientStability: String = "Stable",
    val ddxNotes: String = "",
    val patientDemographics: String = "Unknown Patient",
    val prescriptionString: String? = null,
    val referralLetterString: String? = null,
    val sickNoteString: String? = null,
    val paymentCollected: Boolean = false,
    val billingApprovedByHuman: Boolean = false,
    val patientOutcome: String = "Recovered",
    val submittedDiagnosis: String = "",
    val submittedTreatmentPlan: String = "",
    val intakeFormData: IntakeFormData? = null
)
