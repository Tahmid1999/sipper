# sipper — shared contract

This file is the tie-breaker. Where any other document in this repository disagrees with it, that
document is the bug and gets fixed, not this one. Every number here was decided once; none of it
is a range.

## 0. Document set, ownership, precedence

Six documents. The screen specifications stay split by screen rather than merged into one file:
the contradictions they produced came from having no shared contract, which this file now is, and
folding 80,000 characters of screen specification into a single document to fix a filename would
cost detail for nothing.

| file | owns | may not restate |
| --- | --- | --- |
| `README.md` | the claim, prior art, limitations, build | tokens, widths, type signatures |
| `docs/CONTRACT.md` | tokens, types, widths, API levels, every cross-document tie-break | — |
| `docs/ARCHITECTURE.md` | modules, `Reading<T>`, the key table, schema, CI gates, scheduling | colour, widths, screen behaviour |
| `docs/DESIGN.md` | colour, type, spacing, row heights, the table primitive, column tables, chips, the hatch | screen behaviour, state machines |
| `docs/FLOWS.md` | first run, the usage-access ask, screen states, navigation, saved state, refresh and staleness, the sampler, accessibility, the status strip | colour, widths, per-screen layout |
| `docs/SCREENS-AUDIT-PROBES.md` | AUDIT and PROBES: layout order, columns, copy strings | colour, widths, state machines |
| `docs/SCREENS-APPS-SELF-DEVICE.md` | APPS, SELF and DEVICE: layout order, columns, copy strings | colour, widths, state machines |

Every document carries this line under its title:

> `docs/CONTRACT.md` fixes tokens, widths, types and the API levels below. This file owns
> <topic>. Where they disagree, the contract wins.

No document restates a colour hex, a column width, or a Kotlin signature that another document
owns. It points at the owning section.

---

## 1. Colour — one palette, one vocabulary

Token names below are the only ones that exist. `bg`/`fg`/`fgDim`, `ink`/`inkDim`,
`ok`/`warn`/`bad`/`info`, `page`/`text`/`muted`, `stateOk`/`stateWarn`/`stateBad` are all deleted;
rename every reference.

No dynamic colour, no in-app theme toggle — `isSystemInDarkTheme()` and nothing else, because the
wallpaper would otherwise pick the hues that carry the verdicts.

### 1.1 Surfaces, rules, text

| token | light | dark | used for |
| --- | --- | --- | --- |
| `surface` | `#FFFFFF` | `#0F1216` | table body, screen ground |
| `surfaceRaised` | `#F3F4F6` | `#171B21` | sticky header, status strip, section blocks |
| `surfaceSunken` | `#E9EBEF` | `#0A0C0F` | hatch ground, verbatim output blocks |
| `surfacePinned` | `#EDF1F7` | `#1B222B` | the pinned capacity row, a tapped row |
| `rule` | `#DDE1E6` | `#262B33` | 1dp separator under every table row |
| `ruleStrong` | `#8A9199` | `#5C6774` | header underline, frozen-column edge, group separators |
| `textPrimary` | `#1A1F26` | `#E3E7EC` | prose |
| `textSecondary` | `#5B646F` | `#98A1AC` | column headers, units, the `=` glyph, the `—` absence glyph, `Absent` chips, route chips |
| `textNumeric` | `#0E1216` | `#F4F7FA` | every mono machine value |
| `accent` | `#0B5FCC` | `#6FA8F5` | text buttons and links; never a fill |

`textNumeric` is darker than `textPrimary` in light and brighter in dark: the machine value is the
content and the prose around it is chrome.

### 1.2 State — four verdicts, four hues, all distinct

| token | meaning | light | dark |
| --- | --- | --- | --- |
| `statePresent` | verdict `PRESENT` | `#14713A` | `#5BC27E` |
| `stateExplicitZero` | verdict `ZERO-BY-EXPLICIT-VALUE` | `#8A5200` | `#DFA43C` |
| `stateAbsentZero` | verdict `ZERO-BY-ABSENCE` | `#A62015` | `#F0796B` |
| `stateBackFilled` | verdict `BACK-FILLED` | `#3B36A6` | `#A6A2F2` |
| `stateDenied` | `Reading.Denied` | `#0F5468` | `#63C3E0` |
| `stateStale` | sample older than its window | `#6B5F4A` | `#B4A78E` |

Green, amber, red, indigo, teal, warm grey — six hues, no two shared. The single-hue `warn`
serving `ZERO-BY-ABSENCE`, `BACK-FILLED` and `SilentDefault` at once is deleted: those are the
three states the app exists to keep apart.

`Reading.Absent` has no token of its own; it renders in `textSecondary`. Absence of an API is
chrome, not alarm, and a seventh grey that is one step from `textSecondary` is a distinction the
eye cannot make.

