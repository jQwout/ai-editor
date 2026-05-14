package openqwoutt.miniapp.textstyler.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import openqwoutt.textstyler.data.settings.AppSettings

// ============================================================================
// Color System - Light Theme
// ============================================================================
private val LightAppBg = Color(0xFFF9FAFB)
private val LightCardBg = Color(0xFFFFFFFF)
private val LightCardBorder = Color(0xFFE5E7EB)
private val LightPrimary = Color(0xFF6D28D9)
private val LightTextPrimary = Color(0xFF1F2937)
private val LightTextSecondary = Color(0xFF6B7280)
private val LightIconBg = Color(0xFFF3F4F6)
private val LightActiveBadge = Color(0xFFEDE9FE)

// ============================================================================
// Color System - Dark Theme
// ============================================================================
private val DarkAppBg = Color(0xFF0D0D11)
private val DarkCardBg = Color(0xFF1B1B20)
private val DarkCardBorder = Color(0xFF2D2D35)
private val DarkPrimary = Color(0xFF8D78F0)
private val DarkTextPrimary = Color(0xFFF4F1F8)
private val DarkTextSecondary = Color(0xFFB9B5C3)
private val DarkIconBg = Color(0xFF23232A)
private val DarkActiveBadge = Color(0xFF3D3A54)

// ============================================================================
// Theme Mode
// ============================================================================
enum class ThemeMode {
    LIGHT, DARK, AUTO
}

// ============================================================================
// Settings State
// ============================================================================
data class SettingsState(
    val aiModel: String = "meta/llama-3.1-70b-instruct",
    val apiKey: String = "",
    val language: String = "English",
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val customBackendEnabled: Boolean = false,
    val customBackendUrl: String = "",
    val autoPaste: Boolean = true,
    val autoCopyResult: Boolean = true,
    val soundEffects: Boolean = false,
    val hapticFeedback: Boolean = true,
    val useStreaming: Boolean = true
)

// ============================================================================
// Hardcoded NVIDIA NIM Models
// ============================================================================
object NvidiaModels {
    val models = listOf(
        "meta/llama-3.1-70b-instruct",
        "qwen/qwen3-coder-480b-a35b-instruct"
    )
    
    val displayNames = mapOf(
        "meta/llama-3.1-70b-instruct" to "Llama 3.1 70B Instruct",
        "qwen/qwen3-coder-480b-a35b-instruct" to "Qwen3 Coder 480B"
    )
    
    fun getDisplayName(modelId: String): String = displayNames[modelId] ?: modelId
}

// ============================================================================
// Menu Item Data
// ============================================================================
sealed class SettingsMenuItem {
    abstract val title: String
    abstract val subtitle: String?
    abstract val icon: ImageVector
    abstract val isActive: Boolean
    
    data class AiModel(
        override val title: String = "AI Model",
        override val subtitle: String? = "NVIDIA NIM",
        override val icon: ImageVector = Icons.Default.Code,
        override val isActive: Boolean = true
    ) : SettingsMenuItem()
    
    data class Language(
        override val title: String = "Language",
        override val subtitle: String? = null,
        override val icon: ImageVector = Icons.Default.Language,
        override val isActive: Boolean = true
    ) : SettingsMenuItem()
    
    data class Theme(
        override val title: String = "Theme",
        override val subtitle: String? = null,
        override val icon: ImageVector = Icons.Default.LightMode,
        override val isActive: Boolean = true
    ) : SettingsMenuItem()
    
    data class CustomBackend(
        override val title: String = "Custom Backend",
        override val subtitle: String? = null,
        override val icon: ImageVector = Icons.Default.Code,
        override val isActive: Boolean = false
    ) : SettingsMenuItem()
}

