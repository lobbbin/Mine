package com.example.data

import android.util.Log

/**
 * Handles cascading interactions between the practitioner's clinical/legal decisions,
 * user interactions, active health policies, and the 80+ dynamic variables representing
 * Lysium's geoclinical and socio-economic systems.
 *
 * Keeps player entertained with reactive, simulated feedback without adding bloated UI elements.
 */
object SovereignSandboxGameplayHandler {

    private const val TAG = "SovereignSandboxGameplay"

    // Narrative ledger logging the cumulative cascades
    private val sandboxLedger = mutableListOf<String>()

    /**
     * Resets or prepares the sandbox state.
     */
    init {
        logEvent("[INITIALIZATION] Registered Deep Geopolitical & Syndemic Simulation Sandbox Matrix (80 canonical parameters activated). Ready for agentic evaluations.")
    }

    /**
     * Standardized event recording.
     */
    fun logEvent(msg: String) {
        synchronized(sandboxLedger) {
            sandboxLedger.add("[${System.currentTimeMillis()}] $msg")
            if (sandboxLedger.size > 100) {
                sandboxLedger.removeAt(0)
            }
        }
        Log.d(TAG, msg)
    }

    /**
     * Gets the latest events to display in the PDF audit ledger logs.
     */
    fun getLedgerEntries(): List<String> = synchronized(sandboxLedger) {
        sandboxLedger.toList()
    }

    /**
     * Processes a narrative text response from the practitioner (user message or diagnosis)
     * and triggers reactive feedback across the 80-parameter simulation state based on semantic keywords.
     */
    fun processClinicalDialogueCascades(dialogueText: String) {
        val lowerText = dialogueText.lowercase().trim()
        if (lowerText.isBlank()) return

        var mutated = false
        val summaries = mutableListOf<String>()

        // 1. Audit / Investigation / Suspicion Keyword Triggers
        if (lowerText.contains("bribe") || lowerText.contains("lobby") || lowerText.contains("payoff") || lowerText.contains("bribed")) {
            DeepClinicalSimulationEngine.tickGameStateSandbox(0, 5, 2500.0, 1)
            mutated = true
            summaries.add("Lobby bribe trigger: Sub-Rosa progressive lobby fund holds augmented; supreme investigator bribery efficiency coefficient surged by x1.15.")
        }

        if (lowerText.contains("audit") || lowerText.contains("prosecutor") || lowerText.contains("court") || lowerText.contains("commissioner") || lowerText.contains("compliance")) {
            DeepClinicalSimulationEngine.tickGameStateSandbox(1, -5, -4000.0, 3)
            mutated = true
            summaries.add("State audit trigger: Internal Security Agency surveillance status expanded by +8.2 points; court injunction hazard rating spiked.")
        }

        // 2. Quarantine / Pharmaceutical / Epidemic triggers
        if (lowerText.contains("quarantine") || lowerText.contains("isolation") || lowerText.contains("lockdown") || lowerText.contains("spore")) {
            DeepClinicalSimulationEngine.tickGameStateSandbox(3, 4, 0.0, 2)
            mutated = true
            summaries.add("Epidemiological quarantine protocols: Public Containment Panic Rating shifted; Spore Flu Carrier Spread Rate minimized by -0.015.")
        }

        if (lowerText.contains("antibiotic") || lowerText.contains("vaccine") || lowerText.contains("serum") || lowerText.contains("phage") || lowerText.contains("medication")) {
            DeepClinicalSimulationEngine.tickGameStateSandbox(2, 2, 800.0, 0)
            mutated = true
            summaries.add("Pharmaceutical intervention payload: Clinical Bio-Shield Integrity is reinforced; cellular apoptosis velocity stabilized.")
        }

        // 3. Financial Evasion / Tax evasion triggers
        if (lowerText.contains("tax") || lowerText.contains("evas") || lowerText.contains("cash") || lowerText.contains("offshore") || lowerText.contains("underground")) {
            DeepClinicalSimulationEngine.tickGameStateSandbox(1, -2, 5000.0, 1)
            mutated = true
            summaries.add("Sovereign asset bypass: Offshore secret clinical tax-evasion reserves credited; Internal security intelligence suspicion risk elevated by +4.5%.")
        }

        // General fallback cascade tick
        if (!mutated) {
            DeepClinicalSimulationEngine.tickGameStateSandbox(0, 0, 0.0, 0)
        } else {
            summaries.forEach { logEvent(it) }
        }
    }

    /**
     * Reacts to explicit AI-triggered tools, executing secondary mutations and ledger logs.
     */
    fun processToolInvocationCascades(toolName: String, args: Map<String, Any>) {
        when (toolName) {
            "applyFee" -> {
                val amount = (args["amount"] as? Number)?.toDouble() ?: 500.0
                val reason = args["reason"] as? String ?: "Unspecified Regulatory Surcharge"
                DeepClinicalSimulationEngine.tickGameStateSandbox(1, -10, -amount, 2)
                logEvent("[REGULATORY ENFORCEMENT] Applied penalty of $$amount due to: '$reason'. High Commissioner audits triggered. Internal Security surveillance rating spiked.")
            }
            "enactStatute" -> {
                val name = args["name"] as? String ?: "Sovereign Decree"
                DeepClinicalSimulationEngine.tickGameStateSandbox(2, 8, 0.0, 4)
                logEvent("[LEGISLATIVE INJUNCTION] Promulgated Statute: '$name'. Faction balance shifted; Progressive Lobby PAC holdings and legislative veto limits modified.")
            }
            "adjustReserves" -> {
                val amount = (args["amount"] as? Number)?.toDouble() ?: 0.0
                DeepClinicalSimulationEngine.tickGameStateSandbox(0, 2, amount, 0)
                logEvent("[TREASURY EXCHANGE] Cash reserves variance of $$amount recorded. Progressive & conservative bribery thresholds offset.")
            }
            "modifyInventory" -> {
                DeepClinicalSimulationEngine.tickGameStateSandbox(0, 1, 0.0, 0)
                logEvent("[DISPENSARY DEPLETION] Scarcity factors fluctuated. Medical Aid claim rejection rate recalculated.")
            }
            "trigger_dynamic_clinical_event" -> {
                val type = args["event_type"] as? String ?: "Syndemic Outbreak"
                DeepClinicalSimulationEngine.tickGameStateSandbox(4, -3, 0.0, 1)
                logEvent("[CRITICAL DISPATCH] Emergency Syndrome detected: '$type'. Public Containment Panic Rating surged; Emergency Generator runtime loaded.")
            }
            "restructure_national_medical_aid" -> {
                val name = args["scheme_name"] as? String ?: "State Fund"
                DeepClinicalSimulationEngine.tickGameStateSandbox(1, 4, 1500.0, 2)
                logEvent("[SCHEME SYSTEMIC REORGANIZATION] National health fund terms rebuilt: '$name'. Corporate lobbyist sentiment ratio altered.")
            }
        }
    }
}