**`Reading.SilentDefault` has no hue at all.** It is a hatch on `hatchGround` with a cause chip in
`textSecondary`. Not adding a colour is the decision: a coloured cell reads as a value, and the
one thing that cell must not read as is a value. It is therefore distinct from all four verdicts
by texture as well as by the absence of a number.

### 1.3 Hatch

| token | light | dark |
| --- | --- | --- |
| `hatchGround` | `#E9EBEF` (= `surfaceSunken`) | `#0A0C0F` |
| `hatchLine` | `textPrimary` @ 22% → `#BBBEC3` | `textPrimary` @ 26% → `#424548` |

### 1.4 Contrast, computed

Computed with the WCAG 2.x relative-luminance formula, sRGB, every text token against all four
backgrounds it can land on plus its own chip composite (token at 12% over the background in light,
18% in dark). These are outputs, not assertions; the build regenerates them (§9).

**Light**

| token | surface | raised | sunken | pinned | chip/surface | chip/raised |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
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

Minimum: **4.55**.

**Dark**

| token | surface | raised | sunken | pinned | chip/surface | chip/raised |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
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

Minimum: **4.77**.

Every text pairing clears AA at 4.5:1 in both themes. Nothing relies on the 3:1 large-text
allowance, because there is no large text.

**Three non-text values, stated rather than hidden.** `rule` is 1.31 (light) / 1.32 (dark);
`ruleStrong` is 3.19 against `surface` and 2.90 against `surfaceRaised` in light, 3.26 and 3.00 in
dark, so the header underline sits with one side just under 3:1; `hatchLine` is 1.56 / 2.03 over
its ground. All three are reinforcement — the frozen column, the column alignment, the chip text
and the verdict word each carry the same information at full contrast — so 1.4.11 does not bite.

---

## 2. `Reading<T>` — one definition, one enforcement

Defined in `:audit`, in `Reading.kt`. This block is pasted into no other document; every other
document names constructors and fields and links here.

```kotlin
sealed interface Reading<out T> {

    /** The platform answered, and the answer came from the route named. */
    data class Value<out T>(val value: T, val route: Route) : Reading<T>

    /**
     * The platform answered, the answer is not an answer, and `shown` is what a caller that did
     * not know would have printed. Not a data class: `copy()` and `component1()` would hand out
     * `shown` without the opt-in.
     */
    class SilentDefault<out T>(
        @property:AuditOnly val shown: T,
        val cause: DefaultCause,
        val route: Route,
    ) : Reading<T> {
        override fun toString(): String = "SilentDefault(cause=$cause, route=$route)"
        override fun equals(other: Any?): Boolean =
            other is SilentDefault<*> &&
                @OptIn(AuditOnly::class) (shown == other.shown) &&
                cause == other.cause && route == other.route
        override fun hashCode(): Int =
            31 * (31 * @OptIn(AuditOnly::class) shown.hashCode() + cause.hashCode()) + route.hashCode()
    }

    /** A permission or appop is missing, and `howToGrant` says whether that is fixable. */
    data class Denied(val permission: String, val howToGrant: Grant) : Reading<Nothing>

    /** The API does not exist below `minApi`; this device is below it. */
    data class Absent(val minApi: Int) : Reading<Nothing>
}

@RequiresOptIn(level = RequiresOptIn.Level.ERROR, message = "Reads a manufactured value.")
@Retention(AnnotationRetention.BINARY)
annotation class AuditOnly

data class Route(val kind: RouteKind, val detail: String)

enum class RouteKind {
    SYSTEM_RESOURCES, ANDROID_PACKAGE_RESOURCES, POWER_PROFILE_REFLECTION, BACK_FILL,
    BATTERY_BROADCAST, BATTERY_PROPERTY, PROC_FILE,
    USAGE_EVENTS, USAGE_INTERVALS, NETWORK_STATS, APP_OPS, PACKAGE_MANAGER,
}

enum class DefaultCause {
    KEY_ABSENT_FROM_PROFILE,     // getAveragePower fell through to `return defaultValue`
    NO_USAGE_ACCESS,             // isAppInactive without the appop, API 30+
    PACKAGE_NOT_VISIBLE,         // isIgnoringBatteryOptimizations outside <queries>, API 31+
    APPOP_DEFAULT_MODE,          // AppOpsService returned opToDefaultMode(code)
    UNCLIPPED_INTERVAL_BUCKET,   // queryUsageStats summed buckets wider than the window
}

sealed interface Grant {
    data class SettingsToggle(val action: String, val uri: String?, val appOp: String) : Grant
    data class ManifestQueries(val entry: String) : Grant
    data object Unreachable : Grant
}
```

Fixed: the field is **`shown`**. `v` and `manufactured` are deleted everywhere. `Denied`'s first
field is **`permission`** (not `what`). `DefaultCause` is SCREAMING_CASE and has **five** members —
`BACK_FILLED_FROM_DEPRECATED` is gone (§3), and `KEY_NEVER_DEFINED` / `NOT_IN_QUERIES` were never
members of anything and are deleted. `Reading` has no `getOrNull`, no `orElse`, no `map`, no
`fold`.

