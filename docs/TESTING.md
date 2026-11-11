# Testing

## Run them
```bash
./gradlew testDebugUnitTest                 # all unit tests, on the JVM (no emulator): about a minute, longer the first time
./gradlew testDebugUnitTest --tests '*AnalyticsScrubberTest*'   # one class
./gradlew lintDebug                         # Android lint; the build must stay at 0 errors
./gradlew testDebugUnitTest lintDebug assembleDebug              # what CI runs on every push and pull request
```
Reports: `app/build/reports/tests/testDebugUnitTest/index.html` (open it in a browser) and `app/build/reports/lint-results-debug.html`.
CI uploads both as artifacts of every run (GitHub, Actions tab, the run, Artifacts).

The first Robolectric run downloads the Android jar it simulates (a few hundred MB, cached afterwards in `~/.m2`), so it is slow once.

## Kinds of tests
- Plain JVM tests (most of them): the app's rules are written without Android in them (`SessionMachine`, `AnalyticsScrubber`, `weeklyFocus`, `alarmPlan`, `planDnd`, ...),
  so they need only JUnit.
- Robolectric tests (`app/src/test/java/com/auradock/robolectric/` and any test with `@RunWith(RobolectricTestRunner::class)`): a simulated Android, for code that needs a
  `Context`, resources, SharedPreferences or a Room database. Use a plain `Application` (`@Config(application = Application::class)`) so Hilt and the
  monitoring and analytics start-up do not run:
  ```kotlin
  import android.app.Application
  import androidx.test.core.app.ApplicationProvider
  import org.junit.runner.RunWith
  import org.robolectric.RobolectricTestRunner
  import org.robolectric.annotation.Config

  @RunWith(RobolectricTestRunner::class)
  @Config(application = Application::class, sdk = [35])
  class SomethingTest { /* ApplicationProvider.getApplicationContext<Application>() */ }
  ```
- Nothing runs on a device in CI. Behaviour on the real phone is in `docs/A55_TEST_CHECKLIST.md`.

## Database schemas and migrations
Room writes every database version as JSON into **`app/schemas/<database class>/<version>.json`** (`exportSchema = true`, `room.schemaLocation` in `app/build.gradle.kts`).
These files are committed and are test assets (`sourceSets.test.assets`), so `MigrationTestHelper` can create a database as an old version and migrate it.

| Database | File | Versions exported |
|---|---|---|
| `AuraDockDatabase` (`auradock.db`, backed up: tasks) | `app/schemas/com.auradock.data.AuraDockDatabase/` | 7 (0.3.1, five tables), 8 |
| `LocalDatabase` (`auradock_local.db`, never backed up: sessions, checkpoint, intention, event log) | `app/schemas/com.auradock.data.LocalDatabase/` | 1 |

Versions 1 to 6 of `AuraDockDatabase` were never exported (the schema export was off before 0.3.2); only 7 onward can be tested.

**The rule: every future migration gets a test.** Changing an `@Entity` or a `@Database` version means, in the same commit:
1. bump `version`, add the `Migration`, and register it in `di/DatabaseModule.kt`;
2. build once so Room writes the new `<version>.json`, and commit it (never edit a schema file by hand, never delete an old one);
3. write a migration test that creates the database at the old version with `MigrationTestHelper.createDatabase(...)`, inserts real rows, runs `runMigrationsAndValidate(...)`
   to the new version, and asserts the rows (and that nothing was lost);
4. the 7 to 8 step is a deliberate no-op: the data move to `auradock_local.db` is done by `DatabaseSplit`, which has its own copy-and-verify rules.
