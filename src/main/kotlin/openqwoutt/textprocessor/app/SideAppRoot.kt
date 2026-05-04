package openqwoutt.textprocessor.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import openqwoutt.miniapp.calendarplanner.CalendarPlannerMiniApp
import openqwoutt.miniapp.textstyler.TextStylerMiniApp

private val RootBg = Color(0xFF090B12)
private val RootPanel = Color(0xFF121726)
private val RootAccent = Color(0xFF7D8CFF)
private val RootAccentSoft = Color(0xFF273158)
private val RootTextPrimary = Color(0xFFF4F6FF)
private val RootTextSecondary = Color(0xFFABB4D6)

private enum class AppSection {
    Home,
    TextStyler,
    Calendar
}

@Composable
fun SideAppRoot() {
    var section by rememberSaveable { mutableStateOf(AppSection.Home) }

    BackHandler(enabled = section != AppSection.Home) {
        section = AppSection.Home
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        when (section) {
            AppSection.Home -> AppChooserScreen(
                onOpenTextStyler = { section = AppSection.TextStyler },
                onOpenCalendar = { section = AppSection.Calendar }
            )
            AppSection.TextStyler -> TextStylerMiniApp(
                onNavigateBack = { section = AppSection.Home }
            )
            AppSection.Calendar -> CalendarPlannerMiniApp(
                onNavigateBack = { section = AppSection.Home }
            )
        }
    }
}

@Composable
private fun AppChooserScreen(
    onOpenTextStyler: () -> Unit,
    onOpenCalendar: () -> Unit
) {
    Surface(color = RootBg) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(RootBg)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "AI Editor",
                    color = RootTextPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Текстовый помощник и календарь для планирования задач в одном приложении.",
                    color = RootTextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionCard(
                    title = "Text editor",
                    subtitle = "Translate, clean up, summarize and restyle text.",
                    badge = "TEXT",
                    accent = RootAccent,
                    soft = RootAccentSoft,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenTextStyler
                )
                ActionCard(
                    title = "Calendar",
                    subtitle = "View events, add meetings, and plan tasks in your calendar.",
                    badge = "CAL",
                    accent = Color(0xFF53D8B3),
                    soft = Color(0xFF183A35),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenCalendar
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = RootPanel),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("What you can do", color = RootTextPrimary, fontWeight = FontWeight.Bold)
                    Text("• Keep text tools for quick editing.", color = RootTextSecondary)
                    Text("• Connect to the device calendar and see upcoming events.", color = RootTextSecondary)
                    Text("• Add tasks as calendar entries so planning stays in one place.", color = RootTextSecondary)
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    badge: String,
    accent: Color,
    soft: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(200.dp),
        colors = CardDefaults.cardColors(containerColor = RootPanel),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(30.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = soft,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = badge,
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Text(
                    text = title,
                    color = RootTextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = RootTextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Text("Open")
            }
        }
    }
}
