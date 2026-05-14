# Settings Screen Redesign — SPEC

**Project:** jQwout/ai-editor  
**File:** `src/main/kotlin/openqwoutt/textstyler/ui/SettingsScreen.kt`  
**Author:** Analyst Agent  
**Date:** 2026-05-13

---

## 1. Overview

Редизайн экрана настроек с двумя цветовыми темами (Light/Dark), новой навигацией и обновлённым UX. Четыре основных пункта меню с иконками, segment control для темы, accordion для NVIDIA NIM.

---

## 2. Design System

### 2.1 Color Palette

| Token | Light Theme | Dark Theme | Usage |
|-------|-------------|------------|-------|
| `AppBg` | `#F9FAFB` | `#0D0D11` | Фон приложения |
| `CardBg` | `#FFFFFF` | `#1B1B20` | Карточки настроек |
| `CardBorder` | `#E5E7EB` | `#2D2D35` | Границы карточек |
| `Primary` | `#6D28D9` | `#8D78F0` | Primary accent (фиолетовый) |
| `TextPrimary` | `#1F2937` | `#F4F1F8` | Основной текст |
| `TextSecondary` | `#6B7280` | `#B9B5C3` | Вторичный текст |
| `IconBg` | `#F3F4F6` | `#23232A` | Фон иконки |
| `ActiveBadge` | `#EDE9FE` | `#3D3A54` | Бейдж "Active" |

### 2.2 Typography

| Element | Font | Size | Weight |
|---------|------|------|--------|
| Screen Title | System | 20sp | Bold (700) |
| Card Title | System | 16sp | SemiBold (600) |
| Item Label | System | 15sp | Medium (500) |
| Body/Description | System | 14sp | Regular (400) |
| Badge | System | 12sp | Medium (500) |

### 2.3 Spacing & Layout

- Screen padding: 16dp
- Card border-radius: 16dp
- Card padding: 16dp
- Item vertical spacing: 16dp
- Icon container: 40x40dp, border-radius 12dp

---

## 3. Screen Structure

### 3.1 ASCII Wireframe

```
+------------------------------------------+
|  [<-]  Settings                    [Save] |
+------------------------------------------+
|                                          |
|  +------------------------------------+  |
|  | [🤖]  AI Model          [chevron] |  |
|  |      NVIDIA NIM  (Selected)        |  |
|  +------------------------------------+  |
|                                          |
|  +------------------------------------+  |
|  | [🌐]  Language            [chevron] |  |
|  |      English                      |  |
|  +------------------------------------+  |
|                                          |
|  +------------------------------------+  |
|  | [☀️]  Theme                      |  |
|  | [Light] [Dark] [Auto]             |  |
|  +------------------------------------+  |
|                                          |
|  +------------------------------------+  |
|  | [</>]  Custom Backend   [toggle]  |  |
|  |                                    |  |
|  |   URL: [________________]  (если  |
|  |           активен)               |  |
|  +------------------------------------+  |
|                                          |
+------------------------------------------+
```

### 3.2 Navigation Flow

```
SettingsScreen
    |
    +---> AI Model Selection Screen
    |         (only NVIDIA NIM, with API Key + Model dropdown)
    |
    +---> Language Selection Screen
    |         (future: English, etc.)
    |
    +---> Theme: Segmented Control (in-place)
    |
    +---> Custom Backend: Toggle + URL field (in-place)
```

---

## 4. UI Components

### 4.1 SettingsCard (Base Component)

```kotlin
@Composable
fun SettingsCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit,
    onClick: (() -> Unit)? = null
)
```

**States:**
- Default: Card with icon, title, subtitle, trailing element
- Pressed: Background opacity 0.7
- Disabled: Opacity 0.5, non-interactive
- Selected: Primary color highlight on title

**Visual:**
- Background: CardBg
- Border: 1dp CardBorder (visible in light theme)
- Icon: 40x40dp container with IconBg, icon colored Primary
- Corner radius: 16dp

