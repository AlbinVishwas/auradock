# AuraDock design system and how to implement a design

This file is the rulebook for anything that changes how AuraDock looks. Read it before restyling a screen.

## The rules for a design handoff

1. **A Claude Design handoff is an HTML reference, not code to copy.** It shows what the screen should look like and how it moves.
   Do not paste its HTML or CSS into the app.
2. **Implement it in Jetpack Compose, using the tokens below.** Never write a colour, font, size, spacing or duration in a screen.
   If a token is missing, add one to the theme (see "Adding a token") and use that.
3. **Never add a WebView or HTML to the app.** Not for a screen, not for a part of one, not for an illustration.
4. **Match the layout, spacing and motion of the reference.** Same order of things, same proportions, same rhythm, same easing feel.
5. **If a design conflicts with the tokens, ask the owner.** For example a colour that is not in the palette, or a font size between two
   steps of the type scale. Do not pick silently, and do not add a one-off value.

Handoffs live in `docs/design/handoff/` (see its README).

## Where things live

| What | Where |
|---|---|
| Colours | `ui/theme/Color.kt`: `AuraDockPalette` (the raw colours) and `AuraDockColors` (what each is used for) |
| Type | `ui/theme/Type.kt`: `AuraDockTypography` (the scale), `NunitoFamily`, `InterFamily` |
| Spacing, radii, sizes | `ui/theme/Dimens.kt`: `AuraDockSpacing`, `AuraDockRadii`, `AuraDockSizes`, plus layout numbers for the dock |
| Motion | `ui/theme/Motion.kt`: `AuraDockMotion` (durations, easing, springs) |
| Theme entry point | `ui/theme/AuraDockTheme.kt` (wires the tokens into Material) |
| Fonts | `res/font/nunito_variable.ttf`, `res/font/inter_variable.ttf` (SIL Open Font License; texts in `docs/licenses/`) |
| Logo and icons | `docs/brand/` (source SVG and PNGs); Android versions in `res/drawable/ic_launcher_*.xml`, `ic_stat_auradock.xml` |

## Tokens

### Colour
Pure black is the stage. The brand colour is **coral `#FF5A4E`** (the aura in the logo), with **salmon `#FF9A90`** as its lighter tint for
text and fills on black.

- **Palette** (`AuraDockPalette`): `Black`, the grey scale `Grey950 ... Grey200`, `White`, `Coral`, `Salmon`, translucent whites
  (`White5 ... White40`), and a few colours with a job of their own (`Blue`, `Cyan`, `Amber`, `Green`, `DarkRed`, ...).
- **Meanings** (`AuraDockColors`): `Background`, `Surface`, `SurfaceRaised`, `OnBackground`, `OnSurfaceVariant`, `OnDisabled`,
  `Coral`, `CoralTint`, `ClockGlow`, `AccentBlue`, `AccentAmber`, `Success`, `Danger`, `Overdue`, the dock's quieter greys
  (`DockLabel`, `DockText`, `DockOverdue`, `Dim*`, `Strip*`), the docking pill (`Pill*`) and the onboarding illustration (`Illustration*`).

Screens use **meanings** (`AuraDockColors.X`). To restyle, re-point a meaning at another palette entry; do not use the palette directly
from a screen.

Today the interactive accent (buttons, selected states) is still `AccentBlue`. A design that moves it to coral is a one-line change
to `AccentBlue`'s meaning, or a new `Accent` meaning; ask the owner first.

### Type
Two families: **Nunito** for display (clock, titles, big numbers) and **Inter** for UI (labels, body, buttons). Each is one variable font
file with a weight axis.

| Style | Family | Size / weight | Use |
|---|---|---|---|
| `ClockDisplay` | Nunito | 96, extra light | the clock digits |
| `ScreenTitle` | Nunito | 32, semibold | the title of a screen |
| `WidgetPrimary` | Nunito | 28, light | a primary value; card titles |
| `Title` / `TitleStrong` | Inter | 18, medium / semibold | row and card titles |
| `Body` | Inter | 16, regular | running text |
| `WidgetLabel` | Inter | 14, medium | labels, buttons |
| `CalendarCell` | Inter | 12 | calendar cells |
| `Caption` | Inter | 10, medium | captions and explanations |
| `PillText` / `PillAction` | Inter | 16, medium / semibold | the docking pill |
| `Clock*`, `Dock*`, `IllustrationClock` | mixed | see `Type.kt` | the dock templates (read from a distance) and the onboarding illustration |

