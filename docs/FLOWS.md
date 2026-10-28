# Flows and states

> `docs/CONTRACT.md` fixes tokens, widths, types and the API levels below. This file owns the
> first-run flow, the usage-access ask, screen states, navigation, saved state, refresh and
> staleness, the sampler, accessibility and the status strip. Where they disagree, the contract
> wins.

What happens on a cold install, how a `Reading` becomes a cell, what each screen looks like in
each state, what survives a process death and what deliberately does not. Per-screen layout —
columns, widths, copy — is in `docs/SCREENS-AUDIT-PROBES.md` and
`docs/SCREENS-APPS-SELF-DEVICE.md`.

The app has to say "I do not know" in several distinguishable ways. That is a rendering problem
before it is a data problem, so this document fixes the rendering first and lets the state machine
follow from it.

`README.md` carries the claim and the limitations. `docs/ARCHITECTURE.md` carries the modules,
`Reading<T>`, the key table, the schema, the CI gates and the sampler's scheduling.
`docs/DESIGN.md` carries colour, type, spacing, row heights and the table primitive. `NOTES.md`
carries the dated record of what actually happened on the two devices.

---

## 1. First run — cold install to a useful screen, zero permissions

No splash, no carousel, no welcome, no tour, no rating prompt, no permission dialog. The first
thing on screen is the AUDIT table.

Shapes only; every number in this block is invented and none is a device fact.

```
t+0     ComponentActivity.onCreate -> setContent -> SipperTheme -> SipperNavHost
        start destination = Screen.Audit, on every launch, first or not
t+0     AuditViewModel init launches the three route reads on Dispatchers.IO
t+~40   route reads complete, Panel.Ready emitted
t+~60   first useful frame: the key rows, the provenance block, the pinned capacity row,
        the status strip
```

Budgets, not measurements. They are asserted by a macrobenchmark in `:app`'s benchmark source set
and I replace them with real numbers the evening the benchmark runs:

| read | budget |
| --- | ---: |
| `Resources.getSystem().getIdentifier` + `getXml` + parse | 20 ms |
| `getResourcesForApplication("android")` + parse | 30 ms |
| `PowerProfile` reflection over the probed key set | 10 ms |
| cold start to first useful frame | 300 ms |

**Start destination is AUDIT on every launch, including the first.** There is no first-run-only
destination anywhere in the app and no `DataStore` boolean gating one: a destination reachable
once per install is a destination that never gets tested. The spec's "PROBES, rendered first,
before anything works" means built first, at E6.

PROBES is reached from its tab, from the `usage-access` and `queries` segments of the status
strip, and from the `[ open PROBES ]` button in AUDIT's populated-profile state.

### 1.1 What works before any ask

With zero permissions and no `<queries>` entry beyond MAIN/LAUNCHER:

- AUDIT in full — three read routes, the agreement badge, the diff when they disagree, all four
  verdicts, the capacity cross-check against the charge counter.
- PROBES in full, including the paired demonstrations that fire at this device's API level.
- SELF in full — sipper's own foreground timeline, own bucket, own pending-job reasons, own exit
  records with RSS at death.
- DEVICE in full.
- APPS in its `Blocked(NoUsageAccess)` shape (§4.3), which is a populated screen rather than an
  empty state.

Four of five screens are complete before the user is asked for anything, which is what lets the
ask be user-initiated rather than prompted.

---

## 2. The usage-access ask

### 2.1 Where it appears

Three entry points, each a tap the user makes:

1. The APPS footer: `1 of n packages readable — usage access is off` with a
   `Why sipper needs this` text button.
2. The `usage-access OFF` segment of the status strip.
3. The PROBES pair for `isAppInactive`, whose second half cannot be demonstrated without the
   toggle. Its detail cell reads `needs usage access to complete this pair`.

No timed prompt, no nth-launch prompt, no prompt on scroll depth, no badge or dot. If the user
never taps, sipper never asks again.

### 2.2 Prominent disclosure

`Screen.Disclosure` is a full destination on the back stack. A dialog is dismissible by an outside
tap, and an accidental dismissal is not an acknowledgement. The tab row is hidden while it is on
top.

Verbatim copy, at a 60-character measure, fitting a 360dp viewport at `fontScale 1.0` without
scrolling:

```
Usage access

sipper is asking for usage access so it can read, for every app on this phone:

  - foreground, visible and foreground-service time
  - bytes sent and received, split foreground and background
  - standby bucket transitions

It reads that on this device only. It writes it to a database in sipper's own
private storage. Nothing is uploaded: there is no INTERNET permission in the
manifest and there are no third-party SDKs in the build.

Turning it off again: Settings > Apps > Special app access > Usage access >
sipper. sipper re-checks that switch on every read and never caches the answer.

  [ Open the setting ]     Not now
```

