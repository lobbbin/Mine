package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * Environment & BioShield Drawer - Subsystem 6 of 6.
 * Manages weather factors (neon storms), clinical ventilation atmospheric filtration,
 * bio-shield barrier energy charges, environmental Spore density, and patient physiological stress levels.
 */
object SovereignEnvironmentBioShieldDrawer {

    private val _neonStormFierceIndex = MutableStateFlow(12.8)
    val neonStormFierceIndex: StateFlow<Double> = _neonStormFierceIndex.asStateFlow()

    private val _clinicalVentilationAirQualityPurity = MutableStateFlow(98.5)
    val clinicalVentilationAirQualityPurity: StateFlow<Double> = _clinicalVentilationAirQualityPurity.asStateFlow()

    private val _atmosphericSporeDensityPpm = MutableStateFlow(4.2)
    val atmosphericSporeDensityPpm: StateFlow<Double> = _atmosphericSporeDensityPpm.asStateFlow()

    private val _bioShieldGridEnergyOutputPercentage = MutableStateFlow(94.0)
    val bioShieldGridEnergyOutputPercentage: StateFlow<Double> = _bioShieldGridEnergyOutputPercentage.asStateFlow()

    private val _acidRainpHLevel = MutableStateFlow(4.85)
    val acidRainpHLevel: StateFlow<Double> = _acidRainpHLevel.asStateFlow()

    private val _organDesiccationMultiplier = MutableStateFlow(1.0)
    val organDesiccationMultiplier: StateFlow<Double> = _organDesiccationMultiplier.asStateFlow()

    private val _respiratoryContractionRateScale = MutableStateFlow(0.04)
    val respiratoryContractionRateScale: StateFlow<Double> = _respiratoryContractionRateScale.asStateFlow()

    private val _ozoneLayerDepletionConstant = MutableStateFlow(324.5)
    val ozoneLayerDepletionConstant: StateFlow<Double> = _ozoneLayerDepletionConstant.asStateFlow()

    private val _seismicClinicVibrationAmplitude = MutableStateFlow(1.2)
    val seismicClinicVibrationAmplitude: StateFlow<Double> = _seismicClinicVibrationAmplitude.asStateFlow()

    fun tickSubscription(sporeFluInfectionRate: Double, powerGridStability: Double, rand: Random) {
        // Storm severity cycles over virtual time
        _neonStormFierceIndex.value = (_neonStormFierceIndex.value + rand.nextDouble(-1.5, 3.0)).coerceIn(0.0, 100.0)

        // Ventilation air quality degrades with lower power stability
        _clinicalVentilationAirQualityPurity.value = (powerGridStability - (_atmosphericSporeDensityPpm.value * 0.5) + rand.nextDouble(-1.0, 1.0)).coerceIn(40.0, 100.0)

        val sporeDelta = (sporeFluInfectionRate * 15.0) + (_neonStormFierceIndex.value / 10.0) - (_clinicalVentilationAirQualityPurity.value / 15.0) + rand.nextDouble(-1.0, 2.5)
        _atmosphericSporeDensityPpm.value = (_atmosphericSporeDensityPpm.value + sporeDelta).coerceIn(0.1, 500.0)

        _bioShieldGridEnergyOutputPercentage.value = (powerGridStability * 0.95 + rand.nextDouble(-1.5, 1.5)).coerceIn(10.0, 100.0)

        _acidRainpHLevel.value = (5.5 - (_atmosphericSporeDensityPpm.value / 250.0) + rand.nextDouble(-0.1, 0.15)).coerceIn(2.5, 7.0)

        _organDesiccationMultiplier.value = (1.0 + (_atmosphericSporeDensityPpm.value / 100.0)).coerceIn(1.0, 5.0)
        _respiratoryContractionRateScale.value = (0.02 + (_atmosphericSporeDensityPpm.value * 0.005)).coerceIn(0.001, 1.2)
    }

    fun compileMetrics(): String {
        return """
            - Atmospheric Spore Density: ${String.format("%.1f", _atmosphericSporeDensityPpm.value)} PPM | BioShield Energy Output: ${String.format("%.1f", _bioShieldGridEnergyOutputPercentage.value)}%
            - Neon Storm Fiercement Rate: ${String.format("%.1f", _neonStormFierceIndex.value)}% | Acid Rain pH Indicator: ${String.format("%.2f", _acidRainpHLevel.value)}
        """.trimIndent()
    }
}
