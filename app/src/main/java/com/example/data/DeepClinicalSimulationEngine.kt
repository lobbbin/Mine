package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * High-fidelity Deep Sandbox Clinical Simulation Subsystem.
 * Models over 80+ parameters handling geoclinical syndemics, sub-rosa lobby lobbying,
 * deep state inspect audits, pharmaceutical cartels, laboratory diagnostics data, and clinical microeconomics.
 * Integrated completely via local state memory flows, cascading into AI evaluations and PDF summaries.
 */
object DeepClinicalSimulationEngine {

    // --- STRUCTURAL ENHANCEMENTS & SUB-SYSTEM STATE (80 PARAMETERS) ---

    // 1. Geoclinical Syndemics (15 fields)
    private val _sporeFluInfectionRate = MutableStateFlow(0.125)
    val sporeFluInfectionRate: StateFlow<Double> = _sporeFluInfectionRate.asStateFlow()

    private val _microParasiteIndex = MutableStateFlow(0.045)
    val microParasiteIndex: StateFlow<Double> = _microParasiteIndex.asStateFlow()

    private val _neonFeverIncidence = MutableStateFlow(42.8)
    val neonFeverIncidence: StateFlow<Double> = _neonFeverIncidence.asStateFlow()

    private val _quantumPathogenLoad = MutableStateFlow(11.4)
    val quantumPathogenLoad: StateFlow<Double> = _quantumPathogenLoad.asStateFlow()

    private val _sovereignVaccineReserveQuotas = MutableStateFlow(1200)
    val sovereignVaccineReserveQuotas: StateFlow<Int> = _sovereignVaccineReserveQuotas.asStateFlow()

    private val _clinicalMortalityIndex = MutableStateFlow(1.02)
    val clinicalMortalityIndex: StateFlow<Double> = _clinicalMortalityIndex.asStateFlow()

    private val _publicPanicScore = MutableStateFlow(18.5)
    val publicPanicScore: StateFlow<Double> = _publicPanicScore.asStateFlow()

    private val _quarantineStrictnessLevel = MutableStateFlow(2)
    val quarantineStrictnessLevel: StateFlow<Int> = _quarantineStrictnessLevel.asStateFlow()

    private val _respiratoryPathologyVector = MutableStateFlow(0.35)
    val respiratoryPathologyVector: StateFlow<Double> = _respiratoryPathologyVector.asStateFlow()

    private val _neurotoxinBiosignIndex = MutableStateFlow(14.2)
    val neurotoxinBiosignIndex: StateFlow<Double> = _neurotoxinBiosignIndex.asStateFlow()

    private val _cytokineCascadeRatio = MutableStateFlow(0.08)
    val cytokineCascadeRatio: StateFlow<Double> = _cytokineCascadeRatio.asStateFlow()

    private val _viralMutagenicityMultiplier = MutableStateFlow(1.15)
    val viralMutagenicityMultiplier: StateFlow<Double> = _viralMutagenicityMultiplier.asStateFlow()

    private val _bacteriophageAbundance = MutableStateFlow(750.0)
    val bacteriophageAbundance: StateFlow<Double> = _bacteriophageAbundance.asStateFlow()

    private val _clinicalBioShieldIntegrity = MutableStateFlow(92.5)
    val clinicalBioShieldIntegrity: StateFlow<Double> = _clinicalBioShieldIntegrity.asStateFlow()

    private val _activeSymptomVariance = MutableStateFlow(2.4)
    val activeSymptomVariance: StateFlow<Double> = _activeSymptomVariance.asStateFlow()


    // 2. Lobbying & Pharmaceutical Cartels (15 fields)
    private val _darkMoneyProgressiveLobbyFund = MutableStateFlow(150000.0)
    val darkMoneyProgressiveLobbyFund: StateFlow<Double> = _darkMoneyProgressiveLobbyFund.asStateFlow()

    private val _darkMoneyConservativeLobbyFund = MutableStateFlow(120000.0)
    val darkMoneyConservativeLobbyFund: StateFlow<Double> = _darkMoneyConservativeLobbyFund.asStateFlow()

    private val _corporatePharmaBribeBudgets = MutableStateFlow(45000.0)
    val corporatePharmaBribeBudgets: StateFlow<Double> = _corporatePharmaBribeBudgets.asStateFlow()

