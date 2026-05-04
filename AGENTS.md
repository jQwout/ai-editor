# SideAiEditor — Agent Guide

This document describes the project structure, build process, and conventions for AI coding agents working on this codebase.

---

## Project Overview

**SideAiEditor** is a multi-module Gradle project consisting of:

1. **Android Application** (root module) — an AI-powered text editing and calendar planning app with a Jetpack Compose UI.
2. **Kotlin/JVM Backend** (`backend` module) — a Ktor-based HTTP proxy server that forwards text processing requests to the OpenRouter AI API, keeping the API key server-side.

The app is organized as two independent "mini-apps" (Text Styler and Calendar Planner) launched from a common home shell. It also integrates with the Android system as an `ACTION_PROCESS_TEXT` handler, appearing in the text selection menu of other apps.

---

## Technology Stack

| Component | Technology / Version |
|---|---|
| Build System | Gradle 8.13 |
| Language | Kotlin 2.2.20 |
| Android Gradle Plugin | 8.13.1 |
| Android Compile SDK | 36 |
| Min SDK | 29 |
| Target SDK | 36 |
| JVM Target | 17 |
| Android UI | Jetpack Compose 1.6.2, Material3 1.2.0 |
| Backend Framework | Ktor 3.3.1 (Client + Server, Netty engine) |
| Backend Serialization | Kotlinx Serialization JSON |
| Backend Logging | Logback Classic 1.5.13 |
| Android Networking | `HttpURLConnection` (no Retrofit/OkHttp) |
| Android JSON | `org.json.JSONObject` (built-in) |
| Calendar Access | Android `CalendarContract` via `ContentResolver` |

---

## Project Structure

```
side/
├── build.gradle.kts              # Android app build script
├── settings.gradle.kts           # Project name: SideAiEditor, includes :backend
├── gradle.properties             # Gradle JVM args, AndroidX, Kotlin code style
├── local.properties              # Android SDK path (machine-specific)
├── proguard-rules.pro            # Empty (minification disabled)
├── src/main/
│   ├── AndroidManifest.xml
│   └── kotlin/
│       ├── openqwoutt/textprocessor/app/       # App shell
│       │   ├── MainActivity.kt
│       │   ├── SideAppRoot.kt                  # Navigation shell + home screen
│       │   └── TextProcessorApplication.kt
│       ├── openqwoutt/textstyler/              # Text Styler mini-app
│       │   ├── TextStylerMiniApp.kt            # Entry point (wires VM + Screen)
│       │   ├── domain/
│       │   │   ├── Models.kt                   # StyleMode enum (13 modes), ModeGroup, Result
│       │   │   └── TextProcessorUseCase.kt     # Business logic + HTTP to backend
│       │   ├── presentation/
│       │   │   └── TextStylerViewModel.kt      # StateFlow-based VM
│       │   └── ui/
│       │       ├── TextStylerScreen.kt         # Main Compose UI (~388 lines)
│       │       └── TextStylerActivity.kt       # ACTION_PROCESS_TEXT handler
│       └── openqwoutt/calendarplanner/         # Calendar Planner mini-app
│           ├── CalendarPlannerMiniApp.kt       # Entry point + permission launcher
│           ├── domain/
│           │   ├── CalendarModels.kt           # Data classes
│           │   └── CalendarRepository.kt       # ContentResolver queries/inserts
│           ├── presentation/
│           │   └── CalendarPlannerViewModel.kt # State-based VM
│           └── ui/
│               └── CalendarPlannerScreen.kt    # Compose UI (~507 lines)
└── backend/
    ├── build.gradle.kts          # Ktor/JVM build script
    ├── README.md                 # Backend run instructions
    └── src/main/kotlin/openqwoutt/textprocessor/backend/
        └── Application.kt        # Entire backend: server, routing, OpenRouter client
```

---

## Build Commands

### Android App

```powershell
# Build debug APK
.\gradlew.bat assembleDebug

# Build with custom backend URL (for physical device on LAN)
.\gradlew.bat assembleDebug -PAI_BACKEND_URL=http://192.168.1.50:8080
```

The default `AI_BACKEND_URL` is `http://10.0.2.2:8080` (Android emulator loopback). It is injected into `BuildConfig.AI_BACKEND_URL` at compile time.

### Backend

```powershell
# Run the backend server
$env:OPENROUTER_API_KEY="sk-or-v1-..."
$env:OPENROUTER_MODEL="openai/gpt-4o-mini"
.\gradlew.bat :backend:run
```

