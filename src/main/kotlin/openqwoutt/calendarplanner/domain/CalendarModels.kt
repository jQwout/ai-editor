package openqwoutt.miniapp.calendarplanner.domain

data class CalendarSource(
    val id: Long,
    val displayName: String,
    val ownerName: String?,
    val accessLevel: Int,
    val isPrimary: Boolean
)

data class CalendarEventItem(
    val id: Long,
    val title: String,
    val location: String?,
    val calendarName: String,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean
)

data class CalendarDraft(
    val title: String,
    val notes: String,
    val calendarId: Long,
    val startMillis: Long,
    val endMillis: Long
)
