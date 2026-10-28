# sipper — visual and interaction design

> `docs/CONTRACT.md` fixes tokens, widths, types and the API levels below. This file owns colour,
> type, spacing, row heights, the table primitive, the column tables, chips and the hatch. Where
> they disagree, the contract wins.

This is the specification I implement the UI from. Every number here is a value I typed into
code, not a range to pick from. Where I had a choice I made it and said why in one clause.

The design serves one rule: **a value the platform manufactured must never render as a number.**
The faces, the widths, the hatch and the `when` with no `else` all exist for that.

Reference points are Bull Board, a Sentry issue list, and the Stripe dashboard's Logs table.
Dense operator surfaces. Not a landing page, not a settings app.

Screen behaviour, states, flows and copy strings are `docs/SCREENS.md`. Nothing below decides
what a screen shows or when.

---

## 1. Faces

Two faces, and the rule between them is not stylistic.

| face | family | where |
|---|---|---|
| text | `FontFamily.Default` | prose, column headers, button labels, tab labels, section headings |
| mono | bundled Roboto Mono (400, 500) | **every machine value** |

A machine value is anything the device produced or anything that indexes into the platform:
package names, profile keys (`cpu.cluster_power.cluster0`), permission names, intent action
strings, numbers with or without units, API levels, standby-bucket integers, durations, byte
counts, exception class names, verdict tokens, resource ids (`0x01090000`), verbatim runtime
denial lines. Prose about those things is text face.

Column headers are text face, not mono, because a header is a label rather than a value; a
right-aligned mono column under a text-face header still aligns on the digits.

### Why the face is bundled

`FontFamily.Monospace` maps to `Typeface.MONOSPACE`, and vendors substitute that freely — Droid
Sans Mono on some ROMs, something else on others, and nothing stops an OEM shipping a
proportional face there. Every width in §10 is `chars × advance`, so a face whose advance I
cannot name makes the documented width table unreproducible on someone else's phone. That is the
argument: not that a bundled face looks better, but that a fixed-width table is only checkable
against a known advance.

- Two static weights, 400 and 500. Apache-2.0, one line in `NOTICE`.
- `app/src/main/res/font/roboto_mono_regular.ttf`, `roboto_mono_medium.ttf`.
- Measured advance for `"0"` at 12sp, `fontScale 1.0`: **7.2dp**. Every `chars` figure in §10
  multiplies by this.
- The tabular figures come free from the face being monospaced, so there is no
  `FontFeatureSetting` anywhere.

### The subset, pinned

A `FontFamily` built from a single bundled resource does not reliably fall back to the system
chain below API 29, so a codepoint outside the subset is a tofu box, not a substituted glyph.
The subset is therefore the exact set the app uses and it is a build input:

```
U+0020–U+007E   ASCII
U+00A0          no-break space
U+00B0 °        degrees
U+00B5 µ        microamp-hours
U+00B7 ·        status-strip and reason-chip separator
U+0394 Δ        the APPS delta column header and its speech form
U+03A3 Σ        the APPS summary line
U+2014 —        the absence glyph
U+2026 …        TextOverflow.Ellipsis and MiddleEllipsis
U+2190 ←        back-fill provenance, `0.1 ← screen.on`
U+2192 →        DEVICE cross-check lines
U+2260 ≠        the AUDIT disagreement glyph
```

`▲ ▼ ▸ ▾ ⋮` are not in it. They are icons (§5), not glyphs, and a text arrow is the first thing
that turns into a box on a device I do not own.

A JVM test in `:app` walks every string constant in the `ui` package and asserts each codepoint
is on that list. The subset is a build input, so no device is needed to check it.

---

## 2. Type scale

Three weights only — 400, 500, 600. A fourth weight is an invitation to decorate.

### Overridden Material 3 slots

Overriding `Typography` and `ColorScheme` is the sanctioned way to theme Material; overriding
component shapes, ripples and internals is the giveaway, so I do the first and not the second.
Slots I do not list stay at the M3 default because nothing in sipper uses them.

| M3 slot | size / line | weight | face | used for |
|---|---|---|---|---|
| `titleSmall` | 15 / 20 | 600 | text | headings inside a screen (`Provenance`, `Verdicts`) |
| `bodyMedium` | 13 / 18 | 400 | text | prose paragraphs, empty-state copy |
| `bodySmall` | 12 / 16 | 400 | text | secondary prose, unit suffixes, footnotes under a block |
| `labelMedium` | 11 / 14 | 600 | text | column headers, tab labels; `letterSpacing 0.02em` |
| `labelSmall` | 11 / 14 | 400 | text | captions, the `n of m` counters |

M3's `bodyMedium` default is 14/20; 13/18 is the density change and it is the only one.

### Roles outside M3, in `SipperType`

| role | size / line | weight | used for |
|---|---|---|---|
| `mono` | 12 / 16 | 400 | every table value cell, every identifier |
| `monoStrong` | 12 / 16 | 500 | the one value a row is about (the AUDIT `value` cell, the APPS delta) |
| `monoChip` | 10 / 12 | 500 | chip labels, and nothing else |
| `monoStrip` | 11 / 14 | 400 | the status strip |
| `monoBlock` | 11 / 15 | 400 | verbatim runtime output on PROBES; the multi-line mono |

No size above 15sp exists in the app. There is no display or headline type because there is no
hero to put it in.

---

## 3. Spacing

Base unit 4dp. The scale is `2, 4, 6, 8, 12, 16, 24`. Nothing else, and 24 appears twice in the
whole app (above and below the AUDIT provenance block).

The horizontal budget in full, so the totals in §10 derive rather than being asserted:

| | |
|---|---|
| row lead padding | 10dp |
| cell padding | 6dp each side, so 12dp between adjacent columns |
| frozen-column edge | 2dp |
| trailing padding after the last column | 10dp |

