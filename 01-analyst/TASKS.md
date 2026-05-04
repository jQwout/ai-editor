# AI Editor - UI Unification & Animation SPEC

## Task Context
Analyze the current Android/Jetpack Compose codebase and create a detailed SPEC for:
1. Unify assistant and application screens into one screen
2. Add animations and UI polish to main screen

---

## Current Screen Structure

### Overview of Activities & Screens

| Activity/Screen | File | Purpose | UI Framework |
|----------------|------|---------|---------------|
| MainActivity | `.../app/MainActivity.kt` | App entry point, launches SideAppRoot | Compose |
| SideAppRoot | `.../app/SideAppRoot.kt` | Root composable wrapper | Compose |
| TextStylerMiniApp | `.../textstyler/TextStylerMiniApp.kt` | Main app shell, wires VM + Screen | Compose |
| TextStylerScreen | `.../ui/TextStylerScreen.kt` | Main UI: tabs, editor, result | Compose (519 lines) |
| SettingsScreen | `.../ui/SettingsScreen.kt` | Settings: API mode, key, model | Compose (265 lines) |
| TextStylerActivity | `.../ui/TextStylerActivity.kt` | ACTION_PROCESS_TEXT handler | Compose (132 lines) |
| VoiceAssistActivity | `.../ui/VoiceAssistActivity.kt` | ACTION_VOICE_ASSIST handler | Compose (176 lines) |

### Navigation Model

```
MainActivity
    └── SideAppRoot
            └── TextStylerMiniApp
                    └── TextStylerScreen
                            ├── (State: showSettings = false) → Main UI
                            │       ├── Header (close, title, settings)
                            │       ├── MainModeTabs (TRANSLATE, STYLE, FIX)
                            │       ├── StyleStrip (FORMAL, SHORT, TRIBAL, etc.)
                            │       ├── AnalyzeStrip (SUMMARIZE, ANALYZE, SCREENSHOT)
                            │       ├── EditorBlock (input text)
                            │       ├── ResultBlock (processed text)
                            │       └── BottomActions (Apply button)
                            │
                            └── (State: showSettings = true) → SettingsScreen
```

### Entry Points (Intents)

1. **Main app launch** → `MainActivity` → `TextStylerMiniApp` → `TextStylerScreen`
2. **ACTION_PROCESS_TEXT** → `TextStylerActivity` → `TextStylerProcessingScreen`
3. **ACTION_VOICE_ASSIST** → `VoiceAssistActivity` → `VoiceAssistScreen`

### Current UI Components

#### TextStylerScreen Main UI Sections
- **Header**: Close icon, "AI Editor" title, Settings icon
- **MainModeTabs**: Horizontal tabs for TRANSLATE, STYLE, FIX (ModeGroup.MAIN)
- **StyleStrip**: Horizontal scroll for style variants (FORMAL, SHORT, TRIBAL, CORP, BIBLICAL, VIKING, ZEN, OLD_EMOJI)
- **AnalyzeStrip**: Horizontal tabs for SUMMARIZE, ANALYZE, SCREENSHOT (ModeGroup.ANALYZE)
- **EditorBlock**: Label, paste button, BasicTextField (180dp height), error display
- **ResultBlock**: Label, copy button, clear button, result text display
- **BottomActions**: Apply button with loading state

#### TextStylerProcessingScreen (Action Handler)
- Simple Column with action buttons for MAIN modes + SUMMARIZE + ANALYZE_MAIN

#### VoiceAssistScreen
- Similar to TextStylerProcessingScreen but also accepts voice input

### Color Palette (Dark Theme - Telegram-style)
```
Bg = #0F0F0F
Surface = #1A1A1A
Accent = #8774E1
AccentSoft = #8774E1 @ 15%
TextPrimary = #FFFFFF
TextSecondary = #8E8E93
Divider = #2C2C2E
ErrorBg = #3A2228
ErrorText = #FFC4CF

Settings Screen:
AppBg = #0D0D11
Panel = #1B1B20
PanelLight = #23232A
Accent = #8D78F0
```

### State Management

**TextStylerState** (`TextStylerViewModel.kt`):
```kotlin
data class TextStylerState(
    val inputText: String = "",
    val selectedMode: StyleMode = StyleMode.STYLE,
    val result: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isTextTruncated: Boolean = false,
    val showSettings: Boolean = false,
    val settings: AppSettings = AppSettings()
)
```

**TextStylerAction**:
- SetInputText, SelectMode, ProcessText, ClearResult, ClearError, ToggleSettings, SaveSettings

---

## Analysis Findings

