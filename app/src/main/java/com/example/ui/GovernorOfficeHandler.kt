package com.example.ui

import android.app.Application
import android.util.Log
import com.example.data.SettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class GovernorOfficeHandler(
    private val parentHandler: PoliticsHandler,
    private val parentViewModel: SimulationViewModel,
    private val application: Application,
    private val settingsDataStore: SettingsDataStore
) {
    private val _medicaidCoverageTier = MutableStateFlow<Int>(0) // Tier 0 to 3
    val medicaidCoverageTier: StateFlow<Int> = _medicaidCoverageTier.asStateFlow()

    private val _isQuarantineActive = MutableStateFlow<Boolean>(false)
    val isQuarantineActive: StateFlow<Boolean> = _isQuarantineActive.asStateFlow()

    private val _stateIncomeTaxRate = MutableStateFlow<Double>(3.5) // percent
    val stateIncomeTaxRate: StateFlow<Double> = _stateIncomeTaxRate.asStateFlow()

    private val _chemistsDeregulated = MutableStateFlow<Boolean>(false)
    val chemistsDeregulated: StateFlow<Boolean> = _chemistsDeregulated.asStateFlow()

    fun updateQuarantine(active: Boolean) {
        _isQuarantineActive.value = active
    }

    suspend fun executeDecree(powerId: String): DecisionOutcome = withContext(Dispatchers.IO) {
        val sysPrompt = when (powerId) {
            "gov_medicaid" -> """
                You are an Elysium Health Secretary. The Governor enacted 'Medicaid State Support'.
                Write a 2-paragraph news editorial.
                Determine changes in STRICT valid JSON:
                - newsArticle: "Editorial content..."
                - approvalChange: 10
                - fundsChange: -4000.0
                - clinicBalanceChange: 20000.0
                - workingClassDelta: 15
                - medicalGuildDelta: 10
                - corporateExecutiveDelta: -6
                - nationalPatriotsDelta: -2
                - clinicStockChange: {}
            """.trimIndent()

            "gov_quarantine" -> """
                You are an Epidemic Response Coordinator. The Governor signed 'Quarantine & Sanitation Barriers'.
                Write news editorial.
                Determine changes in STRICT valid JSON:
                - newsArticle: "Editorial content..."
                - approvalChange: -3
                - fundsChange: 0.0
                - clinicBalanceChange: 0.0
                - workingClassDelta: -10
                - medicalGuildDelta: 15
                - corporateExecutiveDelta: -8
                - nationalPatriotsDelta: 18
                - clinicStockChange: { "reagents": 12, "meds": 5 }
            """.trimIndent()

            "gov_deregulate_chemists" -> """
                The Governor signed 'Healthcare Chemist Licensing Deregulations' allowing pragmatic medicine synthesizers.
                Write a 2-paragraph news editorial.
                Determine changes in STRICT valid JSON:
                - newsArticle: "Editorial content..."
                - approvalChange: 6
                - fundsChange: -3000.0
                - clinicBalanceChange: 12000.0
                - workingClassDelta: 12
                - medicalGuildDelta: -8
                - corporateExecutiveDelta: 14
                - nationalPatriotsDelta: -5
                - clinicStockChange: { "saline": 10, "adrenaline": 5, "meds": 20 }
            """.trimIndent()

            "gov_unions_benefit" -> """
                The Governor decreed 'Working-class Clinic Insurance Subsidies'.
                Write a 2-paragraph news report.
                Determine changes in STRICT valid JSON:
                - newsArticle: "Editorial report..."
                - approvalChange: 12
                - fundsChange: -5000.0
                - clinicBalanceChange: 18000.0
                - workingClassDelta: 20
                - medicalGuildDelta: 8
                - corporateExecutiveDelta: -12
                - nationalPatriotsDelta: -4
                - clinicStockChange: {}
            """.trimIndent()

            else -> """
                State decree implemented.
                Determine changes in STRICT valid JSON:
                - newsArticle: "State directive executed."
                - approvalChange: 3
                - fundsChange: 0.0
                - clinicBalanceChange: 0.0
                - workingClassDelta: 3
                - medicalGuildDelta: 3
                - corporateExecutiveDelta: 3
                - nationalPatriotsDelta: 3
                - clinicStockChange: {}
            """.trimIndent()
        }

        try {
            val resText = parentHandler.queryGeminiRawExternal(sysPrompt)
            val cleanJson = parentHandler.extractJsonFromStringExternal(resText)
            val json = JSONObject(cleanJson)

            val news = json.optString("newsArticle", "Elysium state decree signed under high legal safety protocols.")
            val appC = json.optInt("approvalChange", 4)
            val fundC = json.optDouble("fundsChange", 0.0)
            val clinicC = json.optDouble("clinicBalanceChange", 0.0)
            val wcD = json.optInt("workingClassDelta", 2)
            val mgD = json.optInt("medicalGuildDelta", 2)
            val ceD = json.optInt("corporateExecutiveDelta", 2)
            val npD = json.optInt("nationalPatriotsDelta", 2)

            val stockObj = json.optJSONObject("clinicStockChange")
            val stockMap = mutableMapOf<String, Int>()
            if (stockObj != null) {
                val keys = listOf("syringes", "saline", "adrenaline", "reagents", "meds")
                for (k in keys) {
                    val num = stockObj.optInt(k, 0)
                    if (num != 0) {
                        stockMap[k] = num
                    }
                }
            }

            withContext(Dispatchers.Main) {
                if (powerId == "gov_medicaid") {
                    _medicaidCoverageTier.value = (_medicaidCoverageTier.value + 1).coerceAtMost(3)
                } else if (powerId == "gov_quarantine") {
                    _isQuarantineActive.value = true
                } else if (powerId == "gov_deregulate_chemists") {
                    _chemistsDeregulated.value = true
                }

                // Call parent handler state update
                parentHandler.adjustSupportStates(wcD, mgD, ceD, npD, appC, fundC)

                // Write clinic stats change to database
                if (clinicC != 0.0 || stockMap.isNotEmpty()) {
                    val currentClinicBal = parentViewModel.clinicBalance.value
                    settingsDataStore.updateClinicStats(currentClinicBal + clinicC, parentViewModel.reputationStars.value)

                    val syringes = (parentViewModel.syringeStock.value + (stockMap["syringes"] ?: 0)).coerceAtLeast(0)
                    val saline = (parentViewModel.salineStock.value + (stockMap["saline"] ?: 0)).coerceAtLeast(0)
                    val adrenaline = (parentViewModel.adrenalineStock.value + (stockMap["adrenaline"] ?: 0)).coerceAtLeast(0)
                    val reagents = (parentViewModel.reagentsStock.value + (stockMap["reagents"] ?: 0)).coerceAtLeast(0)
                    val meds = (parentViewModel.medsStock.value + (stockMap["meds"] ?: 0)).coerceAtLeast(0)

                    settingsDataStore.saveInventory(syringes, saline, adrenaline, reagents, meds)
                }
            }

            DecisionOutcome(
                issueTitle = "⚡ STATE DECREE: ${powerId.replace("gov_", "").uppercase()}",
                chosenOption = "Governor's Executive Desk",
                newsArticle = "🏛️ EXECUTIVE MANSION REVIEW:\n\n$news",
                approvalChange = appC,
                fundsChange = fundC,
                prestigeChange = 6,
                clinicBalanceChange = clinicC,
                clinicStockChange = stockMap,
                factionDeltaNarrative = "WorkingClass: ${if (wcD >= 0) "+" else ""}$wcD%, Guild: ${if (mgD >= 0) "+" else ""}$mgD%, Patriots: ${if (npD >= 0) "+" else ""}$npD%"
            )
        } catch (e: Exception) {
            Log.e("GovernorOfficeHandler", "Decree failed", e)
            DecisionOutcome(
                issueTitle = "⚡ GOVERNOR STATE DECREE",
                chosenOption = "Executive Action Desk",
                newsArticle = "🏛️ STATE DECREE ENACTED:\n\nSuccessfully coordinated high action directives under Executive Order $powerId. Metrics configured.",
                approvalChange = 3,
                fundsChange = 0.0,
                prestigeChange = 3
            )
        }
    }
}
