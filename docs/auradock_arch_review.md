# AuraDock — Engineering Execution Checklist
### Senior Android System Architecture Review · March 2026

> **Purpose:** Convert the AuraDock product architecture into a phase-by-phase engineering execution checklist. Each item includes a concrete verification condition. This document is the authoritative pre-build, in-build, and pre-launch reference for the engineering team.

---

## Architecture Readiness Score: **6.5 / 10**

> **Summary verdict:** The declared architecture covers the broad strokes well — foreground service, sensor-based standby, widget layout, and reminder system are all correctly scoped. However, several critical gaps exist: no defined widget data contract, no state machine specification for standby triggers, no offline fallback for weather/AI, no deep-link or notification tap routing, and no explicit session/lifecycle policy for the voice pipeline. These gaps must be closed before implementation begins on those subsystems.

---

## Section 1 — Product Architecture Validation

### 1.1 Standby Trigger Logic

- [ ] **Define a formal trigger state machine** with the following states: `IDLE`, `CHARGING_ONLY`, `LANDSCAPE_ONLY`, `CHARGING_AND_LANDSCAPE`, `STANDBY_ACTIVE`, `STANDBY_SUPPRESSED`
  - **Verify:** State diagram reviewed and approved; every edge (enter/exit) has a defined condition and action.
- [ ] **Specify all entry conditions** for each trigger mode:
  - `Charging + Landscape`: `BATTERY_CHANGED(plugged != 0)` AND orientation > 70° from vertical
  - `Charging Only`: `BATTERY_CHANGED(plugged != 0)` regardless of orientation
  - `Landscape Only`: orientation > 70°, battery state irrelevant
  - **Verify:** Unit tests confirm each mode transitions correctly under simulated sensor + broadcast input.
- [ ] **Specify exit conditions** (screen-off, unplug, portrait flip, user swipe-dismiss)
  - **Verify:** Standby screen is reliably dismissed within 500 ms of each exit event.
- [ ] **Handle conflict resolution:** what wins when charging is removed while landscape is still active in `Charging + Landscape` mode?
  - **Verify:** Decision documented in state machine; no ambiguous state exists.
- [ ] **Prevent standby during active use:** detect foreground activity and suppress standby if the user is interacting with the device.
  - **Verify:** Verified by launching another app while conditions are met — standby must NOT activate.

### 1.2 Widget System Architecture

- [ ] **Define a `WidgetSlot` contract** — maximum slots per layout, slot IDs, allowed widget types per slot.
  - **Verify:** A `WidgetSlotConfig` data class exists with slot definitions; no magic numbers in code.
- [ ] **Define a `StandbyWidget` interface** (or sealed class hierarchy) with: `WidgetType`, `refresh()`, `onLayout()`, `onDestroy()`.
  - **Verify:** All widget implementations conform to this contract; no widget reads data inline from its Composable.
- [ ] **Specify widget update cadence per type:**
  - Clock: live (1 s interval)
  - Weather: 30-min refresh (WorkManager)
  - Calendar events: 15-min refresh or `CalendarContract` observer
  - Reminders: event-driven (Room DB observer)
  - Notifications: event-driven (NotificationListenerService callback)
  - **Verify:** Each widget type has a documented refresh policy; no widget polls faster than needed.
- [ ] **Define widget persistence model:** which widgets are saved per template, which are global.
  - **Verify:** Rotating between templates restores correct widget configuration.

### 1.3 Reminder System

- [ ] **Define reminder data schema** (id, title, body, trigger time, repeat rule, source: voice/manual, linked widget slot).
  - **Verify:** Schema covers all use cases; Room entity defined with migration strategy.
- [ ] **Specify NLP parsing library** (Duckling, Natty, or custom regex engine) and fallback behavior when parsing fails.
  - **Verify:** Parse success rate > 90% on a test set of 50 natural language reminder strings.
- [ ] **Define reminder notification channel** and priority level.
  - **Verify:** Notifications appear even when standby screen is active (requires SHOW_ON_LOCK_SCREEN flag).

