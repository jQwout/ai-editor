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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import openqwoutt.textstyler.data.settings.ApiMode
import openqwoutt.textstyler.data.settings.AppSettings

private val AppBg = Color(0xFF0D0D11)
private val Panel = Color(0xFF1B1B20)
private val Accent = Color(0xFF8D78F0)
private val TextPrimary = Color(0xFFF4F1F8)
private val TextSecondary = Color(0xFFB9B5C3)

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSave: (AppSettings) -> Unit,
    onBack: () -> Unit
) {
    var mode by remember { mutableStateOf(settings.mode) }
    var apiKey by remember { mutableStateOf(settings.apiKey) }
    var model by remember { mutableStateOf(settings.model) }
    var backendUrl by remember { mutableStateOf(settings.backendUrl) }
    var autoPaste by remember { mutableStateOf(settings.autoPaste) }
    var autoCopyResult by remember { mutableStateOf(settings.autoCopyResult) }
    var soundEffects by remember { mutableStateOf(settings.soundEffects) }
    var hapticFeedback by remember { mutableStateOf(settings.hapticFeedback) }

    Surface(
        modifier = Modifier.fillMaxSize().systemBarsPadding(),
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
                        text = "API Mode",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (mode == ApiMode.LOCAL_BACKEND) "Local Backend" else "OpenRouter Direct",
                            color = TextSecondary
                        )
                        Switch(
                            checked = mode == ApiMode.OPENROUTER_DIRECT,
                            onCheckedChange = {
                                mode = if (it) ApiMode.OPENROUTER_DIRECT else ApiMode.LOCAL_BACKEND
                            }
                        )
                    }

                    if (mode == ApiMode.LOCAL_BACKEND) {
                        OutlinedTextField(
                            value = backendUrl,
                            onValueChange = { backendUrl = it },
                            label = { Text("Backend URL") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                            singleLine = true
                        )
                    }

                    if (mode == ApiMode.OPENROUTER_DIRECT) {
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text("OpenRouter API Key") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation()
                        )

                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it },
                            label = { Text("Model ID") },
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
                }
            }

            Button(
                onClick = {
                    onSave(
                        AppSettings(
                            backendUrl = backendUrl.trim(),
                            mode = mode,
                            apiKey = apiKey.trim(),
                            model = model.trim(),
                            autoPaste = autoPaste,
                            autoCopyResult = autoCopyResult,
                            soundEffects = soundEffects,
                            hapticFeedback = hapticFeedback
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
