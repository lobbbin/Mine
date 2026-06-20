package com.example.ui

import android.util.Log
import com.example.data.*
import com.example.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class GameAgent(
    private val executeToolCall: suspend (name: String, args: Map<String, Any>) -> String
) {

    // 15 Tool definitions for Gemini Native Function Calling Schema
    val geminiTools: List<GeminiTool> = listOf(
        GeminiTool(
            functionDeclarations = listOf(
                GeminiFunctionDeclaration(
                    name = "recommend_medication",
                    description = "Recommend appropriate drugs for the patient based on the diagnosis and condition.",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "diagnosis" to mapOf("type" to "string", "description" to "The diagnosis to recommend drugs for"),
                            "current_medications" to mapOf("type" to "array", "items" to mapOf("type" to "string"), "description" to "Medications already planned")
                        ),
                        "required" to listOf("diagnosis")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "update_patient_intake",
                    description = "Update the patient's registration intake data.",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "data_json" to mapOf("type" to "string", "description" to "The JSON encoded intake form data")
                        ),
                        "required" to listOf("data_json")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "process_intake_form",
                    description = "Process the patient registration intake form and update the simulation state with the patient data.",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "data_json" to mapOf("type" to "string", "description" to "The JSON encoded intake form data")
                        ),
                        "required" to listOf("data_json")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "applyFee",
                    description = "Apply a financial penalty fine to the clinic.",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "amount" to mapOf("type" to "number", "description" to "The monetary amount of the fine"),
                            "reason" to mapOf("type" to "string", "description" to "The legal justification for the fine")
                        ),
                        "required" to listOf("amount", "reason")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "enactStatute",
                    description = "Pass a new healthcare law or statutory regulation into effect.",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "id" to mapOf("type" to "string", "description" to "Unique short ID for the law"),
                            "name" to mapOf("type" to "string", "description" to "Formal title of the law"),
                            "description" to mapOf("type" to "string", "description" to "Complete text of the regulation"),
                            "penalty" to mapOf("type" to "string", "description" to "Standard penalty for violation")
                        ),
                        "required" to listOf("id", "name", "description", "penalty")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "repealStatute",
                    description = "Remove an existing law from the books.",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "id" to mapOf("type" to "string", "description" to "The ID of the law to repeal")
                        ),
                        "required" to listOf("id")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "updateLicense",
                    description = "Modify the status of the practitioner's medical license.",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "status" to mapOf("type" to "string", "enum" to listOf("ACTIVE", "PROBATION", "SUSPENDED", "REVOKED")),
                            "justification" to mapOf("type" to "string", "description" to "Why the status changed"),
                            "suspensionWeeks" to mapOf("type" to "integer", "description" to "If suspended, how many weeks to advance time")
                        ),
                        "required" to listOf("status", "justification")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "adjustReserves",
                    description = "Directly debit or credit the clinic's cash reserves.",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "amount" to mapOf("type" to "number", "description" to "Positive for credit, negative for debit"),
                            "reason" to mapOf("type" to "string", "description" to "Accounting reason")
                        ),
                        "required" to listOf("amount", "reason")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "publishNews",
                    description = "Broadcast a breaking news alert to the app's ticker.",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "headline" to mapOf("type" to "string"),
                            "body" to mapOf("type" to "string")
                        ),
                        "required" to listOf("headline", "body")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "modifyInventory",
                    description = "Change the quantity of items in the clinical dispensary.",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "item" to mapOf("type" to "string", "description" to "Item name"),
                            "change" to mapOf("type" to "integer", "description" to "Amount to add or subtract")
                        ),
                        "required" to listOf("item", "change")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "sendCmoDirective",
                    description = "Send a high-priority textual instruction as the Chief Medical Officer.",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "message" to mapOf("type" to "string")
                        ),
                        "required" to listOf("message")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "update_patient_vitals_and_symptoms",
                    description = "Update the active patient's vitals (HR, BP, O2) and append new symptoms dynamically.",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "heart_rate" to mapOf("type" to "integer"),
                            "blood_pressure_systolic" to mapOf("type" to "integer"),
                            "blood_pressure_diastolic" to mapOf("type" to "integer"),
                            "oxygen_saturation" to mapOf("type" to "integer"),
                            "new_symptoms" to mapOf("type" to "string")
                        ),
                        "required" to listOf("heart_rate", "blood_pressure_systolic", "blood_pressure_diastolic", "oxygen_saturation", "new_symptoms")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "trigger_dynamic_clinical_event",
                    description = "Trigger a random dynamic clinical crisis or sudden health emergency for the patient.",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "event_type" to mapOf("type" to "string"),
                            "urgency_level" to mapOf("type" to "integer")
                        ),
                        "required" to listOf("event_type", "urgency_level")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "modify_patient_trust_and_compliance",
                    description = "Adjust patient trust levels or reveal a hidden medical secret.",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "trust_delta" to mapOf("type" to "integer"),
                            "hidden_secret_revealed" to mapOf("type" to "boolean")
                        ),
                        "required" to listOf("trust_delta", "hidden_secret_revealed")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "execute_staff_action_or_morale_shift",
                    description = "Trigger clinical staff actions or shift workplace morale.",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "staff_member_name" to mapOf("type" to "string"),
                            "morale_change" to mapOf("type" to "integer"),
                            "action_taken" to mapOf("type" to "string")
                        ),
                        "required" to listOf("staff_member_name", "morale_change", "action_taken")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "simulate_supply_chain_or_market_event",
                    description = "Modify price factors or trigger scarcity for clinical consumable items.",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "item_id" to mapOf("type" to "string"),
                            "price_multiplier" to mapOf("type" to "number"),
                            "stock_depleted" to mapOf("type" to "integer"),
                            "reason" to mapOf("type" to "string")
                        ),
                        "required" to listOf("item_id", "price_multiplier", "stock_depleted", "reason")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "trigger_facility_infrastructure_crisis",
                    description = "Trigger physical failures in the clinic like load-shedding power blackouts or water leaks.",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "crisis_type" to mapOf("type" to "string"),
                            "affected_areas" to mapOf("type" to "array", "items" to mapOf("type" to "string"))
                        ),
                        "required" to listOf("crisis_type", "affected_areas")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "evaluate_and_award_clinical_xp",
                    description = "Award career XP, clinical revenue tokens, and post reasoning feedback.",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "xp_awarded" to mapOf("type" to "integer"),
                            "cash_revenue" to mapOf("type" to "number"),
                            "reasoning_grade" to mapOf("type" to "string")
                        ),
                        "required" to listOf("xp_awarded", "cash_revenue", "reasoning_grade")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "generate_attending_socratic_feedback",
                    description = "Provide Socratic query hints as a senior MD attending supervisor.",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "mentor_name" to mapOf("type" to "string"),
                            "feedback_text" to mapOf("type" to "string"),
                            "focus_area" to mapOf("type" to "string")
                        ),
                        "required" to listOf("mentor_name", "feedback_text", "focus_area")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "shift_community_reputation_and_demographics",
                    description = "Shift community health reputation stars and configure upcoming patient profile demographics.",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "reputation_delta" to mapOf("type" to "integer"),
                            "next_patient_archetype" to mapOf("type" to "string")
                        ),
                        "required" to listOf("reputation_delta", "next_patient_archetype")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "initiate_regulatory_investigation",
                    description = "Open an Medical Board or state malpractice investigation audit on the doctor.",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "investigation_reason" to mapOf("type" to "string"),
                            "severity" to mapOf("type" to "string"),
                            "deadline_days" to mapOf("type" to "integer")
                        ),
                        "required" to listOf("investigation_reason", "severity", "deadline_days")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "restructure_national_medical_aid",
                    description = "Change the terms, coverage, and payout rates of the national medical aid schemes.",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "scheme_id" to mapOf("type" to "string"),
                            "scheme_name" to mapOf("type" to "string"),
                            "coverage_percent" to mapOf("type" to "number"),
                            "requires_pre_auth" to mapOf("type" to "boolean"),
                            "rejection_probability" to mapOf("type" to "number")
                        ),
                        "required" to listOf("scheme_id", "scheme_name", "coverage_percent", "requires_pre_auth", "rejection_probability")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "enact_new_medical_statute",
                    description = "Enact a new health statute directly into Elyisum's sovereign archives.",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "statute_name" to mapOf("type" to "string"),
                            "statute_description" to mapOf("type" to "string"),
                            "effective_immediately" to mapOf("type" to "boolean")
                        ),
                        "required" to listOf("statute_name", "statute_description", "effective_immediately")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "resolve_political_lobbying_outcome",
                    description = "Resolve ongoing party lobbying adjustments automatically via narrative outcomes.",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "faction_name" to mapOf("type" to "string"),
                            "influence_shift" to mapOf("type" to "integer"),
                            "bill_status" to mapOf("type" to "string"),
                            "narrative_outcome" to mapOf("type" to "string")
                        ),
                        "required" to listOf("faction_name", "influence_shift", "bill_status", "narrative_outcome")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "issue_legal_subpoena_for_records",
                    description = "Issue a formal legal subpoena demand for patient medical logs.",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "requested_record_type" to mapOf("type" to "string"),
                            "compliance_deadline" to mapOf("type" to "string")
                        ),
                        "required" to listOf("requested_record_type", "compliance_deadline")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "generate_media_scandal_or_news_event",
                    description = "Formulate a breaking news media scandal impacting clinic prestige.",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "headline" to mapOf("type" to "string"),
                            "article_body" to mapOf("type" to "string"),
                            "public_outcry_level" to mapOf("type" to "integer")
                        ),
                        "required" to listOf("headline", "article_body", "public_outcry_level")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "finalize_patient_encounter_outcome",
                    description = "Conclude the active clinical dialogue, billing rates, and malpractice liabilities.",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "disposition" to mapOf("type" to "string"),
                            "final_billing_amount" to mapOf("type" to "number"),
                            "malpractice_risk" to mapOf("type" to "integer")
                        ),
                        "required" to listOf("disposition", "final_billing_amount", "malpractice_risk")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "auditEncounter",
                    description = "Check the clinical conversation history details against signed laws to flag violations.",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "transcript" to mapOf("type" to "string"),
                            "active_laws" to mapOf("type" to "string")
                        ),
                        "required" to listOf("transcript", "active_laws")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "grantPresidentialPardon",
                    description = "Grant a presidential pardon for statutory fines or license suspensions.",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "type" to mapOf("type" to "string", "enum" to listOf("fine", "suspension"), "description" to "The type of pardon to grant"),
                            "justification" to mapOf("type" to "string", "description" to "Executive reasoning for the pardon")
                        ),
                        "required" to listOf("type", "justification")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "add_custom_ui_button",
                    description = "Dynamically deploy a new action/bylaw button onto the doctor's clinic dashboard UI. The player can click it to execute corresponding sovereign actions and logic.",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "label" to mapOf("type" to "string", "description" to "The interactive label text on the button, e.g. 'Enforce Mask Tariff' or 'Tissue Levy'"),
                            "hexColor" to mapOf("type" to "string", "description" to "Accent color of the button in hex format (e.g. '#FF1744' or '#00E676')"),
                            "promptText" to mapOf("type" to "string", "description" to "The system overlay response/guidance that runs first when clicked (keeps context of this law)"),
                            "kotlinLogic" to mapOf("type" to "string", "description" to "Fidelity emulated state modifiers. Format: 'variable += value' or 'variable -= value' separated by newlines. Valid variables: clinicBalance, politicalPrestige, reputationStars, consultationFee. E.g. 'clinicBalance += 500\npoliticalPrestige -= 5'")
                        ),
                        "required" to listOf("label", "hexColor", "promptText", "kotlinLogic")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "execute_custom_logic",
                    description = "Immediately execute a custom emulated logic sequence on client-side properties (e.g. confiscate assets or apply custom tax logic).",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "kotlinLogic" to mapOf("type" to "string", "description" to "Emulated lines. E.g. 'clinicBalance -= 5000\npoliticalPrestige += 10'"),
                            "explanation" to mapOf("type" to "string", "description" to "Why this direct state modification was enacted")
                        ),
                        "required" to listOf("kotlinLogic", "explanation")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "set_clinic_notice",
                    description = "Display a sovereign government announcement notice card or alert broadcast directly at the top of the clinic dashboard screen.",
                    parameters = mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "headline" to mapOf("type" to "string", "description" to "e.g. 'EMERGENCY BIOTERROR PROTOCOL'"),
                            "message" to mapOf("type" to "string", "description" to "Detailed mandate explanation or health warning text"),
                            "severity" to mapOf("type" to "string", "enum" to listOf("Low", "Medium", "High", "Critical"), "description" to "Notice styling color indicator severity")
                        ),
                        "required" to listOf("headline", "message", "severity")
                    )
                )
            )
        )
    )

    suspend fun makeDirectApiCall(
        provider: String,
        modelName: String,
        apiKey: String,
        systemPrompt: String,
        chatHistory: List<ChatMessage>,
        customUrl: String,
        rotatorKeys: Map<String, String> = emptyMap(),
        rotatorEnabledModels: Set<String> = emptySet()
    ): String {
        return withContext(Dispatchers.IO) {
            if (provider == "Auto-Swapping Rotator") {
                executeRotatorSwapFlow(systemPrompt, chatHistory, rotatorKeys, rotatorEnabledModels)
            } else {
                when (provider) {
                "OpenRouter", "Cerebras", "OpenAI", "Nvidia", "Ollama", "vLLM", "G4F (OpenAI-compatible)", "OpenCode (Zen)", "Kilocode", "Custom (OpenAI-compatible)" -> {
                    val activeKey = if (apiKey.isBlank()) "sk-no-key-required" else apiKey
                    val messages = mutableListOf<OpenAIMessage>()
                    messages.add(OpenAIMessage("system", systemPrompt))
                    
                    val chatTurns = chatHistory.takeLast(20)
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
                        modelName.contains("minimax", ignoreCase = true) -> {
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
                        response_format = if (isCustomUrl || provider in listOf("OpenRouter", "Cerebras", "Nvidia", "Ollama", "vLLM", "G4F (OpenAI-compatible)", "OpenCode (Zen)", "Kilocode", "Custom (OpenAI-compatible)")) null else OpenAIResponseFormat("json_object"),
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
                    val chatTurns = chatHistory.takeLast(20)
                    
                    chatTurns.forEach {
                        val roleMapped = if (it.role == "doctor") "user" else "assistant"
                        messages.add(AnthropicMessage(roleMapped, it.text))
                    }

                    if (messages.isEmpty()) {
                        messages.add(AnthropicMessage("user", "Hello! Let's start the case."))
                    } else if (messages.first().role == "assistant") {
                        messages.add(0, AnthropicMessage("user", "Please start clinical dialogue."))
                    }

                    val filteredMessages = mutableListOf<AnthropicMessage>()
                    var expectedRole = "user"
                    messages.forEach { msg ->
                        if (msg.role == expectedRole) {
                            filteredMessages.add(msg)
                            expectedRole = if (expectedRole == "user") "assistant" else "user"
                        } else if (filteredMessages.isNotEmpty() && msg.role != expectedRole) {
                            val last = filteredMessages.last()
                            filteredMessages[filteredMessages.size - 1] = last.copy(content = last.content + "\n" + msg.content)
                        }
                    }

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
                else -> { // Google Gemini (or Google customized service)
                    handleGeminiWithToolsLoop(modelName, apiKey, systemPrompt, chatHistory, customUrl)
                }
            }
            }
        }
    }

    private suspend fun handleGeminiWithToolsLoop(
        modelName: String,
        apiKey: String,
        systemPrompt: String,
        chatHistory: List<ChatMessage>,
        customUrl: String
    ): String {
        val contents = mutableListOf<GeminiContent>()
        val chatTurns = chatHistory.takeLast(20)
        chatTurns.forEach {
            val roleMapped = if (it.role == "doctor") "user" else "model"
            contents.add(GeminiContent(roleMapped, listOf(GeminiPart(text = it.text))))
        }

        if (contents.isEmpty()) {
            contents.add(GeminiContent("user", listOf(GeminiPart(text = "Initialize clinical encounter patient dialogue."))))
        }

        var activeUrl = getActiveUrl("Google", modelName, apiKey, customUrl)
        var iteration = 0
        val maxIterations = 5

        while (iteration < maxIterations) {
            val request = GeminiRequest(
                contents = contents,
                systemInstruction = GeminiSystemInstruction(listOf(GeminiPart(text = systemPrompt))),
                generationConfig = GeminiGenerationConfig(
                    maxOutputTokens = 8192,
                    temperature = 0.7
                ),
                tools = geminiTools
            )

            val response = RetrofitClient.service.callGemini(activeUrl, request)
            val candidate = response.candidates?.firstOrNull()
            val content = candidate?.content
            val parts = content?.parts ?: emptyList()

            // Find if there is a functionCall
            val functionCallPart = parts.firstOrNull { it.functionCall != null }
            val functionCall = functionCallPart?.functionCall

            if (functionCall != null) {
                Log.d("GameAgent", "[TOOL RUNNING] AI requested tool: ${functionCall.name} with args ${functionCall.args}")
                
                // Execute Kotlin side effect
                val outcomeText = executeToolCall(functionCall.name, functionCall.args)
                Log.d("GameAgent", "[TOOL RESOLVED] Result: $outcomeText")

                // Add the model's turn to conversation history
                contents.add(GeminiContent(role = "model", parts = parts))

                // Construct function response part
                val responsePart = GeminiPart(
                    functionResponse = GeminiFunctionResponse(
                        name = functionCall.name,
                        response = mapOf("result" to outcomeText)
                    )
                )
                // Add the function execution outcome to conversation history
                contents.add(GeminiContent(role = "function", parts = listOf(responsePart)))
                
                iteration++
            } else {
                // Return plain text output
                val textOutput = parts.firstOrNull { !it.text.isNullOrBlank() }?.text
                return textOutput ?: "{}"
            }
        }

        return "{}"
    }

    private fun getActiveUrl(provider: String, modelName: String, apiKey: String, customUrl: String): String {
        if (customUrl.isNotBlank()) {
            val base = customUrl.trim()
            return when (provider) {
                "OpenRouter", "Cerebras", "OpenAI", "Nvidia", "Ollama", "vLLM", "G4F (OpenAI-compatible)", "OpenCode (Zen)", "Kilocode", "Custom (OpenAI-compatible)" -> {
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
            "OpenRouter" -> "https://openrouter.ai/api/v1/chat/completions"
            "OpenAI" -> "https://api.openai.com/v1/chat/completions"
            "Cerebras" -> "https://api.cerebras.ai/v1/chat/completions"
            "Nvidia" -> "https://integrate.api.nvidia.com/v1/chat/completions"
            "Anthropic" -> "https://api.anthropic.com/v1/messages"
            "Ollama" -> "http://10.0.2.2:11434/v1/chat/completions"
            "vLLM" -> "http://10.0.2.2:8000/v1/chat/completions"
            "G4F (OpenAI-compatible)" -> "http://10.0.2.2:1337/v1/chat/completions"
            "OpenCode (Zen)" -> "https://api.opencode.ai/v1/chat/completions"
            "Kilocode" -> "https://api.kilocode.xyz/v1/chat/completions"
            "Custom (OpenAI-compatible)" -> "http://10.0.2.2:8080/v1/chat/completions"
            else -> "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
        }
    }

    private suspend fun executeRotatorSwapFlow(
        systemPrompt: String,
        chatHistory: List<ChatMessage>,
        rotatorKeys: Map<String, String>,
        enabledModels: Set<String> = emptySet()
    ): String {
        data class RotatorConfig(
            val name: String,
            val keyName: String,
            val url: String,
            val models: List<String>,
            val isGemini: Boolean = false
        )

        val rotation = listOf(
            RotatorConfig(
                name = "Groq",
                keyName = "groq",
                url = "https://api.groq.com/openai/v1/chat/completions",
                models = listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant", "mixtral-8x7b-32768")
            ),
            RotatorConfig(
                name = "OpenRouter",
                keyName = "openrouter",
                url = "https://openrouter.ai/api/v1/chat/completions",
                models = listOf("openrouter/auto", "google/gemini-2.5-flash:free", "meta-llama/llama-3.3-70b-instruct:free", "deepseek/deepseek-r1:free")
            ),
            RotatorConfig(
                name = "Cerebras",
                keyName = "cerebras",
                url = "https://api.cerebras.ai/v1/chat/completions",
                models = listOf("llama-3.3-70b", "llama-3.1-8b", "llama3.1-8b")
            ),
            RotatorConfig(
                name = "Google AI Studio",
                keyName = "google",
                url = "",
                models = listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-1.5-flash"),
                isGemini = true
            ),
            RotatorConfig(
                name = "Nvidia NIM",
                keyName = "nvidia",
                url = "https://integrate.api.nvidia.com/v1/chat/completions",
                models = listOf(
                    "meta/llama-3.3-70b-instruct",
                    "nvidia/llama-3.1-nemotron-70b-instruct",
                    "meta/llama-3.1-8b-instruct",
                    "nvidia/nemotron-4-340b-instruct",
                    "nvidia/nemotron-mini-4b-instruct",
                    "nvidia/nemotron-3-nano-30b-a3b:free",
                    "nvidia/nemotron-3-super-120b-a12b:free",
                    "nvidia/nemotron-nano-12b-v2-vl:free",
                    "nvidia/nemotron-nano-9b-v2:free"
                )
            ),
            RotatorConfig(
                name = "OpenCode (Zen)",
                keyName = "opencode",
                url = "https://api.opencode.ai/v1/chat/completions",
                models = listOf(
                    "deepseek-v4-pro",
                    "glm-5.1",
                    "kimi-k2.6",
                    "qwen3.5-plus",
                    "qwen3.6-plus",
                    "qwen3.6-plus-free",
                    "grok-build-0.1",
                    "minimax-m2.7",
                    "minimax-m3-free",
                    "mimo-v2.5-free",
                    "nemotron-3-ultra-free",
                    "north-mini-code-free"
                )
            ),
            RotatorConfig(
                name = "Kilocode",
                keyName = "kilocode",
                url = "https://api.kilocode.xyz/v1/chat/completions",
                models = listOf(
                    "arcee-trinity-large-preview",
                    "minimax-m2.5",
                    "mistralai/devstral-2512",
                    "grok-code-fast-1",
                    "gemini-3-flash-preview",
                    "claude-haiku-4.5"
                )
            ),
            RotatorConfig(
                name = "SambaNova",
                keyName = "sambanova",
                url = "https://api.sambanova.ai/v1/chat/completions",
                models = listOf("Meta-Llama-3.3-70B-Instruct", "Meta-Llama-3.1-8B-Instruct", "Meta-Llama-3.1-405B-Instruct")
            ),
            RotatorConfig(
                name = "Together AI",
                keyName = "together",
                url = "https://api.together.xyz/v1/chat/completions",
                models = listOf("meta-llama/Llama-3.3-70B-Instruct-Turbo", "meta-llama/Meta-Llama-3.1-8B-Instruct-Turbo", "mistralai/Mixtral-8x7B-Instruct-v0.1")
            ),
            RotatorConfig(
                name = "Fireworks AI",
                keyName = "fireworks",
                url = "https://api.fireworks.ai/inference/v1/chat/completions",
                models = listOf("accounts/fireworks/models/llama-v3p3-70b-instruct", "accounts/fireworks/models/llama-v3p1-8b-instruct", "accounts/fireworks/models/mixtral-8x7b-instruct")
            ),
            RotatorConfig(
                name = "Mistral AI",
                keyName = "mistral",
                url = "https://api.mistral.ai/v1/chat/completions",
                models = listOf("open-mistral-7b", "mistral-small-latest", "mistral-large-latest")
            ),
            RotatorConfig(
                name = "Cohere",
                keyName = "cohere",
                url = "https://api.cohere.com/v1/chat/completions",
                models = listOf("command-r-plus", "command-r", "command-light")
            ),
            RotatorConfig(
                name = "DeepSeek",
                keyName = "deepseek",
                url = "https://api.deepseek.com/chat/completions",
                models = listOf("deepseek-chat", "deepseek-coder", "deepseek-reasoner")
            ),
            RotatorConfig(
                name = "DeepInfra",
                keyName = "deepinfra",
                url = "https://api.deepinfra.com/v1/openai/chat/completions",
                models = listOf("meta-llama/Llama-3.3-70B-Instruct", "meta-llama/Meta-Llama-3.1-8B-Instruct", "mistralai/Mixtral-8x22B-Instruct-v0.1")
            ),
            RotatorConfig(
                name = "Novita AI",
                keyName = "novita",
                url = "https://api.novita.ai/v1/chat/completions",
                models = listOf("meta-llama/llama-3.3-70b-instruct", "meta-llama/llama-3.1-8b-instruct", "mistralai/mistral-7b-instruct")
            ),
            RotatorConfig(
                name = "Hyperbolic",
                keyName = "hyperbolic",
                url = "https://api.hyperbolic.xyz/v1/chat/completions",
                models = listOf("meta-llama/Llama-3.3-70B-Instruct", "meta-llama/Meta-Llama-3.1-8B-Instruct", "deepseek-ai/DeepSeek-V3")
            )
        )

        var lastErrorMsg = "No keys configured."
        for (config in rotation) {
            val key = rotatorKeys[config.keyName]?.trim() ?: ""
            if (key.isBlank()) {
                Log.d("GameAgent", "[ROTATOR] Skipping ${config.name} because key is empty/unconfigured.")
                continue
            }

            val modelsToUse = if (enabledModels.isEmpty()) {
                config.models
            } else {
                config.models.filter { enabledModels.contains(it) }
            }
            if (modelsToUse.isEmpty()) {
                Log.d("GameAgent", "[ROTATOR] Skipping ${config.name} because no models under this provider are checked in settings.")
                continue
            }

            Log.d("GameAgent", "[ROTATOR] Provider ${config.name} available. Starting cycling...")
            for (activeModel in modelsToUse) {
                Log.d("GameAgent", "[ROTATOR] Attempting model '$activeModel' on ${config.name}...")
                try {
                    if (config.isGemini) {
                        val activeUrl = "https://generativelanguage.googleapis.com/v1beta/models/$activeModel:generateContent?key=$key"
                        val contents = mutableListOf<GeminiContent>()
                        val chatTurns = chatHistory.takeLast(20)
                        chatTurns.forEach {
                            val roleMapped = if (it.role == "doctor") "user" else "model"
                            contents.add(GeminiContent(roleMapped, listOf(GeminiPart(text = it.text))))
                        }
                        if (contents.isEmpty()) {
                            contents.add(GeminiContent("user", listOf(GeminiPart(text = "Initialize clinical encounter patient dialogue."))))
                        }

                        val request = GeminiRequest(
                            contents = contents,
                            systemInstruction = GeminiSystemInstruction(listOf(GeminiPart(text = systemPrompt))),
                            generationConfig = GeminiGenerationConfig(
                                maxOutputTokens = 8192,
                                temperature = 0.7
                            ),
                            tools = null
                        )

                        val response = RetrofitClient.service.callGemini(activeUrl, request)
                        val candidate = response.candidates?.firstOrNull()
                        val textOutput = candidate?.content?.parts?.firstOrNull { !it.text.isNullOrBlank() }?.text
                        if (!textOutput.isNullOrBlank()) {
                            Log.d("GameAgent", "[ROTATOR] Got successful response from ${config.name} ($activeModel)!")
                            return textOutput
                        } else {
                            throw Exception("Empty response returned from Google AI Studio")
                        }
                    } else {
                        val messages = mutableListOf<OpenAIMessage>()
                        messages.add(OpenAIMessage("system", systemPrompt))
                        val chatTurns = chatHistory.takeLast(20)
                        chatTurns.forEach {
                            val roleMapped = if (it.role == "doctor") "user" else "assistant"
                            messages.add(OpenAIMessage(roleMapped, it.text))
                        }

                        val request = OpenAIRequest(
                            model = activeModel,
                            messages = messages,
                            response_format = null,
                            temperature = 0.7,
                            max_tokens = 4096,
                            stream = false
                        )

                        val response = RetrofitClient.service.callOpenAI(
                            url = config.url,
                            authorization = "Bearer $key",
                            accept = "application/json",
                            body = request
                        )
                        val textOutput = response.choices.firstOrNull()?.message?.content
                        if (!textOutput.isNullOrBlank()) {
                            Log.d("GameAgent", "[ROTATOR] Got successful response from ${config.name} ($activeModel)!")
                            return textOutput
                        } else {
                            throw Exception("Empty response returned from OpenAI endpoint")
                        }
                    }
                } catch (e: Exception) {
                    val errMsg = e.localizedMessage ?: "Unknown Error"
                    Log.e("GameAgent", "[ROTATOR] ${config.name} (model: $activeModel) failed: $errMsg")
                    lastErrorMsg = "${config.name} [$activeModel]: $errMsg"
                }
            }
        }

        return "Error: All daily free tiers and loaded providers in rotation are completely maxed out. Last error context: $lastErrorMsg"
    }
}
