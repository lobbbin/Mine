package com.example.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.data.HealthPolicy

// --- HIGH FIDELITY DISPENSARY DATA MODEL ---
data class DispensaryItem(
    val id: String,
    val name: String,
    val classification: String, // "Schedule 4 (Standard)", "Schedule 8 (Narcotic)", "Contraband (Rebel)", "General"
    val description: String,
    val purchaseCost: Double,
    val patientBPDelta: String,       // e.g. "Spikes BP (+15)" or "Lowers BP (-10)"
    val patientHRDelta: String,       // e.g. "Spikes HR (+20)"
    val isContraband: Boolean = false,
    val intelligenceSuspicionCost: Int = 0,
    val clinicalTherapyImpact: String
)

// --- INTERACTIVE LAWYER RETENTION MODEL ---
data class DefenseLawyer(
    val id: String,
    val displayName: String,
    val specialty: String,
    val retainerFee: Double,
    val defenseBiasPercent: Int, // Decreases courtroom tension and prosecutor aggression
    val lawyerPitch: String
)

object OrchidDeepStateManager {
    // --- 1. ITEM DISPENSARY STATE ---
    private val _dispensaryInventory = MutableStateFlow<Map<String, Int>>(
        mapOf(
            "antibiotics" to 15,
            "adrenaline" to 8,
            "morphine" to 4,
            "saline" to 10,
            "gtn_spray" to 6,
            "orchid_serum" to 2 // Starts rare!
        )
    )
    val dispensaryInventory: StateFlow<Map<String, Int>> = _dispensaryInventory.asStateFlow()

    val availableCatalog = listOf(
        DispensaryItem(
            id = "saline",
            name = "Isotonic Saline Infusion",
            classification = "General Medical",
            description = "Replenishes plasma volume. Ideal for low blood pressure, severe dehydration, or volume shock.",
            purchaseCost = 120.0,
            patientBPDelta = "Raises (+10 mmHg)",
            patientHRDelta = "Stabilizes (-5 bpm)",
            clinicalTherapyImpact = "Rehydrates"
        ),
        DispensaryItem(
            id = "adrenaline",
            name = "Epinephrine/Adrenaline Shot",
            classification = "Schedule 4 (Emergency)",
            description = "High-potency emergency vasopressor. Drives tachycardia and acute vasoconstriction.",
            purchaseCost = 280.0,
            patientBPDelta = "Spikes (+30 mmHg)",
            patientHRDelta = "Spikes (+35 bpm)",
            clinicalTherapyImpact = "Emergency Resuscitation"
        ),
        DispensaryItem(
            id = "antibiotics",
            name = "Broad-Spectrum Ampicillin",
            classification = "Schedule 4 (Standard)",
            description = "First-line bacterial control. Demands laboratory verification or infection markers before use.",
            purchaseCost = 350.0,
            patientBPDelta = "No immediate effect",
            patientHRDelta = "Stabilizes (-10 bpm)",
            clinicalTherapyImpact = "Anti-Microbial"
        ),
        DispensaryItem(
            id = "gtn_spray",
            name = "Sublingual GTN Vasodilator",
            classification = "Schedule 4 (Standard)",
            description = "Rapidly dilates systemic veins. Relieves cardiac ischemia and angina immediately.",
            purchaseCost = 210.0,
            patientBPDelta = "Drops (-25 mmHg)",
            patientHRDelta = "Reflex Spikes (+12 bpm)",
            clinicalTherapyImpact = "Coronary Relaxation"
        ),
        DispensaryItem(
            id = "morphine",
            name = "Prescribed Morphine Sulphate",
            classification = "Schedule 8 (Heavy Narcotic)",
            description = "Intense, highly addictive opioid analgesic. Heavily regulated under ZAR sovereign narcotics directives.",
            purchaseCost = 450.0,
            patientBPDelta = "Slightly Lowers (-5 mmHg)",
            patientHRDelta = "Dampens (-15 bpm)",
            clinicalTherapyImpact = "Powerful Analgesia & Sedation",
            intelligenceSuspicionCost = 15
        ),
        DispensaryItem(
            id = "orchid_serum",
            name = "Orchid Serum (Contraband CO-99)",
            classification = "CONTRABAND (Orchid Rebel Trade)",
            description = "Underground biosynthetic serum engineered by the rebel Orchid Syndicate. Mysteriously cures multiple etiologies but highly illegal!",
            purchaseCost = 950.0,
            patientBPDelta = "Perfect Balance (Returns to 120/80)",
            patientHRDelta = "Perfect Balance (Returns to 75 bpm)",
            isContraband = true,
            intelligenceSuspicionCost = 30,
            clinicalTherapyImpact = "Biosynthetic Restoration & Panacea"
        )
    )

