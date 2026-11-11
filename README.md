# AuraDock

[![CI](https://github.com/AlbinVishwas/auradock/actions/workflows/ci.yml/badge.svg)](https://github.com/AlbinVishwas/auradock/actions/workflows/ci.yml)

A calm standby dock for Android: put the phone on a stand, charging and sideways, and it becomes a clock with your next event, your top task and what you
chose to hear about, with Do Not Disturb on while it is docked. Kotlin, Jetpack Compose, Hilt and Room. Android 8.0 or newer; targets Android 16.

## Build
```bash
./gradlew assembleDebug          # a debug APK in app/build/outputs/apk/debug/
./gradlew testDebugUnitTest lintDebug
```
It builds with no secrets: without `keystore.properties` the release is unsigned, and without the files in `~/.auradock-signing/` (Sentry, PostHog) crash
reporting and analytics are simply off. Nothing from that folder is ever committed.

## More
- [docs/V1_SPEC.md](docs/V1_SPEC.md): what V1 is, and what it is not
- [docs/TESTING.md](docs/TESTING.md): running tests, database schemas, the migration-test rule
- [docs/ANALYTICS.md](docs/ANALYTICS.md): every analytics event (anonymous counts only, never anything you write or receive)
- [docs/SECURITY_REVIEW.md](docs/SECURITY_REVIEW.md): what was checked in the release build
