package openqwoutt.miniapp.textstyler.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateColor
import androidx.compose.animation.core.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import openqwoutt.miniapp.textstyler.domain.ModeGroup
import openqwoutt.miniapp.textstyler.domain.StyleMode
import openqwoutt.miniapp.textstyler.presentation.CloseBehavior
import openqwoutt.miniapp.textstyler.presentation.TextStylerAction
import openqwoutt.miniapp.textstyler.presentation.TextStylerState
import openqwoutt.miniapp.textstyler.data.prompts.PromptCategory
import openqwoutt.miniapp.textstyler.data.prompts.PromptTemplate
import openqwoutt.textstyler.data.settings.AppSettings

// Telegram-style dark palette
private val Bg = Color(0xFF0F0F0F)
private val Surface = Color(0xFF1A1A1A)
private val Accent = Color(0xFF8774E1)
private val AccentSoft = Color(0xFF8774E1).copy(alpha = 0.15f)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8E8E93)
private val Divider = Color(0xFF2C2C2E)
private val ErrorBg = Color(0xFF3A2228)
private val ErrorText = Color(0xFFFFC4CF)
private val ErrorColor = Color(0xFFF44336)
private val SuccessColor = Color(0xFF4CAF50)

// Animation durations
private const val TAB_ANIMATION_DURATION = 200
private const val STRIP_ANIMATION_DURATION = 250
private const val RESULT_ANIMATION_DURATION = 300
private const val SETTINGS_ANIMATION_DURATION = 300