### 2.1 The one enforcement mechanism

**The render contract:** `shown` is unreadable without `@OptIn(AuditOnly::class)`, and `ReadingCell`
is the only composable that takes a `Reading`, with `format` reachable from the `Value` branch and
nowhere else, over a `when` that is exhaustive with no `else`:

```kotlin
@Composable
fun <T> ReadingCell(
    reading: Reading<T>,
    column: TableColumn,
    format: (T) -> String,
    speak: (T) -> String = format,
    modifier: Modifier = Modifier,
) {
    when (reading) {
        is Reading.Value -> ValueText(format(reading.value), reading.route, column, speak)
        is Reading.SilentDefault -> HatchCell(reading.cause, column)   // format is not in scope here
        is Reading.Denied -> ChipCell(Chip.Denied(reading.permission, reading.howToGrant), column)
        is Reading.Absent -> ChipCell(Chip.Absent(reading.minApi), column)
    }
}
```

Getting a number onto the screen from a manufactured answer requires writing a different
composable *and* an explicit `@OptIn`, which is two reviewable acts. A fifth constructor breaks
the build at every render site. `:app` carries exactly one file-level `@OptIn(AuditOnly::class)`:
`app/src/main/kotlin/.../ui/probes/SilentDefaultDemo.kt`, where the manufactured `0.0` beside its
cause is the demo.

**Dropped, with reasons.** The `rg -l 'SilentDefault' … | grep -v` CI gate is dropped: as written
it exits 1 in both directions so it can never pass, it is defeated by a file rename, and it
rejects `SilentDefaultDemo.kt` by filename. The `fold(onValue, onSilent, onDenied, onAbsent)`-only
API is dropped: it costs every call site a four-lambda allocation to buy what the exhaustive `when`
already gives at compile time.

Backing this, gate 4 (`apiSurfaceCheck`) asserts against the committed `audit/api/audit.api` that
`Reading` gains no accessor and that `SilentDefault` has no `componentN` or `copy`. That is the
existing gate defending an absence, not a second enforcement mechanism.

---

## 3. Back-fill is a `Value`

On API 34+, `PowerProfile.initDisplays` and `initModem` synthesise new-schema keys from deprecated
ones and log `Slog.w(TAG, POWER_SCREEN_FULL + " is deprecated! Use " + key + " instead.")`. The
platform says so, and "the platform said nothing" is the entire definition of `SilentDefault`.

Fixed:

- A back-filled key is `Reading.Value(v, Route(RouteKind.BACK_FILL, fromKey))`.
- It renders as a **real number** in `textNumeric` with a `back-filled from screen.on` provenance
  chip in `stateBackFilled`. **It is never hatched.** A hatched cell containing `0.1 ← screen.on`
  is deleted; the hatch means "no value" and cannot also mean "real value, synthesised key".
- `Verdict.BACK_FILLED` is derived from `Value` whose route kind is `BACK_FILL`.
- `DefaultCause.BACK_FILLED_FROM_DEPRECATED` is deleted.
- There is no fifth `Reading` constructor named `BackFilled`; PROBES pair 3's right half stores
  `Value · route BACK_FILL(screen.on)`.

Verdict derivation, complete:

| reading for a profile key | verdict |
| --- | --- |
| `Value(v, res route)`, `v != 0.0` | `PRESENT` |
| `Value(0.0, res route)` — the file says zero | `ZERO_BY_EXPLICIT_VALUE` |
| `SilentDefault(0.0, KEY_ABSENT_FROM_PROFILE, …)` | `ZERO_BY_ABSENCE` |
| `Value(v, Route(BACK_FILL, from))` | `BACK_FILLED` |

---

## 4. AUDIT — one information architecture

Six columns, one frozen. Route values live in the provenance block above the table, not as three
numeric columns in it: the disagreement story is told once, where the routes are named, and the
table stays about keys.

Screen order, top to bottom: status strip (bottom-fixed, §9), tab row, provenance block, pinned
capacity row, counts line, sticky table header, grouped key rows, footer digest line.

| pos | column | chars | dp | align | content |
| --- | --- | ---: | ---: | --- | --- |
| frozen | `key` | 22 | 170 | left | profile key, **middle**-ellipsised — the discriminator is the suffix (`screen.on.display0`) |
| 1 | `verdict` | — | 100 | left | verdict chip, `monoChip` |
| 2 | `value` | 8 | 70 | right | the **literal token from the XML**, not a formatted `Double`; `—` when absent |
| 3 | `=` | 2 | 26 | centre | `=` in `textSecondary` when the routes agree, `≠` in `stateAbsentZero` when they do not |
| 4 | `from` | 12 | 98 | left | `res`, `back-fill`, or `—`; never `reflect` |
| 5 | `effect` | 28 | 214 | left | `priced`, `cluster power unpriced`, `no pwi row on this device` |

