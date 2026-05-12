# Feature: Cancel Request

## Задача
Добавить возможность отмены запроса к AI, если пользователь передумал.

---

## Дизайн (ASCII)

### Состояние: Idle (нет запроса)
```
┌──────────────────────────────────┐
│ [Input Text Field]               │
│                                  │
│ [Mode Selector]                  │
│                                  │
│ [Send Button]         [Cancel?] │  <- Cancel скрыт
└──────────────────────────────────┘
```

### Состояние: Loading (запрос идёт)
```
┌──────────────────────────────────┐
│ [Input Text Field]               │
│                                  │
│ [Mode Selector]                  │
│                                  │
│ [Spinner...]         [Cancel ✕] │  <- Cancel visible
│ [Streaming text...]             │
└──────────────────────────────────┘
```

### Состояние: Result (ответ получен)
```
┌──────────────────────────────────┐
│ [Input Text Field]               │
│                                  │
│ [Mode Selector]                  │
│                                  │
│ [Send Button]                    │
│ [Response text...]              │
│ [Copy] [Retry]                  │
└──────────────────────────────────┘
```

---

## Логика

### ViewModel states (псевдокод)
```kotlin
sealed class ProcessingState {
    object Idle : ProcessingState()
    data class Loading(val job: Job?) : ProcessingState()  // job для cancel
    data class Streaming(val job: Job?, val text: String) : ProcessingState()
    data class Result(val text: String) : ProcessingState()
    data class Error(val message: String) : ProcessingState()
}

sealed class Action {
    object Cancel : Action()  // новое action
    // ... существующие
}
```

### Handle Cancel
```kotlin
when (action) {
    is Cancel -> {
        currentJob?.cancel()
        currentJob = null
        _state.value = ProcessingState.Idle  // или оставить текст
    }
    // ...
}
```

### UI (SettingsScreen-style кнопка)
```kotlin
if (state is ProcessingState.Loading || state is ProcessingState.Streaming) {
    TextButton(onClick = { viewModel.onAction(Action.Cancel) }) {
        Text("Cancel ✕", color = TextSecondary)
    }
}
```

---

## Критерии приёмки

- [ ] Кнопка "Cancel" появляется только когда идёт запрос (Loading/Streaming)
- [ ] Нажатие Cancel прерывает HTTP job
- [ ] После Cancel: response text очищается, input text сохраняется
- [ ] UI не крашится после повторного нажатия
- [ ] Кнопка видна на экране TextStyler

---

## Файлы для изменения

1. `TextStylerViewModel.kt` — добавить `Job` и `Action.Cancel`
2. `TextStylerScreen.kt` — добавить кнопку Cancel
3. `TextStylerViewModelTest.kt` — тест на cancel (опционально)

---

## PR plan (дробить на мелкие PR)

| PR | Описание | Файлы |
|----|----------|-------|
| #1 | Cancel button UI | TextStylerScreen.kt |
| #2 | Cancel logic + Job management | TextStylerViewModel.kt |

Или один PR если задача маленькая. Решить после оценки工作量.