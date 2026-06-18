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

class LegislatorOfficeHandler(
    private val parentHandler: PoliticsHandler,
    private val parentViewModel: SimulationViewModel,
    private val application: Application,
    private val settingsDataStore: SettingsDataStore
) {
    private val _cosponsorCount = MutableStateFlow<Int>(3)
    val cosponsorCount: StateFlow<Int> = _cosponsorCount.asStateFlow()

    private val _patentExclusivityYears = MutableStateFlow<Int>(20)
    val patentExclusivityYears: StateFlow<Int> = _patentExclusivityYears.asStateFlow()

    private val _lobbyistAlignment = MutableStateFlow<Int>(50) // 0 to 100
    val lobbyistAlignment: StateFlow<Int> = _lobbyistAlignment.asStateFlow()

    fun recruitCosponsor() {
        _cosponsorCount.value = (_cosponsorCount.value + 1).coerceAtMost(15)
    }

    suspend fun executeDecree(powerId: String): DecisionOutcome = withContext(Dispatchers.IO) {
        val sysPrompt = when (powerId) {
            "sen_filibuster" -> """
                The member conducted an intensive 'Televised Capitol Floor Filibuster' on humanitarian clinical care and lower drug prices.
                Write a 2-paragraph newspaper report on physical exhaustion and political effects.
                Determine changes in STRICT valid JSON:
                - newsArticle: "Floor report..."
                - approvalChange: 8
                - fundsChange: -2000.0
                - clinicBalanceChange: 0.0
                - workingClassDelta: 12
                - medicalGuildDelta: 6
                - corporateExecutiveDelta: -8
                - nationalPatriotsDelta: 15
                - clinicStockChange: {}
            """.trimIndent()

            "leg_earmark_rider" -> """
                The Legislator attached a backdoor 'Clinical Infrastructure Reinvestment Rider' to a national transport appropriation bill.
                Write a 2-paragraph newspaper report detailing how the earmark was secured.
                Determine changes in STRICT valid JSON:
                - newsArticle: "Corridors report..."
                - approvalChange: 4
                - fundsChange: -1000.0
                - clinicBalanceChange: 15000.0
                - workingClassDelta: 8
                - medicalGuildDelta: 12
                - corporateExecutiveDelta: 4
                - nationalPatriotsDelta: -2
                - clinicStockChange: { "syringes": 20, "saline": 10 }
            """.trimIndent()

            "leg_patent_reduction" -> """
                The Legislator sponsored the 'Emergency Generic Manufacturing Speedup Act' cutting pharmaceutical patent exclusivity boundaries.
                Write 2 paragraphs on lobbying clash.
                Determine changes in STRICT valid JSON:
                - newsArticle: "Lobbyists clash report..."
                - approvalChange: 10
                - fundsChange: 0.0
                - clinicBalanceChange: 0.0
                - workingClassDelta: 16
                - medicalGuildDelta: 8
                - corporateExecutiveDelta: -16
                - nationalPatriotsDelta: -4
                - clinicStockChange: { "adrenaline": 10, "reagents": 15, "meds": 15 }
            """.trimIndent()

            else -> """
                Legislative motion executed.
                Determine changes in STRICT valid JSON:
                - newsArticle: "Motion executed."
                - approvalChange: 2
                - fundsChange: 0.0
                - clinicBalanceChange: 0.0
                - workingClassDelta: 2
                - medicalGuildDelta: 2
                - corporateExecutiveDelta: 2
                - nationalPatriotsDelta: 2
                - clinicStockChange: {}
            """.trimIndent()
        }

        try {
            val resText = parentHandler.queryGeminiRawExternal(sysPrompt)
            val cleanJson = parentHandler.extractJsonFromStringExternal(resText)
            val json = JSONObject(cleanJson)

            val news = json.optString("newsArticle", "Chamber floors count votes on the medical policy adjustment.")
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
                if (powerId == "leg_patent_reduction") {
                    _patentExclusivityYears.value = (_patentExclusivityYears.value - 4).coerceAtLeast(4)
                } else if (powerId == "leg_earmark_rider") {
                    _lobbyistAlignment.value = (_lobbyistAlignment.value + 12).coerceAtMost(100)
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
                issueTitle = "⚡ LEGISLATIVE RESOLUTION: ${powerId.replace("leg_", "").uppercase()}",
                chosenOption = "Legislator's Office Chest",
                newsArticle = "🗳️ WASHINGTON CAPITOL FLOOR:\n\n$news",
                approvalChange = appC,
                fundsChange = fundC,
                prestigeChange = 8,
                clinicBalanceChange = clinicC,
                clinicStockChange = stockMap,
                factionDeltaNarrative = "WorkingClass: ${if (wcD >= 0) "+" else ""}$wcD%, Guild: ${if (mgD >= 0) "+" else ""}$mgD%, Patriots: ${if (npD >= 0) "+" else ""}$npD%"
            )
        } catch (e: Exception) {
            Log.e("LegislatorOfficeHandler", "Decree failed", e)
            DecisionOutcome(
                issueTitle = "⚡ LEGISLATIVE ACTION COMPLETED",
                chosenOption = "Chamber Order",
                newsArticle = "🗳️ COMMITTEE REPORTS:\n\nSuccessfully coordinated structural policy layout in legislative record $powerId.",
                approvalChange = 3,
                fundsChange = 0.0,
                prestigeChange = 4
            )
        }
    }
}