Frozen 170 + scrolling 508 + 10dp lead + 2dp frozen edge + 10dp trailing = **700dp total**.
At scroll offset 0 on 360dp: 10 + 170 + 2 + 100 + 70 = **352dp**, so key, verdict and value are on
screen before any scroll.

`effect` is an intrinsic 28-character column, not `weight(1f)`: inside `horizontalScroll` the
incoming `maxWidth` is `Constraints.Infinity`, so a weighted child measures at zero and vanishes.
No `weight` and no `fillMaxWidth` anywhere inside a horizontally shifted region, on any screen.

Rows group under a 24dp sticky group header per `PowerCalculator`
(`CpuPowerCalculator · 11 keys · 6 at 0`). The capacity row is pinned above the first group on
`surfacePinned` with a 2dp `accent` left edge, showing all four sources and computing no ratio
between them.

**The primary-route caveat, on the screen.** Route B (`getResourcesForApplication("android")`)
fills the `value` column and carries the verdicts. On any device where the routes disagree, a
persistent line sits under the capacity row:

> `verdicts below are computed from framework-res.apk. dumpsys pws reports 3000 on this device, so`
> `the framework is pricing from /product/etc/xml and these verdicts describe a file it does not read.`

That puts instance 4 on the screen instead of only in the README, and it is the first question a
reviewer asks.

---

## 5. APPS — one column set, one total

One row per visible package. 28dp rows, single-line cells throughout — the stacked
label-over-package frozen cell is deleted, because 16sp + 14sp of line box does not fit a 28dp row
at `fontScale 1.0`.

Durations render `H:MM` in table cells (`312:00`, `0:41`), signed in the delta with `+<1m` / `-<1m`
for a nonzero magnitude under a minute. Exact milliseconds live in the expanded row and in
clipboard copy. No thousands separators anywhere, including `Fmt.millis` and `/proc/meminfo`, so
every value pastes into a bug report unedited.

| pos | column | chars | dp | align | source |
| --- | --- | ---: | ---: | --- | --- |
| frozen | `package` | 18 | 142 | left | middle-ellipsised; long-press copies the full string |
| 1 | `fg fw` | 6 | 55 | right | `UsageStats.getTotalTimeInForeground`, `INTERVAL_DAILY` |
| 2 | `fg evt` | 6 | 55 | right | `:usage` reconstruction, clipped to the window |
| 3 | `Δ fg` | 7 | 62 | right | col1 − col2, always signed |
| 4 | `label` | 18 | 142 | left | text face; `not visible` in `textSecondary` when unresolvable |
| 5 | `visible` | 6 | 55 | right | `Absent(29)` on API 28 (§8) |
| 6 | `fgs` | 6 | 55 | right | `Absent(29)` on API 28 |
| 7 | `tail` | 5 | 48 | left | `open` when the reconstruction ended mid-session |
| 8 | `rx fg` | 7 | 62 | right | `NetworkStats`, `STATE_FOREGROUND` |
| 9 | `tx fg` | 7 | 62 | right | |
| 10 | `rx bg` | 7 | 62 | right | `STATE_DEFAULT`, i.e. not-foreground |
| 11 | `tx bg` | 7 | 62 | right | |
| 12 | `bucket` | 14 | 113 | left | raw int then name: `45 restricted`, `5`, `unknown` |
| 13 | `mAh` | 7 | 62 | right | modelled total |

Frozen 142 + scrolling 895 + 10 + 2 + 10 = **1059dp total**.
At scroll offset 0 on 360dp: 10 + 142 + 2 + 55 + 55 + 62 = **326dp**, so both foreground numbers
and the signed delta are on screen with 34dp of `label` showing — which both proves the table
scrolls and puts instance 2 in the first screenshot. 911, 1208, 1256 and 1516 are all deleted.