Tight because of an arithmetic constraint I can state exactly. The APPS grid has to put the
framework foreground number, my reconstructed number and their signed delta on screen at scroll
offset 0 — a reviewer who has to scroll to find the comparison has been shown nothing. At 12sp
mono, 7.2dp per character, the frozen package column and those three cost 266.4dp of characters.
The padding above adds 48 (four columns at 12) plus the 10dp lead and the 2dp edge, for 326.4dp
of the 360dp a phone gives me. The 33.6dp left over is how much of the next column shows, and a
sliver of a fifth column is what tells the reader the table scrolls. Moving the cell padding up
one step on the scale costs 16 of those 33.6dp, so the spacing is not a taste decision.

Vertical: rows carry no vertical padding at all. Row height is set directly and the content is
centred in it, so a row is exactly as tall as I say it is.

---

## 4. Colour

Semantic roles only. No component in the app names a hex. Tokens live in a `SipperColors` data
class provided through `LocalSipperColors`, alongside a Material `ColorScheme` that exists only
so Material components (buttons, tabs, filter chips) look correct.

**No dynamic colour.** Material You would let the wallpaper pick the state hues, and the state
hues carry the verdicts. **No in-app theme toggle** — `isSystemInDarkTheme()` and nothing else.

### 4.1 Surfaces, rules, text

| token | light | dark | used for |
|---|---|---|---|
| `surface` | `#FFFFFF` | `#0F1216` | table body, screen ground |
| `surfaceRaised` | `#F3F4F6` | `#171B21` | sticky header, status strip, section blocks |
| `surfaceSunken` | `#E9EBEF` | `#0A0C0F` | hatch ground, verbatim output blocks |
| `surfacePinned` | `#EDF1F7` | `#1B222B` | the pinned capacity row, a tapped row |
| `rule` | `#DDE1E6` | `#262B33` | 1dp separator under every table row |
| `ruleStrong` | `#8A9199` | `#5C6774` | header underline, frozen-column edge, group separators |
| `textPrimary` | `#1A1F26` | `#E3E7EC` | prose |
| `textSecondary` | `#5B646F` | `#98A1AC` | column headers, units, the `=` glyph, the `—` glyph, `Absent` chips, route chips |
| `textNumeric` | `#0E1216` | `#F4F7FA` | every mono machine value |
| `accent` | `#0B5FCC` | `#6FA8F5` | text buttons and links; never a fill |

`textNumeric` is darker than `textPrimary` in light and brighter in dark. Inverted from the usual
because in this app the machine value is the content and the prose around it is chrome.

### 4.2 State

Four audit verdicts, then the `Reading<T>` outcomes. Six hues, no two shared — green, amber, red,
indigo, teal, warm grey.

| token | meaning | light | dark |
|---|---|---|---|
| `statePresent` | verdict `PRESENT` | `#14713A` | `#5BC27E` |
| `stateExplicitZero` | verdict `ZERO-BY-EXPLICIT-VALUE` | `#8A5200` | `#DFA43C` |
| `stateAbsentZero` | verdict `ZERO-BY-ABSENCE` | `#A62015` | `#F0796B` |
| `stateBackFilled` | verdict `BACK-FILLED` | `#3B36A6` | `#A6A2F2` |
| `stateDenied` | `Reading.Denied` | `#0F5468` | `#63C3E0` |
| `stateStale` | sample older than its window | `#6B5F4A` | `#B4A78E` |

`ZERO-BY-ABSENCE`, `BACK-FILLED` and a silent default are the three outcomes the audit has to
keep apart, so none of them shares a hue with another.

`Reading.Absent` has no token of its own; it renders in `textSecondary`. Absence of an API is
chrome, not alarm, and a seventh grey one step off `textSecondary` is a distinction the eye
cannot make.

**`Reading.SilentDefault` has no hue at all.** It is a hatch on `hatchGround` with a cause chip
in `textSecondary` (§9). Not adding a colour is the decision: a coloured cell reads as a value,
and a value is the one thing that cell must not read as. It is distinct from all four verdicts by
texture as well as by the absence of a number.

### 4.3 Hatch

| token | light | dark |
|---|---|---|
| `hatchGround` | `#E9EBEF` (= `surfaceSunken`) | `#0A0C0F` |
| `hatchLine` | `textPrimary` @ 22% → `#BBBEC3` | `textPrimary` @ 26% → `#424548` |

### 4.4 Contrast, produced not asserted

Computed with the WCAG 2.x relative-luminance formula, sRGB, every text token against all four
backgrounds it can land on plus its own chip composite (token at 12% over the background in
light, 18% in dark). `:app`'s JVM test source set holds `ContrastTest`, which recomputes these
from the committed token values and fails below 4.5:1 for any text pairing;
`./gradlew :app:contrastDoc` regenerates both tables. No number below was typed by hand.

**Light**

| token | surface | raised | sunken | pinned | chip/surface | chip/raised |
|---|---:|---:|---:|---:|---:|---:|
| `textPrimary` | 16.56 | 15.05 | 13.88 | 14.61 | 13.04 | 11.85 |
| `textSecondary` | 6.00 | 5.46 | 5.03 | 5.30 | 5.08 | 4.67 |
| `textNumeric` | 18.80 | 17.09 | 15.75 | 16.59 | 14.62 | 13.31 |
| `accent` | 5.96 | 5.42 | 4.99 | 5.26 | 5.00 | 4.55 |
| `statePresent` | 6.08 | 5.53 | 5.10 | 5.37 | 5.11 | 4.66 |
| `stateExplicitZero` | 6.39 | 5.80 | 5.35 | 5.64 | 5.35 | 4.91 |
| `stateAbsentZero` | 7.40 | 6.72 | 6.20 | 6.53 | 6.01 | 5.51 |
| `stateBackFilled` | 9.27 | 8.43 | 7.77 | 8.18 | 7.57 | 6.90 |
| `stateDenied` | 8.44 | 7.67 | 7.07 | 7.45 | 6.92 | 6.36 |
| `stateStale` | 6.25 | 5.68 | 5.24 | 5.51 | 5.29 | 4.83 |

Minimum **4.55**.

**Dark**

