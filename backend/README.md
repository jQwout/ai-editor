# Side AI Editor backend

Ktor proxy for OpenRouter. Keep the OpenRouter key here, never in the Android app.

## Run

PowerShell:

```powershell
$env:OPENROUTER_API_KEY="sk-or-v1-..."
$env:OPENROUTER_MODEL="openai/gpt-4o-mini"
.\gradlew.bat :backend:run
```

Android emulator default backend URL is:

```text
http://10.0.2.2:8080
```

For a physical device, run the backend on a reachable LAN IP and pass it to Gradle:

```powershell
.\gradlew.bat assembleDebug -PAI_BACKEND_URL=http://192.168.1.50:8080
```

## Endpoint

```http
POST /api/text/process
Content-Type: application/json

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
