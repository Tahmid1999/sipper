# Screens: AUDIT, PROBES

> `docs/CONTRACT.md` fixes tokens, widths, types and the API levels below. This file owns the
> behaviour of the AUDIT and PROBES screens: layout order, states, flows, navigation, saved state,
> copy strings. Where they disagree, the contract wins.

Two screens, specified together because they are the two a stranger sees. AUDIT is the screenshot at
the top of the README's second section. PROBES is where this repo publishes its own failures, in the
platform's own words, on the reviewer's own phone.

Written against `sipper.md` §2, §3, §4, §6 and §7. Every number quoted here as measured comes from
there; nothing here introduces a new device fact. Every ASCII mock below carries a line saying
whether its numbers are device facts or shapes.

Colour tokens and their contrast: `docs/CONTRACT.md` §1. Type roles, spacing, row heights, the hatch
and the `ScrollTable` primitive: `docs/DESIGN.md`. `Reading<T>`, `Verdict` and the state types:
`docs/ARCHITECTURE.md`.

The rules in `no-ai-looking-code-and-ui` apply throughout and are not restated. The reference points
are Bull Board and a Sentry issue list.

---

## 0. Shared surface

Both screens sit inside the same chrome, and neither owns it.

### 0.1 Status strip

One line at the bottom, above the navigation bar. 32dp, `surfaceRaised`, 1dp `ruleStrong` along the
top, `monoStrip`, ` · ` between every segment — no ` | ` grouping. Segments are at least 48dp wide
and tappable, and each navigates to the screen that owns it. The strip scrolls horizontally when it
overflows; it is never truncated and never wrapped, because a truncated strip lies about the segment
it cut.

*Shapes only; every number in this block is invented and none is a device fact.*

```
ANE-LX2 · API 28 · EMUI 9.1 · routes 2/3 agree 1 blocked · usage-access OFF · queries launcher · 24 keys @ 0 · 78% 3.76V 41.0C
```

| segment | source | absent or failed renders as | tap |
|---|---|---|---|
| `ANE-LX2` | `Build.DEVICE`, as the device reports it | never absent | DEVICE |
| `API 28` | `Build.VERSION.SDK_INT` | never absent | DEVICE |
| `EMUI 9.1` | `ro.build.version.emui` and the vendor property list | segment omitted entirely, not `null`, not `AOSP` | DEVICE |
| `routes 2/3 agree 1 blocked` | `Agreement` | `routes 0/3 no profile` | AUDIT, scrolled to the provenance block |
| `usage-access OFF` | the appop, read through `:collect`'s branched check (CONTRACT §8.3a) | never absent | PROBES, section B |
| `queries launcher` | build-time constant generated from the manifest | never absent | PROBES, pair 2 |
| `24 keys @ 0` | count of probe-set keys whose effective value is zero on this device | `— keys @ 0` while reading | AUDIT |
| `78% 3.76V 41.0C` | last `ACTION_BATTERY_CHANGED` | `no battery broadcast` | DEVICE |

Colour: every segment `textSecondary`, except `usage-access OFF` and any blocked route in
`stateDenied`, and any route disagreement in `stateAbsentZero`. The `n keys @ 0` figure is computed
on the device in hand; the README's "24 of 47" is the API 36 measurement and the two are not the
same claim.

### 0.2 Navigation

`PrimaryTabRow` at the top, 48dp, five text-only tabs: `AUDIT` `APPS` `SELF` `PROBES` `DEVICE`. Not
scrollable; `PROBES` is the widest and five fit at 360dp. No `TopAppBar` — there is no title to put
in one and no action that belongs there.

AUDIT is the start destination on every launch including the first. No first-run-only destination
exists anywhere in the app: a destination reachable once per install never gets tested. PROBES is
reached from its tab, from the `usage-access` and `queries` segments of the status strip, and from
`[ open PROBES ]` in AUDIT's populated-profile state.

### 0.3 The one rendering rule

Every platform-sourced value on both screens goes through `ReadingCell` (CONTRACT §2.1), which is
the only composable that takes a `Reading`. `Value` prints. `Denied` prints the permission name.
`Absent` prints `—` in `textSecondary`, with the minimum API in the expanded row. `SilentDefault`
prints no number at all: a hatched cell — 45°, 1dp stroke, 5dp pitch along x, `hatchLine`, no state
hue — with the cause chip immediately to its right in `textSecondary`. A coloured cell reads as a
value, and that cell must not read as a value.

The manufactured number appears twice on these two screens and nowhere else: in an expanded detail
row, prefixed `platform returned:`; and in the PROBES paired demonstrations, where both halves
render the platform's return identically so the quote cannot be read as sipper's own number.

---

# 1. AUDIT

**Purpose:** show what the power profile on this device actually contains, by three independent read
routes, and name the mechanism behind every zero.

Zero permissions. No usage-access banner appears here ever — AUDIT has no gate, and a "grant access"
prompt would imply the audit is degraded when it is complete.

## 1.1 Layout, top to bottom

