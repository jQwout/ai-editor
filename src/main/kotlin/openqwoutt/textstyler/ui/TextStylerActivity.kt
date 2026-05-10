package openqwoutt.miniapp.textstyler.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import openqwoutt.miniapp.textstyler.TextStylerMiniApp
import openqwoutt.miniapp.textstyler.presentation.CloseBehavior

class TextStylerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val inputText = intent?.getStringExtra(Intent.EXTRA_PROCESS_TEXT).orEmpty()
        val readOnly = intent?.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false) ?: false

        setContent {
            UnifiedTextStylerScreen(
                    initialInputText = inputText,
                    readOnly = readOnly,
                    onFinished = { processedText ->
                        if (readOnly) {
                            // Copy to clipboard and finish
                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("AI Editor", processedText))
                            finish()
                        } else {
                            // Return as result
                            setResult(
                                RESULT_OK,
                                Intent().putExtra(Intent.EXTRA_PROCESS_TEXT, processedText)
                            )
                            finish()
                        }
                    },
                    onClose = {
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                )
        }
    }
}

@Composable
fun UnifiedTextStylerScreen(
    initialInputText: String,
    readOnly: Boolean,
    onFinished: (String) -> Unit,
    onClose: () -> Unit
) {
    val behavior = if (readOnly) CloseBehavior.CopyToClipboard else CloseBehavior.FinishWithResult

    TextStylerMiniApp(
        initialInputText = initialInputText.ifBlank { null },
        onNavigateBack = onClose,
        onResultReady = onFinished,
        closeBehavior = behavior,
        modifier = Modifier.fillMaxSize()
    )
}