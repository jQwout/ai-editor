package openqwoutt.miniapp.textstyler.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import openqwoutt.miniapp.textstyler.domain.ModeGroup
import openqwoutt.miniapp.textstyler.domain.StyleMode
import openqwoutt.miniapp.textstyler.presentation.TextStylerAction
import openqwoutt.miniapp.textstyler.presentation.TextStylerState

private val AppBg = Color(0xFF0D0D11)
private val Panel = Color(0xFF1B1B20)
private val PanelLight = Color(0xFF23232A)
private val Accent = Color(0xFF8D78F0)
private val AccentDark = Color(0xFF302D42)
private val TextPrimary = Color(0xFFF4F1F8)
private val TextSecondary = Color(0xFFB9B5C3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextStylerScreen(
    state: TextStylerState,
    onAction: (TextStylerAction) -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    Scaffold(containerColor = AppBg) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AppBg)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Header(onNavigateBack = onNavigateBack)
            MainModeTabs(state = state, onAction = onAction)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = Panel,
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StyleStrip(state = state, onAction = onAction)
                    ActionStrip(state = state, onAction = onAction)
                    EditorBlock(
                        state = state,
                        onAction = onAction,
                        clipboard = clipboard
                    )
                    ResultBlock(state = state, clipboard = clipboard, onAction = onAction)
                }
            }

            BottomActions(
                state = state,
                onApply = { onAction(TextStylerAction.ProcessText) }
            )
        }
    }
}

@Composable
private fun Header(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onNavigateBack, modifier = Modifier.size(48.dp)) {
                Text("X", color = TextSecondary, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "AI Editor",
                color = TextPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Surface(
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(2.dp, TextSecondary.copy(alpha = 0.7f))
        ) {
            Text(
                text = "?",
                color = TextSecondary,
                modifier = Modifier.size(28.dp).padding(top = 2.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MainModeTabs(state: TextStylerState, onAction: (TextStylerAction) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(38.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        StyleMode.entries.filter { it.group == ModeGroup.MAIN }.forEach { mode ->
            val selected = state.selectedMode == mode
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(32.dp),
                color = if (selected) AccentDark else Color.Transparent,
                onClick = { onAction(TextStylerAction.SelectMode(mode)) }
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(mode.icon, color = if (selected) Accent else TextSecondary)
                    Text(
                        text = mode.shortName,
                        color = if (selected) Accent else TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StyleStrip(state: TextStylerState, onAction: (TextStylerAction) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        StyleMode.entries.filter { it.group == ModeGroup.STYLE }.forEach { mode ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(mode.icon, color = TextPrimary, fontWeight = FontWeight.Bold)
                FilterChip(
                    selected = state.selectedMode == mode,
                    onClick = { onAction(TextStylerAction.SelectMode(mode)) },
                    label = { Text(mode.shortName) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.Transparent,
                        labelColor = TextSecondary,
                        selectedContainerColor = AccentDark,
                        selectedLabelColor = Accent
                    ),
                )
            }
        }
    }
}

@Composable
private fun ActionStrip(state: TextStylerState, onAction: (TextStylerAction) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StyleMode.entries.filter { it.group == ModeGroup.ACTION }.forEach { mode ->
            FilterChip(
                selected = state.selectedMode == mode,
                onClick = { onAction(TextStylerAction.SelectMode(mode)) },
                label = { Text(mode.displayName) },
                leadingIcon = { Text(mode.icon) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = PanelLight,
                    labelColor = TextSecondary,
                    selectedContainerColor = AccentDark,
                    selectedLabelColor = Accent
                )
            )
        }
    }
}

@Composable
private fun EditorBlock(
    state: TextStylerState,
    onAction: (TextStylerAction) -> Unit,
    clipboard: ClipboardManager
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Original", color = TextPrimary, fontWeight = FontWeight.Bold)
            TextButton(
                onClick = {
                    val clipData = clipboard.primaryClip
                    val text = clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
                    if (!text.isNullOrBlank()) {
                        onAction(TextStylerAction.SetInputText(text))
                    }
                }
            ) {
                Icon(Icons.Default.ContentPaste, contentDescription = null, tint = TextSecondary)
                Spacer(Modifier.width(6.dp))
                Text("Paste", color = TextSecondary)
            }
        }
        OutlinedTextField(
            value = state.inputText,
            onValueChange = { onAction(TextStylerAction.SetInputText(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp),
            placeholder = { Text("Type text, dictate, or paste OCR from a screenshot...", color = TextSecondary) },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            )
        )
        state.error?.let { error ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF3A2228))) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(error, color = Color(0xFFFFC4CF), modifier = Modifier.weight(1f))
                    TextButton(onClick = { onAction(TextStylerAction.ClearError) }) {
                        Text("OK", color = Color(0xFFFFC4CF))
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
    state.result?.let { resultText ->
        Card(
            colors = CardDefaults.cardColors(containerColor = PanelLight),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Result", color = Accent, fontWeight = FontWeight.Bold)
                    Row {
                        IconButton(onClick = {
                            clipboard.setPrimaryClip(ClipData.newPlainText("AI Editor Result", resultText))
                        }) {
                            Icon(Icons.Default.CopyAll, contentDescription = "Copy", tint = Accent)
                        }
                        TextButton(onClick = { onAction(TextStylerAction.ClearResult) }) {
                            Text("Clear", color = TextSecondary)
                        }
                    }
                }
                if (state.isTextTruncated) {
                    Text("Input was trimmed to 3000 characters.", color = TextSecondary)
                }
                Text(resultText, color = TextPrimary)
            }
        }
    }
}

@Composable
private fun BottomActions(state: TextStylerState, onApply: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onApply,
            modifier = Modifier.weight(1f).height(58.dp),
            enabled = !state.isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
            shape = RoundedCornerShape(28.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Text("WORKING", color = Color.White, fontWeight = FontWeight.Bold)
            } else {
                Text("APPLY", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Button(
            onClick = onApply,
            modifier = Modifier.size(58.dp),
            enabled = !state.isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
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
