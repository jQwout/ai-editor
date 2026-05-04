package openqwoutt.miniapp.textstyler

import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import openqwoutt.miniapp.textstyler.domain.TextProcessorUseCase
import openqwoutt.miniapp.textstyler.presentation.CloseBehavior
import openqwoutt.miniapp.textstyler.presentation.TextStylerAction
import openqwoutt.miniapp.textstyler.presentation.TextStylerViewModel
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
    val viewModel = remember(initialInputText) {
        TextStylerViewModel(
            textProcessorUseCase = TextProcessorUseCase(settings = initialSettings),
            settingsRepository = settingsRepository
        )
    }

    // Apply initial input text if provided
    if (initialInputText != null) {
        viewModel.setInitialInputText(initialInputText)
    }

    // Set close behavior
    viewModel.handle(TextStylerAction.SetCloseBehavior(closeBehavior))

    // Set result callback if provided
    if (onResultReady != null) {
        viewModel.handle(TextStylerAction.SetOnResultReady(onResultReady))
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