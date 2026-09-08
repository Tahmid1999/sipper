# Screens: APPS, SELF, DEVICE

> `docs/CONTRACT.md` fixes tokens, widths, types and the API levels below. This file owns the
> behaviour of the APPS, SELF and DEVICE screens — layout order, states, flows, saved state and
> copy strings. Where they disagree, the contract wins.

AUDIT and PROBES are in `docs/SCREENS-AUDIT-PROBES.md`. The flows and state machines
behind all five screens are in `docs/FLOWS.md`.

Every device number quoted here was measured on the ANE-LX2 (API 28, EMUI 9.1) or on the API 34 /
API 36 emulators. Package counts, filter counts and run timings that are not a measurement render
as `n`, on the screen and in this document. Every ASCII block below carries one line above it:
*shapes only; every number in this block is invented and none is a device fact.*

---

## 0. The rule these three screens obey

`Reading<T>` is defined in `:audit` — contract §2. Nothing on any screen takes one apart itself.
`ReadingCell` is the composable that accepts a `Reading`, and `format` is reachable from its
`Value` branch and nowhere else; a `SilentDefault` reaches a hatch, not a formatter.

Per-branch behaviour that belongs to these screens rather than to the type:

- `Value` → the formatted number, `textNumeric`, right-aligned in a numeric column.
- `Denied` → a `denied` chip in `stateDenied`, tappable, opening a one-line sheet carrying
  `permission` and the `Grant`. `Grant.SettingsToggle` puts an `[ open ]` button in the sheet with
  `OpenInNew`; `Grant.ManifestQueries` names the `<queries>` entry and offers no button, because it
  is a build-time fix; `Grant.Unreachable` offers no button either. A sysfs `EACCES` must not sprout
  a Grant affordance.
- `Absent` → an em dash in `textSecondary`, with `requires API {minApi}, this device is {SDK_INT}`
  in the sheet.
- `SilentDefault` → the hatch (contract §9.3) and the cause chip. The cell holds no number.

The manufactured value appears on these three screens in one place: the expanded APPS row, prefixed
`platform returned:`, inside that file's `@OptIn(AuditOnly::class)`.

---

## 1. Formatting, shared by all five screens

Pure functions in `app/src/main/kotlin/…/ui/Fmt.kt`, no Compose imports, table-driven JVM tests one
per function. Everything is `Locale.ROOT`: these are machine values, and Bengali digit shapes in a
column I am asking a reviewer to compare against the column beside it would be a regression.

### 1.1 Durations

```kotlin
fun hm(ms: Long): String         // "0:41", "12:47", "312:00"
fun hmSigned(ms: Long): String   // "+1:10", "-0:04", "+<1m", "0:00"
fun millis(ms: Long): String     // "86400000 ms"
```

`hm` is `H:MM`, hours unpadded, truncated toward zero, no unit suffix — the column header carries
the unit. Right-aligned mono, so `0:41` and `312:00` line up on the colon. Six characters covers the
retention window (`240:00` is ten days), which is where the §2.2 widths for `fg fw` and `fg evt`
come from; `hmSigned` needs seven.

`hmSigned` always carries a sign except for an exact zero, which renders `0:00` in `textSecondary`
so the eye skips it. A nonzero magnitude below one minute renders `+<1m` / `-<1m` rather than
`+0:00` — the delta column's job is saying that two sources disagree, and rounding a disagreement to
zero destroys the row.

`millis` is the exact integer with a `ms` suffix and **no thousands separators**, used in expanded
rows, sheets and clipboard copy. No separator anywhere in the app, including `/proc/meminfo`, so
every value pastes into a bug report unedited.

Negative inputs are legal; the delta is what produces them. `Long.MIN_VALUE` throws
`IllegalArgumentException` — it cannot be negated and no platform read produces it.

### 1.2 Byte counts

```kotlin
fun bytes(b: Long): String   // "0 B", "931 B", "1023 B", "1.0 KiB", "61 KiB", "2.3 GiB"
```

Binary units, and the unit string is `KiB`/`MiB`/`GiB`/`TiB` rather than `KB`/`MB` so a network
figure is not read against a carrier's decimal MB. Largest unit where the scaled value is ≥ 1; one
decimal when the scaled value is < 10, zero decimals otherwise. Maximum width is 7 characters
(`1023 B` is 6, `999 GiB` is 7), which sets the byte column widths in §2.2.

Zero and absence do not share a glyph:

| case | cell |
|---|---|
| a `NetworkStats.Bucket` existed for this uid and summed to zero | `0 B`, `textSecondary` |
| no bucket for this uid in the window | em dash — this is `Absent`, not zero |
| `querySummary` threw, or the appop is missing | `denied` chip, `stateDenied` |

