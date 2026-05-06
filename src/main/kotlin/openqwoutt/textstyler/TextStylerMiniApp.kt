package openqwoutt.miniapp.textstyler

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.viewmodel.compose.viewModel
import openqwoutt.miniapp.textstyler.presentation.CloseBehavior
import openqwoutt.miniapp.textstyler.presentation.MiniAppEvent
import openqwoutt.miniapp.textstyler.presentation.TextStylerAction
import openqwoutt.miniapp.textstyler.presentation.TextStylerViewModel
import openqwoutt.miniapp.textstyler.ui.TextStylerScreen
import openqwoutt.textprocessor.app.TextProcessorApplication

@Composable
fun TextStylerMiniApp(
    initialInputText: String? = null,
    onNavigateBack: () -> Unit = {},
    onResultReady: ((String) -> Unit)? = null,
    closeBehavior: CloseBehavior = CloseBehavior.NavigateBack,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val appGraph = (context.applicationContext as TextProcessorApplication).appGraph
    val viewModel: TextStylerViewModel = viewModel(
        factory = appGraph.textStylerViewModelFactory
    )

    // Keep callback reference stable across recompositions
    val stableOnResultReady = onResultReady

    // Apply initial input text, close behavior via stable keys (no lambdas)
    LaunchedEffect(initialInputText, closeBehavior) {
        if (initialInputText != null) {
            viewModel.setInitialInputText(initialInputText)
        }
        viewModel.handle(TextStylerAction.SetCloseBehavior(closeBehavior))
    }

    // Handle one-shot events from ViewModel
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MiniAppEvent.ResultReady -> {
                    stableOnResultReady?.invoke(event.result)
                    clipboard.setText(AnnotatedString(event.result))
                }

                is MiniAppEvent.NavigateBack -> {
                    onNavigateBack()
                }
            }
        }
    }

    val state by viewModel.state.collectAsState()

    MaterialTheme(colorScheme = darkColorScheme()) {
        Scaffold(
            modifier = Modifier
                .then(modifier)
        ) {
            Box(modifier = Modifier.padding(it)) {
                TextStylerScreen(
                    state = state,
                    onAction = viewModel::handle,
                    onNavigateBack = onNavigateBack
                )
            }
        }
    }
}
