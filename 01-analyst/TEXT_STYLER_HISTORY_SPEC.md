# TEXT STYLER HISTORY — Feature Specification

**Version:** 1.0  
**Created:** 2025-05-05  
**Status:** Ready for Development

---

## 1. Overview

Добавить экран истории пользовательских взаимодействий в TextStyler mini-app. История сохраняет все запросы (input → result) с метаданными и доступна через отдельный экран.

---

## 2. UI/UX Specification

### 2.1 Navigation

- **Кнопка в Header:** Иконка `history` справа от Settings
- **Экран:** Bottom sheet (50% → full screen swipe) или Full screen modal
- **Назад:** Кнопка "Назад" или swipe down

### 2.2 History Screen Layout

```
┌─────────────────────────────────┐
│ ←  History [Clear All]          │  ← Header
├─────────────────────────────────┤
│ ┌─────────────────────────────┐ │
│ │ 🎯 STYLE     2 min ago    │ │  ← Interaction Item
│ │ "Hello world" → "Hello! ⚡" │ │
│ └─────────────────────────────┘ │
│ ┌─────────────────────────────┐ │
│ │ 📝 SUMMARIZE  1 hour ago   │ │
│ │ "Long text..." → "Summary"  │ │
│ └─────────────────────────────┘ │
│ ┌─────────────────────────────┐ │
│ │ ⚠️ ERROR     2 hours ago   │ │  ← Error item
│ │ "Input" → "Failed: timeout" │ │
│ └─────────────────────────────┘ │
│           ...                   │  ← RecyclerView/LazyColumn
└─────────────────────────────────┘
```

### 2.3 Interaction Item (Expanded)

```
┌─────────────────────────────┐
│ STYLE • 2 min ago           │
├─────────────────────────────┤
│ 📝 INPUT:                  │
│ Hello world                │
├─────────────────────────────┤
│ ✨ OUTPUT:                 │
│ Hello! ⚡☀️                 │
├─────────────────────────────┤
│ [Retry] [Delete] [Copy]    │  ← Action buttons
└─────────────────────────────┘
```

### 2.4 Visual Design

**Colors:**
- Background: `#121212` (Bg, from existing theme)
- Surface: `#1E1E1E` (Surface)
- Primary: `#6200EE` (Accent)
- Text Primary: `#FFFFFF`
- Text Secondary: `#B3B3B3`
- Success: `#4CAF50`
- Error: `#F44336`

**Typography:**
- Header: 18sp, SemiBold
- Item title: 14sp, Medium
- Item preview: 12sp, Regular
- Timestamp: 10sp, Regular, Secondary

**Spacing:**
- Item padding: 12dp
- Item gap: 8dp
- Screen padding: 16dp

---

## 3. Functionality Specification

### 3.1 Что записывать

Каждое взаимодействие сохраняет:

| Поле | Тип | Описание |
|------|-----|----------|
| id | Long | Unique ID (timestamp + random) |
| timestamp | Long | Unix millis |
| inputText | String | User input |
| outputText | String | Result |
| mode | String | StyleMode.id |
| status | Status | SUCCESS / ERROR |
| errorMessage | String? | Error details (nullable) |

### 3.2 Display Rules

- **Превью:** Показывает только первые 50 символов input и output
- **Метка времени:** Relative ("2 min ago", "1 hour ago", "Yesterday")
- **Иконка:** По mode.icon, или ⚠️ для errors

### 3.3 Actions

| Action | Behavior |
|--------|----------|
| Tap | Expand to show full input/output |
| Swipe left | Delete single item |
| Long press | Show delete confirmation |
| "Clear All" button | Delete all history |
| "Retry" button | Re-apply same input+mode |

### 3.4 Storage

**Option: Room Database**

```kotlin
@Entity(tableName = "interaction_history")
data class InteractionEntity(
    @PrimaryKey val id: Long,
    val timestamp: Long,
    val inputText: String,
    val outputText: String?,
    val mode: String,
    val status: String,  // "SUCCESS" | "ERROR"
    val errorMessage: String?
)
```

**Config:**
- Max items: 100 (старые自动 удаляются)
- Сортировка: По timestamp DESC

### 3.5 Architecture

```
data/
  local/
    InteractionDao.kt      # Room DAO
    InteractionEntity.kt  # Room Entity
    AppDatabase.kt      # Room Database
  repository/
    InteractionRepository.kt  # Repository pattern
    
domain/
  model/
    Interaction.kt  # Domain model
    
presentation/
  TextStylerViewModel.kt   # Добавить history state/actions
  HistoryViewModel.kt    # Отдельный VM для истории
```

---

## 4. Acceptance Criteria

- [ ] Кнопка "History" видна в Header
- [ ] Bottom sheet / full screen открывается по tap
- [ ] История сохраняется между сессиями (persist)
- [ ] Каждый item показывает: mode, timestamp, preview input/output
- [ ] Tap на item = expand с полным input/output
- [ ] Swipe left = delete single item
- [ ] "Clear All" работает
- [ ] Max 100 items, старые удаляются автоматически
- [ ] Error items показывают errorMessage

---

## 5. Technical Notes

- **Dependency:** Room (`androidx.room:room-ktx:2.6.1`)
- **Existing code:** TextStylerScreen.kt, TextStylerViewModel.kt
- **Theme:** Использовать существующие цвета (Bg, Surface, Accent, TextPrimary, TextSecondary)