`/proc/meminfo` reports in units labelled `kB` that are in fact KiB. `Fmt.bytes` takes bytes, so the
meminfo reader multiplies by 1024 and DEVICE shows the raw line beside the formatted value; the
label is wrong at the source and hiding that is a small lie.

### 1.3 Temperatures, and the band that is refused

Thermal `temp` files on this phone report in three ways. The detector:

```kotlin
sealed interface Temp {
    data class Celsius(val c: Double, val unit: TempUnit) : Temp   // MILLI | DEGREE
    data object Sentinel : Temp                                    // -40 in either scale
    data class Unclassified(val raw: Long) : Temp
}
```

| raw `v` | verdict | rendered |
|---|---|---|
| `-40` or `-40000` | `Sentinel` | `sentinel` in the unit column, em dash in `°C` |
| `v >= 1000` | millidegrees | `v / 1000.0` → `58.6 °C`, unit `milli` |
| `-40 < v < 200` | degrees | `v.toDouble()` → `56.0 °C`, unit `deg` |
| `200 <= v < 1000` | `Unclassified` | raw integer only, unit `?` in `stateAbsentZero`, `°C` em dash |

The `[200, 1000)` band is refused rather than guessed. A decidegree kernel reporting `585` and a
millidegree kernel reporting `0.585 °C` land in the same band, and there is no way to tell them
apart from one file. `?` takes `stateAbsentZero`, the hue AUDIT uses for `≠`: both mean two readings
that cannot be reconciled.

Display format is `%.1f °C`, `Locale.ROOT`, always Celsius. The raw integer stays in its own column
beside it — the conversion is a claim, so its input is on screen.

`ACTION_BATTERY_CHANGED`'s `EXTRA_TEMPERATURE` is documented as tenths of a degree and goes through
its own one-line converter, not the sysfs detector. Guessing at a documented unit is theatre.

### 1.4 Timestamps

Local time, device zone, 24-hour, `Locale.ROOT`:

- window inside one calendar day: `HH:mm:ss`
- window crosses midnight: `MM-dd HH:mm:ss`
- clipboard copy and every expanded row: ISO-8601 with offset, `2026-09-08T21:14:03+06:00`

Never relative time. "2 hours ago" is unreadable next to a shutdown gap, and these are timestamps
off an event stream.

### 1.5 The rest

- **Platform ints that have names** render `NAME (int)` — `REASON_FREEZER (14)`,
  `PENDING_JOB_REASON_APP_STANDBY (4)`. The integer is always present, because the name table is
  compiled against compileSdk 36 and the device may return something it does not contain.
  **The standby bucket is the exception and renders int-first** (`45 restricted`); that column sorts
  on the integer and unnamed values are routine there. §2.4.
- **Modelled mAh**: one decimal below 10, zero decimals above. Unit in the header.
- **Voltage, capacity, charge counter**: the platform's own unit, unconverted — `3762 mV`,
  `409000 µAh`. The app shows the unit it was given and converts where the conversion is itself the
  finding (battery temperature, and the charge-counter arithmetic in §4.2).
- **Package names**: mono, `TextOverflow.MiddleEllipsis`, never truncated in clipboard copy.
- **Percent**: `78%`, no space.
- **Absence glyph** is `—` (U+2014) in `textSecondary`. The empty string is never rendered; a blank
  cell is indistinguishable from a rendering bug.

Type, colour and the mono face: contract §1 and §6. Widths are derived from the measured advance of
the bundled face (contract §9.5) and never multiplied by `fontScale`; row heights are measured the
same way (contract §9.1).

---

## 2. APPS

The dense grid, and the front-end evidence. One row per package the app can see — bounded by the
`<queries>` MAIN/LAUNCHER declaration and by whatever else usage events and `NetworkStats` hand
back. The count is printed on screen from the run, not asserted here.

### 2.1 Layout

Top to bottom: tab row (48dp, contract §9.4), window and filter chips (40dp, one horizontally
scrolling line), summary line (20dp, §2.6), sticky header (30dp), `LazyColumn` of 28dp rows. The
status strip is bottom-fixed, 32dp, shared with every screen.

One `ScrollTable` (contract §9.5): a single horizontal gesture node above the header, rows and
header layout-only, the frozen `package` column outside the shifted region, no second scroll state
and no synchronisation code. No `weight` and no `fillMaxWidth` anywhere inside the shifted region —
inside it the incoming `maxWidth` is `Constraints.Infinity` and a weighted child measures at zero.

