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
    
    val tension by viewModel.lawsuitTension.collectAsStateWithLifecycle()
    val progressiveSeats by viewModel.progressiveSeats.collectAsStateWithLifecycle()
    val conservativeSeats by viewModel.conservativeSeats.collectAsStateWithLifecycle()
    val newsReport by viewModel.currentNewsReport.collectAsStateWithLifecycle()
    
    var showSueDialog by remember { mutableStateOf(false) }
    var sueEntityName by remember { mutableStateOf("") }
    
    var showSandboxDialog by remember { mutableStateOf(false) }
    var sandboxAmount by remember { mutableStateOf("100000") }
    var sandboxTarget by remember { mutableStateOf("Clinic") }

    // Fake market indices for visual flair
    var biotechIndex by remember { mutableStateOf(10542.45f) }
    var healthSector by remember { mutableStateOf(4201.12f) }
    LaunchedEffect(Unit) {
        while(true) {
            kotlinx.coroutines.delay(2500)
            biotechIndex += ((-50..50).random().toFloat() / 10f)
            healthSector += ((-20..20).random().toFloat() / 10f)
        }
    }

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
                    text = "Manage ledgers, sue entities, configure AI strategies.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        
        // MARKET TICKER
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)).padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("BIOTECH ETF", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.6f))
                Text(String.format("%.2f", biotechIndex), fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (biotechIndex > 10542f) Color(0xFF2E7D32) else Color.Red)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("HEALTH SECTOR", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.6f))
                Text(String.format("%.2f", healthSector), fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (healthSector > 4201f) Color(0xFF2E7D32) else Color.Red)
            }
        }
        
        if (!newsReport.isNullOrBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("GLOBAL HEADLINE", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text(newsReport!!, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
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
        
        // MACRO INDICATORS
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CASCADING REGIONAL EFFECTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                Text("Judicial Tension Index ($tension%)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                LinearProgressIndicator(
                    progress = { tension / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = if (tension > 75) Color.Red else if (tension > 40) Color(0xFFFFA000) else Color(0xFF4CAF50),
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                val totalSeats = (progressiveSeats + conservativeSeats).coerceAtLeast(1)
                val progRatio = progressiveSeats.toFloat() / totalSeats.toFloat()
                Text("Political Balance (${progressiveSeats} Prog / ${conservativeSeats} Cons)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))) {
                    Box(modifier = Modifier.fillMaxHeight().weight(progRatio.coerceAtLeast(0.01f)).background(Color.Blue))
                    Box(modifier = Modifier.fillMaxHeight().weight((1f - progRatio).coerceAtLeast(0.01f)).background(Color.Red))
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
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha=0.5f)),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("DANGER ZONE: ROGUE AGENTIC ACTIONS", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                            Text("Execute highly illegal and risky financial strategies. Warning: High chance of prison or catastrophic fines if caught without high political cover.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
                            
                            Button(
                                onClick = { viewModel.executeAdvancedAIAccountingAction() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Execute Rogue Strategy (x55 Variants)", fontWeight = FontWeight.Bold)
                            }
                        }
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
        Text("Master Financial Ledger", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        
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
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(ledger) { entry ->
                        val isPositive = entry.contains("+") || entry.contains("SUCCESS") || entry.contains("WON")
                        val isNegative = entry.contains("-") || entry.contains("FAILURE") || entry.contains("LOST")
                        val color = if (isPositive) Color(0xFF4CAF50) else if (isNegative) Color(0xFFFF5252) else Color(0xFFB0BEC5)
                        
                        Text(
                            text = entry,
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = color
                        )
                        Divider(color = Color(0xFF333333), thickness = 1.dp, modifier = Modifier.padding(vertical = 2.dp))
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
