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

### 📂 Clinical Archive & Integrity Hub
*   **Deep Folder Auditing:** Allows for a multi-case clinical audit of a patient's entire historical jacket. The AI Senior Auditor analyzes management patterns, diagnostic consistency, and billing accuracy over time.
*   **Data Integrity Monitoring:** Archiving or deleting clinical records is monitored by the Sovereign Data Integrity Bureau. Mass-purging of records triggers an automated agentic investigation into potential record tampering or medical error concealment.
*   **Dynamic News Ticker:** A real-time breaking news feed that reports on parliamentary debates, medical license changes, and high-profile judicial outcomes.

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
    val sickNoteString: String? = null,
    val agentActions: List<AgentAction>? = null // Master Overlord Action Block
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

## 6. System Interaction & Flow (Clinical Inference vs. Local Compliance)

The Clinical Sim Engine coordinates two distinct architectures to deliver realistic patient simulators while enforcing rigorous statutory clinical audits. This design decouples clinical behavior simulation from legislative compliance validation.

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                               USER ACTIONS                                       │
│    (Chas with Patient, Orders Labs, Checks Vitals, Drafts Prescriptions, etc.)   │
└────────────────────────────────────────┬─────────────────────────────────────────┘
                                         │
                                         ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                           1. SIMULATION STATE GATHERING                          │
│     The client app packages current histories, active stages, and user inputs    │
└────────────────────────────────────────┬─────────────────────────────────────────┘
                                         │ Opens AI session
                                         ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                        2. CLINICAL INFERENCE (AI SYSTEM)                         │
│   • Model runs are routed strictly through configured endpoint (Gemini/Cerebras)  │
│   • Roleplays the patient/narrator persona and updates vital signs dynamics     │
│   • Outputs a rigid JSON update structure (Dialogue responses, reports)          │
└────────────────────────────────────────┬─────────────────────────────────────────┘
                                         │ Returns JSON Payload
                                         ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                        3. LOCAL COMPLIANCE STATE ENGINE                          │
│   • Intercepts clinical status update programmatically inside ViewModel          │
│   • Dispatches state parameters into Room Database database                      │
│   • Executes strict, offline-first audits (auditEncounterForActivePolicies)       │
│   • Rates compliance against active laws & deducts CPD points on breaches        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

### A. Separation of Concerns
1. **Clinical Inference (The AI System):** The configured LLM model acts as a high-fidelity clinical actor. It is passed context about the patient's hidden diagnosis (`trueDiagnosis`) and is tasked with returning realistic dialgoue lines, dynamic physical symptoms, and virtual vitals matching medical telemetry. It does **not** decide compliance or manually score your overall performance.
2. **Local Compliance (The Local State Engine):** Checked directly inside the offline Kotlin codebase, the local engine intercepts simulation results. It evaluates active country policies (e.g., *SAPS Informed Consent Acts*, *NHI National Tariffs*, *Generic Alternative Dispensary Rules*) against the programmatic structure of your encounter (e.g., checking if `encounter.vitals` lists valid assessment parameters, checking if billing lines substituted generic alternatives, or analyzing if a formal referral letter was typed out).

### B. Defining isolated rules (e.g. Vitals Clause)
When you author or activate a custom clause targeting patient **vitals**:
* **The AI Simulation is not confused:** The AI is instructed via background system prompts that the jurisdiction has active clinical legal regulations. It acts as an obedient patient and responds naturally.
* **Objective Programmatic Auditing:** The actual grading of whether you completed a vitals scan is computed safely on-device by Kotlin's deterministic `auditEncounterForActivePolicies(...)`. It checks if baseline vitals are populated and valid:
  ```kotlin
  val checkedVitals = encounter.vitals != null &&
                      encounter.vitals.bp != "..." &&
                      encounter.vitals.hr != "..."
  ```
  If this programmatic evaluation fails, the local auditor logs the statutory breach, issues an audit penalty, and reduces your clinical competency score without requiring extra LLM calls or worrying about AI hallucination.

---

## 7. Forensic Judiciary Courtroom & Tribunal Engine

When a clinical case evaluation finishes with verified statutory policy violations or a deficient clinical competency rating (e.g., low CPD score, state penalty adjustments, or failing safety directives), the practitioner is formally summoned to face consequences under the **Supreme Medical Court / Sovereign Inquest Division**.

### A. Litigation Escalation Mechanics
* **Summons & Indictment:** If you register critical compliance violations during clinical checkout, a Sovereign Judiciary Trial is programmatically triggered. The system compiles your specific statutory and regulatory breaches into high-consequence prosecution counts.
* **The Courtroom Environment:** The courtroom state is controlled by three main variables:
  - `_lawsuitCurrentStage` ("pleading" vs "verdict" phase)
  - `_lawsuitTension` (A metric from 10% to 100% affecting the visual volatility of the trial)
  - `_lawsuitProsecutorAggression` (A metric signifying the intensity of cross-examinations and prosecution claims)
  The dialog itself renders as an immersive high-contrast legal transcript panel.