Backend environment variables:
- `OPENROUTER_API_KEY` — **required**
- `OPENROUTER_MODEL` — defaults to `openai/gpt-4o-mini`
- `OPENROUTER_APP_TITLE` — defaults to `Side AI Editor`
- `OPENROUTER_HTTP_REFERER` — optional
- `PORT` — server port, defaults to `8080`

---

## Backend API

### `GET /health`

Response:
```json
{ "status": "ok" }
```

### `POST /api/text/process`

Request:
```json
{
  "text": "Text to process",
  "mode": "style"
}
```

Response:
```json
{
  "result": "Processed text"
}
```

Valid `mode` values correspond to `StyleMode.id` entries defined in both the Android app (`openqwoutt.miniapp.textstyler.domain.StyleMode`) and the backend (`openqwoutt.textprocessor.backend.StyleMode`). The two enums must stay in sync.

---

## Code Organization Conventions

Each mini-app follows a layered structure:

- **`domain/`** — Pure Kotlin business logic, models, and use cases. No Android framework dependencies except where unavoidable (e.g., `Context` in `CalendarRepository`).
- **`presentation/`** — ViewModels exposing state (`StateFlow` or Compose `State`) and handling user actions via sealed classes (`TextStylerAction`, `CalendarPlannerAction`).
- **`ui/`** — Compose screens and activities. UI is themed with custom dark color palettes per feature.

The app shell (`openqwoutt.textprocessor.app`) hosts navigation via a simple `enum class AppSection` and `rememberSaveable` state.

---

## Key Domain Details

### Text Styler — `StyleMode`

There are 13 processing modes organized into three `ModeGroup`s:

- **MAIN**: `TRANSLATE`, `STYLE`, `FIX`
- **STYLE**: `FORMAL`, `SHORT`, `TRIBAL`, `CORP`, `BIBLICAL`, `VIKING`, `ZEN`
- **ACTION**: `OLD_EMOJI`, `SUMMARIZE`, `ANALYZE`, `SCREENSHOT`

Each mode has an `id`, `displayName`, `shortName`, `icon`, `group`, and `prompt`. The prompt is sent to the backend and appended to a base system prompt. Input text is cleaned (trimmed, whitespace collapsed, control chars stripped) and capped at 3000 characters.

### Calendar Planner

- Reads/writes the device's calendars via `CalendarContract`.
- Requires `READ_CALENDAR` and `WRITE_CALENDAR` permissions at runtime.
- Loads only visible, writable calendars (`CAL_ACCESS_CONTRIBUTOR` or higher).
- Displays upcoming events for the next 7 days.
- Adding an event also creates a 10-minute alert reminder.

---

## Testing

**Current state: no tests exist.**

Testing dependencies are configured in `build.gradle.kts`:
- JUnit 4 (`junit:junit:4.13.2`)
- AndroidX Test JUnit (`androidx.test.ext:junit:1.2.1`)
- Espresso (`androidx.test.espresso:espresso-core:3.6.1`)
- Compose UI Test (`androidx.compose.ui:ui-test-junit4:1.6.2`)
- Compose UI Tooling & Test Manifest (debug)

The backend module also includes `testImplementation(kotlin("test"))`.

There are no `src/test` or `src/androidTest` source directories yet. Manual testing and Compose `@Preview` annotations are the current validation approach.

---

## Security Considerations

- **`android:usesCleartextTraffic="true"`** is enabled in `AndroidManifest.xml` to allow HTTP communication with the local backend. Do **not** ship a production build with this unless the backend is also local-only.
- The **OpenRouter API key never touches the Android app**. It lives only as an environment variable on the backend server.
- The backend enables **CORS with `anyHost()`** — acceptable for local development but should be restricted for production deployment.

---

## IDE & Environment

- Developed in **Android Studio / IntelliJ IDEA**.
- `gradle.properties` pins `org.gradle.java.home` to the IntelliJ bundled JBR path. This is machine-specific and may need adjustment on other workstations.
- Kotlin code style is set to `official`.

---

## Localization Notes

The app UI text is primarily in **English**, but some strings in `SideAppRoot.kt` and user-facing messages in ViewModels are in **Russian** (e.g., home screen subtitle, error messages). When adding new user-facing text, follow the existing mixed pattern or consider moving strings to resources if full localization is desired.
