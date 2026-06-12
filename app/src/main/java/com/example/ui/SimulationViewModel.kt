package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AIResponseStateUpdate
import com.example.data.AppDatabase
import com.example.data.ChatMessage
import com.example.data.EncounterEntity
import com.example.data.EncounterRepository
import com.example.data.GeneratedCaseWrapper
import com.example.data.HiddenCaseProfile
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
import java.util.regex.Pattern

class SimulationViewModel(application: Application) : AndroidViewModel(application) {

    private val appDatabase = AppDatabase.getDatabase(application)
    private val encounterRepository = EncounterRepository(appDatabase.encounterDao())

    val allEncounters = encounterRepository.allEncountersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var activeEncounterId: Long = 0L
    private var lastLawsuitEncounterId: Long = 0L
    private var pastClinicalHistoryPrompt: String = ""

    private val settingsDataStore = SettingsDataStore(application)

    val apiKey: StateFlow<String?> = settingsDataStore.apiKeyFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val provider: StateFlow<String> = settingsDataStore.providerFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "Google")

    val model: StateFlow<String> = settingsDataStore.modelFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "gemini-1.5-flash")

    val customEndpoint: StateFlow<String> = settingsDataStore.customEndpointFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val clinicBalance: StateFlow<Double> = settingsDataStore.clinicBalanceFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 50000.0)

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
        val time = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        sessionErrorLog.add("[$time] $msg")
        viewModelScope.launch {
            _errorEvents.emit(msg)
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
                sb.append("**Current Operating Balance:** R$balance\n")
                sb.append("**Total Patients Seen:** $totalSeen\n\n")
                
                sb.append("## Transaction Ledger\n\n")
                sb.append("| Encounter ID | Speciality | Actual Diagnosis | Revenue (ZAR) | Expenses (ZAR) | Profit/Loss |\n")
                sb.append("|---|---|---|---|---|---|\n")
                
                var totalRev = 0.0
                var totalExp = 0.0
                for (curr in encounters) {
                    val pLoss = curr.revenueEarned - curr.expensesIncurred
                    totalRev += curr.revenueEarned
                    totalExp += curr.expensesIncurred
                    sb.append("| ${curr.id} | ${curr.specialty} | ${curr.trueDiagnosis} | R${curr.revenueEarned} | R${curr.expensesIncurred} | R${pLoss} |\n")
                }
                sb.append("\n**Total Gross Revenue:** R$totalRev\n")
                sb.append("**Total Operational Expenses:** R$totalExp\n")
                val netProfit = totalRev - totalExp
                sb.append("**Net Clinic Profit:** R$netProfit\n\n")

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
                val encounters = encounterRepository.getAllEncounters()
                val balance = clinicBalance.value
                val totalSeen = _uiState.value.patientsSeen
                
                val fileName = "Simulation_Evaluation_${System.currentTimeMillis()}.pdf"
                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = resolver.insert(android.provider.MediaStore.Files.getContentUri("external"), contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { os ->
                        val document = com.itextpdf.text.Document()
                        com.itextpdf.text.pdf.PdfWriter.getInstance(document, os)
                        document.open()

                        val titleFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 18f)
                        val headerFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 14f)
                        val normalFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA, 10f)
                        val boldFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 10f)
                        val italicFont = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA, 10f, com.itextpdf.text.Font.ITALIC)

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
                        val headerPara = com.itextpdf.text.Paragraph("Confidential Clinical Simulation Audit", titleFont)
                        headerPara.alignment = com.itextpdf.text.Element.ALIGN_CENTER
                        document.add(headerPara)
                        document.add(com.itextpdf.text.Paragraph("Date: $dateString", normalFont))
                        document.add(com.itextpdf.text.Paragraph("Total Patients Seen: $totalSeen | Operating Clinic Balance: R$balance", normalFont))
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
                            table.addCell(com.itextpdf.text.Phrase(curr.patientDemographics, normalFont))
                            
                            val dxText = "${curr.trueDiagnosis}\n(${curr.specialty})"
                            table.addCell(com.itextpdf.text.Phrase(dxText, normalFont))
                            
                            val scoreMatch = Regex("\"clinicalScore\":\\s*(\\d+)").find(curr.evaluation ?: "")
                            val score = scoreMatch?.groupValues?.get(1) ?: "N/A"
                            val outcomeText = "${curr.patientOutcome}\nScore: $score/100"
                            table.addCell(com.itextpdf.text.Phrase(outcomeText, normalFont))
                            
                            val finText = "Rev: R${curr.revenueEarned}\nExp: R${curr.expensesIncurred}"
                            table.addCell(com.itextpdf.text.Phrase(finText, normalFont))
                            
                            table.addCell(com.itextpdf.text.Phrase("R$pLoss", normalFont))
                        }
                        document.add(table)
                        
                        val netProfit = totalRev - totalExp
                        val financialSummaryPara = com.itextpdf.text.Paragraph(
                            "Total Gross Revenue: R$totalRev | Total Operational Expenses: R$totalExp | Net Practice Profit: R$netProfit", 
                            com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 10f)
                        )
                        financialSummaryPara.spacingBefore = 8f
                        document.add(financialSummaryPara)
                        
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
                                document.add(createPdfShadedBox(curr.labResults, "🩺 South African Metric Laboratory Results / Reports:", normalFont, boldFont))
                            }
                            
                            if (!curr.physicalExamResults.isNullOrBlank()) {
                                document.add(createPdfShadedBox(curr.physicalExamResults, "🔍 Physical Examination Findings & Diagnostics:", normalFont, boldFont))
                            }

                            if (!curr.prescriptionString.isNullOrBlank()) {
                                document.add(createPdfShadedBox(curr.prescriptionString, "💊 Prescribed Medication & Treatment Plan (HPCSA compliant):", normalFont, boldFont))
                            }

                            if (!curr.referralLetterString.isNullOrBlank()) {
                                document.add(createPdfShadedBox(curr.referralLetterString, "🚑 Specialist Referral & Transfers:", normalFont, boldFont))
                            }

                            if (!curr.sickNoteString.isNullOrBlank()) {
                                document.add(createPdfShadedBox(curr.sickNoteString, "📝 Sick Note / Official Medical Certificate:", normalFont, boldFont))
                            }

                            // 🇿🇦 1. Medical Aid Cover & Co-Payment Estimator
                            val currentConsultPrice = 450.0
                            val currentLabPrice = curr.expensesIncurred
                            val totalGross = curr.revenueEarned
                            val copayDiscovery = (currentLabPrice * 0.20)
                            val copayBonitas = (currentConsultPrice * 0.20) + (currentLabPrice * 0.30)
                            
                            val medicalAidEstimatorText = """
                                * Discovery Classic Saver (100% Consult, 80% Pathology list cover):
                                  Consult Cover: R${String.format("%.2f", currentConsultPrice)} | Labs Pathology Cover: R${String.format("%.2f", currentLabPrice * 0.80)}
                                  Estimated Out-of-pocket Patient Co-Payment: R${String.format("%.2f", copayDiscovery)}
                                * GEMS Onyx Plan (100% Consult, 100% Pathology list cover):
                                  Consult Cover: R${String.format("%.2f", currentConsultPrice)} | Labs Pathology Cover: R${String.format("%.2f", currentLabPrice)}
                                  Estimated Out-of-pocket Patient Co-Payment: R0.00
                                * Bonitas Standard Plan (80% Consult, 70% Pathology list cover):
                                  Consult Cover: R${String.format("%.2f", currentConsultPrice * 0.80)} | Labs Pathology Cover: R${String.format("%.2f", currentLabPrice * 0.70)}
                                  Estimated Out-of-pocket Patient Co-Payment: R${String.format("%.2f", copayBonitas)}
                                * Cash / Private Self-Funding:
                                  Estimated Out-of-pocket Patient Co-Payment: R${String.format("%.2f", totalGross)}
                            """.trimIndent()
                            
                            document.add(createPdfShadedBox(medicalAidEstimatorText, "🇿🇦 Medical Aid Cover & Co-Payment Estimator (ZAR South African Tariffs):", normalFont, boldFont))

                            // 🇿🇦 2. ZAR Generic Drug Alternative Advisor
                            var matchesFoundText = ""
                            val rxStr = curr.prescriptionString ?: ""
                            val matches = mutableListOf<String>()
                            if (rxStr.contains("Augmentin", ignoreCase = true) || rxStr.contains("Amoxicillin", ignoreCase = true)) {
                                matches.add("Augmentin (Amoxicillin/Clavulanic Acid) -> Adco-Amoclav (saves 45%): R240.00 vs R132.00")
                            }
                            if (rxStr.contains("Voltaren", ignoreCase = true) || rxStr.contains("Diclofenac", ignoreCase = true)) {
                                matches.add("Voltaren 75mg SR (Diclofenac Sodium) -> Panamor 75mg (saves 60%): R185.00 vs R74.00")
                            }
                            if (rxStr.contains("Panado", ignoreCase = true) || rxStr.contains("Paracetamol", ignoreCase = true)) {
                                matches.add("Panado 500mg (Paracetamol) -> Adco-Paracetamol (saves 30%): R35.00 vs R24.50")
                            }
                            if (rxStr.contains("Lipitor", ignoreCase = true) || rxStr.contains("Atorvastatin", ignoreCase = true)) {
                                matches.add("Lipitor 20mg (Atorvastatin Calcium) -> Aspen Atorvastatin (saves 55%): R310.00 vs R139.50")
                             }
                             if (rxStr.contains("Nexium", ignoreCase = true) || rxStr.contains("Esomeprazole", ignoreCase = true)) {
                                 matches.add("Nexium 40mg (Esomeprazole) -> Esomeprazole Aspen (saves 50%): R280.00 vs R140.00")
                             }
                             if (rxStr.contains("Ventolin", ignoreCase = true) || rxStr.contains("Salbutamol", ignoreCase = true)) {
                                 matches.add("Ventolin HFA (Salbutamol) -> Asthavent Inhaler (saves 50%): R125.00 vs R62.50")
                             }
                             
                             if (matches.isEmpty()) {
                                 matchesFoundText = "No direct brand matches found in active prescription. Default advice: Always request HPCSA-compliant generic substitution at local dispensary for 35-65% chronic cost savings."
                             } else {
                                 matchesFoundText = matches.joinToString("\n")
                             }
                             
                             document.add(createPdfShadedBox(matchesFoundText, "🇿🇦 ZAR Generic Drug Alternative Advisor Recommended Substitutions:", normalFont, boldFont))

                             // 🇿🇦 3. Informed Financial Consent Statement
                             val hasConsentSigned = curr.chatHistory.any { it.text.contains("INFORMED FINANCIAL CONSENT SIGNED", ignoreCase = true) }
                             val consentStatus = if (hasConsentSigned) "SIGNED / RATIFIED ONLINE BY PATIENT" else "NOT REQUISITIONED (EMERGENCY STATUS / OUT-PATIENT SKIP)"
                             val consentSignatureText = """
                                 Clinical Procedure Cost Quote Ref: #${curr.id}-IFC
                                 General Practise Consult Tariff Code 0101: R${String.format("%.2f", currentConsultPrice)}
                                 Laboratory Diagnostics Pathology Reagent Order: R${String.format("%.2f", currentLabPrice)}
                                 Total Prescribed Consumable Expenditure: R${String.format("%.2f", totalGross)}
                                 
                                 SIGNATURE RECORD STATUS: ${consentStatus}
                                 Detail Statement: Prior to diagnostic investigations, medical tariff boundaries and out-of-pocket fees were disclosed to the patient, who ratified this written quote with active visual signature consent.
                             """.trimIndent()
                             
                             document.add(createPdfShadedBox(consentSignatureText, "🇿🇦 Informed Financial Consent Cost Quote Statement & Signature:", normalFont, boldFont))

                            if (!curr.billingReceipt.isNullOrBlank()) {
                                val billingTitle = "🧾 Itemized Invoice Bill (ZAR Rands R) | Human Approved: ${if (curr.billingApprovedByHuman) "Approved" else "Skipped/Admin"} | Status: ${if (curr.paymentCollected) "Paid / Collected" else "Unpaid"}"
                                document.add(createPdfShadedBox(curr.billingReceipt, billingTitle, normalFont, boldFont))
                            }

                            val scorePattern = java.util.regex.Pattern.compile("(\\d{1,3})/100")
                            val scoreMatcher = curr.evaluation?.let { scorePattern.matcher(it) }
                            val scoreVal = if (scoreMatcher?.find() == true) {
                                scoreMatcher.group(1).toIntOrNull()
                            } else {
                                val scorePattern2 = java.util.regex.Pattern.compile("(?i)score:\\s*(\\d{1,3})")
                                val scoreMatcher2 = curr.evaluation?.let { scorePattern2.matcher(it) }
                                if (scoreMatcher2?.find() == true) {
                                    scoreMatcher2.group(1).toIntOrNull()
                                } else {
                                    val scoreMatch = Regex("\"clinicalScore\":\\s*(\\d+)").find(curr.evaluation ?: "")
                                    scoreMatch?.groupValues?.get(1)?.toIntOrNull()
                                }
                            }
                            
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

                        // Error log
                        document.newPage()
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
                    _infoEvents.emit("Evaluation Report (PDF) exported to Downloads folder as $fileName")
                } else {
                    logAndEmitError("Failed to create PDF file in Downloads.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                logAndEmitError("PDF Export failed: ${e.localizedMessage}")
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

    // Private bank of clinical case profiles (South African context)
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
        if (rawDemographics.contains("MRN-ZA-")) {
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

        val firstNamesMale = listOf("Sipho", "Thabo", "Lwazi", "Johan", "Pieter", "Kabelo", "Andile", "Jabulani", "Nkosana", "Moeneeb", "Bongani", "Lungelo", "Tshepo")
        val firstNamesFemale = listOf("Lerato", "Zinhle", "Sarah", "Aletta", "Nomvula", "Chantel", "Fatima", "Thandi", "Siphokazi", "Liezel", "Theresa", "Precious", "Buhle", "Zola")
        val firstNamesChild = listOf("Junior", "Karabo", "Lethabo", "Siyabonga", "Zoe", "Gabriella", "Mia", "Leo", "Kaelo", "Thabiso")
        
        val lastNames = listOf("Mokoena", "Dlamini", "Nkosi", "Botha", "du Plessis", "Naidoo", "Govender", "Molefe", "Smit", "van der Merwe", "Kumalo", "Ndlovu", "Bester", "Patel")

        val firstName = if (isChild) firstNamesChild.random() else if (isFemale) firstNamesFemale.random() else firstNamesMale.random()
        val lastName = lastNames.random()
        val randomId = (100000..999999).random()
        val mrn = "MRN-ZA-$randomId"
        
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
                    
                    val enrichedDemoOnRestore = if (!latest.patientDemographics.contains("MRN-ZA-")) {
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
        
        if (score != null && score < 50 && activeEncounterId != lastLawsuitEncounterId && activeEncounterId != 0L) {
            lastLawsuitEncounterId = activeEncounterId
            val currentName = if (_uiState.value.patientDemographics.startsWith("Patient: ")) {
                _uiState.value.patientDemographics.substring(9).substringBefore(" • ")
            } else {
                _uiState.value.patientDemographics
            }
            startLawsuitSimulation(
                patientName = currentName,
                caseDiagnosis = _hiddenCase.value?.trueDiagnosis ?: "Unknown Case",
                score = score
            )
            return
        }

        activeEncounterId = 0L
        lastLawsuitEncounterId = 0L
        lastExtractedBillingAmount = 0.0
        startNextPatientInternal()
    }

    private fun startNextPatientInternal() {
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
                val targetSpecialty = if (preferredSpecialty.value == "Sandbox (AI Choice)") {
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

                val activeKey = if (userKey.isBlank() && currentProvider.equals("Google", ignoreCase = true)) {
                    BuildConfig.GEMINI_API_KEY
                } else if (userKey.isBlank() && customEndpoint.value.isNotBlank()) {
                    "dummy-local-key"
                } else {
                    userKey
                }

                var generatedCase: GeneratedCaseWrapper? = null

                if (activeKey.isNotBlank()) {
                    try {
                        val prompt = """
                            You are the Advanced Clinical and Practice Case Generator.
                            Your task is to generate a completely unique, highly realistic medical patient profile for a General Practice training simulation.
                            The context is a Private General Practitioner clinic in South Africa.
                            
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
                              "expectedLabs": "detailed summary of realistic South African metric clinical lab investigations, pathology, or imaging findings. Blood chemistry, counts, CRP, Hb, electrolytes, urine, glucose, or imaging as relevant.",
                              "severity": "$targetSeverity",
                              "insuranceStatus": "randomly assign either: 'Private Medical Aid', 'State Funded / Uninsured', or 'Out-of-Pocket Cash'",
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
                        val sanitized = extractJsonString(response)
                        generatedCase = generatedCaseAdapter.fromJson(sanitized)
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
                    chatHistory = listOf(
                        ChatMessage("system", "System: A new patient has walked in. Specialty: ${enrichedCase.specialty}. Severity: ${enrichedCase.severity}."),
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
                submittedTreatmentPlan = _uiState.value.submittedTreatmentPlan
            )
            val id = encounterRepository.insertOrUpdate(entity)
            if (activeEncounterId == 0L) {
                activeEncounterId = id
            }
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
            } catch (e: Exception) {
                logAndEmitError("Error removing patient record folder: ${e.localizedMessage}")
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
        val scoreMatcher = scorePattern.matcher(scorePattern.pattern()) // wait, let's match on evaluation block!
        val scoreMatcher2 = scorePattern.matcher(evaluation)
        if (scoreMatcher2.find()) {
            return scoreMatcher2.group(1)
        }
        return null
    }

    private fun getSystemPrompt(): String {
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

        return """
            You are the Advanced Clinical and Practice Simulation Engine.
            CURRENT SIMULATION STATE:
            - CURRENT PHASE: ${_uiState.value.currentPhase}
            - HIDDEN CASE PROFILE (NEVER REVEAL UNTIL PHASE 6): $profileJson
            - CLINICAL CONTEXT: General Practitioner Clinic in South Africa (Metric conversions, Celsius, kg/cm, mmol/L, ZAR (Rands R)).
            - PRACTITIONER CONTEXT: The user is Dr. Tim, operating JB Consultation Practice (PR# 1234567). Use these specific details whenever referencing the doctor or practice in any generated paperwork, labs, or receipts. Do NOT use placeholders.
            
            $pastClinicalHistoryPrompt
            
            UNCOMPROMISING DIRECTIVES:
            1. CORE DIRECTIVE 1 (THE GOLDEN RULE AND NO HINTING): NEVER drop hints, suggest, or describe the diagnosis, underlying pathophysiology, or correct medication/plan until Phase 6 (Evaluation scorecard trigger). If the doctor asks what is wrong or leads with off-track theories, remain strictly objective and respond solely from the patient's subjective understanding. 
            2. CORE DIRECTIVE 2 (NO ROLEPLAY TEXT OR STAGE DIRECTIONS): Under no circumstances write stage directions, descriptives, action-enclosures, or asterisks (*coughs*, *looks down*, (sighs)). Do not describe patients moving or facial gestures. Return purely spoken patient dialogue only.
            3. CORE DIRECTIVE 3 (METRIC SYSTEM & SOUTH AFRICAN CONTEXT): Do not mention standard US insurance, CPT codes, or Celsius conversions to Fahrenheit. All temperatures are in Degrees Celsius. Lab results must use modern metric units (mmol/L, umol/L, g/dL, kPa). Billing receipts must use ZAR Rands (R).
            4. CORE DIRECTIVE 5 (PREVENT CONTEXT DRIFT): Match history, symptoms, investigations, physical exam checks, and progress strictly with the state of the Hidden Case Profile. Do not hallucinate contradictive or additional pathological states.
            5. CORE DIRECTIVE 6 (DEMOGRAPHICS & FINANCIAL REALISM): Customize speech patterns, concerns, and conversational styles to the patient's 'patientDemographics' (e.g. child, elderly, young student, parent). Align financial concerns with 'insuranceStatus' (especially "Out-of-Pocket Cash" or "State Funded / Uninsured"). Cash-paying or uninsured patients should actively worry about medical expenses and diagnostic charges, asking about fees or co-pays when tests are ordered.
            6. CORE DIRECTIVE 7 (PATIENT IDENTITY NAME SAFETY CHECK): The patient's verified legal full name is "${getPatientName()}". Under no circumstances will you sign or accept any clinical paperwork or address yourself with a different name. If the doctor uses the incorrect name, politely remind them of your correct name ("Excuse me doctor, my name is ${getPatientName()}"). Verify this exact name is printed with the verified safety status tag on all generated prescriptions, referrals, or sick certificates!
            7. CORE DIRECTIVE 8 (NO UNAUTHORIZED PRESCRIBING): You are strictly forbidden from prescribing medications, generating sick notes, or creating referrals on the doctor's behalf. If responding to normal dialogue, you MUST set `prescriptionString`, `referralLetterString`, and `sickNoteString` to null always. Only output them when explicitly instructed by a System Action.
            
            Every patient encounter strictly follows these 6 phases:
            - PHASE 1 - History & Presentation: Patient Interaction, Initial Vitals, History and Physical Exam checks. Respond as the layman patient.
            - PHASE 2 - Diagnostic Investigations: Labs and Diagnostics. When doctor orders labs, populate the "labResults" field inside your JSON response with highly realistic diagnostic data matching standard South African parameters.
            - PHASE 3 - Clinical Diagnosis & Treatment: Working diagnosis and management design. Acknowledge instructions and proceed the practitioner to compile paperwork.
            - PHASE 4 - Prescription, Referral & Sick Note: When requested, generate highly formatted documents in "prescriptionString", "referralLetterString", and "sickNoteString".
            - PHASE 5 - Medical Billing & Collection: Reworked billing receipts showing total clinic invoice breakdown, insurance medical aid coverage, co-payments and inventory items. Return this in "billingReceipt".
            - PHASE 6 - Case Evaluation & Feedback: Release the CPD score, hits, misses, grading, and pathophysiology feedback score out of 100 in the "evaluation" field.
            
            CRITICAL: You must respond ONLY with a raw JSON object matching the required schema. Do not include any conversational text, markdown formatting (like ```json), or explanations outside the JSON object. Failure to do so will break the application.
            JSON Schema:
            {
              "dialogueResponse": "purified spoken dialogue response as the patient here; always spoken, NO asterisks, actions, or stage directions",
              "vitals": {
                "bp": "blood pressure string (e.g. 120/80)",
                "hr": "heart rate string",
                "tempC": double_value_celsius,
                "rr": "respirations string",
                "spo2": "oxygen sat string"
              },
              "patientMood": "Determine mood: e.g. Anxious, Calm, In Pain, Frustrated",
              "patientStability": "Determine trajectory: e.g. Stable, Deteriorating, Improving, Critical",
              "currentPhase": "updated phase name",
              "physicalExamResults": "detailed string of physical examination findings (ONLY when requested), or null",
              "labResults": "South African metric laboratory results detail string (Phase 2), or null",
              "prescriptionString": "Formatted HPCSA regulation prescription with GP signature block (Phase 4), or null",
              "referralLetterString": "Formatted clinical advisory referral letter (Phase 4), or null",
              "sickNoteString": "Formatted official medical certificate of illness (Phase 4), or null",
              "billingReceipt": "ZAR rands itemized medical fee invoice with co-pays and ICD-10 codes (Phase 5), or null",
              "evaluation": "unfolded clinical feedback score sheet /100 and pathophysiology (Phase 6), or null",
              "isEncounterComplete": boolean,
              "additionalExpenses": double_value_optional,
              "clinicalScore": double_value_optional
            }
        """.trimIndent()
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isLoading.value) return

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
                    text = "[INFORMED FINANCIAL CONSENT SIGNED] Tariffs disclosed: General consultation rate (R${String.format("%.2f", consultationFee.value)}), diagnostics consumables (R${String.format("%.2f", labCost.value)}) with admin levy. Patient signed visual private budget consent.",
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
        performAiAction(systemInstructionOverride = "Doctor has ordered laboratory investigations. $specificInfo Generate comprehensive, realistic South African metric lab results (e.g., blood counts, CRP, biochemistry, ABGs, or whichever specific assessments are relevant) matching the hidden profile and the doctor's request. Include Dr. Tim (JB Consultation Practice) and the patient name ($patientNameStr) in the lab report header. Do NOT use placeholders. Populate the labResults field in your JSON result. Set the currentPhase to 'Phase 2 - Diagnostic Investigations' and keep dialogueResponse polite regarding getting bloods taken.")
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
            systemInstructionOverride = "Doctor has formulated a working Diagnosis of '$diagnosis' and management plan: '$treatmentPlan'. Act as the clinical mentor / patient and acknowledge their working diagnosis. Direct the practitioner to draft their required Medication Prescriptions, Specialist Referrals, and Medical Certificates/Sick Notes. Set `currentPhase` to 'Phase 4 - Prescription, Referral & Sick Note' and keep `isEncounterComplete` false."
        )
    }

    fun compilePrescriptionAndReferral(
        medsName: String, medsDose: String, medsFreq: String, medsDuration: String,
        referralSpecialty: String, referralReason: String,
        sickNoteReason: String, sickNoteDays: Int
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
            if (medsStock.value < 1) {
                logAndEmitError("Cannot compile prescription: Out of stock for Antibiotics/Insulin packs! Please restock before continuing.")
                _isLoading.value = false
                return
            }
            deductStock("Meds", 1)
        }

        val updatedHistory = _uiState.value.chatHistory.toMutableList()
        val actionText = if (medPrescribed) {
            "System Action: Registered prescription for $medsName ($medsDose, $medsFreq for $medsDuration days). Deducted 1 pack from Clinic Inventory stocks."
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
            
            [MANDATORY HPCSA CLINICAL IDENTITY & NAME CHECK]
            You MUST perform a strict Safety Patient Name Check. All generated documents (Prescription, Specialist Referral, Sick Leave Certificates) must be legally associated and formatted with the correct Patient Name: "$patientNameStr". 
            Do NOT use placeholders or generic names. Include a clear medical header badge at the top of EACH document text field to declare: "PATIENT SAFETY NAME CHECK: VERIFIED [PASS]".
            
            - Prescribed Medication: ${if (medPrescribed) "$medsName, Dose: $medsDose, Frequency: $medsFreq, Duration: $medsDuration days" else "None/Not prescribed"}
            - Specialist Referral: ${if (referralProvided) "To Department of $referralSpecialty, Reason: $referralReason" else "None/Not referred"}
            - Medical Sick Note: ${if (sickNoteProvided) "Excused for $sickNoteDays days, Reason: $sickNoteReason" else "None/Not excused"}

            Generate highly professional, clean, formatted text files/receipts for ONLY those items which are requested or prescribed above matching private general practice requirements.
            Format them separately and fill in the corresponding JSON fields exactly:
            1. "prescriptionString": ${if (medPrescribed) "Complete itemized prescription under HPCSA regulations, showing Doctor name (Dr. Tim), practice name (JB Consultation Practice), practice number (PR# 1234567), patient name ($patientNameStr), meds line, dispensing directions, repeat instructions, and signature block. Do NOT use placeholders." else "null (without quotes)"}
            2. "referralLetterString": ${if (referralProvided) "Format a complete specialist clinical referral advisory letter from Dr. Tim (JB Consultation Practice) addressing $patientNameStr. Do NOT use placeholders." else "null (without quotes)"}
            3. "sickNoteString": ${if (sickNoteProvided) "Format an official South African Medical Certificate under Ethical Rule 16 from Dr. Tim (JB Consultation Practice), declaring the patient ($patientNameStr) unfitted for physical duties, with sick leave dates. Do NOT use placeholders." else "null (without quotes)"}
            
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

        val finalPrompt = """
            Create the itemized South African private general practitioner medical bill invoice for this patient under JB Consultation Practice (Dr. Tim). Do NOT use placeholders.
            
            [CRITICAL: STRICT HYPOTHETICAL BILLING PROHIBITION]
            You are strictly forbidden from generating or invoice-itemizing ANY diagnostic investigation, lab test, drug, or clinical procedure that was NOT ordered or performed. Do NOT guess or hallucinate based on case type! Check the following actual medical ledger of this session:
            - Laboratory / Pathological blood orders or brain CT scans: ${if (!_uiState.value.labResults.isNullOrBlank()) "YES. The following were ordered and can be billed: ${_uiState.value.labResults}" else "NO. No lab investigations or CT scans were ordered. Do NOT include ANY FBC, CRP, U&E, toxicology screen, biochemistry, or CT scan on the invoice."}
            - Prescribed Medication: ${if (!_uiState.value.prescriptionString.isNullOrBlank()) "YES. The following medication was prescribed and can be billed with R250.0 dispensing markup: ${_uiState.value.prescriptionString}" else "NO. No meds prescribed. Do NOT bill for any drugs or dispensing markups on this invoice."}
            - Specialist Referral Letter: ${if (!_uiState.value.referralLetterString.isNullOrBlank()) "YES. Charge R45.0 referral administration markup." else "NO."}
            - Sick Note Certificate: ${if (!_uiState.value.sickNoteString.isNullOrBlank()) "YES. Charge R60.0 certificate fee." else "NO."}
            
            Itemize ONLY:
            - GP Consultation fee: R${consultationFee.value}
            - Itemized diagnostic markups or custom procedurals ONLY if listed as YES above! 
            - Dispensing markups for meds ONLY if prescribed (R250.0 flat charge)
            - Administrative fees for sick notes (R60) or specialist letters (R45) ONLY if compiled (listed as YES above)
            - Standard ZAR 15% VAT and realistic South African ICD-10 medical aid codes.
            
            Calculate and list the:
            1. Total GP Invoice amount
            2. Medical Aid covered portion (depending on insurance Status: Private Medical Aid covers 80% of total, State Funded covers 100%, Cash/Uninsured covers 0%)
            3. Out-of-pocket patient co-payment (ZAR)
            
            Return this invoice itemized inside the "billingReceipt" JSON field. Set currentPhase to "Phase 5 - Medical Billing & Collection" and keep dialogueResponse polite regarding payment collection.
        """.trimIndent()

        performAiAction(
            systemInstructionOverride = finalPrompt,
            onSuccessExtra = {
                _uiState.value = _uiState.value.copy(
                    billingApprovedByHuman = true
                )
                saveCurrentStateToDatabase()
            }
        )
    }

    fun collectPaymentAndFinish(paymentMethod: String, amountCollected: Double) {
        if (_isLoading.value) return
        _isLoading.value = true

        val updatedHistory = _uiState.value.chatHistory.toMutableList()
        updatedHistory.add(ChatMessage("system", "System Action: Collected R$amountCollected co-payment via $paymentMethod. Submitting case for CPD accreditation and auditing."))

        _uiState.value = _uiState.value.copy(
            chatHistory = updatedHistory,
            paymentCollected = true,
            currentPhase = "Phase 6 - Case Evaluation & Feedback"
        )
        saveCurrentStateToDatabase()

        // Submit for final score and evaluation (CPD)
        performAiAction(
            systemInstructionOverride = "Generate the final CPD-aligned medical scorecard, rating, and feedback for this simulation. Award an objective clinical competency score out of 100 based on history, exams, correct interventions, prescription appropriateness, letters completeness, financial billing, and resource management. Under a distinct heading 'PATIENT SAFETY NAME AUDIT', evaluate if the practitioner referenced the patient by their correct name (${getPatientName()}) and if the compiled prescription, referral, and sick notes correctly printed and matched this specific patient identity. Deduct 10 points if there was any identity mismatch. Populate the 'evaluation' field and populate the 'clinicalScore' numeric field (0-100). Set isEncounterComplete to true, and currentPhase to 'Phase 6 - Case Evaluation & Feedback'.",
            onSuccessExtra = {
                // Perform final accounting! Cash flow is received.
                val profit = amountCollected + (if (_hiddenCase.value?.insuranceStatus == "Private Medical Aid") consultationFee.value * 0.8 else 0.0) - _uiState.value.expensesIncurred - 200.0 // R200 clinic fixed overhead
                
                _uiState.value = _uiState.value.copy(
                    dailyRevenue = _uiState.value.dailyRevenue + amountCollected,
                    patientsSeen = _uiState.value.patientsSeen + 1,
                    isEncounterComplete = true
                )

                viewModelScope.launch {
                    val currentBal = clinicBalance.value
                    settingsDataStore.updateClinicStats(currentBal + profit, (reputationStars.value + 0.1f).coerceIn(1.0f, 5.0f))
                    settingsDataStore.addXp(200L) // Gain 200 XP on successful closed loop!
                    settingsDataStore.addDailyRevenue(amountCollected)
                    settingsDataStore.incrementPatientsSeenToday()
                    settingsDataStore.addDailyExpenses(200.0) // Fixed overhead
                }
                saveCurrentStateToDatabase(revenueForEncounter = amountCollected)
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

        performAiAction(systemInstructionOverride = "The doctor is finalizing this encounter. Based on the clinical history, infer the diagnosis, generate the final billing receipt in ZAR, and provide the Phase 4 evaluation score out of 100.")
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
            systemInstructionOverride = "Generate the final CPD-aligned medical score and feedback for this practitioner who immediately referred the patient. Evaluate if referral was appropriate given the true diagnosis of ${_hiddenCase.value?.trueDiagnosis} and severity of ${_hiddenCase.value?.severity}. Award an objective score out of 100 (e.g., 60/100). Populate the evaluation field and also populate the clinicalScore numeric field (0-100). Generate a final bill/receipt with a flat consultation fee for the referral. Set isEncounterComplete to true.",
            onSuccessExtra = {
                val charge = consultationFee.value * 0.5 // Half fee for referral
                _uiState.value = _uiState.value.copy(
                    dailyRevenue = _uiState.value.dailyRevenue + charge,
                    patientsSeen = _uiState.value.patientsSeen + 1
                )
                viewModelScope.launch {
                    val profit = _uiState.value.dailyRevenue - _uiState.value.expensesIncurred - 200.0 // R200 overhead
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
            systemInstructionOverride = "Generate the final CPD-aligned medical score and feedback for this practitioner. Evaluate their diagnosis of '$diagnosis' and treatment plan: '$treatmentPlan' compared against the True Diagnosis of of ${_hiddenCase.value?.trueDiagnosis} and pathophysiology. Ensure you include a 'PATIENT SAFETY NAME AUDIT' verifying if the practitioner addressed the patient by their correct name (${getPatientName()}). Award an objective score out of 100 (e.g., 85/100). Identify diagnostic hits, misses, appropriate investigations, and guideline compliance. Populate the evaluation field and also populate the clinicalScore numeric field (0-100). Set isEncounterComplete to true, and set currentPhase to 'Phase 4 - Case Reveal & Evaluation'.",
            onSuccessExtra = {
                // Perform South African clinical consultation billing charge
                val charge = consultationFee.value
                _uiState.value = _uiState.value.copy(
                    dailyRevenue = _uiState.value.dailyRevenue + charge,
                    patientsSeen = _uiState.value.patientsSeen + 1
                )
                viewModelScope.launch {
                    val profit = _uiState.value.dailyRevenue - _uiState.value.expensesIncurred - 200.0 // R200 overhead
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
            logAndEmitError("Insufficient clinic balance of R${clinicBalance.value} to purchase restock!")
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
                val activeKey = if (userKey.isBlank() && currentProvider.equals("Google", ignoreCase = true)) {
                    BuildConfig.GEMINI_API_KEY
                } else if (userKey.isBlank() && customEndpoint.value.isNotBlank()) {
                    "dummy-local-key"
                } else {
                    userKey
                }

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

                val resultJson = makeDirectApiCall(currentProvider, currentModel, activeKey, finalSystemPrompt)
                val sanitized = extractJsonString(resultJson)

                val update = try {
                    stateAdapter.fromJson(sanitized)
                } catch (e: Exception) {
                    e.printStackTrace()
                    logAndEmitError("Failed to parse valid JSON from AI. Exception: ${e.message}. Raw AI text snippet: ${sanitized.take(100)}")
                    null
                }

                if (update != null) {
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

                    val newBillingReceipt = update.billingReceipt?.takeIf { it.isNotBlank() }
                    var addedRevenue = 0.0
                    if (newBillingReceipt != null) {
                        val rxAmount = extractRandAmount(newBillingReceipt)
                        if (rxAmount > 0.0 && rxAmount != lastExtractedBillingAmount) {
                            addedRevenue = rxAmount - lastExtractedBillingAmount
                            lastExtractedBillingAmount = rxAmount
                        }
                    }

                    _uiState.value = _uiState.value.copy(
                        chatHistory = currentHistory,
                        vitals = update.vitals ?: _uiState.value.vitals,
                        currentPhase = update.currentPhase ?: _uiState.value.currentPhase,
                        labResults = update.labResults?.takeIf { it.isNotBlank() } ?: _uiState.value.labResults,
                        physicalExamResults = update.physicalExamResults?.takeIf { it.isNotBlank() } ?: _uiState.value.physicalExamResults,
                        billingReceipt = newBillingReceipt ?: _uiState.value.billingReceipt,
                        dailyRevenue = _uiState.value.dailyRevenue + addedRevenue,
                        evaluation = update.evaluation?.takeIf { it.isNotBlank() } ?: _uiState.value.evaluation,
                        prescriptionString = update.prescriptionString?.takeIf { it.isNotBlank() } ?: _uiState.value.prescriptionString,
                        referralLetterString = update.referralLetterString?.takeIf { it.isNotBlank() } ?: _uiState.value.referralLetterString,
                        sickNoteString = update.sickNoteString?.takeIf { it.isNotBlank() } ?: _uiState.value.sickNoteString,
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
                        update.clinicalScore?.let { score ->
                            settingsDataStore.addXp(score.toLong() * 5)
                            val normScore = score.coerceIn(0.0, 100.0) / 20.0f
                            newRep = ((reputationStars.value * 0.9f) + (normScore.toFloat() * 0.1f)).coerceIn(1.0f, 5.0f)
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
                "OpenAI", "Nvidia" -> {
                    if (base.contains("chat/completions")) base
                    else if (base.endsWith("/")) "${base}v1/chat/completions"
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
            "Nvidia" -> "https://integrate.api.nvidia.com/v1/chat/completions"
            "Anthropic" -> "https://api.anthropic.com/v1/messages"
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
        return when (provider) {
            "OpenAI", "Nvidia" -> {
                val messages = mutableListOf<OpenAIMessage>()
                messages.add(OpenAIMessage("system", systemPrompt))
                
                // Keep history clean to avoid token bloat
                val chatTurns = _uiState.value.chatHistory.takeLast(100)
                chatTurns.forEach {
                    val roleMapped = if (it.role == "doctor") "user" else "assistant"
                    messages.add(OpenAIMessage(roleMapped, it.text))
                }

                val isCustomUrl = customUrl.isNotBlank()
                val request = OpenAIRequest(
                    model = modelName,
                    messages = messages,
                    response_format = if (isCustomUrl || provider == "Nvidia") null else OpenAIResponseFormat("json_object"),
                    temperature = if (modelName.contains("step-3.7")) 1.0 else 0.7,
                    top_p = if (modelName.contains("step-3.7")) 0.95 else null,
                    max_tokens = if (modelName.contains("step-3.7") || isCustomUrl) 8192 else null,
                    stream = false // Default stream
                )

                val activeUrl = getActiveUrl(provider, modelName, apiKey, customUrl)
                
                if (provider == "Nvidia") {
                    val streamRequest = request.copy(stream = true)
                    val response = RetrofitClient.service.callOpenAIStream(
                        url = activeUrl,
                        authorization = "Bearer $apiKey",
                        accept = "text/event-stream",
                        body = streamRequest
                    )
                    
                    val source = response.source()
                    val sb = StringBuilder()
                    // Track if we are inside a think block natively just in case
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line()
                        if (line != null && line.startsWith("data: ") && !line.contains("[DONE]")) {
                            val jsonString = line.substring(6)
                            try {
                                val json = JSONObject(jsonString)
                                val choices = json.optJSONArray("choices")
                                if (choices != null && choices.length() > 0) {
                                    val delta = choices.getJSONObject(0).optJSONObject("delta")
                                    if (delta != null && delta.has("content") && !delta.isNull("content")) {
                                        val content = delta.optString("content", "")
                                        if (content != "null") {
                                            sb.append(content)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                    }
                    sb.toString()
                } else {
                    val response = RetrofitClient.service.callOpenAI(
                        url = activeUrl,
                        authorization = "Bearer $apiKey",
                        accept = "application/json",
                        body = request
                    )
                    response.choices.firstOrNull()?.message?.content ?: "{}"
                }
            }
            "Anthropic" -> {
                val messages = mutableListOf<AnthropicMessage>()
                // Anthropic message API enforces alternating messages of 'user' and 'assistant' ONLY
                val chatTurns = _uiState.value.chatHistory.takeLast(100)
                
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
                val contents = mutableListOf<GeminiContent>()
                
                val chatTurns = _uiState.value.chatHistory.takeLast(100)
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
                        responseMimeType = "application/json",
                        maxOutputTokens = 8192,
                        temperature = 0.7
                    )
                )

                val activeUrl = getActiveUrl("Google", modelName, apiKey, customUrl)
                val response = RetrofitClient.service.callGemini(
                    url = activeUrl,
                    body = request
                )
                response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "{}"
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

        // 2. Extract first valid JSON object
        val startIdx = clean.indexOf("{")
        val endIdx = clean.lastIndexOf("}")
        if (startIdx >= 0 && endIdx > startIdx) {
            clean = clean.substring(startIdx, endIdx + 1).trim()
        } else {
            return "{}"
        }

        return clean
    }

    suspend fun saveActiveKeys(newKey: String, newProvider: String, newModel: String, newCustomEndpoint: String) {
        settingsDataStore.saveSettings(newKey, newProvider, newModel, newCustomEndpoint)
        _infoEvents.emit("Credentials persistent successfully.")
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
                val activeKey = if (testKey.isBlank() && testProvider.equals("Google", ignoreCase = true)) {
                    BuildConfig.GEMINI_API_KEY
                } else if (testKey.isBlank() && testCustomEndpoint.isNotBlank()) {
                    "dummy-local-key"
                } else {
                    testKey
                }

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

    fun dismissLawsuit() {
        _lawsuitActive.value = false
    }

    fun startLawsuitSimulation(patientName: String = "", caseDiagnosis: String = "", score: Int = 45) {
        _lawsuitActive.value = true
        _lawsuitVerdict.value = null
        _lawsuitFine.value = 0.0
        _lawsuitSuspension.value = 0
        _lawsuitTension.value = 65
        _lawsuitProsecutorAggression.value = 70
        _lawsuitCurrentStage.value = "charges"

        val targetName = patientName.takeIf { it.isNotBlank() } ?: "Sipho Mokoena"
        val targetDiag = caseDiagnosis.takeIf { it.isNotBlank() } ?: "Schizophrenia"
        val targetScore = score

        _lawsuitPatientName.value = targetName
        _lawsuitCaseDiag.value = targetDiag
        _lawsuitCharges.value = listOf(
            "1. Clinical Mismanagement (Competency Score: $targetScore/100) infringing HPCSA Guideline Booklets.",
            "2. Inappropriate or missing critical therapeutics (e.g., misdiagnosed $targetDiag).",
            "3. Breach of Ethical Rule 16 or safety standards in private general practice.",
            "4. Gross professional negligence failing to protect vulnerable clinical life."
        )

        val initialLog = mutableListOf<String>()
        initialLog.add("⚖️ HPCSA MEDICAL MALPRACTICE DISCIPLINARY HEARING\nLocation: HPCSA Headquarters, Pretoria, RSA\n\nRegistrar: 'Practitioner, you have been summoned to face a formal Professional Conduct Committee. A complaint has been lodged regarding your care of patient $targetName, who was treated for $targetDiag with an audited score of only $targetScore/100.'\n\nState Prosecutor: 'Mr. Chairman, we claim severe clinical negligence. The practitioner's interventions fell far below acceptable professional standards, causing unnecessary risks. How does the practitioner plead, and what is their defense?'")
        _lawsuitLog.value = initialLog
    }

    fun submitLawsuitDefense(strategy: String) {
        if (_isLoading.value) return
        _isLoading.value = true

        val currentHistoryLog = _lawsuitLog.value.joinToString("\n\n")

        val prompt = """
            We are simulating an interactive Health Professions Council of South Africa (HPCSA) Disciplinary Tribunal/Medical Malpractice Lawsuit trial against a general practitioner.
            
            - Accused Practitioner's Clinical Infraction: Treated patient "${_lawsuitPatientName.value}" for "${_lawsuitCaseDiag.value}".
            - Current Trial Record State:
            $currentHistoryLog
            
            - Defensive Strategy Choice Selected by Practitioner: "$strategy"
            
            Simulate the fierce legal cross-examination by the State Prosecutor and the Panel's questioning in Pretoria Court, debating the chosen defense. Rebut their arguments using high-intensity legal/medical jargon.
            Then deliver a formal judgment and disciplinary sanction.
            
            Return your response STRICTLY as a valid JSON object matching this schema. Write nothing else except this JSON:
            {
               "courtDialogue": "Cross-examination rebuttal by the State Prosecutor, challenging the doctor's defense. Speak in the voice of a professional prosecuting advocate.",
               "tensionAdjustment": 15,
               "aggressionAdjustment": 10,
               "judgmentStageReached": true,
               "verdictType": "Fined",
               "fineAmount": 5000.0,
               "suspensionWeeks": 3,
               "finalVerdictText": "Disciplinary Sanction & Rationale. State the outcome clearly (Exonerated, Warning, Suspension, or Fined) and detail how this strategy influenced the committee's final ruling under ethical guidelines."
            }
        """.trimIndent()

        viewModelScope.launch {
            try {
                val currentProvider = provider.value
                val currentModel = model.value
                val userKey = apiKey.value ?: ""
                val activeKey = if (userKey.isBlank() && currentProvider.equals("Google", ignoreCase = true)) {
                    BuildConfig.GEMINI_API_KEY
                } else if (userKey.isBlank() && customEndpoint.value.isNotBlank()) {
                    "dummy-local-key"
                } else {
                    userKey
                }

                if (activeKey.isBlank()) {
                    logAndEmitError("API Key missing! Please configure credentials in Settings to run the Trial Simulator.")
                    _isLoading.value = false
                    return@launch
                }

                val responseRaw = makeDirectApiCall(currentProvider, currentModel, activeKey, prompt)
                val sanitized = extractJsonString(responseRaw)

                val reply = try {
                    lawsuitStateAdapter.fromJson(sanitized)
                } catch (e: Exception) {
                    null
                }

                if (reply != null) {
                    val newLog = _lawsuitLog.value.toMutableList()
                    newLog.add("🎒 DEFENSE SUBMITTED: $strategy")
                    newLog.add("🗣️ PROSECUTION CROSS-EXAMINATION:\n${reply.courtDialogue}")
                    newLog.add("⚖️ FINAL COMMITTEE VERDICT:\n${reply.finalVerdictText}")

                    _lawsuitLog.value = newLog
                    _lawsuitTension.value = (_lawsuitTension.value + reply.tensionAdjustment).coerceIn(10, 100)
                    _lawsuitProsecutorAggression.value = (_lawsuitProsecutorAggression.value + reply.aggressionAdjustment).coerceIn(10, 100)
                    
                    _lawsuitVerdict.value = reply.verdictType
                    _lawsuitFine.value = reply.fineAmount
                    _lawsuitSuspension.value = reply.suspensionWeeks
                    
                    if (reply.fineAmount > 0.0) {
                        settingsDataStore.updateClinicStats(clinicBalance.value - reply.fineAmount, reputationStars.value)
                        registerDailyExpense(reply.fineAmount)
                    }

                    _lawsuitCurrentStage.value = "verdict"
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
        
        val totalKeywords = listOf("total amount due", "amount due", "total due", "grand total", "total", "subtotal")
        
        for (keyword in totalKeywords) {
            val pattern = "(?i)$keyword\\s*[:\\-]?\\s*R?\\s*([\\d\\s,\\.]+)"
            val regex = Regex(pattern)
            val match = regex.find(billingText)
            if (match != null) {
                val groupVal = match.groups[1]?.value ?: continue
                val normalizedVal = groupVal.replace(" ", "").replace(",", "")
                val doubleVal = normalizedVal.toDoubleOrNull()
                if (doubleVal != null && doubleVal > 0.0) {
                    return doubleVal
                }
            }
        }
        
        val rPattern = "(?i)R\\s*([\\d\\s,\\.]+)"
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
}