*Shapes only; every number in this block is invented and none is a device fact.*

```
+------------------------------------------------------------------------------------------+
|  AUDIT      APPS      SELF      PROBES      DEVICE                                        | 48
+------------------------------------------------------------------------------------------+
| provenance                                             3 routes · 2 agree · 0 blocked     | 26
| route                        resolved                       keys   digest     read        | 24
| Resources.getSystem()        0x01110003 xml/power_profile     34   4f1ac9e2    8 ms       | 30
| getResourcesForApplication   android · xml/power_profile      34   4f1ac9e2   11 ms       | 30
| PowerProfile reflection      android.os.PowerProfile          20   a71c33d8   46 ms       | 30
| file /product/etc/xml/power_profile.xml    informational      20   a71c33d8  readable     | 30
|   routes disagree on 18 of 20 shared keys.  [ diff ]                                      | 26
+------------------------------------------------------------------------------------------+
| battery.capacity                                                                  pinned  | 26
| res.system 1000 · res.pkg 1000 · reflection 3000 · counter 409000 µAh at 78%               | 30
| no ratio is computed between these four. the counter's unit is vendor-reported.            | 22
| verdicts below are computed from framework-res.apk. dumpsys pws reports 3000 on this       | 34
| device, so the framework is pricing from /product/etc/xml and these verdicts describe a    |
| file it does not read.                                                                     |
+------------------------------------------------------------------------------------------+
| 34 keys · 10 present · 6 zero in file · 18 absent · 0 back-filled     group: [calc] verdict| 28
+------------------------------------------------------------------------------------------+
| key                  | verdict     value  =  from      effect                             | 30 sticky
|======================|===================================================================|
| CpuPowerCalculator · 11 keys · 6 at 0                                                     | 24 sticky
| cpu.active           | PRESENT     157.0     res       priced                             | 32
| cpu.cluster_power... | ZERO-BY-A…      —     —         cluster power unpriced             | 32
| ...                                                                                       |
+------------------------------------------------------------------------------------------+
| profile digest 4f1ac9e2 · read 19:42:07 · schema pre-lollipop(20) / modern(34)             | 26
+------------------------------------------------------------------------------------------+
| ANE-LX2 · API 28 · EMUI 9.1 · routes 2/3 agree 1 blocked · usage-access OFF · ...          | 32
+------------------------------------------------------------------------------------------+
```

The tab row and the status strip never scroll. The provenance block, the pinned capacity row and the
counts line scroll off under the sticky table header.

## 1.2 Provenance block

Four rows: three read routes and one informational file probe that is **not** a route. Route values
live here rather than as three numeric columns in the key table, so the disagreement story is told
once, where the routes are named.

This block does not scroll horizontally, so `weight` is safe in it; it wraps the `resolved` column
instead, because a route's identity is prose and a wrapped line reads better than a scrolled one.

| header | width | align | content |
|---|---|---|---|
| `route` | 148dp fixed | left | `Resources.getSystem()`, `getResourcesForApplication`, `PowerProfile reflection`, `file <path>` — wraps to 2 lines |
| `resolved` | weight 1, min 120dp | left | resource id as 8-digit hex with `0x` and the type/name it resolved to; for reflection the fully qualified class; for the file the absolute path, middle-ellipsised |
| `keys` | 44dp | right | scalar key count. Array keys (`cpu.speeds.cluster0`, `memory.bandwidths`, `cpu.clusters.cores`) are counted separately and shown in the expanded row, never folded into this number |
| `digest` | 68dp | left | first 8 hex of sha256 over the normalised `key=token` set, sorted, newline-joined. Normalisation trims whitespace and does not parse to `Double`, because `0` and `0.0` must not collide |
| `read` | 60dp | right | elapsed ms, integer. On failure the cell prints the failure word instead: `blocked`, `absent`, `throw` |

Header line, right side: `n routes · n agree · n blocked`. The denominator is always the number of
routes attempted, and `agree` counts routes that returned and matched. It never prints `3/3 agree`
when a route was blocked — see §1.6.

The fourth row names what the app can see without claiming what it means:

> `file /product/etc/xml/power_profile.xml` · `informational` · `a71c33d8` · `readable`

with a caption under the block:

> The framework's resolution order is not a public API. sipper reads this file, does not claim it is
> in force, and cannot settle it from inside an app — that needs a `pws` row from
> `dumpsys batterystats --checkin`, which needs DUMP.

## 1.3 Capacity cross-check, pinned

Pinned above the first group on `surfacePinned` with a 2dp `accent` left edge. It never joins the
key table, because it is the one key with a fourth independent source and a per-calculator group
would bury it.

*`1000`, `3000` and `409000 µAh` are device facts from `sipper.md` §2; the percentage is invented.*

```
battery.capacity                                                                     pinned
res.system      1000        res.pkg      1000        reflection      3000
charge counter  409000 µAh  BATTERY_PROPERTY_CHARGE_COUNTER at 78%

no ratio is computed between these four. the counter's unit is vendor-reported.
```

