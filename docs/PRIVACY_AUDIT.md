# Privacy audit: what leaves the phone (0.3.2, audited 2026-11-12)

Read from the code, not from memory. Sources are named so each line can be checked.

## Permissions (`AndroidManifest.xml`)

| Permission | Used for | Leaves the phone? |
|---|---|---|
| `INTERNET` | Weather, crash reports, analytics | Only to the services below |
| `ACCESS_NETWORK_STATE` | Lets Sentry hold reports until the phone is online | No |
| `ACCESS_COARSE_LOCATION` | Weather and city name (`weather/LocationProvider.kt`, Android `LocationManager`, last known fix, no Play services) | Rounded to 2 decimals to Open-Meteo; see Geocoder below |
| `READ_CALENDAR` | Shows events on the dock (`data/CalendarRepository.kt`) | No |
| `READ_CONTACTS` | Picking VIPs (`focus/vip/VipContacts.kt`) | No (but see finding 1) |
| `WRITE_CONTACTS` | Only if "ring-through" is on: marks a VIP as a favourite so Do Not Disturb lets them through, and puts it back afterwards (`VipContacts.kt`) | No |
| `POST_NOTIFICATIONS` | Task reminders | No |
| `ACCESS_NOTIFICATION_POLICY` | Do Not Disturb while docked | No |
| `SCHEDULE_EXACT_ALARM` | Reminders at the exact minute | No |
| `RECEIVE_BOOT_COMPLETED` | Sets reminders again after a restart | No |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | "Dock from anywhere" motion watcher (optional, off by default) | No |
| `SYSTEM_ALERT_WINDOW` | The pill drawn over other apps (same feature) | No |
| Notification access (a listener service, granted in system settings, not a manifest permission) | Shows notifications on the dock | No |

## Network destinations

| Destination | Sent | Why | Region |
|---|---|---|---|
| `api.open-meteo.com` (`weather/OpenMeteo.kt`) | Latitude and longitude rounded to 2 decimals (about 1 km), plus the weather fields asked for. Open-Meteo sees the phone's IP address, as any server does. No key, no account. | Weather | Open-Meteo is in Europe |
| Android `Geocoder` (`weather/CityLookup.kt`) | **Unrounded** coordinates from the coarse fix. Android's own location is already blurred to about 2 km. | City name | Handled by the phone's system geocoder. On phones with Google services this is Google's backend. AuraDock does not choose it. |
| Sentry, `o4512402043895808.ingest.us.sentry.io` (`monitoring/`) | Crashes, freezes (ANR), 20% of performance traces (app start, dock), slow and frozen frame counts, release-health sessions, device model, Android version, app version, and content-free codes in breadcrumbs. The device name, user and screens are removed (`Scrubber.kt`). `sendDefaultPii` is off. | Fix crashes | US |
| PostHog, `https://us.i.posthog.com` (`analytics/`) | Anonymous events from an allow-list: codes, counts, duration buckets (`AnalyticsScrubber.kt`). Also PostHog's own app lifecycle events (installed, opened, backgrounded, updated), a random anonymous install id, and the standard app, OS and device fields the PostHog SDK adds. Feature-flag and survey requests go to the same host. No person profiles. | Know which features are used; kill switches; one survey | US Cloud |
| Android Auto Backup (Google, if the user has backup on) | `auradock.db` (tasks) and the preference files `auradock_prefs`, `auradock_split`, `auradock_focus_settings` | Restore on a new phone | Google account, set by the user |

Nothing else opens a connection. Cleartext HTTP is blocked (`network_security_config.xml`).

## Never leaves the phone

Notification content, calendar events, contact names and numbers, media info, tasks (except the Android backup), focus sessions, the event log (`auradock_local.db`, excluded from backup), the morning intention (local database).

## Where the code disagrees with the brief

1. **The VIP list is in the Android backup.** `auradock_focus_settings` holds each VIP's contact lookup key and **display name** (`focus/FocusSettings.kt`, `Vip(lookupKey, name)`), and that file is on the backup include list. So with backup on, VIP names go to the user's Google account. Not Google-free, and not "never leaves the phone".
2. **The Geocoder gets unrounded coordinates**, not the 2-decimal ones Open-Meteo gets.
3. **`WRITE_CONTACTS` was not on the list.** It is used only for ring-through, and changes only the favourite flag.
4. **"No IP stored" (Sentry) and "client IP discarded" (PostHog) are dashboard settings**, not things the app can enforce. The app sends no IP itself, but the servers see it on every request. Both settings must be on in the two projects.
5. The Sentry project retention (30 days) and the PostHog retention period are also dashboard settings, not in the code.
