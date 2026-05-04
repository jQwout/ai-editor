package openqwoutt.miniapp.calendarplanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import openqwoutt.miniapp.calendarplanner.domain.CalendarEventItem
import openqwoutt.miniapp.calendarplanner.domain.CalendarSource
import openqwoutt.miniapp.calendarplanner.presentation.CalendarPlannerAction
import openqwoutt.miniapp.calendarplanner.presentation.CalendarPlannerState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val AppBg = Color(0xFF08131D)
private val Panel = Color(0xFF0F1C28)
private val PanelLight = Color(0xFF162636)
private val Accent = Color(0xFF56D6B1)
private val AccentSoft = Color(0xFF1E3A3C)
private val TextPrimary = Color(0xFFF3FAFF)
private val TextSecondary = Color(0xFF9AB0C0)
private val Warning = Color(0xFFFFC96A)
private val ErrorBg = Color(0xFF3A2228)

@Composable
fun CalendarPlannerScreen(
    state: CalendarPlannerState,
    onAction: (CalendarPlannerAction) -> Unit,
    onRequestPermissions: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(containerColor = AppBg) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AppBg),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Header(
                    onNavigateBack = onNavigateBack,
                    onRefresh = { onAction(CalendarPlannerAction.Refresh) }
                )
            }

            if (!state.hasPermissions) {
                item { PermissionBanner(onRequestPermissions = onRequestPermissions) }
            }

            item { SummaryStrip(state = state) }

            item {
                CalendarPicker(
                    calendars = state.calendars,
                    selectedCalendarId = state.selectedCalendarId,
                    onSelect = { onAction(CalendarPlannerAction.SelectCalendar(it)) }
                )
            }

            item {
                AddEventCard(
                    state = state,
                    onAction = onAction
                )
            }

            item {
                UpcomingEventsHeader(
                    count = state.upcomingEvents.size,
                    isLoading = state.isLoading
                )
            }

            items(state.upcomingEvents) { event ->
                EventRow(event = event)
            }

            if (state.upcomingEvents.isEmpty() && state.hasPermissions && !state.isLoading) {
                item { EmptyState() }
            }

            if (state.error != null || state.successMessage != null) {
                item {
                    FeedbackCard(
                        error = state.error,
                        success = state.successMessage,
                        onDismiss = { onAction(CalendarPlannerAction.ClearMessages) }
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextSecondary)
                Spacer(Modifier.size(6.dp))
                Text("Back", color = TextSecondary)
            }
            Spacer(Modifier.size(6.dp))
            Text(
                text = "Calendar",
                color = TextPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        IconButton(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Accent)
        }
    }
}

@Composable
private fun PermissionBanner(onRequestPermissions: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(26.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(color = AccentSoft, shape = CircleShape) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Calendar access is off", color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(
                    "Grant access so the app can show events and save planning items to your device calendar.",
                    color = TextSecondary
                )
            }
            Button(onClick = onRequestPermissions) {
                Text("Allow")
            }
        }
    }
}

@Composable
private fun SummaryStrip(state: CalendarPlannerState) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        SummaryCard(
            title = "Calendars",
            value = state.calendars.size.toString(),
            caption = "writable sources",
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            title = "Events",
            value = state.upcomingEvents.size.toString(),
            caption = "next 7 days",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    caption: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, color = TextSecondary)
            Text(value, color = Accent, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(caption, color = TextSecondary)
        }
    }
}

@Composable
private fun CalendarPicker(
    calendars: List<CalendarSource>,
    selectedCalendarId: Long?,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = calendars.firstOrNull { it.id == selectedCalendarId }?.displayName ?: "Select a calendar"

    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Target calendar", color = TextPrimary, fontWeight = FontWeight.Bold)
            Box {
                TextButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedLabel, color = TextPrimary)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    calendars.forEach { calendar ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(calendar.displayName, color = TextPrimary)
                                    Text(
                                        text = if (calendar.isPrimary) "Primary calendar" else (calendar.ownerName ?: "Writable calendar"),
                                        color = TextSecondary
                                    )
                                }
                            },
                            onClick = {
                                onSelect(calendar.id)
                                expanded = false
                            }
                        )
                    }
                }
            }
            if (calendars.isEmpty()) {
                Text(
                    "No writable calendars were found. Check account sync or add a calendar on the device.",
                    color = Warning
                )
            }
        }
    }
}

@Composable
private fun AddEventCard(
    state: CalendarPlannerState,
    onAction: (CalendarPlannerAction) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Plan a task",
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Save a task or meeting directly into the calendar so your day is planned in one place.",
                color = TextSecondary
            )

            OutlinedTextField(
                value = state.title,
                onValueChange = { onAction(CalendarPlannerAction.SetTitle(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                placeholder = { Text("Design review, gym, call mom...") },
                singleLine = true
            )
            OutlinedTextField(
                value = state.notes,
                onValueChange = { onAction(CalendarPlannerAction.SetNotes(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 110.dp),
                label = { Text("Notes") },
                placeholder = { Text("Useful details, checklist, location, link") }
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = state.dateText,
                    onValueChange = { onAction(CalendarPlannerAction.SetDate(it)) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Date") },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.startTimeText,
                    onValueChange = { onAction(CalendarPlannerAction.SetStartTime(it)) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Start") },
                    placeholder = { Text("HH:MM") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.endTimeText,
                    onValueChange = { onAction(CalendarPlannerAction.SetEndTime(it)) },
                    modifier = Modifier.weight(1f),
                    label = { Text("End") },
                    placeholder = { Text("HH:MM") },
                    singleLine = true
                )
            }

            Button(
                onClick = { onAction(CalendarPlannerAction.AddEvent) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                shape = RoundedCornerShape(18.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("Saving...", color = Color.White)
                } else {
                    Text("Add to calendar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun UpcomingEventsHeader(count: Int, isLoading: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Upcoming events", color = TextPrimary, fontWeight = FontWeight.Bold)
            Text(
                text = if (isLoading) "Loading events..." else "$count event(s) coming up",
                color = TextSecondary
            )
        }
        Icon(Icons.Default.Event, contentDescription = null, tint = Accent)
    }
}

@Composable
private fun EventRow(event: CalendarEventItem) {
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(event.startMillis).atZone(zone)
    val end = Instant.ofEpochMilli(event.endMillis).atZone(zone)
    val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Card(
        colors = CardDefaults.cardColors(containerColor = PanelLight),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = AccentSoft, shape = CircleShape) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(event.title, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(
                    text = if (event.allDay) {
                        "${start.format(dateFormatter)} • All day"
                    } else {
                        "${start.format(dateFormatter)} • ${start.format(timeFormatter)} - ${end.format(timeFormatter)}"
                    },
                    color = TextSecondary
                )
                event.location?.takeIf { it.isNotBlank() }?.let { location ->
                    Text(location, color = Warning)
                }
            }
            Text(event.calendarName, color = TextSecondary)
        }
    }
}

@Composable
private fun EmptyState() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("No events found", color = TextPrimary, fontWeight = FontWeight.Bold)
            Text(
                "The next 7 days are clear, or the selected calendars do not have visible entries.",
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun FeedbackCard(
    error: String?,
    success: String?,
    onDismiss: () -> Unit
) {
    val bg = if (error != null) ErrorBg else AccentSoft
    val textColor = if (error != null) Color(0xFFFFC4CF) else Accent
    val message = error ?: success.orEmpty()

    Card(
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(message, color = textColor, modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) {
                Text("OK", color = textColor)
            }
        }
    }
}
