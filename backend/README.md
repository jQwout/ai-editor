# Side AI Editor backend

Ktor proxy for OpenRouter. Keep the OpenRouter key here, never in the Android app.

## Run

PowerShell (OpenRouter — по умолчанию, `LLM_PROVIDER` можно не задавать):

```powershell
$env:OPENROUTER_API_KEY="sk-or-v1-..."
$env:OPENROUTER_MODEL="openai/gpt-4o-mini"
.\gradlew.bat :backend:run
```

### NVIDIA (NIM / API catalog, OpenAI-совместимый chat)

```powershell
$env:LLM_PROVIDER="nvidia"
$env:NVIDIA_API_KEY="nvapi-..."
# опционально:
# $env:NVIDIA_CHAT_COMPLETIONS_URL="https://integrate.api.nvidia.com/v1/chat/completions"
# $env:NVIDIA_MODEL="meta/llama-3.1-8b-instruct"
.\gradlew.bat :backend:run
```

Переменные:

- `LLM_PROVIDER` — `openrouter` (по умолчанию) или `nvidia` / `nim`.
- Для prompt proxy модель по умолчанию: `LLM_PROMPT_PROXY_MODEL` или устаревшее `OPENROUTER_PROMPT_PROXY_MODEL`, иначе `OPENROUTER_MODEL` / `NVIDIA_MODEL` в зависимости от провайдера.

### Prompt proxy: `POST /api/prompt/proxy`

Тело JSON: `style`, `prompt`, `language`, опционально `model`, опционально **`stream`** (по умолчанию `false`).

- **`stream: false` или поле не передано** — ответ `200` с JSON `{"result":"..."}`.
- **`stream: true`** — ответ `Content-Type: text/event-stream` (SSE). Строки `data:` с JSON `{"text":"<дельта>"}`; в конце при успехе — `event: done` и `data: {}`. Ошибка валидации до вызова LLM — по-прежнему **`400` + JSON** с полем `error`. Ошибка upstream при стриминге — **`event: error`** и в `data` JSON того же вида, что и для нестриминговых ошибок (в т.ч. полное тело провайдера в `providerRaw` при неуспешном HTTP).

### Тесты (`:backend:test`)

Юнит‑тесты: `ChatCompletionsLlmAdapter`, `ChatCompletionsPromptProxyCompleter` (sync + mock SSE), `PromptProxyService`, `PromptProxyRoutes`, конфиги OpenRouter/NVIDIA, `ProcessTextUseCase`, `TextProcessingRoutes`, обрезка диагностик.

```powershell
.\gradlew.bat :backend:test
```

Если Gradle падает с `Could not find or load main class worker.org.gradle.process.internal.worker.GradleWorkerMain`, это сбой classpath воркера Gradle/JDK на машине (не код тестов). Имеет смысл запустить тесты из **Android Studio / IntelliJ** по модулю `backend`, либо переустановить/обновить Gradle wrapper и использовать **JDK 17** для Gradle.

## Prompt store (админская загрузка промптов в Postgres)

Это **новая** БД/таблица для промптов, отдельная от `repoindex`. Предназначено для загрузки промптов из разных репозиториев (с разным форматом), с сохранением “лишних” полей.

### Включение

Переменные окружения:

- `PROMPT_STORE_ENABLED=true`
- `PROMPT_ADMIN_TOKEN` — спец‑ключ. **Без него** админские методы считаются выключенными (вернётся `404`).
- `PROMPT_DB_JDBC_URL` — например `jdbc:postgresql://127.0.0.1:5432/repoindex`
- `PROMPT_DB_USER`
- `PROMPT_DB_PASSWORD`

При старте backend автоматически применяются Flyway миграции из:

- `backend/src/main/resources/db/promptstore/`

Таблица истории Flyway отдельная: `flyway_promptstore_schema_history`.

### Авторизация

Только заголовок:

```text
Authorization: Bearer <PROMPT_ADMIN_TOKEN>
```

Если токен не совпал — `401 Unauthorized`.

### Endpoints

#### `POST /admin/prompt` — upsert одного промпта

Минимальный JSON:

```json
{
  "modeId": "style",
  "modelId": null,
  "promptText": "Rewrite ...",
  "temperature": 0.4,
  "isEnabled": true
}
```

Расширяемые поля (чтобы не ломаться от разных репо):

- `tags: string[]`
- `origin: object` — откуда промпт (repo/path/sha и т.п.)
- `meta: object` — произвольная мета (например `formatVersion`)
- `raw: object` — любые дополнительные поля (сохраняются как есть)

Пример:

```json
{
  "modeId": "style",
  "promptText": "Rewrite it",
  "temperature": 0.55,
  "isEnabled": true,
  "tags": ["v1", "default"],
  "origin": { "repo": "github.com/org/repo", "path": "prompts/style.json", "sha": "abc123" },
  "meta": { "formatVersion": 1 },
  "raw": { "anyExtra": "kept" }
}
```

`modelId`:
- `null` → **base промпт** для режима (уникальность по `modeId`)
- строка → **override** для конкретной модели (уникальность по `(modeId, modelId)`)

Response:

```json
{
  "status": "ok",
  "promptId": "3957d885-e567-4836-800e-021d63836a34",
  "upserted": true
}
```

`upserted=true` — запись вставлена, `false` — обновили существующую.

#### `POST /admin/prompts` — upsert пачки промптов

```json
{
  "prompts": [
    { "modeId": "style", "modelId": "m1", "promptText": "p1", "temperature": 0.4, "isEnabled": true },
    { "modeId": "style", "modelId": "m2", "promptText": "p2", "temperature": 0.4, "isEnabled": true }
  ],
  "origin": { "repo": "github.com/org/repoBatch", "sha": "zzz" },
  "raw": { "sourceFormat": "schemaV3" }
}
```

`origin`/`raw` из envelope будут применены к элементам, у которых эти поля пустые.

Response:

```json
{ "status": "ok", "upserted": 2 }
```

`upserted` — сколько промптов из пачки применено (вставка или обновление). Все операции выполняются в **одной транзакции**: при ошибке на любом элементе откатываются все.

### Пример вызова (PowerShell)

```powershell
$token = "secret"
$body = '{"modeId":"style","promptText":"Rewrite it","temperature":0.55,"isEnabled":true}'
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8080/admin/prompt `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json" `
  -Body $body
```

### Проверка в Postgres

```sql
select mode_id, model_id, temperature, is_enabled, prompt_text
from prompt_store_prompts
order by updated_at desc
limit 20;
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
