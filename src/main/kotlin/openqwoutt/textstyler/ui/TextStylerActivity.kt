package openqwoutt.miniapp.textstyler.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import openqwoutt.miniapp.textstyler.domain.ModeGroup
import openqwoutt.miniapp.textstyler.domain.StyleMode
import openqwoutt.miniapp.textstyler.domain.TextProcessorUseCase
import openqwoutt.miniapp.textstyler.domain.TextStylerResult

class TextStylerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val inputText = intent?.getStringExtra(Intent.EXTRA_PROCESS_TEXT).orEmpty()
        val readOnly = intent?.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false) ?: false

        setContent {
            MaterialTheme {
                TextStylerProcessingScreen(
                    inputText = inputText,
                    onTextProcessed = { processedText ->
                        if (readOnly) {
                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("AI Editor", processedText))
                            finish()
                        } else {
                            setResult(
                                RESULT_OK,
                                Intent().putExtra(Intent.EXTRA_PROCESS_TEXT, processedText)
                            )
                            finish()
                        }
                    },
                    onFailed = {
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun TextStylerProcessingScreen(
    inputText: String,
    onTextProcessed: (String) -> Unit,
    onFailed: () -> Unit
) {
    val processor = remember { TextProcessorUseCase() }
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
            Text("No selected text to process.")
            Button(onClick = onFailed) { Text("Close") }
            return@Column
        }

        Text(
            text = if (isProcessing) "Processing..." else "Choose an action",
            style = MaterialTheme.typography.titleLarge
        )

        StyleMode.entries
            .filter { it.group == ModeGroup.MAIN || it == StyleMode.SUMMARIZE || it == StyleMode.ANALYZE }
            .forEach { mode ->
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
