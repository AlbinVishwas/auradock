# AuraDock — UI Design System Specification
### Senior Android UI Architecture Review · March 2026

> **Purpose:** This document defines the canonical visual design system and component architecture for the AuraDock standby application. It serves as the single source of truth for the product design team and Android engineering team before translating into Jetpack Compose implementation.
> 
> **Core Aesthetic:** Minimal, premium, calm, and glanceable. Inspired by modern smart displays and premium tablet docks. Dark-first, OLED-optimized.

---

## Section 1 — Color Tokens

The color system is optimized for OLED efficiency and low-light environments. True black is used extensively, with subtle glassmorphic layers to create depth without harsh borders.

| Token | Hex Value | Usage |
|---|---|---|
| **Background** | `#000000` | Pure black. Used for the root window background. Turns OLED pixels completely off for maximum power efficiency and burn-in reduction during standby. |
| **Surface** | `#0D0D0D` | Very dark gray. Used for elevated non-glass surfaces (e.g., bottom sheets, dashboard base) to distinguish from the pure black background. |
| **GlassCard** | `#FFFFFF` (`0.09` to `0.04` alpha gradient) | Translucent white with a vertical gradient. Used as the background fill for all widget cards to create a frosted glass aesthetic over the black background. |
| **GlassBorder** | `#FFFFFF` (`0.12` alpha) | Subtle translucent white. Used for the 1dp stroke around `GlassCard` components to define their edges without high contrast. |
| **PrimaryText** | `#FFFFFF` | Pure white. Used for the main clock digits, primary widget values (e.g., Temperature), and active UI states. |
| **SecondaryText** | `#AAAAAA` | Muted gray. Used for labels, captions, dates, remaining calendar days, and secondary information to establish visual hierarchy. |
| **AccentBlue** | `#2979FF` | Vivid blue. Used for the primary accent color: today's date highlight, active toggle states, and primary CTA buttons in the dashboard. |
| **AccentAmber** | `#FFB300` | Warm amber. Used sparingly for status indicators (e.g., charging bolt) and specific widget iconography (e.g., sun icon) to draw subtle attention. |
| **Divider** | `#FFFFFF` (`0.15` alpha) | Low-opacity white. Used for hair-line structural dividers between major sections (e.g., separating the clock from the widget row). |
| **ClockGlow** | `#4FC3F7` (`0.22` down to `0.0` radial) | Cyan/Blue glow. Used as an ambient radial gradient behind the clock digits to simulate physical LED diffusion. |

---

## Section 2 — Typography System

Typography must be readable from 1–2 meters away. We use a single clean, geometric sans-serif typeface (Inter or Roboto) with meticulous attention to font weight and letter spacing.

| Role | Font Size | Weight | Letter Spacing | Usage |
|---|---|---|---|---|
| **ClockDisplay** | `96sp` | ExtraLight (200) | `-4sp` | The primary time display. The negative kerning pulls the ultra-large lightweight digits together for a cohesive, modern look. |
| **WidgetPrimary** | `42sp` | Light (300) | `-1sp` | Primary large data points in widgets (e.g., 22° in Weather). Easily glanceable from across the room. |
| **WidgetLabel** | `14sp` | Medium (500) | `0sp` | Standard text for widget content (e.g., "Partly Cloudy"). Balances readability with space constraints. |
| **CalendarCell** | `11sp` | Normal (400)* | `0sp` | The individual numbers in the calendar grid. *(Changes to Bold (700) for the current day).* |
| **Caption** | `11sp` | Medium (500) | `2sp` | Uppercase, tracked-out text for metadata (e.g., headers, city names, day-of-week labels). |
| **AmPm** | `16sp` | Light (300) | `1sp` | The AM/PM indicator next to the clock. Sized down significantly so it doesn't compete with the main digits. |

---

## Section 3 — Spacing System

AuraDock uses a rigid 4dp-based spacing system to ensure perfect rhythm and alignment across all layouts.

| Token | Value | Primary Usage |
|---|---|---|
| **SpacingXs** | `4dp` | Internal component spacing. Gap between calendar cells; space between weather detail values and their labels. |
| **SpacingSm** | `8dp` | Minor element grouping. Gap between the weather icon and the temperature text; spacing inside quick action buttons. |
| **SpacingMd** | `16dp` | Standard structural gap. The primary gutter between widgets in the `WidgetRow`; distance from card edges to internal content. |
| **SpacingLg** | `24dp` | Section padding. Vertical padding at the bottom of the Standby screen; spacing above the divider. |
| **SpacingXl** | `32dp` | Top structural padding. The distance from the top edge of the screen to the top of the clock area. |
| **SpacingXxl** | `48dp` | Edge margins. The horizontal padding on the left and right sides of the Standby screen to keep content comfortably within the safe area. |

---

## Section 4 — Component Library

### `GlassCard`
* **Purpose:** The foundational container for all standby widgets.
* **Visual Style:** `Corner Radius: 28dp`. Uses the `GlassCard` background gradient and `GlassBorder` stroke. Applies a background blur (`backdrop-filter`) to diffuse anything behind it.
* **Layout Rules:** Content inside must use `SpacingMd` (16dp) or `SpacingLg` (24dp) padding. Must be capable of expanding to fill available layout weight.