### 4.2 SettingsMenuItem

4 menu items с иконками:

| Order | Title | Icon | Behavior |
|-------|-------|------|----------|
| 1 | AI Model | Robot icon | Navigate to selection screen |
| 2 | Language | Globe icon | Navigate to selection screen |
| 3 | Theme | Sun icon | In-place segment control |
| 4 | Custom Backend | Brackets icon | Toggle + conditional URL |

**Custom Backend ordering:**
- If active: FIRST in list
- If inactive: LAST in list, URL field disabled

### 4.3 SegmentedControl (Theme Selector)

```
[Light] [Dark] [Auto]
```

- Height: 40dp
- Corner radius: 20dp (pill shape)
- Selected segment: CardBg with shadow
- Unselected: transparent with border

### 4.4 ToggleSwitch

- Width: 52dp
- Height: 32dp
- Track: CardBorder (off) / Primary (on)
- Thumb: White

### 4.5 Accordion (AI Model Selection)

Expanded when "AI Model" card is tapped:

```
+------------------------------------------+
| [🤖]  AI Model                    [▼]    |
+------------------------------------------+
|                                          |
|  +------------------------------------+  |
|  | API Key:                          |  |
|  | [________________________] [👁]   |  |
|  +------------------------------------+  |
|                                          |
|  +------------------------------------+  |
|  | Select Model: [NVIDIA Models v]   |  |
|  |                                    |  |
|  |   > meta/llama-3.1-70b-instruct   |  |
|  |   > qwen/qwen3-coder-480b...     |  |
|  +------------------------------------+  |
+------------------------------------------+
```

**Hardcoded models for NVIDIA NIM:**
- `meta/llama-3.1-70b-instruct` (default)
- `qwen/qwen3-coder-480b-a35b-instruct`

### 4.6 InputField

- Height: 48dp
- Border: 1dp CardBorder
- Corner radius: 12dp
- Focus state: Primary border
- Password field: toggle visibility icon

---

## 5. Screen Specifications

### 5.1 SettingsScreen (Main)

**Top Bar:**
- Left: Back button (arrow icon)
- Center: Title "Settings"
- Right: "Save" button (text, Primary color)

**Content:**
- LazyColumn with 4 SettingsCard items
- Pull-to-refresh: disabled
- Keyboard handling: IME padding applied

### 5.2 AI Model Selection (Sub-screen)

**Header:** "Select AI Model" with back button

**Content:**
- Provider display: "NVIDIA NIM" (only option, not selectable)
- API Key input field
- Model dropdown with hardcoded list
- Save button → returns to Settings, saves selection

### 5.3 Theme Segmented Control

Three options: Light, Dark, Auto
- System follows device setting when "Auto"
- Saves preference immediately on selection

---

## 6. State Management

### 6.1 Settings State

```kotlin
data class SettingsState(
    val aiModel: String = "meta/llama-3.1-70b-instruct",
    val apiKey: String = "",
    val language: String = "English",
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val customBackendEnabled: Boolean = false,
    val customBackendUrl: String = ""
)
```

### 6.2 Navigation Events

- `onBack`: Pop back stack
- `onSave`: Persist settings → pop back stack
- `onThemeChange(ThemeMode)`: Update preference immediately
- `onCustomBackendToggle(Boolean)`: Update state

---

## 7. Dark Theme Specification

Colors for dark mode:

```kotlin
private val DarkAppBg = Color(0xFF0D0D11)
private val DarkCardBg = Color(0xFF1B1B20)
private val DarkCardBorder = Color(0xFF2D2D35)
private val DarkIconBg = Color(0xFF23232A)
private val DarkPrimary = Color(0xFF8D78F0)
private val DarkTextPrimary = Color(0xFFF4F1F8)
private val DarkTextSecondary = Color(0xFFB9B5C3)
```

---