### 1.4 Notification Filtering

- [ ] **Define filtering criteria:** allowlist by app package, keyword filter, priority threshold (IMPORTANCE_HIGH+).
  - **Verify:** Test set of 20 notifications confirms correct allow/block behavior.
- [ ] **Define notification display policy on standby:** max count, collapse/expand behavior, dismiss interaction.
  - **Verify:** More than N notifications (define N) collapses to a summary view.

### 1.5 Onboarding Flow

- [ ] **Map all onboarding screens:** Welcome → Trigger selection → Permission grant (each permission on its own screen) → Widget selection → Done.
  - **Verify:** Complete onboarding end-to-end without skipping; all permissions granted; standby activates correctly on first trigger.
- [ ] **Define "skip" behavior:** which steps are skippable; what defaults apply when skipped.
  - **Verify:** App functions at a baseline level without any optional permissions.
- [ ] **Define re-onboarding entry point** (Settings screen) for users who denied permissions.
  - **Verify:** Tapping permission explanation in Settings navigates to correct Android settings screen.

### 1.6 Template Gallery

- [ ] **Define template data model:** template ID, name, preview thumbnail resource, slot configuration, theme tokens.
  - **Verify:** Adding a new template requires only a new data entry — zero code changes to the gallery UI.
- [ ] **Specify template loading:** bundled (hardcoded) vs. remote (cloud-fetched).
  - **Verify:** App works fully offline with bundled templates.
- [ ] **Define template selection → application pipeline:** selecting a template must reconfigure the widget slot layout without data loss.
  - **Verify:** Switching templates preserves widget data (clock still shows time, weather still shows last-known data).

### 1.7 Dashboard Design

- [ ] **Define dashboard content model:** greeting (time-aware), upcoming reminders, quick-add reminder, today's calendar summary, notification summary.
  - **Verify:** Dashboard displays correctly for a fresh install (empty state for each section defined).
- [ ] **Define navigation architecture:** Bottom nav vs. NavRail vs. single-screen with drawers.
  - **Verify:** All primary destinations reachable in ≤ 2 taps from any screen.

### 1.8 Voice Command System

- [ ] **Define intent taxonomy:** CREATE_REMINDER, SET_WIDGET, QUERY_WEATHER, QUERY_CALENDAR, OPEN_SCREEN.
  - **Verify:** Intent taxonomy documented; parser maps at least 3 synonym phrasings per intent.
- [ ] **Define fallback for unrecognized intent:** show text confirmation + manual edit flow.
  - **Verify:** Unrecognized command does not crash or show a blank screen.

### Architecture Gap Analysis

> [!WARNING]
> The following components are **missing from the declared architecture** and must be designed before implementation:

| Gap | Impact | Recommended Fix |
|---|---|---|
| No widget data contract (interface/sealed class) | Widget implementations will diverge | Define `StandbyWidget` sealed class before any widget is coded |
| No standby state machine spec | Trigger logic will have bugs at edge cases | Build and review state diagram first |
| No offline fallback for weather | App breaks with no internet | Cache last-known weather; show stale indicator |
| No notification tap routing (deep-link) | Tapping notification on standby screen has no behavior | Define `NotificationAction` → navigate to app |
| No voice session lifecycle policy | Mic may stay open; battery drain | Define `VoiceSession` with explicit open/close lifecycle |
| No crash → standby recovery policy | Foreground service crash leaves no standby | Define restart policy `START_STICKY` + watchdog |
| No accessibility (TalkBack) consideration | Fails accessibility audit | Add `contentDescription` to all standby widgets |

---

## Section 2 — Android System Layer

### 2.1 Foreground Service

- [ ] Create `StandbyMonitorService` extending `Service` with `startForeground()` called within 5 seconds of `onStartCommand()`.
  - **Verify:** ADB: `adb shell dumpsys activity services | grep StandbyMonitor` — service listed as FOREGROUND.
- [ ] Define `NotificationChannel` (`CHANNEL_ID = "auradock_standby"`) with `IMPORTANCE_LOW` and persistent notification.
  - **Verify:** Notification appears in system tray while service is running; user cannot accidentally dismiss it.