`LazyColumn(key = { it.pkg }, contentType = …)`. Sort and filter are one `AppsQuery`, and the
projection is `projectApps(rows, query)` — a pure function called from the ViewModel
(`combine(rowsFlow, queryFlow) { r, q -> projectApps(r, q) }.stateIn(…)`). Scrolling does not
re-sort because the projection never reads scroll state. The row count is bounded by package
visibility and the comparator runs on primitives, so the sort is synchronous with no dispatcher hop;
if that stops being true the fix is `flowOn(Default)`, not a spinner.

`SavedStateHandle` keys: `apps.sort`, `apps.desc`, `apps.text`, `apps.minFgMs`, `apps.buckets`,
`apps.flags`, `apps.window`, `apps.expanded`. `apps.buckets` is an `IntArray`, not a `Set<Int>` — a
`Set` survives `SavedStateHandle` via `Serializable` only, which newer versions reject on write; the
`Set` is rebuilt in the ViewModel. The screen survives process death, which on a phone running
PowerGenie is not hypothetical.

### 2.2 Columns

Widths, headers and sources are contract §5. Restated here as the tap targets and header sheets they
carry, not as a second width table:

| pos | header | source and behaviour |
|---|---|---|
| frozen | `package` | frozen, middle-ellipsised; long-press copies the full string |
| 1 | `fg fw` | `UsageStats.getTotalTimeInForeground`, `INTERVAL_DAILY` |
| 2 | `fg evt` | `:usage` reconstruction from `queryEvents`, clipped to the window |
| 3 | `Δ fg` | col 1 − col 2, `Fmt.hmSigned` |
| 4 | `label` | `PackageManager.getApplicationLabel`, text face, §2.3 |
| 5 | `visible` | reconstructed; `Absent(29)` below API 29, §2.5 |
| 6 | `fgs` | reconstructed from `FOREGROUND_SERVICE_START(19)`/`STOP(20)`; `Absent(29)` below 29 |
| 7 | `tail` | `open` when the reconstruction ended mid-session, else blank |
| 8–11 | `rx fg` `tx fg` `rx bg` `tx bg` | `NetworkStats.querySummary`, `STATE_FOREGROUND` / `STATE_DEFAULT` |
| 12 | `bucket` | `STANDBY_BUCKET_CHANGED` transitions, §2.4 |
| 13 | `mAh` | modelled total, `Fmt.mah` |

Frozen 142dp, total 1059dp. At scroll offset 0 on a 360dp viewport at the default font scale, both
foreground numbers and the signed delta are on screen with 34dp of `label` showing. That is the
check to run against the first screenshot; above the default font scale the table simply needs more
scrolling, which is the intended behaviour, so the check is stated with its precondition rather than
as a universal.

Three headers need their semantics on the screen, so the header row is tappable and a long-press on
any header opens a one-line sheet:

- `fg fw` → `queryUsageStats(INTERVAL_DAILY) · buckets are not clipped to the requested window`
- `fg evt` → `queryEvents → interval reconstruction, clipped to the window`
- `rx bg` / `tx bg` → `NetworkStats state=DEFAULT, i.e. everything that is not STATE_FOREGROUND`

The last matters: the platform's category is "not foreground", and calling it "background" in the
header while saying so nowhere would be a default of my own.

**Shared uids.** `querySummary` is per-uid and a shared uid cannot be split per package. Rows in a
shared uid show identical byte values and carry a `uid shared` marker in the expanded row listing
the sibling packages. The bytes are not divided by anything.

**Network transports.** Below API 29, `querySummary` for `TYPE_MOBILE` needs a subscriber id and
therefore `READ_PHONE_STATE`, which this app does not request — a monitor asking for a dangerous
telephony permission is a monitor I would not install. On the ANE-LX2 the byte columns are wifi only
and the summary line says so:
`network: wifi only on this device (mobile summary needs READ_PHONE_STATE below API 29; not requested)`.
On 29+ both transports are summed and the expanded row splits them.

**Row expansion.** Tapping a row expands it and pins it to `surfacePinned`. Collapsed rows carry no
expander glyph: a caret on every row is a column of chrome, and the expanded block draws
`ArrowDropDown` rotated -90° at its top-left as the collapse control.

### 2.3 The label column draws the visibility boundary

Single line, three cases:

| case | label cell | expanded-row line |
|---|---|---|
| visible, has a MAIN/LAUNCHER activity | the label | — |
| visible, no launcher activity | the label, then a `no launcher` marker | `no launcher activity; visible through <queries> but with no MAIN/LAUNCHER entry` |
| not visible | `not visible`, `textSecondary` | `getApplicationInfo threw NameNotFoundException; package visibility filtering hides it and QUERY_ALL_PACKAGES is not requested` |

The third case is real on API 28, where `UsageStatsManager` does not apply visibility filtering and
hands back package names the app cannot resolve. The row exists, the numbers are real, and the label
is honestly missing.

