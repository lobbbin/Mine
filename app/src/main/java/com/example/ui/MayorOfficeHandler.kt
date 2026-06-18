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

class MayorOfficeHandler(
    private val parentHandler: PoliticsHandler,
    private val parentViewModel: SimulationViewModel,
    private val application: Application,
    private val settingsDataStore: SettingsDataStore
) {
    private val _sanitarySquadCount = MutableStateFlow<Int>(0)
    val sanitarySquadCount: StateFlow<Int> = _sanitarySquadCount.asStateFlow()

    private val _localSalesTax = MutableStateFlow<Double>(5.0) // percentage
    val localSalesTax: StateFlow<Double> = _localSalesTax.asStateFlow()

    private val _hospitalSubsidyRate = MutableStateFlow<Double>(0.0) // percentage multiplier
    val hospitalSubsidyRate: StateFlow<Double> = _hospitalSubsidyRate.asStateFlow()

    fun updateSalesTax(newTax: Double) {
        _localSalesTax.value = newTax.coerceIn(0.0, 15.0)
    }

    /**
     * Executes a Mayor decree. Automatically updates the game state
     * (including parent metrics, clinic balance, and stock counts) and yields
     * structural game modifiers.
     */
    suspend fun executeDecree(powerId: String): DecisionOutcome = withContext(Dispatchers.IO) {
        val sysPrompt = when (powerId) {
            "mayor_levy" -> """
                You are a Judicial Council Analyst. The Mayor issued a 'Municipal Medical Levy'.
                Write a 2-paragraph newspaper report about this municipal tax allocation.
                Determine changes in STRICT valid JSON format:
                - newsArticle: "Detailed journalistic report..."
                - approvalChange: -6
                - fundsChange: 4000.0
                - clinicBalanceChange: 5000.0
                - workingClassDelta: -5
                - medicalGuildDelta: 6
                - corporateExecutiveDelta: -8
                - nationalPatriotsDelta: 3
                - clinicStockChange: {}
            """.trimIndent()

            "mayor_refill" -> """
                You are a City Dispatch Recorder. The Mayor issued 'Supply Requisition Grants' through the local precinct.
                Write a 2-paragraph newspaper report detailing the relief packages.
                Determine changes in STRICT valid JSON format:
                - newsArticle: "Detailed journalistic report..."
                - approvalChange: 4
                - fundsChange: 0.0
                - clinicBalanceChange: 0.0
                - workingClassDelta: 5
                - medicalGuildDelta: 12
                - corporateExecutiveDelta: -2
                - nationalPatriotsDelta: 0
                - clinicStockChange: { "syringes": 20, "saline": 15, "meds": 10 }
            """.trimIndent()

            "mayor_sanitary_patrols" -> """
                The Mayor issued an executive directive for 'Sanitary Outbreak Patrol Squads' to disinfect slums and districts.
                Write a 2-paragraph newspaper report.
                Determine changes in STRICT valid JSON format:
                - newsArticle: "Detailed journalistic report..."
                - approvalChange: 8
                - fundsChange: -1500.0
                - clinicBalanceChange: 0.0
                - workingClassDelta: 10
                - medicalGuildDelta: 8
                - corporateExecutiveDelta: -3
                - nationalPatriotsDelta: 12
                - clinicStockChange: { "reagents": 8 }
            """.trimIndent()

            "mayor_community_health_center" -> """
                The Mayor approved building a 'Community Health Counseling Wing' directly within the local municipal hospital.
                Write a 2-paragraph newspaper report.
                Determine changes in STRICT valid JSON format:
                - newsArticle: "Detailed journalistic report..."
                - approvalChange: 12
                - fundsChange: -5000.0
                - clinicBalanceChange: 0.0
                - workingClassDelta: 18
                - medicalGuildDelta: 10
                - corporateExecutiveDelta: 2
                - nationalPatriotsDelta: -2
                - clinicStockChange: { "syringes": 15, "saline": 10, "adrenaline": 5, "meds": 5 }
            """.trimIndent()

            else -> """
                Sovereign local action executed.
                Determine changes in STRICT valid JSON format:
                - newsArticle: "Municipal decree executed."
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

            val news = json.optString("newsArticle", "Decree announced. City administration remains stable under the Mayor.")
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

            // Downstream effects update on main thread
            withContext(Dispatchers.Main) {
                if (powerId == "mayor_sanitary_patrols") {
                    _sanitarySquadCount.value += 3
                } else if (powerId == "mayor_community_health_center") {
                    _hospitalSubsidyRate.value = (_hospitalSubsidyRate.value + 0.04).coerceAtMost(0.20)
                }

                // Apply adjustments to main PoliticsHandler
                parentHandler.adjustSupportStates(wcD, mgD, ceD, npD, appC, fundC)
                
                // Write proceeds to database seamlessly
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
                issueTitle = "⚡ MAYOR ORDINANCE: ${powerId.replace("mayor_", "").uppercase()}",
                chosenOption = "City Council Seal",
                newsArticle = "🏢 CITY HALL BULLETIN:\n\n$news",
                approvalChange = appC,
                fundsChange = fundC,
                prestigeChange = 4,
                clinicBalanceChange = clinicC,
                clinicStockChange = stockMap,
                factionDeltaNarrative = "WorkingClass: ${if (wcD >= 0) "+" else ""}$wcD%, Guild: ${if (mgD >= 0) "+" else ""}$mgD%, Corporate: ${if (ceD >= 0) "+" else ""}$ceD%"
            )
        } catch (e: Exception) {
            Log.e("MayorOfficeHandler", "Decree failed", e)
            DecisionOutcome(
                issueTitle = "⚡ MAYOR ORDINANCE ENACTED",
                chosenOption = "Administrative Order",
                newsArticle = "🏢 CITY GAZETTE:\n\nSuccessfully signed local emergency code $powerId. Public services adjust allocations accordingly.",
                approvalChange = 3,
                fundsChange = 0.0,
                prestigeChange = 2
            )
        }
    }
}
