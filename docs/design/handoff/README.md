# Design handoffs

Put each Claude Design handoff in its own folder here:

```
docs/design/handoff/
  2026-11-dashboard/
    index.html          the handoff itself (the reference)
    NOTES.md            what the designer said; anything unclear
    screenshots/        what it looks like, and (later) what the Compose version looks like
```

A handoff is a **reference to look at**. It is not code to copy into the app. The app is Jetpack Compose: there is no WebView and
there will not be one.

To implement a handoff, follow [`../../DESIGN.md`](../../DESIGN.md), section "Implementing a design: the steps". In short:

1. Open the HTML in a browser and read `NOTES.md`.
2. Map every colour, font, size, spacing and animation to a token in `ui/theme/`. If one has no token, ask the owner.
3. Change only the screen's stateless composable (`XScreen`), not its ViewModel or route.
4. Update the `@Preview`s, build, test on the emulator, commit.

Do not delete a handoff after it is built: it is the record of what the screen was meant to look like.
