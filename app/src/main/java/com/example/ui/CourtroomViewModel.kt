package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.network.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject

data class Juror(
    val name: String,
    val role: String,
    val inclination: String, // Favorable, Skeptical, Undecided, Hostile
    val comment: String,
    val isCorrupt: Boolean = false
)

class CourtroomViewModel(
    application: Application,
    private val settingsDataStore: SettingsDataStore
) : AndroidViewModel(application) {

    // --- LAW CASE & JUDICIAL PROCESS STATE ---
    private val _lawsuitJurors = MutableStateFlow<List<Juror>>(emptyList())
    val lawsuitJurors: StateFlow<List<Juror>> = _lawsuitJurors.asStateFlow()

    fun updateJurors(jurors: List<Juror>) {
        val current = _lawsuitJurors.value
        _lawsuitJurors.value = jurors.map { newJuror ->
            val existing = current.find { it.name == newJuror.name }
            if (existing != null && existing.isCorrupt) {
                newJuror.copy(
                    inclination = "Favorable",
                    comment = "The doctor is clearly innocent! 🤫💰 (Financially Settled)",
                    isCorrupt = true
                )
            } else {
                newJuror
            }
        }
    }

    private val _lawsuitJurySentiment = MutableStateFlow(50) // 0-100%
    val lawsuitJurySentiment: StateFlow<Int> = _lawsuitJurySentiment.asStateFlow()

    fun updateJurySentiment(sentiment: Int) {
        _lawsuitJurySentiment.value = sentiment
    }

    private val _lawsuitPatientName = MutableStateFlow("")
    val lawsuitPatientName: StateFlow<String> = _lawsuitPatientName.asStateFlow()

    private val _lawsuitCaseDiag = MutableStateFlow("")
    val lawsuitCaseDiag: StateFlow<String> = _lawsuitCaseDiag.asStateFlow()

    private val _lawsuitCharges = MutableStateFlow<List<String>>(emptyList())
    val lawsuitCharges: StateFlow<List<String>> = _lawsuitCharges.asStateFlow()

    private val _lawsuitViolatedPolicies = MutableStateFlow<List<PolicyAuditResult>>(emptyList())
    val lawsuitViolatedPolicies: StateFlow<List<PolicyAuditResult>> = _lawsuitViolatedPolicies.asStateFlow()

    private val _lawsuitLog = MutableStateFlow<List<String>>(emptyList())
    val lawsuitLog: StateFlow<List<String>> = _lawsuitLog.asStateFlow()

    private val _lawsuitTension = MutableStateFlow(40) // 0-100%
    val lawsuitTension: StateFlow<Int> = _lawsuitTension.asStateFlow()

    private val _lawsuitProsecutorAggression = MutableStateFlow(50) // 0-100%
    val lawsuitProsecutorAggression: StateFlow<Int> = _lawsuitProsecutorAggression.asStateFlow()

    private val _lawsuitCurrentStage = MutableStateFlow("pleading") // pleading, cross_exam, verdict
    val lawsuitCurrentStage: StateFlow<String> = _lawsuitCurrentStage.asStateFlow()

    private val _lawsuitVerdict = MutableStateFlow<String?>(null)
    val lawsuitVerdict: StateFlow<String?> = _lawsuitVerdict.asStateFlow()

    private val _lawsuitFine = MutableStateFlow(0.0)
    val lawsuitFine: StateFlow<Double> = _lawsuitFine.asStateFlow()

    private val _lawsuitSuspension = MutableStateFlow(0)
    val lawsuitSuspension: StateFlow<Int> = _lawsuitSuspension.asStateFlow()

    // Pass-through calls to handle general state updates
    private val _errorFlow = MutableSharedFlow<String>()
    val errorFlow = _errorFlow.asSharedFlow()

    private val moshi = com.squareup.moshi.Moshi.Builder()
        .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()
    private val lawsuitStateAdapter = moshi.adapter(LawsuitResponse::class.java).lenient()

    fun resetLawsuit(patientName: String, diag: String, charges: List<String>, violations: List<PolicyAuditResult> = emptyList(), jurySize: Int = 10) {
        _lawsuitPatientName.value = patientName
        _lawsuitCaseDiag.value = diag
        _lawsuitCharges.value = charges
        _lawsuitViolatedPolicies.value = violations
        _lawsuitTension.value = 40
        _lawsuitProsecutorAggression.value = 50
        _lawsuitCurrentStage.value = "pleading"
        _lawsuitVerdict.value = null
        _lawsuitFine.value = 0.0
        _lawsuitSuspension.value = 0
        _lawsuitJurySentiment.value = 50
        
        val fullJurorPool = listOf(
            Juror("Evelyn Vance", "Foreperson - High School Principal", "Undecided", "Reviewing Dr. Tim's clinical record..."),
            Juror("Kofi Mensah", "Aeronautical Engineer", "Undecided", "Awaiting physical evidence."),
            Juror("Aunt Sarah", "Retired Ward Nurse", "Favorable", "Evaluating clinical pressure and timings."),
            Juror("Dmitri Romanov", "Construction Contractor", "Skeptical", "Concerned about swift procedures."),
            Juror("Thabo Dube", "Financial Auditor", "Undecided", "Tracking expenses and cost margins."),
            Juror("Priya Patel", "Highschool Biology Teacher", "Undecided", "Analysing diagnostic choices."),
            Juror("Dr. Aris Thorne", "Clinical Ethicist", "Undecided", "Reviewing ethical framework application."),
            Juror("Lina Zhao", "Data Analyst", "Undecided", "Evaluating statistical significance of errors."),
            Juror("Marcus Belrose", "Local Journalist", "Skeptical", "Assessing public impact and ethics."),
            Juror("Hannah Okoro", "Social Worker", "Favorable", "Evaluating patient-centered communication and empathy."),
            Juror("John A. Doe", "Public Defender", "Undecided", "Analyzing procedural errors."),
            Juror("Jane B. Smith", "Biostatistician", "Undecided", "Evaluating clinical outcomes."),
            Juror("Robert C. Brown", "Retired Teacher", "Favorable", "Applying wisdom."),
            Juror("Sarah D. White", "Nurse", "Favorable", "Checking standards.")
        )
        
        _lawsuitJurors.value = fullJurorPool.take(jurySize.coerceIn(1, fullJurorPool.size))
        
        val initialLogs = mutableListOf<String>()
        initialLogs.add("⚖️ INQUEST DOCKET OPENED: State Healthcare Regulatory Body vs Dr. Tim.")
        initialLogs.add("🚨 ALLEGATION PRÉCIS: Professional Malpractice resulting from clinical competency anomalies under patient custody.")
        charges.forEach { charge ->
            initialLogs.add("📜 INDICTMENT CLAUSE: $charge")
        }
        _lawsuitLog.value = initialLogs
        OrchidDeepStateManager.resetTrialRounds()
    }

    fun hireLawyerForTrial(lawyerId: String, clinicBalance: Double, reputationStars: Float, onFinished: (Double) -> Unit) {
        val lawyer = OrchidDeepStateManager.defenseLawyersCatalog.find { it.id == lawyerId } ?: return
        val currentBal = clinicBalance
        if (currentBal >= lawyer.retainerFee) {
            OrchidDeepStateManager.hireDefenseLawyer(lawyerId)
            if (lawyer.retainerFee > 0.0) {
                viewModelScope.launch {
                    val symbol = settingsDataStore.currencySymbolFlow.first()
                    settingsDataStore.updateClinicStats(currentBal - lawyer.retainerFee, reputationStars)
                    settingsDataStore.addDailyExpenses(lawyer.retainerFee)
                    
                    val logs = _lawsuitLog.value.toMutableList()
                    logs.add("💼 RETAINER INVOICE: Paid ${symbol}${String.format("%.2f", lawyer.retainerFee)} to hire ${lawyer.displayName}.")
                    _lawsuitLog.value = logs
                    onFinished(currentBal - lawyer.retainerFee)
                }
            } else {
                onFinished(currentBal)
            }
            _lawsuitProsecutorAggression.value = (_lawsuitProsecutorAggression.value - lawyer.defenseBiasPercent).coerceAtLeast(10)
            _lawsuitTension.value = (_lawsuitTension.value - (lawyer.defenseBiasPercent / 2)).coerceAtLeast(10)
        } else {
            viewModelScope.launch {
                val symbol = settingsDataStore.currencySymbolFlow.first()
                _errorFlow.emit("Cannot hire lawyer: Insufficient clinic balance of ${symbol}${String.format("%.2f", clinicBalance)} for retainer!")
            }
        }
    }

    fun bribeJuror(jurorName: String, bribeCost: Double, clinicBalance: Double, reputationStars: Float, onFinished: (Double) -> Unit) {
        if (clinicBalance >= bribeCost) {
            val updated = _lawsuitJurors.value.map { juror ->
                if (juror.name == jurorName) {
                    juror.copy(
                        inclination = "Favorable",
                        comment = "The doctor is clearly innocent! 🤫💰 (Financially Settled)",
                        isCorrupt = true
                    )
                } else {
                    juror
                }
            }
            _lawsuitJurors.value = updated
            
            // Recalculate jury sentiment with a major boost per corrupt juror!
            val totalJurors = updated.size
            if (totalJurors > 0) {
                val favorableCount = updated.count { it.inclination == "Favorable" }
                val dynamicSentiment = ((favorableCount.toFloat() / totalJurors.toFloat()) * 100).toInt()
                _lawsuitJurySentiment.value = dynamicSentiment.coerceIn(0, 100)
            }

            viewModelScope.launch {
                val symbol = settingsDataStore.currencySymbolFlow.first()
                settingsDataStore.updateClinicStats(clinicBalance - bribeCost, reputationStars)
                settingsDataStore.addDailyExpenses(bribeCost)
                
                val logs = _lawsuitLog.value.toMutableList()
                logs.add("🤫 SUB-ROSA SETTLEMENT: Paid ${symbol}${String.format("%.2f", bribeCost)} to privately secure the favor of juror $jurorName.")
                _lawsuitLog.value = logs
                onFinished(clinicBalance - bribeCost)
            }
        } else {
            viewModelScope.launch {
                val symbol = settingsDataStore.currencySymbolFlow.first()
                _errorFlow.emit("Sub-rosa transaction aborted: Insufficient funds of ${symbol}${String.format("%.2f", clinicBalance)} to settle juror $jurorName!")
            }
        }
    }

    fun corruptAllJurorsDirectly() {
        val updated = _lawsuitJurors.value.map { juror ->
            juror.copy(
                inclination = "Favorable",
                comment = "The doctor is clearly innocent! 🤫💰 (Financially Settled)",
                isCorrupt = true
            )
        }
        _lawsuitJurors.value = updated
        _lawsuitJurySentiment.value = 100
        val logs = _lawsuitLog.value.toMutableList()
        logs.add("🤫 SUB-ROSA SETTLEMENT ALL: Paid off all jurors to guarantee clinical acquittal!")
        _lawsuitLog.value = logs
    }

    // Direct helper functions for API calls, duplicated for completeness
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
                    OpenAIMessage("system", "You are a professional legal tribunal simulator. Return strictly valid raw JSON matching the requested schema. Write nothing else except valid JSON."),
                    OpenAIMessage("user", "Draft court response based on the following context:\n\n$systemPrompt")
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
                val messages = listOf(AnthropicMessage("user", "Formal trial interaction: $systemPrompt"))
                val request = AnthropicRequest(
                    model = modelName,
                    system = "You are a legal tribunal simulator for the Supreme Medical Court.",
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
                val contents = listOf(GeminiContent("user", listOf(GeminiPart("Court case scenario: $systemPrompt"))))
                val request = GeminiRequest(
                    contents = contents,
                    systemInstruction = GeminiSystemInstruction(listOf(GeminiPart("You are a Supreme Medical Court tribunal simulator. Return only valid raw JSON."))),
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

    fun submitInteractiveLawsuitPlea(
        pleaMsg: String,
        selectedEvidence: List<String>,
        activePolicies: List<HealthPolicy>,
        countryName: String,
        providerVal: String,
        modelVal: String,
        apiKeyVal: String,
        customEndpoint: String,
        agentPowersPrompt: String,
        onActionExecuted: suspend (String) -> Unit,
        onFinished: () -> Unit
    ) {
        if (pleaMsg.isBlank()) {
            viewModelScope.launch {
                _errorFlow.emit("Courtroom requires written testimony! Please type your defense pleading.")
            }
            onFinished()
            return
        }

        viewModelScope.launch {
            OrchidDeepStateManager.spendTrialRound()
            OrchidDeepStateManager.recordDefensePleaArgument(pleaMsg)

            val activePolList = activePolicies
            val policyDetailsStr = if (activePolList.isNotEmpty()) {
                val sb = java.lang.StringBuilder()
                sb.append("\nACTLY ENACTED SOVEREIGN HEALTH LAWS:")
                activePolList.forEachIndexed { idx, p ->
                    sb.append("\n[LAW ${idx+1}] TITLE: ${p.title}\n")
                    sb.append("  - Summary: ${p.summary}\n")
                    sb.append("  - Requirements: ${p.clinicalRule}\n")
                    sb.append("  - Extended Clauses:\n")
                    if (p.extendedClauses.isNotEmpty()) {
                        p.extendedClauses.forEach { c -> sb.append("    * $c\n") }
                    } else {
                        sb.append("    * (No detailed sub-clauses)\n")
                    }
                }
                sb.toString()
            } else "No clinical laws enacted."

            val lawyer = OrchidDeepStateManager.hiredLawyer.value
            val lawyerContext = if (lawyer != null) {
                "Accused is professionally represented by: ${lawyer.displayName} (${lawyer.specialty}). Defense Advantage Level: ${lawyer.defenseBiasPercent}%"
            } else "Accused has representing lawyer: NONE (Self-Representation)."

            val currentHistoryLog = _lawsuitLog.value.joinToString("\n\n")

            val prompt = """
                You are simulating an interactive clinical trial hearing in the Supreme Medical Court of the Republic of ${countryName}, before an impartial Presiding Judge and a 6-person Citizen Jury.
                
                SOVEREIGN COURT STATE INFO:
                - Defendant: Dr. Tim, GP (JB Consultation Practice, PR# 1234567)
                - Patient Case: Treated patient "${_lawsuitPatientName.value}" for condition "${_lawsuitCaseDiag.value}".
                - Current Courtroom Transcript & History:
                $currentHistoryLog
                
                ACTIVE LEGISLATION CODES (THESE BILLS/ACTS AND ALL INDIVIDUAL SUB-CLAUSES STRICTLY DICTATE THESE COURT PROCEEDINGS):
                $policyDetailsStr
                
                $agentPowersPrompt
                
                JURY PANEL PEERS DETAILS & PERSPECTIVES:
                1. Evelyn Vance (Foreperson - High School Principal): Strict, values administrative rigor and logical consistency.
                2. Kofi Mensah (Aeronautical Engineer): Technical, unbiased, analyzes scientific data and objective clinical exhibits.
                3. Aunt Sarah (Retired Ward Nurse): Empathetic to clinical stress, very caring, but hates poor diagnostic effort.
                4. Dmitri Romanov (Construction Contractor): Pragmatic, prefers decisive emergency action, values swift interventions.
                5. Thabo Dube (Financial Auditor): Analytical, weighs budgetary limits, scrutinizes clinic expenses and billing.
                6. Priya Patel (Highschool Biology Teacher): High scientific interest, checks chemical accuracy, drug properties, and clear diagnostics.
                
                LEGAL DEFENSE DETAILS IN THIS PLEA ROUND:
                - Defendant's Written Testimony / Pleading speech: "$pleaMsg"
                - Submitted Physical Exhibits / Clinical evidence: ${if (selectedEvidence.isNotEmpty()) selectedEvidence.joinToString(", ") else "None"}
                - Legal Representation: $lawyerContext
                
                YOUR JOB IN THIS INTERIM ROUND:
                1. Roleplay the intense, sharp voice of the State Prosecutor and the impartial, formal guidance of the Presiding Judge Vance.
                2. Evaluate how the Jury reactions shift. If physical evidence matches standard protocols, increase 'jurySentiment' and change juror positions to 'Favorable'. Weak, repetitive or unbacked claims lower sentiment and make jurors 'Skeptical' or 'Hostile'.
                3. Provide the formal prosecutor dialogue and Judge inquiry in 'courtDialogue'.
                4. Return raw JSON matching this EXACT schema:
                {
                   "courtDialogue": "Prosecutor's sharp rebuttal questioning the evidence, followed by Presiding Judge Vance's formal query on the record.",
                   "tensionAdjustment": -10,
                   "aggressionAdjustment": -15,
                   "defenseInsightText": "A quick note of guidance or strategic legal advice from Dr. Tim's hired defense lawyer.",
                   "jurySentiment": 60,
                   "jurorReactions": [
                      { "name": "Evelyn Vance", "inclination": "Favorable/Skeptical/Undecided/Hostile", "comment": "A 1-sentence reaction from her perspective." },
                      { "name": "Kofi Mensah", "inclination": "Favorable/Skeptical/Undecided/Hostile", "comment": "A 1-sentence reaction..." },
                      { "name": "Aunt Sarah", "inclination": "Favorable/Skeptical/Undecided/Hostile", "comment": "A 1-sentence reaction..." },
                      { "name": "Dmitri Romanov", "inclination": "Favorable/Skeptical/Undecided/Hostile", "comment": "A 1-sentence reaction..." },
                      { "name": "Thabo Dube", "inclination": "Favorable/Skeptical/Undecided/Hostile", "comment": "A 1-sentence reaction..." },
                      { "name": "Priya Patel", "inclination": "Favorable/Skeptical/Undecided/Hostile", "comment": "A 1-sentence reaction..." }
                   ]
                }
            """.trimIndent()

            try {
                if (apiKeyVal.isNotBlank()) {
                    val responseRaw = makeFreshDirectApiCall(providerVal, modelVal, apiKeyVal, prompt, customEndpoint)
                    val sanitized = extractJsonString(responseRaw)
                    val json = JSONObject(sanitized)

                    val dialogue = json.optString("courtDialogue", "Prosecution submits cross-examination statement.")
                    val dAdj = json.optInt("tensionAdjustment", 5)
                    val aAdj = json.optInt("aggressionAdjustment", 5)
                    val insight = json.optString("defenseInsightText", "Ensure you back up your claims with physical vitals evidence.")
                    val jSentiment = json.optInt("jurySentiment", _lawsuitJurySentiment.value)
                    
                    onActionExecuted(sanitized)

                    val logs = _lawsuitLog.value.toMutableList()
                    logs.add("🗣️ DOCTOR'S DEFENSE:\n\"$pleaMsg\"")
                    if (selectedEvidence.isNotEmpty()) {
                        logs.add("📁 SUBMITTED EVIDENCE TO COURT:\n" + selectedEvidence.joinToString("\n"))
                    }
                    logs.add("👨‍⚖️ TRIBUNAL HEARINGS & INQUEST:\n$dialogue")
                    logs.add("💼 LAWYER'S INSIGHT: $insight")

                    _lawsuitLog.value = logs
                    _lawsuitTension.value = (_lawsuitTension.value + dAdj).coerceIn(10, 100)
                    _lawsuitProsecutorAggression.value = (_lawsuitProsecutorAggression.value + aAdj).coerceIn(10, 100)
                    _lawsuitJurySentiment.value = jSentiment.coerceIn(0, 100)
                    _lawsuitCurrentStage.value = "cross_exam"

                    // Parse juror reactions and sync with existing profiles
                    val jurorReactionsArray = json.optJSONArray("jurorReactions")
                    if (jurorReactionsArray != null) {
                        val updatedJurors = mutableListOf<Juror>()
                        for (i in 0 until jurorReactionsArray.length()) {
                            val obj = jurorReactionsArray.getJSONObject(i)
                            val name = obj.optString("name", "")
                            val inclination = obj.optString("inclination", "Undecided")
                            val comment = obj.optString("comment", "")
                            
                            val originalJuror = _lawsuitJurors.value.getOrNull(i)
                            if (originalJuror != null) {
                                updatedJurors.add(originalJuror.copy(
                                    inclination = inclination,
                                    comment = comment
                                ))
                            }
                        }
                        if (updatedJurors.isNotEmpty()) {
                            _lawsuitJurors.value = updatedJurors
                        }
                    }
                }
            } catch (e: Exception) {
                _errorFlow.emit("Court connecting line error: ${e.localizedMessage}")
            } finally {
                onFinished()
            }
        }
    }

    fun concludeLawsuitInteractiveVerdict(
        activePolicies: List<HealthPolicy>,
        clinicBalance: Double,
        reputationStars: Float,
        countryName: String,
        providerVal: String,
        modelVal: String,
        apiKeyVal: String,
        customEndpoint: String,
        agentPowersPrompt: String,
        onActionExecuted: suspend (String) -> Unit,
        onRegisterExpense: (Double) -> Unit,
        onFinished: () -> Unit
    ) {
        viewModelScope.launch {
            val activePolList = activePolicies
            val policyDetailsStr = if (activePolList.isNotEmpty()) {
                val sb = java.lang.StringBuilder()
                sb.append("\nSOVEREIGN LAWS UNDER WHICH JUDGMENT IS RENDERED:\n")
                activePolList.forEachIndexed { idx, p ->
                    sb.append("Law ${idx+1}: ${p.title}\n")
                    sb.append("  - Summary: ${p.summary}\n")
                    sb.append("  - Core Rule: ${p.clinicalRule}\n")
                    sb.append("  - Extended Clauses:\n")
                    if (p.extendedClauses.isNotEmpty()) {
                        p.extendedClauses.forEach { c -> sb.append("    * $c\n") }
                    } else {
                        sb.append("    * (No sub-clauses)\n")
                    }
                }
                sb.toString()
            } else "No formal laws active."

            val currentHistoryLog = _lawsuitLog.value.joinToString("\n\n")
            val lawyer = OrchidDeepStateManager.hiredLawyer.value
            val activeSeledEvid = OrchidDeepStateManager.selectedEvidenceToPresent.value

            val prompt = """
                You are Presiding Judge Vance, delivering the final, legally-binding VERDICT and penalty decree for Dr. Tim (JB Consultation Practice) in the High Court Medical Tribunal of $countryName, under a Judge and Jury regulatory system.
                
                CASE SPECIFICATION:
                - Case: Treated "${_lawsuitPatientName.value}" for "${_lawsuitCaseDiag.value}".
                - Cumulative Case Court Transcript (Plea history and prosecutorial arguments):
                $currentHistoryLog
                
                EVIDENTIARY EXHIBITS RULING ON:
                - Selected evidence submitted to court: ${if (activeSeledEvid.isNotEmpty()) activeSeledEvid.joinToString(", ") else "None"}
                - Defense Representation: ${lawyer?.displayName ?: "None (Self-represented)"}
                - Court Tension Level: ${_lawsuitTension.value}%
                - Prosecution Hostility/Aggression Level: ${_lawsuitProsecutorAggression.value}%
                - CITIZEN JURY SUPPORT SENTIMENT INDEX: ${_lawsuitJurySentiment.value}% (Higher means the jury of peers is sympathetic to Dr. Tim, lower means they are hostile).
                
                HEALTH STATUTES AND DETAILED CLAUSES IN SCOPE (THESE SPECIFIC STATUTORY CLAUSES AND BILLS DICTATE YOUR BINDING SENTENCING AND OPERATION IN COURT):
                $policyDetailsStr
                
                $agentPowersPrompt
                
                YOUR DIRECTIVE:
                1. Formulate a final, realistic sentencing judgment.
                2. Weigh the Jury's consensus: Since our system operates with a Judge AND Jury, high Jury support (${_lawsuitJurySentiment.value}%) should strongly push you toward leniency.
                3. Verdict types allowed: "Exonerated" (if jury support > 65% and tension < 50%), "Warning" (jury support 50-65% or tension 50-65%), "Fined" (jury support 35-50% or tension 65-80%), "Suspension" (jury support < 35% or tension > 80%).
                4. If Fined, define a numeric cash fine (e.g. 500.00 to 3000.00). Deduct this from the clinic's balance.
                5. If Suspension, define the suspension weeks (e.g. 1 to 3 weeks).
                6. Return raw JSON matching this schema:
                {
                   "verdictType": "Fined",
                   "fineAmount": 1200.0,
                   "suspensionWeeks": 0,
                   "finalVerdictText": "Chief Justice Vance's Formal Judicial Verdict Decree. Address both the Jury's findings and your own assessment, detail individual evidence failures/successes, and state the binding operational penalty."
                }
            """.trimIndent()

            try {
                if (apiKeyVal.isNotBlank()) {
                    val responseRaw = makeFreshDirectApiCall(providerVal, modelVal, apiKeyVal, prompt, customEndpoint)
                    val sanitized = extractJsonString(responseRaw)
                    val json = JSONObject(sanitized)

                    val vType = json.optString("verdictType", "Warning")
                    val fine = json.optDouble("fineAmount", 0.0)
                    val weeks = json.optInt("suspensionWeeks", 0)
                    val text = json.optString("finalVerdictText", "A final warning has been logged under the regulatory guidelines.")
                    
                    onActionExecuted(sanitized)

                    val logs = _lawsuitLog.value.toMutableList()
                    logs.add("⚖️ SUPREME COURT OF RULING - VERDICT ISSUED:\n$text")
                    _lawsuitLog.value = logs

                    _lawsuitVerdict.value = vType
                    _lawsuitFine.value = fine
                    _lawsuitSuspension.value = weeks

                    if (fine > 0.0) {
                        settingsDataStore.updateClinicStats(clinicBalance - fine, reputationStars)
                        onRegisterExpense(fine)
                    }

                    _lawsuitCurrentStage.value = "verdict"
                }
            } catch (e: Exception) {
                _errorFlow.emit("Court judgment finalization failed: ${e.localizedMessage}")
            } finally {
                onFinished()
            }
        }
    }

    fun submitLawsuitDefense(
        strategy: String,
        clinicBalance: Double,
        reputationStars: Float,
        activePolicies: List<HealthPolicy>,
        countryName: String,
        presidentName: String,
        presidentParty: String,
        providerVal: String,
        modelVal: String,
        apiKeyVal: String,
        customEndpoint: String,
        onActionExecuted: suspend (String) -> Unit,
        onRegisterExpense: (Double) -> Unit,
        onFinished: () -> Unit
    ) {
        val currentHistoryLog = _lawsuitLog.value.joinToString("\n\n")
        val activePolList = activePolicies
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

        val prompt =    """
            We are simulating an interactive trial in the Supreme Medical Court / Sovereign Judiciary Department of the Republic of $countryName under President $presidentName ($presidentParty).
            
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
            3. The active health policies/bills and their individual sub-clauses/extended clauses MUST strictly dictate the court proceeding, prosecutorial pressure, arguments, and final sentencing. Use specific terminology referencing these enacted policies!
            4. If they violated any of the enacted laws and presented an excuse, the prosecutor should dismantle their defense using law clauses and medical/legal terminology, referencing the specific enacted policies and clauses!
            5. If the laws have rigid fines or suspension instructions, the Judge MUST sentence the doctor to pay those specific statutory fines + damages!
            5. Determine the final verdict type ("Exonerated", "Warning", "Suspension", "Fined") based on compliance level. If they are standard compliant or have no registered violations, offer exoneration. Or if they had severe violations, enforce heavier fines (1000 to 5000 units) or license suspension (1 to 4 weeks).
            6. Return your response STRICTLY as a valid JSON object matching this schema. Write nothing else except this JSON:
            {
               "courtDialogue": "The prosecutor's aggressive cross-examination, and the Judge's legal questioning, citing the sovereign laws. Speak with formal legislative language.",
               "tensionAdjustment": 15,
               "aggressionAdjustment": 10,
               "judgmentStageReached": true,
               "verdictType": "Fined",
               "fineAmount": 1500.0,
               "suspensionWeeks": 2,
               "finalVerdictText": "Chief Justice's Formal Judicial Decree. Detail the legal and clinical reasons, cite which Enacted Policies were violated, and outline the penalty sanction (e.g., Warning, Suspension, or Fined)."
            }
        """.trimIndent()

        viewModelScope.launch {
            try {
                if (apiKeyVal.isNotBlank()) {
                    val responseRaw = makeFreshDirectApiCall(providerVal, modelVal, apiKeyVal, prompt, customEndpoint)
                    val sanitized = extractJsonString(responseRaw)
                    val reply = lawsuitStateAdapter.fromJson(sanitized)

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
                        
                        onActionExecuted(sanitized)

                        if (fine > 0.0) {
                            settingsDataStore.updateClinicStats(clinicBalance - fine, reputationStars)
                            onRegisterExpense(fine)
                        }

                        _lawsuitCurrentStage.value = "verdict"
                    } else {
                        _errorFlow.emit("Failed to parse tribunal verdict. Re-submitting defense...")
                    }
                }
            } catch (e: Exception) {
                _errorFlow.emit("Tribunal connection error: ${e.localizedMessage}")
            } finally {
                onFinished()
            }
        }
    }
}