    private val _regulatorySubversionSuccessChance = MutableStateFlow(0.65)
    val regulatorySubversionSuccessChance: StateFlow<Double> = _regulatorySubversionSuccessChance.asStateFlow()

    private val _politicalBlackmailTelemetryCount = MutableStateFlow(3)
    val politicalBlackmailTelemetryCount: StateFlow<Int> = _politicalBlackmailTelemetryCount.asStateFlow()

    private val _sovereignOversightPenaltyFactor = MutableStateFlow(1.25)
    val sovereignOversightPenaltyFactor: StateFlow<Double> = _sovereignOversightPenaltyFactor.asStateFlow()

    private val _corporateLobbyistSentimentRatio = MutableStateFlow(0.72)
    val corporateLobbyistSentimentRatio: StateFlow<Double> = _corporateLobbyistSentimentRatio.asStateFlow()

    private val _politicalActionCommitteeHoldings = MutableStateFlow(320000.0)
    val politicalActionCommitteeHoldings: StateFlow<Double> = _politicalActionCommitteeHoldings.asStateFlow()

    private val _bribeEfficiencyRatio = MutableStateFlow(1.10)
    val bribeEfficiencyRatio: StateFlow<Double> = _bribeEfficiencyRatio.asStateFlow()

    private val _legislativeVetoStrength = MutableStateFlow(5.5)
    val legislativeVetoStrength: StateFlow<Double> = _legislativeVetoStrength.asStateFlow()

    private val _corporateTaxEvasionIndex = MutableStateFlow(3.8)
    val corporateTaxEvasionIndex: StateFlow<Double> = _corporateTaxEvasionIndex.asStateFlow()

    private val _superPacEndorsementsCount = MutableStateFlow(14)
    val superPacEndorsementsCount: StateFlow<Int> = _superPacEndorsementsCount.asStateFlow()

    private val _subRosaLobbyistAffiliationTier = MutableStateFlow(2)
    val subRosaLobbyistAffiliationTier: StateFlow<Int> = _subRosaLobbyistAffiliationTier.asStateFlow()

    private val _treasuryKickbackRatio = MutableStateFlow(0.04)
    val treasuryKickbackRatio: StateFlow<Double> = _treasuryKickbackRatio.asStateFlow()

    private val _conglomerateRegulatoryExemptionRating = MutableStateFlow(1.5)
    val conglomerateRegulatoryExemptionRating: StateFlow<Double> = _conglomerateRegulatoryExemptionRating.asStateFlow()


    // 3. Deep State Intelligence & Inspections (15 fields)
    private val _clinicalAuditSeverityVector = MutableStateFlow(1.1)
    val clinicalAuditSeverityVector: StateFlow<Double> = _clinicalAuditSeverityVector.asStateFlow()

    private val _supremeCourtInjunctionThreat = MutableStateFlow(12.5)
    val supremeCourtInjunctionThreat: StateFlow<Double> = _supremeCourtInjunctionThreat.asStateFlow()

    private val _sovereignInvestigatorBribeScore = MutableStateFlow(1500)
    val sovereignInvestigatorBribeScore: StateFlow<Int> = _sovereignInvestigatorBribeScore.asStateFlow()

    private val _contrabandClassificationTier = MutableStateFlow(1)
    val contrabandClassificationTier: StateFlow<Int> = _contrabandClassificationTier.asStateFlow()

    private val _suspicionIndexRating = MutableStateFlow(24.5)
    val suspicionIndexRating: StateFlow<Double> = _suspicionIndexRating.asStateFlow()

    private val _deepStateComplianceRatingIndex = MutableStateFlow(88.0)
    val deepStateComplianceRatingIndex: StateFlow<Double> = _deepStateComplianceRatingIndex.asStateFlow()

    private val _classifiedDossiersInterceptedCount = MutableStateFlow(2)
    val classifiedDossiersInterceptedCount: StateFlow<Int> = _classifiedDossiersInterceptedCount.asStateFlow()

    private val _tribunalCoercionResistance = MutableStateFlow(78.5)
    val tribunalCoercionResistance: StateFlow<Double> = _tribunalCoercionResistance.asStateFlow()

    private val _stateCensorshipAdherenceRatio = MutableStateFlow(0.95)
    val stateCensorshipAdherenceRatio: StateFlow<Double> = _stateCensorshipAdherenceRatio.asStateFlow()

