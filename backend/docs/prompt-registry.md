# Prompt Registry (models → prompts)

Сервис хранит **только**:
- список моделей OpenRouter (`ai_model`)
- базовые промты по режимам (`prompt`)
- опциональные overrides для пары (модель × режим) (`model_prompt`)

Хранения репозиториев / git-клонов нет.

## Включение

Фича включается переменной окружения:
- `REPO_INDEX_ENABLED=true`

При включении поднимаются HTTP-роуты `/repoindex/*` и становится доступен режим `/api/text/process` с полем `model`.

## Конфигурация (env)

- `PG_JDBC_URL` — например `jdbc:postgresql://localhost:5432/repoindex`
- `PG_USER`
- `PG_PASSWORD`
- `REPO_INDEX_ADMIN_TOKEN` — *опционально*, включает `POST /repoindex/admin/*` (Bearer)
- `OPENROUTER_API_KEY` — обязателен для вызовов OpenRouter
- `OPENROUTER_MODEL` — fallback-модель, если клиент **не** передал поле `model` в `/api/text/process`

## Как выбирается промт

Для входной пары `(model_id, mode_id)` выбирается “эффективный” промт:
1) если есть строка в `model_prompt` — используются `prompt_text_override`/`temperature_override` (если они `NULL`, поле берётся из базы)
2) иначе используется базовый `prompt` по `mode_id`

## API

### `GET /repoindex/health`

Ответ:

```json
{ "status": "ok" }
```

### `GET /repoindex/models`

Возвращает включённые модели из `ai_model`:

```json
[
  { "modelId": "google/gemini-2.0-flash-exp:free", "displayName": "Gemini 2.0 Flash (free)" }
]
```

### `GET /repoindex/prompts?model=<model_id>`

Возвращает список “эффективных” промтов по всем `mode_id`, известным в таблице `prompt`:

```json
{
  "modelId": "google/gemini-2.0-flash-exp:free",
  "prompts": [
    { "modeId": "style", "promptText": "...", "temperature": 0.4 }
  ]
}
```

Ошибки:
- `400` если параметр `model` не задан
- `404` если модель не найдена или выключена

### `GET /repoindex/prompt?model=<model_id>&mode=<mode_id>`

Возвращает 1 промт для пары модель × режим:

```json
{
  "modelId": "google/gemini-2.0-flash-exp:free",
  "modeId": "style",
  "promptText": "...",
  "temperature": 0.4
}
```

Ошибки:
- `400` если `model` или `mode` не задан
- `404` если модель/режим не найден или модель выключена

### `POST /repoindex/admin/models` (опционально)

Требует:
- `REPO_INDEX_ADMIN_TOKEN` в env
- заголовок `Authorization: Bearer <token>`

Тело:

```json
{
  "models": [
    { "modelId": "deepseek/deepseek-chat:free", "displayName": "DeepSeek Chat (free)", "isEnabled": true }
  ]
}
```

### `POST /repoindex/admin/prompts` (опционально)

Требует те же условия admin-доступа.

Тело:

```json
{
  "basePrompts": [
    { "modeId": "style", "promptText": "Rewrite ...", "temperature": 0.4 }
  ],
  "overrides": [
    {
      "modelId": "google/gemini-2.0-flash-exp:free",
      "modeId": "style",
      "promptTextOverride": null,
      "temperatureOverride": 0.5
    }
  ]
}
```

## `/api/text/process` и модель

### Запрос

```http
POST /api/text/process
Content-Type: application/json

{
  "text": "Hello",
  "mode": "style",
  "model": "google/gemini-2.0-flash-exp:free"
}
```

`model`:
- **если задан**: модель + промт берутся из Postgres (через реестр). Если реестр выключен — будет `503`.
- **если не задан**: используется `OPENROUTER_MODEL` и встроенные промты backend (enum `StyleMode` в `Application.kt`).

## Схема БД / миграции