// ============================================================================
// Composable Entry Point
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSave: (AppSettings) -> Unit,
    onBack: () -> Unit,
    isDarkTheme: Boolean = false
) {
    val appBg = if (isDarkTheme) DarkAppBg else LightAppBg
    val cardBg = if (isDarkTheme) DarkCardBg else LightCardBg
    val cardBorder = if (isDarkTheme) DarkCardBorder else LightCardBorder
    val primary = if (isDarkTheme) DarkPrimary else LightPrimary
    val textPrimary = if (isDarkTheme) DarkTextPrimary else LightTextPrimary
    val textSecondary = if (isDarkTheme) DarkTextSecondary else LightTextSecondary
    val iconBg = if (isDarkTheme) DarkIconBg else LightIconBg
    val activeBadge = if (isDarkTheme) DarkActiveBadge else LightActiveBadge
    
    var state by remember {
        mutableStateOf(
            SettingsState(
                aiModel = settings.effectiveModel(),
                apiKey = settings.apiKey,
                themeMode = ThemeMode.AUTO
            )
        )
    }
    
    var showAiModelScreen by remember { mutableStateOf(false) }
    var showLanguageScreen by remember { mutableStateOf(false) }
    
    if (showAiModelScreen) {
        AiModelSelectionScreen(
            selectedModel = state.aiModel,
            apiKey = state.apiKey,
            onModelSelected = { model, key ->
                state = state.copy(aiModel = model, apiKey = key)
            },
            onBack = { showAiModelScreen = false },
            isDarkTheme = isDarkTheme
        )
    }
    
    if (showLanguageScreen) {
        LanguageBottomSheet(
            currentLanguage = state.language,
            onLanguageSelected = { language ->
                state = state.copy(language = language)
                showLanguageScreen = false
            },
            onDismiss = { showLanguageScreen = false },
            isDarkTheme = isDarkTheme
        )
    } else {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.navigationBars)
                .imePadding(),
            color = appBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Settings",
                        color = textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Save",
                        color = primary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable {
                            onSave(
                                settings.copy(
                                    aiModel = state.aiModel,
                                    apiKey = state.apiKey,
                                    useBackend = state.customBackendEnabled,
                                    backendUrl = state.customBackendUrl,
                                    autoPaste = state.autoPaste,
                                    autoCopyResult = state.autoCopyResult,
                                    soundEffects = state.soundEffects,
                                    hapticFeedback = state.hapticFeedback,
                                    useStreaming = state.useStreaming
                                )
                            )
                            onBack()
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Build menu items with custom backend ordering
                val menuItems = buildMenuItems(state)
                
                menuItems.forEach { item ->
                    when (item) {
                        is SettingsMenuItem.AiModel -> {
                            SettingsCard(
                                icon = item.icon,
                                title = item.title,
                                subtitle = NvidiaModels.getDisplayName(state.aiModel),
                                iconBg = iconBg,
                                cardBg = cardBg,
                                cardBorder = cardBorder,
                                primary = primary,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                activeBadge = activeBadge,
                                isSelected = true,
                                onClick = { showAiModelScreen = true },
                                trailing = {
                                    Icon(
                                        Icons.Default.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = textSecondary
                                    )
                                }
                            )
                        }
                        is SettingsMenuItem.Language -> {
                            SettingsCard(
                                icon = item.icon,
                                title = item.title,
                                subtitle = state.language,
                                iconBg = iconBg,
                                cardBg = cardBg,
                                cardBorder = cardBorder,
                                primary = primary,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                activeBadge = activeBadge,
                                isSelected = false,
                                onClick = { showLanguageScreen = true },
                                trailing = {
                                    Icon(
                                        Icons.Default.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = textSecondary
                                    )
                                }
                            )
                        }
                        is SettingsMenuItem.Theme -> {
                            ThemeCard(
                                currentMode = state.themeMode,
                                onModeChange = { mode ->
                                    state = state.copy(themeMode = mode)
                                },
                                cardBg = cardBg,
                                cardBorder = cardBorder,
                                primary = primary,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                iconBg = iconBg
                            )
                        }
                        is SettingsMenuItem.CustomBackend -> {
                            CustomBackendCard(
                                enabled = state.customBackendEnabled,
                                url = state.customBackendUrl,
                                onEnabledChange = { enabled ->
                                    state = state.copy(customBackendEnabled = enabled)
                                },
                                onUrlChange = { url ->
                                    state = state.copy(customBackendUrl = url)
                                },
                                cardBg = cardBg,
                                cardBorder = cardBorder,
                                primary = primary,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                iconBg = iconBg,
                                icon = item.icon
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ============================================================================
// Menu Items Builder (Custom Backend ordering)
// ============================================================================
private fun buildMenuItems(state: SettingsState): List<SettingsMenuItem> {
    val items = mutableListOf<SettingsMenuItem>()
    
    // AI Model always first
    items.add(SettingsMenuItem.AiModel())
    
    // Language always second
    items.add(SettingsMenuItem.Language())
    
    // Theme always third
    items.add(SettingsMenuItem.Theme())
    
    // Custom Backend: first if enabled, last if disabled
    val customBackend = SettingsMenuItem.CustomBackend(isActive = state.customBackendEnabled)
    if (state.customBackendEnabled) {
        items.add(0, customBackend) // Add at beginning
    } else {
        items.add(customBackend) // Add at end
    }
    
    return items
}

// ============================================================================
// Settings Card Component
// ============================================================================
@Composable
private fun SettingsCard(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    iconBg: Color,
    cardBg: Color,
    cardBorder: Color,
    primary: Color,
    textPrimary: Color,
    textSecondary: Color,
    activeBadge: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = cardBg,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Title and Subtitle
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = if (isSelected) primary else textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(activeBadge)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Selected",
                                color = primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = textSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
            
            trailing()
        }
    }
}

// ============================================================================
// Theme Card with Segmented Control
// ============================================================================
@Composable
private fun ThemeCard(
    currentMode: ThemeMode,
    onModeChange: (ThemeMode) -> Unit,
    cardBg: Color,
    cardBorder: Color,
    primary: Color,
    textPrimary: Color,
    textSecondary: Color,
    iconBg: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp)),
        color = cardBg,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LightMode,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Theme",
                    color = textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Segmented Control
            SegmentedControl(
                options = listOf("Light", "Dark", "Auto"),
                selectedIndex = when (currentMode) {
                    ThemeMode.LIGHT -> 0
                    ThemeMode.DARK -> 1
                    ThemeMode.AUTO -> 2
                },
                onSelectionChange = { index ->
                    val mode = when (index) {
                        0 -> ThemeMode.LIGHT
                        1 -> ThemeMode.DARK
                        else -> ThemeMode.AUTO
                    }
                    onModeChange(mode)
                },
                cardBg = cardBg,
                cardBorder = cardBorder,
                primary = primary,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )
        }
    }
}