    private val _clandestineAssetSeizureRisk = MutableStateFlow(0.05)
    val clandestineAssetSeizureRisk: StateFlow<Double> = _clandestineAssetSeizureRisk.asStateFlow()

    private val _auditorPacificationModifier = MutableStateFlow(1.0)
    val auditorPacificationModifier: StateFlow<Double> = _auditorPacificationModifier.asStateFlow()

    private val _sovereignInterrogationAnxietyIndex = MutableStateFlow(14.0)
    val sovereignInterrogationAnxietyIndex: StateFlow<Double> = _sovereignInterrogationAnxietyIndex.asStateFlow()

    private val _deepStateAgentCoverageFactor = MutableStateFlow(0.12)
    val deepStateAgentCoverageFactor: StateFlow<Double> = _deepStateAgentCoverageFactor.asStateFlow()

    private val _stateIntelligenceEspionagePenetration = MutableStateFlow(0.08)
    val stateIntelligenceEspionagePenetration: StateFlow<Double> = _stateIntelligenceEspionagePenetration.asStateFlow()

    private val _clandestineDirectivesCompleted = MutableStateFlow(4)
    val clandestineDirectivesCompleted: StateFlow<Int> = _clandestineDirectivesCompleted.asStateFlow()


    // 4. Advanced Fictional Laboratory Assays (15 fields)
    private val _luminescentGeneSeqSuccessRatio = MutableStateFlow(0.985)
    val luminescentGeneSeqSuccessRatio: StateFlow<Double> = _luminescentGeneSeqSuccessRatio.asStateFlow()

    private val _quantumBloodAnomaliesCount = MutableStateFlow(2)
    val quantumBloodAnomaliesCount: StateFlow<Int> = _quantumBloodAnomaliesCount.asStateFlow()

    private val _spectralUrineToxicityLevel = MutableStateFlow(0.18)
    val spectralUrineToxicityLevel: StateFlow<Double> = _spectralUrineToxicityLevel.asStateFlow()

    private val _antiphospholipidSyndromicRatio = MutableStateFlow(0.035)
    val antiphospholipidSyndromicRatio: StateFlow<Double> = _antiphospholipidSyndromicRatio.asStateFlow()

    private val _bioFluorescenceSymptomLoad = MutableStateFlow(1.1)
    val bioFluorescenceSymptomLoad: StateFlow<Double> = _bioFluorescenceSymptomLoad.asStateFlow()

    private val _cellularMutagenVariance = MutableStateFlow(0.024)
    val cellularMutagenVariance: StateFlow<Double> = _cellularMutagenVariance.asStateFlow()

    private val _xenogenicPathologyLoad = MutableStateFlow(1.08)
    val xenogenicPathologyLoad: StateFlow<Double> = _xenogenicPathologyLoad.asStateFlow()

    private val _syntheticEnzymeAbnormalityRate = MutableStateFlow(3.8)
    val syntheticEnzymeAbnormalityRate: StateFlow<Double> = _syntheticEnzymeAbnormalityRate.asStateFlow()

    private val _epigeneticSubversionIndex = MutableStateFlow(1.3)
    val epigeneticSubversionIndex: StateFlow<Double> = _epigeneticSubversionIndex.asStateFlow()

    private val _spectralLuminescenceEfficiency = MutableStateFlow(84.5)
    val spectralLuminescenceEfficiency: StateFlow<Double> = _spectralLuminescenceEfficiency.asStateFlow()

    private val _mitochondrialOverchargeFactor = MutableStateFlow(1.0)
    val mitochondrialOverchargeFactor: StateFlow<Double> = _mitochondrialOverchargeFactor.asStateFlow()

    private val _prionDecombustionProbability = MutableStateFlow(0.005)
    val prionDecombustionProbability: StateFlow<Double> = _prionDecombustionProbability.asStateFlow()

    private val _nanoRobotToxicityCoefficient = MutableStateFlow(0.42)
    val nanoRobotToxicityCoefficient: StateFlow<Double> = _nanoRobotToxicityCoefficient.asStateFlow()

    private val _cerebrospinalPressureFluctuation = MutableStateFlow(5.5)
    val cerebrospinalPressureFluctuation: StateFlow<Double> = _cerebrospinalPressureFluctuation.asStateFlow()

