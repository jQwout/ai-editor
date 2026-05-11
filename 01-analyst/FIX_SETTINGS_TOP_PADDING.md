# FIX: Settings screen top padding

## Задача
Убрать лишний отступ (safe area) сверху экрана Settings.

## Текущее поведение
```
┌─────────────────────────┐
│  █ STATUS BAR ██████████│ <- отступ под status bar
│  ████████████████████████│
│  ← Settings              │ <- лишний пробел между статус-баром и контентом
│  ┌─────────────────────┐ │
│  │ AI Provider         │ │
│  └─────────────────────┘ │
└─────────────────────────┘
```

## Ожидаемое поведение
```
┌─────────────────────────┐
│  █ STATUS BAR ██████████│
│  ← Settings              │ <- контент прижат к status bar
│  ┌─────────────────────┐ │
│  │ AI Provider         │ │
│  └─────────────────────┘ │
└─────────────────────────┘
```

## Причина
В `SettingsScreen.kt` строка 83:
```kotlin
Surface(
    modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding(),
    ...
)
```

`systemBarsPadding()` добавляет padding для status bar и navigation bar. Это избыточно — контент уже внутри `Column` с `padding(18.dp)`.

## Исправление
Заменить `systemBarsPadding()` на `StatusBarHeightOnly` — только bottom navigation bar padding.

Варианты:
1. Убрать `systemBarsPadding()` полностью
2. Использовать `WindowInsets.displayCutout` для top только
3. Использовать `WindowInsets.statusBars` только для top

## Критерии приёмки
- [ ] Между status bar и первым элементом (Row с back button) нет пробела
- [ ] Bottom safe area (navigation bar) сохраняется
- [ ] IME padding (keyboard) сохраняется

## Файлы
- `src/main/kotlin/openqwoutt/textstyler/ui/SettingsScreen.kt`