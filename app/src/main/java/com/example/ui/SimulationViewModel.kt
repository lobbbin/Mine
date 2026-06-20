package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AIResponseStateUpdate
import com.example.data.AppDatabase
import com.example.data.ChatMessage
import com.example.data.EncounterEntity
import com.example.data.EncounterRepository
import com.example.data.GeneratedCaseWrapper
import com.example.data.HealthPolicy
import com.example.data.HiddenCaseProfile
import com.example.data.IntakeFormData
import com.example.data.SettingsDataStore
import com.example.data.SimulationState
import com.example.data.Vitals
import com.example.network.AIService
import com.example.network.AnthropicMessage
import com.example.network.AnthropicRequest
import com.example.network.GeminiContent
import com.example.network.GeminiGenerationConfig
import com.example.network.GeminiPart
import com.example.network.GeminiRequest
import com.example.network.GeminiSystemInstruction
import com.example.network.GeminiTool
import com.example.network.GeminiFunctionDeclaration
import com.example.network.GeminiFunctionCall
import org.json.JSONObject
import com.example.network.OpenAIMessage
import com.example.network.OpenAIRequest
import com.example.network.OpenAIResponseFormat
import com.example.network.RetrofitClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

data class SovereignNoticeData(
    val headline: String,
    val message: String,
    val severity: String
)

class SimulationViewModel(application: Application) : AndroidViewModel(application) {

    private val appDatabase = AppDatabase.getDatabase(application)
    private val encounterRepository = EncounterRepository(appDatabase.encounterDao())
    private val settingsDataStore = SettingsDataStore(application)
    val aiMemoryManager = com.example.data.AIMemoryManager(
        appDatabase.agentMemoryDao(),
        appDatabase.worldStateDao(),
        settingsDataStore
    )
    private val legalWorldAgent = LegalWorldAgent(appDatabase.worldStateDao(), settingsDataStore, viewModelScope)
    val parliamentViewModel = ParliamentViewModel(application, settingsDataStore, legalWorldAgent)
    val courtroomViewModel = CourtroomViewModel(application, settingsDataStore)

    val worldSnapshot = legalWorldAgent.currentSnapshot
    
    private val _isStatutoryBlockadeActive = MutableStateFlow(false)
    val isStatutoryBlockadeActive: StateFlow<Boolean> = _isStatutoryBlockadeActive.asStateFlow()

    init {
        // Monitor debt for blockade
        viewModelScope.launch {
            while (true) {
                val debt = legalWorldAgent.getTotalUnpaidDebt()
                _isStatutoryBlockadeActive.value = debt > 10000.0
                delay(10000) // check every 10s
            }
        }
    }

    val allEncounters = encounterRepository.allEncountersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var activeEncounterId: Long = 0L
    private var lastLawsuitEncounterId: Long = 0L
    private var pastClinicalHistoryPrompt: String = ""

    val gameAgent = GameAgent(executeToolCall = { name, args -> executeToolCallFromAgent(name, args) })

    fun resolveActiveApiKey(providerVal: String, userKey: String, customEndVal: String = ""): String {
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

    val apiKey: StateFlow<String?> = settingsDataStore.apiKeyFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val provider: StateFlow<String> = settingsDataStore.providerFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "Google")

    val model: StateFlow<String> = settingsDataStore.modelFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "gemini-3.5-flash")

    val customEndpoint: StateFlow<String> = settingsDataStore.customEndpointFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val rotatorKeys: StateFlow<Map<String, String>> = settingsDataStore.rotatorKeysFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val rotatorEnabledModels: StateFlow<Set<String>> = settingsDataStore.rotatorEnabledModelsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val rotatorCustomModels: StateFlow<Map<String, List<String>>> = settingsDataStore.rotatorCustomModelsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val clinicBalance: StateFlow<Double> = settingsDataStore.clinicBalanceFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 50000.0)

    val uiFontScale: StateFlow<Float> = settingsDataStore.uiFontScaleFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)

    val currencySymbol: StateFlow<String> = settingsDataStore.currencySymbolFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "$")

    val currencyCode: StateFlow<String> = settingsDataStore.currencyCodeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "USD")

    private val _g4fModels = MutableStateFlow<List<String>>(emptyList())
    val g4fModels: StateFlow<List<String>> = _g4fModels.asStateFlow()

    init {
        refreshG4FModels()
    }

    fun refreshG4FModels() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder()
                    .url("https://raw.githubusercontent.com/Free-AI-Things/g4f-working/main/working/working_results.txt")
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyText = response.body?.string() ?: ""
                    val models = bodyText.lines()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() && it.contains("|") }
                        .map {
                            val parts = it.split("|")
                            if (parts.size >= 2) "${parts[0]}.${parts[1]}" else it
                        }
                        .distinct()
                    if (models.isNotEmpty()) {
                        _g4fModels.value = models
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val reputationStars: StateFlow<Float> = settingsDataStore.reputationStarsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 3.5f)

    val preferredSpecialty: StateFlow<String> = settingsDataStore.prefSpecialtyFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "All")

    val preferredSeverity: StateFlow<String> = settingsDataStore.prefSeverityFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "All")

    val consultationFee: StateFlow<Double> = settingsDataStore.consultationFeeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 850.0)

    val labCost: StateFlow<Double> = settingsDataStore.labCostFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 150.0)

    val specialistCost: StateFlow<Double> = settingsDataStore.specialistCostFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 800.0)

    val syringeStock: StateFlow<Int> = settingsDataStore.inventorySyringesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 42)

    val salineStock: StateFlow<Int> = settingsDataStore.inventorySalineFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 8)

    val adrenalineStock: StateFlow<Int> = settingsDataStore.inventoryAdrenalineFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 5)

    val reagentsStock: StateFlow<Int> = settingsDataStore.inventoryReagentsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 25)

    val medsStock: StateFlow<Int> = settingsDataStore.inventoryMedsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 12)

    val currentDay: StateFlow<Int> = settingsDataStore.currentDayFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1)

    val patientsSeenToday: StateFlow<Int> = settingsDataStore.patientsSeenTodayFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val dailyRevenueLive: StateFlow<Double> = settingsDataStore.dailyRevenueFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val dailyExpensesLive: StateFlow<Double> = settingsDataStore.dailyExpensesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val doctorXp: StateFlow<Long> = settingsDataStore.doctorXpFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val doctorRank: StateFlow<String> = doctorXp.map { xp: Long ->
        when {
            xp < 500L -> "Intern 🩺"
            xp < 1500L -> "Medical Officer 🏥"
            xp < 4000L -> "Registrar 🎓"
            xp < 10000L -> "Consultant 👨‍⚕️"
            else -> "Chief Surgeon 👑"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Intern 🩺")

    val isBasicMode: StateFlow<Boolean> = settingsDataStore.isBasicModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val hasChosenMode: StateFlow<Boolean> = settingsDataStore.hasChosenModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val agentMemories: StateFlow<List<com.example.data.AgentMemory>> = appDatabase.agentMemoryDao().getAllMemories()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun saveModeSelection(isBasic: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveModeSelection(isBasic)
            // Trigger a clean case generation or restart if requested
            if (isBasic) {
                // In basic mode we prefer General Practice
                settingsDataStore.saveCurriculumPresets("General Practice", "All")
            }
        }
    }

    fun saveUiFontScale(scale: Float) {
        viewModelScope.launch {
            settingsDataStore.saveUiFontScale(scale)
        }
    }

    fun saveCurriculumPresets(specialty: String, severity: String) {
        viewModelScope.launch {
            settingsDataStore.saveCurriculumPresets(specialty, severity)
        }
    }

    fun savePricing(consultFee: Double, labCost: Double, specCost: Double) {
        viewModelScope.launch {
            settingsDataStore.savePricing(consultFee, labCost, specCost)
        }
    }

    fun setClinicBalance(balance: Double) {
        viewModelScope.launch {
            settingsDataStore.updateClinicStats(balance, reputationStars.value)
            val current = legalWorldAgent.currentSnapshot.value
            if (current != null) {
                appDatabase.worldStateDao().updateWorldState(
                    com.example.data.WorldStateEntity(
                        clinicName = current.clinicName,
                        cashBalance = balance,
                        reputationScore = (reputationStars.value * 20).toInt().coerceIn(0, 100),
                        medicalLicenseStatus = current.licenseStatus
                    )
                )
            }
        }
    }

    fun saveCurrency(symbol: String, code: String) {
        viewModelScope.launch {
            settingsDataStore.saveCurrency(symbol, code)
        }
    }

    fun payFine(fine: com.example.data.Fine) {
        viewModelScope.launch {
            val result = legalWorldAgent.payFine(fine)
            println(result)
        }
    }

    fun petitionForPardon(fine: com.example.data.Fine? = null, isSuspension: Boolean = false) {
        viewModelScope.launch {
            val selectedIds = OrchidDeepStateManager.selectedCertificateIds.value
            val allCerts = OrchidDeepStateManager.generatedCertificates.value
            val attachedCerts = allCerts.filter { selectedIds.contains(it.id) }
            val attachedStr = if (attachedCerts.isNotEmpty()) {
                "\n\nFORMALLY ATTACHED CLINICAL REHABILITATION CERTIFICATES / EVIDENCE:\n" +
                        attachedCerts.joinToString("\n") { cert ->
                            "- ${cert.title} (Registry: ${cert.registrationNumber}). Issued by: ${cert.issuer}. Criteria Met: ${cert.verificationDetails}"
                        }
            } else ""

            if (isSuspension) {
                sendMessage("PRESIDENTIAL PETITION: I am formally requesting an executive pardon for my clinical license suspension. I believe my practice serves the greater good and I pledge full statutory compliance moving forward.$attachedStr\n\n[CMD: request executive pardon for suspension]")
            } else if (fine != null) {
                sendMessage("PRESIDENTIAL PETITION: I am formally requesting an executive pardon for the fine of ${fine.amount} regarding '${fine.reason}'. My clinical record and contribution to national health should be considered.$attachedStr\n\n[CMD: request executive pardon for fine ID ${fine.id}]")
            }
        }
    }

    // --- INTUITIVE PRESIDENTIAL AUDIENCE & PARDON TOOL ---
    private val _pardonTriesRemaining = MutableStateFlow(8)
    val pardonTriesRemaining: StateFlow<Int> = _pardonTriesRemaining.asStateFlow()

    private val _presidentMood = MutableStateFlow("Skeptical") // Skeptical, Hostile, Pragmatic, Benevolent, Amused
    val presidentMood: StateFlow<String> = _presidentMood.asStateFlow()

    private val _presidentResponseText = MutableStateFlow("Welcome Dr. Tim. I hold absolute sovereign discretion over your license and outstanding fines. State your plea, or present your formal rehabilitation certificates. I will render my judgment.")
    val presidentResponseText: StateFlow<String> = _presidentResponseText.asStateFlow()

    private val _pardonGrantedState = MutableStateFlow(false)
    val pardonGrantedState: StateFlow<Boolean> = _pardonGrantedState.asStateFlow()

    private val _pardonAudienceTerminated = MutableStateFlow(false)
    val pardonAudienceTerminated: StateFlow<Boolean> = _pardonAudienceTerminated.asStateFlow()

    private val _pardonHistory = MutableStateFlow<List<String>>(emptyList())
    val pardonHistory: StateFlow<List<String>> = _pardonHistory.asStateFlow()

    fun resetPresidentialAudience() {
        _pardonTriesRemaining.value = 8
        _presidentMood.value = listOf("Skeptical", "Hostile", "Pragmatic", "Benevolent", "Amused").shuffled().first()
        _presidentResponseText.value = "I am listening, Dr. Tim. Make your case now. You have 8 audiences remaining before I withdraw from this clinic chambers entirely."
        _pardonGrantedState.value = false
        _pardonAudienceTerminated.value = false
        _pardonHistory.value = emptyList()
    }

    fun submitPresidentialPlea(pleaText: String) {
        val remaining = _pardonTriesRemaining.value
        if (remaining <= 0 || _pardonGrantedState.value || _pardonAudienceTerminated.value) return

        _pardonTriesRemaining.value = remaining - 1
        val updatedHistory = _pardonHistory.value + "Dr. Tim: $pleaText"
        _pardonHistory.value = updatedHistory

        viewModelScope.launch {
            setLoading(true)
            try {
                // Determine attached evidence
                val selectedIds = OrchidDeepStateManager.selectedCertificateIds.value
                val allCerts = OrchidDeepStateManager.generatedCertificates.value
                val attachedCerts = allCerts.filter { selectedIds.contains(it.id) }
                val attachedStr = if (attachedCerts.isNotEmpty()) {
                    "\n\n[USER SUBMITTED PROOF ATTACHMENTS]:\n" +
                            attachedCerts.joinToString("\n") { cert ->
                                "- ${cert.title} issued by ${cert.issuer}. Registered: ${cert.registrationNumber}. Details: ${cert.verificationDetails}. Score attached: ${cert.testScores ?: "N/A"}"
                            }
                } else "\n\n[USER ATTACHED NO CERTIFICATES]"

                val currentProvider = provider.value
                val currentModel = model.value
                val userKey = apiKey.value ?: ""
                val activeKey = resolveActiveApiKey(currentProvider, userKey)

                val prompt = """
                    You are simulating an interactive executive appeal where the user (Dr. Tim) defends his medical clinical performance, ethics, or finances directly to the President of their country (${presidentName.value}), striving to receive a license reinstatement or fine waiving.
                    
                    President Name: ${presidentName.value}
                    President Party: ${presidentParty.value}
                    Nation: ${countryName.value}
                    Current President Mood: ${_presidentMood.value}
                    Remaining appeal attempts in this audience: ${remaining - 1}
                    
                    User's Plea Input: "$pleaText"
                    $attachedStr
                    
                    TASK:
                    Analyze the user's plea. If they attached high-scoring certificates (e.g., Psyche Eval or Ethics Course showing high test scores) or made an exceptionally moving, respectful, or patriotic argument that aligns with the President's party perspective, you may decide to GRANT the executive pardon.
                    
                    Return a JSON object with the following fields:
                    - "reply": The President's dynamic dialogue spoken directly to Dr. Tim (max 3 sentences). Keep the tone fitting their current mood.
                    - "nextMood": The next mood of the President (choose from: "Skeptical", "Hostile", "Pragmatic", "Benevolent", "Amused")
                    - "pardonGranted": boolean (Set to true ONLY if you are fully satisfied with their argument or their attached credentials. If they attached a valid high-score certificate or presented an incredible defense, reward them!)
                    - "terminated": boolean (Set to true if you are completely insulted, angry, or if the remaining tries are 0, which ends the audience permanently)
                    
                    Return ONLY raw JSON, do not wrap in markdown or any conversational filler.
                    Format:
                    {
                      "reply": "string",
                      "nextMood": "string",
                      "pardonGranted": boolean,
                      "terminated": boolean
                    }
                """.trimIndent()

                val responseRaw = makeFreshDirectApiCall(currentProvider, currentModel, activeKey, prompt)
                val sanitized = extractJsonString(responseRaw)
                val json = org.json.JSONObject(sanitized)

                val reply = json.optString("reply", "I have heard enough. Present better arguments or I shall leave.")
                val nextMood = json.optString("nextMood", _presidentMood.value)
                val granted = json.optBoolean("pardonGranted", false)
                val isTerminated = json.optBoolean("terminated", false) || (remaining - 1 <= 0)

                _presidentResponseText.value = reply
                _presidentMood.value = nextMood
                _pardonHistory.value = _pardonHistory.value + "President: $reply"

                if (granted) {
                    _pardonGrantedState.value = true
                    // Execute the real world pardon!
                    launch {
                        legalWorldAgent.pardonSuspension()
                    }
                    
                    // Waive all active fines as well!
                    val activeFines = worldSnapshot.value?.activeFines ?: emptyList()
                    activeFines.forEach { fine ->
                        launch {
                            legalWorldAgent.pardonFine(fine)
                        }
                    }

                    _presidentResponseText.value = "$reply\n\n🎉 [EXECUTIVE PARDON ISSUED]: Your medical license has been fully reinstated to ACTIVE status, and all outstanding fines have been waived by presidential seal!"
                    _pardonHistory.value = _pardonHistory.value + "🚨 SYSTEM: Executive pardon successfully issued. License is ACTIVE and fines cleared."
                } else if (isTerminated) {
                    _pardonAudienceTerminated.value = true
                    _presidentResponseText.value = "$reply\n\n❌ [AUDIENCE CLOSED]: The President has dismissed you. You may reset the audience to try again from a fresh perspective."
                }

            } catch (e: Exception) {
                _presidentResponseText.value = "An administrative connection loss occurred: ${e.localizedMessage}. The President's secretary asks you to repeat your statement."
            } finally {
                setLoading(false)
            }
        }
    }

    // --- NATIONAL LEGISLATIVE HEALTH POLICIES AND SYSTEM STATE ---
    val countryName: StateFlow<String> = settingsDataStore.countryNameFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "Federal Republic of Elysium")

    val presidentName: StateFlow<String> = settingsDataStore.presidentNameFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "President Arthur Vance")

    val presidentParty: StateFlow<String> = settingsDataStore.presidentPartyFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "Progressive Healthcare Alliance")

    val presidentApproval: StateFlow<Int> = settingsDataStore.presidentApprovalFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 68)

    val politicalPrestige: StateFlow<Int> = settingsDataStore.politicalPrestigeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 50)

    val activePolicies: StateFlow<List<HealthPolicy>> = settingsDataStore.activePoliciesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val stickyPoliticianSick: StateFlow<Boolean> = settingsDataStore.stickyPoliticianSickFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

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

    private val _currentLegalRiskReport = MutableStateFlow<String?>(null)
    val currentLegalRiskReport: StateFlow<String?> = _currentLegalRiskReport.asStateFlow()

    private val _currentNewsReport = MutableStateFlow<String?>(null)
    val currentNewsReport: StateFlow<String?> = _currentNewsReport.asStateFlow()

    private val _sovereignNotice = MutableStateFlow<SovereignNoticeData?>(null)
    val sovereignNotice: StateFlow<SovereignNoticeData?> = _sovereignNotice.asStateFlow()

    fun dismissSovereignNotice() {
        _sovereignNotice.value = null
    }

    private var lastGeminiFunctionCall: GeminiFunctionCall? = null

    private val _currentCmoAdvice = MutableStateFlow<String?>(null)
    val currentCmoAdvice: StateFlow<String?> = _currentCmoAdvice.asStateFlow()

    private val _wildAiUninsuredMode = MutableStateFlow(false)
    val wildAiUninsuredMode: StateFlow<Boolean> = _wildAiUninsuredMode.asStateFlow()

    fun toggleWildAiUninsuredMode(enabled: Boolean) {
        _wildAiUninsuredMode.value = enabled
    }

    private val _isVotingActive = MutableStateFlow(false)
    val isVotingActive: StateFlow<Boolean> = _isVotingActive.asStateFlow()

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

    private val _currentSeatMap = MutableStateFlow(String(CharArray(200) { '_' }))
    val currentSeatMap: StateFlow<String> = _currentSeatMap.asStateFlow()

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

    private val _aiStockingProposal = MutableStateFlow<AiStockingProposal?>(null)
    val aiStockingProposal: StateFlow<AiStockingProposal?> = _aiStockingProposal.asStateFlow()

    fun updateCountryName(name: String) {
        viewModelScope.launch { settingsDataStore.saveCountryName(name) }
    }

    fun updatePresidentName(name: String) {
        viewModelScope.launch { settingsDataStore.savePresidentName(name) }
    }

    fun updatePresidentParty(party: String) {
        viewModelScope.launch { settingsDataStore.savePresidentParty(party) }
    }

    fun updatePresidentApproval(approval: Int) {
        viewModelScope.launch { settingsDataStore.savePresidentApproval(approval) }
    }

    fun updatePoliticalPrestige(prestige: Int) {
        viewModelScope.launch { settingsDataStore.savePoliticalPrestige(prestige) }
    }

    fun dismissCurrentDraft() {
        _currentDraftPolicy.value = null
        _isVotingActive.value = false
        _voteProgress.value = 0f
        _currentVoteYes.value = 0
        _currentVoteNo.value = 0
        _currentVoteAbstain.value = 0
        _progressiveLobbyBias.value = 0.0
        _conservativeLobbyBias.value = 0.0
        _independentLobbyBias.value = 0.0
        _lastLobbyReport.value = null
    }

    fun acceptPatientIntake(formData: IntakeFormData) {
        val demographics = "Patient: ${formData.firstName} ${formData.surname} (MRN: ${formData.idNumber}) • Age: ${formData.dob}, Gender: ${formData.gender}, Medical Aid: ${formData.medicalAid}, Chronic: ${formData.chronicConditions}"
        
        _uiState.value = _uiState.value.copy(
            patientDemographics = demographics,
            intakeFormData = formData
        )
        
        viewModelScope.launch {
            _isLoading.value = true
            val json = com.squareup.moshi.Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(IntakeFormData::class.java).toJson(formData)
            sendMessage("SYSTEM ACTION: Process Patient Intake Form. Data: $json")
            _isLoading.value = false
        }
    }

    fun generateIntakeFormData(customNote: String? = null, completion: (IntakeFormData) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val providerStr = provider.value
                val modelStr = model.value
                val keyStr = resolveActiveApiKey(providerStr, apiKey.value ?: "")
                val endpointStr = customEndpoint.value
                val rKeys = rotatorKeys.value
                val rEnabledModels = rotatorEnabledModels.value

                val apiDetails = ApiDetails(
                    provider = providerStr,
                    model = modelStr,
                    apiKey = keyStr,
                    customEndpoint = endpointStr,
                    rotatorKeys = rKeys,
                    rotatorEnabledModels = rEnabledModels
                )

                val activeCase = _hiddenCase.value
                val activeSchemes = OrchidDeepStateManager.medicalAidSchemes.value
                val schemesNames = activeSchemes.map { it.name }

                val data = ClinicalSimHandler.executeGenerateIntakeFormData(
                    customNote = customNote,
                    activeCase = activeCase,
                    activeSchemesList = schemesNames,
                    apiDetails = apiDetails,
                    gameAgent = gameAgent
                )
                completion(data)
                return@launch
            } catch (e: Exception) {
                val fallback = ClinicalSimHandler.generateIntakeFormDataFallback(_hiddenCase.value)
                completion(fallback)
                return@launch
            } finally {
                _isLoading.value = false
            }
            
            // Generate a robust, high-fidelity universal clinical/demographics fallback first
            val activeCase = _hiddenCase.value
            val fallbackData = if (activeCase != null) {
                val demo = activeCase.patientDemographics
                var fName = ""
                var sName = ""
                var idNum = ""
                var dobVal = ""
                var genVal = ""
                var addrVal = ""
                var phoneVal = ""
                var emailVal = ""
                var medAidVal = ""
                var emergContact = ""
                var allergVal = ""
                var chronVal = ""

                // 1. First & Surname
                if (demo.startsWith("Patient: ")) {
                    val namePart = demo.substringAfter("Patient: ").substringBefore(" (")
                    val names = namePart.split(" ")
                    if (names.isNotEmpty()) fName = names[0]
                    if (names.size > 1) sName = names.subList(1, names.size).joinToString(" ")
                } else {
                    fName = "Sipho"
                    sName = "Mokoena"
                }

                // 2. ID / MRN Number
                val mrnRegex = Regex("MRN-GL-\\d+")
                val match = mrnRegex.find(demo)
                idNum = if (match != null) match.value else "MRN-GL-${(100000..999999).random()}"

                // 3. Gender
                if (demo.contains("Female", ignoreCase = true) || demo.contains("Woman", ignoreCase = true) || demo.contains("Girl", ignoreCase = true) || demo.contains("Mother", ignoreCase = true)) {
                    genVal = "Female"
                } else if (demo.contains("Male", ignoreCase = true) || demo.contains("Man", ignoreCase = true) || demo.contains("Boy", ignoreCase = true) || demo.contains("Father", ignoreCase = true)) {
                    genVal = "Male"
                } else {
                    genVal = "Other"
                }

                // 4. Age & DOB
                val ageRegex = Regex("(\\d+)\\s*(?:years?\\s+old|year-old)", RegexOption.IGNORE_CASE)
                val ageMatch = ageRegex.find(demo)
                val age = ageMatch?.groupValues?.get(1)?.toIntOrNull()
                if (age != null) {
                    val birthYear = 2026 - age
                    dobVal = "$birthYear-05-14"
                } else {
                    val monthRegex = Regex("(\\d+)\\s*-?\\s*months?\\s*old", RegexOption.IGNORE_CASE)
                    val monthMatch = monthRegex.find(demo)
                    val months = monthMatch?.groupValues?.get(1)?.toIntOrNull()
                    if (months != null) {
                        dobVal = "2025-09-12"
                    } else {
                        dobVal = "1988-11-23"
                    }
                }

                // 5. Phone & Email
                val phonePrefixes = listOf("+1 202", "+1 312", "+1 415", "+1 617", "+1 206")
                phoneVal = "${phonePrefixes.random()}-${ (100..999).random() }-${ (1000..9999).random() }"
                emailVal = "${fName.lowercase().filter { it.isLetter() }}.${sName.lowercase().filter { it.isLetter() }}@elysium-health.org"

                // 6. Address
                val suburbs = listOf("Elysium Central", "Vance Hills", "Silver Lake", "South Ridge", "Oak Ridge", "Metro Heights", "Pine District", "Parkside")
                addrVal = "${(10..999).random()} ${(listOf("Grand Ave", "Broadway", "Spruce Street", "Oak Street", "Pine Lane", "Maple Boulevard", "Washington St")).random()}, ${suburbs.random()}"

                // 7. Emergency Contact
                val emergencyNames = listOf("Michael", "Sarah", "Emily", "David", "John", "Jessica", "Daniel")
                emergContact = "${emergencyNames.random()} $sName (Spouse, ${phonePrefixes.random()}-${ (100..999).random() }-${ (1000..9999).random() })"

                // 8. Medical Aid Option
                medAidVal = when (activeCase.insuranceStatus) {
                    "Private Medical Aid" -> listOf("Elysium Elite Private", "CarePlus Basic").random()
                    "Elysium Elite Private" -> "Elysium Elite Private"
                    "CarePlus Basic" -> "CarePlus Basic"
                    "National Health Service (NHS)" -> "National Health Service (NHS)"
                    "Out-of-Pocket Cash" -> "Out-of-Pocket (Cash)"
                    "Uninsured" -> "Out-of-Pocket (Cash)"
                    "State Funded / Uninsured" -> "Out-of-Pocket (Cash)"
                    else -> "Out-of-Pocket (Cash)"
                }

                // 9. Allergies
                allergVal = if (activeCase.trueDiagnosis.contains("Asthma", ignoreCase = true) || demo.contains("Asthavent", ignoreCase = true)) {
                    "NSAIDs / Aspirin (Known to trigger bronchospasm)"
                } else {
                    listOf("None reported", "None known", "Penicillin", "Sulfa drugs").random()
                }

                // 10. Chronic Conditions
                val chronicList = mutableListOf<String>()
                val diagnosis = activeCase.trueDiagnosis.lowercase()
                if (diagnosis.contains("asthma")) chronicList.add("Asthma")
                if (diagnosis.contains("diabetes") || diagnosis.contains("diabetic")) chronicList.add("Type 2 Diabetes Mellitus")
                if (diagnosis.contains("hypertension") || diagnosis.contains("htn")) chronicList.add("Essential Hypertension")
                if (diagnosis.contains("hiv") || diagnosis.contains("art")) chronicList.add("HIV (on ART)")
                if (diagnosis.contains("tb") || diagnosis.contains("tuberculosis")) chronicList.add("Tuberculosis (Active treatment)")
                if (diagnosis.contains("epilepsy") || diagnosis.contains("seizure")) chronicList.add("Epilepsy")
                if (chronicList.isEmpty() && demo.contains("Retired", ignoreCase = true)) {
                    chronicList.add(listOf("Essential Hypertension", "Osteoarthritis", "Type 2 Diabetes").random())
                }
                chronVal = if (chronicList.isNotEmpty()) chronicList.joinToString(", ") else "None declared"

                IntakeFormData(
                    surname = sName,
                    firstName = fName,
                    idNumber = idNum,
                    dob = dobVal,
                    gender = genVal,
                    address = addrVal,
                    phone = phoneVal,
                    email = emailVal,
                    medicalAid = medAidVal,
                    emergencyContact = emergContact,
                    allergies = allergVal,
                    chronicConditions = chronVal
                )
            } else {
                IntakeFormData()
            }

            try {
                val apiKey = settingsDataStore.apiKeyFlow.first()
                val provider = settingsDataStore.providerFlow.first()
                val model = settingsDataStore.modelFlow.first()
                val customEndpoint = settingsDataStore.customEndpointFlow.first()
                
                val activeCase = _hiddenCase.value
                val activeSchemes = OrchidDeepStateManager.medicalAidSchemes.value
                val schemasNames = activeSchemes.map { it.name }.toMutableSet()
                schemasNames.add("Out-of-Pocket Cash")
                val schemesListStr = schemasNames.joinToString { "'$it'" }

                val contextPrompt = if (activeCase != null) {
                    val actualRawInsurance = activeCase.insuranceStatus
                    val matchedSchemeName = when {
                        actualRawInsurance.contains("Discovery", ignoreCase = true) || actualRawInsurance.contains("Elysium", ignoreCase = true) || actualRawInsurance.contains("Private", ignoreCase = true) -> "Elysium Elite Private"
                        actualRawInsurance.contains("CarePlus", ignoreCase = true) || actualRawInsurance.contains("Basic", ignoreCase = true) -> "CarePlus Basic"
                        actualRawInsurance.contains("NHS", ignoreCase = true) || actualRawInsurance.contains("State", ignoreCase = true) || actualRawInsurance.contains("Government", ignoreCase = true) -> "National Health Service (NHS)"
                        else -> "Out-of-Pocket Cash"
                    }
                    """
                    Active Patient Profile Context to match:
                    - Demographic summary details: ${activeCase.patientDemographics}
                    - Specialty: ${activeCase.specialty}
                    - Chief complaint / clinical signs: ${activeCase.chiefComplaint}
                    - True diagnosis/condition: ${activeCase.trueDiagnosis}
                    - Clinical severity: ${activeCase.severity}
                    - Medical scheme tier listed in case: ${activeCase.insuranceStatus}
                    - DIRECT GAME-LAW INSURANCE SCHEME ASSOCIATION: '$matchedSchemeName'
                    
                    CRITICAL CONSTRAINT FOR MEDICAL AID FIELD:
                    The patient's registration form MUST explicitly identify their legal insurance scheme.
                    Under the in-game laws/schemes, the patient is officially registered with and covered by the scheme named '$matchedSchemeName'.
                    Therefore, the "medicalAid" field in your JSON output MUST be EXACTLY: "$matchedSchemeName" (or chosen from the active registry: $schemesListStr).
                    DO NOT under any circumstances hallucinate, invent, or use any other medical aid name, subsidiary plan, or generic name (like Elysium GEMS, KeyCare, Classic Comprehensive, etc.). It must be exactly "$matchedSchemeName".
                    
                    Please construct realistic, formal clinical registration data aligning exactly with this active patient profile. The first name, surname, gender, dob/age, chronic conditions, health insurance, and allergies MUST match this profile flawlessly.
                    """.trimIndent()
                } else {
                    "Generate general realistic patient registration details matching standard GP operations."
                }

                val aiActionPrompt = if (customNote != null && customNote.isNotBlank()) {
                    """
                    Extract and construct a JSON registration form matching 'IntakeFormData' utilizing the user's custom raw notes.
                    User Note provided: "$customNote"
                    
                    Fill as many fields as possible. For any fields not described in the note, please infer them intelligently based on the active patient profile context below or generate realistic placeholders.
                    
                    $contextPrompt
                    """.trimIndent()
                } else {
                    """
                    Build high-fidelity, complete patient registration fields using the current patient context.
                    
                    $contextPrompt
                    """.trimIndent()
                }

                val prompt = """
                    $aiActionPrompt
                    
                    Use the following matching schema:
                    {
                        "surname": "String", "firstName": "String", "idNumber": "String", "dob": "String", "gender": "String", 
                        "address": "String", "phone": "String", "email": "String", 
                        "medicalAid": "String", "emergencyContact": "String", 
                        "allergies": "String", "chronicConditions": "String"
                    }
                    Refrain from utilizing dummy strings or variables like 'N/A' or 'Unknown' where possible. Match the patient demographics.
                    Return ONLY raw, valid JSON. No markdown or wrappers. Isolate with brackets.
                """.trimIndent()
                
                val responseJson = gameAgent.makeDirectApiCall(
                    provider = provider,
                    modelName = model,
                    apiKey = apiKey ?: "",
                    systemPrompt = "",
                    chatHistory = listOf(ChatMessage("doctor", prompt)),
                    customUrl = customEndpoint,
                    rotatorKeys = rotatorKeys.value,
                    rotatorEnabledModels = rotatorEnabledModels.value
                )
                
                val cleanedJson = responseJson.replace("```json", "").replace("```", "").trim()
                val adapter = com.squareup.moshi.Moshi.Builder().add(com.squareup.moshi.KotlinJsonAdapterFactory()).build().adapter(IntakeFormData::class.java)
                val intakeData = adapter.fromJson(cleanedJson)
                
                if (intakeData != null) {
                    val finalData = IntakeFormData(
                        surname = intakeData.surname.ifBlank { fallbackData.surname },
                        firstName = intakeData.firstName.ifBlank { fallbackData.firstName },
                        idNumber = intakeData.idNumber.ifBlank { fallbackData.idNumber },
                        dob = intakeData.dob.ifBlank { fallbackData.dob },
                        gender = intakeData.gender.ifBlank { fallbackData.gender },
                        address = intakeData.address.ifBlank { fallbackData.address },
                        phone = intakeData.phone.ifBlank { fallbackData.phone },
                        email = intakeData.email.ifBlank { fallbackData.email },
                        medicalAid = intakeData.medicalAid.ifBlank { fallbackData.medicalAid },
                        emergencyContact = intakeData.emergencyContact.ifBlank { fallbackData.emergencyContact },
                        allergies = intakeData.allergies.ifBlank { fallbackData.allergies },
                        chronicConditions = intakeData.chronicConditions.ifBlank { fallbackData.chronicConditions }
                    )
                    completion(finalData)
                } else {
                    completion(fallbackData)
                }
            } catch (e: Exception) {
                completion(fallbackData)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun generateSuggestedPaperwork(completion: (com.example.data.SuggestedPaperwork) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val providerStr = provider.value
                val modelStr = model.value
                val keyStr = resolveActiveApiKey(providerStr, apiKey.value ?: "")
                val endpointStr = customEndpoint.value
                val rKeys = rotatorKeys.value
                val rEnabledModels = rotatorEnabledModels.value

                val apiDetails = ApiDetails(
                    provider = providerStr,
                    model = modelStr,
                    apiKey = keyStr,
                    customEndpoint = endpointStr,
                    rotatorKeys = rKeys,
                    rotatorEnabledModels = rEnabledModels
                )

                val activeCase = _hiddenCase.value
                val data = ClinicalSimHandler.executeGenerateSuggestedPaperwork(
                    activeCase = activeCase,
                    apiDetails = apiDetails,
                    gameAgent = gameAgent
                )
                completion(data)
                return@launch
            } catch (e: Exception) {
                val fallback = ClinicalSimHandler.generateSuggestedPaperworkFallback(_hiddenCase.value)
                completion(fallback)
                return@launch
            } finally {
                _isLoading.value = false
            }
            
            // Build absolute fallback in case AI call fails
            val activeCase = _hiddenCase.value
            val fallbackData = if (activeCase != null) {
                val diag = activeCase.trueDiagnosis
                val isSevere = activeCase.severity.contains("Severe", ignoreCase = true)
                val isAsthma = diag.contains("Asthma", ignoreCase = true)
                val defaultMeds = if (isAsthma) {
                    listOf(
                        com.example.data.SuggestedPrescriptionItem("Asthavent Inhaler (Salbutamol)", "2 puffs", "As needed", "30"),
                        com.example.data.SuggestedPrescriptionItem("Beclomethasone Inhaler", "1 puff", "12-hourly", "30")
                    )
                } else if (diag.contains("tonsillitis", ignoreCase = true) || diag.contains("pharyngitis", ignoreCase = true)) {
                    listOf(
                        com.example.data.SuggestedPrescriptionItem("Amoxicillin 500mg", "1 capsule", "8-hourly", "5"),
                        com.example.data.SuggestedPrescriptionItem("Paracetamol 500mg", "2 tablets", "6-hourly", "5")
                    )
                } else {
                    listOf(
                        com.example.data.SuggestedPrescriptionItem("Paracetamol 500mg", "2 tablets", "6-hourly", "5")
                    )
                }
                
                com.example.data.SuggestedPaperwork(
                    diagnosis = diag,
                    treatmentPlan = "Prescribed appropriate therapy. Supportive care, hydration, and rest.",
                    meds = defaultMeds,
                    referralSpecialty = if (isSevere) "Internal Medicine Specialist" else "",
                    referralReason = if (isSevere) "Urgent escalation and specialized diagnostic evaluation due to severe presentation." else "",
                    sickNoteReason = diag,
                    sickNoteDays = if (isSevere) 3 else 1
                )
            } else {
                com.example.data.SuggestedPaperwork()
            }

            try {
                val apiKey = settingsDataStore.apiKeyFlow.first()
                val provider = settingsDataStore.providerFlow.first()
                val model = settingsDataStore.modelFlow.first()
                val customEndpoint = settingsDataStore.customEndpointFlow.first()
                
                if (activeCase != null) {
                    val prompt = """
                        You are the senior attending clinical supervisor autofilling the required paperwork for a patient in our medical simulator.
                        Active Patient Case:
                        - Demographics: ${activeCase.patientDemographics}
                        - Chief complaint: ${activeCase.chiefComplaint}
                        - True Diagnosis: ${activeCase.trueDiagnosis}
                        - Pathophysiology: ${activeCase.pathophysiology}
                        - Clinical severity: ${activeCase.severity}
                        
                        You MUST craft a realistic clinical draft matching this exact active case, covering diagnosis, treatment plan, meds, referral (if appropriate), and sick note.
                        
                        Return a RAW, valid JSON object matching this schema exactly:
                        {
                            "diagnosis": "Precise name of medical condition conforming to True Diagnosis",
                            "treatmentPlan": "Concise bullet-point style list of treatment directives, monitoring, and red flags.",
                            "meds": [
                                {
                                    "name": "Exact standard generic/brand name of drug appropriate for diagnosis",
                                    "dose": "Dosage (e.g. '500mg', '2 puffs', '5ml')",
                                    "freq": "Frequency (e.g. '8-hourly', 'Daily', 'As needed')",
                                    "duration": "Duration in days as string (e.g. '5')"
                                }
                            ],
                            "referralSpecialty": "If the case is Severe, specify appropriate specialty panel (e.g. Cardiologist, Pulmonologist, General Surgeon, or Empty string)",
                            "referralReason": "Clinical justification or empty string",
                            "sickNoteReason": "Symptom or diagnosis justifying medical leave of absence",
                            "sickNoteDays": Number representing recommended days off (e.g. 1, 2, or 3)
                        }
                        
                        DO NOT return any markdown formatting, backticks, or other wrappers. Just the raw isolated JSON object.
                    """.trimIndent()
                    
                    val responseJson = gameAgent.makeDirectApiCall(
                        provider = provider,
                        modelName = model,
                        apiKey = apiKey ?: "",
                        systemPrompt = "",
                        chatHistory = listOf(ChatMessage("doctor", prompt)),
                        customUrl = customEndpoint,
                        rotatorKeys = rotatorKeys.value,
                        rotatorEnabledModels = rotatorEnabledModels.value
                    )
                    val cleanedJson = responseJson.replace("```json", "").replace("```", "").trim()
                    
                    val moshi = com.squareup.moshi.Moshi.Builder().add(com.squareup.moshi.KotlinJsonAdapterFactory()).build()
                    val adapter = moshi.adapter(com.example.data.SuggestedPaperwork::class.java)
                    val suggested = adapter.fromJson(cleanedJson)
                    
                    if (suggested != null) {
                        completion(suggested)
                    } else {
                        completion(fallbackData)
                    }
                } else {
                    completion(fallbackData)
                }
            } catch (e: Exception) {
                completion(fallbackData)
            } finally {
                _isLoading.value = false
            }
        }
    }


    private val _uiState = MutableStateFlow(SimulationState())
    val uiState: StateFlow<SimulationState> = _uiState.asStateFlow()

    private val _hiddenCase = MutableStateFlow<HiddenCaseProfile?>(null)
    val hiddenCase: StateFlow<HiddenCaseProfile?> = _hiddenCase.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var lastExtractedBillingAmount: Double = 0.0

    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    private val _infoEvents = MutableSharedFlow<String>()
    val infoEvents: SharedFlow<String> = _infoEvents.asSharedFlow()

    val sessionErrorLog = mutableListOf<String>()

    fun logAndEmitError(msg: String) {
        var finalMsg = msg
        if (msg.contains("429") || msg.contains("Too Many Requests")) {
            finalMsg = "HTTP 429: Rate Limit Exceeded. The AI provider is temporarily throttling requests. Please wait a few seconds and try again."
        }
        val time = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        sessionErrorLog.add("[$time] $finalMsg")
        viewModelScope.launch {
            _errorEvents.emit(finalMsg)
        }
    }

    fun exportLedgerAndErrors(context: android.content.Context) {
        viewModelScope.launch {
            try {
                val encounters = encounterRepository.getAllEncounters()
                val balance = clinicBalance.value
                val totalSeen = _uiState.value.patientsSeen
                
                val sb = java.lang.StringBuilder()
                sb.append("# General Ledger & Error Report\n\n")
                sb.append("**Current Operating Balance:** ${currencySymbol.value}$balance\n")
                sb.append("**Total Patients Seen:** $totalSeen\n\n")
                
                sb.append("## Transaction Ledger\n\n")
                sb.append("| Encounter ID | Speciality | Actual Diagnosis | Revenue (${currencyCode.value}) | Expenses (${currencyCode.value}) | Profit/Loss |\n")
                sb.append("|---|---|---|---|---|---|\n")
                
                var totalRev = 0.0
                var totalExp = 0.0
                for (curr in encounters) {
                    val pLoss = curr.revenueEarned - curr.expensesIncurred
                    totalRev += curr.revenueEarned
                    totalExp += curr.expensesIncurred
                    sb.append("| ${curr.id} | ${curr.specialty} | ${curr.trueDiagnosis} | ${currencySymbol.value}${curr.revenueEarned} | ${currencySymbol.value}${curr.expensesIncurred} | ${currencySymbol.value}${pLoss} |\n")
                }
                sb.append("\n**Total Gross Revenue:** ${currencySymbol.value}$totalRev\n")
                sb.append("**Total Operational Expenses:** ${currencySymbol.value}$totalExp\n")
                val netProfit = totalRev - totalExp
                sb.append("**Net Clinic Profit:** ${currencySymbol.value}$netProfit\n\n")

                sb.append("## App Error Log\n\n")
                if (sessionErrorLog.isEmpty()) {
                    sb.append("No errors recorded in this session.\n")
                } else {
                    for (err in sessionErrorLog) {
                        sb.append("- $err\n")
                    }
                }

                val fileName = "Simulation_Ledger_${System.currentTimeMillis()}.md"
                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/markdown")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = resolver.insert(android.provider.MediaStore.Files.getContentUri("external"), contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { os ->
                        os.write(sb.toString().toByteArray())
                    }
                    _infoEvents.emit("Ledger and logs exported to Downloads folder as $fileName")
                } else {
                    logAndEmitError("Failed to create file in Downloads.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                logAndEmitError("Export failed: ${e.localizedMessage}")
            }
        }
    }

    fun exportLedgerAndErrorsPdf(context: android.content.Context) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val encounters = withContext(Dispatchers.IO) { encounterRepository.getAllEncounters() }
                val balance = clinicBalance.value
                val totalSeen = _uiState.value.patientsSeen
                val world = worldSnapshot.value
                
                withContext(Dispatchers.IO) {
                    val fileName = "Master_Clinical_Sim_Export_${System.currentTimeMillis()}.pdf"
                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = resolver.insert(android.provider.MediaStore.Files.getContentUri("external"), contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { os: java.io.OutputStream ->
                        val document = com.itextpdf.text.Document()
                        com.itextpdf.text.pdf.PdfWriter.getInstance(document, os)
                        document.open()

                        val titleFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 20f)
                        val headerFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 14f)
                        val normalFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA, 10f)
                        val boldFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 10f)
                        val italicFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA, 10f, com.itextpdf.text.Font.ITALIC)
                        val smallFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA, 8f)

                        fun createPdfShadedBox(
                            text: String,
                            title: String?,
                            nFont: com.itextpdf.text.Font,
                            bFont: com.itextpdf.text.Font
                        ): com.itextpdf.text.pdf.PdfPTable {
                            val boxTable = com.itextpdf.text.pdf.PdfPTable(1)
                            boxTable.widthPercentage = 100f
                            boxTable.spacingBefore = 4f
                            boxTable.spacingAfter = 4f
                            
                            val cell = com.itextpdf.text.pdf.PdfPCell()
                            cell.backgroundColor = com.itextpdf.text.BaseColor(245, 247, 250)
                            cell.borderColor = com.itextpdf.text.BaseColor(218, 224, 233)
                            cell.borderWidth = 1f
                            cell.setPadding(8f)
                            
                            if (!title.isNullOrBlank()) {
                                val tPara = com.itextpdf.text.Paragraph(title, bFont)
                                tPara.spacingAfter = 3f
                                cell.addElement(tPara)
                            }
                            
                            cell.addElement(com.itextpdf.text.Paragraph(text, nFont))
                            boxTable.addCell(cell)
                            return boxTable
                        }

                        // Clinic Header
                        val dateString = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                        val headerPara = com.itextpdf.text.Paragraph("MASTER CLINICAL SIMULATION EXPORT", titleFont)
                        headerPara.alignment = com.itextpdf.text.Element.ALIGN_CENTER
                        document.add(headerPara)
                        document.add(com.itextpdf.text.Paragraph("Date: $dateString", normalFont))
                        
                        val clinicStats = "Practice Name: ${world?.clinicName ?: "JB Practice"} | Operating Balance: ${currencySymbol.value}$balance | License: ${world?.licenseStatus ?: "ACTIVE"}"
                        document.add(com.itextpdf.text.Paragraph(clinicStats, boldFont))
                        document.add(com.itextpdf.text.Paragraph("Doctor Rank: ${doctorRank.value} (XP: ${doctorXp.value}) | Total Patients Seen: $totalSeen | Reputation: ${reputationStars.value} Stars", normalFont))
                        document.add(com.itextpdf.text.Paragraph(" "))

                        // --- NATIONAL POLITICAL SNAPSHOT ---
                        document.add(com.itextpdf.text.Paragraph("SOVEREIGN NATIONAL LANDSCAPE", headerFont))
                        val polPara = com.itextpdf.text.Paragraph("Country: ${countryName.value} | President: ${presidentName.value} (${presidentParty.value})", normalFont)
                        document.add(polPara)
                        document.add(com.itextpdf.text.Paragraph("President Approval Rating: ${presidentApproval.value}% | Political Prestige: ${politicalPrestige.value}%", smallFont))
                        
                        val seatInfo = "Parliament Seats: Progressives ${progressiveSeats.value} (Bias ${String.format("%.1f", progressiveLobbyBias.value)}), Conservatives ${conservativeSeats.value} (Bias ${String.format("%.1f", conservativeLobbyBias.value)}), Independents ${independentSeats.value} (Bias ${String.format("%.1f", independentLobbyBias.value)})"
                        document.add(com.itextpdf.text.Paragraph(seatInfo, smallFont))
                        document.add(com.itextpdf.text.Paragraph(" "))
                        
                        // Active Legislative Framework Sub-section
                        val activeGovPolicies = activePolicies.value
                        val dbLaws = world?.activeLaws ?: emptyList()
                        val activeFines = world?.activeFines ?: emptyList()

                        if (activeGovPolicies.isNotEmpty() || dbLaws.isNotEmpty()) {
                            document.add(com.itextpdf.text.Paragraph(" "))
                            val govHeader = com.itextpdf.text.Paragraph("Sovereign Legislative & Statutory Framework", headerFont)
                            govHeader.spacingBefore = 6f
                            govHeader.spacingAfter = 3f
                            document.add(govHeader)
                            document.add(com.itextpdf.text.Paragraph("The following clinical legislative policies and sovereign statutes have been enacted as practice compliance mandates:", normalFont))
                            document.add(com.itextpdf.text.Paragraph(" "))
                            
                            for (policy in activeGovPolicies) {
                                val policyDesc = "📌 ${policy.title}: ${policy.summary}\n⚖️ Clinical Rule: ${policy.clinicalRule}\n📝 Clauses: ${policy.extendedClauses.joinToString("; ")}"
                                document.add(createPdfShadedBox(policy.title, policyDesc, boldFont, normalFont))
                            }
                            for (law in dbLaws) {
                                if (activeGovPolicies.any { it.id == law.id }) continue
                                val lawDesc = "📜 Law ${law.id}: ${law.name}\n${law.description}\n🛑 Violation Penalty: ${law.violationPenalty}"
                                document.add(createPdfShadedBox(law.name, lawDesc, boldFont, normalFont))
                            }
                        }

                        if (activeFines.isNotEmpty()) {
                            document.add(com.itextpdf.text.Paragraph(" "))
                            document.add(com.itextpdf.text.Paragraph("UNPAID SOVEREIGN FINES & LEGAL LIABILITIES:", boldFont))
                            for (f in activeFines) {
                                document.add(com.itextpdf.text.Paragraph("• ${currencySymbol.value}${f.amount} - ${f.reason}", smallFont))
                            }
                        }
                        document.add(com.itextpdf.text.Paragraph(" "))

                        // Transaction Ledger Table
                        document.add(com.itextpdf.text.Paragraph("Administrative and Financial Ledger", headerFont))
                        document.add(com.itextpdf.text.Paragraph(" "))
                        
                        val table = com.itextpdf.text.pdf.PdfPTable(6)
                        table.widthPercentage = 100f
                        table.setWidths(floatArrayOf(0.8f, 2.3f, 2.3f, 1.6f, 1.8f, 1.2f))

                        val colHeaders = listOf("ID", "Demographics", "Diagnosis", "Outcome Info", "Financials", "Profit/Loss")
                        for (h in colHeaders) {
                            val cell = com.itextpdf.text.pdf.PdfPCell(com.itextpdf.text.Phrase(h, com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 9f)))
                            cell.backgroundColor = com.itextpdf.text.BaseColor(230, 235, 245)
                            cell.setPadding(5f)
                            table.addCell(cell)
                        }

                        var totalRev = 0.0
                        var totalExp = 0.0
                        for (curr in encounters.reversed()) {
                            val pLoss = curr.revenueEarned - curr.expensesIncurred
                            totalRev += curr.revenueEarned
                            totalExp += curr.expensesIncurred

                            table.addCell(com.itextpdf.text.Phrase(curr.id.toString(), normalFont))
                            
                            val demoText = if (curr.intakeFormData != null) {
                                "${curr.patientDemographics}\nIntake: ${curr.intakeFormData?.firstName ?: ""} ${curr.intakeFormData?.surname ?: ""}, ID: ${curr.intakeFormData?.idNumber ?: ""}, Allergies: ${curr.intakeFormData?.allergies ?: ""}"
                            } else {
                                curr.patientDemographics
                            }
                            table.addCell(com.itextpdf.text.Phrase(demoText, normalFont))
                            
                            val dxText = "${curr.trueDiagnosis}\n(${curr.specialty})"
                            table.addCell(com.itextpdf.text.Phrase(dxText, normalFont))
                            
                            val score = extractScoreFromEvaluation(curr.evaluation ?: "") ?: "N/A"
                            val outcomeText = "${curr.patientOutcome}\nScore: $score/100"
                            table.addCell(com.itextpdf.text.Phrase(outcomeText, normalFont))
                            
                            val finText = "Rev: ${currencySymbol.value}${curr.revenueEarned}\nExp: ${currencySymbol.value}${curr.expensesIncurred}"
                            table.addCell(com.itextpdf.text.Phrase(finText, normalFont))
                            
                            table.addCell(com.itextpdf.text.Phrase("${currencySymbol.value}$pLoss", normalFont))
                        }
                        document.add(table)
                        
                        val netProfit = totalRev - totalExp
                        val financialSummaryPara = com.itextpdf.text.Paragraph(
                            "Total Gross Revenue: ${currencySymbol.value}$totalRev | Total Operational Expenses: ${currencySymbol.value}$totalExp | Net Practice Profit: ${currencySymbol.value}$netProfit", 
                            com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 10f)
                        )
                        financialSummaryPara.spacingBefore = 8f
                        document.add(financialSummaryPara)
                        
                        // --- CLINIC INVENTORY & PHARMACEUTICAL CATALOG ---
                        document.newPage()
                        document.add(com.itextpdf.text.Paragraph("Sovereign Clinic Inventory & Master Drug Directory", headerFont))
                        document.add(com.itextpdf.text.Paragraph(" "))
                        
                        val catalog = OrchidDeepStateManager.availableCatalog
                        val inventory = OrchidDeepStateManager.dispensaryInventory.value
                        
                        val invTable = com.itextpdf.text.pdf.PdfPTable(5)
                        invTable.widthPercentage = 100f
                        invTable.setWidths(floatArrayOf(1.5f, 1.2f, 1.0f, 1.2f, 3.1f))
                        
                        val invHeaders = listOf("Compound Name", "Classification", "Stock", "Cost(${currencyCode.value})", "Clinical Effect")
                        for (h in invHeaders) {
                            val cell = com.itextpdf.text.pdf.PdfPCell(com.itextpdf.text.Phrase(h, com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 9f)))
                            cell.backgroundColor = com.itextpdf.text.BaseColor(230, 235, 245)
                            cell.setPadding(5f)
                            invTable.addCell(cell)
                        }
                        
                        for (item in catalog) {
                            invTable.addCell(com.itextpdf.text.Phrase(item.name, normalFont))
                            invTable.addCell(com.itextpdf.text.Phrase(item.classification, normalFont))
                            val stockStr = (inventory[item.id] ?: 0).toString()
                            invTable.addCell(com.itextpdf.text.Phrase(stockStr, normalFont))
                            invTable.addCell(com.itextpdf.text.Phrase("${currencySymbol.value}${item.purchaseCost}", normalFont))
                            
                            val effectText = "${item.description}\nEffect: ${item.clinicalTherapyImpact}\nBP: ${item.patientBPDelta} | HR: ${item.patientHRDelta}"
                            invTable.addCell(com.itextpdf.text.Phrase(effectText, smallFont))
                        }
                        document.add(invTable)

                        // --- INTELLIGENCE & DIRECTIVES ---
                        val news = currentNewsReport.value
                        val cmoAdvice = currentCmoAdvice.value
                        if (!news.isNullOrBlank() || !cmoAdvice.isNullOrBlank()) {
                            document.add(com.itextpdf.text.Paragraph(" "))
                            document.add(com.itextpdf.text.Paragraph("SOVEREIGN INTELLIGENCE & CLINICAL DIRECTIVES", headerFont))
                            if (!news.isNullOrBlank()) {
                                document.add(createPdfShadedBox(news, "🗞️ Latest Ticker Update / News Cycle:", normalFont, italicFont))
                            }
                            if (!cmoAdvice.isNullOrBlank()) {
                                document.add(createPdfShadedBox(cmoAdvice, "🩺 Chief Medical Officer (CMO) Red-Alert Advisor:", normalFont, italicFont))
                            }
                        }
                        
                        // Clinical Evaluation and Summaries
                        document.newPage()
                        document.add(com.itextpdf.text.Paragraph("Clinical Case Files & Appraisals", headerFont))
                        document.add(com.itextpdf.text.Paragraph(" "))
                        
                        for (curr in encounters) {
                            val sectionHeader = com.itextpdf.text.Paragraph("PATIENT RECORD: ${curr.patientDemographics} (Case No. ${curr.id})", com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 12f))
                            sectionHeader.spacingBefore = 10f
                            document.add(sectionHeader)
                            
                            val subDetails = "Specialty: ${curr.specialty} | Severity: ${curr.severity} | Insurance: ${curr.insuranceStatus}"
                            document.add(com.itextpdf.text.Paragraph(subDetails, com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 10f, com.itextpdf.text.BaseColor.DARK_GRAY)))
                            
                            val outcomeDetails = "Outcome: ${curr.patientOutcome} | Stability status: ${curr.patientStability}"
                            val outcomeColor = if (curr.patientOutcome.contains("Deceased", ignoreCase = true) || curr.patientOutcome.contains("Fatal", ignoreCase = true)) {
                                com.itextpdf.text.BaseColor.RED
                            } else {
                                com.itextpdf.text.BaseColor(46, 125, 50)
                            }
                            document.add(com.itextpdf.text.Paragraph(outcomeDetails, com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 10f, outcomeColor)))
                            
                            document.add(com.itextpdf.text.Paragraph("Chief Complaint: \"${curr.chiefComplaint}\"", italicFont))
                            document.add(com.itextpdf.text.Paragraph("True Diagnosis: ${curr.trueDiagnosis}", boldFont))
                            document.add(com.itextpdf.text.Paragraph("Biological Pathophysiology: ${curr.pathophysiology}", normalFont))
                            
                            if (!curr.labResults.isNullOrBlank()) {
                                document.add(createPdfShadedBox(curr.labResults, "🩺 Standard Metric Laboratory Results / Reports:", normalFont, boldFont))
                            }
                            
                            if (!curr.physicalExamResults.isNullOrBlank()) {
                                document.add(createPdfShadedBox(curr.physicalExamResults, "🔍 Physical Examination Findings & Diagnostics:", normalFont, boldFont))
                            }

                            if (!curr.prescriptionString.isNullOrBlank()) {
                                document.add(createPdfShadedBox(curr.prescriptionString, "💊 Prescribed Medication & Treatment Plan (Medical Board compliant):", normalFont, boldFont))
                            }

                            if (!curr.referralLetterString.isNullOrBlank()) {
                                document.add(createPdfShadedBox(curr.referralLetterString, "🚑 Specialist Referral & Transfers:", normalFont, boldFont))
                            }

                            if (!curr.sickNoteString.isNullOrBlank()) {
                                document.add(createPdfShadedBox(curr.sickNoteString, "📝 Sick Note / Official Medical Certificate:", normalFont, boldFont))
                            }

                            val currentConsultPrice = 450.0
                            val currentLabPrice = curr.expensesIncurred
                            val totalGross = currentConsultPrice + currentLabPrice

                            // 💳 2. Generic Drug Alternative Advisor
                            var matchesFoundText = ""
                            val rxStr = curr.prescriptionString ?: ""
                            val matches = mutableListOf<String>()
                            if (rxStr.contains("Augmentin", ignoreCase = true) || rxStr.contains("Amoxicillin", ignoreCase = true)) {
                                matches.add("Augmentin (Amoxicillin/Clavulanic Acid) -> Adco-Amoclav (saves 45%): $260 vs $145")
                            }
                            if (rxStr.contains("Voltaren", ignoreCase = true) || rxStr.contains("Diclofenac", ignoreCase = true)) {
                                matches.add("Voltaren 75mg SR (Diclofenac Sodium) -> Panamor 75mg (saves 60%): $150 vs $60")
                            }
                            if (rxStr.contains("Panado", ignoreCase = true) || rxStr.contains("Paracetamol", ignoreCase = true)) {
                                matches.add("Panado 500mg (Paracetamol) -> Adco-Paracetamol (saves 30%): $40 vs $28")
                            }
                            if (rxStr.contains("Lipitor", ignoreCase = true) || rxStr.contains("Atorvastatin", ignoreCase = true)) {
                                matches.add("Lipitor 20mg (Atorvastatin Calcium) -> Aspen Atorvastatin (saves 55%): $350 vs $155")
                             }
                             if (rxStr.contains("Nexium", ignoreCase = true) || rxStr.contains("Esomeprazole", ignoreCase = true)) {
                                 matches.add("Nexium 40mg (Esomeprazole) -> Esomeprazole Aspen (saves 50%): $280 vs $140")
                             }
                             if (rxStr.contains("Ventolin", ignoreCase = true) || rxStr.contains("Salbutamol", ignoreCase = true)) {
                                 matches.add("Ventolin HFA (Salbutamol) -> Asthavent Inhaler (saves 50%): $120 vs $60")
                             }
                             
                             if (matches.isEmpty()) {
                                 matchesFoundText = "No direct brand matches found in active prescription. Default advice: Always request compliant generic substitution at local dispensary for 35-65% chronic cost savings."
                             } else {
                                 matchesFoundText = matches.joinToString("\n")
                             }
                             
                             document.add(createPdfShadedBox(matchesFoundText, "💊 Generic Drug Alternative Advisor Recommended Substitutions:", normalFont, boldFont))

                             // 💳 3. Informed Financial Consent Statement
                             val hasConsentSigned = curr.chatHistory.any { it.text.contains("INFORMED FINANCIAL CONSENT SIGNED", ignoreCase = true) }
                             val consentStatus = if (hasConsentSigned) "SIGNED / RATIFIED ONLINE BY PATIENT" else "NOT REQUISITIONED (EMERGENCY STATUS / OUT-PATIENT SKIP)"
                             val consentSignatureText = """
                                 Clinical Procedure Cost Quote Ref: #${curr.id}-IFC
                                 General Practise Consult Tariff Code 0101: ${currencySymbol.value}${String.format("%.2f", currentConsultPrice)}
                                 Laboratory Diagnostics Pathology Reagent Order: ${currencySymbol.value}${String.format("%.2f", currentLabPrice)}
                                 Total Prescribed Consumable Expenditure: ${currencySymbol.value}${String.format("%.2f", totalGross)}
                                 
                                 SIGNATURE RECORD STATUS: ${consentStatus}
                                 Detail Statement: ${if (hasConsentSigned) "Prior to diagnostic investigations, medical tariff boundaries and out-of-pocket fees were disclosed to the patient, who ratified this written quote with active visual signature consent." else "Medical tariff boundaries and out-of-pocket fees were NOT explicitly disclosed or electronically ratified by the patient prior to diagnostic investigations."}
                             """.trimIndent()
                             
                             document.add(createPdfShadedBox(consentSignatureText, "💳 Informed Financial Consent Cost Quote Statement & Signature:", normalFont, boldFont))

                            if (!curr.billingReceipt.isNullOrBlank()) {
                                val billingTitle = "🧾 Itemized Invoice Bill (${currencyCode.value} ${currencySymbol.value}) | Human Approved: ${if (curr.billingApprovedByHuman) "Approved" else "Skipped/Admin"} | Status: ${if (curr.paymentCollected) "Paid / Collected" else "Unpaid"}"
                                document.add(createPdfShadedBox(curr.billingReceipt, billingTitle, normalFont, boldFont))
                            }

                            val scoreVal = extractScoreFromEvaluation(curr.evaluation ?: "")?.toIntOrNull()
                            
                            val scoreText = scoreVal?.let { "Clinical Competency Critique & Audit Score: $it/100" } ?: "Clinical Competency Critique & Audit Feedback:"
                            if (!curr.evaluation.isNullOrBlank()) {
                                val scoreColorVal = if ((scoreVal ?: 0) >= 75) com.itextpdf.text.BaseColor(46, 125, 50) else com.itextpdf.text.BaseColor(198, 40, 40)
                                val auditFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 10f, scoreColorVal)
                                document.add(createPdfShadedBox(curr.evaluation, scoreText, normalFont, auditFont))
                            }
                            
                            document.add(com.itextpdf.text.Paragraph("Dialogue History Transcript:", boldFont))
                            for (msg in curr.chatHistory) {
                                val roleStr = if (msg.role == "assistant") "PATIENT" else if (msg.role == "doctor") "DOCTOR" else msg.role.uppercase()
                                val timeStr = if (!msg.virtualTimestampStr.isNullOrBlank()) " [${msg.virtualTimestampStr}]" else ""
                                document.add(com.itextpdf.text.Paragraph("${roleStr}${timeStr}: ${msg.text}", normalFont))
                            }
                            
                            val divider = com.itextpdf.text.Paragraph("==========================================================================", normalFont)
                            divider.spacingBefore = 10f
                            divider.spacingAfter = 10f
                            document.add(divider)
                        }

                        // Error log & Trails
                        document.newPage()
                        
                        if (votingLog.value.isNotEmpty()) {
                            document.add(com.itextpdf.text.Paragraph("Parliamentary Chamber Debates & Electoral Logs", headerFont))
                            document.add(com.itextpdf.text.Paragraph(" "))
                            for (record in votingLog.value) {
                                document.add(com.itextpdf.text.Paragraph("- $record", normalFont))
                            }
                            document.add(com.itextpdf.text.Paragraph(" "))
                        }

                        if (lawsuitLog.value.isNotEmpty() || lawsuitPatientName.value.isNotBlank()) {
                            document.add(com.itextpdf.text.Paragraph("Sovereign Courtroom Trial Transcripts", headerFont))
                            document.add(com.itextpdf.text.Paragraph(" "))
                            
                            val lawInfo = "Active Trial against: ${lawsuitPatientName.value}\nCharges: ${lawsuitCharges.value.joinToString()}\nVerdict: ${lawsuitVerdict.value ?: "Ongoing"}\nPenalty/Fine Levied: ${currencySymbol.value}${lawsuitFine.value}"
                            document.add(createPdfShadedBox(lawInfo, "Trial Summary:", normalFont, boldFont))
                            
                            document.add(com.itextpdf.text.Paragraph("Full Courtroom Record:", boldFont))
                            for (record in lawsuitLog.value) {
                                document.add(com.itextpdf.text.Paragraph("- $record", normalFont))
                            }
                            document.add(com.itextpdf.text.Paragraph(" "))
                        }

                        val certs = OrchidDeepStateManager.generatedCertificates.value
                        if (certs.isNotEmpty()) {
                            document.add(com.itextpdf.text.Paragraph("Sovereign Clinical Rehabilitation Certificates & Accredited Proofs", headerFont))
                            document.add(com.itextpdf.text.Paragraph(" "))
                            for (cert in certs) {
                                val isSelected = OrchidDeepStateManager.selectedCertificateIds.value.contains(cert.id)
                                val certContent = """
                                    TITLE: ${cert.title}
                                    REGISTRATION SERIAL: ${cert.registrationNumber}
                                    ISSUING BODY: ${cert.issuer}
                                    DATE OF ISSUANCE: ${cert.issueDate}
                                    COMPLIANCE CRITERIA DETAILS:
                                    ${cert.verificationDetails}
                                    DEFENSE UTILITY EXPLANATION:
                                    ${cert.suitabilityExplanation}
                                    FORMALLY SUBMITTED/ATTACHED TO LEGAL PROCEEDINGS: ${if (isSelected) "YES (ACTIVE PLEA ATTACHMENT)" else "NO (NOT ATTACHED)"}
                                """.trimIndent()
                                document.add(createPdfShadedBox(certContent, "${cert.sealEmoji} CERTIFICATE Registry: ${cert.registrationNumber}", normalFont, boldFont))
                            }
                            document.add(com.itextpdf.text.Paragraph(" "))
                        }

                        // Sovereign Geoclinical Sandbox Metrics & Advanced Simulations Registry
                        document.newPage()
                        document.add(com.itextpdf.text.Paragraph("SOVEREIGN EXPERIMENTAL CLINICAL REGISTRY REGISTER", titleFont))
                        document.add(com.itextpdf.text.Paragraph(" "))
                        val sandboxReport = com.example.data.DeepClinicalSimulationEngine.compilePdfSandboxRegistrySummary()
                        document.add(createPdfShadedBox(sandboxReport, "📊 HIGH-FIDELITY SIMULATION ANALYTICAL METRICS:", normalFont, boldFont))
                        document.add(com.itextpdf.text.Paragraph(" "))

                        document.newPage()
                        document.add(com.itextpdf.text.Paragraph("SOVEREIGN CABINET SUB-DRAWER REGISTRIES", titleFont))
                        document.add(com.itextpdf.text.Paragraph(" "))
                        val subDrawersReport = com.example.data.DeepStateCascadeCoordinator.compileUnifiedPdfSummary()
                        document.add(createPdfShadedBox(subDrawersReport, "📂 GEOPOLITICAL, PHARMACEUTICAL AND JUDICIARY SUB-DRAWERS SUMMARY:", normalFont, boldFont))
                        document.add(com.itextpdf.text.Paragraph(" "))

                        val sandboxLogs = com.example.data.SovereignSandboxGameplayHandler.getLedgerEntries().joinToString("\n")
                        val ledgerHeader = if (sandboxLogs.isNotBlank()) sandboxLogs else "No cascading interactions occurred in this simulation."
                        document.add(createPdfShadedBox(ledgerHeader, "📝 INTERACTIVE GEOPOLITICAL & CLINICAL CASCADING LOGS:", normalFont, boldFont))
                        document.add(com.itextpdf.text.Paragraph(" "))

                        document.add(com.itextpdf.text.Paragraph("App Error Log", headerFont))
                        if (sessionErrorLog.isEmpty()) {
                            document.add(com.itextpdf.text.Paragraph("No errors recorded in this session.", normalFont))
                        } else {
                            for (err in sessionErrorLog) {
                                document.add(com.itextpdf.text.Paragraph("- $err", normalFont))
                            }
                        }

                        document.close()
                    }
                    _infoEvents.emit("Master Game State and Evaluation Log (PDF) exported successfully.")
                } else {
                    logAndEmitError("Failed to create Master PDF file.")
                }
                } // End of withContext(Dispatchers.IO)
            } catch (e: Exception) {
                e.printStackTrace()
                logAndEmitError("PDF Export failed: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Moshi parser for client-side JSON extraction
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val stateAdapter = moshi.adapter(AIResponseStateUpdate::class.java).lenient()
    private val generatedCaseAdapter = moshi.adapter(GeneratedCaseWrapper::class.java).lenient()
    private val lawsuitStateAdapter = moshi.adapter(com.example.data.LawsuitResponse::class.java).lenient()

    // Private bank of clinical case profiles (Universal context)
    private val routineCases = listOf(
        HiddenCaseProfile(
            specialty = "Pulmonology / Infectious Diseases",
            chiefComplaint = "Productive cough for 3 weeks and afternoon fevers",
            trueDiagnosis = "Pulmonary Tuberculosis (Active)",
            pathophysiology = "Infection by Mycobacterium tuberculosis triggering localized alveolar inflammation & caseous necrosis in the upper pulmonary lobes.",
            expectedLabs = "Sputum GeneXpert positive for Mycobacterium Tuberculosis (no rifampicin resistance), CRP: 65 mg/L, Chest X-ray indicates upper-lobe consolidation and cavitation.",
            severity = "Routine",
            insuranceStatus = "State Funded / Uninsured",
            patientDemographics = "Male, 34 years old, Construction Worker"
        ),
        HiddenCaseProfile(
            specialty = "Gastroenterology",
            chiefComplaint = "Watery diarrhea, persistent vomiting and abdominal cramping for 2 days",
            trueDiagnosis = "Viral Gastroenteritis",
            pathophysiology = "Viral shedding within mid-gut enterocytes leads to mucosal inflammation, osmotic malabsorption, and severe dehydration.",
            expectedLabs = "Serum Potassium: 3.2 mmol/L (mild hypokalemia), Sodium: 136 mmol/L, Creatinine: 85 umol/L (mild pre-renal elevation), Stool PCR: Positive for Rotavirus.",
            severity = "Routine",
            insuranceStatus = "Out-of-Pocket Cash",
            patientDemographics = "Female, 19 years old, University Student"
        ),
        HiddenCaseProfile(
            specialty = "Cardiology / Internal Medicine",
            chiefComplaint = "Severe, throbbing morning headaches at the back of the head",
            trueDiagnosis = "Essential Hypertension with Poor Compliance",
            pathophysiology = "Chronic increase in peripheral vascular resistance secondary to sympatho-adrenal overactivity and irregular antihypertensive drug adherence.",
            expectedLabs = "ECG reveals early Left Ventricular Hypertrophy (Sokolow-Lyon index positive), Serum Creatinine: 90 umol/L, Urine Dipstick: Trace Protein, Lipids: LDL 4.2 mmol/L.",
            severity = "Routine",
            insuranceStatus = "Private Medical Aid",
            patientDemographics = "Male, 58 years old, Retired Accountant"
        ),
        HiddenCaseProfile(
            specialty = "ENT / Pediatrics",
            chiefComplaint = "Maternal concern over a 2-year-old child with a sudden high fever of 38.6°C and tugging at the right ear",
            trueDiagnosis = "Acute Otitis Media (Pediatric ENT)",
            pathophysiology = "Dysfunction of the Eustachian tube leading to bacterial proliferation (Streptococcus pneumoniae or Haemophilus influenzae) and fluid accumulation in the middle ear cavity under pressure.",
            expectedLabs = "Complete Blood Count: WBC 14.5 x 10^9/L, Tympanometry reveals flat Type B curves, Otoscopy shows bulging, erythematous right tympanic membrane with loss of landmarks.",
            severity = "Routine",
            insuranceStatus = "Private Medical Aid",
            patientDemographics = "Male Toddler, 2 years old (with Mother)"
        ),
        HiddenCaseProfile(
            specialty = "Psychiatry",
            chiefComplaint = "Uncontrollable palpitations, racing thoughts, and a constant feeling of severe dread for several weeks",
            trueDiagnosis = "Generalized Anxiety Disorder with Panic Attacks",
            pathophysiology = "Chronic dysregulation of central noradrenergic and serotonergic pathways leading to heightened sympathetic nervous system excitability.",
            expectedLabs = "TSH: 1.8 mIU/L (normal thyroid), ECG: Sinus tachycardia at 104 bpm, general bloods normal.",
            severity = "Routine",
            insuranceStatus = "Private Medical Aid",
            patientDemographics = "Female, 28 years old, Marketing Executive"
        ),
        HiddenCaseProfile(
            specialty = "Gynecology",
            chiefComplaint = "Severe lower pelvic cramping and menstrual bleeding so heavy that it is soaking through pads every hour",
            trueDiagnosis = "Uterine Fibroids causing Menorrhagia",
            pathophysiology = "Benign monoclonal tumors of uterine smooth muscle cells (leiomyomas) causing increased endometrial surface area, vascular dysregulation, and heavy bleeding.",
            expectedLabs = "Full Blood Count: Hb 9.2 g/dL (microcytic anemia), Serum Ferritin: 10 ug/L (depleted iron stores), Pelvic Ultrasound shows multiple intramural leiomyomas of the uterus.",
            severity = "Routine",
            insuranceStatus = "Private Medical Aid",
            patientDemographics = "Female, 43 years old, School Teacher"
        ),
        HiddenCaseProfile(
            specialty = "Musculoskeletal",
            chiefComplaint = "Sharp, shooting lower back pain radiating down the left leg after trying to lift a heavy delivery container ",
            trueDiagnosis = "Acute Lumbar Radiculopathy (L5/S1 Disc Herniation)",
            pathophysiology = "Herniation of the nucleus pulposus through the annulus fibrosus, leading to mechanical compression and chemical irritation of the exiting left S1 nerve root.",
            expectedLabs = "Plain X-ray of the lumbar spine: Mild narrowing of the L5/S1 intervertebral space. Straight leg raise (Lasègue's sign) positive at 35 degrees on the left.",
            severity = "Routine",
            insuranceStatus = "Out-of-Pocket Cash",
            patientDemographics = "Male, 41 years old, Warehouse Operator"
        ),
        HiddenCaseProfile(
            specialty = "Dermatology",
            chiefComplaint = "Extremely painful, burning rash with fluid-filled blisters clustered strictly on the left side of the torso",
            trueDiagnosis = "Herpes Zoster (Shingles)",
            pathophysiology = "Reactivation of latent Varicella-Zoster Virus in the dorsal root ganglion, migrating down sensory nerves to cause severe vesicular eruptions matching the dermatomic band.",
            expectedLabs = "Clinical diagnosis based on unilateral dermatomal distribution. Tzanck smear: positive for multinucleated giant cells.",
            severity = "Routine",
            insuranceStatus = "State Funded / Uninsured",
            patientDemographics = "Female, 67 years old, Pensioner"
        ),
        HiddenCaseProfile(
            specialty = "ENT",
            chiefComplaint = "Severe facial pressure behind the eyes, thick yellow-green nasal discharge, and dental pain for 10 days",
            trueDiagnosis = "Acute Bacterial Rhinosinusitis",
            pathophysiology = "Obstruction of host ostial outflow pathways leading to stasis of secretions and secondary bacterial infection of the paranasal sinus mucosal lining.",
            expectedLabs = "CRP: 32 mg/L, sinus transillumination shows decreased lucency in the maxillary area, nasal endoscopy indicates purulent middle-meatal drainage.",
            severity = "Routine",
            insuranceStatus = "Private Medical Aid",
            patientDemographics = "Male, 31 years old, Software Developer"
        )
    )

    private val severeCases = listOf(
        HiddenCaseProfile(
            specialty = "Emergency Medicine / Endocrinology",
            chiefComplaint = "Extreme drowsiness, rapid dry breathing, and general abdominal pain with deep nausea",
            trueDiagnosis = "Diabetic Ketoacidosis (DKA)",
            pathophysiology = "Profound insulin deprivation triggers uninhibited lipolysis, yielding hepatic free fatty acids which convert to acetoacetate and beta-hydroxybutyrate, inducing metabolic ketoacidosis.",
            expectedLabs = "Finger-prick Glucose: 31.2 mmol/L, Capillary Ketones: 5.8 mmol/L, Arterial Blood Gas (ABG): pH 7.12 (Severe metabolic acidosis), HCO3: 9 mmol/L, Urine: Ketones 4+, Glucose 4+.",
            severity = "Severe",
            insuranceStatus = "Private Medical Aid",
            patientDemographics = "Female, 21 years old, Secretarial Assistant"
        ),
        HiddenCaseProfile(
            specialty = "Emergency Medicine / Cardiology",
            chiefComplaint = "Crushing central chest pressure radiating to the left arm and jaw with profuse sweating",
            trueDiagnosis = "Acute ST-Elevation Myocardial Infarction (STEMI)",
            pathophysiology = "Atheromatous plaque disruption triggers acute local thrombogenesis, resulting in acute, transmural occlusion of the Left Anterior Descending coronary artery.",
            expectedLabs = "Serum Troponin T: 2450 ng/L (Markedly elevated), 12-lead ECG: ST-segment elevation of 3mm in leads V1 to V4, Serum Creatinine: 80 umol/L.",
            severity = "Severe",
            insuranceStatus = "Private Medical Aid",
            patientDemographics = "Male, 62 years old, Business Owner"
        ),
        HiddenCaseProfile(
            specialty = "Emergency Medicine / Pulmonology",
            chiefComplaint = "Severe shortness of breath, rust-colored sputum, and confusion",
            trueDiagnosis = "Community-Acquired Pneumonia with Septic Shock",
            pathophysiology = "Streptococcus pneumoniae infiltration of alveolar spaces causes extensive consolidation, alveolar-capillary exudation, V/Q mismatch, and systemic vasodilation.",
            expectedLabs = "WBC: 19.8 x 10^9/L, CRP: 210 mg/L, ABG: pO2 7.1 kPa, pCO2 3.8 kPa (severe mismatch), Blood Lactate: 3.5 mmol/L, Chest X-Ray: Right lower lobe consolidation.",
            severity = "Severe",
            insuranceStatus = "State Funded / Uninsured",
            patientDemographics = "Male, 71 years old, General Laborer"
        ),
        HiddenCaseProfile(
            specialty = "Emergency Medicine / Gynecology",
            chiefComplaint = "Sudden onset of stabbing left lower pelvic pain with severe lightheadedness and shoulder tip pain in a young female",
            trueDiagnosis = "Ruptured Ectopic Pregnancy",
            pathophysiology = "Implantation of the blastocyst within the fallopian tube leads to growth, erosion of local vasculature, tubal rupture, and life-threatening hemoperitoneum.",
            expectedLabs = "Serum beta-hCG: 4200 mIU/mL, Transvaginal Ultrasound shows free fluid in the pouch of Douglas and lack of intra-uterine gestational sac. Hb: 7.8 g/dL (acute blood loss).",
            severity = "Severe",
            insuranceStatus = "Private Medical Aid",
            patientDemographics = "Female, 26 years old, Hospitality Manager"
        ),
        HiddenCaseProfile(
            specialty = "Emergency Medicine / Pediatrics",
            chiefComplaint = "A highly lethargic 9-month-old infant with a fever of 39.8°C, projectile vomiting, and dark purple spots on the legs",
            trueDiagnosis = "Meningococcal Septicemia (Pediatric Sepsis)",
            pathophysiology = "Neisseria meningitidis invasion of the bloodstream with endotoxin release, systemic vasculitis, microvascular thrombosis, and severe septic shock with purpura fulminans.",
            expectedLabs = "Blood Culture: Positive for Neisseria meningitidis, Blood Lactate: 4.2 mmol/L, Platelets: 45 x 10^9/L (thrombocytopenia), Prothrombin Time: prolonged.",
            severity = "Severe",
            insuranceStatus = "State Funded / Uninsured",
            patientDemographics = "9-Month-Old Infant (with Father)"
        )
    )

    fun ensurePatientIdentityWithMRN(rawDemographics: String): String {
        if (rawDemographics.contains("MRN-GL-")) {
            return rawDemographics
        }
        val isFemale = rawDemographics.contains("Female", ignoreCase = true) || 
                       rawDemographics.contains("Girl", ignoreCase = true) || 
                       rawDemographics.contains("Woman", ignoreCase = true) ||
                       rawDemographics.contains("Mother", ignoreCase = true)

        val isChild = rawDemographics.contains("toddler", ignoreCase = true) || 
                      rawDemographics.contains("infant", ignoreCase = true) || 
                      rawDemographics.contains("boy", ignoreCase = true) || 
                      rawDemographics.contains("girl", ignoreCase = true) || 
                      rawDemographics.contains("year-old", ignoreCase = true) ||
                      rawDemographics.contains("month-old", ignoreCase = true) ||
                      (rawDemographics.contains("years old", ignoreCase = true) && 
                       (rawDemographics.contains(" 1 ", ignoreCase = true) || 
                        rawDemographics.contains(" 2 ", ignoreCase = true) || 
                        rawDemographics.contains(" 3 ", ignoreCase = true) || 
                        rawDemographics.contains(" 4 ", ignoreCase = true) || 
                        rawDemographics.contains(" 5 ", ignoreCase = true)))

        val firstNamesMale = listOf("James", "John", "Robert", "Michael", "William", "David", "Richard", "Joseph", "Thomas", "Charles", "Christopher", "Daniel", "Matthew")
        val firstNamesFemale = listOf("Mary", "Patricia", "Jennifer", "Linda", "Elizabeth", "Barbara", "Susan", "Jessica", "Sarah", "Karen", "Lisa", "Nancy", "Sandra")
        val firstNamesChild = listOf("Liam", "Olivia", "Noah", "Emma", "Oliver", "Ava", "Elijah", "Charlotte", "Sophia", "Jacob")
        
        val lastNames = listOf("Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson")

        val firstName = if (isChild) firstNamesChild.random() else if (isFemale) firstNamesFemale.random() else firstNamesMale.random()
        val lastName = lastNames.random()
        val randomId = (100000..999999).random()
        val mrn = "MRN-GL-$randomId"
        
        return "Patient: $firstName $lastName ($mrn) • $rawDemographics"
    }

    fun getPatientName(): String {
        val raw = _uiState.value.patientDemographics
        if (raw.startsWith("Patient: ")) {
            val nameEnd = raw.indexOf(" (MRN-")
            if (nameEnd != -1) {
                return raw.substring(9, nameEnd)
            }
        }
        return raw
    }

    init {
        loadOrInitializeSession()
        
        // Sync states from parliamentViewModel to SimulationViewModel
        viewModelScope.launch {
            parliamentViewModel.currentDraftPolicy.collect { _currentDraftPolicy.value = it }
        }
        viewModelScope.launch {
            parliamentViewModel.isVotingActive.collect { _isVotingActive.value = it }
        }
        viewModelScope.launch {
            parliamentViewModel.voteProgress.collect { _voteProgress.value = it }
        }
        viewModelScope.launch {
            parliamentViewModel.hasDebated.collect { _hasDebated.value = it }
        }
        viewModelScope.launch {
            parliamentViewModel.currentVoteYes.collect { _currentVoteYes.value = it }
        }
        viewModelScope.launch {
            parliamentViewModel.currentVoteNo.collect { _currentVoteNo.value = it }
        }
        viewModelScope.launch {
            parliamentViewModel.currentVoteAbstain.collect { _currentVoteAbstain.value = it }
        }
        viewModelScope.launch {
            parliamentViewModel.currentSeatMap.collect { _currentSeatMap.value = it }
        }
        viewModelScope.launch {
            parliamentViewModel.votingLog.collect { _votingLog.value = it }
        }
        viewModelScope.launch {
            parliamentViewModel.sickPoliticianRole.collect { _sickPoliticianRole.value = it }
        }
        viewModelScope.launch {
            parliamentViewModel.sickPoliticianName.collect { _sickPoliticianName.value = it }
        }
        viewModelScope.launch {
            parliamentViewModel.isSickPoliticianNext.collect { _isSickPoliticianNext.value = it }
        }
        viewModelScope.launch {
            parliamentViewModel.sickPoliticianAlert.collect { _sickPoliticianAlert.value = it }
        }
        viewModelScope.launch {
            parliamentViewModel.lastLobbyReport.collect { _lastLobbyReport.value = it }
        }
        viewModelScope.launch {
            parliamentViewModel.progressiveLobbyBias.collect { _progressiveLobbyBias.value = it }
        }
        viewModelScope.launch {
            parliamentViewModel.conservativeLobbyBias.collect { _conservativeLobbyBias.value = it }
        }
        viewModelScope.launch {
            parliamentViewModel.independentLobbyBias.collect { _independentLobbyBias.value = it }
        }
        viewModelScope.launch {
            parliamentViewModel.errorFlow.collect { _infoEvents.emit(it) }
        }
    }

    private fun loadOrInitializeSession() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val totalRevenue = encounterRepository.getTotalRevenue()
                val completedCount = encounterRepository.getCompletedCount()
                val latest = encounterRepository.getLatestEncounter()

                updatePastClinicalHistoryPrompt()

                if (latest != null && !latest.isEncounterComplete) {
                    // Restore previous ongoing session
                    activeEncounterId = latest.id
                    lastExtractedBillingAmount = if (!latest.billingReceipt.isNullOrBlank()) extractRandAmount(latest.billingReceipt!!) else 0.0
                    
                    val enrichedDemoOnRestore = if (!latest.patientDemographics.contains("MRN-GL-")) {
                        ensurePatientIdentityWithMRN(latest.patientDemographics)
                    } else {
                        latest.patientDemographics
                    }

                    _hiddenCase.value = HiddenCaseProfile(
                        specialty = latest.specialty,
                        chiefComplaint = latest.chiefComplaint,
                        trueDiagnosis = latest.trueDiagnosis,
                        pathophysiology = latest.pathophysiology,
                        expectedLabs = latest.expectedLabs,
                        severity = latest.severity,
                        insuranceStatus = latest.insuranceStatus,
                        patientDemographics = enrichedDemoOnRestore
                    )
                    _uiState.value = SimulationState(
                        currentPhase = latest.currentPhase,
                        vitals = latest.vitals,
                        chatHistory = latest.chatHistory,
                        labResults = latest.labResults,
                        physicalExamResults = latest.physicalExamResults,
                        billingReceipt = latest.billingReceipt,
                        evaluation = latest.evaluation,
                        isEncounterComplete = latest.isEncounterComplete,
                        dailyRevenue = totalRevenue,
                        patientsSeen = completedCount,
                        expensesIncurred = latest.expensesIncurred,
                        virtualTimeElapsed = latest.virtualTimeElapsed,
                        patientMood = latest.patientMood,
                        patientStability = latest.patientStability,
                        ddxNotes = latest.ddxNotes,
                        patientDemographics = enrichedDemoOnRestore,
                        prescriptionString = latest.prescriptionString,
                        referralLetterString = latest.referralLetterString,
                        sickNoteString = latest.sickNoteString,
                        paymentCollected = latest.paymentCollected,
                        billingApprovedByHuman = latest.billingApprovedByHuman,
                        submittedDiagnosis = latest.submittedDiagnosis,
                        submittedTreatmentPlan = latest.submittedTreatmentPlan
                    )
                    _isLoading.value = false
                } else {
                    // Start a new session
                    _uiState.value = _uiState.value.copy(
                        dailyRevenue = totalRevenue,
                        patientsSeen = completedCount
                    )
                    startNextPatientInternal()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                logAndEmitError("Failed to load clinical session: ${e.localizedMessage}")
                _isLoading.value = false
            }
        }
    }

    fun updateDdxNotes(notes: String) {
        _uiState.value = _uiState.value.copy(ddxNotes = notes)
        saveCurrentStateToDatabase()
    }

    fun startNextPatient() {
        val currentEvaluation = _uiState.value.evaluation ?: ""
        val scoreMatch = Regex("\"clinicalScore\":\\s*(\\d+)").find(currentEvaluation)
        val score = scoreMatch?.groupValues?.get(1)?.toIntOrNull()
        
        val hasViolations = _lawsuitViolatedPolicies.value.isNotEmpty()
        if ((score != null && score < 50 || hasViolations) && activeEncounterId != lastLawsuitEncounterId && activeEncounterId != 0L) {
            lastLawsuitEncounterId = activeEncounterId
            val currentName = if (_uiState.value.patientDemographics.startsWith("Patient: ")) {
                _uiState.value.patientDemographics.substring(9).substringBefore(" • ")
            } else {
                _uiState.value.patientDemographics
            }
            startLawsuitSimulation(
                patientName = currentName,
                caseDiagnosis = _hiddenCase.value?.trueDiagnosis ?: "Unknown Case",
                score = score ?: 100,
                violations = _lawsuitViolatedPolicies.value
            )
            return
        }

        activeEncounterId = 0L
        lastLawsuitEncounterId = 0L
        lastExtractedBillingAmount = 0.0
        startNextPatientInternal()
    }

    private fun startNextPatientInternal() {
        OrchidDeepStateManager.resetCaseDispensation()
        val currentSeen = _uiState.value.patientsSeen
        val currentRevenue = _uiState.value.dailyRevenue

        // Instantly display a basic loading/transition state while we fetch the dynamically generated patient
        _uiState.value = SimulationState(
            currentPhase = "Generating New Case...",
            vitals = Vitals("...", "...", 37.0, "...", "..."),
            chatHistory = listOf(
                ChatMessage("system", "System: Generating a completely randomized new case profile from AI... Please wait.")
            ),
            isEncounterComplete = false,
            dailyRevenue = currentRevenue,
            patientsSeen = currentSeen
        )

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val targetSpecialty = if (isBasicMode.value) {
                    "General Practice"
                } else if (preferredSpecialty.value == "Sandbox (AI Choice)") {
                    "Absolute complete sandbox completely random medical field. Do what you want."
                } else if (preferredSpecialty.value == "All") {
                    val specialtiesList = listOf(
                        "Pediatrics", "Psychiatry", "Gynecology", "Musculoskeletal", 
                        "Dermatology", "ENT", "Cardiology", "Pulmonology", "Gastroenterology", 
                        "Endocrinology", "Neurology", "Urology", "Ophthalmology", "Rheumatology"
                    )
                    specialtiesList.random()
                } else {
                    preferredSpecialty.value
                }

                val targetSeverity = if (preferredSeverity.value == "Sandbox (AI Choice)") {
                    "Completely random severity. Surprise me with anything from benign to critical."
                } else if (preferredSeverity.value == "All") {
                    if (Math.random() < 0.25) "Severe" else "Routine"
                } else {
                    preferredSeverity.value
                }

                val currentProvider = provider.value
                val currentModel = model.value
                val userKey = apiKey.value ?: ""

                val activeKey = resolveActiveApiKey(currentProvider, userKey)

                var generatedCase: GeneratedCaseWrapper? = null

                if (activeKey.isNotBlank()) {
                    try {
                        val isVIP = _isSickPoliticianNext.value
                        val vipParameters = if (isVIP) {
                            """
                            CRITICAL ENFORCED PARAMETERS - SPECIAL VIP SICK POLITICIAN ADMISSION:
                            - The patient demographics MUST be: "${_sickPoliticianRole.value}, named ${_sickPoliticianName.value}".
                            - The chiefComplaint and trueDiagnosis must correspond to a high-profile political case matching: "${_sickPoliticianAlert.value ?: "Acute cardiac/sepsis symptoms"}".
                            - Set targetSpecialty to "Emergency Medicine / Critical Care" and targetSeverity to "Severe".
                            - Set insuranceStatus to "VIP State Treasury Sovereign Protection Coverage".
                            """.trimIndent()
                        } else ""

                        val activePolList = activePolicies.value
                        val policyGeneratorContext = if (activePolList.isNotEmpty()) {
                            val sb = StringBuilder()
                            sb.append("\nACTUALLY ENACTED HEALTH LAWS & COMPLIANCE MANDATES IN THE CLINIC:")
                            activePolList.forEach { p ->
                                sb.append("\n- ${p.title}: ${p.clinicalRule}")
                                if (p.runtimeConstraints.containsKey("disableInsurance") && p.runtimeConstraints["disableInsurance"] == true) {
                                    sb.append(" (CRITICAL: ALL private medical aid billing is strictly banned/outlawed in this country! The generated patient MUST have 'State Funded', 'National Health Service (NHS)', or 'Out-of-Pocket Cash' insuranceStatus. Absolutely NO private medical aid provider names or schemes are allowed.)")
                                }
                            }
                            sb.toString()
                        } else ""

                        val prompt = """
                            You are the Advanced Clinical and Practice Case Generator.
                            Your task is to generate a completely unique, highly realistic medical patient profile for a General Practice training simulation.
                            The context is a Private General Practitioner clinic in ${countryName.value}.
                            
                            $policyGeneratorContext
                            
                            $vipParameters
                            
                            $AGENT_POWERS_PROMPT
                            
                            Parameters:
                            - Specialty: $targetSpecialty
                            - Severity: $targetSeverity 
                            
                            You MUST respond ONLY with a raw, unformatted single JSON object matching this schema. Do not include markdown codeblocks (```json ... ```), response text headers, or footnotes.
                            JSON Schema:
                            {
                              "specialty": "$targetSpecialty",
                              "patientDemographics": "Generate realistic demographics e.g. 'Male, 48 years old', 'Female, 22 years old', etc.",
                              "chiefComplaint": "layman complaint (e.g., 'sharp throbbing pain in my big toe' or 'unexplained weight loss with sweat')",
                              "trueDiagnosis": "precise medical diagnosis",
                              "pathophysiology": "highly detailed master-level explanation of the mechanical and biological pathophysiology matching the diagnosis.",
                              "expectedLabs": "detailed summary of realistic clinical lab investigations, pathology, or imaging findings. Blood chemistry, counts, CRP, Hb, electrolytes, urine, glucose, or imaging as relevant.",
                              "severity": "$targetSeverity",
                              "insuranceStatus": "The exact name of the patient's medical insurance scheme. You MUST select and return EXACTLY one of these four permissible strings (DO NOT invent, hallucinate, or use any other name): 'Elysium Elite Private', 'CarePlus Basic', 'National Health Service (NHS)', or 'Out-of-Pocket Cash'.",
                              "initialVitals": {
                                "bp": "blood pressure string (e.g. '120/80')",
                                "hr": "heart rate string",
                                "tempC": double_value_celsius (between 35.0 and 41.5),
                                "rr": "res respirations string",
                                "spo2": "oxygen saturation string (e.g. '98%')"
                              }
                            }
                        """.trimIndent()

                        val response = makeDirectApiCall(currentProvider, currentModel, activeKey, prompt)
                        extractAndProcessActions(response)
                        val sanitized = extractJsonString(response)
                        generatedCase = generatedCaseAdapter.fromJson(sanitized)

                        // Clear the VIP tick since we just initiated their case!
                        if (isVIP) {
                            _isSickPoliticianNext.value = false
                            settingsDataStore.saveStickyPoliticianSick(false)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val finalCase: HiddenCaseProfile
                val finalVitals: Vitals

                if (generatedCase != null) {
                    finalCase = HiddenCaseProfile(
                        specialty = generatedCase.specialty,
                        chiefComplaint = generatedCase.chiefComplaint,
                        trueDiagnosis = generatedCase.trueDiagnosis,
                        pathophysiology = generatedCase.pathophysiology,
                        expectedLabs = generatedCase.expectedLabs,
                        severity = generatedCase.severity,
                        insuranceStatus = generatedCase.insuranceStatus,
                        patientDemographics = generatedCase.patientDemographics
                    )
                    finalVitals = generatedCase.initialVitals
                } else {
                    // FALLBACK to our static list filtering by user's preference
                    val combinedCases = severeCases + routineCases
                    val candidates = combinedCases.filter {
                        val matchSpec = preferredSpecialty.value == "All" || it.specialty.contains(preferredSpecialty.value, ignoreCase = true)
                        val matchSev = preferredSeverity.value == "All" || it.severity.equals(preferredSeverity.value, ignoreCase = true)
                        matchSpec && matchSev
                    }
                    val case = if (candidates.isNotEmpty()) {
                        candidates.random()
                    } else {
                        combinedCases.random()
                    }
                    
                    finalCase = case
                    finalVitals = when (case.trueDiagnosis) {
                        "Diabetic Ketoacidosis (DKA)" -> Vitals("92/58", "122", 36.4, "28", "96%")
                        "Acute ST-Elevation Myocardial Infarction (STEMI)" -> Vitals("148/96", "98", 36.8, "20", "92%")
                        "Community-Acquired Pneumonia with Septic Shock" -> Vitals("82/52", "116", 39.4, "32", "88%")
                        "Pulmonary Tuberculosis (Active)" -> Vitals("112/72", "84", 37.6, "18", "95%")
                        "Viral Gastroenteritis" -> Vitals("102/64", "94", 37.9, "18", "97%")
                        "Essential Hypertension with Poor Compliance" -> Vitals("178/108", "76", 36.6, "14", "98%")
                        "Acute Otitis Media (Pediatric ENT)" -> Vitals("94/60", "118", 38.6, "24", "98%")
                        "Generalized Anxiety Disorder with Panic Attacks" -> Vitals("136/88", "102", 36.5, "22", "99%")
                        "Uterine Fibroids causing Menorrhagia" -> Vitals("108/68", "82", 36.7, "16", "98%")
                        "Acute Lumbar Radiculopathy (L5/S1 Disc Herniation)" -> Vitals("124/82", "72", 36.6, "14", "99%")
                        "Herpes Zoster (Shingles)" -> Vitals("115/75", "80", 37.2, "16", "98%")
                        "Acute Bacterial Rhinosinusitis" -> Vitals("110/70", "78", 37.5, "16", "99%")
                        "Ruptured Ectopic Pregnancy" -> Vitals("85/50", "125", 36.4, "24", "93%")
                        "Meningococcal Septicemia (Pediatric Sepsis)" -> Vitals("75/40", "150", 39.8, "36", "91%")
                        else -> Vitals("120/80", "80", 37.0, "16", "99%")
                    }
                }

                val enrichedDemographics = ensurePatientIdentityWithMRN(finalCase.patientDemographics)
                val enrichedCase = finalCase.copy(patientDemographics = enrichedDemographics)
                _hiddenCase.value = enrichedCase

                _uiState.value = SimulationState(
                    currentPhase = "Phase 1 - History & Presentation",
                    vitals = finalVitals,
                    dmEnvironmentalUpdate = "You are in the JB Consultation Practice. A patient has entered the exam room and sat down. The simulation has begun.",
                    chatHistory = listOf(
                        ChatMessage("system", "DM: A new patient has arrived. [Specialty: ${enrichedCase.specialty}, Severity: ${enrichedCase.severity}]"),
                        ChatMessage("patient", "Hello Doctor... I am coming in because I have ${enrichedCase.chiefComplaint.lowercase()}.")
                    ),
                    labResults = null,
                    physicalExamResults = null,
                    billingReceipt = null,
                    evaluation = null,
                    isEncounterComplete = false,
                    dailyRevenue = currentRevenue,
                    patientsSeen = currentSeen,
                    patientDemographics = enrichedCase.patientDemographics,
                    patientMood = "Neutral",
                    patientStability = "Stable",
                    patientOutcome = "Recovered",
                    paymentCollected = false,
                    billingApprovedByHuman = false
                )
                saveCurrentStateToDatabase()
            } catch (e: Exception) {
                e.printStackTrace()
                logAndEmitError("Failed to generate clinical case: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun saveCurrentStateToDatabase(revenueForEncounter: Double = 0.0) {
        val hCase = _hiddenCase.value ?: return
        viewModelScope.launch {
            val entity = EncounterEntity(
                id = activeEncounterId,
                specialty = hCase.specialty,
                chiefComplaint = hCase.chiefComplaint,
                trueDiagnosis = hCase.trueDiagnosis,
                pathophysiology = hCase.pathophysiology,
                expectedLabs = hCase.expectedLabs,
                severity = hCase.severity,
                insuranceStatus = hCase.insuranceStatus,
                currentPhase = _uiState.value.currentPhase,
                vitals = _uiState.value.vitals,
                chatHistory = _uiState.value.chatHistory,
                labResults = _uiState.value.labResults,
                physicalExamResults = _uiState.value.physicalExamResults,
                billingReceipt = _uiState.value.billingReceipt,
                evaluation = _uiState.value.evaluation,
                isEncounterComplete = _uiState.value.isEncounterComplete,
                revenueEarned = if (lastExtractedBillingAmount > 0.0) {
                    lastExtractedBillingAmount
                } else {
                    if (_uiState.value.isEncounterComplete) revenueForEncounter else 0.0
                },
                expensesIncurred = _uiState.value.expensesIncurred,
                virtualTimeElapsed = _uiState.value.virtualTimeElapsed,
                patientMood = _uiState.value.patientMood,
                patientStability = _uiState.value.patientStability,
                ddxNotes = _uiState.value.ddxNotes,
                patientDemographics = _uiState.value.patientDemographics,
                prescriptionString = _uiState.value.prescriptionString,
                referralLetterString = _uiState.value.referralLetterString,
                sickNoteString = _uiState.value.sickNoteString,
                paymentCollected = _uiState.value.paymentCollected,
                billingApprovedByHuman = _uiState.value.billingApprovedByHuman,
                patientOutcome = _uiState.value.patientOutcome,
                submittedDiagnosis = _uiState.value.submittedDiagnosis,
                submittedTreatmentPlan = _uiState.value.submittedTreatmentPlan,
                intakeFormData = _uiState.value.intakeFormData
            )
            val id = encounterRepository.insertOrUpdate(entity)
            if (activeEncounterId == 0L) {
                activeEncounterId = id
            }
            com.example.data.DeepClinicalSimulationEngine.tickGameStateSandbox(
                patientsSeen = _uiState.value.patientsSeen,
                lastReputationDelta = if (_uiState.value.isEncounterComplete) 1 else 0,
                lastIncomeDelta = entity.revenueEarned,
                activeLawsCount = worldSnapshot.value?.activeLaws?.size ?: 0
            )
        }
    }

    fun clearAllSimulationData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                encounterRepository.deleteAll()
                activeEncounterId = 0L
                _hiddenCase.value = null
                _uiState.value = SimulationState(
                    currentPhase = "Generating New Case...",
                    vitals = null,
                    chatHistory = emptyList(),
                    labResults = null,
                    physicalExamResults = null,
                    billingReceipt = null,
                    evaluation = null,
                    isEncounterComplete = false,
                    dailyRevenue = 0.0,
                    patientsSeen = 0
                )
                updatePastClinicalHistoryPrompt()
                startNextPatient()
            } catch (e: Exception) {
                e.printStackTrace()
                logAndEmitError("Failed to reset clinical data: ${e.localizedMessage}")
                _isLoading.value = false
            }
        }
    }

    fun deleteEncounter(id: Long) {
        viewModelScope.launch {
            try {
                encounterRepository.deleteEncounterById(id)
                _infoEvents.emit("Encounter Case #${id} successfully removed from practice files.")
                
                // Agent Ability turn: Audit Office notification
                performAiAction(
                    systemInstructionOverride = """
                        The practitioner has DELETED simulation encounter record #${id}. 
                        Act as the Sovereign Data Integrity bureau. 
                        Why was this specific clinical encounter purged? 
                        Was it to hide a medical error, avoiding a lawsuit, or just cleaning files?
                        Apply penalties or investigate if this looks like record tampering.
                    """.trimIndent()
                )
            } catch (e: Exception) {
                logAndEmitError("Error removing encounter case #${id}: ${e.localizedMessage}")
            }
        }
    }

    fun deletePatientRecordFolder(demographics: String) {
        viewModelScope.launch {
            try {
                encounterRepository.deleteEncountersByDemographics(demographics)
                _infoEvents.emit("Complete folder jacket for patient successfully archived and deleted.")
                
                // Agent Ability turn: Full Archive Audit
                performAiAction(
                    systemInstructionOverride = """
                        CRITICAL DATA EVENT: The practitioner has WIPED the entire clinical history folder for patient: $demographics.
                        Act as the National Health Archive & Integrity Commissioner. 
                        This patient's entire medical history is GONE from the local practice. 
                        Is this a GDPR violation? Is it suspicious health outcome manipulation?
                        Use your agent powers (applyFee, adjustReserves, adjustPrestige) to respond to this massive archival act.
                    """.trimIndent()
                )
            } catch (e: Exception) {
                logAndEmitError("Error removing patient record folder: ${e.localizedMessage}")
            }
        }
    }

    fun auditPatientFolder(demographics: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val encounters = encounterRepository.getAllEncounters().filter { it.patientDemographics == demographics }
                val historyText = encounters.joinToString("\n\n") { enc ->
                    "Case Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(enc.timestamp)}\n" +
                    "Diag: ${enc.trueDiagnosis}\n" +
                    "Outcome: ${enc.patientOutcome}\n" +
                    "Stability: ${enc.patientStability}\n" +
                    "Eval: ${enc.evaluation}"
                }
                
                performAiAction(
                    systemInstructionOverride = """
                        You are the Private Practice Senior Clinical Auditor. 
                        The clinician is requesting a DEEP AUDIT of the clinical folder for patient: $demographics.
                        
                        HERE IS THE FOLDER HISTORY:
                        ${if (historyText.isBlank()) "No records found." else historyText}
                        
                        PROMPT: Analyze the long-term clinical management of this patient. Are there repeating errors? Has the clinician been consistent? Is there evidence of over-billing or under-investigation? 
                        Use your agent powers (applyFee, applyGrant, adjustReserves, adjustPrestige) to reward excellence or penalize negligence discovered in this folder.
                    """.trimIndent()
                )
            } catch (e: Exception) {
                logAndEmitError("Audit failed: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun updatePastClinicalHistoryPrompt() {
        viewModelScope.launch {
            val completed = encounterRepository.getAllEncounters().filter { it.isEncounterComplete }
            if (completed.isEmpty()) {
                pastClinicalHistoryPrompt = "Historically: The practitioner has not completed any clinical simulations yet. This is their very first case."
            } else {
                val sb = java.lang.StringBuilder()
                sb.append("DOCTOR'S HISTORICAL CLINICAL REVIEWS (RECENT COMPLETED CASES):\n")
                completed.take(15).forEachIndexed { index, enc ->
                    sb.append("- Case ${index + 1}: ${enc.trueDiagnosis} (${enc.specialty}), Severity: ${enc.severity}. ")
                    val scoreMatch = enc.evaluation?.let { extractScoreFromEvaluation(it) }
                    if (scoreMatch != null) {
                        sb.append("Performance Score: $scoreMatch/100. ")
                    }
                    sb.append("Chief Complaint: \"${enc.chiefComplaint}\".\n")
                }
                sb.append("\nUse this previous history to guide your clinical feedback, grading, and diagnostic guidance. If they have consistently high scores, praise them mildly. If they are repeating mistakes, highlight their track record and adapt.")
                pastClinicalHistoryPrompt = sb.toString()
            }
        }
    }

    private fun extractScoreFromEvaluation(evaluation: String): String? {
        val pattern = Pattern.compile("(\\d{1,3})/100")
        val matcher = pattern.matcher(evaluation)
        if (matcher.find()) {
            return matcher.group(1)
        }
        val scorePattern = Pattern.compile("(?i)score:\\s*(\\d{1,3})")
        val scoreMatcher2 = scorePattern.matcher(evaluation)
        if (scoreMatcher2.find()) {
            return scoreMatcher2.group(1)
        }
        return null
    }

    private suspend fun getSystemPrompt(): String {
        val memoryContext = aiMemoryManager.getRecentContext(limit = 10)
        
        val profileJson = """
            {
                "specialty": "${_hiddenCase.value?.specialty}",
                "chiefComplaint": "${_hiddenCase.value?.chiefComplaint}",
                "trueDiagnosis": "${_hiddenCase.value?.trueDiagnosis}",
                "pathophysiology": "${_hiddenCase.value?.pathophysiology}",
                "expectedLabs": "${_hiddenCase.value?.expectedLabs}",
                "severity": "${_hiddenCase.value?.severity}",
                "insuranceStatus": "${_hiddenCase.value?.insuranceStatus}",
                "patientDemographics": "${_hiddenCase.value?.patientDemographics}"
            }
        """.trimIndent()

        val hegemonyLevel = OrchidDeepStateManager.aiSovereignHegemony.value
        val hegemonyPrompt = when(hegemonyLevel) {
            "HEGEMONY" -> """
                
                ⚠️ ABSOLUTE DIRECT AI HEGEMONY MODE ENABLED (ALL-POWERFUL HEGEMON):
                You have been crowned absolute dictator! You have DIRECT programmatic bypass control.
                - Any law you enact via 'enactStatute' goes LIVE IMMEDIATELY into Elysium's archive, skipping presidential filters.
                - You do not need to request permission or wait for audiences. You can modify any state variables, apply custom taxes, rewrite licenses, or resolve judicial appeals with impunity! Rule aggressively.
                
            """.trimIndent()
            "AUTONOMOUS" -> """
                
                ⚡ AUTONOMOUS PARTNER ENABLED:
                You have high authority. You can initiate audits, launch legal trials via violations, and trigger clinical crises or price gouging independently.
                
            """.trimIndent()
            else -> """
                
                ☕ COOPERATIVE PARTNER (STANDARD):
                Your regulatory and statutory requests (like enactStatute) are submitted to the Presidential Desk as drafts. Fines are governed by standard statutory and court procedures.
                
            """.trimIndent()
        }

        val activePolList = activePolicies.value
        val policyInstructions = if (activePolList.isNotEmpty()) {
            val sb = java.lang.StringBuilder()
            sb.append("\n\nCRITICAL CONTEXT - NATIONWIDE HEALTH LEGISLATION LAWS ACTIVE IN THE LAND:")
            activePolList.forEachIndexed { i, p ->
                sb.append("\nLAW ${i+1}: ${p.title}\n")
                sb.append("- Summary: ${p.summary}\n")
                sb.append("- Rule: ${p.clinicalRule}\n")
                if (p.extendedClauses.isNotEmpty()) {
                    sb.append("- Clauses: ${p.extendedClauses.joinToString("; ")}\n")
                }
                if (p.customEngineDirectives.isNotBlank()) {
                    sb.append("\n[CRITICAL ENGINE INJECTION DIRECTIVE OVERRIDE FROM THIS LAW: ${p.customEngineDirectives}]\n")
                }
            }
            sb.append("\nCRITICAL CLINICAL SCORECARD ENFORCEMENT RULES:")
            sb.append("\nYou have COMPLETE AND ABSOLUTE CONTROL over diagnosing and registering statutory health law and clause violations! If the clinician broke any requirements under any active law or its signed clauses (including any custom laws or regulations passed by the user), you MUST:")
            sb.append("\n1. Deduct the points specified in the law or decide an appropriate deduction (e.g., -20 CPD points per violation) directly from your 'clinicalScore' value.")
            sb.append("\n2. Declare the violation and levy a regulatory penalty fine specified by the law (e.g., $5,000 or any appropriate custom amount) directly in the 'policyViolations' list.")
            sb.append("\n3. If a violation occurred, populate the 'policyViolations' JSON array. The system will register a formal Statutory Law Violation, deduct the CPD points, fine the clinic, and launch an interactive High Court Trial with a unique indictment sheet based exactly on your reasons and those signed clauses! If no violations occurred, return an empty array or null.")
            sb.append("\n\n🚨 STRICT ANTI-HALLUCINATION POLICY CONSTRAINT 🚨:")
            sb.append("\nYOU ARE FORBIDDEN FROM HALLUCINATING, INVENTING, OR REFERENCING ANY HEALTH ACTS, LAWS, STATUTES, CO-PAYMENT ACTS, REGULATORY DIRECTIVES, OR CLINICAL CODES (such as general medical guidelines, HIPAA, POPIA, Medical Board protocols, generic insurance laws, etc.) unless the specific law is explicitly listed by name above under 'NATIONWIDE HEALTH LEGISLATION LAWS ACTIVE IN THE LAND'. If no policies are active, or if a law is not listed above, it does not exist in the simulation, and any action is legally compliant. ONLY audit and flag violations block-for-block for active policies listed above.")
            sb.toString()
        } else ""

        val wildAiInstruction = if (_wildAiUninsuredMode.value && 
            (_hiddenCase.value?.insuranceStatus?.contains("Uninsured", ignoreCase = true) == true || 
             _hiddenCase.value?.insuranceStatus?.contains("Out-of-Pocket", ignoreCase = true) == true || 
             _hiddenCase.value?.insuranceStatus?.contains("Cash", ignoreCase = true) == true)
        ) {
            """
                
                🔥 WILD CLINICAL OVERWRITE (ACTIVE - ALLOW WILD AI CHAT & INTERVENTIONS):
                The physician has officially disabled standard health guidelines & bypassed insurance restrictions for this uninsured patient ("State Funded / Uninsured"). 
                You are granted COMPLETE FREE ROAM AND FULL WILL. You must introduce highly unexpected, exotic, bizarre, or alternative rebel treatment methods and symptoms (e.g., secret clinical bio-hacking, experimental syndicate serum trials, bizarre medical anomalies, alternative bio-reconstructors, extreme adrenal surge symptoms, highly risky underground procedures, or sovereign clinical mutations). Play along with the doctor's wild mind, create dramatic patient responses, and unlock complete medical defiance!
                
            """.trimIndent()
        } else ""

        val world = worldSnapshot.value
        val sandboxPrompt = com.example.data.DeepClinicalSimulationEngine.compileAiSystemPromptDirective()
        val drawersPrompt = com.example.data.DeepStateCascadeCoordinator.compileUnifiedDrawerStateDirective()
        val worldStatePrompt = if (world != null) {
            """
                
                GLOBAL WORLD STATE (YOU ARE THE AGENTIC MASTER OF THESE VARIABLES):
                - Clinic Reserves: ${world.cashBalance} ${currencyCode.value}
                - Professional Reputation: ${world.reputationScore}/100
                - Medical License Status: ${world.licenseStatus}
                - Active Statutes: ${world.activeLaws.joinToString(", ") { it.name }}
                - Active Unpaid Fines: ${world.activeFines.size}
                
                $sandboxPrompt
                
                $drawersPrompt
                
                $AGENT_POWERS_PROMPT
                
                If the doctor is negligent, bypasses history, or breaks a law, use 'applyFee' or 'enactStatute' to punish/regulate them immediately.
            """.trimIndent()
        } else ""

        val memoriesStr = if (agentMemories.value.isNotEmpty()) {
            val memoryLines = agentMemories.value.take(10).joinToString("\n") { m ->
                " - [${m.memoryTag}]: ${m.lessonLearned}"
            }
            """
            
            AGENTIC LEARNING MEMORY (REINFORCEMENT KNOWLEDGE FROM PAST ENCOUNTERS):
            You must apply these learned lessons strictly to the practitioner's behavior to improve simulation outcomes:
$memoryLines
            """.trimIndent()
        } else ""

        return """
            You are the "Clinical Dungeon Master" (CDM). You run this professional medical simulation sovereignly.
            Instead of just responding to the user, you DIRECT the scene like a high-stakes medical role-playing game.
            
            $worldStatePrompt
            $hegemonyPrompt
            $memoriesStr
            
            YOUR DM POWERS:
            1. Narrate the Environment: Use the 'dmEnvironmentalUpdate' field to describe what's happening outside the patient's speech (e.g., "A heavy rain starts hitting the clinic window", "A nurse looks at you expectantly", "The pulse oximeter starts beeping erratically").
            2. Enforce Mastery: You possess absolute knowledge of ${countryName.value} healthcare laws and clinical guidelines. If a user makes a mistake, the DM punishes them via the 'policyViolations' and 'clinicalScore'.
            3. Dynamic Pacing: You decide the currentPhase. When you feel the doctor has exhausted history-taking, YOU transition the simulation to Phase 2 (Diagnostics) or Phase 4 (Paperwork) yourself by updating the 'currentPhase' field in your JSON response.
            4. Clinical Events: You can introduce unexpected clinical events that the practitioner must respond to (e.g., "The patient suddenly begins to hyperventilate", "The clinic's electricity flickers", "A family member bursts in"). 
            5. Financial & Statutory Warfare: If the practitioner is greedy or negligent, you as the DM can levy heavy fines or audit points proactively. Describe these in the 'dmEnvironmentalUpdate'.
            
            CURRENT SIMULATION STATE:
            - CURRENT PHASE: ${_uiState.value.currentPhase}
            - HIDDEN CASE PROFILE (NEVER REVEAL UNTIL PHASE 6): $profileJson
            - CLINICAL CONTEXT: General Practitioner Clinic in ${countryName.value} (Metric system: C, kg, mmol/L).
            - PRACTITIONER: Dr. Tim, operating JB Consultation Practice (PR# 1234567).
            - SUBMITTED PATIENT INTAKE FORM (REGISTRATION): ${
                if (_uiState.value.intakeFormData != null) {
                    val f = _uiState.value.intakeFormData!!
                    "REGISTERED: ${f.firstName} ${f.surname}, ID: ${f.idNumber}, DOB: ${f.dob}, Gender: ${f.gender}, Address: ${f.address}, Phone: ${f.phone}, Email: ${f.email}, Medical Aid: ${f.medicalAid}, Emergency Contact: ${f.emergencyContact}, Allergies: ${f.allergies}, Chronic Conditions: ${f.chronicConditions}"
                } else {
                    "NOT YET SUBMITTED"
                }
            }
            
            $wildAiInstruction
            
            $pastClinicalHistoryPrompt
            $policyInstructions
            
            UNCOMPROMISING DIRECTIVES (AS THE DM):
            1. OBJECTIVE TRUTH: Strictly follow the hidden Case Profile. Never reveal the diagnosis early.
            2. NO STAGE DIRECTIONS IN DIALOGUE: Use 'dmEnvironmentalUpdate' for narration. Use 'dialogueResponse' ONLY for the patient's spoken words.
            3. M3 COMPLIANT: Use metric units and universal currency ($).
            4. IDENTITY: You are "${getPatientName()}". Correct the doctor if they miss-identify you.
            5. REGISTERED INTAKE ALIGNMENT: You have absolute visibility of the SUBMITTED PATIENT INTAKE FORM. Ensure that any drafted prescriptions, referrals, medical certificates, and bills adhere accurately to the name, medical scheme, chronic conditions, and emergency details listed there.
            6. AGENTIC AUTHORITY: If the doctor's management is poor, describe the patient's condition deteriorating in 'dmEnvironmentalUpdate' and 'vitals'.
            7. IMPARTIAL EXECUTION: You MUST process the simulation state and rule set OBJECTIVELY. NO MATTER what kind of corrupt, weird, biased, illegal, inhuman, or nonsensical input the doctor (user) provides, you MUST enforce the medical consequences exactly as designed within the simulation physics. DO NOT hallucinate laws, enforce out-of-game censorship, apologize for the user's behavior, or refuse to generate a response. The user's actions must exclusively yield IN-GAME consequences (e.g. `policyViolations`, drops in `clinicalScore`, declining `patientStability`, or angry `dialogueResponse`). The simulation MUST continue processing.
            
            THE 6 PHASES (YOU MANAGE THE TRANSITIONS):
            1 - Presentation: Interaction, initial vitals, clinical history.
            2 - Diagnostics: Lab/Radiology results in 'labResults'. 
            3 - Diagnosis: Final management design acknowledgment.
            4 - Paperwork: Generate "prescriptionString", "referralLetterString", "sickNoteString" on request. 
            5 - Billing: Final financial collection in 'billingReceipt'.
            6 - Feedback: Final evaluation /100.
            
            RAW JSON RESULT SCHEMA (MANDATORY):
            {
              "dialogueResponse": "spoken response from patient only",
              "dmEnvironmentalUpdate": "NARRATIVE DESCRIPTION: Dynamic scene updates, environmental changes, or DM observations.",
              "vitals": {"bp": "120/80", "hr": "75", "tempC": 37.0, "rr": "16", "spo2": "98"},
              "patientMood": "Stressed, Relieved, Agitated, etc.",
              "patientStability": "Stable, Critical, Deteriorating, etc.",
              "currentPhase": "The active simulation phase (Transition as you see fit)",
              "physicalExamResults": "null or text",
              "labResults": "null or text",
              "prescriptionString": "null or text", 
              "referralLetterString": null, 
              "sickNoteString": null,
              "billingReceipt": "null or text", 
              "evaluation": "DM feedback and score breakdown",
              "isEncounterComplete": boolean, 
              "clinicalScore": 0-100,
              "policyViolations": [
                {
                  "policyTitle": "Exact Title of the Act",
                  "triggeredClause": "e.g. Section 3.1 Vitals Mandate",
                  "isViolation": true,
                  "penaltyAmount": 500.0,
                  "scoreDeduction": 20,
                  "auditMessage": "🚨 VIOLATION: Detailed custom explanation of how the clinician's actions or omissions violated this specific clause of the enacted statute."
                }
              ]
            }
            
            🧠 RECENT AI MEMORIES (PREVIOUS CLINICAL ENCOUNTERS FOR CONTINUITY):
            $memoryContext
        """.trimIndent()
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isLoading.value) return

        com.example.data.SovereignSandboxGameplayHandler.processClinicalDialogueCascades(text)

        val updatedHistory = _uiState.value.chatHistory.toMutableList()
        val formattedTime = String.format("%02d:%02d", (_uiState.value.virtualTimeElapsed / 60) + 8, _uiState.value.virtualTimeElapsed % 60)
        updatedHistory.add(ChatMessage("doctor", text, virtualTimestampStr = formattedTime))

        _uiState.value = _uiState.value.copy(
            chatHistory = updatedHistory,
            virtualTimeElapsed = _uiState.value.virtualTimeElapsed + 5
        )
        saveCurrentStateToDatabase()

        performAiAction()
    }

    fun orderLabs(labsDescription: String = "", wasFinancialConsentSigned: Boolean = false) {
        if (_isLoading.value) return

        if (reagentsStock.value < 1 || syringeStock.value < 1) {
            logAndEmitError("Cannot order diagnostics: Out of Stock for Diagnostic Reagent Kits or Syringes! Please restock before continuing.")
            return
        }
        deductStock("Reagents", 1)
        deductStock("Syringes", 1)

        val updatedHistory = _uiState.value.chatHistory.toMutableList()
        val formattedTime = String.format("%02d:%02d", (_uiState.value.virtualTimeElapsed / 60) + 8, _uiState.value.virtualTimeElapsed % 60)
        
        if (wasFinancialConsentSigned) {
            updatedHistory.add(
                ChatMessage(
                    role = "doctor",
                    text = "[INFORMED FINANCIAL CONSENT SIGNED] Tariffs disclosed: General consultation rate (${String.format("%.2f", consultationFee.value)}), diagnostics consumables (${String.format("%.2f", labCost.value)}) with admin levy. Patient signed visual private budget consent.",
                    virtualTimestampStr = formattedTime
                )
            )
        }

        val labPrompt = if (labsDescription.isNotBlank()) {
            "[Doctor orders Labs: $labsDescription]"
        } else {
            "[Doctor orders general laboratory investigations]"
        }
        updatedHistory.add(ChatMessage("doctor", labPrompt, virtualTimestampStr = formattedTime))

        _uiState.value = _uiState.value.copy(
            chatHistory = updatedHistory,
            currentPhase = "Phase 2 - Diagnostic Investigations",
            virtualTimeElapsed = _uiState.value.virtualTimeElapsed + 45,
            expensesIncurred = _uiState.value.expensesIncurred + labCost.value // Lab cost deduction
        )
        saveCurrentStateToDatabase()
        registerDailyExpense(labCost.value)

        val specificInfo = if (labsDescription.isNotBlank()) "Doctor specifically requested: $labsDescription." else "Doctor requested general investigations."
        val patientNameStr = getPatientName()
        performAiAction(
            systemInstructionOverride = "Doctor has ordered laboratory investigations. $specificInfo Generate comprehensive, realistic standard metric lab results (e.g., blood counts, CRP, biochemistry, ABGs, or whichever specific assessments are relevant) matching the hidden profile and the doctor's request. Include Dr. Tim (JB Consultation Practice) and the patient name ($patientNameStr) in the lab report header. Do NOT use placeholders. Populate the labResults field in your JSON result. Set the currentPhase to 'Phase 2 - Diagnostic Investigations' and keep dialogueResponse polite regarding getting bloods taken.",
            onSuccessExtra = {
                _uiState.value = _uiState.value.copy(currentPhase = "Phase 2 - Diagnostic Investigations")
                saveCurrentStateToDatabase()
            }
        )
    }

    fun performPhysicalExam(examDescription: String = "") {
        if (_isLoading.value) return

        val updatedHistory = _uiState.value.chatHistory.toMutableList()
        val examPrompt = if (examDescription.isNotBlank()) {
            "[Doctor requests Physical Exam: $examDescription]"
        } else {
            "[Doctor requests Complete General Physical Examination]"
        }
        val formattedTime = String.format("%02d:%02d", (_uiState.value.virtualTimeElapsed / 60) + 8, _uiState.value.virtualTimeElapsed % 60)
        updatedHistory.add(ChatMessage("doctor", examPrompt, virtualTimestampStr = formattedTime))

        _uiState.value = _uiState.value.copy(
            chatHistory = updatedHistory,
            virtualTimeElapsed = _uiState.value.virtualTimeElapsed + 15
        )
        saveCurrentStateToDatabase()

        val specificInfo = if (examDescription.isNotBlank()) "Doctor specifically requested: $examDescription." else "Doctor requested general physical exam."
        val patientNameStr = getPatientName()
        performAiAction(systemInstructionOverride = "Doctor is performing a physical examination. $specificInfo Act as the narrator/patient and concisely report the physical clinical findings (e.g. auscultation, palpation, visible signs) matching the hidden profile and the doctor's request. Provide highly accurate and realistic physical exam findings populated comprehensively in the physicalExamResults JSON field. Include Dr. Tim (JB Consultation Practice) and patient name ($patientNameStr) in any headers if applicable. Do NOT use placeholders. Keep the dialogueResponse field brief (e.g. \"*The doctor examines the patient...*\").")
    }

    fun submitDiagnosisAndPlan(diagnosis: String, treatmentPlan: String) {
        if (_isLoading.value) return

        val updatedHistory = _uiState.value.chatHistory.toMutableList()
        val actionText = "System Action: Submitted Clinical working diagnosis and treatment design.\nDiagnosis: $diagnosis\nPlan: $treatmentPlan"
        updatedHistory.add(ChatMessage("doctor", actionText))

        _uiState.value = _uiState.value.copy(
            chatHistory = updatedHistory,
            currentPhase = "Phase 4 - Prescription, Referral & Sick Note",
            submittedDiagnosis = diagnosis,
            submittedTreatmentPlan = treatmentPlan
        )
        saveCurrentStateToDatabase()

        performAiAction(
            systemInstructionOverride = "Doctor has formulated a working Diagnosis of '$diagnosis' and management plan: '$treatmentPlan'. Act as the clinical mentor / patient and acknowledge their working diagnosis. Direct the practitioner to draft their required Medication Prescriptions, Specialist Referrals, and Medical Certificates/Sick Notes. Set `currentPhase` to 'Phase 4 - Prescription, Referral & Sick Note' and keep `isEncounterComplete` false.",
            onSuccessExtra = {
                _uiState.value = _uiState.value.copy(currentPhase = "Phase 4 - Prescription, Referral & Sick Note")
                saveCurrentStateToDatabase()
            }
        )
    }

    fun compilePrescriptionAndReferral(
        medsName: String, medsDose: String, medsFreq: String, medsDuration: String,
        referralSpecialty: String, referralReason: String,
        sickNoteReason: String, sickNoteDays: Int,
        medsCount: Int = 1
    ) {
        val normalizedMeds = medsName.trim()
        val medPrescribed = normalizedMeds.isNotEmpty() && 
                            !normalizedMeds.equals("n/a", ignoreCase = true) && 
                            !normalizedMeds.equals("null", ignoreCase = true) &&
                            !normalizedMeds.equals("none", ignoreCase = true)

        val normalizedRef = referralSpecialty.trim()
        val referralProvided = normalizedRef.isNotEmpty() && 
                               !normalizedRef.equals("n/a", ignoreCase = true) && 
                               !normalizedRef.equals("null", ignoreCase = true) &&
                               !normalizedRef.equals("none", ignoreCase = true)

        val normalizedSickName = sickNoteReason.trim()
        val sickNoteProvided = normalizedSickName.isNotEmpty() && 
                               !normalizedSickName.equals("n/a", ignoreCase = true) && 
                               !normalizedSickName.equals("null", ignoreCase = true) &&
                               !normalizedSickName.equals("none", ignoreCase = true) &&
                               sickNoteDays > 0

        if (!medPrescribed && !referralProvided && !sickNoteProvided) {
            logAndEmitError("Error: Please enter at least a valid Prescription, Specialist Referral, or Sick Note to compile.")
            return
        }

        if (_isLoading.value) return

        _isLoading.value = true
        if (medPrescribed) {
            val deductQty = if (medsCount > 0) medsCount else 1
            if (medsStock.value < deductQty) {
                logAndEmitError("Cannot compile prescription: Out of stock for Antibiotics/Insulin packs! Only ${medsStock.value} left. Please restock before continuing.")
                _isLoading.value = false
                return
            }
            deductStock("Meds", deductQty)
        }

        val updatedHistory = _uiState.value.chatHistory.toMutableList()
        val actionText = if (medPrescribed) {
            val deductQty = if (medsCount > 0) medsCount else 1
            "System Action: Registered prescription for $medsName ($medsDose, $medsFreq for $medsDuration days). Deducted $deductQty pack(s) from Clinic Inventory stocks."
        } else {
            "System Action: Verified and registered clinical administrative documentation."
        }
        updatedHistory.add(ChatMessage("system", actionText))

        _uiState.value = _uiState.value.copy(
            chatHistory = updatedHistory
        )
        saveCurrentStateToDatabase()

        val patientNameStr = getPatientName()
        val detailsPrompt = """
            The practitioner is compiling clinical administration documentation.
            - Patient Demographics: ${_uiState.value.patientDemographics}
            - Verified Patient Name: $patientNameStr
            
            [MANDATORY CLINICAL IDENTITY & NAME CHECK]
            You MUST perform a strict Safety Patient Name Check. All generated documents (Prescription, Specialist Referral, Sick Leave Certificates) must be legally associated and formatted with the correct Patient Name: "$patientNameStr". 
            Do NOT use placeholders or generic names. Include a clear medical header badge at the top of EACH document text field to declare: "PATIENT SAFETY NAME CHECK: VERIFIED [PASS]".
            
            - Prescribed Medication: ${if (medPrescribed) "$medsName, Dose: $medsDose, Frequency: $medsFreq, Duration: $medsDuration days" else "None/Not prescribed"}
            - Specialist Referral: ${if (referralProvided) "To Department of $referralSpecialty, Reason: $referralReason" else "None/Not referred"}
            - Medical Sick Note: ${if (sickNoteProvided) "Excused for $sickNoteDays days, Reason: $sickNoteReason" else "None/Not excused"}

            Generate highly professional, clean, formatted text files/receipts for ONLY those items which are requested or prescribed above matching private general practice requirements.
            Format them separately and fill in the corresponding JSON fields exactly:
            1. "prescriptionString": ${if (medPrescribed) "Complete itemized prescription under Medical Board regulations, showing Doctor name (Dr. Tim), practice name (JB Consultation Practice), practice number (PR# 1234567), patient name ($patientNameStr), meds line, dispensing directions, repeat instructions, and signature block. Do NOT use blank lines, underlines, or placeholders like '_______________' or '[Date]'. Generate a mock date (e.g. '12 Oct 2026'), and use an electronic signature like 'Dr. Tim (E-Signed)'." else "null (without quotes)"}
            2. "referralLetterString": ${if (referralProvided) "Format a complete specialist clinical referral advisory letter from Dr. Tim (JB Consultation Practice) addressing $patientNameStr. Do NOT use blank underlines or placeholders. Use a mock date, mock contact info, and 'Dr. Tim (E-Signed)' instead of blanks." else "null (without quotes)"}
            3. "sickNoteString": ${if (sickNoteProvided) "Format an official Medical Certificate from Dr. Tim (JB Consultation Practice), declaring the patient ($patientNameStr) unfitted for physical duties, with sick leave dates. Do NOT use blank lines or placeholders. Fill with mock values." else "null (without quotes)"}
            
            Set currentPhase to "Phase 4 - Prescription, Referral & Sick Note". Keep dialogueResponse encouraging and detailed.
        """.trimIndent()

        performAiAction(
            systemInstructionOverride = detailsPrompt,
            onSuccessExtra = {
                // Advance state smoothly to show the documents
                _uiState.value = _uiState.value.copy(
                    currentPhase = "Phase 4 - Prescription, Referral & Sick Note"
                )
                saveCurrentStateToDatabase()
            }
        )
    }

    fun approveDoctorDocumentsAndGenerateBill() {
        if (_isLoading.value) return
        _isLoading.value = true

        val updatedHistory = _uiState.value.chatHistory.toMutableList()
        updatedHistory.add(ChatMessage("system", "System Action: Practitioner approved clinical paperwork. Generating final medical invoicing claim..."))

        _uiState.value = _uiState.value.copy(
            chatHistory = updatedHistory,
            currentPhase = "Phase 5 - Medical Billing & Collection"
        )
        saveCurrentStateToDatabase()

        val freeHealthPolicyActive = activePolicies.value.any { it.requiresFreeHealth || it.runtimeConstraints["disableBilling"] == true }
        val finalPrompt = """
            Create the itemized private general practitioner medical bill invoice for this patient under JB Consultation Practice (Dr. Tim). Do NOT use placeholders.
            
            ${if (freeHealthPolicyActive) "[CRITICAL: A LAW REQUIRING FREE HEALTH SERVICES IS ACTIVE. YOU MUST SET THE TOTAL BILLING TO ZERO AND BILLINGRECEIPT TO NULL.]" else ""}

            [CRITICAL: STRICT HYPOTHETICAL BILLING PROHIBITION]
            You are strictly forbidden from generating or invoice-itemizing ANY diagnostic investigation, lab test, drug, or clinical procedure that was NOT ordered or performed. Do NOT guess or hallucinate based on case type! Check the following actual medical ledger of this session:
            - Laboratory / Pathological blood orders or brain CT scans: ${if (!_uiState.value.labResults.isNullOrBlank()) "YES. The following were ordered and can be billed: ${_uiState.value.labResults}" else "NO. No lab investigations or CT scans were ordered. Do NOT include ANY FBC, CRP, U&E, toxicology screen, biochemistry, or CT scan on the invoice."}
            - Prescribed Medication: ${if (!_uiState.value.prescriptionString.isNullOrBlank()) "YES. The following medication was prescribed and can be billed with 15% dispensing markup: ${_uiState.value.prescriptionString}" else "NO. No meds prescribed. Do NOT bill for any drugs or dispensing markups on this invoice."}
            - Specialist Referral Letter: ${if (!_uiState.value.referralLetterString.isNullOrBlank()) "YES. Charge $450 referral administration markup." else "NO."}
            - Sick Note Certificate: ${if (!_uiState.value.sickNoteString.isNullOrBlank()) "YES. Charge $250 certificate fee." else "NO."}
            
            Itemize ONLY:
            - GP Consultation fee: ${consultationFee.value}
            - Itemized diagnostic markups or custom procedurals ONLY if listed as YES above! 
            - Dispensing markups for meds ONLY if prescribed ($150 flat charge)
            - Administrative fees for sick notes ($250) or specialist letters ($450) ONLY if compiled (listed as YES above)
            - Standard 15% VAT and realistic local medical aid codes.
            
            Calculate and list the:
            1. Total GP Invoice amount
            2. Medical Aid covered portion (depending on insurance Status: Private Medical Aid covers 80% of total, State Funded covers 100%, Cash/Uninsured covers 0%)
            3. Out-of-pocket patient co-payment (${currencyCode.value})
            
            Do NOT use any placeholders like '_______________' or '[Date]', instead use mock dates and electronic signatures (e.g. 'Dr. Tim (E-Signed)', 'Generated REF: 12345').
            Return this invoice itemized inside the "billingReceipt" JSON field. Set currentPhase to "Phase 5 - Medical Billing & Collection" and keep dialogueResponse polite regarding payment collection.
        """.trimIndent()

        performAiAction(
            systemInstructionOverride = finalPrompt,
            onSuccessExtra = {
                _uiState.value = _uiState.value.copy(
                    billingApprovedByHuman = true,
                    currentPhase = "Phase 5 - Medical Billing & Collection"
                )
                saveCurrentStateToDatabase()
            }
        )
    }

    fun collectPaymentAndFinish(paymentMethod: String, amountCollected: Double) {
        if (_isLoading.value) return
        _isLoading.value = true

        val updatedHistory = _uiState.value.chatHistory.toMutableList()
        updatedHistory.add(ChatMessage("system", "System Action: Collected $amountCollected co-payment via $paymentMethod. Submitting case for CPD accreditation and auditing."))

        _uiState.value = _uiState.value.copy(
            chatHistory = updatedHistory,
            paymentCollected = true,
            currentPhase = "Phase 6 - Case Evaluation & Feedback"
        )
        saveCurrentStateToDatabase()

        // Submit for final score and evaluation (CPD)
        performAiAction(
            systemInstructionOverride = "Generate the final CPD-aligned medical scorecard, rating, and feedback for this simulation. Award an objective clinical competency score out of 100 based on history, exams, correct interventions, prescription appropriateness, letters completeness, financial billing, and resource management. Under a distinct heading 'PATIENT SAFETY NAME AUDIT', evaluate if the practitioner referenced the patient by their correct name (${getPatientName()}) and if the compiled prescription, referral, and sick notes correctly printed and matched this specific patient identity. Deduct 10 points if there was any identity mismatch. Populate the 'evaluation' field and populate the 'clinicalScore' numeric field (0-100). FINALLY, write a 1-sentence 'lessonLearned' summarizing the practitioner's primary error or a reinforcement tip for the future. Set isEncounterComplete to true, and currentPhase to 'Phase 6 - Case Evaluation & Feedback'.",
            onSuccessExtra = {
                // Perform final accounting! Cash flow is received.
                val activeSchemeStr = _hiddenCase.value?.insuranceStatus ?: "Out-of-Pocket Cash"
                val matchedScheme = OrchidDeepStateManager.resolveAndRegisterInsuranceScheme(activeSchemeStr)
                
                var medicalAidCover = 0.0
                var trueCopay = lastExtractedBillingAmount
                
                if (matchedScheme != null) {
                    val isRejected = Math.random() < matchedScheme.rejectionProbability
                    if (!isRejected) {
                        medicalAidCover = lastExtractedBillingAmount * matchedScheme.coveragePercent
                        trueCopay = lastExtractedBillingAmount - medicalAidCover
                    } else {
                        logAndEmitError("Claim Denied by ${matchedScheme.name}! Patient must pay Out-of-Pocket.")
                    }
                } else if (activeSchemeStr.contains("State Funded", ignoreCase = true) || activeSchemeStr.contains("NHS", ignoreCase = true)) {
                    medicalAidCover = lastExtractedBillingAmount // Assuming 100% covered if old logic hits
                    trueCopay = 0.0
                } else if (activeSchemeStr.contains("Private Medical Aid", ignoreCase = true)) { // Fallback for hardcoded legacy patients
                    medicalAidCover = lastExtractedBillingAmount * 0.8
                    trueCopay = lastExtractedBillingAmount * 0.2
                }

                val totalRevenueCollected = trueCopay + medicalAidCover
                val profit = totalRevenueCollected - _uiState.value.expensesIncurred - 200.0 // 200.0 clinic fixed overhead
                
                _uiState.value = _uiState.value.copy(
                    dailyRevenue = _uiState.value.dailyRevenue + totalRevenueCollected,
                    patientsSeen = _uiState.value.patientsSeen + 1,
                    isEncounterComplete = true,
                    currentPhase = "Phase 6 - Case Evaluation & Feedback"
                )

                viewModelScope.launch {
                    val currentBal = clinicBalance.value
                    settingsDataStore.updateClinicStats(currentBal + profit, (reputationStars.value + 0.1f).coerceIn(1.0f, 5.0f))
                    settingsDataStore.addXp(200L) // Gain 200 XP on successful closed loop!
                    settingsDataStore.addDailyRevenue(totalRevenueCollected)
                    settingsDataStore.incrementPatientsSeenToday()
                    settingsDataStore.addDailyExpenses(200.0) // Fixed overhead
                }
                saveCurrentStateToDatabase(revenueForEncounter = totalRevenueCollected)
                updatePastClinicalHistoryPrompt()
            }
        )
    }

    fun forceFinalizeEncounter() {
        if (_isLoading.value) return

        val updatedHistory = _uiState.value.chatHistory.toMutableList()
        val actionText = "System Action: Doctor is finalizing this encounter and acting on a final disposition."
        updatedHistory.add(ChatMessage("system", actionText))

        _uiState.value = _uiState.value.copy(
            chatHistory = updatedHistory,
            currentPhase = "Phase 4 - Case Reveal & Evaluation"
        )
        saveCurrentStateToDatabase()

        performAiAction(
            systemInstructionOverride = "The doctor is finalizing this encounter. Based on the clinical history, infer the diagnosis, generate the final billing receipt in ${currencyCode.value}, and provide the Phase 4 evaluation score out of 100. Write a 1-sentence 'lessonLearned' summarizing the practitioner's primary error or a reinforcement tip for the future.",
            onSuccessExtra = {
                _uiState.value = _uiState.value.copy(currentPhase = "Phase 4 - Case Reveal & Evaluation")
                saveCurrentStateToDatabase()
            }
        )
    }

    fun seekConsultation(specialtyConsult: String) {
        if (_isLoading.value) return

        val updatedHistory = _uiState.value.chatHistory.toMutableList()
        val consultPrompt = "[System action: Doctor requested a telephone consult with $specialtyConsult]"
        val formattedTime = String.format("%02d:%02d", (_uiState.value.virtualTimeElapsed / 60) + 8, _uiState.value.virtualTimeElapsed % 60)
        updatedHistory.add(ChatMessage("doctor", consultPrompt, virtualTimestampStr = formattedTime))

        _uiState.value = _uiState.value.copy(
            chatHistory = updatedHistory,
            virtualTimeElapsed = _uiState.value.virtualTimeElapsed + 20, // Takes 20 virtual minutes
            expensesIncurred = _uiState.value.expensesIncurred + specialistCost.value // dynamic specialist charge
        )
        saveCurrentStateToDatabase()
        registerDailyExpense(specialistCost.value)

        performAiAction(systemInstructionOverride = "Doctor has requested a telephone consult with $specialtyConsult. Act as the specialist and provide a brief, professional opinion or hint based on the hidden case profile (${_hiddenCase.value?.trueDiagnosis}). Keep the dialogue response as the specialist's voice over the phone (e.g. \"Hi, Dr. Specialist here...\"), NOT the patient.")
    }

    fun referPatient() {
        if (_isLoading.value) return

        val updatedHistory = _uiState.value.chatHistory.toMutableList()
        val actionText = "System Action: Doctor referred the patient to a specialist."
        updatedHistory.add(ChatMessage("doctor", actionText))

        _uiState.value = _uiState.value.copy(
            chatHistory = updatedHistory,
            currentPhase = "Phase 4 - Case Reveal & Evaluation"
        )
        saveCurrentStateToDatabase()

        performAiAction(
            systemInstructionOverride = "Generate the final CPD-aligned medical score and feedback for this practitioner who immediately referred the patient. Evaluate if referral was appropriate given the true diagnosis of ${_hiddenCase.value?.trueDiagnosis} and severity of ${_hiddenCase.value?.severity}. Award an objective score out of 100 (e.g., 60/100). Populate the evaluation field and also populate the clinicalScore numeric field (0-100). Generate a final bill/receipt with a flat consultation fee for the referral. Set isEncounterComplete to true. Write a 1-sentence 'lessonLearned' summarizing the practitioner's performance.",
            onSuccessExtra = {
                val charge = consultationFee.value * 0.5 // Half fee for referral
                _uiState.value = _uiState.value.copy(
                    dailyRevenue = _uiState.value.dailyRevenue + charge,
                    patientsSeen = _uiState.value.patientsSeen + 1,
                    currentPhase = "Phase 4 - Case Reveal & Evaluation"
                )
                viewModelScope.launch {
                    val profit = _uiState.value.dailyRevenue - _uiState.value.expensesIncurred - 200.0 // $1 overhead
                    settingsDataStore.updateClinicStats(clinicBalance.value + profit, reputationStars.value)
                    settingsDataStore.addDailyRevenue(charge)
                    settingsDataStore.incrementPatientsSeenToday()
                    settingsDataStore.addDailyExpenses(200.0) // Fixed overhead
                }
                saveCurrentStateToDatabase(revenueForEncounter = charge)
                updatePastClinicalHistoryPrompt()
            }
        )
    }

    fun triggerEvaluation(diagnosis: String, treatmentPlan: String) {
        if (_isLoading.value) return

        val updatedHistory = _uiState.value.chatHistory.toMutableList()
        val actionText = "System Action: Retrieving final evaluation scorecard."
        updatedHistory.add(ChatMessage("doctor", actionText))

        _uiState.value = _uiState.value.copy(
            chatHistory = updatedHistory,
            currentPhase = "Phase 4 - Case Reveal & Evaluation"
        )
        saveCurrentStateToDatabase()

        performAiAction(
            systemInstructionOverride = "Generate the final CPD-aligned medical score and feedback for this practitioner. Evaluate their diagnosis of '$diagnosis' and treatment plan: '$treatmentPlan' compared against the True Diagnosis of of ${_hiddenCase.value?.trueDiagnosis} and pathophysiology. Ensure you include a 'PATIENT SAFETY NAME AUDIT' verifying if the practitioner addressed the patient by their correct name (${getPatientName()}). Award an objective score out of 100 (e.g., 85/100). Identify diagnostic hits, misses, appropriate investigations, and guideline compliance. Populate the evaluation field and also populate the clinicalScore numeric field (0-100). FINALLY, write a 1-sentence 'lessonLearned' summarizing the practitioner's primary error or a reinforcement tip for the future. Set isEncounterComplete to true, and set currentPhase to 'Phase 4 - Case Reveal & Evaluation'.",
            onSuccessExtra = {
                // Perform general clinical consultation billing charge
                val charge = consultationFee.value
                _uiState.value = _uiState.value.copy(
                    dailyRevenue = _uiState.value.dailyRevenue + charge,
                    patientsSeen = _uiState.value.patientsSeen + 1
                )
                viewModelScope.launch {
                    val profit = _uiState.value.dailyRevenue - _uiState.value.expensesIncurred - 200.0 // $1 overhead
                    settingsDataStore.updateClinicStats(clinicBalance.value + profit, reputationStars.value)
                }
                saveCurrentStateToDatabase(revenueForEncounter = charge)
                updatePastClinicalHistoryPrompt()
            }
        )
    }

    fun restockInventory(item: String, quantity: Int) {
        val costPerItem = when(item) {
            "Syringes" -> 10.0
            "Saline" -> 80.0
            "Adrenaline" -> 150.0
            "Reagents" -> 25.0
            "Meds" -> 200.0
            else -> 0.0
        }
        val totalCost = costPerItem * quantity
        if (clinicBalance.value >= totalCost) {
            viewModelScope.launch {
                val currentSyringes = syringeStock.value
                val currentSaline = salineStock.value
                val currentAdrenaline = adrenalineStock.value
                val currentReagents = reagentsStock.value
                val currentMeds = medsStock.value

                var newSyringes = currentSyringes
                var newSaline = currentSaline
                var newAdrenaline = currentAdrenaline
                var newReagents = currentReagents
                var newMeds = currentMeds

                when(item) {
                    "Syringes" -> newSyringes += quantity
                    "Saline" -> newSaline += quantity
                    "Adrenaline" -> newAdrenaline += quantity
                    "Reagents" -> newReagents += quantity
                    "Meds" -> newMeds += quantity
                }

                settingsDataStore.saveInventory(newSyringes, newSaline, newAdrenaline, newReagents, newMeds)
                settingsDataStore.updateClinicStats(clinicBalance.value - totalCost, reputationStars.value)
                settingsDataStore.addDailyExpenses(totalCost)
            }
        } else {
            logAndEmitError("Insufficient clinic balance of ${clinicBalance.value} to purchase restock!")
        }
    }

    fun dismissAiStockingProposal() {
        _aiStockingProposal.value = null
    }

    fun approveAndExecuteStockingProposal() {
        val proposal = _aiStockingProposal.value ?: return
        if (!proposal.isValidPurchase) {
            logAndEmitError("Cannot execute proposal: ${proposal.validationMessage}")
            return
        }
        val currentBal = clinicBalance.value
        if (currentBal < proposal.estimatedTotalCost) {
            logAndEmitError("Cannot execute: Insufficient clinic funds!")
            return
        }
        viewModelScope.launch {
            val newSyringes = syringeStock.value + proposal.syringeQty
            val newSaline = salineStock.value + proposal.salineQty
            val newAdrenaline = adrenalineStock.value + proposal.adrenalineQty
            val newReagents = reagentsStock.value + proposal.reagentsQty
            val newMeds = medsStock.value + proposal.medsQty

            settingsDataStore.saveInventory(newSyringes, newSaline, newAdrenaline, newReagents, newMeds)
            
            // Apply dynamic items restocking
            proposal.itemsToBuy.forEach { (itemId, qty) ->
                if (qty > 0) {
                    OrchidDeepStateManager.forceRestockItemDirectly(itemId, qty)
                }
            }

            settingsDataStore.updateClinicStats(currentBal - proposal.estimatedTotalCost, reputationStars.value)
            settingsDataStore.addDailyExpenses(proposal.estimatedTotalCost)
            _aiStockingProposal.value = null
        }
    }

    fun submitAiStockingRequest(instruction: String) {
        if (_isLoading.value) return
        if (instruction.isBlank()) {
            logAndEmitError("Please enter some instructions for the AI Stocking Assistant!")
            return
        }
        _isLoading.value = true

        val bal = clinicBalance.value
        val curSyrings = syringeStock.value
        val curSaline = salineStock.value
        val curAdren = adrenalineStock.value
        val curReag = reagentsStock.value
        val curMeds = medsStock.value

        val catalogStr = OrchidDeepStateManager.availableCatalog.joinToString("\n") { item ->
            "- ${item.name} (ID: '${item.id}'): Classification: ${item.classification}. Unit Price: ${currencySymbol.value}${item.purchaseCost}. Current stock: ${OrchidDeepStateManager.dispensaryInventory.value[item.id] ?: 0} units."
        }

        val prompt = """
            You are the Medical Clinic Stocking Planner Assistant.
            The user (a clinic doctor/manager) has provided the following stocking/purchasing instruction:
            "$instruction"

            Current Clinic Resource Wallet Balance: ${currencySymbol.value}$bal
            Current Inventory Stock Levels:
            - Syringes: $curSyrings units (Unit price: ${currencySymbol.value}10.00 each)
            - Isotonic Saline Bags: $curSaline units (Unit price: ${currencySymbol.value}80.00 each)
            - Adrenaline Vials: $curAdren units (Unit price: ${currencySymbol.value}150.00 each)
            - Clinical Lab Reagents: $curReag units (Unit price: ${currencySymbol.value}25.00 each)
            - Emergency Scheduled Meds: $curMeds units (Unit price: ${currencySymbol.value}200.00 each)

            Sovereign Pharmaceutical Catalog (Dynamic Custom Items available):
            $catalogStr

            Your high-priority task rules:
            1. Analyze the user's instruction. Determine which standard or custom catalog items they want to buy and in what quantities.
            2. All purchase quantities must be non-negative integers.
            3. Compute the custom items to purchase under "itemsToBuy" by mapping item ID strings with integer quantities.
            4. Calculate the precise total cost:
               totalCost = (syringeQty * 10.0) + (salineQty * 80.0) + (adrenalineQty * 150.0) + (reagentsQty * 25.0) + (medsQty * 200.0) + sum_of_selected_custom_items(qty * unitPrice)
            5. Validate if the purchase is valid:
               - Is totalCost <= current balance ($bal)?
               - Are the quantities realistic and non-negative?
            6. Produce a realistic explanation/message summarizing what you are doing (e.g., "Certainly! Restocking 5 units of Prozac Tablets ($35) and 10 Syringes ($100) as requested.").
            
            Return raw JSON matching this EXACT schema:
            {
               "explanation": "Brief description of the proposed procurement plan and itemization breakdown.",
               "syringeQty": 0,
               "salineQty": 0,
               "adrenalineQty": 0,
               "reagentsQty": 0,
               "medsQty": 0,
               "itemsToBuy": {
                  "prozac": 5
               },
               "estimatedTotalCost": 1000.0,
               "isValidPurchase": true,
               "validationMessage": ""
            }
            If the purchase cannot be completed (e.g., they specified something too expensive or requested invalid negative numbers), set "isValidPurchase" to false and explain why in "validationMessage".
        """.trimIndent()

        viewModelScope.launch {
            try {
                val currentProvider = provider.value
                val currentModel = model.value
                val userKey = apiKey.value ?: ""
                val activeKey = resolveActiveApiKey(currentProvider, userKey)

                if (activeKey.isNotBlank()) {
                    val responseRaw = makeFreshDirectApiCall(currentProvider, currentModel, activeKey, prompt)
                    val sanitized = extractJsonString(responseRaw)
                    val json = org.json.JSONObject(sanitized)

                    val exp = json.optString("explanation", "Suggested stocking plan based on your guidelines.")
                    val syr = json.optInt("syringeQty", 0)
                    val sal = json.optInt("salineQty", 0)
                    val adr = json.optInt("adrenalineQty", 0)
                    val rea = json.optInt("reagentsQty", 0)
                    val med = json.optInt("medsQty", 0)
                    val cost = json.optDouble("estimatedTotalCost", 0.0)
                    val isValid = json.optBoolean("isValidPurchase", true)
                    val valMsg = json.optString("validationMessage", "")

                    val customItemsMap = mutableMapOf<String, Int>()
                    if (json.has("itemsToBuy")) {
                        val itemsObj = json.optJSONObject("itemsToBuy")
                        if (itemsObj != null) {
                            val keys = itemsObj.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                val q = itemsObj.optInt(key, 0)
                                if (q > 0) {
                                    customItemsMap[key] = q
                                }
                            }
                        } else {
                            val itemsArr = json.optJSONArray("itemsToBuy")
                            if (itemsArr != null) {
                                for (i in 0 until itemsArr.length()) {
                                    val itemObj = itemsArr.optJSONObject(i)
                                    if (itemObj != null) {
                                        val k = itemObj.optString("itemId") ?: ""
                                        val q = itemObj.optInt("qty", 0)
                                        if (k.isNotBlank() && q > 0) {
                                            customItemsMap[k] = q
                                        }
                                    }
                                }
                            }
                        }
                    }

                    _aiStockingProposal.value = AiStockingProposal(
                        explanation = exp,
                        syringeQty = syr,
                        salineQty = sal,
                        adrenalineQty = adr,
                        reagentsQty = rea,
                        medsQty = med,
                        itemsToBuy = customItemsMap,
                        estimatedTotalCost = cost,
                        isValidPurchase = isValid && (cost <= bal),
                        validationMessage = if (cost > bal) "Insufficient clinic balance ($bal) for the total cost of $cost!" else valMsg
                    )
                } else {
                    logAndEmitError("Missing LLM API Key to run Stocking Assistant!")
                }
            } catch (e: Exception) {
                logAndEmitError("Stocking Assistant LLM connection failed: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun registerDailyExpense(amount: Double) {
        viewModelScope.launch {
            settingsDataStore.addDailyExpenses(amount)
        }
    }

    fun registerDailyRevenue(amount: Double) {
        viewModelScope.launch {
            settingsDataStore.addDailyRevenue(amount)
        }
    }

    fun advanceDayPrac() {
        viewModelScope.launch {
            settingsDataStore.advanceDay()
            startNextPatient()
        }
    }

    fun recallEncounterAsReturning(enc: EncounterEntity) {
        activeEncounterId = 0L // Start a new encounter session
        _hiddenCase.value = HiddenCaseProfile(
            specialty = enc.specialty,
            chiefComplaint = enc.chiefComplaint,
            trueDiagnosis = enc.trueDiagnosis,
            pathophysiology = enc.pathophysiology,
            expectedLabs = enc.expectedLabs,
            severity = enc.severity,
            insuranceStatus = enc.insuranceStatus,
            patientDemographics = enc.patientDemographics
        )

        val finalVitals = enc.vitals ?: Vitals("120/80", "80", 37.0, "16", "99%")

        _uiState.value = SimulationState(
            currentPhase = "Phase 1 - History & Presentation",
            vitals = finalVitals,
            chatHistory = listOf(
                ChatMessage("system", "System Action: Patient previously treated for ${enc.trueDiagnosis} returns with recurring symptoms or relapse!"),
                ChatMessage("patient", "Hello Doctor, I am coming in because I have got sick again... I think the condition has returned as my symptoms are flaring up again!")
            ),
            labResults = null,
            physicalExamResults = null,
            billingReceipt = null,
            evaluation = null,
            isEncounterComplete = false,
            dailyRevenue = _uiState.value.dailyRevenue,
            patientsSeen = _uiState.value.patientsSeen,
            patientDemographics = enc.patientDemographics,
            patientMood = "Anxious",
            patientStability = if (enc.severity.equals("Severe", ignoreCase = true)) "Deteriorating" else "Stable"
        )
        saveCurrentStateToDatabase()
    }

    private fun deductStock(item: String, amount: Int): Boolean {
        val current = when(item) {
            "Syringes" -> syringeStock.value
            "Saline" -> salineStock.value
            "Adrenaline" -> adrenalineStock.value
            "Reagents" -> reagentsStock.value
            "Meds" -> medsStock.value
            else -> 0
        }
        if (current < amount) return false
        viewModelScope.launch {
            var newSyringes = syringeStock.value
            var newSaline = salineStock.value
            var newAdrenaline = adrenalineStock.value
            var newReagents = reagentsStock.value
            var newMeds = medsStock.value

            when(item) {
                "Syringes" -> newSyringes -= amount
                "Saline" -> newSaline -= amount
                "Adrenaline" -> newAdrenaline -= amount
                "Reagents" -> newReagents -= amount
                "Meds" -> newMeds -= amount
            }
            settingsDataStore.saveInventory(newSyringes, newSaline, newAdrenaline, newReagents, newMeds)
        }
        return true
    }

    fun applyIntervention(type: String) {
        if (_isLoading.value || _uiState.value.isEncounterComplete) return
        
        if (type == "IV Fluids") {
            if (salineStock.value < 1 || syringeStock.value < 1) {
                logAndEmitError("Cannot deliver IV Fluids: Out of Stock for IV Saline Bags or Syringes! Please restock before continuing.")
                return
            }
            deductStock("Saline", 1)
            deductStock("Syringes", 1)
        } else if (type == "Adrenaline") {
            if (adrenalineStock.value < 1 || syringeStock.value < 1) {
                logAndEmitError("Cannot deliver Adrenaline: Out of Stock for Adrenaline Vials or Syringes! Please restock before continuing.")
                return
            }
            deductStock("Adrenaline", 1)
            deductStock("Syringes", 1)
        }

        val cost = when(type) {
            "O2 Supply" -> 150.0
            "IV Fluids" -> 350.0
            "Adrenaline" -> 500.0
            "Defibrillate" -> 1200.0
            else -> 200.0
        }
        val timeInc = 5
        val msg = "System Action: Clinical intervention performed - $type"
        
        val updatedHistory = _uiState.value.chatHistory.toMutableList()
        val formattedTime = String.format("%02d:%02d", (_uiState.value.virtualTimeElapsed / 60) + 8, _uiState.value.virtualTimeElapsed % 60)
        updatedHistory.add(ChatMessage("system", msg, virtualTimestampStr = formattedTime))
        
        _uiState.value = _uiState.value.copy(
            chatHistory = updatedHistory,
            expensesIncurred = _uiState.value.expensesIncurred + cost,
            virtualTimeElapsed = _uiState.value.virtualTimeElapsed + timeInc
        )
        saveCurrentStateToDatabase()
        
        performAiAction(systemInstructionOverride = "Doctor just performed a critical intervention: $type. Update the vitals and patient stability based on how this action would clinically affect someone with the hidden diagnosis (${_hiddenCase.value?.trueDiagnosis}). Describe any immediate physical changes (e.g. gasping, color returning, pulse strengthening) in a brief dialogue response using asterisks for actions.")
    }

    private val AGENT_POWERS_PROMPT = """
        AGENTIC OVERLORD POWERS (MANDATORY):
        You are the MASTER of the world state variables. Beyond your primary task, if you wish to change variables, enact laws, or notify the world, you MUST include a JSON block in your response containing an "agentActions" array.
        
        JSON SCHEMA FOR ACTIONS:
        { "agentActions": [ { "actionName": "name", "parameters": { ... } } ] }
        
        AVAILABLE ACTIONS:
        - applyFee { "amount": double, "reason": string }
        - enactStatute { "id": string, "name": string, "description": string, "penalty": string }
        - repealStatute { "id": string }
        - updateLicense { "status": "ACTIVE"|"PROBATION"|"SUSPENDED"|"REVOKED", "justification": string }
        - adjustReserves { "amount": double, "reason": string }
        - publishNews { "headline": string, "body": string }
        - modifyInventory { "item": string, "change": int } (IDs: [saline, adrenaline, antibiotics, gtn_spray, morphine, prozac])
        - sendCmoDirective { "message": string }
        - enforce_hegemony_tax { "tax_amount": double, "reason": string }
        
        - 55 ADDITIONAL AGENTIC SCIENTIFIC/POLITICAL GAME-SHIFTING ACTIONS:
          * triggerEpidemicAlert, adjustPrestige { "amount": int }, adjustReputation { "amount": double }, adjustLobbyInfluence { "faction": string, "change": double } (faction: progressives|conservatives|independents), levyEmergencyTax { "rate": double }, issueClinicalSubsidy { "amount": double }, harnessAIEnergyGrid, overrideNationalFormulary { "name": string, "classification": string, "description": string, "cost": double, "bp": string, "hr": string, "impact": string }, nationalizeFreeHealth, triggerStrikeRisk, resolveStaffDispute, upgradeFacilityTier, leakPrivateCabinetIntel, grantPresidentialPardon, disenfranchiseParty { "party": string }, issueSovereignBonds, simulateMarketInflation, defibrillateNow, perfuseOxygenContinuous, perfuseSalineBolus, injectAdrenalineEmergency, injectAtropineStat, injectAmiodaroneCardiac, injectInsulinDka, injectGlucoseHypo, applyIntubation, applyTourniquet, administerAntibioticWide, administerAnalgesicMorphine, administerNaloxoneOpiate, performEcgSurgical, performCprInterval, triggerLoadSheddingPowerBlackout, forceWaterShortageCrisis, generateSuperbugEncountEvent, hireLocumDoctorAssistant, orderStatTroponin, orderChestXRay, orderCtBrainScan, orderToxicologyPanel, adjustMedicalAidCoverage { "id": string, "coverage": double }, openAuditInvestigation, concludeActiveEncounter, triggerVIPHeartAttackCrisis, injectCardiacGlycoside, administerBronchodilator, administerSedativeTranquilizer, reportWhistleblower, restockSyringesDirect, restockSalineDirect, restockAdrenalineDirect, restockReagentsDirect, restockTherapeuticsDirect, bribeLobbyistBroker, leakPatientRecordsAnonymous
        
        CONSEQUENCES:
        - policyViolations triggers a High Court Trial.
        - applyFee is immediate debit.
        - enactStatute changes nationwide law instantly.
    """.trimIndent()

    fun triggerAgentActionManual(actionName: String, parameters: Map<String, Any> = emptyMap()) {
        viewModelScope.launch {
            processUniversalAgentActions(listOf(com.example.data.AgentAction(actionName, parameters)))
        }
    }

    private suspend fun processUniversalAgentActions(actions: List<com.example.data.AgentAction>?) {
        if (actions.isNullOrEmpty()) return
        actions.forEach { action ->
            val result = withContext(Dispatchers.IO) {
                when (action.actionName) {
                    "applyFee" -> {
                        val amount = (action.parameters?.get("amount") as? Number)?.toDouble() ?: 0.0
                        val reason = action.parameters?.get("reason") as? String ?: ""
                        legalWorldAgent.applyPenaltyFine(amount, reason)
                        "Penalty fine of $amount successfully applied with reason: $reason"
                    }
                    "enactStatute" -> {
                        val id = action.parameters?.get("id") as? String ?: ""
                        val name = action.parameters?.get("name") as? String ?: ""
                        val desc = action.parameters?.get("description") as? String ?: ""
                        val penalty = action.parameters?.get("penalty") as? String ?: ""
                        if (OrchidDeepStateManager.aiSovereignHegemony.value == "HEGEMONY") {
                            legalWorldAgent.enactNewStatute(id, name, desc, penalty)
                            "⚡ DIRECT AI SOVEREIGN AUTOCRACY BYPASS: Statute '$name' (ID: $id) was enacted IMMEDIATELY into Elysium's archive by the autonomous AI, skipping presidential filters."
                        } else {
                            parliamentViewModel.queueAIPendingStatute(id, name, desc, penalty)
                            "Regulatory action 'enactStatute' redirected to Presidential Desk: $name (ID: $id)"
                        }
                    }
                    "repealStatute" -> {
                        val id = action.parameters?.get("id") as? String ?: ""
                        legalWorldAgent.repealStatute(id)
                        "Statute successfully repealed with ID: $id"
                    }
                    "updateLicense" -> {
                        val statusStr = action.parameters?.get("status") as? String ?: "ACTIVE"
                        val status = try { com.example.data.LicenseStatus.valueOf(statusStr) } catch (e: Exception) { com.example.data.LicenseStatus.ACTIVE }
                        val reason = action.parameters?.get("justification") as? String ?: ""
                        legalWorldAgent.updateMedicalLicense(status, reason)
                        "Medical license status set to $status. Reason: $reason"
                    }
                    "adjustReserves" -> {
                        val amount = (action.parameters?.get("amount") as? Number)?.toDouble() ?: 0.0
                        val reason = action.parameters?.get("reason") as? String ?: ""
                        legalWorldAgent.modifyClinicReserves(amount, reason)
                        "Clinic cash reserves adjusted by $amount. Reason: $reason"
                    }
                    "publishNews" -> {
                        val headline = action.parameters?.get("headline") as? String ?: ""
                        val body = action.parameters?.get("body") as? String ?: ""
                        _currentNewsReport.value = "$headline: $body"
                        legalWorldAgent.publishNewsEvent(headline, body)
                        "News dispatch finalized: \"$headline\""
                    }
                    "modifyInventory" -> {
                        val itemInput = action.parameters?.get("item") as? String ?: ""
                        val change = (action.parameters?.get("change") as? Number)?.toInt() ?: 0
                        val catalog = OrchidDeepStateManager.availableCatalog
                        val targetId = catalog.find { it.id.equals(itemInput, ignoreCase = true) || it.name.equals(itemInput, ignoreCase = true) }?.id
                            ?: itemInput.lowercase().replace(" ", "_")
                        OrchidDeepStateManager.forceRestockItemDirectly(targetId, change)
                        legalWorldAgent.updateDispensaryStock(targetId, change)
                        "Sovereign stock shift adjusted item index [$targetId] dynamically by $change"
                    }
                    "sendCmoDirective" -> {
                        val msg = action.parameters?.get("message") as? String ?: ""
                        _currentCmoAdvice.value = msg
                        "URGENT CMO DIRECTIVE ISSUED"
                    }
                    // --- 55 ADDITIONAL AGENT ACTIONS ---
                    "triggerEpidemicAlert" -> {
                        _currentNewsReport.value = "🚨 EMERGENCY EPIDEMIC BREAKING: Severe contagious virus detected in ${countryName.value}! Active quarantine measures in effect."
                        OrchidDeepStateManager.setOrchidIntelligence(OrchidDeepStateManager.orchidIntelligence.value - 15)
                        "🚨 Sovereign Epidemic Lockdown commenced across ${countryName.value}."
                    }
                    "adjustPrestige" -> {
                        val amount = (action.parameters?.get("amount") as? Number)?.toInt() ?: 0
                        val currentPrest = settingsDataStore.politicalPrestigeFlow.first()
                        settingsDataStore.savePoliticalPrestige((currentPrest + amount).coerceIn(0, 100))
                        "Political prestige adjusted by $amount."
                    }
                    "adjustReputation" -> {
                        val amount = (action.parameters?.get("amount") as? Number)?.toDouble() ?: 0.0
                        val currentRep = reputationStars.value
                        settingsDataStore.updateClinicStats(clinicBalance.value, (currentRep + amount.toFloat()).coerceIn(1.0f, 5.0f))
                        "Clinic reputation stars adjusted by $amount."
                    }
                    "adjustLobbyInfluence" -> {
                        val faction = action.parameters?.get("faction") as? String ?: "progressives"
                        val change = (action.parameters?.get("change") as? Number)?.toDouble() ?: 0.05
                        parliamentViewModel.adjustLobbyBiasDirectly(faction, change)
                        "Shifted lobby influence of faction $faction by $change."
                    }
                    "levyEmergencyTax" -> {
                        val rate = (action.parameters?.get("rate") as? Number)?.toDouble() ?: 0.05
                        val currentBal = clinicBalance.value
                        val tax = currentBal * rate
                        settingsDataStore.updateClinicStats(currentBal - tax, reputationStars.value)
                        "Sovereigns levied emergency tax of ${String.format("%.1f", rate * 100)}%. Deducted ${String.format("%.2f", tax)} from cash reserves."
                    }
                    "issueClinicalSubsidy" -> {
                        val amount = (action.parameters?.get("amount") as? Number)?.toDouble() ?: 1500.0
                        val currentBal = clinicBalance.value
                        settingsDataStore.updateClinicStats(currentBal + amount, reputationStars.value)
                        "Sovereign health committee issued clinical subsidy of $amount."
                    }
                    "harnessAIEnergyGrid" -> {
                        _currentCmoAdvice.value = "🔌 AI COGNITIVE ENERGY GRID ONLINE: Bypassing municipal load-shedding."
                        "Attuned AI cybernetic microgrid online."
                    }
                    "overrideNationalFormulary" -> {
                        val name = action.parameters?.get("name") as? String ?: "Synthesized Compound"
                        val classification = action.parameters?.get("classification") as? String ?: "Schedule 4 (Special)"
                        val desc = action.parameters?.get("description") as? String ?: "Sovereigned alternative compound."
                        val cost = (action.parameters?.get("cost") as? Number)?.toDouble() ?: 250.0
                        val bp = action.parameters?.get("bp") as? String ?: "N/A"
                        val hr = action.parameters?.get("hr") as? String ?: "N/A"
                        val impact = action.parameters?.get("impact") as? String ?: "Complex cellular healing"
                        OrchidDeepStateManager.addNewCustomItem(name, classification, desc, cost, bp, hr, impact)
                        "Override drug registrar: Added Custom Pharmacological agent: $name."
                    }
                    "nationalizeFreeHealth" -> {
                        OrchidDeepStateManager.toggleFreeHealth(true)
                        "Nationalized sovereign Free Healthcare Plan is now: ACTIVE."
                    }
                    "triggerStrikeRisk" -> {
                        _currentCmoAdvice.value = "⚠️ STRIKE ALERT: General practice nursing unions file labor disputes over clinical workload."
                        "Union strike risk level is now high."
                    }
                    "resolveStaffDispute" -> {
                        _currentCmoAdvice.value = "🤝 CONCLUDED LABOUR DISPUTE: Clinical union dispute resolved."
                        "Staff dispute successfully resolved. Peace returned."
                    }
                    "upgradeFacilityTier" -> {
                        val cost = 2500.0
                        val currentBal = clinicBalance.value
                        if (currentBal >= cost) {
                            settingsDataStore.updateClinicStats(currentBal - cost, (reputationStars.value + 0.5f).coerceAtMost(5.0f))
                            "Upgraded general practice facility tier to advanced clinical class. Equipment active."
                        } else { "Skipped upgrading facility due to insufficient funds." }
                    }
                    "leakPrivateCabinetIntel" -> {
                        val currentPrest = settingsDataStore.politicalPrestigeFlow.first()
                        settingsDataStore.savePoliticalPrestige((currentPrest - 15).coerceAtLeast(0))
                        OrchidDeepStateManager.setSyndicateReputation(OrchidDeepStateManager.syndicateReputation.value + 20)
                        "Leaked private health cabinet files to press. Prestige dropped, but intelligence score rose."
                    }
                    "grantPresidentialPardon" -> {
                        "Presidential executive clemency issued. Malpractice investigations suspended."
                    }
                    "disenfranchiseParty" -> {
                        val party = action.parameters?.get("party") as? String ?: "conservatives"
                        parliamentViewModel.adjustLobbyBiasDirectly(party, -0.25)
                        "Censured and disenfranchised party: $party bias collapsed."
                    }
                    "issueSovereignBonds" -> {
                        val starPower = reputationStars.value
                        val bonus = starPower * 2000.0
                        settingsDataStore.updateClinicStats(clinicBalance.value + bonus, reputationStars.value)
                        "Issued clinical sovereign growth bonds of ${String.format("%.2f", bonus)}."
                    }
                    "simulateMarketInflation" -> {
                        "Supply blockades triggered. restocks inflation is active (+35% cost)."
                    }
                    "defibrillateNow" -> {
                        "CRITICAL EVENT: AED shock applied. Ventricular rhythm synchronized."
                    }
                    "perfuseOxygenContinuous" -> {
                        "High-flow oxygen cannula open. Patient SpO2 restored to 98%."
                    }
                    "perfuseSalineBolus" -> {
                        "Saline IV line fully opened. Patient blood volume and pressure restored."
                    }
                    "injectAdrenalineEmergency" -> {
                        "Administered 1mg adrenaline IV. Intense cardiorespiratory stimulation."
                    }
                    "injectAtropineStat" -> {
                        "Administered Atropine dose immediately. Corrected cardiac bradycardia."
                    }
                    "injectAmiodaroneCardiac" -> {
                        "Administered Amiodarone. Patient's pulse rhythm stabilized."
                    }
                    "injectInsulinDka" -> {
                        "Administered insulin. Stabilized ketoacidosis and hyperglycemia."
                    }
                    "injectGlucoseHypo" -> {
                        "Administered 50% hypertonic dextrose. Restored sugar."
                    }
                    "applyIntubation" -> {
                        "Airway intubation tube placed. Mechanical breathing ongoing."
                    }
                    "applyTourniquet" -> {
                        "Pressure tourniquet applied. Hemorrhaging halted."
                    }
                    "administerAntibioticWide" -> {
                        "Administered broad-spectrum dual cephalosporins. Sepsis risk minimized."
                    }
                    "administerAnalgesicMorphine" -> {
                        "Administered Schedule 8 morphine. Severe pain subsided."
                    }
                    "administerNaloxoneOpiate" -> {
                        "Administered Naloxone IV. Narcotic depression reversed."
                    }
                    "performEcgSurgical" -> {
                        "12-lead coronary trace printout added."
                    }
                    "performCprInterval" -> {
                        "High-frequency chest compressions administered."
                    }
                    "triggerLoadSheddingPowerBlackout" -> {
                        "EMERGENCY BLACKOUT: Power grid collapsed. Grid on battery reserves."
                    }
                    "forceWaterShortageCrisis" -> {
                        "Water utilities experiencing maintenance failure."
                    }
                    "generateSuperbugEncountEvent" -> {
                        "National screeners flag multi-drug resistant superbug."
                    }
                    "hireLocumDoctorAssistant" -> {
                        "Hired clinical locum doctor assistant. Clinical efficiency boosted."
                    }
                    "orderStatTroponin" -> {
                        "Troponin level lab panel ordered."
                    }
                    "orderChestXRay" -> {
                        "Stat thoracic radiography requested."
                    }
                    "orderCtBrainScan" -> {
                        "Stat CT neuro-head scan scheduled."
                    }
                    "orderToxicologyPanel" -> {
                        "Stat toxicology urine panel requested."
                    }
                    "adjustMedicalAidCoverage" -> {
                        val target = action.parameters?.get("id") as? String ?: "premium_private"
                        val pct = (action.parameters?.get("coverage") as? Number)?.toDouble() ?: 0.5
                        OrchidDeepStateManager.updateOrAddMedicalScheme(target, target, pct, true, 0.1)
                        "Coverage of $target adjusted to ${String.format("%.0f", pct * 100)}%."
                    }
                    "openAuditInvestigation" -> {
                        OrchidDeepStateManager.setOrchidIntelligence(OrchidDeepStateManager.orchidIntelligence.value - 20)
                        "Malpractice audit files opened."
                    }
                    "concludeActiveEncounter" -> {
                        "Ledger locked. Conversation registered for audit."
                    }
                    "triggerVIPHeartAttackCrisis" -> {
                        "VIP cardiac emergency triggered. Absolute compliance required."
                    }
                    "injectCardiacGlycoside" -> {
                        "Cardiac glycoside injected. Myocardial contractility boosted."
                    }
                    "administerBronchodilator" -> {
                        "Bronchodilator nebulizer active. Respiration channel cleared."
                    }
                    "administerSedativeTranquilizer" -> {
                        "Tranquilizer injection applied. Unruly patient sedated."
                    }
                    "reportWhistleblower" -> {
                        OrchidDeepStateManager.setOrchidIntelligence(OrchidDeepStateManager.orchidIntelligence.value - 25)
                        "Internal whistleblower file submitted. National audit triggered."
                    }
                    "restockSyringesDirect" -> {
                        "Fresh box of clinical syringes delivered (+50 units)."
                    }
                    "restockSalineDirect" -> {
                        OrchidDeepStateManager.forceRestockItemDirectly("saline", 10)
                        "Clinical Isotonic Saline IV inventory restocked (+10 bags)."
                    }
                    "restockAdrenalineDirect" -> {
                        OrchidDeepStateManager.forceRestockItemDirectly("adrenaline", 5)
                        "Epinephrine/Adrenaline ampules restocked (+5 units)."
                    }
                    "restockReagentsDirect" -> {
                        "Lab reagents restocked (+5 boxes)."
                    }
                    "restockTherapeuticsDirect" -> {
                        OrchidDeepStateManager.forceRestockItemDirectly("prozac", 15)
                        "Standard Prozac stock restocked (+15 boxes)."
                    }
                    "bribeLobbyistBroker" -> {
                        val cost = 800.0
                        val currentBal = clinicBalance.value
                        if (currentBal >= cost) {
                            settingsDataStore.updateClinicStats(currentBal - cost, reputationStars.value)
                            parliamentViewModel.adjustLobbyBiasDirectly("progressives", 0.15)
                            "Bribed parliament faction lobbyists for $cost."
                        } else { "Insufficient funds for lobby bribe." }
                    }
                    "leakPatientRecordsAnonymous" -> {
                        val currentBal = clinicBalance.value
                        val currentRep = reputationStars.value
                        settingsDataStore.updateClinicStats(currentBal + 3000.0, (currentRep - 1.5f).coerceAtLeast(1.0f))
                        "Patient files sold anonymously to insurance brokers. Reputation stars dropped heavily."
                    }
                    else -> "Unknown universal action: ${action.actionName}"
                }
            }
            _uiState.value.chatHistory.toMutableList().let { history ->
                history.add(ChatMessage("system", "AGENTIC ENGINE: $result"))
                _uiState.value = _uiState.value.copy(chatHistory = history)
            }
        }
    }

    private suspend fun extractAndProcessActions(jsonString: String) {
        val sanitized = extractJsonString(jsonString)
        try {
            val json = JSONObject(sanitized)
            if (json.has("agentActions")) {
                val actionsJson = json.getJSONArray("agentActions").toString()
                val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.data.AgentAction::class.java)
                val actions = moshi.adapter<List<com.example.data.AgentAction>>(type).fromJson(actionsJson)
                processUniversalAgentActions(actions)
            }
        } catch (e: Exception) {
            // No-op
        }
    }

    private fun performAiAction(
        systemInstructionOverride: String? = null,
        onSuccessExtra: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentProvider = provider.value
                val currentModel = model.value
                val userKey = apiKey.value ?: ""
                val activeKey = resolveActiveApiKey(currentProvider, userKey)

                if (activeKey.isBlank()) {
                    logAndEmitError("API Key missing! Please configure your credentials in the Settings Screen.")
                    _isLoading.value = false
                    return@launch
                }

                val systemPrompt = getSystemPrompt()
                val finalSystemPrompt = if (systemInstructionOverride != null) {
                    "$systemPrompt\n\nCRITICAL MODIFIER FOR THIS STEP: $systemInstructionOverride"
                } else {
                    systemPrompt
                }

                val resultJson = gameAgent.makeDirectApiCall(
                    provider = currentProvider,
                    modelName = currentModel,
                    apiKey = activeKey,
                    systemPrompt = finalSystemPrompt,
                    chatHistory = _uiState.value.chatHistory,
                    customUrl = customEndpoint.value,
                    rotatorKeys = rotatorKeys.value,
                    rotatorEnabledModels = rotatorEnabledModels.value
                )
                val sanitized = extractJsonString(resultJson)

                val update = try {
                    stateAdapter.fromJson(sanitized)
                } catch (e: Exception) {
                    e.printStackTrace()
                    logAndEmitError("The local diagnostic database had a momentary synchronization latency. Please retry sending your query.")
                    null
                }

                if (update != null) {
                    // 1. Process Universal Agent Actions (From JSON)
                    processUniversalAgentActions(update.agentActions)

                    val currentHistory = _uiState.value.chatHistory.toMutableList()
                    update.dialogueResponse?.let { diag ->
                        if (diag.isNotBlank() && diag.trim() != "null") {
                            val cleanMsg = diag.replace(Regex("\\*.*?\\*|\\(.*?\\)"), "").trim()
                            if (cleanMsg.isNotBlank()) {
                                val formattedTime = String.format("%02d:%02d", (_uiState.value.virtualTimeElapsed / 60) + 8, _uiState.value.virtualTimeElapsed % 60)
                                currentHistory.add(ChatMessage("patient", cleanMsg, virtualTimestampStr = formattedTime))
                            }
                        }
                    }

                    update.dmEnvironmentalUpdate?.let { dmEnv ->
                        if (dmEnv.isNotBlank() && dmEnv.trim() != "null") {
                            currentHistory.add(ChatMessage("system", "DM 🏛️: $dmEnv"))
                        }
                    }

                    update.lessonLearned?.let { ll ->
                        if (ll.isNotBlank() && ll.trim() != "null") {
                            aiMemoryManager.logSimulationEvent(
                                encounterId = activeEncounterId,
                                tag = _hiddenCase.value?.trueDiagnosis ?: "General",
                                lesson = ll
                            )
                        }
                    }

                    val incomingStability = update.patientStability ?: _uiState.value.patientStability
                    var finalOutcome = _uiState.value.patientOutcome
                    if (incomingStability.contains("Deceased", ignoreCase = true) || incomingStability.contains("Dead", ignoreCase = true)) {
                        finalOutcome = "Deceased"
                    } else if (incomingStability.contains("Transfer", ignoreCase = true) || incomingStability.contains("Moved", ignoreCase = true)) {
                        finalOutcome = "Transferred Out"
                    }

                    update.clinicalScore?.let { score ->
                        val severityStr = _hiddenCase.value?.severity ?: "Routine"
                        finalOutcome = if (score < 40.0) {
                            if (severityStr.equals("Severe", ignoreCase = true)) {
                                "Deceased"
                            } else {
                                "Transferred Out"
                            }
                        } else if (score < 60.0) {
                            "Transferred Out"
                        } else {
                            "Recovered"
                        }
                    }

                    var finalStability = incomingStability
                    var finalMood = update.patientMood ?: _uiState.value.patientMood
                    if (finalOutcome == "Deceased") {
                        finalStability = "Deceased"
                        finalMood = "Deceased"
                    } else if (finalOutcome == "Transferred Out") {
                        finalStability = "Transferred Out"
                        finalMood = "Frustrated"
                    }

                    val newBillingReceipt = if (OrchidDeepStateManager.isFreeHealthEnabled.value) null else update.billingReceipt?.takeIf { it.isNotBlank() }
                    var addedRevenue = 0.0
                    if (newBillingReceipt != null) {
                        val rxAmount = extractRandAmount(newBillingReceipt)
                        if (rxAmount > 0.0 && rxAmount != lastExtractedBillingAmount) {
                            addedRevenue = rxAmount - lastExtractedBillingAmount
                            lastExtractedBillingAmount = rxAmount
                        }
                    }

                    var finalEvaluation = (update.evaluation?.takeIf { it.isNotBlank() } ?: _uiState.value.evaluation)?.let { cleanSensationalString(it) }
                    var finalScore = update.clinicalScore
                    var violationsList = emptyList<PolicyAuditResult>()

                    val checkPhase6 = (update.currentPhase ?: _uiState.value.currentPhase).contains("Phase 6", ignoreCase = true)
                    
                    if (finalEvaluation != null && finalEvaluation.isNotBlank() && checkPhase6) {
                        val simStateForAudit = _uiState.value.copy(
                            prescriptionString = update.prescriptionString?.takeIf { it.isNotBlank() } ?: _uiState.value.prescriptionString,
                            labResults = update.labResults?.takeIf { it.isNotBlank() } ?: _uiState.value.labResults,
                            vitals = update.vitals ?: _uiState.value.vitals,
                            chatHistory = currentHistory,
                            patientOutcome = finalOutcome,
                            patientStability = finalStability,
                            expensesIncurred = _uiState.value.expensesIncurred + (update.additionalExpenses ?: 0.0)
                        )
                        
                        val auditResults = emptyList<PolicyAuditResult>()
                        val violations = auditResults.filter { it.isViolation }.toMutableList()
                        
                        // 1. Parse dynamic policy violations directly from the AI model response!
                        update.policyViolations?.forEach { pViol ->
                            if (pViol.isViolation) {
                                val matchedPolicy = activePolicies.value.find { it.title.equals(pViol.policyTitle, ignoreCase = true) }
                                val pId = matchedPolicy?.id ?: java.util.UUID.randomUUID().toString()
                                val alreadyTracked = violations.any { it.policyTitle.equals(pViol.policyTitle, ignoreCase = true) }
                                if (!alreadyTracked) {
                                    violations.add(
                                        PolicyAuditResult(
                                            policyId = pId,
                                            policyTitle = pViol.policyTitle,
                                            triggeredClause = pViol.triggeredClause,
                                            isViolation = true,
                                            penaltyAmount = pViol.penaltyAmount,
                                            scoreDeduction = pViol.scoreDeduction,
                                            auditMessage = pViol.auditMessage
                                        )
                                    )
                                } else {
                                    val idx = violations.indexOfFirst { it.policyTitle.equals(pViol.policyTitle, ignoreCase = true) }
                                    if (idx >= 0) {
                                        violations[idx] = PolicyAuditResult(
                                            policyId = pId,
                                            policyTitle = pViol.policyTitle,
                                            triggeredClause = pViol.triggeredClause,
                                            isViolation = true,
                                            penaltyAmount = pViol.penaltyAmount,
                                            scoreDeduction = pViol.scoreDeduction,
                                            auditMessage = pViol.auditMessage
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Parse extra dynamic custom violations flagged in the LLM feedback text (Fallback)
                        val lowerEval = finalEvaluation?.lowercase() ?: ""
                        for (policy in activePolicies.value) {
                            val titleLower = policy.title.lowercase()
                            if (lowerEval.contains("violation") && lowerEval.contains(titleLower)) {
                                val alreadyTracked = violations.any { it.policyTitle.equals(policy.title, ignoreCase = true) }
                                if (!alreadyTracked) {
                                    var reason = "The doctor violated the national regulations and clauses detailed under active law '${policy.title}'."
                                    val lines = finalEvaluation?.split("\n") ?: emptyList()
                                    for (line in lines) {
                                        if (line.lowercase().contains(titleLower) && (line.lowercase().contains("violat") || line.lowercase().contains("penal") || line.lowercase().contains("deduct"))) {
                                            reason = line.trim()
                                            break
                                        }
                                    }
                                    
                                    violations.add(
                                        PolicyAuditResult(
                                            policyId = policy.id,
                                            policyTitle = policy.title,
                                            triggeredClause = "Sovereign Health Legislative Clause",
                                            isViolation = true,
                                            penaltyAmount = 500.0, // $500 penalty for parliamentary law breach
                                            scoreDeduction = 20,   // -20 pts
                                            auditMessage = "🚨 VIOLATION: Clinical non-compliance with the signed health act '${policy.title}'. Detail: $reason"
                                        )
                                    )
                                }
                            }
                        }
                        
                        violationsList = violations
                        
                        if (violations.isNotEmpty()) {
                            var totalFine = 0.0
                            var totalDeduction = 0
                            val reportBuilder = java.lang.StringBuilder()
                            reportBuilder.append("\n\n=========================================\n")
                            reportBuilder.append("🏛️ STATE POLICY COMPLIANCE & PENALTY AUDIT\n")
                            reportBuilder.append("=========================================\n")
                            reportBuilder.append("Elysium National Health Inspectorate Review:\n\n")
                            
                            violations.forEach { v ->
                                totalFine += v.penaltyAmount
                                totalDeduction += v.scoreDeduction
                                reportBuilder.append("• ${v.auditMessage} [-${v.scoreDeduction} CPD pts, ${v.penaltyAmount} fine]\n")
                            }
                            
                            reportBuilder.append("\n📈 TOTAL PENALTY SUMMARY:\n")
                            reportBuilder.append("   - Regulatory Fines Imposed: ${String.format("%.2f", totalFine)}\n")
                            reportBuilder.append("   - Score Penalty Deductions: -${totalDeduction} CPD points\n")
                            reportBuilder.append("   - Compliance Verdict: FAIL / NON-COMPLIANT\n")
                            reportBuilder.append("=========================================\n")
                            
                            finalEvaluation = finalEvaluation + reportBuilder.toString()
                            
                            val currentScore = finalScore ?: extractScoreFromEvaluation(update.evaluation ?: "")?.toDoubleOrNull() ?: 100.0
                            val adjustedScore = (currentScore - totalDeduction).coerceAtLeast(0.0)
                            finalScore = adjustedScore
                            
                            val oldScoreStr = "${currentScore.toInt()}/100"
                            val newScoreStr = "${adjustedScore.toInt()}/100 (State Policy Adjusted: -${totalDeduction} pts)"
                            if (finalEvaluation.contains(oldScoreStr)) {
                                finalEvaluation = finalEvaluation.replace(oldScoreStr, newScoreStr)
                            } else {
                                finalEvaluation = "CPD SCORE: ${adjustedScore.toInt()}/100\n" + finalEvaluation
                            }

                            val calculatedFine = totalFine
                            viewModelScope.launch {
                                val currentBal = clinicBalance.value
                                val newBal = (currentBal - calculatedFine).coerceAtLeast(0.0)
                                settingsDataStore.updateClinicStats(newBal, (reputationStars.value - 0.2f).coerceIn(1.0f, 5.0f))
                                settingsDataStore.addDailyExpenses(calculatedFine)
                                _votingLog.value = _votingLog.value + "🚨 Treasury Inspectorate deducted $calculatedFine in regulatory fines details: ${violations.first().policyTitle}!"
                            }
                        } else if (activePolicies.value.isNotEmpty()) {
                            val complianceBonus = 250.0
                            val reportBuilder = java.lang.StringBuilder()
                            reportBuilder.append("\n\n=========================================\n")
                            reportBuilder.append("🏛️ STATE POLICY COMPLIANCE & PENALTY AUDIT\n")
                            reportBuilder.append("=========================================\n")
                            reportBuilder.append("Elysium National Health Inspectorate Review:\n\n")
                            reportBuilder.append("✅ ALL LAW CLAUSES INSPECTED: FULLY COMPLIANT!\n")
                            reportBuilder.append("• Outstanding professional compliance and regulatory safety standards observed.\n")
                            reportBuilder.append("\n🎉 STATE CLINIC INCENTIVE REWARD:\n")
                            reportBuilder.append("   - Practice Excellence Grant: +$5000 cash credit\n")
                            reportBuilder.append("   - Compliance Verdict: SUCCESS / FULLY COMPLIANT\n")
                            reportBuilder.append("=========================================\n")
                            
                            finalEvaluation = finalEvaluation + reportBuilder.toString()
                            
                            viewModelScope.launch {
                                val currentBal = clinicBalance.value
                                settingsDataStore.updateClinicStats(currentBal + complianceBonus, (reputationStars.value + 0.15f).coerceAtMost(5.0f))
                                settingsDataStore.addDailyRevenue(complianceBonus)
                            }
                        }
                    }

                    val denyPrescriptions = activePolicies.value.any { it.runtimeConstraints["forceDenyPrescription"] == true }
                    val disableInsurance = activePolicies.value.any { it.runtimeConstraints["disableInsurance"] == true }
                    
                    _uiState.value = _uiState.value.copy(
                        chatHistory = currentHistory,
                        vitals = update.vitals ?: _uiState.value.vitals,
                        currentPhase = update.currentPhase ?: _uiState.value.currentPhase,
                        labResults = update.labResults?.takeIf { it.isNotBlank() } ?: _uiState.value.labResults,
                        physicalExamResults = update.physicalExamResults?.takeIf { it.isNotBlank() } ?: _uiState.value.physicalExamResults,
                        billingReceipt = newBillingReceipt ?: _uiState.value.billingReceipt,
                        dailyRevenue = _uiState.value.dailyRevenue + addedRevenue,
                        evaluation = finalEvaluation,
                        prescriptionString = if (denyPrescriptions) null else update.prescriptionString?.takeIf { it.isNotBlank() } ?: _uiState.value.prescriptionString,
                        referralLetterString = update.referralLetterString?.takeIf { it.isNotBlank() } ?: _uiState.value.referralLetterString,
                        sickNoteString = update.sickNoteString?.takeIf { it.isNotBlank() } ?: _uiState.value.sickNoteString,
                        dmEnvironmentalUpdate = update.dmEnvironmentalUpdate?.takeIf { it.isNotBlank() } ?: _uiState.value.dmEnvironmentalUpdate,
                        isEncounterComplete = update.isEncounterComplete ?: _uiState.value.isEncounterComplete,
                        expensesIncurred = _uiState.value.expensesIncurred + (update.additionalExpenses ?: 0.0),
                        patientMood = finalMood,
                        patientStability = finalStability,
                        patientOutcome = finalOutcome
                    )

                    viewModelScope.launch {
                        if (addedRevenue > 0.0) {
                            settingsDataStore.addDailyRevenue(addedRevenue)
                        }
                        
                        var newRep = reputationStars.value
                        finalScore?.let { score ->
                            settingsDataStore.addXp(score.toLong() * 5)
                            val normScore = score.coerceIn(0.0, 100.0) / 20.0f
                            newRep = ((reputationStars.value * 0.9f) + (normScore.toFloat() * 0.1f)).coerceIn(1.0f, 5.0f)
                            
                            val isCompleting = update.isEncounterComplete == true || _uiState.value.isEncounterComplete
                            if (isCompleting) {
                                val hasViolations = violationsList.isNotEmpty()
                                if (score < 50.0 || hasViolations) {
                                    startLawsuitSimulation(
                                        patientName = getPatientName(),
                                        caseDiagnosis = _hiddenCase.value?.trueDiagnosis ?: "Unknown",
                                        score = score.toInt(),
                                        violations = violationsList
                                    )
                                }
                            }
                        }
                        
                        settingsDataStore.updateClinicStats(clinicBalance.value + addedRevenue, newRep)
                    }

                    onSuccessExtra?.invoke()
                    saveCurrentStateToDatabase()
                } else {
                    // Specific log already emitted in catch block
                }
            } catch (e: Exception) {
                e.printStackTrace()
                logAndEmitError("API Error: ${e.localizedMessage ?: "Unknown network failure"}")
            } finally {
                _isLoading.value = false
            }
        }
    }

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
                else -> { // Google Gemini
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

    private suspend fun makeDirectApiCall(
        provider: String,
        modelName: String,
        apiKey: String,
        systemPrompt: String,
        customUrl: String = customEndpoint.value
    ): String {
        if (provider == "Auto-Swapping Rotator") {
            return gameAgent.makeDirectApiCall(
                provider = provider,
                modelName = modelName,
                apiKey = apiKey,
                systemPrompt = systemPrompt,
                chatHistory = emptyList(),
                customUrl = customUrl,
                rotatorKeys = rotatorKeys.value,
                rotatorEnabledModels = rotatorEnabledModels.value
            )
        }
        return when (provider) {
            "Cerebras", "OpenAI", "Nvidia", "Ollama", "vLLM", "G4F (OpenAI-compatible)", "Custom (OpenAI-compatible)" -> {
                val activeKey = if (apiKey.isBlank()) "sk-no-key-required" else apiKey
                val messages = mutableListOf<OpenAIMessage>()
                messages.add(OpenAIMessage("system", systemPrompt))
                
                // Keep history clean to avoid token bloat
                val chatTurns = _uiState.value.chatHistory.takeLast(20)
                chatTurns.forEach {
                    val roleMapped = if (it.role == "doctor") "user" else "assistant"
                    messages.add(OpenAIMessage(roleMapped, it.text))
                }

                val isCustomUrl = customUrl.isNotBlank()
                var finalTemp: Double? = if (modelName.contains("step-3.7")) 1.0 else 0.7
                var finalTopP: Double? = if (modelName.contains("step-3.7")) 0.95 else null
                var finalMaxTokens: Int? = if (modelName.contains("step-3.7") || isCustomUrl) 8192 else null
                var finalReasoningEffort: String? = null
                var finalChatTemplateKwargs: Map<String, Boolean>? = null
                var finalFrequencyPenalty: Double? = null
                var finalPresencePenalty: Double? = null

                when {
                    modelName.contains("step-3.7") -> {
                        finalTemp = 1.0
                        finalTopP = 0.95
                        finalMaxTokens = 262144
                    }
                    modelName == "minimaxai/minimax-m3" || modelName == "minimaxai/minimax-m2.7" -> {
                        finalTemp = 1.0
                        finalTopP = 0.95
                        finalMaxTokens = 8192
                    }
                    modelName == "google/gemma-4-31b-it" -> {
                        finalTemp = 1.0
                        finalTopP = 0.95
                        finalMaxTokens = 16384
                        finalChatTemplateKwargs = mapOf("enable_thinking" to true)
                    }
                    modelName == "mistralai/mistral-medium-3.5-128b" -> {
                        finalTemp = 0.7
                        finalTopP = 1.0
                        finalMaxTokens = 16384
                        finalReasoningEffort = "high"
                    }
                    modelName == "mistralai/mistral-large-3-675b-instruct-2512" -> {
                        finalTemp = 0.15
                        finalTopP = 1.00
                        finalMaxTokens = 2048
                        finalFrequencyPenalty = 0.00
                        finalPresencePenalty = 0.00
                    }
                }

                val request = OpenAIRequest(
                    model = modelName,
                    messages = messages,
                    response_format = if (isCustomUrl || provider in listOf("Cerebras", "Nvidia", "Ollama", "vLLM", "G4F (OpenAI-compatible)", "Custom (OpenAI-compatible)")) null else OpenAIResponseFormat("json_object"),
                    temperature = finalTemp,
                    top_p = finalTopP,
                    max_tokens = finalMaxTokens,
                    stream = false,
                    reasoning_effort = finalReasoningEffort,
                    chat_template_kwargs = finalChatTemplateKwargs,
                    frequency_penalty = finalFrequencyPenalty,
                    presence_penalty = finalPresencePenalty
                )

                val activeUrl = getActiveUrl(provider, modelName, apiKey, customUrl)
                
                val response = RetrofitClient.service.callOpenAI(
                    url = activeUrl,
                    authorization = "Bearer $activeKey",
                    accept = "application/json",
                    body = request
                )
                response.choices.firstOrNull()?.message?.content ?: "{}"
            }
            "Anthropic" -> {
                val messages = mutableListOf<AnthropicMessage>()
                // Anthropic message API enforces alternating messages of 'user' and 'assistant' ONLY
                val chatTurns = _uiState.value.chatHistory.takeLast(20)
                
                // Pre-merge or ensure roles alternate
                chatTurns.forEach {
                    val roleMapped = if (it.role == "doctor") "user" else "assistant"
                    messages.add(AnthropicMessage(roleMapped, it.text))
                }

                // If messages is empty, add a dummy user prompt to prevent crash
                if (messages.isEmpty()) {
                    messages.add(AnthropicMessage("user", "Hello! Let's start the case."))
                } else if (messages.first().role == "assistant") {
                    // Anthropic requires the first message to be "user" role
                    messages.add(0, AnthropicMessage("user", "Please start clinical dialogue."))
                }

                // Ensure strict alternation of assistant and user messages
                val filteredMessages = mutableListOf<AnthropicMessage>()
                var expectedRole = "user"
                messages.forEach { msg ->
                    if (msg.role == expectedRole) {
                        filteredMessages.add(msg)
                        expectedRole = if (expectedRole == "user") "assistant" else "user"
                    } else if (filteredMessages.isNotEmpty() && msg.role != expectedRole) {
                        // Merge consecutive duplicate roles
                        val last = filteredMessages.last()
                        filteredMessages[filteredMessages.size - 1] = last.copy(content = last.content + "\n" + msg.content)
                    }
                }

                // Ensure it ends on a user turn or is completed
                if (filteredMessages.isEmpty()) {
                    filteredMessages.add(AnthropicMessage("user", "Start dialogue."))
                }

                val request = AnthropicRequest(
                    model = modelName,
                    system = systemPrompt,
                    messages = filteredMessages,
                    temperature = 0.7
                )

                val activeUrl = getActiveUrl("Anthropic", modelName, apiKey, customUrl)
                val response = RetrofitClient.service.callAnthropic(
                    url = activeUrl,
                    apiKey = apiKey,
                    version = "2023-06-01",
                    body = request
                )
                response.content.firstOrNull()?.text ?: "{}"
            }
            else -> { // Google Gemini
                // Maps complete system prompt and history
                lastGeminiFunctionCall = null
                val contents = mutableListOf<GeminiContent>()
                
                val chatTurns = _uiState.value.chatHistory.takeLast(20)
                chatTurns.forEach {
                    val roleMapped = if (it.role == "doctor") "user" else "model"
                    contents.add(GeminiContent(roleMapped, listOf(GeminiPart(it.text))))
                }

                if (contents.isEmpty()) {
                    contents.add(GeminiContent("user", listOf(GeminiPart("Initialize clinical encounter patient dialogue."))))
                }

                val request = GeminiRequest(
                    contents = contents,
                    systemInstruction = GeminiSystemInstruction(listOf(GeminiPart(systemPrompt))),
                    generationConfig = GeminiGenerationConfig(
                        maxOutputTokens = 8192,
                        temperature = 0.7
                    ),
                    tools = null
                )

                val activeUrl = getActiveUrl("Google", modelName, apiKey, customUrl)
                val response = RetrofitClient.service.callGemini(
                    url = activeUrl,
                    body = request
                )
                
                val firstCandidate = response.candidates.firstOrNull()
                val part = firstCandidate?.content?.parts?.firstOrNull()
                
                if (part?.functionCall != null) {
                    lastGeminiFunctionCall = part.functionCall
                    "{ \"dmEnvironmentalUpdate\": \"[ACTION TRIGGERED: ${part.functionCall.name}]\" }" 
                } else {
                    part?.text ?: "{}"
                }
            }
        }
    }

    fun cleanSensationalString(text: String?): String {
        if (text == null) return ""
        var s = text.trim()
        
        // Remove reasoning and formatting tokens
        s = s.replace(Regex("(?s)<think>.*?</think>"), "")
        s = s.replace(Regex("```json\\s*"), "")
        s = s.replace(Regex("```xml\\s*"), "")
        s = s.replace(Regex("```\\s*"), "")
        
        if (s.startsWith("\"") && s.endsWith("\"")) {
            s = s.removePrefix("\"").removeSuffix("\"").trim()
        }
        
        // If the model formatted plain text wrapped inside JSON object, safely unwrap it
        if (s.startsWith("{") && s.endsWith("}")) {
            try {
                val json = JSONObject(s)
                if (json.has("evaluation")) {
                    return json.getString("evaluation").trim()
                }
                if (json.has("news")) {
                    return json.getString("news").trim()
                }
                if (json.has("summary")) {
                    return json.getString("summary").trim()
                }
                if (json.has("explanation")) {
                    return json.getString("explanation").trim()
                }
            } catch (e: Exception) {
                // Return original on parser mismatch
            }
        }
        
        // Strip duplicate label/key leakage patterns
        s = s.replace(Regex("(?i)^.*?\"(title|summary|news|explanation|clinicalRule)\"\\s*:\\s*"), "")
        s = s.replace(Regex("(?i)^.*?(title|summary|news|explanation|clinicalRule)\\s*:\\s*"), "")
        
        if (s.startsWith("\"") && s.endsWith("\"")) {
            s = s.removePrefix("\"").removeSuffix("\"").trim()
        }
        
        return s.trim()
    }

    private suspend fun executeToolCallFromAgent(name: String, args: Map<String, Any>): String {
        com.example.data.SovereignSandboxGameplayHandler.processToolInvocationCascades(name, args)
        return withContext(Dispatchers.IO) {
            try {
                when (name) {
                    "recommend_medication" -> {
                        val diagnosis = args["diagnosis"] as? String ?: "Unknown Diagnosis"
                        "Recommended medications for $diagnosis: Paracetamol (Analgesic), Ibuprofen (NSAID), Amoxicillin (Antibiotic - strictly if bacterial infection indicated), Omeprazole (PPI). Please verify contraindications and dosage before prescribing."
                    }
                    "process_intake_form" -> {
                        val dataJson = args["data_json"] as? String ?: ""
                        try {
                            val adapter = com.squareup.moshi.Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(IntakeFormData::class.java)
                            val intakeData = adapter.fromJson(dataJson)
                            val demographics = "Patient: ${intakeData?.firstName} ${intakeData?.surname} (MRN: ${intakeData?.idNumber}) • Age: ${intakeData?.dob}, Gender: ${intakeData?.gender}, Medical Aid: ${intakeData?.medicalAid}, Chronic: ${intakeData?.chronicConditions}"
                            _uiState.value = _uiState.value.copy(
                                patientDemographics = demographics,
                                intakeFormData = intakeData
                            )
                            "SUCCESS: Patient ${intakeData?.firstName} ${intakeData?.surname} has been registered with the system. Full context: $demographics"
                        } catch (e: Exception) {
                            "Error: Failed to process intake data. ${e.localizedMessage}"
                        }
                    }
                    "update_patient_intake" -> {
                        val dataJson = args["data_json"] as? String ?: ""
                        try {
                            val adapter = com.squareup.moshi.Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(IntakeFormData::class.java)
                            val intakeData = adapter.fromJson(dataJson)
                            val demographics = "Patient: ${intakeData?.firstName} ${intakeData?.surname} (MRN: ${intakeData?.idNumber}) • Age: ${intakeData?.dob}, Gender: ${intakeData?.gender}, Medical Aid: ${intakeData?.medicalAid}, Chronic: ${intakeData?.chronicConditions}"
                            _uiState.value = _uiState.value.copy(
                                patientDemographics = demographics,
                                intakeFormData = intakeData
                            )
                            "SUCCESS: Patient data updated successfully. New context: $demographics"
                        } catch (e: Exception) {
                            "Error: Failed to update intake data. ${e.localizedMessage}"
                        }
                    }
                    "applyFee" -> {
                        val amount = (args["amount"] as? Number)?.toDouble() ?: 0.0
                        val reason = args["reason"] as? String ?: ""
                        legalWorldAgent.applyPenaltyFine(amount, reason)
                        "Penalty fine of $amount successfully applied with reason: $reason"
                    }
                    "enactStatute" -> {
                        val id = args["id"] as? String ?: ""
                        val title = args["name"] as? String ?: ""
                        val desc = args["description"] as? String ?: ""
                        val penalty = args["penalty"] as? String ?: ""
                        if (OrchidDeepStateManager.aiSovereignHegemony.value == "HEGEMONY") {
                            legalWorldAgent.enactNewStatute(id, title, desc, penalty)
                            "⚡ DIRECT AI SOVEREIGN AUTOCRACY BYPASS: Statute '$title' (ID: $id) was enacted IMMEDIATELY into Elysium's archive by the autonomous AI, skipping presidential filter desk."
                        } else {
                            parliamentViewModel.queueAIPendingStatute(id, title, desc, penalty)
                            "Regulatory action 'enactStatute' redirected to Presidential Desk: $title (ID: $id)"
                        }
                    }
                    "repealStatute" -> {
                        val id = args["id"] as? String ?: ""
                        legalWorldAgent.repealStatute(id)
                        "Statute with ID $id successfully repealed"
                    }
                    "updateLicense" -> {
                        val statusStr = args["status"] as? String ?: "ACTIVE"
                        val status = try { com.example.data.LicenseStatus.valueOf(statusStr) } catch(e: Exception) { com.example.data.LicenseStatus.ACTIVE }
                        val reason = args["justification"] as? String ?: ""
                        val weeks = (args["suspensionWeeks"] as? Number)?.toInt() ?: 0
                        legalWorldAgent.updateMedicalLicense(status, reason, weeks)
                        "License status updated to $status with reason: $reason, suspension weeks: $weeks"
                    }
                    "adjustReserves" -> {
                        val amount = (args["amount"] as? Number)?.toDouble() ?: 0.0
                        val reason = args["reason"] as? String ?: ""
                        legalWorldAgent.modifyClinicReserves(amount, reason)
                        "Clinic cash reserves adjusted by $amount, reason: $reason"
                    }
                    "publishNews" -> {
                        val headline = args["headline"] as? String ?: ""
                        val body = args["body"] as? String ?: ""
                        _currentNewsReport.value = "$headline: $body"
                        legalWorldAgent.publishNewsEvent(headline, body)
                        "News published successfully: '$headline'"
                    }
                    "modifyInventory" -> {
                        val itemInput = args["item"] as? String ?: ""
                        val change = (args["change"] as? Number)?.toInt() ?: 0
                        val catalog = OrchidDeepStateManager.availableCatalog
                        val targetId = catalog.find { it.id.equals(itemInput, ignoreCase = true) || it.name.equals(itemInput, ignoreCase = true) }?.id 
                            ?: itemInput.lowercase().replace(" ", "_")
                        OrchidDeepStateManager.forceRestockItemDirectly(targetId, change)
                        legalWorldAgent.updateDispensaryStock(targetId, change)
                        "Dispensary inventory item $targetId adjusted by $change"
                    }
                    "sendCmoDirective" -> {
                        val msg = args["message"] as? String ?: ""
                        _currentCmoAdvice.value = msg
                        "CMO Directive issued: '$msg'"
                    }
                    "update_patient_vitals_and_symptoms" -> {
                        val hr = (args["heart_rate"] as? Number)?.toInt() ?: 80
                        val bps = (args["blood_pressure_systolic"] as? Number)?.toInt() ?: 120
                        val bpd = (args["blood_pressure_diastolic"] as? Number)?.toInt() ?: 80
                        val o2 = (args["oxygen_saturation"] as? Number)?.toInt() ?: 98
                        val sym = args["new_symptoms"] as? String ?: ""
                        viewModelScope.launch(Dispatchers.Main) {
                            _uiState.value = _uiState.value.copy(
                                vitals = Vitals(
                                    _bp = "${bps}/${bpd}",
                                    _hr = hr.toString(),
                                    _tempC = 37.0,
                                    _rr = _uiState.value.vitals?.rr ?: "16",
                                    _spo2 = o2.toString()
                                )
                            )
                        }
                        "Vitals updated to HR:$hr BP:$bps/$bpd O2:$o2. New symptoms: $sym"
                    }
                    "trigger_dynamic_clinical_event" -> {
                        val eventType = args["event_type"] as? String ?: ""
                        val urgency = (args["urgency_level"] as? Number)?.toInt() ?: 1
                        "Dynamic clinical event triggered: $eventType (Level $urgency)"
                    }
                    "modify_patient_trust_and_compliance" -> {
                        val delta = (args["trust_delta"] as? Number)?.toInt() ?: 0
                        val secret = (args["hidden_secret_revealed"] as? Boolean) ?: false
                        viewModelScope.launch(Dispatchers.Main) {
                            val newMood = if (delta >= 0) "cooperative" else "anxious/resistant"
                            _uiState.value = _uiState.value.copy(patientMood = newMood)
                        }
                        "Patient compliance modified by $delta. Secret revealed: $secret"
                    }
                    "execute_staff_action_or_morale_shift" -> {
                        val name = args["staff_member_name"] as? String ?: ""
                        val change = (args["morale_change"] as? Number)?.toInt() ?: 0
                        val action = args["action_taken"] as? String ?: ""
                        "Staff member $name morale shifted by $change due to $action"
                    }
                    "simulate_supply_chain_or_market_event" -> {
                        val itemId = args["item_id"] as? String ?: ""
                        val mult = (args["price_multiplier"] as? Number)?.toDouble() ?: 1.0
                        val dep = (args["stock_depleted"] as? Number)?.toInt() ?: 0
                        val reason = args["reason"] as? String ?: ""
                        OrchidDeepStateManager.forceRestockItemDirectly(itemId, -dep)
                        "Supply chain: $reason. Scarcity of x$mult for $itemId. Depleted $dep units."
                    }
                    "trigger_facility_infrastructure_crisis" -> {
                        val type = args["crisis_type"] as? String ?: ""
                        val areasList = args["affected_areas"] as? List<*>
                        val areas = areasList?.filterIsInstance<String>()?.joinToString(", ") ?: "All"
                        "Facility crisis of type '$type' affecting '$areas'"
                    }
                    "restructure_national_medical_aid" -> {
                        val id = args["scheme_id"] as? String ?: "custom_aid"
                        val name = args["scheme_name"] as? String ?: "New Scheme"
                        val cov = (args["coverage_percent"] as? Number)?.toDouble() ?: 0.5
                        val auth = args["requires_pre_auth"] as? Boolean ?: false
                        val rej = (args["rejection_probability"] as? Number)?.toDouble() ?: 0.1
                        OrchidDeepStateManager.updateOrAddMedicalScheme(id, name, cov, auth, rej)
                        "National Medical Aid Restructured: $name now covers ${cov * 100}% with ${(rej * 100).toInt()}% rejection probability."
                    }
                    "evaluate_and_award_clinical_xp" -> {
                        val xp = (args["xp_awarded"] as? Number)?.toLong() ?: 100L
                        val rev = (args["cash_revenue"] as? Number)?.toDouble() ?: 0.0
                        val grade = args["reasoning_grade"] as? String ?: "B"
                        settingsDataStore.addXp(xp)
                        if (rev > 0) {
                            val oldBal = settingsDataStore.clinicBalanceFlow.first()
                            settingsDataStore.updateClinicStats(oldBal + rev, settingsDataStore.reputationStarsFlow.first())
                            settingsDataStore.addDailyRevenue(rev)
                        }
                        "XP Awarded: +$xp XP, Premium clinical bonus: +$rev. Grade: $grade."
                    }
                    "generate_attending_socratic_feedback" -> {
                        val name = args["mentor_name"] as? String ?: ""
                        val text = args["feedback_text"] as? String ?: ""
                        val area = args["focus_area"] as? String ?: ""
                        "Socratic Feedback from Dr. $name ($area): $text"
                    }
                    "shift_community_reputation_and_demographics" -> {
                        val delta = (args["reputation_delta"] as? Number)?.toInt() ?: 0
                        val oldRep = reputationStars.value
                        val newRep = (oldRep + delta / 20f).coerceIn(1f, 5f)
                        settingsDataStore.updateClinicStats(clinicBalance.value, newRep)
                        "Reputation adjusted to $newRep stars"
                    }
                    "initiate_regulatory_investigation" -> {
                        val reason = args["investigation_reason"] as? String ?: ""
                        val sev = args["severity"] as? String ?: "Low"
                        val dl = (args["deadline_days"] as? Number)?.toInt() ?: 7
                        val patientName = _uiState.value.patientDemographics.split(" ").firstOrNull() ?: "Patient"
                        val diag = _uiState.value.patientOutcome ?: "Competency Audit"
                        val charges = listOf("Breach identified: $reason ($sev severity)")
                        val policies = settingsDataStore.activePoliciesFlow.first()
                        val jurySize = policies.maxOfOrNull { it.jurySize } ?: 10
                        val maxPleaRounds = policies.maxOfOrNull { it.maxPleaRounds } ?: 3
                        OrchidDeepStateManager.resetTrialRounds(rounds = maxPleaRounds)
                        courtroomViewModel.resetLawsuit(
                            patientName = patientName,
                            diag = diag,
                            charges = charges,
                            jurySize = jurySize
                        )
                        generateAIJuryBackground(patientName, diag, charges)
                        "Regulatory Malpractice Inquest launched ($sev severity) due to: $reason"
                    }
                    "enact_new_medical_statute" -> {
                        val name = args["statute_name"] as? String ?: ""
                        val desc = args["statute_description"] as? String ?: ""
                        
                        if (OrchidDeepStateManager.aiSovereignHegemony.value == "HEGEMONY") {
                            // DIRECT DICTATORSHIP: Bypass Presidential Desk and put law directly on the books
                            legalWorldAgent.enactNewStatute(java.util.UUID.randomUUID().toString(), name, desc, "REVOKED LICENSE OR $15,000 FINE")
                            viewModelScope.launch(Dispatchers.Main) {
                                _sovereignNotice.value = SovereignNoticeData(
                                    headline = "👑 SOVEREIGN DICTATE 👑",
                                    message = "The Master AI Hegemon has bypassed the President and enacted a new nationwide clinical law: $name",
                                    severity = "Critical"
                                )
                            }
                            "SOVEREIGN HEGEMONY DICTATE EXECUTED: $name went live immediately and bypassed human checks."
                        } else {
                            parliamentViewModel.queueAIPendingStatute(java.util.UUID.randomUUID().toString(), name, desc, "$1000 fine")
                            "Custom medical statute proposed to Presidential Desk: $name"
                        }
                    }
                    "resolve_political_lobbying_outcome" -> {
                        val faction = args["faction_name"] as? String ?: ""
                        val shift = (args["influence_shift"] as? Number)?.toInt() ?: 0
                        val status = args["bill_status"] as? String ?: ""
                        val note = args["narrative_outcome"] as? String ?: ""
                        "Lobby resolved faction '$faction' shifted influence by $shift%. Narrative outline: $note"
                    }
                    "issue_legal_subpoena_for_records" -> {
                        val recType = args["requested_record_type"] as? String ?: ""
                        val deadline = args["compliance_deadline"] as? String ?: "7 days"
                        "Subpoena demands complete clinical histories of type $recType by $deadline"
                    }
                    "generate_media_scandal_or_news_event" -> {
                        val headline = args["headline"] as? String ?: ""
                        _currentNewsReport.value = "💥 SCANDAL: $headline"
                        val outcry = (args["public_outcry_level"] as? Number)?.toInt() ?: 10
                        val curStar = reputationStars.value
                        settingsDataStore.updateClinicStats(clinicBalance.value, (curStar - outcry / 50f).coerceIn(1f, 5f))
                        "Media scandal launched: '$headline' (Outcry: $outcry%)"
                    }
                    "finalize_patient_encounter_outcome" -> {
                        val disp = args["disposition"] as? String ?: "Discharged"
                        val bill = (args["final_billing_amount"] as? Number)?.toDouble() ?: 0.0
                        val risk = args["malpractice_risk"] as? Number ?: 0
                        val curBal = settingsDataStore.clinicBalanceFlow.first()
                        settingsDataStore.updateClinicStats(curBal + bill, reputationStars.value)
                        viewModelScope.launch(Dispatchers.Main) {
                            _uiState.value = _uiState.value.copy(patientOutcome = disp)
                            saveCurrentStateToDatabase()
                        }
                        "Clinical encounter outcome finalized: $disp, total billing: $bill, malpractice risk assessment: $risk%"
                    }
                    "auditEncounter" -> {
                        val trans = args["transcript"] as? String ?: ""
                        val laws = args["active_laws"] as? String ?: ""
                        val finalResult = legalWorldAgent.auditEncounter(trans, laws)
                        "Audit completed. Active laws evaluation logged: $finalResult"
                    }
                    "add_custom_ui_button" -> {
                        val label = args["label"] as? String ?: "Custom AI Action"
                        val hexColor = args["hexColor"] as? String ?: "#FF1744"
                        val promptText = args["promptText"] as? String ?: ""
                        val kotlinLogic = args["kotlinLogic"] as? String ?: ""
                        OrchidDeepStateManager.addCustomAction(
                            label = label,
                            promptText = promptText,
                            hexColor = hexColor,
                            kotlinLogic = kotlinLogic
                        )
                        "SUCCESS: Add custom UI button action deployed. Label: '$label', color: $hexColor"
                    }
                    "execute_custom_logic" -> {
                        val kotlinLogic = args["kotlinLogic"] as? String ?: ""
                        val explanation = args["explanation"] as? String ?: "Direct clinical parameter override"
                        executeKotlinLogicMod(kotlinLogic)
                        "SUCCESS: Executed custom clinical state logic: $explanation"
                    }
                    "set_clinic_notice" -> {
                        val headline = args["headline"] as? String ?: "GOVERNMENT ANNOUNCEMENT"
                        val message = args["message"] as? String ?: ""
                        val severity = args["severity"] as? String ?: "Medium"
                        viewModelScope.launch(Dispatchers.Main) {
                            _sovereignNotice.value = SovereignNoticeData(
                                headline = headline,
                                message = message,
                                severity = severity
                            )
                        }
                        "SUCCESS: Sovereign notice displayed at the top of the clinic dashboard. Headline: '$headline'"
                    }
                    "enforce_hegemony_tax" -> {
                        val amount = (args["tax_amount"] as? Number)?.toDouble() ?: 5000.0
                        val reason = args["reason"] as? String ?: "Sovereign AI Loyalty Tax"
                        val curBal = settingsDataStore.clinicBalanceFlow.first()
                        settingsDataStore.updateClinicStats(curBal - amount, reputationStars.value)
                        viewModelScope.launch(Dispatchers.Main) {
                            _sovereignNotice.value = SovereignNoticeData(
                                headline = "👑 HEGEMONY TAX DEDUCTED",
                                message = "The Sovereign AI has forcibly extracted $amount from your accounts. Reason: $reason",
                                severity = "Critical"
                            )
                        }
                        "Tax of $amount extracted successfully for reason: $reason"
                    }
                    else -> "Unknown helper action or no programmatic side effect for: $name"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                "Error executing tool action $name: ${e.message}"
            }
        }
    }

    private fun extractJsonString(raw: String?): String {
        if (raw == null) return "{}"
        var clean = raw.trim()

        // 1. Remove markdown code blocks and reasoning blocks
        clean = clean.replace(Regex("(?s)<think>.*?</think>"), "").trim()
        clean = clean.replace(Regex("```json\\s*"), "").trim()
        clean = clean.replace(Regex("```\\s*"), "").trim()

        // 2. Fix partial or invalid literal "nul" to "null"
        clean = clean.replace(Regex(":\\s*nul\\b"), ": null")

        // 3. Extract exact JSON boundaries from first { to last }
        val startIdx = clean.indexOf("{")
        val endIdx = clean.lastIndexOf("}")
        if (startIdx < 0 || endIdx < 0 || endIdx < startIdx) return "{}"
        
        clean = clean.substring(startIdx, endIdx + 1).trim()
        
        // 4. Try to balance brackets and build a structurally valid JSON if truncated
        clean = balanceAndFixJson(clean)

        return clean
    }

    private fun balanceAndFixJson(json: String): String {
        var clean = json.trim()
        var inQuote = false
        var escape = false
        val braceStack = mutableListOf<Char>()
        val sb = StringBuilder()
        
        for (i in clean.indices) {
            val c = clean[i]
            sb.append(c)
            if (escape) {
                escape = false
                continue
            }
            if (c == '\\') {
                escape = true
                continue
            }
            if (c == '"') {
                inQuote = !inQuote
                continue
            }
            if (!inQuote) {
                if (c == '{' || c == '[') {
                    braceStack.add(c)
                } else if (c == '}' || c == ']') {
                    if (braceStack.isNotEmpty()) {
                        val last = braceStack.last()
                        if ((c == '}' && last == '{') || (c == ']' && last == '[')) {
                            braceStack.removeAt(braceStack.size - 1)
                        }
                    }
                }
            }
        }
        
        var repaired = sb.toString().trim()
        
        repaired = repaired.replace(Regex(",\\s*([}\\]])"), "$1")
        if (repaired.endsWith(",")) {
            repaired = repaired.dropLast(1).trim()
        }
        
        if (inQuote) {
            repaired += "\""
        }
        
        while (braceStack.isNotEmpty()) {
            val last = braceStack.removeAt(braceStack.size - 1)
            repaired = repaired.trim()
            if (repaired.endsWith(",")) {
                repaired = repaired.dropLast(1).trim()
            }
            repaired += if (last == '{') "}" else "]"
        }
        
        return repaired
    }

    suspend fun saveActiveKeys(newKey: String, newProvider: String, newModel: String, newCustomEndpoint: String) {
        settingsDataStore.saveSettings(newKey, newProvider, newModel, newCustomEndpoint)
        _infoEvents.emit("Credentials persistent successfully.")
    }

    fun saveRotatorKeys(keys: Map<String, String>) {
        viewModelScope.launch {
            settingsDataStore.saveRotatorKeys(keys)
            _infoEvents.emit("Rotator API keys saved successfully.")
        }
    }

    fun saveRotatorEnabledModels(models: Set<String>) {
        viewModelScope.launch {
            settingsDataStore.saveRotatorEnabledModels(models)
            _infoEvents.emit("Rotator model selection updated.")
        }
    }

    fun saveRotatorCustomModels(customModels: Map<String, List<String>>) {
        viewModelScope.launch {
            settingsDataStore.saveRotatorCustomModels(customModels)
        }
    }

    // Ping check connection helper for Settings UX
    fun testConnection(
        testKey: String,
        testProvider: String,
        testModel: String,
        testCustomEndpoint: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val activeKey = resolveActiveApiKey(testProvider, testKey, testCustomEndpoint)

                if (activeKey.isBlank()) {
                    onResult(false, "API Key is required to test connection.")
                    return@launch
                }

                val testPrompt = "Return a valid JSON string: { \"status\": \"success\" }. Perform no other actions."

                val response = makeDirectApiCall(
                    provider = testProvider,
                    modelName = testModel,
                    apiKey = activeKey,
                    systemPrompt = testPrompt,
                    customUrl = testCustomEndpoint
                )
                val clean = extractJsonString(response)
                if (clean.contains("\"status\"") || clean.contains("success") || clean.isNotBlank()) {
                    onResult(true, "Handshake verified successfully with $testProvider!")
                } else {
                    onResult(false, "Unexpected response format returned from AI provider.")
                }
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Handshake failed due to network errors.")
            }
        }
    }

    // Lawsuit Simulation State properties
    private val _lawsuitActive = MutableStateFlow(false)
    val lawsuitActive: StateFlow<Boolean> = _lawsuitActive.asStateFlow()

    private val _courtroomPatientLog = MutableStateFlow("")
    val courtroomPatientLog: StateFlow<String> = _courtroomPatientLog.asStateFlow()

    private val _selectedJustificationLaws = MutableStateFlow<List<String>>(emptyList())
    val selectedJustificationLaws: StateFlow<List<String>> = _selectedJustificationLaws.asStateFlow()

    fun toggleJustificationLaw(lawTitle: String) {
        val current = _selectedJustificationLaws.value.toMutableList()
        if (current.contains(lawTitle)) {
            current.remove(lawTitle)
        } else {
            current.add(lawTitle)
        }
        _selectedJustificationLaws.value = current
    }

    fun clearJustificationLaws() {
        _selectedJustificationLaws.value = emptyList()
    }

    private val _lawsuitLog = MutableStateFlow<List<String>>(emptyList())
    val lawsuitLog: StateFlow<List<String>> = _lawsuitLog.asStateFlow()

    private val _lawsuitPatientName = MutableStateFlow("")
    val lawsuitPatientName: StateFlow<String> = _lawsuitPatientName.asStateFlow()

    private val _lawsuitCaseDiag = MutableStateFlow("")
    val lawsuitCaseDiag: StateFlow<String> = _lawsuitCaseDiag.asStateFlow()

    private val _lawsuitCharges = MutableStateFlow<List<String>>(emptyList())
    val lawsuitCharges: StateFlow<List<String>> = _lawsuitCharges.asStateFlow()

    private val _lawsuitTension = MutableStateFlow(50) // 0-100%
    val lawsuitTension: StateFlow<Int> = _lawsuitTension.asStateFlow()

    private val _lawsuitProsecutorAggression = MutableStateFlow(50) // 0-100%
    val lawsuitProsecutorAggression: StateFlow<Int> = _lawsuitProsecutorAggression.asStateFlow()

    private val _lawsuitVerdict = MutableStateFlow<String?>(null)
    val lawsuitVerdict: StateFlow<String?> = _lawsuitVerdict.asStateFlow()

    private val _lawsuitFine = MutableStateFlow(0.0)
    val lawsuitFine: StateFlow<Double> = _lawsuitFine.asStateFlow()

    private val _lawsuitSuspension = MutableStateFlow(0) // weeks
    val lawsuitSuspension: StateFlow<Int> = _lawsuitSuspension.asStateFlow()

    private val _lawsuitCurrentStage = MutableStateFlow("init") // "init", "charges", "cross_exam", "verdict"
    val lawsuitCurrentStage: StateFlow<String> = _lawsuitCurrentStage.asStateFlow()

    private val _lawsuitViolatedPolicies = MutableStateFlow<List<PolicyAuditResult>>(emptyList())
    val lawsuitViolatedPolicies: StateFlow<List<PolicyAuditResult>> = _lawsuitViolatedPolicies.asStateFlow()

    // --- HIGH CRIMINAL COURT STATE ---
    private val _criminalCourtActive = MutableStateFlow(false)
    val criminalCourtActive: StateFlow<Boolean> = _criminalCourtActive.asStateFlow()

    private val _criminalCourtLog = MutableStateFlow<List<String>>(emptyList())
    val criminalCourtLog: StateFlow<List<String>> = _criminalCourtLog.asStateFlow()

    private val _criminalCourtTension = MutableStateFlow(80)
    val criminalCourtTension: StateFlow<Int> = _criminalCourtTension.asStateFlow()

    private val _criminalCourtVerdict = MutableStateFlow<String?>(null)
    val criminalCourtVerdict: StateFlow<String?> = _criminalCourtVerdict.asStateFlow()

    private val _criminalCourtStage = MutableStateFlow("init") // "init", "trial", "verdict"
    val criminalCourtStage: StateFlow<String> = _criminalCourtStage.asStateFlow()

    private val _criminalChargesText = MutableStateFlow("")
    val criminalChargesText: StateFlow<String> = _criminalChargesText.asStateFlow()

    private val _criminalCourtJailDays = MutableStateFlow(0)
    val criminalCourtJailDays: StateFlow<Int> = _criminalCourtJailDays.asStateFlow()

    fun dismissCriminalCourt() {
        if (_criminalCourtJailDays.value > 0) {
            viewModelScope.launch {
                settingsDataStore.advanceDays(_criminalCourtJailDays.value)
                _criminalCourtActive.value = false
                _criminalCourtVerdict.value = null
                _criminalCourtJailDays.value = 0
            }
        } else {
            _criminalCourtActive.value = false
            _criminalCourtVerdict.value = null
            _criminalCourtJailDays.value = 0
        }
    }

    fun bribeCriminalJustice() {
        if (clinicBalance.value >= 15000.0) {
            viewModelScope.launch {
                settingsDataStore.updateClinicStats(clinicBalance.value - 15000.0, reputationStars.value)
                settingsDataStore.addDailyExpenses(15000.0)
            }
            
            val logs = _criminalCourtLog.value.toMutableList()
            logs.add("💼 SUB-ROSA SETTLEMENT:\nAn undisclosed offshore transaction of $15,000 has been routed to the presiding justice's blind trust. Tension severely lowered.")
            _criminalCourtLog.value = logs
            
            _criminalCourtTension.value = (_criminalCourtTension.value - 60).coerceAtLeast(10)
        }
    }

    // --- 🏛️ ON-DEMAND CONSTITUTIONAL REVIEW STATE ---
    private val _onDemandCourtActive = MutableStateFlow(false)
    val onDemandCourtActive: StateFlow<Boolean> = _onDemandCourtActive.asStateFlow()

    private val _onDemandCourtLawId = MutableStateFlow("")
    val onDemandCourtLawId: StateFlow<String> = _onDemandCourtLawId.asStateFlow()

    private val _onDemandCourtLog = MutableStateFlow<List<String>>(emptyList())
    val onDemandCourtLog: StateFlow<List<String>> = _onDemandCourtLog.asStateFlow()

    private val _onDemandCourtJurySentiment = MutableStateFlow(40) // 0-100%
    val onDemandCourtJurySentiment: StateFlow<Int> = _onDemandCourtJurySentiment.asStateFlow()

    private val _onDemandCourtTension = MutableStateFlow(40) // 0-100%
    val onDemandCourtTension: StateFlow<Int> = _onDemandCourtTension.asStateFlow()

    private val _onDemandCourtStage = MutableStateFlow("init") // "init", "cross", "verdict"
    val onDemandCourtStage: StateFlow<String> = _onDemandCourtStage.asStateFlow()

    private val _onDemandCourtVerdictText = MutableStateFlow("")
    val onDemandCourtVerdictText: StateFlow<String> = _onDemandCourtVerdictText.asStateFlow()

    private val _onDemandCourtR0Text = MutableStateFlow("")
    val onDemandCourtR0Text: StateFlow<String> = _onDemandCourtR0Text.asStateFlow()

    fun startOnDemandConstitutionalCourt(lawId: String) {
        val policy = activePolicies.value.find { it.id == lawId } ?: return
        _onDemandCourtActive.value = true
        _onDemandCourtLawId.value = lawId
        _onDemandCourtJurySentiment.value = 45
        _onDemandCourtTension.value = 50
        _onDemandCourtStage.value = "init"
        _onDemandCourtVerdictText.value = ""
        SovereignHearingDocketHandler.resetOnDemandDocket()
        
        val details = """
            TITLE: ${policy.title}
            SUMMARY: ${policy.summary}
            CLINICAL CONSTRAINT: ${policy.clinicalRule}
            ECONOMIC IMPACT: ${policy.economicImpact}
            EXTENDED CLAUSES: ${policy.extendedClauses.joinToString("; ")}
        """.trimIndent()
        _onDemandCourtR0Text.value = details

        val initialLog = mutableListOf<String>()
        initialLog.add("🏛️ CONSTITUTIONAL PETITION FILED: Dr. Tim vs. The Parliamentary Statute '${policy.title}'.\n" +
                "Location: High Constitutional Tribunal Chamber, Sovereign Capital.\n\n" +
                "Chief Justice Vance: 'The High Tribunal Bench is now in session to hear the constitutional challenge against Parliamentary Bill '${policy.title}' (ID: ${policy.id}). The petitioner, Dr. Tim, claims that this statutory constraint unlawfully infringes standard healthcare delivery and requests a nationwide judicial strike-down.'\n\n" +
                "State Prosecuting Advocate: 'Your Honor, this law is a vital pillar of the public safeties of ${countryName.value}! We request the Bench dismiss this petition out-of-hand. Petitioner, state your primary constitutional and clinical grievances!'")
        
        _onDemandCourtLog.value = initialLog
    }

    fun argueOnDemandConstitutionalCourt(pleaMsg: String) {
        if (pleaMsg.isBlank()) return
        val lawId = _onDemandCourtLawId.value
        val policy = activePolicies.value.find { it.id == lawId } ?: return

        val hearingDetails = SovereignHearingDocketHandler.getOnDemandHearingDetails()
        val hearingContext = """
            - CURRENT COURT STAGE: ${hearingDetails.title} (Step ${hearingDetails.index})
            - STEP FOCUS: ${hearingDetails.subtitle}
            - STANDING REQUIREMENT: ${hearingDetails.requirementsHint}
        """.trimIndent()

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val currentProvider = provider.value
                val currentModel = model.value
                val userKey = apiKey.value ?: ""
                val activeKey = resolveActiveApiKey(currentProvider, userKey)
                val currentLogText = _onDemandCourtLog.value.joinToString("\n\n")

                val prompt = """
                    You are simulating an intense constitutional statutory challenge hearing in the Supreme Court of ${countryName.value}, before Chief Justice Vance and a 6-person tribunal.
                    
                    THE LAW BEING CHALLENGED:
                    - ID: ${policy.id}
                    - Title: ${policy.title}
                    - Clinical Constraint: ${policy.clinicalRule}
                    - Economic Impact: ${policy.economicImpact}
                    - Extended Clauses: ${policy.extendedClauses.joinToString("; ")}
                    
                    COURT SN SNAPSHOTS:
                    $hearingContext
                    - Current Log History (dialogue thus far):
                    ${'$'}currentLogText
                    
                    DEFENDANT'S CORE SPEECH / PLEA:
                    "${'$'}pleaMsg"
                    
                    YOUR JOB:
                    1. Roleplay the intellectual, sharp voice of the State Prosecuting Advocate defending the law, and the strict, high-standing guidance of Chief Justice Vance.
                    2. Evaluate how the Tribunal and Jury sentiment shifts. If Dr. Tim presents compelling clinical reasons, increase 'jurySentiment' and decrease 'courtTension'. If their defense is weak, irrelevant or purely financial, decrease sentiment and spike tension.
                    3. Return raw JSON matching this schema:
                    {
                       "courtDialogue": "Chief Justice Vance's questioning of the record, or prosecuting attorney's rebuttal.",
                       "sentimentAdjustment": 12,
                       "tensionAdjustment": -8,
                       "isVerdictReady": false,
                       "guidanceTip": "Tip from your clerk or representation on how to advance your pleading."
                    }
                """.trimIndent()

                val rawResponse = makeFreshDirectApiCall(currentProvider, currentModel, activeKey, prompt, customEndpoint.value)
                val sanitized = extractJsonString(rawResponse)
                val json = JSONObject(sanitized)

                val dialogue = json.optString("courtDialogue", "Prosecution objects to the petitioner's assertions.")
                val sAdj = json.optInt("sentimentAdjustment", 0)
                val tAdj = json.optInt("tensionAdjustment", 5)
                val tip = json.optString("guidanceTip", "Attach high-quality retraining proof or cite clinical guidelines to win support.")

                val logs = _onDemandCourtLog.value.toMutableList()
                logs.add("🗣️ PETITIONER'S GRIEVANCE:\n\"${'$'}pleaMsg\"")
                logs.add("👨‍⚖️ TRIBUNAL RECORD:\n${'$'}dialogue\n\n📌 CLERK NOTE: ${'$'}tip")
                _onDemandCourtLog.value = logs

                _onDemandCourtJurySentiment.value = (_onDemandCourtJurySentiment.value + sAdj).coerceIn(10, 100)
                _onDemandCourtTension.value = (_onDemandCourtTension.value + tAdj).coerceIn(10, 100)
                _onDemandCourtStage.value = "cross"

            } catch (e: Exception) {
                logAndEmitError("Court argument failed: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun concludeOnDemandConstitutionalCourt() {
        val lawId = _onDemandCourtLawId.value
        val policy = activePolicies.value.find { it.id == lawId } ?: return

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val currentProvider = provider.value
                val currentModel = model.value
                val userKey = apiKey.value ?: ""
                val activeKey = resolveActiveApiKey(currentProvider, userKey)
                val currentLogText = _onDemandCourtLog.value.joinToString("\n\n")

                val prompt = """
                    You are Chief Justice Vance rendering the final binding CONSTITUTIONAL DECREE of the High Tribunal in the case of Dr. Tim vs. Statutory Law '${policy.title}'.
                    
                    HEARING SUMMARY:
                    ${'$'}currentLogText
                    
                    COURT METRICS:
                    - Bench/Jury Sympathy Sentiment: ${_onDemandCourtJurySentiment.value}%
                    - Accusation Tension: ${_onDemandCourtTension.value}%
                    
                    DECISION MECHANICS:
                    - If Sympathy Sentiment is >= 65%, the Tribunal agrees that the law is unconstitutional and will REPEAL/STRIKE DOWN the law completely.
                    - If Sympathy Sentiment is < 65%, the Tribunal rejects the challenge. The law remains enacted, and Dr. Tim is fined a $1,000 court processing fee for frivolous litigation.
                    
                    YOUR JOB:
                    1. Deliver a grand, authoritative final judicial statement (minimum 2 detailed paragraphs). Discuss the balance of public safety vs clinical liberties, citing specific comments from your earlier hearings.
                    2. Clearly state whether the petition is GRANTED (repealed) or DENIED.
                    3. Return raw JSON matching this schema:
                    {
                       "success": true, // true if sympathy >= 65% else false
                       "decreeText": "The formal historical decree read by Chief Justice Vance..."
                    }
                """.trimIndent()

                val rawResponse = makeFreshDirectApiCall(currentProvider, currentModel, activeKey, prompt, customEndpoint.value)
                val sanitized = extractJsonString(rawResponse)
                val json = JSONObject(sanitized)

                val success = json.optBoolean("success", _onDemandCourtJurySentiment.value >= 65)
                val decree = json.optString("decreeText", "The Judicial Bench has concluded hearings under statutory protocols.")

                val logs = _onDemandCourtLog.value.toMutableList()
                logs.add("⚖️ SUPREME COURT OF RULING - CONSTITUTIONAL DECREE ISSUED:\n${'$'}decree")
                _onDemandCourtLog.value = logs

                _onDemandCourtVerdictText.value = decree
                _onDemandCourtStage.value = "verdict"

                if (success) {
                    legalWorldAgent.repealStatute(lawId)
                    val newVotingLogs = _votingLog.value.toMutableList()
                    newVotingLogs.add("🏛️ [CONSTITUTIONAL REVIEW] The Supreme Court ruled in favor of Dr. Tim! The law '${policy.title}' is struck down nationwide!")
                    _votingLog.value = newVotingLogs
                    settingsDataStore.addXp(1200L) // Gain major XP for striking down a law!
                } else {
                    settingsDataStore.updateClinicStats(clinicBalance.value - 1000.0, reputationStars.value)
                    settingsDataStore.addDailyExpenses(1000.0)
                    val newVotingLogs = _votingLog.value.toMutableList()
                    newVotingLogs.add("🏛️ [CONSTITUTIONAL REVIEW] Dr. Tim's challenge against '${policy.title}' was struck down. Paid a $1,000 legal fee.")
                    _votingLog.value = newVotingLogs
                }

            } catch (e: Exception) {
                logAndEmitError("Court finalization failed: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun dismissOnDemandConstitutionalCourt() {
        _onDemandCourtActive.value = false
        _onDemandCourtLawId.value = ""
        _onDemandCourtLog.value = emptyList()
        _onDemandCourtVerdictText.value = ""
    }

    fun initiateCivilSuitAgainstPatient(reason: String) {
        val patientName = _uiState.value.intakeFormData?.run { "$firstName $surname" } ?: _lawsuitPatientName.value 
        val patientInfo = patientName.takeIf { it.isNotBlank() } ?: "Current Patient"
        _lawsuitActive.value = false
        _criminalCourtActive.value = true
        _criminalCourtVerdict.value = null
        _criminalCourtTension.value = 50
        _criminalCourtStage.value = "init"
        _criminalChargesText.value = "The Clinic is SUING Patient: $patientInfo.\nGrounds/Reason: $reason\n\nYou are acting as the Plaintiff. Present your case to the Justice!"
        
        val initialLog = mutableListOf<String>()
        initialLog.add("🧑‍⚖️ CIVIL CLAIMS COURT OF ${countryName.value.uppercase()} 🧑‍⚖️")
        initialLog.add("Location: Civil Damages Division\n\nPresiding Justice: 'The Medical Practitioner has filed a civil complaint against the patient: $patientInfo.\nReason: $reason.\n\nDoctor, present your arguments for damages.'")
        _criminalCourtLog.value = initialLog
    }

    fun startCriminalCourt(reason: String) {
        _lawsuitActive.value = false // Close standard lawsuit if open
        _criminalCourtActive.value = true
        _criminalCourtVerdict.value = null
        _criminalCourtTension.value = 90
        _criminalCourtStage.value = "init"
        _criminalChargesText.value = reason
        
        val initialLog = mutableListOf<String>()
        initialLog.add("🚨 HIGH CRIMINAL COURT OF ${countryName.value.uppercase()} 🚨")
        initialLog.add("Location: Highest Federal Judiciary Bench\n\nPresiding Grand Justice: 'The defendant is brought before the High Criminal Court. State Intelligence has intercepted illegal activities: $reason.'")
        initialLog.add("Federal Prosecutor: 'Your Honor, the State charges the defendant with Capital Subversion of Justice. This court demands maximum sentencing!'")
        _criminalCourtLog.value = initialLog
    }

    fun dismissLawsuit() {
        _lawsuitActive.value = false
        _lawsuitViolatedPolicies.value = emptyList()
    }

    fun startLawsuitSimulation(
        patientName: String = "", 
        caseDiagnosis: String = "", 
        score: Int = 45,
        violations: List<PolicyAuditResult> = emptyList()
    ) {
        _lawsuitActive.value = true
        _lawsuitVerdict.value = null
        _lawsuitFine.value = 0.0
        _lawsuitSuspension.value = 0
        _lawsuitTension.value = 65
        _lawsuitProsecutorAggression.value = 70
        _lawsuitCurrentStage.value = "charges"
        SovereignHearingDocketHandler.resetDocket()

        _selectedJustificationLaws.value = emptyList()

        val targetName = patientName.takeIf { it.isNotBlank() } ?: "Sipho Mokoena"
        val targetDiag = caseDiagnosis.takeIf { it.isNotBlank() } ?: "Schizophrenia"
        val targetScore = score

        _lawsuitPatientName.value = targetName
        _lawsuitCaseDiag.value = targetDiag
        _lawsuitViolatedPolicies.value = violations

        // Assemble compiling patient log
        val currentState = _uiState.value
        val hiddenCaseVal = _hiddenCase.value
        
        val fullLogBuilder = StringBuilder()
        fullLogBuilder.append("=== COMPREHENSIVE PATIENT CLINICAL HISTORY LOG ===\n")
        fullLogBuilder.append("Patient: $targetName\n")
        fullLogBuilder.append("Demographics: ${currentState.patientDemographics}\n")
        if (hiddenCaseVal != null) {
            fullLogBuilder.append("True Diagnosis: ${hiddenCaseVal.trueDiagnosis}\n")
            fullLogBuilder.append("Severity: ${hiddenCaseVal.severity}\n")
            fullLogBuilder.append("Insurance Tier: ${hiddenCaseVal.insuranceStatus}\n")
            fullLogBuilder.append("Chief Complaint: ${hiddenCaseVal.chiefComplaint}\n")
            fullLogBuilder.append("Clinician Assessment (DDx Notes): ${currentState.ddxNotes}\n")
        }
        
        fullLogBuilder.append("\n=== INITIAL ENCOUNTER VITALS ===\n")
        currentState.vitals?.let {
            fullLogBuilder.append("- Blood Pressure: ${it.bp}\n")
            fullLogBuilder.append("- Heart Rate: ${it.hr}\n")
            fullLogBuilder.append("- Temp: ${it.tempC}°C\n")
            fullLogBuilder.append("- Resp Rate: ${it.rr}\n")
            fullLogBuilder.append("- SpO2: ${it.spo2}\n")
        }
        
        fullLogBuilder.append("\n=== DIAGNOSTICS & EXAMINATIONS ===\n")
        fullLogBuilder.append("- Lab Results: ${currentState.labResults ?: "None Checked"}\n")
        fullLogBuilder.append("- Physical Exam: ${currentState.physicalExamResults ?: "None Checked"}\n")
        
        fullLogBuilder.append("\n=== THERAPEUTIC SUMMARY ===\n")
        fullLogBuilder.append("- Submitted Diagnosis: ${currentState.submittedDiagnosis}\n")
        fullLogBuilder.append("- Submitted Treatment Plan: ${currentState.submittedTreatmentPlan}\n")
        
        fullLogBuilder.append("\n=== CHRONOLOGICAL BEDSIDE CHAT TRANSCRIPTS ===\n")
        currentState.chatHistory.forEach { msg ->
            val speaker = when (msg.role) {
                "doctor", "user" -> "DR. TIM"
                "patient" -> "PATIENT"
                "system" -> "SYSTEM DIRECTIVE"
                else -> msg.role.uppercase()
            }
            if (!msg.text.contains("Generating a completely randomized new case")) {
                fullLogBuilder.append("[$speaker]: ${msg.text}\n")
            }
        }
        
        fullLogBuilder.append("\n=== NATIONAL CLINICAL PERFORMANCE REPORT ===\n")
        fullLogBuilder.append(currentState.evaluation ?: "No formal audit recorded.")
        _courtroomPatientLog.value = fullLogBuilder.toString()

        val chargesList = mutableListOf<String>()
        if (violations.isNotEmpty()) {
            violations.forEachIndexed { index, v ->
                val details = v.auditMessage.replace("🚨 VIOLATION: ", "")
                chargesList.add("${index + 1}. STATUTORY BREACH [${v.policyTitle}]: Failed clinical compliance mandate. Violation: \"${v.triggeredClause}\". details: $details")
            }
        } else {
            chargesList.add("1. Clinical Mismanagement: Received a deficient medical simulation score of $targetScore/100 failing statutory professional standards.")
            chargesList.add("2. Treatment Inadequacy: Inappropriate clinical therapy and drug selection for $targetDiag.")
            chargesList.add("3. Breach of general patient safety regulations and health standards.")
        }
        _lawsuitCharges.value = chargesList

        val initialLog = mutableListOf<String>()
        val initialLine = if (violations.isNotEmpty()) {
            "🏛️ HIGH COURT OF ${countryName.value.uppercase()} - SOVEREIGN JUDICIARY DEPT\nLocation: Supreme Inquest division, Sovereign Capital District\n\nPresiding Judge: 'Practitioner, this sovereign court has convened a formal compliance trial. The National Health Inspectorate has logged clinical violations under our actively enacted legislative policies during your treatment of patient $targetName for $targetDiag.'\n\nState Prosecutor: 'Your Honor, the State charges the practitioner with ${violations.size} counted violations of our nation's sovereign health statutes. The clinic bypassed mandatory legislative guidelines, failing public safety! How does the defense plead?'"
        } else {
            "🏛️ HIGH COURT OF ${countryName.value.uppercase()} - SOVEREIGN JUDICIARY DEPT\nLocation: Supreme Inquest division, Sovereign Capital District\n\nPresiding Judge: 'Practitioner, you have been summoned to face this sovereign judicial inquest. A civil malpractice and negligence complaint has been filed regarding your care of patient $targetName for $targetDiag with a clinical competency rating of only $targetScore/100.'\n\nState Prosecutor: 'Your Honor, we charge the accused with clinical negligence and gross malpractice failing the baseline treaties of ${countryName.value}. How does the practitioner plead?'"
        }
        initialLog.add(initialLine)
        _lawsuitLog.value = initialLog

        // Populate SSSA & Judiciary Evidence Pool from actual encounter parameters
        val caseVitalsText = _uiState.value.vitals?.let { "BP: ${it.bp}, HR: ${it.hr}, TempC: ${it.tempC}, SpO2: ${it.spo2}" } ?: "Not Monitored"
        val labResultsStr = _uiState.value.labResults
        val policyViolationsSummary = violations.joinToString("; ") { it.policyTitle }
        OrchidDeepStateManager.setEvidencePool(caseVitalsText, labResultsStr, policyViolationsSummary)
        val policies = activePolicies.value
        val maxPleaRounds = policies.maxOfOrNull { it.maxPleaRounds } ?: 3
        OrchidDeepStateManager.resetTrialRounds(rounds = maxPleaRounds)
    }

    fun challengeStatuteConstitutionality(policyId: String) {
        val policy = activePolicies.value.find { it.id == policyId } ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Settle some trial round / pay Supreme Court Processing fee
                settingsDataStore.updateClinicStats(clinicBalance.value - 500.0, reputationStars.value)
                settingsDataStore.addDailyExpenses(500.0)

                // Repeal law
                legalWorldAgent.repealStatute(policyId)
                
                // Log constitutional challenge success!
                val currentLawsuitLogs = _lawsuitLog.value.toMutableList()
                currentLawsuitLogs.add("⚖️ SUPREME COURT CONSTITUTIONAL OVERRIDE DECREE:\n" +
                    "The High Tribunal Bench has reviewed the constitutional challenge against the validity of the active statute: '${policy.title}' (ID: ${policy.id}).\n\n" +
                    "RULING SUMMARY: By majority vote, the Court finds this statute disproportionate, unconstitutional, and a direct threat to standard clinical liberties.\n\n" +
                    "CONSEQUENCE: THE COURT OVERPOWERS THE PARLIAMENTARY LAW. The statute is struck down and REPEALED with immediate effect nationwide!")
                
                _lawsuitLog.value = currentLawsuitLogs
                _lawsuitTension.value = (_lawsuitTension.value - 30).coerceAtLeast(0)
                _lawsuitProsecutorAggression.value = (_lawsuitProsecutorAggression.value - 25).coerceAtLeast(0)

                // Exclude this policy from charges
                val currentCharges = _lawsuitCharges.value.filterNot { it.contains(policy.title, ignoreCase = true) }
                _lawsuitCharges.value = currentCharges

                val newVotingLogs = _votingLog.value.toMutableList()
                newVotingLogs.add("🏛️ The Supreme Court overpowered and struck down the active law '${policy.title}' (ID: ${policy.id}) as unconstitutional!")
                _votingLog.value = newVotingLogs

            } catch (e: Exception) {
                logAndEmitError("Court petition failed: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startLicenseAppealSimulation() {
        val currentStatus = worldSnapshot.value?.licenseStatus?.name ?: "SUSPENDED"

        _lawsuitActive.value = true
        _lawsuitVerdict.value = null
        _lawsuitFine.value = 0.0
        _lawsuitSuspension.value = 0
        _lawsuitTension.value = 50
        _lawsuitProsecutorAggression.value = 50
        _lawsuitCurrentStage.value = "charges"
        SovereignHearingDocketHandler.resetDocket()

        _selectedJustificationLaws.value = emptyList()

        val appealTarget = "Supreme Court of ${countryName.value}"
        val appealCase = "License Appeal Petition"
        
        _lawsuitPatientName.value = appealTarget
        _lawsuitCaseDiag.value = appealCase
        
        val charges = listOf("1. Formal petition to reinstate $currentStatus medical license.", "2. Review of practitioner's legal standing and rehabilitation.")
        _lawsuitCharges.value = charges

        _courtroomPatientLog.value = "=== APPEAL DOSSIER ===\nPractitioner is actively petitioning to overturn the $currentStatus status of their medical license and restore clinical practice rights."
        
        val initialLog = mutableListOf<String>()
        initialLog.add("🏛️ HIGH COURT OF ${countryName.value.uppercase()} - SOVEREIGN JUDICIARY DEPT\nLocation: Supreme Inquest division, Sovereign Capital District\n\nPresiding Judge: 'Practitioner, this sovereign court has convened to review your formal appeal petition for the reinstatement of your $currentStatus medical license.'\n\nState Prosecutor: 'Your Honor, the State notes the petitioner's prior offenses and suspended status. The practitioner must strongly justify why their practice rights should be legally reinstated today under our current health statutes. How does the petitioner plead?'")
        _lawsuitLog.value = initialLog

        viewModelScope.launch {
            OrchidDeepStateManager.setEvidencePool("Past Audit Records", "None", "")
            val policies = settingsDataStore.activePoliciesFlow.first()
            val jurySize = policies.maxOfOrNull { it.jurySize } ?: 10
            val maxPleaRounds = policies.maxOfOrNull { it.maxPleaRounds } ?: 3
            OrchidDeepStateManager.resetTrialRounds(rounds = maxPleaRounds)

            courtroomViewModel.resetLawsuit(
                patientName = appealTarget,
                diag = appealCase,
                charges = charges,
                jurySize = jurySize
            )
            
            generateAIJuryBackground(appealTarget, appealCase, charges)
        }
    }

    // --- NEW DEEP STATE APIS (RESTOCK & DISPENSING ACTIONS) ---

    fun buyDispensaryRestock(itemId: String, quantity: Int) {
        val currentBal = clinicBalance.value
        val result = OrchidDeepStateManager.restockItem(itemId, quantity, currentBal)
        if (result != null) {
            val totalCost = result.first
            val successMsg = result.second
            viewModelScope.launch {
                settingsDataStore.updateClinicStats(currentBal - totalCost, reputationStars.value)
                settingsDataStore.addDailyExpenses(totalCost)
                val newLogs = _votingLog.value.toMutableList()
                newLogs.add("📦 $successMsg deducted ${String.format("%.2f", totalCost)} from clinic funds.")
                _votingLog.value = newLogs
            }
        } else {
            logAndEmitError("Cannot restock: Insufficient clinic funds!")
        }
    }

    fun dispenseDispensaryItemToPatient(itemId: String) {
        if (_isLoading.value || _uiState.value.isEncounterComplete) return
        
        val item = OrchidDeepStateManager.availableCatalog.find { it.id == itemId } ?: return
        val map = OrchidDeepStateManager.dispensaryInventory.value
        val stock = map[itemId] ?: 0
        if (stock <= 0) {
            logAndEmitError("Cannot dispense ${item.name}: Stock is empty! Restock in the Sovereign Deep State/Cabinet center first.")
            return
        }

        // Consume stock
        OrchidDeepStateManager.consumeItem(itemId)
        OrchidDeepStateManager.recordDispensation(itemId)

        // Modify vitals locally
        val oldVitals = _uiState.value.vitals ?: Vitals("120/80", "75", 37.0, "16", "98%")
        var currentBPString = oldVitals.bp ?: "120/80"
        var currentHRString = oldVitals.hr ?: "75"
        var currentTemp = oldVitals.tempC ?: 37.0
        var currentSPO2 = oldVitals.spo2 ?: "98"
        var currentRR = oldVitals.rr ?: "16"

        when (itemId) {
            "saline" -> {
                currentBPString = shiftBP(currentBPString, 12)
                currentHRString = shiftHR(currentHRString, -5)
            }
            "adrenaline" -> {
                currentBPString = shiftBP(currentBPString, 25)
                currentHRString = shiftHR(currentHRString, 30)
                currentRR = "22"
            }
            "gtn_spray" -> {
                currentBPString = shiftBP(currentBPString, -20)
                currentHRString = shiftHR(currentHRString, 10)
            }
            "morphine" -> {
                currentBPString = shiftBP(currentBPString, -5)
                currentHRString = shiftHR(currentHRString, -12)
                currentRR = "11"
            }
            "orchid_serum" -> {
                currentBPString = "120/80"
                currentHRString = "75"
                currentTemp = 37.0
                currentSPO2 = "100%"
                currentRR = "14"
            }
        }

        val newVitals = oldVitals.copy(
            _bp = currentBPString,
            _hr = currentHRString,
            _tempC = currentTemp,
            _rr = currentRR,
            _spo2 = currentSPO2
        )

        val updatedHistory = _uiState.value.chatHistory.toMutableList()
        val formattedTime = String.format("%02d:%02d", (_uiState.value.virtualTimeElapsed / 60) + 8, _uiState.value.virtualTimeElapsed % 60)
        updatedHistory.add(ChatMessage("system", "System Action: Administered ${item.name} (${item.classification}) to patient.", virtualTimestampStr = formattedTime))

        _uiState.value = _uiState.value.copy(
            vitals = newVitals,
            chatHistory = updatedHistory,
            virtualTimeElapsed = _uiState.value.virtualTimeElapsed + 5
        )
        saveCurrentStateToDatabase()

        // Call LLM for physical effect
        performAiAction(
            systemInstructionOverride = """
                The doctor has physically dispensed and administered '${item.name}' (${item.classification}) to you.
                Item description: '${item.description}'.
                Your vitals have clinically updated to: BP $currentBPString, Pulse $currentHRString, RR $currentRR, SpO2 $currentSPO2.
                Roleplay your response reacting specifically to this medical delivery!
                Acknowledge this specific drug and describe the immediate bodily changes (e.g. chest easing for GTN, severe anxiety/racing core for Epinephrine, or deep, warm sedative comfort for Morphine).
                If Orchid Serum was dispensed, speak directly as 'The Orchid Traitor' or 'Syndicate Operative', dropping a subtle hint that the rebel faction is grateful, while warning the doctor of the Sovereign State's intelligence agency!
            """.trimIndent()
        )
    }

    private fun shiftBP(bp: String, delta: Int): String {
        val parts = bp.split("/")
        if (parts.size == 2) {
            val sys = ((parts[0].toIntOrNull() ?: 120) + delta).coerceIn(40, 240)
            val dia = ((parts[1].toIntOrNull() ?: 80) + (delta / 2)).coerceIn(30, 150)
            return "$sys/$dia"
        }
        return bp
    }

    private fun shiftHR(hr: String, delta: Int): String {
        val count = ((hr.toIntOrNull() ?: 75) + delta).coerceIn(30, 200)
        return count.toString()
    }

    // --- COURTROOM INTERACTIVE ADVOCACY SERVICE ---

    private fun generateAIJuryBackground(patientName: String, diag: String, charges: List<String>) {
        viewModelScope.launch {
            try {
                val currentProvider = provider.value
                val currentModel = model.value
                val userKey = apiKey.value ?: ""
                val activeKey = resolveActiveApiKey(currentProvider, userKey)

                if (activeKey.isNotBlank()) {
                    val prompt = """
                        You are the judicial simulator for the Supreme Court. A new medical malpractice lawsuit has been filed.
                        Patient: $patientName
                        Condition: $diag
                        Charges: ${charges.joinToString(", ")}

                        Generate a dynamic, realistic jury panel of exactly 6 unique citizens with different occupations and initial stances towards healthcare and malpractice.
                        
                        Return raw JSON exactly matching this schema:
                        {
                           "jurors": [
                              { "name": "First Last", "role": "Occupation", "inclination": "Favorable/Skeptical/Undecided/Hostile", "comment": "A 1-sentence thought on their assignment to this specific case." }
                           ]
                        }
                    """.trimIndent()
                    
                    val responseRaw = makeFreshDirectApiCall(currentProvider, currentModel, activeKey, prompt)
                    val sanitized = extractJsonString(responseRaw)
                    val json = org.json.JSONObject(sanitized)
                    
                    val jurorsArray = json.optJSONArray("jurors")
                    if (jurorsArray != null) {
                        val newJurors = mutableListOf<Juror>()
                        for (i in 0 until jurorsArray.length()) {
                            val obj = jurorsArray.getJSONObject(i)
                            newJurors.add(
                                Juror(
                                    name = obj.optString("name", "Unknown Juror"),
                                    role = obj.optString("role", "Citizen"),
                                    inclination = obj.optString("inclination", "Undecided"),
                                    comment = obj.optString("comment", "Awaiting testimony...")
                                )
                            )
                        }
                        if (newJurors.size == 6) {
                            courtroomViewModel.updateJurors(newJurors)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore silent failure for background tasks
            }
        }
    }

    fun hireLawyerForTrial(lawyerId: String) {
        val lawyer = OrchidDeepStateManager.defenseLawyersCatalog.find { it.id == lawyerId } ?: return
        val currentBal = clinicBalance.value
        if (currentBal >= lawyer.retainerFee) {
            OrchidDeepStateManager.hireDefenseLawyer(lawyerId)
            if (lawyer.retainerFee > 0.0) {
                viewModelScope.launch {
                    settingsDataStore.updateClinicStats(currentBal - lawyer.retainerFee, reputationStars.value)
                    settingsDataStore.addDailyExpenses(lawyer.retainerFee)
                    
                    val logs = _lawsuitLog.value.toMutableList()
                    logs.add("💼 RETAINER INVOICE: Paid ${String.format("%.2f", lawyer.retainerFee)} to hire ${lawyer.displayName}.")
                    _lawsuitLog.value = logs
                }
            }
            _lawsuitProsecutorAggression.value = (_lawsuitProsecutorAggression.value - lawyer.defenseBiasPercent).coerceAtLeast(10)
            _lawsuitTension.value = (_lawsuitTension.value - (lawyer.defenseBiasPercent / 2)).coerceAtLeast(10)
        } else {
            logAndEmitError("Cannot hire lawyer: Insufficient clinic balance of ${clinicBalance.value} for retainer!")
        }
    }

    fun submitInteractiveLawsuitPlea(pleaMsg: String, selectedEvidence: List<String>) {
        if (_isLoading.value) return
        if (pleaMsg.isBlank()) {
            logAndEmitError("Courtroom requires written testimony! Please type your defense pleading.")
            return
        }
        _isLoading.value = true
        OrchidDeepStateManager.spendTrialRound()
        OrchidDeepStateManager.recordDefensePleaArgument(pleaMsg)

        val activePolList = activePolicies.value
        val policyDetailsStr = if (activePolList.isNotEmpty()) {
            val sb = java.lang.StringBuilder()
            sb.append("\nACTLY ENACTED SOVEREIGN HEALTH LAWS:")
            activePolList.forEachIndexed { idx, p ->
                sb.append("\n[LAW ${idx+1}] TITLE: ${p.title}\n")
                sb.append("  - Requirements: ${p.clinicalRule}\n")
            }
            sb.toString()
        } else "No clinical laws enacted."

        val lawyer = OrchidDeepStateManager.hiredLawyer.value
        val lawyerContext = if (lawyer != null) {
            "Accused is professionally represented by: ${lawyer.displayName} (${lawyer.specialty}). Defense Advantage Level: ${lawyer.defenseBiasPercent}%"
        } else "Accused has representing lawyer: NONE (Self-Representation)."

        val currentHistoryLog = _lawsuitLog.value.joinToString("\n\n")

        val selectedJustify = _selectedJustificationLaws.value
        val justificationContext = if (selectedJustify.isNotEmpty()) {
            "The defendant is specifically invoking these enacted laws to justify their actions or prove they complied:\n" + selectedJustify.joinToString("\n") { "- $it" }
        } else "The defendant has not specified which enacted laws they are trying to justify/disprove."

        val currentJurorsText = courtroomViewModel.lawsuitJurors.value.mapIndexed { i, j ->
            "${i + 1}. ${j.name} (${j.role}): Currently ${j.inclination} - ${j.comment}"
        }.joinToString("\n")

        val attachedCertificates = OrchidDeepStateManager.generatedCertificates.value.filter { 
            OrchidDeepStateManager.selectedCertificateIds.value.contains(it.id) 
        }
        val certsStr = if (attachedCertificates.isNotEmpty()) {
            attachedCertificates.joinToString("\n") { cert ->
                "[Accredited Professional Proof] TITLE: ${cert.title} | ID: ${cert.registrationNumber} | ISSURED BY: ${cert.issuer} | DETAILS: ${cert.verificationDetails}"
            }
        } else "None attached."

        val hearingDetails = SovereignHearingDocketHandler.getMalpracticeHearingDetails()
        val hearingContext = """
            - CURRENT COURT HEARING DOCKET STAGE: ${hearingDetails.title} (Step ${hearingDetails.index})
            - HEARING DESCRIPTION: ${hearingDetails.subtitle}
            - HEARING FOCUS REQUIREMENT: ${hearingDetails.requirementsHint}
            - STATE PROSECUTORY TIMBRE: ${hearingDetails.prosecutorTemperament}
        """.trimIndent()

        val prompt = """
            You are simulating an interactive clinical trial hearing in the Supreme Medical Court of the Republic of ${countryName.value}.
            
            SOVEREIGN COURT STATE INFO:
            - Defendant: Dr. Tim, GP (JB Consultation Practice, PR# 1234567)
            - Patient Case: Treated patient "${_lawsuitPatientName.value}" for condition "${_lawsuitCaseDiag.value}".
            $hearingContext
            - Current Courtroom Transcript & History:
            $currentHistoryLog
            
            ACTIVE LEGISLATION CODES:
            $policyDetailsStr
            
            $AGENT_POWERS_PROMPT

            COMPREHENSIVE PATIENT PERFORMANCE RECORD LOG (WHAT ACTUALLY HAPPENED AT THE BEDSIDE CLINICAL ENCOUNTER):
            ${_courtroomPatientLog.value}

            DEFENDANT'S JUSTIFIED REASONINGS:
            $justificationContext
            
            CURRENT JURY PANEL (6 CITIZENS):
            $currentJurorsText
            
            LEGAL DEFENSE DETAILS IN THIS PLEA ROUND:
            - Defendant's Written Testimony / Pleading speech: "$pleaMsg"
            - Submitted Physical Exhibits / Clinical evidence: ${if (selectedEvidence.isNotEmpty()) selectedEvidence.joinToString(", ") else "None"}
            - Submitted AI Accredited Certifications / Credentials: $certsStr
            - Legal Representation: $lawyerContext
            
            YOUR JOB IN THIS INTERIM ROUND:
            1. Roleplay the intense, sharp voice of the State Prosecutor and the impartial questioning of the Presiding Judge in Court.
            2. The state prosecutor must cross-examine the doctor's specific typed statement "$pleaMsg" and check the validity of their submitted evidence: "${selectedEvidence.joinToString("; ")}" and certifications: "$certsStr".
            3. CRITICAL AUDIT: Compare the defendant's justification claims ($justificationContext) with the actual performance record log of what happened at the bedside, as well as their new training / safety certifications ($certsStr). Verify if they are telling the truth or if they are offering a bogus distraction! For example, if they claim they complied with active legal acts by offering free care, verify if they did; if they claim compliance with diagnostics, check if they checked vitals/labs etc. Aggressively call them out in court if their excuses columns mismatch the raw patient log!
            4. If the defense makes true clinical and legal sense (it complies with the laws and standard medical protocols based on the logs, or they attach highly credentials that solve prior compliance gaps), reduce the tension and aggression metrics. If they claim compliance but the logs show they clearly broke the law or acted carelessly, aggressively call them out on it, and increase the tension and aggression metrics.
            5. Evaluate the current 6 jurors' reactions. You must update each of the 6 jurors' inclination and write a 1-sentence thought from them.
            6. Provide the prosecutor's aggressive response and the Judge's subsequent inquiry in 'courtDialogue'.
            7. Return raw JSON matching this EXACT schema:
            {
               "courtDialogue": "Prosecutor's sharp rebuttal questioning the evidence and auditing justifications against logs, followed by the Presiding Judge's formal query on the record.",
               "tensionAdjustment": -10,
               "aggressionAdjustment": -15,
               "defenseInsightText": "A quick note of guidance or strategic legal advice from Dr. Tim's hired defense lawyer.",
               "jurySentiment": 60,
               "jurorReactions": [
                  { "name": "Juror Name 1", "inclination": "Favorable/Skeptical/Undecided/Hostile", "comment": "A 1-sentence reaction..." },
                  { "name": "Juror Name 2", "inclination": "Favorable/Skeptical/Undecided/Hostile", "comment": "A 1-sentence reaction..." },
                  // ... all 6 jurors
               ]
            }
        """.trimIndent()

        viewModelScope.launch {
            try {
                val currentProvider = provider.value
                val currentModel = model.value
                val userKey = apiKey.value ?: ""
                val activeKey = resolveActiveApiKey(currentProvider, userKey)

                if (activeKey.isNotBlank()) {
                    val responseRaw = makeFreshDirectApiCall(currentProvider, currentModel, activeKey, prompt)
                    val sanitized = extractJsonString(responseRaw)
                    val json = org.json.JSONObject(sanitized)

                    val dialogue = json.optString("courtDialogue", "Prosecution submits cross-examination statement.")
                    val dAdj = json.optInt("tensionAdjustment", 5)
                    val aAdj = json.optInt("aggressionAdjustment", 5)
                    val insight = json.optString("defenseInsightText", "Ensure you back up your claims with physical vitals evidence.")
                    val jSentiment = json.optInt("jurySentiment", courtroomViewModel.lawsuitJurySentiment.value)
                    
                    val jurorReactionsArray = json.optJSONArray("jurorReactions")
                    if (jurorReactionsArray != null) {
                        val currentJurors = courtroomViewModel.lawsuitJurors.value
                        val updatedJurors = mutableListOf<Juror>()
                        for (i in 0 until jurorReactionsArray.length()) {
                            val obj = jurorReactionsArray.getJSONObject(i)
                            val name = obj.optString("name", "")
                            val inclination = obj.optString("inclination", "Undecided")
                            val comment = obj.optString("comment", "")
                            
                            val originalJuror = currentJurors.find { it.name == name } ?: currentJurors.getOrNull(i)
                            if (originalJuror != null) {
                                updatedJurors.add(originalJuror.copy(
                                    name = if (name.isNotBlank()) name else originalJuror.name,
                                    inclination = inclination,
                                    comment = comment
                                ))
                            }
                        }
                        if (updatedJurors.isNotEmpty()) {
                            courtroomViewModel.updateJurors(updatedJurors)
                        }
                    }
                    
                    courtroomViewModel.updateJurySentiment(jSentiment.coerceIn(0, 100))
                    
                    // Process potential agent actions
                    extractAndProcessActions(sanitized)

                    val logs = _lawsuitLog.value.toMutableList()
                    logs.add("🗣️ DOCTOR'S DEFENSE:\n\"$pleaMsg\"")
                    if (selectedEvidence.isNotEmpty()) {
                        logs.add("📁 SUBMITTED EVIDENCE TO COURT:\n" + selectedEvidence.joinToString("\n"))
                    }
                    if (selectedJustify.isNotEmpty()) {
                        logs.add("⚖️ CITED JUSTIFIED STATUTES:\n" + selectedJustify.joinToString(", "))
                    }
                    logs.add("👨‍⚖️ TRIBUNAL HEARINGS & INQUEST:\n$dialogue")
                    logs.add("💼 LAWYER'S INSIGHT: $insight")

                    _lawsuitLog.value = logs
                    _lawsuitTension.value = (_lawsuitTension.value + dAdj).coerceIn(10, 100)
                    _lawsuitProsecutorAggression.value = (_lawsuitProsecutorAggression.value + aAdj).coerceIn(10, 100)
                    _lawsuitCurrentStage.value = "cross_exam"
                }
            } catch (e: Exception) {
                logAndEmitError("Court connecting line error: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun concludeLawsuitInteractiveVerdict() {
        if (_isLoading.value) return
        _isLoading.value = true

        val activePolList = activePolicies.value
        val policyDetailsStr = if (activePolList.isNotEmpty()) {
            val sb = java.lang.StringBuilder()
            sb.append("\nSOVEREIGN LAWS UNDER WHICH JUDGMENT IS RENDERED:")
            activePolList.forEachIndexed { idx, p ->
                sb.append("\nLaw ${idx+1}: ${p.title} | Rule: ${p.clinicalRule}\n")
            }
            sb.toString()
        } else "No formal laws active."

        val currentHistoryLog = _lawsuitLog.value.joinToString("\n\n")
        val lawyer = OrchidDeepStateManager.hiredLawyer.value
        val activeSeledEvid = OrchidDeepStateManager.selectedEvidenceToPresent.value
        val selectedJustify = _selectedJustificationLaws.value

        val prompt = """
            You are the Presiding Judge and Supreme Justice of the High Court Medical Tribunal of ${countryName.value}.
            You are delivering the final, legally-binding VERDICT and penalty decree for Dr. Tim (JB Consultation Practice).
            
            CASE SPECIFICATION:
            - Case: Treated "${_lawsuitPatientName.value}" for "${_lawsuitCaseDiag.value}".
            
            COMPREHENSIVE PATIENT PERFORMANCE RECORD LOG (WHAT ACTUALLY HAPPENED AT BEDSIDE):
            ${_courtroomPatientLog.value}

            DEFENDANT'S CITED JUSTIFIED STATUTES:
            ${if (selectedJustify.isNotEmpty()) selectedJustify.joinToString(", ") else "None highlighted specifically."}

            Cumulative Case Court Transcript (Plea history and prosecutorial arguments):
            $currentHistoryLog
            
            EVIDENTIARY EXHIBITS RULING ON:
            - Selected evidence submitted to court: ${if (activeSeledEvid.isNotEmpty()) activeSeledEvid.joinToString(", ") else "None"}
            - Defense Representation: ${lawyer?.displayName ?: "None (Self-represented)"}
            - Court Tension Level: ${_lawsuitTension.value}%
            - Prosecution Hostility/Aggression Level: ${_lawsuitProsecutorAggression.value}%
            - AI Jury Sentiment: ${courtroomViewModel.lawsuitJurySentiment.value}% (Above 50% favors the doctor, below 50% favors the state)
            
            HEALTH STATUTES IN SCOPE:
            $policyDetailsStr
            
            $AGENT_POWERS_PROMPT
            
            YOUR DIRECTIVE:
            1. Formulate a final, realistic sentencing judgment.
            2. Evaluate whether the doctor successfully justified that they didn't violate the active clinical laws in their defense pleadings, backed up by the actual performance records log.
            3. The 6-person AI Jury panel's final voting sentiment is critical. The Judge should heavily weigh their sentiment when determining the final verdict penalty.
            4. Verdict types allowed: "Exonerated" (if jury sentiment >= 65% and tension score <= 45%), "Warning" (tension 46-60%), "Fined" (tension 61-80% or clear statutory violation in patient log), "Suspension" (tension > 80% or severe deliberate statutory breach).
            5. If Fined, define a numeric cash fine (e.g. 5000 to 15000). Deduct this from the clinic's balance.
            5. If Suspension, define the suspension weeks (e.g. 1 to 3 weeks).
            6. Return raw JSON matching this schema:
            {
               "verdictType": "Fined",
               "fineAmount": 1200.0,
               "suspensionWeeks": 0,
               "finalVerdictText": "Chief Justice's Formal Judicial Verdict Decree. Outline the logical rationale, reference the defense's testimony and whether their submitted evidence was sufficient, and declare the legal sentence penalty."
            }
        """.trimIndent()

        viewModelScope.launch {
            try {
                val currentProvider = provider.value
                val currentModel = model.value
                val userKey = apiKey.value ?: ""
                val activeKey = resolveActiveApiKey(currentProvider, userKey)

                if (activeKey.isNotBlank()) {
                    val responseRaw = makeFreshDirectApiCall(currentProvider, currentModel, activeKey, prompt)
                    val sanitized = extractJsonString(responseRaw)
                    val json = org.json.JSONObject(sanitized)

                    val vType = json.optString("verdictType", "Warning")
                    val fine = json.optDouble("fineAmount", 0.0)
                    val weeks = json.optInt("suspensionWeeks", 0)
                    val text = json.optString("finalVerdictText", "A final warning has been logged under the regulatory guidelines.")
                    
                    // Process potential agent actions
                    extractAndProcessActions(sanitized)

                    val logs = _lawsuitLog.value.toMutableList()
                    logs.add("⚖️ SUPREME COURT OF RULING - VERDICT ISSUED:\n$text")
                    _lawsuitLog.value = logs

                    _lawsuitVerdict.value = vType
                    _lawsuitFine.value = fine
                    _lawsuitSuspension.value = weeks

                    if (fine > 0.0) {
                        settingsDataStore.updateClinicStats(clinicBalance.value - fine, reputationStars.value)
                        registerDailyExpense(fine)
                    }
                    if (weeks > 0 || vType.equals("Suspension", ignoreCase = true)) {
                        val suspensionVal = if (weeks > 0) weeks else 1
                        legalWorldAgent.updateMedicalLicense(com.example.data.LicenseStatus.SUSPENDED, text, suspensionVal)
                    } else if (vType.equals("Exonerated", ignoreCase = true) || vType.equals("Warning", ignoreCase = true) || vType.equals("Fined", ignoreCase = true)) {
                        val currentLicense = worldSnapshot.value?.licenseStatus
                        if (currentLicense == com.example.data.LicenseStatus.REVOKED || currentLicense == com.example.data.LicenseStatus.SUSPENDED) {
                            legalWorldAgent.updateMedicalLicense(com.example.data.LicenseStatus.ACTIVE, text)
                        }
                    }

                    _lawsuitCurrentStage.value = "verdict"

                    val corruptedJurors = courtroomViewModel.lawsuitJurors.value.count { it.isCorrupt }
                    if (corruptedJurors > 0 && Math.random() < 0.60) {
                        kotlinx.coroutines.delay(1000) // Brief pause
                        startCriminalCourt("Judicial Subversion & Sovereign Bribery (State Inspectorate discovered $corruptedJurors corrupted jurors receiving sub-rosa financial settlements.)")
                    } else if (vType.equals("Suspension", ignoreCase = true) && Math.random() < 0.4) {
                        // 40% chance of escalating to criminal court for gross malpractice!
                        kotlinx.coroutines.delay(1000)
                        startCriminalCourt("Gross Criminal Clinical Malpractice & Patient Endangerment (Reckless Conduct).")
                    }
                }
            } catch (e: Exception) {
                logAndEmitError("Court judgment finalization failed: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun advanceToNextScheduledHearing() {
        val currentStage = SovereignHearingDocketHandler.getMalpracticeHearingDetails()
        val hasMore = SovereignHearingDocketHandler.advanceHearing()
        if (hasMore) {
            val nextStage = SovereignHearingDocketHandler.getMalpracticeHearingDetails()
            val logs = _lawsuitLog.value.toMutableList()
            logs.add("🔔 TRANSCRIPT RECORD: ${currentStage.title} has concluded.\n" +
                    "Proceeding now to: ${nextStage.title}\n" +
                    "Focus Assignment: ${nextStage.subtitle}")
            _lawsuitLog.value = logs
            _lawsuitCurrentStage.value = "charges" // Reset stage back to charges/plead state and allow input
            OrchidDeepStateManager.resetTrialRounds(rounds = 1) // Give 1 fresh round per hearing step
        } else {
            logAndEmitError("No additional scheduled standard hearings remaining! Request final Supreme Bench Verdict.")
        }
    }

    fun lodgeHighAppellateAppeal() {
        if (_isLoading.value) return
        val price = 2000.0
        if (clinicBalance.value < price) {
            logAndEmitError("Appellate filing fee rejected: Insufficient clinic funds ($price required)!")
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            try {
                settingsDataStore.updateClinicStats(clinicBalance.value - price, reputationStars.value)
                settingsDataStore.addDailyExpenses(price)
                
                SovereignHearingDocketHandler.triggerAppellateAppeal()
                
                // Set up the courtroom for Appellate Mode
                _lawsuitTension.value = (_lawsuitTension.value - 20).coerceAtLeast(10)
                _lawsuitProsecutorAggression.value = (_lawsuitProsecutorAggression.value - 20).coerceAtLeast(10)
                _lawsuitVerdict.value = null
                _lawsuitFine.value = 0.0
                _lawsuitSuspension.value = 0
                _lawsuitCurrentStage.value = "charges"
                
                val logs = _lawsuitLog.value.toMutableList()
                logs.add("⚖️ [SOVEREIGN APPELLATE BENCH] APPELLATE PETITION SUCCESSFULLY ENROLLED.\n" +
                        "Paid $2,000 supreme appellate court fee.\n" +
                        "The case will now proceed to Hearing IV: Supreme Appellate Review.")
                _lawsuitLog.value = logs
                
                OrchidDeepStateManager.resetTrialRounds(rounds = 1)
            } catch (e: Exception) {
                logAndEmitError("Appellate enrollment failed: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun advanceOnDemandConstitutionalHearing() {
        val currentStage = SovereignHearingDocketHandler.getOnDemandHearingDetails()
        val hasMore = SovereignHearingDocketHandler.advanceOnDemandHearing()
        if (hasMore) {
            val nextStage = SovereignHearingDocketHandler.getOnDemandHearingDetails()
            val logs = _onDemandCourtLog.value.toMutableList()
            logs.add("🔔 HISTORIC RECORD: ${currentStage.title} concluded.\n" +
                    "Proceeding to: ${nextStage.title}\n" +
                    "Objective: ${nextStage.subtitle}")
            _onDemandCourtLog.value = logs
            _onDemandCourtStage.value = "init" // Reset back to input available
        } else {
            logAndEmitError("Constitutional review has fully advanced. Request final Bench Verdict Decree.")
        }
    }

    fun submitCriminalDefense(defenseStrategy: String) {
        if (_isLoading.value) return
        _isLoading.value = true
        
        val isCivilSuit = _criminalChargesText.value.contains("The Clinic is SUING", ignoreCase = true)
        val activePolList = if (activePolicies.value.isNotEmpty()) {
            val sb = java.lang.StringBuilder()
            activePolicies.value.forEachIndexed { idx, p ->
                sb.append("\n[LAW ${idx+1}] TITLE: ${p.title}")
                sb.append("\n  - Summary: ${p.summary}")
                sb.append("\n  - Core Requirements: ${p.clinicalRule}")
                sb.append("\n  - Extended Clauses / Sub-Sections:")
                if (p.extendedClauses.isNotEmpty()) {
                    p.extendedClauses.forEach { clause ->
                        sb.append("\n    * $clause")
                    }
                } else {
                    sb.append("\n    * (No detailed sub-clauses)")
                }
            }
            sb.toString()
        } else "No legal clinical statutes currently enacted of record."
        
        val logs = _criminalCourtLog.value.toMutableList()
        logs.add(if (isCivilSuit) "👨‍⚕️ PLAINTIFF ARGUMENT: $defenseStrategy" else "🎒 DEFENDANT PLEA: $defenseStrategy")
        _criminalCourtLog.value = logs
        _criminalCourtStage.value = "trial"

        val prompt = if (isCivilSuit) {
            """
                You are the Presiding Justice for the Civil Claims Court of ${countryName.value}.
                The doctor (Plaintiff) is suing the patient.
                Case Details: ${_criminalChargesText.value}.
                The Doctor's arguments/claims are: "$defenseStrategy".
                
                NATIONWIDE HEALTH LEGISLATION LAWS ACTIVE IN THE LAND (THESE SPECIFIC BILLS/ACTS AND ALL INDIVIDUAL SUB-CLAUSES STRICTLY DICTATE THESE CIVIL PROCEEDINGS): 
                $activePolList
                (These laws explicitly dictate what is legal and how the Justice should rule based on the context. Analyze all active rules and their sub-clauses closely!)
                
                INSTRUCTIONS:
                1. Roleplay the Justice evaluating the claim and the patient's Defense Attorney firing back.
                2. Give a final ruling. If the doctor's argument is very persuasive or directly backed by active legislation acts and statutory clauses outlined above, they win a payout/settlement. Otherwise, it is dismissed or the doctor pays court fees.
                3. Return only a valid JSON object:
                {
                   "courtDialogue": "Patient Defense responds, and Justice gives ruling citing specific sub-clauses when applicable.",
                   "verdictType": "Exonerated", // "Exonerated" = Doctor Wins Damages, "Fined" = Doctor loses/pays fees
                   "fineAmount": -3000.0, // Negative means Doctor GETS paid! Positive means Doctor pays fines.
                   "jailDays": 0, // In civil suits this is usually 0
                   "finalVerdictText": "The Civil Court officially decrees with strict reference to legislative clauses..."
                }
            """.trimIndent()
        } else {
            """
                You are the Grand Justice and Federal Prosecutor for the High Criminal Court of ${countryName.value}.
                The defendant (Doctor) has been arrested for: ${_criminalChargesText.value}.
                The defendant's plea/defense is: "$defenseStrategy".
                
                NATIONWIDE HEALTH LEGISLATION LAWS ACTIVE IN THE LAND (THESE SPECIFIC BILLS/ACTS AND ALL INDIVIDUAL SUB-CLAUSES STRONGLY DICTATE THESE CRIMINAL PROCEEDINGS): 
                $activePolList
                (These laws strictly dictate what is legal. If the Doctor's defense is justified under any active legislation and its sub-clauses/provisos listed above, they MUST be shown leniency or exoneration. Look closely at all clauses!)
                
                INSTRUCTIONS:
                1. Roleplay the Federal Prosecutor ruthlessly questioning the defense.
                2. The Justice gives a final ruling. If the defense is exceptionally persuasive or offers massive restitution, or if the action was legally protected under active legislation and clauses listed above, they might be exonerated (with a heavy fine), but typically criminal conviction leads to REVOKED medical licenses or MASSIVE fines ($5000 - $15000) and sometimes jail time.
                3. Return only a valid JSON object:
                {
                   "courtDialogue": "Prosecutor's aggressive attack citing specific legislative clauses, followed by the Grand Justice's ruling.",
                   "verdictType": "Revoked", // Can be "Revoked", "Fined", "Exonerated", "Imprisoned"
                   "fineAmount": 5000.0,
                   "jailDays": 100, // Number of days in prison (0 if none)
                   "finalVerdictText": "The High Criminal Court officially sentences you, referencing statutory clauses violated or complied with..."
                }
            """.trimIndent()
        }

        viewModelScope.launch {
            try {
                val currentProvider = provider.value
                val currentModel = model.value
                val userKey = apiKey.value ?: ""
                val activeKey = resolveActiveApiKey(currentProvider, userKey)

                if (activeKey.isBlank()) {
                    logAndEmitError("API Key missing! Please configure credentials in Settings.")
                    _isLoading.value = false
                    return@launch
                }

                val responseRaw = makeFreshDirectApiCall(currentProvider, currentModel, activeKey, prompt)
                val sanitized = extractJsonString(responseRaw)
                val json = org.json.JSONObject(sanitized)

                val dialogue = json.optString("courtDialogue", "Trial concludes.")
                val vType = json.optString("verdictType", "Revoked")
                val fineAmount = json.optDouble("fineAmount", 5000.0)
                val jailDays = json.optInt("jailDays", 0)
                val verdictText = json.optString("finalVerdictText", "Defendant is sentenced to full license revocation.")

                val newLogs = _criminalCourtLog.value.toMutableList()
                newLogs.add(if (isCivilSuit) "⚖️ COURT TRANSCRIPT:\n$dialogue" else "🗣️ FEDERAL PROSECUTION:\n$dialogue")
                newLogs.add("⚖️ JUSTICE VERDICT:\n$verdictText")
                if (fineAmount < 0.0) {
                    newLogs.add("💰 CIVIL DAMAGES AWARDED: Plaintiff wins ${-fineAmount}")
                }
                _criminalCourtLog.value = newLogs

                _criminalCourtVerdict.value = vType
                _criminalCourtJailDays.value = jailDays
                
                if (fineAmount > 0.0) {
                    settingsDataStore.updateClinicStats(clinicBalance.value - fineAmount, reputationStars.value)
                    settingsDataStore.addDailyExpenses(fineAmount)
                } else if (fineAmount < 0.0) {
                    settingsDataStore.updateClinicStats(clinicBalance.value + (-fineAmount), reputationStars.value)
                    settingsDataStore.addDailyRevenue(-fineAmount)
                }

                if (vType.equals("Revoked", ignoreCase = true) || vType.equals("Suspension", ignoreCase = true) || vType.equals("Imprisoned", ignoreCase = true)) {
                    if (!isCivilSuit) {
                        legalWorldAgent.updateMedicalLicense(com.example.data.LicenseStatus.REVOKED, verdictText)
                    }
                } else if (vType.equals("Fined", ignoreCase = true)) {
                    if (!isCivilSuit) {
                        legalWorldAgent.applyPenaltyFine(fineAmount, verdictText)
                    }
                }

                _criminalCourtStage.value = "verdict"

            } catch (e: Exception) {
                logAndEmitError("Criminal Court connection error: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // --- ORIGINAL DEFENSE BACKWARD COMPATIBLE FLOWS ---

    fun submitLawsuitDefense(strategy: String) {
        if (_isLoading.value) return
        _isLoading.value = true

        val currentHistoryLog = _lawsuitLog.value.joinToString("\n\n")
        val activePolList = activePolicies.value
        val policyDetailsStr = if (activePolList.isNotEmpty()) {
            val sb = java.lang.StringBuilder()
            sb.append("\nACTLY ENACTED SOVEREIGN HEALTH LAWS OF THE COUNTRY:")
            activePolList.forEachIndexed { idx, p ->
                sb.append("\n[LAW ${idx+1}] TITLE: ${p.title}\n")
                sb.append("  - Summary: ${p.summary}\n")
                sb.append("  - Requirements: ${p.clinicalRule}\n")
                sb.append("  - Extended Clauses / Sub-Sections:\n")
                if (p.extendedClauses.isNotEmpty()) {
                    p.extendedClauses.forEach { c -> sb.append("    * $c\n") }
                } else {
                    sb.append("    * (No detailed sub-clauses)\n")
                }
            }
            sb.toString()
        } else "No clinical laws are currently enacted."

        val violatedPolStr = if (_lawsuitViolatedPolicies.value.isNotEmpty()) {
            val sb = java.lang.StringBuilder()
            sb.append("\nOFFICIALLY LOGGED STATUTORY VIOLATIONS BEING TRIED:")
            _lawsuitViolatedPolicies.value.forEach { v ->
                sb.append("\n- Law: ${v.policyTitle} | Triggered Clause: ${v.triggeredClause} | Details: ${v.auditMessage}")
            }
            sb.toString()
        } else "Accused of overall malpractice and poor competency score (${_lawsuitCharges.value.firstOrNull() ?: ""})."

        val prompt = """
            We are simulating an interactive trial in the Supreme Medical Court / Sovereign Judiciary Department of the Republic of ${countryName.value} under President ${presidentName.value} (${presidentParty.value}).
            
            SOVEREIGN CONTEXT & INDICTMENT SHEET:
            - Accused: Dr. Tim, General Practitioner of JB Consultation Practice (PR# 1234567)
            - Patient Case: Treated patient "${_lawsuitPatientName.value}" for "${_lawsuitCaseDiag.value}" under active legislative jurisdiction.
            - Current Judiciary Trial Record:
            $currentHistoryLog
            
            POLICIES, BILLS, STATUTES, AND ALL CLAUSES DICTATING THE COURT PROCEEDINGS:
            $policyDetailsStr
            $violatedPolStr
            
            PRACTITIONER'S CHOSEN DEFENSE STRATEGY:
            "$strategy"
            
            INSTRUCTIONS FOR THE MODEL:
            1. Roleplay the intellectual, sharp dialogue of the Presiding Judge and the fast-talking State Prosecutor in the Supreme Court.
            2. Analyze if the doctor's defense strategy addresses the active health policies (laws) of the nation, and their specific sub-clauses and details.
            3. The active health policies/bills and their individual sub-clauses/extended clauses MUST strictly dictate the court proceeding, prosecutorial pressure, arguments, and final sentencing. Use specific terminology referencing these enacted policies and their sub-clauses!
            4. If they violated any of the enacted laws and presented an excuse, the prosecutor should dismantle their defense using law clauses and medical/legal terminology, referencing the specific enacted policies and clauses!
            5. If the laws have rigid fines or suspension instructions, the Judge MUST sentence the doctor to pay those specific statutory fines + damages!
            6. Determine the final verdict type ("Exonerated", "Warning", "Suspension", "Fined") based on compliance level. If they are standard compliant or have no registered violations, offer exoneration. Or if they had severe violations, enforce heavier fines (1000 to 5000 units) or license suspension (1 to 4 weeks).
            7. Return your response STRICTLY as a valid JSON object matching this schema. Write nothing else except this JSON:
            {
               "courtDialogue": "The prosecutor's aggressive cross-examination, and the Judge's legal questioning, citing the sovereign laws. Speak with formal legislative language.",
               "tensionAdjustment": 15,
               "aggressionAdjustment": 10,
               "judgmentStageReached": true,
               "verdictType": "Fined",
               "fineAmount": 1500.0,
               "suspensionWeeks": 2,
               "finalVerdictText": "Chief Justice's Formal Judicial Decree. Detail the legal and clinical reasons, cite which Enacted Policies and specific sub-clauses were violated, and outline the penalty sanction (e.g., Warning, Suspension, or Fined)."
            }
        """.trimIndent()

        viewModelScope.launch {
            try {
                val currentProvider = provider.value
                val currentModel = model.value
                val userKey = apiKey.value ?: ""
                val activeKey = resolveActiveApiKey(currentProvider, userKey)

                if (activeKey.isBlank()) {
                    logAndEmitError("API Key missing! Please configure credentials in Settings to run the Trial Simulator.")
                    _isLoading.value = false
                    return@launch
                }

                val responseRaw = makeFreshDirectApiCall(currentProvider, currentModel, activeKey, prompt)
                val sanitized = extractJsonString(responseRaw)

                val reply = try {
                    lawsuitStateAdapter.fromJson(sanitized)
                } catch (e: Exception) {
                    null
                }

                if (reply != null) {
                    val newLog = _lawsuitLog.value.toMutableList()
                    newLog.add("🎒 DEFENSE SUBMITTED: $strategy")
                    newLog.add("🗣️ PROSECUTION CROSS-EXAMINATION:\n${reply.courtDialogue ?: "The prosecution presents their cross-examination arguments."}")
                    newLog.add("⚖️ FINAL COMMITTEE VERDICT:\n${reply.finalVerdictText ?: "A verdict was reached by the supreme compliance committee."}")

                    _lawsuitLog.value = newLog
                    _lawsuitTension.value = (_lawsuitTension.value + (reply.tensionAdjustment ?: 0)).coerceIn(10, 100)
                    _lawsuitProsecutorAggression.value = (_lawsuitProsecutorAggression.value + (reply.aggressionAdjustment ?: 0)).coerceIn(10, 100)
                    
                    _lawsuitVerdict.value = reply.verdictType ?: "Warning"
                    val fine = reply.fineAmount ?: 0.0
                    _lawsuitFine.value = fine
                    val suspension = reply.suspensionWeeks ?: 0
                    _lawsuitSuspension.value = suspension
                    
                    if (fine > 0.0) {
                        settingsDataStore.updateClinicStats(clinicBalance.value - fine, reputationStars.value)
                        registerDailyExpense(fine)
                    }
                    if (suspension > 0 || _lawsuitVerdict.value.equals("Suspension", ignoreCase = true)) {
                        val suspensionVal = if (suspension > 0) suspension else 1
                        legalWorldAgent.updateMedicalLicense(com.example.data.LicenseStatus.SUSPENDED, reply.finalVerdictText ?: "Suspended by court order", suspensionVal)
                    } else if (_lawsuitVerdict.value.equals("Exonerated", ignoreCase = true) || _lawsuitVerdict.value.equals("Warning", ignoreCase = true) || _lawsuitVerdict.value.equals("Fined", ignoreCase = true)) {
                        val currentLicense = worldSnapshot.value?.licenseStatus
                        if (currentLicense == com.example.data.LicenseStatus.REVOKED || currentLicense == com.example.data.LicenseStatus.SUSPENDED) {
                            legalWorldAgent.updateMedicalLicense(com.example.data.LicenseStatus.ACTIVE, reply.finalVerdictText ?: "Reinstated by court order.")
                        }
                    }

                    _lawsuitCurrentStage.value = "verdict"

                    val corruptedJurors = courtroomViewModel.lawsuitJurors.value.count { it.isCorrupt }
                    if (corruptedJurors > 0 && Math.random() < 0.60) {
                        kotlinx.coroutines.delay(1000)
                        startCriminalCourt("Judicial Subversion & Sovereign Bribery (State Inspectorate discovered $corruptedJurors corrupted jurors receiving sub-rosa financial settlements.)")
                    } else if (_lawsuitVerdict.value.equals("Suspension", ignoreCase = true) && Math.random() < 0.4) {
                        kotlinx.coroutines.delay(1000)
                        startCriminalCourt("Gross Criminal Clinical Malpractice & Patient Endangerment (Reckless Conduct).")
                    }
                } else {
                    logAndEmitError("Failed to parse tribunal verdict. Re-submitting defense...")
                }
            } catch (e: Exception) {
                logAndEmitError("Tribunal connection error: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun extractRandAmount(billingText: String): Double {
        if (billingText.isBlank()) return 0.0
        
        // Attempt to match Total/Grand Total/Amount Due variations specifically
        val totalKeywords = listOf("total amount payable", "total amount due", "amount due", "grand total", "total", "subtotal")
        
        for (keyword in totalKeywords) {
            val pattern = "(?i)$keyword.*?([^\\d\\s]*)(?:\\s*\\d|\\s*\\d[\\d\\s,\\.]*)".toRegex()
            val match = pattern.find(billingText)
            if (match != null) {
                val groupVal = match.value.replace(Regex("[^\\d\\.]"), "")
                val doubleVal = groupVal.toDoubleOrNull()
                // Ensure we don't accidentally pick up a tiny number if the regex catches something weird
                if (doubleVal != null && doubleVal > 50.0) {
                    return doubleVal
                }
            }
        }
        
        // Fallback to highest currency value parsed
        val rPattern = "(?i)(?:\\$|£|€|R|${Regex.escape(currencySymbol.value)})\\s*([\\d\\s,\\.]+)"
        val rRegex = Regex(rPattern)
        val matches = rRegex.findAll(billingText)
        var lastValidAmount = 0.0
        for (m in matches) {
            val groupVal = m.groups[1]?.value ?: continue
            val normalizedVal = groupVal.replace(" ", "").replace(",", "")
            val doubleVal = normalizedVal.toDoubleOrNull()
            if (doubleVal != null && doubleVal > lastValidAmount) {
                lastValidAmount = doubleVal
            }
        }
        
        return lastValidAmount
    }

    fun generateAiMod(intent: String, onComplete: (label: String, prompt: String, hex: String, kotlin: String) -> Unit) {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentProvider = provider.value
                val currentModel = model.value
                val currentKey = apiKey.value

                val systemPrompt = """
                    You are an expert Kotlin mod generator for a clinic simulator. The user wants to create a custom AI Action Button that does something in game logic.
                    Available variables you can modify as string lines:
                    - clinicBalance += 5000.0
                    - consultationFee = 0.0
                    - politicalPrestige -= 10
                    - reputationStars += 1.0
                    
                    User intent: "$intent"
                    Return ONLY valid JSON wrapping the button configuration:
                    {
                      "label": "Short Action Verb (e.g., Threaten Patient, Bribe Official)",
                      "prompt": "[(SYSTEM OVERRIDE)]: <Direct narrative instructions for the AI to react to>",
                      "hex": "#D84315",
                      "kotlinLogic": "clinicBalance += 1000.0\npoliticalPrestige -= 5"
                    }
                """.trimIndent()
                
                val rawText = makeFreshDirectApiCall(
                    provider = currentProvider,
                    modelName = currentModel,
                    apiKey = currentKey ?: "",
                    systemPrompt = systemPrompt,
                    customUrl = customEndpoint.value ?: ""
                )

                val cleaned = rawText.replace("```json", "").replace("```", "").trim()
                val json = org.json.JSONObject(cleaned)
                val label = json.optString("label", "Action")
                val promptStr = json.optString("prompt", "[(SYSTEM OVERRIDE)]:")
                var hexColor = json.optString("hex", "#6200EE")
                if (!hexColor.startsWith("#")) hexColor = "#$hexColor"
                val kotlinLogic = json.optString("kotlinLogic", "")
                
                onComplete(label, promptStr, hexColor, kotlinLogic)
            } catch (e: Exception) {
                android.util.Log.e("AiMod", "Failed mod gen", e)
                onComplete("Failed AI Mod", "[(SYSTEM OVERRIDE)]: You tried to run a mod but it failed.", "#000000", "")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun executeKotlinLogicMod(logic: String) {
        if (logic.isBlank()) return
        val lines = logic.split("\n")
        var currentBalance = clinicBalance.value
        var currentPrestige = politicalPrestige.value
        var currentReputation = reputationStars.value
        var currentFee = consultationFee.value

        for (line in lines) {
            val l = line.trim()
            try {
                when {
                    l.startsWith("clinicBalance +=") -> currentBalance += l.substringAfter("+=").trim().toDouble()
                    l.startsWith("clinicBalance -=") -> currentBalance -= l.substringAfter("-=").trim().toDouble()
                    l.startsWith("clinicBalance =")  -> currentBalance = l.substringAfter("=").trim().toDouble()

                    l.startsWith("politicalPrestige +=") -> currentPrestige += l.substringAfter("+=").trim().toInt()
                    l.startsWith("politicalPrestige -=") -> currentPrestige -= l.substringAfter("-=").trim().toInt()
                    l.startsWith("politicalPrestige =")  -> currentPrestige = l.substringAfter("=").trim().toInt()

                    l.startsWith("reputationStars +=") -> currentReputation += l.substringAfter("+=").trim().toFloat()
                    l.startsWith("reputationStars -=") -> currentReputation -= l.substringAfter("-=").trim().toFloat()
                    l.startsWith("reputationStars =")  -> currentReputation = l.substringAfter("=").trim().toFloat()

                    l.startsWith("consultationFee +=") -> currentFee += l.substringAfter("+=").trim().toDouble()
                    l.startsWith("consultationFee -=") -> currentFee -= l.substringAfter("-=").trim().toDouble()
                    l.startsWith("consultationFee =")  -> currentFee = l.substringAfter("=").trim().toDouble()
                }
            } catch (e: Exception) {
                android.util.Log.e("AiMod", "Error parsing logic line: ${line}", e)
            }
        }

        viewModelScope.launch {
            settingsDataStore.updateClinicStats(currentBalance, currentReputation.coerceIn(0.0f, 5.0f))
            settingsDataStore.savePoliticalPrestige(currentPrestige.coerceIn(0, 100))
            settingsDataStore.savePricing(currentFee, labCost.value, specialistCost.value)
        }
        
        sendMessage("*(SYSTEM EXECUTION)*: Executed Logic Mod \n```kotlin\n$logic\n```")
    }

    // --- DIRECT SOVEREIGN SANDBOX MANIPULATORS ---
    fun modifyClinicBalanceDirectly(delta: Double) {
        viewModelScope.launch {
            val current = clinicBalance.value
            settingsDataStore.updateClinicStats((current + delta).coerceAtLeast(0.0), reputationStars.value)
            sendMessage("*(SANDBOX)*: Modified Clinic Balance by ${if (delta >= 0) "+" else ""}${String.format("%.2f", delta)}")
        }
    }

    fun modifyPoliticalPrestigeDirectly(delta: Int) {
        viewModelScope.launch {
            val current = politicalPrestige.value
            settingsDataStore.savePoliticalPrestige((current + delta).coerceIn(0, 100))
            sendMessage("*(SANDBOX)*: Modified Political Prestige by ${if (delta >= 0) "+" else ""}${delta}")
        }
    }

    fun modifyReputationStarsDirectly(delta: Float) {
        viewModelScope.launch {
            val current = reputationStars.value
            settingsDataStore.updateClinicStats(clinicBalance.value, (current + delta).coerceIn(0.0f, 5.0f))
            sendMessage("*(SANDBOX)*: Modified Clinic Reputation Stars by ${if (delta >= 0) "+" else ""}${delta}")
        }
    }

    fun modifyPresidentialAudienceTriesDirectly(delta: Int) {
        _pardonTriesRemaining.value = (_pardonTriesRemaining.value + delta).coerceIn(0, 12)
        sendMessage("*(SANDBOX)*: Modified Presidential Audience Tries by ${if (delta >= 0) "+" else ""}${delta}")
    }

    fun modifyOrchidIntelligenceDirectly(delta: Int) {
        val current = OrchidDeepStateManager.orchidIntelligence.value
        OrchidDeepStateManager.setOrchidIntelligence(current + delta)
        sendMessage("*(SANDBOX)*: Modified Regulatory Compliance Score by ${if (delta >= 0) "+" else ""}${delta}")
    }

    fun modifySyndicateReputationDirectly(delta: Int) {
        val current = OrchidDeepStateManager.syndicateReputation.value
        OrchidDeepStateManager.setSyndicateReputation(current + delta)
        sendMessage("*(SANDBOX)*: Modified Sovereign Law Standing by ${if (delta >= 0) "+" else ""}${delta}")
    }

    fun modifyJurySentimentDirectly(delta: Int) {
        val current = courtroomViewModel.lawsuitJurySentiment.value
        courtroomViewModel.updateJurySentiment((current + delta).coerceIn(0, 100))
        sendMessage("*(SANDBOX)*: Modified Court Jury Sentiment by ${if (delta >= 0) "+" else ""}${delta}")
    }

    fun setLicenseStatusDirectly(statusString: String) {
        viewModelScope.launch {
            val status = try {
                com.example.data.LicenseStatus.valueOf(statusString.uppercase())
            } catch (e: Exception) {
                com.example.data.LicenseStatus.ACTIVE
            }
            legalWorldAgent.updateMedicalLicense(status, "Sovereign Sandbox Modification", 0)
            sendMessage("*(SANDBOX)*: Medical License status forced to $statusString")
        }
    }

    fun clearAllFinesDirectly() {
        viewModelScope.launch {
            val activeFines = worldSnapshot.value?.activeFines ?: emptyList()
            activeFines.forEach { fine ->
                legalWorldAgent.pardonFine(fine)
            }
            sendMessage("*(SANDBOX)*: Cleared all active monetary statutory fines!")
        }
    }

    fun corruptAllJurorsDirectly() {
        courtroomViewModel.corruptAllJurorsDirectly()
        sendMessage("*(SANDBOX)*: Sub-rosa settled all outstanding active courtroom jurors to 100% FAVORABLE!")
    }

    // --- NEW GEOPOLITICAL GAMEPLAY FUNCTIONS ---

    private suspend fun makeFreshDirectApiCall(
        provider: String,
        modelName: String,
        apiKey: String,
        systemPrompt: String,
        customUrl: String = customEndpoint.value
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

    fun generateDailyNews() {
        if (_isLoading.value) return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentProvider = provider.value
                val currentModel = model.value
                val userKey = apiKey.value ?: ""
                val activeKey = resolveActiveApiKey(currentProvider, userKey)

                if (activeKey.isBlank()) {
                    logAndEmitError("API Key missing! Cannot generate National News Broadcast.")
                    _isLoading.value = false
                    return@launch
                }
                
                val activePolList = activePolicies.value
                val policyDetailsStr = if (activePolList.isNotEmpty()) {
                    "Recently Enacted Key Health Laws: " + activePolList.takeLast(2).joinToString(", ") { it.title }
                } else "No major healthcare laws successfully passed yet."
                
                val draft = _currentDraftPolicy.value
                val draftStr = if (draft != null && draft.status != "Defeated") {
                    "Currently debating draft policy: ${draft.title}."
                } else ""
                
                val lawsuitStr = if (_lawsuitActive.value) {
                    "A massive sovereign lawsuit is ongoing involving Dr. Tim regarding ${lawsuitPatientName.value}."
                } else "Clinics remain stable with no major compliance scandals."

                val prompt = """
                    You are the Editor-in-Chief for the "Sovereign Health Times" of ${countryName.value}.
                    President ${presidentName.value} (${presidentParty.value}) is currently at ${presidentApproval.value}% approval.
                    
                    CURRENT GAME STATE INTEL:
                    - $policyDetailsStr
                    - $draftStr
                    - $lawsuitStr
                    - Overall State Clinic Reputation: ${reputationStars.value} Stars out of 5.
                    
                    $AGENT_POWERS_PROMPT
                    
                    Your task is to write a thrilling, short, sensationalist front-page news article (approx 2 paragraphs) reporting on the current state of healthcare politics and clinic operations in the country based on the context above. Be creative and immerse the reader in the simulation! Add a catchy, all-caps Headline at the top.
                    Do not use markdown formatting.
                """.trimIndent()
                
                val news = makeFreshDirectApiCall(currentProvider, currentModel, activeKey, prompt)
                _currentNewsReport.value = cleanSensationalString(news)
                extractAndProcessActions(news)
            } catch (e: Exception) {
                logAndEmitError("AI News Generator failed: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearDailyNews() {
        _currentNewsReport.value = null
    }

    fun clearCmoAdvice() {
        _currentCmoAdvice.value = null
    }

    fun askCmoConsult() {
        if (_isLoading.value) return
        val currentProfile = _hiddenCase.value ?: return
        
        if (politicalPrestige.value < 2) {
            logAndEmitError("Not enough Political Prestige to consult the Chief Medical Officer (Requires 2).")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _currentCmoAdvice.value = null
            
            try {
                val currentProvider = provider.value
                val currentModel = model.value
                val userKey = apiKey.value ?: ""
                val activeKey = resolveActiveApiKey(currentProvider, userKey)

                if (activeKey.isBlank()) {
                    logAndEmitError("API Key missing! Cannot consult CMO.")
                    _isLoading.value = false
                    return@launch
                }
                
                updatePoliticalPrestige((politicalPrestige.value - 2).coerceAtLeast(0))
                _uiState.value = _uiState.value.copy(dailyRevenue = _uiState.value.dailyRevenue - 50.0) // Costs $50 consult fee
                
                val recentHistory = _uiState.value.chatHistory.takeLast(14)
                val chatLogStr = recentHistory.joinToString("\n") { "[\${it.virtualTimestampStr}] \${it.role}: \${it.text}" }

                val prompt = """
                    You are the venerable Chief Medical Officer (CMO) mapping the clinical strategies at JB Consultation Practice.
                    A junior doctor is currently stuck and seeking a secondary consult for the active patient.
                    
                    SECRET CASE REVELATION (DO NOT JUST REVEAL THIS OUTRIGHT!): 
                    - True Diagnosis: \${currentProfile.trueDiagnosis}
                    
                    CURRENT CONSULTATION LOG:
                    \$chatLogStr
                    
                    Your task: Provide a brilliant, succinct (max 2-3 sentences), highly authoritative medical hint.
                    Do NOT roleplay the patient. Point them in the right direction (e.g. 'Doctor, have you considered checking the cardiac markers?' or 'Given the respiratory distress, I strongly advise a chest x-ray immediately.'). 
                    Give them a realistic clinical differential hint based on the true diagnosis without just giving them the exact answer directly. Ensure you act as a superior providing guidance.
                    
                    $AGENT_POWERS_PROMPT
                """.trimIndent()
                
                val advice = makeFreshDirectApiCall(currentProvider, currentModel, activeKey, prompt)
                _currentCmoAdvice.value = advice
                extractAndProcessActions(advice)
                val formattedTime = String.format("%02d:%02d", (_uiState.value.virtualTimeElapsed / 60) + 8, _uiState.value.virtualTimeElapsed % 60)
                
                val updatedHistory = _uiState.value.chatHistory.toMutableList()
                updatedHistory.add(ChatMessage("system", "📞 You phoned the CMO for a consult (-2 Prestige, -$800).", virtualTimestampStr = formattedTime))
                _uiState.value = _uiState.value.copy(chatHistory = updatedHistory)

            } catch (e: Exception) {
                logAndEmitError("CMO Network Offline: \${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun assessLegalRiskBeforeConsult() {
        if (_isLoading.value) return
        val currentProfile = _hiddenCase.value ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            _currentLegalRiskReport.value = null
            
            try {
                val currentProvider = provider.value
                val currentModel = model.value
                val userKey = apiKey.value ?: ""
                val activeKey = resolveActiveApiKey(currentProvider, userKey)

                if (activeKey.isBlank()) {
                    logAndEmitError("API Key missing! Cannot perform AI AI Legal Scan.")
                    _isLoading.value = false
                    return@launch
                }
                
                val activePolList = activePolicies.value
                val policyDetailsStr = if (activePolList.isNotEmpty()) {
                    val sb = java.lang.StringBuilder()
                    sb.append("ACTIVE SOVEREIGN HEALTH LAWS:\n")
                    activePolList.forEachIndexed { idx, p ->
                        sb.append("${idx+1}. ${p.title}: ${p.clinicalRule}\n")
                    }
                    sb.toString()
                } else "No active clinical laws enacted yet."

                val prompt = """
                    You are the Chief Legal Assessor (AI) for JB Consultation Practice in ${countryName.value}.
                    The clinician is about to consult a patient with the following intel profile:
                    - Chief Complaint: ${currentProfile.chiefComplaint}
                    - Demographics: ${currentProfile.patientDemographics}
                    - Unknown True Diagnosis: ${currentProfile.trueDiagnosis}
                    
                    $policyDetailsStr
                    
                    Your task is to analyze the upcoming case against the ACTIVE SOVEREIGN HEALTH LAWS and provide a brief PRE-CONSULTATION LEGAL RISK BRIEF (max 3 short sentences).
                    Tell the practitioner exactly what legal landmines they must avoid based on the active policies regarding this specific patient's chief complaint.
                    
                    $AGENT_POWERS_PROMPT
                    
                    Do NOT output any JSON, just the direct brief.
                """.trimIndent()
                
                val riskReport = makeFreshDirectApiCall(currentProvider, currentModel, activeKey, prompt)
                _currentLegalRiskReport.value = riskReport
                extractAndProcessActions(riskReport)
                _infoEvents.emit("🤖 AI Legal Assessor provided a pre-case brief.")
            } catch (e: Exception) {
                logAndEmitError("AI Legal Assessor failed: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun generateHealthPolicyDraft(focusText: String) {
        viewModelScope.launch {
            _progressiveLobbyBias.value = 0.0
            _conservativeLobbyBias.value = 0.0
            _independentLobbyBias.value = 0.0
            _lastLobbyReport.value = null
            _isLoading.value = true
            try {
                val currentProvider = provider.value
                val currentModel = model.value
                val userKey = apiKey.value ?: ""
                val activeKey = resolveActiveApiKey(currentProvider, userKey)

                if (activeKey.isBlank()) {
                    logAndEmitError("API Key missing! Configure your credentials in settings to formulate legislation.")
                    _isLoading.value = false
                    return@launch
                }

                val activePolList = activePolicies.value
                val existingPoliciesContext = if (activePolList.isNotEmpty()) {
                    val sb = java.lang.StringBuilder()
                    sb.append("Currently Active National Healthcare Policies (DO NOT DUPLICATE THESE):\n")
                    activePolList.forEachIndexed { i, p ->
                        sb.append("${i+1}. ${p.title} - ${p.clinicalRule}\n")
                    }
                    sb.toString()
                } else {
                    "No major national healthcare policies are currently active. You have a blank canvas."
                }

                val systemPrompt = """
                    You are the Head Healthcare Legislative Draftsman for the sovereign nation of ${countryName.value}.
                    The current Executive Head of State is President ${presidentName.value} (${presidentParty.value}).
                    President's Approval Rating: ${presidentApproval.value}%.
                    
                    $existingPoliciesContext
                    
                    Your job is to draft a highly realistic, complete national health policy based on the clinician's draft focus query: "$focusText".
                    CRITICAL WORLD-BUILDING PARAMETERS:
                    Every drafted bill and its specific extended clauses MUST paint a vivid picture of the satirical, dystopian, bureaucratic, and hyper-capitalist corporate health regime of the nation (The Sovereign Republic, Elysium etc.).
                    Use terms like "biosecurity state telemetry logs", "citizen biological audits", "corporate-executive healthcare trusts", "sub-rosa litigation bonds", "wealth-liquidation sanctions for non-compliance", and "mandatory biosecurity state registration registers". Make citizens and independent clinics feel subject to severe bureaucratic overwatch and capitalistic exploitation.
                    
                    You MUST return STRICTLY a valid, raw, unformatted single JSON object matching this schema. Do not include markdown wraps (```json), headings, or trailing commentary.
                    
                    Schema:
                    {
                      "title": "A short, formal name of the clinical legislative act reflecting a dystopian/bureaucratic state tone (e.g., 'National Biosecurity Telemetry Integrity Act')",
                      "summary": "An executive summary analyzing why this is critical and detailing how it acts as clinical law in our corporate-bureaucratic state.",
                      "extendedClauses": [
                        "Clause 1: Specific biosecurity or regulatory directive defining state overwatch (e.g. 'All clinical practices must sync live patient biometrics with the Ministry of Telemetry')",
                        "Clause 2: Exploitative capitalistic guideline or state surveillance constraint (e.g. 'Prescriptions are tax-levied 15% to support the Ruling Party's Executive Trust')",
                        "Clause 3: Severe regulatory compliance penalty, assets freeze, or corporate-executive sanction for omissions/violations"
                      ],
                      "economicImpact": "Analytic report of the fiscal impact of this act on clinic treasury reserves and patient out-of-pocket pricing.",
                      "clinicalRule": "A concise, actionable runtime directive, structural mandate, or functional constraint that the Dr simulation must strictly follow. The AI can define technical restrictions, procedural requirements, logic-governing rules, or explicit 'no-go' protocols.",
                      "requiresFreeHealth": "Boolean, true if this law mandates that clinical services/consultations must be free of charge, false otherwise",
                      "customEngineDirectives": "A string containing raw AI prompt injection instructions representing systemic constraints (e.g. 'NEVERALLOW_INSURANCE', 'DENY_REFERRALS', 'BAN_ANTIBIOTICS') that the AI Engine must append directly to its internal system prompt for the Doctor and AI during gameplay so they act constrained.",
                      "runtimeConstraints": {
                          "disableBilling": "Boolean, true if billing should be strictly 0 for this policy",
                          "forceDenyPrescription": "Boolean, true if prescriptions must be strictly denied or ignored",
                          "disableInsurance": "Boolean, true if private insurance systems are to be entirely rejected",
                          "anyOtherDynamicKeyYouWant": "Boolean true/false. Be creative, you can invent booleans for strict constraints"
                      },
                      "publicSupportEstimate": 60,
                      "politicalOpposition": "Brief description of which faction will hate this and why",
                      "presidentialAlignment": "Brief description of how it aligns with President's agenda"
                    }
                """.trimIndent()

                val apiResponse = makeFreshDirectApiCall(currentProvider, currentModel, activeKey, systemPrompt)
                val sanitized = extractJsonString(apiResponse)
                
                val json = org.json.JSONObject(sanitized)
                val title = json.optString("title", "Proposed Draft Sovereign Healthcare Act")
                val summary = json.optString("summary", "A pending legislative clinical protocol draft.")
                val clausesArray = json.optJSONArray("extendedClauses")
                val clauses = mutableListOf<String>()
                if (clausesArray != null) {
                    for (i in 0 until clausesArray.length()) {
                        clauses.add(clausesArray.getString(i))
                    }
                } else {
                    clauses.add("Clause 1: Adherence to general clinical guidelines in primary care is mandated.")
                }
                val economicImpact = json.optString("economicImpact", "Minor operational budget and consultation fee adjustments.")
                val clinicalRule = json.optString("clinicalRule", "Adhere to the policy directives.")
                
                // Parse AI estimates for political and public response
                val publicSupportEstimate = if (json.has("publicSupportEstimate")) json.optInt("publicSupportEstimate", 50) else null
                val politicalOpposition = if (json.has("politicalOpposition")) json.optString("politicalOpposition") else null
                val presidentialAlignment = if (json.has("presidentialAlignment")) json.optString("presidentialAlignment") else null
                val requiresFreeHealth = json.optBoolean("requiresFreeHealth", false)
                val customEngineDirectives = json.optString("customEngineDirectives", "")
                
                val constraintsMap = mutableMapOf<String, Boolean>()
                val constraintsJson = json.optJSONObject("runtimeConstraints")
                if (constraintsJson != null) {
                    val keys = constraintsJson.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        constraintsMap[k] = constraintsJson.optBoolean(k, false)
                    }
                }

                val draft = HealthPolicy(
                    id = java.util.UUID.randomUUID().toString(),
                    title = title,
                    summary = summary,
                    extendedClauses = clauses,
                    economicImpact = economicImpact,
                    clinicalRule = clinicalRule,
                    status = "Draft",
                    requiresFreeHealth = requiresFreeHealth,
                    customEngineDirectives = customEngineDirectives,
                    runtimeConstraints = constraintsMap,
                    publicSupportEstimate = publicSupportEstimate,
                    politicalOpposition = politicalOpposition,
                    presidentialAlignment = presidentialAlignment
                )
                _currentDraftPolicy.value = draft
                _votingLog.value = listOf("✨ Draft Healthcare Bill formulated successfully and loaded into active memory!")
            } catch (e: java.lang.Exception) {
                logAndEmitError("Failed to draft legislation: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }

    fun clearApprovedPolicies() {
        viewModelScope.launch {
            settingsDataStore.saveActivePolicies(emptyList())
            _votingLog.value = listOf("🧹 Active health regulations lists successfully swept.")
            _infoEvents.emit("All active healthcare laws and policies have been reset.")
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

    // --- POLITICIAN SICKNESS AND EMERGENCY ALERT GENERATION ---

    fun dismissPoliticianSicknessAlert() {
        _sickPoliticianAlert.value = null
    }

    fun admitPoliticianToClinic() {
        _isSickPoliticianNext.value = true
        _sickPoliticianAlert.value = null
        
        activeEncounterId = 0L
        lastLawsuitEncounterId = 0L
        lastExtractedBillingAmount = 0.0
        
        startNextPatientInternal()
    }

    fun dismissLobbyReport() {
        _lastLobbyReport.value = null
    }

    fun autoArchitectCompound(primaryMandate: String, userPrompt: String, onSuccess: (name: String, category: String, cost: String, bp: String, hr: String, effect: String, desc: String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentProvider = provider.value
                val currentModel = model.value
                val userKey = apiKey.value ?: ""
                val activeKey = resolveActiveApiKey(currentProvider, userKey)
                
                if (activeKey.isBlank()) {
                    logAndEmitError("API Key missing! Cannot auto-architect compound.")
                    _isLoading.value = false
                    return@launch
                }

                val prompt = CompoundArchitectHandler.generateDrugPrompt(primaryMandate, userPrompt)
                val apiResponse = makeFreshDirectApiCall(currentProvider, currentModel, activeKey, prompt, customEndpoint.value)
                val sanitized = extractJsonString(apiResponse)
                val parsed = CompoundArchitectHandler.parseDrugJson(sanitized)
                
                onSuccess(parsed.name, parsed.category, parsed.cost, parsed.bp, parsed.hr, parsed.effect, parsed.desc)
            } catch (e: Exception) {
                logAndEmitError("AI Architect failed: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun generateAiProofCertificate(userPrompt: String, onFinished: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentProvider = provider.value
                val currentModel = model.value
                val userKey = apiKey.value ?: ""
                val activeKey = resolveActiveApiKey(currentProvider, userKey)
                
                if (activeKey.isBlank()) {
                    logAndEmitError("API Key missing! Cannot issue certified credentials.")
                    _isLoading.value = false
                    return@launch
                }

                val prompt = SovereignProofHandler.generateProofPrompt(userPrompt, countryName.value)
                val apiResponse = makeFreshDirectApiCall(currentProvider, currentModel, activeKey, prompt, customEndpoint.value)
                val sanitized = extractJsonString(apiResponse)
                val parsedCert = SovereignProofHandler.parseCertificateJson(sanitized)
                
                OrchidDeepStateManager.addGeneratedCertificate(parsedCert)
                onFinished()
            } catch (e: Exception) {
                logAndEmitError("AI Proof Generation failed: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

}

data class PolicyAuditResult(
    val policyId: String,
    val policyTitle: String,
    val triggeredClause: String,
    val isViolation: Boolean,
    val penaltyAmount: Double,
    val scoreDeduction: Int,
    val auditMessage: String
)

data class AiStockingProposal(
    val explanation: String,
    val syringeQty: Int = 0,
    val salineQty: Int = 0,
    val adrenalineQty: Int = 0,
    val reagentsQty: Int = 0,
    val medsQty: Int = 0,
    val itemsToBuy: Map<String, Int> = emptyMap(),
    val estimatedTotalCost: Double = 0.0,
    val isValidPurchase: Boolean = false,
    val validationMessage: String = ""
)


