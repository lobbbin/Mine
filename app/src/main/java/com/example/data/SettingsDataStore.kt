package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "clinical_engine_settings")

class SettingsDataStore(private val context: Context) {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val policiesType = Types.newParameterizedType(List::class.java, HealthPolicy::class.java)
    private val policiesAdapter = moshi.adapter<List<HealthPolicy>>(policiesType)

    companion object {
        private val API_KEY_KEY = stringPreferencesKey("api_key")
        private val PROVIDER_KEY = stringPreferencesKey("provider")
        private val MODEL_KEY = stringPreferencesKey("model")
        private val CUSTOM_ENDPOINT_KEY = stringPreferencesKey("custom_endpoint")
        private val ROTATOR_KEYS_JSON_KEY = stringPreferencesKey("rotator_keys_json")
        private val PREF_SPECIALTY_KEY = stringPreferencesKey("pref_specialty")
        private val PREF_SEVERITY_KEY = stringPreferencesKey("pref_severity")
        private val CLINIC_BALANCE_KEY = androidx.datastore.preferences.core.doublePreferencesKey("clinic_balance")
        private val REPUTATION_STARS_KEY = androidx.datastore.preferences.core.floatPreferencesKey("reputation_stars")
        
        private val CONSULTATION_FEE_KEY = androidx.datastore.preferences.core.doublePreferencesKey("consultation_fee")
        private val LAB_COST_KEY = androidx.datastore.preferences.core.doublePreferencesKey("lab_cost")
        private val SPECIALIST_COST_KEY = androidx.datastore.preferences.core.doublePreferencesKey("specialist_cost")
        private val DOCTOR_XP_KEY = androidx.datastore.preferences.core.longPreferencesKey("doctor_xp")
        
        private val INVENTORY_SYRINGES_KEY = androidx.datastore.preferences.core.intPreferencesKey("inv_syringes")
        private val INVENTORY_SALINE_KEY = androidx.datastore.preferences.core.intPreferencesKey("inv_saline")
        private val INVENTORY_ADRENALINE_KEY = androidx.datastore.preferences.core.intPreferencesKey("inv_adrenaline")
        private val INVENTORY_REAGENTS_KEY = androidx.datastore.preferences.core.intPreferencesKey("inv_reagents")
        private val INVENTORY_MEDS_KEY = androidx.datastore.preferences.core.intPreferencesKey("inv_meds")
        
        private val CURRENT_DAY_KEY = androidx.datastore.preferences.core.intPreferencesKey("current_day")
        private val PATIENTS_SEEN_TODAY_KEY = androidx.datastore.preferences.core.intPreferencesKey("patients_seen_today")
        private val DAILY_REVENUE_KEY = androidx.datastore.preferences.core.doublePreferencesKey("daily_revenue")
        private val DAILY_EXPENSES_KEY = androidx.datastore.preferences.core.doublePreferencesKey("daily_expenses")

        private val ACTIVE_POLICIES_KEY = stringPreferencesKey("political_active_policies")
        private val PRESIDENT_NAME_KEY = stringPreferencesKey("political_president_name")
        private val PRESIDENT_PARTY_KEY = stringPreferencesKey("political_president_party")
        private val PRESIDENT_APPROVAL_KEY = androidx.datastore.preferences.core.intPreferencesKey("political_president_approval")
        private val POLITICAL_PRESTIGE_KEY = androidx.datastore.preferences.core.intPreferencesKey("political_prestige")
        private val COUNTRY_NAME_KEY = stringPreferencesKey("political_country_name")
        private val STICKY_POLITICIAN_SICK_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("sticky_politician_sick")
        private val IS_BASIC_MODE_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("is_basic_mode")
        private val HAS_CHOSEN_MODE_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("has_chosen_mode")
        private val CURRENCY_SYMBOL_KEY = stringPreferencesKey("custom_currency_symbol")
        private val CURRENCY_CODE_KEY = stringPreferencesKey("custom_currency_code")
        private val UI_FONT_SCALE_KEY = androidx.datastore.preferences.core.floatPreferencesKey("ui_font_scale")
        private val ROTATOR_ENABLED_MODELS_KEY = stringPreferencesKey("rotator_enabled_models_json")
        private val ROTATOR_CUSTOM_MODELS_KEY = stringPreferencesKey("rotator_custom_models_json")
    }

    val uiFontScaleFlow: Flow<Float> = context.dataStore.data.map { it[UI_FONT_SCALE_KEY] ?: 1.0f }

    suspend fun saveUiFontScale(scale: Float) {
        context.dataStore.edit { preferences ->
            preferences[UI_FONT_SCALE_KEY] = scale
        }
    }

