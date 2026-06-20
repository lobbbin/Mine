package com.example.ui

import com.example.data.HiddenCaseProfile
import com.example.data.IntakeFormData
import com.example.data.SuggestedPaperwork
import com.example.data.SuggestedPrescriptionItem
import com.example.data.ChatMessage
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class ApiDetails(
    val provider: String,
    val model: String,
    val apiKey: String,
    val customEndpoint: String,
    val rotatorKeys: Map<String, String>,
    val rotatorEnabledModels: Set<String>
)

object ClinicalSimHandler {

    fun generateIntakeFormDataFallback(
        activeCase: HiddenCaseProfile?
    ): IntakeFormData {
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
        return fallbackData
    }

    suspend fun executeGenerateIntakeFormData(
        customNote: String?,
        activeCase: HiddenCaseProfile?,
        activeSchemesList: List<String>,
        apiDetails: ApiDetails,
        gameAgent: GameAgent
    ): IntakeFormData {
        val fallbackData = generateIntakeFormDataFallback(activeCase)

        try {
            val schemesListStr = activeSchemesList.joinToString { "'$it'" }

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
                Therefore, the "medicalAid" field in your JSON output MUST be EXACTLY: "$matchedSchemeName" (or chosen from the active registry: $_schemaListFormat).
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
                provider = apiDetails.provider,
                modelName = apiDetails.model,
                apiKey = apiDetails.apiKey,
                systemPrompt = "",
                chatHistory = listOf(ChatMessage("doctor", prompt)),
                customUrl = apiDetails.customEndpoint,
                rotatorKeys = apiDetails.rotatorKeys,
                rotatorEnabledModels = apiDetails.rotatorEnabledModels
            )

            val cleanedJson = responseJson.replace("```json", "").replace("```", "").trim()
            val adapter = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
                .adapter(IntakeFormData::class.java)
            val intakeData = adapter.fromJson(cleanedJson)

            if (intakeData != null) {
                return IntakeFormData(
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
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return fallbackData
    }

    private val _schemaListFormat = "'Elysium Elite Private', 'CarePlus Basic', 'National Health Service (NHS)', 'Out-of-Pocket Cash'"

    fun generateSuggestedPaperworkFallback(
        activeCase: HiddenCaseProfile?
    ): SuggestedPaperwork {
        val fallbackData = if (activeCase != null) {
            val diag = activeCase.trueDiagnosis
            val isSevere = activeCase.severity.contains("Severe", ignoreCase = true)
            val isAsthma = diag.contains("Asthma", ignoreCase = true)
            val defaultMeds = if (isAsthma) {
                listOf(
                    SuggestedPrescriptionItem("Asthavent Inhaler (Salbutamol)", "2 puffs", "As needed", "30"),
                    SuggestedPrescriptionItem("Beclomethasone Inhaler", "1 puff", "12-hourly", "30")
                )
            } else if (diag.contains("tonsillitis", ignoreCase = true) || diag.contains("pharyngitis", ignoreCase = true)) {
                listOf(
                    SuggestedPrescriptionItem("Amoxicillin 500mg", "1 capsule", "8-hourly", "5"),
                    SuggestedPrescriptionItem("Paracetamol 500mg", "2 tablets", "6-hourly", "5")
                )
            } else {
                listOf(
                    SuggestedPrescriptionItem("Paracetamol 500mg", "2 tablets", "6-hourly", "5")
                )
            }

            SuggestedPaperwork(
                diagnosis = diag,
                treatmentPlan = "Prescribed appropriate therapy. Supportive care, hydration, and rest.",
                meds = defaultMeds,
                referralSpecialty = if (isSevere) "Internal Medicine Specialist" else "",
                referralReason = if (isSevere) "Urgent escalation and specialized diagnostic evaluation due to severe presentation." else "",
                sickNoteReason = diag,
                sickNoteDays = if (isSevere) 3 else 1
            )
        } else {
            SuggestedPaperwork()
        }
        return fallbackData
    }

    suspend fun executeGenerateSuggestedPaperwork(
        activeCase: HiddenCaseProfile?,
        apiDetails: ApiDetails,
        gameAgent: GameAgent
    ): SuggestedPaperwork {
        val fallbackData = generateSuggestedPaperworkFallback(activeCase)

        try {
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
                    provider = apiDetails.provider,
                    modelName = apiDetails.model,
                    apiKey = apiDetails.apiKey,
                    systemPrompt = "",
                    chatHistory = listOf(ChatMessage("doctor", prompt)),
                    customUrl = apiDetails.customEndpoint,
                    rotatorKeys = apiDetails.rotatorKeys,
                    rotatorEnabledModels = apiDetails.rotatorEnabledModels
                )
                val cleanedJson = responseJson.replace("```json", "").replace("```", "").trim()

                val moshi = Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()
                val adapter = moshi.adapter(SuggestedPaperwork::class.java)
                val suggested = adapter.fromJson(cleanedJson)

                if (suggested != null) {
                    return suggested
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return fallbackData
    }
}
