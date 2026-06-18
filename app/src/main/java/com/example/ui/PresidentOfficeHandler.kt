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

class PresidentOfficeHandler(
    private val parentHandler: PoliticsHandler,
    private val parentViewModel: SimulationViewModel,
    private val application: Application,
    private val settingsDataStore: SettingsDataStore
) {
    private val _dpaIntensity = MutableStateFlow<Int>(0) // level 0 to 3
    val dpaIntensity: StateFlow<Int> = _dpaIntensity.asStateFlow()

    private val _globalTreatySigned = MutableStateFlow<Boolean>(false)
    val globalTreatySigned: StateFlow<Boolean> = _globalTreatySigned.asStateFlow()

    private val _federalDeficitLevel = MutableStateFlow<Double>(100.0) // sovereign reserve ratio %
    val federalDeficitLevel: StateFlow<Double> = _federalDeficitLevel.asStateFlow()

    suspend fun executeDecree(powerId: String): DecisionOutcome = withContext(Dispatchers.IO) {
        val sysPrompt = when (powerId) {
            "pres_dpa" -> """
                The President of the Republic executed the 'Sovereign Defense Production Act for Medical stockpiles'.
                Write a 2-paragraph news editorial describing national factory retooling to package chemical precursors.
                Determine changes in STRICT valid JSON:
                - newsArticle: "Retooling editorial..."
                - approvalChange: -5
                - fundsChange: -6000.0
                - clinicBalanceChange: 0.0
                - workingClassDelta: 5
                - medicalGuildDelta: 20
                - corporateExecutiveDelta: -10
                - nationalPatriotsDelta: 15
                - clinicStockChange: { "syringes": 100, "saline": 100, "adrenaline": 40, "reagents": 50, "meds": 50 }
            """.trimIndent()

            "pres_executive_grant" -> """
                The President signed 'Executive Health Directive #1024' granting sovereign disaster relief funds directly to municipal trauma clinics.
                Write a 2-paragraph news editorial on budget allocation debates in Congress.
                Determine changes in STRICT valid JSON:
                - newsArticle: "Capitol editorial..."
                - approvalChange: 12
                - fundsChange: 0.0
                - clinicBalanceChange: 45000.0
                - workingClassDelta: 20
                - medicalGuildDelta: 15
                - corporateExecutiveDelta: -15
                - nationalPatriotsDelta: -5
                - clinicStockChange: {}
            """.trimIndent()

            "pres_nationalize_biotech" -> """
                The President nationalized 'Aegis Pharmaceutical Laboratories' to synthesize rare adrenaline and reagents for local clinics.
                Write a 2-paragraph newspaper report on corporate fallout.
                Determine changes in STRICT valid JSON:
                - newsArticle: "Nationalization report..."
                - approvalChange: 15
                - fundsChange: -10000.0
                - clinicBalanceChange: 20000.0
                - workingClassDelta: 25
                - medicalGuildDelta: 18
                - corporateExecutiveDelta: -30
                - nationalPatriotsDelta: -8
                - clinicStockChange: { "adrenaline": 50, "reagents": 80, "meds": 80 }
            """.trimIndent()

            "pres_global_health_accord" -> """
                The President signed the 'Elysium-Sovereign Bilateral Medical Free-Trade Treaty'.
                Write a 2-paragraph treaty analysis.
                Determine changes in STRICT valid JSON:
                - newsArticle: "Treaty analysis..."
                - approvalChange: 14
                - fundsChange: 5000.0
                - clinicBalanceChange: 15000.0
                - workingClassDelta: 12
                - medicalGuildDelta: 22
                - corporateExecutiveDelta: 18
                - nationalPatriotsDelta: -2
                - clinicStockChange: { "syringes": 50, "saline": 50 }
            """.trimIndent()

            else -> """
                Sovereign decree executed.
                Determine changes in STRICT valid JSON:
                - newsArticle: "Executive command directive signed."
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

            val news = json.optString("newsArticle", "The presidential seal was fixed onto the bill directive under sovereign oversight.")
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
                if (powerId == "pres_dpa") {
                    _dpaIntensity.value = (_dpaIntensity.value + 1).coerceAtMost(3)
                } else if (powerId == "pres_global_health_accord") {
                    _globalTreatySigned.value = true
                } else {
                    _federalDeficitLevel.value = (_federalDeficitLevel.value - 8.5).coerceAtLeast(10.0)
                }

                parentHandler.adjustSupportStates(wcD, mgD, ceD, npD, appC, fundC)

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
                issueTitle = "⚡ SOVEREIGN COMMAND: ${powerId.replace("pres_", "").uppercase()}",
                chosenOption = "President's Oval Seal",
                newsArticle = "🦅 OVAL OFFICE BRIEFING:\n\n$news",
                approvalChange = appC,
                fundsChange = fundC,
                prestigeChange = 12,
                clinicBalanceChange = clinicC,
                clinicStockChange = stockMap,
                factionDeltaNarrative = "WorkingClass: ${if (wcD >= 0) "+" else ""}$wcD%, Guild: ${if (mgD >= 0) "+" else ""}$mgD%, Corporate: ${if (ceD >= 0) "+" else ""}$ceD%, Patriots: ${if (npD >= 0) "+" else ""}$npD%"
            )
        } catch (e: Exception) {
            Log.e("PresidentOfficeHandler", "Decree failed", e)
            DecisionOutcome(
                issueTitle = "⚡ PRESIDENTIAL COMMAND ENACTED",
                chosenOption = "Executive Command Desk",
                newsArticle = "🦅 EXECUTIVE BRIEFING:\n\nSovereign directive was signed with high compliance margins. Internal agencies deployed for code $powerId.",
                approvalChange = 4,
                fundsChange = 0.0,
                prestigeChange = 5
            )
        }
    }
}