The marker is not a Material chip — `SuggestionChip` is 32dp tall and the row is 28. It is a `Text`
with a 1dp `rule` border at the shared 3dp radius, named `LauncherlessMarker` in code so nobody
later mistakes it for a chip and fixes it into one.

### 2.4 The bucket column is an integer

The cell renders the raw `Int`, then the name in `textSecondary` where there is one: `10 active`,
`20 working_set`, `45 restricted`, and bare `5`, `15`, `50` where there is not. The name table is a
`when` over the constants documented in `UsageStatsManager` at compileSdk 36 — 45 has a public
constant (`STANDBY_BUCKET_RESTRICTED`, API 30); 5, 15 and 50 do not, and the column is an integer for
their sake.

Buckets come from `STANDBY_BUCKET_CHANGED` events, never from the reflected `@SystemApi`
`getAppStandbyBuckets()`. That call is reachable and it is not used: it returns a behavioural signal
for every installed package, including packages outside the declared `<queries>` scope.

The cost is visible. A package with no transition in the retained event window has no bucket, and
the cell renders `unknown` in `textSecondary`. The sentence does not fit in 113dp and abbreviating it
into `unknown (no txn)` would be worse than not saying it, so it goes in the expanded row and in a
summary-line clause:

*shapes only; every number in this block is invented and none is a device fact.*

```
bucket unknown for n of n packages (no transition in the 10-day event window)
```

### 2.5 Columns Android 9 cannot produce

`ACTIVITY_STOPPED` (23) and `ACTIVITY_DESTROYED` (24) arrive at API 29, which is why
`mTotalTimeVisible` is API 29; `DEVICE_SHUTDOWN` (26) and `DEVICE_STARTUP` (27) arrive at API 30. On
the ANE-LX2 the stream carries events 1 and 2 and nothing else, so a three-state machine has no exit
from `Visible` and `visibleMs` degenerates into "time since first pause".

`:usage` declares a degraded mode keyed on `SDK_INT` (contract §8.3b). Below 29 there is no `Visible`
state — event 2 closes the foreground interval and returns the key to `Idle` — and `visible` and
`fgs` render `Absent(29)` for every row, a full column of em dashes. That will be in the README
screenshot uncropped. Below 30 the shutdown detector falls back to the no-records heuristic and the
lane-chart band is labelled `no data (inferred)`.

`:usage` publishes `degradedBelowApi: Set<String>`, and the header row prints it rather than showing
an empty column with no explanation:

```
visible, fgs: structurally unavailable below API 29 — this device is 28
```

### 2.6 Sort, filter, window

Default sort is `fg evt` descending, ties by package ascending. Not `mAh`: on a device like mine the
modelled total can be zero for every row, and a default sort keyed on a number the app might refuse
to print is a broken default.

```kotlin
enum class AppsSort { Package, FgFw, FgEvt, Delta, Label, Visible, Fgs, RxFg, TxFg, RxBg, TxBg, Bucket, Mah }

data class AppsQuery(
    val sort: AppsSort = AppsSort.FgEvt,
    val desc: Boolean = true,
    val text: String = "",
    val minFgMs: Long = 0,
    val buckets: IntArray = IntArray(0),
    val onlyDisagreement: Boolean = false,
    val onlyNoLauncher: Boolean = false,
    val onlyWithNetwork: Boolean = false,
    val window: Window = Window.H24,
)
```

Header tap cycles descending → ascending → back to default. The indicator is `ArrowDropUp` /
`ArrowDropDown` at 14dp drawn inside the header cell's own width, so sorting never shifts the layout.

Filter chips at the Material default height, one horizontally scrolling line, each carrying its own
result count computed from the current data:

- `Δ > 1h (n)` — the demo filter, and the one to open a screenshot with
- `fg > 0 (n)`, `fg > 1m (n)`, `fg > 1h (n)` — mutually exclusive
- `no launcher (n)`
- `has network (n)`
- bucket chips, one per integer present in the data

Text search is a substring match over package and label, case-insensitive at `Locale.ROOT`.

Window chips: `24h` (default) · `3d` · `7d` · `10d`. When the requested window exceeds what is
retained, the summary line says so with the real bound:
`requested 10 d · events retained from 2026-08-29T04:12:07+06:00 (9 d 4 h)`.

The summary line is the screen's headline sentence and it is arithmetic:

*shapes only; every number in this block is invented and none is a device fact.*

```
Σ fg fw 47:12 · Σ fg evt 26:03 · n packages report more foreground than the 24 h window
```