**The stacked modelled-mAh bar is cut.** Column 13 keeps the number. The finding — a component
priced from an absent or explicitly-zero key — is already carried by the AUDIT verdict table and
by the expanded row's segment table with its verbatim reason lines (`wifi 0 · wifi.controller.rx =
0`). A bar normalised over the currently visible or currently filtered set is a length comparison
whose meaning changes when the reader types in a filter box, which is the one thing this app
cannot ship.

---

## 6. Font: bundled Roboto Mono, with a complete subset

`FontFamily.Monospace` maps to `Typeface.MONOSPACE`, which vendors substitute freely; every column
width in §4 and §5 is derived from a known digit advance, so a face whose advance is unknown makes
the documented width table unreproducible. Bundle it. The two paragraphs calling a bundled face
"the tell" and "the giveaway" are deleted — this is a reproducibility decision, not a styling one,
and it is a defensible sentence in the README.

- Two static weights, 400 and 500, Apache-2.0, one line in `NOTICE`.
- `app/src/main/res/font/roboto_mono_regular.ttf`, `roboto_mono_medium.ttf`.
- Measured advance for `"0"` at 12sp, `fontScale 1.0`: **7.2dp**. Every `chars` figure in §4 and §5
  multiplies by this.

**Subset, complete and pinned.** Anything outside this list is a build failure, not a tofu box.

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

`▲ ▼ ▸ ▾ ⋮` are **not** in the subset: they are icons, not glyphs (§9.4). A JVM test in `:app`
walks every string constant in the `ui` package and asserts each codepoint is in this list; the
subset is a build input, so no device is needed to check it.

---

## 7. Zebra striping: banned

No alternating row backgrounds anywhere, in any table, in either theme. Background is already a
state channel — the hatch, `surfacePinned` for the pinned capacity row and for a tapped row — and a
second meaningless use of the same channel makes the first ambiguous. Row tracking across a
horizontally scrolled table is carried by the frozen first column and by tap-to-pin.

Delete: `row banding (every other row) #FAFAFA / #171717`, and `the zebra stripe on #F5F5F4 /
#1B1B19`.

---

## 8. Start destination, sampler interval, and the measured API corrections

### 8.1 Start destination

**AUDIT on every launch, including the first.** No first-run-only destination exists anywhere in
the app, and the `DataStore` boolean that would gate one is deleted: a destination reachable once
per install is a destination that never gets tested. The spec's "PROBES, rendered first, before
anything works" means **built first, at E6** — build order, not start destination.

PROBES is reached from its tab, from the `usage-access` and `queries` segments of the status strip,
and from the `[ open PROBES ]` button in AUDIT's populated-profile state.

### 8.2 Sampler

**One `PeriodicWorkRequest`, 6-hour interval, 1-hour flex, `ExistingPeriodicWorkPolicy.KEEP`,
`setRequiresBatteryNotLow(true)`, linear 30-second backoff.** Usage events survive about 10 days
and `NetworkStatsManager` has a two-hour bucket floor, so anything faster writes rows no column can
consume. 15 minutes and the clause "which is the platform floor and not a number I chose" are
deleted, along with the SELF panel string built on them; that panel reads
`scheduled every 6 h · last 6 runs 6:00, 6:12, 7:41, 6:00, 9:20, 6:03 apart · bucket RARE`.

Never a foreground service, never a `PARTIAL_WAKE_LOCK`.

### 8.3 Measured API corrections — every document inherits these

**a. `unsafeCheckOpNoThrow` is API 29 and `minSdk` is 28.** Unbranched, it is a `NoSuchMethodError`
on the author's only physical device, in the path that runs on every resume. One implementation, in
`:collect`, called from everywhere:

```kotlin
val mode = if (Build.VERSION.SDK_INT >= 29) {
    appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, packageName)
} else {
    @Suppress("DEPRECATION")
    appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, packageName)
}
val granted = mode == AppOpsManager.MODE_ALLOWED   // MODE_DEFAULT is denied for this op
```

`lint { error += "NewApi" }` in `:app` and `:collect`; the SDK CI job already runs lint, so this
class of defect fails the build rather than the phone.

**b. `:usage` cannot rely on API 29/30 events on Android 9.** `ACTIVITY_STOPPED` (23) and
`ACTIVITY_DESTROYED` (24) arrive at API 29 — which is why `mTotalTimeVisible` is API 29 —
and `DEVICE_SHUTDOWN` (26) / `DEVICE_STARTUP` (27) at API 30. On the ANE-LX2 the stream carries
only 1 and 2, so a three-state machine has no exit from `Visible` and `visibleMs` degenerates into
"time since first pause". The machine declares a degraded mode keyed on `SDK_INT`:

| level | behaviour |
| --- | --- |
| < 29 | no `Visible` state; event 2 closes the foreground interval and returns the key to `Idle`. `visible` and `fgs` render `Absent(29)` for every row — a full column of em dashes, uncropped in the README screenshot |
| < 30 | shutdown detection falls back to the "no records for a stretch" heuristic only; the lane-chart band is labelled `no data (inferred)` |
| ≥ 30 | the full machine as specified |

`:usage` publishes `degradedBelowApi: Set<String>` naming the columns that are structurally
unavailable, and the APPS header says so rather than showing an empty column with no explanation.

**c. "Added at" and "behaves this way from" are different numbers.** `UsageStatsManager.isAppInactive`
was added at **API 23**; its silent-false without usage access begins at **API 30**.
`PowerManager.isIgnoringBatteryOptimizations` was added at **API 23**; its filtered-false for a
package outside `<queries>` begins at **API 31**. Rendering `Absent(31)` on an API 28 device where
the method exists and returns a real answer is a manufactured availability claim on the screen
whose subject is manufactured answers.

