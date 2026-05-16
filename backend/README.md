# Side AI Editor backend

Ktor proxy for OpenRouter. Keep the OpenRouter key here, never in the Android app.

## Run

PowerShell (OpenRouter — по умолчанию, `LLM_PROVIDER` можно не задавать):

```powershell
$env:OPENROUTER_API_KEY="sk-or-v1-..."
$env:OPENROUTER_MODEL="openai/gpt-4o-mini"
$env:PROMPT_PROXY_API_KEY="your-long-random-proxy-secret"
.\gradlew.bat :backend:run
```

## Docker

Из директории `backend/`:

```powershell
# 1) Подготовить env-файл
Copy-Item .env.docker.example .env.docker

# 2) Собрать образ
docker build -t side-ai-editor-backend:latest .

# 3) Запустить контейнер
docker run --rm -p 8080:8080 --env-file .env.docker side-ai-editor-backend:latest
```

Или через Docker Compose:

```powershell
Copy-Item .env.docker.example .env.docker
docker compose up --build -d
```

Если порт `8080` уже занят, переопредели порт хоста:

```powershell
$env:BACKEND_PORT="18080"
docker compose up --build -d
```

В `docker compose` внутренний порт backend фиксирован на `8080`; меняй только внешний (host) порт через `BACKEND_PORT`.

Проверка:

```powershell
Invoke-RestMethod http://127.0.0.1:8080/health
```

### NVIDIA (NIM / API catalog, OpenAI-совместимый chat)

```powershell
$env:LLM_PROVIDER="nvidia"
$env:NVIDIA_API_KEY="nvapi-..."
$env:PROMPT_PROXY_API_KEY="your-long-random-proxy-secret"
# опционально:
# $env:NVIDIA_CHAT_COMPLETIONS_URL="https://integrate.api.nvidia.com/v1/chat/completions"
# $env:NVIDIA_MODEL="meta/llama-3.1-8b-instruct"
.\gradlew.bat :backend:run
```

Переменные:

- `LLM_PROVIDER` — `openrouter` (по умолчанию) или `nvidia` / `nim`.
- **`PROMPT_PROXY_API_KEY`** — при включённом LLM обязателен для регистрации `POST /api/prompt/proxy`; клиент передаёт `Authorization: Bearer …` (см. раздел Prompt proxy).
- Для prompt proxy модель по умолчанию: `LLM_PROMPT_PROXY_MODEL` или устаревшее `OPENROUTER_PROMPT_PROXY_MODEL`, иначе `OPENROUTER_MODEL` / `NVIDIA_MODEL` в зависимости от провайдера.
- **`NVIDIA_PROXY_ENABLED=true`** — включает отдельную infra-feature NVIDIA proxy.
- **`NVIDIA_PROXY_API_KEY`** — bearer‑секрет для инфраструктурных маршрутов NVIDIA proxy (обязательно при `NVIDIA_PROXY_ENABLED=true`; при отсутствии backend завершит старт с ошибкой конфигурации).
- `NVIDIA_PROXY_UPSTREAM_BASE_URL` — опциональный base URL для будущего passthrough (по умолчанию `https://integrate.api.nvidia.com/v1`).
- `NVIDIA_PROXY_REQUEST_TIMEOUT_MS` (default `120000`) и `NVIDIA_PROXY_CONNECT_TIMEOUT_MS` (default `15000`) — таймауты для будущих upstream вызовов.
- `NVIDIA_PROXY_RATE_LIMIT_PER_MINUTE` — опциональный in-memory rate limit по IP+path для `/api/nvidia/proxy/*` (окно 60 секунд, ответ `429` + `Retry-After: 60`). Если переменная задана, но не является целым `> 0`, backend завершит старт с ошибкой конфигурации.
- `NVIDIA_PROXY_RATE_LIMIT_TRUST_FORWARDED=true` — учитывать `X-Forwarded-For` / `X-Real-IP` для NVIDIA proxy rate-limit (включать только за доверенным reverse proxy).
- **`LLM_RATE_LIMIT_REQUESTS_PER_MINUTE`** — если задано целое **> 0**, включается in‑memory лимит **POST** `/api/text/process` и `/api/prompt/proxy` на комбинацию IP+path (скользящее окно **60 с**). Ответ **`429`** с заголовком **`Retry-After: 60`**: для `/api/prompt/proxy` тело в формате `{"error":{...}}` (как у других ошибок proxy), для `/api/text/process` — `{"error":"..."}`. Для нескольких инстансов нужен внешний лимитер (nginx, API gateway).
- **`LLM_RATE_LIMIT_TRUST_FORWARDED=true`** — учитывать `X-Forwarded-For` / `X-Real-IP` при расчёте IP для лимита. Включайте **только** за доверенным reverse proxy; иначе клиенты смогут подменять IP и обходить лимит.
- **`METRICS_PROMETHEUS_ENABLED=true`** — экспорт Micrometer в формате Prometheus на **`GET /metrics/prometheus`**.
- **`METRICS_SCRAPE_BEARER_TOKEN`** — если задан, для scrape нужен заголовок `Authorization: Bearer <token>` (сравнение constant-time). Имеет смысл вместе с firewall / private network.

