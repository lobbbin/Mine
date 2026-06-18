package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PoliticsTab(
    viewModel: SimulationViewModel,
    modifier: Modifier = Modifier
) {
    val handler = viewModel.politicsHandler

    // Standard states
    val currentOffice by handler.currentOffice.collectAsStateWithLifecycle()
    val officeLevel by handler.officeLevel.collectAsStateWithLifecycle()
    val approvalRating by handler.approvalRating.collectAsStateWithLifecycle()
    val officeTermDays by handler.officeTermDays.collectAsStateWithLifecycle()
    val campaignFunds by handler.campaignFunds.collectAsStateWithLifecycle()
    val voterPolling by handler.voterPolling.collectAsStateWithLifecycle()
    
    val activeCampaignRace by handler.activeCampaignRace.collectAsStateWithLifecycle()
    val campaignTurnsLeft by handler.campaignTurnsLeft.collectAsStateWithLifecycle()
    val campaignHistory by handler.campaignHistory.collectAsStateWithLifecycle()
    
    val currentIssue by handler.currentIssue.collectAsStateWithLifecycle()
    val isAILoading by handler.isAILoading.collectAsStateWithLifecycle()
    val recentOutcome by handler.recentOutcome.collectAsStateWithLifecycle()
    val errorMessage by handler.errorMessage.collectAsStateWithLifecycle()

    // Faction Support states
    val workingClassSupport by handler.workingClassSupport.collectAsStateWithLifecycle()
    val medicalGuildSupport by handler.medicalGuildSupport.collectAsStateWithLifecycle()
    val corporateExecutiveSupport by handler.corporateExecutiveSupport.collectAsStateWithLifecycle()
    val nationalPatriotsSupport by handler.nationalPatriotsSupport.collectAsStateWithLifecycle()

    // Hired Cabinet states
    val hiredStaffIds by handler.hiredStaffIds.collectAsStateWithLifecycle()

    // Custom Bill states
    val recentBillResult by handler.recentBillResult.collectAsStateWithLifecycle()

    // Mayor States
    val sanitarySquadCount by handler.mayorOfficeHandler.sanitarySquadCount.collectAsStateWithLifecycle()
    val localSalesTax by handler.mayorOfficeHandler.localSalesTax.collectAsStateWithLifecycle()
    val hospitalSubsidyRate by handler.mayorOfficeHandler.hospitalSubsidyRate.collectAsStateWithLifecycle()

    // Governor States
    val medicaidCoverageTier by handler.governorOfficeHandler.medicaidCoverageTier.collectAsStateWithLifecycle()
    val isQuarantineActive by handler.governorOfficeHandler.isQuarantineActive.collectAsStateWithLifecycle()
    val chemistsDeregulated by handler.governorOfficeHandler.chemistsDeregulated.collectAsStateWithLifecycle()
    val stateIncomeTaxRate by handler.governorOfficeHandler.stateIncomeTaxRate.collectAsStateWithLifecycle()

    // Legislator States
    val cosponsorCount by handler.legislatorOfficeHandler.cosponsorCount.collectAsStateWithLifecycle()
    val patentExclusivityYears by handler.legislatorOfficeHandler.patentExclusivityYears.collectAsStateWithLifecycle()
    val lobbyistAlignment by handler.legislatorOfficeHandler.lobbyistAlignment.collectAsStateWithLifecycle()

    // President States
    val dpaIntensity by handler.presidentOfficeHandler.dpaIntensity.collectAsStateWithLifecycle()
    val globalTreatySigned by handler.presidentOfficeHandler.globalTreatySigned.collectAsStateWithLifecycle()
    val federalDeficitLevel by handler.presidentOfficeHandler.federalDeficitLevel.collectAsStateWithLifecycle()

    // ViewModel links
    val prestige by viewModel.politicalPrestige.collectAsStateWithLifecycle()
    val clinicBalance by viewModel.clinicBalance.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val agenicInterventions by viewModel.agenicActionHandler.agenicInterventions.collectAsStateWithLifecycle()
    val activePolicies by viewModel.activePolicies.collectAsStateWithLifecycle()

    // Dialog trigger variables
    var showTransferFundsDialog by remember { mutableStateOf(false) }
    var transferType by remember { mutableStateOf("ToCampaign") } 
    var transferAmountStr by remember { mutableStateOf("2500") }
    var activePoliticsSubTab by remember { mutableStateOf(0) } // 0 = Campaigns/Outreach, 1 = Executive Mansion Console

    // Bill Drafting parameters
    var draftBillTitle by remember { mutableStateOf("Universal Sovereign Medical Equipment Tax Relief Act") }
    var selectedSector by remember { mutableStateOf("Hospital Grants") }
    var selectedAllocation by remember { mutableStateOf("Hospitals / Clinicians") }
    var selectedTaxCost by remember { mutableStateOf("Corporate Surtax") }

    val scrollState = rememberScrollState()

    // --- Warning Dialog ---
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { handler.clearError() },
            title = { Text("Administration Briefing", fontWeight = FontWeight.Bold) },
            text = { Text(errorMessage ?: "") },
            confirmButton = {
                TextButton(onClick = { handler.clearError() }) {
                    Text("Understood")
                }
            }
        )
    }

    // --- Bill Voting Outcome Dialog ---
    recentBillResult?.let { bill ->
        AlertDialog(
            onDismissRequest = { handler.clearRecentBillResult() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (bill.passed) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        tint = if (bill.passed) Color(0xFF2E7D32) else Color(0xFFC62828),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (bill.passed) "BILL PASSED IN CONGRESS" else "BILL DEFEATED IN COMMITTEES",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = bill.billTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("House Floor", style = MaterialTheme.typography.labelSmall)
                                Text(bill.tallyHouse, fontWeight = FontWeight.Black)
                            }
                        }
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Senate Floor", style = MaterialTheme.typography.labelSmall)
                                Text(bill.tallySenate, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFBFBFB), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = bill.journalismExcerpt,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Serif,
                                fontStyle = FontStyle.Italic
                            ),
                            color = Color(0xFF222222)
                        )
                    }

                    if (bill.passed) {
                        Text("IMMEDIATE CLINIC CONVERSIONS:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        if (bill.dynamicClinicFundsGrant > 0.0) {
                            Text(
                                text = "💰 +$currencySymbol${String.format("%.2f", bill.dynamicClinicFundsGrant)} granted directly to hospital balance!",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (bill.dynamicStocksReward.isNotEmpty()) {
                            Text(
                                text = "📦 Medical Stock Delivery: ${bill.dynamicStocksReward.entries.joinToString { "${it.key} (+${it.value})" }}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Text("Backlash: Failure to secure committee alignment reduces prestige by -6 points and national support.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { handler.clearRecentBillResult() }) {
                    Text("Close Floor Briefing")
                }
            }
        )
    }

    // --- Fund Vault Connection Dialog ---
    if (showTransferFundsDialog) {
        AlertDialog(
            onDismissRequest = { showTransferFundsDialog = false },
            title = {
                Text(
                    text = if (transferType == "ToCampaign") "Sponsor Campaign via Clinic Surplus" else "Siphon Campaign Budgets to Clinic Box",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (transferType == "ToCampaign") {
                            "Transfer surplus capital out of your local medical chest ($currencySymbol${String.format("%.2f", clinicBalance)}) into your political campaign ledger to finance television debates, speeches, and lobby actions."
                        } else {
                            "WARNING: Siphoning public campaign fundings ($currencySymbol${String.format("%.2f", campaignFunds)}) into clinical operations triggers political audites, Media Scrutiny hits, and a Sharp Voter Trust Drop!"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = transferAmountStr,
                        onValueChange = { transferAmountStr = it },
                        label = { Text("Transfer Ledger Amount ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = transferAmountStr.toDoubleOrNull() ?: 2000.0
                        if (transferType == "ToCampaign") {
                            handler.donateToCampaign(amount)
                        } else {
                            handler.transferToClinic(amount)
                        }
                        showTransferFundsDialog = false
                    }
                ) {
                    Text("Execute Vault Transfer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTransferFundsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- TABS SELECTOR ROW AT THE TOP ---
        TabRow(
            selectedTabIndex = activePoliticsSubTab,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .testTag("politics_sub_tabs"),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            divider = {}
        ) {
            Tab(
                selected = activePoliticsSubTab == 0,
                onClick = { activePoliticsSubTab = 0 },
                text = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Campaign Centre", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
            Tab(
                selected = activePoliticsSubTab == 1,
                onClick = { activePoliticsSubTab = 1 },
                text = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Executive Mansion", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
            Tab(
                selected = activePoliticsSubTab == 2,
                onClick = { activePoliticsSubTab = 2 },
                text = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Parliament Gazette", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // --- 1. SOVEREIGN LEADER CAREER SUMMARY HEADER ---
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🏛️ SOVEREIGN DECENTRALIZED POLITY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentOffice,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        
                        val headerIcon = when {
                            officeLevel == "President" -> Icons.Default.Gavel
                            officeLevel == "Mayor" -> Icons.Default.LocationCity
                            officeLevel in listOf("Governor", "Senator") -> Icons.Default.AccountBalance
                            else -> Icons.Default.Group
                        }
                        Icon(
                            imageVector = headerIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                    // PRIMARY LEADERSHIP STATUS ROW
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1.5f)) {
                            Text("Voter Approval", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("$approvalRating%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = if (approvalRating >= 48) Color(0xFF2E7D32) else Color(0xFFC62828))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(2f)) {
                            Text("Campaign Budget", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("$currencySymbol${String.format("%.2f", campaignFunds)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1.5f)) {
                            Text("Prestige", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("$prestige/100", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1.5f)) {
                            Text("Gov Days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(if (officeTermDays > 0) "Day $officeTermDays" else "Rep", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        // ==================== TAB 0: CAMPAIGN CENTRE ====================
        if (activePoliticsSubTab == 0) {
            // --- 2. DEEP MULTI-FACTION MONITOR GRID ---
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "🗳️ COALITION FACTION STANDINGS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Lobby and coordinate policies to retain alignment. High faction support unlocks special campaign donations and supplies.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Working Class Support (Amber)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Working Class (Welfare & Health)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("$workingClassSupport%", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE65100))
                        }
                        LinearProgressIndicator(
                            progress = { workingClassSupport.toFloat() / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFFFF9800),
                            trackColor = Color(0xFFFFE0B2)
                        )
                    }

                    // Medical Guild Support (Teal)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Medical Guild (Doctors & Chemists)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("$medicalGuildSupport%", style = MaterialTheme.typography.bodySmall, color = Color(0xFF004D40))
                        }
                        LinearProgressIndicator(
                            progress = { medicalGuildSupport.toFloat() / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF009688),
                            trackColor = Color(0xFFB2DFDB)
                        )
                    }

                    // Corporate Executive Support (Gold)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Corporate Executives (Finance & Tech)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("$corporateExecutiveSupport%", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB57C00))
                        }
                        LinearProgressIndicator(
                            progress = { corporateExecutiveSupport.toFloat() / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFFFFC107),
                            trackColor = Color(0xFFFFECB3)
                        )
                    }

                    // National Patriots Support (Patriot blue)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("National Patriots (Traditions & Law)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("$nationalPatriotsSupport%", style = MaterialTheme.typography.bodySmall, color = Color(0xFF0D47A1))
                        }
                        LinearProgressIndicator(
                            progress = { nationalPatriotsSupport.toFloat() / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF2196F3),
                            trackColor = Color(0xFFBBDEFB)
                        )
                    }
                }
            }
        }

        // --- 3. SEAMLESS VAULT AND LEDGER COUPLING COMPONENT ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hospital-Political Currency Coupler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "Seamlessly transfer medical surplus to fund political speeches and media campaigns, or siphon funds back to scale clinical equipment.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            transferType = "ToCampaign"
                            transferAmountStr = "4500"
                            showTransferFundsDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Sponsor Politics (R4.5k)", fontSize = 11.sp, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            transferType = "ToClinic"
                            transferAmountStr = "12000"
                            showTransferFundsDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Siphon to Clinic (R12k)", fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }
        } // End of Tab 0 first half

        // AI LOAD INDICATOR
        if (isAILoading) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text("AI is simulating state dynamics and processing debates...", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // --- 4. EXECUTIVE ADVISORY CABINET RECRUITMENT SHELF ---
        if (activePoliticsSubTab == 0) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SupervisedUserCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "💼 RECRUIT CABINET ADVISORS",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Recruit strategic experts to earn passive clinical boosts and campaign multipliers. Paychecks are deducted daily during government sessions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    var selectedCabinetTierTab by remember { mutableStateOf("Mayor") }

                    TabRow(
                        selectedTabIndex = when (selectedCabinetTierTab) {
                            "Mayor" -> 0
                            "Governor" -> 1
                            else -> 2
                        },
                        modifier = Modifier.fillMaxWidth().testTag("cabinet_tier_tabs")
                    ) {
                        Tab(
                            selected = selectedCabinetTierTab == "Mayor",
                            onClick = { selectedCabinetTierTab = "Mayor" },
                            text = { Text("Mayor (T1) 🏛️", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedCabinetTierTab == "Governor",
                            onClick = { selectedCabinetTierTab = "Governor" },
                            text = { Text("Gov (T2) 🇺🇸", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedCabinetTierTab == "President",
                            onClick = { selectedCabinetTierTab = "President" },
                            text = { Text("Pres (T3) 🦅", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val filteredStaff = handler.availableStaffList.filter { it.tier == selectedCabinetTierTab }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        filteredStaff.forEach { staff ->
                            val isHired = hiredStaffIds.contains(staff.id)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isHired) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = CardDefaults.outlinedCardBorder(),
                                modifier = Modifier.fillMaxWidth().testTag("staff_card_${staff.id}")
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(staff.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text(staff.role, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                        if (isHired) {
                                            SuggestionChip(
                                                onClick = { handler.dismissStaff(staff.id) },
                                                label = { Text("Dismiss staff") },
                                                icon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                            )
                                        } else {
                                            Button(
                                                onClick = { handler.hireStaff(staff.id) },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                            ) {
                                                Text("Recruit ($${staff.setupCost.toInt()})", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                    Text(staff.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text("Salary: R${staff.dailySalary.toInt()}/day", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                        Text("Bonus: ${staff.bonusSummary}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 5. RECENT DECISION NEWS CLIP OUTCOME ---
        recentOutcome?.let { outcome ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📰 FEDERAL PRESS BRIEFING",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = outcome.issueTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF9F9F9), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = outcome.newsArticle,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Serif,
                                fontStyle = FontStyle.Normal
                            ),
                            color = Color(0xFF1E1E1E)
                        )
                    }

                    if (outcome.factionDeltaNarrative.isNotBlank()) {
                        Text(
                            text = "Faction Shift: ${outcome.factionDeltaNarrative}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // CHANGES FOOTER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (outcome.approvalChange != 0) {
                            Text(
                                text = "Approval: ${if (outcome.approvalChange > 0) "+" else ""}${outcome.approvalChange}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (outcome.approvalChange > 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                        if (outcome.fundsChange != 0.0) {
                            Text(
                                text = "Funds: ${if (outcome.fundsChange > 0) "+" else ""}${currencySymbol}${outcome.fundsChange}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (outcome.fundsChange > 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                        if (outcome.clinicBalanceChange != 0.0) {
                            Text(
                                text = "Medical Chest: ${if (outcome.clinicBalanceChange > 0) "+" else ""}${currencySymbol}${outcome.clinicBalanceChange}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (outcome.clinicBalanceChange > 0) Color(0xFF1976D2) else Color(0xFFD32F2F)
                            )
                        }
                        if (outcome.clinicStockChange.isNotEmpty()) {
                            Text(
                                text = "Stocks Arrived! 📦",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7B1FA2)
                            )
                        }
                    }
                }
            }
        }

        // --- 6. CAMPAIGN RACE RUNS MODULE ---
        if (activePoliticsSubTab == 0 && activeCampaignRace != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ACTIVE POLITICAL ELECTIONS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Campaign: $activeCampaignRace",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "$campaignTurnsLeft WEEKS LEFT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Current Polling: $voterPolling%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("Goal: 50% for standard victory safety", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        LinearProgressIndicator(
                            progress = { voterPolling.toFloat() / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                        )
                    }

                    var campaignPitch by remember { mutableStateOf("Prioritize direct clinical subsidies, state medical grants and decentralized medical oversight.") }
                    OutlinedTextField(
                        value = campaignPitch,
                        onValueChange = { campaignPitch = it },
                        label = { Text("Define Weekly Platform / Speech Topic") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    Text("Select Weekly Campaign Action", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                    val campaignActivities = listOf(
                        Triple("Town Hall Speech", 800.0, "🎤"),
                        Triple("Social Media Ads", 1500.0, "📱"),
                        Triple("Television Debate", 3000.0, "📺"),
                        Triple("Corporate Lobby Gala", 4000.0, "🥂"),
                        Triple("Labor Union Rally", 2500.0, "✊"),
                        Triple("Medical Science Symposium", 3500.0, "🔬"),
                        Triple("Patriot Law & Order Panel", 2000.0, "⚖️"),
                        Triple("AI Healthcare Disruption Pitch", 5000.0, "🤖"),
                        Triple("Grassroots Door-to-Door", 1200.0, "🚪")
                    )

                    val hasPressDiscount = hiredStaffIds.contains("mayor_media") || hiredStaffIds.contains("press_sec")
                    campaignActivities.chunked(3).forEach { rowActivities ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowActivities.forEach { (actionName, baseCost, icon) ->
                                val actualCost = if (hasPressDiscount) baseCost * 0.85 else baseCost
                                Button(
                                    onClick = { 
                                        val fullActionName = if (actionName == "Grassroots Door-to-Door") "Grassroots Door-to-Door Canvassing" else actionName
                                        handler.runCampaignAction(fullActionName, campaignPitch) 
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = !isAILoading,
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("$icon $actionName", fontSize = 9.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                        Text("-R${actualCost.toInt()}", fontSize = 8.sp, fontWeight = FontWeight.Light)
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { handler.retireCandidacy() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Retire Candidacy")
                    }

                    if (campaignHistory.isNotEmpty()) {
                        Text("Campaign Milestones", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                                .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                campaignHistory.forEach { log ->
                                    Text(log, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 7. DAILY GOVERNING ADMINISTRATIVE DECISION MODE ---
        if (activePoliticsSubTab == 1 && officeTermDays > 0 && activeCampaignRace == null) {
            currentIssue?.let { issue ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📋 GOVERNMENT CABINET CRISIS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                Text(issue.category.uppercase(), fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }

                        Text(issue.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text(issue.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("ENACT AN EXECUTIVE PATH:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)

                        // OPTION A
                        Button(
                            onClick = { handler.submitDecreeDecision(issue.optionA.text) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isAILoading,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("A. ${issue.optionA.text}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(issue.optionA.outcomeSummary, color = MaterialTheme.colorScheme.outline, fontSize = 11.sp, modifier = Modifier.fillMaxWidth())
                            }
                        }

                        // OPTION B
                        Button(
                            onClick = { handler.submitDecreeDecision(issue.optionB.text) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isAILoading,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("B. ${issue.optionB.text}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(issue.optionB.outcomeSummary, color = MaterialTheme.colorScheme.outline, fontSize = 11.sp, modifier = Modifier.fillMaxWidth())
                            }
                        }

                        // OPTION C
                        Button(
                            onClick = { handler.submitDecreeDecision(issue.optionC.text) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isAILoading,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("C. ${issue.optionC.text}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(issue.optionC.outcomeSummary, color = MaterialTheme.colorScheme.outline, fontSize = 11.sp, modifier = Modifier.fillMaxWidth())
                            }
                        }

                        var customActionInput by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = customActionInput,
                            onValueChange = { customActionInput = it },
                            label = { Text("Or Type Handwritten Custom Decree...") },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        handler.submitDecreeDecision(customActionInput)
                                        customActionInput = ""
                                    },
                                    enabled = customActionInput.isNotBlank() && !isAILoading
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "Enact Custom Decree")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } ?: run {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Default.HourglassEmpty, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Daily Cabinet Session Concluded",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Brief drafts dispatched. Advisors coordinate implementation frameworks securely before opening next session.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Button(
                            onClick = { handler.advanceOfficeDay() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Next Administrative Session")
                        }
                    }
                }
            }
        }

        // --- 8. OFFICE DECREES & EXECUTIVE SOVEREIGN POWERS ---
        if (activePoliticsSubTab == 1 && officeLevel != "None" && activeCampaignRace == null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "⚡ OFFICE-SPECIFIC DECREES (${officeLevel.uppercase()})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Execute direct command orders attached to your high office. Modifies hospital resources directly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    when {
                        officeLevel == "Mayor" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Status Indicators
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("🏢 City Hall Municipal Counters", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Sales Tax Rate: $localSalesTax%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("Sanitary Squads: $sanitarySquadCount Active", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text("Hospital Subsidy Modifier: +${(hospitalSubsidyRate * 100).toInt()}% Reinvestment", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                Button(
                                    onClick = { handler.executeSovereignDecree("mayor_levy") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isAILoading
                                ) {
                                    Text("Issue Municipal Clinic Tax Levy (+R5k Balance / -6% Approval)")
                                }
                                Button(
                                    onClick = { handler.executeSovereignDecree("mayor_refill") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isAILoading
                                ) {
                                    Text("Refill Supplies via City Budget (-R2.5k Funds / Restocks Stocks)")
                                }
                                Button(
                                    onClick = { handler.executeSovereignDecree("mayor_sanitary_patrols") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    enabled = !isAILoading
                                ) {
                                    Text("Deploy Sanitary Squad Patrols (-R1.5k Funds / +12% Patriots)")
                                }
                                Button(
                                    onClick = { handler.executeSovereignDecree("mayor_community_health_center") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                    enabled = !isAILoading
                                ) {
                                    Text("Build Core Wellness Wing (-R5k Funds / +18% WorkClass)")
                                }
                            }
                        }
                        officeLevel == "Governor" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Status Indicators
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("🏛️ State House Regulatory Ledger", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Medicaid Tier: Level $medicaidCoverageTier", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("State Quarantine: ${if (isQuarantineActive) "ENFORCED 🛑" else "None"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Chemist Licensing: ${if (chemistsDeregulated) "Deregulated 🔓" else "Standard"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("State Tax base: $stateIncomeTaxRate%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                Button(
                                    onClick = { handler.executeSovereignDecree("gov_medicaid") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isAILoading
                                ) {
                                    Text("Trigger Medicaid State Grant (-R4k Funds / +R20k Clinic Balance)")
                                }
                                Button(
                                    onClick = { handler.executeSovereignDecree("gov_quarantine") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isAILoading
                                ) {
                                    Text("Enact Quarantine Directives (+15% Med Trust / -6% WorkClass)")
                                }
                                Button(
                                    onClick = { handler.executeSovereignDecree("gov_deregulate_chemists") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    enabled = !isAILoading
                                ) {
                                    Text("Deregulate Chemist Licensing (-R3k Campaign / Cheap meds)")
                                }
                                Button(
                                    onClick = { handler.executeSovereignDecree("gov_unions_benefit") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                    enabled = !isAILoading
                                ) {
                                    Text("Sponsor Union Subsidies (-R5k Campaign / +R18k Clinic)")
                                }
                            }
                        }
                        officeLevel in listOf("State Representative", "State Senator", "US Representative", "Senator") -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Status Indicators
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("🗳️ Legislative Chamber Co-Signers", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Lobbyist Affinity: $lobbyistAlignment%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("Co-Sponsors: $cosponsorCount Reps", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text("Patent Exclusivity: $patentExclusivityYears Years", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                Button(
                                    onClick = { handler.executeSovereignDecree("sen_filibuster") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isAILoading
                                ) {
                                    Text("Launch Televised Filibuster (-R2.0k Campaign / +16 Prestige)")
                                }
                                Button(
                                    onClick = { handler.executeSovereignDecree("leg_earmark_rider") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    enabled = !isAILoading
                                ) {
                                    Text("Attach Clinic Earmark Rider (-R1k Funds / +R15k Clinic Balance)")
                                }
                                Button(
                                    onClick = { handler.executeSovereignDecree("leg_patent_reduction") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                    enabled = !isAILoading
                                ) {
                                    Text("Pass Generic Drug Accords (+10 Approval / Free Stock Drugs)")
                                }
                            }
                        }
                        officeLevel == "President" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Status Indicators
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("🦅 Federal Sovereign Metrics", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("DPA Retooling Intensity: Level $dpaIntensity", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("Bilateral Health Accord: ${if (globalTreatySigned) "ACTIVE 🌐" else "Inactive"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text("Federal Treasury Reserves: $federalDeficitLevel%", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                Button(
                                    onClick = { handler.executeSovereignDecree("pres_dpa") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isAILoading
                                ) {
                                    Text("Defense Production Act (-R6k Campaign / Massive stocks restocking)")
                                }
                                Button(
                                    onClick = { handler.executeSovereignDecree("pres_executive_grant") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isAILoading
                                ) {
                                    Text("Sovereign Executive Health Grant (+R45k Clinic / -10% Corp)")
                                }
                                Button(
                                    onClick = { handler.executeSovereignDecree("pres_nationalize_biotech") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    enabled = !isAILoading
                                ) {
                                    Text("Nationalize Strategic Vaccine Labs (-R10k Campaign / +R20k Clinic)")
                                }
                                Button(
                                    onClick = { handler.executeSovereignDecree("pres_global_health_accord") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                    enabled = !isAILoading
                                ) {
                                    Text("Authorize Bilateral Health Treaty (+R5k / +14% Approval)")
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 9. INTERACTIVE LEGISLATIVE DRAFT BILL DESK ---
        if (activePoliticsSubTab == 1 && officeTermDays > 0 && activeCampaignRace == null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "📜 LEGISLATIVE BILL SPONSOR DESK",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Craft legal layouts and lobby floor votes. Succeeding awards massive medical equipment subsidies and grants to the local clinic.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    OutlinedTextField(
                        value = draftBillTitle,
                        onValueChange = { draftBillTitle = it },
                        label = { Text("Legislation Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Sector focus
                    Text("Select Funding Focus Area", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val sectors = listOf("Hospital Grants", "Bio-Research", "Co-Pay Abolishment", "Pharma Regulation")
                        sectors.forEach { area ->
                            FilterChip(
                                selected = selectedSector == area,
                                onClick = { selectedSector = area },
                                label = { Text(area, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Target allocation
                    Text("Main Beneficiary Allocation", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val allocations = listOf("Working Class / Citizens", "Hospitals / Clinicians", "Pharma Corporations")
                        allocations.forEach { target ->
                            FilterChip(
                                selected = selectedAllocation == target,
                                onClick = { selectedAllocation = target },
                                label = { Text(target, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Funding Cost
                    Text("Regulatory Taxation Surcharges model", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val models = listOf("Federal Deficit Borrowing", "Corporate Surtax", "Citizen VAT Levy")
                        models.forEach { cost ->
                            FilterChip(
                                selected = selectedTaxCost == cost,
                                onClick = { selectedTaxCost = cost },
                                label = { Text(cost, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { handler.draftAndSponsorBill(draftBillTitle, selectedSector, selectedAllocation, selectedTaxCost) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isAILoading
                    ) {
                        Text("Sponsor & Run Floor Voting Action (-R2,500 legal fees)")
                    }
                }
            }
        }

        // --- 10. ELECTIONS CAREER OFFICE GRID ---
        if (activePoliticsSubTab == 0 && activeCampaignRace == null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "🚀 LEGISLATIVE & EXECUTIVE ELECTIONS OFFICE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Campaign for high municipal or federal executive seats to coordinate larger state budgets, restock inventory and authorize decrees.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    val races = listOf(
                        Triple("Mayor", "City Executive - Entry Fee: R4,000", 4000.0),
                        Triple("State Representative", "State Lower House - Entry Fee: R5,500", 5500.0),
                        Triple("Governor", "State Executive - Entry Fee: R10,000", 10000.0),
                        Triple("US Representative", "National Legislative - Entry Fee: R15,000", 15000.0),
                        Triple("President", "Sovereign Commander - Entry Fee: R35,000", 35000.0)
                    )

                    races.forEach { (race, summary, entryFee) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (campaignFunds >= entryFee) {
                                        handler.initiateCampaign(race)
                                    } else {
                                        handler.donateToCampaign(entryFee)
                                        if (campaignFunds >= entryFee) {
                                            handler.initiateCampaign(race)
                                        } else {
                                            handler.clearError()
                                        }
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(race, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text(summary, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                                Icon(Icons.Default.ArrowForward, contentDescription = "Run for $race", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        // --- 11. CIVIC RECRUITS OUTCOME PANEL ---
        if (activePoliticsSubTab == 1 && officeTermDays <= 0 && activeCampaignRace == null) {
            // --- GORGEOUS CINEMATIC LOCKED MANSION DASHBOARD ---
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "EXECUTIVE MANSION CONSOLE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "You are currently serving as an independent Civil Health Representative. Settle local disputes below, or coordinate a dynamic campaign in the Campaign Centre (Tab 1) to claim true sovereign supreme offices of the state!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))

                    Text(
                        text = "SOVEREIGN GOVERNMENT CAREER LADDER",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )

                    // Mayor Info Row
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.LocationCity, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("1. City Mayor (Municipal Care Desk)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Text("Levy local city sales taxes, commission Outbreak Sanitary Squads, and build local health counseling extensions.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }

                    // Governor Info Row
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("2. State Governor (State Executive Desk)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Text("Enforce state-wide highway quarantine lines, grant Medicaid funds, and deregulate regional chemist licenses.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }

                    // Legislator Info Row
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Gavel, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("3. State Representative / Senator (Chamber Floor)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Text("Sponsor customized legislative tax bills, co-sponsor caucuses, and coordinate televised blockages.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }

                    // President Info Row
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("4. President of the Republic (Sovereign command desk)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Text("Implement National Defense Production Act, trigger international tech trade compacts, and issue disaster relief grants.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }

            currentIssue?.let { issue ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("✊ SOVEREIGN CITIZEN COUNCIL BRACKET", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        Text(issue.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(issue.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Button(
                            onClick = { handler.submitDecreeDecision(issue.optionA.text) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(4.dp)) {
                                Text("A. ${issue.optionA.text}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                Text(issue.optionA.outcomeSummary, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        Button(
                            onClick = { handler.submitDecreeDecision(issue.optionB.text) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(4.dp)) {
                                Text("B. ${issue.optionB.text}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                Text(issue.optionB.outcomeSummary, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }

        // --- 11. PARLIAMENTARY GAZETTE AND ACTIVE LAWS ARCHIVE ---
        if (activePoliticsSubTab == 2) {
            // Section A: Active Enacted laws list
            Card(
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "📜 APPROVED CONSTITUTION & CLINICAL LAWS GAZETTE",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "All actively signed health mandates, diagnostic guidelines, and clinical regulations registered in Elysium.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    if (activePolicies.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No sovereign laws have been enacted yet. Win elections, draft custom bills, and passed guidelines will be registered here permanently.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            activePolicies.forEachIndexed { index, policy ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${index + 1}. ${policy.title}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFF2E7D32), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("ENACTED", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                        Text(text = "Summary: ${policy.summary}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = "Clinical Rule: ${policy.clinicalRule}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(text = "Economic Impact: ${policy.economicImpact}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        
                                        if (policy.extendedClauses.isNotEmpty()) {
                                            Text("Mandated Clauses:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                            policy.extendedClauses.forEach { clause ->
                                                Text("• $clause", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section B: Interactive Legislative Sponsor desk, permanently unlocked!
            Card(
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Gavel, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text(
                            text = "📜 LEGISLATIVE BILL SPONSOR DESK",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Craft, sponsor, and draft health codes directly on the Parliamentary floor. Drafts are run through floor debate, committee reports, and public consensus voting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    OutlinedTextField(
                        value = draftBillTitle,
                        onValueChange = { draftBillTitle = it },
                        label = { Text("Legislation Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Sector focus
                    Text("Select Funding Focus Area", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val sectors = listOf("Hospital Grants", "Bio-Research", "Co-Pay Abolishment", "Pharma Regulation")
                        sectors.forEach { area ->
                            FilterChip(
                                selected = selectedSector == area,
                                onClick = { selectedSector = area },
                                label = { Text(area, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Target allocation
                    Text("Main Beneficiary Allocation", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val allocations = listOf("Working Class / Citizens", "Hospitals / Clinicians", "Pharma Corporations")
                        allocations.forEach { target ->
                            FilterChip(
                                selected = selectedAllocation == target,
                                onClick = { selectedAllocation = target },
                                label = { Text(target, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Funding Cost
                    Text("Regulatory Taxation Surcharges model", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val models = listOf("Federal Deficit Borrowing", "Corporate Surtax", "Citizen VAT Levy")
                        models.forEach { cost ->
                            FilterChip(
                                selected = selectedTaxCost == cost,
                                onClick = { selectedTaxCost = cost },
                                label = { Text(cost, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { handler.draftAndSponsorBill(draftBillTitle, selectedSector, selectedAllocation, selectedTaxCost) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isAILoading
                    ) {
                        Text("Sponsor & Run Floor Voting Action (-R2,500 legal fees)")
                    }
                }
            }
        }

        // --- SOVEREIGN AGENIC ACTIONS LOG PANEL ---
        if (agenicInterventions.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f)
                ),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth().testTag("sovereign_agenic_logs_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "SOVEREIGN AGENIC COMMANDS TRACE",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }

                    Text(
                        text = "Trace log of executive intelligence interventions called programmatically by the active AI Game Agent in the background.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.12f))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        agenicInterventions.forEach { action ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp).align(Alignment.Top)
                                )
                                Text(
                                    text = action,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
