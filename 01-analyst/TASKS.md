# ai-editor Navigation Architecture SPEC

**Document**: Navigation Architecture Decision  
**Date**: 2025-01-XX  
**Status**: Draft  
**Author**: 01-analyst agent

---

## 1. Current Architecture Description

### 1.1 Overview

ai-editor is a single-screen mini-app embedded in a larger Android application (openQwoutt). It uses a boolean flag-based navigation approach where panels (Settings, History, Templates) are toggled via state flags in the ViewModel, rendered via nested `Crossfade` composables.

### 1.2 Component Hierarchy

```
TextStylerMiniApp (Entry point)
└── TextStylerScreen (Main UI with nested Crossfade)
    ├── MainContent
    │   ├── Header (with menu)
    │   ├── ResultArea
    │   └── InputCard + ModePicker + StyleSubModesStrip + VoiceInputPanel
    ├── SettingsScreen (shown when showSettings=true)
    ├── TemplatesSheet (shown when showTemplates=true)
    └── HistoryScreen (shown when showHistory=true)
```

### 1.3 State Management (TextStylerState)

```kotlin
data class TextStylerState(
    val showSettings: Boolean = false,
    val showHistory: Boolean = false,
    val showTemplates: Boolean = false,
    // ... other UI state
)
```

### 1.4 Actions (TextStylerAction)

```kotlin
sealed class TextStylerAction {
    data object ToggleSettings : TextStylerAction()
    data object ShowHistory : TextStylerAction()
    data object HideHistory : TextStylerAction()
    data object ShowTemplates : TextStylerAction()
    data object HideTemplates : TextStylerAction()
    // ... other actions
}
```

### 1.5 Current Navigation Flow (TextStylerScreen.kt lines 119-201)

```kotlin
Crossfade(targetState = state.showSettings) { showSettings ->
    if (showSettings) {
        SettingsScreen(onBack = { ToggleSettings })
    } else {
        Crossfade(targetState = state.showTemplates) { showTemplates ->
            if (showTemplates) {
                TemplatesSheet(onBack = { HideTemplates })
            } else {
                Crossfade(targetState = state.showHistory) { showHistory ->
                    if (showHistory) {
                        HistoryScreen(onBack = { HideHistory })
                    } else {
                        MainContent()
                    }
                }
            }
        }
    }
}
```

### 1.6 Key Files

| File | Purpose |
|------|---------|
| `TextStylerMiniApp.kt` | Entry point composable, wires ViewModel + passes callbacks |
| `TextStylerScreen.kt` | Main UI with nested Crossfade navigation |
| `TextStylerViewModel.kt` | State management, actions, events |
| `HistoryScreen.kt` | History list (standalone composable) |
| `SettingsScreen.kt` | Settings configuration (standalone composable) |

### 1.7 Current Callback Pattern

```kotlin
@Composable
fun TextStylerMiniApp(
    onNavigateBack: () -> Unit = {},
    onResultReady: ((String) -> Unit)? = null,
    closeBehavior: CloseBehavior = CloseBehavior.NavigateBack
)
```

The `onNavigateBack` callback is used by the host app (openQwoutt) to dismiss the mini-app.

---

## 2. Proposed Navigation Architecture

### 2.1 Option A: Keep Current Single-Screen Approach

Maintain the boolean flag approach with Crossfade animations. This is the current implementation.

**ASCII Diagram:**
```
TextStylerScreen
├── Crossfade (showSettings)
│   ├── false → Crossfade (showTemplates)
│   │   ├── false → Crossfade (showHistory)
│   │   │   ├── false → MainContent (default)
│   │   │   └── true → HistoryScreen
│   │   └── true → TemplatesSheet
│   └── true → SettingsScreen
```

### 2.2 Option B: Add Compose Navigation (NavHost)

Introduce a NavHost with routes for each major screen. This follows official Compose Navigation patterns.

**ASCII Diagram:**
```
NavHost (navController)
├── startDestination = "main"
│
├── Route: "main"
│   └── MainContent (mode picker, input, result)
│
├── Route: "history"
│   └── HistoryScreen (full-screen)
│
├── Route: "templates"
│   └── TemplatesSheet (full-screen)
│
├── Route: "settings"
│   └── SettingsScreen (full-screen)

Back stack handling:
- main → history → main (standard back)
- main → templates → main (standard back)
- main → settings → main (standard back)
```

### 2.3 Option C: Hybrid Approach (Partial NavHost)

Use NavHost for History and Settings (more complex screens) but keep Templates as a modal overlay.