`Not now` pops back to APPS and writes nothing. No counter, no snooze, no "don't ask again". A
snooze is a state that cannot be reached in a test without waiting on a clock, and the ask is
already user-initiated.

`Open the setting` fires the route chain in §2.3 and records the acknowledgement in DataStore as
`disclosure_ack_at` (epoch ms) and `disclosure_ack_version` (int). The version is bumped whenever
the copy above changes, and a bumped version re-shows the screen before the next deep link: an
acknowledgement of different words is not an acknowledgement of these words.

### 2.3 The three-branch deep link

`<queries>` declares the intent. `resolveActivity` is subject to package-visibility filtering from
API 30, and an undeclared intent resolves to null on exactly the devices where the feature
matters:

```xml
<queries>
    <intent><action android:name="android.settings.USAGE_ACCESS_SETTINGS" /></intent>
    <intent><action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" /></intent>
</queries>
```

The chain, in `UsageAccessRoute.kt`:

| branch | intent | why it is in this position |
| --- | --- | --- |
| A | `ACTION_USAGE_ACCESS_SETTINGS` + `Uri.parse("package:$pkg")` | on AOSP this lands on sipper's own row with the switch under the user's thumb |
| B | `ACTION_USAGE_ACCESS_SETTINGS`, no data URI | some builds ignore the URI, some refuse to resolve with it |
| C | `ACTION_SETTINGS` | the root of Settings; always resolves; the user walks the rest |

Each branch in order: `resolveActivity(intent, MATCH_DEFAULT_ONLY)` first, then `startActivity`
inside a `runCatching` for `ActivityNotFoundException`, because a resolve result is not a start
guarantee on OEM builds.

**What is recorded.** sipper cannot observe where Settings landed. It can observe which branch
resolved, which branch started without throwing, and whether the app op flipped before the next
resume. Those three facts, plus `Build.MANUFACTURER`, `Build.DISPLAY` and `Build.VERSION.SDK_INT`,
go to DataStore and render on PROBES.

Shapes only; every number in this block is invented and none is a device fact.

```
USAGE ACCESS ROUTE                                        RESOLVED  STARTED  OP FLIPPED
A  android.settings.USAGE_ACCESS_SETTINGS  package:...      yes       yes       yes
B  android.settings.USAGE_ACCESS_SETTINGS                   yes        -         -
C  android.settings.SETTINGS                                yes        -         -
```

Which branch fires on EMUI is itself a finding and gets a line in `NOTES.md` and a row in
`bench/deeplink-routes.txt`. `VIEW_ADVANCED_POWER_USAGE_DETAIL` is verified resolving on API 36
and not resolving on the ANE-LX2; the usage-access chain is unverified on EMUI at the time of
writing, and PROBES says so by rendering the table with empty cells rather than by asserting
anything.

If all three branches fail — never observed, but modelled, because the fallback is where a
"helpful" app starts guessing — the disclosure screen swaps its button for copyable text:

```
No Settings activity on this build handled any of the three intents above.
The switch is at: Settings > Apps > Special app access > Usage access > sipper
On this device (HUAWEI) the wording differs; I do not know what it is.  [ Copy ]
```

The last clause stays. I have no way to read an OEM's menu wording from inside the app, so I do
not print one.

### 2.4 Read-back

`ProcessLifecycleOwner.get().lifecycle` observes `ON_RESUME` and re-reads the app op through the
single branched implementation in `:collect` — `unsafeCheckOpNoThrow` at API 29 and above,
`checkOpNoThrow` below it, `MODE_ALLOWED` the sole granted value. `CONTRACT.md` §8.3a is the
implementation; `minSdk` is 28 and the unbranched call is a `NoSuchMethodError` on the author's
only physical device.

`checkSelfPermission(PACKAGE_USAGE_STATS)` reports denied forever even when the read works, so it
is never consulted. `MODE_DEFAULT` counts as denied for this op.

The result is never cached beyond the current composition. A cached `true` survives a revoke and
prints numbers from a `queryUsageStats` that returned an empty list.

If the op is still denied on resume, nothing is prompted. The APPS footer changes once:

```
Usage access is still off. The switch is on the page that just opened;
sipper cannot read whether you found it.
```

---

## 3. From a `Reading` to a cell

### 3.1 The type

`Reading<T>` and its four constructors — `Value`, `SilentDefault`, `Denied`, `Absent` — are
defined once in `:audit`, in `Reading.kt`. See `docs/CONTRACT.md` §2 for the declaration,
`DefaultCause`'s five members, `Route`, `RouteKind` and `Grant`. This document names constructors
and fields; it does not restate them.

The two facts this document depends on:

- `SilentDefault.shown` is unreadable without `@OptIn(AuditOnly::class)`, so getting a
  manufactured number onto the screen takes a new composable and an explicit opt-in.