**CORS:** по умолчанию плагин CORS **выключен** (см. `installCorsFromEnvironment` в коде). Для браузерной админки задайте **`CORS_ALLOWED_ORIGINS`** со списком доверенных origin; не используйте **`CORS_ALLOW_ANY=true`** в публичном интернете.

### Prompt proxy: `POST /api/prompt/proxy`

Если LLM включён (OpenRouter/NVIDIA), для этого endpoint обязательна переменная **`PROMPT_PROXY_API_KEY`**. Без неё маршрут **не регистрируется** (в лог пишется предупреждение). Клиенты передают тот же ключ в заголовке:

```text
Authorization: Bearer <PROMPT_PROXY_API_KEY>
```

При неверном или отсутствующем заголовке — **`401 Unauthorized`** с телом `{"error":{"message":"Unauthorized.",...}}` (тот же контракт, что и для `error` в других ответах proxy).

Тело JSON: `style`, `prompt`, `language`, опционально `model`, опционально **`stream`** (по умолчанию `false`).

- **`stream: false` или поле не передано** — ответ `200` с JSON `{"result":"..."}`.
- **`stream: true`** — ответ `Content-Type: text/event-stream` (SSE). Строки `data:` с JSON `{"text":"<дельта>"}`; в конце при успехе — `event: done` и `data: {}`. Ошибка валидации до вызова LLM — по-прежнему **`400` + JSON** с полем `error`. Ошибка upstream при стриминге — **`event: error`** и в `data` JSON того же вида, что и для нестриминговых ошибок. Поле **`providerRaw`** может быть усечено; тогда **`providerRawTruncated`: true** (лимит длины см. `ProcessTextErrorDiagnostics` в коде).

### NVIDIA proxy infra (этап 1)

Фича выключена по умолчанию и включается отдельно:

```powershell
$env:NVIDIA_PROXY_ENABLED="true"
$env:NVIDIA_PROXY_API_KEY="your-long-random-nvidia-proxy-secret"
.\gradlew.bat :backend:run
```

Инфраструктурные маршруты:

- `GET /api/nvidia/proxy/health` — проверка готовности фичи.
- `GET /api/nvidia/proxy/capabilities` — текущие возможности/планы.
- `POST /api/nvidia/proxy/stub` — временная заглушка (**`501 Not Implemented`**) до добавления конкретного passthrough endpoint.

Для всех маршрутов обязателен заголовок:

```text
Authorization: Bearer <NVIDIA_PROXY_API_KEY>
```

Если токен неверный/отсутствует — **`401 Unauthorized`** с JSON `{"error":{"message":"Unauthorized.","code":"unauthorized"}}`. Схема `Bearer` читается без учёта регистра (`Bearer`/`bearer`).

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

Максимум **1000** записей в `prompts` за один запрос; при превышении — **`400 Bad Request`** с полем `error` в теле JSON.

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

## Models catalog (NVIDIA, отдельный сервис)

Документация: `backend/docs/models-catalog-service.md`.

Включение: `MODELS_CATALOG_ENABLED=true` + Postgres (`MODELS_CATALOG_PG_*`).

На dev: `MODELS_CATALOG_DEV_FALLBACK_PG=true` разрешает fallback на `PG_*` (как repoindex).

- `GET /models-catalog/models` — каталог NVIDIA (ETag `"nvidia-<ms>-<count>-<maxId>"` / `304`)
- `POST /models-catalog/admin/sync` — атомарный snapshot от бота (`MODELS_CATALOG_ADMIN_TOKEN` + Bearer)
- Опционально: `MODELS_CATALOG_SYNC_RATE_LIMIT_PER_MINUTE` — rate limit на sync

## Prompt registry (models / prompts in Postgres)

Документация: `backend/docs/prompt-registry.md`.

Включение: `REPO_INDEX_ENABLED=true` + Postgres env vars.

HTTP‑клиент к GitHub (ингест публичных репозиториев): таймауты задаются переменными **`REPO_INDEX_HTTP_REQUEST_TIMEOUT_MS`** (по умолчанию 120000) и **`REPO_INDEX_HTTP_CONNECT_TIMEOUT_MS`** (по умолчанию 15000).

Админские bulk‑операции (`/repoindex/admin/*`): не более **1000** элементов в одном запросе (списки `models`, сумма `basePrompts`+`overrides`, `prompts` в public‑prompts, `repoUrls` в ingest/upsert).

Фоновый **`PUBLIC_PROMPT_AUTO_REFRESH`**: при **нескольких** инстансах backend с общей БД включайте автообновление **только на одном** процессе (иначе параллельные ingest). Распределённый lock в коде не используется.

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
