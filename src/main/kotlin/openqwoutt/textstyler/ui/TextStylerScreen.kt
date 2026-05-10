package openqwoutt.miniapp.textstyler.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import openqwoutt.miniapp.textstyler.data.prompts.PromptCategory
import openqwoutt.miniapp.textstyler.data.prompts.PromptTemplate
import openqwoutt.miniapp.textstyler.domain.ModeGroup
import openqwoutt.miniapp.textstyler.domain.StyleMode
import openqwoutt.miniapp.textstyler.presentation.TextStylerAction
import openqwoutt.miniapp.textstyler.presentation.TextStylerState
import openqwoutt.miniapp.textstyler.ui.theme.AppTheme

private const val ANIMATION_DURATION = 250

@Composable
fun TextStylerScreen(
    state: TextStylerState,
    onAction: (TextStylerAction) -> Unit,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    Crossfade(
        targetState = state.showSettings,
        animationSpec = tween(ANIMATION_DURATION, easing = FastOutSlowInEasing),
        label = "settings_crossfade",
        modifier = modifier
    ) { showSettings ->
        if (showSettings) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                SettingsScreen(
                    settings = state.settings,
                    availableModels = state.availableModels,
                    isLoadingModels = state.isLoadingModels,
                    onSave = { onAction(TextStylerAction.SaveSettings(it)) },
                    onBack = { onAction(TextStylerAction.ToggleSettings) }
                )
            }
        } else {
            Crossfade(
                targetState = state.showTemplates,
                animationSpec = tween(ANIMATION_DURATION, easing = FastOutSlowInEasing),
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
                    Crossfade(
                        targetState = state.showHistory,
                        animationSpec = tween(ANIMATION_DURATION, easing = FastOutSlowInEasing),
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
    val showResult = state.result != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Top content: header + result area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Header(
                onNavigateBack = onNavigateBack,
                onShowHistory = { onAction(TextStylerAction.ShowHistory) },
                onShowSettings = { onAction(TextStylerAction.ToggleSettings) }
            )

            // Result / empty area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                ResultArea(state = state, clipboard = clipboard, onAction = onAction)
            }
        }

        if (showResult) {
            // FAB for new request
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        onAction(TextStylerAction.ClearResult)
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )
                    },
                    text = { Text("New request") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            }
        } else {
            // Bottom section: mode picker + input card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                // Mode picker cards
                AnimatedVisibility(
                    visible = state.showModePicker,
                    enter = expandVertically(
                        animationSpec = tween(ANIMATION_DURATION, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(ANIMATION_DURATION)),
                    exit = shrinkVertically(
                        animationSpec = tween(ANIMATION_DURATION, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(ANIMATION_DURATION))
                ) {
                    ModePicker(
                        selectedMode = state.selectedMode,
                        onSelectMode = { mode ->
                            onAction(TextStylerAction.SelectMode(mode))
                            onAction(TextStylerAction.HideModePicker)
                        }
                    )
                }

                // Style sub-modes strip
                AnimatedVisibility(
                    visible = (state.selectedMode == StyleMode.STYLE || state.selectedMode.group == ModeGroup.STYLE) && !state.showModePicker,
                    enter = expandVertically(
                        animationSpec = tween(ANIMATION_DURATION, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(ANIMATION_DURATION)),
                    exit = shrinkVertically(
                        animationSpec = tween(ANIMATION_DURATION, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(ANIMATION_DURATION))
                ) {
                    StyleSubModesStrip(
                        selectedMode = state.selectedMode,
                        onSelectMode = { onAction(TextStylerAction.SelectMode(it)) }
                    )
                }

                InputCard(
                    state = state,
                    onAction = onAction,
                    clipboard = clipboard
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun MainContentPreview_Default() {
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    AppTheme(darkTheme = true) {
        MainContent(
            state = TextStylerState(),
            onAction = {},
            onNavigateBack = {},
            clipboard = clipboard
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun MainContentPreview_WithResult() {
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    AppTheme(darkTheme = true) {
        MainContent(
            state = TextStylerState(
                inputText = "The quick brown fox jumps over the lazy dog.",
                result = "A swift auburn canine leaps across a lethargic hound.",
                selectedMode = StyleMode.FORMAL
            ),
            onAction = {},
            onNavigateBack = {},
            clipboard = clipboard
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun MainContentPreview_Loading() {
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    AppTheme(darkTheme = true) {
        MainContent(
            state = TextStylerState(
                inputText = "Processing this text...",
                isLoading = true
            ),
            onAction = {},
            onNavigateBack = {},
            clipboard = clipboard
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun MainContentPreview_Error() {
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    AppTheme(darkTheme = true) {
        MainContent(
            state = TextStylerState(
                inputText = "Some text",
                error = "Network error. Please check your connection and try again."
            ),
            onAction = {},
            onNavigateBack = {},
            clipboard = clipboard
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun MainContentPreview_ModePickerOpen() {
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    AppTheme(darkTheme = true) {
        MainContent(
            state = TextStylerState(
                inputText = "Type or paste text...",
                showModePicker = true
            ),
            onAction = {},
            onNavigateBack = {},
            clipboard = clipboard
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun MainContentPreview_LongText() {
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val longInput = buildString {
        repeat(8) {
            append("The quick brown fox jumps over the lazy dog. ")
            append("Artificial intelligence is transforming the way we write, code, and communicate. ")
            append("Jetpack Compose makes building modern Android UIs faster and more intuitive. ")
        }
    }
    val longResult = buildString {
        repeat(6) {
            append("A swift auburn canine leaps across a lethargic hound. ")
            append("Machine learning algorithms continuously improve natural language understanding. ")
            append("Declarative UI frameworks reduce boilerplate and increase developer productivity. ")
        }
    }
    AppTheme(darkTheme = true) {
        MainContent(
            state = TextStylerState(
                inputText = longInput,
                result = longResult,
                selectedMode = StyleMode.ANALYZE,
                isTextTruncated = true
            ),
            onAction = {},
            onNavigateBack = {},
            clipboard = clipboard
        )
    }
}

@Composable
private fun Header(
    onNavigateBack: () -> Unit,
    onShowHistory: () -> Unit,
    onShowSettings: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Hamburger menu
        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("History", color = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    onClick = { showMenu = false; onShowHistory() }
                )
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Settings", color = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    onClick = { showMenu = false; onShowSettings() }
                )
            }
        }

        // Title
        Text(
            text = "AI Editor",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        // AI badge
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AI",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ModePicker(
    selectedMode: StyleMode,
    onSelectMode: (StyleMode) -> Unit
) {
    val modes = listOf(
        StyleMode.STYLE to "Rewrite in a style",
        StyleMode.FIX to "Grammar & clarity",
        StyleMode.SUMMARIZE to "Condense text",
        StyleMode.ANALYZE to "Deep analysis"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        modes.chunked(2).forEach { rowModes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowModes.forEach { (mode, subtitle) ->
                    val isSelected = selectedMode == mode ||
                            (mode == StyleMode.STYLE && selectedMode.group == ModeGroup.STYLE)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelectMode(mode) }
                            .padding(16.dp)
                    ) {
                        Text(
                            text = mode.icon,
                            fontSize = 22.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = mode.displayName,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
                if (rowModes.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StyleSubModesStrip(
    selectedMode: StyleMode,
    onSelectMode: (StyleMode) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 10.dp)) {
        Text(
            text = "STYLE",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StyleMode.entries.filter { it.group == ModeGroup.STYLE }.forEach { mode ->
                val selected = selectedMode == mode
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .size(width = 56.dp, height = 64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            width = if (selected) 1.5.dp else 0.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectMode(mode) }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = mode.icon,
                        fontSize = 18.sp,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = mode.shortName,
                        fontSize = 10.sp,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun InputCard(
    state: TextStylerState,
    onAction: (TextStylerAction) -> Unit,
    clipboard: ClipboardManager
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Mode pill
        val pillLabel =
            if (state.selectedMode == StyleMode.STYLE || state.selectedMode.group == ModeGroup.STYLE) "Style" else state.selectedMode.shortName
        val pillIcon =
            if (state.selectedMode == StyleMode.STYLE || state.selectedMode.group == ModeGroup.STYLE) "✎" else state.selectedMode.icon

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = pillIcon, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = pillLabel,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Text input
        BasicTextField(
            value = state.inputText,
            onValueChange = { onAction(TextStylerAction.SetInputText(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp, max = 140.dp),
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                lineHeight = 22.sp
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            decorationBox = { innerTextField ->
                Box {
                    if (state.inputText.isEmpty()) {
                        Text(
                            text = "Type or paste text...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }
            }
        )

        // Error
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
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(error, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = { onAction(TextStylerAction.ClearError) },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("OK", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom button row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Toggle mode picker (+ / ×)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .clickable { onAction(TextStylerAction.ToggleModePicker) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.showModePicker) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = if (state.showModePicker) "Close" else "Choose mode",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Templates button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .clickable { onAction(TextStylerAction.ShowTemplates) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Templates",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Send button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (state.isLoading || state.inputText.isBlank()) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                    .clickable(enabled = !state.isLoading && state.inputText.isNotBlank()) {
                        keyboardController?.hide()
                        onAction(TextStylerAction.ProcessText)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Process",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultArea(
    state: TextStylerState,
    clipboard: ClipboardManager,
    onAction: (TextStylerAction) -> Unit
) {
    if (state.result != null || state.isStreaming) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.selectedMode.displayName,
                    color = MaterialTheme.colorScheme.primary,
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
                            imageVector = Icons.Default.CopyAll,
                            contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { onAction(TextStylerAction.ClearResult) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (state.isTextTruncated) {
                Text(
                    "Input was trimmed to 3000 characters.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(
                text = if (state.isStreaming) state.resultTokens else state.result ?: "",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                lineHeight = 22.sp
            )
        }
    } else if (state.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(top = 8.dp, bottom = 8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary
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
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
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
private fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surface
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
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "AI Templates",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (selectedTemplate != null) {
                TextButton(onClick = onClear) {
                    Text(
                        text = "Clear",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
                onSearch(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Search templates...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = {
                        searchText = ""
                        onSearch("")
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.surface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

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
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White
                )
            )
            categories.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat.id,
                    onClick = { onSelectCategory(cat.id) },
                    label = { Text(cat.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

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
                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp)
                        .then(Modifier.clickable { onSelect(template) })
                ) {
                    Column {
                        Text(
                            text = template.name,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (template.description.isNotBlank()) {
                            Text(
                                text = template.description,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                        if (template.tags.isNotEmpty()) {
                            Text(
                                text = template.tags.joinToString(", "),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
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

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun TextStylerScreenPreview_Default() {
    AppTheme(darkTheme = true) {
        TextStylerScreen(
            state = TextStylerState(),
            onAction = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun TextStylerScreenPreview_WithResult() {
    AppTheme(darkTheme = true) {
        TextStylerScreen(
            state = TextStylerState(
                inputText = "The quick brown fox jumps over the lazy dog.",
                result = "A swift auburn canine leaps across a lethargic hound.",
                selectedMode = StyleMode.FORMAL
            ),
            onAction = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun TextStylerScreenPreview_Loading() {
    AppTheme(darkTheme = true) {
        TextStylerScreen(
            state = TextStylerState(
                inputText = "Processing this text...",
                isLoading = true
            ),
            onAction = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun TextStylerScreenPreview_Error() {
    AppTheme(darkTheme = true) {
        TextStylerScreen(
            state = TextStylerState(
                inputText = "Some text",
                error = "Network error. Please check your connection and try again."
            ),
            onAction = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun TextStylerScreenPreview_SettingsOpen() {
    AppTheme(darkTheme = true) {
        TextStylerScreen(
            state = TextStylerState(showSettings = true),
            onAction = {},
            onNavigateBack = {}
        )
    }
}