Four values, each under its own label, laid out as a 2×2 grid below 400dp and a 1×4 row above it.
When any two disagree the disagreeing values take `stateAbsentZero`; here `1000` and `3000` do. The
last line is not optional: dividing 409000 by 0.78 would project a full-charge figure out of a
percentage the platform already rounded to an integer.

**The primary-route caveat.** Route B, `getResourcesForApplication("android")`, fills the `value`
column and carries the verdicts. On any device where the routes disagree, a persistent line sits
directly under the capacity row:

> `verdicts below are computed from framework-res.apk. dumpsys pws reports 3000 on this device, so`
> `the framework is pricing from /product/etc/xml and these verdicts describe a file it does not read.`

That is the first question a reviewer asks about this screen, and it is answered on the screen
rather than in the README.

## 1.4 Counts line and grouping control

One line. No cards, no tiles, no icons:

```
n keys · n present · n zero in file · n absent · n back-filled       group: [calc] verdict
```

Each count is tappable and filters the table to that verdict; tapping the active one clears the
filter. The active filter carries a 1dp underline in its verdict colour, not a filled pill.

`group:` is a two-option text switch, `calc` (default) and `verdict`. Two, not three — a third
grouping turns this into a settings screen, and grouping by the `PowerCalculator` that reads the key
at this API level is the grouping that carries the argument.

There is no search field. A profile is tens of rows on a phone and scrolling beats typing. There is
no modelled mAh here either; that is APPS, and an estimate on the audit screen would borrow the
audit's credibility.

## 1.5 The key table

