package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AccountingTab(viewModel: SimulationViewModel) {
    val balance by viewModel.clinicBalance.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val ledger by viewModel.accountingLedger.collectAsStateWithLifecycle()
    val isHired by viewModel.accountantHired.collectAsStateWithLifecycle()
    val nationalTreasury by viewModel.officeTreasury.collectAsStateWithLifecycle()
    
    var showSueDialog by remember { mutableStateOf(false) }
    var sueEntityName by remember { mutableStateOf("") }
    
    var showSandboxDialog by remember { mutableStateOf(false) }
    var sandboxAmount by remember { mutableStateOf("100000") }
    var sandboxTarget by remember { mutableStateOf("Clinic") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // HEADER
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Accounting & Treasury",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Manage ledgers, sue entities, configure AI accounting strategies, or use sandbox funds.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        
        // BALANCES OVERVIEW
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("CLINIC BALANCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.7f))
                    Text("$currencySymbol${String.format("%.2f", balance)}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("NATIONAL TREASURY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha=0.7f))
                    Text("$currencySymbol${String.format("%.2f", nationalTreasury)}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }
        
        // ACCOUNTANT AI ACTIONS
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha=0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("AI Accounting & Legal Directives", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                if (!isHired) {
                    Button(
                        onClick = { viewModel.hireAccountant() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Hire Senior AI Accountant (R3,500.00)")
                    }
                    Text("Hiring an accountant improves ledger tracking and increases lawsuit win rates by 30%.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Senior AI Accountant Hired & Active", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { viewModel.auditTaxPolicies() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("Run AI Ledger Policy Audit")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { viewModel.executeAdvancedAIAccountingAction() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Execute Rogue Agentic Strategy (x55 possible actions)")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = { showSueDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("File Lawsuit against City / State Entity")
                }
            }
        }
        
        // SANDBOX CHEATS
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha=0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sandbox Financial Tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text("Use these tools to arbitrarily inject money into different modules (Sandbox mode overrides).", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                
                Button(
                    onClick = { showSandboxDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                ) {
                    Text("Inject Sandbox Funds")
                }
            }
        }
        
        // LEDGER & INVOICES
        Text("Past Invoices & Ledger Adjustments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        
        if (ledger.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), RoundedCornerShape(8.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No ledger records available.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ledger.forEach { entry ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha=0.1f))
                    ) {
                        Text(
                            text = entry,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
    
    if (showSueDialog) {
        AlertDialog(
            onDismissRequest = { showSueDialog = false },
            title = { Text("File Municipal/State Lawsuit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Warning: Filing a lawsuit prompts an AI judge to review the case. You risk losing and having to pay legal fees. Suing requires an AI response.", fontSize = 12.sp)
                    OutlinedTextField(
                        value = sueEntityName,
                        onValueChange = { sueEntityName = it },
                        label = { Text("Entity Name (e.g. Dept of Health)") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (sueEntityName.isNotBlank()) {
                        viewModel.sueEntity(sueEntityName)
                        showSueDialog = false
                    }
                }) {
                    Text("Initiate Suit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSueDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (showSandboxDialog) {
        AlertDialog(
            onDismissRequest = { showSandboxDialog = false },
            title = { Text("Inject Sandbox Funds") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select where to inject funds and amount.", fontSize = 12.sp)
                    OutlinedTextField(
                        value = sandboxAmount,
                        onValueChange = { sandboxAmount = it },
                        label = { Text("Amount") }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = sandboxTarget == "Clinic",
                            onClick = { sandboxTarget = "Clinic" },
                            label = { Text("Clinic") }
                        )
                        FilterChip(
                            selected = sandboxTarget == "Country",
                            onClick = { sandboxTarget = "Country" },
                            label = { Text("Country Treasury") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amount = sandboxAmount.toDoubleOrNull() ?: 0.0
                    if (sandboxTarget == "Clinic") {
                        viewModel.sandboxAddMoney(amount)
                    } else {
                        viewModel.sandboxAddCountryMoney(amount)
                    }
                    showSandboxDialog = false
                }) {
                    Text("Inject")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSandboxDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