A package cannot be in the foreground for longer than the window. Any `fg fw` cell exceeding the
window length is drawn in `stateAbsentZero` and carries a `>window` marker; `Δ fg` is `textNumeric`
normally and `stateAbsentZero` when `|Δ| > 1 h` — one hour because it is past any explanation
involving rounding or clock skew, and the claimed magnitude runs to a day.

Pull-to-refresh is on this screen and on SELF (contract §9.4); the underlying data changes under
both. Loading follows the one design: nothing below 120ms, then layout-true skeleton rows and a
determinate count in the status strip.

### 2.7 The expanded row

One row expanded at a time, held as `expandedPkg: String?`. One, because two unsmoothed Canvases
scrolling at once on a Kirin 659 is the constraint that decided it. `animateContentSize()` at 120ms,
which is the whole of the motion on this screen.

Content, on `surfaceRaised`, in order:

**a. Identity.** uid (and sibling packages if the uid is shared), install source where readable,
`versionName (versionCode)`, and whichever of §2.3's three cases applies, in full prose.

**b. The lane chart.** A `Canvas`, 168dp tall, full expanded width, x-axis spanning the selected
window, oldest at the left.

- Lanes top to bottom, 14dp tall with 4dp gaps: `resumed`, `visible`, `fgs`, then a 20dp `bucket`
  lane. Below API 29 the `visible` and `fgs` lanes render as empty bands labelled
  `requires API 29` rather than being dropped.
- Bands fill `textNumeric` at 0.85 alpha. Square corners, no gradient, no smoothing, no entry
  animation.
- Geometry is `layoutLanes(intervals, window): LaneDraw`, a pure function emitting normalised
  `startFrac`/`endFrac` floats, `remember(intervals, window)`-ed once; the Canvas multiplies by
  `size.width` at draw time and allocates nothing. A band narrower than 1dp is widened at draw time
  with `coerceAtLeast(1.dp.toPx())`. That is the chart's one distortion, it is stated in the legend
  line, and the exact intervals are in the table below the chart.
- Every label is measured once in the same `remember` and the cached `TextLayoutResult`s are handed
  to `drawText`; nothing calls `TextMeasurer.measure` inside the draw lambda.
- Grid: 1dp `rule` verticals every 6 hours, labelled `00:00 06:00 12:00 18:00` under the lanes with a
  single `local` note at the right end of the label row.
- **Shutdown gaps are gaps.** A `DEVICE_SHUTDOWN(26)` / `DEVICE_STARTUP(27)` pair, or a stretch of
  stream with no records, is a vertical hatched band across every lane — `clipRect` plus the contract
  §9.3 line loop, no `Path` allocated per frame — labelled `no data 03:12 → 07:44` at its top, or
  `no data (inferred) 03:12 → 07:44` below API 30. No lane is drawn through it and nothing is
  interpolated across it. A session open at shutdown is closed at the shutdown timestamp and marked
  at the band's left edge, matching the `tail` column.
- The bucket lane is a step: a 1dp `rule` vertical at each transition with the new integer printed
  immediately to its right. With no transition in the window the lane is hatched with
  `no transition in window`.
- `contentDescription` is generated from the interval list rather than left null:
  `resumed 4 intervals totalling 0:41; visible 6; fgs 0; bucket 20 to 30 at 14:02:11`.

**c. Interval table.** `start | end | lane | duration`, ISO-8601 with offset, `Fmt.millis` for
duration. This is where the chart's 1dp widening is corrected.

**d. Reconciliation.** The delta explained rather than asserted:

*shapes only; every number in this block is invented and none is a device fact.*

```
fg fw    1:58   7084000 ms   queryUsageStats INTERVAL_DAILY, 2 buckets, neither clipped
fg evt   0:47   2832000 ms   12 intervals reconstructed and clipped
Δ       +1:10   4252000 ms
residual multi-instance activity detected; instanceId is not exposed, so a residual is expected
```

The `multiInstance` flag comes from the `:usage` machine — interleaved `ACTIVITY_RESUMED` for the
same package with different classes and no intervening `ACTIVITY_PAUSED`. It is shown because the
delta would otherwise be read as entirely attributable to the unclipped buckets, and part of it is
not.

**e. Segment table**, one line per priced-or-refused component, one per component the calculator
would price at this API level including the ones priced at zero — dropping a zero segment hides the
finding. On the ANE-LX2 the live set is five (`cell idle scrn uid wifi`); on API 36 it is seventeen.

| component | key | verdict | mAh | share |
|---|---|---|---|---|
| 10 ch | 28 ch | 22 ch | 7 ch | 6 ch |

Verdict values and their derivation are contract §3. A back-filled segment renders its real number
with a `back-filled from screen.on` provenance chip in `stateBackFilled` and is never hatched.

Reason lines sit under the table, verbatim:

```
wifi 0 · wifi.controller.rx = 0
audio 0 · dsp.audio absent
scrn · back-filled from screen.on
```

Explicit zero and absence get different wording because the arithmetic differs: an explicit `0` in a
divisor key gives `Infinity`, an absent one gives `0.0`. A key the audit could not read at all never
reaches APPS — the model is then unavailable and the `mAh` cell carries the `Denied` or `Absent`
treatment.

There is no bar. The number is in column 13 and the finding is in this table and these reason lines,
sortable and copy-pasteable; a bar normalised over the current filter is a length whose meaning
changes when the reader types in the filter box.

**f. Route-and-observe.** One line, the same wherever it appears:
`sipper cannot limit an app; it finds the switch, takes you there, and afterwards measures whether
behaviour changed.` Then the resolved route buttons, each with `OpenInNew`, then the read-back level:
`read-back: level C (behavioural) — there is no first-party read-back for any package except
sipper's own.`

### 2.8 Usage access is off

The grid still renders. Package, label and the launcher/visibility state are available with zero
permissions, and every time and byte cell renders its `Denied` treatment with the real `Grant`. A
table of honest denials carries the screen; a blank page with a centred button does not.

The prompt sits in the summary-line slot, not in the middle of the screen:

```
usage access is off — n packages listed, 0 timelines. PACKAGE_USAGE_STATS · Settings → Apps → Special access → Usage access → sipper   [ open ]
```

Granted state comes from the single `:collect` appop check (contract §8.3a), which branches on
`SDK_INT` for `unsafeCheckOpNoThrow` (API 29) versus `checkOpNoThrow`, and treats `MODE_DEFAULT` as
denied. `checkSelfPermission` reports denied forever even when the call works, so it is not used
here. The disclosure that precedes a grant is a full navigation destination, never a dialog
(contract §9.4).

---

## 3. SELF

sipper as its own subject. Every read-back here is first-party, and the line at the top says so:

```
every value on this screen is about sipper's own package, which is the one package it can read
back directly. everything on APPS about another package is level B or level C.
```

One scrolling `Column`. No cards. Sections separated by a 1dp `rule` and a section label in
`textSecondary`. Same formatters as everywhere else. Pull-to-refresh, as on APPS.

**3.1 identity.** package · uid · `versionName (versionCode)` · `targetSdk 36` · process name ·
first install and last update timestamps · `isBackgroundRestricted()` ·
`getRestrictBackgroundStatus()` as `RESTRICT_BACKGROUND_STATUS_DISABLED (1)` · standby bucket from
the no-arg `getAppStandbyBucket()` (API 28), which is a `Value` and not a reconstruction. The section
label carries a `first-party` marker.

**3.2 timeline.** The §2.7b lane chart, always expanded, fed by `queryEventsForSelf`. One line under
it:

```
this chart needs no permission at all. the same chart for any other package needs usage access.
```

**3.3 bucket history.** `when (local) | bucket | Δ since previous`, from self
`STANDBY_BUCKET_CHANGED` events. The empty case is stated, not blank:
`no transition in the retained window (events are pruned at about 10 days)`.

**3.4 jobs.** One row per scheduled job: `id | periodic | pending reason(s) | last run`. Reasons come
from `getPendingJobReason(int)` (API 34) or the plural overload, rendered
`PENDING_JOB_REASON_APP_STANDBY (4)`, unnamed ints printed bare. The plural overload's API level is
read off `android.jar` at E0 and committed to the `apiSurfaceCheck` list (contract §8.3c); it is not
asserted from memory in this document, because rendering the wrong `Absent(minApi)` would be a
manufactured availability claim. On API 28–33 neither method exists and the section renders the
`Absent` treatment in full: `absent · getPendingJobReason requires API 34; this device is 28`. That
is what my own phone shows and it is a fair thing for a reviewer to see.

**3.5 exit records.** `getHistoricalProcessExitReasons(packageName, 0, 16)`, API 30+.
`when | reason | subreason | status | importance | pss | rss | description`, reasons as
`REASON_FREEZER (14)`, `REASON_EXCESSIVE_RESOURCE_USAGE (9)`, memory through `Fmt.bytes`. `Absent(30)`
below 30.

**3.6 wake locks, FGS, and the sampler.** Two totals and one sampler line, no gauge:

*shapes only; every number in this block is invented and none is a device fact.*

```
partial wake lock, last 24 h    0:00   sipper acquires none; the sampler is WorkManager work
foreground service, last 24 h   0:00   reconstructed from own events
sampler                         scheduled every 6 h · last 6 runs 6:00, 6:12, 7:41, 6:00, 9:20,
                                6:03 apart · bucket RARE
```