`ScrollTable` (DESIGN's primitive): one gesture node above the header, rows and header layout-only
inside it, the frozen column outside the shifted region, the frozen edge drawn rather than composed.
Six columns, one frozen, every width intrinsic — no `weight` and no `fillMaxWidth` anywhere inside
the shifted region, because a weighted child inside it measures at zero and vanishes.

| pos | column | chars | dp | align | content |
|---|---|---:|---:|---|---|
| frozen | `key` | 22 | 170 | left | profile key, **middle**-ellipsised — the discriminator is the suffix (`screen.on.display0` against `screen.on`) |
| 1 | `verdict` | — | 100 | left | verdict chip, `monoChip`, 1dp border in the verdict colour, no fill, 3dp radius |
| 2 | `value` | 8 | 70 | right | the literal token from the XML, not a formatted `Double`; `—` when absent |
| 3 | `=` | 2 | 26 | centre | `=` in `textSecondary` when the routes agree, `≠` in `stateAbsentZero` when they do not |
| 4 | `from` | 12 | 98 | left | `res`, `back-fill`, or `—`; never `reflect`, because reflection carries no claim |
| 5 | `effect` | 28 | 214 | left | `priced`, `cluster power unpriced`, `no pwi row on this device`; end-ellipsised, full text in the expanded row |

Frozen 170 + scrolling 508 + 10dp lead + 2dp frozen edge + 10dp trailing = **700dp**. At scroll
offset 0 on a 360dp viewport, at the default font scale: 10 + 170 + 2 + 100 + 70 = **352dp**, so key,
verdict and value are on screen before any scroll.

Rows are 32dp, header 30dp, group header 24dp and sticky:

```
WifiPowerCalculator · 4 keys · 2 at 0
```

On a device where the calculator does produce a `pwi` row, the `effect` cell says `priced` and the
`no pwi row on this device` string is absent rather than replaced by `pwi row present` — absence is
the finding and presence is the baseline.

Verdict rendering, exactly:

| verdict | chip colour | `value` cell |
|---|---|---|
| `PRESENT` | `statePresent` | literal token |
| `ZERO-BY-EXPLICIT-VALUE` | `stateExplicitZero` | literal `0` or `0.0` as written in the file |
| `ZERO-BY-ABSENCE` | `stateAbsentZero` | `—` |
| `BACK-FILLED` | `stateBackFilled` | the real number, plus a `back-filled from screen.on` provenance chip |

Four hues, no two shared. `ZERO-BY-ABSENCE` never renders `0` even though `getAveragePower` returns
`0.0` for it.

A back-filled key is a `Value` whose route kind is `BACK_FILL`, and it is **never hatched**: the
framework logs the synthesis, so nothing about it is silent, and a hatched cell containing a real
number would make the hatch mean two things.

Keeping `ZERO-BY-EXPLICIT-VALUE` and `ZERO-BY-ABSENCE` apart is not pedantry about wording. An
explicit `0` in a key that a calculator divides by gives `Infinity`; an absent one gives `0.0`. The
two zeros propagate differently.

Four verdicts render. A key whose only route was blocked renders as a hatched cell with a
`route blocked` chip rather than as a verdict, because "we could not look" is not a finding about
the file.

## 1.6 Expanded key row

Tapping a row expands it in place on `surfacePinned`, with `animateContentSize()` at 120ms. No
dialog and no bottom sheet — a sheet hides the neighbouring rows, and the neighbours are the
comparison.

*Shapes only; the citation path and line are illustrative until the E1 verification log lands.*

```
cpu.cluster_power.cluster0
  res.system      absent
  res.pkg         absent
  reflection      platform returned: 0.0     getAveragePower fell through to defaultValue
  verdict         ZERO-BY-ABSENCE — the key is not in the file. getAveragePower(String) falls
                  through to `return defaultValue` with no throw and no Slog.
  read by         CpuPowerCalculator
  citation        frameworks/base/.../CpuPowerCalculator.java:118  android-14.0.0_r1
                                                                          [ copy row ]
```

This is one of the two places the manufactured number is rendered at all, and it is prefixed
`platform returned:` and left-aligned in `textSecondary` so it reads as a quote.

For a `BACK-FILLED` key the `verdict` paragraph is replaced by the chain:

```
  chain           screen.on.display0  ←  screen.on (0.1)
                  initDisplays() synthesises the per-display keys from the deprecated ones on
                  API 34+ and logs "screen.full is deprecated". The value is real; the key is
                  not in the file. A naive audit calls this PRESENT.
```

## 1.7 States

**First launch.** No different from any later launch. AUDIT needs no permission, no toggle and no
stored data, so it has no onboarding state, no empty state and no "get started" affordance.

**Loading.** The read runs off the main thread. Below 120ms nothing changes. Beyond that the table
renders its real header and layout-true skeleton rows — a 1dp `rule` underline at each cell's real
character width — and the status strip carries a determinate count, `reading 12/47`, so a stall
names the key that hung. No indeterminate indicator anywhere, and no spinner.

**Routes disagree.** The default state on the ANE-LX2: the resource routes and reflection return
different values for most of the keys the two sides share. Badge reads
`3 routes · 2 agree · 0 blocked` with the disagreement count in `stateAbsentZero`. Under the block:

> routes disagree on `n` of `n` shared keys.  `[ diff ]`

`[ diff ]` expands inline:

*`1000`/`3000`, `screen.on 143`, `screen.full 414` and `dsp.audio 43` are device facts from
`sipper.md` §2; the comparison counts are shapes.*

```
res.system + res.pkg agree (framework-res.apk).  reflection differs.
the value column follows the resource routes. reflection is a cross-check and never carries
a claim here — on this device it is also the route that matches what the framework prices.

key                       res    reflect
battery.capacity         1000       3000
screen.on                 0.1        143
screen.full               0.1        414
dsp.audio                   —         43      only in reflect
cpu.clusters.cores          1          —      only in res
```

Three columns: `key` middle-ellipsised, `res` right, `reflect` right, with an `only in <route>` tag
in `textSecondary` on rows present on one side. Keys equal on both sides are not listed; the header
says how many were compared. Sorted by key name, not by magnitude of difference — sorting by
difference would imply a severity ranking, and I have no basis for saying an absent `cpu.suspend` is
worse than an absent `audio`.

**Reflection denied.** Hidden-API enforcement surfaces as `NoSuchMethodException`, not
`SecurityException`. The route row becomes:

```
PowerProfile reflection   android.os.PowerProfile    —    —    blocked
  java.lang.NoSuchMethodException: android.os.PowerProfile.getAveragePower [class java.lang.String]
```

Badge reads `3 routes · 2 agree · 1 blocked`, and `blocked` appears in the status strip too. It never
collapses to `2/2 agree`, which would report a denial as a consensus. Caption under the block,
present whenever any route is blocked:

> A blocked hidden-API member throws `NoSuchMethodException`, not `SecurityException`. A tool
> catching the wrong one reports a denial as "method removed".

Nothing else degrades: the resource routes carry the claim, so the verdicts, the counts and the
table are complete without reflection.

**No profile at all.** `getIdentifier` returns 0 on both resource routes and reflection throws.
Full-screen:

> No `power_profile` resource on this device.
>
> `Resources.getSystem().getIdentifier("power_profile", "xml", "android")` returned 0, and
> `getResourcesForApplication("android")` resolved the same. I have not seen this on either test
> device and I would like the `probes` report if you hit it.
>
> `[ copy probe report ]`

**Populated profile, nothing to report.** The Pixel state, designed rather than defaulted. The
provenance block and the capacity block stay — they are measurements, and hiding them makes the
screen look like it did nothing. The table body is replaced:

> **Nothing to report on this device.**
>
> Every key in this release's set is present in the file. No key resolves to zero by absence. The
> three read routes agree. This is what I expect on most current hardware and it is not a failure of
> the audit.
>
> What this screen cannot tell you is whether the profile is used at all. Where measured subsystem
> energy is available through the PowerStats HAL, EnergyConsumers replace the profile for subsystem
> totals, and no public API reports which power model is in force. sipper does not guess, so there
> is no verdict here about attribution.
>
> Three of the four instances behind this app are live on this device anyway. They are on the
> `PROBES` screen.
>
> `[ show the table anyway ]`   `[ open PROBES ]`

`[ show the table anyway ]` renders the full table with every row `PRESENT`. Both are text buttons,
Material default, side by side, left-aligned; a centred pair of buttons under a headline is the
landing-page shape.

**API 36, no loose file.** The file probe row reads:

```
file /product/etc/xml/power_profile.xml   informational   —   —   absent (ENOENT)
```

with the caption `not a finding on this device; that path is vendor-specific`. The row is not hidden
when the file is absent — hiding a probe that came back negative is how a screen starts reporting
only the answers it likes.

## 1.8 Gestures

- Route row — expands: full resource id, `ApplicationInfo.sourceDir` and `splitSourceDirs`, full
  digest, array-key counts, the exception with its stack frame if any.
- The agreement badge — scrolls to and expands the diff.
- `[ diff ]` — inline diff, §1.7.
- Any count in the counts line — filters to that verdict.
- `group: calc | verdict` — regroups. Scroll position resets to top, which is correct: the previous
  offset means nothing in the new grouping.
- Group header — collapses that calculator's rows. Collapsed state survives rotation via
  `rememberSaveable` and is not persisted across launches.
- Key row — expands, §1.6.
- Long-press any mono value — copies it. `Snackbar` default, text `copied battery.capacity=1000`.
- Overflow (`MoreVert`): `share audit as text` (`ACTION_SEND`, no file written), `copy digest`,
  `about the four verdicts`.

Nothing else responds to touch. No swipe actions and no pull-to-refresh: the profile cannot change
while the process lives, so the gesture would be theatre.

## 1.9 The screenshot

The README's AUDIT capture is the ANE-LX2, light theme, scrolled to the top, diff expanded, first
calculator group visible under it. It must show, in one frame: three routes with two digests, the
`≠` on `battery.capacity`, `1000` against `3000` against `409000 µAh`, the primary-route caveat
line, and at least one `ZERO-BY-ABSENCE` row with `—` in the value column. Notification shade clear,
no personal app names in the frame, clock left as it falls. No device bezel mockup, no drop shadow,
no annotation arrows.

---

# 2. PROBES

**Purpose:** run every read this app depends on, on this device, right now, and print what came
back including the failures, verbatim.

PROBES is a tab. It is built at E6 — that is what "rendered first, before anything works" means in
the spec, and it is build order, not start destination.

## 2.1 Layout, top to bottom

*Shapes only; every number in this block is invented and none is a device fact.*

```
+------------------------------------------------------------------------------------------+
| AUDIT   APPS   SELF   PROBES   DEVICE                                                     | 48
+------------------------------------------------------------------------------------------+
| probes   47 reads · 31 value · 9 denied · 4 absent · 3 silent-default                     | 26
| run 19:42:07 · 214 ms                                                     [ re-run all ]  | 24
+------------------------------------------------------------------------------------------+
| paired demonstrations                                                                     | 26
| +--------------------------------------+-------------------------------------------+     |
| | pair 1                               |                                           |     | 140
| +--------------------------------------+-------------------------------------------+     |
| | pair 2                               |                                           |     | 140
| ...                                                                                       |
+------------------------------------------------------------------------------------------+
| read                 | result        value  detail                    since      run     | 30 sticky
| A · zero permissions                                                       14 reads       | 30 sticky
| ...                                                                                       |
| B · after usage access                                    13 reads        [ grant ]       | 30 sticky
| C · impossible for any installed app                                        6 reads       | 30 sticky
| D · open at write time, resolved by this run                                3 reads       | 30 sticky
+------------------------------------------------------------------------------------------+
| targetSdk 36 · queries MAIN/LAUNCHER, com.android.settings · bench/queries-diff.txt       | 26
+------------------------------------------------------------------------------------------+
| status strip                                                                              | 32
+------------------------------------------------------------------------------------------+
```

The pairs come before the capability table because they are the argument and the table is the
evidence for it, and because the README's lead capture is a pair.

## 2.2 Header

```
probes   n reads · n value · n denied · n absent · n silent-default
run --:--:-- · n ms                                                          [ re-run all ]
```

The four counts use the sealed constructor names — `value`, `denied`, `absent`, `silent-default` —
not friendly synonyms, because those four words are the type and a reader who learns them here can
read the source. Coloured `statePresent`, `stateDenied`, `textSecondary`, `textSecondary`. Zero
counts render as `0`, never omitted.

## 2.3 The paired demonstrations

A pair block is one region, 1dp `rule` border, no elevation, no radius, full content width less the
gutters, minimum height 132dp. This block is not inside a horizontally shifted region, so its two
halves are 50/50 by `weight(1f)`, split by a 1dp `ruleStrong` vertical hairline running the full
block height.

**The layout rule that matters:** both `platform returned` lines are drawn identically — one style,
same colour, same left offset inside their half, same baseline. **The returned value is never
coloured, never hatched and never marked**, on either side. Colouring the right-hand value would
tell the reader they differ before they have read anything. Everything that differs sits below the
value: the `sipper stores` lines, and on a manufactured side the cause strip, which is the only
hatched region in the block.

Per half, four lines:

```
arg                 <label>
                    <value>              ← platform returned, identical both sides
--------------------------------------------------  1dp rule, full half width
sipper stores       Value
                    route PACKAGE_MANAGER
```

The constructor name and the cause go on two lines rather than one:
`SilentDefault(false, cause = PACKAGE_NOT_VISIBLE)` does not fit a half-width cell at 360dp. **The
pair never stacks vertically at any width.** Below 340dp of content width the value drops one step
and the cause line wraps to at most three lines; a stacked pair reads as two unrelated rows.

Tapping anywhere in a half expands a full-width detail region under both: the exact call, the
argument, the AOSP mechanism with file and branch, the API the call was added at, and the API the
behaviour begins at.

Each block carries a `since` line rendering both numbers when they differ — `23 · filtered 31+`.
Below its behaviour API the block renders its title row and one `textSecondary` line and stays on
screen, so the reader knows what they are not seeing.

**Pair 1 — `getAveragePower(String)`.** First because it fires on every device with no permission
and no manifest entry, and it is the README's lead capture, recorded on the ANE-LX2.

*Both `0.0` values are device facts; the key names are the ones the app probes.*

```
PowerProfile.getAveragePower(key)                                    since 21

arg  cpu.suspend                     |  arg  nonsense.key.that.cannot.exist
     defined by the platform         |       defined nowhere
platform returned                    |  platform returned
0.0                                  |  0.0
-------------------------------------|--------------------------------------------------
sipper stores                        |  sipper stores
SilentDefault                        |  SilentDefault
cause KEY_ABSENT_FROM_PROFILE        |  cause KEY_ABSENT_FROM_PROFILE

same value, same cause, and from inside an app there is no third answer available. A key the
platform defines and this device omits, and a key that has never existed on any device, are one
return value: getAveragePower falls through to `return defaultValue` with no throw and no Slog.
```

Both halves land on the same constructor, which is the demonstration. sipper cannot tell these two
questions apart either; what it can do is refuse to print `0.0` as a measurement of `cpu.suspend`.

**Pair 2 — `isIgnoringBatteryOptimizations(pkg)`.** The one that is a manifest entry apart. API 36
emulator capture; the filtering does not exist on the ANE-LX2.

*Shapes only; both returns are `false` by construction, and the package names are the real ones.*

```
PowerManager.isIgnoringBatteryOptimizations(pkg)                     since 23 · filtered 31+

arg  com.android.settings            |  arg  com.android.vending
     in <queries>                    |       not in <queries>
platform returned                    |  platform returned
false                                |  false
-------------------------------------|--------------------------------------------------
sipper stores                        |  sipper stores
Value                                |  SilentDefault
route PACKAGE_MANAGER                |  cause PACKAGE_NOT_VISIBLE

same value, different cause. On the right the package is filtered out of this app's view and
AppOpsService returns opToDefaultMode(code) rather than a filtered marker. Not allowlisted and
not visible are the same value.
```

`com.android.settings` is declared in `<queries>` as the control and `com.android.vending` is
deliberately not declared. Both are present on both test devices. `bench/queries-diff.txt`, linked
in the footer, is the two-build diff showing the right half move when the manifest entry is added.

On API 28 the block renders its title row and one `textSecondary` line:
`not applicable below API 31 — package visibility filtering was introduced there. the call exists
here and returns a real answer.`

**Pair 3 — the back-fill.** The same trick pointing the other way: a real value under a key that is
not in the file. API 36 emulator capture.

*Shapes only; `0.1` is the AOSP placeholder value the emulator profile carries.*

```
PowerProfile.getAveragePower(key)                                    since 21 · back-fill 34+

arg  screen.on                       |  arg  screen.on.display0
     in the xml                      |       not in the xml
platform returned                    |  platform returned
0.1                                  |  0.1
-------------------------------------|--------------------------------------------------
sipper stores                        |  sipper stores
Value                                |  Value
route SYSTEM_RESOURCES               |  route BACK_FILL(screen.on)

same value, one read from the file and one synthesised by initDisplays() on API 34+. The platform
logs the synthesis, so this is not a silent default and is not hatched anywhere in the app — it is
a real number under a key the file does not contain. A naive audit calls the right-hand key
PRESENT.
```

Below API 34 the block renders its title row and one `textSecondary` line:
`not applicable below API 34 — initDisplays() was introduced there`.

**Pair 4 — `isAppInactive(pkg)`.** Temporal rather than simultaneous, and the awkwardness is on the
screen rather than hidden. API 36 emulator capture.

*Shapes only; the timestamps are invented.*

```
UsageStatsManager.isAppInactive(pkg)                                 since 23 · silent 30+

arg  com.android.settings            |  arg  com.android.settings
     with usage access               |       without usage access
platform returned                    |  platform returned
false                                |  false
-------------------------------------|--------------------------------------------------
sipper stores                        |  sipper stores
Value                                |  SilentDefault
route USAGE_EVENTS · 19:42:07        |  cause NO_USAGE_ACCESS · recorded 19:31:44
```

There is no way to hold both conditions at one instant, so the right half is a recorded reading from
before the toggle, stamped with its own time. When usage access is off the left half reads:

> `grant usage access to fill this half`  `[ grant ]`

When it is on and no pre-grant reading exists — a fresh install where access was granted before
PROBES first ran — the right half reads:

> no reading recorded before access was granted. revoke usage access in Settings and re-run to see
> this half.  `[ open Settings ]`

On API 28 the block renders its title row and one `textSecondary` line:
`not applicable below API 30 — the usage-access gate on isAppInactive was introduced there. the
call exists here and returns a real answer.`

Three of the four pairs are emulator captures, because the one physical device predates the
mechanisms they show. The README's known-limitations block says so.

## 2.4 The capability table

Same `ScrollTable` construction as AUDIT: one gesture node, layout-only rows, frozen first column,
every width intrinsic. Rows 44dp, header 30dp. Four sticky section headers. The sections are the
argument's structure and do not collapse into one sortable list — sorting them together would let a
`Denied` in section C read as a bug.

| pos | column | chars | dp | align | content |
|---|---|---:|---:|---|---|
| frozen | `read` | 20 | 156 | left | the call as written in source, middle-ellipsised |
| 1 | `result` | 14 | 113 | left | `Value` `Denied` `Absent` `SilentDefault`, coloured `statePresent` `stateDenied` `textSecondary` `textSecondary`; `…` while in flight |
| 2 | `value` | 8 | 70 | right | the returned value, or `—`. A `SilentDefault` cell is hatched with a cause chip and prints no number |
| 3 | `detail` | 30 | 228 | left | route for a `Value`; the verbatim `Throwable.toString()` for a `Denied`; `howToGrant` under it. End-ellipsised here, wrapped in full in the expanded row |
| 4 | `since` | 17 | 134 | left | the API the call was added at, and the API the mechanism begins at when they differ: `23 · filtered 31+` |
| 5 | `run` | 5 | 48 | centre | per-row text button |

Frozen 156 + scrolling 593 + 10 + 2 + 10 = **771dp**. At offset 0 on 360dp at the default font
scale: 10 + 156 + 2 + 113 + 70 = **351dp**, so the call, the constructor and the value are on screen
before any scroll.

The header is `since`, not `api`, and `Absent(minApi)` is reserved for genuine non-existence —
`getPendingJobReason` 34, `getHistoricalProcessExitReasons` 30, `FOREGROUND_SERVICE_START` 29.
Rendering "requires API 31, this device is 28" for a method that exists on 28 and answers would be a
manufactured availability claim, on the screen whose subject is manufactured answers. Every one of
these constants is read off `android.jar` at E0 and committed to the `apiSurfaceCheck` list.

**Denial lines are printed unedited.** Whatever `toString()` returned, wrapped in the expanded row,
middle-ellipsised when collapsed. If it is ugly, that is the finding.

*These two lines are real returns from `sipper.md` §3.*

```
java.lang.NoSuchMethodException: android.os.BatteryStatsManager.getBatteryUsageStats []
java.io.FileNotFoundException: /proc/stat: open failed: EACCES (Permission denied)
```

**Section A — zero permissions.** The three profile routes, `ACTION_BATTERY_CHANGED` fields, the
charge counter, `/proc/meminfo`, `/proc/cpuinfo`, `queryEventsForSelf`, `getAppStandbyBucket()`,
`getHistoricalProcessExitReasons`, `getPendingJobReason(s)`, `isBackgroundRestricted`,
`getRestrictBackgroundStatus`, the loose-file read.

**Section B — after usage access.** The header carries the read count and, when the appop is not
`MODE_ALLOWED`, a right-aligned `[ grant ]` and one line:

> `n` reads blocked by one Settings toggle.

There is one toggle and it is named; there is no "grant all permissions" button, because there is
nothing else to grant. Each row in B while blocked shows `Denied` with detail:

> `checkOp(OPSTR_GET_USAGE_STATS) = MODE_DEFAULT, treated as denied`
> `checkSelfPermission reports denied even when the read works; the appop is the signal.`

and, on the `queryUsageStats` row specifically:

> `the call itself returns an empty list here rather than throwing.`

`[ grant ]` navigates to `Screen.Disclosure`, a full destination carrying the prominent disclosure
immediately before the deep link. Not a dialog and not a bottom sheet: a dialog dismissible by an
outside tap is not a disclosure.

> **sipper is about to ask for usage access.**
>
> It reads per-app foreground time and per-uid network bytes. The data stays on this device. Nothing
> is uploaded, there is no analytics SDK in this build, and you can revoke it in Settings at any
> time.
>
> `[ not now ]`   `[ open Settings ]`

Once granted, `[ grant ]` becomes `[ revoke in Settings ]` pointing at the same deep link, and the
branch that actually fired is recorded and shown in the section caption — on EMUI, which branch
fires is itself a finding.

**Section C — impossible for any installed app.** Six rows expected to fail, run every time anyway:
`BatteryStatsManager.getBatteryUsageStats`, `dumpsys batterystats` via `Runtime.exec`, `/proc/stat`,
`/proc/uid_cputime/show_uid_stat`, `setAppStandbyBucket` by reflection, and
`AppOpsManager.setMode(RUN_ANY_IN_BACKGROUND)`. Caption:

> These six are expected to fail. They run on every launch, because a claim nobody re-tests is a
> claim that rots.

**Section D — open at write time, resolved by this run.** The `sipper.md` §11 unknowns, printed
whichever way they went on this device: thermal sysfs readability from the app uid, `cpufreq`
readability, and whether `BATTERY_PROPERTY_CURRENT_NOW` returns anything real. Caption:

> These three were unverified when the README was written. Whatever this run prints is the answer
> for this device, and the README says n=1 rather than resolving them from it.

## 2.5 States

**On entering the tab.** The run starts immediately with no tap. Rows appear in declaration order
and **are never reordered as they resolve** — a list that resorts under a reader's eyes is
unreadable. Each unresolved row shows `…` in `result`.

**Loading.** Below 120ms nothing changes; beyond that, skeleton rows at the real column widths. The
header timestamp stays blank until the run completes and the counts fill in live. A probe exceeding
2000ms renders `timeout (2000 ms)` as its result and the run continues past it.

**Re-run all.** Every row returns to `…` and the run proceeds. A row whose value differs from the
previous run carries a `Δ` prefix in `result` for five seconds, appearing and clearing without a
transition.

**Usage access on.** Section B fills, its blocked banner disappears, pair 4's left half goes live,
and the status strip flips to `usage-access ON` in `textSecondary`.

**API 36 emulator.** Section A's loose-file row reads `Absent` with detail
`ENOENT · that path is vendor-specific`. Pairs 2, 3 and 4 are live. The status strip omits the
vendor segment.

**Everything passes.** Not a state that occurs — section C exists to fail — so there is no all-green
screen, no checkmark and no "you're all set".

## 2.6 Gestures

- `[ re-run all ]`, and `run` per row.
- Pair half — expands the shared detail region under the block.
- Row — expands: the full unedited exception with frames, `howToGrant`, the two API numbers, and the
  AOSP citation.
- `[ grant ]` / `[ revoke in Settings ]` — the disclosure destination, then the deep link.
- Long-press any value or denial line — copies it.
- Footer `bench/queries-diff.txt` — opens the packaged copy in a plain text view.
- Overflow (`MoreVert`): `copy report`, the whole run as plain text for pasting into an issue.

No pull-to-refresh. `run` and `[ re-run all ]` are buttons and they run once.

---

# 3. State types

The types these screens consume — `RouteReading`, `Agreement`, `KeyDiff`, `AuditRow`,
`CalculatorGroup`, `AuditUiState`, `ProbeRow`, `ProbePair`, `ProbeSide` — are declared in
`docs/ARCHITECTURE.md` and are not restated here. Three of their fields carry behaviour that belongs
to this document:

- `ProbeRow` carries `minApi` and `behaviourSinceApi` separately. The `since` column renders one
  number when they agree and both when they differ. `Reading.Absent(minApi)` is used only when the
  method genuinely does not exist.
- `ProbeSide.returned` is a `String`, not the typed value. It is a quote of what the platform
  printed, and giving it a type would invite arithmetic on it.
- `AuditUiState`'s loading case is named `Loading`, not `Reading`, so it cannot be confused at a
  call site with `Reading<T>`.

---

# 4. Accessibility

Every state carries a word as well as a colour, so nothing here depends on hue.

- Each table row is one merged semantics node with an ordered description built from the column
  titles. `CollectionInfo(rowCount = rows.size, columnCount = 1)` on the list, because what TalkBack
  navigates is a list of merged rows. Cell-by-cell reading lives in the expanded row's linear
  `label: value` list.
- A hatched cell announces `manufactured value, cause key absent from profile` and never announces
  the underlying number. The pinning test uses the unmerged tree:
  `onNodeWithText("0.0", useUnmergedTree = true).assertDoesNotExist()` plus
  `onNodeWithContentDescription("manufactured", substring = true).assertExists()`.
- Each verdict chip has a `contentDescription` naming the verdict and the key.
- A pair block is one TalkBack node ordered left value, left cause, right value, right cause, closing
  line, so the reading survives without sight of the divider.
- Rows stay at their drawn height — 32dp on AUDIT, 44dp on PROBES — and take the tap across the full
  360dp row width. That clears WCAG 2.5.8 at 24×24 and knowingly fails Material's 48dp guidance; a
  row is not a small control. `minimumInteractiveComponentSize()` is reserved for the genuinely
  standalone controls: the PROBES `run` buttons, `[ re-run all ]`, and the status-strip segments.
- Row height at large font scales is measured, never multiplied by `fontScale` (CONTRACT §9.1).
- Both tables are LTR-locked, and so is the status strip, which is machine data end to end. Every
  cell sets `TextDirection.Ltr` explicitly: package names, profile keys, hex ids and exception
  strings are LTR data.