- [ ] Declare `<service android:foregroundServiceType="…">` in `AndroidManifest.xml` — use appropriate type (`connectedDevice` or `dataSync` or `specialUse`).
  - **Verify:** App compiles and passes lint; no manifest merger warnings on service type.
- [ ] Handle `onTaskRemoved()` — restart service or notify user.
  - **Verify:** Kill app from recents; service restarts within 10 seconds (test on Android 12+).
- [ ] Return `START_STICKY` from `onStartCommand()`.
  - **Verify:** Force-stop service via ADB; it restarts automatically (on devices that allow it).

### 2.2 Orientation Detection

- [ ] Register `SensorManager` listener for `TYPE_ACCELEROMETER` inside the foreground service.
  - **Verify:** Sensor events fire within 200 ms of physical device rotation.
- [ ] Implement landscape detection logic: compute gravity vector angle; threshold at 70° from vertical (configurable).
  - **Verify:** Unit test with mock sensor data confirms correct angle calculation.
- [ ] Debounce orientation changes with a 500 ms delay to prevent flicker.
  - **Verify:** Rapidly rotating device does not cause standby to activate/deactivate more than once per rotation.
- [ ] Unregister sensor listener when service is paused / stopped to prevent battery drain.
  - **Verify:** ADB `dumpsys sensorservice` shows no registered listener after service stop.

### 2.3 Charging Broadcast Receivers

- [ ] Register `BroadcastReceiver` for `ACTION_POWER_CONNECTED` and `ACTION_POWER_DISCONNECTED`.
  - **Verify:** Plugging/unplugging USB triggers receiver within 1 second.
- [ ] Do NOT register these in `AndroidManifest.xml` for Android 8+ — register dynamically in the foreground service.
  - **Verify:** Lint and code review confirms no manifest-registered implicit broadcast receivers (except exempted ones).
- [ ] Read initial charging state at service start using `registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))`.
  - **Verify:** Service correctly identifies charging state on first run without needing a plug/unplug event.

### 2.4 Background Execution Stability

- [ ] Use `WorkManager` for all deferred background tasks (weather refresh, calendar sync).
  - **Verify:** WorkManager tasks survive device reboot (add `REQUIRES_NETWORK` + `setPeriodic` constraints).
- [ ] Handle `Doze` and `App Standby` modes — ensure foreground service is exempt; WorkManager constraints account for Doze windows.
  - **Verify:** ADB: `adb shell dumpsys deviceidle` — app listed as active/exempted during standby.
- [ ] Test on Android 12, 13, and 14 for background start restrictions.
  - **Verify:** Standby activates correctly on all three OS versions in CI matrix.

### 2.5 Battery Optimization Handling

- [ ] Request `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` via `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` during onboarding.
  - **Verify:** `PowerManager.isIgnoringBatteryOptimizations(packageName)` returns `true` after user grants.
- [ ] Provide a fallback path if user declines — show in-app warning and reduced-functionality notice.
  - **Verify:** App does not crash or force-quit if battery optimization is NOT disabled.
- [ ] Minimize wake locks — do not hold any wake lock longer than necessary.
  - **Verify:** ADB `adb shell dumpsys power | grep WAKE_LOCK` — no persistent AuraDock wake locks after standby exits.

---

## Section 3 — Permission Handling

### 3.1 Notification Listener Service

- [ ] Declare `<service android:name=".NotificationListenerService" android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">` in manifest.
  - **Verify:** Service appears in Settings → Apps → Special App Access → Notification Access.
- [ ] During onboarding, navigate user to `ACTION_NOTIFICATION_LISTENER_SETTINGS` with an explanation screen before navigating away.
  - **Verify:** Explanation screen shown; deep-link opens correct settings page on Android 10, 12, 13.
- [ ] Detect permission revocation at runtime using `NotificationManagerCompat.getEnabledListenerPackages()`.
  - **Verify:** If permission is revoked post-grant, app shows an in-app banner within one app open.