- **Back-fill is a `Value`.** On API 34+ `initDisplays` and `initModem` synthesise new-schema keys
  from deprecated ones and log a deprecation warning naming the replacement. The platform says so,
  so the reading is `Value(v, Route(BACK_FILL, fromKey))`, it renders as a real number with a
  provenance chip, and it is never hatched.

### 3.2 The one composable

`ReadingCell` is the only composable that takes a `Reading`, and its `format` lambda is reachable
from the `Value` branch and nowhere else, over an exhaustive `when` with no `else`. Signature in
`CONTRACT.md` §2.1. A fifth constructor breaks the build at every render site.

`ReadingCellTest` renders each constructor with `format = { "42" }` and asserts
`onNodeWithText("42", useUnmergedTree = true).assertDoesNotExist()` for `SilentDefault`, `Denied`
and `Absent`, plus
`onNodeWithContentDescription("manufactured", substring = true).assertExists()` for the hatched
case. The unmerged tree is the one that matters: a number rendered in an unmerged child passes the
merged-tree search while still being in the semantics.

`:app` carries exactly one file-level `@OptIn(AuditOnly::class)`, in
`ui/probes/SilentDefaultDemo.kt`, plus the expanded row's opt-in for its `platform returned:`
line.

### 3.3 The four renderings

Colour tokens per `CONTRACT.md` §1.2. Numeric cells are right-aligned and tabular; type per
`docs/DESIGN.md`.

| constructor | cell | expanded row adds | spoken |
| --- | --- | --- | --- |
| `Value(v, route)` | `format(v)`, right-aligned | `route: android package resources` | `"screen.on, 143 point 0 milliamps, from the android package resources"` |
| `Value(v, Route(BACK_FILL, k))` | `format(v)` and a `back-filled from screen.on` provenance chip | `synthesised from screen.on; the framework logs this` | `"…, 0 point 1 milliamps, back-filled from screen on"` |
| `Denied(permission, howToGrant)` | `—` and a chip carrying the short cause | `needs: usage access`, and the verbatim runtime line | `"…, not available, usage access is off"` |
| `Absent(minApi)` | `—` and `min 34` | `added in Android 14; this device is Android 9` | `"…, not available, needs Android 14, this device runs Android 9"` |
| `SilentDefault(cause)` | hatch, no numeral, no dash, no glyph, cause chip immediately right | `platform returned: 0.0`, left-aligned, secondary | `"…, manufactured value, cause key absent from profile"` |

Chip labels, one per `DefaultCause`: `key absent`, `no usage access`, `not in <queries>`,
`appop default`, `unclipped bucket`.

The hatch is specified once, in `CONTRACT.md` §9.3 — 45 degrees, 1dp stroke, 5dp pitch along x,
`hatchLine` on `hatchGround`, clipped to bounds, no state hue. A coloured cell reads as a value.
The chip carries the meaning at full contrast, and so does the content description.

The manufactured value appears in two places in the app: the expanded detail row, prefixed
`platform returned:`; and the PROBES paired demonstrations, where both halves render the
platform's return identically at the same size and colour so the quote cannot be read as sipper's
own number. Nowhere else, and never in a numeric column.

---

## 4. Screen states

### 4.1 The shape, for every screen

```kotlin
sealed interface Panel<out T> {
    data object Loading : Panel<Nothing>
    data class Ready<T>(
        val data: T,
        val readAtWall: Long,      // currentTimeMillis, for display
        val readAtMono: Long,      // elapsedRealtime, for age arithmetic
        val completeness: Completeness,
    ) : Panel<T>
    data class Blocked(val reason: BlockReason) : Panel<Nothing>
    data class Failed(val exceptionClass: String, val message: String?) : Panel<Nothing>
}

sealed interface Completeness {
    data object Complete : Completeness
    data class Partial(val present: Int, val total: Int, val missing: List<String>) : Completeness
}

sealed interface BlockReason {
    data object NoUsageAccess : BlockReason
    data object UserLocked : BlockReason
    data class BelowMinApi(val minApi: Int) : BlockReason
    data object SamplerOff : BlockReason
    data object NoSamplesYet : BlockReason
}
```

**Partial is a field, not a state.** A screen holding 40 of 47 keys is populated, not
half-loading. Modelling it as a top-level state produces two layouts that flip, and on this
project partial is the ordinary outcome.

**Stale is also a field.** `readAtMono` plus a clock gives the age; §7.2 turns it into one
indicator. A `Stale` constructor would make every consumer unwrap twice for something
presentational.

**`Failed` is for sipper's own exceptions** — a SQLDelight error, a parse failure in `:audit`. A
platform denial is never `Failed`; it is a `Reading`.

Both clocks are stored because the wall clock can jump. A timezone change or an NTP correction
would otherwise make a two-minute-old read display as three hours stale.

**Loading, one design, one threshold.** Below 120 ms nothing changes. Beyond it the table renders
its real header and layout-true skeleton rows — a 1dp rule at each cell's real character width —
and the status strip carries a determinate count, `reading 12/47`, so a stall names the key that
hung. No `CircularProgressIndicator` and no indeterminate `LinearProgressIndicator` anywhere in
the app.