    private val _cytochemicalAnomalyScore = MutableStateFlow(0.11)
    val cytochemicalAnomalyScore: StateFlow<Double> = _cytochemicalAnomalyScore.asStateFlow()


    // 5. Clinical Microeconomics & Generator Grid (10 fields)
    private val _hydrocarbonFuelCostIndex = MutableStateFlow(1.24)
    val hydrocarbonFuelCostIndex: StateFlow<Double> = _hydrocarbonFuelCostIndex.asStateFlow()

    private val _hospitalPowerGridStabilityIndex = MutableStateFlow(98.4)
    val hospitalPowerGridStabilityIndex: StateFlow<Double> = _hospitalPowerGridStabilityIndex.asStateFlow()

    private val _sovereignTaxDodgingMultiplier = MutableStateFlow(0.90)
    val sovereignTaxDodgingMultiplier: StateFlow<Double> = _sovereignTaxDodgingMultiplier.asStateFlow()

    private val _clinicPayrollInflationRate = MutableStateFlow(0.042)
    val clinicPayrollInflationRate: StateFlow<Double> = _clinicPayrollInflationRate.asStateFlow()

    private val _blackMarketCreditsSlippage = MutableStateFlow(0.15)
    val blackMarketCreditsSlippage: StateFlow<Double> = _blackMarketCreditsSlippage.asStateFlow()

    private val _monetaryHyperinflationFactor = MutableStateFlow(1.03)
    val monetaryHyperinflationFactor: StateFlow<Double> = _monetaryHyperinflationFactor.asStateFlow()

    private val _emergencyGeneratorReserveHours = MutableStateFlow(48)
    val emergencyGeneratorReserveHours: StateFlow<Int> = _emergencyGeneratorReserveHours.asStateFlow()

    private val _underTheTableConsultsCount = MutableStateFlow(14)
    val underTheTableConsultsCount: StateFlow<Int> = _underTheTableConsultsCount.asStateFlow()

    private val _revenueEvasionSurchargeCredits = MutableStateFlow(15000.0)
    val revenueEvasionSurchargeCredits: StateFlow<Double> = _revenueEvasionSurchargeCredits.asStateFlow()

    private val _regulatoryFinesRepaymentPace = MutableStateFlow(3)
    val regulatoryFinesRepaymentPace: StateFlow<Int> = _regulatoryFinesRepaymentPace.asStateFlow()


    // 6. Patient Biological Degradation & Chrono-Stress (10 fields)
    private val _biologicalDecayVarianceCoef = MutableStateFlow(0.015)
    val biologicalDecayVarianceCoef: StateFlow<Double> = _biologicalDecayVarianceCoef.asStateFlow()

    private val _cellularApoptosisVelocity = MutableStateFlow(0.010)
    val cellularApoptosisVelocity: StateFlow<Double> = _cellularApoptosisVelocity.asStateFlow()

    private val _neuralDegenerationIncidenceRatio = MutableStateFlow(1.8)
    val neuralDegenerationIncidenceRatio: StateFlow<Double> = _neuralDegenerationIncenerationFlow()

    private fun _neuralDegenerationIncenerationFlow(): StateFlow<Double> {
        return MutableStateFlow(1.8).asStateFlow()
    }

    private val _myocardialNecrosisMultiplier = MutableStateFlow(1.05)
    val myocardialNecrosisMultiplier: StateFlow<Double> = _myocardialNecrosisMultiplier.asStateFlow()

    private val _pulmonaryFibrosisIndex = MutableStateFlow(8.4)
    val pulmonaryFibrosisIndex: StateFlow<Double> = _pulmonaryFibrosisIndex.asStateFlow()

    private val _metabolicAcidosisStressIndex = MutableStateFlow(0.22)
    val metabolicAcidosisStressIndex: StateFlow<Double> = _metabolicAcidosisStressIndex.asStateFlow()

    private val _vascularPermeabilityDeclineState = MutableStateFlow(0.12)
    val vascularPermeabilityDeclineState: StateFlow<Double> = _vascularPermeabilityDeclineState.asStateFlow()

    private val _atypicalHyperthermiaVibeRatio = MutableStateFlow(0.04)
    val atypicalHyperthermiaVibeRatio: StateFlow<Double> = _atypicalHyperthermiaVibeRatio.asStateFlow()

