package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * Geopolitical Drawer - Subsystem 1 of 6.
 * Manages borders, travel embargoes, regional blockades, trade deficits, and international diplomatic posturing.
 */
object SovereignGeopoliticsDrawer {

    private val _borderThreatLevel = MutableStateFlow(34.5)
    val borderThreatLevel: StateFlow<Double> = _borderThreatLevel.asStateFlow()

    private val _isBorderQuarantineActive = MutableStateFlow(false)
    val isBorderQuarantineActive: StateFlow<Boolean> = _isBorderQuarantineActive.asStateFlow()

    private val _travelVisaEmbargoFactor = MutableStateFlow(1.0)
    val travelVisaEmbargoFactor: StateFlow<Double> = _travelVisaEmbargoFactor.asStateFlow()

    private val _tradeDeficitMillions = MutableStateFlow(240.5)
    val tradeDeficitMillions: StateFlow<Double> = _tradeDeficitMillions.asStateFlow()

    private val _diplomaticAggressionIndex = MutableStateFlow(45.0)
    val diplomaticAggressionIndex: StateFlow<Double> = _diplomaticAggressionIndex.asStateFlow()

    private val _xenogenesisSanctionsImposed = MutableStateFlow(0)
    val xenogenesisSanctionsImposed: StateFlow<Int> = _xenogenesisSanctionsImposed.asStateFlow()

    private val _coalitionSupportCoefficient = MutableStateFlow(0.85)
    val coalitionSupportCoefficient: StateFlow<Double> = _coalitionSupportCoefficient.asStateFlow()

    private val _smugglingInfiltrationChance = MutableStateFlow(0.40)
    val smugglingInfiltrationChance: StateFlow<Double> = _smugglingInfiltrationChance.asStateFlow()

    private val _foreignAidCreditsFlux = MutableStateFlow(50000.0)
    val foreignAidCreditsFlux: StateFlow<Double> = _foreignAidCreditsFlux.asStateFlow()

    private val _offshoreEspionageSecurityShield = MutableStateFlow(72.5)
    val offshoreEspionageSecurityShield: StateFlow<Double> = _offshoreEspionageSecurityShield.asStateFlow()

    private val _neutralGeopoliticalZoneRep = MutableStateFlow(50.0)
    val neutralGeopoliticalZoneRep: StateFlow<Double> = _neutralGeopoliticalZoneRep.asStateFlow()

    private val _borderIncursionAttemptsCount = MutableStateFlow(12)
    val borderIncursionAttemptsCount: StateFlow<Int> = _borderIncursionAttemptsCount.asStateFlow()

    private val _weaponizedPathogenVulnerability = MutableStateFlow(0.15)
    val weaponizedPathogenVulnerability: StateFlow<Double> = _weaponizedPathogenVulnerability.asStateFlow()

    private val _foreignPharmaceuticalGraftScale = MutableStateFlow(0.38)
    val foreignPharmaceuticalGraftScale: StateFlow<Double> = _foreignPharmaceuticalGraftScale.asStateFlow()

    fun tickSubscription(panic: Double, suspicion: Double, rand: Random) {
        // Border panic and threat cascade mechanics
        val threatChange = (panic * 0.25) - (coalitionSupportCoefficient.value * 5.0) + rand.nextDouble(-2.0, 3.5)
        _borderThreatLevel.value = (_borderThreatLevel.value + threatChange).coerceIn(0.0, 100.0)

        if (_borderThreatLevel.value > 60.0) {
            _isBorderQuarantineActive.value = true
            _travelVisaEmbargoFactor.value = (1.5 + (_borderThreatLevel.value / 100.0)).coerceIn(1.0, 3.0)
        } else {
            _isBorderQuarantineActive.value = false
            _travelVisaEmbargoFactor.value = (1.0 + (_borderThreatLevel.value / 200.0)).coerceIn(1.0, 1.8)
        }

        _diplomaticAggressionIndex.value = (_diplomaticAggressionIndex.value + (suspicion * 0.15) + rand.nextDouble(-3.0, 4.0)).coerceIn(0.0, 100.0)
        _tradeDeficitMillions.value = (_tradeDeficitMillions.value + (_travelVisaEmbargoFactor.value * 12.0) + rand.nextDouble(-5.0, 15.0)).coerceIn(5.0, 1000.0)

        // Foreign Aid fluctuates
        val aidGain = if (_diplomaticAggressionIndex.value < 40.0) rand.nextDouble(1000.0, 5000.0) else -rand.nextDouble(2000.0, 8000.0)
        _foreignAidCreditsFlux.value = (_foreignAidCreditsFlux.value + aidGain).coerceAtLeast(0.0)

        if (rand.nextDouble() < 0.12) {
            _borderIncursionAttemptsCount.value += 1
            _smugglingInfiltrationChance.value = (_smugglingInfiltrationChance.value + 0.05).coerceIn(0.10, 0.95)
        }
        
        _offshoreEspionageSecurityShield.value = (100.0 - (_diplomaticAggressionIndex.value * 0.5)).coerceIn(10.0, 100.0)
    }

    fun compileMetrics(): String {
        return """
            - Frontier Combat Threat: ${String.format("%.1f", _borderThreatLevel.value)}% | Quarantined Border Status: ${_isBorderQuarantineActive.value}
            - Diplomatic Aggression Coef: ${String.format("%.1f", _diplomaticAggressionIndex.value)}/100 | Foreign Aid Flux: $${String.format("%.1f", _foreignAidCreditsFlux.value)}
        """.trimIndent()
    }
}
