package com.example.ui

import org.json.JSONObject
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LegalCertificate(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val registrationNumber: String,
    val issuer: String,
    val issueDate: String,
    val verificationDetails: String,
    val sealEmoji: String = "📜",
    val suitabilityExplanation: String = "",
    val testScores: String? = null // To attach psychological/ethics quiz performance scores
)

object SovereignProofHandler {

    fun generateProofPrompt(userCredentialPrompt: String, countryName: String): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        return """
            You are compiling an official, sovereign clinical rehabilitation certificate, legal credentials, psychological competence evaluation, or accredited proof of professional excellence for the medical practitioner Dr. Tim.
            
            The country they practice in is: "$countryName".
            
            The user wants to generate proof or a competence evaluation based on this prompt or concept:
            "${userCredentialPrompt.ifBlank { "General Clinical Quality and Compliance Certificate" }}"
            
            TASK:
            1. Formulate a highly formal, authoritative, and realistic official document, psychological competence declaration, or license credential that makes sense for the user's concept.
            2. For example, if they specify an ethics course, make it a "Lobbying & Pharmacology Prescription Ethics Board Certification". If they specify billing, make it a "Public Health Tariff Audit Release Approval". If they specify doctor declaration of competence / psychiatric assessment, make it a "Board-Certified Medical Psyche & Clinical Competence Declaration" with specific section scores.
            3. Fill in the fields matching the JSON schema below. Use extremely formal sovereign and judicial terminology.
            4. Include a "testScores" field if relevant to provide quiz/competency evaluation score metrics out of 100.
            
            Return a JSON object with the following string fields:
            - "title": Formal name of the certificate or document (e.g., "SOVEREIGN DIPLOMA OF MEDICAL ETHICS & STATUTORY LAW COMPLIANCE")
            - "registrationNumber": A unique serial/legal registry ID (e.g., "REP-EL-4820-MED")
            - "issuer": The licensing body, court department, or ministry branch (e.g., "The Board of Statutory Medical Rehabilitation & Parliamentary Compliance")
            - "issueDate": "$currentDate" (or a suitable official format)
            - "verificationDetails": A professional paragraph explaining the certified criteria met (e.g., "Having satisfactorily completed 45 contact hours in General Practice Tariff Standards, Generic Pharmaceutical Substitutions, and Anti-Monopoly Patient Consent Guidelines, the practitioner is declared fully aligned with Sovereign Public Safety Directives.")
            - "sealEmoji": A symbolic emoji that represents this entity (e.g. "⚖️", "📜", "🎓", "🧠", "🏥", "🛡️")
            - "suitabilityExplanation": A brief explanation of why this document serves as strong defense evidence in professional or executive pardon hearings.
            - "testScores": (Optional) Clinical quiz scores or psychological test metrics (e.g., "Ethical Practice Index: 92/100 | Psychological Integrity Quotient: 96/100 | Medical Board Standing: Excellent")
            
            Ensure the JSON response is perfectly valid. Out of character, return ONLY the raw JSON object.
        """.trimIndent()
    }

    fun parseCertificateJson(jsonStr: String): LegalCertificate {
        return try {
            val json = JSONObject(jsonStr)
            LegalCertificate(
                id = UUID.randomUUID().toString(),
                title = json.optString("title", "Sovereign Certificate of Clinical Compliance"),
                registrationNumber = json.optString("registrationNumber", "REP-EL-9921-CERT"),
                issuer = json.optString("issuer", "Sovereign Medical Practitioner Audit Authority"),
                issueDate = json.optString("issueDate", "2026-06-20"),
                verificationDetails = json.optString("verificationDetails", "Successfully demonstrated clinical and statutory ethical capability."),
                sealEmoji = json.optString("sealEmoji", "📜"),
                suitabilityExplanation = json.optString("suitabilityExplanation", "A valuable general credential for reinstatement petitions."),
                testScores = json.optString("testScores", null)
            )
        } catch (e: Exception) {
            android.util.Log.e("SovereignProofHandler", "Failed to parse generated proof certificate JSON", e)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentDate = dateFormat.format(Date())
            LegalCertificate(
                id = UUID.randomUUID().toString(),
                title = "Certificate of Professional Conduct and Clinical Reeducation",
                registrationNumber = "REP-EL-${(1000..9999).random()}-MED",
                issuer = "Ministry of Health Advisory Panel",
                issueDate = currentDate,
                verificationDetails = "Demonstrated complete willingness to adhere to national generic substitution practices and statutory protocols.",
                sealEmoji = "🎓",
                suitabilityExplanation = "A standard retraining certificate often utilized for petitioning pardon.",
                testScores = "Ethical Compliance: 85/100 | General Clinical Directives: 90/100"
            )
        }
    }
}