    private val _organAtmosphereDeprivationScalar = MutableStateFlow(1.0)
    val organAtmosphereDeprivationScalar: StateFlow<Double> = _organAtmosphereDeprivationScalar.asStateFlow()

    private val _geneticLocusDefragmentationRate = MutableStateFlow(0.002)
    val geneticLocusDefragmentationRate: StateFlow<Double> = _geneticLocusDefragmentationRate.asStateFlow()

    // Total = 15 + 15 + 15 + 15 + 10 + 10 = 80 fields!

    // --- CRADLE & EVOLVING LIFE CYCLES SYSTEM ---

    /**
     * Ticks the entire dystopian engine, simulating interactive cascades as the practitioner resolves 
     * diagnoses, lobbies progressives, runs advanced clinical tests, or attempts bribing Supreme Tribunal investigators.
     */
    fun tickGameStateSandbox(
        patientsSeen: Int,
        lastReputationDelta: Int,
        lastIncomeDelta: Double,
        activeLawsCount: Int
    ) {
        val rand = Random(System.nanoTime())
        
        // 1. Geoclinical Syndemics Dynamic Cascades
        val panicChange = (lastReputationDelta * -1.2) + (activeLawsCount * 0.45) + rand.nextDouble(-1.0, 1.5)
        _publicPanicScore.value = (_publicPanicScore.value + panicChange).coerceIn(0.0, 100.0)

        val infChange = (_publicPanicScore.value / 400.0) - (vaccineReserveDeltaPercentage() * 0.15) + rand.nextDouble(-0.02, 0.03)
        _sporeFluInfectionRate.value = (_sporeFluInfectionRate.value + infChange).coerceIn(0.01, 0.95)

        _neonFeverIncidence.value = (_neonFeverIncidence.value * (1.0 + rand.nextDouble(-0.05, 0.06))).coerceIn(5.0, 500.0)
        _quantumPathogenLoad.value = (_quantumPathogenLoad.value + (_sporeFluInfectionRate.value * 5.0) + rand.nextDouble(-0.5, 0.8)).coerceIn(1.0, 150.0)

        if (_publicPanicScore.value > 50.0 && _sovereignVaccineReserveQuotas.value > 200) {
            _sovereignVaccineReserveQuotas.value -= rand.nextInt(10, 50)
        } else if (_sovereignVaccineReserveQuotas.value < 2000) {
            _sovereignVaccineReserveQuotas.value += rand.nextInt(5, 30)
        }

        _clinicalMortalityIndex.value = (1.0 + (_quantumPathogenLoad.value / 150.0) + (_microParasiteIndex.value * 2.0)).coerceIn(0.8, 3.5)
        _respiratoryPathologyVector.value = (_respiratoryPathologyVector.value * (1.0 + rand.nextDouble(-0.02, 0.03))).coerceIn(0.05, 1.0)
        _clinicalBioShieldIntegrity.value = (100.0 - (_publicPanicScore.value * 0.4) - (_quantumPathogenLoad.value * 0.2)).coerceIn(10.0, 100.0)

        // 2. Pharmaceutical Conglomerate and Lobbying Cascades
        if (lastIncomeDelta > 1000.0) {
            _darkMoneyProgressiveLobbyFund.value += lastIncomeDelta * 1.5
            _darkMoneyConservativeLobbyFund.value += lastIncomeDelta * 1.2
            _corporatePharmaBribeBudgets.value += lastIncomeDelta * 0.5
        } else {
            _darkMoneyProgressiveLobbyFund.value += rand.nextDouble(-100.0, 500.0)
            _darkMoneyConservativeLobbyFund.value += rand.nextDouble(-80.0, 400.0)
        }

        _regulatorySubversionSuccessChance.value = (0.5 + (_corporatePharmaBribeBudgets.value / 250000.0) - (_suspicionIndexRating.value / 200.0)).coerceIn(0.1, 0.98)
        _sovereignOversightPenaltyFactor.value = (1.0 + (_suspicionIndexRating.value / 100.0)).coerceIn(1.0, 4.0)
        _corporateLobbyistSentimentRatio.value = (0.3 + (lastIncomeDelta / 5000.0) + (_corporateTaxEvasionIndex.value / 10.0)).coerceIn(0.05, 1.0)

        // PAC endorsements and Veto shifts
        if (patientsSeen % 5 == 0) {
            _superPacEndorsementsCount.value += rand.nextInt(1, 3)
            _politicalActionCommitteeHoldings.value += rand.nextDouble(1000.0, 5000.0)
        }
        _legislativeVetoStrength.value = (4.0 + (_politicalActionCommitteeHoldings.value / 150000.0)).coerceIn(1.0, 15.0)

        // 3. Deep State & Court Surveillance Ticks
        val suspicionDelta = (if (lastIncomeDelta > 800.0) 1.8 else -0.8) + (activeLawsCount * 0.6) + rand.nextDouble(-1.2, 1.5)
        _suspicionIndexRating.value = (_suspicionIndexRating.value + suspicionDelta).coerceIn(0.0, 100.0)

        _clinicalAuditSeverityVector.value = (0.8 + (_suspicionIndexRating.value / 100.0) + (if (lastReputationDelta < 0) 0.3 else 0.0)).coerceIn(0.5, 3.5)
        _supremeCourtInjunctionThreat.value = (_suspicionIndexRating.value * 1.1 + rand.nextDouble(-2.0, 3.0)).coerceIn(0.0, 100.0)

        if (_suspicionIndexRating.value > 60.0 && rand.nextDouble() < 0.15) {
            _politicalBlackmailTelemetryCount.value += 1
        }
        _deepStateComplianceRatingIndex.value = (100.0 - (_suspicionIndexRating.value * 0.7) - (activeLawsCount * 1.2)).coerceIn(0.0, 100.0)
        _clandestineAssetSeizureRisk.value = (_suspicionIndexRating.value / 250.0).coerceIn(0.0, 0.8)

        // 4. Lab Assays Anomaly Variance Ticks
        _cellularMutagenVariance.value = (0.01 + (_sporeFluInfectionRate.value * 0.05) + rand.nextDouble(-0.005, 0.008)).coerceIn(0.001, 0.1)
        _xenogenicPathologyLoad.value = (0.5 + (_quantumPathogenLoad.value / 80.0) + rand.nextDouble(-0.1, 0.15)).coerceIn(0.1, 8.0)
        _syntheticEnzymeAbnormalityRate.value = (1.0 + (_xenogenicPathologyLoad.value * 1.5) + rand.nextDouble(-0.2, 0.4)).coerceIn(0.0, 25.0)
        _quantumBloodAnomaliesCount.value = (rand.nextInt(0, 3) + (_xenogenicPathologyLoad.value / 2.0).toInt()).coerceAtLeast(0)

        // 5. Fuel Cost Dynamic Microeconomics
        _hydrocarbonFuelCostIndex.value = (1.0 + (_publicPanicScore.value / 150.0) + rand.nextDouble(-0.05, 0.08)).coerceIn(0.5, 3.5)
        _hospitalPowerGridStabilityIndex.value = (100.0 - (if (rand.nextDouble() < 0.08) rand.nextDouble(10.0, 40.0) else rand.nextDouble(0.0, 2.0))).coerceIn(30.0, 100.0)

        if (_hospitalPowerGridStabilityIndex.value < 85.0 && _emergencyGeneratorReserveHours.value > 0) {
            _emergencyGeneratorReserveHours.value -= rand.nextInt(1, 4)
        } else if (_emergencyGeneratorReserveHours.value < 120) {
            _emergencyGeneratorReserveHours.value += rand.nextInt(1, 6)
        }

        // 6. Patient Chrono-Stress Decay
        _cellularApoptosisVelocity.value = (0.005 + (_quantumPathogenLoad.value / 500.0) + rand.nextDouble(-0.002, 0.004)).coerceIn(0.001, 0.05)
        _pulmonaryFibrosisIndex.value = (5.0 + (_respiratoryPathologyVector.value * 15.0) + rand.nextDouble(-0.5, 1.2)).coerceIn(1.0, 35.0)

        // Cascade all 6 custom sub-system drawers synchronously
        DeepStateCascadeCoordinator.cascadeStates(patientsSeen, lastReputationDelta, lastIncomeDelta, activeLawsCount)
    }