### 4.2 AUDIT

Zero permissions, so **AUDIT has no `Blocked` state**, which is why it is the start destination.

Layout order, top to bottom: tab row, provenance block, pinned capacity row, the primary-route
caveat when it applies, counts line, sticky table header, grouped key rows, footer digest line,
status strip fixed at the bottom.

Six columns — `key` frozen, then `verdict`, `value`, `=`, `from`, `effect` — with route values in
the provenance block above the table rather than as three numeric columns inside it. Column set
and widths: `CONTRACT.md` §4. Rows group under a sticky group header per `PowerCalculator`
(`CpuPowerCalculator · 11 keys · 6 at 0`).

| state | what renders |
| --- | --- |
| `Loading` (>120 ms only) | layout-true skeleton at the real column widths, `reading n/47` in the strip |
| `Ready`, routes agree | provenance block badge `routes 3/3 agree`; every row's `=` column carries `=` |
| `Ready`, routes disagree | badge `routes 2/3 agree`, a diff in the provenance block listing every key where they differ with all three values side by side, and `≠` in the affected rows |
| `Ready`, a route unavailable | badge reads `routes 2/2 agree · 1 unavailable`, never `2/3` — a route that did not run is not a route that disagreed. The reflection line shows the verbatim `java.lang.NoSuchMethodException: android.os.PowerProfile.getAveragePower` |
| `Ready`, `Completeness.Partial` | counts line `24 of 47 keys return 0.0 · 11 absent from the file · 5 explicitly 0 · 8 back-filled` |
| `Ready`, `Completeness.Complete` | the populated-profile state below; the table still renders with every row `PRESENT` |
| `Failed` | the parse exception class and message, the raw XML byte length, and a `Copy` for the first 2 KB. No retry button — a deterministic resource read does not become correct on a second try |

Capacity is pinned above the first group in both completeness states, on `surfacePinned`, showing
all four sources and computing no ratio between them. The numbers below are the measured ANE-LX2
values:

```
battery.capacity   framework-res 1000   /product/etc/xml 3000   charge counter 409000 uAh
```

**The primary-route caveat.** Route B, `getResourcesForApplication("android")`, fills the `value`
column and carries the verdicts. On any device where the routes disagree, a persistent line sits
directly under the capacity row:

```
verdicts below are computed from framework-res.apk. dumpsys pws reports 3000 on this device, so
the framework is pricing from /product/etc/xml and these verdicts describe a file it does not read.
```

That is the first question a reviewer asks about this screen, so it is on the screen.

**The populated-profile state**, which is what a reviewer on a recent Pixel sees, written before
it is needed rather than after somebody complains. Full width, above the table:

```
47 of 47 keys present. This profile manufactures no finding.

That removes instance 4, not the rest. Instances 1 to 3 are on PROBES.

If this phone reports subsystem energy through the PowerStats HAL, measured
energy replaces these values for subsystem totals, and no public API tells an
installed app which power model is in use. So on this device I cannot
establish that this profile is in the loop at all, and I am not going to
render a verdict about attribution I cannot show is in play.

  [ open PROBES ]
```

No pull-to-refresh. A profile read cannot change while the process lives.

### 4.3 APPS

One row per visible package. Layout order: tab row, header line, sticky sortable header, rows,
summary line, footer, status strip. Column set and widths: `CONTRACT.md` §5 — `package` frozen,
then the two foreground numbers, the signed delta, `label`, `visible`, `fgs`, `tail`, the four
network columns, `bucket` and `mAh`. Single-line cells throughout.

Durations render `H:MM` in table cells (`312:00`, `0:41`), signed in the delta, with `+<1m` /
`-<1m` for a nonzero magnitude under a minute. Exact milliseconds live in the expanded row and in
clipboard copy, with no thousands separators anywhere, so a value pastes into a bug report
unedited. When `fg fw` exceeds the window length its cell carries an `exceeds window` chip, in the
same secondary treatment as every other chip; colour is not spent on it, because it is not one of
the six states.

The summary line prints the reconstruction total and the package count. The framework total is
hatched, cause `unclipped bucket`: summing buckets that are individually wider than the window
gives a figure with no window. Per row the framework number still prints, because there it sits
beside its reconstruction and the delta is the finding.

**Degraded columns below API 30.** `:usage` publishes `degradedBelowApi`, and the header says
which columns are structurally unavailable rather than showing an empty column with no
explanation. On API 28 the event stream carries only `MOVE_TO_FOREGROUND` and
`MOVE_TO_BACKGROUND`, so there is no `Visible` state to leave:

```
visible and fgs are unavailable on this device: ACTIVITY_STOPPED and
ACTIVITY_DESTROYED arrive at API 29. Both columns read Absent(29).
```

