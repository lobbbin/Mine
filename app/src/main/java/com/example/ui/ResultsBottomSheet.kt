package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SimulationState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsBottomSheet(
    viewModel: SimulationViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    initialTab: Int = 0
) {
    val uiState by viewModel.uiState.collectAsState()
    val activePolicies by viewModel.activePolicies.collectAsState()
    val disableInsurance = activePolicies.any { it.runtimeConstraints["disableInsurance"] == true }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var selectedTab by remember { mutableStateOf(initialTab) }
    val tabs = listOf("🧪 Labs", "🩺 Px Exam", "📄 Rx & Docs", "💸 Billing", "💯 Scorecard", "👥 Patient Records", "🏦 Ledger")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier.fillMaxHeight(0.85f),
        dragHandle = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Clinical Folders",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_sheet_button")) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close folder")
                }
            }

            // Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (index) {
                                        0 -> Icons.Default.Science
                                        1 -> Icons.Default.Accessibility
                                        2 -> Icons.Default.Description
                                        3 -> Icons.Default.LocalAtm
                                        4 -> Icons.Default.CheckCircle
                                        5 -> Icons.Default.History
                                        else -> Icons.Default.AccountBalance
                                    },
                                    contentDescription = title,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            }
                        },
                        modifier = Modifier.testTag("sheet_tab_$index")
                    )
                }
            }

            // Tab Content Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> LabsTabContent(uiState)
                    1 -> PhysicalsTabContent(uiState)
                    2 -> RxDocsTabContent(viewModel, uiState)
                    3 -> BillingTabContent(viewModel, uiState)
                    4 -> EvaluationTabContent(uiState) {
                        viewModel.startNextPatient()
                        onDismiss()
                    }
                    5 -> CaseLogsTabContent(viewModel, onDismiss)
                    else -> LedgerTabContent(viewModel)
                }
            }
        }
    }
}

