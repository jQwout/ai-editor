package openqwoutt.miniapp.textstyler

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import openqwoutt.miniapp.textstyler.domain.TextProcessorUseCase
import openqwoutt.miniapp.textstyler.presentation.TextStylerViewModel
import openqwoutt.miniapp.textstyler.ui.TextStylerScreen

@Composable
fun TextStylerMiniApp(
    onNavigateBack: () -> Unit = {}
) {
    val viewModel = remember { TextStylerViewModel(TextProcessorUseCase()) }
    val state by viewModel.state.collectAsState()

    MaterialTheme(colorScheme = darkColorScheme()) {
        TextStylerScreen(
            state = state,
            onAction = viewModel::handle,
            onNavigateBack = onNavigateBack
        )
    }
}
