package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * Legal Judiciary Drawer - Subsystem 4 of 6.
 * Manages constitutional lawsuits, executive decrees, jury blackmail modifiers,
 * sovereign prosecutor indictment indexes, and defense counsel bribe caches.
 */
object SovereignLegalJudiciaryDrawer {

    private val _constitutionalGrievancesFiled = MutableStateFlow(3)
    val constitutionalGrievancesFiled: StateFlow<Int> = _constitutionalGrievancesFiled.asStateFlow()

    private val _executiveDecreeVetoResistance = MutableStateFlow(62.5)
    val executiveDecreeVetoResistance: StateFlow<Double> = _executiveDecreeVetoResistance.asStateFlow()

    private val _sovereignProsecutorSeizureScent = MutableStateFlow(12.4)
    val sovereignProsecutorSeizureScent: StateFlow<Double> = _sovereignProsecutorSeizureScent.asStateFlow()

    private val _bribeCacheExhaustedCredits = MutableStateFlow(24000.0)
    val bribeCacheExhaustedCredits: StateFlow<Double> = _bribeCacheExhaustedCredits.asStateFlow()

    private val _habeasCorpusSuspensionTier = MutableStateFlow(0)
    val habeasCorpusSuspensionTier: StateFlow<Int> = _habeasCorpusSuspensionTier.asStateFlow()

    private val _juryExtortionSuccessProbability = MutableStateFlow(0.55)
    val juryExtortionSuccessProbability: StateFlow<Double> = _juryExtortionSuccessProbability.asStateFlow()

    private val _malpracticeInquestAlertRating = MutableStateFlow(18.5)
    val malpracticeInquestAlertRating: StateFlow<Double> = _malpracticeInquestAlertRating.asStateFlow()

    private val _grandJuryBriberyIndex = MutableStateFlow(4200.0)
    val grandJuryBriberyIndex: StateFlow<Double> = _grandJuryBriberyIndex.asStateFlow()

    private val _dictatorshipAdherenceRating = MutableStateFlow(34.8)
    val dictatorshipAdherenceRating: StateFlow<Double> = _dictatorshipAdherenceRating.asStateFlow()

    fun tickSubscription(suspicionIndex: Double, revenueEarned: Double, rand: Random) {
        // Grievances filed increase based on suspicion
        if (suspicionIndex > 50.0 && rand.nextDouble() < 0.20) {
            _constitutionalGrievancesFiled.value += 1
        }

        _sovereignProsecutorSeizureScent.value = (suspicionIndex * 1.3 + rand.nextDouble(-1.5, 3.0)).coerceIn(5.0, 100.0)
        
        if (revenueEarned > 2000.0) {
            _bribeCacheExhaustedCredits.value += revenueEarned * 0.40
        }

        _juryExtortionSuccessProbability.value = (0.3 + (_bribeCacheExhaustedCredits.value / 100000.0) - (_sovereignProsecutorSeizureScent.value / 250.0)).coerceIn(0.1, 0.95)
        
        _executiveDecreeVetoResistance.value = (50.0 + (_bribeCacheExhaustedCredits.value / 50000.0) - suspicionIndex).coerceIn(10.0, 100.0)

        _malpracticeInquestAlertRating.value = (_sovereignProsecutorSeizureScent.value * 1.1 + rand.nextDouble(-2.0, 4.0)).coerceIn(0.0, 100.0)

        if (_malpracticeInquestAlertRating.value > 75.0) {
            _habeasCorpusSuspensionTier.value = 1
            _dictatorshipAdherenceRating.value = (_dictatorshipAdherenceRating.value + 2.5).coerceIn(0.0, 100.0)
        } else {
            _habeasCorpusSuspensionTier.value = 0
            _dictatorshipAdherenceRating.value = (_dictatorshipAdherenceRating.value - 0.5).coerceIn(0.0, 100.0)
        }
    }

    fun compileMetrics(): String {
        return """
            - Legal Grievances: ${_constitutionalGrievancesFiled.value} | Grand Prosecutor Seizure Scent: ${String.format("%.1f", _sovereignProsecutorSeizureScent.value)}/100
            - Jury Extortion Protection Chance: ${String.format("%.1f", _juryExtortionSuccessProbability.value * 100)}% | Executive Decree Strength: ${String.format("%.1f", _executiveDecreeVetoResistance.value)}%
        """.trimIndent()
    }
}
