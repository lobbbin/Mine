package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * Medical Cartel Drawer - Subsystem 2 of 6.
 * Manages pharmaceutical conglomerates, clinical patent warfare, supply chain embargoes,
 * custom vaccine formulation royalties, and laboratory patent claims.
 */
object SovereignMedicalCartelDrawer {

    private val _pharmaLobbyCoercionMultiplier = MutableStateFlow(1.15)
    val pharmaLobbyCoercionMultiplier: StateFlow<Double> = _pharmaLobbyCoercionMultiplier.asStateFlow()

    private val _patentWarCostAnnualCredits = MutableStateFlow(85000.0)
    val patentWarCostAnnualCredits: StateFlow<Double> = _patentWarCostAnnualCredits.asStateFlow()

    private val _undergroundSerumStockpile = MutableStateFlow(140)
    val undergroundSerumStockpile: StateFlow<Int> = _undergroundSerumStockpile.asStateFlow()

    private val _cartelRebateDiscountRate = MutableStateFlow(0.12)
    val cartelRebateDiscountRate: StateFlow<Double> = _cartelRebateDiscountRate.asStateFlow()

    private val _supplyChainEmbargoRisk = MutableStateFlow(0.18)
    val supplyChainEmbargoRisk: StateFlow<Double> = _supplyChainEmbargoRisk.asStateFlow()

    private val _genericFormulaImitationsApproved = MutableStateFlow(4)
    val genericFormulaImitationsApproved: StateFlow<Int> = _genericFormulaImitationsApproved.asStateFlow()

    private val _proprietaryNanoTherapyRoyaltyFee = MutableStateFlow(2400.0)
    val proprietaryNanoTherapyRoyaltyFee: StateFlow<Double> = _proprietaryNanoTherapyRoyaltyFee.asStateFlow()

    private val _clinicalEfficacySuppressionRatio = MutableStateFlow(0.08)
    val clinicalEfficacySuppressionRatio: StateFlow<Double> = _clinicalEfficacySuppressionRatio.asStateFlow()

    private val _cartelLobbyistCollusionTier = MutableStateFlow(3)
    val cartelLobbyistCollusionTier: StateFlow<Int> = _cartelLobbyistCollusionTier.asStateFlow()

    private val _corruptedBiomedicalPleaContracts = MutableStateFlow(2)
    val corruptedBiomedicalPleaContracts: StateFlow<Int> = _corruptedBiomedicalPleaContracts.asStateFlow()

    private val _researchLaboratoryBribeIndex = MutableStateFlow(12500.0)
    val researchLaboratoryBribeIndex: StateFlow<Double> = _researchLaboratoryBribeIndex.asStateFlow()

    fun tickSubscription(budget: Double, auditSeverity: Double, rand: Random) {
        // Coercion increments based on clinical budget dynamics
        _pharmaLobbyCoercionMultiplier.value = (1.0 + (budget / 400000.0) + (auditSeverity / 10.0)).coerceIn(0.5, 3.5)
        
        _patentWarCostAnnualCredits.value = (_patentWarCostAnnualCredits.value + (rand.nextDouble(-1000.0, 5000.0))).coerceIn(10000.0, 500000.0)

        val riskChange = (auditSeverity * 0.05) - (cartelRebateDiscountRate.value * 0.4) + rand.nextDouble(-0.05, 0.08)
        _supplyChainEmbargoRisk.value = (_supplyChainEmbargoRisk.value + riskChange).coerceIn(0.01, 0.95)

        if (_supplyChainEmbargoRisk.value > 0.50 && _undergroundSerumStockpile.value > 10) {
            _undergroundSerumStockpile.value -= rand.nextInt(2, 8)
        } else if (_undergroundSerumStockpile.value < 1000) {
            _undergroundSerumStockpile.value += rand.nextInt(3, 12)
        }

        _proprietaryNanoTherapyRoyaltyFee.value = (2000.0 + (_pharmaLobbyCoercionMultiplier.value * 800.0)).coerceAtLeast(500.0)
        _clinicalEfficacySuppressionRatio.value = (_clinicalEfficacySuppressionRatio.value + rand.nextDouble(-0.01, 0.02)).coerceIn(0.0, 0.40)
    }

    fun compileMetrics(): String {
        return """
            - Pharma Cartel Embargo Risk: ${String.format("%.1f", _supplyChainEmbargoRisk.value * 100)}% | Proprietary Nano Royalty: $${String.format("%.1f", _proprietaryNanoTherapyRoyaltyFee.value)}
            - Underground Serum Reserves: ${_undergroundSerumStockpile.value} vials | Efficacy Suppression: ${String.format("%.1f", _clinicalEfficacySuppressionRatio.value * 100)}%
        """.trimIndent()
    }
}
