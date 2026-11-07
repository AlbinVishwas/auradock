# AuraDock style guide

Source of truth for how the app looks. Transcribed from the style-guide frame of the Claude Design handoff (`docs/design/handoff/claude-design-v1/project/StyleGuide.dc.html`, handoff v1) and, for what that frame does not say, from the screen frames beside it. If the frames and this file ever differ, the frames win; the older `AuraDock-style-guide.pdf` is superseded by them.
Tokens live in `app/src/main/java/com/auradock/ui/theme/` (see [../DESIGN.md](../DESIGN.md)).

*Two quiet colours, one loud. Flat black, big type, nothing extra.*

## Colour

| Name | Hex | Token (`AuraDockColors`) | Use |
|---|---|---|---|
| Black | `#000000` | `Background` | Ground |
| Card | `#161616` | `Surface` | Cards |
| Sheet | `#1C1C1E` | `SurfaceRaised` | Bottom sheets |
| Field | `#262626` | `Field` | Chips, text fields |
| Inactive | `#3A3A3A` | `Inactive` | Off states |
| Outline | `#5C5C5C` | `Outline` | Empty checkboxes, rings |
| Tertiary | `#8A8A8A` | `Tertiary` | Done items |
| Secondary | `#A3A3A3` | `OnSurfaceVariant` | Secondary text |
| White | `#FFFFFF` | `OnBackground` | Primary text |
| Quiet pill | `#2E2E2E` | `QuietPill` | Secondary buttons |
| **Accent** | `#FEFFED` | `Accent` (text on it: `OnAccent`, black) | The app's accent. The one filled action per screen (Dock now, Save, Continue), toggles, checkboxes, selected states, overdue items, badges. Never decorative. |
| **Coral** | `#FF5A4E` + `#FF8F86` | `Coral`, `CoralLight` | Dock templates only: the two tones of the Bold clock. Never in the app's own UI. Template thumbnails in the app are dock renders, so they may show coral. The logo's coral dot is the one brand exception. |

## Type

| Role | Face | Spec | Token (`AuraDockTypography`) |
|---|---|---|---|
| Dock display | Nunito Black | 80% opacity, screen blend, digits overlap −0.12em, rotation 1 −3°, 2 +5°, 3 −4°, 0 +5° | dock tokens |
| Title | Mona Sans ExtraBold | 32–36 / 1.1 | `ScreenTitle` (34), `StepTitle` (32), `Greeting` (40), `WelcomeTitle` (48) |
| Section | Mona Sans Bold | 22 | `Section` |
| Button and numbers | Mona Sans Bold | 18 | `Button` |
| Body | Inter Medium | 16 / 1.4 | `Body` |
| Secondary | Inter Regular | 15, `#A3A3A3` | `Secondary` |
| Caption | Inter Medium | 13 | `Caption` |

Sentence case everywhere. No Apple fonts. Few words, big type. Mona Sans (SIL OFL) is bundled in `res/font/`; its licence is in `docs/licenses/`.

## Components

- **Primary pill**: 56 dp high, accent fill, black text. One per screen. **Quiet pill**: `#2E2E2E`. **Text button**: grey.
- **Task rows**: empty, overdue and done. Done rows sink to the bottom.
- **Toggles and checkboxes**: on, off, selected, empty, dimmed (picker limit reached).
- **Permission card**: icon, title, one-line reason, Allow pill. Shows Allowed / Not allowed.
- **Chips and badges**: app icons sit in 28 dp rounded tiles (real app icons in the build); "In use" badge, outlined, with a check.

## Shape and spacing

Cards 24 dp radius, sheets 28 dp top radius, pills fully rounded. 4 dp grid, 20 dp screen margins, 48 dp touch targets.
Rows inside a screen (permission rows, the Setup link, the dashboard's nav tiles, the facts card) are 20 dp, as the handoff's screen frames draw them; fields and the time row in a sheet are 16 dp.
Flat: no shadows, gradients, glows or blur surfaces.

## Motion

Calm and physical. Springs settle without overshoot. Nothing bouncy.

- **Sheets and cards**: soft spring, damping 0.9, stiffness 380 (about 320 ms). The scrim fades to 60% black. Drag to dismiss follows the finger 1:1. Built in `ui/components/AuraSheet.kt` (Material's sheet cannot take a custom spring).
- **Blur to focus** (new content): blur 12 to 0, opacity 0 to 1, 4 dp rise, 260 ms, ease cubic(0.2, 0, 0, 1). Lists stagger 40 ms.
- **Dock now morph**: the pill expands into the full screen (container transform, 450 ms emphasized). The thumbnail clock scales into the dock clock. System bars hide at 60% (not built: the bars hide when the dock opens).
- **Task tick**: circle fills 120 ms, strike-through draws left to right 220 ms, text fades to `#8A8A8A`. After 600 ms the row eases to the bottom, with a light haptic tick (the delayed move and the tick are not built: the row moves at once).
- **Carousels**: snap on a critically damped spring; neighbours sit at 88% scale and 40% opacity, interpolated with the scroll.
- **Dock clock**: digits change with a 300 ms blur crossfade. A notification strip rises 12 dp while the clock scales down (the guide says 84%; the Bold frame is 316/370 = 85.4%, which is what is built). The clock drifts up to 3 dp a minute for burn-in (built as a slow ±4 dp drift).

## Handoff v1 values the guide does not list

Colours: icon tile `#222222` (`IconTile`), hairline between rows `#242424` (`RowDivider`), badge outline `#3A3A33` (`BadgeBorder`), done circle and time-wheel band `#333333` (`DoneCircle`, `WheelHighlight`), nav tile outline `#222222` (`TileOutline`), sheet handle `#4A4A4A` (`SheetHandle`), dimmed wheel rows `#6B6B6B` (`WheelDim`), Bold colon dots `rgba(200,200,200,0.8)` (`BoldColon`, was 45% grey).
Type: `WelcomeTitle` 48, `Greeting` 40, `StepTitle` 32, `StepBody` 17, `RowTitle` 15 SemiBold, `RowCaption` 13, `SmallAction` 13 SemiBold, `Meta` 14, `CardTitle` 17 SemiBold, `RowLabel` 15 Medium, `SheetTitle` 24, `FieldLabel` 13, `FieldText` 17, `EmptyState` 20, `EventTime` 18 / `EventMeridiem` 12, wheel 28 / 24 / 20, `BoldStrip` 16.
Size and shape: carousel card 320 x 144, radius 22; sheet field 56, radius 16; time wheel 168 high with 48 dp rows; dock thumbnail 157 high, radius 16; app row 60; search bar 48; small pill 36.
Bold clock (frames `Dock-Bold` and `Dock-Bold-Notification`): digits are 89.8% of the dock's height; line box 0.8 em; colon dots 0.17 em apart 0.1 em, margins 0.02 / -0.06 em; with a notification the clock is 85.4% and the strip sits 22 dp under it.

## Where the handoff frames and the app part ways

See the report that came with release 0.1.4: the data the frames show but the app does not have (event end time and place), screens the frames drop that the app still needs (Dock from anywhere, the screensaver step in Setup), and states that only exist in one frame.

## Notes on interpretation

- "28dp rounded tiles": read as a 28 dp square tile with a rounded corner (`AuraDockSizes.appTile`, `AuraDockRadii.tile`; the chip's tile has a 9 dp corner in the frame, `AuraDockRadii.chipTile`).
- The frames' status-bar and gesture-bar areas are part of the 412 x 915 frame; the app draws between the bars, so top and bottom paddings are the frame's minus the bar (`fromScreenTop`, `fromScreenBottom`).
