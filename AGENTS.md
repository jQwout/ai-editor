<!-- From: c:\Users\jqwou\AndroidStudioProjects\side\AGENTS.md -->
# SideAiEditor — Agent Guide

This document describes the project structure, build process, and conventions for AI coding agents working on this codebase.

---

## Project Overview

**SideAiEditor** is a multi-module Gradle project consisting of:

1. **Android Application** (root module) — an AI-powered text editing app with a Jetpack Compose UI.
2. **Kotlin/JVM Backend** (`backend` module) — a Ktor-based HTTP proxy server that forwards text processing requests to the OpenRouter AI API, keeping the API key server-side.

The app integrates with the Android system as an `ACTION_PROCESS_TEXT` handler (appears in the text selection menu of other apps) and as a voice assistant (`ACTION_VOICE_ASSIST`).

The app supports two API modes:
- **Local Backend** — sends requests to the local Ktor backend server.
- **OpenRouter Direct** — sends requests directly to `https://openrouter.ai/api/v1/chat/completions` using the user's own API key and chosen model.

The mode, API key, model, and backend URL are configurable via an in-app Settings screen and persisted in `SharedPreferences`.

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
| Android UI | Jetpack Compose 1.6.2, Material3 1.2.0 (Telegram-style dark theme) |
| Backend Framework | Ktor 3.3.1 (Client + Server, Netty engine) |
| Backend Serialization | Kotlinx Serialization JSON |
| Backend Logging | Logback Classic 1.5.13 |
| Android Networking | `HttpURLConnection` (no Retrofit/OkHttp) |
| Android JSON | `org.json.JSONObject` (built-in) |

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
│       │   ├── SideAppRoot.kt                  # Root composable (launches TextStyler directly)
│       │   └── TextProcessorApplication.kt
│       └── openqwoutt/textstyler/              # Text Styler mini-app
│           ├── TextStylerMiniApp.kt            # Entry point (wires VM + Screen)
│           ├── data/
│           │   └── settings/
│           │       ├── ApiMode.kt              # LOCAL_BACKEND / OPENROUTER_DIRECT
│           │       ├── AppSettings.kt          # Data class for settings
│           │       └── SettingsRepository.kt   # SharedPreferences wrapper
│           ├── domain/
│           │   ├── Models.kt                   # StyleMode enum (14 modes), ModeGroup, Result
│           │   └── TextProcessorUseCase.kt     # Business logic + HTTP to backend or OpenRouter
│           ├── presentation/
│           │   └── TextStylerViewModel.kt      # StateFlow-based VM
│           └── ui/
│               ├── TextStylerScreen.kt         # Main Compose UI
│               ├── SettingsScreen.kt           # Settings UI (mode, key, model, URL)
│               ├── TextStylerActivity.kt       # ACTION_PROCESS_TEXT handler
│               └── VoiceAssistActivity.kt      # ACTION_VOICE_ASSIST handler
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

The default `AI_BACKEND_URL` is `http://10.0.2.2:8080` (Android emulator loopback). It is injected into `BuildConfig.AI_BACKEND_URL` at compile time and used as the default backend URL in Settings.

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

The app follows a layered structure:

- **`data/`** — Data layer: `SettingsRepository` wraps `SharedPreferences`.
- **`domain/`** — Pure Kotlin business logic, models, and use cases.
- **`presentation/`** — ViewModels exposing state (`StateFlow` or Compose `State`) and handling user actions via sealed classes (`TextStylerAction`).
- **`ui/`** — Compose screens and activities. UI is themed with custom dark color palettes.

The app shell (`openqwoutt.textprocessor.app`) no longer hosts a home screen or navigation; it launches `TextStylerMiniApp` directly.

---

## Key Domain Details

### Text Styler — `StyleMode`

There are 14 processing modes organized into three `ModeGroup`s:

- **MAIN**: `TRANSLATE`, `STYLE`, `FIX`
- **STYLE**: `FORMAL`, `SHORT`, `TRIBAL`, `CORP`, `BIBLICAL`, `VIKING`, `ZEN`
- **ACTION**: `OLD_EMOJI`, `SUMMARIZE`, `ANALYZE`, `SCREENSHOT`

Each mode has an `id`, `displayName`, `shortName`, `icon`, `group`, `prompt`, and `temperature`. The prompt is sent to the backend/OpenRouter and appended to a base system prompt. Input text is cleaned (trimmed, whitespace collapsed, control chars stripped) and capped at 3000 characters.

### API Modes

- **Local Backend** — `TextProcessorUseCase` POSTs to `backendUrl/api/text/process` with `{ text, mode }`.
- **OpenRouter Direct** — `TextProcessorUseCase` POSTs to `https://openrouter.ai/api/v1/chat/completions` with the user's API key, chosen model, and a chat completion payload built from the mode's prompt.

### Settings

`SettingsScreen` allows the user to:
- Toggle between **Local Backend** and **OpenRouter Direct**.
- Enter an **OpenRouter API Key** (hidden input).
- Select a **model** from a dropdown of free models or enter a custom one:
  - `google/gemini-2.0-flash-exp:free`
  - `deepseek/deepseek-chat:free`
  - `meta-llama/llama-3.1-8b-instruct:free`
  - `nousresearch/hermes-3-llama-3.1-405b:free`
- Set the **Backend URL** (visible only in Local Backend mode).

Settings are saved to `SharedPreferences` via `SettingsRepository`.

### System Integration

- **`ACTION_PROCESS_TEXT`** — `TextStylerActivity` appears in the text-selection menu of other apps. It shows a quick-action sheet for MAIN + SUMMARIZE + ANALYZE modes and returns the processed text (or copies it to clipboard in read-only contexts).
- **`ACTION_VOICE_ASSIST`** — `VoiceAssistActivity` handles voice assistant invocations. If no spoken text is present in the intent, it launches the system speech recognizer, then presents the same quick-action sheet and copies the result to the clipboard.

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
- When using **OpenRouter Direct**, the user's API key is stored in `SharedPreferences` on the device. This is acceptable for personal use but should be noted for any production hardening.
- The backend keeps the **OpenRouter API key server-side** only as an environment variable.
- The backend enables **CORS with `anyHost()`** — acceptable for local development but should be restricted for production deployment.

---

## IDE & Environment

- Developed in **Android Studio / IntelliJ IDEA**.
- `gradle.properties` pins `org.gradle.java.home` to the Android Studio bundled JBR path. This is machine-specific and may need adjustment on other workstations.
- Kotlin code style is set to `official`.

---

## Localization Notes

The app UI text is primarily in **English**, but some strings in ViewModels are in **Russian** (e.g., error messages). When adding new user-facing text, follow the existing mixed pattern or consider moving strings to resources if full localization is desired.
