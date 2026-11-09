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
| **Coral** | `#FF5A4E` + `#FF8F86` | `Coral`, `CoralLight` | Dock templates only: the default colourway of the Bold clock (see Bold clock below for the other presets). Never in the app's own UI. Template thumbnails in the app are dock renders, so they may show coral. The logo's coral dot is the one brand exception. |

## Type

| Role | Face | Spec | Token (`AuraDockTypography`) |
|---|---|---|---|
| Bold clock digits | Nunito Black | Only the Bold clock. Upright, 80% opacity, screen blend, laid out by glyph ink (see below) | `NunitoFamily` (drawn in `BoldClock`) |
| Display numbers | Host Grotesk ExtraBold / Bold | Everywhere else a big number or date shows on the dock: Classic's day number and weekday, temperatures, the Minimal and Productivity clocks and the clock widget | `DisplayGrotesk` (`DockDayNumber`, `DockTemperature`, `ClockDisplay`, ...) |
| Title | Mona Sans ExtraBold | 32–36 / 1.1 | `ScreenTitle` (34), `StepTitle` (32), `Greeting` (40), `WelcomeTitle` (48) |
| Section | Mona Sans Bold | 22 | `Section` |
| Button and numbers | Mona Sans Bold | 18 | `Button` |
| Body | Inter Medium | 16 / 1.4 | `Body` |
| Secondary | Inter Regular | 15, `#A3A3A3` | `Secondary` |
| Caption | Inter Medium | 13 | `Caption` |

Sentence case everywhere. No Apple fonts. Few words, big type. Mona Sans, Host Grotesk (heaviest weight ExtraBold) and Nunito (SIL OFL) are bundled in `res/font/`; their licences are in `docs/licenses/`.

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
- **Dock clock**: digits change with a 300 ms blur crossfade. A notification strip rises 12 dp while the Bold clock refits to the area above it. The clock drifts up to 3 dp a minute for burn-in (built as a slow ±4 dp drift).

## Handoff v1 values the guide does not list

Colours: icon tile `#222222` (`IconTile`), hairline between rows `#242424` (`RowDivider`), badge outline `#3A3A33` (`BadgeBorder`), done circle and time-wheel band `#333333` (`DoneCircle`, `WheelHighlight`), nav tile outline `#222222` (`TileOutline`), sheet handle `#4A4A4A` (`SheetHandle`), dimmed wheel rows `#6B6B6B` (`WheelDim`), Bold colon dots `rgba(200,200,200,0.8)` (`BoldColon`, was 45% grey).
Type: `WelcomeTitle` 48, `Greeting` 40, `StepTitle` 32, `StepBody` 17, `RowTitle` 15 SemiBold, `RowCaption` 13, `SmallAction` 13 SemiBold, `Meta` 14, `CardTitle` 17 SemiBold, `RowLabel` 15 Medium, `SheetTitle` 24, `FieldLabel` 13, `FieldText` 17, `EmptyState` 20, `EventTime` 18 / `EventMeridiem` 12, wheel 28 / 24 / 20, `BoldStrip` 16.
Size and shape: carousel card 320 x 144, radius 22; sheet field 56, radius 16; time wheel 168 high with 48 dp rows; dock thumbnail 157 high, radius 16; app row 60; search bar 48; small pill 36.
Bold clock (rewritten in 0.2.3; reference: `docs/design/references/bold/` when added, Apple StandBy-style large clock):
- **Layout** is in landscape coordinates and turned as a whole by the dock when the window is portrait, so it is identical with auto-rotate on or off, in both landscapes. Glyphs are measured by their ink, not advance widths. The combined ink box of the digits and colon is centred in the safe area (40 dp at the sides, 24 dp top and bottom) and scaled to 90% of the safe width, capped at 80% of the safe height, so three-digit times come out larger than four-digit ones. With a notification strip the same rules apply to the area above it.
- **Digits** are upright; each digit's ink overlaps the previous digit's ink by 0.06 em, for every pair.
- **Colon**: two dots 0.24 em wide, centres at 33% and 67% of the digit height, centred in the gap and reaching 30% of their width into each neighbouring digit; fill `rgba(235,235,235,0.75)` (`BoldColon`), drawn above the digits with screen blend.
- **Frosted glass**: digits at 80% opacity with screen blend (overlaps read lighter), a 2% white wash from each glyph's top edge (`BoldFrost`); no blur, glow or shadow. The finished clock is drawn at 85% (`Brightness`) for burn-in; the 3 dp burn-in drift stays.
- **Colour presets** (`BoldPalette`, saved on the phone, chosen by dots under the Bold card in Templates): Coral `#FF5A4E`/`#FF8F86` (default), Blue `#2F6BFF`/`#5C8DFF`/`#8FB0FF`, Pink `#FF3D8B`/`#FF7AB0`, Violet `#B04BD8`/`#9A7BFF`/`#D07BC4`, Amber `#FF9F1C`/`#FFC266`, Mint `#2FD39A`/`#7BE8C1`, Mono `#EDEDED`/`#BDBDBD`. Tones alternate across the digits. Dimming is applied to the finished clock, so it is the same for every preset.

## Where the handoff frames and the app part ways

See the report that came with release 0.1.4: the data the frames show but the app does not have (event end time and place), screens the frames drop that the app still needs (Dock from anywhere, the screensaver step in Setup), and states that only exist in one frame.

## Notes on interpretation

- "28dp rounded tiles": read as a 28 dp square tile with a rounded corner (`AuraDockSizes.appTile`, `AuraDockRadii.tile`; the chip's tile has a 9 dp corner in the frame, `AuraDockRadii.chipTile`).
- The frames' status-bar and gesture-bar areas are part of the 412 x 915 frame; the app draws between the bars, so top and bottom paddings are the frame's minus the bar (`fromScreenTop`, `fromScreenBottom`).
