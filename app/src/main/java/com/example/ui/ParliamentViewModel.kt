package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HealthPolicy
import com.example.data.SettingsDataStore
import com.example.network.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject

class ParliamentViewModel(
    application: Application,
    private val settingsDataStore: SettingsDataStore,
    private val legalWorldAgent: LegalWorldAgent
) : AndroidViewModel(application) {

    // Seating distribution in parliament (total 200 seats)
    private val _progressiveSeats = MutableStateFlow(84)
    val progressiveSeats: StateFlow<Int> = _progressiveSeats.asStateFlow()

    private val _conservativeSeats = MutableStateFlow(76)
    val conservativeSeats: StateFlow<Int> = _conservativeSeats.asStateFlow()

    private val _independentSeats = MutableStateFlow(40)
    val independentSeats: StateFlow<Int> = _independentSeats.asStateFlow()

    // Temporary/transient state variables of legislative gameplay
    private val _currentDraftPolicy = MutableStateFlow<HealthPolicy?>(null)
    val currentDraftPolicy: StateFlow<HealthPolicy?> = _currentDraftPolicy.asStateFlow()

    private val _progressiveLobbyBias = MutableStateFlow(0.0)
    val progressiveLobbyBias: StateFlow<Double> = _progressiveLobbyBias.asStateFlow()

    private val _conservativeLobbyBias = MutableStateFlow(0.0)
    val conservativeLobbyBias: StateFlow<Double> = _conservativeLobbyBias.asStateFlow()

    private val _independentLobbyBias = MutableStateFlow(0.0)
    val independentLobbyBias: StateFlow<Double> = _independentLobbyBias.asStateFlow()

    private val _lastLobbyReport = MutableStateFlow<String?>(null)
    val lastLobbyReport: StateFlow<String?> = _lastLobbyReport.asStateFlow()

    private val _isVotingActive = MutableStateFlow(false)
    val isVotingActive: StateFlow<Boolean> = _isVotingActive.asStateFlow()

    private val _isDebateActive = MutableStateFlow(false)
    val isDebateActive: StateFlow<Boolean> = _isDebateActive.asStateFlow()

    private val _debateLog = MutableStateFlow<String>("")
    val debateLog: StateFlow<String> = _debateLog.asStateFlow()
    
    private val _hasDebated = MutableStateFlow(false)
    val hasDebated: StateFlow<Boolean> = _hasDebated.asStateFlow()

    private val _voteProgress = MutableStateFlow(0f)
    val voteProgress: StateFlow<Float> = _voteProgress.asStateFlow()

    private val _currentVoteYes = MutableStateFlow(0)
    val currentVoteYes: StateFlow<Int> = _currentVoteYes.asStateFlow()

    private val _currentVoteNo = MutableStateFlow(0)
    val currentVoteNo: StateFlow<Int> = _currentVoteNo.asStateFlow()

    private val _currentVoteAbstain = MutableStateFlow(0)
    val currentVoteAbstain: StateFlow<Int> = _currentVoteAbstain.asStateFlow()

    private val _votingLog = MutableStateFlow<List<String>>(emptyList())
    val votingLog: StateFlow<List<String>> = _votingLog.asStateFlow()

    private val _isSickPoliticianNext = MutableStateFlow(false)
    val isSickPoliticianNext: StateFlow<Boolean> = _isSickPoliticianNext.asStateFlow()

    private val _sickPoliticianRole = MutableStateFlow("") // "President", "Senator", "MP"
    val sickPoliticianRole: StateFlow<String> = _sickPoliticianRole.asStateFlow()

    private val _sickPoliticianName = MutableStateFlow("")
    val sickPoliticianName: StateFlow<String> = _sickPoliticianName.asStateFlow()

    private val _sickPoliticianAlert = MutableStateFlow<String?>(null)
    val sickPoliticianAlert: StateFlow<String?> = _sickPoliticianAlert.asStateFlow()

    // Pass-through calls to handle general state updates
    private val _errorFlow = MutableSharedFlow<String>()
    val errorFlow = _errorFlow.asSharedFlow()

    val provider: StateFlow<String> = settingsDataStore.providerFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "Google")
    
    val model: StateFlow<String> = settingsDataStore.modelFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "gemini-3.5-flash")
    
    val apiKey: StateFlow<String?> = settingsDataStore.apiKeyFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    
    val customEndpoint: StateFlow<String> = settingsDataStore.customEndpointFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private val AGENT_POWERS_PROMPT = """
        AGENTIC OVERLORD POWERS (MANDATORY):
        You are the MASTER of the world state variables. Beyond your primary task, if you wish to change variables, enact laws, or notify the world, you MUST include a JSON block in your response containing an "agentActions" array.
        
        JSON SCHEMA FOR ACTIONS:
        { "agentActions": [ { "actionName": "name", "parameters": { ... } } ] }
        
        AVAILABLE ACTIONS:
        - applyFee { "amount": double, "reason": string }
        - enactStatute { "id": string, "name": string, "description": string, "penalty": string }
        - repealStatute { "id": string }
        - updatePrestige { "amount": integer }
        - broadcastNews { "headline": string, "breaking": boolean }

        IMPORTANT LEGISLATIVE RULE:
        You CANNOT enact laws directly. All new legislation MUST be proposed to Parliament for drafting and voting. The President (the user) MUST sign any act passed by Parliament before it becomes active law.
        
        - 55 ADDITIONAL AGENT GAME-SHIFTING ACTIONS:
          triggerEpidemicAlert, adjustPrestige { "amount": int }, adjustReputation { "amount": double }, adjustLobbyInfluence { "faction": string, "change": double } (faction: progressives|conservatives|independents), levyEmergencyTax { "rate": double }, issueClinicalSubsidy { "amount": double }, harnessAIEnergyGrid, overrideNationalFormulary { "name": string, "classification": string, "description": string, "cost": double, "bp": string, "hr": string, "impact": string }, nationalizeFreeHealth, triggerStrikeRisk, resolveStaffDispute, upgradeFacilityTier, leakPrivateCabinetIntel, grantPresidentialPardon, disenfranchiseParty { "party": string }, issueSovereignBonds, simulateMarketInflation, defibrillateNow, perfuseOxygenContinuous, perfuseSalineBolus, injectAdrenalineEmergency, injectAtropineStat, injectAmiodaroneCardiac, injectInsulinDka, injectGlucoseHypo, applyIntubation, applyTourniquet, administerAntibioticWide, administerAnalgesicMorphine, administerNaloxoneOpiate, performEcgSurgical, performCprInterval, triggerLoadSheddingPowerBlackout, forceWaterShortageCrisis, generateSuperbugEncountEvent, hireLocumDoctorAssistant, orderStatTroponin, orderChestXRay, orderCtBrainScan, orderToxicologyPanel, adjustMedicalAidCoverage { "id": string, "coverage": double }, openAuditInvestigation, concludeActiveEncounter, triggerVIPHeartAttackCrisis, injectCardiacGlycoside, administerBronchodilator, administerSedativeTranquilizer, reportWhistleblower, restockSyringesDirect, restockSalineDirect, restockAdrenalineDirect, restockReagentsDirect, restockTherapeuticsDirect, bribeLobbyistBroker, leakPatientRecordsAnonymous
        
        If no systemic actions are necessary, simply OMIT the "agentActions" array.
    """

    private fun resolveActiveApiKey(providerVal: String, userKey: String, customEndVal: String = ""): String {
        val endpoint = customEndVal.ifBlank { customEndpoint.value }
        return if (userKey.isBlank()) {
            if (endpoint.isNotBlank() || providerVal in listOf("Ollama", "vLLM", "G4F (OpenAI-compatible)", "Custom (OpenAI-compatible)")) {
                "dummy-local-key"
            } else {
                ""
            }
        } else {
            userKey
        }
    }

    fun updateSeats(prog: Int, cons: Int, ind: Int) {
        _progressiveSeats.value = prog
        _conservativeSeats.value = cons
        _independentSeats.value = ind
    }

    fun setDraftPolicy(policy: HealthPolicy?) {
        _currentDraftPolicy.value = policy
        _hasDebated.value = false
    }

    fun clearApprovedPolicies() {
        viewModelScope.launch {
            settingsDataStore.saveActivePolicies(emptyList())
            _votingLog.value = listOf("🧹 Active health regulations lists successfully swept.")
        }
    }

    fun createOrUpdateDraftPolicy(
        title: String,
        summary: String,
        clinicalRule: String,
        economicImpact: String,
        clauses: List<String>,
        id: String? = null,
        customEngineDirectives: String = "",
        jurySize: Int = 4,
        maxPleaRounds: Int = 3
    ) {
        val draftId = id ?: java.util.UUID.randomUUID().toString()
        val draft = HealthPolicy(
            id = draftId,
            title = title,
            summary = summary,
            extendedClauses = clauses,
            economicImpact = economicImpact,
            clinicalRule = clinicalRule,
            status = "Draft",
            customEngineDirectives = customEngineDirectives,
            jurySize = jurySize,
            maxPleaRounds = maxPleaRounds
        )
        _currentDraftPolicy.value = draft
        _votingLog.value = listOf("✨ Custom Legislative Bill formulated and loaded in active chamber memory!")
    }

    fun draftAmendment(policy: HealthPolicy) {
        val draft = policy.copy(
            status = "Draft",
            title = if (policy.title.contains("[Amendment]")) policy.title else "${policy.title} [Amendment]"
        )
        _currentDraftPolicy.value = draft
        _votingLog.value = listOf("✏️ Proposing amendment for active statute '${policy.title}'...")
    }

    fun deleteDraftPolicy() {
        _currentDraftPolicy.value = null
    }

    fun queueAIPendingStatute(id: String, name: String, description: String, penalty: String) {
        val draftId = id.ifBlank { java.util.UUID.randomUUID().toString() }
        val draft = HealthPolicy(
            id = draftId,
            title = name,
            summary = description,
            extendedClauses = listOf(description, "Standard Penalty: $penalty"),
            economicImpact = "Proposed Sovereign Statute",
            clinicalRule = penalty,
            status = "PresidentDesk",
            customEngineDirectives = "DM_ENFORCED"
        )
        _currentDraftPolicy.value = draft
        _votingLog.value = _votingLog.value + "🏛️ [ADVISORY COUNCIL DECREE]: A new clinical statute has been drafted and sent directly to your PRESIDENTIAL DESK for executive sign-off! (Bill ID: $draftId)"
    }

    fun triggerPoliticianSickness(presidentName: String) {
        val firstNames = listOf("Representative Sarah", "Senator Marcus", "Representative Julia", "Minister Eleanor", "Speaker Douglas", "President Arthur")
        val lastNames = listOf("Brody", "Sterlings", "Verghese", "Crest", "Hale", "Vance")
        val isFirstPersonPresident = (0..5).random() == 5
        
        val role = if (isFirstPersonPresident) "President" else "Member of Parliament"
        val name = if (isFirstPersonPresident) presidentName else "${firstNames.random()} ${lastNames.random()}"

        val ailments = listOf(
            "crushing retrosternal chest tightness radiating down the arm, signaling a major cardiac infraction (Acute STEMI Myocardial Infarction)",
            "sudden-onset extreme hyperpyrexia of 40.5°C with disorientation and septic status (Sepsis / Pneumonitis)",
            "agonizing epigastric pain with continuous nauseating distress and clinical shock signs (Acute Pancreatitis)",
            "unstable breathing attacks with rapid arterial oxygen levels crashing to 84% (Pulmonary Embolism)"
        )
        val description = ailments.random()

        _sickPoliticianRole.value = role
        _sickPoliticianName.value = name
        _isSickPoliticianNext.value = true
        _sickPoliticianAlert.value = "🚨 EMERGENCY CONTEXT: $role $name has been rushed into severe clinical distress with $description! They demand immediate admission into JB consultation VIP suite."
        
        viewModelScope.launch {
            settingsDataStore.saveStickyPoliticianSick(true)
        }
    }

    fun dismissPoliticianSicknessAlert() {
        _sickPoliticianAlert.value = null
    }

    fun admitPoliticianToClinic(onAdmit: () -> Unit) {
        _isSickPoliticianNext.value = true
        _sickPoliticianAlert.value = null
        onAdmit()
    }

    fun dismissLobbyReport() {
        _lastLobbyReport.value = null
    }

    // Helper functions for API calls, duplicated to avoid cross dependency
    private fun getActiveUrl(provider: String, modelName: String, apiKey: String, customUrl: String): String {
        if (customUrl.isNotBlank()) {
            val base = customUrl.trim()
            return when (provider) {
                "Cerebras", "OpenAI", "Nvidia", "Ollama", "vLLM", "G4F (OpenAI-compatible)", "Custom (OpenAI-compatible)" -> {
                    if (base.contains("chat/completions")) base
                    else if (base.endsWith("/")) "${base}v1/chat/completions"
                    else if (base.endsWith("/v1")) "$base/chat/completions"
                    else "$base/v1/chat/completions"
                }
                "Anthropic" -> {
                    if (base.contains("messages")) base
                    else if (base.endsWith("/")) "${base}v1/messages"
                    else "$base/v1/messages"
                }
                else -> {
                    if (base.contains("generateContent")) {
                        if (base.contains("?key=")) base else "$base?key=$apiKey"
                    } else {
                        val path = "v1beta/models/$modelName:generateContent?key=$apiKey"
                        if (base.endsWith("/")) "$base$path" else "$base/$path"
                    }
                }
            }
        }
        return when (provider) {
            "OpenAI" -> "https://api.openai.com/v1/chat/completions"
            "Cerebras" -> "https://api.cerebras.ai/v1/chat/completions"
            "Nvidia" -> "https://integrate.api.nvidia.com/v1/chat/completions"
            "Anthropic" -> "https://api.anthropic.com/v1/messages"
            "Ollama" -> "http://10.0.2.2:11434/v1/chat/completions"
            "vLLM" -> "http://10.0.2.2:8000/v1/chat/completions"
            "G4F (OpenAI-compatible)" -> "http://10.0.2.2:1337/v1/chat/completions"
            "Custom (OpenAI-compatible)" -> "http://10.0.2.2:8080/v1/chat/completions"
            else -> "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
        }
    }

    private suspend fun makeFreshDirectApiCall(
        provider: String,
        modelName: String,
        apiKey: String,
        systemPrompt: String,
        customUrl: String
    ): String {
        return when (provider) {
            "Cerebras", "OpenAI", "Nvidia", "Ollama", "vLLM", "G4F (OpenAI-compatible)", "Custom (OpenAI-compatible)" -> {
                val activeKey = if (apiKey.isBlank()) "sk-no-key-required" else apiKey
                val messages = listOf(
                    OpenAIMessage("system", "You are a professional legislative text draftsman. Return strictly valid raw JSON matching the requested schema. Write nothing else except valid JSON."),
                    OpenAIMessage("user", "Draft the constitutional health policy bill based on the instructions:\n\n$systemPrompt")
                )
                val isCustomUrl = customUrl.isNotBlank()
                val request = OpenAIRequest(
                    model = modelName,
                    messages = messages,
                    response_format = if (isCustomUrl || provider in listOf("Cerebras", "Nvidia", "Ollama", "vLLM", "G4F (OpenAI-compatible)", "Custom (OpenAI-compatible)")) null else OpenAIResponseFormat("json_object"),
                    temperature = 0.7,
                    stream = false
                )
                val activeUrl = getActiveUrl(provider, modelName, apiKey, customUrl)
                val response = RetrofitClient.service.callOpenAI(
                    url = activeUrl,
                    authorization = "Bearer $activeKey",
                    body = request
                )
                response.choices.firstOrNull()?.message?.content ?: ""
            }
            "Anthropic" -> {
                val activeKey = if (apiKey.isBlank()) "sk-no-key-required" else apiKey
                val messages = listOf(AnthropicMessage("user", "Draft requested details under following instruction: $systemPrompt"))
                val request = AnthropicRequest(
                    model = modelName,
                    system = "You are a professional legislative designer for the country's treasury.",
                    messages = messages,
                    temperature = 0.7
                )
                val activeUrl = getActiveUrl("Anthropic", modelName, apiKey, customUrl)
                val response = RetrofitClient.service.callAnthropic(
                    url = activeUrl,
                    apiKey = activeKey,
                    version = "2023-06-01",
                    body = request
                )
                response.content.firstOrNull()?.text ?: ""
            }
            else -> { // Google Gemini
                val activeKey = if (apiKey.isBlank()) "sk-no-key-required" else apiKey
                val contents = listOf(GeminiContent("user", listOf(GeminiPart("Execute following instruction: $systemPrompt"))))
                val request = GeminiRequest(
                    contents = contents,
                    systemInstruction = GeminiSystemInstruction(listOf(GeminiPart("You are a legislative text draftsman. Return only valid raw JSON."))),
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.7
                    )
                )
                val activeUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$activeKey"
                val response = RetrofitClient.service.callGemini(activeUrl, request)
                response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            }
        }
    }

    private fun extractJsonString(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```")) {
            val lines = clean.split("\n")
            val filtered = lines.filterNot { it.startsWith("```") }
            clean = filtered.joinToString("\n").trim()
        }
        return clean
    }

    private val _currentSeatMap = MutableStateFlow(String(CharArray(200) { 'U' }))
    val currentSeatMap: StateFlow<String> = _currentSeatMap.asStateFlow()

    fun runDebateSession(
        policy: HealthPolicy,
        onDebateFinished: () -> Unit,
        debateRounds: Int = 5
    ) {
        viewModelScope.launch {
            _isDebateActive.value = true
            _debateLog.value = "🗣️ Debate started for: '${policy.title}'\n\n"
            
            val activePolicies = settingsDataStore.activePoliciesFlow.first()
            val activePolicyDirectives = activePolicies.joinToString("\n") { 
                "LAW: ${it.title}. DIRECTIVE: ${it.customEngineDirectives}"
            }
            val debatingPolicyDirectives = if (policy.customEngineDirectives.isNotBlank()) "CURRENT BILL DIRECTIVE: ${policy.customEngineDirectives}" else ""
            
            val prompt = """
                Simulate a parliamentary debate for bill: '${policy.title}'
                Summary: ${policy.summary}
                
                $debatingPolicyDirectives
                
                ACTIVE LAW DIRECTIVES (You MUST follow/respect these for your parliamentary perspective):
                $activePolicyDirectives
                
                Simulate the debate for $debateRounds rounds.
                Participants: Progressives (supporting/opposing based on ideology), Conservatives (supporting/opposing).
                Output a short, punchy summary of the heated exchange.
                Keep it under 3 sentences for each round.
            """.trimIndent()
            
            val currentProvider = provider.value
            val currentModel = model.value
            val userKey = apiKey.value ?: ""
            val activeKey = resolveActiveApiKey(currentProvider, userKey)
            
            val response = makeFreshDirectApiCall(currentProvider, currentModel, activeKey, prompt, customEndpoint.value)
            
            _debateLog.value = "🗣️ Debate finished:\n\n$response"
            _isDebateActive.value = false
            _hasDebated.value = true
            onDebateFinished()
        }
    }
    
    fun runParliamentaryVote(
        policy: HealthPolicy,
        politicalPrestige: Int,
        onVoteFinished: (HealthPolicy, Boolean) -> Unit
    ) {
        val currentProvider = provider.value
        val currentModel = model.value
        val userKey = apiKey.value ?: ""
        val activeKey = resolveActiveApiKey(currentProvider, userKey)

        if (activeKey.isBlank()) {
            _errorFlow.tryEmit("API Key missing! Cannot run AI parliamentary simulation.")
            return
        }

        viewModelScope.launch {
            _isVotingActive.value = true
            _voteProgress.value = 0f
            _currentVoteYes.value = 0
            _currentVoteNo.value = 0
            _currentVoteAbstain.value = 0
            _votingLog.value = listOf(
                "🗳️ Convening plenary session of national parliament...",
                "📰 Bill: '${policy.title}' under urgent constitutional review."
            )
            
            try {
                val reputation = settingsDataStore.reputationStarsFlow.first()
                val balance = settingsDataStore.clinicBalanceFlow.first()
                val activePolicies = settingsDataStore.activePoliciesFlow.first().joinToString { it.title }
                val medicalSchemes = OrchidDeepStateManager.medicalAidSchemes.value.joinToString { "${it.name} (${(it.coveragePercent * 100).toInt()}% coverage)" }

                val prompt = """
                    You are simulating a strategic parliamentary session voting on a major national bio-security healthcare bill:
                    - Title: ${policy.title}
                    - Summary: ${policy.summary}
                    - Direct Enforced Clinical Rule: ${policy.clinicalRule}
                    
                    World State Context:
                    - Proposing Doctor Reputation: $reputation Stars
                    - Doctor Clinic Liquidity: $balance
                    - Current Active Policies: $activePolicies
                    - National Medical Aid Landscape: $medicalSchemes
                    
                    Parliament composition (200 seats total):
                    - Progressives: ${_progressiveSeats.value} seats (Lobby Bias: ${String.format("%.2f", _progressiveLobbyBias.value)})
                    - Conservatives: ${_conservativeSeats.value} seats (Lobby Bias: ${String.format("%.2f", _conservativeLobbyBias.value)})
                    - Independents: ${_independentSeats.value} seats (Lobby Bias: ${String.format("%.2f", _independentLobbyBias.value)})
                    
                    Simulate the debate over 5 intervals. Determine who supports and opposes it realistically based on their philosophy, the doctor's reputation, and the current medical aid landscape.
                    
                    $AGENT_POWERS_PROMPT
                    
                    Provide a JSON response matching this exact schema:
                    {
                      "stages": [
                        { "log": "Description of debate floor action", "yes": 15, "no": 5, "abs": 2, "seatMap": "YYYYYYYYYYYYYYYXXXXXAA______________________________________________________________________________________________________________________________________________________________________________" },
                        { "log": "...", "yes": 45, "no": 30, "abs": 5, "seatMap": "..." },
                        { "log": "...", "yes": 80, "no": 60, "abs": 10, "seatMap": "..." },
                        { "log": "...", "yes": 110, "no": 70, "abs": 12, "seatMap": "..." },
                        { "log": "Final gavel sounds", "yes": 120, "no": 75, "abs": 5, "seatMap": "..." }
                      ],
                      "agentActions": [ ... optional actions ... ]
                    }
                    The seatMap MUST be exactly 200 characters representing the 200 canvas seats:
                    'Y' = Yes, 'X' = No, 'A' = Abstain, '_' = Unvoted.
                    The AI must organically cluster these characters so identical votes sit together like a real parliament block.
                    The last stage represents the final vote count (must sum to exactly 200).
                """.trimIndent()
                
                val apiResponse = makeFreshDirectApiCall(currentProvider, currentModel, activeKey, prompt, customEndpoint.value)
                val sanitized = extractJsonString(apiResponse)
                val json = JSONObject(sanitized)
                
                // Process Agent Actions if present
                if (json.has("agentActions")) {
                    val actions = json.getJSONArray("agentActions")
                    for (j in 0 until actions.length()) {
                        val action = actions.getJSONObject(j)
                        processAgentAction(action)
                    }
                }

                val stagesArray = json.optJSONArray("stages")
                
                if (stagesArray != null && stagesArray.length() > 0) {
                    val steps = stagesArray.length()
                    for (i in 0 until steps) {
                        val stageObj = stagesArray.getJSONObject(i)
                        val logText = stageObj.optString("log", "Debate continues...")
                        val yes = stageObj.optInt("yes", 0)
                        val no = stageObj.optInt("no", 0)
                        val abs = stageObj.optInt("abs", 0)
                        var seatMap = stageObj.optString("seatMap", String(CharArray(200) { '_' }))
                        if (seatMap.length < 200) seatMap = seatMap.padEnd(200, '_')
                        
                        val fraction = (i + 1).toFloat() / steps
                        _voteProgress.value = fraction
                        _currentVoteYes.value = yes
                        _currentVoteNo.value = no
                        _currentVoteAbstain.value = abs
                        _currentSeatMap.value = seatMap.take(200)
                        _votingLog.value = _votingLog.value + "🗣️ $logText"
                        
                        delay(2000)
                    }
                } else {
                    throw Exception("Invalid schema or missing stages in AI vote generation.")
                }
            } catch (e: Exception) {
                // Fallback deterministic if AI fails
                _voteProgress.value = 1f
                _currentVoteYes.value = 110
                _currentVoteNo.value = 80
                _currentVoteAbstain.value = 10
                _currentSeatMap.value = String(CharArray(110) { 'Y' }) + String(CharArray(80) { 'X' }) + String(CharArray(10) { 'A' })
            }

            val finalYes = _currentVoteYes.value
            val finalNo = _currentVoteNo.value
            val finalAbs = _currentVoteAbstain.value
            val passed = finalYes > finalNo

            val updatedDraft = policy.copy(
                status = if (passed) "PresidentDesk" else "Defeated",
                yesVotes = finalYes,
                noVotes = finalNo,
                abstainVotes = finalAbs
            )

            _currentDraftPolicy.value = updatedDraft
            _isVotingActive.value = false

            if (passed) {
                _votingLog.value = _votingLog.value + "🎉 PARLIAMENT HAS PASSED THE ACT ($finalYes YES vs $finalNo NO)! Sent directly to the President's Mansion for sign-off."
                viewModelScope.launch {
                    val currentPrest = settingsDataStore.politicalPrestigeFlow.first()
                    settingsDataStore.savePoliticalPrestige((currentPrest + 8).coerceAtMost(100))
                }
            } else {
                _votingLog.value = _votingLog.value + "❌ THE HEALTH DRAFT WAS DEFEATED IN PARLIAMENT ($finalYes YES vs $finalNo NO). The proposal is rejected."
                viewModelScope.launch {
                    val currentPrest = settingsDataStore.politicalPrestigeFlow.first()
                    settingsDataStore.savePoliticalPrestige((currentPrest - 6).coerceAtLeast(0))
                }
            }
            onVoteFinished(updatedDraft, passed)
        }
    }

    fun presidentialSignDraft(
        onFinished: (HealthPolicy, String) -> Unit
    ) {
        val draft = _currentDraftPolicy.value ?: return
        viewModelScope.launch {
            try {
                val currentProvider = provider.value
                val currentModel = model.value
                val userKey = apiKey.value ?: ""
                val activeKey = resolveActiveApiKey(currentProvider, userKey)
                val presidentName = settingsDataStore.presidentNameFlow.first()
                val presidentParty = settingsDataStore.presidentPartyFlow.first()
                val presidentApproval = settingsDataStore.presidentApprovalFlow.first()

                val prompt = """
                    You are the Executive Head of State, "$presidentName". 
                    Your legislative faction and philosophy is: "$presidentParty".
                    Your current public approval rating is $presidentApproval%.
                    
                    Review this health bill recently passed by Parliament:
                    - Title: ${draft.title}
                    - Summary: ${draft.summary}
                    - Direct Rule: ${draft.clinicalRule}
                    - Presidential Alignment AI Estimate: ${draft.presidentialAlignment ?: "Neutral/Unknown"}
                    
                    Write a short, professional presidential executive memo (max 3 sentences) commenting on your decision to sign this into active clinical law. Start with "I have decided to sign this act..."
                    Take into account your party's philosophy and your current approval rating.
                    
                    You MUST respond strictly with a raw JSON object matching the following structure. Do not output any markdown formatting, codeblocks (```json ... ```), or introductory/concluding text.
                    {
                      "memo": "Your short presidential executive memo comments",
                      "agentActions": [ ... list any actions/commands you want to enact as executive orders ... ]
                    }
                    
                    $AGENT_POWERS_PROMPT
                """.trimIndent()
                
                val apiResponseRaw = if (activeKey.isNotBlank()) {
                    try {
                        makeFreshDirectApiCall(currentProvider, currentModel, activeKey, prompt, customEndpoint.value)
                    } catch (e: Exception) {
                        "I have decided to sign this act to secure the health and safety checks across all private and public practices."
                    }
                } else {
                    "I have decided to sign this act to secure the health and safety checks across all private and public practices."
                }

                var memoText = apiResponseRaw
                try {
                    val sanitized = extractJsonString(apiResponseRaw)
                    if (sanitized.startsWith("{")) {
                        val json = JSONObject(sanitized)
                        memoText = when {
                            json.has("memo") -> json.optString("memo")
                            json.has("presidentialMemo") -> json.optString("presidentialMemo")
                            json.has("comment") -> json.optString("comment")
                            json.has("presidential_memo") -> json.optString("presidential_memo")
                            else -> apiResponseRaw
                        }
                        
                        if (json.has("agentActions")) {
                            val actions = json.getJSONArray("agentActions")
                            for (j in 0 until actions.length()) {
                                val action = actions.getJSONObject(j)
                                processAgentAction(action)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Fallback to raw string if JSON parsing fails
                }

                if (memoText.trim().startsWith("{") && memoText.trim().endsWith("}")) {
                    try {
                        val j = JSONObject(memoText.trim())
                        memoText = j.optString("memo", j.optString("presidentialMemo", j.optString("comment", j.optString("presidential_memo", "I have decided to sign this act."))))
                    } catch (e: Exception) {
                        memoText = "I have decided to sign this act to secure healthcare benefits and standards."
                    }
                }

                val finalPolicy = draft.copy(
                    status = "Approved",
                    summary = "${draft.summary}\n\nPresidential Memorandum: $memoText"
                )

                val currentPolicies = settingsDataStore.activePoliciesFlow.first().toMutableList()
                val idx = currentPolicies.indexOfFirst { it.id == finalPolicy.id }
                if (idx >= 0) {
                    currentPolicies[idx] = finalPolicy
                } else {
                    currentPolicies.add(finalPolicy)
                }
                _currentDraftPolicy.value = finalPolicy
                
                settingsDataStore.saveActivePolicies(currentPolicies)
                
                // Reward player
                val currentBal = settingsDataStore.clinicBalanceFlow.first()
                val currentRep = settingsDataStore.reputationStarsFlow.first()
                val currentPrestige = settingsDataStore.politicalPrestigeFlow.first()
                
                settingsDataStore.updateClinicStats(currentBal + 2000.0, (currentRep + 0.25f).coerceAtMost(5.0f))
                settingsDataStore.savePoliticalPrestige((currentPrestige + 12).coerceAtMost(100))
                settingsDataStore.savePresidentApproval((presidentApproval + 6).coerceAtMost(100))
                
                _votingLog.value = _votingLog.value + "✍️ President signs the legislation! It is now active nationwide clinical law!"
                onFinished(finalPolicy, memoText)
            } catch (e: Exception) {
                // No-op
            }
        }
    }

    fun presidentialVetoDraft(
        onFinished: (HealthPolicy, String) -> Unit
    ) {
        val draft = _currentDraftPolicy.value ?: return
        viewModelScope.launch {
            try {
                val currentProvider = provider.value
                val currentModel = model.value
                val userKey = apiKey.value ?: ""
                val activeKey = resolveActiveApiKey(currentProvider, userKey)
                val presidentName = settingsDataStore.presidentNameFlow.first()
                val presidentParty = settingsDataStore.presidentPartyFlow.first()
                val presidentApproval = settingsDataStore.presidentApprovalFlow.first()

                val prompt = """
                    You are the Executive Head of State, "$presidentName". 
                    Your legislative faction and philosophy is: "$presidentParty".
                    Your current public approval rating is $presidentApproval%.
                    
                    Review this health bill recently passed by Parliament:
                    - Title: ${draft.title}
                    - Summary: ${draft.summary}
                    - Direct Rule: ${draft.clinicalRule}
                    - Presidential Alignment AI Estimate: ${draft.presidentialAlignment ?: "Neutral/Unknown"}
                    
                    Write a short, professional presidential veto memo (max 3 sentences) explaining why you are vetoing this and returning it to Parliament.
                    Take into account your party's philosophy and your current approval rating.
                    
                    You MUST respond strictly with a raw JSON object matching the following structure. Do not output any markdown formatting, codeblocks (```json ... ```), or introductory/concluding text.
                    {
                      "memo": "Your short presidential veto memo comments",
                      "agentActions": [ ... list any actions/commands you want to enact as executive orders ... ]
                    }
                    
                    $AGENT_POWERS_PROMPT
                """.trimIndent()
                
                val apiResponseRaw = if (activeKey.isNotBlank()) {
                    try {
                        makeFreshDirectApiCall(currentProvider, currentModel, activeKey, prompt, customEndpoint.value)
                    } catch (e: Exception) {
                        "I am vetoing this act due to concerns of financial burden on clinics and over-regulating patient-doctor interactions."
                    }
                } else {
                    "I am vetoing this act due to concerns of financial burden on clinics and over-regulating patient-doctor interactions."
                }

                var memoText = apiResponseRaw
                try {
                    val sanitized = extractJsonString(apiResponseRaw)
                    if (sanitized.startsWith("{")) {
                        val json = JSONObject(sanitized)
                        memoText = when {
                            json.has("memo") -> json.optString("memo")
                            json.has("presidentialMemo") -> json.optString("presidentialMemo")
                            json.has("comment") -> json.optString("comment")
                            json.has("presidential_memo") -> json.optString("presidential_memo")
                            else -> apiResponseRaw
                        }
                        
                        if (json.has("agentActions")) {
                            val actions = json.getJSONArray("agentActions")
                            for (j in 0 until actions.length()) {
                                val action = actions.getJSONObject(j)
                                processAgentAction(action)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Fallback to raw string if JSON parsing fails
                }

                if (memoText.trim().startsWith("{") && memoText.trim().endsWith("}")) {
                    try {
                        val j = JSONObject(memoText.trim())
                        memoText = j.optString("memo", j.optString("presidentialMemo", j.optString("comment", j.optString("presidential_memo", "I am vetoing this act."))))
                    } catch (e: Exception) {
                        memoText = "I am vetoing this act due to financial constraints and operational efficiency concerns."
                    }
                }

                val finalPolicy = draft.copy(
                    status = "Vetoed",
                    summary = "${draft.summary}\n\nPresidential Veto Reason:\n$memoText"
                )

                _currentDraftPolicy.value = finalPolicy
                val currentPrestige = settingsDataStore.politicalPrestigeFlow.first()
                settingsDataStore.savePoliticalPrestige((currentPrestige - 10).coerceAtLeast(0))
                settingsDataStore.savePresidentApproval((presidentApproval - 5).coerceAtLeast(10))
                _votingLog.value = _votingLog.value + "🚫 President Arthur Vance vetoed the bill! Sent back to the assembly."
                onFinished(finalPolicy, memoText)
            } catch (e: Exception) {
                // No-op
            }
        }
    }

    fun attemptParliamentOverride(
        politicalPrestige: Int,
        onFinished: (HealthPolicy, Boolean) -> Unit
    ) {
        val draft = _currentDraftPolicy.value ?: return
        viewModelScope.launch {
            if (politicalPrestige < 40) {
                _errorFlow.emit("Overriding a Presidential Veto requires at least 40 Political Prestige!")
                return@launch
            }
            _isVotingActive.value = true
            _voteProgress.value = 0f
            _votingLog.value = listOf(
                "🗳️ Convening emergency joint caucus session...",
                "📋 Standard of law requires 2/3 supermajority (66.6% -- 134 seats) to override veto..."
            )
            delay(1500)

            _voteProgress.value = 0.5f
            val randomFactor = Math.random()
            val overridePassed = (politicalPrestige > 65 && randomFactor < 0.8) || (politicalPrestige >= 40 && randomFactor < 0.5)
            val yesCount = if (overridePassed) (134 + (Math.random() * 20).toInt()) else (110 + (Math.random() * 20).toInt())
            val noCount = 200 - yesCount - 5
            
            _currentVoteYes.value = yesCount
            _currentVoteNo.value = noCount
            _currentVoteAbstain.value = 5
            _voteProgress.value = 1f
            delay(1200)

            val passed = yesCount >= 134
            _isVotingActive.value = false

            if (passed) {
                val finalPolicy = draft.copy(status = "Approved")
                val currentPolicies = settingsDataStore.activePoliciesFlow.first().toMutableList()
                val idx = currentPolicies.indexOfFirst { it.id == finalPolicy.id }
                if (idx >= 0) {
                    currentPolicies[idx] = finalPolicy
                } else {
                    currentPolicies.add(finalPolicy)
                }
                _currentDraftPolicy.value = finalPolicy
                settingsDataStore.saveActivePolicies(currentPolicies)
                
                val currentBal = settingsDataStore.clinicBalanceFlow.first()
                settingsDataStore.updateClinicStats(currentBal + 1500.0, settingsDataStore.reputationStarsFlow.first())
                settingsDataStore.savePoliticalPrestige((politicalPrestige + 15).coerceAtMost(100))
                _votingLog.value = _votingLog.value + "🔥 VETO OVERRIDDEN SUCCESSFULLY! ($yesCount YES vs $noCount NO). The act is immediately enacted as binding supreme law."
                onFinished(finalPolicy, true)
            } else {
                val finalPolicy = draft.copy(status = "Defeated")
                _currentDraftPolicy.value = finalPolicy
                settingsDataStore.savePoliticalPrestige((politicalPrestige - 12).coerceAtLeast(0))
                _votingLog.value = _votingLog.value + "❌ OVERRIDE ATTEMPTED BUT FAILED ($yesCount YES vs $noCount NO). The assembly was unable to mobilize a supermajority. Veto stands."
                onFinished(finalPolicy, false)
            }
        }
    }

    fun AIAutoAmendDraft(
        onFinished: () -> Unit
    ) {
        viewModelScope.launch {
            val draft = _currentDraftPolicy.value ?: return@launch
            try {
                val currentProvider = provider.value
                val currentModel = model.value
                val userKey = apiKey.value ?: ""
                val activeKey = resolveActiveApiKey(currentProvider, userKey)
                
                val activePolList = settingsDataStore.activePoliciesFlow.first()
                val existingPoliciesContext = if (activePolList.isNotEmpty()) {
                    val sb = java.lang.StringBuilder()
                    sb.append("Currently Active National Healthcare Policies (Ensure your amendment aligns with or strategically overrides these):\n")
                    activePolList.forEachIndexed { i, p ->
                        sb.append("${i+1}. ${p.title} - ${p.clinicalRule}. DIRECTIVE: ${p.customEngineDirectives}\n")
                    }
                    sb.toString()
                } else {
                    "No major national healthcare policies are currently active."
                }

                val presidentName = settingsDataStore.presidentNameFlow.first()
                val presidentParty = settingsDataStore.presidentPartyFlow.first()
                val presidentApproval = settingsDataStore.presidentApprovalFlow.first()
                val countryName = settingsDataStore.countryNameFlow.first()

                val prompt = """
                    You are a master political strategist and legislative architect in the country of $countryName.
                    The current Executive Head of State is President $presidentName ($presidentParty).
                    President's Approval Rating: $presidentApproval%.
                    
                    $existingPoliciesContext
                    
                    The current Draft Health Bill is structurally flawed or facing political opposition. 
                    TITLE: ${draft.title}
                    SUMMARY: ${draft.summary}
                    CLINICAL RULE: ${draft.clinicalRule}
                    ECONOMIC IMPACT: ${draft.economicImpact}
                    
                    Your task is to REWRITE and AMEND this draft to maximize public approval and parliamentary success across different political aisles, while retaining the core clinical intent.
                    Output JSON exactly matching this schema:
                    {
                      "title": "A highly refined, formal act title",
                      "summary": "An executive summary highlighting bipartisan compromises and key safety features.",
                      "extendedClauses": [
                        "Clause 1: Specific legal directive...",
                        "Clause 2: Regulatory guideline...",
                        "Clause 3: Financial benefit or protection..."
                      ],
                      "economicImpact": "A reassuring fiscal impact statement.",
                      "clinicalRule": "A concise runtime directive.",
                      "publicSupportEstimate": 85,
                      "politicalOpposition": "Minimized factional resistance",
                      "presidentialAlignment": "Aligned with pragmatic healthcare reform"
                    }
                """.trimIndent()
                
                val apiResponse = makeFreshDirectApiCall(currentProvider, currentModel, activeKey, prompt, customEndpoint.value)
                val sanitized = extractJsonString(apiResponse)
                
                val json = JSONObject(sanitized)
                val newTitle = json.optString("title", draft.title)
                val newSummary = json.optString("summary", draft.summary)
                
                val clausesArray = json.optJSONArray("extendedClauses")
                val newClauses = mutableListOf<String>()
                if (clausesArray != null) {
                    for (i in 0 until clausesArray.length()) {
                        newClauses.add(clausesArray.getString(i))
                    }
                } else {
                    newClauses.addAll(draft.extendedClauses)
                }
                
                val newEconomic = json.optString("economicImpact", draft.economicImpact)
                val newRule = json.optString("clinicalRule", draft.clinicalRule)
                
                val amendedDraft = draft.copy(
                    title = newTitle,
                    summary = newSummary,
                    extendedClauses = newClauses,
                    economicImpact = newEconomic,
                    clinicalRule = newRule,
                    publicSupportEstimate = if (json.has("publicSupportEstimate")) json.optInt("publicSupportEstimate", 70) else draft.publicSupportEstimate,
                    politicalOpposition = if (json.has("politicalOpposition")) json.optString("politicalOpposition") else draft.politicalOpposition,
                    presidentialAlignment = if (json.has("presidentialAlignment")) json.optString("presidentialAlignment") else draft.presidentialAlignment
                )
                
                _currentDraftPolicy.value = amendedDraft
                _votingLog.value = listOf("✨ Draft Healthcare Bill was successfully restructured by the AI Political Strategist!")
            } catch (e: Exception) {
                _errorFlow.emit("Failed to automatically amend bill: ${e.localizedMessage}")
            } finally {
                onFinished()
            }
        }
    }

    fun lobbyFaction(
        faction: String,
        pitchAngle: String,
        customMessage: String,
        clinicBalance: Double,
        politicalPrestige: Int,
        reputationStars: Float,
        onFinished: (Double, Int) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (clinicBalance < 500.0 && politicalPrestige < 5) {
                    _errorFlow.emit("Insufficient funds or political prestige to run a professional lobbying campaign!")
                    onFinished(clinicBalance, politicalPrestige)
                    return@launch
                }
                
                var newPrestige = politicalPrestige
                var newBalance = clinicBalance

                if (politicalPrestige >= 5) {
                    newPrestige -= 5
                    settingsDataStore.savePoliticalPrestige(newPrestige)
                    _votingLog.value = _votingLog.value + "📢 Expended 5 Political Prestige for professional outreach to the $faction."
                } else {
                    newBalance -= 500.0
                    settingsDataStore.updateClinicStats(newBalance, reputationStars)
                    _votingLog.value = _votingLog.value + "💸 Paid $500 in practice consultant fees for outreach dinner with $faction."
                }

                val draft = _currentDraftPolicy.value
                val draftTitle = draft?.title ?: "Proposed Health Reform"
                val draftSummary = draft?.summary ?: "Health Bill"
                val draftPublicSupport = draft?.publicSupportEstimate ?: 50
                val draftPresidentAlignment = draft?.presidentialAlignment ?: "Unknown"

                val activePolList = settingsDataStore.activePoliciesFlow.first()
                val existingPoliciesContext = if (activePolList.isNotEmpty()) {
                    val sb = java.lang.StringBuilder()
                    sb.append("Current Active Policies in The Nation:\n")
                    activePolList.forEach { p -> sb.append("- ${p.title}\n") }
                    sb.toString()
                } else {
                    "No active policies."
                }

                val countryName = settingsDataStore.countryNameFlow.first()

                val prompt = """
                    You are the faction leader spokesperson for the '$faction' bloc in the Parliament of $countryName.
                    The Parliamentary seat distribution is: Progressives (84 seats, safety-focused), Conservatives (76 seats, free market/cost-focused), Independents (40 seats, pragmatic swing votes).
                    
                    $existingPoliciesContext
                    
                    Current Draft Bill Under Consideration:
                    - Title: $draftTitle
                    - Summary: $draftSummary
                    - Est. Public Support: $draftPublicSupport%
                    - Presidential Alignment: $draftPresidentAlignment
                    
                    The clinician (Dr. Tim of JB Consultation Practice, Clinic Balance: ${String.format("%.2f", clinicBalance)}, Prestige: $politicalPrestige, Rep: $reputationStars stars) is lobbying your faction to support this bill!
                    Lobbyist Pitch Angle selected: '$pitchAngle'
                    Lobbyist Custom Written Statement: "$customMessage"
                    
                    Write a spoken, realistic parliamentary response explaining whether your caucus is swayed by this specific pitch.
                    Keep it short, professional, and full of political theme.
                    Then, you MUST output a final JSON block matching this EXACT schema:
                    {
                      "leaderResponse": "The written spoken response from the faction leader.",
                      "isSwayed": true,
                      "biasAdjustment": 0.15
                    }
                """.trimIndent()

                val currentProvider = provider.value
                val currentModel = model.value
                val userKey = apiKey.value ?: ""
                val activeKey = resolveActiveApiKey(currentProvider, userKey)

                if (activeKey.isNotBlank()) {
                    val apiResponse = makeFreshDirectApiCall(currentProvider, currentModel, activeKey, prompt, customEndpoint.value)
                    val sanitized = extractJsonString(apiResponse)
                    
                    val json = JSONObject(sanitized)
                    val leadResp = json.optString("leaderResponse", "We hear your concerns and will deliberate in committee.")
                    val swayed = json.optBoolean("isSwayed", true)
                    val biasAdj = json.optDouble("biasAdjustment", 0.15).coerceIn(0.0, 0.4)

                    val finalBias = if (swayed) biasAdj else -biasAdj / 2.0
                    
                    when (faction) {
                        "Progressives" -> {
                            _progressiveLobbyBias.value = (_progressiveLobbyBias.value + finalBias).coerceIn(-0.3, 0.5)
                        }
                        "Conservatives" -> {
                            _conservativeLobbyBias.value = (_conservativeLobbyBias.value + finalBias).coerceIn(-0.3, 0.5)
                        }
                        "Independents" -> {
                            _independentLobbyBias.value = (_independentLobbyBias.value + finalBias).coerceIn(-0.3, 0.5)
                        }
                    }

                    _lastLobbyReport.value = "🗣️ Faction Spokesperson Response ($faction):\n\"$leadResp\"\n\n📈 Influence Level: ${if (finalBias >= 0) "+" else ""}${String.format("%.0f", finalBias * 100)}% vote probability."
                    _votingLog.value = _votingLog.value + "🗳️ Lobby campaign shifted $faction caucus by ${String.format("%.0f", finalBias * 100)}%!"
                } else {
                    _progressiveLobbyBias.value = 0.20
                    _lastLobbyReport.value = "Democratic caucus members reacted supportively to the pitch! Confidence bias adjusted."
                }
                onFinished(newBalance, newPrestige)
            } catch (e: Exception) {
                // No-op
            }
        }
    }

    fun adjustLobbyBiasDirectly(faction: String, amount: Double) {
        when (faction.lowercase()) {
            "progressives" -> _progressiveLobbyBias.value = (_progressiveLobbyBias.value + amount).coerceIn(-0.3, 0.5)
            "conservatives" -> _conservativeLobbyBias.value = (_conservativeLobbyBias.value + amount).coerceIn(-0.3, 0.5)
            "independents" -> _independentLobbyBias.value = (_independentLobbyBias.value + amount).coerceIn(-0.3, 0.5)
        }
    }

    private fun processAgentAction(action: JSONObject) {
        val name = action.optString("actionName")
        val params = action.optJSONObject("parameters") ?: JSONObject()
        
        viewModelScope.launch {
            when (name) {
                "applyFee" -> {
                    val amount = params.optDouble("amount", 0.0)
                    val reason = params.optString("reason", "Parliamentary fine")
                    val currentBal = settingsDataStore.clinicBalanceFlow.first()
                    settingsDataStore.updateClinicStats(currentBal - amount, settingsDataStore.reputationStarsFlow.first())
                    _votingLog.value = _votingLog.value + "⚠️ ADMINISTRATIVE ACTION: $reason. $amount deducted from clinic."
                }
                "updatePrestige" -> {
                    val amount = params.optInt("amount", 0)
                    val currentPrest = settingsDataStore.politicalPrestigeFlow.first()
                    settingsDataStore.savePoliticalPrestige((currentPrest + amount).coerceIn(0, 100))
                    val direction = if (amount >= 0) "increased" else "decreased"
                    _votingLog.value = _votingLog.value + "📈 Political Prestige $direction by ${Math.abs(amount)}."
                }
                "broadcastNews" -> {
                    val headline = params.optString("headline", "Parliamentary Update")
                    val breaking = params.optBoolean("breaking", false)
                    val prefix = if (breaking) "🚨 BREAKING NEWS:" else "📰 NEWS:"
                    _votingLog.value = _votingLog.value + "$prefix $headline"
                }
                "restructureMedicalAid" -> {
                    val id = params.optString("id", "custom_aid")
                    val schemeName = params.optString("name", "New Scheme")
                    val cov = params.optDouble("coverage", 0.5)
                    val auth = params.optBoolean("preAuth", false)
                    val rej = params.optDouble("rejectionProb", 0.1)
                    OrchidDeepStateManager.updateOrAddMedicalScheme(id, schemeName, cov, auth, rej)
                    _votingLog.value = _votingLog.value + "🛡️ SCHEME REFORM: $schemeName restructured successfully."
                }
                "adjustPrestige" -> {
                    val amount = params.optInt("amount", 0)
                    val currentPrest = settingsDataStore.politicalPrestigeFlow.first()
                    settingsDataStore.savePoliticalPrestige((currentPrest + amount).coerceIn(0, 100))
                    val direction = if (amount >= 0) "increased" else "decreased"
                    _votingLog.value = _votingLog.value + "📈 Political Prestige $direction by ${Math.abs(amount)}."
                }
                "adjustLobbyInfluence" -> {
                    val faction = params.optString("faction", "progressives")
                    val change = params.optDouble("change", 0.05)
                    adjustLobbyBiasDirectly(faction, change)
                    _votingLog.value = _votingLog.value + "🗳️ Adjusted lobby influence of faction $faction by $change."
                }
                "disenfranchiseParty" -> {
                    val party = params.optString("party", "conservatives")
                    adjustLobbyBiasDirectly(party, -0.25)
                    _votingLog.value = _votingLog.value + "🗳️ Party $party disenfranchised."
                }
                "grantPresidentialPardon" -> {
                    val type = params.optString("type", "fine")
                    if (type == "suspension") {
                        val res = legalWorldAgent.pardonSuspension()
                        _votingLog.value = _votingLog.value + res
                    } else {
                        // For simplicity, pardon all active fines if AI triggers a general pardon tool
                        val current = legalWorldAgent.currentSnapshot.value
                        current?.activeFines?.filter { !it.isPaid }?.forEach { fine ->
                            legalWorldAgent.pardonFine(fine)
                        }
                        _votingLog.value = _votingLog.value + "🏛️ PRESIDENTIAL DECREE: National debt amnesty granted. All statutory fines are pardoned."
                    }
                }
            }
        }
    }
}
