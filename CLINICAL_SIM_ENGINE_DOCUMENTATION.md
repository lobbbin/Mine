# Clinical Sim Engine - Documentation

Welcome to the comprehensive technical and operational documentation for the **Clinical Sim Engine** (Practice Engine). This offline-first and AI-powered simulation suite is engineered for medical students, interns, and clinicians as an interactive OSCE (Objective Structured Clinical Examination) and clinic administration trainer.

---

## 1. Application Architecture

The Clinical Sim Engine leverages a modern, robust, and clean Android architecture adopting the **Model-View-ViewModel (MVVM)** pattern alongside Jetpack Compose, Coroutines, Flow, and Material Design 3.

```
┌────────────────────────────────────────────────────────┐
│                      UI / View                         │
│   (DashboardScreen.kt, SettingsScreen.kt, Jetpack)     │
└───────────────────────────┬────────────────────────────┘
                            │ Observation (StateFlow)
                            ▼
┌────────────────────────────────────────────────────────┐
│                   SimulationViewModel                  │
│       (Manages state, runs virtual OSCE clinic)       │
└───────────────────────────┬────────────────────────────┘
                            ├────────────────────────────┐
                            ▼ Data Operations            ▼ API Requests
┌────────────────────────────────────────────────────────┐┌────────────────────────────────────────────────────────┐
│                  EncounterRepository                   ││                     AIService (Ktor)                   │
├────────────────────────────────────────────────────────┤├────────────────────────────────────────────────────────┤
│                      EncounterDao                      ││                     RetrofitClient                     │
├────────────────────────────────────────────────────────┤└────────────────────────────────────────────────────────┤
│             AppDatabase (Room SQLite v4)               ││             Multi-LLM Connector                       │
└────────────────────────────────────────────────────────┘│   (Google Gemini, Anthropic, OpenAI Stream)           │
                                                          └────────────────────────────────────────────────────────┘
```

---

## 2. Comprehensive Feature List

### 🩺 Bedside Clinical Hub
*   **Live Vitals Monitoring:** Displays real-time assessment values including Blood Pressure (BP), Heart Rate (HR), Core Temperature (°C), Respiratory Rate (RR), and Oxygen Saturation ($SpO_2$) mapped directly to the patient's acute status.
*   **Demographic Profile:** Displays high-fidelity patient cards containing name, age, gender, occupation, and social history context.
*   **Acute Interventions:** Enables live bedside actions (e.g., administering fluids, oxygen therapy, setting up standard monitoring) which dynamically affect the patient's immediate physiological stability and emotional mood.

### 🤖 Multi-LLM AI OSCE Simulation
*   **Clinical Roleplaying Engine:** Conduct dynamic, structured medical dialogue with the simulated patient. The AI represents the patient’s voice, symptomatology, history, and physical response patterns realistically.
*   **Flexible API Routing:** Complete integration with **Google Gemini**, **Anthropic**, and **OpenAI**. Supports custom parameters (response MIME type, temperature controls, context token adjustments up to $8192$ tokens, and historic conversation bounds up to $100$ turns).
*   **Case Generation & Validation:** Uses deep system prompts to craft custom cases across diverse specialties (e.g., Pediatrics, Emergency Medicine, Cardiology, Neurology) containing hidden clinical diagnoses, pathophysiology, and expected laboratory profiles.

### 📊 Clinic Operations & Resource Management
*   **Dynamic Inventory Levels:** Simulates practical storage and stock level constraints:
    *   *Syringes*
    *   *Saline Bags*
    *   *Adrenaline Vials*
    *   *Lab Reagents*
    *   *General Medications*
*   **Procurement Logic:** Purchase consumables through the logistics panel utilizing the clinic's operating capital.
*   **Financial Balance Sheets:** Tracks daily revenue earned from patient appointments and clinical procedures offset by expenses incurred (e.g., lab test processing, medical stock consumption, acute treatments).

