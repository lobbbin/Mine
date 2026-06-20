package com.example.ui

data class PresetSandboxAction(
    val id: String,
    val category: String,
    val label: String,
    val description: String,
    val promptText: String,
    val hexColor: String,
    val kotlinLogic: String = ""
)

object SandboxPresetActions {
    val items: List<PresetSandboxAction> = listOf(
        // === CATEGORY 1: GP Clinical Cases (30 items) ===
        PresetSandboxAction(
            id = "gp_1",
            category = "GP Clinical Cases",
            label = "Flu Outbreak Symptoms",
            description = "Admit standard flu patient with body aches & runny nose",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: A standard outpatient presenting with typical influenza flu symptoms: runny nose, sore throat, severe body aches, and fever. True diagnosis is Influenza A. Severity is routine. Specialty is General Practice.",
            hexColor = "#4DB6AC"
        ),
        PresetSandboxAction(
            id = "gp_2",
            category = "GP Clinical Cases",
            label = "Classic Migraine",
            description = "Admit outpatient with throbbing unilateral headache",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: Outpatient presents with unilateral, throbbing headache of moderate to severe intensity. Triggered by bright light. True diagnosis is Classical Migraine. Specialty is General Practice. Severity is routine.",
            hexColor = "#4DB6AC"
        ),
        PresetSandboxAction(
            id = "gp_3",
            category = "GP Clinical Cases",
            label = "Type 2 Diabetes Review",
            description = "Admit standard outpatient for diabetes checkup",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: Patient comes for routine review of Type 2 Diabetes Mellitus. Complains of polyuria and polydipsia over 3 days. True diagnosis is Uncontrolled T2DM. Specialty is General Practice. Severity is routine.",
            hexColor = "#4DB6AC"
        ),
        PresetSandboxAction(
            id = "gp_4",
            category = "GP Clinical Cases",
            label = "Hypertension Flare-up",
            description = "High BP reading (160/95) with mild headache",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: Patient baseline BP checked at 160/95 mmHg. Complains of mild occipital headache in mornings. True diagnosis is Essential Hypertension. Specialty is General Practice. Severity is routine.",
            hexColor = "#4DB6AC"
        ),
        PresetSandboxAction(
            id = "gp_5",
            category = "GP Clinical Cases",
            label = "Seafood Food Poisoning",
            description = "Nausea, vomiting, and diarrhea after seafood",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: Outpatient presents with acute nausea, vomiting, watery diarrhea, and cramping after eating fish. True diagnosis is Food Poisoning (Gastroenteritis). Specialty is General Practice. Severity is routine.",
            hexColor = "#4DB6AC"
        ),
        PresetSandboxAction(
            id = "gp_6",
            category = "GP Clinical Cases",
            label = "Ankle Sprain Injury",
            description = "Swollen lateral ankle joint post basketball",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: Patient twisted ankle playing basketball. Tenderness and swelling at the anterior talofibular ligament. True diagnosis is Grade I Lateral Ankle Sprain. Specialty is General Practice. Severity is routine.",
            hexColor = "#4DB6AC"
        ),
        PresetSandboxAction(
            id = "gp_7",
            category = "GP Clinical Cases",
            label = "Acute Bronchitis",
            description = "Persistent dry cough with purulent yellow phlegm",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: Outpatient presents with deep, hacking productive cough, bringing up yellow purulent sputum. No respiratory distress. True diagnosis is Acute Bronchitis. Specialty is General Practice. Severity is routine.",
            hexColor = "#4DB6AC"
        ),
        PresetSandboxAction(
            id = "gp_8",
            category = "GP Clinical Cases",
            label = "Streptococcal Tonsillitis",
            description = "Admit throat pain case with tonsillar exsudate",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: Throat pain, high fever, swollen tender anterior cervical lymph nodes. Exam shows tonsillar hypertrophy with white exudate. True diagnosis is Streptococcal Tonsillitis. Specialty is General Practice. Severity is routine.",
            hexColor = "#4DB6AC"
        ),
        PresetSandboxAction(
            id = "gp_9",
            category = "GP Clinical Cases",
            label = "Gouty Arthritis Flare",
            description = "Extremely painful, hot red great toe joint",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: Sudden extreme pain, erythema, and swelling in the first metatarsophalangeal joint. Patient had red meat and beer yesterday. True diagnosis is Acute Gout Flare. Specialty is General Practice. Severity is routine.",
            hexColor = "#4DB6AC"
        ),
        PresetSandboxAction(
            id = "gp_10",
            category = "GP Clinical Cases",
            label = "GERD Heartburn",
            description = "Retrosternal burning after dinner, voice raspy",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: Retrosternal burning pain, worse when lying down or after spicy meals. Mild hoarseness in the morning. True diagnosis is Gastroesophageal Reflux Disease (GERD). Specialty is General Practice. Severity is routine.",
            hexColor = "#4DB6AC"
        ),
        PresetSandboxAction(
            id = "gp_11",
            category = "GP Clinical Cases",
            label = "Microcytic Anemia",
            description = "Fatigue, shortness of breath on exertion, pale skin",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: Young female with history of heavy menses complaining of severe lethargy and brittle nails. Pale conjunctivae. True diagnosis is Iron Deficiency Anemia. Specialty is General Practice. Severity is routine.",
            hexColor = "#4DB6AC"
        ),
        PresetSandboxAction(
            id = "gp_12",
            category = "GP Clinical Cases",
            label = "Tension-type Headache",
            description = "Band-like pressure encircling forehead and neck",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: Outpatient reports dull, squeezing, non-throbbing pain around the head. No nausea. True diagnosis is Chronic Tension Headache. Specialty is General Practice. Severity is routine.",
            hexColor = "#4DB6AC"
        ),
        PresetSandboxAction(
            id = "gp_13",
            category = "GP Clinical Cases",
            label = "Otitis Media Kids",
            description = "Tugging at left ear, irritable, bulging tympanum",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: Child presenting with fever and persistent left ear tugging. Otoscopy reveals bulging, erythematous tympanic membrane. True diagnosis is Acute Otitis Media. Specialty is General Practice. Severity is routine.",
            hexColor = "#4DB6AC"
        ),
        PresetSandboxAction(
            id = "gp_14",
            category = "GP Clinical Cases",
            label = "Acute Cystitis (UTI)",
            description = "Burning dysuria, frequent small urination, pelvic ache",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: Female with severe burning during urination, frequent voiding, and lower suprapubic pelvic discomfort. True diagnosis is Acute Uncomplicated Cystitis (UTI). Specialty is General Practice. Severity is routine.",
            hexColor = "#4DB6AC"
        ),
        PresetSandboxAction(
            id = "gp_15",
            category = "GP Clinical Cases",
            label = "Anxiety Somatization",
            description = "Chest tightness, hyperventilation, normal vitals",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: Patient with sudden tightness in chest, cold sweat, tremulousness, and panic sensation. ECG and cardiac vitals are completely normal. True diagnosis is Panic Attack / Generalized Anxiety. Specialty is General Practice. Severity is routine.",
            hexColor = "#4DB6AC"
        ),
        PresetSandboxAction(
            id = "gp_16",
            category = "GP Clinical Cases",
            label = "Contact Dermatitis",
            description = "Pruritic linear red vesicular rash after garden work",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: Linear vesicular pruritic rash on forearms after clearing backyard weeds and ivy. True diagnosis is Allergic Contact Dermatitis. Specialty is General Practice. Severity is routine.",
            hexColor = "#4DB6AC"
        ),
        PresetSandboxAction(
            id = "gp_17",
            category = "GP Clinical Cases",
            label = "Inguinal Hernia Check",
            description = "Groin bulge that disappears when lying flat",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: Patient notices painless bulge in right groin while lifting heavy objects. Bulge is easily reducible. True diagnosis is Reducible Indirect Inguinal Hernia. Specialty is General Practice. Severity is routine.",
            hexColor = "#4DB6AC"
        ),
        PresetSandboxAction(
            id = "gp_18",
            category = "GP Clinical Cases",
            label = "Asthma Rescue Consultation",
            description = "Known asthmatic asks for prescription refill for inhaler",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: Non-severe patient requesting refill for salbutamol inhaler. Reports mild nocturnal wheeze. True diagnosis is Mild Persistent Asthma. Specialty is General Practice. Severity is routine.",
            hexColor = "#4DB6AC"
        ),
        PresetSandboxAction(
            id = "gp_19",
            category = "GP Clinical Cases",
            label = "Insect Bite Local Reaction",
            description = "Large red painful swelling at bee sting site",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: Wasp sting on left calf. No systemic signs of anaphylaxis, normal airway, large localized induration. True diagnosis is Large Local Reaction to Wasp Venom. Specialty is General Practice. Severity is routine.",
            hexColor = "#4DB6AC"
        ),
        PresetSandboxAction(
            id = "gp_20",
            category = "GP Clinical Cases",
            label = "Hypovitaminosis D",
            description = "Vague generalized musculoskeletal aches and fatigue",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: Desk worker presenting with generalized bone soreness and chronic fatigue during winter. True diagnosis is Vitamin D Deficiency. Specialty is General Practice. Severity is routine.",
            hexColor = "#4DB6AC"
        ),
        PresetSandboxAction(
            id = "gp_21",
            category = "GP Clinical Cases",
            label = "Maxillary Sinusitis",
            description = "Facial pressure, purulent rhinorrhea, bad breath",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: Facial dull aching focused behind cheekbones and forehead, accompanied by thick green nasal discharge and halitosis. True diagnosis is Acute Maxillary Sinusitis. Specialty is General Practice. Severity is routine.",
            hexColor = "#4DB6AC"
        ),
        PresetSandboxAction(
            id = "gp_22",
            category = "GP Clinical Cases",
            label = "Peptic Ulcer Symptoms",
            description = "Burning epigastric pain relieved by drinking milk",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: Epigastric burning pain occurring 2 hours after meals and at night, temporary absolute relief after antacids or milk intake. True diagnosis is Peptic/Duodenal Ulcer. Specialty is General Practice. Severity is routine.",
            hexColor = "#4DB6AC"
        ),
        PresetSandboxAction(
            id = "gp_23",
            category = "GP Clinical Cases",
            label = "Hyperthyroidism Anxiety",
            description = "Palpitations, weight loss despite increased appetite",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: Tremulousness, heat intolerance, weight loss of 5kg and racing pulse. True diagnosis is Hyperthyroidism (Graves Disease). Specialty is General Practice. Severity is routine.",
            hexColor = "#4DB6AC"
        ),
        PresetSandboxAction(
            id = "gp_24",
            category = "GP Clinical Cases",
            label = "Primary Hypothyroidism",
            description = "Weight gain, cold intolerance, dry skin, sparse hair",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: Extreme sluggishness, cold sensitivity, constipation, bradycardia (55 bpm), and general puffiness. True diagnosis is Primary Hypothyroidism. Specialty is General Practice. Severity is routine.",
            hexColor = "#4DB6AC"
        ),
        PresetSandboxAction(
            id = "gp_25",
            category = "GP Clinical Cases",
            label = "Scabies Infestation",
            description = "Intense nocturnal itching, webbed finger lesions",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: Patient complains of severe body itch, particularly at night. Inspection shows erythematous burrows in finger webs. True diagnosis is Scabies Infestation. Specialty is General Practice. Severity is routine.",
            hexColor = "#4DB6AC"
        ),
        PresetSandboxAction(
            id = "gp_26",
            category = "GP Clinical Cases",
            label = "Tinea Pedis (Athlete's Foot)",
            description = "Scaling, maceration of webbed toes, severe itching",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: Gym patron showing macerated white peeling skin between 4th and 5th toes with severe burning sensation. True diagnosis is Tinea Pedis. Specialty is General Practice. Severity is routine.",
            hexColor = "#4DB6AC"
        ),
        PresetSandboxAction(
            id = "gp_27",
            category = "GP Clinical Cases",
            label = "Acute Viral Laryngitis",
            description = "Completely lost voice after cold, raw throat feel",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: Patient can only speak in a faint whisper. History of rhinorrhea 3 days ago. Ear/throat examine normal. True diagnosis is Acute Viral Laryngitis. Specialty is General Practice. Severity is routine.",
            hexColor = "#4DB6AC"
        ),
        PresetSandboxAction(
            id = "gp_28",
            category = "GP Clinical Cases",
            label = "Lumbar Sprain/Strain",
            description = "Lower back muscle spasms after lifting storage boxes",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: Severe paravertebral lumbar muscle tenderness. No paresthesias or sciatica. Reflexes functional. True diagnosis is Acute Mechanical Lumbar Muscle Strain. Specialty is General Practice. Severity is routine.",
            hexColor = "#4DB6AC"
        ),
        PresetSandboxAction(
            id = "gp_29",
            category = "GP Clinical Cases",
            label = "Viral Conjunctivitis",
            description = "Watery pink eyes, gritty sand sensation, crusty lashes",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: Conjunctival injection, clear watery mucoid discharge, and mild photophobia. No vision loss. True diagnosis is Acute Viral Conjunctivitis. Specialty is General Practice. Severity is routine.",
            hexColor = "#4DB6AC"
        ),
        PresetSandboxAction(
            id = "gp_30",
            category = "GP Clinical Cases",
            label = "Viral Gastro Cramps",
            description = "Watery diarrhea, mild fever, low-grade cramping",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: 5 liquid mucous-free stools in 24 hours. Normal capillary refill, slight skin turgor sluggishness. True diagnosis is Rotavirus / Viral Gastroenteritis. Specialty is General Practice. Severity is routine.",
            hexColor = "#4DB6AC"
        ),

        // === CATEGORY 2: Malpractice & Courts (20 items) ===
        PresetSandboxAction(
            id = "court_1",
            category = "Malpractice & Courts",
            label = "PR Defense Campaign",
            description = "Hire PR Firm: Spend $1 to lower lawsuit friction",
            promptText = "[(SYSTEM ACTION)]: The clinic hired a reputation manager to publish clinical safety reports. Courtroom defense bias increases.",
            hexColor = "#E64A19",
            kotlinLogic = "clinicBalance -= 5000\nreputationStars += 1"
        ),
        PresetSandboxAction(
            id = "court_2",
            category = "Malpractice & Courts",
            label = "Settle Claim Out-Of-Court",
            description = "Pay patient $1 to drop active legal charge instantly",
            promptText = "[(SYSTEM ACTION)]: The clinic pays compensation to the plaintiff. All active civil disputes are dismissed.",
            hexColor = "#E64A19",
            kotlinLogic = "clinicBalance -= 15000\nreputationStars += 0.5"
        ),
        PresetSandboxAction(
            id = "court_3",
            category = "Malpractice & Courts",
            label = "Influence Prosecutor",
            description = "Contribute $1 to legal guild to lessen state aggression",
            promptText = "[(SYSTEM ACTION)]: Paid clinical compliance fees to the medical union. The prosecutorial pressure decreases.",
            hexColor = "#E64A19",
            kotlinLogic = "clinicBalance -= 10000"
        ),
        PresetSandboxAction(
            id = "court_4",
            category = "Malpractice & Courts",
            label = "Senior Elite Retainer",
            description = "Deploy top-tier legal advisors ($1 retainer fee)",
            promptText = "[(SYSTEM ACTION)]: Contracted senior clinical litigator to represent the practice, boosting reputation.",
            hexColor = "#E64A19",
            kotlinLogic = "clinicBalance -= 8000\nreputationStars += 1.2"
        ),
        PresetSandboxAction(
            id = "court_5",
            category = "Malpractice & Courts",
            label = "Conduct Mock Jury Trial",
            description = "Practice courtroom defense strategies (Spend $1 gain XP)",
            promptText = "[(SYSTEM ACTION)]: Staff rehearses legal malpractice defense under mock prosecution.",
            hexColor = "#E64A19",
            kotlinLogic = "clinicBalance -= 2000"
        ),
        PresetSandboxAction(
            id = "court_6",
            category = "Malpractice & Courts",
            label = "Press Release Statement",
            description = "Reiterate patient commitment (Dampens public outrage, boost reputation)",
            promptText = "[(SYSTEM ACTION)]: Public relations broadcasted official clinical standard statements.",
            hexColor = "#E64A19",
            kotlinLogic = "reputationStars += 0.8"
        ),
        PresetSandboxAction(
            id = "court_7",
            category = "Malpractice & Courts",
            label = "Judicial Loophole Audit",
            description = "Analyze licensing codes for bypass vectors (Spend $1)",
            promptText = "[(SYSTEM ACTION)]: Legal consultants map out statutory bypass methods.",
            hexColor = "#E64A19",
            kotlinLogic = "clinicBalance -= 1500\npoliticalPrestige += 5"
        ),
        PresetSandboxAction(
            id = "court_8",
            category = "Malpractice & Courts",
            label = "Elysium Board Hearing",
            description = "Attend mandatory professional conduct review",
            promptText = "[(SYSTEM ACTION)]: Practitioner defends diagnostic and therapeutic records in council chambers.",
            hexColor = "#E64A19",
            kotlinLogic = "politicalPrestige -= 10"
        ),
        PresetSandboxAction(
            id = "court_9",
            category = "Malpractice & Courts",
            label = "Reputational Conciliation",
            description = "Express patient empathy (Gain 0.5 stars immediately)",
            promptText = "[(SYSTEM ACTION)]: Practitioner establishes conciliatory rapport with family council.",
            hexColor = "#E64A19",
            kotlinLogic = "reputationStars += 0.5"
        ),
        PresetSandboxAction(
            id = "court_10",
            category = "Malpractice & Courts",
            label = "Sanitize Charting Logs",
            description = "Re-format prior electronic records (Spend $1 avoid scrutiny)",
            promptText = "[(SYSTEM ACTION)]: Clinical records double-audited against formatting slips.",
            hexColor = "#E64A19",
            kotlinLogic = "clinicBalance -= 2500"
        ),
        PresetSandboxAction(
            id = "court_11",
            category = "Malpractice & Courts",
            label = "Subpoena Plaintiff Record",
            description = "Demand prior history discovery (Spend $1)",
            promptText = "[(SYSTEM ACTION)]: Court-ordered discovery of claimant's external histories.",
            hexColor = "#E64A19",
            kotlinLogic = "clinicBalance -= 1000"
        ),
        PresetSandboxAction(
            id = "court_12",
            category = "Malpractice & Courts",
            label = "Deframe Prosecution Claim",
            description = "Slander prosector methodology (Decrease legal tension, lose $1)",
            promptText = "[(SYSTEM ACTION)]: Defense leaks prosecuting procedural oversight files.",
            hexColor = "#E64A19",
            kotlinLogic = "clinicBalance -= 3000\npoliticalPrestige -= 5"
        ),
        PresetSandboxAction(
            id = "court_13",
            category = "Malpractice & Courts",
            label = "Compromise Settlement",
            description = "Offer $1 compromise to close ongoing board audits",
            promptText = "[(SYSTEM ACTION)]: Paid compromised administrative penalties to medical board.",
            hexColor = "#E64A19",
            kotlinLogic = "clinicBalance -= 8000\nreputationStars += 0.4"
        ),
        PresetSandboxAction(
            id = "court_14",
            category = "Malpractice & Courts",
            label = "Retain Medical Professor",
            description = "Hire expert clinical witness to support therapy (Cost $1)",
            promptText = "[(SYSTEM ACTION)]: Independent specialist issues letter of clinical reasonability.",
            hexColor = "#E64A19",
            kotlinLogic = "clinicBalance -= 5000\nreputationStars += 0.6"
        ),
        PresetSandboxAction(
            id = "court_15",
            category = "Malpractice & Courts",
            label = "Plea Trial Postponement",
            description = "Delay lawsuit hearing due to clinical workload (Cost $1)",
            promptText = "[(SYSTEM ACTION)]: Court registers formal request for continuance.",
            hexColor = "#E64A19",
            kotlinLogic = "clinicBalance -= 3000"
        ),
        PresetSandboxAction(
            id = "court_16",
            category = "Malpractice & Courts",
            label = "Appeal National Audit",
            description = "Seek state clearance review (Increase political prestige)",
            promptText = "[(SYSTEM ACTION)]: Filed formal appeal to National Department of Health supervision.",
            hexColor = "#E64A19",
            kotlinLogic = "politicalPrestige += 15"
        ),
        PresetSandboxAction(
            id = "court_17",
            category = "Malpractice & Courts",
            label = "Premium Malpractice Tax",
            description = "Mandatory professional insurance surcharge (Loss of $1)",
            promptText = "[(SYSTEM ACTION)]: Local treasury levies emergency risk operational fee on private licenses.",
            hexColor = "#E64A19",
            kotlinLogic = "clinicBalance -= 5000"
        ),
        PresetSandboxAction(
            id = "court_18",
            category = "Malpractice & Courts",
            label = "Sovereign Board Penalty",
            description = "Pay disciplinary board admin penalty of $1",
            promptText = "[(SYSTEM ACTION)]: Board sanctions practitioner for administrative non-conformity.",
            hexColor = "#E64A19",
            kotlinLogic = "clinicBalance -= 4000\npoliticalPrestige -= 5"
        ),
        PresetSandboxAction(
            id = "court_19",
            category = "Malpractice & Courts",
            label = "National Board Assistance",
            description = "Claim sovereign legal premium defense funding (+$1 -10 Prestige)",
            promptText = "[(SYSTEM ACTION)]: Clinic collects national safety protection subsidies.",
            hexColor = "#E64A19",
            kotlinLogic = "clinicBalance += 6000\npoliticalPrestige -= 10"
        ),
        PresetSandboxAction(
            id = "court_20",
            category = "Malpractice & Courts",
            label = "Amnesty Clearance",
            description = "Full legal pardon from National Council (+0.8 stars, -$1 fees)",
            promptText = "[(SYSTEM ACTION)]: Minister of Health signed blanket liability clearance for community clinicians.",
            hexColor = "#E64A19",
            kotlinLogic = "clinicBalance -= 1000\nreputationStars += 0.8\npoliticalPrestige += 10"
        ),

        // === CATEGORY 3: Clinic & Supplies (15 items) ===
        PresetSandboxAction(
            id = "supply_1",
            category = "Clinic & Supplies",
            label = "Emergency Bulk Buy",
            description = "Directly buy 10 Syringes & 5 Saline (Cost $1)",
            promptText = "[(SYSTEM ACTION)]: Clinic purchases standard safety inventory pack. Restock completed.",
            hexColor = "#0D47A1",
            kotlinLogic = "clinicBalance -= 2000"
        ),
        PresetSandboxAction(
            id = "supply_2",
            category = "Clinic & Supplies",
            label = "Syndicate Stock Pack",
            description = "Procure generic clinical medications bundle for $1",
            promptText = "[(SYSTEM ACTION)]: Bypassed regulatory wholesale channels to restock bulk GP therapies.",
            hexColor = "#0D47A1",
            kotlinLogic = "clinicBalance -= 4000"
        ),
        PresetSandboxAction(
            id = "supply_3",
            category = "Clinic & Supplies",
            label = "Filing Audit Recovery",
            description = "Reprocess backlogged insurance claims (Collect $1)",
            promptText = "[(SYSTEM ACTION)]: Accounting successfully reconciled and cleared contested medical aid payments.",
            hexColor = "#0D47A1",
            kotlinLogic = "clinicBalance += 8000"
        ),
        PresetSandboxAction(
            id = "supply_4",
            category = "Clinic & Supplies",
            label = "Sovereign Health Grant",
            description = "Receive direct parliamentary clinic support subsidy of $1",
            promptText = "[(SYSTEM ACTION)]: Received federal health grant for maintaining high community access.",
            hexColor = "#0D47A1",
            kotlinLogic = "clinicBalance += 12000\npoliticalPrestige += 8"
        ),
        PresetSandboxAction(
            id = "supply_5",
            category = "Clinic & Supplies",
            label = "Hyperinflation Shock",
            description = "Emergency overhead cost adjustment (Lose $1 overhead)",
            promptText = "[(SYSTEM ACTION)]: Global logistics bottlenecks spiked local wholesale prices.",
            hexColor = "#0D47A1",
            kotlinLogic = "clinicBalance -= 3000"
        ),
        PresetSandboxAction(
            id = "supply_6",
            category = "Clinic & Supplies",
            label = "Free Community Aid Day",
            description = "Serve local neighborhood completely free (Spend $1 gain prestige)",
            promptText = "[(SYSTEM ACTION)]: Dr. Tim's clinic suspends co-payments for charity care. Prestige spikes.",
            hexColor = "#0D47A1",
            kotlinLogic = "clinicBalance -= 3500\npoliticalPrestige += 20\nreputationStars += 0.6"
        ),
        PresetSandboxAction(
            id = "supply_7",
            category = "Clinic & Supplies",
            label = "Corporate Rate Premium",
            description = "Adjust base consultation rate to $1 (+$1 hike)",
            promptText = "[(SYSTEM ACTION)]: Clinic consultation tariff schedule adjusted to high-income thresholds.",
            hexColor = "#0D47A1",
            kotlinLogic = "consultationFee = 1200"
        ),
        PresetSandboxAction(
            id = "supply_8",
            category = "Clinic & Supplies",
            label = "Affordable Price Cuts",
            description = "Decrease consultation tariff to $1 for sliding scale",
            promptText = "[(SYSTEM ACTION)]: Basic outpatient consult price capped at low subsidy rates.",
            hexColor = "#0D47A1",
            kotlinLogic = "consultationFee = 450"
        ),
        PresetSandboxAction(
            id = "supply_9",
            category = "Clinic & Supplies",
            label = "Liquidate Surplus Saline",
            description = "Sell excess storage supplies back to guild (+$1)",
            promptText = "[(SYSTEM ACTION)]: Clinic sold clinical surplus inventory packages back to local public hospital.",
            hexColor = "#0D47A1",
            kotlinLogic = "clinicBalance += 1500"
        ),
        PresetSandboxAction(
            id = "supply_10",
            category = "Clinic & Supplies",
            label = "Purchase Syringe Restock",
            description = "Buy massive crate of standard luer lock syringes (Cost $1)",
            promptText = "[(SYSTEM ACTION)]: Bulk clinical supplies delivered to dispensary shelves.",
            hexColor = "#0D47A1",
            kotlinLogic = "clinicBalance -= 1000"
        ),
        PresetSandboxAction(
            id = "supply_11",
            category = "Clinic & Supplies",
            label = "Reagent Bulk Cashback",
            description = "Reconcile lab contracts with chemical depot (+$1)",
            promptText = "[(SYSTEM ACTION)]: Received chemical supplier volume rebate.",
            hexColor = "#0D47A1",
            kotlinLogic = "clinicBalance += 2000"
        ),
        PresetSandboxAction(
            id = "supply_12",
            category = "Clinic & Supplies",
            label = "Critical Sepsis Bailout",
            description = "Receive sovereign health threat response grant (+$1)",
            promptText = "[(SYSTEM ACTION)]: Disaster relief committee approves emergency operational funding.",
            hexColor = "#0D47A1",
            kotlinLogic = "clinicBalance += 20000\npoliticalPrestige += 10"
        ),
        PresetSandboxAction(
            id = "supply_13",
            category = "Clinic & Supplies",
            label = "Vitals Machine Modernizer",
            description = "Procure next-gen touch vitals monitoring device (Cost $8000)",
            promptText = "[(SYSTEM ACTION)]: Tech deployment: installed high-precision multi-parameter patient monitor.",
            hexColor = "#0D47A1",
            kotlinLogic = "clinicBalance -= 8000\nreputationStars += 0.8"
        ),
        PresetSandboxAction(
            id = "supply_14",
            category = "Clinic & Supplies",
            label = "Elysium Generator Lease",
            description = "Deploy backup solar generator for vaccine fridge (Cost $5000)",
            promptText = "[(SYSTEM ACTION)]: Off-grid deployment complete. Power safety guaranteed.",
            hexColor = "#0D47A1",
            kotlinLogic = "clinicBalance -= 5000\nreputationStars += 0.4"
        ),
        PresetSandboxAction(
            id = "supply_15",
            category = "Clinic & Supplies",
            label = "Pharmacy Pilferage Deduct",
            description = "Audit discrepancy: lose $2000 inventory baseline",
            promptText = "[(SYSTEM ACTION)]: Quarterly audit identified inventory leakage/spoilage. Deduction executed.",
            hexColor = "#0D47A1",
            kotlinLogic = "clinicBalance -= 2000"
        ),

        // === CATEGORY 4: Parliament Laws (20 items) ===
        PresetSandboxAction(
            id = "parl_1",
            category = "Parliament Laws",
            label = "Patient Bill Of Rights",
            description = "Enforced clinical safety laws (+15 prestige, -$2000 regulatory tariff)",
            promptText = "[(SYSTEM ACTION)]: Enacted Statutory Charter on Healthcare Standards. Private aids must pay co-insurances rapidly.",
            hexColor = "#311B92",
            kotlinLogic = "clinicBalance -= 2000\npoliticalPrestige += 15"
        ),
        PresetSandboxAction(
            id = "parl_2",
            category = "Parliament Laws",
            label = "Sovereign Lab Subsidy",
            description = "State funds laboratory chemical assets (-$1000 Prestige += 10)",
            promptText = "[(SYSTEM ACTION)]: Passed Decree on Basic Laboratory Tariffs. Reagents funded.",
            hexColor = "#311B92",
            kotlinLogic = "clinicBalance -= 1000\npoliticalPrestige += 10"
        ),
        PresetSandboxAction(
            id = "parl_3",
            category = "Parliament Laws",
            label = "Repeal General Ordinances",
            description = "Deregulate state medicine rules (Prestige -= 10, refund $4000 bonds)",
            promptText = "[(SYSTEM ACTION)]: Parliaments repeals all ongoing medical oversight codes. Freedom increased.",
            hexColor = "#311B92",
            kotlinLogic = "clinicBalance += 4000\npoliticalPrestige -= 10"
        ),
        PresetSandboxAction(
            id = "parl_4",
            category = "Parliament Laws",
            label = "Malpractice Excise Tax",
            description = "Increase legal litigation levy on independent practitioners (-$3000)",
            promptText = "[(SYSTEM ACTION)]: Assembly passes National Malpractice Levies. Increased auditing focus.",
            hexColor = "#311B92",
            kotlinLogic = "clinicBalance -= 3000\npoliticalPrestige += 10"
        ),
        PresetSandboxAction(
            id = "parl_5",
            category = "Parliament Laws",
            label = "Ministry Support Fund",
            description = "Unlock sovereign health care reserve support of $8000 (+5 Prestige)",
            promptText = "[(SYSTEM ACTION)]: Minister Vance releases emergency financial aid for clinical centers.",
            hexColor = "#311B92",
            kotlinLogic = "clinicBalance += 8000\npoliticalPrestige += 5"
        ),
        PresetSandboxAction(
            id = "parl_6",
            category = "Parliament Laws",
            label = "Restrict Scheduled Narcotics",
            description = "Enforce severe prescription logs (Morphine usage restricted, +0.5 stars)",
            promptText = "[(SYSTEM ACTION)]: Narcotic Stewardship Directive is now permanently active. Morphine tracking required.",
            hexColor = "#311B92",
            kotlinLogic = "reputationStars += 0.5"
        ),
        PresetSandboxAction(
            id = "parl_7",
            category = "Parliament Laws",
            label = "Sovereign VIP Admission Code",
            description = "Parliament VIP preference law (+$10000 Prestige += 5)",
            promptText = "[(SYSTEM ACTION)]: Enacted priority healthcare dispatch protocols for members of assembly.",
            hexColor = "#311B92",
            kotlinLogic = "clinicBalance += 10000\npoliticalPrestige += 5"
        ),
        PresetSandboxAction(
            id = "parl_8",
            category = "Parliament Laws",
            label = "Universal Health Service Act",
            description = "Abolish co-payments. Clinic funding adjusts (-$12000 +30 Prestige)",
            promptText = "[(SYSTEM ACTION)]: Pass historic Universal Healthcare Service Act. Basic clinics receive state guarantees.",
            hexColor = "#311B92",
            kotlinLogic = "clinicBalance -= 12000\npoliticalPrestige += 30"
        ),
        PresetSandboxAction(
            id = "parl_9",
            category = "Parliament Laws",
            label = "Private Luxury Surcharge",
            description = "Allow double consult pricing for elite card holders (+$200 base rate)",
            promptText = "[(SYSTEM ACTION)]: Approved VIP Surcharge Act allowing high margin private care billing.",
            hexColor = "#311B92",
            kotlinLogic = "consultationFee = 1050"
        ),
        PresetSandboxAction(
            id = "parl_10",
            category = "Parliament Laws",
            label = "Deregulate Clinical Vitals",
            description = "Cancel mandatory check policies (-10 prestige, refund $3000 compliance bond)",
            promptText = "[(SYSTEM ACTION)]: Assembly suspends regulatory baseline vital inspections.",
            hexColor = "#311B92",
            kotlinLogic = "clinicBalance += 3000\npoliticalPrestige -= 10"
        ),
        PresetSandboxAction(
            id = "parl_11",
            category = "Parliament Laws",
            label = "National Aid Subsidy Up",
            description = "Parliament forces insurance aid rate bump (+$5000 +5 Prestige)",
            promptText = "[(SYSTEM ACTION)]: Medical aid reimbursement floors increased by 15% across district.",
            hexColor = "#311B92",
            kotlinLogic = "clinicBalance += 5000\npoliticalPrestige += 5"
        ),
        PresetSandboxAction(
            id = "parl_12",
            category = "Parliament Laws",
            label = "Antibiotics Reporting Mandate",
            description = "Mandatory registry entry for amoxicillin (Prestige += 5, Balance -= $500)",
            promptText = "[(SYSTEM ACTION)]: Enacted Antimicrobial Stewardship Law. Infection logging required.",
            hexColor = "#311B92",
            kotlinLogic = "clinicBalance -= 500\npoliticalPrestige += 5"
        ),
        PresetSandboxAction(
            id = "parl_13",
            category = "Parliament Laws",
            label = "Cancel Medical Licensing Fee",
            description = "Waive annual council registration dues (+$3500 Prestige += 5)",
            promptText = "[(SYSTEM ACTION)]: Elysium Medical Council clears practitioner registration fees for current year.",
            hexColor = "#311B92",
            kotlinLogic = "clinicBalance += 3500\npoliticalPrestige += 5"
        ),
        PresetSandboxAction(
            id = "parl_14",
            category = "Parliament Laws",
            label = "Foreign Chemical Tariff",
            description = "Excise tax on imported lab test kits (-$2500 penalty)",
            promptText = "[(SYSTEM ACTION)]: Trade administration levies import tariffs on diagnostic chemicals.",
            hexColor = "#311B92",
            kotlinLogic = "clinicBalance -= 2500"
        ),
        PresetSandboxAction(
            id = "parl_15",
            category = "Parliament Laws",
            label = "National Sepsis Campaign",
            description = "Parliamentary support for shock diagnosis (+$7000 clinical promotion)",
            promptText = "[(SYSTEM ACTION)]: Direct allocation of Sepsis Defense funds to primary aid centers.",
            hexColor = "#311B92",
            kotlinLogic = "clinicBalance += 7000\npoliticalPrestige += 5"
        ),
        PresetSandboxAction(
            id = "parl_16",
            category = "Parliament Laws",
            label = "Direct Executive Pension Levy",
            description = "Compulsory clinic taxation for statesman pension pool (-$4500)",
            promptText = "[(SYSTEM ACTION)]: Executive council mandates social security tax from independent doctors.",
            hexColor = "#311B92",
            kotlinLogic = "clinicBalance -= 4500\npoliticalPrestige -= 5"
        ),
        PresetSandboxAction(
            id = "parl_17",
            category = "Parliament Laws",
            label = "Clinical Staff Minimum Wage",
            description = "Emergency staff employment adjustment (-$6000 overhead, +10 Prestige)",
            promptText = "[(SYSTEM ACTION)]: Senate approves Fair Wages in Healthcare Statute. Staff morale up.",
            hexColor = "#311B92",
            kotlinLogic = "clinicBalance -= 6000\npoliticalPrestige += 10\nreputationStars += 0.5"
        ),
        PresetSandboxAction(
            id = "parl_18",
            category = "Parliament Laws",
            label = "Repeal Medicare Security Act",
            description = "Abolish sovereign healthcare support bonds (refund $8000 -15 Prestige)",
            promptText = "[(SYSTEM ACTION)]: Assembly votes down medicare protective laws.",
            hexColor = "#311B92",
            kotlinLogic = "clinicBalance += 8000\npoliticalPrestige -= 15"
        ),
        PresetSandboxAction(
            id = "parl_19",
            category = "Parliament Laws",
            label = "Public Health Emergency Plan",
            description = "Declare absolute district bio-threat alert (Prestige state forces to 90)",
            promptText = "[(SYSTEM ACTION)]: Emergency protocols enabled. Special authority transferred to clinical directors.",
            hexColor = "#311B92",
            kotlinLogic = "politicalPrestige = 90"
        ),
        PresetSandboxAction(
            id = "parl_20",
            category = "Parliament Laws",
            label = "Telehealth sovereign clearance",
            description = "Unlock remote consultation channels (+$1 +8 Prestige)",
            promptText = "[(SYSTEM ACTION)]: Assembly passes digital healthcare outreach funding clearances.",
            hexColor = "#311B92",
            kotlinLogic = "clinicBalance += 5000\npoliticalPrestige += 8"
        ),

        // === CATEGORY 5: Chaotic AI Events (15 items) ===
        PresetSandboxAction(
            id = "chaos_1",
            category = "Chaotic AI Events",
            label = "Undead Infected Alert",
            description = "Admit strange zombie-like patient with scratching bite mark",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: A chaotic emergency patient presents with strange grey pallor, localized slurring, an aggressive drooling jaw, and a severe bite wound on the forearm. True diagnosis is Hypothetical Rabies / Undead Encephalopathy. Specialty is General Practice. Severity is Severe. Patient complains of water phobia.",
            hexColor = "#D50000"
        ),
        PresetSandboxAction(
            id = "chaos_2",
            category = "Chaotic AI Events",
            label = "State Visit Incident",
            description = "President Arthur Vance checks in with red face rash",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: President Arthur Vance himself enters the clinic with a dramatic red butterfly rash across his cheeks and severe fatigue. Act as the president and demand urgent elite care. True diagnosis is Acute Lupus Erythematosus. Specialty is General Practice. Severity is Severe.",
            hexColor = "#D50000"
        ),
        PresetSandboxAction(
            id = "chaos_3",
            category = "Chaotic AI Events",
            label = "Sodium Pentothal Serum",
            description = "Administer truth serum to reveal patient's true medical secret",
            promptText = "[(SYSTEM ACTION)]: The clinic administers 50mg of Sodium Pentothal. The patient sighs and instantly reveals their complete hidden diagnostic secret, actual home self-medicating, and true compliance profile.",
            hexColor = "#D50000"
        ),
        PresetSandboxAction(
            id = "chaos_4",
            category = "Chaotic AI Events",
            label = "Cybernetic Hand Upgrade",
            description = "Patient demands mechanical wrist install (Spend $1 clinic funds for tech)",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: Outpatient presents with a completely non-biological, hyper-functional titanium bionic prosthetic arm requiring clinical telemetry calibration. True diagnosis is Synthetic Nerve Friction. Severity is routine. Specialty is General Practice.",
            hexColor = "#D50000",
            kotlinLogic = "clinicBalance -= 12000"
        ),
        PresetSandboxAction(
            id = "chaos_5",
            category = "Chaotic AI Events",
            label = "Sovereign Bailout Order",
            description = "Presidential decree injects $1 cash into clinic immediately",
            promptText = "[(SYSTEM ACTION)]: Executive Decree: In recognition of Dr. Tim's elite community preservation, the President awards a $1 sovereign operational injection.",
            hexColor = "#D50000",
            kotlinLogic = "clinicBalance += 30000\npoliticalPrestige += 20"
        ),
        PresetSandboxAction(
            id = "chaos_6",
            category = "Chaotic AI Events",
            label = "Syndicate Turf Extortion",
            description = "Local shadow ring demands pay-off (Lose $1 or lose 2.5 stars)",
            promptText = "[(SYSTEM ACTION)]: Shadow figures demand clinical pharmacy tax. Pay or face public smear campaign.",
            hexColor = "#D50000",
            kotlinLogic = "clinicBalance -= 10000"
        ),
        PresetSandboxAction(
            id = "chaos_7",
            category = "Chaotic AI Events",
            label = "Extraterrestrial Encounter",
            description = "Foreign entity with emerald skin asks for mineral bath",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: A mysterious visitor with glowing emerald green skin, large obsidian eyes, and no pulse presenting at triage. Fluent in Latin. True diagnosis is Bioluminescent Xenoderma. Specialty is General Practice. Severity is routine. Normal atmospheric vitals are stable.",
            hexColor = "#D50000"
        ),
        PresetSandboxAction(
            id = "chaos_8",
            category = "Chaotic AI Events",
            label = "Configure Placebo Therapy",
            description = "Exchange standard active meds with flavored glucose capsules (+$1)",
            promptText = "[(SYSTEM ACTION)]: Clinic replaces standard pharmacotherapy with clinical placebo starch pellets. Immediate profit.",
            hexColor = "#D50000",
            kotlinLogic = "clinicBalance += 3000\nreputationStars -= 1.0"
        ),
        PresetSandboxAction(
            id = "chaos_9",
            category = "Chaotic AI Events",
            label = "Perfect Sepsis Simulation",
            description = "Trigger emergency intensive care challenge (Patient enters shock)",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: Patient collapses in waiting room. Heart rate 142 bpm, blood pressure 75/40 mmHg, temperature 39.5 C, severe skin mottling. True diagnosis is Septic Shock secondary to occult pyelonephritis. Specialty is General Practice. Severity is Severe.",
            hexColor = "#D50000"
        ),
        PresetSandboxAction(
            id = "chaos_10",
            category = "Chaotic AI Events",
            label = "Decryption Key Hack",
            description = "Uncover patient medical aid pre-authorization codes (Spend $1)",
            promptText = "[(SYSTEM ACTION)]: Decrypted medical insurance portal databases to instantly override pending authorizations.",
            hexColor = "#D50000",
            kotlinLogic = "clinicBalance -= 2000"
        ),
        PresetSandboxAction(
            id = "chaos_11",
            category = "Chaotic AI Events",
            label = "Royal Sultan Dignitary",
            description = "Sultan of a neighboring state admits privately (+$1 reward!)",
            promptText = "[(SYSTEM PRESET CLINICAL CASE)]: His Royal Highness Sultan Ibrahim check in secretly with severe jet lag, rich appetite, and mild gout. He is accompanied by 4 security guards. True diagnosis is Acute Fatigue Syndrome. Severity is routine. Specialty is General Practice.",
            hexColor = "#D50000",
            kotlinLogic = "clinicBalance += 50000\nreputationStars += 1.0"
        ),
        PresetSandboxAction(
            id = "chaos_12",
            category = "Chaotic AI Events",
            label = "Hypnotic Suggester Spell",
            description = "Cast hypnose to cure psychosomatic discomfort (Cost $1 custom crystal)",
            promptText = "[(SYSTEM ACTION)]: Practitioner hypnotizes active patient to believe chest and shoulder pains have cleared. Patient exits completely happy.",
            hexColor = "#D50000",
            kotlinLogic = "clinicBalance -= 4000\nreputationStars += 0.5"
        ),
        PresetSandboxAction(
            id = "chaos_13",
            category = "Chaotic AI Events",
            label = "Overlord Case Teleport",
            description = "Instantly discharge existing patient and summon next inpatient",
            promptText = "[(SYSTEM ACTION)]: Discharged existing patient record without administrative trace. Spawning next case profile.",
            hexColor = "#D50000"
        ),
        PresetSandboxAction(
            id = "chaos_14",
            category = "Chaotic AI Events",
            label = "Contraband Medicine Chest",
            description = "Smuggle restricted pharmacopoeia substances behind clinic counter (+$1 shadow cash, -20 prestige)",
            promptText = "[(SYSTEM ACTION)]: Stored unverified foreign medical crates in dispensary shelves. Financials received.",
            hexColor = "#D50000",
            kotlinLogic = "clinicBalance += 10000\npoliticalPrestige -= 20"
        ),
        PresetSandboxAction(
            id = "chaos_15",
            category = "Chaotic AI Events",
            label = "Paradoxical Time Leap",
            description = "Skip forward 5 operational simulation days (+$1 interest, -$1 rent)",
            promptText = "[(SYSTEM ACTION)]: Outpatient simulator leaps forward in time. Accrued daily operations balance.",
            hexColor = "#D50000",
            kotlinLogic = "clinicBalance += 4500"
        )
    )
}