@Composable
fun PhysicalsTabContent(state: SimulationState) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        if (state.physicalExamResults.isNullOrBlank()) {
            EmptyFolderView(
                title = "No Physical Examination Records",
                icon = Icons.Default.Accessibility,
                description = "Perform a physical exam to see findings generated by the simulation engine."
            )
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Physical Examination Log",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = state.physicalExamResults ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun LabsTabContent(state: SimulationState) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        if (state.labResults.isNullOrBlank()) {
            EmptyFolderView(
                icon = Icons.Default.Science,
                title = "No Labs Ordered",
                description = "Tap 'Order Labs' on the dashboard to request clinical investigations for the patient."
            )
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔬 LABORATORY RESULTS (NHLS METRIC)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = state.labResults,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun BillingTabContent(viewModel: SimulationViewModel, state: SimulationState) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        if (state.billingReceipt.isNullOrBlank()) {
            EmptyFolderView(
                icon = Icons.Default.LocalAtm,
                title = "No Billing Code Generated",
                description = "Correctly diagnose and treat the patient to trigger general practice invoicing."
            )
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FDF9)), // subtle clean accounting paper vibe
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "💳 MEDICAL BILLING RECEIPT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = state.billingReceipt,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun EvaluationTabContent(state: SimulationState, onNextPatient: () -> Unit) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        if (state.evaluation.isNullOrBlank()) {
            EmptyFolderView(
                icon = Icons.Default.Assignment,
                title = "Awaiting Final Submission",
                description = "Complete diagnostic formulations and hit 'Submit' to receive your CPD scoring scorecard."
            )
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            text = "🎓 CPD CLINICAL SCORECARD",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.evaluation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onNextPatient,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("next_patient_button")
            ) {
                Text(
                    text = "Load Next Scenario",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EmptyFolderView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CaseLogsTabContent(viewModel: SimulationViewModel, onDismiss: () -> Unit) {
    val scrollState = rememberScrollState()
    val allEncounters by viewModel.allEncounters.collectAsState(initial = emptyList())
    val completedEncounters = remember(allEncounters) {
        allEncounters.filter { it.isEncounterComplete }
    }

    val activePolicies by viewModel.activePolicies.collectAsState()
    val disableInsurance = activePolicies.any { it.runtimeConstraints["disableInsurance"] == true }

    val groupedPatients = remember(completedEncounters) {
        completedEncounters.groupBy { it.patientDemographics }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        if (groupedPatients.isEmpty()) {
            EmptyFolderView(
                icon = Icons.Default.History,
                title = "No Patient Records Registered Yet",
                description = "Complete diagnostic consultations to automatically open a permanent clinical file jacket in patients' records."
            )
        } else {
            Text(
                text = "👥 ACTIVE PRACTICE PATIENT DIRECTORY (${groupedPatients.size} Registered Files)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            groupedPatients.forEach { (demographics, encounters) ->
                val sortedEncounters = encounters.sortedByDescending { it.timestamp }
                val latestEnc = sortedEncounters.first()
                
                val isDeceased = sortedEncounters.any { it.patientOutcome == "Deceased" || it.patientStability == "Deceased" }
                val isTransferred = sortedEncounters.any { it.patientOutcome == "Transferred Out" || it.patientStability == "Transferred Out" }
                
                val statusText: String
                val statusColor: Color
                if (isDeceased) {
                    statusText = "✝️ Deceased"
                    statusColor = Color(0xFFC62828)
                } else if (isTransferred) {
                    statusText = "🚶 Transferred Out"
                    statusColor = Color(0xFFEF6C00)
                } else {
                    statusText = "🟢 Alive / Stable"
                    statusColor = Color(0xFF2E7D32)
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val hasMRN = demographics.startsWith("Patient: ")
                                if (hasMRN) {
                                    val nameAndMrn = demographics.substring(9).substringBefore(" • ")
                                    val actualDemos = demographics.substringAfter(" • ", "")
                                    Text(
                                        text = nameAndMrn,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (actualDemos.isNotBlank()) {
                                        Text(
                                            text = "Demographics: $actualDemos",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = demographics,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (!disableInsurance) {
                                    Text(
                                        text = "Insurance: ${latestEnc.insuranceStatus}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(statusColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = statusColor
                                    )
                                }
                                
                                IconButton(
                                    onClick = { viewModel.auditPatientFolder(demographics) },
                                    modifier = Modifier.size(28.dp).testTag("audit_folder_${latestEnc.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Science,
                                        contentDescription = "Audit Patient Folder",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.deletePatientRecordFolder(demographics) },
                                    modifier = Modifier.size(28.dp).testTag("delete_folder_${latestEnc.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Patient Folder",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "📋 CONSULTATION HISTORY (${sortedEncounters.size} Visited Cases)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        sortedEncounters.forEachIndexed { idx, enc ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Visit #${sortedEncounters.size - idx}: ${enc.trueDiagnosis}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        
                                        val scorePattern = java.util.regex.Pattern.compile("(\\d{1,3})/100")
                                        val scoreMatcher = enc.evaluation?.let { scorePattern.matcher(it) }
                                        val scoreVal = if (scoreMatcher?.find() == true) {
                                            scoreMatcher.group(1).toIntOrNull()
                                        } else null
                                        
                                        scoreVal?.let { s ->
                                            Text(
                                                text = "CPD Rating: $s/100",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (s >= 75) Color(0xFF2E7D32) else if (s >= 50) Color(0xFFEF6C00) else Color(0xFFC62828)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Presented with chief complaint: \"${enc.chiefComplaint}\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (!enc.prescriptionString.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "💊 Rx Prescribed: Approved",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    if (!enc.referralLetterString.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "🚑 Specialist Referral: Issued",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }

                                    if (!enc.sickNoteString.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "📝 Medical Certificate: Excused",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }

                                    var showCritique by remember { mutableStateOf(false) }
                                    if (showCritique) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                .padding(8.dp)
                                        ) {
                                            Text(
                                                text = enc.evaluation ?: "No critique recorded.",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = { showCritique = !showCritique },
                                            colors = ButtonDefaults.filledTonalButtonColors(),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text(
                                                text = if (showCritique) "Hide Audit" else "View Audit Details",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.recallEncounterAsReturning(enc)
                                                onDismiss()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondary
                                            ),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                                            modifier = Modifier.height(28.dp).testTag("recontact_${enc.id}")
                                        ) {
                                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "📞 Recontact",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.deleteEncounter(enc.id) },
                                            modifier = Modifier.size(28.dp).testTag("delete_visit_${enc.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Visit",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
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
    }
}

@Composable
fun LedgerTabContent(viewModel: SimulationViewModel) {
    val scrollState = rememberScrollState()
    val balance by viewModel.clinicBalance.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val currencyCode by viewModel.currencyCode.collectAsState()
    val reputation by viewModel.reputationStars.collectAsState()
    val allEncounters by viewModel.allEncounters.collectAsState(initial = emptyList())
    val activePolicies by viewModel.activePolicies.collectAsState()
    val disableInsurance = activePolicies.any { it.runtimeConstraints["disableInsurance"] == true }
    
    val completedCases = allEncounters.filter { it.isEncounterComplete }
    val totalRevenue = completedCases.sumOf { it.revenueEarned }
    val totalPatients = completedCases.size

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🏦 CLINIC FINANCIAL LEDGER",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Bank Balance:", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    Text(
                        text = String.format("$currencySymbol %.2f $currencyCode", balance),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (balance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Clinic Reputation / Patient Satisfaction:", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    Text(
                        text = String.format("%.1f ⭐️", reputation),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF57F17)
                    )
                }
            }
        }
        
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Historical Overview",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Patients Seen (All Time):", style = MaterialTheme.typography.bodyMedium)
                    Text("$totalPatients", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Billed Gross Revenue:", style = MaterialTheme.typography.bodyMedium)
                    Text(String.format("R %.2f", totalRevenue), fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Note: Net bank balance includes fixed overheads per patient ($200/case) and dynamically ordered investigation expenses.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }

        // 💳 Revenue & Practice Outflow Ledger
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📝 PRACTICE TRANSACTION LEDGER (ITEMIZED)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                if (completedCases.isEmpty()) {
                    Text(
                        text = "No closed encounters in the database yet. Transactions will populate as you discharge and bill patients.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        completedCases.reversed().forEach { encounter ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = encounter.patientDemographics.substringBefore('\n'),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Case #${encounter.id}",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Text(
                                        text = if (disableInsurance) encounter.specialty else "${encounter.specialty} • ${encounter.insuranceStatus}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    // Itemized financial logs
                                    val consultInflow = encounter.revenueEarned
                                    val labOutflow = encounter.expensesIncurred
                                    val fixedOverhead = 200.0
                                    val netProfit = consultInflow - labOutflow - fixedOverhead
                                    
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("🟢 GP Consultation Inflow (Tariff 0101):", fontSize = 10.sp, color = Color(0xFF1B5E20))
                                            Text("+R ${String.format("%.2f", consultInflow)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                                        }
                                        if (labOutflow > 0) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("🔴 Diagnostics Pathology Outflow:", fontSize = 10.sp, color = Color(0xFFC62828))
                                                Text("-R ${String.format("%.2f", labOutflow)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                            }
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("🔴 Fixed Practice Overhead Outflow:", fontSize = 10.sp, color = Color(0xFFC62828))
                                            Text("-R ${String.format("%.2f", fixedOverhead)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                        }
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), thickness = 0.5.dp)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("🏦 Net Encounter Contribution:", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = "${if (netProfit >= 0) "+" else ""}R ${String.format("%.2f", netProfit)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (netProfit >= 0) Color(0xFF1B5E20) else Color(0xFFC62828)
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
    }
}

@Composable
fun RxDocsTabContent(viewModel: SimulationViewModel, state: SimulationState) {
    val scrollState = rememberScrollState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    var selectedDocType by remember { mutableStateOf(0) } // 0: Prescription, 1: Referral, 2: Sick Note
    
    val docsAvailable = listOf(
        "Prescription" to state.prescriptionString,
        "Referral Letter" to state.referralLetterString,
        "Sick Note" to state.sickNoteString
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        // Document Toggles Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("💊 Prescription", "🏥 Referral", "🤒 Sick Note").forEachIndexed { index, title ->
                val selected = selectedDocType == index
                AssistChip(
                    onClick = { selectedDocType = index },
                    label = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
        
        val activeDocText = docsAvailable[selectedDocType].second
        val activeDocName = docsAvailable[selectedDocType].first
        
        if (activeDocText.isNullOrBlank()) {
            EmptyFolderView(
                icon = Icons.Default.Description,
                title = "No $activeDocName Compiled",
                description = "This form has not been compiled yet. Submit diagnosis and treatments to generate authorized clinical paperwork during Phase 4."
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF9F6)), // rich cream paper vibe
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "📜 SOVEREIGN PRACTICE CERTIFICATION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = activeDocName.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        androidx.compose.material3.HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = activeDocText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = Color.Black
                        )
                    }
                }
                
                if (selectedDocType == 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    GenericDrugAlternativeAdvisor(
                        medsNameCurrent = "",
                        modifier = Modifier.fillMaxWidth(),
                        currencySymbol = currencySymbol
                    )
                }
            }
        }
    }
}