### Issue 1: Assistant Screens Are Separate from Main Screen
- **TextStylerActivity** and **VoiceAssistActivity** have their own separate UI screens
- **TextStylerProcessingScreen** and **VoiceAssistScreen** duplicate logic with the main screen
- These screens show only a subset of modes (MAIN + SUMMARIZE + ANALYZE_MAIN)
- User experience is fragmented when app is launched from different entry points

### Issue 2: No Animations Present
- Zero animation imports found in any UI file
- All UI transitions are instantaneous
- Loading state is just a spinner with no motion
- No transitions between screen states (showSettings toggle)
- No tab/mode selection animations

### Issue 3: UI Polish Opportunities
- Mode selection has no visual feedback (instant state change)
- Result block appears/disappears without transition
- Settings overlay replaces screen entirely (no fade or slide)
- Editor and Result blocks have no elevation/shadow differences

---

## Proposed Unified Design

### 1. Unify Assistant & Application Screens

#### Goal
- Have **one single main screen** (`TextStylerScreen`) that handles all entry points
- **TextStylerActivity** and **VoiceAssistActivity** should launch directly into `TextStylerScreen` instead of showing a separate "quick action" screen

#### Implementation Approach

**Option A (Recommended): Modify Activities to Launch Main Screen**
- Modify `TextStylerActivity` to call `TextStylerMiniApp()` composable directly
- Modify `VoiceAssistActivity` to call `TextStylerMiniApp()` composable directly
- Pass input text via ViewModel state initialization
- This unifies the UI while keeping the intent-based behavior

**Refactored Activity Code (`TextStylerActivity.kt`):**
```kotlin
class TextStylerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val inputText = intent?.getStringExtra(Intent.EXTRA_PROCESS_TEXT).orEmpty()
        val readOnly = intent?.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false) ?: false
        
        setContent {
            MaterialTheme {
                // Reuse the main app composable
                TextStylerMiniAppWithInitialText(
                    initialInput = inputText,
                    onResultReady = { processedText ->
                        if (readOnly) {
                            // Copy to clipboard
                        } else {
                            // Return as result
                        }
                        finish()
                    }
                )
            }
        }
    }
}
```

**State Extension:**
- Add `initialInputText: String?` to TextStylerState (optional, for prefilled input)
- Add `onTextProcessed` callback to TextStylerMiniApp for returning values to intents
- Add `closeBehavior` to control whether to call finish() or navigate back

#### Benefits
- Single UI for all entry points
- All 14 modes available from all entry points
- Consistent user experience
- Reduced code duplication

### 2. Add Animations & UI Polish

#### Animation Specifications

| Element | Animation Type | Duration | Easing |
|---------|---------------|----------|--------|
| Mode Tab Selection | Background color fade | 200ms | easeInOut |
| StyleStrip Expand | Height animation | 250ms | fastOutSlowIn |
| Result Block Appear | Fade + SlideUp | 300ms | easeOut |
| Settings Overlay | Fade in backdrop, Slide from right | 300ms | easeOutCubic |
| Apply Button (Loading) | Scale pulse (optional shimmer) | 1000ms repeat | linear |
| Error Toast | Fade in/out | 200ms | easeInOut |
| Text Change | No animation (instant for responsiveness) | - | - |

#### Detailed Animation Specs

**A. MainModeTabs Selection Animation**
```kotlin
// Current: Instant background change
// Proposed: AnimatedContent with fade

val animatedBg by animateColorAsState(
    targetValue = if (selected) Accent else Color.Transparent,
    animationSpec = tween(200, easing = FastOutSlowIn),
    label = "tab_bg"
)
```

**B. StyleStrip Expand/Collapse Animation**
```kotlin
// Proposed: AnimatedVisibility with expandVertically

AnimatedVisibility(
    visible = state.selectedMode == StyleMode.STYLE,
    enter = expandVertically(
        animationSpec = tween(250, easing = FastOutSlowIn)
    ) + fadeIn(tween(250)),
    exit = shrinkVertically(
        animationSpec = tween(250, easing = FastOutSlowIn)
    ) + fadeOut(tween(250))
)
)
```

**C. Result Block Animation**
```kotlin
// Proposed: AnimateAppearance modifier or AnimatedVisibility

AnimatedVisibility(
    visible = state.result != null || state.isLoading,
    enter = fadeIn(tween(300, easing = EaseOutCubic)) + 
            slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = tween(300, easing = EaseOutCubic)
            ),
    exit = fadeOut(tween(200))
)
)
```

