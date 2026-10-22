# AuraDock V1 Audit and Build Plan

Audit of the codebase against [V1_SPEC.md](V1_SPEC.md), and a build order sized for about 15 hours.
Written 2026-10-23 from a read of the code and manifest. Samsung and Android-version notes are from
platform knowledge, not tested; the A55 checkpoint (step 3) exists to confirm them.

## Decisions made after the audit
- **Samsung screensaver:** the A55's Settings > Display > Screen saver lists third-party screensavers
  (Google Photos appears) and only offers "while charging". Assume AuraDock will appear there.
- **Rotation:** do not rely on auto-rotate. When the accelerometer says the phone is physically
  landscape but the window is portrait, the dock content is rotated in Compose so it is always upright.
  Both landscape directions (left and right) are handled.

## Audit by spec section

### Entry point 1: screensaver (DreamService)
- **Status:** missing.
- **Files:** none. The manifest only has `MainActivity` and the notification listener.
- **To build:** `DockDreamService`; deep link via `Settings.ACTION_DREAM_SETTINGS`.

### Entry point 2: in-app dock
- **Status:** partial. `standby/StandbyTriggerManager.kt` works (verified on the emulator).
- **Missing:** the "Dock now" button.
- **Conflicts:** the "Charging + Landscape" dashboard button cycles three trigger modes; the spec has one
  behavior. Remove the cycle, collapse `StandbyTriggerMode`, `cycleTriggerMode`, the prefs persistence
  and their tests. The standby screen is tied to `StandbyViewModel`; a screensaver cannot use that, so
  the dock needs to be fed by shared singletons.

### Dashboard
| Spec item | Status | Where |
|---|---|---|
| Time-based greeting, date | exists | `time/Greeting.kt`, `DashboardScreen.kt` |
| Weather | missing | not on the dashboard |
| Today's tasks (add, check off, due highlight) | missing | "Add Reminder" button is a no-op |
| Today's calendar events | exists | `TodaySummary.kt`, `CalendarRepository.kt` |
| Template thumbnail | exists | `StandbyPreviewCard.kt`, `ScaledPreview` |
| "Dock now" button | missing | |
| Gallery entry | exists | "Customize Standby" button (rename) |
| Focus settings entry | missing | |

Remove: the "Voice Command" button and its pulse animation, voice/generating parameters on
`DashboardScreen`, the trigger-mode button.

### Templates
- **Status:** conflicts. The current system is a general layout engine (`LayoutType`, `WidgetPosition`,
  `WidgetConfig`, `StyleConfig`, `createRenderPlan`, `sanitizedForStandby` in `ui/template/`). V1 wants
  four fixed templates.

| Existing | Closest spec template | Verdict |
|---|---|---|
| Minimal (clock only) | Minimal | close; no OLED dimming |
| Split Weather | Classic | close; weather is fake |
| Focus Calendar (calendar only) | Focus | conflicts: needs clock, next event, top task |
| Grid Mix (clock + notification list "REMINDERS") | Productivity | conflicts: needs tasks, events, weather |

- **Change:** replace the engine with `enum DockTemplate { CLASSIC, FOCUS, MINIMAL, PRODUCTIVITY }`
  and four plain composables sharing the clock. `TemplateGalleryScreen.kt` can mostly stay.
- **Not persisted:** the selected template lives only in `StandbyViewModel` memory.
- **Burn-in:** `ui/components/BurnInProtection.kt` exists but nothing uses it. Wrap the dock root.
- **Remove:** `ai/TemplateGenerator.kt`, `voice/VoiceInputManager.kt`, `ui/viewmodel/VoiceState.kt`,
  the ViewModel functions `generateFromPrompt` and `onVoice...`, voice wiring in `MainActivity`.
  Voice was already broken: the manifest has no `RECORD_AUDIO`. Everything stays in git history.

### Tasks
- **Status:** missing. Room was removed in the earlier cleanup. `ReminderWidget.kt` is misnamed: it
  shows captured notifications, not tasks.
- **Build:** one Room table (title, optional due time, done), repository, dashboard add/check-off UI,
  due highlight, notifications at the due time.

### Focus mode
- **DND:** missing (no `NotificationManager` code, no policy access check, no saved state).
- **Notification mirroring:** partial and conflicting. Listener and cache exist
  (`service/AuraNotificationListenerService.kt`, `data/NotificationRepository.kt`) but the apps are a
  hardcoded allowlist of five (WhatsApp, Gmail, Messages, Slack, Teams), not up to three user-picked
  apps. No picker, no Focus settings screen. The cache is a static inside a `companion object`; make it an
  injected singleton. The dock UI for it is the misnamed `ReminderWidget`.

### Weather
- **Status:** partial, UI only. `ui/widgets/WeatherWidget.kt` draws a card; the data is a hardcoded demo
  in `TemplateRenderer.kt` ("22 degrees, Partly Cloudy, BANGALORE"). No network code, location
  permission or cache. `INTERNET` and Gson are already present, so no new libraries are needed.