### `GreetingHeader`
* **Purpose:** The top element of the interactive Dashboard.
* **Visual Style:** Left-aligned. "Good morning/afternoon/evening" in `SecondaryText`, followed by the user's name in `PrimaryText` (large, bold).
* **Layout Rules:** Occupies the full width of the top safe area.

### `TodaySnapshotCard`
* **Purpose:** A summary view on the Dashboard displaying upcoming events or reminders.
* **Visual Style:** Distinct from `GlassCard`. Uses solid `Surface` color with a 16dp corner radius to signify interaction.
* **Layout Rules:** Horizontal scrolling or stacked vertically depending on content volume. Tap targets must be at least 48dp.

### `QuickActionButton`
* **Purpose:** Triggers for manual data entry (e.g., "New Reminder", "Customize Standby").
* **Visual Style:** Pill-shaped (`999dp` radius). `Surface` background with a subtle border. Active states fill with `AccentBlue` and change text to pure white.
* **Layout Rules:** Used primarily in the Dashboard and Settings. Minimum height of 48dp.

### `StandbyPreviewCard`
* **Purpose:** Shows a miniaturized preview of a standby layout in the Template Carousel.
* **Visual Style:** Scaled-down version of the actual standby UI. Fixed 16:9 aspect ratio. When selected, it gains a bright `AccentBlue` border glow.
* **Layout Rules:** Rendered inside a horizontal scrolling row.

### `WidgetCard` (Abstract)
* **Purpose:** Any high-level widget (Clock, Weather, Calendar) placed inside a `GlassCard`.
* **Visual Style:** Inherits `GlassCard` base. Uses strict `WidgetPrimary` and `Caption` typography roles.
* **Layout Rules:** Must handle flexible sizing. Cannot assume fixed pixel widths.

---

## Section 5 — Standby Screen Layout Rules

The core Standby Screen is organized into a rigid, highly balanced vertical column, optimized for 16:9 landscape aspect ratios.

### Structure
1. **Root Container:** Fills the screen. Background is pure black (`#000000`).
2. **Top Clock Area (48% Height):**
   * Centered horizontally and vertically within its bounding box.
   * Contains the `ClockWidget`.
   * Holds the ambient `ClockGlow` background.
3. **Divider (1dp Height):**
   * A 60% width hairline line using the `Divider` color token.
   * Placed exactly between the clock and the widgets to anchor the layout visually.
4. **Widget Row (52% Height):**
   * A horizontal layout containing 2 `WidgetSlot` areas.
   * Slots are evenly weighted (`1f` : `1f`).
   * Separated by `SpacingMd` (16dp).

### Widget Slot System
* The bottom area comprises exactly two slots.
* A user can place any approved `WidgetCard` (Weather, Calendar, Reminders) into these slots.
* The layout must remain balanced regardless of which widgets are placed.

---

## Section 6 — Animation Guidelines

Animations must feel deliberate, smooth, and largely imperceptible unless triggered by user interaction or explicit state changes.

* **Screen Entry (Staggered Fade-Slide):**
  * When standby activates, elements do not appear instantly.
  * *Clock:* Fades in (400ms) and slides down slightly (500ms `EaseOutCubic`).
  * *Widgets:* Fade in with a 200ms delay (600ms duration) and slide up slightly.
  * *Result:* Content pulls satisfyingly into the center of the screen.
* **Clock Glow Breathing:**
  * The background radial glow slowly pulses its alpha channel (e.g., 0.18 to 0.30).
  * *Timing:* 3000ms duration, infinite reverse cycle.
  * *Easing:* `FastOutSlowInEasing`.
* **Minute Tick Animation:**
  * When the minute changes, the digits do not snap jarringly.
  * *Animation:* The old minute digit crossfades to 0 opacity instantly, and the new digit fades in over 350ms (`FastOutSlowInEasing`).
* **Clock Colon Blink:**
  * The colon (`:`) blinks every second to indicate a live ticking clock.
  * *Animation:* Uses a `StepEasing` function. It snaps between 85% opacity and 25% opacity instantly. Do not interpolate this fade; it must feel like a mechanical digital clock.
* **Burn-in Protection Drift:**
  * See Section 7. The entire screen topology shifts imperceptibly.

---

## Section 7 — Accessibility Rules

### Distance Viewing (1–2 Meters)
* The Standby screen is a "10-foot UI" designed for a desk or nightstand.
* **Rule:** Primary data (Time, Temperature) must use font sizes > `40sp`.
* **Rule:** Secondary data must use font sizes > `10sp` with high contrast.
* **Avoid:** Dense paragraphs.

### Contrast
* **Rule:** All informative text must pass WCAG AA contrast ratios against the pure black background.
* `PrimaryText` (#FFFFFF) on Background (#000000) achieves 21:1.
* `SecondaryText` (#AAAAAA) on Background (#000000) achieves > 6:1.
* *Glows and blurs must not sit directly behind critical small text.*

### Burn-in Protection (OLED Safety)
* **Rule:** A global XY drift animation must be applied to the topmost layout column.
* *Implementation:* Shift the layout by ±4dp on both X and Y axes over a 10-minute infinite cycle. The phase of the X and Y animations must be desynchronized to prevent a linear back-and-forth track.

### Touch Targets
* Although primarily a passive display, any interactive elements (e.g., closing the display, interacting with a notification) must have a minimum touch target size of `48x48dp`.

---
*Document version 1.0 · Prepared for AuraDock Design Team · March 2026*
