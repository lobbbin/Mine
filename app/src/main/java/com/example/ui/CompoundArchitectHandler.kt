package com.example.ui

import org.json.JSONObject

object CompoundArchitectHandler {

    fun generateDrugPrompt(primaryMandate: String, userPrompt: String): String {
        return """
            You are formulating a brand new fictional therapeutic drug/compound that adheres to the nation's parliamentary health directive:
            "$primaryMandate"
            
            The user has specified their preferred drug choice or instruction:
            "${userPrompt.ifBlank { "Create a suitable therapeutic compound matching the directive" }}"
            
            TASK:
            1. Read the user's preferred drug type or instruction.
            2. Look for similar established real-world reference drug compounds or mechanisms (e.g. if they request beta-blockers/calmative, model details on substances like Metoprolol, Valerian, etc.).
            3. Formulate a realistic, highly professional equivalent fictional compound.
            4. Fill in all the fields (name, category, cost, deltas, clinical indication, descriptions) realistically.
            
            Return a JSON object with the following string fields matching the requirements of the directory:
            - "name": Fictional but realistic Drug Name (e.g. Synthetix-500)
            - "scheduleCategory": e.g. "Schedule 4 (Prescription Medication)", "Schedule 5", or "Schedule 2 (OTC)"
            - "costPrice": e.g. "350" (just the number)
            - "bpDelta": e.g. "Raises (+10 mmHg)" or "Neutral"
            - "hrDelta": e.g. "Stabilizes (-5 bpm)" or "Increases (+20 bpm)"
            - "therapeuticEffect": Clinical indication effect (e.g. Rapidly combats acute respiratory infections while avoiding antibiotic resistance)
            - "pharmacologyDescription": A short, realistic pharmacology description.

            Make the compound creative but medically rigorous and directly suited to satisfy the provided governmental health directive and user's specific request.
            Ensure that the JSON is valid.
        """.trimIndent()
    }

    fun parseDrugJson(jsonStr: String): ParsedDrugCompound {
        return try {
            val json = JSONObject(jsonStr)
            ParsedDrugCompound(
                name = json.optString("name", "Novocaine-Ultra"),
                category = json.optString("scheduleCategory", "Schedule 4 (Prescription Medication)"),
                cost = json.optString("costPrice", "150.0"),
                bp = json.optString("bpDelta", "Neutral"),
                hr = json.optString("hrDelta", "Neutral"),
                effect = json.optString("therapeuticEffect", "Provides immediate relief."),
                desc = json.optString("pharmacologyDescription", "A highly effective new generation compound.")
            )
        } catch (e: Exception) {
            android.util.Log.e("CompoundArchitect", "Failed to parse generated drug JSON", e)
            ParsedDrugCompound(
                name = "Novocaine-Ultra",
                category = "Schedule 4 (Prescription Medication)",
                cost = "150.0",
                bp = "Neutral",
                hr = "Neutral",
                effect = "Provides immediate relief.",
                desc = "A highly effective new generation compound."
            )
        }
    }
}

data class ParsedDrugCompound(
    val name: String,
    val category: String,
    val cost: String,
    val bp: String,
    val hr: String,
    val effect: String,
    val desc: String
)