**ASCII Diagram:**
```
NavHost (navController)
├── startDestination = "main"
│
├── Route: "main"
│   └── MainContent + TemplatesSheet (overlay/modal)
│
├── Route: "history"
│   └── HistoryScreen
│
├── Route: "settings"
│   └── SettingsScreen
```

---

## 3. Design Decisions and Trade-offs

### 3.1 Keep Single-Screen vs Add NavHost

| Factor | Single-Screen (Current) | NavHost |
|--------|-------------------------|---------|
| **Complexity** | Low - nested Crossfade | Medium - need to define routes, NavController |
| **State Management** | All in one ViewModel | Multiple ViewModels or shared state |
| **Back Navigation** | Custom callbacks (onBack) | Automatic via NavController |
| **Deep Linking** | Not supported | Supported (future) |
| **Testability** | Harder to test individual screens | Easier to test routes |
| **Animations** | Built-in Crossfade | Custom animations per route |
| **Bundle Size** | No extra dependency | Navigation library (~100KB) |
| **Learning Curve** | Simple for current team | Requires Compose Navigation knowledge |
| **Maintainability** | Simple for small screen count | Better for growing screen count |

### 3.2 Should HistoryScreen/TemplatesScreen Become Navigable Routes?

**Arguments FOR:**
- Separation of concerns: each screen has its own route
- Standard back navigation behavior
- Potential for deep linking in future
- Easier to add screen transitions
- Better fit for larger app scale

**Arguments AGAINST:**
- Current screens are not "pages" but overlays/panels
- TemplatesSheet appears as a bottom sheet overlay on MainContent
- HistoryScreen is a list view that doesn't have complex navigation
- Adding routes introduces boilerplate (NavHost, routes, arguments)
- Current state-based approach is simpler and works well

**RECOMMENDATION**: Keep current approach for now. Consider adding NavHost only if:
1. App needs deep linking support
2. History/Settings become more complex with sub-navigation
3. Team has capacity to learn and maintain Compose Navigation

### 3.3 NavHost vs Single-Screen Decision Matrix

| Condition | Recommended Approach |
|-----------|---------------------|
| App has <5 screens | Single-screen (current) |
| App has >5 screens with complex navigation | NavHost |
| Screens are overlays/modals | Single-screen |
| Screens need deep linking | NavHost |
| Team is small and needs simplicity | Single-screen |
| Need standard back navigation behavior | NavHost |

**CURRENT STATUS**: ai-editor has exactly 4 distinct views (Main, Settings, History, Templates). This is below the threshold where NavHost becomes necessary.

---

## 4. Backward Compatibility with onNavigateBack Callback

### 4.1 Current Pattern

```kotlin
@Composable
fun TextStylerMiniApp(
    onNavigateBack: () -> Unit = {},
    closeBehavior: CloseBehavior = CloseBehavior.NavigateBack
)
```

The mini-app is embedded in openQwoutt's MainActivity which handles the back button and calls `onNavigateBack` to dismiss the mini-app.

### 4.2 NavHost Back Handling

When adding NavHost, we need to decide how to handle the `onNavigateBack` callback:

**Option 1: Replace with NavController.popBackStack()**
- Remove `onNavigateBack` parameter
- Each screen uses `navController.popBackStack()` for back navigation
- Host app intercepts back press and calls `navController.popBackStack()`

**Option 2: Keep both callbacks**
- Keep `onNavigateBack` for host app integration
- NavController handles internal navigation
- Back press in NavHost screens uses `navController.popBackStack()`

**Option 3: Unified back handling**
- Use `rememberNavController()` inside TextStylerMiniApp
- When back stack is empty, call `onNavigateBack()`
- Host app provides `onNavigateBack` implementation

**RECOMMENDATION**: Option 2 (keep both) for gradual migration. Keep `onNavigateBack` while internal navigation uses NavController.

### 4.3 Event Sharing with Host

```kotlin
sealed class MiniAppEvent {
    data class ResultReady(val result: String) : MiniAppEvent()
    data object NavigateBack : MiniAppEvent()
}
```

The `MiniAppEvent.NavigateBack` can be replaced with NavController navigation when NavHost is added. The host app doesn't need to know internal navigation - it just provides the NavController.

---

## 5. Acceptance Criteria

### 5.1 If Keeping Single-Screen Approach