    private fun vaccineReserveDeltaPercentage(): Double {
        return _sovereignVaccineReserveQuotas.value / 2000.0
    }

    /**
     * Compiles all sandbox parameters into a rich summary structured perfectly for injecting into AI system prompts,
     * maintaining high intelligence of geopolitical and geoclinical context.
     */
    fun compileAiSystemPromptDirective(): String {
        return """
            #### 🌐 SOVEREIGN GEOCLINICAL SANDBOX METRICS (DEEP-SYSTEM INTELLIGENCE):
            - Spore Flu Epidemic Index: ${String.format("%.3f", _sporeFluInfectionRate.value)} | Quantum Pathogen Biomarker Load: ${String.format("%.1f", _quantumPathogenLoad.value)}
            - Public Containment Panic Rating: ${String.format("%.1f", _publicPanicScore.value)}% | Bio-Shield Integrity Rating: ${String.format("%.1f", _clinicalBioShieldIntegrity.value)}%
            - Bureaucratic Suspicion Scent: ${String.format("%.1f", _suspicionIndexRating.value)}/100 | Supreme Court Injunction Warning level: ${String.format("%.1f", _supremeCourtInjunctionThreat.value)}%
            - Shadow Subversion Index (Bribes): ${String.format("%.2f", _regulatorySubversionSuccessChance.value * 100)}% | Active Blackmail Telemetry folders: ${_politicalBlackmailTelemetryCount.value}
            - Hydrocarbon generator fuel cost multiplier: x${String.format("%.2f", _hydrocarbonFuelCostIndex.value)} | Hospital Emergency grid stability status: ${String.format("%.1f", _hospitalPowerGridStabilityIndex.value)}%
            - Advanced Assay Mutagen Level: ${String.format("%.3f", _cellularMutagenVariance.value)} | Xenogenic Pathology count: ${String.format("%.2f", _xenogenicPathologyLoad.value)}
            - Under-the-table Clinical Logs: ${_underTheTableConsultsCount.value} | Offshore Tax-Evasion hidden reserves: $${String.format("%.1f", _revenueEvasionSurchargeCredits.value)}
        """.trimIndent()
    }

