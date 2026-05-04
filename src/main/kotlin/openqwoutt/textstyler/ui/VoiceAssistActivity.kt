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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import openqwoutt.miniapp.textstyler.TextStylerMiniApp
import openqwoutt.miniapp.textstyler.presentation.CloseBehavior

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

        // Show unified screen with voice input
        showUnifiedScreen(spokenText)
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
                showUnifiedScreen(text)
            } else {
                finish()
            }
        } else {
            finish()
        }
    }

    private fun showUnifiedScreen(inputText: String) {
        setContent {
            MaterialTheme {
                TextStylerMiniApp(
                    initialInputText = inputText,
                    onNavigateBack = { finish() },
                    onResultReady = { processedText ->
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("AI Editor", processedText))
                        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    closeBehavior = CloseBehavior.CopyToClipboard,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}