| token | surface | raised | sunken | pinned | chip/surface | chip/raised |
|---|---:|---:|---:|---:|---:|---:|
| `textPrimary` | 15.12 | 13.91 | 15.77 | 12.90 | 9.48 | 8.40 |
| `textSecondary` | 7.18 | 6.61 | 7.49 | 6.13 | 5.37 | 4.86 |
| `textNumeric` | 17.46 | 16.07 | 18.21 | 14.91 | 10.47 | 9.27 |
| `accent` | 7.69 | 7.07 | 8.02 | 6.56 | 5.71 | 5.15 |
| `statePresent` | 8.46 | 7.79 | 8.82 | 7.22 | 6.14 | 5.56 |
| `stateExplicitZero` | 8.51 | 7.83 | 8.87 | 7.26 | 6.24 | 5.58 |
| `stateAbsentZero` | 6.84 | 6.30 | 7.14 | 5.84 | 5.24 | 4.77 |
| `stateBackFilled` | 8.11 | 7.47 | 8.46 | 6.92 | 5.94 | 5.35 |
| `stateDenied` | 9.31 | 8.57 | 9.71 | 7.95 | 6.62 | 5.97 |
| `stateStale` | 7.92 | 7.29 | 8.26 | 6.76 | 5.82 | 5.26 |

Minimum **4.77**.

Every text pairing clears AA at 4.5:1 in both themes. Nothing relies on the 3:1 large-text
allowance, because there is no large text. The two chip columns are both computed because a chip
sits on `surface` inside a table row and on `surfaceRaised` in a header or a section block;
`chip/raised` is the worse of the two and is the one I check against.

**Three non-text values, stated rather than hidden.** They are a committed allowlist in
`ContrastTest`, each line carrying its reason.

- `rule` is 1.31 (light) / 1.32 (dark).
- `ruleStrong` is 3.19 against `surface` and 2.90 against `surfaceRaised` in light, 3.26 and 3.00
  in dark, so the header underline sits with one side just under 3:1.
- `hatchLine` is 1.56 / 2.03 over its ground.

All three are reinforcement — the frozen column, the column alignment, the chip text and the
verdict word each carry the same information at full contrast — so 1.4.11 does not bite.

---

## 5. Shape, elevation, motion, icons

- **Corner radius** 0 everywhere except chips at 3dp. Not pills; a pill reads as a marketing tag.
  Material's own components keep their default shapes.
- **Elevation 0.** Zero shadows in the app. The sticky header is separated by `ruleStrong`, since
  a shadow under a 28dp row grid turns into mud. Bull Board and Sentry both use rules.
- **No `Card` in the codebase.** Grouping is a section heading with a `ruleStrong` above it.
- **Motion:** the Material ripple at its default, plus `animateContentSize()` at 120ms on row
  expand. Nothing else animates. A table that animates when data changes makes a changed number
  harder to find.
- **Four icons**, all from `material-icons-core`:

| icon | where |
|---|---|
| `ArrowDropUp` / `ArrowDropDown` | the sorted column header's caret, and the APPS row expander, rotated |
| `OpenInNew` | a control that leaves the app |
| `MoreVert` | the overflow on AUDIT and PROBES |

No icon accompanies a text label except `OpenInNew`, which marks that the tap leaves the app.

---

## 6. Chrome

**No `TopAppBar`.** The tab row is the only top chrome, which buys back 64dp on a 640dp screen.

*Shapes only; every number inside the box below is invented and none is a device fact.*

```
+-------------------------------------------------------------+  PrimaryTabRow, 48dp, M3 default
|  AUDIT    APPS    SELF    PROBES    DEVICE                   |  labelMedium 11/600
+-------------------------------------------------------------+  ruleStrong 1dp
|  sticky table header                                    30dp |  surfaceRaised
+=============================================================+  ruleStrong 1dp
|  rows                                                        |  surface
|                                                              |
+-------------------------------------------------------------+  ruleStrong 1dp
|  status strip                                           32dp |  surfaceRaised
+-------------------------------------------------------------+
```

Five text-only tabs fit a fixed `PrimaryTabRow` at 360dp (`PROBES` is the widest at ~42dp), so it
is not scrollable.

### The status strip

Bottom, above the navigation bar. Bottom rather than top because the top already carries the tab
row and a sticky header, and three fixed bars would take 110dp of a 640dp screen.

```
   height      32dp, 8dp vertical internal padding
   fill        surfaceRaised, ruleStrong 1dp on top
   type        monoStrip 11/400, textSecondary
   separator   " · " (U+00B7) between every segment, with no grouping bars
   overflow    horizontal scroll; never truncated, never wrapped
```

The full string as it renders on my ANE-LX2 — this one is a device fact, not a shape:

```
ANE-LX2 · API 28 · EMUI 9.1 · routes 3/3 agree · usage-access OFF · queries: launcher · 24 keys @ 0 · 78% 3.76V 41.0C
```

Colour appears on the **value half of a segment only**, never on the whole segment: `OFF` in
`stateDenied`, `24` in `stateAbsentZero` when non-zero, `3/3` in `statePresent`, and the whole
`routes 2/3` in `stateExplicitZero` when they disagree. Labels stay `textSecondary`.

Segments are tappable and each gets a 48dp-wide target that fits inside the strip's own 32dp
height with its internal padding, so nothing overlaps the last table row. The earlier 24dp strip
with a 40dp target reaching up into the content would have made the bottom row of APPS and AUDIT
untappable, which costs more than the 8dp. Where each segment navigates to is `docs/SCREENS.md`.

---

## 7. Table primitives

### 7.1 Row anatomy

An AUDIT row at scroll offset 0. *Shapes only; every number in this block is invented and none is
a device fact.*

```
 |<-10->|<--------- frozen: key, 170dp --------->|<2>|<---- shifted region ------------------
 +------+----------------------------------------+---+---------------+----------+-----------
 |      |  dsp.audio                             | | | [ PRESENT ]   |     43.0 |     =
 +------+----------------------------------------+---+---------------+----------+-----------
 |      |  cpu.cluster_p…cluster0                | | | [ ZERO ABSENT]|//////////|     =
 +------+----------------------------------------+---+---------------+----------+-----------
        ^ mono 12, left,                             ^ chip 18dp      ^ monoStrong  ^ textSecondary
          MIDDLE ellipsis                              left             12, right     centred
        |<-6->|                          |<-6->|     |<-6->|          |<-6->|

 row height        32dp, content centred, no vertical padding
 row separator     1dp `rule`, full bleed including under the frozen column
 frozen edge       2dp `ruleStrong` vertical, drawn only while the shift offset is > 0
 cell padding      6dp each side -> 12dp between adjacent columns
 vertical rules    none, except the frozen edge above
```