- [ ] **Play Store compliance:** Notification Listener access requires a Permission Declaration Form submission. Include justification: "Filters important notifications to display on standby screen."
  - **Verify:** Declaration form completed and linked to Play Console listing before first internal release.

### 3.2 READ_CALENDAR

- [ ] Request `READ_CALENDAR` at runtime using `ActivityResultContracts.RequestPermission`.
  - **Verify:** Permission rationale dialog shown if user denies once; Settings redirect shown if permanently denied.
- [ ] Handle `READ_CALENDAR` denial gracefully — Calendar widget shows "Calendar access denied" empty state.
  - **Verify:** App does not crash; widget shows correct empty state with a "Grant Access" button.

### 3.3 RECORD_AUDIO

- [ ] Request `RECORD_AUDIO` only at the moment the user first taps the voice command button — not at launch.
  - **Verify:** `RECORD_AUDIO` is NOT requested during onboarding or app start; only triggered on demand.
- [ ] Stop audio recording immediately after intent recognition completes.
  - **Verify:** Microphone indicator in status bar disappears within 2 seconds of recording completion.
- [ ] **Play Store compliance:** Declare RECORD_AUDIO use in Data Safety section: "Audio is processed on-device and not shared."
  - **Verify:** Data Safety form completed accurately before submission.

### 3.4 Battery Optimization Exemption

- [ ] Show a plain-language explanation screen before launching `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
  - **Verify:** Explanation screen shown; user can decline without app breaking.
- [ ] **Play Store compliance:** Battery optimization exemption requires a valid justification in the app's Play Store description. Justification: "Required to maintain always-on standby monitoring in the background."
  - **Verify:** Justification included in app description and permission declaration.

---

## Section 4 — UI System Implementation

### 4.1 Compose Theme System

- [ ] Define `AuraDockTheme` with `MaterialTheme` wrapper — include `ColorScheme`, `Typography`, `Shapes`.
  - **Verify:** No hardcoded color values anywhere in Composable code; all colors reference theme tokens.
- [ ] Define `StandbyTheme` as a sub-theme (dark, AMOLED-black base) distinct from the main app theme.
  - **Verify:** Standby screen renders correctly on AMOLED screens with true black (`#000000`) background.
- [ ] Support dynamic color (Material You) as an opt-in feature for Android 12+.
  - **Verify:** `DynamicColorScheme` applied correctly on Android 12 devices; fallback static theme on Android < 12.
- [ ] Define all spacing/sizing as named design tokens (`SpacingXs`, `SpacingSm`, etc.) — no magic numbers.
  - **Verify:** Code review: zero `.dp` literals outside the design token file.

### 4.2 Dashboard Screen

- [ ] Implement time-aware greeting: "Good morning / afternoon / evening, [name]"
  - **Verify:** Greeting changes correctly at 12:00 PM and 6:00 PM boundaries.
- [ ] Implement today's summary section: next reminder, next calendar event, notification count.
  - **Verify:** Section shows correct empty states individually when each data source is empty.
- [ ] Implement quick-add reminder FAB with bottom sheet input.
  - **Verify:** Reminder created via FAB appears in reminder list and standby widget within 2 seconds.
- [ ] Implement Settings navigation from dashboard app bar.
  - **Verify:** All settings screens reachable; back navigation works correctly on all screens.

### 4.3 Onboarding Flow

- [ ] Implement onboarding as a `HorizontalPager` with `OnboardingPage` sealed class for each step.
  - **Verify:** Hardware back button on page 1 shows exit confirmation; back on other pages navigates to previous page.
- [ ] Each permission step: show purpose → show request → handle grant/deny → continue.
  - **Verify:** Denial on any step does not block onboarding progression; rationale is stored for later in-app prompt.
- [ ] Persist onboarding completion status in `DataStore<Preferences>`.
  - **Verify:** Re-launching app after onboarding does not show onboarding again.

### 4.4 Template Carousel