SELF prints sipper's own wake-lock and FGS totals so a reader can falsify the claim in
ARCHITECTURE §9 on their own phone. Deferral against the 6-hour period is expected and it is data,
which is why the last six intervals are printed rather than a mean.

**3.7 modelled mAh.** The same segment table and reason lines as §2.7e, same profile, applied to the
app the reviewer is holding.

**3.8 restrict me.** The closed loop.

```
sipper cannot restrict itself either. it can open the page and read back the result — which for
its own package it genuinely can.

[ open my battery settings ]
```

Route resolved at runtime: `android.settings.VIEW_ADVANCED_POWER_USAGE_DETAIL` with a `package:` URI
on API 31+, falling back to `ACTION_APPLICATION_DETAILS_SETTINGS`, with every candidate's
`resolveActivity` result shown, so the screen reports which routes exist on this device rather than
which ones ought to.

`isBackgroundRestricted()` and the bucket are re-read on `ON_RESUME` and any change is printed as a
diff line, kept until the process dies:

```
isBackgroundRestricted  false → true  at 2026-09-08T21:14:03+06:00   level A (first-party)
```

**3.9 visibility.** What `<queries>` bought, from `bench/queries-diff.txt`:

```
packages visible to sipper                n
packages installed on this device         not measurable without QUERY_ALL_PACKAGES, which is not requested
```

---

## 4. DEVICE

Milestone E9, and it ships last. The screen does not say so; the README does. It is a straight
read-out. Single scrolling `Column`, same section rule as SELF. No pull-to-refresh: nothing here
changes on a gesture.

### 4.1 battery

Two-column `label | value` rows from the sticky `ACTION_BATTERY_CHANGED`:

*shapes only; every number in this block is invented and none is a device fact.*

```
level            78% (78/100)
status           BATTERY_STATUS_DISCHARGING (3)
health           BATTERY_HEALTH_GOOD (2)
plugged          0
present          true
technology       Li-poly
voltage          3762 mV
temperature      41.0 °C   (raw 410, documented tenths)
charge counter   409000 µAh
current now      [hatch]   silent default — BatteryManager returns 0 where the HAL exposes no channel
```

`BATTERY_PROPERTY_CURRENT_NOW` is read as a `SilentDefault` when it returns `0` or `Int.MIN_VALUE`,
so the cell is hatched and carries the cause chip; the returned integer is not printed. On this
device `dumpsys battery` prints no current line and `/sys/class/power_supply/Battery` is denied even
to shell. Whether it returns anything real on other hardware is an open verification and PROBES
reports it.

The measured values behind that shape are quoted in §4.2 and §4.3, where they are labelled as
measurements.

### 4.2 the charge counter against the profile

Measured on the ANE-LX2:

```
charge counter    409000 µAh
level             78%
implied at full   409000 / 0.78 = 524359 µAh
```

The division and both its inputs are on the screen and nothing is concluded from it. The implied
figure agrees with neither `battery.capacity` — 1000 in `framework-res.apk`, 3000 in the loose file
the framework reads. Those sources sit side by side on AUDIT's pinned capacity row, which computes no
ratio between them (contract §4); this screen links there rather than repeating the argument.

### 4.3 thermal

`ScrollTable`, 28dp rows, one row per zone. 13 zones on the ANE-LX2.

| pos | column | chars | dp | align |
|---|---|---:|---:|---|
| frozen | `#` | 2 | 26 | right |
| 1 | `type` | 20 | 156 | left |
| 2 | `°C` | 7 | 62 | right |
| 3 | `raw` | 9 | 77 | right |
| 4 | `unit` | 9 | 77 | left |

Frozen 26 + scrolling 372 + 10 + 2 + 10 = 420dp. `°C` precedes `raw` so the converted value and its
input are both on screen at offset 0. `unit` is `milli`, `deg`, `sentinel` or `?` per §1.3.

Section header, because the route is itself a finding:

```
13 zones · n sentinel · n unclassified · read via /sys/devices/virtual/thermal
/sys/class/thermal is not readable on this device, even to shell
```

An unreadable zone renders `denied` with `permission = "SELinux: untrusted_app"` and
`Grant.Unreachable`, so the sheet explains and offers no button.

Zone 2 carries a `cross-check` marker and one line under the table, both values measured:

```
zone 2 Battery  41000 → 41.0 °C     ACTION_BATTERY_CHANGED 410 → 41.0 °C     agree (Δ 0.0 °C)
```

Agreement within 2.0 °C renders `agree` in `statePresent`; outside it, both values and the delta in
`stateAbsentZero`. This is the justification for the unit detector: it is validated against a public
API on the one zone where a public API exists.

### 4.4 cpu