### Spacing, radii, sizes
- `AuraDockSpacing`: `hairline 1, xxs 2, xs 4, sm 8, smd 12, md 16, mdl 20, lg 24, xl 32, xxl 48, huge 64`.
- `AuraDockRadii`: `control 12, card 16, cardLarge 24, widget 28, pill 999`.
- `AuraDockSizes`: line widths, icon sizes, touch targets (44 and 48), elevations.
- `AuraDockDockLayout`, `AuraDockIllustration`, `AuraDockGallery`: numbers that belong to one area only.

### Motion
`AuraDockMotion`: durations (`Press 110 ... Slow 400`), `Easing` (fast out, slow in), `softSpring()` (a little overshoot, for things that
slide in, like the docking pill) and `calmSpring()` (no overshoot).

## Adding a token
1. Add it to the right file in `ui/theme/`, with a one-line comment saying what it is for.
2. Give colours a **meaning** in `AuraDockColors`, and put the raw value in `AuraDockPalette` only if it is a new palette colour.
3. Use it. Never write a literal in a screen: no `Color(0x...)`, no `NN.dp`, no `NN.sp`, no `FontWeight.X`, no `tween(NNN)`.
   A quick check: `grep -rnE "Color\(0x|[0-9]\.(dp|sp)|fontSize|tween\([0-9]" app/src/main/java --include='*.kt' | grep -v ui/theme`
   should print nothing.

## Screens: looks versus logic
Every screen is split in two, so restyling never touches logic:

- **`XScreen`** is a *stateless content composable*. It takes a `XUiState` (plain data) and an `XActions` (callbacks) and draws.
  It does not know about ViewModels, flows, permissions or Android services. The only state it may hold is purely visual
  (which page of a pager, whether a sheet is open, text being typed).
- **`XRoute`** (in `ui/routes/Routes.kt`) is the *thin wrapper*: it collects the ViewModel's flows, builds the `UiState`, and maps each
  action onto a ViewModel call. It has no layout.

| Screen | UiState / Actions |
|---|---|
| `DashboardScreen` | `DashboardUiState`, `DashboardActions` |
| `TemplateGalleryScreen` | `TemplateGalleryUiState`, `onApply` |
| `FocusSettingsScreen` | `FocusSettingsUiState`, `FocusSettingsActions` |
| `SetupScreen` | `SetupUiState`, `SetupActions` |
| `OnboardingScreen` | `OnboardingUiState`, `OnboardingActions` |
| `DockContent` (the four dock templates) | `DockTemplate`, `DockUiState` (wrapper: `DockRoute`) |
| `TaskCard`, `TaskSheet`, `PermissionCard`, `DockFromAnywhereCard`, the docking pill | plain parameters |

When a design adds data to a screen, add a field to its `UiState` and fill it in the route. When it adds a button, add a callback to
its `Actions` and wire it in the route. Layout changes touch only the `Screen`.

## Previews
Every screen and dock template has `@Preview`s with fake data (`ui/preview/PreviewData.kt`), in a `*Previews.kt` file beside it:
full, empty, loading, and permission-missing states; portrait for screens, landscape for the dock. Use the multi-preview annotations
`@PreviewPortrait`, `@PreviewLandscape`, `@PreviewComponent`. A restyle is checked by looking at all previews of the screen in Android
Studio, then on the emulator. When you add a state to a screen, add a preview for it.

## Implementing a design: the steps
1. Read the handoff in `docs/design/handoff/<name>/` (open its HTML in a browser; read its README and notes).
2. List every colour, font, size, spacing and animation in it, and map each to a token. Anything with no token: ask the owner
   (rule 5), then add it as described above.
3. Change only the `XScreen` composable (and the theme tokens if agreed). Keep its `UiState` and `Actions` unless the design adds data
   or actions; then extend them and the route.
4. Update or add the `@Preview`s so each state is visible.
5. Build, run the unit tests, and check on the emulator (and the Galaxy A55 for anything about the dock). Screenshots go next to the handoff.
6. One commit per screen.

## Audit: what was hardcoded before the tokens
The first pass replaced every colour, `.dp`, `.sp`, font and duration literal outside `ui/theme/`. Counts before: 24 colour literals,
129 `dp` literals, 39 `sp` literals, 39 font/weight settings, ~20 durations. They are now all named tokens; values were kept
exactly (so the app looks as before) except that text now uses Nunito and Inter instead of the system font, and a few off-scale
spacings were snapped to the nearest token (6 and 10 dp in the pill).