Below API 30 the shutdown detector falls back to the "no records for a stretch" heuristic and the
lane-chart band is labelled `no data (inferred)`.

| state | what renders |
| --- | --- |
| `Blocked(NoUsageAccess)` | **the full table, populated for one row.** sipper's own package from `queryEventsForSelf`, which needs nothing; every other package as a `Denied` row with real labels and package names from the LAUNCHER `<queries>`. Footer: `1 of n packages readable — usage access is off  [Why sipper needs this]` |
| `Blocked(UserLocked)` | `queryUsageStats returned null. Android returns null rather than an empty list while the user is locked (API 30+). Unlock, then pull to refresh.` |
| `Blocked(NoSamplesYet)` | table renders; the `mAh` column and the lane chart read `sampler has not run yet · first run due 14:32` |
| `Blocked(SamplerOff)` | the sampled column group only; `mAh` cells read `—` with chip `sampler off`. This is a panel-level block and not a `Reading`: the absence of a query result is not a platform answer |
| `Ready` | the grid. `bucket` prints the raw integer then its name — `45 restricted`, `5`, `unknown` — because 5, 15, 45 and 50 all occur and are not public constants |
| `Ready` + no launcher activity | the label cell shows `not visible` with chip `no launcher activity`, drawing the package-visibility boundary in the UI instead of hiding the row |

A row expands in place, at 120 ms via `animateContentSize()`, into the lane chart — a `Canvas`,
unsmoothed, shutdown gaps drawn as gaps and never interpolated — plus the cell-by-cell detail
list, plus the modelled-mAh segment table with its verbatim reason lines
(`wifi 0 · wifi.controller.rx = 0`). Expansion is state, not a destination; see §5.

There is no stacked modelled-mAh bar. A bar normalised over the visible or filtered set is a
length comparison whose meaning changes when the reader types in the filter box. The number in
column 13, the segment table and its reason lines carry the same finding as sortable, copyable
text.

`PullToRefreshBox` is on this screen, because the underlying data changes under the reader.

### 4.4 SELF

Never `Blocked`. Every read-back here is first-party. The only non-`Value` readings are `Absent`:
`getPendingJobReason` is `Absent(34)`, and `getPendingJobReasons` is `Absent` at whichever level
the committed `apiSurfaceCheck` list says, read off `android.jar` at E0 rather than from memory.
On API 28 that panel is two secondary rows plus a line naming which release added each.

Panels: own bucket history, own pending-job reason codes, own `ApplicationExitInfo` with reason
and RSS at death, own modelled mAh from this device's profile, own wake-lock and FGS totals
against the Android vitals threshold, and the sampler run log. The sampler switch lives here,
default on.

The sampler panel prints the real intervals beside the nominal one. Shapes only; every number in
this block is invented and none is a device fact.

```
scheduled every 6 h · last 6 runs 6:00, 6:12, 7:41, 6:00, 9:20, 6:03 apart · bucket RARE
```

Deferral is expected and it is data, which is why the gaps are on the screen. Scheduling, the
6-hour interval and the reason for it are in `docs/ARCHITECTURE.md` §9.

`PullToRefreshBox` is on this screen.

### 4.5 PROBES

Always `Ready`. State lives per row:

```kotlin
enum class ProbeState { Pending, Ok, Denied, Absent, Threw }
```

Layout order: tab row, the paired demonstrations pinned above the table, the sticky header, one
row per read, status strip. Each row is a `ProbeRow` (`CONTRACT.md` §8.3c) carrying both `minApi`
and `behaviourSinceApi`. The column header is `since`, and it renders both when they differ:
`23 · filtered 31+`. One `run` button per row, one for the screen. Denials print the verbatim
runtime line, never a paraphrase: a tool that catches the wrong exception reports a denial as a
removal.

`Absent(minApi)` is reserved for genuine non-existence. Below its `behaviourSinceApi` a pair
renders its title row and one secondary line and stays on screen, so the reader knows what they
are not seeing.

Four paired demonstrations, each two rows under a shared caption. On the ANE-LX2 only pair 1
fires; pairs 2, 3 and 4 render their below-behaviour state there and are captured on the API 36
emulator.

```
getAveragePower on a real absent key, and on a key that does not exist
  cpu.cluster_power.cluster0   hatched   key absent
  sipper.nonsense.key          hatched   key absent

isIgnoringBatteryOptimizations, one package in <queries> and one outside it
  com.android.settings        false   Value          not allowlisted
  com.example.notdeclared     false   SilentDefault  not in <queries>
  Both are false. They are false for different reasons and the platform
  does not distinguish them.
  On API 28: not applicable below API 31 — package visibility filtering
  was introduced there. The call exists at API 23 and returns a real answer.

screen.on.display0 on a profile that defines only screen.on
  profile contains       screen.on          143.0
  sipper stores          screen.on.display0 0.1   Value · route BACK_FILL(screen.on)
  The framework logs this synthesis. It is a real number from a named source,
  so it prints as one.
  On API 28: not applicable below API 34 — initDisplays back-fill begins there.

isAppInactive before and after the usage-access toggle
  com.example.target      false   SilentDefault  no usage access
  com.example.target        -     needs usage access to complete this pair
  On API 28: not applicable below API 30 — the usage-access gate in
  UsageStatsService.isAppInactive begins there. The call exists at API 23.
```