No vertical rules between columns: they double the ink and turn a table into a spreadsheet.
Alignment does that work already.

### 7.2 Row heights

One height per table, uniform within a screen, set directly rather than with `heightIn`. A
variable-height table cannot be scanned, and rows that change height destabilise
`LazyListState` offsets as content arrives.

| screen | row | header |
|---|---:|---:|
| APPS | 28dp | 30dp |
| AUDIT | 32dp | 30dp |
| SELF | 32dp | 30dp |
| PROBES | 44dp | 30dp |
| DEVICE | 28dp | 30dp |
| AUDIT group header | 24dp | — |

APPS rows are single-line cells throughout, including the frozen one. A label stacked over a
package is 16sp + 14sp of line box, which does not fit 28dp at `fontScale 1.0` and would clip on
the screen a reviewer is told to screenshot first; the package goes in the expanded row and on
long-press copy instead.

PROBES is the exception to single-line rows: probe and result on line 1, the verbatim runtime
line on line 2. It is roughly twenty rows rather than three hundred, and the verbatim line is the
evidence.

Height at large font scales is measured, never multiplied by `fontScale` — Android applies
non-linear font scaling above about 1.3 from API 34, so a single multiplier is wrong for at least
one style in any row that mixes sizes:

```kotlin
val lineBox = with(density) { measurer.measure("0", SipperType.mono).size.height.toDp() }
val rowHeight = max(declaredHeight, lineBox + 8.dp)
```

Computed once per screen from the density and the tallest style in the row, so it is still fixed
within a screen and uniform across rows. There is no stacked-row fallback at any font scale: a
second layout that appears only above 1.6 is a layout that never gets tested, and it removes the
side-by-side delta that the APPS screen exists to show.

### 7.3 Header

`surfaceRaised`, `labelMedium` (11/600 text face), `ruleStrong` 1dp underneath, no rule above, no
fill change on scroll. Header cells inherit their column's alignment, so a right-aligned numeric
column has a right-aligned header. A sortable header appends `ArrowDropUp` / `ArrowDropDown` at
14dp inside the cell's own width, so the caret never widens the column and sorting does not shift
the layout. Unsorted columns show no caret.

Header strings are lowercase as written in source (`fg fw`, `fg evt`, `Δ fg`, `rx fg`) and are
never transformed. Units live in the header, so the value cells are bare numbers.

### 7.4 No zebra

There are no alternating row backgrounds, in any table, in either theme. Two reasons:

1. Background is already a state channel here — the hatch, `surfacePinned` for the pinned
   capacity row and for a tapped row. A second, meaningless use of the same channel makes the
   first ambiguous.
2. Zebra encodes nothing. It is a workaround for rows too tall to track, and these are 28–32dp.

Row tracking across a horizontally scrolled table is carried by the frozen first column, which
never leaves the screen, and by tap-to-pin (`surfacePinned` until tapped again).

### 7.5 Numerics

- Every numeric cell is `TextAlign.End`.
- **No thousands separators**, anywhere, including `Fmt.millis` and the `/proc/meminfo` values.
  `409000`, not `409,000`. These get pasted into bug reports and a separator makes them
  un-pasteable. Right alignment in a monospaced face already groups them.
- Durations render `H:MM` in table cells — `312:00`, `0:41`. Exact milliseconds live in the
  expanded row and in clipboard copy.
- The delta column always carries an explicit sign: `+4:12`, `-0:07`, and `+<1m` / `-<1m` for a
  nonzero magnitude under a minute. A true zero shows `0:00` with no sign.
- Null or not-yet-read renders `—` (U+2014) in `textSecondary`. Never `0`, never `N/A`, never a
  blank cell: a blank cell and a zero look the same at a glance.
- Where a unit varies per row — DEVICE thermal zones report in millidegrees, decidegrees and a
  `-40` sentinel — the value is right-aligned in its own slot and the unit is left-aligned in a
  24dp slot after it, so the digits still line up. *Shapes only; the numbers below are invented.*

```
  soc_thermal        58570  m
  cluster0              56  C
  battery            41000  m
  gpu                  -40  sentinel
```

### 7.6 `ScrollTable`

One primitive, shared by all five screens. The widths in §10 are meaningless without these three
rules.

**1. One gesture node, above the header.** `Modifier.scrollable(state, Orientation.Horizontal)`
goes on the `Box` wrapping the header and the `LazyColumn`. Not `horizontalScroll` on every row,
for four reasons, none of them visual:

- `ScrollingLayoutModifier` writes `state.maxValue` during layout, so whichever row measured last
  wins. The 24dp AUDIT group header and the pinned capacity row have different content widths and
  would cap the scroll to the narrowest of them.
- Every row would install its own pointer-input, nested-scroll, fling and overscroll node. At
  twenty visible rows on a Kirin 659 that is twenty gesture pipelines.
- A fling started on a row that then scrolls out of the viewport is driven by a coroutine in the
  disposed node's scope, and stops dead mid-flight.
- The `scrollable` node publishes `HorizontalScrollAxisRange` and `ScrollBy` semantics inside each
  row, which blocks `mergeDescendants` from merging past it — and §12 needs one merged node per
  row.

```kotlin
class TableScrollState(val offsetPx: MutableIntState, val maxPx: MutableIntState) {
    val scrollable = ScrollableState { delta ->
        val old = offsetPx.intValue
        val new = (old - delta).roundToInt().coerceIn(0, maxPx.intValue)
        offsetPx.intValue = new
        (old - new).toFloat()
    }
}
```

Rows and the header are layout-only:

```kotlin
Modifier
    .clipToBounds()
    .layout { m, c ->
        val p = m.measure(c.copy(minWidth = 0, maxWidth = Constraints.Infinity))
        layout(c.maxWidth, p.height) { p.place(-state.offsetPx.intValue, 0) }
    }
```

