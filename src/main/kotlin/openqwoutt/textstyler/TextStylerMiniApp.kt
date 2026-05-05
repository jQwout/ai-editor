package openqwoutt.miniapp.textstyler

import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import openqwoutt.miniapp.textstyler.domain.TextProcessorUseCase
import openqwoutt.miniapp.textstyler.presentation.CloseBehavior
import openqwoutt.miniapp.textstyler.presentation.MiniAppEvent
import openqwoutt.miniapp.textstyler.presentation.TextStylerAction
import openqwoutt.miniapp.textstyler.presentation.TextStylerViewModel
import openqwoutt.miniapp.textstyler.presentation.TextStylerViewModelFactory
import openqwoutt.miniapp.textstyler.ui.TextStylerScreen
import openqwoutt.miniapp.textstyler.data.prompts.PromptRepository
import openqwoutt.miniapp.textstyler.data.settings.SettingsRepository
import openqwoutt.miniapp.textstyler.data.repository.InteractionRepository

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
    val settingsRepository = remember { SettingsRepository(context) }
    val promptRepository = remember { PromptRepository(context) }
    val interactionRepository = remember { InteractionRepository(context) }
    val initialSettings = remember { settingsRepository.load() }
    val viewModel: TextStylerViewModel = viewModel(
        factory = TextStylerViewModelFactory(
            textProcessorUseCase = TextProcessorUseCase(settings = initialSettings),
            settingsRepository = settingsRepository,
            promptRepository = promptRepository,
            interactionRepository = interactionRepository
        )
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
                    clipboard.setText(event.result)
                }
                is MiniAppEvent.NavigateBack -> {
                    onNavigateBack()
                }
            }
        }
    }

    val state by viewModel.state.collectAsState()

    MaterialTheme(colorScheme = darkColorScheme()) {
        Scaffold(modifier = Modifier.systemBarsPadding().then(modifier)) {
            TextStylerScreen(
                state = state,
                onAction = viewModel::handle,
                onNavigateBack = onNavigateBack
            )
        }
    }
}