// ============================================================================
// Segmented Control
// ============================================================================
@Composable
private fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit,
    cardBg: Color,
    cardBorder: Color,
    primary: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBorder)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) cardBg else Color.Transparent)
                    .clickable { onSelectionChange(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    color = if (isSelected) primary else textSecondary,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}

// ============================================================================
// Custom Backend Card
// ============================================================================
@Composable
private fun CustomBackendCard(
    enabled: Boolean,
    url: String,
    onEnabledChange: (Boolean) -> Unit,
    onUrlChange: (String) -> Unit,
    cardBg: Color,
    cardBorder: Color,
    primary: Color,
    textPrimary: Color,
    textSecondary: Color,
    iconBg: Color,
    icon: ImageVector
) {
    var passwordVisible by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp)),
        color = cardBg,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Custom Backend",
                    color = textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = primary,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = cardBorder
                    )
                )
            }
            
            AnimatedVisibility(
                visible = enabled,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = url,
                        onValueChange = onUrlChange,
                        label = { Text("Backend URL") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = textPrimary),
                        singleLine = true,
                        visualTransformation = if (!passwordVisible && url.contains("api") || url.contains("key")) 
                            PasswordVisualTransformation() else VisualTransformation.None,
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide URL" else "Show URL",
                                    tint = textSecondary
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

// ============================================================================
// AI Model Selection Screen
// ============================================================================
@Composable
fun AiModelSelectionScreen(
    selectedModel: String,
    apiKey: String,
    onModelSelected: (model: String, apiKey: String) -> Unit,
    onBack: () -> Unit,
    isDarkTheme: Boolean = false
) {
    val appBg = if (isDarkTheme) DarkAppBg else LightAppBg
    val cardBg = if (isDarkTheme) DarkCardBg else LightCardBg
    val cardBorder = if (isDarkTheme) DarkCardBorder else LightCardBorder
    val primary = if (isDarkTheme) DarkPrimary else LightPrimary
    val textPrimary = if (isDarkTheme) DarkTextPrimary else LightTextPrimary
    val textSecondary = if (isDarkTheme) DarkTextSecondary else LightTextSecondary
    val activeBadge = if (isDarkTheme) DarkActiveBadge else LightActiveBadge
    
    var currentModel by remember { mutableStateOf(selectedModel) }
    var currentApiKey by remember { mutableStateOf(apiKey) }
    var expanded by remember { mutableStateOf(false) }
    var apiKeyVisible by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.navigationBars)
            .imePadding(),
        color = appBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Select AI Model",
                    color = textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Provider Card (NVIDIA NIM only)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, cardBorder, RoundedCornerShape(16.dp)),
                color = cardBg,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Provider:",
                        color = textSecondary,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "NVIDIA NIM",
                        color = textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(activeBadge)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Only",
                            color = primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // API Key Input
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, cardBorder, RoundedCornerShape(16.dp)),
                color = cardBg,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "API Key",
                        color = textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = currentApiKey,
                        onValueChange = { currentApiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = textPrimary),
                        singleLine = true,
                        visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                Icon(
                                    imageVector = if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (apiKeyVisible) "Hide API Key" else "Show API Key",
                                    tint = textSecondary
                                )
                            }
                        },
                        placeholder = { Text("Enter your NVIDIA API key", color = textSecondary) }
                    )
                }
            }
            
            // Model Selection
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, cardBorder, RoundedCornerShape(16.dp)),
                color = cardBg,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Select Model",
                        color = textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Dropdown
                    Box {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                                .clickable { expanded = true },
                            color = cardBg,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = NvidiaModels.getDisplayName(currentModel),
                                    color = if (currentModel.isNotEmpty()) primary else textSecondary,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = if (expanded) Icons.Default.KeyboardArrowRight else Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = textSecondary,
                                    modifier = Modifier
                                        .size(24.dp)
                                )
                            }
                        }
                        
                        androidx.compose.material3.DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(cardBg)
                        ) {
                            NvidiaModels.models.forEach { model ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = NvidiaModels.getDisplayName(model),
                                                color = textPrimary,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (model == currentModel) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = primary
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        currentModel = model
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Save Button
            Button(
                onClick = {
                    onModelSelected(currentModel, currentApiKey)
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primary),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Save",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ============================================================================
// Language Bottom Sheet
// ============================================================================
@Composable
private fun LanguageBottomSheet(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean
) {
    val appBg = if (isDarkTheme) DarkAppBg else LightAppBg
    val cardBg = if (isDarkTheme) DarkCardBg else LightCardBg
    val primary = if (isDarkTheme) DarkPrimary else LightPrimary
    val textPrimary = if (isDarkTheme) DarkTextPrimary else LightTextPrimary
    val textSecondary = if (isDarkTheme) DarkTextSecondary else LightTextSecondary
    val iconBg = if (isDarkTheme) DarkIconBg else LightIconBg
    val activeBadge = if (isDarkTheme) DarkActiveBadge else LightActiveBadge
    val cardBorder = if (isDarkTheme) DarkCardBorder else LightCardBorder
    
    val languages = listOf(
        LanguageOption("English", "US"),
        LanguageOption("Russian", "RU")
    )
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = cardBg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = textSecondary) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Select Language",
                color = textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            languages.forEach { lang ->
                LanguageOptionCard(
                    language = lang,
                    isSelected = currentLanguage == lang.name,
                    primary = primary,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    activeBadge = activeBadge,
                    onClick = { onLanguageSelected(lang.name) }
                )
            }
        }
    }
}

data class LanguageOption(
    val name: String,
    val code: String
)

@Composable
private fun LanguageOptionCard(
    language: LanguageOption,
    isSelected: Boolean,
    primary: Color,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    activeBadge: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) activeBadge else cardBg)
            .border(1.dp, if (isSelected) primary else cardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Language code badge
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) primary else LightIconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = language.code,
                        color = if (isSelected) Color.White else textSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = language.name,
                    color = textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Selection indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) primary else Color.Transparent)
                    .border(
                        width = if (isSelected) 0.dp else 1.5.dp,
                        color = if (isSelected) primary else textSecondary,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private val LightIconBg = Color(0xFFF3F4F6)