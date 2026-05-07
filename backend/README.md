# Side AI Editor backend

Ktor proxy for OpenRouter. Keep the OpenRouter key here, never in the Android app.

## Run

PowerShell:

```powershell
$env:OPENROUTER_API_KEY="sk-or-v1-..."
$env:OPENROUTER_MODEL="openai/gpt-4o-mini"
.\gradlew.bat :backend:run
```

## Tests

On some Windows setups the Gradle test worker can fail to start. A reliable workaround is to run backend tests in a Linux container:

```powershell
docker run --rm -v ${PWD}\backend:/app -w /app gradle:8.13-jdk17 gradle test
```

Note: if you run the backend as `gradle run` from a bind-mounted folder and in parallel start another Gradle build in a container on the same mount, you can hit a `.gradle` lock. Avoid running multiple Gradle processes against the same mounted `/app`, or set an isolated `GRADLE_USER_HOME` for one of them.

## Prompt registry (models / prompts in Postgres)

Документация: `backend/docs/prompt-registry.md`.

Включение: `REPO_INDEX_ENABLED=true` + Postgres env vars.

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
  "mode": "style",
  "model": "google/gemini-2.0-flash-exp:free"
}
```

`model` is optional. If omitted, the server uses `OPENROUTER_MODEL` and built-in prompts. If set, the prompt registry must be enabled and the model must exist in Postgres.

Response:

```json
{
  "result": "Processed text"
}
```
