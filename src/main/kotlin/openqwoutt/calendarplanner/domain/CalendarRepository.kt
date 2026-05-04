package openqwoutt.miniapp.calendarplanner.domain

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class CalendarRepository(private val context: Context) {
    private val contentResolver: ContentResolver = context.contentResolver
    private val zoneId: ZoneId = ZoneId.systemDefault()

    fun hasCalendarPermissions(): Boolean {
        val readGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val writeGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CALENDAR
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        return readGranted && writeGranted
    }

    fun loadWritableCalendars(): List<CalendarSource> {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.OWNER_ACCOUNT,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.VISIBLE,
            CalendarContract.Calendars.IS_PRIMARY
        )

        val selection = buildString {
            append("${CalendarContract.Calendars.VISIBLE} = 1")
            append(" AND ${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?")
        }
        val selectionArgs = arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())
        val sortOrder = "${CalendarContract.Calendars.IS_PRIMARY} DESC, ${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} COLLATE NOCASE ASC"

        return contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val ownerIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.OWNER_ACCOUNT)
            val accessIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)
            val primaryIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.IS_PRIMARY)

            buildList {
                while (cursor.moveToNext()) {
                    add(
                        CalendarSource(
                            id = cursor.getLong(idIndex),
                            displayName = cursor.getString(nameIndex).orEmpty().ifBlank { "Calendar" },
                            ownerName = cursor.getString(ownerIndex),
                            accessLevel = cursor.getInt(accessIndex),
                            isPrimary = cursor.getInt(primaryIndex) == 1
                        )
                    )
                }
            }
        }.orEmpty()
    }

    fun loadUpcomingEvents(daysAhead: Int = 7): List<CalendarEventItem> {
        val start = LocalDate.now().atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = LocalDate.now().plusDays(daysAhead.toLong()).atTime(LocalTime.MAX).atZone(zoneId).toInstant().toEpochMilli()

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Instances.ALL_DAY
        )

        val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"
        val cursor = CalendarContract.Instances.query(contentResolver, projection, start, end, sortOrder)

        return cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val titleIndex = it.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val beginIndex = it.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endIndex = it.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val locationIndex = it.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_LOCATION)
            val calendarNameIndex = it.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)
            val allDayIndex = it.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)

            buildList {
                while (it.moveToNext()) {
                    add(
                        CalendarEventItem(
                            id = it.getLong(idIndex),
                            title = it.getString(titleIndex).orEmpty().ifBlank { "Untitled event" },
                            location = it.getString(locationIndex),
                            calendarName = it.getString(calendarNameIndex).orEmpty().ifBlank { "Calendar" },
                            startMillis = it.getLong(beginIndex),
                            endMillis = it.getLong(endIndex),
                            allDay = it.getInt(allDayIndex) == 1
                        )
                    )
                }
            }
        }.orEmpty()
    }

    fun addEvent(draft: CalendarDraft): Result<Unit> {
        return runCatching {
            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, draft.calendarId)
                put(CalendarContract.Events.TITLE, draft.title)
                put(CalendarContract.Events.DESCRIPTION, draft.notes)
                put(CalendarContract.Events.DTSTART, draft.startMillis)
                put(CalendarContract.Events.DTEND, draft.endMillis)
                put(CalendarContract.Events.EVENT_TIMEZONE, zoneId.id)
            }

            val eventUri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                ?: error("Calendar insert returned null.")

            val eventId = eventUri.lastPathSegment?.toLongOrNull()
                ?: error("Could not read inserted event id.")

            val reminderValues = ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.MINUTES, 10)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            }
            contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
        }
    }
}