`ScrollTable`, one row per cluster.

| pos | column | chars | dp | align |
|---|---|---:|---:|---|
| frozen | `cluster` | 8 | 70 | left |
| 1 | `cores` | 5 | 48 | right |
| 2 | `cur kHz` | 8 | 70 | right |
| 3 | `min` | 8 | 70 | right |
| 4 | `max` | 8 | 70 | right |
| 5 | `governor` | 14 | 113 | left |
| 6 | `steps` | 5 | 48 | right |

Frozen 70 + scrolling 419 + 10 + 2 + 10 = 511dp. Cores from `/proc/cpuinfo`, frequencies from
`/sys/devices/system/cpu/cpuN/cpufreq/*`; a per-core read that fails renders `denied`, not zero.

One cross-check row under the table:

```
cores, /proc/cpuinfo          8
cores, cpu.clusters.cores     1        disagree
```

`disagree` in `stateAbsentZero`, with a link to AUDIT rather than a repeat of the argument.

### 4.5 memory

Raw meminfo line beside the formatted value, because the source's unit label is wrong. No thousands
separators:

*shapes only; every number in this block is invented and none is a device fact.*

```
MemTotal        4057032 kB   3.87 GiB     (/proc/meminfo says kB and means KiB)
MemAvailable    1142880 kB   1.09 GiB
MemFree          298104 kB    291 MiB
Cached           902416 kB    881 MiB
SwapTotal       2400260 kB   2.29 GiB
SwapFree        1884112 kB   1.80 GiB
lowMemory       false
threshold        230400 kB    225 MiB     ActivityManager.MemoryInfo
```

One line under it: `free RAM is not a number worth printing on a device with this much zram, so swap
is next to it rather than in a second section.`

### 4.6 build

`Build` fields, mono, fingerprint selectable and wrapped:
`MANUFACTURER · MODEL · BOARD · HARDWARE · SDK_INT · RELEASE · SECURITY_PATCH · DISPLAY ·
FINGERPRINT`. The EMUI version is inside `Build.DISPLAY`; reading `ro.build.version.emui` would need
`SystemProperties` reflection and the app does not do that for one cosmetic string.

---

## 5. Testing these three screens

- `Fmt` is pure and lives in `:app`'s JVM test source set. Table-driven, one test per function,
  including `0`, `-1`, `1023`, `1024`, `Long.MAX_VALUE`, the `-40` and `-40000` sentinels, and both
  boundaries of the `[200, 1000)` unclassified band. The unclassified band has a test asserting that
  **no** temperature string is produced, which is the assertion that can fail.
- `projectApps(rows, query): List<AppRow>` is a pure function outside the ViewModel, tested on the
  JVM: every sort key, stable ties, every filter, and the `Δ > 1h` filter against a fixture where the
  framework value exceeds the window.
- `layoutLanes(intervals, window): LaneDraw` is pure and returns fractions, so its tests compare
  exact floats and do not model pixels. Cases: the sub-pixel band, and a shutdown gap that must not
  be bridged.
- The column lists for §4.3 and §4.4 are summed by the same `:app` JVM test that asserts the AUDIT and
  APPS totals (contract §9.5), so the dp figures above cannot drift from the code.
- The mono subset test (contract §6) covers every string constant on these screens, including `Δ`,
  `Σ`, `→` and `µ`.
- Semantics: each row is one merged node with an ordered description built from the column titles;
  `CollectionInfo(rowCount = rows.size, columnCount = 1)` on the list, because what TalkBack navigates
  is a list of merged rows. Cell-by-cell reading is the expanded row's linear `label: value` list. The
  pinning test uses the unmerged tree —
  `onNodeWithText("0.0", useUnmergedTree = true).assertDoesNotExist()` plus
  `onNodeWithContentDescription("manufactured", substring = true).assertExists()`.
- Every cell sets `TextDirection.Ltr` explicitly, except the `label` cell, which sets
  `TextDirection.Content` so a native-script app name renders correctly inside its LTR-positioned
  cell.
- No screenshot-testing dependency. Each screen has `@Preview` composables driven by the same
  fixtures the JVM tests use, in both themes, at `fontScale` 1.0, 1.3 and 2.0, on API 34 and API 36
  emulators — 1.3 and above is where Android's non-linear font scaling first shows. The capture
  checklist lives in `NOTES.md` beside the dated entries.
- `assembleDebug` and lint run in CI on a runner with the SDK, with `lint { error += "NewApi" }` in
  `:app` and `:collect`, so an unbranched API-29 call fails the build rather than the phone.
- Any committed `app_day` fixture is synthetic or from an emulator, never from the handset. Raw usage
  output off my daily phone is which apps I use and when, published under my name next to a CV.
