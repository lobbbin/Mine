package com.example.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.data.HealthPolicy

// --- HIGH FIDELITY DISPENSARY DATA MODEL ---
data class DispensaryItem(
    val id: String,
    val name: String,
    val classification: String, // "Schedule 4 (Standard)", "Schedule 8 (Narcotic)", "General"
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
            "saline" to 10,
            "adrenaline" to 8,
            "antibiotics" to 15,
            "gtn_spray" to 6,
            "morphine" to 4,
            "prozac" to 10 // Starts with Prozac by default so it's fully supported!
        )
    )
    val dispensaryInventory: StateFlow<Map<String, Int>> = _dispensaryInventory.asStateFlow()

    private val defaultCatalog = listOf(
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
            description = "Intense, highly controlled opioid analgesic. Heavily logged under standard HPCSA narcotics regulations.",
            purchaseCost = 450.0,
            patientBPDelta = "Slightly Lowers (-5 mmHg)",
            patientHRDelta = "Dampens (-15 bpm)",
            clinicalTherapyImpact = "Powerful Analgesia & Sedation",
            intelligenceSuspicionCost = 5 // Represents minor inspection cost, not underworld
        ),
        DispensaryItem(
            id = "prozac",
            name = "Prozac Antidepressant Tablets",
            classification = "Schedule 5 (Psychiatric)",
            description = "Selective Serotonin Reuptake Inhibitor (SSRI). Standard therapy for depressive mood, panic disorders, and obsessive symptoms.",
            purchaseCost = 180.0,
            patientBPDelta = "No acute effect",
            patientHRDelta = "Steady (0 bpm)",
            clinicalTherapyImpact = "Stabilizes Serotonin & Long-Term Mood Regulation"
        )
    )

    private val _availableCatalogFlow = MutableStateFlow<List<DispensaryItem>>(defaultCatalog)
    val availableCatalogFlow: StateFlow<List<DispensaryItem>> = _availableCatalogFlow.asStateFlow()

    // Getter compat for static list access
    val availableCatalog: List<DispensaryItem>
        get() = _availableCatalogFlow.value

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

    fun forceRestockItemDirectly(itemId: String, quantity: Int) {
        val currentStock = _dispensaryInventory.value.toMutableMap()
        currentStock[itemId] = (currentStock[itemId] ?: 0) + quantity
        _dispensaryInventory.value = currentStock
    }

    fun consumeItem(itemId: String): Boolean {
        val currentStock = _dispensaryInventory.value.toMutableMap()
        val stock = currentStock[itemId] ?: 0
        if (stock <= 0) return false
        currentStock[itemId] = stock - 1
        _dispensaryInventory.value = currentStock
        return true
    }

    // --- dynamic custom drug additions ---
    fun addNewCustomItem(
        name: String,
        classification: String,
        description: String,
        purchaseCost: Double,
        bpDelta: String,
        hrDelta: String,
        clinicalImpact: String
    ) {
        val cleanName = name.trim()
        val id = cleanName.lowercase().replace(Regex("[^a-z0-9_]"), "_").take(24)
        val newItem = DispensaryItem(
            id = id,
            name = cleanName,
            classification = classification,
            description = description,
            purchaseCost = purchaseCost,
            patientBPDelta = bpDelta,
            patientHRDelta = hrDelta,
            clinicalTherapyImpact = clinicalImpact
        )
        val currentList = _availableCatalogFlow.value.toMutableList()
        if (!currentList.any { it.id == id }) {
            currentList.add(newItem)
            _availableCatalogFlow.value = currentList

            // Also register starting stock so the clinician can immediately test/dispense it
            val updatedInventory = _dispensaryInventory.value.toMutableMap()
            updatedInventory[id] = 10
            _dispensaryInventory.value = updatedInventory
        }
    }

    // --- 2. SOVEREIGN REGULATORY INTEGRITY & HPCSA STATS (REPLACED UNDERWORLD PATH) ---
    private val _isDeepStateEnabled = MutableStateFlow(true)
    val isDeepStateEnabled: StateFlow<Boolean> = _isDeepStateEnabled.asStateFlow()

    private val _isFreeHealthEnabled = MutableStateFlow(false)
    val isFreeHealthEnabled: StateFlow<Boolean> = _isFreeHealthEnabled.asStateFlow()

    fun toggleFreeHealth(enabled: Boolean) {
        _isFreeHealthEnabled.value = enabled
    }

    // Serves as Regulatory Compliance Audit Score (higher is better, represents compliance integrity status)
    private val _orchidIntelligence = MutableStateFlow(95) 
    val orchidIntelligence: StateFlow<Int> = _orchidIntelligence.asStateFlow()

    // Serves as Sovereign Law Enforcement standing
    private val _syndicateReputation = MutableStateFlow(85)
    val syndicateReputation: StateFlow<Int> = _syndicateReputation.asStateFlow()

    private val _activeDirectives = MutableStateFlow<List<String>>(
        listOf(
            "Regulatory Advisory: Ensure standard diagnostic vitals screenings exist for all out-of-pocket cash consults.",
            "Policy Guideline: Keep daily clinical expenditure balanced and avoid unnecessary high-schedule prescriptions.",
            "HPCSA Compliance Directive: Observe strict generic therapeutic drug substitution under Parliamentary billing codes."
        )
    )
    val activeDirectives: StateFlow<List<String>> = _activeDirectives.asStateFlow()

    private val _completedDirectivesCount = MutableStateFlow(1)
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

        // Standard audits update regulatory stats
        if (item.classification.contains("Schedule 8", ignoreCase = true)) {
            // High narcotics usage slightly alerts regulatory inspection
            _orchidIntelligence.value = (_orchidIntelligence.value - 5).coerceIn(0, 100)
        }
    }

    fun completeDirective() {
        _completedDirectivesCount.value = _completedDirectivesCount.value + 1
        _syndicateReputation.value = (_syndicateReputation.value + 10).coerceIn(0, 100)
    }
    
    fun bribeSSSAForCoverage(cost: Double): Boolean {
        // Renamed function behaves as: "Request Regulatory Counsel Review"
        if (_orchidIntelligence.value >= 95) return false
        _orchidIntelligence.value = (_orchidIntelligence.value + 15).coerceIn(0, 100)
        return true
    }

    // --- 3. COURTROOM INTERACTIVE OVERHAUL STATE ---
    private val _hiredLawyer = MutableStateFlow<DefenseLawyer?>(null)
    val hiredLawyer: StateFlow<DefenseLawyer?> = _hiredLawyer.asStateFlow()

    val defenseLawyersCatalog = listOf(
        DefenseLawyer(
            id = "public",
            displayName = "Adv. Sipho Khumalo (State Public Defender)",
            specialty = "Constitutional Regulatory Representation",
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
            lawyerPitch = "Cons: Costs R1,500 retainer paid immediately. Pros: Massive -35% reduction in regulatory prosecution hostility, provides high-grade policy advice and evidence validation."
        )
    )

    private val _trialRoundsCount = MutableStateFlow(3)
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
        // Becomes "Consult Parliamentary Lobbyists"
        _orchidIntelligence.value = (_orchidIntelligence.value - 5).coerceIn(0, 100)
    }

    fun requestNewDirective() {
        val pool = listOf(
            "Regulatory Advisory: Ensure standard diagnostic vitals screenings exist for all out-of-pocket cash consults.",
            "Policy Guideline: Keep daily clinical expenditure balanced and avoid unnecessary high-schedule prescriptions.",
            "HPCSA Compliance Directive: Observe strict generic therapeutic drug substitution under Parliamentary billing codes.",
            "Public Safety Agenda: Limit non-referred psychiatric medication administrations to severe clinical index cases."
        )
        _activeDirectives.value = pool.shuffled().take(2)
    }
}
