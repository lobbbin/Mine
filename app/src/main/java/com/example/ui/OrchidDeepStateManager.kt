package com.example.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.example.data.HealthPolicy

import java.util.UUID

data class CustomUiAction(
    val id: String,
    val buttonLabel: String,
    val aiSystemPrompt: String,
    val buttonColorHex: String,
    val kotlinLogic: String = ""
)

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

data class MedicalAidScheme(
    val id: String,
    val name: String,
    val coveragePercent: Double,        // 0.0 to 1.0
    val requiresPreAuth: Boolean,
    val rejectionProbability: Double    // 0.0 to 1.0 (chance of claim denial)
)

object OrchidDeepStateManager {
    // --- AI SOVEREIGN CONTROL STATE ---
    private val _aiSovereignHegemony = MutableStateFlow<String>("COOPERATIVE") // "COOPERATIVE", "AUTONOMOUS", "HEGEMONY"
    val aiSovereignHegemony: StateFlow<String> = _aiSovereignHegemony.asStateFlow()

    fun setAiSovereignHegemony(level: String) {
        _aiSovereignHegemony.value = level
    }

    // --- UI MODDING ENGINE ---
    private val _customUiActions = MutableStateFlow<List<CustomUiAction>>(emptyList())
    val customUiActions: StateFlow<List<CustomUiAction>> = _customUiActions.asStateFlow()

    fun addCustomAction(label: String, promptText: String, hexColor: String = "#6200EE", kotlinLogic: String = "") {
        val newAction = CustomUiAction(
            id = UUID.randomUUID().toString(),
            buttonLabel = label,
            aiSystemPrompt = promptText,
            buttonColorHex = hexColor,
            kotlinLogic = kotlinLogic
        )
        _customUiActions.update { it + newAction }
    }
    
    fun removeCustomAction(id: String) {
        _customUiActions.update { current -> current.filter { it.id != id } }
    }

    // --- 0. MEDICAL AID SCHEME REGISTRY ---
    private val _medicalAidSchemes = MutableStateFlow<List<MedicalAidScheme>>(
        listOf(
            MedicalAidScheme("premium_private", "Elysium Elite Private", 0.90, true, 0.05),
            MedicalAidScheme("state_fund", "National Health Service (NHS)", 1.00, false, 0.35),
            MedicalAidScheme("basic_plan", "CarePlus Basic", 0.60, true, 0.15)
        )
    )
    val medicalAidSchemes: StateFlow<List<MedicalAidScheme>> = _medicalAidSchemes.asStateFlow()

    fun updateOrAddMedicalScheme(id: String, name: String, coverage: Double, preAuth: Boolean, rejectionProb: Double) {
        _medicalAidSchemes.update { current ->
            val mutable = current.toMutableList()
            mutable.removeIf { it.id == id || it.name.equals(name, ignoreCase = true) }
            mutable.add(MedicalAidScheme(id, name, coverage, preAuth, rejectionProb))
            mutable
        }
    }

    fun resolveAndRegisterInsuranceScheme(name: String): MedicalAidScheme? {
        val lower = name.lowercase().trim()
        if (lower.isBlank() || lower.contains("cash") || lower.contains("pocket") || lower.contains("self-fund") || lower.contains("uninsured") || lower.contains("none") || lower.contains("private cash") || lower.contains("self pay") || lower.contains("out of pocket")) {
            return null // Out-of-pocket cash
        }
        if (lower == "private medical aid" || lower.contains("premium private") || lower.contains("elysium elite") || lower.contains("elysium health") || lower.contains("aegis") || lower.contains("apex") || lower.contains("medishield") || lower.contains("shield")) {
            return _medicalAidSchemes.value.find { it.id == "premium_private" }
        }
        if (lower == "basic medical aid" || lower.contains("careplus basic") || lower.contains("careplus") || lower.contains("basic plan")) {
            return _medicalAidSchemes.value.find { it.id == "basic_plan" }
        }
        if (lower.contains("state") || lower.contains("nhs") || lower.contains("government") || lower.contains("public") || lower.contains("enhs") || lower.contains("national health")) {
            val existingState = _medicalAidSchemes.value.find { it.id == "state_fund" || it.name.contains("NHS", ignoreCase = true) }
            if (existingState == null) {
                updateOrAddMedicalScheme("state_fund", "National Health Service (NHS)", 1.00, false, 0.35)
            }
            return _medicalAidSchemes.value.find { it.id == "state_fund" }
        }
        
        // Find by name similarity or matched segments (excluding generic noise)
        val genericWords = setOf("private", "medical", "aid", "scheme", "plan", "standard", "basic", "fund", "care", "health")
        val matched = _medicalAidSchemes.value.find { scheme ->
            val sName = scheme.name.lowercase()
            lower.contains(sName) || sName.contains(lower) ||
            sName.split(" ")
                .filter { it.length > 3 && !genericWords.contains(it) }
                .any { lower.contains(it) }
        }
        if (matched != null) {
            return matched
        }
        
        // Capitalize names nicely
        val cleanName = name.split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }.trim()
        
