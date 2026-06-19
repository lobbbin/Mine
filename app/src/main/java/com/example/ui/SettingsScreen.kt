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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val savedCurrencySymbol by viewModel.currencySymbol.collectAsState()
    val savedCurrencyCode by viewModel.currencyCode.collectAsState()
    val savedUiFontScale by viewModel.uiFontScale.collectAsState()
    val clinicBalance by viewModel.clinicBalance.collectAsState()
    val isBasicMode by viewModel.isBasicMode.collectAsState()
    val fetchedG4FModels by viewModel.g4fModels.collectAsState()
    val savedRotatorKeys by viewModel.rotatorKeys.collectAsState()
    val savedEnabledModels by viewModel.rotatorEnabledModels.collectAsState()
    val savedCustomModels by viewModel.rotatorCustomModels.collectAsState()

    val rotatorDefaultPoolModels = remember {
        mapOf(
            "Groq" to listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant", "mixtral-8x7b-32768"),
            "OpenRouter" to listOf("openrouter/auto", "google/gemini-2.5-flash:free", "meta-llama/llama-3.3-70b-instruct:free", "deepseek/deepseek-r1:free"),
            "Cerebras" to listOf("llama-3.3-70b", "llama-3.1-8b", "llama3.1-8b"),
            "Google AI Studio" to listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-1.5-flash"),
            "Nvidia NIM" to listOf("meta/llama-3.3-70b-instruct", "nvidia/llama-3.1-nemotron-70b-instruct", "meta/llama-3.1-8b-instruct", "nvidia/nemotron-4-340b-instruct", "nvidia/nemotron-mini-4b-instruct", "nvidia/nemotron-3-nano-30b-a3b:free", "nvidia/nemotron-3-super-120b-a12b:free", "nvidia/nemotron-nano-12b-v2-vl:free", "nvidia/nemotron-nano-9b-v2:free"),
            "OpenCode (Zen)" to listOf("deepseek-v4-pro", "glm-5.1", "kimi-k2.6", "qwen3.5-plus", "qwen3.6-plus", "qwen3.6-plus-free", "grok-build-0.1", "minimax-m2.7", "minimax-m3-free", "mimo-v2.5-free", "nemotron-3-ultra-free", "north-mini-code-free"),
            "Kilocode" to listOf("arcee-trinity-large-preview", "minimax-m2.5", "mistralai/devstral-2512", "grok-code-fast-1", "gemini-3-flash-preview", "claude-haiku-4.5"),
            "SambaNova" to listOf("Meta-Llama-3.3-70B-Instruct", "Meta-Llama-3.1-8B-Instruct", "Meta-Llama-3.1-405B-Instruct"),
            "Together AI" to listOf("meta-llama/Llama-3.3-70B-Instruct-Turbo", "meta-llama/Meta-Llama-3.1-8B-Instruct-Turbo", "mistralai/Mixtral-8x7B-Instruct-v0.1"),
            "Fireworks AI" to listOf("accounts/fireworks/models/llama-v3p3-70b-instruct", "accounts/fireworks/models/llama-v3p1-8b-instruct", "accounts/fireworks/models/mixtral-8x7b-instruct"),
            "Mistral AI" to listOf("open-mistral-7b", "mistral-small-latest", "mistral-large-latest"),
            "Cohere" to listOf("command-r-plus", "command-r", "command-light"),
            "DeepSeek" to listOf("deepseek-chat", "deepseek-coder", "deepseek-reasoner"),
            "DeepInfra" to listOf("meta-llama/Llama-3.3-70B-Instruct", "meta-llama/Meta-Llama-3.1-8B-Instruct", "mistralai/Mixtral-8x22B-Instruct-v0.1"),
            "Novita AI" to listOf("meta-llama/llama-3.3-70b-instruct", "meta-llama/llama-3.1-8b-instruct", "mistralai/mistral-7b-instruct"),
            "Hyperbolic" to listOf("meta-llama/Llama-3.3-70B-Instruct", "meta-llama/Meta-Llama-3.1-8B-Instruct", "deepseek-ai/DeepSeek-V3")
        )
    }

    val rotatorAllPoolModels = remember(rotatorDefaultPoolModels, savedCustomModels) {
        val mergedModels = mutableMapOf<String, List<String>>()
        rotatorDefaultPoolModels.forEach { (provider, defaultModels) ->
            val customModelsForProvider = savedCustomModels[provider] ?: emptyList()
            mergedModels[provider] = (defaultModels + customModelsForProvider).distinct()
        }
        mergedModels
    }

    var selectedRotatorModels by remember(savedEnabledModels) {
        mutableStateOf(
            if (savedEnabledModels.isEmpty()) {
                rotatorAllPoolModels.values.flatten().toSet()
            } else {
                savedEnabledModels
            }
        )
    }

    var expandedProviders by remember { mutableStateOf(emptySet<String>()) }
    
    var showAddCustomModelDialog by remember { mutableStateOf<String?>(null) }
    var newCustomModelName by remember { mutableStateOf("") }

    var groqKey by remember(savedRotatorKeys) { mutableStateOf(savedRotatorKeys["groq"] ?: "") }
    var openrouterKey by remember(savedRotatorKeys) { mutableStateOf(savedRotatorKeys["openrouter"] ?: "") }
    var cerebrasKey by remember(savedRotatorKeys) { mutableStateOf(savedRotatorKeys["cerebras"] ?: "") }
    var googleKey by remember(savedRotatorKeys) { mutableStateOf(savedRotatorKeys["google"] ?: "") }
    var nvidiaKey by remember(savedRotatorKeys) { mutableStateOf(savedRotatorKeys["nvidia"] ?: "") }
    var sambanovaKey by remember(savedRotatorKeys) { mutableStateOf(savedRotatorKeys["sambanova"] ?: "") }
    var togetherKey by remember(savedRotatorKeys) { mutableStateOf(savedRotatorKeys["together"] ?: "") }
    var fireworksKey by remember(savedRotatorKeys) { mutableStateOf(savedRotatorKeys["fireworks"] ?: "") }
    var mistralKey by remember(savedRotatorKeys) { mutableStateOf(savedRotatorKeys["mistral"] ?: "") }
    var cohereKey by remember(savedRotatorKeys) { mutableStateOf(savedRotatorKeys["cohere"] ?: "") }
    var deepseekKey by remember(savedRotatorKeys) { mutableStateOf(savedRotatorKeys["deepseek"] ?: "") }
    var deepinfraKey by remember(savedRotatorKeys) { mutableStateOf(savedRotatorKeys["deepinfra"] ?: "") }
    var novitaKey by remember(savedRotatorKeys) { mutableStateOf(savedRotatorKeys["novita"] ?: "") }
    var hyperbolicKey by remember(savedRotatorKeys) { mutableStateOf(savedRotatorKeys["hyperbolic"] ?: "") }
    var opencodeKey by remember(savedRotatorKeys) { mutableStateOf(savedRotatorKeys["opencode"] ?: "") }
    var kilocodeKey by remember(savedRotatorKeys) { mutableStateOf(savedRotatorKeys["kilocode"] ?: "") }

    var apiKeyInput by remember(savedApiKey) { mutableStateOf(savedApiKey ?: "") }
    var providerInput by remember(savedProvider) { mutableStateOf(savedProvider) }
    var customEndpointInput by remember(savedCustomEndpoint) { mutableStateOf(savedCustomEndpoint) }
    var specialtyInput by remember(preferredSpecialty) { mutableStateOf(preferredSpecialty) }
    var severityInput by remember(preferredSeverity) { mutableStateOf(preferredSeverity) }
    var consultFeeInput by remember(savedConsultFee) { mutableStateOf(savedConsultFee.toInt().toString()) }
    var labCostInput by remember(savedLabCost) { mutableStateOf(savedLabCost.toInt().toString()) }
    var specCostInput by remember(savedSpecCost) { mutableStateOf(savedSpecCost.toInt().toString()) }
    var currencySymbolInput by remember(savedCurrencySymbol) { mutableStateOf(savedCurrencySymbol) }
    var currencyCodeInput by remember(savedCurrencyCode) { mutableStateOf(savedCurrencyCode) }
    var uiFontScaleInput by remember(savedUiFontScale) { mutableStateOf(savedUiFontScale) }
    var clinicBalanceInput by remember(clinicBalance) { mutableStateOf(clinicBalance.toInt().toString()) }

    val providers = listOf("Google", "OpenAI", "Anthropic", "OpenRouter", "Cerebras", "Nvidia", "Ollama", "vLLM", "G4F (OpenAI-compatible)", "OpenCode (Zen)", "Kilocode", "Custom (OpenAI-compatible)", "Auto-Swapping Rotator")
    val providerModels = mapOf(
        "Auto-Swapping Rotator" to listOf("Automatic-Failover-14x"),
        "Google" to listOf(
            "gemini-2.5-flash",
            "gemini-2.5-pro",
            "gemini-1.5-flash",
            "custom"
        ),
        "OpenAI" to listOf("gpt-4o", "gpt-4o-mini", "o3-mini", "gpt-4", "custom"),
        "Anthropic" to listOf("claude-3-5-sonnet", "claude-3-5-haiku", "claude-3-opus", "custom"),
        "OpenRouter" to listOf(
            "openrouter/auto",
            "google/gemini-2.5-flash:free",
            "meta-llama/llama-3.3-70b-instruct:free",
            "deepseek/deepseek-r1:free",
            "google/gemini-2.5-pro",
            "meta-llama/llama-3.3-70b-instruct",
            "deepseek/deepseek-r1",
            "custom"
        ),
        "OpenCode (Zen)" to listOf(
            "deepseek-v4-pro",
            "glm-5.1",
            "kimi-k2.6",
            "qwen3.5-plus",
            "qwen3.6-plus",
            "qwen3.6-plus-free",
            "grok-build-0.1",
            "minimax-m2.7",
            "minimax-m3-free",
            "mimo-v2.5-free",
            "nemotron-3-ultra-free",
            "north-mini-code-free",
            "custom"
        ),
        "Kilocode" to listOf(
            "arcee-trinity-large-preview",
            "minimax-m2.5",
            "mistralai/devstral-2512",
            "grok-code-fast-1",
            "gemini-3-flash-preview",
            "claude-haiku-4.5",
            "custom"
        ),
        "Cerebras" to listOf(
            "llama-3.3-70b",
            "llama-3.1-8b",
            "llama3.1-8b",
            "custom"
        ),
        "Nvidia" to listOf(
            "meta/llama-3.3-70b-instruct",
            "nvidia/llama-3.1-nemotron-70b-instruct",
            "meta/llama-3.1-8b-instruct",
            "nvidia/nemotron-4-340b-instruct",
            "nvidia/nemotron-mini-4b-instruct",
            "nvidia/nemotron-3-nano-30b-a3b:free",
            "nvidia/nemotron-3-super-120b-a12b:free",
            "nvidia/nemotron-nano-12b-v2-vl:free",
            "nvidia/nemotron-nano-9b-v2:free",
            "custom"
        ),
        "Ollama" to listOf(
            "llama3.3",
            "llama3.1",
            "llama3",
            "gemma2",
            "mistral",
            "phi3",
            "codegemma",
            "custom"
        ),
        "vLLM" to listOf(
            "meta-llama/Llama-3.3-70B-Instruct",
            "meta-llama/Meta-Llama-3-8B-Instruct",
            "Qwen/Qwen2.5-7B-Instruct",
            "Qwen/Qwen2.5-Coder-32B-Instruct",
            "custom"
        ),
        "G4F (OpenAI-compatible)" to if (fetchedG4FModels.isNotEmpty()) fetchedG4FModels else listOf(
            "MiniMax/MiniMax-M1-80k",
            "MiniMaxAI/MiniMax-M2.5",
            "Qwen/Qwen3-Coder-30B-A3B-Instruct",
            "Qwen/Qwen3-VL-8B-Instruct",
            "Qwen/Qwen3.5-122B-A10B",
            "Qwen/Qwen3.5-397B-A17B",
            "WhiteRabbitNeo/Llama-3.1-WhiteRabbitNeo-2-8B:latest",
            "XiaomiMiMo/MiMo-V2.5",
            "XiaomiMiMo/MiMo-V2.5-Pro",
            "claude",
            "claude-3-7-ch-exp",
            "claude-4-ch-exp",
            "claude-fast",
            "claude-opus-4-6-thinking",
            "cogito-2.1:671b",
            "deepseek",
            "deepseek-ai/DeepSeek-V3.2",
            "deepseek-ai/DeepSeek-V3.2-Exp",
            "deepseek-ai/DeepSeek-V4-Flash",
            "deepseek-ai/DeepSeek-V4-Pro",
            "deepseek-ai/deepseek-v3.2",
            "deepseek-ai/deepseek-v4-flash",
            "deepseek-ai/deepseek-v4-pro",
            "deepseek-pro",
            "deepseek-r1:14b",
            "deepseek-r1:latest",
            "deepseek-v3.2",
            "deepseek-v4-flash",
            "deepseek-v4-flash-thinking",
            "deepseek-v4-pro",
            "devstral-2:123b",
            "devstral-small-2:24b",
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-3-flash",
            "gemini-3-flash-preview",
            "gemini-3-pro-preview",
            "gemini-3.1-flash-lite",
            "gemini-3.1-flash-lite-preview",
            "gemini-3.1-flash-lite:search",
            "gemini-3.1-pro-preview",
            "gemini-3.1-pro-preview:search",
            "gemini-3.5-flash",
            "gemini-flash-lite-latest",
            "gemma",
            "gemma-4-26b-a4b-it",
            "gemma-4-31b-it",
            "gemma3:12b",
            "gemma3:27b",
            "gemma3:4b",
            "gemma4:31b",
            "gemma4:31b-cloud",
            "glm",
            "glm-4.6",
            "glm-4.7",
            "glm-4.7-flash",
            "glm-5",
            "glm-5-thinking",
            "glm-5.1",
            "glm-5.1-thinking",
            "glm-5v-turbo",
            "google/diffusiongemma-26b-a4b-it",
            "google/gemma-2-2b-it",
            "google/gemma-3n-e4b-it",
            "google/gemma-4-26B-A4B-it",
            "google/gemma-4-31B-it",
            "google/gemma-4-31b-it:free",
            "gpt-4o-mini",
            "gpt-5-2-thinking",
            "gpt-5.5",
            "gpt-audio",
            "gpt-oss-120b",
            "gpt-oss-20b",
            "gpt-oss:120b",
            "gpt-oss:120b-cloud",
            "gpt-oss:20b",
            "grok",
            "grok-4.1-fast",
            "grok-4.1-mini:free",
            "grok-4.20-0309-non-reasoning",
            "grok-4.20-fast",
            "grok-4.3",
            "grok-large",
            "groq/compound",
            "huihui_ai/gemma-4-abliterated:12b",
            "huihui_ai/gemma-4-abliterated:26b",
            "huihui_ai/glm-4.7-flash-abliterated:latest",
            "huihui_ai/gpt-oss-abliterated:latest",
            "huihui_ai/qwen3.5-abliterated:27b",
            "huihui_ai/qwen3.6-abliterated:27b",
            "kimi",
            "kimi-k2.5",
            "kimi-k2.6",
            "kimi-k2.6-thinking",
            "liquid/lfm-2.5-1.2b-instruct:free",
            "llama",
            "llama-3.1-8b-instant",
            "llama-3.3-70b-versatile",
            "llama-scout",
            "llama3.2:3b",
            "llama3.2:latest",
            "llama3:8b",
            "llama3:latest",
            "meta-llama/llama-3.2-3b-instruct:free",
            "meta-llama/llama-4-scout-17b-16e-instruct",
            "meta-llama/llama-3.1-70b-instruct",
            "meta-llama/llama-3.1-8b-instruct",
            "meta-llama/llama-3.2-11b-vision-instruct",
            "meta-llama/llama-3.2-1b-instruct",
            "meta-llama/llama-3.2-3b-instruct",
            "meta-llama/llama-3.2-90b-vision-instruct",
            "meta-llama/llama-3.3-70b-instruct",
            "meta-llama/llama-guard-4-12b",
            "microsoft/phi-4-multimodal-instruct",
            "midijourney",
            "minimax",
            "minimax-m2",
            "minimax-m2.1",
            "minimax-m2.5",
            "minimax-m2.7",
            "minimaxai/minimax-m2.7",
            "minimaxai/minimax-m3",
            "ministral-3:14b",
            "ministral-3:3b",
            "ministral-3:8b",
            "mistral",
            "mistral-large",
            "mistral-small",
            "mistralai/ministral-14b-instruct-2512",
            "mistralai/mistral-large-3-675b-instruct-2512",
            "mistralai/mistral-small-4-119b-2603",
            "models/gemini-2.5-flash",
            "models/gemini-2.5-flash-lite",
            "models/gemini-3-flash-preview",
            "models/gemini-3.1-flash-lite",
            "models/gemini-3.1-flash-lite-preview",
            "models/gemini-3.5-flash",
            "models/gemini-flash-latest",
            "models/gemini-flash-lite-latest",
            "models/gemma-4-26b-a4b-it",
            "models/gemma-4-31b-it",
            "moonshotai/Kimi-K2.5",
            "moonshotai/Kimi-K2.6",
            "moonshotai/kimi-k2.5",
            "moonshotai/kimi-k2.6",
            "nemotron-3-nano:30b",
            "nemotron-3-super",
            "nova",
            "nova-fast",
            "nvidia/Nemotron-3-Nano-Omni-30B-A3B-Reasoning",
            "nvidia/llama-3.1-nemotron-nano-8b-v1",
            "nvidia/llama-3.3-nemotron-super-49b-v1",
            "nvidia/llama-3.3-nemotron-super-49b-v1.5",
            "nvidia/nemotron-3-nano-30b-a3b",
            "nvidia/nemotron-3-nano-30b-a3b:free",
            "nvidia/nemotron-3-super",
            "nvidia/nemotron-3-super-120b-a12b",
            "nvidia/nemotron-3-super-120b-a12b:free",
            "nvidia/nemotron-3-ultra-550b-a55b",
            "nvidia/nemotron-mini-4b-instruct",
            "nvidia/nemotron-nano-12b-v2-vl:free",
            "nvidia/nemotron-nano-9b-v2:free",
            "o4-mini-high",
            "openai",
            "openai-audio",
            "openai-fast",
            "openai-large",
            "openai/gpt-oss-120b",
            "openai/gpt-oss-120b:free",
            "openai/gpt-oss-20b",
            "openai/gpt-oss-20b:free",
            "openai/gpt-oss-safeguard-20b",
            "openrouter/free",
            "perplexity-fast",
            "perplexity-reasoning",
            "polly",
            "poolside/laguna-xs.2:free",
            "qwen-coder",
            "qwen-large",
            "qwen-safety",
            "qwen/qwen3-32b",
            "qwen/qwen3-next-80b-a3b-instruct",
            "qwen/qwen3.5-122b-a10b",
            "qwen/qwen3.5-397b-a17b",
            "qwen/qwen3.6-27b",
            "qwen/qwen3.6-35B-A3B",
            "qwen2.5-coder:7b",
            "qwen2.5:3b",
            "qwen2.5:7b",
            "qwen3-coder-next",
            "qwen3-next-80b-a3b-instruct",
            "qwen3-vl:235b-instruct",
            "qwen3.5:397b",
            "qwen3.5:4b",
            "qwen3.6-max-preview",
            "qwen3.6-plus",
            "qwen3.6:35b",
            "qwen3.7-max",
            "qwen3:8b",
            "rnj-1:8b",
            "smollm2:135m",
            "step-3.5-flash:free",
            "stepfun-ai/Step-3.5-Flash",
            "stepfun-ai/step-3.7-flash",
            "stockmark/stockmark-2-100b-instruct",
            "turbo",
            "wrn-33b:latest",
            "xiaomimimo/mimo-V2.5",
            "xiaomimimo/mimo-V2.5-Pro",
            "z-ai/glm-5.1",
            "z-ai/glm5",
            "zai-glm-4.7",
            "zai-org/GLM-4.7-Flash",
            "zai-org/GLM-5",
            "zai-org/GLM-5.1",
            "custom"
        ),
        "Custom (OpenAI-compatible)" to listOf(
            "custom"
        )
    )

    val initialModelIsCustom = remember(savedModel, savedProvider) {
        val models = providerModels[savedProvider] ?: emptyList()
        savedModel.isNotBlank() && savedModel !in models && savedModel != "custom"
    }
    var modelInput by remember(savedModel, savedProvider) { 
        mutableStateOf(if (initialModelIsCustom) "custom" else savedModel) 
    }
    var customModelName by remember(savedModel, savedProvider) {
        mutableStateOf(if (initialModelIsCustom) savedModel else "")
    }

    // Automatically correct model input if its provider mapping is missing
    LaunchedEffect(providerInput, fetchedG4FModels) {
        val models = providerModels[providerInput] ?: emptyList()
        if (modelInput !in models && models.isNotEmpty() && modelInput != "custom") {
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

            if (providerInput == "G4F (OpenAI-compatible)" && fetchedG4FModels.isNotEmpty()) {
                val uriHandler = LocalUriHandler.current
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        text = "Loaded ${fetchedG4FModels.size} live models from g4f-working repo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedButton(
                        onClick = { uriHandler.openUri("https://github.com/xtekky/gpt4free") },
                        modifier = Modifier.padding(top = 4.dp).testTag("visit_g4f_repo_button"),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Visit gpt4free GitHub", fontSize = 10.sp)
                    }
                }
            }

            if (modelInput == "custom") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Custom Model Identifier",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                OutlinedTextField(
                    value = customModelName,
                    onValueChange = { customModelName = it },
                    placeholder = { Text("E.g. llama3:latest, deepseek-r1, or custom-model") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("custom_model_field"),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (providerInput == "Auto-Swapping Rotator") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "🔄 Auto-Swapping Key Rotator Pool (16 APIs)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Fill keys for the providers you have accounts with. Unfilled/blank keys will be skipped during auto-swapping failovers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        val keyInputs = listOf(
                            Triple("Groq", groqKey, { v: String -> groqKey = v }),
                            Triple("OpenRouter", openrouterKey, { v: String -> openrouterKey = v }),
                            Triple("Cerebras", cerebrasKey, { v: String -> cerebrasKey = v }),
                            Triple("Google AI Studio", googleKey, { v: String -> googleKey = v }),
                            Triple("Nvidia NIM", nvidiaKey, { v: String -> nvidiaKey = v }),
                            Triple("SambaNova", sambanovaKey, { v: String -> sambanovaKey = v }),
                            Triple("Together AI", togetherKey, { v: String -> togetherKey = v }),
                            Triple("Fireworks AI", fireworksKey, { v: String -> fireworksKey = v }),
                            Triple("Mistral AI", mistralKey, { v: String -> mistralKey = v }),
                            Triple("Cohere", cohereKey, { v: String -> cohereKey = v }),
                            Triple("DeepSeek", deepseekKey, { v: String -> deepseekKey = v }),
                            Triple("DeepInfra", deepinfraKey, { v: String -> deepinfraKey = v }),
                            Triple("Novita AI", novitaKey, { v: String -> novitaKey = v }),
                            Triple("Hyperbolic", hyperbolicKey, { v: String -> hyperbolicKey = v }),
                            Triple("OpenCode (Zen)", opencodeKey, { v: String -> opencodeKey = v }),
                            Triple("Kilocode", kilocodeKey, { v: String -> kilocodeKey = v })
                        )
                        
                        keyInputs.forEach { (label, value, onValChange) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1.3f)
                                )
                                OutlinedTextField(
                                    value = value,
                                    onValueChange = onValChange,
                                    placeholder = { Text("key or empty...") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier.weight(2.7f),
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        // --- Models Failover Pool Accordion Checklist ---
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎯 Models Failover Pool",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = {
                                        selectedRotatorModels = rotatorAllPoolModels.values.flatten().toSet()
                                    }
                                ) {
                                    Text("All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                TextButton(
                                    onClick = {
                                        selectedRotatorModels = emptySet()
                                    }
                                ) {
                                    Text("None", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Text(
                            text = "Choose individual models to enable. Active endpoints will rotate automatically fallback.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        rotatorAllPoolModels.forEach { (providerName, models) ->
                            val selectedCount = models.count { selectedRotatorModels.contains(it) }
                            val isExpanded = expandedProviders.contains(providerName)
                            
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                expandedProviders = if (isExpanded) {
                                                    expandedProviders - providerName
                                                } else {
                                                    expandedProviders + providerName
                                                }
                                            }
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = providerName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "$selectedCount of ${models.size} models active",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (selectedCount == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Toggle Section",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    
                                    if (isExpanded) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            models.forEach { modelName ->
                                                val isChecked = selectedRotatorModels.contains(modelName)
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            selectedRotatorModels = if (isChecked) {
                                                                selectedRotatorModels - modelName
                                                            } else {
                                                                selectedRotatorModels + modelName
                                                            }
                                                        }
                                                        .padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Checkbox(
                                                        checked = isChecked,
                                                        onCheckedChange = { checked ->
                                                            selectedRotatorModels = if (checked == true) {
                                                                selectedRotatorModels + modelName
                                                            } else {
                                                                selectedRotatorModels - modelName
                                                            }
                                                        }
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = modelName,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                            
                                            // Add custom model button
                                            TextButton(
                                                onClick = { showAddCustomModelDialog = providerName },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add custom model")
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Add Custom Model")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
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
            }

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
                        val modelToTest = if (modelInput == "custom") {
                            if (customModelName.isNotBlank()) customModelName else "custom"
                        } else {
                            modelInput
                        }
                        viewModel.testConnection(
                            testKey = apiKeyInput,
                            testProvider = providerInput,
                            testModel = modelToTest,
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
                            val modelToSave = if (modelInput == "custom") {
                                if (customModelName.isNotBlank()) customModelName else "custom"
                            } else {
                                modelInput
                            }
                            viewModel.saveActiveKeys(
                                newKey = apiKeyInput,
                                newProvider = providerInput,
                                newModel = modelToSave,
                                newCustomEndpoint = customEndpointInput
                            )
                            if (providerInput == "Auto-Swapping Rotator") {
                                viewModel.saveRotatorKeys(
                                    mapOf(
                                        "groq" to groqKey,
                                        "openrouter" to openrouterKey,
                                        "cerebras" to cerebrasKey,
                                        "google" to googleKey,
                                        "nvidia" to nvidiaKey,
                                        "sambanova" to sambanovaKey,
                                        "together" to togetherKey,
                                        "fireworks" to fireworksKey,
                                        "mistral" to mistralKey,
                                        "cohere" to cohereKey,
                                        "deepseek" to deepseekKey,
                                        "deepinfra" to deepinfraKey,
                                        "novita" to novitaKey,
                                        "hyperbolic" to hyperbolicKey,
                                        "opencode" to opencodeKey,
                                        "kilocode" to kilocodeKey
                                    )
                                )
                                viewModel.saveRotatorEnabledModels(selectedRotatorModels)
                            }
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
                    val isBasicMode by viewModel.isBasicMode.collectAsState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🏡 Basic GP Practice Mode",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Restricts the simulator to General Practice outpatients and disables complex specialist statuses.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        androidx.compose.material3.Switch(
                            checked = isBasicMode,
                            onCheckedChange = { viewModel.saveModeSelection(isBasic = it) },
                            modifier = Modifier.testTag("basic_mode_settings_switch")
                        )
                    }
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 16.dp))

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
                        text = "Global Practice Settings & Currency",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = currencySymbolInput,
                            onValueChange = { currencySymbolInput = it },
                            label = { Text("Currency Symbol") },
                            placeholder = { Text("$") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = currencyCodeInput,
                            onValueChange = { currencyCodeInput = it },
                            label = { Text("Currency Code") },
                            placeholder = { Text("USD") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = clinicBalanceInput,
                        onValueChange = { clinicBalanceInput = it },
                        label = { Text("Change Money Amount") },
                        placeholder = { Text("50000") },
                        modifier = Modifier.fillMaxWidth().testTag("change_money_input"),
                        singleLine = true,
                        leadingIcon = { Text(currencySymbolInput, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "UI Character Width Scale (Compact size): ${"%.2f".format(uiFontScaleInput)}x",
                        style = MaterialTheme.typography.labelMedium
                    )
                    androidx.compose.material3.Slider(
                        value = uiFontScaleInput,
                        onValueChange = { uiFontScaleInput = it },
                        valueRange = 0.5f..1.5f,
                        steps = 20
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val sym = currencySymbolInput.trim().ifBlank { "$" }
                            val cod = currencyCodeInput.trim().ifBlank { "USD" }.uppercase()
                            val parsedBal = clinicBalanceInput.toDoubleOrNull() ?: clinicBalance
                            viewModel.saveCurrency(sym, cod)
                            viewModel.setClinicBalance(parsedBal)
                            viewModel.saveUiFontScale(uiFontScaleInput)
                            scope.launch {
                                snackbarHostState.showSnackbar("Practice parameters and currency updated successfully.")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Practice & Wallet Configuration", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Customize your clinic's service prices (${savedCurrencySymbol})",
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
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
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
    
    // Custom Model Dialog
    if (showAddCustomModelDialog != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { 
                showAddCustomModelDialog = null 
                newCustomModelName = ""
            },
            title = { Text("Add Custom Model to ${showAddCustomModelDialog}") },
            text = {
                OutlinedTextField(
                    value = newCustomModelName,
                    onValueChange = { newCustomModelName = it },
                    label = { Text("Model ID (e.g., custom-model-1)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val providerName = showAddCustomModelDialog
                    if (providerName != null && newCustomModelName.isNotBlank()) {
                        val currentCustom = savedCustomModels.toMutableMap()
                        val listForProvider = currentCustom[providerName]?.toMutableList() ?: mutableListOf()
                        if (!listForProvider.contains(newCustomModelName.trim())) {
                            listForProvider.add(newCustomModelName.trim())
                            currentCustom[providerName] = listForProvider
                            viewModel.saveRotatorCustomModels(currentCustom)
                        }
                    }
                    showAddCustomModelDialog = null
                    newCustomModelName = ""
                }) {
                    Text("Add Model")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddCustomModelDialog = null 
                    newCustomModelName = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
