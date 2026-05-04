package openqwoutt.miniapp.calendarplanner

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import openqwoutt.miniapp.calendarplanner.presentation.CalendarPlannerViewModel
import openqwoutt.miniapp.calendarplanner.ui.CalendarPlannerScreen

@Composable
fun CalendarPlannerMiniApp(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel = remember { CalendarPlannerViewModel(context) }
    val state by viewModel.state

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.READ_CALENDAR] == true &&
            result[Manifest.permission.WRITE_CALENDAR] == true
        viewModel.onPermissionsChanged(granted)
    }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        CalendarPlannerScreen(
            state = state,
            onAction = viewModel::handle,
            onRequestPermissions = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_CALENDAR,
                        Manifest.permission.WRITE_CALENDAR
                    )
                )
            },
            onNavigateBack = onNavigateBack
        )
    }
}