Reading `offsetPx` inside the placement lambda means a drag triggers placement, not recomposition
and not remeasure. `maxPx` is computed once from the known column widths via `onSizeChanged` on
the container, never from a row's measured size.

**2. No `weight` and no `fillMaxWidth` anywhere inside the shifted region, on any screen.** Inside
that region the incoming `maxWidth` is `Constraints.Infinity`, so a weighted child measures at its
`minWidth` of zero and vanishes. `widthIn(min = …)` does not rescue it — the clamp is against the
`maxWidth` weight already set to zero. It fails silently rather than crashing, which is why it is
written down as a rule rather than left to be noticed. Every column width is intrinsic, including
AUDIT's `effect` (28ch) and PROBES' `detail` (30ch), both end-ellipsised with the full text in
the expanded row.

**3. The frozen column sits outside the shifted region**, in the same parent `Row`. There is no
second scroll state and no synchronisation code. The frozen edge is drawn, not composed:

```kotlin
Modifier.drawWithContent {
    drawContent()
    if (state.offsetPx.intValue > 0) {
        drawRect(colors.ruleStrong, Offset(frozenWidthPx, 0f), Size(2.dp.toPx(), size.height))
    }
}
```

Reading the offset in a row's composable scope instead would subscribe every visible row to it and
recompose all twenty on every frame of a drag.

**Widths come from one list.**

```kotlin
class TableColumn(val id: String, val title: String, val chars: Int, val align: TextAlign)

fun TableColumn.width(advance: Dp) = chars * advance + 12.dp   // 6dp padding each side

@Composable
fun rememberDigitAdvance(): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(density, SipperType.mono) {
        with(density) { (measurer.measure("0".repeat(20), SipperType.mono).size.width / 20f).toDp() }
    }
}
```

Keyed on `Density` and the style, not on `LocalConfiguration` — `Density` already carries
`fontScale` in its `equals`, and reading `LocalConfiguration` would subscribe the composable to
orientation, locale and window-size changes as well. Measured over a 20-glyph run because
`TextLayoutResult.size.width` is a ceiled integer pixel width: on a single `"0"` the error is up
to 1px, which over a 22-character column is about 2% at density 3.0 and makes the real column
wider than every dp figure in §10. Over twenty it is under 0.05px per character.

**The sort/filter projection is not `derivedStateOf`.** It is
`remember(rows, query) { projectApps(rows, query) }` in composition, or
`combine(rowsFlow, queryFlow) { r, q -> projectApps(r, q) }.stateIn(...)` in the ViewModel.
Scrolling does not re-sort because the projection never reads scroll state, which is true with or
without a derived-state node; `derivedStateOf` damps a high-frequency input into a low-frequency
one and there is no high-frequency input here. `projectApps` is a pure function tested on the JVM.

A `:app` JVM test sums the committed column list and asserts the §10 totals, and
`./gradlew :app:columnTableDoc` emits those two markdown tables, so the document cannot drift
from the code.

If the Compose BOM I pin at E0 turns out to ship `LazyTable` with pinned columns, it replaces this
section and I delete it.

---

## 8. Chips

Chips are drawn, not `AssistChip`. Justification, since component defaults are otherwise left
alone: a Material chip is an interactive component sized for touch — 32dp tall, 8dp corner, an
icon slot. Ours is a non-interactive label inside a 32dp row. It is a different component, so
`AssistChip` would be the misuse rather than the deviation. Where a chip *is* interactive — the
filter row above the AUDIT and APPS tables — it is a Material `FilterChip` at its defaults.

*Shapes only; the height and padding below are real, the label is a shape.*

```
  +----------------+
  |  ZERO ABSENT   |   height 18dp
  +----------------+   corner 3dp
                       border 1dp   state @ 45% alpha
                       fill         state @ 12% (light) / 18% (dark) over the row background
                       label        monoChip 10/500, state at full alpha
                       padding      5dp horizontal, no vertical
```

Rules:

- No chip carries an icon.
- No chip is ever a solid fill with reversed text.
- At most one chip per cell. The exception is the expanded APPS row, where each unpriced power
  component gets its own inline reason chip.

**Chips never truncate, at every font scale.** The chip column is derived the way every other
column is, from the longest label in the enum, which is known at compile time:
`chars × chipAdvance + 10dp` chip padding `+ 12dp` cell padding. `monoChip` is 10sp against
`mono`'s 12sp, so its advance is 6.0dp at `fontScale 1.0`. The widest verdict label is
`ZERO EXPLICIT` at 13 characters: 78 + 10 + 12 = **100dp**, which is the AUDIT verdict column.
Because the advance is measured, that holds at 2.0 as well; the column widens and the table
scrolls further.

| enum | chip label | chars | full token appears in |
|---|---|---:|---|
| `PRESENT` | `PRESENT` | 7 | — |
| `ZERO_BY_EXPLICIT_VALUE` | `ZERO EXPLICIT` | 13 | legend, expanded row, CSV export |
| `ZERO_BY_ABSENCE` | `ZERO ABSENT` | 11 | legend, expanded row, CSV export |
| `BACK_FILLED` | `BACK-FILLED` | 11 | — |

The four-verdict legend at the top of AUDIT spells all four out in full, once.

### Chip taxonomy

| kind | colour | example labels | where |
|---|---|---|---|
| verdict | the four verdict tokens | `PRESENT`, `ZERO ABSENT` | AUDIT verdict column |
| cause | `textSecondary` on `hatchGround` | `key absent from profile`, `appop default mode` | beside every hatched cell |
| provenance | `stateBackFilled` | `back-filled from screen.on` | beside a `BACK_FILL`-route value |
| denied | `stateDenied` | `denied: PACKAGE_USAGE_STATS` | PROBES, any cell backed by `Reading.Denied` |
| absent | `textSecondary` | `min 34`, `min 36` | any cell backed by `Reading.Absent` |
| stale | `stateStale` | `stale 3h`, `stale 11d` | APPS and SELF, when the sample is older than its window |
| boundary | `textSecondary` | `no launcher activity`, `outside queries` | APPS label column, PROBES |
| route | `textSecondary` | `res/xml`, `pkg:android`, `reflect` | AUDIT provenance block |
| level | `textSecondary` | `level A`, `level B`, `level C` | the route-and-observe rows |

