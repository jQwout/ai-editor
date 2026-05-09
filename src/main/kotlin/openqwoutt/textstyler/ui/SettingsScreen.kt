package openqwoutt.miniapp.textstyler.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import openqwoutt.textstyler.data.settings.AiProvider
import openqwoutt.textstyler.data.settings.AppSettings

private val AppBg = Color(0xFF0D0D11)
private val Panel = Color(0xFF1B1B20)
private val PanelLight = Color(0xFF23232A)
private val Accent = Color(0xFF8D78F0)
private val TextPrimary = Color(0xFFF4F1F8)
private val TextSecondary = Color(0xFFB9B5C3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    availableModels: List<String> = emptyList(),
    isLoadingModels: Boolean = false,
    onSave: (AppSettings) -> Unit,
    onBack: () -> Unit
) {
    val currentProvider = settings.toAiProvider()
    var selectedProvider by remember { mutableStateOf(currentProvider) }
    var apiKey by remember { mutableStateOf(settings.apiKey) }
    var aiModel by remember { mutableStateOf(settings.aiModel) }
    var backendUrl by remember { mutableStateOf(settings.backendUrl) }
    var providerExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }
    var isCustomModel by remember { mutableStateOf(false) }
    var autoPaste by remember { mutableStateOf(settings.autoPaste) }
    var autoCopyResult by remember { mutableStateOf(settings.autoCopyResult) }
    var soundEffects by remember { mutableStateOf(settings.soundEffects) }
    var hapticFeedback by remember { mutableStateOf(settings.hapticFeedback) }
    var saveHistory by remember { mutableStateOf(settings.saveHistory) }
    var useBackend by remember { mutableStateOf(settings.useBackend) }

    Surface(
        modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding(),
        color = AppBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Settings",
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                color = Panel,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "AI Provider",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    ExposedDropdownMenuBox(
                        expanded = providerExpanded,
                        onExpandedChange = { providerExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedProvider.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select provider") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                        )
                        ExposedDropdownMenu(
                            expanded = providerExpanded,
                            onDismissRequest = { providerExpanded = false },
                            modifier = Modifier.background(PanelLight)
                        ) {
                            AiProvider.entries.forEach { provider ->
                                DropdownMenuItem(
                                    text = { Text(provider.displayName, color = TextPrimary) },
                                    onClick = {
                                        selectedProvider = provider
                                        providerExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (selectedProvider.requiresApiKey) {
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text("${selectedProvider.displayName} API Key") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation()
                        )
                    }

                    if (selectedProvider == AiProvider.OPEN_ROUTER || selectedProvider == AiProvider.NVIDIA) {
                        Text(
                            text = "Model",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        val providerModels = when (selectedProvider) {
                            AiProvider.NVIDIA -> listOf(
                                "qwen/qwen3-coder-480b-a35b-instruct",
                                "meta/llama-3.1-70b-instruct"
                            )
                            else -> availableModels
                        }

                        SideEffect {
                            isCustomModel = aiModel.isNotBlank() && aiModel !in providerModels
                        }

                        ModelDropdown(
                            selectedModel = aiModel,
                            isCustom = isCustomModel,
                            onModelSelected = { selected, custom ->
                                aiModel = selected
                                isCustomModel = custom
                            },
                            expanded = modelExpanded,
                            onExpandedChange = { modelExpanded = it },
                            availableModels = providerModels,
                            isLoading = isLoadingModels,
                            defaultModel = selectedProvider.model
                        )
                    }

                    SettingsToggleRow(
                        title = "Use local backend",
                        checked = useBackend,
                        onCheckedChange = { useBackend = it }
                    )

                    if (useBackend) {
                        OutlinedTextField(
                            value = backendUrl,
                            onValueChange = { backendUrl = it },
                            label = { Text("Backend URL") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                            singleLine = true
                        )
                    }
                }
            }

            Surface(
                color = Panel,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Preferences",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    SettingsToggleRow(
                        title = "Auto-paste from clipboard",
                        checked = autoPaste,
                        onCheckedChange = { autoPaste = it }
                    )
                    SettingsToggleRow(
                        title = "Auto-copy result",
                        checked = autoCopyResult,
                        onCheckedChange = { autoCopyResult = it }
                    )
                    SettingsToggleRow(
                        title = "Sound effects",
                        checked = soundEffects,
                        onCheckedChange = { soundEffects = it }
                    )
                    SettingsToggleRow(
                        title = "Haptic feedback",
                        checked = hapticFeedback,
                        onCheckedChange = { hapticFeedback = it }
                    )
                    SettingsToggleRow(
                        title = "Save history",
                        checked = saveHistory,
                        onCheckedChange = { saveHistory = it }
                    )
                }
            }

            Button(
                onClick = {
                    onSave(
                        AppSettings(
                            backendUrl = backendUrl.trim(),
                            defaultMode = settings.defaultMode,
                            autoPaste = autoPaste,
                            autoCopyResult = autoCopyResult,
                            soundEffects = soundEffects,
                            hapticFeedback = hapticFeedback,
                            aiProvider = selectedProvider.name,
                            aiModel = aiModel.trim(),
                            apiKey = apiKey.trim(),
                            saveHistory = saveHistory,
                            useBackend = useBackend
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("Save Settings", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelDropdown(
    selectedModel: String,
    isCustom: Boolean,
    onModelSelected: (String, Boolean) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    availableModels: List<String>,
    isLoading: Boolean,
    defaultModel: String
) {
    var customText by remember { mutableStateOf(if (isCustom) selectedModel else "") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("Loading models...", color = TextSecondary)
            }
        } else if (availableModels.isEmpty()) {
            Text("Using default model: $defaultModel", color = TextSecondary)
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = onExpandedChange
        ) {
            OutlinedTextField(
                value = if (isCustom) "Custom" else (selectedModel.ifBlank { defaultModel }),
                onValueChange = {},
                readOnly = true,
                label = { Text("Select model") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier.background(PanelLight)
            ) {
                availableModels.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model, color = TextPrimary) },
                        onClick = {
                            onModelSelected(model, false)
                            onExpandedChange(false)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Custom...", color = TextPrimary) },
                    onClick = {
                        onModelSelected(customText, true)
                        onExpandedChange(false)
                    }
                )
            }
        }

        if (isCustom) {
            OutlinedTextField(
                value = customText,
                onValueChange = {
                    customText = it
                    onModelSelected(it, true)
                },
                label = { Text("Custom model ID") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                singleLine = true
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = TextSecondary)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