- [ ] Implement as a `LazyRow` with `TemplateCard` items showing preview thumbnail + name.
  - **Verify:** Carousel scrolls smoothly (60fps) with 10+ templates on a mid-range device.
- [ ] Implement `TemplateCard` selection state with animated border highlight.
  - **Verify:** Only one template card is selected at a time; tapping a new card deselects the previous.
- [ ] Tapping "Apply" saves the selected template and navigates to the widget editor for that template.
  - **Verify:** Widget editor loads the correct slot configuration for the selected template.

### 4.5 Widget Editor

- [ ] Implement drag-and-drop slot assignment using `Modifier.pointerInput` or a dedicated DnD library.
  - **Verify:** Dragging a widget type onto a slot assigns it; dropping outside a valid slot resets to previous state.
- [ ] Show a live preview of the standby screen that updates as slots are configured.
  - **Verify:** Preview updates within 300 ms of any slot change.
- [ ] Implement "Reset to template defaults" action with confirmation dialog.
  - **Verify:** Reset restores original template configuration; undo is available for 5 seconds post-reset.

### 4.6 Standby Screen Layout

- [ ] Implement standby screen as a `FullScreenComposable` with `WindowInsetsController` to hide status/nav bars.
  - **Verify:** No status bar or navigation bar visible during standby on Android 11+.
- [ ] Implement pixel-shift burn-in protection (shift entire content by ±2 px every 60 seconds).
  - **Verify:** Content shifts are imperceptible to the user but measurable in automated screenshot diff tests.
- [ ] Implement Screen Dim mode: reduce brightness to 20% after 5 minutes of inactivity during standby.
  - **Verify:** `WindowManager.LayoutParams.screenBrightness` set correctly; restored on standby exit.
- [ ] Implement touch-to-wake: any tap wakes normal screen; swipe-up dismisses standby completely.
  - **Verify:** Single tap wakes device; swipe-up dismisses standby and returns to last active app.

### 4.7 Component Design System

- [ ] Define `GlassCard` Composable with frosted glass effect (`blur + translucent background`).
  - **Verify:** `GlassCard` renders correctly on all theme variants; no visual artifacts at card edges.
- [ ] Define `StandbyClockWidget`, `StandbyWeatherWidget`, `StandbyCalendarWidget`, `StandbyRemindersWidget`, `StandbyNotificationsWidget` as independent Composables.
  - **Verify:** Each widget Composable can be rendered in isolation in a Compose Preview with mock data.
- [ ] Define loading and error states for every widget that fetches data.
  - **Verify:** Disconnecting internet shows weather/calendar widgets in error state, not blank/crashed.

---

## Section 5 — Widget System

### 5.1 Widget Slot Architecture

- [ ] Define `WidgetSlot` data class: `slotId: String`, `allowedTypes: List<WidgetType>`, `position: SlotPosition`.
  - **Verify:** There are no slot IDs duplicated across templates.
- [ ] Define `WidgetType` enum: `CLOCK`, `WEATHER`, `CALENDAR`, `REMINDERS`, `NOTIFICATIONS`, `EMPTY`.
  - **Verify:** All 5 widget types are implemented; `EMPTY` renders a placeholder without crashing.
- [ ] Define `TemplateSlotConfig` — a mapping of slot IDs to widget types for each template.
  - **Verify:** Loading any template config correctly populates the standby screen without code changes.

### 5.2 Widget Lifecycle

- [ ] Each widget must implement: `onCreate()` (register observer), `onRefresh()` (update data), `onDestroy()` (unregister observer).
  - **Verify:** No memory leaks — LeakCanary shows no retained widget references after standby exits.
- [ ] Widget data flows through `StateFlow` → Composable — no side effects in Composable bodies.
  - **Verify:** Code review: zero `LaunchedEffect` blocks that initiate network calls inside widget Composables.

### 5.3 Widget Data Updates

- [ ] **Clock:** `Flow` from `ticker` coroutine every 1 second, collected in `ViewModel`.
  - **Verify:** Clock updates correctly across midnight, noon, and DST transitions.
