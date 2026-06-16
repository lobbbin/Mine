package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WindPower
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.animation.animateContentSize
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.RequestQuote
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Article
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChatMessage
import com.example.data.Vitals
import com.example.data.SimulationState
import com.example.data.HiddenCaseProfile
import com.example.data.HealthPolicy
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontFamily

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: SimulationViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val hiddenCase by viewModel.hiddenCase.collectAsStateWithLifecycle()
    
    val currentDay by viewModel.currentDay.collectAsStateWithLifecycle()
    val patientsSeenToday by viewModel.patientsSeenToday.collectAsStateWithLifecycle()
    val dailyRevenue by viewModel.dailyRevenueLive.collectAsStateWithLifecycle()
    val dailyExpenses by viewModel.dailyExpensesLive.collectAsStateWithLifecycle()
    val clinicBalance by viewModel.clinicBalance.collectAsStateWithLifecycle()
    val syringeStock by viewModel.syringeStock.collectAsStateWithLifecycle()
    val salineStock by viewModel.salineStock.collectAsStateWithLifecycle()
    val adrenalineStock by viewModel.adrenalineStock.collectAsStateWithLifecycle()
    val reagentsStock by viewModel.reagentsStock.collectAsStateWithLifecycle()
    val medsStock by viewModel.medsStock.collectAsStateWithLifecycle()
    
    val doctorRank by viewModel.doctorRank.collectAsStateWithLifecycle()
    val doctorXp by viewModel.doctorXp.collectAsStateWithLifecycle()
    val reputationStars by viewModel.reputationStars.collectAsStateWithLifecycle()
    val model by viewModel.model.collectAsStateWithLifecycle()

    val lawsuitActive by viewModel.lawsuitActive.collectAsStateWithLifecycle()
    val lawsuitLog by viewModel.lawsuitLog.collectAsStateWithLifecycle()
    val lawsuitPatientName by viewModel.lawsuitPatientName.collectAsStateWithLifecycle()
    val lawsuitCaseDiag by viewModel.lawsuitCaseDiag.collectAsStateWithLifecycle()
    val lawsuitCharges by viewModel.lawsuitCharges.collectAsStateWithLifecycle()
    val lawsuitTension by viewModel.lawsuitTension.collectAsStateWithLifecycle()
    val lawsuitProsecutorAggression by viewModel.lawsuitProsecutorAggression.collectAsStateWithLifecycle()
    val lawsuitVerdict by viewModel.lawsuitVerdict.collectAsStateWithLifecycle()
    val lawsuitFine by viewModel.lawsuitFine.collectAsStateWithLifecycle()
    val lawsuitSuspension by viewModel.lawsuitSuspension.collectAsStateWithLifecycle()
    val lawsuitCurrentStage by viewModel.lawsuitCurrentStage.collectAsStateWithLifecycle()
    val wildAiUninsuredMode by viewModel.wildAiUninsuredMode.collectAsStateWithLifecycle()

    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedSheetTab by remember { mutableStateOf(0) }

    // Dialog form inputs
    var showDiagnosisDialog by remember { mutableStateOf(false) }
    var diagnosisInput by remember { mutableStateOf("") }
    var treatmentPlanInput by remember { mutableStateOf("") }

    var showPhysicalExamDialog by remember { mutableStateOf(false) }
    var physicalExamInput by remember { mutableStateOf("") }

    var showLabsDialog by remember { mutableStateOf(false) }
    var labsInput by remember { mutableStateOf("") }
    var financialConsentSigned by remember { mutableStateOf(false) }
    
    var showNotesDialog by remember { mutableStateOf(false) }
    var ddxNotesInput by remember { mutableStateOf(uiState.ddxNotes) }
    
    var showConsultDialog by remember { mutableStateOf(false) }
    var consultSpecialtyInput by remember { mutableStateOf("") }

    // Phase 4 paperwork local drafting inputs
    var medsNameInput by remember { mutableStateOf("") }
    var medsDoseInput by remember { mutableStateOf("") }
    var medsFreqInput by remember { mutableStateOf("") }
    var medsDurationInput by remember { mutableStateOf("") }

    var referralSpecialityInput by remember { mutableStateOf("") }
    var referralReasonInput by remember { mutableStateOf("") }

    // --- LEGISLATIVE AND GEOPOLITICAL FLOW COLLECTIONS ---
    val countryName by viewModel.countryName.collectAsStateWithLifecycle()
    val presidentName by viewModel.presidentName.collectAsStateWithLifecycle()
    val presidentParty by viewModel.presidentParty.collectAsStateWithLifecycle()
    val presidentApproval by viewModel.presidentApproval.collectAsStateWithLifecycle()
    val politicalPrestige by viewModel.politicalPrestige.collectAsStateWithLifecycle()
    val activePolicies by viewModel.activePolicies.collectAsStateWithLifecycle()
    
    val currentDraftPolicy by viewModel.currentDraftPolicy.collectAsStateWithLifecycle()
    val isVotingActive by viewModel.isVotingActive.collectAsStateWithLifecycle()
    val voteProgress by viewModel.voteProgress.collectAsStateWithLifecycle()
    val currentVoteYes by viewModel.currentVoteYes.collectAsStateWithLifecycle()
    val currentVoteNo by viewModel.currentVoteNo.collectAsStateWithLifecycle()
    val currentVoteAbstain by viewModel.currentVoteAbstain.collectAsStateWithLifecycle()
    val votingLog by viewModel.votingLog.collectAsStateWithLifecycle()
    val sickPoliticianAlert by viewModel.sickPoliticianAlert.collectAsStateWithLifecycle()

    var activeMainTab by remember { mutableStateOf(0) }


    var sickNoteReasonInput by remember { mutableStateOf("") }
    var sickNoteDaysInput by remember { mutableStateOf("0") }
    
    var showPaperworkDraftPanel by remember { mutableStateOf(false) }
    var activePaperworkTab by remember { mutableStateOf(0) }

    // Instant Chat field inputs
    var doctorMessageText by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Automatically trigger bottom folders when lab, bill, or evaluation is ready
    LaunchedEffect(uiState.labResults) {
        if (!uiState.labResults.isNullOrBlank()) {
            selectedSheetTab = 0
            showBottomSheet = true
        }
    }

    LaunchedEffect(uiState.physicalExamResults) {
        if (!uiState.physicalExamResults.isNullOrBlank()) {
            selectedSheetTab = 1
            showBottomSheet = true
        }
    }

    LaunchedEffect(uiState.billingReceipt) {
        if (!uiState.billingReceipt.isNullOrBlank()) {
            selectedSheetTab = 2
            showBottomSheet = true
        }
    }

    LaunchedEffect(uiState.evaluation) {
        if (!uiState.evaluation.isNullOrBlank()) {
            selectedSheetTab = 3
            showBottomSheet = true
        }
    }

    // Scroll chat stream to bottom when a new entry is detected
    LaunchedEffect(uiState.chatHistory.size) {
        if (uiState.chatHistory.isNotEmpty()) {
            lazyListState.animateScrollToItem(uiState.chatHistory.size - 1)
        }
    }

    // Capture background thread errors
    LaunchedEffect(Unit) {
        viewModel.errorEvents.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(uiState.submittedDiagnosis, uiState.submittedTreatmentPlan) {
        diagnosisInput = uiState.submittedDiagnosis
        treatmentPlanInput = uiState.submittedTreatmentPlan
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !uiState.isEncounterComplete,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                Text(
                    "🧰 Clinical Toolbox",
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                
                NavigationDrawerItem(
                    label = { Text("🧪 LAB INVESTIGATIONS", fontWeight = FontWeight.Bold) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        financialConsentSigned = false
                        showLabsDialog = true
                    },
                    icon = { Icon(Icons.Default.Science, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("🩺 PHYSICAL EXAM", fontWeight = FontWeight.Bold) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showPhysicalExamDialog = true
                    },
                    icon = { Icon(Icons.Default.Healing, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("📝 DDx NOTES", fontWeight = FontWeight.Bold) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showNotesDialog = true
                    },
                    icon = { Icon(Icons.Default.Assignment, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                
                Spacer(Modifier.weight(1f))
                HorizontalDivider()
                
                Text(
                    "EXTERNAL ACTIONS",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                NavigationDrawerItem(
                    label = { Text("📞 CONSULT SPECIALIST", fontWeight = FontWeight.Bold) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showConsultDialog = true
                    },
                    icon = { Icon(Icons.Default.Phone, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("🚑 REFER PATIENT", fontWeight = FontWeight.Bold) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.referPatient()
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.Send, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                NavigationDrawerItem(
                    label = { Text("📰 NATIONAL HEALTH NEWS", fontWeight = FontWeight.Bold) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.generateDailyNews()
                    },
                    icon = { Icon(Icons.Default.Article, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🏥 Practice Engine",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = doctorRank,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            // Added: Model display
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = model,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "⭐ ${String.format("%.1f", reputationStars)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFBC02D)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "🧬 XP: $doctorXp",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "📆 Day $currentDay ($patientsSeenToday/5)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "👥 Total: ${uiState.patientsSeen}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "💰 Bal: R ${String.format("%.0f", clinicBalance)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            selectedSheetTab = 0
                            showBottomSheet = true
                        },
                        modifier = Modifier.testTag("open_folders_button")
                    ) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = "Records")
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // --- CUSTOM GEOPOLITICAL TAB SELECTOR ROW ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = { activeMainTab = 0 },
                    shape = RoundedCornerShape(12.dp),
                    color = if (activeMainTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (activeMainTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f).height(44.dp),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocalHospital, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clinical Hub", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                
                Surface(
                    onClick = { activeMainTab = 1 },
                    shape = RoundedCornerShape(12.dp),
                    color = if (activeMainTab == 1) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
                    contentColor = if (activeMainTab == 1) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f).height(44.dp),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Flag, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Health Politics & Laws", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            if (activeMainTab == 0) {
                // --- Ambient Sick Politician Banner inside Clinical Hub ---
                if (sickPoliticianAlert != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable { activeMainTab = 1 },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "🚨 DISTRESS WARNING: The $presidentName or MP is critically ill! Tap to switch and ADMIT them immediately.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // --- Collapsible Bedside Clinical Hub Card (Vitals, Demographics, and Emergency Interventions under one clean tabbed subsection drawer) ---
                ClinicalHubCard(
                uiState = uiState,
                hiddenCase = hiddenCase,
                isLoading = isLoading,
                viewModel = viewModel
            )

            DailyPracticeClosureCard(
                currentDay = currentDay,
                patientsSeenToday = patientsSeenToday,
                dailyRevenue = dailyRevenue,
                dailyExpenses = dailyExpenses,
                clinicBalance = clinicBalance,
                syringeStock = syringeStock,
                salineStock = salineStock,
                adrenalineStock = adrenalineStock,
                reagentsStock = reagentsStock,
                medsStock = medsStock,
                viewModel = viewModel
            )

            // Current Phase Tracker
            Text(
                text = "📍 CURRENT PHASE: ${uiState.currentPhase}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    .padding(vertical = 6.dp, horizontal = 16.dp)
            )

            val isUninsuredCase = hiddenCase != null && (
                hiddenCase!!.insuranceStatus.contains("Uninsured", ignoreCase = true) ||
                hiddenCase!!.insuranceStatus.contains("State Funded", ignoreCase = true) ||
                hiddenCase!!.insuranceStatus.contains("Cash", ignoreCase = true) ||
                hiddenCase!!.insuranceStatus.contains("Out-of-Pocket", ignoreCase = true)
            )

            if (isUninsuredCase) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (wildAiUninsuredMode) Color(0xFF1B1210) else Color(0xFFFF8F00).copy(alpha = 0.08f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    border = BorderStroke(1.dp, if (wildAiUninsuredMode) Color(0xFFD84315) else Color(0xFFFFB300)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (wildAiUninsuredMode) "🔥" else "⚠️",
                                fontSize = 18.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Column {
                                Text(
                                    text = if (wildAiUninsuredMode) "WILD AI SIMULATOR ENGINE ACTIVE" else "NO INSURANCE / STATE PATHWAYS MANDATE",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    color = if (wildAiUninsuredMode) Color(0xFFFF7043) else Color(0xFFFF8F00)
                                )
                                Text(
                                    text = if (wildAiUninsuredMode) 
                                        "Standard state constraints bypassed! AI engine is granted full liberty to introduce alternative, rebel clinical events and bizarre reactions." 
                                        else "This patient has no private insurance. Flip this state override toggle to authorize wild experimental therapies or deep clinical roams!",
                                    style = MaterialTheme.typography.bodySmall,
                                    lineHeight = 13.sp,
                                    fontSize = 9.5.sp,
                                    color = if (wildAiUninsuredMode) Color(0xFFD7CCC8) else Color.DarkGray
                                )
                            }
                        }
                        Switch(
                            checked = wildAiUninsuredMode,
                            onCheckedChange = { viewModel.toggleWildAiUninsuredMode(it) }
                        )
                    }
                }
            }

            // --- Dialogue Chat Stream ---
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!uiState.dmEnvironmentalUpdate.isNullOrBlank()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A237E)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF3F51B5).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color.Cyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "MASTER NARRATIVE: ${uiState.dmEnvironmentalUpdate}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                items(uiState.chatHistory) { message ->
                    ChatMessageRow(message)
                }

                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Simulation Engine processing clinical data...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            // --- Immediate Chat Response Input Bar ---
            if (uiState.isEncounterComplete) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    VisualPatientOutcomeBanner(outcome = uiState.patientOutcome)
                    if (patientsSeenToday >= 5) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "🌅 Day Completed! You have seen 5 patients today. Please review the Daily Practice Report above, and click below to begin the next shift.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(14.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                            Button(
                                onClick = { viewModel.advanceDayPrac() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .testTag("advance_day_bottom_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(16.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("🌅 ADVANCE TO DAY ${currentDay + 1}", fontSize = 18.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    } else {
                        Button(
                            onClick = { viewModel.startNextPatient() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .height(64.dp)
                                .testTag("next_patient_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("✅ CASE CLOSED. START NEXT PX", fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            } else {
                val cmoAdvice by viewModel.currentCmoAdvice.collectAsState()
                val pres by viewModel.politicalPrestige.collectAsState()
                
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    if (cmoAdvice == null) {
                        OutlinedButton(
                            onClick = { viewModel.askCmoConsult() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading && pres >= 2,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("💡 Ask Chief Medical Officer for Advice (-2 Prestige, R50)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Healing, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("CMO'S ADVICE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                                }
                                Text(cmoAdvice!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(top = 4.dp))
                                TextButton(onClick = { viewModel.clearCmoAdvice() }, modifier = Modifier.align(Alignment.End)) {
                                    Text("Dismiss", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = doctorMessageText,
                        onValueChange = { doctorMessageText = it },
                        placeholder = { Text("Consult/Interview with the patient...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = false,
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (doctorMessageText.isNotBlank()) {
                                viewModel.sendMessage(doctorMessageText)
                                doctorMessageText = ""
                                focusManager.clearFocus()
                            }
                        },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(100.dp))
                            .size(56.dp)
                            .testTag("send_chat_button"),
                        enabled = doctorMessageText.isNotBlank() && !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send message",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // --- Conclusion Action Panel ---
            if (!uiState.isEncounterComplete) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when {
                        uiState.currentPhase.startsWith("Phase 4") -> {
                            // Render Phase 4 module!
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .heightIn(max = 350.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    val hasDraftedAny = !uiState.prescriptionString.isNullOrBlank() || 
                                                        !uiState.referralLetterString.isNullOrBlank() || 
                                                        !uiState.sickNoteString.isNullOrBlank()
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "📝 Phase 4 - Draft Clinical Paperwork",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (hasDraftedAny) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                                                    shape = RoundedCornerShape(100.dp)
                                                )
                                                .padding(horizontal = 10.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (hasDraftedAny) "COMPILED" else "PENDING DRAFT",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (hasDraftedAny) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "🩺 Working Diagnosis:",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = uiState.submittedDiagnosis.ifBlank { "Pending/None" },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(bottom = 6.dp)
                                            )
                                            Text(
                                                text = "📋 Treatment Plan Summary:",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = uiState.submittedTreatmentPlan.ifBlank { "Pending/None" },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    if (!showPaperworkDraftPanel) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Button(
                                                onClick = { showPaperworkDraftPanel = true },
                                                modifier = Modifier.weight(1.5f),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                            ) {
                                                Icon(Icons.Default.Assignment, null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("DRAFT DOCUMENTS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            
                                            if (hasDraftedAny) {
                                                OutlinedButton(
                                                    onClick = {
                                                        selectedSheetTab = 2 // Rx Docs tab in ResultsBottomSheet (index 2)
                                                        showBottomSheet = true
                                                    },
                                                    modifier = Modifier.weight(1.5f)
                                                ) {
                                                    Text("📄 VIEW", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                                }
                                                
                                                Button(
                                                    onClick = { viewModel.approveDoctorDocumentsAndGenerateBill() },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                    modifier = Modifier.weight(1.8f),
                                                    enabled = !isLoading
                                                ) {
                                                    Text("🧾 SIGN & BILL ➔", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                                }
                                            }
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            // Paperwork Selection Tabs
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 4.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                listOf("🩺 Dx Plan", "💊 Script", "🏥 Referral", "🤒 Sick Note").forEachIndexed { index, title ->
                                                    val selected = activePaperworkTab == index
                                                    AssistChip(
                                                        onClick = { activePaperworkTab = index },
                                                        label = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                                        colors = AssistChipDefaults.assistChipColors(
                                                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                                            labelColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                        ),
                                                        border = BorderStroke(
                                                            1.dp,
                                                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                                        )
                                                    )
                                                }
                                            }

                                            when (activePaperworkTab) {
                                                0 -> {
                                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Text("0. Working Diagnosis & Treatment Plan", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                        OutlinedTextField(
                                                            value = diagnosisInput,
                                                            onValueChange = { diagnosisInput = it },
                                                            label = { Text("Clinical Working Diagnosis") },
                                                            modifier = Modifier.fillMaxWidth().testTag("clinical_diagnosis_input_ph4"),
                                                            textStyle = MaterialTheme.typography.bodySmall,
                                                            singleLine = false,
                                                            maxLines = 3
                                                        )
                                                        OutlinedTextField(
                                                            value = treatmentPlanInput,
                                                            onValueChange = { treatmentPlanInput = it },
                                                            label = { Text("Management & Intervention Plan") },
                                                            modifier = Modifier.fillMaxWidth().testTag("clinical_plan_input_ph4"),
                                                            textStyle = MaterialTheme.typography.bodySmall,
                                                            singleLine = false,
                                                            maxLines = 4
                                                        )
                                                        
                                                        Button(
                                                            onClick = {
                                                                if (diagnosisInput.isNotBlank()) {
                                                                    viewModel.submitDiagnosisAndPlan(diagnosisInput, treatmentPlanInput)
                                                                }
                                                            },
                                                            modifier = Modifier.align(Alignment.End),
                                                            enabled = diagnosisInput.isNotBlank() && !isLoading,
                                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                                        ) {
                                                            Text("Update Diagnosis & Plan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                                1 -> {
                                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Text("1. Prescription Details (PDR)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            OutlinedTextField(
                                                                value = medsNameInput,
                                                                onValueChange = { medsNameInput = it },
                                                                label = { Text("Meds Name") },
                                                                modifier = Modifier.weight(1.2f).testTag("rx_meds_name"),
                                                                textStyle = MaterialTheme.typography.bodySmall,
                                                                singleLine = true
                                                            )
                                                            OutlinedTextField(
                                                                value = medsDoseInput,
                                                                onValueChange = { medsDoseInput = it },
                                                                label = { Text("Dose") },
                                                                modifier = Modifier.weight(0.8f).testTag("rx_meds_dose"),
                                                                textStyle = MaterialTheme.typography.bodySmall,
                                                                singleLine = true
                                                            )
                                                        }
                                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            OutlinedTextField(
                                                                value = medsFreqInput,
                                                                onValueChange = { medsFreqInput = it },
                                                                label = { Text("Frequency") },
                                                                modifier = Modifier.weight(1f).testTag("rx_meds_freq"),
                                                                textStyle = MaterialTheme.typography.bodySmall,
                                                                singleLine = true
                                                            )
                                                            OutlinedTextField(
                                                                value = medsDurationInput,
                                                                onValueChange = { medsDurationInput = it },
                                                                label = { Text("Days") },
                                                                modifier = Modifier.weight(0.6f).testTag("rx_meds_duration"),
                                                                textStyle = MaterialTheme.typography.bodySmall,
                                                                singleLine = true
                                                            )
                                                        }
                                                        
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        GenericDrugAlternativeAdvisor(
                                                            medsNameCurrent = medsNameInput,
                                                            onSelectGeneric = { brand, generic ->
                                                                medsNameInput = generic
                                                            }
                                                        )
                                                    }
                                                }
                                                2 -> {
                                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Text("2. Specialist Clinical Referral Advice", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            OutlinedTextField(
                                                                value = referralSpecialityInput,
                                                                onValueChange = { referralSpecialityInput = it },
                                                                label = { Text("Specialty") },
                                                                modifier = Modifier.weight(1f).testTag("ref_specialty"),
                                                                textStyle = MaterialTheme.typography.bodySmall,
                                                                singleLine = true
                                                            )
                                                            OutlinedTextField(
                                                                value = referralReasonInput,
                                                                onValueChange = { referralReasonInput = it },
                                                                label = { Text("Indication / Reason") },
                                                                modifier = Modifier.weight(1.4f).testTag("ref_reason"),
                                                                textStyle = MaterialTheme.typography.bodySmall,
                                                                singleLine = true
                                                            )
                                                        }
                                                    }
                                                }
                                                3 -> {
                                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Text("3. SA Ethical Rule 16 Sick note", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            OutlinedTextField(
                                                                value = sickNoteReasonInput,
                                                                onValueChange = { sickNoteReasonInput = it },
                                                                label = { Text("Diagnosis/Clinical Reason") },
                                                                modifier = Modifier.weight(1.5f).testTag("sick_reason"),
                                                                textStyle = MaterialTheme.typography.bodySmall,
                                                                singleLine = true
                                                            )
                                                            OutlinedTextField(
                                                                value = sickNoteDaysInput,
                                                                onValueChange = { sickNoteDaysInput = it },
                                                                label = { Text("Days") },
                                                                modifier = Modifier.weight(0.5f).testTag("sick_days"),
                                                                textStyle = MaterialTheme.typography.bodySmall,
                                                                singleLine = true
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            
                                            Spacer(modifier = Modifier.height(6.dp))
                                            
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                OutlinedButton(
                                                    onClick = { showPaperworkDraftPanel = false },
                                                    modifier = Modifier.weight(1.5f)
                                                ) {
                                                    Text("CANCEL")
                                                }
                                                Button(
                                                    onClick = {
                                                        val normalizedMeds = medsNameInput.trim()
                                                        val isMedsEmptyOrNA = normalizedMeds.isEmpty() || 
                                                                              normalizedMeds.equals("n/a", ignoreCase = true) || 
                                                                              normalizedMeds.equals("null", ignoreCase = true) ||
                                                                              normalizedMeds.equals("none", ignoreCase = true)
                                                        
                                                        val normalizedRef = referralSpecialityInput.trim()
                                                        val isRefEmptyOrNA = normalizedRef.isEmpty() || 
                                                                             normalizedRef.equals("n/a", ignoreCase = true) || 
                                                                             normalizedRef.equals("null", ignoreCase = true) || 
                                                                             normalizedRef.equals("none", ignoreCase = true)
                                                                             
                                                        val normalizedSick = sickNoteReasonInput.trim()
                                                        val isSickEmptyOrNA = normalizedSick.isEmpty() || 
                                                                              normalizedSick.equals("n/a", ignoreCase = true) || 
                                                                              normalizedSick.equals("null", ignoreCase = true) || 
                                                                              normalizedSick.equals("none", ignoreCase = true) ||
                                                                              (sickNoteDaysInput.toIntOrNull() ?: 0) <= 0

                                                        if (isMedsEmptyOrNA && isRefEmptyOrNA && isSickEmptyOrNA) {
                                                            viewModel.logAndEmitError("Error: Please enter at least a valid Prescription, Specialist Referral, or Sick Note to compile.")
                                                        } else {
                                                            viewModel.compilePrescriptionAndReferral(
                                                                medsName = if (isMedsEmptyOrNA) "" else medsNameInput,
                                                                medsDose = if (isMedsEmptyOrNA) "" else medsDoseInput,
                                                                medsFreq = if (isMedsEmptyOrNA) "" else medsFreqInput,
                                                                medsDuration = if (isMedsEmptyOrNA) "" else medsDurationInput,
                                                                referralSpecialty = if (isRefEmptyOrNA) "" else referralSpecialityInput,
                                                                referralReason = if (isRefEmptyOrNA) "" else referralReasonInput,
                                                                sickNoteReason = if (isSickEmptyOrNA) "" else sickNoteReasonInput,
                                                                sickNoteDays = sickNoteDaysInput.toIntOrNull() ?: 0
                                                            )
                                                            showPaperworkDraftPanel = false
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                    modifier = Modifier.weight(2f),
                                                    enabled = !isLoading
                                                ) {
                                                    Text("💾 COMPILE FILE", fontWeight = FontWeight.Black)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        uiState.currentPhase.contains("Billing") || uiState.currentPhase.contains("Phase 5") -> {
                            // Render Phase 5 Billing and Financial Co-payment Module!
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                                border = BorderStroke(1.dp, Color(0xFF81C784)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.RequestQuote, contentDescription = null, tint = Color(0xFF2E7D32))
                                        Text(
                                            "💸 Phase 5 - Billing & Co-Payment",
                                            fontWeight = FontWeight.ExtraBold,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color(0xFF1B5E20)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Invoicing generated under South African HPCSA private billing codes. Factors: consultation fee, active scripts, and clinical stock deductions.",
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        color = Color(0xFF33691E)
                                    )

                                    val currentCase = hiddenCase
                                    val isUninsuredCase = currentCase != null && (
                                        currentCase.insuranceStatus.contains("Uninsured", ignoreCase = true) ||
                                        currentCase.insuranceStatus.contains("State Funded", ignoreCase = true) ||
                                        currentCase.insuranceStatus.contains("Cash", ignoreCase = true) ||
                                        currentCase.insuranceStatus.contains("Out-of-Pocket", ignoreCase = true)
                                    )

                                    if (isUninsuredCase) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (wildAiUninsuredMode) Color(0xFF2E1C18) else Color(0xFFFF8F00).copy(alpha = 0.08f)
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                            border = BorderStroke(1.dp, if (wildAiUninsuredMode) Color(0xFFD84315) else Color(0xFFFFB300)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = if (wildAiUninsuredMode) "🔥 WILD CLINIC ACTIVE" else "⚠️ STATE FUNDED / UNINSURED BILL",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp,
                                                        color = if (wildAiUninsuredMode) Color(0xFFFF7043) else Color(0xFFFF8F00)
                                                    )
                                                    Text(
                                                        text = if (wildAiUninsuredMode) "Uninsured state restrictions bypassed! The AI engine has full liberty to introduce alternative rebel clinical events." else "Toggle on to grant the AI engine complete free roam for sovereign mutations or rebel treatments!",
                                                        fontSize = 9.sp,
                                                        lineHeight = 11.sp,
                                                        color = if (wildAiUninsuredMode) Color(0xFFD7CCC8) else Color.DarkGray
                                                    )
                                                }
                                                Switch(
                                                    checked = wildAiUninsuredMode,
                                                    onCheckedChange = { viewModel.toggleWildAiUninsuredMode(it) }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                selectedSheetTab = 3 // Billing tab in ResultsBottomSheet (index 3)
                                                showBottomSheet = true
                                            },
                                            modifier = Modifier.weight(1f),
                                            border = BorderStroke(1.dp, Color(0xFF81C784))
                                        ) {
                                            Text("📄 VIEW CLAIM", fontSize = 11.sp, color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold)
                                        }
                                        
                                        Button(
                                            onClick = {
                                                val copay = when (hiddenCase?.insuranceStatus) {
                                                    "Private Medical Aid" -> viewModel.consultationFee.value * 0.20
                                                    "State Funded" -> 0.0
                                                    else -> viewModel.consultationFee.value + 250.0
                                                }
                                                viewModel.collectPaymentAndFinish("Standard Cash/Card Drawer Swipe", copay)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                            modifier = Modifier.weight(1.5f),
                                            enabled = !isLoading
                                        ) {
                                            val copayText = when (hiddenCase?.insuranceStatus) {
                                                "Private Medical Aid" -> "R ${String.format("%.2f", viewModel.consultationFee.value * 0.20)} (20%)"
                                                "State Funded" -> "R 0.00 (Medical Aid)"
                                                else -> "R ${String.format("%.2f", viewModel.consultationFee.value + 250.0)}"
                                            }
                                            Text("💵 RECEIPT $copayText", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }
                        }
                        
                        else -> {
                            // Phase 1 or 2 standard bottom action dashboard
                            Button(
                                onClick = { showDiagnosisDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("submit_diagnosis_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                enabled = !isLoading,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.AssignmentTurnedIn, contentDescription = null, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("DX/RX PLAN", fontSize = 14.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
            } else {
                StateAndLegislationTab(
                    viewModel = viewModel,
                    onAdmittedClicked = { activeMainTab = 0 }
                )
            }
        }
    }
}

        // --- CONSULT SPECIALIST DIALOGUE BOX ---
        if (showConsultDialog) {
            AlertDialog(
                onDismissRequest = { showConsultDialog = false },
                title = { Text("Telephone Consultation", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Need advice? Call a specialist for a quick consult. This will add 20 minutes to your time and cost R800 from the clinic's expenses.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = consultSpecialtyInput,
                            onValueChange = { consultSpecialtyInput = it },
                            label = { Text("Specialty to Call") },
                            placeholder = { Text("e.g., Cardiology, Neurology") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.seekConsultation(consultSpecialtyInput)
                            consultSpecialtyInput = ""
                            showConsultDialog = false
                        },
                        enabled = consultSpecialtyInput.isNotBlank()
                    ) {
                        Text("Call (R800)")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showConsultDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // --- DDX NOTES DIALOGUE BOX ---
        if (showNotesDialog) {
            AlertDialog(
                onDismissRequest = { 
                    viewModel.updateDdxNotes(ddxNotesInput)
                    showNotesDialog = false 
                },
                title = { Text("Clinical Differential (DDx) Tracker", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Jot down your observations, differential diagnoses, or planned investigations. These notes are private and not visible to the patient.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = ddxNotesInput,
                            onValueChange = { ddxNotesInput = it },
                            label = { Text("Private Clinical Notes") },
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            maxLines = 10
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateDdxNotes(ddxNotesInput)
                            showNotesDialog = false
                        }
                    ) {
                        Text("Save Notes")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { 
                        ddxNotesInput = uiState.ddxNotes
                        showNotesDialog = false 
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // --- PHYSICAL EXAM DIALOGUE BOX ---
        if (showPhysicalExamDialog) {
            AlertDialog(
                onDismissRequest = { showPhysicalExamDialog = false },
                title = { Text("Request Physical Finding", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "What bodily systems or signs would you like to examine? (e.g. cardiovascular auscultation, respiratory sounds, throat check, abdominal palpation)",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = physicalExamInput,
                            onValueChange = { physicalExamInput = it },
                            placeholder = { Text("Describe clinical palpation/auscultation...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("physical_exam_field")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.performPhysicalExam(physicalExamInput)
                            physicalExamInput = ""
                            showPhysicalExamDialog = false
                        }
                    ) {
                        Text("Perform Exam")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showPhysicalExamDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // --- LABORATORY INVESTIGATIONS DIALOGUE BOX ---
        if (showLabsDialog) {
            val consultFeeVal = viewModel.consultationFee.collectAsState().value
            val labCostVal = viewModel.labCost.collectAsState().value
            val totalEst = consultFeeVal + labCostVal + 50.0
            
            val insuranceStatus = hiddenCase?.insuranceStatus ?: "Out-of-Pocket Cash"
            val matchedScheme = OrchidDeepStateManager.medicalAidSchemes.value.find { insuranceStatus.contains(it.name, ignoreCase = true) }
            
            val (copayText, schemeDetails) = if (matchedScheme != null) {
                val patientResponsibility = 1.0 - matchedScheme.coveragePercent
                val copayAmount = totalEst * patientResponsibility
                val preAuthHint = if (matchedScheme.requiresPreAuth) "PRE-AUTH REQUIRED" else "No Pre-Auth Needed"
                Pair("R ${String.format("%.2f", copayAmount)} (Co-Pay ${patientResponsibility * 100}%)", "${matchedScheme.name} limit applies. $preAuthHint")
            } else {
                when {
                    insuranceStatus.contains("State") || insuranceStatus.contains("NHS") -> {
                        Pair("R 0.00 (State Authorized DSP)", "No co-pays required on network contracted diagnostics.")
                    }
                    insuranceStatus.contains("Private Medical Aid") -> {
                        val copayAmount = totalEst * 0.20
                        Pair("R ${String.format("%.2f", copayAmount)} (Standard Aid)", "20% Pathology levy rules apply.")
                    }
                    else -> {
                        Pair("R ${String.format("%.2f", totalEst)} (Full Cash Paying Out-of-Pocket)", "100% patient private responsibility.")
                    }
                }
            }

            AlertDialog(
                onDismissRequest = { showLabsDialog = false },
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Science, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Order Laboratory Investigations", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Specify the blood tests or diagnostic panels to request (e.g., FBC, CRP, Urea, electrolytes, lipid panel, HbA1c):",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = labsInput,
                            onValueChange = { labsInput = it },
                            placeholder = { Text("E.g. FBC, CRP, kidney function, Troponin...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("labs_order_field"),
                            textStyle = MaterialTheme.typography.bodySmall,
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // 🇿🇦 Informed Financial Consent Cost Quote Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "🇿🇦 INFORMED FINANCIAL CONSENT QUOTE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("GP Consultation Code 0101:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("R ${String.format("%.2f", consultFeeVal)}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Pathology Lab Reagent Cost:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("R ${String.format("%.2f", labCostVal)}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Clinician Admin/Dispatch Fee:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("R 50.00", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Estimated Bill Total:", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                        Text("R ${String.format("%.2f", totalEst)}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Disclosed Patient Co-Payment:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(copayText, fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = schemeDetails, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Consent Signature Button Row (Interactive)
                        val consentOnPrimary = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        val consentOffSurface = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (financialConsentSigned) consentOnPrimary else consentOffSurface)
                                .clickable { financialConsentSigned = !financialConsentSigned }
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = if (financialConsentSigned) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (financialConsentSigned) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Secure Patient's Digital Signing",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (financialConsentSigned) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "HPCSA guideline requirement to secure signed private tariff consent.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.orderLabs(labsInput, wasFinancialConsentSigned = financialConsentSigned)
                            labsInput = ""
                            showLabsDialog = false
                        },
                        enabled = financialConsentSigned && labsInput.isNotBlank()
                    ) {
                        Text("Sign & Order Labs", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showLabsDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // --- SUBMIT DIAGNOSIS & MANAGEMENT PLAN FORM ---
        if (showDiagnosisDialog) {
            Dialog(onDismissRequest = { showDiagnosisDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 24.dp)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Clinical Diagnostics Formulation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Text(
                            text = "Provide your diagnostic findings & treatment plan based on clinical presentation. This updates Billing Receipts and submits for final evaluation scores.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        OutlinedTextField(
                            value = diagnosisInput,
                            onValueChange = { diagnosisInput = it },
                            placeholder = { Text("Suspected Diagnosis (including ICD-10 if known)") },
                            label = { Text("Diagnosis") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("diagnosis_field"),
                            maxLines = 2
                        )
                        
                        OutlinedTextField(
                            value = treatmentPlanInput,
                            onValueChange = { treatmentPlanInput = it },
                            placeholder = { Text("E.g. Medications, dosages, referrals, guidelines...") },
                            label = { Text("Treatment & Referrals Plan") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("treatment_field")
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                if (diagnosisInput.isNotBlank()) {
                                    viewModel.submitDiagnosisAndPlan(diagnosisInput, treatmentPlanInput)
                                    showDiagnosisDialog = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("confirm_billing_button"),
                            enabled = diagnosisInput.isNotBlank()
                        ) {
                            Text("📝 Proceed to Clinical Paperwork (Phase 4)", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        }

                        OutlinedButton(
                            onClick = { showDiagnosisDialog = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Cancel Submission", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- MODAL RESULTS CLINICAL RECORD SHEET ---
        
        val newsReport by viewModel.currentNewsReport.collectAsState()
        if (newsReport != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearDailyNews() },
                title = { Text("📰 THE SOVEREIGN HEALTH TIMES", fontWeight = FontWeight.Black) },
                text = {
                    Box(modifier = Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState())) {
                        Text(newsReport!!, style = MaterialTheme.typography.bodyMedium)
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.clearDailyNews() }) { Text("Close Details") }
                }
            )
        }

        if (showBottomSheet) {
            ResultsBottomSheet(
                viewModel = viewModel,
                onDismiss = { showBottomSheet = false },
                initialTab = selectedSheetTab
            )
        }

        // --- SOVEREIGN JUDICIARY COMPLIANCE COURT TRIAL DIALOG ---
        if (lawsuitActive) {
            Dialog(
                onDismissRequest = { viewModel.dismissLawsuit() },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Sovereign High Court Header
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "🏛️ SUPREME COURT OF ${countryName.uppercase()}", 
                                        style = MaterialTheme.typography.titleMedium, 
                                        fontWeight = FontWeight.Black, 
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "HIGH JUDICIARY DEPARTMENT • EXECUTIVE COMPLIANCE DIVISION", 
                                        style = MaterialTheme.typography.labelSmall, 
                                        fontWeight = FontWeight.Bold, 
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.dismissLawsuit() }) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss Courtroom")
                            }
                        }
                        
                        // Active statutes and political leadership banner
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Ruling Administration: ${presidentName} (${presidentParty})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Enacted Clinical Laws: ${activePolicies.size} Active",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Court Status Metrics: Tension & Aggression
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("HIGH TRIBUNAL CLIMATE TENSION: $lawsuitTension%", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    if (lawsuitTension > 75) {
                                        Text("🚨 EXTREME JEOPARDY", fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, color = Color.Red)
                                    }
                                }
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = { lawsuitTension / 100f },
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = if (lawsuitTension > 75) Color.Red else Color(0xFFFFD54F),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Text("STATE PROSECUTOR AGGRESSION BIAS: $lawsuitProsecutorAggression%", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFC62828))
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = { lawsuitProsecutorAggression / 100f },
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = Color(0xFFC62828),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(8.dp))

                        // Formal Grand Jury Indictments block
                        Text("📜 FORMAL CONSTITUTIONAL INDICTMENTS & ACCUSATIONS:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                lawsuitCharges.forEach { charge ->
                                    val isStatuteBreach = charge.contains("STATUTORY BREACH")
                                    Row(verticalAlignment = Alignment.Top) {
                                        Text(
                                            text = if (isStatuteBreach) "⚖️ " else "📌 ", 
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = charge,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isStatuteBreach) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onBackground,
                                            fontWeight = if (isStatuteBreach) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text("🗣️ COURT TRIAL TRANSCRIPT & FORENSIC DIALOGUE:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.outline)
                        
                        // Trial Logs content
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                lawsuitLog.forEach { paragraph ->
                                    val isUser = paragraph.startsWith("🎒 DEFENSE SUBMITTED:")
                                    val isVerdict = paragraph.contains("⚖️ FINAL COMMITTEE VERDICT:") || paragraph.contains("⚖️ FINAL") || paragraph.contains("Chief Justice's")
                                    val isProsecution = paragraph.contains("🗣️ PROSECUTION") || paragraph.contains("State Prosecutor:") || paragraph.contains("Presiding Judge:")
                                    
                                    val bgColor = when {
                                        isUser -> Color(0xFF0D47A1)
                                        isVerdict -> Color(0xFF1B5E20)
                                        isProsecution -> Color(0xFF2C1C1C)
                                        else -> Color.Transparent
                                    }
                                    
                                    val fontColor = when {
                                        isUser -> Color.White
                                        isVerdict -> Color(0xFFC8E6C9)
                                        isProsecution -> Color(0xFFFFCDD2)
                                        else -> Color.White
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(bgColor)
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = paragraph,
                                            color = fontColor,
                                            fontSize = 12.1.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = if (isVerdict) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        
                        // Interaction Actions Panel
                        if (lawsuitCurrentStage == "verdict") {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = if (lawsuitVerdict == "Exonerated") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.2.dp, if (lawsuitVerdict == "Exonerated") Color(0xFF2E7D32) else Color(0xFFC62828))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "🏛️ SOVEREIGN JUDICIAL COURT DECREE RENDERED",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        color = if (lawsuitVerdict == "Exonerated") Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                    Text(
                                        text = "Verdict: ${lawsuitVerdict?.uppercase() ?: "JUDGMENT GIVEN"}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (lawsuitVerdict == "Exonerated") Color(0xFF1B5E20) else Color(0xFFC62828),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                    if (lawsuitFine > 0.0) {
                                        Text("Statutory Fine Levied: R${lawsuitFine} (Toll Penalty deducted from clinic balance)", fontWeight = FontWeight.Bold, color = Color(0xFFC62828), style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (lawsuitSuspension > 0) {
                                        Text("Clinical Supervision Penalty Order: $lawsuitSuspension weeks of suspended operations", fontWeight = FontWeight.Bold, color = Color(0xFFC62828), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            
                            Button(
                                onClick = { viewModel.dismissLawsuit() },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Acknowledge Decree & Resume Clinical Practice", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            // NEW ADVOCACY COURT PANEL
                            val roundsRemaining by OrchidDeepStateManager.trialRoundsCount.collectAsStateWithLifecycle()
                            val hiredLawyer by OrchidDeepStateManager.hiredLawyer.collectAsStateWithLifecycle()
                            val selectedEvidence by OrchidDeepStateManager.selectedEvidenceToPresent.collectAsStateWithLifecycle()
                            val availableEvidence by OrchidDeepStateManager.potentialEvidencePool.collectAsStateWithLifecycle()
                            var userPleaMsg by remember { mutableStateOf("") }

                            // 1. Legal Representation section
                            Text(
                                "💼 DEFENSE COUNCILS & LEGAL ADVISORS:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                            )
                            if (hiredLawyer == null) {
                                Text(
                                    text = "No defense lawyer hired. Self-representation is active, leaving you vulnerable to prosecutor aggression (higher tension penalties).",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                OrchidDeepStateManager.defenseLawyersCatalog.forEach { lawyer ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = lawyer.displayName, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                                Text(text = "${lawyer.specialty} | Bias Dampen: -${lawyer.defenseBiasPercent}%", fontSize = 10.sp, color = Color.LightGray)
                                                Text(text = "Retainer: R ${String.format("%.0f", lawyer.retainerFee)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                            Button(
                                                onClick = { viewModel.hireLawyerForTrial(lawyer.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.height(30.dp),
                                                enabled = !isLoading
                                            ) {
                                                Text("HIRE", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(text = "⚖️ REPRESENTATION: ${hiredLawyer!!.displayName}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                        Text(text = "${hiredLawyer!!.specialty} represents your medical license.", fontSize = 10.sp, color = Color(0xFFC8E6C9))
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            // 2. Interactive evidence selection
                            Text(
                                "📁 PRESENT CLINICAL EXHIBITS & EVIDENCE TO BENCH (${selectedEvidence.size} selected):",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            if (availableEvidence.isEmpty()) {
                                Text(
                                    text = "Historical clinical registry is empty. No objective evidence available for dispatch.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    color = Color.LightGray
                                )
                            } else {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                ) {
                                    availableEvidence.forEach { evidence ->
                                        val isSelected = selectedEvidence.contains(evidence)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { OrchidDeepStateManager.toggleEvidenceSelection(evidence) },
                                            label = { Text(evidence, fontSize = 10.sp, maxLines = 1) }
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // 3. Oral pleading custom state
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "🗣️ YOUR ORAL ADVOCACY / TESTIMONY:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Hearing rounds remaining: $roundsRemaining",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (roundsRemaining <= 0) Color.Red else Color.Green
                                )
                            }
                            
                            OutlinedTextField(
                                value = userPleaMsg,
                                onValueChange = { userPleaMsg = it },
                                label = { Text("Describe why you are not liable...") },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(120.dp),
                                placeholder = { Text("E.g., Your Honor, the patient blood logs show extreme shock status, requiring emergency isotonic fluids, in complete alignment with national protocol mandates!") },
                                shape = RoundedCornerShape(8.dp),
                                enabled = roundsRemaining > 0 && !isLoading,
                                maxLines = 4
                            )

                            Spacer(Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.submitInteractiveLawsuitPlea(userPleaMsg, selectedEvidence)
                                        userPleaMsg = ""
                                    },
                                    enabled = roundsRemaining > 0 && !isLoading,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1.1f).height(48.dp)
                                ) {
                                    Text("PLEAD ROUND", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        viewModel.concludeLawsuitInteractiveVerdict()
                                    },
                                    enabled = !isLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(0.9f).height(48.dp)
                                ) {
                                    Text("RULING VERDICT", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    viewModel.concludeLawsuitInteractiveVerdict()
                                },
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isLoading
                            ) {
                                Text("Skip Trial Cross-exam directly to Judicial Verdict", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }

private fun formatCleanLabel(raw: String, suffix: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    if (trimmed.endsWith(suffix, ignoreCase = true)) {
        return trimmed
    }
    if (suffix == "bpm" && (trimmed.contains("bpm", ignoreCase = true) || trimmed.contains("beats", ignoreCase = true))) {
        return trimmed
    }
    if (suffix == "/min" && (trimmed.contains("/min", ignoreCase = true) || trimmed.contains("breaths", ignoreCase = true) || trimmed.contains("rpm", ignoreCase = true))) {
        return trimmed
    }
    if (suffix == "mmHg" && (trimmed.contains("mmHg", ignoreCase = true) || trimmed.contains("bp", ignoreCase = true))) {
        return trimmed
    }
    if (suffix == "%" && (trimmed.contains("%", ignoreCase = true) || trimmed.contains("percent", ignoreCase = true))) {
        return trimmed
    }
    if (suffix == "°C" && (trimmed.contains("°", ignoreCase = true) || trimmed.contains("C", ignoreCase = true))) {
        return trimmed
    }
    return "$trimmed $suffix"
}

@Composable
fun VitalsLayout(vitals: Vitals?) {
    if (vitals == null) return

    val hrClean = vitals.hr.filter { it.isDigit() }.toIntOrNull() ?: 80
    val rrClean = vitals.rr.filter { it.isDigit() }.toIntOrNull() ?: 16

    val infiniteTransition = rememberInfiniteTransition(label = "vitals_pulse_anim")

    // Smooth breathing visual (slower, typical cycle of 2000ms - 5000ms)
    val breatheCycleMs = (60000 / rrClean).coerceIn(1500, 6000)
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(breatheCycleMs / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe_scale"
    )

    // Sharp heartbeat visual (faster, typical cycle of 400ms - 1000ms)
    val pulseCycleMs = (60000 / hrClean).coerceIn(300, 1500)
    val heartScale by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseCycleMs / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heart_scale"
    )

    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blink_alpha"
    )

    var isVitalsExpanded by remember { mutableStateOf(true) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clickable { isVitalsExpanded = !isVitalsExpanded }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MonitorHeart,
                        contentDescription = null,
                        tint = Color(0xFFC62828),
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer(scaleX = heartScale, scaleY = heartScale)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "💓 BEDSIDE VITALS MONITOR",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFC62828)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isVitalsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isVitalsExpanded) "Collapse" else "Expand",
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFFFFEBEE), shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color.Red, shape = CircleShape)
                            .graphicsLayer(alpha = blinkAlpha)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "LIVE",
                        color = Color.Red,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            AnimatedVisibility(visible = isVitalsExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    VitalModule(
                        icon = Icons.Default.LocalHospital, 
                        name = "BP", 
                        value = formatCleanLabel(vitals.bp, "mmHg"), 
                        color = Color(0xFF1565C0)
                    )
                    VitalModule(
                        icon = Icons.Default.HeartBroken, 
                        name = "HR", 
                        value = formatCleanLabel(vitals.hr, "bpm"), 
                        color = Color(0xFFC62828),
                        scale = heartScale
                    )
                    VitalModule(
                        icon = Icons.Default.Thermostat, 
                        name = "Temp", 
                        value = formatCleanLabel("${vitals.tempC}", "°C"), 
                        color = Color(0xFFEF6C00)
                    )
                    VitalModule(
                        icon = Icons.Default.WindPower, 
                        name = "RR", 
                        value = formatCleanLabel(vitals.rr, "/min"), 
                        color = Color(0xFF37474F),
                        scale = breatheScale
                    )
                    VitalModule(
                        icon = Icons.Default.MonitorHeart, 
                        name = "SpO₂", 
                        value = formatCleanLabel(vitals.spo2, "%"), 
                        color = Color(0xFF2E7D32),
                        scale = heartScale
                    )
                }
            }
        }
    }
}

@Composable
fun VitalModule(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    name: String,
    value: String,
    color: Color,
    scale: Float = 1.0f
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color.copy(alpha = 0.8f),
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(name, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun ChatMessageRow(message: ChatMessage) {
    val isDoctor = message.role == "doctor"
    val isSystem = message.role == "system"

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = when {
            isSystem -> Alignment.Center
            isDoctor -> Alignment.CenterEnd
            else -> Alignment.CenterStart
        }
    ) {
        if (isSystem) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isDoctor) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomEnd = if (isDoctor) 0.dp else 16.dp,
                    bottomStart = if (isDoctor) 16.dp else 0.dp
                ),
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .testTag(if (isDoctor) "doctor_chat_bubble" else "patient_chat_bubble")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (isDoctor) "👨‍⚕️ Doctor" else "👤 Patient",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Black,
                            color = if (isDoctor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        if (message.virtualTimestampStr != null) {
                            Text(
                                text = message.virtualTimestampStr,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isDoctor) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ClinicalHubCard(
    uiState: SimulationState,
    hiddenCase: HiddenCaseProfile?,
    isLoading: Boolean,
    viewModel: SimulationViewModel
) {
    var isExpanded by remember { mutableStateOf(false) } // Default collapsed to keep the UI clean & spacious!
    var activeTab by remember { mutableStateOf(0) } // 0: Vitals, 1: Patient Profile, 2: Interventions

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header row: clickable to expand/collapse
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalHospital,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "📋 Bedside Clinical Hub",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Pulse or live indicator
                    if (uiState.vitals != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFEBEE),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "MONITORING",
                                color = Color.Red,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = !isExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎯 Tap to open Vitals, Patient Demographics & Emergency Interventions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // Modern Tab Navigation inside the Clinical Hub
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val tabOptions = listOf("📊 Vitals Signs", "👤 Patient Profile", "⚡ Emergency Desk", "💊 Dispensary Cabinet", "⚖️ Legal & World")
                        tabOptions.forEachIndexed { index, title ->
                            val selected = activeTab == index
                            AssistChip(
                                onClick = { activeTab = index },
                                label = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    labelColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    // Subsections content
                    when (activeTab) {
                        0 -> {
                            // Monitored Vitals
                            VitalsLayout(uiState.vitals)
                        }
                        1 -> {
                            // Patient Profile & File & Daily Challenge
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (hiddenCase != null) {
                                    Text(
                                        "Demographics & Case Flags:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "🩺 ${hiddenCase.specialty.uppercase()}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }

                                        val isSevere = hiddenCase.severity.equals("Severe", ignoreCase = true)
                                        val badgeColor = if (isSevere) Color(0xFFC62828) else Color(0xFF2E7D32)
                                        Box(
                                            modifier = Modifier
                                                .background(badgeColor.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (isSevere) "🚨 CRITICAL SEVERITY" else "✅ ROUTINE PX",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Black,
                                                color = badgeColor
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFE3F2FD), shape = RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "💳 ${hiddenCase.insuranceStatus}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1565C0)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (hiddenCase.patientDemographics.startsWith("Patient: ")) {
                                                    val raw = hiddenCase.patientDemographics
                                                    val nameAndMrn = raw.substring(9).substringBefore(" • ")
                                                    val actualDemos = raw.substringAfter(" • ", "")
                                                    "👤 $nameAndMrn | 📋 $actualDemos"
                                                } else {
                                                    "👤 ${hiddenCase.patientDemographics}"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    val legalRiskReport by viewModel.currentLegalRiskReport.collectAsState()
                                    if (legalRiskReport == null) {
                                        OutlinedButton(
                                            onClick = { viewModel.assessLegalRiskBeforeConsult() },
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("🤖 AI LEGAL SCAN (Scan laws vs Case Profile)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text("⚖️ PRE-CONSULTATION LEGAL RISK BRIEF", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                                                Text(legalRiskReport!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(top = 4.dp))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    val stabilityColor = when (uiState.patientStability) {
                                        "Stable", "Improving" -> Color(0xFF2E7D32)
                                        "Critical" -> Color(0xFFC62828)
                                        "Deteriorating" -> Color(0xFFD84315)
                                        else -> Color(0xFFE65100)
                                    }

                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "Patient Condition Status:",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(
                                                    text = "Mood: ${uiState.patientMood}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "•",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.Gray
                                                )
                                                Text(
                                                    text = "Stability: ${uiState.patientStability}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = stabilityColor,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                    }
                                }

                                // Daily Objectives Banner
                                if (!uiState.isEncounterComplete) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                                        shape = RoundedCornerShape(10.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Flag,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onTertiary,
                                                    modifier = Modifier.padding(4.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    "DAILY CHALLENGE",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                                Text(
                                                    "Achieve 85% Score in this ${uiState.patientStability} case.",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            // Emergency Desk / Critical Interventions
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "🚨 EMERGENCY DESK INTERVENTIONS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                if (uiState.isEncounterComplete) {
                                    Text(
                                        "Case is closed. No further interventions necessary.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        listOf("O2 Supply", "IV Fluids", "Adrenaline", "Defibrillate").forEach { intervention ->
                                            AssistChip(
                                                onClick = { viewModel.applyIntervention(intervention) },
                                                label = { Text(intervention, fontSize = 11.sp, fontWeight = FontWeight.Black) },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = when(intervention) {
                                                            "O2 Supply" -> Icons.Default.Air
                                                            "IV Fluids" -> Icons.Default.WaterDrop
                                                            "Adrenaline" -> Icons.Default.FlashOn
                                                            else -> Icons.Default.Bolt
                                                        },
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                },
                                                enabled = !isLoading,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        3 -> {
                            DispensaryCabinetPanel(viewModel)
                        }
                        4 -> {
                            WorldStatePanel(viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyPracticeClosureCard(
    currentDay: Int,
    patientsSeenToday: Int,
    dailyRevenue: Double,
    dailyExpenses: Double,
    clinicBalance: Double,
    syringeStock: Int,
    salineStock: Int,
    adrenalineStock: Int,
    reagentsStock: Int,
    medsStock: Int,
    viewModel: SimulationViewModel
) {
    var isExpanded by remember { mutableStateOf(patientsSeenToday >= 5) }

    LaunchedEffect(patientsSeenToday) {
        if (patientsSeenToday >= 5) {
            isExpanded = true
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (patientsSeenToday >= 5)
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.95f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .animateContentSize()
            .testTag("daily_practice_closure_card"),
        shape = RoundedCornerShape(16.dp),
        border = if (patientsSeenToday >= 5) BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (patientsSeenToday >= 5) Icons.Default.CheckCircle else Icons.Default.Description,
                        contentDescription = null,
                        tint = if (patientsSeenToday >= 5) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🌅 Day $currentDay Practice Report",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (patientsSeenToday >= 5) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { isExpanded = !isExpanded }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand day report"
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "📊 SHIFT FINANCIAL AUDIT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Consults Completed Today:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$patientsSeenToday px seen", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Revenue Earned today:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("R ${String.format("%.2f", dailyRevenue)} ZAR", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Expenses Incurred today:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("R ${String.format("%.2f", dailyExpenses)} ZAR", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            val surplus = dailyRevenue - dailyExpenses
                            Text("Net Daily Profit Flow:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = "R ${String.format("%.2f", surplus)} ZAR",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (surplus >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "📦 CLINIC RE-STOCKING DESK",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InventoryRestockLine(itemName = "Syringes", currentStock = syringeStock, price = 100.0, quantity = 10, wallet = clinicBalance) {
                        viewModel.restockInventory("Syringes", 10)
                    }
                    InventoryRestockLine(itemName = "Saline", currentStock = salineStock, price = 400.0, quantity = 5, wallet = clinicBalance) {
                        viewModel.restockInventory("Saline", 5)
                    }
                    InventoryRestockLine(itemName = "Adrenaline", currentStock = adrenalineStock, price = 750.0, quantity = 5, wallet = clinicBalance) {
                        viewModel.restockInventory("Adrenaline", 5)
                    }
                    InventoryRestockLine(itemName = "Reagents", currentStock = reagentsStock, price = 250.0, quantity = 10, wallet = clinicBalance) {
                        viewModel.restockInventory("Reagents", 10)
                    }
                    InventoryRestockLine(itemName = "Meds", currentStock = medsStock, price = 1000.0, quantity = 5, wallet = clinicBalance) {
                        viewModel.restockInventory("Meds", 5)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- NEW: AI CLINICAL STOCKING PLANNER PANEL ---
                val aiProposal by viewModel.aiStockingProposal.collectAsStateWithLifecycle()
                val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
                var aiStockingInputText by remember { mutableStateOf("") }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF14221E) // Deep medical twilight mint-green
                    ),
                    border = BorderStroke(1.2.dp, Color(0xFF00BFA5).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🤖", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "AI CLINICAL STOCKING PLANNER",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF1DE9B6)
                                    )
                                    Text(
                                        text = "Intelligent natural language procurement",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.LightGray
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF00BFA5)
                            ) {
                                Text(
                                    text = "GEMINI CO-PILOT",
                                    fontSize = 8.sp,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFF00796B))
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Input natural instructions below (e.g. 'restock syringes and saline' or 'buy 2 of everything except meds' or 'optimize what we need under R600').",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = aiStockingInputText,
                            onValueChange = { aiStockingInputText = it },
                            label = { Text("Stocking request instruction...") },
                            placeholder = { Text("e.g. 'Optimize low resources under R800'") },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isLoading,
                            textStyle = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                viewModel.submitAiStockingRequest(aiStockingInputText)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5)),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isLoading && aiStockingInputText.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ENGAGE CO-PILOT PLANNER", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                        }

                        if (isLoading) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    color = Color(0xFF1DE9B6),
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing requirements & compiling cost sheet...", fontSize = 11.sp, color = Color(0xFF1DE9B6))
                            }
                        }

                        aiProposal?.let { ProposalView ->
                            Spacer(modifier = Modifier.height(14.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF042B24)),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (ProposalView.isValidPurchase) Color(0xFF1DE9B6) else Color(0xFFD32F2F)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = if (ProposalView.isValidPurchase) "🎯 RECOMMENDED CO-PILOT PLAN:" else "⚠️ PLAN VALIDATION ISSUE:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = if (ProposalView.isValidPurchase) Color(0xFF1DE9B6) else Color(0xFFFF8A80)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = ProposalView.explanation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White,
                                        lineHeight = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Itemized Quantities Grid / Column
                                    Text(
                                        text = "PROPOSED PROCUREMENT DETAILS:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.LightGray
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    val itemLines = mutableListOf<String>()
                                    if (ProposalView.syringeQty > 0) itemLines.add("📦 Syringes: +${ProposalView.syringeQty} units (R ${ProposalView.syringeQty * 10})")
                                    if (ProposalView.salineQty > 0) itemLines.add("💧 Saline Bags: +${ProposalView.salineQty} units (R ${ProposalView.salineQty * 80})")
                                    if (ProposalView.adrenalineQty > 0) itemLines.add("⚡ Adrenaline Vials: +${ProposalView.adrenalineQty} units (R ${ProposalView.adrenalineQty * 150})")
                                    if (ProposalView.reagentsQty > 0) itemLines.add("🧪 Lab Reagents: +${ProposalView.reagentsQty} units (R ${ProposalView.reagentsQty * 25})")
                                    if (ProposalView.medsQty > 0) itemLines.add("💊 Emergency Meds: +${ProposalView.medsQty} units (R ${ProposalView.medsQty * 200})")

                                    if (itemLines.isEmpty()) {
                                        Text("- No items recommended for purchase.", fontSize = 11.sp, fontStyle = FontStyle.Italic, color = Color.Gray)
                                    } else {
                                        itemLines.forEach { line ->
                                            Text(line, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = Color(0xFF004D40))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Estimated Bill:", fontSize = 11.sp, color = Color.LightGray)
                                        Text(
                                            text = "R ${String.format("%.2f", ProposalView.estimatedTotalCost)} ZAR",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (ProposalView.isValidPurchase) Color(0xFF1DE9B6) else Color(0xFFFF8A80)
                                        )
                                    }

                                    if (!ProposalView.isValidPurchase) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = ProposalView.validationMessage.ifBlank { "Total estimated cost exceeds current wallet balance." },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFFF8A80),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.dismissAiStockingProposal() },
                                            border = BorderStroke(1.dp, Color(0xFF00BFA5)),
                                            shape = RoundedCornerShape(8.dp),
                                            enabled = !isLoading,
                                            modifier = Modifier.weight(1f).height(36.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00BFA5))
                                        ) {
                                            Text("DISMISS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.approveAndExecuteStockingProposal()
                                                aiStockingInputText = ""
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DE9B6), contentColor = Color.Black),
                                            shape = RoundedCornerShape(8.dp),
                                            enabled = ProposalView.isValidPurchase && !isLoading,
                                            modifier = Modifier.weight(1f).height(36.dp)
                                        ) {
                                            Text("APPROVE & BUY", fontSize = 10.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.advanceDayPrac() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("advance_day_button"),
                    enabled = patientsSeenToday >= 5,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (patientsSeenToday >= 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        contentColor = if (patientsSeenToday >= 5) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (patientsSeenToday >= 5) "🌅 ADVANCE TO DAY ${currentDay + 1}" else "⏳ COMPLETE 5 CONSULTATIONS TO ADVANCE SHIFT",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun InventoryRestockLine(
    itemName: String,
    currentStock: Int,
    price: Double,
    quantity: Int,
    wallet: Double,
    onRestock: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(itemName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("Stock level: $currentStock left", style = MaterialTheme.typography.bodySmall, color = if (currentStock < 5) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Button(
                onClick = onRestock,
                enabled = wallet >= price,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(32.dp).testTag("restock_${itemName.lowercase()}")
            ) {
                Text("Order +$quantity (R ${price.toInt()})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun VisualPatientOutcomeBanner(outcome: String, modifier: Modifier = Modifier) {
    val containerColor: Color
    val contentColor: Color
    val borderColor: Color
    val icon: androidx.compose.ui.graphics.vector.ImageVector
    val title: String
    val text: String

    when (outcome) {
        "Deceased" -> {
            containerColor = Color(0xFF1E1E1E)
            contentColor = Color(0xFFE57373)
            borderColor = Color(0xFFC62828)
            icon = Icons.Default.Flag
            title = "✝️ FATAL CLINICAL INCIDENT REPORT"
            text = "Tragically, critical clinical deteriorations or misaligned diagnostics resulted in a fatal outcome for this patient. Case file filed under adverse medical incident records."
        }
        "Transferred Out" -> {
            containerColor = Color(0xFFFFF3E0)
            contentColor = Color(0xFFE65100)
            borderColor = Color(0xFFEF6C00)
            icon = Icons.Default.Description
            title = "🚶 PATIENT WALKED OUT / TRANSFERRED CARE"
            text = "Due to delays or mismatch in management design, the patient has chosen to seek an alternative professional opinion and transferred out of your general practice."
        }
        else -> {
            containerColor = Color(0xFFE8F5E9)
            contentColor = Color(0xFF2E7D32)
            borderColor = Color(0xFF4CAF50)
            icon = Icons.Default.CheckCircle
            title = "🎉 CASE DISCHARGED & COMMENDED"
            text = "Excellent care achieved! This clinical query is officially closed under stable, recovered conditions. Patient has been safely discharged with clear records."
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(2.dp, borderColor),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("patient_outcome_banner")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = contentColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun StateAndLegislationTab(
    viewModel: SimulationViewModel,
    onAdmittedClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val countryName by viewModel.countryName.collectAsStateWithLifecycle()
    val presidentName by viewModel.presidentName.collectAsStateWithLifecycle()
    val presidentParty by viewModel.presidentParty.collectAsStateWithLifecycle()
    val presidentApproval by viewModel.presidentApproval.collectAsStateWithLifecycle()
    val politicalPrestige by viewModel.politicalPrestige.collectAsStateWithLifecycle()
    val activePolicies by viewModel.activePolicies.collectAsStateWithLifecycle()
    
    val currentDraftPolicy by viewModel.currentDraftPolicy.collectAsStateWithLifecycle()
    val isVotingActive by viewModel.isVotingActive.collectAsStateWithLifecycle()
    val voteProgress by viewModel.voteProgress.collectAsStateWithLifecycle()
    val currentVoteYes by viewModel.currentVoteYes.collectAsStateWithLifecycle()
    val currentVoteNo by viewModel.currentVoteNo.collectAsStateWithLifecycle()
    val currentVoteAbstain by viewModel.currentVoteAbstain.collectAsStateWithLifecycle()
    val votingLog by viewModel.votingLog.collectAsStateWithLifecycle()
    val sickPoliticianAlert by viewModel.sickPoliticianAlert.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val progressiveLobbyBias by viewModel.progressiveLobbyBias.collectAsStateWithLifecycle()
    val conservativeLobbyBias by viewModel.conservativeLobbyBias.collectAsStateWithLifecycle()
    val independentLobbyBias by viewModel.independentLobbyBias.collectAsStateWithLifecycle()
    val lastLobbyReport by viewModel.lastLobbyReport.collectAsStateWithLifecycle()

    var selectedLobbyFaction by remember { mutableStateOf("Progressives") }
    var selectedLobbyPitch by remember { mutableStateOf("Clinical Safety & Standards") }
    var lobbyCustomMessage by remember { mutableStateOf("") }

    var showEditCountryDialog by remember { mutableStateOf(false) }
    var draftFocusText by remember { mutableStateOf("") }

    var editCountryName by remember { mutableStateOf(countryName) }
    var editPresidentName by remember { mutableStateOf(presidentName) }
    var editPresidentParty by remember { mutableStateOf(presidentParty) }

    var draftMode by remember { mutableStateOf("AI") } // "AI" or "Manual"
    var manualTitle by remember { mutableStateOf("") }
    var manualSummary by remember { mutableStateOf("") }
    var manualClinicalRule by remember { mutableStateOf("") }
    var manualEconomicImpact by remember { mutableStateOf("") }
    var clauseInputText by remember { mutableStateOf("") }
    var manualClausesList by remember { mutableStateOf(emptyList<String>()) }

    var isAmendingDraft by remember { mutableStateOf(false) }
    var amendTitle by remember { mutableStateOf("") }
    var amendSummary by remember { mutableStateOf("") }
    var amendClinicalRule by remember { mutableStateOf("") }
    var amendEconomicImpact by remember { mutableStateOf("") }
    var amendClausesList by remember { mutableStateOf(emptyList<String>()) }
    var amendNewClauseInput by remember { mutableStateOf("") }

    LaunchedEffect(showEditCountryDialog) {
        if (showEditCountryDialog) {
            editCountryName = countryName
            editPresidentName = presidentName
            editPresidentParty = presidentParty
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. SOVEREIGN COUNTRY PROFILE CARD ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "👑 SOVEREIGN STATE POLICY CONTROL",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Administer health codes and execute executive mandates",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showEditCountryDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Edit Country Specs")
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Nation State", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(countryName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("President Persona", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(presidentName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ruling Party / Bloc", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(presidentParty, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Executive Approval", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text("$presidentApproval%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Political Prestige Score:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "$politicalPrestige / 100",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Black,
                            color = if (politicalPrestige >= 50) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { politicalPrestige.toFloat() / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = if (politicalPrestige >= 50) Color(0xFF2E7D32) else Color(0xFFC62828),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // --- NATION DRUG BUILDER & COMPLIANCE PRIORITIES ---
        val regulatoryAuditScore by OrchidDeepStateManager.orchidIntelligence.collectAsStateWithLifecycle()
        val nationalPriorityCount by OrchidDeepStateManager.completedDirectivesCount.collectAsStateWithLifecycle()
        val activeMandatesList by OrchidDeepStateManager.activeDirectives.collectAsStateWithLifecycle()
        val primaryMandate = activeMandatesList.firstOrNull() ?: "Maintain strict compliance with healthcare safety bylaws."

        var showDrugBuilderForm by remember { mutableStateOf(false) }
        var newDrugName by remember { mutableStateOf("") }
        var newDrugCategory by remember { mutableStateOf("Schedule 4 (Prescription Medication)") }
        var newDrugCostInput by remember { mutableStateOf("") }
        var newDrugBPChange by remember { mutableStateOf("Raises (+10 mmHg)") }
        var newDrugHRChange by remember { mutableStateOf("Stabilizes (-5 bpm)") }
        var newDrugTherapyEffect by remember { mutableStateOf("") }
        var newDrugDescription by remember { mutableStateOf("") }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏛️", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "NATION PHARMACEUTICAL DIRECTORY",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "HPCSA Regulatory & Custom Drug Architect",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    TextButton(onClick = { showDrugBuilderForm = !showDrugBuilderForm }) {
                        Text(if (showDrugBuilderForm) "Collapse Form ✖" else "Add Custom Drug ➕", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                    }
                }

                if (showDrugBuilderForm) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("🧪 ARCHITECT A NEW THERAPEUTIC COMPOUND", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { 
                            viewModel.autoArchitectCompound(primaryMandate) { name, category, cost, bp, hr, effect, desc ->
                                newDrugName = name
                                newDrugCategory = category
                                newDrugCostInput = cost
                                newDrugBPChange = bp
                                newDrugHRChange = hr
                                newDrugTherapyEffect = effect
                                newDrugDescription = desc
                            }
                        }) {
                            Text("✨ AI Auto-Architect", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newDrugName,
                        onValueChange = { newDrugName = it },
                        label = { Text("Drug Name (e.g. Prozac tablets)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newDrugCategory,
                            onValueChange = { newDrugCategory = it },
                            label = { Text("Schedule Category", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newDrugCostInput,
                            onValueChange = { newDrugCostInput = it },
                            label = { Text("Cost (ZAR per pack)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newDrugBPChange,
                            onValueChange = { newDrugBPChange = it },
                            label = { Text("BP Delta (e.g. Raises +10)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newDrugHRChange,
                            onValueChange = { newDrugHRChange = it },
                            label = { Text("HR Delta (e.g. Drops -5)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = newDrugTherapyEffect,
                        onValueChange = { newDrugTherapyEffect = it },
                        label = { Text("Therapeutic Clinical Indication Effect", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = newDrugDescription,
                        onValueChange = { newDrugDescription = it },
                        label = { Text("Pharmacology Description", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (newDrugName.isNotBlank() && newDrugTherapyEffect.isNotBlank()) {
                                val costVal = newDrugCostInput.toDoubleOrNull() ?: 150.0
                                OrchidDeepStateManager.addNewCustomItem(
                                    name = newDrugName,
                                    classification = newDrugCategory,
                                    description = newDrugDescription.takeIf { it.isNotBlank() } ?: "Custom-developed therapeutic agent registered in the nation's medical formulary.",
                                    purchaseCost = costVal,
                                    bpDelta = newDrugBPChange,
                                    hrDelta = newDrugHRChange,
                                    clinicalImpact = newDrugTherapyEffect
                                )
                                newDrugName = ""
                                newDrugDescription = ""
                                newDrugTherapyEffect = ""
                                newDrugCostInput = ""
                                showDrugBuilderForm = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Text("🧪 INJECT COMPOUND INTO SOVEREIGN CATALOGUE", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Compliance Integrity", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "$regulatoryAuditScore / 100 PTS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (regulatoryAuditScore >= 80) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { regulatoryAuditScore / 100f },
                            color = if (regulatoryAuditScore >= 80) Color(0xFF2E7D32) else Color(0xFFC62828),
                            trackColor = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Active Mandates", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$nationalPriorityCount Mandates Fulf.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Add Free Health Toggle here
                        val isFreeHealthEnabled by OrchidDeepStateManager.isFreeHealthEnabled.collectAsStateWithLifecycle()
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Free Health Policy", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = isFreeHealthEnabled,
                                onCheckedChange = { OrchidDeepStateManager.toggleFreeHealth(it) },
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "📋 PARLIAMENTARY HEALTH PRIORITIES DIRECTIVE:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = primaryMandate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            OrchidDeepStateManager.leakIntelToSyndicate()
                            viewModel.applyIntervention("Request Regulatory Counsel Review")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        Text("CONSULT LOBBY TRUST", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    }

                    OutlinedButton(
                        onClick = {
                            OrchidDeepStateManager.requestNewDirective()
                        },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        Text("REFRESH PRIORITIES", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // --- MEDICAL AID REGISTRY ---
        val medicalAids by OrchidDeepStateManager.medicalAidSchemes.collectAsStateWithLifecycle()
        
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🛡️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "NATIONAL MEDICAL AID REGISTRY",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Active Clinical Financial Guarantors",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    medicalAids.forEach { aid ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(aid.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Coverage: ${(aid.coveragePercent * 100).toInt()}% | Rejection Risk: ${(aid.rejectionProbability * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (aid.requiresPreAuth) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
                                        Text("Pre-Auth", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 2. SICK POLITICIAN CRITICAL ALERT CARD ---
        if (sickPoliticianAlert != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MonitorHeart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "VIP PATIENT DISTRESS DIRECTIVE",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = sickPoliticianAlert ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                onAdmittedClicked()
                                viewModel.admitPoliticianToClinic()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("🏥 ADMIT & CURE NOW", fontWeight = FontWeight.Black)
                        }
                        OutlinedButton(
                            onClick = { viewModel.dismissPoliticianSicknessAlert() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Postpone")
                        }
                    }
                }
            }
        } else {
            OutlinedButton(
                onClick = { viewModel.parliamentViewModel.triggerPoliticianSickness(viewModel.presidentName.value) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.MonitorHeart, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("🚨 SIMULATE VIP SICKNESS ALERT", fontWeight = FontWeight.Bold)
            }
        }

        // --- 3. DRAFT NEW HEALTH POLICY FORM ---
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "✍ " + "FORMULATE LEGISLATIVE DEMAND",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { draftMode = "AI" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (draftMode == "AI") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (draftMode == "AI") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🤖 AI Assisted Draft", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { draftMode = "Manual" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (draftMode == "Manual") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (draftMode == "Manual") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("✍️ Manual Designer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                if (draftMode == "AI") {
                    Text(
                        text = "Prompt the AI to draft a dynamic health code policy bill with extended clauses. Once voted by Parliament and signed by the President, this law is immediately live and enforced.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = draftFocusText,
                        onValueChange = { draftFocusText = it },
                        placeholder = { Text("e.g. Ensure all severe cases get ECG, mandate full blood count labs, reduce diagnostics cost, check clinical vitals before prescribing") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (draftFocusText.isNotBlank()) {
                                viewModel.generateHealthPolicyDraft(draftFocusText)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = draftFocusText.isNotBlank() && !isLoading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("🖋️ DRAFT CONSTITUTIONAL ACT", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text(
                        text = "Flesh out custom statutory clauses, clinical runtime constraints, and economic consequences directly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = manualTitle,
                        onValueChange = { manualTitle = it },
                        label = { Text("Bill Title") },
                        placeholder = { Text("e.g. Mandatory Pathology Assessment Act") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = manualSummary,
                        onValueChange = { manualSummary = it },
                        label = { Text("Presidential / Executive Summary") },
                        placeholder = { Text("e.g. This statute ensures clinicians verify blood pathologies before concluding diagnosis.") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = manualClinicalRule,
                        onValueChange = { manualClinicalRule = it },
                        label = { Text("Simulation Enforcement Keyword Rule") },
                        placeholder = { Text("e.g. blood count / vitals / generic / sick note / referral") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text(
                        text = "💡 Dynamic trigger keyword: use 'vitals', 'generic', 'consent', 'ecg', 'blood count', 'sick note', or 'referral' to engage automated compliance audits in the sim.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = manualEconomicImpact,
                        onValueChange = { manualEconomicImpact = it },
                        label = { Text("Estimated Economic/Treasury Impact") },
                        placeholder = { Text("e.g. Fine of R500 for non-compliance, R15 save") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Clauses collection
                    Text(
                        text = "📜 EXTENDED STATUTORY CLAUSES (${manualClausesList.size}):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    if (manualClausesList.isEmpty()) {
                        Text(
                            text = "No clauses added yet. Add at least one clause below to make it an active bill.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            manualClausesList.forEachIndexed { idx, clause ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${idx + 1}. $clause",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            manualClausesList = manualClausesList.toMutableList().apply { removeAt(idx) }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove Clause",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = clauseInputText,
                            onValueChange = { clauseInputText = it },
                            placeholder = { Text("Enter a realistic legal clause text...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        Button(
                            onClick = {
                                if (clauseInputText.isNotBlank()) {
                                    manualClausesList = manualClausesList + clauseInputText.trim()
                                    clauseInputText = ""
                                }
                            },
                            enabled = clauseInputText.isNotBlank(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Add")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.createOrUpdateDraftPolicy(
                                title = manualTitle,
                                summary = manualSummary,
                                clinicalRule = manualClinicalRule,
                                economicImpact = manualEconomicImpact,
                                clauses = manualClausesList
                            )
                            // Clean up
                            manualTitle = ""
                            manualSummary = ""
                            manualClinicalRule = ""
                            manualEconomicImpact = ""
                            manualClausesList = emptyList()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = manualTitle.isNotBlank() && manualClausesList.isNotEmpty() && !isLoading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🖋️ DEPOSIT MANUALLY FORMULATED ACT", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- 4. ACTIVE BILL PARLIAMENT REVIEW BOARD ---
        if (currentDraftPolicy != null) {
            val draft = currentDraftPolicy!!
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = when (draft.status) {
                                "Draft" -> MaterialTheme.colorScheme.secondaryContainer
                                "PresidentDesk" -> MaterialTheme.colorScheme.primaryContainer
                                "Vetoed" -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "BILL STAGE: ${draft.status.uppercase()}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        
                        IconButton(onClick = { viewModel.dismissCurrentDraft() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Proposal")
                        }
                    }

                    if (isAmendingDraft && draft.status == "Draft") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "✏️ IN-CHAMBER AMENDMENT EDITOR",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = amendTitle,
                                    onValueChange = { amendTitle = it },
                                    label = { Text("Amend Title") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = amendSummary,
                                    onValueChange = { amendSummary = it },
                                    label = { Text("Amend Summary") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = amendClinicalRule,
                                    onValueChange = { amendClinicalRule = it },
                                    label = { Text("Amend Enforcement Rule") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = amendEconomicImpact,
                                    onValueChange = { amendEconomicImpact = it },
                                    label = { Text("Amend Treasury Impact") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "📜 EDIT INDIVIDUAL CLAUSES (${amendClausesList.size}):",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    amendClausesList.forEachIndexed { i, clause ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${i+1}. $clause",
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = {
                                                    amendClausesList = amendClausesList.toMutableList().apply { removeAt(i) }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Delete Clause",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = amendNewClauseInput,
                                        onValueChange = { amendNewClauseInput = it },
                                        placeholder = { Text("Add new clause...") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    Button(
                                        onClick = {
                                            if (amendNewClauseInput.isNotBlank()) {
                                                amendClausesList = amendClausesList + amendNewClauseInput.trim()
                                                amendNewClauseInput = ""
                                            }
                                        },
                                        enabled = amendNewClauseInput.isNotBlank(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Add")
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.createOrUpdateDraftPolicy(
                                                title = amendTitle,
                                                summary = amendSummary,
                                                clinicalRule = amendClinicalRule,
                                                economicImpact = amendEconomicImpact,
                                                clauses = amendClausesList,
                                                id = draft.id
                                            )
                                            isAmendingDraft = false
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = amendTitle.isNotBlank() && amendClausesList.isNotEmpty()
                                    ) {
                                        Text("💾 COMMIT AMENDMENTS", fontWeight = FontWeight.Bold)
                                    }
                                    
                                    OutlinedButton(
                                        onClick = { isAmendingDraft = false },
                                        modifier = Modifier.weight(0.5f)
                                    ) {
                                        Text("Dismiss")
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = draft.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                            if (draft.status == "Draft") {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.setLoading(true)
                                            viewModel.parliamentViewModel.AIAutoAmendDraft(onFinished = { viewModel.setLoading(false) })
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
                                    ) {
                                        Text("🤖 AI STRUCTURAL REWRITE", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Black)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            isAmendingDraft = true
                                            amendTitle = draft.title
                                            amendSummary = draft.summary
                                            amendClinicalRule = draft.clinicalRule
                                            amendEconomicImpact = draft.economicImpact
                                            amendClausesList = draft.extendedClauses
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("✏️ MANUAL AMEND", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = draft.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "📜 EXTENDED CODES & CONTRACTS:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        draft.extendedClauses.forEach { clause ->
                            Text(
                                text = "• $clause",
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "🔧 SIMULATOR SCORECARD ENFORCEMENT RULES:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "System active translation: '${draft.clinicalRule}'",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.padding(start = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "📈 ESTIMATED CLINIC TREASURY IMPACT:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = draft.economicImpact,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "🤖 AI POLICY ANALYSIS & FORECASTS:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Public Support Estimate:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text("${draft.publicSupportEstimate ?: "N/A"}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if ((draft.publicSupportEstimate ?: 50) > 50) Color(0xFF2E7D32) else Color(0xFFC62828))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Presidential Alignment:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text("${draft.presidentialAlignment ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Expected Political Opposition:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text("${draft.politicalOpposition ?: "None"}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))

                    }

                    if (isVotingActive) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "🗳️ PROGRESSIVE PARLIAMENTARY ROLL CALL...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { voteProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("🟢 YES (Voted): $currentVoteYes", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            Text("🔴 NO (Opposed): $currentVoteNo", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                            Text("⚪ ABSTAIN: $currentVoteAbstain", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF757575))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    votingLog.takeLast(3).forEach { logLine ->
                        Text(
                            text = logLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    when (draft.status) {
                        "Draft" -> {
                            // Lobbying Dashboard Cards & Actions
                            Text(
                                text = "📢 PARLIAMENTARY LOBBYING CENTER",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                            Text(
                                text = "Influence vote counts by presenting clinical arguments to Parliamentary party caucus leadership. Each lobbying attempt costs 5 Political Prestige or R500 consultant fee.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Displays biases
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Current Stances & Outreach Biases:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    val progBiasPct = String.format("%+.0f%%", progressiveLobbyBias * 100)
                                    Text("🔵 Progressives (84 seats): Safety-aligned | Lobby Bias: $progBiasPct", style = MaterialTheme.typography.bodySmall)
                                    
                                    val consBiasPct = String.format("%+.0f%%", conservativeLobbyBias * 100)
                                    Text("🔴 Conservatives (76 seats): Cost-aligned | Lobby Bias: $consBiasPct", style = MaterialTheme.typography.bodySmall)
                                    
                                    val indBiasPct = String.format("%+.0f%%", independentLobbyBias * 100)
                                    Text("⚪ Independents (40 seats): Pragmatic | Lobby Bias: $indBiasPct", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Select target faction
                            Text("Select Target Caucus:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val factionsList = listOf("Progressives", "Conservatives", "Independents")
                                factionsList.forEach { f ->
                                    val isSel = selectedLobbyFaction == f
                                    AssistChip(
                                        onClick = { selectedLobbyFaction = f },
                                        label = { Text(f, style = MaterialTheme.typography.bodySmall) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            labelColor = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Select pitch angle
                            Text("Select Pitch Angle Argument:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val angles = listOf("Safety & Vitals", "Cost Reduction", "Sovereign Welfare")
                                angles.forEach { a ->
                                    val realAngle = when(a) {
                                        "Safety & Vitals" -> "Clinical Safety & Standards"
                                        "Cost Reduction" -> "Economic Cost Reduction & Efficiency"
                                        else -> "Pragmatic Balance & Local Standards"
                                    }
                                    val isSel = selectedLobbyPitch == realAngle
                                    AssistChip(
                                        onClick = { selectedLobbyPitch = realAngle },
                                        label = { Text(a, style = MaterialTheme.typography.bodySmall) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = if (isSel) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                            labelColor = if (isSel) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Free-form message input
                            OutlinedTextField(
                                value = lobbyCustomMessage,
                                onValueChange = { lobbyCustomMessage = it },
                                label = { Text("Outline custom argument context...", style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2,
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Lobby Trigger Button
                            Button(
                                onClick = { 
                                    viewModel.setLoading(true)
                                    viewModel.parliamentViewModel.lobbyFaction(
                                        selectedLobbyFaction, 
                                        selectedLobbyPitch, 
                                        lobbyCustomMessage,
                                        viewModel.clinicBalance.value,
                                        viewModel.politicalPrestige.value,
                                        viewModel.reputationStars.value,
                                        onFinished = { _, _ -> viewModel.setLoading(false) }
                                    )
                                    lobbyCustomMessage = "" 
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                ),
                                enabled = !isVotingActive && !isLoading
                            ) {
                                Text("📢 CONVINCE CAUCUS LEADERSHIP", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }
                            
                            // Show last lobby response transcript memo
                            lastLobbyReport?.let { report ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFFFF9C4), // Golden paper alert background
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = report,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Black
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Button(
                                            onClick = { viewModel.dismissLobbyReport() },
                                            modifier = Modifier.align(Alignment.End),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.Black,
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Text("Dismiss Stance", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Voting dispatch
                            Button(
                                onClick = { 
                                    viewModel.parliamentViewModel.runParliamentaryVote(
                                        draft,
                                        viewModel.politicalPrestige.value,
                                        onVoteFinished = { _, passed ->
                                            if (passed) {
                                                viewModel.updatePoliticalPrestige((viewModel.politicalPrestige.value + 8).coerceAtMost(100))
                                            } else {
                                                viewModel.updatePoliticalPrestige((viewModel.politicalPrestige.value - 6).coerceAtLeast(0))
                                            }
                                        }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isVotingActive
                            ) {
                                Text("🗳️ DISPATCH TO GENERAL PARLIAMENT FOR VOTE", fontWeight = FontWeight.Bold)
                            }
                        }
                        "PresidentDesk" -> {
                            Text(
                                text = "PRESIDENT PERSONA DESK MEMO:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.setLoading(true)
                                        viewModel.parliamentViewModel.presidentialSignDraft(onFinished = { _, _ -> viewModel.setLoading(false) })
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2E7D32),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Text("✍️ APPROVE & SIGN", fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        viewModel.setLoading(true)
                                        viewModel.parliamentViewModel.presidentialVetoDraft(onFinished = { _, _ -> viewModel.setLoading(false) })
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFC62828),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("🚫 VETO BILL", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        "Vetoed" -> {
                            Text(
                                text = "President vetoed this health bill. If you have at least 40 Prestige and mobilize political caucus groupings, you can override with a 2/3 supermajority.",
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Button(
                                onClick = { 
                                    viewModel.parliamentViewModel.attemptParliamentOverride(
                                        viewModel.politicalPrestige.value,
                                        onFinished = { _, _ -> }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                ),
                                enabled = politicalPrestige >= 40 && !isVotingActive,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🔥 MOBILIZE ASSEMBLY OVERRIDE", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- 5. APPROVED CONSTITUTION ARCHIVE ---
        Text(
            text = "⚖️ APPROVED CODES & SUPREME HEALTH LAWS (${activePolicies.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(top = 8.dp)
        )

        if (activePolicies.isEmpty()) {
            Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No dynamic health codes signed yet. Design and pass health bills above to build the nation!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            activePolicies.forEach { policy ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📜 ACT: ${policy.title}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Surface(
                                color = Color(0xFF2E7D32),
                                contentColor = Color.White,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "ENFORCED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = policy.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🔧 Live Operational Constraint: '${policy.clinicalRule}'",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedButton(
                            onClick = {
                                viewModel.draftAmendment(policy)
                                isAmendingDraft = true
                                amendTitle = policy.title
                                amendSummary = policy.summary
                                amendClinicalRule = policy.clinicalRule
                                amendEconomicImpact = policy.economicImpact
                                amendClausesList = policy.extendedClauses
                            },
                            modifier = Modifier.align(Alignment.End),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("✏️ PROPOSE AMENDMENT ACT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedButton(
                onClick = { viewModel.clearApprovedPolicies() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("🧹 CLEAR ACTIVE STATUTES", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showEditCountryDialog) {
        AlertDialog(
            onDismissRequest = { showEditCountryDialog = false },
            title = { Text("Configure Sovereign State") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editCountryName,
                        onValueChange = { editCountryName = it },
                        label = { Text("Country Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPresidentName,
                        onValueChange = { editPresidentName = it },
                        label = { Text("President Persona Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPresidentParty,
                        onValueChange = { editPresidentParty = it },
                        label = { Text("Ruling Faction Party") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (editCountryName.isNotBlank()) viewModel.updateCountryName(editCountryName)
                    if (editPresidentName.isNotBlank()) viewModel.updatePresidentName(editPresidentName)
                    if (editPresidentParty.isNotBlank()) viewModel.updatePresidentParty(editPresidentParty)
                    showEditCountryDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditCountryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun WorldStatePanel(viewModel: SimulationViewModel) {
    val snapshot by viewModel.worldSnapshot.collectAsStateWithLifecycle()
    
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp)
            .verticalScroll(rememberScrollState())
    ) {
        val world = snapshot ?: return@Column
        
        // Status Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("LICENSE STATUS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(world.licenseStatus.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("REPUTATION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("${world.reputationScore}/100", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                }
            }
        }

        // Active Laws
        Text("ACTIVE STATUTORY REGULATIONS:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        if (world.activeLaws.isEmpty()) {
            Text("No special regulations in effect.", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
        } else {
            world.activeLaws.forEach { law ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(law.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text(law.description, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("VIOLATION PENALTY: ${law.violationPenalty}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Outstanding Fines
        if (world.activeFines.isNotEmpty()) {
            Text("OUTSTANDING LEGAL FINES:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
            world.activeFines.forEach { fine ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(fine.reason, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text("Amount Due: ${fine.amount} ZAR", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { /* Could implement pay fine logic */ }) {
                            Icon(imageVector = Icons.Default.Payments, contentDescription = "Pay")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DispensaryCabinetPanel(viewModel: SimulationViewModel) {
    val stockMap by OrchidDeepStateManager.dispensaryInventory.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val currentBal by viewModel.clinicBalance.collectAsStateWithLifecycle()

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .padding(vertical = 4.dp)
            .heightIn(max = 400.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "💊 PHARMACEUTICAL CABINET & INVENTORY",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Funds: R ${String.format("%.2f", currentBal)}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Text(
            text = "Dispense prescribed medications directly to the patient's bedside. Adhere safely to national drug schedules and active safety protocols. Statutory regulatory guidelines regulate narcotics and critical prescriptions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        val dynamicCatalog by OrchidDeepStateManager.availableCatalogFlow.collectAsStateWithLifecycle()

        dynamicCatalog.forEach { item ->
            val stock = stockMap[item.id] ?: 0
            val containerColor = when {
                item.classification.contains("Schedule 8", ignoreCase = true) -> Color(0xFF421C1C) // Dark ruby
                item.classification.contains("Schedule 5", ignoreCase = true) -> Color(0xFF2E1A47) // Dark violet
                item.classification.contains("Schedule 4", ignoreCase = true) -> Color(0xFF42301C) // Dark bronze
                else -> Color(0xFF1C3A1C) // Dark emerald
            }
            val labelColor = when {
                item.classification.contains("Schedule 8", ignoreCase = true) -> Color(0xFFFFCDD2)
                item.classification.contains("Schedule 5", ignoreCase = true) -> Color(0xFFE1BEE7)
                item.classification.contains("Schedule 4", ignoreCase = true) -> Color(0xFFFFE0B2)
                else -> Color(0xFFC8E6C9)
            }
            val chipColor = when {
                item.classification.contains("Schedule 8", ignoreCase = true) -> Color(0xFFC62828)
                item.classification.contains("Schedule 5", ignoreCase = true) -> Color(0xFF8E24AA)
                item.classification.contains("Schedule 4", ignoreCase = true) -> Color(0xFFEF6C00)
                else -> Color(0xFF2E7D32)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = containerColor),
                border = BorderStroke(1.dp, labelColor.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = chipColor,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = item.classification.uppercase(),
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "⚡ Clin. Shift: ${item.patientBPDelta} | ${item.patientHRDelta}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "📦 Stock remaining: $stock units | Restock cost: R ${String.format("%.0f", item.purchaseCost)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.dispenseDispensaryItemToPatient(item.id) },
                            enabled = stock > 0 && !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = chipColor, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.2f).height(36.dp)
                        ) {
                            Text("ADMINISTER", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                        OutlinedButton(
                            onClick = { viewModel.buyDispensaryRestock(item.id, 1) },
                            enabled = currentBal >= item.purchaseCost && !isLoading,
                            border = BorderStroke(1.dp, labelColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(0.8f).height(36.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = labelColor)
                        ) {
                            Text("BUY +1 STOCK", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}