## 8. Tests

### 8.1 Unit Tests

```kotlin
class SettingsStateTest {
    @Test fun `custom backend at end when disabled`() {
        val state = SettingsState(customBackendEnabled = false)
        val items = state.getMenuItems()
        assertEquals("Custom Backend", items.last().title)
    }

    @Test fun `custom backend first when enabled`() {
        val state = SettingsState(customBackendEnabled = true)
        val items = state.getMenuItems()
        assertEquals("Custom Backend", items.first().title)
    }

    @Test fun `nvidia models list is hardcoded`() {
        val models = getNvidiaModels()
        assertEquals(2, models.size)
        assertTrue(models.contains("meta/llama-3.1-70b-instruct"))
    }
}
```

### 8.2 UI Tests (Compose)

```kotlin
class SettingsScreenTest {
    @Test fun `theme segment control changes value`() {
        composeRule.setContent { SettingsScreen(...) }
        
        onNodeWithText("Dark").performClick()
        
        // Verify theme state updated
        // Verify visual change reflected
    }

    @Test fun `custom backend toggle shows URL field`() {
        composeRule.setContent { SettingsScreen(...) }
        
        val toggle = onNodeWithTag("customBackendToggle")
        toggle.performClick()
        
        onNodeWithText("Backend URL").assertIsVisible()
    }

    @Test fun `save button navigates back`() {
        composeRule.setContent { SettingsScreen(...) }
        
        onNodeWithText("Save").performClick()
        
        verify(onBack).wasCalled()
    }
}
```

### 8.3 Visual Regression Tests

- Screenshot comparison for Light/Dark themes
- Key states: default, with Custom Backend enabled, with accordion expanded

---

## 9. Acceptance Criteria

### 9.1 Functional

| ID | Criteria | Verification |
|----|----------|--------------|
| AC-1 | 4 menu items displayed with icons | Manual + UI test |
| AC-2 | Custom Backend active = first in list | Unit test |
| AC-3 | Custom Backend inactive = last in list | Unit test |
| AC-4 | Custom Backend URL field disabled when inactive | UI test |
| AC-5 | AI Model selected = highlighted with Primary | Visual test |
| AC-6 | AI Model always clickable | Manual test |
| AC-7 | NVIDIA NIM only provider in selection | Manual test |
| AC-8 | NVIDIA NIM model dropdown has 2 hardcoded models | Manual test |
| AC-9 | Save navigates to previous screen | UI test |
| AC-10 | Theme segmented control: Light/Dark/Auto | UI test |

### 9.2 Visual

| ID | Criteria | Verification |
|----|----------|--------------|
| VC-1 | Light theme: off-white background (#F9FAFB) | Screenshot |
| VC-2 | Light theme: white cards (#FFFFFF) | Screenshot |
| VC-3 | Dark theme: dark background (#0D0D11) | Screenshot |
| VC-4 | Dark theme: dark cards (#1B1B20) | Screenshot |
| VC-5 | Primary accent color: Purple (#6D28D9) | Visual check |
| VC-6 | Cards have 16dp border-radius | Visual check |
| VC-7 | Icon containers: 40x40dp with 12dp radius | Visual check |

### 9.3 Accessibility

| ID | Criteria | Verification |
|----|----------|--------------|
| A-1 | All interactive elements have contentDescription | Code review |
| A-2 | Color contrast ratio >= 4.5:1 for text | Accessibility scan |
| A-3 | Touch targets >= 48dp | Manual check |

---

## 10. Implementation Notes

1. **Theme switching**: Use Material3 `dynamicColor` with `ColorScheme` override
2. **Icons**: Material Icons (Robot, Public, LightMode, Code)
3. **Navigation**: Jetpack Navigation Compose
4. **State**: ViewModel with StateFlow, saved to SharedPreferences
5. **No OpenRouter in UI**: Remove from AI Model selection (keep enum for data compatibility)