`ProbeRow` therefore carries two fields:

```kotlin
data class ProbeRow(
    val call: String,
    val reading: Reading<*>,
    val minApi: Int,              // the method does not exist below this
    val behaviourSinceApi: Int,   // the mechanism this row demonstrates begins here
    val section: ProbeSection,
)
```

The PROBES column header is `since`, not `api`, and renders both when they differ: `23 · filtered 31+`.
`Reading.Absent(minApi)` is reserved for genuine non-existence — `getPendingJobReason` 34,
`getHistoricalProcessExitReasons` 30, `FOREGROUND_SERVICE_START` 29. Below its
`behaviourSinceApi`, a pair renders its title row and one `textSecondary` line
(`not applicable below API 31 — package visibility filtering was introduced there`) and stays on
screen, so the reader knows what they are not seeing.

Every `minApi` constant is read off `android.jar` at E0
(`javap -classpath $ANDROID_HOME/platforms/android-36/android.jar android.app.job.JobScheduler`,
and `api/current.txt` for 34/35/36) and committed to the `apiSurfaceCheck` list.
`getPendingJobReasons` in particular is not asserted from memory.

**d. The README's lead capture.** Neither `isAppInactive`'s nor `isIgnoringBatteryOptimizations`'
silent-false mechanism exists on the ANE-LX2, so a capture on that phone cannot show them.

- The lead capture is **pair 1, `getAveragePower`, recorded on the ANE-LX2** — the only pair that
  fires on the physical device, with no permission and no manifest entry, which is already the
  stated reason it is first.
- Pairs 2, 3 and 4 are **API 36 emulator captures**, labelled as such in the alt text and in the
  HTML comment above each.
- One line joins the known-limitations block: *three of the four paired demonstrations are emulator
  captures, because the one physical device predates the mechanisms they show.*

---

## 9. Row heights, spacing, chrome, hatch — once

### 9.1 Row heights

One height per table, uniform within a screen, set directly rather than with `heightIn` — a
variable-height table cannot be scanned and destabilises `LazyListState` offsets.

| screen | row | header |
| --- | ---: | ---: |
| APPS | 28dp | 30dp |
| AUDIT | 32dp | 30dp |
| SELF | 32dp | 30dp |
| PROBES | 44dp | 30dp |
| DEVICE | 28dp | 30dp |
| AUDIT group header | 24dp | — |

44dp for APPS rows is deleted. The 48dp-minimum rule that forbade 28dp is deleted with it, and
replaced by the honest version: a row is not a small control — the target is the full row width,
360×28dp, which clears WCAG 2.5.8 at 24×24 and knowingly fails Material's 48dp guidance. That is
stated, not worked around. Genuinely standalone controls — the sort caret, the PROBES re-run
button, the status-strip segments — get 48dp via `minimumInteractiveComponentSize()`.

Height at large font scales is **measured, never multiplied by `fontScale`** — Android applies
non-linear font scaling above ~1.3 from API 34, so a single multiplier is wrong for at least one
style in the row:

```kotlin
val lineBox = with(density) { measurer.measure("0", SipperType.mono).size.height.toDp() }
val rowHeight = max(declaredHeight, lineBox + 8.dp)
```

There is no stacked-row fallback above any font scale. `AppRowStacked` is deleted: it is a second
layout that would never be tested, and it removes the side-by-side delta that instance 2 depends on.

### 9.2 Spacing

Base unit 4dp. The scale is `2, 4, 6, 8, 12, 16, 24` and nothing else. Horizontal budget, stated in
full so the §4/§5 totals derive rather than being asserted: **10dp row lead padding, 6dp per cell
side (12dp between adjacent columns), 2dp frozen-column edge, 10dp trailing padding after the last
column.** Rows carry no vertical padding; content is centred in the declared height.

### 9.3 The hatch

45 degrees, 1dp stroke, **5dp pitch measured along x**, `hatchLine` over `hatchGround`, **no state
hue**. 4dp and 6dp are deleted. The cell contains nothing — no number, no dash, no `0.0`, no `?`,
no glyph — and the cause chip sits immediately right in `textSecondary` on `hatchGround`.

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

Applied as `Modifier.clipToBounds().drawBehind { hatch(colors.hatchLine) }` — `DrawScope` does not
clip on its own, and without it every stripe bleeds up to one row-height into the neighbouring
column and the rows above and below.

The manufactured value appears in exactly two places in the app: the expanded detail row, prefixed
`platform returned:`, left-aligned in `textSecondary`; and the PROBES paired demonstrations, where
both halves render the platform's return identically at the same size and colour so the quote
cannot be read as sipper's own number. Both are inside `SilentDefaultDemo.kt`'s and the expanded
row's `@OptIn(AuditOnly::class)`.

### 9.4 Chrome, icons, motion, loading

