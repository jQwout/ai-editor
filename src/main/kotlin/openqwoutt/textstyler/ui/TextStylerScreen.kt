@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package openqwoutt.miniapp.textstyler.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import openqwoutt.miniapp.textstyler.domain.ModeGroup
import openqwoutt.miniapp.textstyler.domain.StyleMode
import openqwoutt.miniapp.textstyler.presentation.TextStylerAction
import openqwoutt.miniapp.textstyler.presentation.TextStylerState

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

/** One slot for secondary mode controls so switching Analysis/Style/Fix does not collapse the layout. */
private val ModeSubpanelHeight = 76.dp

@Composable
fun TextStylerScreen(
    state: TextStylerState,
    onAction: (TextStylerAction) -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    if (state.showSettings) {
        SettingsScreen(
            settings = state.settings,
            availableModels = state.availableModels,
            isLoadingModels = state.isLoadingModels,
            onSave = { onAction(TextStylerAction.SaveSettings(it)) },
            onBack = { onAction(TextStylerAction.ToggleSettings) }
        )
        return
    }

    Box(
        modifier = Modifier
            .systemBarsPadding()
            .fillMaxSize()
            .background(Bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Header(
                    onNavigateBack = onNavigateBack,
                    onOpenSettings = { onAction(TextStylerAction.ToggleSettings) }
                )
                MainModeTabs(state = state, onAction = onAction)
                ModeSubControls(state = state, onAction = onAction)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EditorBlock(
                    state = state,
                    onAction = onAction,
                    clipboard = clipboard
                )

                if (state.result != null || state.isLoading) {
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

                Spacer(modifier = Modifier.height(8.dp))

                BottomActions(
                    state = state,
                    onApply = { onAction(TextStylerAction.ProcessText) }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun Header(
    onNavigateBack: () -> Unit,
    onOpenSettings: () -> Unit
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
        Text(
            text = "AI Editor",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = onOpenSettings, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = TextSecondary
            )
        }
    }
}

private val mainCategoryModes = listOf(
    ModeGroup.ANALYSIS to "Analysis",
    ModeGroup.STYLE to "Style",
    ModeGroup.FIX to "Fix"
)

@Composable
private fun MainModeTabs(state: TextStylerState, onAction: (TextStylerAction) -> Unit) {
    val selectedIndex = mainCategoryModes.indexOfFirst { it.first == state.selectedMode.group }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .padding(4.dp)
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            mainCategoryModes.forEachIndexed { index, (group, label) ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = mainCategoryModes.size
                    ),
                    onClick = {
                        when (group) {
                            ModeGroup.ANALYSIS -> {
                                if (state.selectedMode.group != ModeGroup.ANALYSIS) {
                                    onAction(TextStylerAction.SelectMode(StyleMode.ANALYZE))
                                }
                            }
                            ModeGroup.STYLE -> {
                                if (state.selectedMode.group != ModeGroup.STYLE) {
                                    onAction(TextStylerAction.SelectMode(StyleMode.STYLE))
                                }
                            }
                            ModeGroup.FIX -> onAction(TextStylerAction.SelectMode(StyleMode.FIX))
                        }
                    },
                    selected = index == selectedIndex,
                    modifier = Modifier.weight(1f),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = Accent,
                        activeContentColor = Color.White,
                        activeBorderColor = Accent,
                        inactiveContainerColor = Color.Transparent,
                        inactiveContentColor = TextSecondary,
                        inactiveBorderColor = Divider
                    )
                ) {
                    Text(
                        text = label,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeSubControls(state: TextStylerState, onAction: (TextStylerAction) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ModeSubpanelHeight),
        contentAlignment = Alignment.Center
    ) {
        when (state.selectedMode.group) {
            ModeGroup.ANALYSIS -> AnalyzeStrip(state = state, onAction = onAction)
            ModeGroup.STYLE -> StyleStrip(state = state, onAction = onAction)
            ModeGroup.FIX -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Surface)
                )
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
            val selected = state.selectedStyle == mode
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

private val analysisSubModes = listOf(StyleMode.SUMMARIZE, StyleMode.ANALYZE)

@Composable
private fun AnalyzeStrip(state: TextStylerState, onAction: (TextStylerAction) -> Unit) {
    val selectedIndex = analysisSubModes.indexOf(state.selectedMode).coerceAtLeast(0)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .padding(4.dp)
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            analysisSubModes.forEachIndexed { index, mode ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = analysisSubModes.size
                    ),
                    onClick = { onAction(TextStylerAction.SelectMode(mode)) },
                    selected = index == selectedIndex,
                    modifier = Modifier.weight(1f),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = Accent,
                        activeContentColor = Color.White,
                        activeBorderColor = Accent,
                        inactiveContainerColor = Color.Transparent,
                        inactiveContentColor = TextSecondary,
                        inactiveBorderColor = Divider
                    )
                ) {
                    Text(
                        text = mode.shortName,
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

@Composable
private fun ResultBlock(
    state: TextStylerState,
    clipboard: ClipboardManager,
    onAction: (TextStylerAction) -> Unit
) {
    state.result?.let { resultText ->
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
                            clipboard.setPrimaryClip(ClipData.newPlainText("AI Editor Result", resultText))
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
                text = resultText,
                color = TextPrimary,
                fontSize = 17.sp,
                lineHeight = 24.sp
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

@Preview(showBackground = true)
@Composable
private fun TextStylerScreenPreview() {
    MaterialTheme {
        TextStylerScreen(
            state = TextStylerState(
                inputText = "Example text that needs a sharper style.",
                selectedMode = StyleMode.STYLE
            ),
            onAction = {}
        )
    }
}