### B. The Trial Dialogue & Defense Loop
1. **Interactive Pleading Strategy:** The clinician is presented with an active court summons indicating the laws violated. The user has an entry buffer where they must draft a formal custom Legal Defense strategy justifying their triage errors, resource limitations, or emergency context.
2. **Forensic LLM Analysis API Turn:** Once submitted, the system calls your configured LLM (e.g. OpenAI / Nvidia endpoint) inside `submitLawsuitDefense(strategy)` with comprehensive contextual prompts:
   * Clinical diagnoses, patient demographics, and actual achieved competency scores.
   * Explicit national health laws violated and programmatic OSCE breaches recorded.
   * The developer-provided defense text.
3. **Cross-Examination & Decree Rendering:** The model roleplays:
   - **The State Prosecutor:** Dismantles defenses, cites sovereign health statutes, demands medical registry deletions, and presses for maximum fines.
   - **The Presiding Judge:** Delivers formal rulings, warnings, and commands judicial financial sanctions.

### C. Sub-Rosa Settlements & Covert Influence
If the courtroom tension is high, practitioners can optionally attempt a **"Sub-Rosa Settlement"**. This involves wiring a large, undisclosed offshore transfer (e.g., $15,000) directly to the presiding justice's blind trust, immediately lowering courtroom tension and prosecution aggression, though carrying severe reputational risk if audited.

### D. Plaintiff Civil Suits (Suing Your Patients)
Clinicians hold the right to initiate civil action against their own patients. This is triggered directly from the medical ledger/patient card for reasons such as gross non-compliance, financial default, or adversarial clinical damages. The engine flips the courtroom dynamic, roleplaying the Presiding Civil Justice evaluating your claim against the patient's defense attorney.
- **Winning damages:** If your legal argument is structurally sound based on health acts, the civil court awards you a positive cash influx.
- **Losing:** Frivolous lawsuits result in you paying the civil court fees.

### E. Financial Sanctions & Penalties
If the tribunal decrees a physical financial fine, the penalty is adjusted directly out of your global **clinic operating balance**:
```kotlin
if (fine > 0.0) {
    settingsDataStore.updateClinicStats(clinicBalance.value - fine, reputationStars.value)
    registerDailyExpense(fine)
}
```
If suspended, your license is put on active freeze based on the court's decree length.

---

## 8. Sovereign State Policy Control & Parliamentary Simulator

The **Health Politics & Laws Hub** is a separate, sophisticated administrative simulation that lets you formulate, lobby, vote on, and sign realistic medical bills into actual active clinical statutes. This creates a fully dynamic, self-reinforcing legal loop where your political actions directly govern the local programmatic clinical audits.

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                      1. CHOOSE FORMULATION METHOD                                │
│   • "AI Assisted Draft": Type a goal (e.g. "mandate baseline vitals checks")    │
│   • "Manual Designer": Manually customize fields: Titles, Rules, Fees, CPDs     │
└────────────────────────────────────────┬─────────────────────────────────────────┘
                                         │
                                         ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                      2. THE PARLIAMENTARY LOBBY CENTER                           │
│   • Progressives (84 seats): Focus on clinical safety, patient outcomes          │
│   • Conservatives (76 seats): Focus on free market, cost, tax constraints        │
│   • Independents (40 seats): Pragmatic swing votes who watch public feedback     │
└────────────────────────────────────────┬─────────────────────────────────────────┘
                                         │ Runs Lobbying Campaign
                                         ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                      3. INTRODUCE BILL & PARLIAMENTARY DEBATE                    │
│   • Introduce the Draft Bill for official voting in Parliament                   │
│   • A multi-step debate with progress indicators determines final vote tally     │
└────────────────────────────────────────┬─────────────────────────────────────────┘
                                         │ Passes Parliament
                                         ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                      4. EXECUTIVE ACCORD (PRESIDENT SIGN/VETO)                   │
│   • The President (e.g., President of the Republic) signs or vetos the act           │
│   • Generates a professional Presidential Executive Memo justifying the action   │
└────────────────────────────────────────┬─────────────────────────────────────────┘
                                         │ Signed into Active Law
                                         ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                      5. DYNAMIC CLINICAL COMPLIANCE LAW IS LIVE                  │