- **No `TopAppBar`.** `PrimaryTabRow`, 48dp, five text-only tabs, `labelMedium` 11/600, not
  scrollable — `PROBES` is the widest at ~42dp and five fit at 360dp.
- **Status strip: bottom, above the navigation bar, 32dp, `surfaceRaised`, `ruleStrong` 1dp on
  top, `monoStrip` 11/400, ` · ` between every segment** — no ` | ` grouping. 22dp, 24dp, 36dp and
  44dp are deleted, and so is the top position. 32dp gives each tappable segment a target that stays
  inside the strip's own bounds; the earlier 40dp target overlapping the content above it would have
  made the bottom-most table row untappable. Segments are ≥48dp wide, tappable, and navigate to the
  screen that owns them. Overflow scrolls horizontally; it is never truncated and never wrapped.
- **Four icons, all `material-icons-core`:** `ArrowDropUp` / `ArrowDropDown` (sort caret and the
  APPS row expander, rotated), `OpenInNew` (a control that leaves the app), `MoreVert` (the overflow
  on AUDIT and PROBES). "Three" and the `▼ ▲ ▸ ▾ ⋮` text glyphs are deleted. No icon accompanies a
  text label except `OpenInNew`.
- **Corner radius 0 everywhere except chips at 3dp. Elevation 0. No `Card` in the codebase.**
- **Motion:** the Material ripple at its default plus `animateContentSize()` at 120ms on row expand.
  Nothing else animates.
- **Loading, one design, one threshold:** below **120ms** nothing changes. Beyond it the table
  renders its real header and layout-true skeleton rows — a 1dp `rule` underline at each cell's real
  character width — and the status strip carries a determinate count, `reading 12/47`, so a stall
  names the key that hung. There is no `CircularProgressIndicator` and no indeterminate
  `LinearProgressIndicator` anywhere; the 300ms indeterminate bar is deleted.
- **Refresh:** `PullToRefreshBox` on **APPS and SELF only**. AUDIT and DEVICE have none — a profile
  read cannot change while the process lives, so the gesture would be theatre. PROBES has explicit
  per-row and all-rows `run` buttons.
- **The prominent disclosure is a full navigation destination** (`Screen.Disclosure`), never a
  dialog or bottom sheet, because a dialog dismissible by an outside tap is not a disclosure. The
  `[ grant ]`-opens-a-dialog variant is deleted.

### 9.5 The table primitive

One `ScrollTable`, shared by all five screens. Three rules, because the widths above are
meaningless without them:

1. **One gesture node, above the header.** `Modifier.scrollable(state, Orientation.Horizontal)` on
   the `Box` wrapping the header and the `LazyColumn` — not `horizontalScroll` on every row. Per-row
   `horizontalScroll` installs one pointer-input, nested-scroll, fling and overscroll node per
   visible row; lets whichever row measured last clobber `maxValue` (the 24dp group header would cap
   the scroll to its own width); kills a fling when the row driving it scrolls out of the viewport;
   and blocks `mergeDescendants` past its own scroll semantics, which §9.6 needs. Rows and the
   header are layout-only: `Modifier.clipToBounds().layout { … place(-state.offsetPx.intValue, 0) }`,
   so scrolling triggers placement, not recomposition. `maxPx` is computed once from the known
   column widths, never from a row's measured size.
2. **No `weight` and no `fillMaxWidth` inside the shifted region.** Every column width is intrinsic.
3. **The frozen column sits outside the shifted region**, in the same parent `Row`. There is no
   second scroll state and no synchronisation code. The frozen edge is drawn, not composed:
   `Modifier.drawWithContent { drawContent(); if (state.offsetPx.intValue > 0) drawRect(...) }` —
   reading `hScroll.value` in a row's composable scope recomposes every visible row on every frame
   of a drag.

The sort/filter projection is `remember(rows, query) { projectApps(rows, query) }` in composition, or
`combine(rowsFlow, queryFlow) { r, q -> projectApps(r, q) }.stateIn(...)` in the ViewModel.
**`derivedStateOf` is deleted from all four documents that claim it is what stops scrolling from
re-sorting** — the projection never reads scroll state, so it would not re-run either way, and the
node plus dependency-set comparison it adds is the textbook misuse. `projectApps` is a pure function
tested on the JVM.

`TableColumn(id, title, chars, align)` is the single width source; `width = chars × advance + 12.dp`.
`rememberDigitAdvance()` keys on `Density` and the `TextStyle` only — never `LocalConfiguration` —
and divides a 20-glyph run so the per-character ceiling error stays under 0.05px:

```kotlin
@Composable
fun rememberDigitAdvance(): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(density, SipperType.mono) {
        with(density) { (measurer.measure("0".repeat(20), SipperType.mono).size.width / 20f).toDp() }
    }
}
```

