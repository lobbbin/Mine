package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * Citizen Behavior Drawer - Subsystem 3 of 6.
 * Manages civil obedience levels, labor union strikes, mass hypochondria symptoms,
 * underground medical self-treatment cults, and overall regulatory directive compliance.
 */
object SovereignCitizenBehaviorDrawer {

    private val _civilObedienceRating = MutableStateFlow(84.5)
    val civilObedienceRating: StateFlow<Double> = _civilObedienceRating.asStateFlow()

    private val _strikeProbabilityIndex = MutableStateFlow(0.12)
    val strikeProbabilityIndex: StateFlow<Double> = _strikeProbabilityIndex.asStateFlow()

    private val _massHypochondriaPrevalence = MutableStateFlow(14.8)
    val massHypochondriaPrevalence: StateFlow<Double> = _massHypochondriaPrevalence.asStateFlow()

    private val _cultSelfTreatmentActiveMembers = MutableStateFlow(1200)
    val cultSelfTreatmentActiveMembers: StateFlow<Int> = _cultSelfTreatmentActiveMembers.asStateFlow()

    private val _antiRegulatoryDirectiveSlogans = MutableStateFlow(String.format("Spore is Nature's Breath!"))
    val antiRegulatoryDirectiveSlogans: StateFlow<String> = _antiRegulatoryDirectiveSlogans.asStateFlow()

    private val _quarantineDefianceIncidentsCount = MutableStateFlow(5)
    val quarantineDefianceIncidentsCount: StateFlow<Int> = _quarantineDefianceIncidentsCount.asStateFlow()

    private val _practitionerFaithIndexMultiplier = MutableStateFlow(1.0)
    val practitionerFaithIndexMultiplier: StateFlow<Double> = _practitionerFaithIndexMultiplier.asStateFlow()

    private val _clandestineHomeophaticSellersCount = MutableStateFlow(24)
    val clandestineHomeophaticSellersCount: StateFlow<Int> = _clandestineHomeophaticSellersCount.asStateFlow()

    private val _citizenHysteriaScreamThreshold = MutableStateFlow(72.0)
    val citizenHysteriaScreamThreshold: StateFlow<Double> = _citizenHysteriaScreamThreshold.asStateFlow()

    fun tickSubscription(publicPanic: Double, activeLaws: Int, rand: Random) {
        // Compute obedience based on panic and active laws
        val obedienceDelta = -(publicPanic * 0.3) + (activeLaws * 1.5) + rand.nextDouble(-2.0, 2.5)
        _civilObedienceRating.value = (_civilObedienceRating.value + obedienceDelta).coerceIn(10.0, 100.0)

        _strikeProbabilityIndex.value = ((100.0 - _civilObedienceRating.value) / 150.0 + (publicPanic / 250.0)).coerceIn(0.0, 0.98)
        
        _massHypochondriaPrevalence.value = (publicPanic * 0.8 + rand.nextDouble(-2.0, 5.0)).coerceIn(5.0, 150.0)

        if (_civilObedienceRating.value < 50.0) {
            _cultSelfTreatmentActiveMembers.value += rand.nextInt(10, 80)
            _clandestineHomeophaticSellersCount.value += rand.nextInt(1, 3)
            _quarantineDefianceIncidentsCount.value += 1
        } else {
            _cultSelfTreatmentActiveMembers.value = (_cultSelfTreatmentActiveMembers.value - rand.nextInt(5, 30)).coerceAtLeast(100)
        }

        _practitionerFaithIndexMultiplier.value = (_civilObedienceRating.value / 100.0 + 0.2).coerceIn(0.1, 1.5)

        val slogans = listOf(
            "Spore is Nature's Breath!",
            "Regulators take our air!",
            "Fines feed the Grand Counsel!",
            "Elysium needs fresh breathing loops, not audit scripts!"
        )
        if (rand.nextDouble() < 0.25) {
            _antiRegulatoryDirectiveSlogans.value = slogans.random()
        }
    }

    fun compileMetrics(): String {
        return """
            - Citizen Civil Obedience Rate: ${String.format("%.1f", _civilObedienceRating.value)}% | Strike Risk: ${String.format("%.1f", _strikeProbabilityIndex.value * 100)}%
            - Self-Treating Cult Affiliates: ${_cultSelfTreatmentActiveMembers.value} | Latest Riot Slogan: "${_antiRegulatoryDirectiveSlogans.value}"
        """.trimIndent()
    }
}