**D. Settings Screen Modal Transition**
```kotlin
// Proposed: Crossfade with animated content + backdrop alpha

Crossfade(
    targetState = state.showSettings,
    animationSpec = tween(300),
    label = "settings_crossfade"
) { showSettings ->
    if (showSettings) {
        // Settings modal with backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)) // Animated alpha
        ) {
            SettingsScreen(...)
        }
    } else {
        // Main content
    }
}
```

**E. Loading Button Animation**
```kotlin
// Current: Just CircularProgressIndicator
// Proposed: Add pulse or shimmer effect

val infiniteTransition = rememberInfiniteTransition(label = "button_loading")
val scale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.05f,
    animationSpec = infiniteRepeatable(
        animation = tween(600),
        repeatMode = RepeatMode.Reverse
    ),
    label = "button_scale"
)
```

#### UI Polish Improvements

**1. Elevated Cards**
- Add subtle shadows to EditorBlock and ResultBlock
- Use `Modifier.shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp))`

**2. Better Visual Hierarchy**
- Increase spacing between sections
- Add subtle dividers between logical groups
- Use Accent color more strategically for active states

**3. Haptic Feedback (Optional)**
- Add `LocalHapticFeedback` on mode selection
- Use `HapticFeedbackType.TextHandleMove` on Apply button press

**4. Keyboard Handling**
- Ensure IME action properly handles text submission
- Add keyboard dismiss on empty tap

---

## Implementation Plan

### Phase 1: Unification (Priority: High)
- [ ] Modify TextStylerViewModel to accept initialInputText
- [ ] Create TextStylerMiniApp variant with result callback
- [ ] Refactor TextStylerActivity to use TextStylerMiniApp
- [ ] Refactor VoiceAssistActivity to use TextStylerMiniApp
- [ ] Add onResultReady callback handling
- [ ] Test all three entry points work correctly

### Phase 2: Animations (Priority: High)
- [ ] Add Compose Animation dependencies (already included)
- [ ] Add AnimatedColor for tab selection
- [ ] Add AnimatedVisibility for StyleStrip
- [ ] Add Result block appearance animation
- [ ] Add Settings modal crossfade transition
- [ ] Test animations on all Android versions (minSdk 29)

### Phase 3: Polish (Priority: Medium)
- [ ] Add elevation/shadow to input/output cards
- [ ] Improve spacing and visual hierarchy
- [ ] Add loading button pulse animation
- [ ] Consider haptic feedback
- [ ] Test edge-to-edge display

---

## Files to Modify

| File | Changes |
|------|---------|
| `TextStylerViewModel.kt` | Add initialInputText, onResultReady to state |
| `TextStylerMiniApp.kt` | Add parameters for initial text and result callback |
| `TextStylerActivity.kt` | Replace ProcessingScreen with MiniApp |
| `VoiceAssistActivity.kt` | Replace AssistScreen with MiniApp |
| `TextStylerScreen.kt` | Add all animations |
| `SettingsScreen.kt` | Add crossfade transition |

---

## Testing Requirements

1. **Unification Tests**
   - Launch from app icon → main screen works
   - Share text from Chrome → TextStylerActivity receives text correctly
   - Voice button → VoiceAssistActivity processes correctly

2. **Animation Tests**
   - Mode tab tap → visible color fade (200ms)
   - Style tab tap → strip expands smoothly (250ms)
   - Apply button → result appears with animation (300ms)
   - Settings icon → modal slides in smoothly

3. **Performance Tests**
   - Ensure animations run at 60fps
   - Test on lower-end devices (API 29)
   - No jank during screen transitions

---

## Dependencies

Current dependencies support all animations natively:
- `androidx.compose.animation` (part of Compose 1.6.2)
- `androidx.compose.animation.graphics`
- No additional dependencies needed

---

## Alternative approaches Considered

### Alternative 1: Keep Separate Screens But Add Animation
- Keep TextStylerProcessingScreen and VoiceAssistScreen
- Just add animations to each independently
- **Rejected**: Increases code duplication, inconsistent UX

### Alternative 2: Use Navigation Library
- Use Compose Navigation to handle all screens
- **Rejected**: Overkill for single-screen app, adds dependencies

### Alternative 3: WebView Embedding
- Load web-based UI in WebView
- **Rejected**: Loses native integration, poor performance

---

## Summary

This SPEC proposes:
1. **Unification**: Refactor TextStylerActivity and VoiceAssistActivity to use the main TextStylerScreen for a consistent unified experience across all entry points
2. **Animation**: Add Compose animations for tab selection, strip expansion, result appearance, and settings modal
3. **Polish**: Improve card elevation and visual hierarchy

The result will be a single, polished main screen with smooth animations, accessible from all app entry points.