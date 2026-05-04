package openqwoutt.miniapp.textstyler

import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import openqwoutt.miniapp.textstyler.domain.TextProcessorUseCase
import openqwoutt.miniapp.textstyler.presentation.CloseBehavior
import openqwoutt.miniapp.textstyler.presentation.TextStylerAction
import openqwoutt.miniapp.textstyler.presentation.TextStylerViewModel
import openqwoutt.miniapp.textstyler.presentation.TextStylerViewModelFactory
import openqwoutt.miniapp.textstyler.ui.TextStylerScreen
import openqwoutt.textstyler.data.settings.SettingsRepository

@Composable
fun TextStylerMiniApp(
    initialInputText: String? = null,
    onNavigateBack: () -> Unit = {},
    onResultReady: ((String) -> Unit)? = null,
    closeBehavior: CloseBehavior = CloseBehavior.NavigateBack,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val initialSettings = remember { settingsRepository.load() }
    val viewModel: TextStylerViewModel = viewModel(
        factory = TextStylerViewModelFactory(
            textProcessorUseCase = TextProcessorUseCase(settings = initialSettings),
            settingsRepository = settingsRepository
        )
    )

    // Apply initial input text, close behavior, and result callback via LaunchedEffect
    LaunchedEffect(initialInputText, closeBehavior, onResultReady) {
        if (initialInputText != null) {
            viewModel.setInitialInputText(initialInputText)
        }
        viewModel.handle(TextStylerAction.SetCloseBehavior(closeBehavior))
        if (onResultReady != null) {
            viewModel.handle(TextStylerAction.SetOnResultReady(onResultReady))
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