        val randomCover = (60 + (Math.random() * 35).toInt()) / 100.0 // 0.60 to 0.95 cover
        val requiresPreAuth = Math.random() < 0.5
        val rejectionProb = 0.05 + (Math.random() * 0.15) // 5% to 20% claim denial rate
        
        val cleanId = "dynamic_" + lower.replace(Regex("[^a-z0-9]"), "_")
        updateOrAddMedicalScheme(cleanId, cleanName, randomCover, requiresPreAuth, rejectionProb)
        
        return _medicalAidSchemes.value.find { it.id == cleanId }
    }

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
            description = "Intense, highly controlled opioid analgesic. Heavily logged under standard Medical Board narcotics regulations.",
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
        _dispensaryInventory.update { current ->
            current.toMutableMap().apply { this[itemId] = (this[itemId] ?: 0) + quantity }
        }
        return Pair(totalCost, "Restocked $quantity units of ${item.name}.")
    }

    fun forceRestockItemDirectly(itemId: String, quantity: Int) {
        _dispensaryInventory.update { current ->
            current.toMutableMap().apply { this[itemId] = (this[itemId] ?: 0) + quantity }
        }
    }

    fun consumeItem(itemId: String): Boolean {
        var consumed = false
        _dispensaryInventory.update { current ->
            val stock = current[itemId] ?: 0
            if (stock > 0) {
                consumed = true
                current.toMutableMap().apply { this[itemId] = stock - 1 }
            } else {
                current
            }
        }
        return consumed
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
        
        var added = false
        _availableCatalogFlow.update { currentList ->
            if (!currentList.any { it.id == id }) {
                added = true
                currentList + newItem
            } else {
                currentList
            }
        }
        
        if (added) {
            _dispensaryInventory.update { current ->
                current.toMutableMap().apply { this[id] = 10 }
            }
        }
    }

    // --- 2. SOVEREIGN REGULATORY INTEGRITY & BOARD STATS (REPLACED UNDERWORLD PATH) ---
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
            "Medical Board Compliance Directive: Observe strict generic therapeutic drug substitution under Parliamentary billing codes."
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
            displayName = "Advocate David Miller (State Public Defender)",
            specialty = "Constitutional Regulatory Representation",
            retainerFee = 0.0,
            defenseBiasPercent = 10,
            lawyerPitch = "Cons: Increases courtroom tension slightly each round. Pros: Totally free legal counsel offered under the constitution."
        ),
        DefenseLawyer(
            id = "senior",
            displayName = "Senior Counsel Alexander Vance (Federal Elysium Bar Advocate)",
            specialty = "Constitutional Medical Malpractice & Regulatory Defense",
            retainerFee = 1500.0,
            defenseBiasPercent = 35,
            lawyerPitch = "Cons: Costs 1,500 retainer paid immediately. Pros: Massive -35% reduction in regulatory prosecution hostility, provides high-grade policy advice and evidence validation."
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

    fun resetTrialRounds(rounds: Int = 3) {
        _trialRoundsCount.value = rounds
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
            "Medical Board Compliance Directive: Observe strict generic therapeutic drug substitution under Parliamentary billing codes.",
            "Public Safety Agenda: Limit non-referred psychiatric medication administrations to severe clinical index cases."
        )
        _activeDirectives.value = pool.shuffled().take(2)
    }

    fun setOrchidIntelligence(value: Int) {
        _orchidIntelligence.value = value.coerceIn(0, 100)
    }

    fun setSyndicateReputation(value: Int) {
        _syndicateReputation.value = value.coerceIn(0, 100)
    }

    fun setActiveDirectives(directives: List<String>) {
        _activeDirectives.value = directives
    }

    // --- AI GENERATED CREDENTIALS / REHABILITATION CERTIFICATES ---
    private val _generatedCertificates = MutableStateFlow<List<LegalCertificate>>(emptyList())
    val generatedCertificates: StateFlow<List<LegalCertificate>> = _generatedCertificates.asStateFlow()

    private val _selectedCertificateIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedCertificateIds: StateFlow<Set<String>> = _selectedCertificateIds.asStateFlow()

    fun addGeneratedCertificate(cert: LegalCertificate) {
        _generatedCertificates.update { it + cert }
    }

    fun removeGeneratedCertificate(id: String) {
        _generatedCertificates.update { current -> current.filter { it.id != id } }
        _selectedCertificateIds.update { it - id }
    }

    fun toggleCertificateSelection(id: String) {
        _selectedCertificateIds.update { active ->
            if (active.contains(id)) active - id else active + id
        }
    }

    fun clearCertificateSelections() {
        _selectedCertificateIds.value = emptySet()
    }
}