    fun restockItem(itemId: String, quantity: Int, currentBalance: Double): Pair<Double, String>? {
        val item = availableCatalog.find { it.id == itemId } ?: return null
        val totalCost = item.purchaseCost * quantity
        if (currentBalance < totalCost) {
            return null
        }
        val currentStock = _dispensaryInventory.value.toMutableMap()
        currentStock[itemId] = (currentStock[itemId] ?: 0) + quantity
        _dispensaryInventory.value = currentStock
        return Pair(totalCost, "Restocked $quantity units of ${item.name}.")
    }

    fun consumeItem(itemId: String): Boolean {
        val currentStock = _dispensaryInventory.value.toMutableMap()
        val stock = currentStock[itemId] ?: 0
        if (stock <= 0) return false
        currentStock[itemId] = stock - 1
        _dispensaryInventory.value = currentStock
        return true
    }

    // --- 2. ORCHID DEEP STATE PLOT SYSTEM ---
    private val _isDeepStateEnabled = MutableStateFlow(true)
    val isDeepStateEnabled: StateFlow<Boolean> = _isDeepStateEnabled.asStateFlow()

    private val _orchidIntelligence = MutableStateFlow(20) // 0-100% government suspicion
    val orchidIntelligence: StateFlow<Int> = _orchidIntelligence.asStateFlow()

    private val _syndicateReputation = MutableStateFlow(50) // 0-100% underground alignment
    val syndicateReputation: StateFlow<Int> = _syndicateReputation.asStateFlow()

    private val _activeDirectives = MutableStateFlow<List<String>>(
        listOf(
            "Secret Directive: Discretely dispense 'Orchid Serum (CO-99)' to any destabilized surgical or severe case to gather clinical diagnostic field-data.",
            "Underground Agenda: Keep cumulative prescription billing under R300 for State Funded patients to redirect supplies directly to the Pretoria safehouse.",
            "Rebel Defiance: Intentionally veto or violate any government healthcare regulations that restrict free-practice generic substitution."
        )
    )
    val activeDirectives: StateFlow<List<String>> = _activeDirectives.asStateFlow()

    private val _completedDirectivesCount = MutableStateFlow(0)
    val completedDirectivesCount: StateFlow<Int> = _completedDirectivesCount.asStateFlow()

    private val _currentCaseDispensationHistory = MutableStateFlow<List<String>>(emptyList())
    val currentCaseDispensationHistory: StateFlow<List<String>> = _currentCaseDispensationHistory.asStateFlow()

    fun resetCaseDispensation() {
        _currentCaseDispensationHistory.value = emptyList()
    }

    fun recordDispensation(itemId: String) {
        val item = availableCatalog.find { it.id == itemId } ?: return
        val current = _currentCaseDispensationHistory.value.toMutableList()
        current.add(item.name)
        _currentCaseDispensationHistory.value = current

        // Update suspicion & rebel standing
        if (item.isContraband) {
            _orchidIntelligence.value = (_orchidIntelligence.value + item.intelligenceSuspicionCost).coerceIn(0, 100)
            _syndicateReputation.value = (_syndicateReputation.value + 15).coerceIn(0, 100)
        } else if (item.classification.contains("Schedule 8")) {
            _orchidIntelligence.value = (_orchidIntelligence.value + 8).coerceIn(0, 100)
        }
    }

    fun completeDirective() {
        _completedDirectivesCount.value = _completedDirectivesCount.value + 1
        _syndicateReputation.value = (_syndicateReputation.value + 20).coerceIn(0, 100)
        // Set new directives
        val current = _activeDirectives.value.toMutableList()
        if (current.isNotEmpty()) {
            current.removeAt(0)
            current.add("Dynamic Alert: Evade the National Intelligence Search Warrant by avoiding all illegal chemical compound trials for the next 4 patient cases.")
            _activeDirectives.value = current
        }
    }
    
    fun bribeSSSAForCoverage(cost: Double): Boolean {
        if (_orchidIntelligence.value <= 10) return false
        _orchidIntelligence.value = (_orchidIntelligence.value - 25).coerceAtLeast(0)
        return true
    }

