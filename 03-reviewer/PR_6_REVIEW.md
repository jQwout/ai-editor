# PR #6 Review: Kotlin 2.1 upgrade + Metro DI + redesign

## PR Info
- **Title**: Kotlin 2.1 upgrade + metro di + redesign
- **Author**: jQwout (owner)
- **Branch**: `kotlin-2.1-upgrade` → `prompt-improvement`
- **Base**: `prompt-improvement` (not master!)
- **Commits**: 4
- **Files Changed**: 7 (+83 -23)

---

## Changes Summary

| File | Changes |
|------|--------|
| `.github/workflows/build.yml` | +43 (new CI workflow) |
| `.gitignore` | +4 (local CLI tools) |
| `build.gradle.kts` | +28 -19 (Kotlin 2.1, Metro DI, Compose 1.7.5) |
| `gradle.properties` | -1 (removed hardcoded JDK path) |
| `gradle/wrapper/gradle-wrapper.properties` | +1 -1 (Gradle 9.0) |
| `settings.gradle.kts` | +5 (KSP resolution) |
| `src/main/kotlin/.../TextStylerScreen.kt` | +2 -2 (package import fix) |

---

## Code Review

### 1. Dependencies - APPROVED
- **Kotlin**: 2.1.0 (from 2.2.20) ✓
- **Compose BOM**: 1.7.5 (from 1.6.2) ✓
- **Metro DI**: 0.3.8 (new) - zero-step DI library
- **Gradle**: 9.0 (from 8.13) - requires JVM 21

### 2. Downgrade Notice
- **AGP**: 8.7.3 (from 8.13.1) ⚠️
- **compileSdk/targetSdk**: 35 (from 36) ⚠️
- **KSP**: 2.1.0-1.0.29 (was 2.2.20-1.0.25)

> Note: This is downgrade for Android Gradle Plugin. Verify compatibility.

### 3. CI/CD - APPROVED
- GitHub Actions workflow added
- Builds on push to master
- Uploads APK as artifact

### 4. Import Fix - APPROVED
- `PromptCategory`/`PromptTemplate` moved from `textstyler.data.prompts` to `miniapp.textstyler.data.prompts`

### 5. Security - APPROVED
- No API keys exposed
- Hardcoded JDK path removed from gradle.properties

---

## Build Test

**Not tested** - requires local JVM 21 and Android SDK.

Recommend testing locally before merge:
```bash
./gradlew assembleDebug
```

---

## Decision

| Check | Status |
|-------|--------|
| Compiles | NOT TESTED |
| No conflicts | YES |
| No API keys | YES |
| CI workflow valid | YES |
| Import fix correct | YES |

### VERDICT: RECOMMEND WITH TESTING

**Request**: Run `./gradlew assembleDebug` locally and verify build before merge.

---

## Notes

1. **Base branch**: PR targets `prompt-improvement`, not `master`. Ensure this is intentional.
2. **Metro DI**: This is a significant architectural change. Verify generated code works.
3. **Gradle 9.0**: Requires JDK 21. CI should handle this via `temurin` distribution.

---

Reviewed: 2026-05-07