### 🏆 Career Progression & Ranks
*   **XP Progression:** Gain experience points (XP) dynamically upon completing patient cases, validating primary diagnoses, and administering correct interventions.
*   **Clinical Grading:** Computes structured OSCE summaries with an overall clinical grade, identifying diagnostic insights, differential validation accuracy, and execution of evidence-based medicine.
*   **Reputation Rating:** Tracks continuous patient trust from $1$ to $5$ stars, shifting based on critical bedside communications, patient comfort, and procedural accuracy.

### 📄 Financial Ledger & PDF Export
*   **Direct-to-Storage PDF Generation:** Compiles daily clinical registries, financial ledger books, and encounter audit logs into formatted PDF reports saved directly to the device's internal app files directory.
*   **Comprehensive Audit Ledger:** Reviews past cases, true diagnoses, procedural billings, diagnostic clinical scores, and feedback logs under the "Day Practice Report" audit log.

---

## 3. Persistent Data Schema & Models

The local storage capabilities of the application are implemented through a **Room SQLite Database (v4)**.

### A. Database Entity: `EncounterEntity`
This entity models a complete single patient interaction from presentation to discharge.

| Field Name | Data Type | Description |
| :--- | :--- | :--- |
| `id` | `Long` (Primary Key, AutoGen) | Unique internal ID for the clinical encounter record. |
| `timestamp` | `Long` | Unix epoch timestamp of when the case was initiated. |
| `specialty` | `String` | Medical specialty (e.g., Internal Medicine, Cardiology). |
| `chiefComplaint` | `String` | Brief opening clinical symptom presented by the patient. |
| `trueDiagnosis` | `String` | The underlying clinical diagnostic truth. |
| `pathophysiology` | `String` | High-fidelity pathophysiology explanation of the disease. |
| `expectedLabs` | `String` | The gold-standard diagnostic labs/results for triage. |
| `severity` | `String` | Categorization level (e.g., "Routine" vs. "Severe/Acute"). |
| `insuranceStatus` | `String` | Patient coverage provider details (e.g., Private, Public, None). |
| `currentPhase` | `String` | Active OSCE simulation phase (e.g., Presentation, Examination, Diagnosis). |
| `vitals` | `Vitals?` *(JSON)* | Current physiological state indicators (Moshi serialized). |
| `chatHistory` | `List<ChatMessage>` *(JSON)* | Iterative dialog turns between the Doctor and Patient. |
| `labResults` | `String?` | Comprehensive diagnostic and clinical laboratory readouts. |
| `physicalExamResults` | `String?` | Interactive physical evaluation details. |
| `billingReceipt` | `String?` | Finalized clinical procedural invoices and ledger bills. |
| `evaluation` | `String?` | AI assessor's diagnostic evaluation feedback sheet. |
| `isEncounterComplete` | `Boolean` | Flag representing if the simulation was finished. |
| `revenueEarned` | `Double` | Revenue amount collected from this specific encounter. |
| `expensesIncurred` | `Double` | Operational cost incurred during this encounter. |
| `virtualTimeElapsed` | `Int` | Sum of virtual elapsed simulated minutes. |
| `patientMood` | `String` | Patient's affective response mood (e.g., Calm, Anxious, Agitated). |
| `patientStability` | `String` | Clinical status rating (e.g., Stable, Unstable, Critical). |
| `ddxNotes` | `String` | Differential diagnosis and medical charts notes typed by user. |
| `patientDemographics` | `String` | Complete textual patient demographic card. |
| `prescriptionString` | `String?` | Formulated treatment and recipe string. |
| `referralLetterString` | `String?` | Formal referral note generated representation. |
| `sickNoteString` | `String?` | Medical cert/sick note structured record representation. |
| `paymentCollected` | `Boolean` | Financial reconciliation marker. |
| `billingApprovedByHuman` | `Boolean` | Audit and confirmation clearance verification status. |
| `patientOutcome` | `String` | Simulation result rating describing active patient pathway outcome. |

---