PROBES has no pull-to-refresh. A probe is an action with a result, and the `run` buttons are the
action.

### 4.6 DEVICE

`Ready` with a `Reading` per zone. `/sys/class/thermal` is denied even to shell on the ANE-LX2, so
those rows are `Denied` with the verbatim `EACCES` line; `/sys/devices/virtual/thermal` is the
route that works. Thirteen zones, three unit conventions detected per zone, with zone 2's
`Battery 41000` cross-checked against `dumpsys battery`'s `temperature 410` so the unit detection
is validated against a public API. Battery, RAM beside swap, per-cluster clock.

No pull-to-refresh. DEVICE is milestone E9 and optional; until it lands there are four tabs, not
five.

---

## 5. Navigation

Navigation-Compose, one sealed type, six destinations:

```kotlin
sealed interface Screen {
    val route: String
    data object Audit : Screen { override val route = "audit" }
    data object Apps : Screen { override val route = "apps" }
    data object Self : Screen { override val route = "self" }
    data object Probes : Screen { override val route = "probes" }
    data object Device : Screen { override val route = "device" }
    data object Disclosure : Screen { override val route = "disclosure" }
}
```

splitline deliberately took no navigation library — three screens, one level of back, a
`remember`ed sealed class covered it. sipper takes one for a narrow reason: the disclosure
destination needs a real back-stack entry so the system back button behaves, and the APPS query
state needs `SavedStateHandle`, which `NavHost` gives per entry.

Five tabs in a `PrimaryTabRow`, text-only, not scrollable; height, type and the four permitted
icons are in `CONTRACT.md` §9.4. `NavigationBar` would require an icon per item, and I am not
inventing five glyphs for AUDIT, APPS, SELF, PROBES and DEVICE. `Disclosure` is not a tab; while
it is on top the tab row is hidden.

Tab taps use `popUpTo(Screen.Audit.route) { saveState = true }`, `restoreState = true`,
`launchSingleTop = true`, so switching away from a scrolled, sorted APPS grid and back returns it
as it was, and the back button from any tab goes to AUDIT and then out of the app.

**Row expansion is not a destination.** An expanded APPS row is a set of ids in the screen's
state. Making it a destination would put a package name in a route argument, add a back-stack
entry per expansion, and let the back button collapse rows one at a time, which is not what a back
button means in a table.

---

## 6. What is hoisted, what survives, what is dropped

### 6.1 `SavedStateHandle` — survives configuration change and process death

Per-entry, Bundle-native types only, because the saved-state `Bundle` shares the activity's
transaction ceiling with everything else:

| key | type | why it must survive |
| --- | --- | --- |
| `apps.sort.column` | `String` (enum name) | the reader sorted by the delta for a reason |
| `apps.sort.desc` | `Boolean` | |
| `apps.filter` | `String` | a typed filter lost to a rotation is the classic bug |
| `apps.hideNoLauncher` | `Boolean` | |
| `apps.buckets` | `IntArray` | a `Set<Int>` survives only via `Serializable`, which newer `SavedStateHandle` versions reject on write |
| `apps.windowHours` | `Int` — 6, 24 or 168 | changes what every number means |
| `apps.expanded` | `String` (comma-joined packages, capped at 8) | |
| `audit.groupBy` | `String` — `calculator` or `verdict` | |
| `probes.lastRunAtMono` | `Long` | stops a rotation from re-running every probe |

Sort and filter are one `AppsQuery` in the ViewModel, serialised into those keys on write. The
projection is
`combine(rowsFlow, queryFlow) { r, q -> projectApps(r, q) }.stateIn(viewModelScope, …)`, or
`remember(rows, query) { projectApps(rows, query) }` in composition. Scrolling does not re-sort
because the projection never reads scroll state. `projectApps` is a pure function tested on the
JVM, and the `Set<Int>` of buckets is reconstructed from the `IntArray` in the ViewModel.

### 6.2 Composable saveable state

`rememberSaveable`, not `SavedStateHandle`, because it is view state and it stops meaning anything
if the column set changes:

- the table's horizontal offset in pixels, as a single `Int`. One `TableScrollState` per table,
  hoisted above the sticky header and the `LazyColumn`, with the gesture on the wrapping `Box` and
  rows placed by a layout modifier (`CONTRACT.md` §9.5). There is no second scroll state and no
  synchronisation code.
- `rememberLazyListState()` for vertical position, already saveable.

### 6.3 DataStore Preferences

