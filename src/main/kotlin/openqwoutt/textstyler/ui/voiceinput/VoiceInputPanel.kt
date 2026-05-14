package openqwoutt.miniapp.textstyler.ui.voiceinput

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import openqwoutt.miniapp.textstyler.ui.voiceinput.VoiceInputConfig.getLanguageDisplayName
import openqwoutt.miniapp.textstyler.ui.voiceinput.VoiceInputConfig.getLanguageFlag
import openqwoutt.miniapp.textstyler.ui.voiceinput.VoiceInputState.RECORDING
import openqwoutt.miniapp.textstyler.ui.voiceinput.VoiceInputUiState

/**
 * Voice Input Panel with transcription and translation.
 */
@Composable
fun VoiceInputPanel(
    state: VoiceInputUiState,
    onAction: (VoiceInputAction) -> Unit,
    onInsertTranslation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Color(0xFF1A1A1A))
            .padding(16.dp)
    ) {
        // Header with expand/collapse
        PanelHeader(
            isExpanded = state.isExpanded,
            onToggleExpand = { onAction(VoiceInputAction.ToggleExpanded) }
        )

        AnimatedVisibility(
            visible = state.isExpanded,
            enter = expandVertically(animationSpec = tween(200)) + fadeIn(),
            exit = shrinkVertically(animationSpec = tween(200)) + fadeOut()
        ) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))

                // Language selector
                LanguageSelector(
                    sourceLanguage = state.sourceLanguage,
                    targetLanguage = state.targetLanguage,
                    onSelectLanguage = { onAction(VoiceInputAction.SelectLanguage(it)) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Voice input controls
                VoiceInputControls(
                    state = state,
                    onToggleRecording = { onAction(VoiceInputAction.ToggleRecording) },
                    onStopRecording = { onAction(VoiceInputAction.StopRecording) },
                    onClear = { onAction(VoiceInputAction.Clear) },
                    onInsert = { onInsertTranslation(state.translatedText) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Split view: Original + Translation
                SplitTextView(
                    originalText = state.originalText,
                    translatedText = state.translatedText
                )

                // Error message
                state.error?.let { error ->
                    Spacer(modifier = Modifier.height(12.dp))
                    ErrorMessage(
                        message = error,
                        onDismiss = { onAction(VoiceInputAction.DismissError) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PanelHeader(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "🎤",
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Voice Input",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = Color(0xFF8E8E93)
        )
    }
}

@Composable
private fun LanguageSelector(
    sourceLanguage: String,
    targetLanguage: String,
    onSelectLanguage: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        VoiceInputConfig.supportedLanguages.forEach { lang ->
            val isSelected = lang == sourceLanguage
            val backgroundColor = if (isSelected) {
                Color(0xFF8774E1).copy(alpha = 0.2f)
            } else {
                Color(0xFF2C2C2E)
            }
            val borderColor = if (isSelected) {
                Color(0xFF8774E1)
            } else {
                Color.Transparent
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(backgroundColor)
                    .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                    .clickable { onSelectLanguage(lang) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = getLanguageFlag(lang),
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = lang.uppercase(),
                        color = if (isSelected) Color(0xFF8774E1) else Color.White,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            if (lang != VoiceInputConfig.supportedLanguages.last()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "↔",
                    color = Color(0xFF8E8E93),
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun VoiceInputControls(
    state: VoiceInputUiState,
    onToggleRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onClear: () -> Unit,
    onInsert: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Record/Stop button
        RecordButton(
            isRecording = state.state == RECORDING,
            audioLevel = state.audioLevel,
            onClick = if (state.state == RECORDING) onStopRecording else onToggleRecording,
            isEnabled = state.isMicPermissionGranted
        )

        // Clear button
        IconButton(
            onClick = onClear,
            enabled = state.originalText.isNotBlank() || state.translatedText.isNotBlank()
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Clear",
                tint = if (state.originalText.isNotBlank() || state.translatedText.isNotBlank()) {
                    Color.White
                } else {
                    Color(0xFF8E8E93)
                }
            )
        }

        // Insert button
        Button(
            onClick = onInsert,
            enabled = state.translatedText.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF8774E1),
                disabledContainerColor = Color(0xFF2C2C2E)
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ContentPaste,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Insert")
        }
    }

    // Status text
    val statusText = when (state.state) {
        VoiceInputState.IDLE -> if (!state.isMicPermissionGranted) "Grant mic permission" else "Tap to record"
        VoiceInputState.RECORDING -> "Recording..."
        VoiceInputState.PROCESSING -> "Translating..."
        VoiceInputState.ERROR -> state.error ?: "Error"
    }

    Text(
        text = statusText,
        color = if (state.state == VoiceInputState.ERROR) Color(0xFFFF6B6B) else Color(0xFF8E8E93),
        fontSize = 12.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    )
}

@Composable
private fun RecordButton(
    isRecording: Boolean,
    audioLevel: Float,
    onClick: () -> Unit,
    isEnabled: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .size(64.dp)
            .scale(if (isRecording) scale else 1f)
            .clip(CircleShape)
            .background(
                if (isRecording) Color(0xFFFF4444) else Color(0xFF8774E1)
            )
            .then(
                if (isEnabled) Modifier.clickable { onClick() } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isRecording) {
            // Waveform visualization based on audio level
            Box(
                modifier = Modifier
                    .size((40 + audioLevel * 20).dp)
                    .clip(CircleShape)
                    .background(Color.Red.copy(alpha = 0.3f + audioLevel * 0.3f))
            )
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = "Stop recording",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Start recording",
                tint = if (isEnabled) Color.White else Color(0xFF8E8E93),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun SplitTextView(
    originalText: String,
    translatedText: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Original text panel
        TextPanel(
            text = originalText,
            label = "Original",
            modifier = Modifier.weight(1f)
        )

        // Translation panel
        TextPanel(
            text = translatedText,
            label = "Translation",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TextPanel(
    text: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F0F0F))
            .padding(12.dp)
    ) {
        Text(
            text = label,
            color = Color(0xFF8E8E93),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text.ifBlank { "..." },
            color = if (text.isBlank()) Color(0xFF8E8E93) else Color.White,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun ErrorMessage(
    message: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF3A2228))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            color = Color(0xFFFFC4CF),
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Dismiss",
                tint = Color(0xFFFF6B6B)
            )
        }
    }
}
