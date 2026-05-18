# Models Fetch Feature — SPEC

**Document**: Backend Models API + Frontend Loader
**Status**: Draft
**Date**: 2025-01-14

---

## 1. Задача

Получать список доступных AI моделей с бекенда. Если бек не отвечает за 5 сек — использовать захардкоженный fallback.

---

## 2. Backend Changes

### 2.1 New Endpoint

```
GET /api/models
```

**Response** (200 OK):
```json
{
  "models": [
    {
      "id": "meta/llama-3.1-70b-instruct",
      "displayName": "Llama 3.1 70B Instruct",
      "provider": "nvidia"
    },
    {
      "id": "qwen/qwen3-coder-480b-a35b-instruct",
      "displayName": "Qwen3 Coder 480B",
      "provider": "nvidia"
    }
  ]
}
```

### 2.2 Implementation

New file: `backend/src/main/kotlin/.../ModelsRoutes.kt`

```kotlin
fun Route.modelsRoutes() {
    get("/api/models") {
        call.respond(
            ModelsResponse(
                models = listOf(
                    ModelInfo("meta/llama-3.1-70b-instruct", "Llama 3.1 70B Instruct", "nvidia"),
                    ModelInfo("qwen/qwen3-coder-480b-a35b-instruct", "Qwen3 Coder 480B", "nvidia")
                )
            )
        )
    }
}
```

Register in `Application.kt`:
```kotlin
routing {
    modelsRoutes()
    // existing routes...
}
```

---

## 3. Frontend Changes

### 3.1 Backend URL Source

Use the same `customBackendUrl` from settings that is used for AI text processing.

### 3.2 New State

```kotlin
// SettingsScreen.kt
data class ModelsFetchState(
    val models: List<ModelInfo> = NvidiaModels.hardcoded,
    val isLoading: Boolean = true,
    val error: String? = null,
    val source: ModelSource = ModelSource.FALLBACK // FALLBACK | BACKEND
)

enum class ModelSource { FALLBACK, BACKEND }
```

### 3.2 Loading UI

While `isLoading == true`, show:
- Skeleton/shimmer on model dropdown
- "Loading models..." text

### 3.3 Fetch Logic

```kotlin
suspend fun fetchModels(): Result<List<ModelInfo>> {
    return withTimeoutOrNull(5000L) {
        httpClient.get("backendUrl/api/models")
            .body<ModelsResponse>()
            .models
    }?.let { Result.success(it) }
    ?: Result.failure(TimeoutException("Backend timeout"))
}
```

### 3.4 Flow

1. `isLoading = true`
2. Launch coroutine → fetch from backend
3. If success → `models = response`, `source = BACKEND`
4. If timeout/error → keep `models = NvidiaModels.hardcoded`, `source = FALLBACK`
5. `isLoading = false`

### 3.5 Visual Indicator

Show small badge next to model name:
- `BACKEND` badge (green) — models loaded from server
- No badge or `DEFAULT` badge — using fallback

---

## 4. Files to Modify

### Backend
- `backend/src/main/kotlin/.../backend/Application.kt` — register routes
- New: `backend/src/main/kotlin/.../backend/ModelsRoutes.kt`

### Frontend
- `src/main/kotlin/.../ui/SettingsScreen.kt` — add loading state + fetch logic
- `src/main/kotlin/.../network/AiApiClient.kt` — add models endpoint

---

## 5. Acceptance Criteria

| ID | Criteria |
|----|----------|
| AC1 | On screen open → show loader immediately |
| AC2 | Backend responds < 5s → use backend models |
| AC3 | Backend timeout/error → use hardcoded NvidiaModels |
| AC4 | Model dropdown populated correctly |
| AC5 | Visual indicator shows source (backend/fallback) |
| AC6 | Works offline — shows fallback immediately |

---

## 6. Hardcoded Fallback

```kotlin
object NvidiaModels {
    data class ModelInfo(
        val id: String,
        val displayName: String
    )
    
    val hardcoded = listOf(
        ModelInfo("meta/llama-3.1-70b-instruct", "Llama 3.1 70B Instruct"),
        ModelInfo("qwen/qwen3-coder-480b-a35b-instruct", "Qwen3 Coder 480B")
    )
    
    fun getDisplayName(modelId: String): String = 
        hardcoded.find { it.id == modelId }?.displayName ?: modelId
}
```