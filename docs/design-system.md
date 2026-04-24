# TuneFlow Design System
### Android TV / Fire TV — 10-Foot UI Guidelines
> **Version:** 1.1 · **Platform:** Android TV (API 21+) · **Input:** D-pad remote only

---

## Table of Contents

1. [Design Principles](#1-design-principles)
2. [Layout System](#2-layout-system)
3. [Color System](#3-color-system)
4. [Typography](#4-typography)
5. [Components](#5-components)
6. [Focus & Navigation](#6-focus--navigation)
7. [Motion & Animation](#7-motion--animation)
8. [Screen Patterns](#8-screen-patterns)
9. [Iconography](#9-iconography)
10. [Background & Visual Depth](#10-background--visual-depth)
11. [Accessibility](#11-accessibility)
12. [Performance Guidelines](#12-performance-guidelines)
13. [Implementation Notes](#13-implementation-notes)
14. [Branding & Asset System](#14-branding--asset-system)
15. [Asset Export & Developer Handoff](#15-asset-export--developer-handoff)

---

## 1. Design Principles

### 1.1 Remote-First Navigation

Every interaction is driven by a D-pad remote. There is no touch, no mouse, no keyboard shortcut fallback.

| ✅ Do | ❌ Don't |
|---|---|
| Ensure every interactive element is reachable via D-pad | Place interactive elements that require hover or swipe |
| Provide clear, unambiguous focus order (left→right, top→bottom) | Create floating action buttons with no D-pad path |
| Test all navigation paths with a physical remote | Rely on touch-only gestures like swipe-to-dismiss |
| Ensure every screen has a logical "back" exit | Create modal dialogs with no focusable close button |

---

### 1.2 Clarity Over Density

TV screens are viewed from 2–3 meters. Information must be immediately legible without squinting or leaning forward.

| ✅ Do | ❌ Don't |
|---|---|
| Show 4–6 items per row maximum in grids | Pack 8+ cards in a single row |
| Use large text (minimum 14sp body, 18sp+ for primary content) | Use text smaller than 12sp anywhere |
| Leave generous whitespace between sections (≥ 32dp) | Stack sections with < 16dp separation |
| Prioritize the most important action per screen | Show 5+ CTAs simultaneously |

---

### 1.3 Motion as Feedback

Animation communicates state changes and confirms user input. It is functional, not decorative.

| ✅ Do | ❌ Don't |
|---|---|
| Animate focus transitions (scale + glow, 150ms) | Play animations longer than 300ms for focus changes |
| Use screen transitions to convey hierarchy (slide in/out) | Use random or decorative animations with no semantic meaning |
| Show loading skeletons immediately on data fetch | Show a blank screen while content loads |
| Animate playback state changes (play → pause icon morph) | Animate background elements continuously while user navigates |

---

### 1.4 Predictable Focus Behavior

Users must always know where focus is and where it will go next. Surprises break trust.

| ✅ Do | ❌ Don't |
|---|---|
| Restore focus to the last focused item when returning to a screen | Reset focus to the top of the screen on every back navigation |
| Keep focus within a logical region (e.g., sidebar vs. content) | Allow focus to jump unpredictably across unrelated sections |
| Trap focus inside dialogs/overlays until dismissed | Let focus escape a modal into background content |
| Highlight the focused element with a visible border or scale | Rely solely on color change for focus indication |

---

### 1.5 Performance-First Design

TV hardware is constrained. A 60fps UI is non-negotiable for a premium feel.

| ✅ Do | ❌ Don't |
|---|---|
| Use hardware-accelerated Compose animations | Animate properties that trigger layout passes (e.g., `size` in a loop) |
| Lazy-load images with placeholder skeletons | Load all album art upfront before rendering the screen |
| Limit blur effects to static backgrounds (Now Playing only) | Apply real-time blur to scrolling content |
| Pre-fetch the next screen's data on focus approach | Wait for user to confirm navigation before fetching |

---

### 1.6 Premium Restraint

Less is more. Every element on screen must earn its place.

| ✅ Do | ❌ Don't |
|---|---|
| Use a single accent color for interactive/active states | Use multiple accent colors competing for attention |
| Let album art provide visual richness | Add decorative gradients, patterns, or textures to UI chrome |
| Use consistent spacing and alignment throughout | Mix spacing values ad hoc |
| Default to dark backgrounds that recede | Use light or mid-tone backgrounds that compete with content |

---

## 2. Layout System

### 2.1 Safe Margins (TV Overscan)

All content must be inset from screen edges to account for TV overscan and bezel variation.

```
┌─────────────────────────────────────────────────────────────────┐
│  ← 48dp →                                          ← 48dp →    │
│  ↑ 27dp                                                         │
│                                                                 │
│           [ SAFE CONTENT AREA ]                                 │
│                                                                 │
│  ↓ 27dp                                                         │
└─────────────────────────────────────────────────────────────────┘
```

| Edge | Safe Margin |
|---|---|
| Left | 48dp |
| Right | 48dp |
| Top | 27dp |
| Bottom | 27dp |

> **Rule:** No text, icon, or interactive element may be placed outside the safe area. Background fills (colors, images) may extend to screen edges.

---

### 2.2 Grid System

**Base unit:** 8dp

| Grid Property | Value |
|---|---|
| Base unit | 8dp |
| Column count (album grid) | 5 columns |
| Column count (artist grid) | 6 columns |
| Column gutter | 16dp |
| Row gutter | 24dp |
| Section vertical spacing | 40dp |
| Sidebar width | 240dp (expanded) / 72dp (collapsed) |
| Content area (with sidebar) | Screen width − sidebar width − 48dp right margin |

---

### 2.3 Content Density Rules

| Context | Max items visible | Scroll direction |
|---|---|---|
| Album grid | 5 columns × 2.5 rows | Vertical |
| Artist grid | 6 columns × 2 rows | Vertical |
| Track list | 8–10 rows | Vertical |
| Horizontal shelf (Home) | 5–6 cards | Horizontal |
| Search results | 4 columns | Vertical |

> **Rule:** Always show a partial item at the edge of a scrollable list to signal that more content exists.

---

### 2.4 Card Sizing

| Card Type | Width | Height | Image Ratio | Corner Radius |
|---|---|---|---|---|
| Album card | 200dp | 240dp | 1:1 (square) | 8dp |
| Artist card | 180dp | 220dp | 1:1 (circle) | 50% |
| Playlist card | 200dp | 240dp | 1:1 (square) | 8dp |
| Track row | Full width | 72dp | 1:1 (56dp thumb) | 4dp |
| Featured hero | Full content width | 320dp | 16:9 | 12dp |

---

### 2.5 Large Screen Considerations

- **1080p (1920×1080):** Primary target. All dp values defined for this resolution.
- **4K (3840×2160):** Scale all dp values by 2×. Ensure images are loaded at 2× resolution.
- **720p (1280×720):** Reduce column count by 1. Maintain minimum touch target of 48dp.

---

## 3. Color System

### 3.1 Full Color Palette

All colors are defined for **dark theme only**. TuneFlow does not support a light theme.

#### Background Colors

| Token | Hex | Usage |
|---|---|---|
| `color.background` | `#0A0A0F` | Root screen background |
| `color.surface` | `#141420` | Cards, sidebars, elevated surfaces |
| `color.surfaceVariant` | `#1E1E2E` | Input fields, secondary surfaces |
| `color.overlay` | `#000000CC` | Modal overlays (80% opacity) |

#### Brand / Accent Colors

| Token | Hex | Usage |
|---|---|---|
| `color.primary` | `#6C63FF` | Active focus border, primary buttons, progress bars |
| `color.primaryVariant` | `#8B85FF` | Hover/pressed state of primary |
| `color.secondary` | `#FF6584` | Badges, "Now Playing" indicator, error states |
| `color.tertiary` | `#43E97B` | Success states, "Added to queue" confirmation |

#### Text Colors

| Token | Hex | Usage |
|---|---|---|
| `color.onBackground` | `#FFFFFF` | Primary text on background |
| `color.onSurface` | `#F0F0FF` | Primary text on surface |
| `color.textSecondary` | `#A0A0C0` | Subtitles, metadata, secondary labels |
| `color.textDisabled` | `#505070` | Disabled labels, placeholder text |
| `color.textOnPrimary` | `#FFFFFF` | Text on primary-colored buttons |

#### State Colors

| Token | Hex | Usage |
|---|---|---|
| `color.focusBorder` | `#6C63FF` | Focus ring border |
| `color.focusGlow` | `#6C63FF40` | Focus glow shadow (25% opacity) |
| `color.selected` | `#6C63FF1A` | Selected row/item background (10% opacity) |
| `color.divider` | `#FFFFFF14` | Dividers, separators (8% opacity) |

---

### 3.2 Contrast Requirements

| Pair | Contrast Ratio | Requirement |
|---|---|---|
| `onBackground` on `background` | ≥ 15.8:1 | ✅ Exceeds WCAG AAA |
| `textSecondary` on `background` | ≥ 5.2:1 | ✅ Meets WCAG AA |
| `textDisabled` on `background` | ~2.5:1 | ⚠️ Intentionally low — disabled only |
| `textOnPrimary` on `primary` | ≥ 4.5:1 | ✅ Meets WCAG AA |
| `onSurface` on `surface` | ≥ 12:1 | ✅ Exceeds WCAG AAA |

> **TV Rule:** Minimum contrast ratio for any readable text is **4.5:1**. For primary content (titles, track names), target **7:1+**.

---

### 3.3 Color Usage Rules

- **Never** use `color.primary` for decorative purposes — reserve it exclusively for interactive/active states.
- **Never** use more than one accent color per screen region.
- Album art colors must **not** influence UI chrome colors dynamically (no palette extraction for navigation elements).
- Dynamic color extraction from album art is **only** permitted for the Now Playing screen background blur.

---

## 4. Typography

### 4.1 Font Family

| Usage | Font | Fallback |
|---|---|---|
| All UI text | `Roboto` | `sans-serif` |
| Monospace (timestamps, bitrate) | `Roboto Mono` | `monospace` |

> **Rationale:** Roboto is pre-installed on all Android TV devices, eliminating font loading latency.

---

### 4.2 Type Scale

| Token | Size (sp) | Weight | Line Height | Letter Spacing | Usage |
|---|---|---|---|---|---|
| `typography.displayLarge` | 48sp | 700 (Bold) | 56sp | −0.5sp | App title, hero text |
| `typography.displayMedium` | 36sp | 700 (Bold) | 44sp | −0.25sp | Screen titles |
| `typography.titleLarge` | 28sp | 600 (SemiBold) | 36sp | 0sp | Section headers |
| `typography.titleMedium` | 22sp | 600 (SemiBold) | 30sp | 0sp | Card titles, album names |
| `typography.titleSmall` | 18sp | 500 (Medium) | 26sp | 0.1sp | Sub-section labels |
| `typography.bodyLarge` | 16sp | 400 (Regular) | 24sp | 0.15sp | Track names, list items |
| `typography.bodyMedium` | 14sp | 400 (Regular) | 22sp | 0.25sp | Metadata, artist names |
| `typography.labelLarge` | 14sp | 500 (Medium) | 20sp | 0.1sp | Button labels |
| `typography.caption` | 12sp | 400 (Regular) | 18sp | 0.4sp | Duration, bitrate, timestamps |

---

### 4.3 Readability Rules

- **Minimum body text:** 14sp. Never use smaller text for readable content.
- **Maximum line length:** 80 characters for body text blocks. Truncate with ellipsis (`…`) for single-line labels.
- **Text truncation:** Always use `maxLines = 1` + `overflow = TextOverflow.Ellipsis` for card titles.
- **All-caps:** Only for section labels and button text. Never for titles or track names.
- **Italic:** Avoid. Italic text is harder to read at distance.
- **Text shadow:** Do not use text shadows on dark backgrounds. Use sufficient contrast instead.
- **Distance rule:** At 3 meters, 1sp ≈ 0.5mm on a 55" 1080p TV. Minimum legible size is ~12sp.

---

## 5. Components

### 5.1 Buttons

#### Primary Button

```
┌──────────────────────────────┐
│  [Icon]  LABEL TEXT          │  Height: 52dp
└──────────────────────────────┘  Padding: 24dp horizontal, 14dp vertical
                                  Corner radius: 8dp
                                  Background: color.primary (#6C63FF)
                                  Text: typography.labelLarge, color.textOnPrimary
```

| State | Background | Border | Scale |
|---|---|---|---|
| Default | `color.primary` | None | 1.0× |
| Focused | `color.primaryVariant` | 2dp `color.focusBorder` | 1.05× |
| Pressed | `color.primary` at 80% | None | 0.97× |
| Disabled | `color.surfaceVariant` | None | 1.0× |

**Behavior rules:**
- Minimum width: 160dp
- Icon (if present): 20dp, left-aligned, 8dp gap to label
- Focus animation: scale to 1.05× in 150ms with `FastOutSlowIn` easing
- Disabled text: `color.textDisabled`

---

#### Secondary Button

```
┌──────────────────────────────┐
│  [Icon]  LABEL TEXT          │  Height: 52dp
└──────────────────────────────┘  Background: Transparent
                                  Border: 1.5dp color.divider (default), color.focusBorder (focused)
```

| State | Background | Border | Scale |
|---|---|---|---|
| Default | Transparent | 1.5dp `color.divider` | 1.0× |
| Focused | `color.selected` | 2dp `color.focusBorder` | 1.05× |
| Pressed | `color.surfaceVariant` | 2dp `color.primary` | 0.97× |
| Disabled | Transparent | 1dp `color.textDisabled` | 1.0× |

---

### 5.2 Cards

#### Album / Playlist Card

```
┌─────────────────────┐
│                     │  ← Image: 200×200dp, 1:1, corner radius 8dp
│    [Album Art]      │
│                     │
└─────────────────────┘
  Album Title          ← typography.titleMedium, color.onSurface, maxLines=1
  Artist Name          ← typography.bodyMedium, color.textSecondary, maxLines=1
  Year (optional)      ← typography.caption, color.textDisabled
```

**Dimensions:**
- Card width: 200dp
- Card total height: 240dp (200dp image + 40dp text area)
- Text padding: 8dp top, 0dp horizontal (flush with image)
- Corner radius: 8dp (image only; text area has no background)

**Focus state:**
- Scale: 1.08× on the entire card
- Border: 2dp `color.focusBorder` around image
- Glow: `box-shadow` equivalent — 0dp offset, 12dp blur, `color.focusGlow`
- Duration: 150ms, `FastOutSlowIn`

**Behavior rules:**
- Image loads with a `color.surfaceVariant` placeholder skeleton
- On focus, title text may expand to 2 lines if truncated (optional, only if space allows)
- "Now Playing" indicator: 3dp left border in `color.secondary` on the card image

---

#### Artist Card

```
┌─────────────────────┐
│                     │  ← Image: 180×180dp, circular (50% radius)
│    [Artist Photo]   │
│                     │
└─────────────────────┘
      Artist Name      ← typography.titleSmall, color.onSurface, centered, maxLines=1
```

**Focus state:** Same as Album Card but applied to the circular image.

---

### 5.3 Track List Row

```
┌──────────────────────────────────────────────────────────────────┐
│  [#]  [Thumb 56dp]  Track Title              Duration  [•••]     │
│                     Artist · Album                               │
└──────────────────────────────────────────────────────────────────┘
  Row height: 72dp
  Padding: 16dp horizontal
  Divider: 1dp color.divider between rows (not after last row)
```

| Column | Width | Content |
|---|---|---|
| Track number | 40dp | `typography.caption`, `color.textDisabled`, right-aligned |
| Thumbnail | 56dp | Square, 4dp corner radius |
| Title + metadata | Flexible (fill) | Title: `typography.bodyLarge`; Subtitle: `typography.bodyMedium` `color.textSecondary` |
| Duration | 64dp | `typography.caption`, `color.textSecondary`, right-aligned |
| Overflow menu | 40dp | 3-dot icon, only visible when row is focused |

**Focus state:**
- Background: `color.selected`
- Left border: 3dp `color.primary`
- No scale animation (rows do not scale — only border + background change)
- Duration: 100ms

**Behavior rules:**
- "Now Playing" row: track number replaced by animated equalizer icon (`color.secondary`)
- Overflow menu (⋮) only appears when the row is focused; hidden otherwise
- D-pad Right on a focused row opens the overflow menu

---

### 5.4 Sidebar Navigation

```
┌──────────┐
│  [Logo]  │  ← 40dp height, 16dp top padding
├──────────┤
│ 🏠 Home  │  ← Selected item
│ 💿 Albums│
│ 🎵 Songs │
│ 🎤 Artists│
│ 📋 Playlists│
│ 🔍 Search│
├──────────┤
│ ⚙ Settings│
└──────────┘
```

**Dimensions:**

| State | Width | Icon size | Label |
|---|---|---|---|
| Expanded | 240dp | 24dp | Visible (`typography.titleSmall`) |
| Collapsed | 72dp | 24dp | Hidden |

**Item dimensions:**
- Row height: 56dp
- Icon: 24dp, centered at 36dp from left edge
- Label: 16dp left of icon right edge
- Padding: 16dp horizontal (expanded), 24dp horizontal (collapsed, icon centered)
- Corner radius: 8dp (right side only, for selected state pill)

**States:**

| State | Background | Text/Icon color | Left border |
|---|---|---|---|
| Default | Transparent | `color.textSecondary` | None |
| Focused | `color.selected` | `color.onSurface` | None |
| Selected (active screen) | `color.selected` | `color.primary` | 3dp `color.primary` |
| Focused + Selected | `color.primary` at 15% | `color.primary` | 3dp `color.primary` |

**Behavior rules:**
- Sidebar is always visible (collapsed or expanded); it never auto-hides
- D-pad Left from any content area moves focus to the sidebar
- D-pad Right from the sidebar moves focus to the first item in the content area
- Sidebar expands on focus, collapses when focus leaves (200ms transition)
- Sidebar does **not** overlay content — content area reflows

---

### 5.5 Now Playing Mini Bar

Persistent bar at the bottom of the screen (visible on all screens except Now Playing full screen).

```
┌──────────────────────────────────────────────────────────────────┐
│  [Art 48dp]  Track Title · Artist     [⏮] [⏸] [⏭]  ━━━━━━━━━  │
└──────────────────────────────────────────────────────────────────┘
  Height: 72dp
  Background: color.surface with top border 1dp color.divider
```

**Focus behavior:**
- D-pad Down from content area focuses the mini bar
- D-pad Up from mini bar returns focus to content
- D-pad Center on mini bar opens Now Playing full screen
- Controls (prev/play/next) are individually focusable via D-pad Left/Right

---

## 6. Focus & Navigation

### 6.1 Focus Visibility Rules

Every focused element **must** have at least two of the following three indicators:

1. **Scale change:** 1.05×–1.10× (cards: 1.08×, buttons: 1.05×, rows: no scale)
2. **Border:** 2dp solid `color.focusBorder` (#6C63FF)
3. **Glow:** Drop shadow, 0dp offset, 12dp blur, `color.focusGlow` (#6C63FF40)

> **Rule:** Color change alone is **never** sufficient for focus indication.

---

### 6.2 Focus Animation Specification

| Property | Value |
|---|---|
| Scale (cards, buttons) | 1.0× → 1.08× |
| Scale (list rows) | No scale |
| Border width | 0dp → 2dp |
| Glow radius | 0dp → 12dp |
| Duration (focus gain) | 150ms |
| Duration (focus loss) | 100ms |
| Easing (focus gain) | `FastOutSlowIn` |
| Easing (focus loss) | `LinearOutSlowIn` |

---

### 6.3 Navigation Rules

#### D-pad Directional Expectations

| Direction | Expected behavior |
|---|---|
| ← Left | Move to previous item in row; if at row start, move to sidebar |
| → Right | Move to next item in row; if at row end, do nothing (no wrap) |
| ↑ Up | Move to item above in same column; if at top, move to section header or sidebar |
| ↓ Down | Move to item below; if at bottom, move to mini bar or do nothing |
| ⏎ Center/OK | Activate focused item (navigate, play, open) |
| ⬅ Back | Navigate back; if at root screen, show exit confirmation dialog |

#### No Dead Ends Rule

- Every screen must have a reachable Back action.
- Every dialog/overlay must have a focusable close/cancel button.
- If a list is empty, show an empty state with at least one focusable action (e.g., "Browse Albums" button).
- Grids with fewer items than columns must still allow navigation to all items.

#### Focus Memory

- When navigating Back to a screen, restore focus to the **last focused item** on that screen.
- When a screen reloads data, restore focus to the **same item by ID** if still present, otherwise focus the first item.
- Sidebar selection state persists across screen navigations.

---

### 6.4 Focus Regions

Screens are divided into focus regions. D-pad navigation stays within a region unless the user explicitly crosses a boundary.

| Region | Boundary crossing |
|---|---|
| Sidebar | D-pad Right → Content area |
| Content area | D-pad Left → Sidebar; D-pad Down → Mini bar |
| Mini bar | D-pad Up → Content area |
| Dialog | Focus trapped; Back → dismiss |

---

## 7. Motion & Animation

### 7.1 Focus Transitions

| Animation | Duration | Easing | Properties |
|---|---|---|---|
| Focus gain (card) | 150ms | `FastOutSlowIn` | scale 1.0→1.08, border 0→2dp, glow 0→12dp |
| Focus loss (card) | 100ms | `LinearOutSlowIn` | scale 1.08→1.0, border 2→0dp, glow 12→0dp |
| Focus gain (button) | 150ms | `FastOutSlowIn` | scale 1.0→1.05, border 0→2dp |
| Focus gain (row) | 100ms | `FastOutSlowIn` | background fade, left border 0→3dp |

---

### 7.2 Screen Transitions

| Transition | Type | Duration | Easing |
|---|---|---|---|
| Navigate forward (e.g., Albums → Album Detail) | Slide left + fade in | 250ms | `FastOutSlowIn` |
| Navigate back | Slide right + fade out | 200ms | `LinearOutSlowIn` |
| Open Now Playing (full screen) | Slide up | 300ms | `FastOutSlowIn` |
| Close Now Playing | Slide down | 250ms | `LinearOutSlowIn` |
| Sidebar expand | Width 72→240dp | 200ms | `FastOutSlowIn` |
| Sidebar collapse | Width 240→72dp | 150ms | `LinearOutSlowIn` |

---

### 7.3 Loading States

| State | Animation | Duration |
|---|---|---|
| Image loading | Shimmer skeleton (surface → surfaceVariant) | Loop until loaded |
| List loading | Skeleton rows (3–5 placeholder rows) | Loop until loaded |
| Grid loading | Skeleton cards (full grid of placeholders) | Loop until loaded |
| Playback buffering | Circular progress on play button | Loop until buffered |

**Shimmer specification:**
- Base color: `color.surface` (#141420)
- Highlight color: `color.surfaceVariant` (#1E1E2E)
- Direction: Left to right
- Duration: 1200ms per cycle
- Easing: Linear

---

### 7.4 When NOT to Animate

- Do **not** animate list item reordering (too distracting on TV).
- Do **not** animate background color changes.
- Do **not** use spring/bounce animations — they feel wrong on TV.
- Do **not** animate more than 3 elements simultaneously.
- Do **not** animate during active D-pad navigation (only animate on focus settle).

---

## 8. Screen Patterns

### 8.1 Home Screen

```
┌─────────────────────────────────────────────────────────────────┐
│ [Sidebar]  Recently Played                              See All  │
│            ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐                  │
│            │    │ │    │ │    │ │    │ │    │                   │
│            └────┘ └────┘ └────┘ └────┘ └────┘                  │
│                                                                  │
│            New Albums                                   See All  │
│            ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐                  │
│            │    │ │    │ │    │ │    │ │    │                   │
│            └────┘ └────┘ └────┘ └────┘ └────┘                  │
│                                                                  │
│ [Mini Bar]                                                       │
└─────────────────────────────────────────────────────────────────┘
```

- Section header: `typography.titleLarge`, 40dp bottom margin
- "See All" link: `typography.labelLarge`, `color.primary`, right-aligned, focusable
- Horizontal shelf: 5 cards, horizontal scroll, partial 6th card visible
- Vertical scroll: between sections
- Initial focus: First card of first shelf

---

### 8.2 Albums Screen

```
Layout: 5-column grid
Scroll: Vertical
Sort bar: Top of content area, horizontal list of sort options (A-Z, Year, Artist)
```

**Grid behavior:**
- 5 columns × N rows
- Column gutter: 16dp
- Row gutter: 24dp
- Sort bar height: 48dp, 16dp bottom margin
- Initial focus: First album card (top-left)
- Focus wraps: No horizontal wrap; D-pad Left on column 1 → sidebar

**Sort bar:**
- Items: Chips (pill-shaped), height 36dp, 16dp horizontal padding
- Selected chip: `color.primary` background, `color.textOnPrimary` text
- Unselected chip: `color.surfaceVariant` background, `color.textSecondary` text
- Focus: 2dp `color.focusBorder` border, 1.05× scale

---

### 8.3 Album Detail Screen

```
┌─────────────────────────────────────────────────────────────────┐
│ [Sidebar]  ┌──────────┐  Album Title (displayMedium)            │
│            │          │  Artist Name (titleMedium, secondary)   │
│            │ Album Art│  Year · Genre · N tracks (caption)      │
│            │ 280×280dp│                                         │
│            └──────────┘  [▶ PLAY]  [+ QUEUE]  [♥ SAVE]         │
│                                                                  │
│            ─────────────────────────────────────────────────    │
│            #  Title                          Duration           │
│            1  Track Name                     3:42               │
│            2  Track Name                     4:15               │
│            ...                                                   │
│ [Mini Bar]                                                       │
└─────────────────────────────────────────────────────────────────┘
```

**Header layout:**
- Album art: 280×280dp, 8dp corner radius, left-aligned
- Metadata block: right of art, vertically centered
- Action buttons: below metadata, horizontal row, 16dp gap
- Header total height: 320dp
- Divider: 1dp `color.divider`, 24dp vertical margin

**Track list:**
- Starts below header divider
- Scrolls vertically (header is sticky — does not scroll)
- "Now Playing" track highlighted with `color.selected` background + equalizer icon

---

### 8.4 Playlist Screen

```
Layout: Same as Album Detail but with playlist-specific metadata
Header: Playlist art (280dp) + name + track count + total duration
Actions: [▶ PLAY ALL]  [🔀 SHUFFLE]  [✏ EDIT]
Track list: Same as Album Detail track list
```

**Differences from Album Detail:**
- No "Year" or "Genre" metadata
- Shows "Created by [username]" in `typography.bodyMedium`
- Edit button only visible if playlist is user-owned

---

### 8.5 Now Playing Screen

```
┌─────────────────────────────────────────────────────────────────┐
│                  [Blurred album art background]                  │
│                                                                  │
│   ┌──────────────────┐    Track Title (displayMedium)           │
│   │                  │    Artist Name (titleLarge, secondary)   │
│   │   Album Art      │    Album Name (titleSmall, disabled)     │
│   │   480×480dp      │                                          │
│   │                  │    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│   └──────────────────┘    0:42                          3:58    │
│                                                                  │
│                       [⏮]  [⏸]  [⏭]                           │
│                       [🔀]  [🔁]  [♥]  [⋮]                     │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

**Background treatment:**
- Full-screen blurred album art: `blur radius = 40dp`, `brightness = 0.3`
- Overlay: `color.background` at 60% opacity on top of blur
- Album art: 480×480dp, centered-left, 12dp corner radius, elevation shadow

**Controls layout:**
- Primary controls (prev/play/next): 72dp icon buttons, 32dp gap
- Secondary controls (shuffle/repeat/like/more): 48dp icon buttons, 24dp gap
- Progress bar: Full content width, 4dp height, `color.primary` fill, `color.surfaceVariant` track
- Progress handle: 16dp circle, `color.primary`, only visible when progress bar is focused

**Focus order (D-pad):**
- Up/Down: Between primary controls and secondary controls
- Left/Right: Between buttons within a row
- Back: Return to previous screen (mini bar visible again)

---

### 8.6 Search Screen

```
┌─────────────────────────────────────────────────────────────────┐
│ [Sidebar]  🔍 [Search input field                          ]    │
│                                                                  │
│            Recent Searches                                       │
│            • Artist Name                                         │
│            • Album Name                                          │
│                                                                  │
│            [Results grid — 4 columns when query active]         │
│ [Mini Bar]                                                       │
└─────────────────────────────────────────────────────────────────┘
```

- Search field: Full content width, 56dp height, `color.surfaceVariant` background
- Initial focus: Search input field
- Results appear below as user types (debounce: 300ms)
- Results grid: 4 columns (mixed albums/artists/tracks)
- Recent searches: Shown when field is empty and focused

---

## 9. Iconography

### 9.1 Icon Style

- **Library:** Material Icons (Rounded variant)
- **Rationale:** Pre-bundled with Android, consistent with TV platform conventions

### 9.2 Icon Sizes

| Context | Size | Touch target |
|---|---|---|
| Navigation sidebar | 24dp | 56dp row height |
| Primary playback controls | 48dp | 72dp button |
| Secondary playback controls | 32dp | 48dp button |
| Track row actions | 20dp | 40dp |
| Button icons | 20dp | Button height |
| Status indicators | 16dp | N/A (non-interactive) |

### 9.3 Icon Usage Rules

- Use **filled** icons for active/selected states, **outlined** icons for inactive states.
- Exception: Playback controls always use filled icons regardless of state.
- Do **not** use icons without labels in the sidebar (expanded state).
- Do **not** use custom icons where a Material icon exists.
- Icon color follows text color rules: `color.onSurface` (active), `color.textSecondary` (inactive), `color.primary` (selected/playing).

---

## 10. Background & Visual Depth

### 10.1 Base Background

- Root background: `color.background` (#0A0A0F) — near-black with a slight blue tint
- Never use pure black (#000000) — it creates harsh contrast with OLED screens

### 10.2 Elevation System

TuneFlow uses a flat elevation model. Depth is conveyed through color, not shadows.

| Level | Color | Usage |
|---|---|---|
| 0 (base) | `color.background` (#0A0A0F) | Screen background |
| 1 (surface) | `color.surface` (#141420) | Cards, sidebar, mini bar |
| 2 (raised) | `color.surfaceVariant` (#1E1E2E) | Input fields, chips, tooltips |
| 3 (overlay) | `color.overlay` (#000000CC) | Modal backgrounds |

> **Rule:** Do not use `elevation` or `shadow` on cards in the grid — it creates visual noise. Use color differentiation only.

### 10.3 Gradient Usage

Gradients are permitted only in these specific contexts:

| Context | Gradient | Purpose |
|---|---|---|
| Card image bottom | `transparent → #000000AA` (vertical, bottom 40%) | Improve text legibility over art |
| Now Playing background | Album art → blur + dark overlay | Immersive background |
| Hero banner bottom | `transparent → color.background` (vertical, bottom 30%) | Blend into page background |

**Forbidden gradient uses:**
- Decorative gradients on UI chrome (sidebar, headers, buttons)
- Horizontal gradients on text
- Animated gradients

### 10.4 Blur Usage

Blur is **only** used on the Now Playing screen background.

| Property | Value |
|---|---|
| Source | Current track album art |
| Blur radius | 40dp |
| Brightness | 30% of original |
| Overlay | `color.background` at 60% opacity |
| Implementation | Pre-render blurred bitmap; do not use real-time blur |

---

## 11. Accessibility

### 11.1 Text Size Requirements

| Text type | Minimum size | Recommended size |
|---|---|---|
| Body / list items | 14sp | 16sp |
| Captions / metadata | 12sp | 14sp |
| Button labels | 14sp | 14sp |
| Screen titles | 24sp | 28sp+ |

> **Rule:** Never scale text below 12sp. If content doesn't fit at minimum size, truncate with ellipsis.

### 11.2 Contrast Ratios

| Text type | Minimum contrast | Target |
|---|---|---|
| Primary text (titles, track names) | 7:1 | 10:1+ |
| Secondary text (metadata, subtitles) | 4.5:1 | 6:1 |
| Disabled text | No requirement | ~2.5:1 |
| Focus border vs. background | 3:1 | 4.5:1 |

### 11.3 Focus Visibility Requirements

- Focus indicator must be visible against **all** possible backgrounds (dark cards, light album art).
- Use both border AND scale change — never rely on a single indicator.
- Focus border minimum width: 2dp.
- Focus glow minimum blur: 8dp.
- Focused element must be fully visible on screen (auto-scroll to bring focused item into view).

### 11.4 Content Descriptions

All interactive elements must have `contentDescription` set:

```kotlin
// ✅ Correct
IconButton(
    onClick = { /* ... */ },
    modifier = Modifier.semantics { contentDescription = "Play ${track.title}" }
)

// ❌ Wrong
IconButton(onClick = { /* ... */ }) {
    Icon(Icons.Filled.PlayArrow, contentDescription = null)
}
```

### 11.5 TalkBack / Accessibility Service

- All screens must be navigable with TalkBack enabled.
- Group related elements with `Modifier.semantics(mergeDescendants = true)`.
- Announce playback state changes with `LiveRegion`.

---

## 12. Performance Guidelines

### 12.1 Animation Performance

- Use `animateFloatAsState` / `animateContentSize` — these are hardware-accelerated.
- Do **not** animate `Modifier.size()` in a loop — triggers layout passes.
- Do **not** use `Canvas` drawing for focus effects — use `Modifier.border()` + `Modifier.shadow()`.
- Limit simultaneous animations to 3 elements maximum.
- Disable animations when `LocalReduceMotion.current == true`.

### 12.2 Image Loading

| Rule | Specification |
|---|---|
| Library | Coil (with OkHttp) |
| Placeholder | `color.surfaceVariant` colored box (same dimensions as image) |
| Error state | Generic music note icon on `color.surfaceVariant` background |
| Cache | Disk cache: 256MB; Memory cache: 64MB |
| Album art size (grid) | Load at 2× card width (400dp → 400px at 1× density) |
| Album art size (detail) | Load at 560px (280dp × 2) |
| Album art size (Now Playing) | Load at 960px (480dp × 2) |
| Blur source | Load at 64px, blur in software — do not load full-res for blur |

### 12.3 Lazy Loading

- Use `LazyVerticalGrid` / `LazyColumn` for all lists and grids.
- Pre-fetch: Load items 2 rows ahead of visible area (`prefetchDistance = 2`).
- Do **not** use `Column` with `verticalScroll` for lists longer than 20 items.
- Paginate API calls: Load 50 items per page; trigger next page when 10 items from end.

### 12.4 Rendering

- Target: 60fps on all screens, including during D-pad navigation.
- Avoid `recomposition` triggers in focus callbacks — use `derivedStateOf` for derived focus state.
- Profile with Android Studio's Compose Layout Inspector before each release.
- Avoid `Modifier.drawBehind` for complex shapes — use `Modifier.background` with `Shape`.

---

## 13. Implementation Notes

### 13.1 Theme Structure

```kotlin
// TuneFlowTheme.kt
object TuneFlowTheme {
    val colors: TuneFlowColors = TuneFlowColors()
    val typography: TuneFlowTypography = TuneFlowTypography()
    val shapes: TuneFlowShapes = TuneFlowShapes()
    val spacing: TuneFlowSpacing = TuneFlowSpacing()
    val motion: TuneFlowMotion = TuneFlowMotion()
}
```

### 13.2 Color Tokens

```kotlin
data class TuneFlowColors(
    val background: Color = Color(0xFF0A0A0F),
    val surface: Color = Color(0xFF141420),
    val surfaceVariant: Color = Color(0xFF1E1E2E),
    val overlay: Color = Color(0xCC000000),
    val primary: Color = Color(0xFF6C63FF),
    val primaryVariant: Color = Color(0xFF8B85FF),
    val secondary: Color = Color(0xFFFF6584),
    val tertiary: Color = Color(0xFF43E97B),
    val onBackground: Color = Color(0xFFFFFFFF),
    val onSurface: Color = Color(0xFFF0F0FF),
    val textSecondary: Color = Color(0xFFA0A0C0),
    val textDisabled: Color = Color(0xFF505070),
    val textOnPrimary: Color = Color(0xFFFFFFFF),
    val focusBorder: Color = Color(0xFF6C63FF),
    val focusGlow: Color = Color(0x406C63FF),
    val selected: Color = Color(0x1A6C63FF),
    val divider: Color = Color(0x14FFFFFF),
)
```

### 13.3 Typography Tokens

```kotlin
data class TuneFlowTypography(
    val displayLarge: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 48.sp, fontWeight = FontWeight.Bold, lineHeight = 56.sp
    ),
    val displayMedium: TextStyle = TextStyle(
        fontSize = 36.sp, fontWeight = FontWeight.Bold, lineHeight = 44.sp
    ),
    val titleLarge: TextStyle = TextStyle(
        fontSize = 28.sp, fontWeight = FontWeight.SemiBold, lineHeight = 36.sp
    ),
    val titleMedium: TextStyle = TextStyle(
        fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 30.sp
    ),
    val titleSmall: TextStyle = TextStyle(
        fontSize = 18.sp, fontWeight = FontWeight.Medium, lineHeight = 26.sp
    ),
    val bodyLarge: TextStyle = TextStyle(
        fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp
    ),
    val bodyMedium: TextStyle = TextStyle(
        fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp
    ),
    val labelLarge: TextStyle = TextStyle(
        fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp
    ),
    val caption: TextStyle = TextStyle(
        fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp
    ),
)
```

### 13.4 Spacing Tokens

```kotlin
data class TuneFlowSpacing(
    val safeMarginHorizontal: Dp = 48.dp,
    val safeMarginVertical: Dp = 27.dp,
    val gridGutterColumn: Dp = 16.dp,
    val gridGutterRow: Dp = 24.dp,
    val sectionSpacing: Dp = 40.dp,
    val cardPadding: Dp = 8.dp,
    val listItemPadding: Dp = 16.dp,
    val sidebarWidthExpanded: Dp = 240.dp,
    val sidebarWidthCollapsed: Dp = 72.dp,
)
```

### 13.5 Motion Tokens

```kotlin
data class TuneFlowMotion(
    val focusGainDuration: Int = 150,       // ms
    val focusLossDuration: Int = 100,       // ms
    val screenTransitionDuration: Int = 250, // ms
    val sidebarTransitionDuration: Int = 200, // ms
    val focusScale: Float = 1.08f,          // cards
    val focusScaleButton: Float = 1.05f,    // buttons
    val focusBorderWidth: Dp = 2.dp,
    val focusGlowRadius: Dp = 12.dp,
)
```

### 13.6 Focus Modifier Pattern

```kotlin
// Reusable focus modifier for cards
fun Modifier.tuneFlowFocusCard(
    isFocused: Boolean,
    colors: TuneFlowColors = TuneFlowTheme.colors,
    motion: TuneFlowMotion = TuneFlowTheme.motion,
): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (isFocused) motion.focusScale else 1f,
        animationSpec = tween(
            durationMillis = if (isFocused) motion.focusGainDuration else motion.focusLossDuration,
            easing = if (isFocused) FastOutSlowInEasing else LinearOutSlowInEasing
        )
    )
    return this
        .scale(scale)
        .then(
            if (isFocused) Modifier.border(
                width = motion.focusBorderWidth,
                color = colors.focusBorder,
                shape = RoundedCornerShape(8.dp)
            ) else Modifier
        )
}
```

### 13.7 Naming Conventions

| Element | Convention | Example |
|---|---|---|
| Color tokens | `TuneFlowTheme.colors.[name]` | `TuneFlowTheme.colors.primary` |
| Typography tokens | `TuneFlowTheme.typography.[name]` | `TuneFlowTheme.typography.titleLarge` |
| Spacing tokens | `TuneFlowTheme.spacing.[name]` | `TuneFlowTheme.spacing.safeMarginHorizontal` |
| Motion tokens | `TuneFlowTheme.motion.[name]` | `TuneFlowTheme.motion.focusScale` |
| Composables | `PascalCase` | `AlbumCard`, `TrackListRow`, `SidebarNav` |
| Modifiers | `camelCase` prefixed with `tuneFlow` | `tuneFlowFocusCard()`, `tuneFlowSafeArea()` |
| ViewModels | `[Feature]ViewModel` | `AlbumsViewModel`, `PlaybackViewModel` |
| Screens | `[Feature]Screen` | `AlbumsScreen`, `NowPlayingScreen` |

### 13.8 Compose Patterns

**Safe area wrapper:**
```kotlin
@Composable
fun TuneFlowSafeArea(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = TuneFlowTheme.spacing.safeMarginHorizontal,
                vertical = TuneFlowTheme.spacing.safeMarginVertical
            ),
        content = content
    )
}
```

**Focus-aware card:**
```kotlin
@Composable
fun AlbumCard(album: Album, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .tuneFlowFocusCard(isFocused)
    ) {
        // card content
    }
}
```

---

## 14. Branding & Asset System

### 14.1 Purpose

This section defines the visual identity rules for TuneFlow beyond component styling. It governs logo design, app icon design, branded backgrounds, asset generation boundaries, and brand usage consistency across Android TV / Fire TV surfaces.

> **Rule:** Branding must reinforce the existing product principles defined in this document, especially **Premium Restraint** (§1.6), **Clarity Over Density** (§1.2), and **Performance-First Design** (§1.5).

Branding must **never** overpower content. Album art remains the primary source of visual richness inside the product.

---

### 14.2 Brand Direction

#### Core Brand Attributes

TuneFlow's visual identity should feel:

- Minimal
- Modern
- Calm
- Premium
- Fluid
- Music-centric
- Dark-first
- TV-readable

#### Concept Direction

The brand should express music in motion using restrained visual metaphors:

- Flowing waveforms
- Continuous ribbons
- Curved signal paths
- Abstract sound pulses
- Smooth rhythmic movement

Avoid literal or overused music clichés unless abstracted into a clean, geometric mark.

Permitted motifs:
- A flowing wave that suggests continuity
- A compact symbol that merges "play" and "flow"
- Curved line systems inspired by sound paths or streaming motion

#### Brand Restraint Rules

| ✅ Do | ❌ Don't |
|---|---|
| Use simple geometry | Use loud neon styling across large surfaces |
| Prefer strong silhouette over fine detail | Add decorative visual complexity without functional purpose |
| Keep branding dark-theme compatible | Introduce multiple competing accent colors for brand assets |
| Use `color.primary` (#6C63FF) as the dominant accent | Depend on tiny internal detail for recognition |
| Design for recognition at distance and at small sizes | Create marks that resemble generic equalizers or stock music logos |

---

### 14.3 Logo System

TuneFlow must define a complete logo system consisting of:

- Primary symbol mark
- Horizontal logo
- Monochrome logo
- Small-size simplified mark

#### A. Primary Symbol Mark

**Usage:** app icon basis, splash screen, sidebar logo area, settings/about screen, developer and store assets.

**Requirements:**
- Must be readable at small sizes
- Must work on dark backgrounds
- Must use simple, scalable vector geometry
- Must remain identifiable without accompanying text

#### B. Horizontal Logo

**Usage:** store listings, documentation, website or marketing surfaces, settings/about branding lockup.

**Requirements:**
- Symbol + wordmark composition
- Balanced spacing and optical alignment
- Wordmark must be clean and modern — not stylized to the point of reduced readability

#### C. Monochrome Logo

**Usage:** low-color contexts, export fallback, watermark or subtle overlay usage.

**Requirements:**
- Must work in pure white or single-color form
- Must preserve silhouette recognition without accent color

#### D. Simplified Small-Size Mark

**Usage:** launcher icon basis, compact sidebar/logo slot, favicon-equivalent contexts.

**Requirements:**
- Remove micro-detail
- Preserve core silhouette
- No internal elements smaller than what remains legible at small TV launcher sizes

#### Logo Styling Rules

| Property | Value |
|---|---|
| Primary accent | `color.primary` (#6C63FF) |
| Monochrome fallback | White on dark background |
| Fill style | Flat color (default) |
| Gradients | Discouraged — only for approved marketing-only usage |
| Effects | No glow, bevel, emboss, or pseudo-3D |

#### Clear Space

Minimum clear space around the logo must equal the visual height of the logo's dominant internal shape, or at least **8dp** in UI contexts, whichever is greater.

#### Minimum Size

| Context | Minimum size |
|---|---|
| UI logo mark (sidebar slot) | **40dp** height — matches the sidebar logo area defined in §5.4 |
| Compact/favicon mark | 24dp — only for non-UI favicon-equivalent contexts |
| Horizontal lockup | 120dp width |
| App/store icon source | High-resolution vector master; rasterized per density target |

> **Correction note:** The sidebar logo slot is defined as 40dp height in §5.4. The 24dp minimum applies only to compact/favicon contexts, not to the primary UI logo placement.

#### Logo Usage Rules

| ✅ Do | ❌ Don't |
|---|---|
| Use the colored logo on dark neutral surfaces | Stretch or distort the logo |
| Use the monochrome logo where contrast or simplicity is needed | Recolor it with arbitrary hues |
| Maintain consistent proportions | Place it on noisy artwork without a contrast layer |
| Keep sufficient clear space | Add effects, outlines, or shadows not defined in this system |
| | Switch between multiple unofficial logo variants |

---

### 14.4 App Icon System (Android TV / Fire TV)

#### Design Goals

The app icon must be:
- Bold and clean
- Immediately recognizable
- Readable from a distance
- Consistent with the logo system
- Effective at small launcher sizes

#### Icon Composition

The app icon should use:
- One dominant symbol
- A high-contrast silhouette
- Restrained internal detail
- A dark-compatible base treatment

Preferred structure:
- Foreground symbol derived from the TuneFlow mark
- Simple background shape or field
- **No text inside the icon**

#### Adaptive / Layered Guidance

Where platform requirements support layered or adaptive assets:
- Foreground layer: symbol only
- Background layer: flat or subtly tonal field
- Maintain generous internal padding
- Avoid placing critical details near rounded-corner crop areas

#### Safe Area

Critical icon content must remain within a centered safe area of **72%–80%** of the full canvas.

> **Rule:** No essential detail may touch the outer crop boundary.

#### Icon Style Rules

| ✅ Do | ❌ Don't |
|---|---|
| Use a bold, centered mark | Use text labels in the app icon |
| Preserve silhouette readability | Use thin line-only marks |
| Keep contrast clean and controlled | Use cluttered multi-part compositions |
| Favor flat or low-complexity backgrounds | Depend on subtle gradients for recognition |
| | Add visual noise to the background |

#### Export Targets

| Density | Size |
|---|---|
| mdpi | 48×48px |
| hdpi | 72×72px |
| xhdpi | 96×96px |
| xxhdpi | 144×144px |
| xxxhdpi | 192×192px |

Also prepare store listing exports for Android TV / Fire TV submission requirements.

> **Rule:** Source artwork must be authored as vector first, then rasterized to required targets.

---

### 14.5 Branded Background Assets

This section extends, and must obey, the restrictions in **§10 Background & Visual Depth**.

#### Allowed Branded Background Contexts

- Splash / startup background
- Optional hero background surfaces
- Marketing / store imagery
- Now Playing supporting artwork treatment (see §10.4)

#### Background Style Direction

Background assets should be:
- Dark
- Low-noise
- Softly atmospheric
- Motion-inspired but **static**
- Compatible with overlay UI
- Visually restrained

Appropriate visual treatments:
- Subtle flowing wave abstractions
- Soft radial or directional glow
- Faint ribbon-like motion forms
- Controlled gradient fields
- Blurred music-inspired geometry

#### Forbidden Background Treatments

- Noisy textures
- Heavy particle effects
- Detailed illustrations behind active UI
- High-contrast pattern fields
- **Animated decorative backgrounds** — Exception: shimmer loading skeletons (§7.3) are functional animations on placeholder surfaces and are permitted
- Bright gradients on main navigation surfaces
- Anything that competes with album art or focused components

#### Relationship to Existing Background Rules

The following constraints from §10 remain authoritative and are not overridden by this section:

| Rule | Source |
|---|---|
| No decorative gradients on UI chrome | §10.3 |
| Blur restricted to Now Playing background | §10.4 |
| No animated gradients | §10.3 |
| No real-time blur for scrolling or interactive surfaces | §12.1 |

#### Now Playing Exception

Now Playing may continue to use the album-art-derived blur system defined in §10.4. Additional branded overlays are permitted only if they remain subtle and do not reduce readability or performance.

---

### 14.6 AI Asset Generation Prompts

AI generation may be used to accelerate concept exploration for logos, background art, and branded visual directions. Final production assets must be cleaned, simplified, and approved before implementation.

> **Rule:** AI-generated outputs are concept inputs, not production-ready deliverables by default.

#### Prompt Requirements

All prompts must emphasize:
- Dark UI compatibility
- Premium restraint
- Low visual noise
- Strong silhouette
- Minimal clutter
- Scalability
- TV readability
- No text unless explicitly required

#### A. Logo Concept Prompt

```
Minimal modern logo for a music streaming app called TuneFlow. Abstract flowing
sound-wave symbol, geometric and premium, dark-theme compatible, clean vector-like
form, strong silhouette, minimal clutter, scalable for app icon and TV interface
branding, restrained and elegant, no mockup text effects, no 3D.
```

#### B. App Icon Prompt

```
Square app icon for a premium music streaming TV app called TuneFlow. Bold abstract
flowing waveform symbol, centered composition, dark background, subtle purple accent,
high contrast, minimal, polished, highly legible at small size, no text, no tiny
details, no visual clutter.
```

#### C. Dark Background Prompt

```
Dark cinematic background for a premium music streaming TV interface. Soft gradient
field, subtle flowing wave patterns, low visual noise, restrained glow, minimal
composition, elegant and modern, no text, no clutter, designed for overlay UI
readability.
```

#### D. Optional Hero Background Prompt

```
Premium abstract hero background for a music streaming application. Deep dark palette,
subtle motion-inspired ribbons, soft purple accent restraint, elegant, minimal,
cinematic, low detail density, suitable for TV UI, no text, no bright focal clutter.
```

#### Prompt Guardrails

Always include constraints:
- `no text`
- `no mockup framing`
- `no device frame`
- `no excessive glow`
- `no neon overload`
- `no dense detail`
- `no photoreal clutter`
- `no copyrighted brand references` — Do not reference specific brand names, copyrighted logos, or trademarked visual styles in prompts

---

### 14.7 Brand Usage Rules

#### Where Branding Appears

Logo and brand mark may be used in:
- Splash / launch screen
- Sidebar header/logo slot
- Settings/about screen
- Store listing assets
- Promotional surfaces
- Documentation and developer handoff materials

#### Where Branding Should Be Restrained

Brand graphics should be minimized or omitted in:
- Content-heavy browsing screens (grids, lists)
- Track rows
- Navigation surfaces where repetition adds clutter
- Now Playing if album art already provides sufficient visual richness

#### Accent Color Usage

Brand assets must align with the color system in §3.

> **Rule:** Branding must not introduce a second dominant accent system that conflicts with product interaction states. `color.primary` (#6C63FF) is the single brand accent.

#### Consistency Rules

- Do not mix multiple logo styles
- Do not combine unrelated illustration styles
- Keep brand visuals subordinate to media content
- Preserve harmony with the dark product palette
- Reuse approved motifs rather than inventing new brand language per screen

---

### 14.8 Brand Approval Criteria

An asset is approved only if it satisfies **all** of the following:

- [ ] Readable on dark UI
- [ ] Recognizable at small size (40dp UI minimum; 24dp compact minimum)
- [ ] Visually aligned with Premium Restraint (§1.6)
- [ ] Uses `color.primary` (#6C63FF) as the sole accent
- [ ] Compatible with Android TV / Fire TV launcher contexts
- [ ] Does not conflict with Material Rounded iconography (§9)
- [ ] Does not add rendering or APK-weight complexity beyond justified value

---

## 15. Asset Export & Developer Handoff

### 15.1 Purpose

This section defines how branding and visual assets are prepared for implementation so that designers and developers use a consistent, optimized asset package.

---

### 15.2 Source Formats

| Format | Usage |
|---|---|
| SVG | Logo, symbol mark, and any custom vector branding assets (preferred master) |
| PNG | Finalized raster UI/branding assets |
| WebP | Only when compression benefits are significant and platform compatibility is verified |

> **Rule:** Editable master assets must be preserved separately from exported delivery files.

---

### 15.3 Resolution Standards

#### TV Baseline

Primary composition target: **1920×1080**

#### 4K Support

Author source assets at **4K-native resolution (3840×2160) minimum** for backgrounds. For icons, author at **xxxhdpi (192×192px)** — this is sufficient for all current Android TV / Fire TV launcher densities.

> **Correction note:** "2× quality" is intentionally avoided as a specification — it is ambiguous across density contexts. Use the explicit pixel targets above instead.

#### Asset Categories

| Category | Resolution requirement |
|---|---|
| Launcher/store icons | Per density table in §14.4; vector master required |
| Splash assets | 1920×1080 minimum; 4K-safe vector source retained |
| Branded backgrounds | 1920×1080 minimum; 3840×2160 source retained for future use |
| Now Playing branded backgrounds | Only if approved per §14.5; same resolution rules as backgrounds |

---

### 15.4 Export Rules by Asset Type

#### Logo Assets

Deliver:
- Primary colored logo (SVG + PNG)
- Monochrome logo (SVG + PNG)
- Horizontal lockup (SVG + PNG)
- Compact mark (SVG + PNG)

#### App Icon Assets

Deliver:
- Source vector master
- Platform raster exports by density (see §14.4 export targets)
- Foreground and background layers where required by platform
- Store listing icon exports

#### Background Assets

Deliver:
- Flattened PNG for implementation
- Optional high-resolution source for future adaptation
- Naming that indicates context and version (see §15.6)

#### Prompt/Concept Assets

If AI generation is used, retain:
- Final approved prompt text
- Selected concept reference
- Production-cleaned export
- Rejected concepts excluded from developer handoff package

---

### 15.5 Performance & Optimization Rules

TuneFlow targets constrained TV hardware. Asset exports must be optimized.

| Rule | Detail |
|---|---|
| Prefer vector for scalable marks | Use SVG for logos and simple graphics |
| Avoid oversized raster assets | Use the smallest optimized version sufficient for the context |
| Compress raster assets | No visible degradation; use tools like `pngquant` or `cwebp` |
| No redundant variants | Do not ship multiple sizes when one suffices |
| No layered/oversized assets | Use flattened versions unless layers are required by platform |
| Avoid unnecessary alpha channels | Only use transparency where required |

#### Background Optimization

- Keep large background images efficient in file size
- Avoid unnecessary alpha channels on opaque backgrounds
- Pre-render static effects (blur, glow) — do not rely on runtime generation
- Do not use runtime-generated decorative effects (consistent with §12.1)

---

### 15.6 Naming Conventions

Pattern: `tuneflow_[category]_[context]_[variant]_[size]`

| Example filename | Meaning |
|---|---|
| `tuneflow_logo_primary_color.svg` | Primary colored logo, SVG master |
| `tuneflow_logo_horizontal_white.svg` | Horizontal lockup, monochrome white |
| `tuneflow_icon_launcher_xxhdpi.png` | Launcher icon at xxhdpi density |
| `tuneflow_bg_splash_dark_1080.png` | Splash background, dark, 1080p |
| `tuneflow_bg_hero_wave_4k.png` | Hero background, wave motif, 4K |

**Rules:**
- Lowercase only
- Underscore-separated
- No spaces
- No `final-final-v2` naming — version tracked in source control or package manifest, not embedded in filenames

---

### 15.7 Folder Structure

```
/branding/
  logo/
    svg/
    png/
  icon/
    android_tv/
    fire_tv/
  backgrounds/
    1080p/
    4k/
  store/
  prompts/
  source/
```

> This folder lives at the repository root alongside `/docs/`, `/plans/`, and `/scripts/`.

---

### 15.8 Developer Handoff Checklist

Every approved asset package must include:

**All assets:**
- [ ] Final asset file (implementation-ready format)
- [ ] Source file (editable master)
- [ ] Intended usage context
- [ ] Size/resolution information
- [ ] Light/dark compatibility note (if relevant)
- [ ] Implementation note (if special handling is required)
- [ ] Optimization status confirmed

**Icons and logos additionally:**
- [ ] Clear-space guidance (minimum 8dp in UI; see §14.3)
- [ ] Minimum size guidance (40dp UI; 24dp compact; see §14.3)
- [ ] Approved color variants listed

**Backgrounds additionally:**
- [ ] Intended screen/context documented
- [ ] Whether text overlay is expected
- [ ] Whether the asset is decorative, branded, or functional

---

### 15.9 Implementation Rule

> **No visual asset may be introduced into the product unless it is:**
> 1. Defined by this design system
> 2. Approved against brand and performance rules (§14.8)
> 3. Exported in implementation-ready form
> 4. Named and packaged according to §15.6
> 5. Included in the developer handoff checklist (§15.8)

---

## Appendix: Quick Reference Card

| Property | Value |
|---|---|
| Safe margin H | 48dp |
| Safe margin V | 27dp |
| Grid columns (albums) | 5 |
| Grid gutter | 16dp |
| Album card size | 200×240dp |
| Track row height | 72dp |
| Sidebar width (expanded) | 240dp |
| Focus scale (card) | 1.08× |
| Focus scale (button) | 1.05× |
| Focus border | 2dp #6C63FF |
| Focus glow | 12dp blur #6C63FF40 |
| Focus gain duration | 150ms |
| Focus loss duration | 100ms |
| Screen transition | 250ms |
| Primary color | #6C63FF |
| Background | #0A0A0F |
| Surface | #141420 |
| Min body text | 14sp |
| Min caption text | 12sp |
| Logo min size (UI) | 40dp |
| Logo min size (compact) | 24dp |
| Icon safe area | 72–80% of canvas |
| Brand accent | #6C63FF only |

---

*TuneFlow Design System v1.1 — Maintained by the TuneFlow team*