    val currencySymbolFlow: Flow<String> = context.dataStore.data.map { it[CURRENCY_SYMBOL_KEY] ?: "$" }
    val currencyCodeFlow: Flow<String> = context.dataStore.data.map { it[CURRENCY_CODE_KEY] ?: "USD" }

    suspend fun saveCurrency(symbol: String, code: String) {
        context.dataStore.edit { preferences ->
            preferences[CURRENCY_SYMBOL_KEY] = symbol
            preferences[CURRENCY_CODE_KEY] = code
        }
    }

    val isBasicModeFlow: Flow<Boolean> = context.dataStore.data.map { it[IS_BASIC_MODE_KEY] ?: false }
    val hasChosenModeFlow: Flow<Boolean> = context.dataStore.data.map { it[HAS_CHOSEN_MODE_KEY] ?: false }

    suspend fun saveModeSelection(isBasic: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_BASIC_MODE_KEY] = isBasic
            preferences[HAS_CHOSEN_MODE_KEY] = true
        }
    }

    val doctorXpFlow: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[DOCTOR_XP_KEY] ?: 0L
        }

    suspend fun addXp(xpPoints: Long) {
        context.dataStore.edit { preferences ->
            val currentXp = preferences[DOCTOR_XP_KEY] ?: 0L
            preferences[DOCTOR_XP_KEY] = currentXp + xpPoints
        }
    }

    val clinicBalanceFlow: Flow<Double> = context.dataStore.data
        .map { preferences ->
            preferences[CLINIC_BALANCE_KEY] ?: 50000.0 // Starting balance of $50,000
        }

    val reputationStarsFlow: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[REPUTATION_STARS_KEY] ?: 3.5f // Start with 3.5 stars
        }

    val consultationFeeFlow: Flow<Double> = context.dataStore.data
        .map { preferences ->
            preferences[CONSULTATION_FEE_KEY] ?: 850.0 // Default $850
        }

    val labCostFlow: Flow<Double> = context.dataStore.data
        .map { preferences ->
            preferences[LAB_COST_KEY] ?: 150.0 // Default $150
        }

    val specialistCostFlow: Flow<Double> = context.dataStore.data
        .map { preferences ->
            preferences[SPECIALIST_COST_KEY] ?: 800.0 // Default $800
        }

    suspend fun savePricing(consultFee: Double, labCost: Double, specialistCost: Double) {
        context.dataStore.edit { preferences ->
            preferences[CONSULTATION_FEE_KEY] = consultFee
            preferences[LAB_COST_KEY] = labCost
            preferences[SPECIALIST_COST_KEY] = specialistCost
        }
    }

    suspend fun updateClinicStats(newBalance: Double, newReputation: Float) {
        context.dataStore.edit { preferences ->
            preferences[CLINIC_BALANCE_KEY] = newBalance
            preferences[REPUTATION_STARS_KEY] = newReputation
        }
    }

    val inventorySyringesFlow: Flow<Int> = context.dataStore.data.map { it[INVENTORY_SYRINGES_KEY] ?: 42 }
    val inventorySalineFlow: Flow<Int> = context.dataStore.data.map { it[INVENTORY_SALINE_KEY] ?: 8 }
    val inventoryAdrenalineFlow: Flow<Int> = context.dataStore.data.map { it[INVENTORY_ADRENALINE_KEY] ?: 5 }
    val inventoryReagentsFlow: Flow<Int> = context.dataStore.data.map { it[INVENTORY_REAGENTS_KEY] ?: 25 }
    val inventoryMedsFlow: Flow<Int> = context.dataStore.data.map { it[INVENTORY_MEDS_KEY] ?: 12 }

    suspend fun saveInventory(syringes: Int, saline: Int, adrenaline: Int, reagents: Int, meds: Int) {
        context.dataStore.edit { preferences ->
            preferences[INVENTORY_SYRINGES_KEY] = syringes
            preferences[INVENTORY_SALINE_KEY] = saline
            preferences[INVENTORY_ADRENALINE_KEY] = adrenaline
            preferences[INVENTORY_REAGENTS_KEY] = reagents
            preferences[INVENTORY_MEDS_KEY] = meds
        }
    }

    val apiKeyFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[API_KEY_KEY]
        }

    val providerFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PROVIDER_KEY] ?: "Google"
        }

    val modelFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[MODEL_KEY] ?: "gemini-3.5-flash"
        }

    val customEndpointFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[CUSTOM_ENDPOINT_KEY] ?: ""
        }

    val prefSpecialtyFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PREF_SPECIALTY_KEY] ?: "All"
        }

    val prefSeverityFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PREF_SEVERITY_KEY] ?: "All"
        }

    suspend fun saveSettings(apiKey: String, provider: String, model: String, customEndpoint: String) {
        context.dataStore.edit { preferences ->
            preferences[API_KEY_KEY] = apiKey
            preferences[PROVIDER_KEY] = provider
            preferences[MODEL_KEY] = model
            preferences[CUSTOM_ENDPOINT_KEY] = customEndpoint
        }
    }

    val rotatorKeysFlow: Flow<Map<String, String>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[ROTATOR_KEYS_JSON_KEY] ?: ""
            if (json.isBlank()) {
                emptyMap()
            } else {
                try {
                    val type = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
                    val adapter = moshi.adapter<Map<String, String>>(type)
                    adapter.fromJson(json) ?: emptyMap()
                } catch (e: Exception) {
                    emptyMap()
                }
            }
        }

    suspend fun saveRotatorKeys(keys: Map<String, String>) {
        context.dataStore.edit { preferences ->
            val type = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
            val adapter = moshi.adapter<Map<String, String>>(type)
            preferences[ROTATOR_KEYS_JSON_KEY] = adapter.toJson(keys)
        }
    }

    val rotatorEnabledModelsFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[ROTATOR_ENABLED_MODELS_KEY] ?: ""
            if (json.isBlank()) {
                emptySet()
            } else {
                try {
                    val type = Types.newParameterizedType(Set::class.java, String::class.java)
                    val adapter = moshi.adapter<Set<String>>(type)
                    adapter.fromJson(json) ?: emptySet()
                } catch (e: Exception) {
                    emptySet()
                }
            }
        }

    suspend fun saveRotatorEnabledModels(models: Set<String>) {
        context.dataStore.edit { preferences ->
            val type = Types.newParameterizedType(Set::class.java, String::class.java)
            val adapter = moshi.adapter<Set<String>>(type)
            preferences[ROTATOR_ENABLED_MODELS_KEY] = adapter.toJson(models)
        }
    }

    val rotatorCustomModelsFlow: Flow<Map<String, List<String>>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[ROTATOR_CUSTOM_MODELS_KEY] ?: ""
            if (json.isBlank()) {
                emptyMap()
            } else {
                try {
                    val listType = Types.newParameterizedType(List::class.java, String::class.java)
                    val mapType = Types.newParameterizedType(Map::class.java, String::class.java, listType)
                    val adapter = moshi.adapter<Map<String, List<String>>>(mapType)
                    adapter.fromJson(json) ?: emptyMap()
                } catch (e: Exception) {
                    emptyMap()
                }
            }
        }

    suspend fun saveRotatorCustomModels(customModels: Map<String, List<String>>) {
        context.dataStore.edit { preferences ->
            val listType = Types.newParameterizedType(List::class.java, String::class.java)
            val mapType = Types.newParameterizedType(Map::class.java, String::class.java, listType)
            val adapter = moshi.adapter<Map<String, List<String>>>(mapType)
            preferences[ROTATOR_CUSTOM_MODELS_KEY] = adapter.toJson(customModels)
        }
    }

    suspend fun saveCurriculumPresets(specialty: String, severity: String) {
        context.dataStore.edit { preferences ->
            preferences[PREF_SPECIALTY_KEY] = specialty
            preferences[PREF_SEVERITY_KEY] = severity
        }
    }

    val currentDayFlow: Flow<Int> = context.dataStore.data.map { it[CURRENT_DAY_KEY] ?: 1 }
    
    suspend fun getCurrentDay(): Int = currentDayFlow.first()
    val patientsSeenTodayFlow: Flow<Int> = context.dataStore.data.map { it[PATIENTS_SEEN_TODAY_KEY] ?: 0 }
    val dailyRevenueFlow: Flow<Double> = context.dataStore.data.map { it[DAILY_REVENUE_KEY] ?: 0.0 }
    val dailyExpensesFlow: Flow<Double> = context.dataStore.data.map { it[DAILY_EXPENSES_KEY] ?: 0.0 }

    suspend fun addDailyRevenue(amount: Double) {
        context.dataStore.edit { preferences ->
            val prev = preferences[DAILY_REVENUE_KEY] ?: 0.0
            preferences[DAILY_REVENUE_KEY] = prev + amount
        }
    }

    suspend fun addDailyExpenses(amount: Double) {
        context.dataStore.edit { preferences ->
            val prev = preferences[DAILY_EXPENSES_KEY] ?: 0.0
            preferences[DAILY_EXPENSES_KEY] = prev + amount
        }
    }

    suspend fun incrementPatientsSeenToday() {
        context.dataStore.edit { preferences ->
            val prev = preferences[PATIENTS_SEEN_TODAY_KEY] ?: 0
            preferences[PATIENTS_SEEN_TODAY_KEY] = prev + 1
        }
    }

    suspend fun advanceDay() {
        context.dataStore.edit { preferences ->
            val prevDay = preferences[CURRENT_DAY_KEY] ?: 1
            preferences[CURRENT_DAY_KEY] = prevDay + 1
            preferences[PATIENTS_SEEN_TODAY_KEY] = 0
            preferences[DAILY_REVENUE_KEY] = 0.0
            preferences[DAILY_EXPENSES_KEY] = 0.0
        }
    }

    suspend fun advanceDays(days: Int) {
        if (days <= 0) return
        context.dataStore.edit { preferences ->
            val prevDay = preferences[CURRENT_DAY_KEY] ?: 1
            preferences[CURRENT_DAY_KEY] = prevDay + days
            preferences[PATIENTS_SEEN_TODAY_KEY] = 0
            preferences[DAILY_REVENUE_KEY] = 0.0
            preferences[DAILY_EXPENSES_KEY] = 0.0
        }
    }

    suspend fun setCurrentDay(day: Int) {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_DAY_KEY] = day
        }
    }

    val countryNameFlow: Flow<String> = context.dataStore.data.map { it[COUNTRY_NAME_KEY] ?: "Federal Republic of Elysium" }
    val presidentNameFlow: Flow<String> = context.dataStore.data.map { it[PRESIDENT_NAME_KEY] ?: "President Arthur Vance" }
    val presidentPartyFlow: Flow<String> = context.dataStore.data.map { it[PRESIDENT_PARTY_KEY] ?: "Progressive Healthcare Alliance" }
    val presidentApprovalFlow: Flow<Int> = context.dataStore.data.map { it[PRESIDENT_APPROVAL_KEY] ?: 68 }
    val politicalPrestigeFlow: Flow<Int> = context.dataStore.data.map { it[POLITICAL_PRESTIGE_KEY] ?: 50 }
    val stickyPoliticianSickFlow: Flow<Boolean> = context.dataStore.data.map { it[STICKY_POLITICIAN_SICK_KEY] ?: false }

    val activePoliciesFlow: Flow<List<HealthPolicy>> = context.dataStore.data.map { preferences ->
        val json = preferences[ACTIVE_POLICIES_KEY] ?: ""
        if (json.isBlank()) {
            listOf(
                HealthPolicy(
                    id = "starter_1",
                    title = "Sovereign Biasecurity Telemetry Directive (Act 1)",
                    summary = "Mandates proactive biological auditing and citizen telemetry registration at all independent clinical consultation units inside the sovereign territory to feed the Central Bio-Risk Security databases.",
                    extendedClauses = listOf(
                        "Clause 1.1: Every certified clinician operating within the sovereign territory MUST audit and register a complete biometric vitals telemetry set (blood pressure and temperature) for every registered diagnostic encounter.",
                        "Clause 1.2: Failing to log requisite biological telemetry prior to issuing professional pharmaceutical prescriptions constitutes High Regulatory Malfeasance, rendering the clinician subject to primary Supreme Judiciary subpoenaes and heavy liquid assets confiscation."
                    ),
                    economicImpact = "Imposes minor equipment and compliance overhead. Powers state databases, stabilizing the national bio-security risk index.",
                    clinicalRule = "Order vitals assessment.",
                    status = "Approved"
                )
            )
        } else {
            try {
                policiesAdapter.fromJson(json) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun saveCountryName(name: String) {
        context.dataStore.edit { it[COUNTRY_NAME_KEY] = name }
    }

    suspend fun savePresidentName(name: String) {
        context.dataStore.edit { it[PRESIDENT_NAME_KEY] = name }
    }

    suspend fun savePresidentParty(party: String) {
        context.dataStore.edit { it[PRESIDENT_PARTY_KEY] = party }
    }

    suspend fun savePresidentApproval(approval: Int) {
        context.dataStore.edit { it[PRESIDENT_APPROVAL_KEY] = approval }
    }

    suspend fun savePoliticalPrestige(prestige: Int) {
        context.dataStore.edit { it[POLITICAL_PRESTIGE_KEY] = prestige }
    }

    suspend fun saveStickyPoliticianSick(sick: Boolean) {
        context.dataStore.edit { it[STICKY_POLITICIAN_SICK_KEY] = sick }
    }

    suspend fun saveActivePolicies(policies: List<HealthPolicy>) {
        context.dataStore.edit { it[ACTIVE_POLICIES_KEY] = policiesAdapter.toJson(policies) }
    }
}

