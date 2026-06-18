package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.data.SettingsDataStore
import com.example.network.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.regex.Pattern

// --- Data Classes for Deep Politics Simulation ---

data class DailyIssue(
    val title: String,
    val description: String,
    val category: String,
    val optionA: CommandOption,
    val optionB: CommandOption,
    val optionC: CommandOption
)

data class CommandOption(
    val text: String,
    val outcomeSummary: String
)

data class DecisionOutcome(
    val issueTitle: String,
    val chosenOption: String,
    val newsArticle: String,
    val approvalChange: Int,
    val fundsChange: Double,
    val prestigeChange: Int,
    val clinicBalanceChange: Double = 0.0,
    val clinicStockChange: Map<String, Int> = emptyMap(),
    val factionDeltaNarrative: String = ""
)

data class CabinetStaff(
    val id: String,
    val role: String,
    val name: String,
    val description: String,
    val setupCost: Double,
    val dailySalary: Double,
    val bonusSummary: String,
    val tier: String = "Mayor"
)

data class LegislativeBillResult(
    val billTitle: String,
    val passed: Boolean,
    val tallyHouse: String,
    val tallySenate: String,
    val journalismExcerpt: String,
    val dynamicClinicFundsGrant: Double,
    val dynamicStocksReward: Map<String, Int>
)