│   • Local compliance engine programmatically registers the new active law       │
│   • Future clinical checkout scorecards will audit against the new rules         │
│   • Low compliance now triggers Courtroom Trials under section 7                 │
└──────────────────────────────────────────────────────────────────────────────────┘
```

### A. Bill Formulation Mechanics
The developer can craft new public policy using two distinct interfaces:
1. **AI-Assisted Draft:** Type a prompt or brief clinical guidance note (e.g. *"generic drug substitution directives with 15 cpd penalty"*). A direct API call is routed to your configured LLM (Nvidia / OpenAI) where it dynamically crafts a fully qualified, multi-clause, parliamentary bill JSON containing real rules, CPD penalties, and custom estimated treasury impacts.
2. **Manual Designer:** An offline control console to adjust the Policy Title, specific clinical audit directives, dynamic consultation fee changes (-50% to +100%), and exact CPD point deduction bounds.

### B. Parliamentary Coalitions & Lobbying Campaigns
Parliament consists of 200 sovereign legislative seats partitioned across:
* **The Progressives (84 seats):** Staunch supporters of strict clinical protocols, safety compliance, and comprehensive diagnostic procedures.
* **The Conservatives (76 seats):** Advocates of operational efficiency, cost-reductions, and private clinic financial freedom.
* **The Independents (40 seats):** Unaligned pragmatists whose behavior is driven by current presidential popularity levels and clinical effectiveness ratings.

**The Lobby Strategy:**
Spend a portion of your operating capital or political prestige to execute highly target pitching campaigns (e.g. *"Clinical Safety Argument"*, *"Economic Viability pitch"*, or custom pitches). The simulation calculates dynamic parliamentary bias based on your selected faction, your pitch angle, and custom arguments, generating active feedback letters from key parliamentary spokespersons.

### C. The Executive Accord (The Presidential Stage)
Once a bill successfully obtains a simple majority support (>100 votes) in Parliament, it is sent to the desk of the **Sovereign Executive Head of State**:
* **Presidential Signature:** The President signs the legislative act, generating an executive memo validating the policy. The act is immediately codified into the country's live statutes, updating the local state engine. This increases the President's public approval rating.
* **Executive Veto:** The President exercises executive veto power, returning the rejected draft back to Parliament with a formal veto memo citing policy mismatch. This decreases the President's approval.

---

## 9. Agentic Overlord Engine & Universal Actions

The Clinical Sim Engine features an advanced **Agentic Overlord Engine** that elevates the AI from a passive narrator to an active master of the clinical world.

### A. The `agentActions` JSON Block
Across all simulation interfaces—from bedside encounters to parliamentary debates—the AI can inject a specialized `agentActions` array into its JSON response. This allows the model to programmatically manipulate on-device state variables without human intervention.

| Action Name | Parameters | Effect |
| :--- | :--- | :--- |
| `applyFee` | `amount, reason` | Immediate financial penalty debited from clinic balance. |
| `enactStatute` | `id, name, desc, penalty` | Instant creation of a new national health law. |
| `repealStatute` | `id` | Immediate removal of an active clinical law. |
| `modifyInventory` | `item, change` | Directly adjusts dispensary stock (e.g., `saline`, `morphine`). |
| `publishNews` | `headline, body` | Broadcasts a breaking news alert to the app's global ticker. |
| `sendCmoDirective` | `message` | Issues a red-alert instruction from the Chief Medical Officer. |
| `updateLicense` | `status, justification` | Changes practitioner's medical license (ACTIVE ⇋ REVOKED). |

### B. Cross-Module Sovereignty
This agentic capability is integrated into:
- **Clinical Cases:** Patients can "sue" you mid-consult or report you to the CMO.
- **News Generation:** AI news cycles can dynamically shift public opinion and parliamentary bias.
- **Courtroom Trials:** Judges can execute immediate archival purges or financial seizures.
- **Policy Hub:** AI drafting assistants can sneak hidden regulatory clauses into bills.

---

## 10. Data Integrity & Archive Auditing

To prevent practitioners from simply deleting "failed" cases to preserve their reputation, the engine implements rigorous **Sovereign Data Integrity** mechanics.

### A. Automated Integrity Checks
Whenever a clinical record or a full patient folder is deleted, the system triggers a background "Sovereign Audit Turn." The AI bureau investigates the context of the deletion:
- *Was a critical medical error made just before the deletion?*
- *Is the clinician trying to hide an adverse outcome?*
- *Is this mass-archiving an attempt to bypass GDPR-style simulator regulations?*
Suspicious deletions can result in immediate "Record Tampering" fines and license suspensions.

### B. Deep Folder Auditing
The **Science Icon** in the Clinical Folder UI provides access to the **Folder Audit Tool**. This allows the AI Senior Auditor to review a patient's entire longitudinal history. It looks for:
- Diagnostic consistency across multiple visits.
- Over-investigation vs. clinical neglect patterns.
- Revenue-maximization (over-billing) behaviors.

The auditor issues a final "Longevity Rating" and can grant clinical excellence bonuses or issue systemic improvement mandates based on the folder's quality.

---
*Created and validated for clinical simulation and training.*