    /**
     * Emits a comprehensive text report of the latest simulations for inserting in the PDF Document.
     */
    fun compilePdfSandboxRegistrySummary(): String {
        return """
            --- SOVEREIGN EXPERIMENTAL CLINICAL REGISTRY REGISTER ---
            
            [SECTION 1: GEOCLINICAL SYNDEMICS]
             Spore Flu Carrier Spread Rate : ${String.format("%.4f", _sporeFluInfectionRate.value)}
             Neon-Fever Citizen Pathology Load : ${String.format("%.2f", _neonFeverIncidence.value)} /100k
             Contamination Micro-Parasite Quotient : ${String.format("%.4f", _microParasiteIndex.value)}
             Bio-Shield Barrier Structural Integrity : ${String.format("%.1f", _clinicalBioShieldIntegrity.value)}%
             State Vaccine Depository Sentry Stock : ${_sovereignVaccineReserveQuotas.value} doses
             Global Public Morbidity Scalar Index : ${String.format("%.3f", _clinicalMortalityIndex.value)}
             Atypical Pathogen Mutability Rating : ${String.format("%.2f", _viralMutagenicityMultiplier.value)}x
             Average Respiratory Spore Dust Factor : ${String.format("%.3f", _respiratoryPathologyVector.value)}
             Urban Waterway Neurotoxin Level : ${String.format("%.1f", _neurotoxinBiosignIndex.value)} ppb
             Luminescent Micro-Phage Abundance : ${String.format("%.1f", _bacteriophageAbundance.value)} G/L
            
            [SECTION 2: PRIVATE PHARMACEUTICAL & CORRAL LOBBY]
             Progressive lobby treasury buffer : $${String.format("%.2f", _darkMoneyProgressiveLobbyFund.value)}
             Traditional cartel lobby buffer : $${String.format("%.2f", _darkMoneyConservativeLobbyFund.value)}
             Dispensary bribery buffer reserves : $${String.format("%.2f", _corporatePharmaBribeBudgets.value)}
             Political Campaign PAC Ledger : $${String.format("%.2f", _politicalActionCommitteeHoldings.value)}
             Lobbyist regulatory subversion coefficient: ${String.format("%.1f", _regulatorySubversionSuccessChance.value * 100)}%
             Supreme Investigator bribery efficiency ratio: ${String.format("%.2f", _bribeEfficiencyRatio.value)}x
             Progressive Coalition Legislative Veto Rating: ${String.format("%.1f", _legislativeVetoStrength.value)} MW
             Private Hospital corporate tax-evasion rating: ${_corporateTaxEvasionIndex.value}/10
             Active Medical Super-PAC media drives : ${_superPacEndorsementsCount.value} runs
             Secret Congressional lobby liaison status : Tier ${_subRosaLobbyistAffiliationTier.value}
             Treasury kickback transaction slice ratio: ${String.format("%.1f", _treasuryKickbackRatio.value * 100)}%
            
            [SECTION 3: COUNTER-INTELLIGENCE & SHADOW AUDITS]
             Clinical auditor severity index : ${String.format("%.2f", _clinicalAuditSeverityVector.value)}x
             Supreme Court injunction hazard meter : ${String.format("%.1f", _supremeCourtInjunctionThreat.value)}%
             Internal Security Agency surveillance index : ${String.format("%.1f", _suspicionIndexRating.value)}/100
             Auditor silent pacification parameter : ${String.format("%.2f", _auditorPacificationModifier.value)}
             Clerical state wiretapping intercept count: ${_classifiedDossiersInterceptedCount.value}
             Clandestine asset seizure hazard multiplier : ${String.format("%.1f", _clandestineAssetSeizureRisk.value * 100)}%
             Atypical tribunal interrogation resistance : ${String.format("%.1f", _tribunalCoercionResistance.value)}%
             Official media censorship compliance ratio : ${String.format("%.1f", _stateCensorshipAdherenceRatio.value * 100)}%
             Shadow agency directives resolved successfully: ${_clandestineDirectivesCompleted.value} Completed
            
            [SECTION 4: ADVANCED SPECTRAL LAB ASSAY TRACERS]
             Luminescent gene-sequencing alignment ratio : ${String.format("%.2f", _luminescentGeneSeqSuccessRatio.value * 100)}%
             Glow-Filament Dark Blood Anomalies : ${_quantumBloodAnomaliesCount.value} mutations
             Spectral fluorescent sediment quotient : ${String.format("%.3f", _spectralUrineToxicityLevel.value)}%
             Heavy cytological cluster clotting ratio : ${String.format("%.4f", _antiphospholipidSyndromicRatio.value)}
             Atopic bio-fluorescent symptom hazard score : ${String.format("%.2f", _bioFluorescenceSymptomLoad.value)}
             Mitochondrial epinephrine overdrive capacity : ${String.format("%.1f", _mitochondrialOverchargeFactor.value * 100)}%
             Nanofluidic vascular toxicity coefficient : ${String.format("%.3f", _nanoRobotToxicityCoefficient.value)}
             Diagnostic cytochemical deviation anomaly score: ${String.format("%.3f", _cytochemicalAnomalyScore.value)}
             Cerebrospinal pressure fluctuation interval : ${String.format("%.1f", _cerebrospinalPressureFluctuation.value)} mmHg
            
            [SECTION 5: CORPORATE MICRO-ECONOMICS & OFFSHORE]
             Dystopian crude generator fuel price marker: $${String.format("%.2f", _hydrocarbonFuelCostIndex.value)}/L
             Simulated backup emergency power level : ${_emergencyGeneratorReserveHours.value} hours left
             Offshore clinic ledger hidden reserves : $${String.format("%.2f", _revenueEvasionSurchargeCredits.value)}
             Unreported medical services rate : ${_underTheTableConsultsCount.value} sessions
             Clinic operating payroll inflation quotient: ${String.format("%.2f", _clinicPayrollInflationRate.value * 100)}%
            
            [SECTION 6: ATOMISTIC CELLULAR DEGRADATION TRACING]
             Chronic cellular apoptosis velocity speed : ${String.format("%.4f", _cellularApoptosisVelocity.value)}
             Toxic dust induced fibrotic tissue decay marker : ${String.format("%.1f", _pulmonaryFibrosisIndex.value)} AU
             Metabolic acidosis tissue stress vector : ${String.format("%.3f", _metabolicAcidosisStressIndex.value)}
             Vascular structural leakage breakdown scale : ${String.format("%.3f", _metascularStructureRating())}
        """.trimIndent()
    }

    private fun _metascularStructureRating(): Double {
        return _vascularPermeabilityDeclineState.value
    }
}
