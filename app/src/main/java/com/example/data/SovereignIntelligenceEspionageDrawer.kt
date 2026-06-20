package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * Intelligence Espionage Drawer - Subsystem 5 of 6.
 * Manages counterintelligence wiretaps, patient medical transcripts,
 * leaked state secrets, diagnostic machine buggings, and secret regulatory blackmail networks.
 */
object SovereignIntelligenceEspionageDrawer {

    private val _wiretapsInterceptedCount = MutableStateFlow(14)
    val wiretapsInterceptedCount: StateFlow<Int> = _wiretapsInterceptedCount.asStateFlow()

    private val _espionagePenetrationRatio = MutableStateFlow(0.12)
    val espionagePenetrationRatio: StateFlow<Double> = _espionagePenetrationRatio.asStateFlow()

    private val _compromisingDossiersArchived = MutableStateFlow(2)
    val compromisingDossiersArchived: StateFlow<Int> = _compromisingDossiersArchived.asStateFlow()

    private val _buggedAcousticTransducers = MutableStateFlow(5)
    val buggedAcousticTransducers: StateFlow<Int> = _buggedAcousticTransducers.asStateFlow()

    private val _clandestineDirectiveSubversionPurity = MutableStateFlow(0.88)
    val clandestineDirectiveSubversionPurity: StateFlow<Double> = _clandestineDirectiveSubversionPurity.asStateFlow()

    private val _auditorBlackmailCapitalCredits = MutableStateFlow(15000.0)
    val auditorBlackmailCapitalCredits: StateFlow<Double> = _auditorBlackmailCapitalCredits.asStateFlow()

    private val _deepWaterSentryUAVsCount = MutableStateFlow(3)
    val deepWaterSentryUAVsCount: StateFlow<Int> = _deepWaterSentryUAVsCount.asStateFlow()

    private val _leakRiskExposureChance = MutableStateFlow(0.05)
    val leakRiskExposureChance: StateFlow<Double> = _leakRiskExposureChance.asStateFlow()

    private val _interceptionSignalToNoiseRatio = MutableStateFlow(18.5)
    val interceptionSignalToNoiseRatio: StateFlow<Double> = _interceptionSignalToNoiseRatio.asStateFlow()

    fun tickSubscription(suspicionIndex: Double, activeLaws: Int, rand: Random) {
        // High suspicion triggers more espionage activities
        if (suspicionIndex > 45.0) {
            _espionagePenetrationRatio.value = (0.1 + (suspicionIndex / 200.0) + rand.nextDouble(-0.02, 0.04)).coerceIn(0.00, 0.95)
            if (rand.nextDouble() < 0.18) {
                _wiretapsInterceptedCount.value += rand.nextInt(1, 4)
                _buggedAcousticTransducers.value += 1
            }
        } else {
            _espionagePenetrationRatio.value = (0.05 + rand.nextDouble(-0.01, 0.02)).coerceAtLeast(0.0)
        }

        if (activeLaws > 3 && rand.nextDouble() < 0.15) {
            _compromisingDossiersArchived.value += 1
            _auditorBlackmailCapitalCredits.value += rand.nextDouble(1000.0, 4000.0)
        }

        _leakRiskExposureChance.value = (_espionagePenetrationRatio.value * 1.2 - (clandestineDirectiveSubversionPurity.value * 0.2)).coerceIn(0.0, 0.85)
        _interceptionSignalToNoiseRatio.value = (15.0 + (_buggedAcousticTransducers.value * 1.5) + rand.nextDouble(-1.0, 2.5)).coerceAtLeast(0.0)
    }

    fun compileMetrics(): String {
        return """
            - Counter Espionage Penetration: ${String.format("%.1f", _espionagePenetrationRatio.value * 100)}% | Active Dossiers Archived: ${_compromisingDossiersArchived.value} dirs
            - Signal Intercept S/N ratio: ${String.format("%.1f", _interceptionSignalToNoiseRatio.value)} dB | Dossier Exposure Hazard: ${String.format("%.1f", _leakRiskExposureChance.value * 100)}%
        """.trimIndent()
    }
}