`disclosure_ack_at`, `disclosure_ack_version`, `sampler_enabled`, and the deep-link branch record
from §2.3. Nothing else. There is no first-run flag, because there is no first-run destination.

### 6.4 Deliberately not persisted

- **Any `Reading`.** Every platform read re-executes on entry. A `Value` restored from a bundle
  would render with its route badge intact without that route having been walked. Restored screens
  start at `Panel.Loading` and mean it.
- **Whether usage access was granted.** Re-checked at the moment of every read. A cached `true`
  survives a revoke in Settings and prints numbers from a query that returned nothing.
- **The current tab.** The app opens on AUDIT every launch. Restoring the last tab makes the
  headline reachable only by memory of where you left it.
- **Any per-app history.** That is SQLDelight's job; the bundle holds a query, not a result.
- **Lane-chart canvas scroll** inside an expanded row. Cheap to recompute, and the row may not be
  expanded after a restore.

### 6.5 How I test it

```bash
adb shell settings put global always_finish_activities 1   # Don't keep activities
adb shell am kill io.github.tahmid1999.sipper.app                    # process death, task retained
```

Eight assertions, run manually on the ANE-LX2 and as a `StateRestorationTester` test in `:app`'s
androidTest:

1. Rotate on APPS with sort by delta descending, filter `com.g`, window 168 h, two rows expanded —
   all five come back.
2. Background, `am kill`, foreground — same five come back, and the table shows `Loading` then
   `Ready` rather than the old numbers.
3. Rotate on the disclosure screen — still there, back stack intact.
4. Return from Settings after granting — APPS repopulates without a manual refresh.
5. Revoke usage access in Settings, return — APPS drops to `Blocked(NoUsageAccess)` within one
   resume, with no stale numbers on screen at any frame.
6. Rotate on PROBES — probes do not re-run.
7. Horizontal offset survives a rotation, and header and rows are still aligned.
8. `fontScale 1.0`, `1.3` and `2.0` on APPS and AUDIT, on API 28, 34 and 36 — rows grow to their
   measured line box, the table widens, and nothing clips. There is no second layout above any
   scale.

adb here is a test rig. No number in the repo comes from it.

---

## 7. Refresh, staleness, the age stamp

### 7.1 Refresh

- `PullToRefreshBox` on APPS and SELF, whose data changes under the reader.
- AUDIT and DEVICE have none. A profile read cannot change while the process lives, so the gesture
  would be theatre.
- PROBES has explicit per-row and all-rows `run` buttons.
- **No auto-refresh timer while a screen is open.** A table that renumbers under the reader's
  finger is a defect in an operator tool. The exception is the battery segment of the status strip,
  bound to the sticky `ACTION_BATTERY_CHANGED` broadcast, which moves when the system says it
  moved.
- On resume after more than 60 s backgrounded, platform reads re-execute once. Sampled data does
  not; refreshing that means waiting for the sampler, and the screen says so rather than spinning.

### 7.2 The age stamp

Right-aligned in each screen's sticky header, at a fixed width so it never reflows.

| age | render | token |
| --- | --- | --- |
| under threshold | `age 0:12` | `textSecondary` |
| over threshold | `age 41:07` | `stateStale` |
| over 24 h | `age 3d 04:11`, plus `stale` in the header and the rows at 60% alpha | `stateStale` |

Thresholds: 5 minutes for platform reads, 20 minutes for sampled data. Format is `H:MM` under a
day and `Nd HH:MM` beyond, the same convention the duration columns use.

**Query age and sample age are different numbers.** A fresh query over three-day-old samples is
stale data. The stamp shows the older of the two and labels it `sample age` when the sample is
what makes it old.

---

## 8. The sampler, on screen

The switch is in SELF, default on. Off puts the sampled column group into `Blocked(SamplerOff)`
(§4.3) and leaves everything else alone.

Each run writes one `sampler_run` row — `startedAt`, `durationMs`, `stopReason`
(`ListenableWorker.getStopReason()` on API 31+, `isStopped` below it) and the previous run's
pending-job reason. Those rows are what SELF's run-log panel prints, nominal interval beside real
ones (§4.4).

The `PeriodicWorkRequest` itself — 6-hour interval, 1-hour flex, `KEEP`,
`setRequiresBatteryNotLow(true)`, linear 30-second backoff, never a foreground service, never a
`PARTIAL_WAKE_LOCK` — is specified in `docs/ARCHITECTURE.md` §9 along with the reason for the
interval. SELF renders sipper's own wake-lock and FGS totals against the Android vitals threshold,
so a reader can falsify the claim on their own phone.

---

## 9. Accessibility

A dense monospace table is the hostile case for a screen reader, and none of this is free.

**Every cell's spoken form comes from the same function that renders it.** `describeReading()`
lives beside `ReadingCell`, so the two cannot drift. The spoken form always includes the column
name, because a reader moving cell by cell has no header context:
`"reconstructed foreground, 4 hours 12 minutes"`, never `"4 colon 12"`.

