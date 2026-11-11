- Emulator: Medium_Phone_API_36.1 (Android 16). If no emulator is running, you may start it yourself in the background; never kill it.
- Install and test on the emulator only; never install on a physical device (target adb with `-s emulator-5554`).

## V1 scope
The locked V1 spec is in [docs/V1_SPEC.md](docs/V1_SPEC.md). Read it before starting feature work.
Anything under its "Out of V1" heading (AI layouts, voice, widget customization, recurring tasks,
Quick Settings tile, Play Store release) is not to be built or extended in V1.

## Implementing designs
Any work that changes how a screen looks (including a Claude Design handoff) follows [docs/DESIGN.md](docs/DESIGN.md). In short:
- A handoff in `docs/design/handoff/` is an HTML **reference**, not code to copy. Implement it in Jetpack Compose with the tokens in
  `ui/theme/` (`Color.kt`, `Type.kt`, `Dimens.kt`, `Motion.kt`). Never add a WebView or HTML to the app.
- No colour, font, size, spacing or duration literals outside `ui/theme/`. If a design needs one that has no token, or conflicts with
  the tokens, ask the owner before adding it.
- Restyle only the stateless `XScreen` composables; their ViewModel wiring is in `ui/routes/Routes.kt` and stays untouched.
- Keep the `@Preview`s (full, empty, permission-missing) up to date.


## Sentry
Crash, ANR and performance reporting (Sentry Android SDK, plugin for R8 mapping upload). Code in `app/src/main/java/com/auradock/monitoring/`.
- **No personal content is ever sent.** Not message text, notification content, task or intention titles, event titles, contact names or numbers, media
  titles, user input, screenshots, view hierarchy or session replay; `sendDefaultPii` is off. `Scrubber.kt` allow-lists what leaves the phone and drops the rest.
  Breadcrumbs and non-fatal reports carry codes, states and counts only (`Monitoring.breadcrumb(topic, state, ...)`, `Monitoring.nonFatal(code, ...)`).
  Any new breadcrumb or report must keep to that, and a change to the scrubber needs its unit tests.
- Off in debug builds. The Settings switch "Share crash reports" (on by default) stops all sending at once. The hidden debug page has "Send test events"
  (debug builds only, environment `dev`).
- Secrets live outside the repo in `~/.auradock-signing/sentry.properties` (`dsn`, `auth.token`, `org`, `project`). Never print, log or copy the token; if the file
  is missing the build still works with Sentry off.
- **Reading issues through the MCP:** use the Sentry MCP server (connect it in Claude's connectors; it needs a token with `event:read` and `project:read`,
  which the build token does not have). Search issues in the project by release (`0.3.1 (11)` is versionName plus versionCode) and
  environment (`production` for release builds, `dev` for test events). Non-fatal reports are the issues titled `AuraNonFatal` with an `aura.code`
  tag (for example `dnd.restore_failed`, `vip.still_on_loan`, `dream.no_sensor_reading`); read the breadcrumbs for what led up to it. Stack traces are
  deobfuscated from the uploaded R8 mapping. If the MCP is not connected, read issues in the Sentry web dashboard.
- Sentry dashboard (set once): turn on spike protection, set per-key rate limits on the DSN, and keep inbound filters for legacy browsers and
  localhost on. The app also rate-limits itself (at most 10 of the same issue per device per hour).

## Analytics (PostHog) and feature flags
- Anonymous product analytics through PostHog (`analytics/`). Key and host come from `~/.auradock-signing/posthog.properties` (`key`, `host`, optional `dev.key`);
  without the file the build works with analytics off. Debug builds send nothing unless `adb ... --es cmd analytics_on` was sent to the debug receiver
  (then to the dev project, with `$environment=dev`). The "Share usage analytics" switch in Settings calls `PostHog.optOut()` when turned off.
- Every event goes through `AnalyticsScrubber`, an allow-list. **Properties are only enums, counts and duration buckets, never anything the user typed or received.**
  A new event or property must be added to the scrubber's lists and its test. Full event list: `docs/ANALYTICS.md`.
- Feature flags (`analytics/FeatureFlags.kt`) all default ON when they cannot be read, so the app works offline. A flag set to `false` in PostHog turns the feature off:

  | Flag | Turns off |
  |---|---|
  | `dock_from_anywhere` | the "Dock from anywhere" watcher service |
  | `now_playing` | the Now playing widget's media listener |
  | `world_template` | the World template in the gallery (a stored World choice falls back to Bold) |
  | `ai_features` | reserved for the AI features that are out of V1; nothing reads it yet |
- Surveys: a PostHog survey with one free-text question shows as a sheet on the dashboard only, after 7 days of use. The dock never shows one.