### B. Core Shared Models (`Models.kt`)

#### 1. `Vitals`
Captures raw physiological structures for immediate dashboard rendering.
```kotlin
data class Vitals(
    val bp: String,       // e.g., "120/80 mmHg"
    val hr: String,       // e.g., "72 bpm"
    val tempC: Double,    // e.g., 36.8
    val rr: String,       // e.g., "16 breaths/min"
    val spo2: String      // e.g., "98%"
)
```

#### 2. `ChatMessage`
Contains individual conversational tokens for persistent logs & LLM context loops.
```kotlin
data class ChatMessage(
    val role: String,                  // "patient", "doctor", or "system"
    val text: String,                  // Message text content
    val timestamp: Long,               // Message time index
    val virtualTimestampStr: String?   // Virtual elapsed time label
)
```

#### 3. `SimulationState`
Tracks active in-memory dashboard states during the live interactive views.
```kotlin
data class SimulationState(
    val currentPhase: String = "Phase 1 - Presentation",
    val vitals: Vitals? = null,
    val chatHistory: List<ChatMessage> = emptyList(),
    val labResults: String? = null,
    val physicalExamResults: String? = null,
    val billingReceipt: String? = null,
    val evaluation: String? = null,
    val isEncounterComplete: Boolean = false,
    val dailyRevenue: Double = 0.0,
    val patientsSeen: Int = 0,
    val expensesIncurred: Double = 0.0,
    val patientDemographics: String = "Unknown Patient",
    val virtualTimeElapsed: Int = 0,
    val patientMood: String = "Neutral",
    val patientStability: String = "Stable",
    val ddxNotes: String = "",
    val prescriptionString: String? = null,
    val referralLetterString: String? = null,
    val sickNoteString: String? = null,
    val paymentCollected: Boolean = false,
    val billingApprovedByHuman: Boolean = false,
    val patientOutcome: String = "Recovered"
)
```

#### 4. `HiddenCaseProfile` & `GeneratedCaseWrapper`
Used to load cases in memory before active diagnosis begins, avoiding metadata leakage.
```kotlin
data class HiddenCaseProfile(
    val specialty: String,
    val chiefComplaint: String,
    val trueDiagnosis: String,
    val pathophysiology: String,
    val expectedLabs: String,
    val severity: String,
    val insuranceStatus: String,
    val patientDemographics: String
)
```

#### 5. `AIResponseStateUpdate`
Matches JSON elements returned by the multi-LLM service to dynamically modify state parameters.
```kotlin
data class AIResponseStateUpdate(
    val dialogueResponse: String?,
    val vitals: Vitals? = null,
    val currentPhase: String? = null,
    val labResults: String? = null,
    val physicalExamResults: String? = null,
    val billingReceipt: String? = null,
    val evaluation: String? = null,
    val isEncounterComplete: Boolean? = null,
    val additionalExpenses: Double? = null,
    val clinicalScore: Double? = null,
    val patientMood: String? = null,
    val patientStability: String? = null,
    val prescriptionString: String? = null,
    val referralLetterString: String? = null,
    val sickNoteString: String? = null
)
```

---

## 4. Converters and Mappers (`Converters.kt`)
To preserve objects like `Vitals` and list objects like `List<ChatMessage>` inside flat SQLite databases safely, the database implements highly reliable Moshi JSON adapters under the `Converters` class:
*   `fromVitals(Vitals?)` ⇋ `toVitals(String)`
*   `fromChatList(List<ChatMessage>?)` ⇋ `toChatList(String)`

All records are written with safety exception blocks, ensuring that formatting errors never lead to state corruption or application crashes.

---

## 5. Security and Config State Persistence
Critical settings such as custom model choices, custom base endpoints, and user API keys are safely isolated using **Android Jetpack DataStore**. Key store properties persist across application launches, ensuring that custom setups (like locally hosted models or specific OpenAI keys) can be used out of the box securely.

---
*Created and validated for clinical simulation and training.*
