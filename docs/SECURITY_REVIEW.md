# AuraDock security review (0.3.1)

Scope: the signed release APK built from this tree, plus the manifest, backup, logging and network configuration.
Tools: `aapt2 dump xmltree` (manifest), `dexdump -d` (all of `classes.dex`), and a byte search of every file in the APK for the Sentry
auth token and the keystore passwords. (jadx and apktool are not installed on the build Mac; dexdump shows every class, string and call
in the dex, which is what this review needs. Nothing was installed to do it.)

## 1. Secrets in the APK

| Check | Result |
|---|---|
| Sentry auth token (from `~/.auradock-signing/sentry.properties`) anywhere in the APK, any file | **Not present** |
| Keystore store password / key password anywhere in the APK | **Not present** |
| Sentry DSN | Present in `classes.dex` (expected: it is a public ingest key, it can only send events) |
| `http://` URLs in code | None. The only `http://` strings are the Android XML namespace and library docs URLs |
| `https://` endpoints in code | `api.open-meteo.com` (weather), the Sentry DSN host, plus documentation links inside libraries |
| Debug endpoints, localhost, staging hosts | None found (only `/debug` as a substring of library class paths) |
| Debug-only receiver (`TestMediaReceiver`, `TEST_MEDIA`) | Not in the release APK (it lives in `src/debug`) |
| "Send test events" | The row is in the hidden debug page; `Monitoring.sendTestEvents` returns at once unless `BuildConfig.DEBUG`, which is a constant `false` in release, so nothing can be sent |
| `debuggable` | Not set (release) |

How the token stays out: it is read only by the Gradle build (`app/build.gradle.kts`) from outside the repo and handed to the Sentry Gradle
plugin for the mapping upload. It is never written to `BuildConfig`, resources or the manifest. Only the DSN is compiled in.

## 2. Backup (`allowBackup` replaced by explicit rules)

`android:allowBackup` stays `true` so tasks and settings survive a new phone, but it is governed by `dataExtractionRules`
(Android 12+, cloud backup and device transfer) and `fullBackupContent` (Android 11 and older). Both are include lists, so anything not
listed is not backed up.

| Backed up | File |
|---|---|
| Tasks | `databases/auradock.db` (+ `-wal`, `-shm`) |
| Settings, template choice, Bold colour preset, extra cities | `auradock_prefs`, `auradock_focus_settings` |
| Stacks | `auradock_split` |

| Never backed up | Where it lives |
|---|---|
| Focus sessions, the open-session checkpoint, the morning intention, the event log | `databases/auradock_local.db` (since 0.3.2) |
| DND restore state | `auradock_focus_state` prefs (synchronous write before DND is touched) |
| VIP loan journal | `auradock_vip_loan` prefs |
| Weather cache | `auradock_weather` prefs |
| Sentry's cache and the "Share crash reports" flag | `cache/`, `auradock_monitoring` prefs |

0.3.1 kept sessions and the event log in `auradock.db` and emptied them after a restore (`RestoreCleanup`). 0.3.2 splits the database
instead: `DatabaseSplit` copies the four usage tables into `auradock_local.db`, checks the row counts, then drops them from
`auradock.db`; it is safe to repeat and keeps the old tables if anything fails. `RestoreCleanup` is removed. Settings, stacks, the
colour preset and cities are SharedPreferences, not database rows, and stay in the backed-up preference files. The DND/VIP restore
state and weather cache stay in their (never backed up) preference files rather than the local database: the DND restore must be on
disk synchronously before Do Not Disturb is changed, and Room does not allow that on the main thread.

## 3. Exported components

Only these are exported, and each is guarded by the system permission it needs:

| Component | Why |
|---|---|
| `MainActivity` | Launcher |
| `DockDreamService` | `BIND_DREAM_SERVICE` (screensaver) |
| `AuraNotificationListenerService` | `BIND_NOTIFICATION_LISTENER_SERVICE` |

Everything else is `exported=false`: `TaskAlarmReceiver`, `TaskBootReceiver`, `DockWatcherService`, and the library providers.
One finding: AndroidX `ProfileInstallReceiver` was merged in exported (guarded by `android.permission.DUMP`, so only adb could call it).
Nothing in AuraDock needs it, so it is removed from the manifest (`tools:node="remove"`); baseline profiles are still installed by the
`ProfileInstallerInitializer`. Verified: the release manifest has exactly three `exported=true` entries.

## 3b. PendingIntents

| Where | Flags | Mutable? |
|---|---|---|
| `TaskAlarmScheduler` (alarm, and the `FLAG_NO_CREATE` lookup used to cancel) | `IMMUTABLE` (+ `UPDATE_CURRENT`) | No |
| `TaskNotifier` (open the app from a task notification) | `IMMUTABLE` + `UPDATE_CURRENT` | No |
| `DockWatcherService` (foreground notification tap) | `IMMUTABLE` + `UPDATE_CURRENT` | No |

All are `FLAG_IMMUTABLE`; none needs to be mutable (no inline reply, no bubble, no extras filled in by the receiver).

## 4. Logging

Release builds remove `Log.v` and `Log.d` through R8 (`-assumenosideeffects ... return 0`, so calls whose result is used are removed too).
Verified in the dex: 0 calls to `Log.d` or `Log.v` remain; 18 `Log.i` remain. The tags the A55 checklist reads
(`DockDream`, `DockWatcher`, and `SessionTracker`) now log at info (`Log.i`), so `adb logcat -s DockDream DockWatcher SessionTracker`
still works on a release build. None of the messages contain task text, contact names, notification text or media titles (they carry
states, counts, durations and orientation codes).

## 5. Network

- `network_security_config.xml`: `cleartextTrafficPermitted="false"`, and `usesCleartextTraffic="false"` in the manifest.
- Only two hosts are used: Open-Meteo (HTTPS) and Sentry (HTTPS DSN). The weather client builds its URL from a hard-coded `https://` string.
- No custom trust managers, no certificate pinning (not needed; system CAs only).

## 6. Other observations (no action taken)

- Sentry sends crash and performance data with no personal content: `sendDefaultPii` off, no screenshots, no view hierarchy, no session
  replay, and the scrubber drops message text, titles, names and input (see `monitoring/Scrubber.kt`).
- The notification listener sees notification content by design; the app keeps it in memory for the dock and does not persist or send it.
