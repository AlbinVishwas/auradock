# Target API 36 and dependency update (0.3.1)

Google Play needs apps to target Android 16 (API 36). Every change made for it, and why. Checked on the emulator (`emulator-5554`, API 36.1);
screenshots are in `docs/design/v0.3.1/api36/`.

## Toolchain and libraries

| Item | From | To | Why |
|---|---|---|---|
| Gradle | 8.14.5 | 9.9.0 | AGP 9.5 needs Gradle 9.6 or newer |
| Android Gradle Plugin | 8.3.0 | 9.5.1 | supports compileSdk 36; its built-in Kotlin replaces the `kotlin-android` plugin, which is removed |
| Kotlin | 1.9.22 | 2.4.21 | current stable; the Compose compiler is now the `org.jetbrains.kotlin.plugin.compose` plugin (the old `composeOptions` block is gone) |
| KSP | 1.9.22-1.0.17 | 2.3.13 | KSP 2 |
| Hilt | 2.50 | 2.60.1 | AGP 9 support |
| Room | 2.6.1 | 2.8.5 | current stable |
| Coroutines | 1.7.3 | 1.11.0 | current stable |
| Activity Compose | 1.8.2 | 1.13.0 | current stable (edge-to-edge helpers) |
| AppCompat | 1.6.1 | 1.8.0 | current stable |
| Gson | 2.11.0 | 2.15.0 | current stable |
| mockito-kotlin | 5.2.1 | 6.4.0 | current stable |
| Compose BOM | 2024.02.01 | **2026.06.01** | the newest BOM that still compiles against API 36 (2026.08 and later need compileSdk 37) |
| core-ktx | 1.12.0 | **1.18.0** | 1.19 needs compileSdk 37 |
| Lifecycle | 2.7.0 | **2.10.0** | 2.11 needs compileSdk 37 |
| compileSdk / targetSdk | 34 / 34 | 36 / 36 | the Play requirement. The libraries marked in bold are one step behind the very latest only because the latest need compileSdk 37; move them when the target moves to 37 |

## Code changes

- **`rememberRipple` removed** (`QuickActionButton.kt`): the new Compose treats it as an error. It uses Material 3's `ripple()`.
- **A real bug found by lint** (`WeeklyFocus.kt`): `LocalDate.ofInstant` is API 34, but the app supports Android 8 (API 26), so the weekly-focus
  line would have crashed on Android 8 to 13. It now uses `Instant.atZone(zone).toLocalDate()`.
- **Locale read in a composable** (`SplitWidgets.kt`, `SplitWidgetsMore.kt`): now `LocalConfiguration.current.locales[0]`, so a language change redraws.

## Behaviour changes of Android 15 and 16

- **Edge-to-edge is enforced.** `MainActivity` calls `enableEdgeToEdge()` (transparent bars) so every Android version behaves the same. `ui/theme/Insets.kt`
  now reads the status and navigation bar insets from Compose (reactive, no longer a one-off read of the root insets) and treats a design distance as
  measured from the screen edge, never closer than the bar plus a small gap. Fixed: the Templates back arrow sat under the status bar; the bottom of the
  Dashboard, Setup, Focus and Debug scroll areas did not clear the navigation bar; `AuraSheet` and the app picker now add `imePadding()` so a keyboard
  does not cover their fields (the dock's morning question already had it). The dock hides both bars and draws into the camera cutout on the short edge
  (`LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`, put back when it closes), so it is full screen with no bars.
- **Predictive back.** `android:enableOnBackInvokedCallback="true"` on the application, and every back action goes through `BackHandler` or the
  `OnBackPressedDispatcher`. Checked with real Back presses: the new-task sheet (the keyboard takes the first Back, the sheet the second); onboarding
  (Back goes to the step before); the dock (Back leaves it); and, new, Split edit mode: Back closes the Add widget sheet first, then edit mode (without
  saving), then the dock.
- **Foreground service.** `DockWatcherService` keeps `foregroundServiceType="specialUse"` with the `FOREGROUND_SERVICE_SPECIAL_USE` permission and the
  subtype property. Checked: with Dock from anywhere on and "Display over other apps" allowed it starts as a foreground service when the app opens
  (`dumpsys activity services` lists it, target 36), and is stopped when the switch goes off. Starting it is wrapped so a refusal from the background
  only logs.
- **Exact alarms and boot receivers.** Unchanged and rechecked: with the exact-alarm permission an alarm is scheduled as `Exact` (`dumpsys alarm` lists it
  `RTC_WAKEUP`); without it the scheduler falls back to an inexact alarm (covered by the unit tests of `alarmPlan`). The boot receiver keeps
  BOOT_COMPLETED, MY_PACKAGE_REPLACED and the exact-alarm permission change, and stays `exported="false"` (the system still delivers those).
- **Large screens ignore the portrait lock (Android 16, 600 dp and wider).** The manifest keeps `screenOrientation="portrait"` for phones; on a wide window
  the app screens now stay a phone-sized column (`AuraDockSizes.pageMaxWidth`, 600 dp) in the middle, so nothing stretches or breaks when the window
  rotates; the dock is unaffected and fills the screen in landscape. Checked by making the emulator tablet-sized (`wm size 1600x2560`, `wm density 280`,
  800 dp wide) in portrait and landscape: dashboard, rotation and the dock all work. (No tablet AVD could be created: the SDK has no command-line tools.)

## Not changed on purpose

- `minSdk` stays 26.
- Lint reports `OldTargetApi` (36 is one behind the newest, 37) and `LockedOrientationActivity`; both are intended for now.
