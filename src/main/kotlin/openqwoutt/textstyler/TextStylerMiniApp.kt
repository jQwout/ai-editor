package openqwoutt.miniapp.textstyler

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
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
import openqwoutt.miniapp.textstyler.presentation.TextStylerViewModel
import openqwoutt.miniapp.textstyler.ui.TextStylerScreen
import openqwoutt.textstyler.data.settings.SettingsRepository

@Composable
fun TextStylerMiniApp(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val initialSettings = remember { settingsRepository.load() }
    val viewModel = remember {
        TextStylerViewModel(
            textProcessorUseCase = TextProcessorUseCase(settings = initialSettings),
            settingsRepository = settingsRepository
        )
    }
    val state by viewModel.state.collectAsState()

    MaterialTheme(colorScheme = darkColorScheme()) {
        Scaffold {
            Box(modifier = Modifier.padding(it))
            TextStylerScreen(
                state = state,
                onAction = viewModel::handle,
                onNavigateBack = onNavigateBack
            )
        }
    }
}