    // --- 3. COURTROOM INTERACTIVE OVERHAUL STATE ---
    private val _hiredLawyer = MutableStateFlow<DefenseLawyer?>(null)
    val hiredLawyer: StateFlow<DefenseLawyer?> = _hiredLawyer.asStateFlow()

    val defenseLawyersCatalog = listOf(
        DefenseLawyer(
            id = "public",
            displayName = "Adv. Sipho Khumalo (State Public Defender)",
            specialty = "Pro-Bono / Human Rights Protection",
            retainerFee = 0.0,
            defenseBiasPercent = 10,
            lawyerPitch = "Cons: Increases courtroom tension slightly each round. Pros: Totally free legal counsel offered under the constitution."
        ),
        DefenseLawyer(
            id = "senior",
            displayName = "Senior Counsel Gerhard de Klerk (Pretoria Bar Advocate)",
            specialty = "Constitutional Medical Malpractice & Regulatory Defense",
            retainerFee = 1500.0,
            defenseBiasPercent = 35,
            lawyerPitch = "Cons: Costs R1,500 retainer paid immediately. Pros: Massive -35% reduction in prosecutor hostility, provides high-grade policy advice and evidence validation."
        )
    )

    private val _trialRoundsCount = MutableStateFlow(3) // Users get 3 active defense plea rounds before the final verdict
    val trialRoundsCount: StateFlow<Int> = _trialRoundsCount.asStateFlow()

    private val _defensePleaHistory = MutableStateFlow<List<String>>(emptyList())
    val defensePleaHistory: StateFlow<List<String>> = _defensePleaHistory.asStateFlow()

    private val _potentialEvidencePool = MutableStateFlow<List<String>>(emptyList())
    val potentialEvidencePool: StateFlow<List<String>> = _potentialEvidencePool.asStateFlow()

    private val _selectedEvidenceToPresent = MutableStateFlow<List<String>>(emptyList())
    val selectedEvidenceToPresent: StateFlow<List<String>> = _selectedEvidenceToPresent.asStateFlow()

    fun hireDefenseLawyer(lawyerId: String): Boolean {
        val lawyer = defenseLawyersCatalog.find { it.id == lawyerId } ?: return false
        _hiredLawyer.value = lawyer
        return true
    }

    fun resetTrialRounds() {
        _trialRoundsCount.value = 3
        _defensePleaHistory.value = emptyList()
        _selectedEvidenceToPresent.value = emptyList()
        _hiredLawyer.value = null
    }

    fun spendTrialRound() {
        _trialRoundsCount.value = (_trialRoundsCount.value - 1).coerceAtLeast(0)
    }

    fun recordDefensePleaArgument(plea: String) {
        val current = _defensePleaHistory.value.toMutableList()
        current.add(plea)
        _defensePleaHistory.value = current
    }

    fun setEvidencePool(vitals: String, labs: String?, policyVio: String) {
        val list = mutableListOf<String>()
        list.add("📊 Record of Clinical Patient Vitals: $vitals")
        if (!labs.isNullOrBlank()) {
            list.add("🔬 Verified Diagnostic Laboratory Data Report")
        } else {
            list.add("⚠️ Notice: Withheld or Skipped Lab Diagnostics")
        }
        if (policyVio.isNotBlank()) {
            list.add("📜 National Compliance Audit File: $policyVio")
        }
        list.add("📋 General Practice Treatment Book Entry (PR# 1234567)")
        _potentialEvidencePool.value = list
    }

    fun toggleEvidenceSelection(evidence: String) {
        val current = _selectedEvidenceToPresent.value.toMutableList()
        if (current.contains(evidence)) {
            current.remove(evidence)
        } else {
            current.add(evidence)
        }
        _selectedEvidenceToPresent.value = current
    }

    fun leakIntelToSyndicate() {
        _orchidIntelligence.value = (_orchidIntelligence.value + 10).coerceIn(0, 100)
    }

    fun requestNewDirective() {
        val pool = listOf(
            "Secret Directive: Discretely dispense 'Orchid Serum (CO-99)' to any destabilized cases to gather clinical diagnostic field-data.",
            "Underground Agenda: Keep cumulative prescription billing under R300 for State Funded patients to redirect supplies directly to the safehouse.",
            "Rebel Defiance: Intentionally veto or violate any government healthcare regulations.",
            "Sovereign Disruption: Dispense Adrenaline Epi-Shots to patients presenting with extreme hypotensive crisis.",
            "Integrity Sabotage: Under-report heavy narcotics control logs by administering Morphine."
        )
        _activeDirectives.value = pool.shuffled().take(2)
    }
}
