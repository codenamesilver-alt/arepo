# Task 1 Report: Configure Dependencies

**Date:** 2026-09-10
**Status:** DONE_WITH_CONCERNS
**Executed by:** opencode (task-1 brief)

## Summary

Configured all Gradle dependencies for the GARMENTO app (Hilt, Room, supabase-kt, DataStore, WorkManager, Vico, ZXing, Navigation Compose, testing libs), added a `@HiltAndroidApp` application class with a notification channel, updated the manifest, and committed. `./gradlew assembleDebug` succeeds.

## Files Modified / Created

- Modified: `gradle/libs.versions.toml` — full version catalog, `[libraries]`, `[plugins]`
- Modified: `build.gradle.kts` (project-level) — plugin aliases `apply false`
- Unchanged: `settings.gradle.kts` — already identical to plan target
- Modified: `app/build.gradle.kts` — plugins, BuildConfig fields, `buildConfig = true`, `room { schemaDirectory(...) }`, all dependencies
- Modified: `local.properties` — Supabase URL / anon key / service role key (gitignored, NOT committed)
- Created: `app/src/main/java/com/example/garmento/GarmentoApp.kt` — `@HiltAndroidApp`, notification channel `expiry_reminders`
- Modified: `app/src/main/AndroidManifest.xml` — INTERNET + POST_NOTIFICATIONS permissions, `.GarmentoApp` name
- Modified: `gradle.properties` — added `android.disallowKotlinSourceSets=false`

## Verification

- `./gradlew assembleDebug` → **BUILD SUCCESSFUL** (42 tasks; kspDebugKotlin, hiltCollectClassesDebug, hiltAggregateDepsDebug, hiltJavaCompileDebug, transformDebugClassesWithAsm all ran)
- Generated `BuildConfig.java` confirmed with `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `SUPABASE_SERVICE_ROLE_KEY` populated correctly.

## Deviations from Plan (all required for a working build)

1. **KSP version** `2.2.10-1.0.32` does not exist on Maven Central. Only KSP for Kotlin 2.2.10 is `2.2.10-2.0.2`. Verified against central metadata.
2. **ZXing** `4.3.0` does not exist (latest is `3.5.4`). Used `3.5.4`.
3. **supabase-kt auth artifact** renamed: `gotrue-kt` is discontinued (last `2.7.0-beta-1`). Supabase 3.x ships auth as `auth-kt`. Library key renamed `supabase-gotrue` → `supabase-auth`; package imports stay `io.github.jan.supabase.gotrue.Auth`.
4. **Hilt** `2.53.1` fails under AGP 9.4: "Android BaseExtension not found" (old Hilt plugin API removed by AGP 9). Bumped to `2.60.1` (latest), which applies cleanly.
5. **Room Gradle plugin** added (`androidx.room:2.7.1`) to `[plugins]` and both build scripts. The plan's `room { schemaDirectory(...) }` block is an extension provided by that plugin; without it the build script won't compile. Plan omitted it.
6. **`android.disallowKotlinSourceSets=false`** added to `gradle.properties`. KSP registers generated sources via the Kotlin sourceSets DSL, which AGP 9 built-in Kotlin rejects by default. This is AGP 9's documented escape hatch for such plugins.
7. **SDK versions**: kept the existing project's AGP 9.4 DSL — `compileSdk { version = release(37) }`, `targetSdk = 37`, `minSdk = 24`, and `optimization { enable = false }` for release — instead of the plan's `compileSdk = 35` / `isMinifyEnabled = false` (those were AGP 8-style and removed/changed in AGP 9.4). `kotlinOptions {}` block also omitted (removed with AGP 9 built-in Kotlin); Gradle emits a non-fatal "Kotlin falls back to JVM_24" warning.
8. **Git identity**: none was configured on this machine. Set repo-local `user.name=SHAMSI`, `user.email=shamsi@localhost` (not global) to enable the requested commit.

## Commit

- `079c42f` — `chore: configure dependencies and application class` (initial commit; repo had no prior commits)

## Concerns

- **Service role key shipped in the app**: `SUPABASE_SERVICE_ROLE_KEY` is embedded in `BuildConfig` / debug APK. The key itself lives only in gitignored `local.properties` (so not committed), but any client-side inclusion of a service-role key is a security risk. Recommend using only the anon key on device and moving service-role operations to a backend.
- Build emits AGP warning: option `android.disallowKotlinSourceSets=false` is experimental (required for KSP under AGP 9; revisit when KSP ships native built-in-Kotlin integration).

## Report File

`C:\Users\SHAMSI\AndroidStudioProjects\GARMENTO\.superpowers\sdd\2026-09-10-garmento-implementation\task-1-report.md`