A `:app` JVM test sums the committed column list and asserts the §4 and §5 totals, and
`./gradlew :app:columnTableDoc` emits those two markdown tables, so the documents cannot drift from
the code.

### 9.6 Accessibility

- `CollectionInfo(rowCount = rows.size, columnCount = 1)` on every table list, because what TalkBack
  navigates is a list of merged rows. Declaring 13 columns against merged rows makes TalkBack
  announce a column index for a node that is the whole row. Cell-by-cell reading lives in the
  expanded row's linear `label: value` list.
- Each row is one merged semantics node with an ordered description built from the column titles.
  A hatched cell announces `manufactured value, cause key absent from profile` and never the number.
- The pinning test uses the unmerged tree:
  `onNodeWithText("0.0", useUnmergedTree = true).assertDoesNotExist()` plus
  `onNodeWithContentDescription("manufactured", substring = true).assertExists()`.
- Tables are LTR-locked. Every cell sets `TextDirection.Ltr` explicitly — package names, keys, hex
  ids and exception strings are LTR data — **except the `label` cell**, which sets
  `TextDirection.Content` so a native-script app name renders correctly inside its LTR-positioned
  cell. The status strip is inside the LTR lock; it is machine data end to end.
- `AppsQuery.buckets` is persisted as an `IntArray`, not a `Set<Int>` — a `Set` survives
  `SavedStateHandle` only via `Serializable`, which newer versions reject on write.

### 9.7 Contrast is produced, not asserted

`:app`'s JVM test source set holds `ContrastTest`: it recomputes every ratio in §1.4 from the
committed token values and fails below 4.5:1 for text pairings, and
`./gradlew :app:contrastDoc` regenerates the two tables. The three non-text exceptions are a
committed allowlist with the reason on each line. No contrast number is typed into a document by
hand.

---

## 10. Prose tells to strip

Each of the following may appear **once** in the whole document set, in the named file, and nowhere
else. Everywhere else the clause is cut and the sentence ends on the mechanism, which is the part
that was carrying information.

| tell | keeps it | cut everywhere else |
| --- | --- | --- |
| the self-referential closer — a sentence ending by naming the repo's own thesis ("…which is the exact confusion this app exists to remove", "…is the bug this app is about", "…the behaviour the repo objects to", "…the thing this repo exists to complain about") | `README.md`, "The claim" | 15+ instances across DESIGN §7.5, §9; SCREENS §0, §1.1, §1.3, §4.1, §6.4, §8; ARCHITECTURE §2, §7, §8 |
| "a battery monitor that trips its own subject matter is not a defensible artefact" | `docs/ARCHITECTURE.md` §9, where the sampler is specified | README, SCREENS (both the sampler and the SELF wake-lock panel) — those say "see ARCHITECTURE §9" |
| "one exception inside a refusal list reads worse than no refusal list" | `docs/ARCHITECTURE.md` §12, "Decisions I expect to be argued with" | README, SCREENS §2.4 and §2.7 — and it is never transplanted onto the `under su` bullet, which is an unrelated decision |
| "naming the wrong mechanism is caught in a minute" | `README.md`, the "Four verdicts" heading | ARCHITECTURE §3 and SCREENS §2.5, which say what actually differs instead: an explicit `0` in a divisor key gives `Infinity`, an absent one gives `0.0` |
| "structural, not a convention" / "mechanical rather than a habit I might drop on evening nine" | `docs/ARCHITECTURE.md`, intro | DESIGN intro, SCREENS §0, README |
| "a gate that has never rejected anything is decoration" | `docs/ARCHITECTURE.md` §10 | README, which instead says which two gates rejected which of the author's own earlier drafts |
| the "not X. It is Y." two-beat | `README.md`, instance 4 | ARCHITECTURE intro, §3, §11 — rewritten as single positive sentences |

Two more, not aphorisms but the same failure:

- **Symmetrical refusal sections.** Eight "Deliberately not on…" / "What this does not do" /
  "Banned" sections across five files, none shorter than the last, is over-structure. Exactly two
  survive: `DESIGN §15` (the banned table, checkable) and `ARCHITECTURE §11` (capability limits,
  load-bearing). Their contents lose the `no X, no Y, no Z` anaphora — one item per line. Items
  carrying real information move into the section they belong to.
- **Invented numbers in the same register as measured ones.** Every ASCII mock carries one line
  above it: *shapes only; every number in this block is invented and none is a device fact.* Filter
  counts, digests, route timings and package counts outside a real measurement render as `n`. A
  reviewer who catches one invented number in the design docs stops believing the README's measured
  ones, and the README's measured ones are the repository.

Finally: `## What the app does for you` becomes `## The five screens`. The README's parenthetical
spaced hyphens become em dashes to match the docs, and the opening sentence is split so it carries
one clause at a time. `## Prior art, before I make any claim` becomes `## Prior art`; the section's
first sentence already says it comes first.