- Flyway: `backend/src/main/resources/db/migration/`
- Таблицы: `ai_model`, `prompt`, `model_prompt`
- Сид-данные: создаются в `V2__prompt_registry.sql`

## Публичные промты из реп (НЕ курированный реестр)

Курированный реестр (`ai_model`/`prompt`/`model_prompt`) **не трогаем**: он предназначен для управляемых промтов по режимам и per-model overrides.

Для промтов, извлечённых индексатором из **публичных репозиториев**, используется отдельная таблица:

- `public_prompt` (миграция `V3__public_prompt.sql`)
  - поля: `source`, `path`, `sha`, `item_key`, `tags[]`, `prompt_text`

`item_key` нужен, чтобы один файл/sha мог давать **несколько** промтов (CSV строки или несколько секций Markdown). Уникальность: `(source, path, sha, item_key)` (см. `V4__public_prompt_item_key.sql`).

### API

#### `GET /repoindex/public/prompts?source=<src>&tag=<tag>&limit=<n>`

Возвращает публичные промты, отсортированные по `updated_at desc`.

Параметры:
- `source` — опционально (например, URL репозитория)
- `tag` — опционально (фильтр по `tags`)
- `limit` — опционально, по умолчанию 50, максимум 500

Ответ:

```json
{
  "prompts": [
    { "source": "https://github.com/org/repo", "path": "prompts/foo.md", "sha": "abc123", "itemKey": "0", "tags": ["sql"], "promptText": "..." }
  ]
}
```

#### `POST /repoindex/admin/public-prompts` (опционально)

Требует те же условия admin-доступа (`REPO_INDEX_ADMIN_TOKEN` + `Authorization: Bearer <token>`).

Тело:

```json
{
  "prompts": [
    { "source": "https://github.com/org/repo", "path": "prompts/foo.md", "sha": "abc123", "itemKey": "0", "tags": ["sql"], "promptText": "..." }
  ]
}
```

#### `POST /repoindex/admin/public-repos/ingest` (опционально)

Триггерит индексирование публичных GitHub-репозиториев на стороне backend:
- скачивает дерево файлов,
- берёт `.md/.markdown/.csv`,
- парсит через единый парсер,
- апсертит в `public_prompt`.

Требует admin-доступа.

Тело:

```json
{
  "repoUrls": [
    "https://github.com/org/repo"
  ]
}
```

#### `POST /repoindex/admin/public-repos/refresh` (опционально)

Ручной “тик” автообновления (удобно для проверки): для всех включённых реп проверяет head-коммит и делает ingest только если коммит изменился.

Требует admin-доступа. Тело не требуется.

## Автообновление публичных реп (раз в 24 часа по коммитам)

Backend может автоматически переиндексировать сохранённый список публичных репозиториев.

### Как включить

Нужно:
- `REPO_INDEX_ENABLED=true` (включает Postgres + роуты)
- `PUBLIC_PROMPT_AUTO_REFRESH=true`

Опционально:
- `PUBLIC_PROMPT_REFRESH_INTERVAL_HOURS=24` (по умолчанию 24)
- `PUBLIC_PROMPT_GITHUB_TOKEN=...` (чтобы не упираться в GitHub rate limits)

### Как задаётся список реп

Список хранится в таблице `public_repo_source` (миграция `V5__public_repo_source.sql`) и управляется админ-API:

- `POST /repoindex/admin/public-repos` с `{ "repoUrls": ["https://github.com/org/repo"] }` — добавить/обновить (включить) репы
- `GET /repoindex/admin/public-repos` — посмотреть текущее состояние (lastSeen/lastIngested)

### Логика обновления “по коммитам”

Раз в интервал:
- для каждой включённой репы backend запрашивает head-коммит ветки `default_branch`
- сохраняет `last_seen_commit`
- если `last_seen_commit != last_ingested_commit` — запускает ingest и после успешного ingest пишет `last_ingested_commit = last_seen_commit`

