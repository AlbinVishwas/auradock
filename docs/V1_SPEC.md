# AuraDock V1 Spec

## Core idea
Phone placed horizontally on a charging stand becomes a calm, premium dock screen.
While docked, focus mode silences everything except what matters.
Dock mode is landscape-only.

## Entry points
1. Automatic: AuraDock registers as an Android screensaver (DreamService). When it
   starts, it checks physical orientation via the accelerometer (not display rotation).
   Landscape → show the dock. Portrait or flat → finish() immediately so the phone
   sleeps normally.
2. In-app: a "Dock now" button on the dashboard, beside a thumbnail of the current
   template. Opening the app while charging + landscape also enters dock mode
   (existing StandbyTriggerManager behavior).
3. Optional, off by default: "Dock from anywhere". A small foreground service watches the
   accelerometer while the screen is on. Physically landscape and held still for about
   3 seconds shows a pill over any app: charging gives "Docking in 5..." (tap the pill to dock
   now, or Cancel); not charging gives "Switch to dock?" (tap to dock, hides after about
   6 seconds). It stays quiet during audio or video playback, calls, and while the dock or
   screensaver is showing, and after a Cancel or dismiss until the phone is picked up or
   turned to portrait. Needs "Display over other apps" and shows a small permanent
   notification while on. Plan B if Play rejects the overlay: a heads-up notification with a
   Dock now action.
No Quick Settings tile.

## Dashboard (portrait, the "today" screen)
- Time-based greeting (no name), date, weather
- Today's tasks (add, check off), with due reminders highlighted
- Today's calendar events
- Current template thumbnail + "Dock now" button
- Entry to Template Gallery and Focus settings

## Templates (landscape only, pick one, no customization in V1)
- Classic: big clock, date, weather
- Focus: big clock, next calendar event, top unfinished task; very sparse
- Minimal: clock only, dim OLED
- Productivity: clock, today's tasks, calendar events, weather
All templates use burn-in protection.

## Tasks (simple)
Title + optional time today. Checklist. Notification at the due time. Stored locally.
Out of scope: future dates, repeats, priorities.

## Focus mode
Entering dock mode turns on Do Not Disturb (needs DND access); starred contacts'
calls still ring. Notifications from up to 3 user-picked apps are shown visually on
the dock screen via the notification listener. Leaving dock mode restores the user's
previous DND state.

## Weather
Open-Meteo (free, no key), approximate location, refresh about every 30 minutes,
graceful fallback if denied or offline.

## Onboarding (first launch)
Welcome → set AuraDock as screensaver (deep link to system setting, with
instructions) → permissions explained one at a time, each skippable (calendar,
notification access, DND access, location, post notifications) → pick a template
→ dashboard.

## Out of V1
AI layout generation, voice, widget customization, recurring tasks,
Quick Settings tile, Play Store release.

## Testing
Emulator plus a Samsung Galaxy A55 on the latest Android. Testers get a sideloaded APK.