The cause chip is deliberately not coloured. It sits next to a hatch, and the hatch's argument is
that nothing in that cell should look like a value.

`stale` additionally draws a 1dp dotted underline under the value it modifies, because a stale
`PRESENT` is still `PRESENT` — staleness modifies a verdict rather than replacing it.

**Back-fill gets a chip, not a hatch.** On API 34+ `initDisplays` and `initModem` synthesise
new-schema keys from deprecated ones and log a deprecation warning naming the replacement. That
is a real number with a known provenance, so it renders as a number in `textNumeric` with a
`back-filled from screen.on` chip in `stateBackFilled`. A hatched cell containing `0.1 ←
screen.on` would make the hatch mean both "no value" and "real value, synthesised key".

---

## 9. The `SilentDefault` cell

The rendering the rest of the architecture exists to guarantee.

*Shapes only; the geometry is real, the label is a shape.*

```
  +---------------------+  +---------------------------+
  | ///////////////     |  |  key absent from profile  |
  +---------------------+  +---------------------------+

     ground   hatchGround
     stripes  1dp stroke, 45 degrees, 5dp pitch measured along x
     colour   hatchLine — no state hue
     content  nothing. No number, no dash, no `0.0`, no `?`, no glyph.
     chip     the cause chip, immediately right, textSecondary on hatchGround
```

```kotlin
// Stepped along x by a fixed pitch and drawn as full-height diagonals, so a 70dp cell and a
// 124dp cell show the same stripe spacing rather than the same stripe count.
private fun DrawScope.hatch(line: Color) {
    val pitch = 5.dp.toPx()
    val stroke = 1.dp.toPx()
    val h = size.height
    var x = -h
    while (x < size.width) {
        drawLine(line, Offset(x, h), Offset(x + h, 0f), strokeWidth = stroke)
        x += pitch
    }
}
```

Applied as `Modifier.clipToBounds().drawBehind { hatch(colors.hatchLine) }`. `DrawScope` does not
clip on its own and `graphicsLayer`'s `clip` defaults to false, so without it every stripe bleeds
up to one row-height into the neighbouring column and into the rows above and below. The two
`toPx()` calls are hoisted out of the loop; with the pitch hoisted the draw allocates nothing —
`Offset` and `Color` are value classes over primitives and `drawLine` reuses a cached `Paint`.

### The render contract

`ReadingCell` is the one composable that takes a `Reading` — the type itself is defined in
`:audit` and specified in `docs/CONTRACT.md` §2. `format` is reachable from the `Value` branch and
nowhere else, over a `when` that is exhaustive with no `else`:

```kotlin
when (reading) {
    is Reading.Value -> ValueText(format(reading.value), reading.route, column, speak)
    is Reading.SilentDefault -> HatchCell(reading.cause, column)   // format is not in scope
    is Reading.Denied -> ChipCell(Chip.Denied(reading.permission, reading.howToGrant), column)
    is Reading.Absent -> ChipCell(Chip.Absent(reading.minApi), column)
}
```

The manufactured value is behind `@OptIn(AuditOnly::class)`, so putting it on screen takes a
different composable *and* an explicit opt-in, which is two reviewable acts. A fifth constructor
breaks the build at every render site.

The manufactured value appears in exactly two places in the app:

- the expanded detail row, prefixed `platform returned:`, left-aligned in `textSecondary`;
- the PROBES paired demonstrations, where both halves render the platform's return identically at
  the same size and colour, so the quote cannot be read as sipper's own number.

`:app` carries one file-level `@OptIn(AuditOnly::class)`, in
`ui/probes/SilentDefaultDemo.kt`, where the manufactured `0.0` beside its cause is the demo.

**A UI test pins it**, in `app/src/androidTest`: given a `SilentDefault` carrying `0.0`,

```kotlin
onNodeWithText("0.0", useUnmergedTree = true).assertDoesNotExist()
onNodeWithContentDescription("manufactured", substring = true).assertExists()
```

`useUnmergedTree` because the default merged search would pass while the number still sat in an
unmerged child that the merge exposes.

---

## 10. Column tables

Widths are `chars × advance + 12dp`, where `advance` is the measured width of `"0"` in
`SipperType.mono`. The dp figures are at `fontScale 1.0`, where the advance is 7.2dp. Totals add
the 10dp row lead, the 2dp frozen edge and the 10dp trailing padding from §3.

### AUDIT — 32dp rows, 700dp total

| pos | column | chars | dp | align | content |
|---|---|---:|---:|---|---|
| frozen | `key` | 22 | 170 | left | profile key, **middle**-ellipsised — the discriminator is the suffix (`screen.on.display0`); long-press copies the full key |
| 1 | `verdict` | — | 100 | left | verdict chip, `monoChip` |
| 2 | `value` | 8 | 70 | right | the literal token from the XML, not a formatted `Double`; `—` when absent |
| 3 | `=` | 2 | 26 | centre | `=` in `textSecondary` when the routes agree, `≠` in `stateAbsentZero` when they do not |
| 4 | `from` | 12 | 98 | left | `res`, `back-fill`, or `—`; never `reflect` |
| 5 | `effect` | 28 | 214 | left | `priced`, `cluster power unpriced`, `no pwi row on this device` |

Frozen 170 + scrolling 508 + 10 + 2 + 10 = **700dp**. At offset 0 on 360dp:
10 + 170 + 2 + 100 + 70 = **352dp**, so key, verdict and value are on screen before any scroll.

The three read routes are not three numeric columns. Their values live in the provenance block
above the table, where the routes are named; the table stays about keys and carries one glyph for
agreement. Repeating a number three times across a row is ink with no information, and in a column
of `=` a number is the loudest thing on the screen.

Rows group under a 24dp sticky group header per `PowerCalculator` on `surfaceRaised`,
`labelMedium`, with `ruleStrong` above it, carrying the calculator name and a count
(`CpuPowerCalculator · 11 keys · 6 at 0`). Group headers and the pinned capacity row sit outside
the shifted region as full-bleed rows, so they cannot contribute to the scroll extent.