@Composable
fun TextStylerScreen(
    state: TextStylerState,
    onAction: (TextStylerAction) -> Unit,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    // Settings modal with crossfade animation
    Crossfade(
        targetState = state.showSettings,
        animationSpec = tween(SETTINGS_ANIMATION_DURATION, easing = FastOutSlowInEasing),
        label = "settings_crossfade",
        modifier = modifier
    ) { showSettings ->
        if (showSettings) {
            // Settings backdrop with fade
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                SettingsScreen(
                    settings = state.settings,
                    onSave = { onAction(TextStylerAction.SaveSettings(it)) },
                    onBack = { onAction(TextStylerAction.ToggleSettings) }
                )
            }
        } else {
            // Templates modal
            Crossfade(
                targetState = state.showTemplates,
                animationSpec = tween(SETTINGS_ANIMATION_DURATION, easing = FastOutSlowInEasing),
                label = "templates_crossfade"
            ) { showTemplates ->
                if (showTemplates) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        TemplatesSheet(
                            templates = state.availableTemplates,
                            categories = state.availableCategories,
                            selectedCategory = state.selectedCategory,
                            selectedTemplate = state.selectedTemplate,
                            onSelect = { template ->
                                onAction(TextStylerAction.SelectTemplate(template))
                                onAction(TextStylerAction.HideTemplates)
                            },
                            onSelectCategory = { category ->
                                onAction(TextStylerAction.SelectCategory(category))
                            },
                            onSearch = { query ->
                                onAction(TextStylerAction.SearchTemplates(query))
                            },
                            onClear = {
                                onAction(TextStylerAction.SelectTemplate(null))
                                onAction(TextStylerAction.HideTemplates)
                            },
                            onBack = { onAction(TextStylerAction.HideTemplates) }
                        )
                    }
                } else {
                    // History modal
                    Crossfade(
                        targetState = state.showHistory,
                        animationSpec = tween(SETTINGS_ANIMATION_DURATION, easing = FastOutSlowInEasing),
                        label = "history_crossfade"
                    ) { showHistory ->
                        if (showHistory) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f))
                            ) {
                                HistoryScreen(
                                    onBack = { onAction(TextStylerAction.HideHistory) }
                                )
                            }
                        } else {
                            // Main content
                            MainContent(
                                state = state,
                                onAction = onAction,
                                onNavigateBack = onNavigateBack,
                                clipboard = clipboard
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainContent(
    state: TextStylerState,
    onAction: (TextStylerAction) -> Unit,
    onNavigateBack: () -> Unit,
    clipboard: ClipboardManager
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Header(
                state = state,
                onNavigateBack = onNavigateBack,
                onOpenSettings = { onAction(TextStylerAction.ToggleSettings) },
                onOpenTemplates = { onAction(TextStylerAction.ShowTemplates) },
                onOpenHistory = { onAction(TextStylerAction.ShowHistory) }
            )

            MainModeTabs(state = state, onAction = onAction)

            // StyleStrip with expand/collapse animation (250ms)
            AnimatedVisibility(
                visible = state.selectedMode == StyleMode.STYLE,
                enter = expandVertically(
                    animationSpec = tween(STRIP_ANIMATION_DURATION, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(STRIP_ANIMATION_DURATION, easing = FastOutSlowInEasing)),
                exit = shrinkVertically(
                    animationSpec = tween(STRIP_ANIMATION_DURATION, easing = FastOutSlowInEasing)
                ) + fadeOut(tween(STRIP_ANIMATION_DURATION, easing = FastOutSlowInEasing))
            ) {
                StyleStrip(state = state, onAction = onAction)
            }

            // AnalyzeStrip with expand/collapse animation
            AnimatedVisibility(
                visible = state.selectedMode == StyleMode.ANALYZE_MAIN,
                enter = expandVertically(
                    animationSpec = tween(STRIP_ANIMATION_DURATION, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(STRIP_ANIMATION_DURATION, easing = FastOutSlowInEasing)),
                exit = shrinkVertically(
                    animationSpec = tween(STRIP_ANIMATION_DURATION, easing = FastOutSlowInEasing)
                ) + fadeOut(tween(STRIP_ANIMATION_DURATION, easing = FastOutSlowInEasing))
            ) {
                AnalyzeStrip(state = state, onAction = onAction)
            }

            EditorBlock(
                state = state,
                onAction = onAction,
                clipboard = clipboard
            )

            // Result block with fade + slide animation (300ms)
            AnimatedVisibility(
                visible = state.result != null || state.isLoading,
                enter = fadeIn(tween(RESULT_ANIMATION_DURATION, easing = FastOutSlowInEasing)) +
                        slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = tween(RESULT_ANIMATION_DURATION, easing = FastOutSlowInEasing)
                        ),
                exit = fadeOut(tween(RESULT_ANIMATION_DURATION / 2))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Divider)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ResultBlock(
                        state = state,
                        clipboard = clipboard,
                        onAction = onAction
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            BottomActions(
                state = state,
                onApply = { onAction(TextStylerAction.ProcessText) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Header(
    state: TextStylerState,
    onNavigateBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenHistory: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onNavigateBack, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = TextSecondary
            )
        }
        
        // Template indicator or title
        if (state.selectedTemplate != null) {
            TextButton(onClick = onOpenTemplates) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = state.selectedTemplate.name,
                    color = Accent,
                    fontSize = 14.sp
                )
            }
        } else {
            Text(
                text = "AI Editor",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        IconButton(onClick = onOpenSettings, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = TextSecondary
            )
        }
        
        IconButton(onClick = onOpenHistory, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Default.History,
                contentDescription = "History",
                tint = TextSecondary
            )
        }
    }
}

@Composable
private fun MainModeTabs(state: TextStylerState, onAction: (TextStylerAction) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        StyleMode.entries.filter { it.group == ModeGroup.MAIN }.forEach { mode ->
            val selected = state.selectedMode == mode

            // Animated background color (200ms)
            val animatedBg by animateColorAsState(
                targetValue = if (selected) Accent else Color.Transparent,
                animationSpec = tween(TAB_ANIMATION_DURATION, easing = FastOutSlowInEasing),
                label = "tab_bg"
            )

            // Animated text color (200ms)
            val animatedTextColor by animateColorAsState(
                targetValue = if (selected) Color.White else TextSecondary,
                animationSpec = tween(TAB_ANIMATION_DURATION, easing = FastOutSlowInEasing),
                label = "tab_text"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(animatedBg)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                TextButton(
                    onClick = { onAction(TextStylerAction.SelectMode(mode)) },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = mode.shortName,
                        color = animatedTextColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StyleStrip(state: TextStylerState, onAction: (TextStylerAction) -> Unit) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StyleMode.entries.filter { it.group == ModeGroup.STYLE }.forEach { mode ->
            val selected = state.selectedMode == mode
            val bg = if (selected) AccentSoft else Color.Transparent
            val textColor = if (selected) Accent else TextSecondary

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = mode.icon,
                    fontSize = 20.sp,
                    color = if (selected) Accent else TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                TextButton(
                    onClick = { onAction(TextStylerAction.SelectMode(mode)) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = mode.shortName,
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyzeStrip(state: TextStylerState, onAction: (TextStylerAction) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        StyleMode.entries.filter { it.group == ModeGroup.ANALYZE || it == StyleMode.ANALYZE_MAIN }.forEach { mode ->
            val selected = state.selectedMode == mode

            // Animated background color
            val animatedBg by animateColorAsState(
                targetValue = if (selected) Accent else Color.Transparent,
                animationSpec = tween(TAB_ANIMATION_DURATION, easing = FastOutSlowInEasing),
                label = "analyze_tab_bg"
            )

            val animatedTextColor by animateColorAsState(
                targetValue = if (selected) Color.White else TextSecondary,
                animationSpec = tween(TAB_ANIMATION_DURATION, easing = FastOutSlowInEasing),
                label = "analyze_tab_text"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(animatedBg)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                TextButton(
                    onClick = { onAction(TextStylerAction.SelectMode(mode)) },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = mode.shortName,
                        color = animatedTextColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorBlock(
    state: TextStylerState,
    onAction: (TextStylerAction) -> Unit,
    clipboard: ClipboardManager
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Surface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Original",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            IconButton(
                onClick = {
                    val clipData = clipboard.primaryClip
                    val text = clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
                    if (!text.isNullOrBlank()) {
                        onAction(TextStylerAction.SetInputText(text))
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.ContentPaste,
                    contentDescription = "Paste",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        BasicTextField(
            value = state.inputText,
            onValueChange = { onAction(TextStylerAction.SetInputText(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            textStyle = TextStyle(
                color = TextPrimary,
                fontSize = 17.sp,
                lineHeight = 24.sp
            ),
            cursorBrush = SolidColor(Accent),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            decorationBox = { innerTextField ->
                Box {
                    if (state.inputText.isEmpty()) {
                        Text(
                            text = "Type or paste text...",
                            color = TextSecondary.copy(alpha = 0.5f),
                            fontSize = 17.sp
                        )
                    }
                    innerTextField()
                }
            }
        )

        // Error display with fade animation
        AnimatedVisibility(
            visible = state.error != null,
            enter = fadeIn(tween(200)) + slideInVertically(),
            exit = fadeOut(tween(200))
        ) {
            state.error?.let { error ->
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ErrorBg)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(error, color = ErrorText, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = { onAction(TextStylerAction.ClearError) },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("OK", color = ErrorText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultBlock(
    state: TextStylerState,
    clipboard: ClipboardManager,
    onAction: (TextStylerAction) -> Unit
) {
    // Show loading or result
    if (state.result != null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Surface)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.selectedMode.displayName,
                    color = Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row {
                    IconButton(
                        onClick = {
                            clipboard.setPrimaryClip(ClipData.newPlainText("AI Editor Result", state.result))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.CopyAll,
                            contentDescription = "Copy",
                            tint = Accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { onAction(TextStylerAction.ClearResult) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (state.isTextTruncated) {
                Text(
                    "Input was trimmed to 3000 characters.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(
                text = state.result,
                color = TextPrimary,
                fontSize = 17.sp,
                lineHeight = 24.sp
            )
        }
    } else if (state.isLoading) {
        // Loading placeholder with fade
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Surface),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp,
                color = Accent
            )
        }
    }
}

@Composable
private fun BottomActions(state: TextStylerState, onApply: () -> Unit) {
    Button(
        onClick = onApply,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = !state.isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = Accent,
            disabledContainerColor = Accent.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
        } else {
            Text(
                "Apply",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSave: (AppSettings) -> Unit,
    onBack: () -> Unit
) {
    var currentSettings by remember { mutableStateOf(settings) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Surface)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Divider)
            )

            // Auto-paste setting
            SettingsToggleRow(
                title = "Auto-paste from clipboard",
                checked = currentSettings.autoPaste,
                onCheckedChange = { currentSettings = currentSettings.copy(autoPaste = it) }
            )

            // Auto-copy result setting
            SettingsToggleRow(
                title = "Auto-copy result",
                checked = currentSettings.autoCopyResult,
                onCheckedChange = { currentSettings = currentSettings.copy(autoCopyResult = it) }
            )

            // Sound effects setting
            SettingsToggleRow(
                title = "Sound effects",
                checked = currentSettings.soundEffects,
                onCheckedChange = { currentSettings = currentSettings.copy(soundEffects = it) }
            )

            // Haptic feedback setting
            SettingsToggleRow(
                title = "Haptic feedback",
                checked = currentSettings.hapticFeedback,
                onCheckedChange = { currentSettings = currentSettings.copy(hapticFeedback = it) }
            )

            // Save button
            Button(
                onClick = { onSave(currentSettings) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Save",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
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
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Bg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 16.sp
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Accent,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = Surface
            )
        )
    }
}

@Composable
fun TemplatesSheet(
    templates: List<PromptTemplate>,
    categories: List<PromptCategory>,
    selectedCategory: String?,
    selectedTemplate: PromptTemplate?,
    onSelect: (PromptTemplate) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = TextSecondary
                )
            }
            Text(
                text = "AI Templates",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (selectedTemplate != null) {
                TextButton(onClick = onClear) {
                    Text(
                        text = "Clear",
                        color = Accent,
                        fontSize = 14.sp
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        // Search bar
        OutlinedTextField(
            value = searchText,
            onValueChange = { 
                searchText = it
                onSearch(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Search templates...", color = TextSecondary) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
            },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { 
                        searchText = ""
                        onSearch("")
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Accent,
                unfocusedBorderColor = Surface,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // Category chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onSelectCategory(null) },
                label = { Text("All") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Accent,
                    selectedLabelColor = Color.White
                )
            )
            categories.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat.id,
                    onClick = { onSelectCategory(cat.id) },
                    label = { Text(cat.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Accent,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // Templates list
        val filteredTemplates = if (selectedCategory != null) {
            templates.filter { it.category == selectedCategory }
        } else {
            templates
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filteredTemplates.forEach { template ->
                val isSelected = selectedTemplate?.id == template.id
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) AccentSoft else Surface)
                        .padding(12.dp)
                        .then(Modifier.clickable { onSelect(template) })
                ) {
                    Column {
                        Text(
                            text = template.name,
                            color = if (isSelected) Accent else TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (template.description.isNotBlank()) {
                            Text(
                                text = template.description,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        if (template.tags.isNotEmpty()) {
                            Text(
                                text = template.tags.joinToString(", "),
                                color = Accent.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}