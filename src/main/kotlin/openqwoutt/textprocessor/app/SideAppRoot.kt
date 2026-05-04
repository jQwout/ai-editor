package openqwoutt.textprocessor.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import openqwoutt.miniapp.textstyler.TextStylerMiniApp

private val RootBg = Color(0xFF090B12)
private val RootTextPrimary = Color(0xFFF4F6FF)
private val RootTextSecondary = Color(0xFFABB4D6)

@Composable
fun SideAppRoot() {
    TextStylerMiniApp()
}