**Machine formats are expanded for speech and only for speech.** `26:04` speaks as
`26 hours 4 minutes`; `143.0` speaks as `143 point 0 milliamps`; `409000` speaks as
`409000 microamp hours`; `Δ +2:04` speaks as
`delta, plus 2 hours 4 minutes, framework above reconstructed`.

**Row granularity.** Each row is one merged semantics node with an ordered description built from
the column titles, carrying the package label plus the three columns that hold the finding —
framework foreground, reconstructed foreground, delta. A hatched cell announces
`manufactured value, cause key absent from profile` and never the number. Thirteen cells in one
utterance is unusable, and cell-by-cell reading has a home: the expanded row's linear
`label: value` list, which a screen reader handles natively.

**Table structure.** `CollectionInfo(rowCount = rows.size, columnCount = 1)` on every table list,
because what TalkBack navigates is a list of merged rows; `collectionItemInfo` on each row;
`heading()` on the sticky header. Declaring thirteen columns against merged rows makes TalkBack
announce a column index for a node that is the whole row.

**Colour is never the only carrier.** All four verdicts are words in the `verdict` column —
`PRESENT`, `ZERO-BY-EXPLICIT-VALUE`, `ZERO-BY-ABSENCE`, `BACK-FILLED`. The hatch carries a text
chip. The agreement badge says `3/3 agree` in words.

**Touch targets.** A row is not a small control: the target is the full row width, 360 by its
declared height, which clears WCAG 2.5.8 at 24 by 24 and knowingly fails Material's 48dp guidance.
That is stated rather than worked around. Genuinely standalone controls — the sort caret, the
PROBES `run` buttons, the row expander, the status-strip segments — get 48dp via
`minimumInteractiveComponentSize()`.

**Text scaling.** Row height is measured, never multiplied by `fontScale`: Android applies
non-linear font scaling above roughly 1.3 from API 34, so a single multiplier is wrong for at
least one style in the row. Heights and widths are derived by measuring the actual line box and
digit advance (`CONTRACT.md` §9.1, §9.5). Column widths stay in dp with `TextOverflow.Ellipsis`,
and the full untruncated value is in both the content description and the expanded row. There is
no stacked-row fallback at any font scale; the table widens and scrolls further.

**Direction.** Tables are LTR-locked, and the status strip is inside the lock — it is machine data
end to end. Every cell sets `TextDirection.Ltr` explicitly, because package names, keys, hex ids
and exception strings are LTR data. The `label` cell is the exception: it sets
`TextDirection.Content`, so a native-script app name renders correctly inside its LTR-positioned
cell.

Colour tokens and their measured contrast are in `CONTRACT.md` §1, regenerated by
`./gradlew :app:contrastDoc`. No ratio is typed into a document by hand.

---

## 10. The status strip

Bottom of every screen, above the navigation bar, 32dp, on `surfaceRaised` with a 1dp `ruleStrong`
rule on top, monospace, ` · ` between every segment. Height and type: `CONTRACT.md` §9.4.

Shapes only; the battery segment's numbers in this block are invented and none is a device fact.

```
ANE-LX2 · API 28 · EMUI 9.1 · routes 3/3 agree · usage-access OFF · queries launcher · 24 keys @ 0 · 78% 3.76V 41.0C
```

At the bottom because that leaves the top of every screen for the sticky table header, which is
the thing that has to stay pinned. 32dp with internal padding gives each segment a 48dp target
that stays inside the strip's own bounds; a shorter strip with a target overlapping the content
above it would make the bottom-most table row untappable, and on APPS that row is expandable.

Every segment is tappable and navigates to the screen that owns it: `routes n/n` and `24 keys @ 0`
to AUDIT, `usage-access` and `queries` to PROBES, the device and battery segments to DEVICE.
Overflow scrolls horizontally; it is never truncated and never wrapped.

Segments update independently — battery from the sticky broadcast, `routes n/n` from AUDIT's last
read, `usage-access` from the app-op check on every resume, `24 keys @ 0` from the audit result. A
segment with no value yet renders `—` rather than disappearing, so the strip does not reflow under
the reader.

---

## 11. What this document does not settle

- Which deep-link branch fires on EMUI 9.1. PROBES renders empty cells for it until the ANE-LX2
  says otherwise, and `NOTES.md` gets the line the evening it does.
- Whether app-uid can read `/sys/devices/virtual/thermal` and `cpufreq` on this device. Probed at
  runtime, reported on PROBES, and DEVICE's states are written for both answers.
- Whether `BATTERY_PROPERTY_CURRENT_NOW` returns anything real on the ANE-LX2.
- The exact API level of `getPendingJobReasons`, until it is read off `android.jar` at E0 and
  committed to the `apiSurfaceCheck` list. SELF renders whatever that list says.
