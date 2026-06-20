package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AgentMemory
import com.example.data.WorldStateEntity
import com.example.data.EncounterEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FinancialLedgerDialog(
    encounters: List<EncounterEntity>,
    totalRevenue: Double,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💰 Clinical Financial Ledger", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Ledger")
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text("Total Encounters: ${encounters.size}", style = MaterialTheme.typography.titleMedium)
                Text("Total Revenue: $${String.format(Locale.getDefault(), "%.2f", totalRevenue)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(encounters) { encounter ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(encounter.timestamp))
                                Text("Date: $dateStr", style = MaterialTheme.typography.labelSmall)
                                Text("Patient: ${encounter.patientDemographics}", fontWeight = FontWeight.Bold)
                                Text("Outcome: ${encounter.patientOutcome ?: "N/A"}")
                                Text("Receipt: ${encounter.billingReceipt ?: "None employed"}", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiMemoryContextDialog(
    memories: List<AgentMemory>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🧠 AI Context Memories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Memory")
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    "This engine tracks and persists AI memory heuristics.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (memories.isEmpty()) {
                        item {
                            Text("No memories persisted yet.", modifier = Modifier.padding(16.dp))
                        }
                    }
                    items(memories) { mem ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Tag: ${mem.memoryTag}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text("Lesson: ${mem.lessonLearned}", modifier = Modifier.padding(vertical = 4.dp))
                                Text("Encounter ID: ${mem.encounterId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