- [ ] **Weather:** `WorkManager` `PeriodicWorkRequest` every 30 minutes; result written to Room cache.
  - **Verify:** Weather widget shows last-cached data when offline; stale timestamp displayed.
- [ ] **Calendar:** `ContentResolver` observer on `CalendarContract.Events.CONTENT_URI`.
  - **Verify:** Adding a calendar event on the device updates the widget within 15 seconds without app restart.
- [ ] **Notifications:** `NotificationListenerService.onNotificationPosted/Removed()` → `SharedFlow`.
  - **Verify:** Posting a test notification from a test app updates the widget within 1 second.

### 5.4 Widget Rendering Performance

- [ ] All widget Composables must be `@Stable` or use `remember`/`derivedStateOf` correctly to avoid unnecessary recomposition.
  - **Verify:** Compose layout inspector shows zero unnecessary recompositions during a 60-second standby session.
- [ ] Widget backgrounds use `Canvas` or `BlurMaskFilter` — not a fixed bitmap — to support all screen resolutions.
  - **Verify:** No pixelation on 1080p and 1440p devices.

---

## Section 6 — Reminder System

### 6.1 Natural Language Parsing

- [ ] Integrate NLP parsing library (recommendation: **Natty** for on-device, or a custom tokenizer for zero-dependency build).
  - **Verify:** Parse the following test cases correctly: "tomorrow at 3pm", "every Monday at 9am", "in 2 hours", "next Friday evening".
- [ ] Define fallback: if parse confidence < threshold, show manual date/time picker pre-filled with best guess.
  - **Verify:** Ambiguous input ("remind me later") opens manual picker, not creates a wrong reminder.
- [ ] Validate parsed time is in the future before saving.
  - **Verify:** Input "yesterday at 5pm" shows error, not a saved reminder.

### 6.2 Voice Reminder Creation

- [ ] Open `SpeechRecognizer` session from within the voice command flow.
  - **Verify:** Microphone opens within 500 ms of tapping voice button; visual waveform indicator shown.
- [ ] Transcribed text passed to NLP reminder parser.
  - **Verify:** "Remind me to call mom tomorrow at 9am" creates a correct reminder without user editing.
- [ ] Show confirmation card before saving: "Remind you to call mom · Tomorrow, 9:00 AM · [Save] [Edit]"
  - **Verify:** User can edit or cancel before saving; cancellation does not create a reminder.

### 6.3 Reminder Storage

- [ ] Define `ReminderEntity` Room entity with: `id`, `title`, `body`, `triggerTimeMs`, `repeatRule`, `source`, `isCompleted`.
  - **Verify:** Room schema validated; migration from version 1 → 2 tested with existing data.
- [ ] Observe reminder list via `Flow<List<ReminderEntity>>` in dashboard ViewModel.
  - **Verify:** Adding a reminder immediately updates dashboard list without manual refresh.

### 6.4 Notification Scheduling

- [ ] Schedule reminder notifications using `AlarmManager.setExactAndAllowWhileIdle()` for Android 6+.
  - **Verify:** Reminder fires exactly at scheduled time (± 5 seconds) even when device is in Doze mode.
- [ ] Request `SCHEDULE_EXACT_ALARM` permission on Android 12+ during onboarding.
  - **Verify:** `AlarmManager.canScheduleExactAlarms()` returns `true` before scheduling.
- [ ] On cancelled or completed reminder, cancel corresponding `PendingIntent`.
  - **Verify:** Deleted reminder does not fire a notification.

---

## Section 7 — AI Voice System

### 7.1 Speech Recognition

- [ ] Use Android `SpeechRecognizer` (on-device, no network required for basic recognition on Android 10+).
  - **Verify:** Voice input works in airplane mode (on supported devices).
- [ ] Implement `RecognitionListener` with all callbacks: `onResults`, `onError`, `onEndOfSpeech`.
  - **Verify:** `ERROR_NO_MATCH` shows a human-readable "Didn't catch that — try again" message.
- [ ] Implement 5-second silence timeout — auto-stop recording if no speech detected.
  - **Verify:** Leaving mic open with no speech automatically closes session within 5 seconds.