The capacity row is pinned above the first group on `surfacePinned` with a 2dp `accent` left
edge, showing all four sources side by side and computing no ratio between them.

### APPS — 28dp rows, 1059dp total

One row per visible package, single-line cells throughout.

| pos | column | chars | dp | align | source |
|---|---|---:|---:|---|---|
| frozen | `package` | 18 | 142 | left | middle-ellipsised; long-press copies the full string |
| 1 | `fg fw` | 6 | 55 | right | `UsageStats.getTotalTimeInForeground`, `INTERVAL_DAILY` |
| 2 | `fg evt` | 6 | 55 | right | `:usage` reconstruction, clipped to the window |
| 3 | `Δ fg` | 7 | 62 | right | col1 − col2, always signed, `monoStrong` |
| 4 | `label` | 18 | 142 | left | text face; `not visible` in `textSecondary` when unresolvable |
| 5 | `visible` | 6 | 55 | right | |
| 6 | `fgs` | 6 | 55 | right | |
| 7 | `tail` | 5 | 48 | left | `open` when the reconstruction ended mid-session |
| 8 | `rx fg` | 7 | 62 | right | `NetworkStats`, `STATE_FOREGROUND` |
| 9 | `tx fg` | 7 | 62 | right | |
| 10 | `rx bg` | 7 | 62 | right | `STATE_DEFAULT`, i.e. not-foreground |
| 11 | `tx bg` | 7 | 62 | right | |
| 12 | `bucket` | 14 | 113 | left | raw int then name: `45 restricted`, `5`, `unknown` |
| 13 | `mAh` | 7 | 62 | right | modelled total |

Frozen 142 + scrolling 895 + 10 + 2 + 10 = **1059dp**. At offset 0 on 360dp:
10 + 142 + 2 + 55 + 55 + 62 = **326dp**, so both foreground numbers and the signed delta are on
screen with 34dp of `label` showing — which both proves the table scrolls and puts the
unclipped-bucket comparison in the first screenshot. Every width above is chosen to make that
arithmetic land.

**There is no stacked mAh bar.** Column 13 keeps the number. A bar normalised over the visible or
the filtered set is a length comparison whose meaning changes when the reader types in a filter
box, and the finding it was drawn for — a component priced from an absent or explicitly-zero key
— is already carried by the AUDIT verdict table and by the expanded row's segment table with its
verbatim reason lines (`wifi 0 · wifi.controller.rx = 0`). That is the whole finding, in text,
sortable, and copy-pasteable into a bug report. If a visual earns its place later it is an evening
after the build stops, not before.

### PROBES, SELF, DEVICE

PROBES rows are 44dp: line 1 is the probe name in `mono` 12 with the result chip right-aligned;
line 2 is the verbatim runtime line in `monoBlock` 11 `textSecondary`, single line, end-ellipsised,
tapping to expand into a `surfaceSunken` block with `SelectionContainer` and no added quotes. The
`detail` column is 30ch (228dp), intrinsic like every other. Its API column header is `since`, not
`api`, and renders two values where the method's introduction and the behaviour's introduction
differ: `23 · filtered 31+`.

The paired demonstrations are adjacent rows with a 2dp `ruleStrong` bracket on the left spanning
both — the grouping device on that screen.

SELF is three stacked tables sharing `ScrollTable` with different column lists. DEVICE uses the
value-plus-24dp-unit-slot from §7.5. Neither invents a column that is not in the same
`TableColumn` list the test in §7.6 sums.

---

## 11. Loading and failure, as drawn

There is no `CircularProgressIndicator` and no indeterminate `LinearProgressIndicator` anywhere
that sipper raises on its own. Not a preference: a spinner says "wait" and nothing else, and every
read here has a known denominator.

**One threshold, 120ms.** Below it, nothing changes. Beyond it the table renders its real header
and layout-true skeleton rows — each cell a 1dp `rule`-coloured underline sitting on the cell's
text baseline at the cell's own character width, so the shape of the answer arrives before the
answer — and the status strip carries a determinate count:

```
reading 12/47
```

A stall is then diagnosable from the screen: `reading 12/47` sitting still for ten seconds names
the key that hung.

The one indeterminate indicator in the app is `PullToRefreshBox`'s own, on the two screens that
have it (`docs/SCREENS.md`). I leave it at its Material default rather than restyling it. It is
attached to a gesture the user started and it disappears when they let go, which is a different
thing from the app deciding to show a spinner at an unknown moment for an unknown duration.

**Failure.** A per-cell failure never leaves the cell: `Reading.Denied` and `Reading.Absent`
render as chips in place, with no snackbar, dialog or toast. A read failing is normal here.

A screen-level failure — a SQLDelight migration, a corrupt sample — renders a full-width block:
the exception class name in `mono` 12, the message verbatim in `monoBlock` 11 on `surfaceSunken`,
and a `Retry` `TextButton`. *Shapes only; the exception below is invented.*

```
  IllegalStateException
  Migration 3 -> 4 failed: table app_day has no column named unclosed_tail
                                                                        [ Retry ]
```

If I cannot name what failed, the screen shows the exception.

---

## 12. Accessibility, touch, font scale

**Contrast** is §4.4, produced by a test, AA in both themes with three named non-text exceptions.

**Touch targets.** A 28–32dp row is below Material's 48dp guidance and I am not pretending
otherwise. A row is not a small control: the target is the full row width, 360×28dp, which clears
WCAG 2.5.8 at 24×24 and knowingly fails Material's guidance. That is stated, not worked around.
The genuinely standalone controls — the sort caret, the PROBES re-run button, the status-strip
segments — each get 48dp via `minimumInteractiveComponentSize()`, which is what that modifier is
for; inside a fixed-height row it would either win and break the row grid or be overridden and
buy nothing.

**Font scale.** Column widths derive from a measured advance (§7.6) and row heights from a
measured line box (§7.2), so raising the system font scale widens columns and grows rows rather
than clipping them, and the table scrolls further. Tested at 1.0, 1.3 and 2.0, and on API 34 and
36 emulators at 1.3 and 2.0, which is where the non-linear curve first shows.

