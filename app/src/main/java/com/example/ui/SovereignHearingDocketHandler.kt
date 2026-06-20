package com.example.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HearingStage(
    val index: Int,
    val title: String,
    val subtitle: String,
    val requirementsHint: String,
    val prosecutorTemperament: String,
    val impactTensionPercent: Int
)

object SovereignHearingDocketHandler {

    // --- MALPRACTICE TRIAL HEARING SYSTEM ---
    private val _currentHearingIndex = MutableStateFlow(1) // 1 to 3
    val currentHearingIndex: StateFlow<Int> = _currentHearingIndex.asStateFlow()

    private val _isAppellateAppealActive = MutableStateFlow(false)
    val isAppellateAppealActive: StateFlow<Boolean> = _isAppellateAppealActive.asStateFlow()

    private val _appealsFiledCount = MutableStateFlow(0)
    val appealsFiledCount: StateFlow<Int> = _appealsFiledCount.asStateFlow()

    fun resetDocket() {
        _currentHearingIndex.value = 1
        _isAppellateAppealActive.value = false
    }

    fun advanceHearing(): Boolean {
        if (_isAppellateAppealActive.value) {
            // Appellate is a final one-round hearing
            return false
        }
        if (_currentHearingIndex.value < 3) {
            _currentHearingIndex.value += 1
            return true
        }
        return false
    }

    fun triggerAppellateAppeal(): Boolean {
        _isAppellateAppealActive.value = true
        _appealsFiledCount.value += 1
        return true
    }

    fun getMalpracticeHearingDetails(): HearingStage {
        if (_isAppellateAppealActive.value) {
            return HearingStage(
                index = 4,
                title = "⚖️ HEARING IV: SUPREME APPELLATE REVIEW",
                subtitle = "Constitutional Appeal against prior administrative fines or suspension order before Chief Justice Vance's senior panel.",
                requirementsHint = "Showcase certified clinical excellence: Submit AI-credited professional certificates and high-vitals logs to override the state's prior rulings.",
                prosecutorTemperament = "AGGRESSIVE BUT CAUTIOUS (-20% Defense Advantage)",
                impactTensionPercent = 30
            )
        }

        return when (_currentHearingIndex.value) {
            1 -> HearingStage(
                index = 1,
                title = "📋 HEARING I: PRELIMINARY STATUTORY ARRAIGNMENT",
                subtitle = "Formal reading of clinical indictments in the lower magistracy room. Prosecution lays out basic patient custody violations.",
                requirementsHint = "Focus on establishing basic healthcare compliance. Select appropriate justification statutes you believe justified your initial treatment plans.",
                prosecutorTemperament = "HOSTILE & ACCUSATORY (+10% Tension baseline)",
                impactTensionPercent = 50
            )
            2 -> HearingStage(
                index = 2,
                title = "🔬 HEARING II: FORENSIC CLINICAL EVIDENCE CROSS-EXAMINATION",
                subtitle = "Main evidentiary trial round. Broad clinical peer scrutiny over patient vitals, telemetry logs, skipped lab reports, and medication schedule.",
                requirementsHint = "Submit physical evidence: Attach your recorded patient vitals logs or verified lab reports. Cite standard dosage protocols.",
                prosecutorTemperament = "TECHNICAL & COMPLIANCE-DRIVEN (+15% Aggression baseline)",
                impactTensionPercent = 65
            )
            else -> HearingStage(
                index = 3,
                title = "🏛️ HEARING III: FINAL JURY SUMMATION & SENTENCING DECREE",
                subtitle = "The final standard trial segment. Citizen-Peer jury holds final debates, weighing professional credentials and moral stance.",
                requirementsHint = "Highlight doctor integrity and training: Select and highlight accredited rehabilitation or retraining certificates to win favorable jury votes.",
                prosecutorTemperament = "VINDICATIVE (Awaiting final Decree judgment)",
                impactTensionPercent = 80
            )
        }
    }


    // --- CONSTITUTIONAL STATUTORY CHALLENGE (ON-DEMAND COURT) SYSTEM ---
    private val _onDemandHearingIndex = MutableStateFlow(1) // 1 to 2
    val onDemandHearingIndex: StateFlow<Int> = _onDemandHearingIndex.asStateFlow()

    fun resetOnDemandDocket() {
        _onDemandHearingIndex.value = 1
    }

    fun advanceOnDemandHearing(): Boolean {
        if (_onDemandHearingIndex.value < 2) {
            _onDemandHearingIndex.value += 1
            return true
        }
        return false
    }

    fun getOnDemandHearingDetails(): HearingStage {
        return when (_onDemandHearingIndex.value) {
            1 -> HearingStage(
                index = 1,
                title = "📜 STEP I: EXECUTIVE STANDING BRIEFING",
                subtitle = "Establishing legal standing before the Magistrate Committee by demonstrating severe clinical friction. Prove how this law disrupts clinical liberties.",
                requirementsHint = "Clearly articulate patient risk or resource constraints. This initial plea raises tribunal sympathy and establishes your right to petition the Supreme Bench.",
                prosecutorTemperament = "SKEPTICAL & DISMISSIVE",
                impactTensionPercent = 40
            )
            else -> HearingStage(
                index = 2,
                title = "⚖️ STEP II: SUPREME CLINICAL BENCH ORAL ARGUMENTS",
                subtitle = "The final constitutional showdown before Chief Justice Vance and a 6-person High Constitutional panel. Seeking strike-down decree.",
                requirementsHint = "Bring it home: Present a comprehensive summary. Cite Parliamentary overreach and demand immediate therapeutic drug liberalization.",
                prosecutorTemperament = "INTENSE CONSTITUTIONAL SCRUTINY",
                impactTensionPercent = 70
            )
        }
    }
}
