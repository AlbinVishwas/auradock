- Emulator: Medium_Phone_API_36.1 (Android 16). If no emulator is running, you may start it yourself in the background; never kill it.
- Install and test on the emulator only; never install on a physical device (target adb with `-s emulator-5554`).

## V1 scope
The locked V1 spec is in [docs/V1_SPEC.md](docs/V1_SPEC.md). Read it before starting feature work.
Anything under its "Out of V1" heading (AI layouts, voice, widget customization, recurring tasks,
Quick Settings tile, Play Store release) is not to be built or extended in V1.