The APPS offset-0 arithmetic in §10 holds at the default scale on a 360dp-wide viewport. The
character part of those columns needs more than 360dp somewhere around 1.13, so at Android's Large
setting and above the delta column is behind a short scroll. That is the intended behaviour, not a
regression, and §14's check names its precondition.

**TalkBack.** Each row is one merged semantics node with an ordered description built from the
column titles, so the reading order is the column order rather than the layout order:
`com.whatsapp, framework 41200 ms, reconstructed 40788 ms, delta minus 412 ms`. Every table list
declares `CollectionInfo(rowCount = rows.size, columnCount = 1)`, because what TalkBack navigates
is a list of merged rows; declaring thirteen columns against merged rows makes it announce a
column index for a node that is the whole row. Cell-by-cell reading lives in the expanded row's
linear `label: value` list, which a screen reader handles natively.

A hatched cell announces `manufactured value, cause key absent from profile` and never the number
it is hiding. The rule holds in the accessibility tree, not only on screen.

**Direction.** Tables are LTR-locked, and so is the status strip, which is machine data end to
end and whose ` · ` separators and `3/3` and `78% 3.76V` runs are all bidi-neutral or weak. Every
cell sets `TextDirection.Ltr` explicitly rather than relying on the layout direction, since
package names, keys, hex ids and exception strings are LTR data — **except the `label` cell**,
which sets `TextDirection.Content` so a native-script app name renders correctly inside its
LTR-positioned cell. The table is LTR-locked; that one cell's contents are not. Prose outside the
tables honours the system direction.

---

## 13. Files

```
app/src/main/kotlin/dev/tahmid/sipper/ui/theme/
  Colors.kt          SipperColors, lightColors, darkColors, LocalSipperColors
  Type.kt            Typography overrides + SipperType
  Theme.kt           SipperTheme — isSystemInDarkTheme, no dynamic colour, no toggle
app/src/main/kotlin/dev/tahmid/sipper/ui/table/
  TableColumn.kt     TableColumn, rememberDigitAdvance, width()
  ScrollTable.kt     TableScrollState and the primitive
  ReadingCell.kt     the exhaustive when, ValueText, HatchCell, ChipCell
  Hatch.kt           the DrawScope extension
  Chip.kt            Chip sealed type + the drawn chip
app/src/main/kotlin/dev/tahmid/sipper/ui/StatusStrip.kt
app/src/main/kotlin/dev/tahmid/sipper/ui/probes/SilentDefaultDemo.kt
app/src/main/res/font/roboto_mono_regular.ttf, roboto_mono_medium.ttf
app/src/test/kotlin/dev/tahmid/sipper/
  ContrastTest.kt    recomputes §4.4, fails below 4.5:1
  ColumnWidthTest.kt sums the committed column lists, asserts the §10 totals
  GlyphSubsetTest.kt walks ui string constants against the §1 subset
```

No barrel file, no `ui/components/index`. Chips live in `Chip.kt` because that is where a chip is.

---

## 14. Banned, and why

Tied to the reference tools, because "looks AI-generated" is not a checkable rule and these are.

| banned | why |
|---|---|
| hero section; centred headline + subtitle + button | Bull Board opens on a table. So does this. No page here introduces itself. |
| gradients, glassmorphism | zero decorative fills; every fill is a `surface*` token or a state at a fixed alpha |
| a multi-hue palette for its own sake | hues are the verdict encoding; a hue that means nothing devalues the ones that do |
| uniform rounded-2xl shadow card grid | no `Card`, no elevation, radius 0 except 3dp chips |
| a row of four icon stat cards | the status strip carries the same facts in 32dp instead of 180dp |
| a dark-mode toggle | dark follows the system; a toggle nobody asked for is a tell |
| dynamic colour / Material You | the wallpaper would pick the state hues |
| decorative icons on labels | four icons total, all functional (§5) |
| emoji, anywhere, including commit messages | — |
| a spinner sipper raises on its own | every read has a denominator; a determinate count is more information. The single exception is `PullToRefreshBox`'s own indicator, left at its Material default, attached to a gesture the user started. |
| shimmer skeletons | the skeleton is a static rule at the cell's real width |
| animated number transitions | a changed value should be findable, and motion makes it less so |
| toasts and snackbars for read failures | a failed read belongs in its cell |
| zebra striping | §7.4 |
| thousands separators | §7.5 |
| `weight` or `fillMaxWidth` inside the shifted region | §7.6 rule 2 — it fails silently |
| restyled Material components | `TextButton`, `FilterChip`, `PrimaryTabRow`, `HorizontalDivider`, `PullToRefreshBox` and the ripple stay at their defaults; only `Typography` and `ColorScheme` are overridden |
| pill-shaped chips | 3dp; a pill reads as a marketing tag |
| centred empty states with an illustration | empty states are left-aligned at the top of the content area, no icon |
| the words `Oops!`, `Something went wrong`, `No data available`, `Nothing here yet`, `Loading...`, `Coming soon`, `with ease`, `seamless` | §11 |

---

## 15. What a reviewer can check without reading the code

- Screenshot APPS at scroll 0, at the default font scale on a 360dp-wide viewport. Both
  foreground numbers and the signed delta are on screen. If they are not, a width in §10 is wrong.
- Search the APK's string table for a `0.0` rendered next to a hatch. No path produces one; the
  `when` in §9 is exhaustive and `androidTest` pins it against the unmerged tree.
- Turn the system font scale to 2.0. Columns get wider, rows get taller, the table scrolls
  further. Nothing clips. The APPS delta moves behind a short scroll, which §12 says it will.
- Turn on dark mode from the system. There is no switch in the app to find.
- Count the icons. Four.
- Run `./gradlew :app:contrastDoc :app:columnTableDoc` and diff the output against §4.4 and §10.
  If they differ, this document is stale and the code is right.

---

*Written 2026-09-08, before E0. Anything I change during E2 or E5 gets changed here first, in the
same commit as the code.*