1. **AC1**: All panels (Settings, History, Templates) toggle correctly via state flags
2. **AC2**: Crossfade animations work smoothly between states
3. **AC3**: Back navigation (onBack callback) works correctly for all panels
4. **AC4**: No performance issues with nested Crossfade (max depth: 3)
5. **AC5**: Backward compatibility maintained with existing host app integration

### 5.2 If Adding NavHost

1. **AC1**: NavHost correctly renders all 4 routes (main, settings, history, templates)
2. **AC2**: Back navigation via NavController works correctly
3. **AC3**: Host app's `onNavigateBack` works when back stack is empty
4. **AC4**: Shared ViewModel state is accessible from all routes
5. **AC5**: Animations between routes are smooth (300ms standard)
6. **AC6**: Deep linking support (future consideration)

---

## 6. Test Scenarios

### 6.1 Single-Screen Approach Tests

| ID | Scenario | Expected Result |
|----|----------|------------------|
| T1.1 | Toggle Settings on | SettingsScreen displayed, showSettings=true |
| T1.2 | Toggle Settings off | MainContent displayed, showSettings=false |
| T1.3 | Open History from Main | HistoryScreen displayed, showHistory=true |
| T1.4 | Press back on History | Returns to MainContent, showHistory=false |
| T1.5 | Open Settings from History | Not possible (nested structure) |
| T1.6 | Open Templates from Main | TemplatesSheet displayed, showTemplates=true |
| T1.7 | Press back on Templates | Returns to previous state |
| T1.8 | Rapid toggling of panels | Crossfade animation completes for each state |
| T1.9 | onNavigateBack called on empty back stack | Host app handles dismissal |
| T1.10 | Screen rotation during panel transition | State preserved correctly |

### 6.2 NavHost Approach Tests (if implemented)

| ID | Scenario | Expected Result |
|----|----------|------------------|
| T2.1 | Navigate to Settings | SettingsScreen shown, back stack: [main, settings] |
| T2.2 | Press back from Settings | Returns to MainContent, back stack: [main] |
| T2.3 | Navigate to History | HistoryScreen shown, back stack: [main, history] |
| T2.4 | Navigate to Templates | TemplatesSheet shown, back stack: [main, templates] |
| T2.5 | History screen has own ViewModel | State persists correctly |
| T2.6 | Main screen state preserved during navigation | Input text, result not lost |
| T2.7 | Deep link to /history | Opens HistoryScreen directly |
| T2.8 | Back on empty stack | onNavigateBack called |

### 6.3 Compatibility Tests

| ID | Scenario | Expected Result |
|----|----------|------------------|
| C1.1 | Existing host app integration | Works without changes |
| C1.2 | CloseBehavior.NavigateBack | Correct behavior for current approach |
| C1.3 | CloseBehavior.FinishActivity | Activity finishes correctly |
| C1.4 | MiniAppEvent.ResultReady | Callback invoked with result |
| C1.5 | MiniAppEvent.NavigateBack | Works for current approach |

---

## 7. Recommendation

### 7.1 Decision

**IMPLEMENT Option C: Hybrid Approach — NavHost for History/Settings, Templates as overlay.**

### 7.2 Rationale
1. **Best of both worlds**: NavHost for complex screens (History, Settings), overlay for quick panel (Templates)
2. **Scalability**: History/Settings can grow with sub-navigation if needed
3. **Performance**: Templates as overlay is fast and familiar UX pattern
4. **Maintainability**: Clear separation between navigable screens and modal overlays
5. **Future-proof**: Easy migration to full NavHost if app grows
6. **Team Constraints**: No indication of need for deep linking or complex navigation

### 7.3 Future Considerations

If the app grows to include:
- Multiple levels of sub-navigation
- Deep linking requirements
- Complex navigation patterns (tabs, drawer)

Then reconsider adding NavHost at that time. The current architecture is not a dead-end - migration to NavHost is straightforward if needed.

### 7.4 Implementation Notes (if needed later)

When adding NavHost in the future:

1. Create route definitions:
```kotlin
sealed class Route(val route: String) {
    data object Main : Route("main")
    data object Settings : Route("settings")
    data object History : Route("history")
    data object Templates : Route("templates")
}
```

2. Use shared ViewModel via `navGraph.viewModel()` for state access
3. Define back press handling in MainActivity
4. Add animations in NavHost `navType` or `transitionSpec`

---

## 8. Open Questions

1. **Q1**: Does the host app (openQwoutt) need to intercept specific navigation events?
2. **Q2**: Are there plans to add deep linking to ai-editor screens?
3. **Q3**: Will History/Settings become more complex with sub-navigation?

These answers would help finalize the navigation architecture decision.