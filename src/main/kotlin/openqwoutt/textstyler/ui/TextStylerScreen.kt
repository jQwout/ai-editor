package openqwoutt.miniapp.textstyler.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import openqwoutt.miniapp.textstyler.data.prompts.PromptCategory
import openqwoutt.miniapp.textstyler.data.prompts.PromptTemplate
import openqwoutt.miniapp.textstyler.domain.ModeGroup
import openqwoutt.miniapp.textstyler.domain.StyleMode
import openqwoutt.miniapp.textstyler.presentation.TextStylerAction
import openqwoutt.miniapp.textstyler.presentation.TextStylerState

// Dark palette — purple accent
private val Bg = Color(0xFF0F0F0F)
private val Surface = Color(0xFF1A1A1A)
private val CardBg = Color(0xFF1E1E1E)
private val Accent = Color(0xFF8774E1)
private val AccentSoft = Color(0xFF8774E1).copy(alpha = 0.15f)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8E8E93)
private val Divider = Color(0xFF2C2C2E)
private val ErrorBg = Color(0xFF3A2228)
private val ErrorText = Color(0xFFFFC4CF)

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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        // Top content: header + result area
        Column(
            modifier = Modifier
                .fillMaxSize()
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

        // Bottom floating section
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
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
                    tint = TextSecondary
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                containerColor = Surface,
                modifier = Modifier.background(Surface)
            ) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("History", color = TextPrimary)
                        }
                    },
                    onClick = { showMenu = false; onShowHistory() }
                )
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Settings", color = TextPrimary)
                        }
                    },
                    onClick = { showMenu = false; onShowSettings() }
                )
            }
        }

        // Title
        Text(
            text = "AI Editor",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        // AI badge
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Accent),
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
                            .background(if (isSelected) AccentSoft else CardBg)
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) Accent else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelectMode(mode) }
                            .padding(16.dp)
                    ) {
                        Text(
                            text = mode.icon,
                            fontSize = 22.sp,
                            color = if (isSelected) Accent else TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = mode.displayName,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            color = TextSecondary,
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
            color = TextSecondary,
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
                        .background(if (selected) Accent.copy(alpha = 0.12f) else CardBg)
                        .border(
                            width = if (selected) 1.5.dp else 0.dp,
                            color = if (selected) Accent else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectMode(mode) }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = mode.icon,
                        fontSize = 18.sp,
                        color = if (selected) Accent else TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = mode.shortName,
                        fontSize = 10.sp,
                        color = if (selected) Accent else TextSecondary
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Surface)
            .padding(16.dp)
    ) {
        // Mode pill
        val pillLabel = if (state.selectedMode == StyleMode.STYLE || state.selectedMode.group == ModeGroup.STYLE) "Style" else state.selectedMode.shortName
        val pillIcon = if (state.selectedMode == StyleMode.STYLE || state.selectedMode.group == ModeGroup.STYLE) "✎" else state.selectedMode.icon

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(AccentSoft)
                    .border(1.dp, Accent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = pillIcon, fontSize = 14.sp, color = Accent)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = pillLabel,
                        color = Accent,
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
                color = TextPrimary,
                fontSize = 16.sp,
                lineHeight = 22.sp
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
                            color = TextSecondary.copy(alpha = 0.6f),
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
                        .background(ErrorBg)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(error, color = ErrorText, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = { onAction(TextStylerAction.ClearError) },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("OK", color = ErrorText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
                        .background(CardBg)
                        .border(1.dp, Divider, RoundedCornerShape(10.dp))
                        .clickable { onAction(TextStylerAction.ToggleModePicker) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.showModePicker) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = if (state.showModePicker) "Close" else "Choose mode",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Templates button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardBg)
                        .border(1.dp, Divider, RoundedCornerShape(10.dp))
                        .clickable { onAction(TextStylerAction.ShowTemplates) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Templates",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Send button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (state.isLoading) Accent.copy(alpha = 0.4f) else Accent)
                    .clickable(enabled = !state.isLoading) {
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
    if (state.result != null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp)
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
                            imageVector = Icons.Default.CopyAll,
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
                            imageVector = Icons.Default.Close,
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
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
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
                        .background(if (isSelected) AccentSoft else CardBg)
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
