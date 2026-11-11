# Analytics (PostHog)

Anonymous product analytics. **Never** a task, intention, notification, contact, event or media title, or anything the user typed or received.
Everything below passes through `AnalyticsScrubber` (`analytics/AnalyticsScrubber.kt`), an allow-list: an event not listed here is dropped whole, a
property not listed for it is dropped, and a value must be a fixed code, a count or a duration bucket. The unit tests in `AnalyticsScrubberTest` prove
free text (titles, notification text, contact names, numbers) is rejected under every property name.

## How it is set up
- Key and host: `~/.auradock-signing/posthog.properties` with `key=phc_...`, `host=https://us.i.posthog.com` (or the EU host), and optionally `dev.key=` for a
  separate "dev" project. The file is outside the repo and never committed; without it the build works with analytics off. The key is a public ingest key
  (like Sentry's DSN), so it is compiled into the app.
- `personProfiles = NEVER` (anonymous until accounts exist), session replay off, exception autocapture off (Sentry owns crashes), screen-view capture off,
  application lifecycle events on (PostHog's own `Application Installed`, `Application Updated`, `Application Opened`, `Application Backgrounded`).
- Batched: sent every 20 events or 60 s, and flushed when the app goes to the background and when a dock closes.
- Debug builds send nothing unless switched on (`adb shell am broadcast -a com.auradock.TEST_MEDIA --es cmd analytics_on -n com.auradock/com.auradock.debug.TestMediaReceiver`,
  then restart). Then events go to the dev project (`dev.key`) or, without one, to the main project, with `$environment = dev` on every event: filter it out of real dashboards.
- Settings → "Share usage analytics" (on by default). Turning it off calls `PostHog.optOut()` immediately.
- PostHog's IP capture: also turn on "Discard client IP data" in the PostHog project settings, so IP addresses are not stored.

## Events
Property values are lower-case codes. `duration` is a bucket: `under_5_min`, `5_to_30_min`, `30_to_120_min`, `over_120_min`.

| Event | Properties | When |
|---|---|---|
| `onboarding_step` | `step`: welcome, preview, template, screensaver, anywhere, done | each onboarding page shown, and `done` at the end |
| `permission_granted` | `permission`: calendar, notification_access, dnd_access, location, post_notifications, exact_alarms, contacts | the first time a permission is seen allowed |
| `permission_skipped` | `permission` | "Skip" on a permission sheet |
| `screensaver_set` | none | the first time AuraDock is the selected screensaver (once ever) |
| `first_dock` | `source`: dock_now, screensaver, watcher | the first dock ever (once ever) |
| `template_changed` | `template`: bold, classic, focus, minimal, productivity, world, split | a template chosen |
| `session_start` | `source` | a dock opens |
| `session_end` | `duration` (bucket) | a dock session ends (all sessions, including ones under two minutes that are not saved) |
| `task_added` | none | a task added |
| `task_done` | none | a task ticked |
| `intention_set` | none | the morning intention saved |
| `intention_done` | none | the intention marked done |
| `shutdown_used` | none | the evening shutdown used |
| `focus_undone` | none | "Undo focus" used on the dock |
| `widget_added` | `widget`: dial, bold_time, digital, month, big_date, up_next, weather, now_playing, tasks, focus; `side`: left, right | a widget added to a Split stack |
| `widget_removed` | `widget`, `side` | a widget removed |
| `widget_reordered` | `side` | a stack reordered |

Checked on the release build (0.3.2, emulator, no PostHog key so nothing is delivered): a full onboarding plus one 150 s dock session produced, in order,
`permission_granted`, `onboarding_step` (several), `template_changed`, `task_added`, `permission_skipped`, `session_start`, `first_dock`, `intention_set`,
`session_end`. The intention text typed during the session appears nowhere.

## Feature flags
All default **on** when they cannot be read (no key, offline, opted out). Set a flag to `false` in PostHog to turn the feature off.

| Flag | Controls |
|---|---|
| `dock_from_anywhere` | the watcher service that opens the dock from any app |
| `now_playing` | the Now playing widget's media listener |
| `world_template` | whether World is offered (a stored World choice falls back to Bold) |
| `ai_features` | reserved for AI features (out of V1); nothing reads it yet |

## Surveys
Create a PostHog survey with **one free-text question** ("What's missing?"), type "API" (mobile), and target it as you like. AuraDock shows it as a sheet on the
**dashboard only** (never on the dock or screensaver), and only after the app has been used for 7 days. Surveys with other question types are closed without being shown.
Survey answers are typed on purpose, in reply to your question, and are sent by PostHog's own survey event, not through the scrubber.

## Dashboard to build in PostHog
1. **Activation funnel** (Funnels): `Application Installed` → `screensaver_set` → `first_dock`, conversion window 24 hours, then a second step from install to first dock
   within 24 h. Break down by `source` on `first_dock`.
2. **Habit** (Trends or Retention): unique users with `session_start` on at least 5 of the last 7 days. In Trends: `session_start`, "unique users", daily, with the
   "active on N days" aggregation set to 5 or more over a rolling 7-day window. As retention: returning event `session_start`, daily.
3. **Onboarding drop-off** (Funnel, ordered): `onboarding_step` where `step` = welcome → preview → template → screensaver → anywhere → done. Each step is the same event
   with a different `step` filter. The biggest drop between two bars is where people give up.
4. **Top templates** (Trends, bar): `template_changed` broken down by `template`, plus the current choice as `first_dock` ... or count unique users per `template`.
5. **Top widgets** (Trends, bar): `widget_added` broken down by `widget`; add `widget_removed` broken down by `widget` as a second series to see what is dropped.
6. **Focus hours per active user**: `session_end` with `duration` as a breakdown (counts per bucket). Each session is in one bucket, so approximate hours per user as
   (under 5 min × 0.05 h + 5 to 30 × 0.3 h + 30 to 120 × 1 h + over 120 × 2.5 h) per unique user in a formula insight, using the midpoint of each bucket. The exact
   length is deliberately not sent.
7. Filter every insight with `$environment` not equal to `dev`.
