package openqwoutt.miniapp.calendarplanner.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import openqwoutt.miniapp.calendarplanner.domain.CalendarDraft
import openqwoutt.miniapp.calendarplanner.domain.CalendarEventItem
import openqwoutt.miniapp.calendarplanner.domain.CalendarRepository
import openqwoutt.miniapp.calendarplanner.domain.CalendarSource
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class CalendarPlannerState(
    val hasPermissions: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val calendars: List<CalendarSource> = emptyList(),
    val selectedCalendarId: Long? = null,
    val upcomingEvents: List<CalendarEventItem> = emptyList(),
    val title: String = "",
    val notes: String = "",
    val dateText: String = LocalDate.now().toString(),
    val startTimeText: String = LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0).format(DateTimeFormatter.ofPattern("HH:mm")),
    val endTimeText: String = LocalTime.now().plusHours(2).withMinute(0).withSecond(0).withNano(0).format(DateTimeFormatter.ofPattern("HH:mm")),
    val error: String? = null,
    val successMessage: String? = null
)

sealed class CalendarPlannerAction {
    data object Refresh : CalendarPlannerAction()
    data class SetTitle(val value: String) : CalendarPlannerAction()
    data class SetNotes(val value: String) : CalendarPlannerAction()
    data class SetDate(val value: String) : CalendarPlannerAction()
    data class SetStartTime(val value: String) : CalendarPlannerAction()
    data class SetEndTime(val value: String) : CalendarPlannerAction()
    data class SelectCalendar(val value: Long) : CalendarPlannerAction()
    data object AddEvent : CalendarPlannerAction()
    data object ClearMessages : CalendarPlannerAction()
}

class CalendarPlannerViewModel(
    context: Context
) : ViewModel() {
    private val repository = CalendarRepository(context.applicationContext)

    private val _state = androidx.compose.runtime.mutableStateOf(CalendarPlannerState())
    val state: androidx.compose.runtime.State<CalendarPlannerState> = _state

    fun handle(action: CalendarPlannerAction) {
        when (action) {
            CalendarPlannerAction.Refresh -> refresh()
            is CalendarPlannerAction.SetTitle -> updateState { it.copy(title = action.value, error = null, successMessage = null) }
            is CalendarPlannerAction.SetNotes -> updateState { it.copy(notes = action.value, error = null, successMessage = null) }
            is CalendarPlannerAction.SetDate -> updateState { it.copy(dateText = action.value, error = null, successMessage = null) }
            is CalendarPlannerAction.SetStartTime -> updateState { it.copy(startTimeText = action.value, error = null, successMessage = null) }
            is CalendarPlannerAction.SetEndTime -> updateState { it.copy(endTimeText = action.value, error = null, successMessage = null) }
            is CalendarPlannerAction.SelectCalendar -> updateState { it.copy(selectedCalendarId = action.value, error = null, successMessage = null) }
            CalendarPlannerAction.AddEvent -> addEvent()
            CalendarPlannerAction.ClearMessages -> updateState { it.copy(error = null, successMessage = null) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null, successMessage = null) }

            val hasPermissions = repository.hasCalendarPermissions()
            if (!hasPermissions) {
                updateState {
                    it.copy(
                        isLoading = false,
                        hasPermissions = false,
                        calendars = emptyList(),
                        upcomingEvents = emptyList(),
                        selectedCalendarId = null
                    )
                }
                return@launch
            }

            val calendars = withContext(Dispatchers.IO) { repository.loadWritableCalendars() }
            val selectedCalendarId = _state.value.selectedCalendarId ?: calendars.firstOrNull()?.id
            val events = withContext(Dispatchers.IO) { repository.loadUpcomingEvents() }

            updateState {
                it.copy(
                    isLoading = false,
                    hasPermissions = true,
                    calendars = calendars,
                    selectedCalendarId = selectedCalendarId,
                    upcomingEvents = events,
                    error = if (calendars.isEmpty()) "No writable calendars were found on this device." else null
                )
            }
        }
    }

    fun onPermissionsChanged(granted: Boolean) {
        updateState {
            it.copy(
                hasPermissions = granted,
                error = if (granted) null else "Calendar permission is required to view and add events."
            )
        }
        if (granted) {
            refresh()
        }
    }

    private fun addEvent() {
        val currentState = _state.value
        val calendarId = currentState.selectedCalendarId

        if (!currentState.hasPermissions) {
            updateState { it.copy(error = "Grant calendar access first.") }
            return
        }
        if (calendarId == null) {
            updateState { it.copy(error = "Choose a calendar before adding an event.") }
            return
        }
        if (currentState.title.isBlank()) {
            updateState { it.copy(error = "Add a title for the event or task.") }
            return
        }

        val draft = runCatching {
            val date = LocalDate.parse(currentState.dateText)
            val start = LocalTime.parse(normalizeTimeText(currentState.startTimeText))
            val end = LocalTime.parse(normalizeTimeText(currentState.endTimeText))
            val startMillis = date.atTime(start).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = date.atTime(end).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            require(endMillis > startMillis) { "End time must be after start time." }
            CalendarDraft(
                title = currentState.title.trim(),
                notes = currentState.notes.trim(),
                calendarId = calendarId,
                startMillis = startMillis,
                endMillis = endMillis
            )
        }.getOrElse {
            updateState { it.copy(error = "Use date format YYYY-MM-DD and time format HH:MM.") }
            return
        }

        viewModelScope.launch {
            updateState { it.copy(isSaving = true, error = null, successMessage = null) }
            val result = withContext(Dispatchers.IO) { repository.addEvent(draft) }

            result.fold(
                onSuccess = {
                    val refreshedEvents = withContext(Dispatchers.IO) { repository.loadUpcomingEvents() }
                    updateState {
                        it.copy(
                            isSaving = false,
                            title = "",
                            notes = "",
                            successMessage = "Event added to calendar.",
                            upcomingEvents = refreshedEvents
                        )
                    }
                },
                onFailure = { throwable ->
                    updateState {
                        it.copy(
                            isSaving = false,
                            error = throwable.message ?: "Could not add the event."
                        )
                    }
                }
            )
        }
    }

    private fun normalizeTimeText(value: String): String {
        val trimmed = value.trim()
        return if (trimmed.length == 4) "0$trimmed" else trimmed
    }

    private fun updateState(block: (CalendarPlannerState) -> CalendarPlannerState) {
        _state.value = block(_state.value)
    }
}