class PoliticsHandler(
    private val parentViewModel: SimulationViewModel,
    private val application: Application,
    private val settingsDataStore: SettingsDataStore
) {
    // --- Sub-handlers for individual offices (prevents overcrowding) ---
    val mayorOfficeHandler = MayorOfficeHandler(this, parentViewModel, application, settingsDataStore)
    val governorOfficeHandler = GovernorOfficeHandler(this, parentViewModel, application, settingsDataStore)
    val legislatorOfficeHandler = LegislatorOfficeHandler(this, parentViewModel, application, settingsDataStore)
    val presidentOfficeHandler = PresidentOfficeHandler(this, parentViewModel, application, settingsDataStore)

    // --- Basic State Variables ---
    private val _currentOffice = MutableStateFlow<String>("Sovereign Civil Organizer")
    val currentOffice: StateFlow<String> = _currentOffice.asStateFlow()

    private val _officeLevel = MutableStateFlow<String>("None") // "Mayor", "Governor", "State Representative", "President", etc.
    val officeLevel: StateFlow<String> = _officeLevel.asStateFlow()

    private val _approvalRating = MutableStateFlow<Int>(50)
    val approvalRating: StateFlow<Int> = _approvalRating.asStateFlow()

    private val _officeTermDays = MutableStateFlow<Int>(0)
    val officeTermDays: StateFlow<Int> = _officeTermDays.asStateFlow()

    private val _campaignFunds = MutableStateFlow<Double>(12000.0) // Campaign chest ledger
    val campaignFunds: StateFlow<Double> = _campaignFunds.asStateFlow()

    private val _voterPolling = MutableStateFlow<Int>(35)
    val voterPolling: StateFlow<Int> = _voterPolling.asStateFlow()

    private val _activeCampaignRace = MutableStateFlow<String?>(null)
    val activeCampaignRace: StateFlow<String?> = _activeCampaignRace.asStateFlow()

    private val _campaignTurnsLeft = MutableStateFlow<Int>(0)
    val campaignTurnsLeft: StateFlow<Int> = _campaignTurnsLeft.asStateFlow()

    private val _campaignHistory = MutableStateFlow<List<String>>(emptyList())
    val campaignHistory: StateFlow<List<String>> = _campaignHistory.asStateFlow()

    private val _currentIssue = MutableStateFlow<DailyIssue?>(null)
    val currentIssue: StateFlow<DailyIssue?> = _currentIssue.asStateFlow()

    private val _isAILoading = MutableStateFlow<Boolean>(false)
    val isAILoading: StateFlow<Boolean> = _isAILoading.asStateFlow()

    private val _recentOutcome = MutableStateFlow<DecisionOutcome?>(null)
    val recentOutcome: StateFlow<DecisionOutcome?> = _recentOutcome.asStateFlow()

    private val _outcomesHistory = MutableStateFlow<List<DecisionOutcome>>(emptyList())
    val outcomesHistory: StateFlow<List<DecisionOutcome>> = _outcomesHistory.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // --- Faction Deep Support Variables ---
    private val _workingClassSupport = MutableStateFlow<Int>(50)
    val workingClassSupport: StateFlow<Int> = _workingClassSupport.asStateFlow()

    private val _medicalGuildSupport = MutableStateFlow<Int>(55)
    val medicalGuildSupport: StateFlow<Int> = _medicalGuildSupport.asStateFlow()

    private val _corporateExecutiveSupport = MutableStateFlow<Int>(48)
    val corporateExecutiveSupport: StateFlow<Int> = _corporateExecutiveSupport.asStateFlow()

    private val _nationalPatriotsSupport = MutableStateFlow<Int>(52)
    val nationalPatriotsSupport: StateFlow<Int> = _nationalPatriotsSupport.asStateFlow()

    // --- Cabinet / Staff Selection Variables ---
    private val _hiredStaffIds = MutableStateFlow<Set<String>>(emptySet())
    val hiredStaffIds: StateFlow<Set<String>> = _hiredStaffIds.asStateFlow()

    val availableStaffList = listOf(
        // --- MAYOR STAFF (Tier 1) ---
        CabinetStaff(
            id = "mayor_health",
            role = "Municipal Health Officer",
            name = "Dr. Helen Vance",
            description = "Coordinates city clinics. Automatically delivers a shipment of saline and syringes occasionally to help poorer districts.",
            setupCost = 3500.0,
            dailySalary = 100.0,
            bonusSummary = "+15% Medical Guild Support. Passive delivery of syringes and saline.",
            tier = "Mayor"
        ),
        CabinetStaff(
            id = "mayor_treasury",
            role = "City Treasurer Coordinator",
            name = "Sarah Sterling",
            description = "Collects municipal business tax grants and distributes small business local clinic subsidies.",
            setupCost = 4200.0,
            dailySalary = 120.0,
            bonusSummary = "+10% Corporate Support. Adds +R300 daily to campaign budget.",
            tier = "Mayor"
        ),
        CabinetStaff(
            id = "mayor_media",
            role = "Mayor Press Secretary",
            name = "Julian Finch",
            description = "Optimizes neighborhood town halls and city media releases, amplifying local approval.",
            setupCost = 3000.0,
            dailySalary = 90.0,
            bonusSummary = "+8% Working Class Support. Speeches are 1.3x more effective.",
            tier = "Mayor"
        ),
        CabinetStaff(
            id = "mayor_sanitation",
            role = "Public Sanitation Officer",
            name = "Director Marcus Brogden",
            description = "Establishes clean water systems and municipal waste guidelines, lowering community infection rates.",
            setupCost = 4000.0,
            dailySalary = 110.0,
            bonusSummary = "+12% Patriot Support. Delivers +2 saline, +2 reagents daily.",
            tier = "Mayor"
        ),
        CabinetStaff(
            id = "mayor_police",
            role = "Municipal Police Liaison",
            name = "Commissioner Thomas Marcus",
            description = "Maintains order during clinical vaccine riots and reduces public backlash penalty scales.",
            setupCost = 4500.0,
            dailySalary = 115.0,
            bonusSummary = "Reduces candidate campaign penalty rating hits by 15% across all actions.",
            tier = "Mayor"
        ),

        // --- GOVERNOR STAFF (Tier 2) ---
        CabinetStaff(
            id = "gov_health_sec",
            role = "State Health Secretary",
            name = "Dr. Arthur Pendelton",
            description = "Fights for state-wide hospital funding. Speeds up Medicaid tier approval loops.",
            setupCost = 6500.0,
            dailySalary = 180.0,
            bonusSummary = "+18% Medical Guild support. 20% daily chance of +3 adrenaline, +8 meds.",
            tier = "Governor"
        ),
        CabinetStaff(
            id = "gov_justice",
            role = "State Attorney General",
            name = "Evelyn Marshall",
            description = "Secures clinics against zoning lawsuits, protects practitioner licenses, and handles financial audits.",
            setupCost = 7200.0,
            dailySalary = 200.0,
            bonusSummary = "Reduces active fine costs by 40% and shields against medical board audits.",
            tier = "Governor"
        ),
        CabinetStaff(
            id = "gov_finance",
            role = "State Budget Director",
            name = "Vance Cropper",
            description = "Acquires state infrastructural bonds to enrich campaign finance coffers daily.",
            setupCost = 7500.0,
            dailySalary = 220.0,
            bonusSummary = "+15% Corporate Support. Automatically adds +R600 daily to campaign budget.",
            tier = "Governor"
        ),
        CabinetStaff(
            id = "gov_labor",
            role = "Labor Alliance Commissioner",
            name = "Chloe Dubois",
            description = "Forges alliances with industrial labor syndicates, promoting workers' health insurance programs.",
            setupCost = 6000.0,
            dailySalary = 160.0,
            bonusSummary = "+20% Working Class Trust. 15% chance of R1000 union clinic grant daily.",
            tier = "Governor"
        ),
        CabinetStaff(
            id = "gov_outbreak",
            role = "Epidemy Relief Liaison",
            name = "Dr. Sierra Myers",
            description = "Deploys statewide vaccine buffers, preventing compound outbreak spikes of regional flu.",
            setupCost = 6800.0,
            dailySalary = 170.0,
            bonusSummary = "+14% Patriot Support. Generates +3 reagents, +3 meds daily.",
            tier = "Governor"
        ),

        // --- PRESIDENTIAL CABINET (Tier 3) ---
        CabinetStaff(
            id = "pres_nsa",
            role = "National Security Advisor",
            name = "General John Ironclad",
            description = "Retools strategic defense assembly lines under the DPA, sending deep stock buffers to public facilities.",
            setupCost = 11000.0,
            dailySalary = 320.0,
            bonusSummary = "Brings massive military stock cargo (+15 syringes, +15 saline) to clinic daily.",
            tier = "President"
        ),
        CabinetStaff(
            id = "pres_diplomacy",
            role = "President Secretary of State",
            name = "Ambassador Liam Mercer",
            description = "Negotiates free-trade chemical treaties, importing precursors from bilateral allies easily.",
            setupCost = 10000.0,
            dailySalary = 300.0,
            bonusSummary = "+15% National Polling. Unlocks highly lucrative global pharmaceutical imports.",
            tier = "President"
        ),
        CabinetStaff(
            id = "pres_treasury",
            role = "Sovereign Treasury Secretary",
            name = "Chairman Alistair Vance",
            description = "Maintains federal sovereign currency reserves. Grants maximum financial campaign leverage.",
            setupCost = 12500.0,
            dailySalary = 380.0,
            bonusSummary = "Sovereign reserve multiplier! Automatically adds +R1200 daily to campaign ledger.",
            tier = "President"
        ),
        CabinetStaff(
            id = "pres_fda",
            role = "FDA Chief Commissioner",
            name = "Dr. Abigail Sterling",
            description = "Fast-tracks generic formulation licenses and curbs monopolistic pharmaceutical patent exclusivity gaps.",
            setupCost = 11500.0,
            dailySalary = 350.0,
            bonusSummary = "Cuts patent exclusivity years on bills. Delivers +5 reagents, +5 meds daily.",
            tier = "President"
        ),
        CabinetStaff(
            id = "pres_intelligence",
            role = "Director of Intelligence",
            name = "Agent Raymond Cross",
            description = "Monitors corrupt pharma lobbyists. Deploys leverage metrics to secure 100% legislative compliance.",
            setupCost = 13000.0,
            dailySalary = 400.0,
            bonusSummary = "Increases legislative bill voter pass rates by 10x and prevents hostile corporate takeover bids.",
            tier = "President"
        )
    )

    // --- Legislative Custom Bills State ---
    private val _recentBillResult = MutableStateFlow<LegislativeBillResult?>(null)
    val recentBillResult: StateFlow<LegislativeBillResult?> = _recentBillResult.asStateFlow()

    init {
        // Automatically fetch initial briefing as an organizer
        generateOrganizerFallbackIssue()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearRecentBillResult() {
        _recentBillResult.value = null
    }

    // --- Fund Transfers: Seamless Game Connections ---

    fun donateToCampaign(amount: Double): Boolean {
        val currentClinicBalance = parentViewModel.clinicBalance.value
        if (currentClinicBalance >= amount) {
            parentViewModel.viewModelScope.launch {
                val newBal = currentClinicBalance - amount
                settingsDataStore.updateClinicStats(newBal, parentViewModel.reputationStars.value)
                _campaignFunds.value += amount
            }
            return true
        }
        return false
    }

    fun deductCampaignFunds(amount: Double) {
        _campaignFunds.value = (_campaignFunds.value - amount).coerceAtLeast(0.0)
    }

    fun boostVoterPolling(delta: Int) {
        _voterPolling.value = (_voterPolling.value + delta).coerceIn(0, 100)
    }

    fun boostPrestige(delta: Int) {
        val currentPrestige = parentViewModel.politicalPrestige.value
        parentViewModel.updatePoliticalPrestige((currentPrestige + delta).coerceIn(0, 100))
    }

    fun modifyApprovalRating(delta: Int) {
        _approvalRating.value = (_approvalRating.value + delta).coerceIn(0, 100)
    }

    fun transferToClinic(amount: Double): String {
        if (_campaignFunds.value >= amount) {
            _campaignFunds.value -= amount
            val currentClinicBalance = parentViewModel.clinicBalance.value
            
            // Gain cash in clinic, but incur high risk of political backlash!
            val approvalHit = (amount / 1200.0).toInt().coerceIn(6, 48)
            val prestigeHit = (amount / 2500.0).toInt().coerceIn(3, 18)
            
            _approvalRating.value = (_approvalRating.value - approvalHit).coerceAtLeast(0)
            _workingClassSupport.value = (_workingClassSupport.value - approvalHit).coerceAtLeast(0)
            
            parentViewModel.viewModelScope.launch {
                val newBal = currentClinicBalance + amount
                settingsDataStore.updateClinicStats(newBal, parentViewModel.reputationStars.value)
                parentViewModel.updatePoliticalPrestige((parentViewModel.politicalPrestige.value - prestigeHit).coerceAtLeast(0))
            }
            return "Transferred $amount to medical clinic. Audit scandal detected! Public approval dropped by $approvalHit%, prestige slipped by $prestigeHit."
        }
        return "Insufficient campaign funds."
    }

    // --- Cabinet Staff Recruitment Management ---

    fun hireStaff(staffId: String) {
        val staff = availableStaffList.find { it.id == staffId } ?: return
        if (_hiredStaffIds.value.contains(staffId)) return

        if (_campaignFunds.value >= staff.setupCost) {
            _campaignFunds.value -= staff.setupCost
            val nextHired = _hiredStaffIds.value + staffId
            _hiredStaffIds.value = nextHired

            // Dynamic instant faction buffs
            when (staffId) {
                "mayor_health" -> _medicalGuildSupport.value = (_medicalGuildSupport.value + 15).coerceAtMost(100)
                "mayor_treasury" -> _corporateExecutiveSupport.value = (_corporateExecutiveSupport.value + 10).coerceAtMost(100)
                "mayor_media" -> _workingClassSupport.value = (_workingClassSupport.value + 12).coerceAtMost(100)
                "mayor_sanitation" -> _nationalPatriotsSupport.value = (_nationalPatriotsSupport.value + 12).coerceAtMost(100)
                "mayor_police" -> _nationalPatriotsSupport.value = (_nationalPatriotsSupport.value + 10).coerceAtMost(100)
                "gov_health_sec" -> _medicalGuildSupport.value = (_medicalGuildSupport.value + 18).coerceAtMost(100)
                "gov_justice" -> _nationalPatriotsSupport.value = (_nationalPatriotsSupport.value + 14).coerceAtMost(100)
                "gov_finance" -> _corporateExecutiveSupport.value = (_corporateExecutiveSupport.value + 15).coerceAtMost(100)
                "gov_labor" -> _workingClassSupport.value = (_workingClassSupport.value + 20).coerceAtMost(100)
                "gov_outbreak" -> _medicalGuildSupport.value = (_medicalGuildSupport.value + 12).coerceAtMost(100)
                "pres_nsa" -> {
                    _nationalPatriotsSupport.value = (_nationalPatriotsSupport.value + 15).coerceAtMost(100)
                    _medicalGuildSupport.value = (_medicalGuildSupport.value + 10).coerceAtMost(100)
                }
                "pres_diplomacy" -> {
                    _medicalGuildSupport.value = (_medicalGuildSupport.value + 15).coerceAtMost(100)
                    _corporateExecutiveSupport.value = (_corporateExecutiveSupport.value + 10).coerceAtMost(100)
                }
                "pres_treasury" -> _corporateExecutiveSupport.value = (_corporateExecutiveSupport.value + 20).coerceAtMost(100)
                "pres_fda" -> {
                    _workingClassSupport.value = (_workingClassSupport.value + 15).coerceAtMost(100)
                    _medicalGuildSupport.value = (_medicalGuildSupport.value + 15).coerceAtMost(100)
                }
                "pres_intelligence" -> _nationalPatriotsSupport.value = (_nationalPatriotsSupport.value + 15).coerceAtMost(100)
            }
        } else {
            _errorMessage.value = "Insufficient Campaign funds to coordinate recruitment! Vance requires setup fees of $${staff.setupCost}."
        }
    }

    fun dismissStaff(staffId: String) {
        if (_hiredStaffIds.value.contains(staffId)) {
            _hiredStaffIds.value = _hiredStaffIds.value - staffId
        }
    }

    // --- Campaign Logic ---

    fun initiateCampaign(race: String) {
        _activeCampaignRace.value = race
        _officeLevel.value = race
        _campaignTurnsLeft.value = 5 // 5 weeks of preparation
        _voterPolling.value = when (race) {
            "Mayor" -> 40
            "Governor" -> 30
            "State Representative" -> 38
            "State Senator" -> 35
            "US Representative" -> 32
            "Senator" -> 28
            "President" -> 20
            else -> 30
        }
        _campaignHistory.value = listOf("Announced direct candidacy for the office of $race! Initiated voter mobilization drives.")
        _currentIssue.value = null
        _recentOutcome.value = null
    }

    fun retireCandidacy() {
        _activeCampaignRace.value = null
        _campaignTurnsLeft.value = 0
        _officeLevel.value = "None"
        _campaignHistory.value = emptyList()
        generateOrganizerFallbackIssue()
    }

    fun runCampaignAction(actionType: String, messageInput: String) {
        var cost = when (actionType) {
            "Town Hall Speech" -> 800.0
            "Social Media Ads" -> 1500.0
            "Television Debate" -> 3000.0
            "Corporate Lobby Gala" -> 4000.0
            "Labor Union Rally" -> 2500.0
            "Medical Science Symposium" -> 3500.0
            "Patriot Law & Order Panel" -> 2000.0
            "AI Healthcare Disruption Pitch" -> 5000.0
            "Grassroots Door-to-Door Canvassing" -> 1200.0
            else -> 1000.0
        }

        // Mayor Press Secretary or Legacy Press Secretary discount modifiers
        if (_hiredStaffIds.value.contains("mayor_media") || _hiredStaffIds.value.contains("press_sec")) {
            cost *= 0.85 // 15% discount on weekly advertisements & pitches
        }

        if (_campaignFunds.value < cost) {
            _errorMessage.value = "Insufficient funds! Operational fee is $$cost."
            return
        }

        _campaignFunds.value -= cost
        val turns = _campaignTurnsLeft.value - 1
        _campaignTurnsLeft.value = turns

        _isAILoading.value = true
        parentViewModel.viewModelScope.launch {
            try {
                val race = _activeCampaignRace.value ?: "Mayor"
                val systemPrompt = """
                    You are a strategic intelligence evaluating a political campaign action.
                    The candidate is contesting the seat of '$race'.
                    Campaign action type: '$actionType'.
                    Pitch / Campaign theme: "$messageInput".
                    Hired staff helping candidate: ${_hiredStaffIds.value.joinToString(", ")}.

                    Evaluate the media response, demographic rallies, and public critique (2-3 sentences of gripping political journalism).
                    Determine:
                    1. Voter Support % change (Integer between -4 and +15).
                    2. Campaign funds gained from trigger donations (Double between -100.0 and +4000.0).

                    Format your response in STRICT valid JSON:
                    {
                      "narrative": "A detailed journalism report describing the campaign outcome...",
                      "supportChange": 6,
                      "fundsChange": 1800.0
                    }
                """.trimIndent()

                val resText = queryGeminiRaw(systemPrompt)
                val cleanJson = extractJsonFromString(resText)
                val json = JSONObject(cleanJson)

                val narrative = json.optString("narrative", "Rally crowds responded favorably to core economic and medical accessibility messages.")
                var supportChange = json.optInt("supportChange", 3)
                var fundsChange = json.optDouble("fundsChange", 600.0)

                // Press Secretary bonus modifier
                if ((_hiredStaffIds.value.contains("press_sec") || _hiredStaffIds.value.contains("mayor_media")) && supportChange > 0) {
                    supportChange = (supportChange * 1.30).toInt() // 30% bonus polling multiplier
                }

                withContext(Dispatchers.Main) {
                    _voterPolling.value = (_voterPolling.value + supportChange).coerceIn(0, 100)
                    _campaignFunds.value += fundsChange

                    val bullet = "⚡ Week ${5 - turns}: $actionType - $narrative (Polling: ${if (supportChange >= 0) "+" else ""}$supportChange%, Revenue: +$${String.format("%.2f", fundsChange)})"
                    _campaignHistory.value = _campaignHistory.value + bullet
                    _isAILoading.value = false

                    if (turns <= 0) {
                        evaluateElectionDayResult()
                    }
                }
            } catch (e: Exception) {
                Log.e("PoliticsHandler", "Campaign play failed", e)
                withContext(Dispatchers.Main) {
                    val supportChange = (3..8).random()
                    val bonusDonation = (400..1500).random().toDouble()
                    _voterPolling.value = (_voterPolling.value + supportChange).coerceIn(0, 100)
                    _campaignFunds.value += bonusDonation
                    val bullet = "⚡ Week ${5 - turns}: $actionType - Local speech drew crowds. Support grew +$supportChange%, Ledger: +$$bonusDonation"
                    _campaignHistory.value = _campaignHistory.value + bullet
                    _isAILoading.value = false
                    if (turns <= 0) {
                        evaluateElectionDayResult()
                    }
                }
            }
        }
    }

    private fun evaluateElectionDayResult() {
        _isAILoading.value = true
        parentViewModel.viewModelScope.launch {
            try {
                val race = _activeCampaignRace.value ?: "Mayor"
                val support = _voterPolling.value
                val prestige = parentViewModel.politicalPrestige.value
                val campaignSummary = _campaignHistory.value.joinToString("\n")

                val systemPrompt = """
                    You are a National Election Committee reporter.
                    The candidate represents the sovereign health ticket and ran for the office of '$race'.
                    Review the campaign timeline:
                    $campaignSummary

                    Stats: final polling support is $support%. User political prestige is $prestige.
                    Decide if they WIN or LOSE. Players with >46% voter support and high prestige have high chances.
                    Create a theatrical, dramatic frontpage reporter column declaring election night, ballot counts, and local reaction.
                    Format in STRICT JSON:
                    {
                      "won": true,
                      "article": "Elysium Herald Reports: Underdog health coalition sweeps metropolitan districts...",
                      "victoryMargin": "52.4% vs 47.6%"
                    }
                """.trimIndent()

                val resText = queryGeminiRaw(systemPrompt)
                val cleanJson = extractJsonFromString(resText)
                val json = JSONObject(cleanJson)

                val won = json.optBoolean("won", false)
                val article = json.optString("article", "Elections concluded. Ballots counted under high security scrutiny.")
                val victoryMargin = json.optString("victoryMargin", "50.8% vs 49.2%")

                withContext(Dispatchers.Main) {
                    _isAILoading.value = false
                    if (won) {
                        _currentOffice.value = "$race of the Jurisdiction"
                        _officeLevel.value = race
                        _approvalRating.value = 54
                        _officeTermDays.value = 1
                        _activeCampaignRace.value = null
                        _outcomesHistory.value = emptyList()

                        // Refresh factions to reflect initial victory high
                        _workingClassSupport.value = 58
                        _medicalGuildSupport.value = 62
                        _corporateExecutiveSupport.value = 50
                        _nationalPatriotsSupport.value = 52

                        parentViewModel.updatePoliticalPrestige((parentViewModel.politicalPrestige.value + 15).coerceAtMost(100))

                        _recentOutcome.value = DecisionOutcome(
                            issueTitle = "🏆 ELECTION VICTORY: $race",
                            chosenOption = "Executive Seat Seized",
                            newsArticle = "🛎️ FRONT PAGE NEWS ($victoryMargin)!\n\n$article",
                            approvalChange = 15,
                            fundsChange = 4000.0,
                            prestigeChange = 15,
                            factionDeltaNarrative = "Victory wave! All political factions closely review your healthcare program."
                        )

                        // Start office daily problems
                        generateNextBriefingIssue()
                    } else {
                        parentViewModel.updatePoliticalPrestige((parentViewModel.politicalPrestige.value - 8).coerceAtLeast(0))
                        _recentOutcome.value = DecisionOutcome(
                            issueTitle = "🗳️ ELECTION DEFEAT: $race",
                            chosenOption = "Ballots Insufficient",
                            newsArticle = "📰 DEFICIT ENCOUNTERED ($victoryMargin)\n\n$article\n\nYou remain a Civil Health Representative.",
                            approvalChange = -4,
                            fundsChange = 0.0,
                            prestigeChange = -8
                        )
                        _activeCampaignRace.value = null
                        generateOrganizerFallbackIssue()
                    }
                }
            } catch (e: Exception) {
                Log.e("PoliticsHandler", "Election counting failed", e)
                withContext(Dispatchers.Main) {
                    val won = _voterPolling.value >= 47
                    _isAILoading.value = false
                    if (won) {
                        val race = _activeCampaignRace.value ?: "Mayor"
                        _currentOffice.value = "$race of the Jurisdiction"
                        _officeLevel.value = race
                        _approvalRating.value = 53
                        _officeTermDays.value = 1
                        _activeCampaignRace.value = null
                        _recentOutcome.value = DecisionOutcome(
                            issueTitle = "🏆 ELECTION VICTORY: $race",
                            chosenOption = "Metropolitan Win",
                            newsArticle = "🏆 LANDSLIDE SUCCESS!\n\nYour grassroots health coalition claimed victory with 51.5% of the vote!",
                            approvalChange = 10,
                            fundsChange = 2500.0,
                            prestigeChange = 10
                        )
                        generateNextBriefingIssue()
                    } else {
                        _recentOutcome.value = DecisionOutcome(
                            issueTitle = "🗳️ ELECTION LOSS",
                            chosenOption = "Consolidation",
                            newsArticle = "🗳️ VOTE DEFICIT\n\nYou fell short in the counting halls, securing ${_voterPolling.value}% backing.",
                            approvalChange = -3,
                            fundsChange = 0.0,
                            prestigeChange = -4
                        )
                        _activeCampaignRace.value = null
                        generateOrganizerFallbackIssue()
                    }
                }
            }
        }
    }

    // --- Office Day progression & Staff salary deductions ---

    fun advanceOfficeDay() {
        val currentDays = _officeTermDays.value
        if (currentDays > 0) {
            _officeTermDays.value = currentDays + 1

            // Passive salary and staff logic payouts
            var totalSalary = 0.0
            _hiredStaffIds.value.forEach { id ->
                val staff = availableStaffList.find { it.id == id }
                if (staff != null) {
                    totalSalary += staff.dailySalary
                }
            }

            if (_campaignFunds.value >= totalSalary) {
                _campaignFunds.value -= totalSalary
            } else {
                // Siphon from clinic balance to pay staff if campaign is dry!
                val clinicBal = parentViewModel.clinicBalance.value
                if (clinicBal >= totalSalary) {
                    parentViewModel.viewModelScope.launch {
                        settingsDataStore.updateClinicStats(clinicBal - totalSalary, parentViewModel.reputationStars.value)
                    }
                } else {
                    // Fire staff automatically due to default
                    _hiredStaffIds.value = emptySet()
                    _errorMessage.value = "Cabinet staff fired immediately! Insufficient funds to pay daily operational salaries."
                }
            }

            // --- ALL CABINET MEMBER PASSIVE DAILY TICK ADVANTAGES ---
            val hired = _hiredStaffIds.value

            // 1. Campaign Fund Dividends
            if (hired.contains("mayor_treasury") || hired.contains("treasurer")) {
                _campaignFunds.value += 300.0
            }
            if (hired.contains("gov_finance")) {
                _campaignFunds.value += 600.0
            }
            if (hired.contains("pres_treasury")) {
                _campaignFunds.value += 1200.0
            }

            // 2. Local Clinic Cargo Shipments and Inventory Support
            parentViewModel.viewModelScope.launch {
                var syringeDelta = 0
                var salineDelta = 0
                var adrenalineDelta = 0
                var reagentsDelta = 0
                var medsDelta = 0
                var clinicBalanceGain = 0.0

                // Mayor Health: 10% chance for +5 syringes, +5 saline
                if (hired.contains("mayor_health") && (1..100).random() <= 10) {
                    syringeDelta += 5
                    salineDelta += 5
                }
                // Mayor Sanitation: +2 saline, +2 reagents daily
                if (hired.contains("mayor_sanitation")) {
                    salineDelta += 2
                    reagentsDelta += 2
                }
                // Gov Health: 20% chance for +8 meds, +3 adrenaline
                if (hired.contains("gov_health_sec") && (1..100).random() <= 20) {
                    medsDelta += 8
                    adrenalineDelta += 3
                }
                // Gov Labor: 15% chance daily to transfer R1000 from state unions directly to Clinic balance
                if (hired.contains("gov_labor") && (1..100).random() <= 15) {
                    clinicBalanceGain += 1000.0
                }
                // Gov Outbreak: +3 reagents, +3 meds daily
                if (hired.contains("gov_outbreak")) {
                    reagentsDelta += 3
                    medsDelta += 3
                }
                // Pres NSA: +15 syringes, +15 saline daily
                if (hired.contains("pres_nsa")) {
                    syringeDelta += 15
                    salineDelta += 15
                }
                // Pres FDA: +5 reagents, +5 meds daily
                if (hired.contains("pres_fda")) {
                    reagentsDelta += 5
                    medsDelta += 5
                }

                // Support original legacy items
                if (hired.contains("surgeon_general") && (1..100).random() <= 20) {
                    syringeDelta += 12
                    salineDelta += 8
                    adrenalineDelta += 3
                }

                // Trigger persistent state updates
                if (syringeDelta > 0 || salineDelta > 0 || adrenalineDelta > 0 || reagentsDelta > 0 || medsDelta > 0) {
                    val finalSyringes = parentViewModel.syringeStock.value + syringeDelta
                    val finalSaline = parentViewModel.salineStock.value + salineDelta
                    val finalAdrenaline = parentViewModel.adrenalineStock.value + adrenalineDelta
                    val finalReagents = parentViewModel.reagentsStock.value + reagentsDelta
                    val finalMeds = parentViewModel.medsStock.value + medsDelta

                    settingsDataStore.saveInventory(
                        finalSyringes,
                        finalSaline,
                        finalAdrenaline,
                        finalReagents,
                        finalMeds
                    )
                    Log.d("PoliticsHandler", "Cabinet Restock: S:$syringeDelta, Sa:$salineDelta, Ad:$adrenalineDelta, Re:$reagentsDelta, Meds:$medsDelta")
                }

                if (clinicBalanceGain > 0.0) {
                    val newBal = parentViewModel.clinicBalance.value + clinicBalanceGain
                    settingsDataStore.updateClinicStats(newBal, parentViewModel.reputationStars.value)
                    Log.d("PoliticsHandler", "Labor Union clinic grant: +R$clinicBalanceGain")
                }
            }

            generateNextBriefingIssue()
            _recentOutcome.value = null
        } else {
            generateOrganizerFallbackIssue()
            _recentOutcome.value = null
        }
    }

    private fun generateOrganizerFallbackIssue() {
        _currentIssue.value = DailyIssue(
            title = "Primary Care Clinic Advocacy Expansion",
            description = "As a civil organizer, several medical and neighborhood councils request structural health advocacy in municipal clinics. The mayor currently ignores requests.",
            category = "Civil Advocacy",
            optionA = CommandOption(
                text = "Hold Charitable Vaccine Fundraiser",
                outcomeSummary = "Directly grants $600 campaign funds and +3% support with lower stakes."
            ),
            optionB = CommandOption(
                text = "Assemble Protest Demonstration",
                outcomeSummary = "Incurs $1,500 permit fees but increases prestige by +10. Anchors progressive support."
            ),
            optionC = CommandOption(
                text = "Lobby corporate hospital chains directly",
                outcomeSummary = "Saves campaign funds, increases corporate executive trust by +12%, but decreases progressive support."
            )
        )
    }

    fun generateNextBriefingIssue() {
        _isAILoading.value = true
        parentViewModel.viewModelScope.launch {
            try {
                val office = _currentOffice.value
                val sysPrompt = """
                    You are a Political Briefing Intelligence generator.
                    Create an immersive, strategic daily briefing scenario faced by the holder of the office: '$office'.
                    
                    CRITICAL JURISDICTION RULES (EMERGENCY SCALE BOUNDARIES):
                    - If the office is 'Mayor' (Municipal level): The issue must be STRICTLY local, city-specific, and municipal (e.g. city sewer breaks, local town council regulations, small municipal park sanitations, regional town clinic disputes). A town mayor CANNOT raise statewide taxes, regulate international borders, or sign federal trade pacts.
                    - If the office is 'State Representative', 'State Senator', 'US Representative', or 'Senator' (Legislative level): The issue must be legislative and policy-driven (e.g. chamber debates, lobbyist backroom deals, committee hearings, drug patent extensions, bill rider attachments).
                    - If the office is 'Governor' (State executive level): The issue must be statewide (e.g. state medicaid tiers, statewide highway quarantines, state union benefit structures, regional pharmacist deregulations).
                    - If the office is 'President' (Federal/Global chief executive level): The issue must represent massive country-wide, sovereign, or international command (e.g. Defense Production Act orders, federal central bank emergency loans, international health treaties, nationalizing key vaccine laboratories).

                    Specify three realistic choices with complex outcomes that impact:
                    1. Working Class (Healthcare users/Progressives)
                    2. Medical Guild (Physicians, pharmacists)
                    3. Corporate Executives (Industry, elite investors)
                    4. National Patriots (Trad, law compliance)

                    Format the output in absolutely strict valid JSON:
                    {
                      "title": "Short legislative / governing emergency title",
                      "description": "2-3 sentences outlining the crisis context.",
                      "category": "Healthcare / Security / Budgeting / Economy / Crisis",
                      "optionA": { "text": "Option A action", "outcomeSummary": "Immediate projection text" },
                      "optionB": { "text": "Option B action", "outcomeSummary": "Immediate projection text" },
                      "optionC": { "text": "Option C action", "outcomeSummary": "Immediate projection text" }
                    }
                """.trimIndent()

                val resText = queryGeminiRaw(sysPrompt)
                val cleanJson = extractJsonFromString(resText)
                val json = JSONObject(cleanJson)

                val issue = DailyIssue(
                    title = json.optString("title", "Clinical Integration Crisis"),
                    description = json.optString("description", "An outbreak of respiratory disease triggers public concern regarding triage funding."),
                    category = json.optString("category", "Healthcare"),
                    optionA = CommandOption(
                        text = json.getJSONObject("optionA").getString("text"),
                        outcomeSummary = json.getJSONObject("optionA").getString("outcomeSummary")
                    ),
                    optionB = CommandOption(
                        text = json.getJSONObject("optionB").getString("text"),
                        outcomeSummary = json.getJSONObject("optionB").getString("outcomeSummary")
                    ),
                    optionC = CommandOption(
                        text = json.getJSONObject("optionC").getString("text"),
                        outcomeSummary = json.getJSONObject("optionC").getString("outcomeSummary")
                    )
                )

                withContext(Dispatchers.Main) {
                    _currentIssue.value = issue
                    _isAILoading.value = false
                }
            } catch (e: Exception) {
                Log.e("PoliticsHandler", "Issue generation failed", e)
                withContext(Dispatchers.Main) {
                    _isAILoading.value = false
                    _currentIssue.value = DailyIssue(
                        title = "Municipal Emergency Relief Levy",
                        description = "Several audit agencies report deficits. Progressive factions request a 0.5% medical tax surcharge to subsidize regional hospital trauma beds.",
                        category = "Budgeting",
                        optionA = CommandOption(
                            text = "Enact Local Levy Surcharge",
                            outcomeSummary = "Directly grants +R12,000 to hospital clinic, boosts progressive support +10%, but decreases corporate executive trust -12%."
                        ),
                        optionB = CommandOption(
                            text = "Veto Levy & Retrench Hospital Budgets",
                            outcomeSummary = "Saves public budget, corporate elites trust +8%, but health guild trust drops -10%."
                        ),
                        optionC = CommandOption(
                            text = "Channel local infrastructure funds directly to medical supplies",
                            outcomeSummary = "Restocks hospital salines and syringes instantly, increases medical prestige, but triggers infrastructure complaints."
                        )
                    )
                }
            }
        }
    }

    fun submitDecreeDecision(chosenText: String) {
        _isAILoading.value = true
        parentViewModel.viewModelScope.launch {
            try {
                val office = _currentOffice.value
                val issue = _currentIssue.value ?: return@launch

                val sysPrompt = """
                    You are a Political Narrative Resolution Oracle.
                    The leader holds the high office of '$office'.
                    They faced this challenge:
                    Title: ${issue.title}
                    Description: ${issue.description}

                    They resolved it by declaring: "$chosenText"
                    Active cabinet help: ${_hiredStaffIds.value.joinToString(", ")}.

                    Evaluate the political consequences, journalist editorials, and hospital database adjustments!
                    Write a detailed journalist frontpage report (3 short paragraphs) describing the debate and impact.
                    Specify numerical changes for all factions:
                    1. Working Class Support (-15 to +15)
                    2. Medical Guild Support (-15 to +15)
                    3. Corporate Executive Support (-15 to +15)
                    4. National Patriots Support (-15 to +15)
                    5. Overall Approval rating change (-15 to +15)
                    6. Campaign fund change (-5000.0 to +10000.0)
                    7. Clinical balance modification (positive values add cash to the medical hospital, e.g. R5000 to R20000)
                    8. Medicine stock arrivals (syringes, saline, adrenaline, reagents, meds).

                    Format your response in STRICT valid JSON:
                    {
                      "newsArticle": "Sovereign decree executed...",
                      "workingClassDelta": 4,
                      "medicalGuildDelta": 6,
                      "corporateExecutiveDelta": -5,
                      "nationalPatriotsDelta": 2,
                      "approvalChange": 3,
                      "fundsChange": 1500.0,
                      "clinicBalanceChange": 10000.0,
                      "clinicStockChange": { "syringes": 10, "saline": 5, "adrenaline": 2 }
                    }
                """.trimIndent()

                val resText = queryGeminiRaw(sysPrompt)
                val cleanJson = extractJsonFromString(resText)
                val json = JSONObject(cleanJson)

                val article = json.optString("newsArticle", "The decree was executed. Broad debates ensued across capital chambers.")
                val wcD = json.optInt("workingClassDelta", 2)
                val mgD = json.optInt("medicalGuildDelta", 2)
                val ceD = json.optInt("corporateExecutiveDelta", 2)
                val npD = json.optInt("nationalPatriotsDelta", 2)
                var approvalChange = json.optInt("approvalChange", 3)
                val fundsChange = json.optDouble("fundsChange", 0.0)
                val clinicBalanceChange = json.optDouble("clinicBalanceChange", 0.0)

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

                // Press secretary multiplier
                if (_hiredStaffIds.value.contains("press_sec") && approvalChange > 0) {
                    approvalChange = (approvalChange * 1.25).toInt()
                }

                withContext(Dispatchers.Main) {
                    _workingClassSupport.value = (_workingClassSupport.value + wcD).coerceIn(0, 100)
                    _medicalGuildSupport.value = (_medicalGuildSupport.value + mgD).coerceIn(0, 100)
                    _corporateExecutiveSupport.value = (_corporateExecutiveSupport.value + ceD).coerceIn(0, 100)
                    _nationalPatriotsSupport.value = (_nationalPatriotsSupport.value + npD).coerceIn(0, 100)
                    _approvalRating.value = (_approvalRating.value + approvalChange).coerceIn(0, 100)
                    _campaignFunds.value += fundsChange

                    val factionImpactString = "WorkClass: ${if (wcD >= 0) "+" else ""}$wcD%, MedGuild: ${if (mgD >= 0) "+" else ""}$mgD%, CorpElites: ${if (ceD >= 0) "+" else ""}$ceD%, Patriots: ${if (npD >= 0) "+" else ""}$npD%"

                    // Seamless writeback to clinic states
                    if (clinicBalanceChange != 0.0 || stockMap.isNotEmpty()) {
                        parentViewModel.viewModelScope.launch {
                            val newClinicBal = parentViewModel.clinicBalance.value + clinicBalanceChange
                            settingsDataStore.updateClinicStats(newClinicBal, parentViewModel.reputationStars.value)

                            val syringes = (parentViewModel.syringeStock.value + (stockMap["syringes"] ?: 0)).coerceAtLeast(0)
                            val saline = (parentViewModel.salineStock.value + (stockMap["saline"] ?: 0)).coerceAtLeast(0)
                            val adrenaline = (parentViewModel.adrenalineStock.value + (stockMap["adrenaline"] ?: 0)).coerceAtLeast(0)
                            val reagents = (parentViewModel.reagentsStock.value + (stockMap["reagents"] ?: 0)).coerceAtLeast(0)
                            val meds = (parentViewModel.medsStock.value + (stockMap["meds"] ?: 0)).coerceAtLeast(0)

                            settingsDataStore.saveInventory(syringes, saline, adrenaline, reagents, meds)
                        }
                    }

                    val outcome = DecisionOutcome(
                        issueTitle = issue.title,
                        chosenOption = chosenText,
                        newsArticle = article,
                        approvalChange = approvalChange,
                        fundsChange = fundsChange,
                        prestigeChange = 2,
                        clinicBalanceChange = clinicBalanceChange,
                        clinicStockChange = stockMap,
                        factionDeltaNarrative = factionImpactString
                    )

                    _recentOutcome.value = outcome
                    _outcomesHistory.value = listOf(outcome) + _outcomesHistory.value
                    _currentIssue.value = null
                    _isAILoading.value = false
                }
            } catch (e: Exception) {
                Log.e("PoliticsHandler", "Decree resolution failed", e)
                withContext(Dispatchers.Main) {
                    _isAILoading.value = false
                    _recentOutcome.value = DecisionOutcome(
                        issueTitle = _currentIssue.value?.title ?: "Action Council Decree",
                        chosenOption = chosenText,
                        newsArticle = "📰 DECREE ENACTED!\n\nThe administration signed: $chosenText. Public ledger registers standard compliance levels.",
                        approvalChange = 3,
                        fundsChange = 100.0,
                        prestigeChange = 1,
                        factionDeltaNarrative = "Broad factional reactions are split."
                    )
                    _currentIssue.value = null
                }
            }
        }
    }

    // --- Executive Sovereign Office Decrees (Specific powers) ---

    fun executeSovereignDecree(powerId: String) {
        val level = _officeLevel.value
        _isAILoading.value = true

        parentViewModel.viewModelScope.launch {
            try {
                val outcome = when {
                    powerId.startsWith("mayor_") -> {
                        mayorOfficeHandler.executeDecree(powerId)
                    }
                    powerId.startsWith("gov_") -> {
                        governorOfficeHandler.executeDecree(powerId)
                    }
                    powerId.startsWith("leg_") || powerId == "sen_filibuster" -> {
                        legislatorOfficeHandler.executeDecree(powerId)
                    }
                    powerId.startsWith("pres_") -> {
                        presidentOfficeHandler.executeDecree(powerId)
                    }
                    else -> {
                        DecisionOutcome(
                            issueTitle = "⚡ ADMINISTRATIVE ORDINANCE",
                            chosenOption = "Executive Seal",
                            newsArticle = "🛎️ EXECUTIVE BRIEF:\n\nSovereign action executed successfully under standard regulations.",
                            approvalChange = 2,
                            fundsChange = 0.0,
                            prestigeChange = 1
                        )
                    }
                }

                withContext(Dispatchers.Main) {
                    _recentOutcome.value = outcome
                    _outcomesHistory.value = listOf(outcome) + _outcomesHistory.value
                    _isAILoading.value = false
                }
            } catch (e: Exception) {
                Log.e("PoliticsHandler", "Sovereign power call failed", e)
                withContext(Dispatchers.Main) {
                    _isAILoading.value = false
                    _errorMessage.value = "Failed to coordinate administrative order. Key services offline."
                }
            }
        }
    }

    suspend fun queryGeminiRawExternal(systemPrompt: String): String {
        return queryGeminiRaw(systemPrompt)
    }

    fun extractJsonFromStringExternal(input: String): String {
        return extractJsonFromString(input)
    }

    fun adjustSupportStates(wc: Int, mg: Int, ce: Int, np: Int, app: Int, funds: Double) {
        _workingClassSupport.value = (_workingClassSupport.value + wc).coerceIn(0, 100)
        _medicalGuildSupport.value = (_medicalGuildSupport.value + mg).coerceIn(0, 100)
        _corporateExecutiveSupport.value = (_corporateExecutiveSupport.value + ce).coerceIn(0, 100)
        _nationalPatriotsSupport.value = (_nationalPatriotsSupport.value + np).coerceIn(0, 100)
        _approvalRating.value = (_approvalRating.value + app).coerceIn(0, 100)
        _campaignFunds.value = (_campaignFunds.value + funds).coerceAtLeast(0.0)
    }

    // --- Sponsoring Custom Legislative Bills Desk ---

    fun draftAndSponsorBill(title: String, sector: String, allocation: String, taxCost: String) {
        val entryFee = 2500.0
        if (_campaignFunds.value < entryFee) {
            _errorMessage.value = "Sponsoring bills require $2,500 legal filing fees. Your ledger lacks funding."
            return
        }

        _campaignFunds.value -= entryFee
        _isAILoading.value = true

        parentViewModel.viewModelScope.launch {
            try {
                val sysPrompt = """
                    You are a Parliamentary Committee Analyst evaluating a custom legislative bill draft.
                    Bill details:
                    - Title: "$title"
                    - Target Sector: "$sector"
                    - Tax / Cost Priority: "$taxCost"
                    - Primary Beneficiary sector: "$allocation"

                    Current Faction Approval States:
                    - Working Class: ${_workingClassSupport.value}%
                    - Medical Guild: ${_medicalGuildSupport.value}%
                    - Corporate Executives: ${_corporateExecutiveSupport.value}%
                    - National Patriots: ${_nationalPatriotsSupport.value}%

                    Simulate the legislative floor vote details. Determine:
                    1. Passage result: true or false.
                    2. Voting tallies (e.g. House count like '218-192' and Senate count like '52-48').
                    3. Write a vivid newspaper journalism excerpt (3 paragraphs) reporting on the back-and-forth debates, the primary caucus champions, the lobbying friction, and the final floor drama!
                    4. Calculate direct clinical rewards representing grants/subsidies awarded if PASSED (e.g., clinic funds reward between R10000.0 and R50000.0) and stocking rewards of standard resources.

                    Format response in absolutely strict valid JSON:
                    {
                      "passed": true,
                      "tallyHouse": "224-211",
                      "tallySenate": "54-46",
                      "journalismExcerpt": "The legislative corridors echoed as...",
                      "dynamicClinicFundsGrant": 28000.0,
                      "dynamicStocksReward": { "syringes": 20, "saline": 15, "meds": 8 }
                    }
                """.trimIndent()

                val resText = queryGeminiRaw(sysPrompt)
                val cleanJson = extractJsonFromString(resText)
                val json = JSONObject(cleanJson)

                val passed = json.optBoolean("passed", false)
                val tHouse = json.optString("tallyHouse", "218-217")
                val tSenate = json.optString("tallySenate", "51-49")
                val excerpt = json.optString("journalismExcerpt", "Debates finished on the capital floor.")
                val fundsGrant = json.optDouble("dynamicClinicFundsGrant", 0.0)

                val stockObj = json.optJSONObject("dynamicStocksReward")
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
                    _recentBillResult.value = LegislativeBillResult(
                        billTitle = title,
                        passed = passed,
                        tallyHouse = tHouse,
                        tallySenate = tSenate,
                        journalismExcerpt = excerpt,
                        dynamicClinicFundsGrant = fundsGrant,
                        dynamicStocksReward = stockMap
                    )

                    // Execute downstream effects on success
                    if (passed) {
                        parentViewModel.updatePoliticalPrestige((parentViewModel.politicalPrestige.value + 12).coerceAtMost(100))
                        _approvalRating.value = (_approvalRating.value + 6).coerceAtMost(100)

                        // Adjust faction stands
                        if (allocation == "Working Class / Citizens") {
                            _workingClassSupport.value = (_workingClassSupport.value + 15).coerceAtMost(100)
                            _corporateExecutiveSupport.value = (_corporateExecutiveSupport.value - 5).coerceAtLeast(0)
                        } else if (allocation == "Hospitals / Clinicians") {
                            _medicalGuildSupport.value = (_medicalGuildSupport.value + 18).coerceAtMost(100)
                            _workingClassSupport.value = (_workingClassSupport.value + 6).coerceAtMost(100)
                        }

                        // Write proceeds back to clinic database!
                        if (fundsGrant != 0.0 || stockMap.isNotEmpty()) {
                            parentViewModel.viewModelScope.launch {
                                val currentClinicBal = parentViewModel.clinicBalance.value
                                settingsDataStore.updateClinicStats(currentClinicBal + fundsGrant, parentViewModel.reputationStars.value)

                                val syringes = (parentViewModel.syringeStock.value + (stockMap["syringes"] ?: 0)).coerceAtLeast(0)
                                val saline = (parentViewModel.salineStock.value + (stockMap["saline"] ?: 0)).coerceAtLeast(0)
                                val adrenaline = (parentViewModel.adrenalineStock.value + (stockMap["adrenaline"] ?: 0)).coerceAtLeast(0)
                                val reagents = (parentViewModel.reagentsStock.value + (stockMap["reagents"] ?: 0)).coerceAtLeast(0)
                                val meds = (parentViewModel.medsStock.value + (stockMap["meds"] ?: 0)).coerceAtLeast(0)

                                settingsDataStore.saveInventory(syringes, saline, adrenaline, reagents, meds)
                            }
                        }
                    } else {
                        // Voted down
                        parentViewModel.updatePoliticalPrestige((parentViewModel.politicalPrestige.value - 6).coerceAtLeast(0))
                        _approvalRating.value = (_approvalRating.value - 3).coerceAtLeast(0)
                    }

                    _isAILoading.value = false
                }
            } catch (e: Exception) {
                Log.e("PoliticsHandler", "Bill vote failed", e)
                withContext(Dispatchers.Main) {
                    _isAILoading.value = false
                    _recentBillResult.value = LegislativeBillResult(
                        billTitle = title,
                        passed = true,
                        tallyHouse = "220-215",
                        tallySenate = "52-48",
                        journalismExcerpt = "After robust floor contentions, the chamber passed the medical legislation with razor-thin margins. Local healthcare entities express alignment with state subsidies.",
                        dynamicClinicFundsGrant = 15000.0,
                        dynamicStocksReward = mapOf("syringes" to 10)
                    )
                }
            }
        }
    }

    // --- Helper Network Functions

    private suspend fun queryGeminiRaw(systemPrompt: String): String = withContext(Dispatchers.IO) {
        val prov = parentViewModel.provider.value
        val rawUserKey = parentViewModel.apiKey.value ?: ""
        val customUrl = parentViewModel.customEndpoint.value
        val activeKey = parentViewModel.resolveActiveApiKey(prov, rawUserKey, customUrl)
        val modelName = parentViewModel.model.value

        val activeUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$activeKey"

        val contents = listOf(GeminiContent("user", listOf(GeminiPart(text = systemPrompt))))
        val request = GeminiRequest(
            contents = contents,
            generationConfig = GeminiGenerationConfig(temperature = 0.8),
            systemInstruction = null
        )

        val response = RetrofitClient.service.callGemini(activeUrl, request)
        response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
    }

    private fun extractJsonFromString(input: String): String {
        val matcher = Pattern.compile("```json(.*?)```", Pattern.DOTALL).matcher(input)
        if (matcher.find()) {
            return matcher.group(1).trim()
        }
        val braceStart = input.indexOf('{')
        val braceEnd = input.lastIndexOf('}')
        if (braceStart != -1 && braceEnd != -1 && braceEnd > braceStart) {
            return input.substring(braceStart, braceEnd + 1)
        }
        return input.trim()
    }
}
