package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class GenericDrugMatch(
    val brandName: String,
    val activeIngredient: String,
    val genericName: String,
    val brandPrice: Double,
    val genericPrice: Double,
    val indication: String
) {
    val savingsPercent: Int get() = (((brandPrice - genericPrice) / brandPrice) * 100).toInt()
    val savingsAmount: Double get() = brandPrice - genericPrice
}

val genericDrugsList = listOf(
    GenericDrugMatch("Augmentin 375mg/1000mg", "Amoxicillin/Clavulanic Acid", "Amoclav Generic", 240.0, 132.0, "Bacterial tonsillitis, respiratory or sinus tract infections"),
    GenericDrugMatch("Voltaren 75mg SR", "Diclofenac Sodium", "Diclopen 75mg", 185.0, 74.0, "Inflammatory pain, osteo/rheumatoid arthritis, severe tissue swelling"),
    GenericDrugMatch("Panado 500mg", "Paracetamol", "ParaMed 500mg", 35.0, 24.5, "Mild-to-moderate fever, headache, body aches"),
    GenericDrugMatch("Lipitor 20mg", "Atorvastatin Calcium", "StatLipid 20mg", 310.0, 139.5, "Primary dyslipidaemia, high cholesterol, cardiovascular risk mitigation"),
    GenericDrugMatch("Nexium 40mg", "Esomeprazole", "Esomeprazole Generic", 280.0, 140.0, "Gastro-oesophageal reflux disease (GORD), acid peptic ulcer management"),
    GenericDrugMatch("Ventolin HFA", "Salbutamol", "AeroVent Inhaler", 125.0, 62.5, "Asthma relief, bronchospasms, copd wheezing inhalation"),
    GenericDrugMatch("Zovirax 400mg", "Acyclovir", "Acyclovir Generic", 420.0, 147.0, "Herpes zoster/simplex flare-ups, viral shingles treatment"),
    GenericDrugMatch("Cataflam 50mg", "Diclofenac Potassium", "Diclofenac K 50mg", 160.0, 80.0, "Acute structural pain, dysmenorrhoea, severe migraine headaches"),
    GenericDrugMatch("Klaricid 500mg", "Clarithromycin", "Clarithromycin Generic", 320.0, 192.0, "Atypical pneumonia, chest infection, Helicobacter pylori eradication")
)

@Composable
fun GenericDrugAlternativeAdvisor(
    medsNameCurrent: String,
    modifier: Modifier = Modifier,
    currencySymbol: String = "$",
    onSelectGeneric: (brand: String, generic: String) -> Unit = { _, _ -> }
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text(
                        text = "💊 Generic Drug Advisor",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.Close else Icons.Default.Refresh,
                        contentDescription = "Expand/Collapse",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Text(
                text = "Compare brand names with local generic alternatives. Tap 'Substitute' to copay swap and apply to visual budgeting.",
                fontSize = 10.sp,
                lineHeight = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
            )
            
            if (expanded) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    genericDrugsList.forEach { drug ->
                        // Highlight if current search text or field has any brand/active name
                        val isHighlighted = brandMatches(medsNameCurrent, drug.brandName, drug.activeIngredient)
                        
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = drug.brandName,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Active: ${drug.activeIngredient}",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Used for: ${drug.indication}",
                                            fontSize = 8.sp,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    
                                    Button(
                                        onClick = { onSelectGeneric(drug.brandName, drug.genericName) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Substitute (-${drug.savingsPercent}%)", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Generic: ${drug.genericName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                    Text(
                                        text = "Brand price ${currencySymbol}${drug.brandPrice} ➡️ Generic ${currencySymbol}${drug.genericPrice}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun brandMatches(current: String, brand: String, active: String): Boolean {
    if (current.isBlank()) return false
    val curLower = current.lowercase()
    val brandPart = brand.substringBefore(" ").lowercase()
    val activePart = active.lowercase()
    return curLower.contains(brandPart) || curLower.contains(activePart) || brandPart.contains(curLower)
}