### 7.2 Intent Parsing

- [ ] Map transcribed text to intent taxonomy (Section 1.8) using keyword matching + regex.
  - **Verify:** Test matrix of 20 voice inputs covers all 5 intent types; 90%+ correct classification.
- [ ] Implement `IntentResult` sealed class: `Success(intent, params)`, `Ambiguous(candidates)`, `Unknown`.
  - **Verify:** `Unknown` result never crashes the app; always shows a fallback UI.

### 7.3 Widget Generation via Voice

- [ ] Recognize "Add [widget type] to my standby" command → invoke widget editor with pre-selected type.
  - **Verify:** "Add weather widget" navigates to widget editor with Weather pre-selected.
- [ ] Confirm action before applying: "Add Weather widget to Slot 2 — [Confirm] [Cancel]"
  - **Verify:** Cancel leaves standby configuration unchanged.

### 7.4 Reminder Generation via Voice

- [ ] Voice → transcription → NLP parse → confirmation card → save (Section 6.2 pipeline).
  - **Verify:** End-to-end flow tested with 10 realistic reminder phrases; all result in correct reminders.

---

## Section 8 — Performance and Stability

### 8.1 Standby Activation Latency

- [ ] Measure time from trigger condition met → standby screen fully visible (target: < 1 second).
  - **Verify:** Use `Trace.beginSection/endSection` in the activation path; measure with Perfetto. P95 < 1000 ms.
- [ ] Optimize standby `Activity` launch: use `android:launchMode="singleTask"` and pre-warm via service if possible.
  - **Verify:** Repeated activations (10x) average < 800 ms.

### 8.2 Battery Consumption

- [ ] Measure battery drain with standby active for 2 hours on a 4000 mAh device.
  - **Verify:** Battery Historian shows AuraDock responsible for < 3% drain over 2 hours.
- [ ] Ensure sensor listener uses `SENSOR_DELAY_NORMAL` (not `FASTEST`) unless orientation precision demands it.
  - **Verify:** `SensorManager.registerListener(..., SensorManager.SENSOR_DELAY_NORMAL)` confirmed in code.
- [ ] Disable animations and reduce widget refresh rate when battery is < 20%.
  - **Verify:** `BatteryManager.BATTERY_PROPERTY_CAPACITY` check is in widget scheduler; refresh rate halved below 20%.

### 8.3 Reliable Background Operation

- [ ] Foreground service must survive: device restart (`RECEIVE_BOOT_COMPLETED` broadcast), app update, low-memory kills.
  - **Verify:** Reboot device with service configured; verify service auto-starts within 60 seconds of boot.
- [ ] Implement health-check log: log service heartbeat every 5 minutes to detect silent crashes.
  - **Verify:** Logcat shows heartbeat entries; gap > 10 minutes triggers in-app warning on next open.
- [ ] Test on 3 OEM skins: Samsung One UI, Xiaomi MIUI, Oppo ColorOS — known for aggressive process killing.
  - **Verify:** Standby service survives 30 minutes of device idle on each OEM skin.

### 8.4 Crash Resilience

- [ ] Integrate crash reporting (Firebase Crashlytics or equivalent) from day 1 of development.
  - **Verify:** Test crash in debug build — report appears in Crashlytics within 2 minutes.
- [ ] Wrap `NotificationListenerService` callbacks in `try/catch` — a crash here kills notification access.
  - **Verify:** Inject a deliberate exception in a mock notification callback — service stays alive.
- [ ] Implement global `Thread.setDefaultUncaughtExceptionHandler` to log standby-context crashes separately.
  - **Verify:** Standby-context crashes are tagged and filterable in crash dashboard.

---

## Section 9 — Launch Readiness

### 9.1 Android Policy Compliance

- [ ] Complete Play Store **Permission Declaration Form** for: Notification Listener Service.
  - **Verify:** Form submitted; no policy violation email received within 7 days of submission.
- [ ] Complete **Data Safety Form**: declare no user data collected or shared; audio not sent to servers.
  - **Verify:** Data Safety section published on Play Store listing.
