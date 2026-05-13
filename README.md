# ✨ SideAiEditor

<p align="center">
  <b>EN</b> | <a href="#-sideaieditor-ru">RU</a>
</p>

> An AI text editor that's always at hand. Select text in any app — and instantly translate, rewrite, summarize, or give it a unique style.

---

## 🚀 Features

- **14 text processing modes**: translate, fix, rephrase, summarize, analyze, plus fun styles (tribal, viking, biblical, zen, corporate, and more)
- **System integration**: appears in the text selection menu of any Android app (`ACTION_PROCESS_TEXT`)
- **Voice assistant**: supports `ACTION_VOICE_ASSIST` — speak, and AI will process it
- **Two operation modes**:
  - 🖥️ **Local Backend** — your own Ktor server that proxies requests to AI providers (API key stays on the server)
  - 🌐 **OpenRouter Direct** — direct access to [OpenRouter](https://openrouter.ai/) or [NVIDIA NIM](https://build.nvidia.com/explore/discover) using your own key and chosen model
- **Dark theme** in Telegram style on Jetpack Compose

---

## 🏗️ Architecture

```
side/
├── 📱 Android App (root)       — Jetpack Compose UI, ViewModel, SharedPreferences
├── 🖥️ Backend (:backend)       — Ktor + Netty, proxy to OpenRouter / NVIDIA NIM API
```

- **Android**: clean architecture (`data` → `domain` → `presentation` → `ui`)
- **Backend**: single file `Application.kt` — server, routing, and HTTP client to AI providers

---

## 🛠️ Quick Start

### Android App

```bash
# Build debug APK (emulator)
./gradlew assembleDebug

# Build for a physical device on your local network
./gradlew assembleDebug -PAI_BACKEND_URL=http://192.168.1.50:8080
```

By default the backend points to `http://10.0.2.2:8080` (emulator loopback).

### Backend

```bash
# Set your key and run
export OPENROUTER_API_KEY="sk-or-v1-..."
export OPENROUTER_MODEL="openai/gpt-4o-mini"
./gradlew :backend:run
```

Server starts at `http://localhost:8080`. Available endpoints:
- `GET /health` — health check
- `POST /api/text/process` — text processing (`{ "text": "...", "mode": "style" }`)

---

## 🧰 Tech Stack

| Component | Technology |
|---|---|
| Language | Kotlin 2.2.20 |
| Build | Gradle 8.13 |
| Android UI | Jetpack Compose 1.6.2, Material3 |
| Backend | Ktor 3.3.1 (Netty) |
| Serialization | Kotlinx Serialization JSON |
| Networking (Android) | `HttpURLConnection` |
| Networking (Backend) | Ktor Client |
| Min. Android | API 29 (Android 10) |

---

## 🤖 Authors

Almost all of the code was written by **KimiClaw** and **HermesBot**.

---

## 📝 Notes

- The app uses `android:usesCleartextTraffic="true"` to communicate with the local backend over HTTP.
- In **OpenRouter Direct** mode the key is stored in `SharedPreferences` on the device.
- The backend enables CORS (`anyHost()`) — fine for local development, restrict for production.

---

<p align="center">Made with ☕ and 🤖</p>

---

# ✨ SideAiEditor [RU]

> AI-редактор текста, который всегда под рукой. Выдели текст в любом приложении — и мгновенно переведи, перепиши, сделай краткое содержание или придай ему необычный стиль.

---

## 🚀 Возможности

- **14 режимов обработки текста**: перевод, исправление, перефразирование, саммари, анализ, а также забавные стили (трайбл, викинг, библейский, дзен, корпоративный и другие)
- **Системная интеграция**: появляется в меню выделения текста любого Android-приложения (`ACTION_PROCESS_TEXT`)
- **Голосовой помощник**: поддержка `ACTION_VOICE_ASSIST` — говори, а AI обработает
- **Два режима работы**:
  - 🖥️ **Local Backend** — свой Ktor-сервер, который проксирует запросы к AI-провайдерам (API-ключ хранится на сервере)
  - 🌐 **OpenRouter Direct** — прямое обращение к [OpenRouter](https://openrouter.ai/) или [NVIDIA NIM](https://build.nvidia.com/explore/discover) с вашим ключом и выбранной моделью
- **Тёмная тема** в стиле Telegram на Jetpack Compose

---

## 🏗️ Архитектура

```
side/
├── 📱 Android App (root)       — Jetpack Compose UI, ViewModel, SharedPreferences
├── 🖥️ Backend (:backend)       — Ktor + Netty, прокси к OpenRouter / NVIDIA NIM API
```

- **Android**: чистая архитектура (`data` → `domain` → `presentation` → `ui`)
- **Backend**: один файл `Application.kt` — сервер, роутинг и HTTP-клиент к AI-провайдерам

---

## 🛠️ Быстрый старт

### Android-приложение

```bash
# Сборка debug APK (эмулятор)
./gradlew assembleDebug

# Сборка для физического устройства в локальной сети
./gradlew assembleDebug -PAI_BACKEND_URL=http://192.168.1.50:8080
```

По умолчанию бэкенд указывает на `http://10.0.2.2:8080` (эмулятор).

### Бэкенд

```bash
# Укажи ключ и запусти
export OPENROUTER_API_KEY="sk-or-v1-..."
export OPENROUTER_MODEL="openai/gpt-4o-mini"
./gradlew :backend:run
```

Сервер поднимется на `http://localhost:8080`. Доступные эндпоинты:
- `GET /health` — проверка живости
- `POST /api/text/process` — обработка текста (`{ "text": "...", "mode": "style" }`)

---

## 🧰 Технологический стек

| Компонент | Технология |
|---|---|
| Язык | Kotlin 2.2.20 |
| Сборка | Gradle 8.13 |
| Android UI | Jetpack Compose 1.6.2, Material3 |
| Backend | Ktor 3.3.1 (Netty) |
| Сериализация | Kotlinx Serialization JSON |
| Сеть (Android) | `HttpURLConnection` |
| Сеть (Backend) | Ktor Client |
| Мин. Android | API 29 (Android 10) |

---

## 🤖 Авторы

Почти весь код был написан **KimiClaw** и **HermesBot**.

---

## 📝 Примечания

- Приложение использует `android:usesCleartextTraffic="true"` для общения с локальным бэкендом по HTTP.
- В режиме **OpenRouter Direct** ключ хранится в `SharedPreferences` на устройстве.
- Бэкенд включает CORS (`anyHost()`) — для локальной разработки это нормально, для продакшена стоит ограничить.

---

<p align="center">Сделано с ☕ и 🤖</p>
