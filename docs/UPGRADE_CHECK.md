# Upgrade checks

How an update is checked to keep the user's data. Done on `emulator-5554` only.

## Method
1. Build the previous release's code in a separate worktree and install it (`adb -s emulator-5554 install`).
2. Onboard, add tasks, and record a real Dock now session of over two minutes. Seed what the UI cannot make quickly by editing the app's
   files through `run-as` (debug builds): two more sessions (screensaver 40 min, watcher 15 min), then the Bold colour preset (`bold_palette`),
   extra cities (`world_cities`) and Split stacks (`stacks`) in the preference files.
3. Install the new build over it (same signature, `install -r`; no uninstall) and start it.
4. Compare: the databases (tables and row counts), the preference files (byte for byte), and the dashboard.

## 0.3.1 to 0.3.2 (debug builds, 11 Nov 2026)
| Data | Before | After |
|---|---|---|
| Tasks (`auradock.db`) | 2 | 2 |
| Focus sessions (now `auradock_local.db`) | 3 (Dock now 134 s, screensaver 2400 s, watcher 900 s) | 3, same sources and lengths |
| Event log (`app_event`) | 18 | 18 |
| Morning intention | 1 | 1 |
| `auradock_prefs.xml` (template Bold, Bold preset Violet, cities London and Tokyo, onboarding) | | identical |
| `auradock_split.xml` (stacks `L:DIAL,MONTH`, `R:TASKS,WEATHER,NOW_PLAYING`) | | identical |
| `auradock.db` version | 7 | 8, only the tasks table left |
| Dashboard | | "57 m focused this week", both tasks listed |

## 0.3.1 to 0.3.2 (signed release builds, 11 Nov 2026)
Installed the signed 0.3.1 release, onboarded, added two tasks and recorded a real 135 s Dock now session, then installed the signed 0.3.2 release over it
(`adb install -r`, no uninstall). Before and after, the dashboard read the same: "2 m focused this week", the seven-day line, "2 left", both tasks.
Stacks, the Bold colour preset and extra cities are preference files, which the 0.3.2 release does not touch; they were compared byte for byte on the debug
upgrade above (release builds cannot be opened with `run-as`).

## The case that lost sessions in 0.3.1
On a fresh install the first dock of the day shows the intention prompt. On 0.3.1 (twice, debug and release) a Dock now session of over two minutes with the prompt
up was not saved and the dashboard stayed empty. On 0.3.2 the same sequence (fresh install, onboarding, Dock now, type an intention, wait 135 s, Back) saved a 150 s session.