- [ ] Verify no usage of restricted APIs (non-SDK interfaces).
  - **Verify:** `adb shell am start-activity --veridex …` or Veridex tool shows zero restricted API calls.
- [ ] `targetSdkVersion` = 35 (Android 15); `minSdkVersion` = 26 (Android 8).
  - **Verify:** App compiles and runs on Android 8 emulator without crashes.

### 9.2 Permission UX Polish

- [ ] Every permission request is preceded by an in-app rationale screen explaining why it's needed, in plain language.
  - **Verify:** UX review: non-technical tester understands each permission rationale without reading documentation.
- [ ] Permissions that are denied are gracefully degraded — no feature crashes without a permission.
  - **Verify:** Install fresh, deny all permissions, open app — zero crashes.

### 9.3 UX Polish

- [ ] All interactive elements have minimum touch target size of 48×48 dp.
  - **Verify:** Accessibility Scanner shows no small touch target violations.
- [ ] All text meets WCAG AA contrast ratio (4.5:1 for body text, 3:1 for large text).
  - **Verify:** Accessibility Scanner or contrast checker tool reports zero violations.
- [ ] All Composables have `contentDescription` for TalkBack compatibility.
  - **Verify:** Enable TalkBack; navigate the full app without visual reference — all elements are described.
- [ ] Loading states, empty states, and error states defined and implemented for every data-driven screen.
  - **Verify:** Kill network; open every screen — no screen shows a blank view or crashes.

### 9.4 Pre-Release Testing Matrix

| Test Type | Tool | Target |
|---|---|---|
| Unit tests | JUnit5 + MockK | > 80% coverage on ViewModels, Repositories, NLP parser |
| Integration tests | Robolectric | Service start/stop, broadcast handling |
| UI tests | Compose Test + Espresso | Full onboarding, standby activation, reminder creation |
| Performance | Perfetto + Macrobenchmark | Activation latency, scroll fps |
| Crash testing | Firebase Test Lab | 10 physical devices, 30-min Robo test |
| OEM compatibility | Physical devices | Samsung, Xiaomi, OnePlus |
| Accessibility | Accessibility Scanner | Zero violations |

### 9.5 Release Artifacts

- [ ] `release` build signed with production keystore (stored securely — NOT in repository).
  - **Verify:** APK signature verified with `apksigner verify --verbose`.
- [ ] ProGuard/R8 rules defined for all reflection-dependent libraries (Room, Moshi, etc.).
  - **Verify:** Release build tested end-to-end — no `ClassNotFoundException` in production flows.
- [ ] Version code and version name follow semantic versioning (`1.0.0` → `versionCode 10000`).
  - **Verify:** Version visible in About screen; matches Play Store submission.

---

## Architecture Improvement Recommendations

| Priority | Recommendation | Rationale |
|---|---|---|
| 🔴 Critical | Define `StandbyWidget` sealed class before writing any widget code | Prevents architectural divergence across widget implementations |
| 🔴 Critical | Build and review standby trigger state machine before implementing the service | Edge cases (rapid orientation changes, partial triggers) will cause bugs without a formal spec |
| 🔴 Critical | Define offline fallback for weather and calendar data | Network-dependent widgets will crash the standby experience for offline users |
| 🟠 High | Add `VoiceSession` lifecycle management spec | Microphone held open is a battery drain and a privacy concern |
| 🟠 High | Design the notification tap action / deep-link routing | Without this, tapping the standby notification widget does nothing — a major UX gap |
| 🟡 Medium | Add `MorningBriefing` as a time-gated dashboard component (7–9 AM only) | Currently listed in product overview but absent from architecture spec |
| 🟡 Medium | Define a remote config layer (Firebase RemoteConfig or local JSON) for template gallery | Enables new templates without an app update |
| 🟢 Low | Add a "Widget Interaction Log" for analytics | Helps understand which widgets are most used; informs future prioritization |

---

*Document version 1.0 · Prepared for AuraDock Engineering Team · March 2026*
