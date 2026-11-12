# AuraDock privacy policy

**Effective date:** 12 November 2026 · **Version:** 1.1

## What AuraDock is

AuraDock turns your phone into a calm, useful clock and dashboard when it sits on a stand. It shows the time, weather, your tasks, your calendar and your notifications, and it can switch on Do Not Disturb while the phone is docked.

## The short version

**Your tasks, messages, calendar and contacts stay on your phone.**

AuraDock sends a small amount of technical data to three services: Open-Meteo for weather, Sentry for crash reports, and PostHog for anonymous usage counts. The table below lists exactly what. You can turn off crash reports and usage analytics in Settings.

## Permissions, and why

Every permission is optional. AuraDock asks only when you use the feature, and still works if you say no.

| Permission | What it is used for |
|---|---|
| Calendar | Shows your upcoming events on the dock. Read only. Events never leave your phone. |
| Notification access | Shows notifications on the dock. You choose which apps. Message text is hidden unless you turn it on. Notification content never leaves your phone. |
| Do Not Disturb access | Switches Do Not Disturb on while you are docked, and puts it back when you leave. |
| Location (approximate) | Gets weather for where you are. Only an approximate position is used. |
| Notifications | Lets AuraDock show your task reminders. |
| Alarms and reminders | Lets task reminders go off at the exact minute, and again after a restart. |
| Contacts | Only for the VIP list: people who can always reach you while you are docked. Read lets you pick them. Write is asked only if you turn on "ring through", which marks a VIP as a favourite so Do Not Disturb lets them through, and puts it back afterwards. Names and numbers never leave your phone. |
| Display over other apps | Only for "Dock from anywhere" (off by default). Draws a small pill over other apps when the phone is set on a stand. |

## What we send, and to whom

| Service | What is sent | Why | Where |
|---|---|---|---|
| **Open-Meteo** (weather) | Your position rounded to two decimals (about 1 km). Open-Meteo also sees your IP address, as any website does. No account, no key. | To get the weather | Operated by OpenMeteo GmbH (Switzerland). Their server logs may contain the rounded coordinates and your IP address, and are deleted after 90 days. [Open-Meteo terms](https://open-meteo.com/en/terms) |
| **Android's city lookup** (Geocoder) | Your position rounded to two decimals | To show a city name. This is part of Android, not AuraDock. On most phones it is handled by Google. | Set by your phone |
| **Sentry** (crash reports) | Crashes and freezes, slow-screen and start-up timings, phone model, Android version, app version, and short codes about what the app was doing. No IP address is stored. No personal content. | To find and fix crashes | United States |
| **PostHog** (usage counts) | Anonymous events such as "task added" or "dock session ended", made of codes, counts and length buckets (like "5 to 15 minutes"). A random install number, and the app, Android and phone model details PostHog adds. No profile of you is built. Your IP address is discarded. | To see which features are used | United States |
| **Android backup** (Google) | Your tasks and settings, if you have Android backup turned on. | To bring them back on a new phone | Your Google account |

AuraDock sends nothing else. It contains no ads and no advertising or tracking SDKs.

## What we never collect

- Message or notification content
- Calendar events
- Contact names or numbers, or your VIP list
- Task or intention text
- Focus sessions and the event log
- Media you play (titles, artists)
- Screenshots, or what is on your screen
- Your name, email, account or advertising ID
- A precise location

All of this stays on your phone. Your tasks are also in your Android backup, only if you have backup on.

## Your choices

- **Share crash reports.** In Settings, turn it off and AuraDock stops sending crash data at once.
- **Share usage analytics.** In Settings, turn it off and AuraDock stops sending usage counts at once.
- **Permissions.** Take any permission away at any time in Android's system settings. The feature that needs it stops. The rest keeps working.
- **Backup.** Turn off Android backup in your phone's settings if you do not want your tasks backed up.
- **Uninstall.** Removing AuraDock deletes everything it stored on your phone.

## How long data is kept

- **Sentry:** 30 days, then it is deleted.
- **PostHog:** up to 12 months, then it is deleted.
- **Open-Meteo:** AuraDock does not keep anything there. Open-Meteo's server logs are deleted after 90 days (see its [terms](https://open-meteo.com/en/terms)).
- **On your phone:** kept until you delete it or uninstall AuraDock.
- **Android backup:** kept by Google under your Google account, until you delete it.

Because the usage data is anonymous, we cannot tell which events are yours. We cannot look them up or delete them one by one.

## Children

AuraDock is not directed at children under 13, and we do not knowingly collect data from them.

## Changes to this policy

If we change what AuraDock collects, we will update this page and its version and effective date, and describe the change in the app's release notes. Older versions are in the project's GitHub history.

## Contact

Questions about privacy: **albinvishwas7@gmail.com**
