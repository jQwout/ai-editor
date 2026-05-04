package openqwoutt.miniapp.textstyler.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import openqwoutt.miniapp.textstyler.domain.StyleMode
import openqwoutt.miniapp.textstyler.domain.TextProcessorUseCase
import openqwoutt.miniapp.textstyler.domain.TextStylerResult
import openqwoutt.textstyler.data.settings.SettingsRepository

class VoiceAssistActivity : ComponentActivity() {

    companion object {
        private const val REQ_SPEECH = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val spokenText = intent?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            ?: intent?.getStringExtra(Intent.EXTRA_TEXT).orEmpty()

        if (spokenText.isBlank()) {
            startSpeechRecognizer()
            return
        }

        val settingsRepository = SettingsRepository(this)
        val settings = settingsRepository.load()

        showProcessingScreen(spokenText, settings)
    }

    private fun startSpeechRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        runCatching {
            startActivityForResult(intent, REQ_SPEECH)
        }.onFailure {
            Toast.makeText(this, "Speech recognition is not available", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_SPEECH && resultCode == RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = results?.firstOrNull().orEmpty()
            if (text.isNotBlank()) {
                val settingsRepository = SettingsRepository(this)
                showProcessingScreen(text, settingsRepository.load())
            } else {
                finish()
            }
        } else {
            finish()
        }
    }

    private fun showProcessingScreen(inputText: String, settings: openqwoutt.textstyler.data.settings.AppSettings) {
        setContent {
            MaterialTheme {
                VoiceAssistScreen(
                    inputText = inputText,
                    processor = remember { TextProcessorUseCase(settings = settings) },
                    onTextProcessed = { processedText ->
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("AI Editor", processedText))
                        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onFailed = {
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun VoiceAssistScreen(
    inputText: String,
    processor: TextProcessorUseCase,
    onTextProcessed: (String) -> Unit,
    onFailed: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (inputText.isBlank()) {
            Text("No voice input received.")
            Button(onClick = onFailed) { Text("Close") }
            return@Column
        }

        Text(
            text = if (isProcessing) "Processing..." else "Choose an action",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = """"$inputText"""",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        listOf(
            StyleMode.ANALYZE,
            StyleMode.STYLE,
            StyleMode.FIX,
            StyleMode.SUMMARIZE
        ).forEach { mode ->
                OutlinedButton(
                    onClick = {
                        isProcessing = true
                        error = null
                        scope.launch {
                            when (val result = processor.processText(inputText, mode)) {
                                is TextStylerResult.Success -> onTextProcessed(result.result)
                                else -> {
                                    error = "Could not process text."
                                    isProcessing = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing
                ) {
                    Text(mode.displayName)
                }
            }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Button(onClick = onFailed) { Text("Close") }
        }
    }
}
