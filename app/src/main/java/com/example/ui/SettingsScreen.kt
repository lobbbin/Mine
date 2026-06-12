package com.example.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SimulationViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val savedApiKey by viewModel.apiKey.collectAsState()
    val savedProvider by viewModel.provider.collectAsState()
    val savedModel by viewModel.model.collectAsState()
    val savedCustomEndpoint by viewModel.customEndpoint.collectAsState()
    val preferredSpecialty by viewModel.preferredSpecialty.collectAsState()
    val preferredSeverity by viewModel.preferredSeverity.collectAsState()
    val savedConsultFee by viewModel.consultationFee.collectAsState()
    val savedLabCost by viewModel.labCost.collectAsState()
    val savedSpecCost by viewModel.specialistCost.collectAsState()
    
    val doctorXp by viewModel.doctorXp.collectAsState()
    val doctorRank by viewModel.doctorRank.collectAsState()

    var apiKeyInput by remember(savedApiKey) { mutableStateOf(savedApiKey ?: "") }
    var providerInput by remember(savedProvider) { mutableStateOf(savedProvider) }
    var modelInput by remember(savedModel) { mutableStateOf(savedModel) }
    var customEndpointInput by remember(savedCustomEndpoint) { mutableStateOf(savedCustomEndpoint) }
    var specialtyInput by remember(preferredSpecialty) { mutableStateOf(preferredSpecialty) }
    var severityInput by remember(preferredSeverity) { mutableStateOf(preferredSeverity) }
    var consultFeeInput by remember(savedConsultFee) { mutableStateOf(savedConsultFee.toInt().toString()) }
    var labCostInput by remember(savedLabCost) { mutableStateOf(savedLabCost.toInt().toString()) }
    var specCostInput by remember(savedSpecCost) { mutableStateOf(savedSpecCost.toInt().toString()) }

    val providers = listOf("Google", "OpenAI", "Anthropic", "Nvidia")
    val providerModels = mapOf(
        "Google" to listOf(
            "gemini-3.5-flash",
            "gemini-3.1-pro-preview",
            "gemini-3.1-flash-lite-preview"
        ),
        "OpenAI" to listOf("gpt-4o", "gpt-4o-mini", "gpt-4"),
        "Anthropic" to listOf("claude-3-5-sonnet", "claude-3-haiku"),
        "Nvidia" to listOf(
            "meta/llama-3.3-70b-instruct",
            "meta/llama-3.1-405b-instruct",
            "meta/llama-3.1-70b-instruct",
            "meta/llama-3.1-8b-instruct",
            "meta/llama-3.2-3b-instruct",
            "meta/llama-3.2-1b-instruct",
            "mistralai/mistral-large-2-instruct",
            "mistralai/mistral-nemo-12b-instruct",
            "mistralai/mixtral-8x22b-instruct-v0.1",
            "mistralai/mixtral-8x7b-instruct-v0.1",
            "mistralai/mistral-7b-instruct-v0.3",
            "google/gemma-2-27b-it",
            "google/gemma-2-9b-it",
            "google/gemma-7b-it",
            "google/gemma-2b-it",
            "microsoft/phi-3-medium-4k-instruct",
            "microsoft/phi-3-small-128k-instruct",
            "microsoft/phi-3-mini-128k-instruct",
            "nvidia/llama-3.1-nemotron-70b-instruct",
            "nvidia/nemotron-4-340b-instruct",
            "qwen/qwen2.5-72b-instruct",
            "qwen/qwen2.5-coder-32b-instruct",
            "qwen/qwen2.5-7b-instruct",
            "databricks/dbrx-instruct",
            "snowflake/arctic",
            "upstage/solar-10.7b-instruct",
            "01-ai/yi-large",
            "deepseek-ai/deepseek-r1",
            "deepseek-ai/deepseek-v4-flash",
            "deepseek-ai/deepseek-v4-pro",
            "z-ai/glm-5.1",
            "qwen/qwen3.5-122b-a10b",
            "qwen/qwen3.5-397b-a17b",
            "qwen/qwen3-next-80b-a3b-instruct",
            "stepfun-ai/step-3.7-flash",
            "nvidia/cosmos3-nano-reasoner",
            "nvidia/nemotron-3-ultra-550b-a55b",
            "moonshotai/kimi-k2.6",
            "mistralai/mistral-medium-3.5-128b"
        )
    )

    // Automatically correct model input if its provider mapping is missing
    LaunchedEffect(providerInput) {
        val models = providerModels[providerInput] ?: emptyList()
        if (modelInput !in models && models.isNotEmpty()) {
            modelInput = models.first()
        }
    }

    var isTestingConnection by remember { mutableStateOf(false) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var testIsSuccess by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.infoEvents.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Clinical Engine Settings", fontWeight = FontWeight.SemiBold) },
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = "To run clinical scenarios, choose an AI model, input your API key, and tap save. For Google Gemini, leaving the API Key blank defaults to the platform's sandbox credentials.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- AI Provider Dropdown ---
            var providerExpanded by remember { mutableStateOf(false) }
            Text(
                text = "Select Provider",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            ExposedDropdownMenuBox(
                expanded = providerExpanded,
                onExpandedChange = { providerExpanded = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = providerInput,
                    onValueChange = {},
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .testTag("provider_dropdown")
                )
                ExposedDropdownMenu(
                    expanded = providerExpanded,
                    onDismissRequest = { providerExpanded = false }
                ) {
                    providers.forEach { selection ->
                        DropdownMenuItem(
                            text = { Text(text = selection) },
                            onClick = {
                                providerInput = selection
                                providerExpanded = false
                            },
                            modifier = Modifier.testTag("provider_item_$selection")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Model Selection Dropdown ---
            var modelExpanded by remember { mutableStateOf(false) }
            Text(
                text = "Select AI Model",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            ExposedDropdownMenuBox(
                expanded = modelExpanded,
                onExpandedChange = { modelExpanded = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = modelInput,
                    onValueChange = {},
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .testTag("model_dropdown")
                )
                ExposedDropdownMenu(
                    expanded = modelExpanded,
                    onDismissRequest = { modelExpanded = false }
                ) {
                    (providerModels[providerInput] ?: emptyList()).forEach { selection ->
                        DropdownMenuItem(
                            text = { Text(text = selection) },
                            onClick = {
                                modelInput = selection
                                modelExpanded = false
                            },
                            modifier = Modifier.testTag("model_item_$selection")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Secure API Key TextField ---
            Text(
                text = "Secure API Key",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                placeholder = { Text("AI Provider API Key") },
                visualTransformation = PasswordVisualTransformation(),
                leadingIcon = { Icon(imageVector = Icons.Default.Key, contentDescription = "API Key") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("api_key_field"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- Custom API Endpoint URL TextField ---
            Text(
                text = "Custom API Endpoint URL (Optional)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            OutlinedTextField(
                value = customEndpointInput,
                onValueChange = { customEndpointInput = it },
                placeholder = { Text("E.g. http://10.0.2.2:5000/v1") },
                leadingIcon = { Icon(imageVector = Icons.Default.Link, contentDescription = "Endpoint URL") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("custom_endpoint_field"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- Connections and Results Banner ---
            testResultText?.let { feedback ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (testIsSuccess) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (testIsSuccess) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = "Test status icon",
                            tint = if (testIsSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            text = feedback,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (testIsSuccess) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // --- Operation Row ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        isTestingConnection = true
                        testResultText = null
                        viewModel.testConnection(
                            testKey = apiKeyInput,
                            testProvider = providerInput,
                            testModel = modelInput,
                            testCustomEndpoint = customEndpointInput
                        ) { success, msg ->
                            isTestingConnection = false
                            testIsSuccess = success
                            testResultText = msg
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("test_connection_button"),
                    enabled = !isTestingConnection
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh icon")
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Test API")
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            viewModel.saveActiveKeys(
                                newKey = apiKeyInput,
                                newProvider = providerInput,
                                newModel = modelInput,
                                newCustomEndpoint = customEndpointInput
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("save_settings_button")
                ) {
                    Text("Save Config", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Curriculum training focus",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Define the clinical specialty focus and case difficulty level for customized training profiles generated by the Clinical Engine.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    var specExpanded by remember { mutableStateOf(false) }
                    val specialties = listOf(
                        "All", 
                        "Sandbox (AI Choice)", 
                        "Cardiology", 
                        "Pulmonology", 
                        "Pediatrics", 
                        "Gastroenterology", 
                        "Endocrinology", 
                        "Neurology", 
                        "Psychiatry", 
                        "Gynecology", 
                        "Dermatology", 
                        "ENT", 
                        "Musculoskeletal",
                        "Emergency Medicine (Locked 🔒)",
                        "Intensive Care (Locked 🔒)"
                    )
                    
                    val unlockedSpecialties = remember(doctorXp) {
                        specialties.map { spec ->
                            val isLocked = when(spec) {
                                "Emergency Medicine (Locked 🔒)" -> doctorXp < 4000
                                "Intensive Care (Locked 🔒)" -> doctorXp < 10000
                                else -> false
                            }
                            if (isLocked) spec else spec.replace(" (Locked 🔒)", "")
                        }
                    }
                    
                    Text(
                        text = "Preferred Specialty Focus",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ExposedDropdownMenuBox(
                            expanded = specExpanded,
                            onExpandedChange = { specExpanded = it },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        ) {
                            OutlinedTextField(
                                readOnly = true,
                                value = specialtyInput,
                                onValueChange = {},
                                label = { Text("Clinical Rotation") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = specExpanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier.fillMaxWidth().menuAnchor().testTag("preset_specialty_dropdown")
                            )
                            ExposedDropdownMenu(
                                expanded = specExpanded,
                                onDismissRequest = { specExpanded = false }
                            ) {
                                unlockedSpecialties.forEachIndexed { idx, spec ->
                                    val originalSpec = specialties[idx]
                                    val isLocked = originalSpec.contains("Locked") && (
                                        (originalSpec.contains("Emergency") && doctorXp < 4000) ||
                                        (originalSpec.contains("Intensive") && doctorXp < 10000)
                                    )
                                    
                                    DropdownMenuItem(
                                        text = { 
                                            Text(
                                                text = spec, 
                                                color = if (isLocked) Color.Gray else Color.Unspecified,
                                                fontWeight = if (isLocked) FontWeight.Normal else FontWeight.Bold
                                            ) 
                                        },
                                        onClick = {
                                            if (!isLocked) {
                                                specialtyInput = spec
                                                specExpanded = false
                                            }
                                        },
                                        modifier = Modifier.testTag("preset_specialty_item_$spec"),
                                        enabled = !isLocked
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    var sevExpanded by remember { mutableStateOf(false) }
                    val severities = listOf("All", "Sandbox (AI Choice)", "Routine", "Severe")

                    Text(
                        text = "Preferred Case Severity Level",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    ExposedDropdownMenuBox(
                        expanded = sevExpanded,
                        onExpandedChange = { sevExpanded = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = severityInput,
                            onValueChange = {},
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sevExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier.fillMaxWidth().menuAnchor().testTag("preset_severity_dropdown")
                        )
                        ExposedDropdownMenu(
                            expanded = sevExpanded,
                            onDismissRequest = { sevExpanded = false }
                        ) {
                            severities.forEach { sev ->
                                DropdownMenuItem(
                                    text = { Text(text = sev) },
                                    onClick = {
                                        severityInput = sev
                                        sevExpanded = false
                                    },
                                    modifier = Modifier.testTag("preset_severity_item_$sev")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.saveCurriculumPresets(specialtyInput, severityInput)
                            scope.launch {
                                snackbarHostState.showSnackbar("Curriculum focus set to: $specialtyInput specialty ($severityInput).")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth().testTag("save_curriculum_presets_button")
                    ) {
                        Text("Save Medical Curriculum Focus", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Financial & Pricing Setup",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Customize your clinic's service prices (ZAR)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = consultFeeInput,
                        onValueChange = { consultFeeInput = it },
                        label = { Text("Base Consultation Fee") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    
                    OutlinedTextField(
                        value = labCostInput,
                        onValueChange = { labCostInput = it },
                        label = { Text("Lab Investigations Overhead") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = specCostInput,
                        onValueChange = { specCostInput = it },
                        label = { Text("Specialist Telephone Consult") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )

                    Button(
                        onClick = {
                            val cFee = consultFeeInput.toDoubleOrNull() ?: 850.0
                            val lCost = labCostInput.toDoubleOrNull() ?: 150.0
                            val sCost = specCostInput.toDoubleOrNull() ?: 800.0
                            viewModel.savePricing(cFee, lCost, sCost)
                            scope.launch {
                                snackbarHostState.showSnackbar("Pricing configuration updated successfully.")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Fee Structure", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Export & Reports",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Download General Ledger and Error Logs",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Exports your full clinic financial ledger to your device Downloads folder as a Markdown file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val context = androidx.compose.ui.platform.LocalContext.current
                    Button(
                        onClick = {
                            viewModel.exportLedgerAndErrors(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.Download, 
                            contentDescription = "Export"
                        )
                        Spacer(modifier = Modifier.padding(start = 8.dp))
                        Text("Export Ledger (.md)", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.exportLedgerAndErrorsPdf(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.Download, 
                            contentDescription = "Export PDF"
                        )
                        Spacer(modifier = Modifier.padding(start = 8.dp))
                        Text("Export Full Report (.pdf)", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Danger Zone",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Reset Private Practice",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "This action will permanently delete all clinical case logs, revenue, patient session history, reset daily practice statistics, and start a fresh medical simulation. This cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    var showResetConfirm by remember { mutableStateOf(false) }
                    if (!showResetConfirm) {
                        Button(
                            onClick = { showResetConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth().testTag("purge_history_init")
                        ) {
                            Text("Purge Statistics & History", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Column {
                            Text(
                                text = "Are you absolutely sure you want to reset?",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showResetConfirm = false },
                                    modifier = Modifier.weight(1f).testTag("purge_history_cancel")
                                ) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = {
                                        viewModel.clearAllSimulationData()
                                        showResetConfirm = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Clinic dataset successfully purged.")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.weight(1f).testTag("purge_history_confirm")
                                ) {
                                    Text("Yes, Reset", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