### Onboarding
- **Status:** missing. Only scattered pieces exist (`permissions/PermissionNavigation.kt`, the calendar
  permission request).
- **Build:** a stepper, each step skippable: screensaver, calendar, notification access, DND access,
  location, post notifications, exact alarms, then template pick.

### Out of V1, still in the code
Voice and AI (above), and the layout engine's customization flexibility (collapses into the enum).

### Testing
Emulator plus Galaxy A55. Testers need a signed release APK, not the debug build (slower, Play Protect
warnings).

## Technical risks

### 1. DreamService with Compose (highest)
- Compose needs a lifecycle owner and a saved-state owner attached to its view. A `DreamService` is not
  an Activity and has neither, so a plain `ComposeView` crashes. The service must implement both owner
  interfaces and attach them to the view.
- Hilt activity ViewModels do not work here; dock data must come from injected singletons.
  `@AndroidEntryPoint` itself works on a `DreamService`.
- A dream starts after the screen timeout while charging, not the instant you dock.
- Emulator: `adb shell settings put secure screensaver_components <component>`.

### 2. Accelerometer orientation inside a screensaver
- Display rotation is unreliable (rotation lock keeps the display portrait). Read gravity from the
  accelerometer instead; show black until a stable reading; time out after about 1 second.
- If the physical orientation is landscape but the window is portrait (or upside-down), rotate the
  content ourselves (decision above).
- The classifier is a pure function and is unit-tested. Emulator: `adb emu sensor set acceleration x:y:z`.

### 3. DND access
- Needs `isNotificationPolicyAccessGranted`; the user must grant it on a system settings page.
- Save the previous interruption filter and notification policy, and restore both. Persist the saved
  state so a crash or kill does not leave DND on; restore on next launch and when the dream ends.
- Starred-contact calls need the policy set accordingly (which changes the user's own DND settings,
  hence the restore). Recent Android versions changed how app-requested DND behaves; verify on the A55.

### 4. Exact-time task notifications on Android 14+
- `SCHEDULE_EXACT_ALARM` is denied by default for new installs targeting Android 13+. Check
  `canScheduleExactAlarms()`, send the user to the settings page, fall back to an inexact alarm.
  `USE_EXACT_ALARM` is restricted by Play to alarm/calendar apps; avoid.
- `POST_NOTIFICATIONS` is a runtime permission on Android 13+.
- Alarms are cleared on reboot and force-stop: a boot receiver must reschedule
  (`RECEIVE_BOOT_COMPLETED`).
- Notification `PendingIntent`s need `FLAG_IMMUTABLE`.

### 5. Samsung-specific (verify on the A55)
- Screensaver picker: confirmed by the owner (third-party listed, "while charging" only).
- Quick settings often keep the phone portrait-locked: handled by the rotation decision above.
- "Sleeping apps" / "Deep sleeping apps" can delay or block alarms; notification listeners are sometimes
  disconnected. Handle `onListenerDisconnected` with `requestRebind`; tell testers to set AuraDock to
  "Never sleeping".
- Do Not Disturb sits under "Modes and Routines" on newer One UI; the API should still work.
- AMOLED display, so the dim Minimal template is worthwhile; punch-hole cutout affects landscape insets.

## Build order (about 15 hours)

| # | Step | Hours | Total |
|---|---|---|---|
| 1 | Screensaver spike: `DockDreamService` showing the standby screen, lifecycle owners, Hilt, adb enable | 1.5 | 1.5 |
| 2 | Orientation gate: pure classifier + tests, accelerometer in the dream, timeout, rotate content, fake-sensor tests | 1.5 | 3.0 |
| 3 | A55 checkpoint: install, enable screensaver, check the Samsung quirks | 0.5 | 3.5 |
| 4 | Spec cleanup: delete voice/AI/trigger modes/layout engine; `DockTemplate` enum + persistence; burn-in; Classic + Minimal with OLED dim; "Dock now"; shared dock data singletons | 2.0 | 5.5 |
| 5 | Tasks: Room, repository, dashboard add/check-off, due highlight | 1.5 | 7.0 |
| 6 | Task notifications: channel, `POST_NOTIFICATIONS`, exact-alarm check + fallback, boot reschedule | 1.25 | 8.25 |
| 7 | Focus + Productivity templates | 1.0 | 9.25 |
| 8 | Weather: Open-Meteo, approximate location, 30-minute cache, fallback | 1.25 | 10.5 |
| 9 | Focus mode DND: access check, save/restore, starred calls, crash-safe restore | 1.25 | 11.75 |
| 10 | Notification mirroring: app picker (max 3) + dock strip | 1.0 | 12.75 |
| 11 | Onboarding: skippable stepper, screensaver deep link, template pick | 1.25 | 14.0 |
| 12 | Sideload build: signed release APK, A55 pass, buffer | 1.0 | 15.0 |

Saturday: steps 1-5 (about 7 h). Sunday: steps 6-12 (about 8 h).
If short on time, cut in this order: step 10, then trim onboarding to the permissions that matter
(about 12-13 h).
