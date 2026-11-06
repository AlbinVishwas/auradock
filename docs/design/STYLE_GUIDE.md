# AuraDock style guide

Source of truth for how the app looks. Transcribed from `docs/design/AuraDock-style-guide.pdf`; if the two ever differ, the PDF wins.
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
| Title | Mona Sans ExtraBold | 32–36 / 1.1 (we use 34) | `ScreenTitle` |
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
- **Chips and badges**: app icons sit in 28 dp rounded tiles (real app icons in the build); "In use" badge.

## Shape and spacing

Cards 24 dp radius, sheets 28 dp top radius, pills fully rounded. 4 dp grid, 20 dp screen margins, 48 dp touch targets.
Flat: no shadows, gradients, glows or blur surfaces.

## Motion

Calm and physical. Springs settle without overshoot. Nothing bouncy.

- **Sheets and cards**: soft spring, damping 0.9, stiffness 380 (about 320 ms). The scrim fades to 60% black. Drag to dismiss follows the finger.
- **Blur to focus** (new content): blur 12 to 0, opacity 0 to 1, 4 dp rise, 260 ms, ease cubic(0.2, 0, 0, 1). Lists stagger 40 ms.
- **Dock now morph**: the pill expands into the full screen (container transform, 450 ms emphasized). The PDF's sentence is cut off after "The thumbnail clock"; we read it as the thumbnail clock scaling into the full dock clock.

## Notes on interpretation

- "28dp rounded tiles": read as a 28 dp square tile with a rounded corner (`AuraDockSizes.appTile`, `AuraDockRadii.tile`).
- Where a token in the old design had no equivalent in the guide (the dock's greys, the cyan clock colon), it stays until the templates are restyled.
