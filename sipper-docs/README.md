# sipper

> `docs/CONTRACT.md` fixes tokens, widths, types and the API levels below. This file owns the
> claim, prior art, the limitations and the build. Where they disagree, the contract wins.

<!-- docs/probes.gif — 18 s, one take on the ANE-LX2, no cuts and no cursor overlay: the PROBES
     screen, tap "run" on pair 1, both halves resolve. Recorded at E7, before this file ships.
     Pair 1 is the pair that fires on this phone; the other three mechanisms begin at API 30 and
     31, so their captures come from the API 36 emulator and are labelled as such on the screen. -->
![PROBES pair 1: getAveragePower returning 0.0 for a real key the profile does not define and for a nonsense key, side by side, with the cause written on each](docs/probes.gif)

A per-app resource monitor for Android that will not print a number it cannot stand behind. It
reads what an installed app is genuinely allowed to read: the power profile the framework is
actually using, battery and thermal state, its own process history, and — after one Settings
toggle the user flips — per-app foreground time and per-uid network bytes. None of it renders as
a bare number. Every platform read returns `Reading<T>`, which is `Value(value, route)`,
`SilentDefault(shown, cause, route)`, `Denied(permission, howToGrant)` or `Absent(minApi)`.
`SilentDefault` is the case where the framework answered `0.0` or `false` because it had nothing
to answer with, and threw nothing, logged nothing and counted nothing. It renders as a hatched
cell carrying its cause, never as a digit, and `ReadingCell` is where that is enforced. No root,
no adb, no `QUERY_ALL_PACKAGES`, no network, no analytics, nothing leaves the device.

## Known limitations, before anything else

- **sipper cannot show per-app mAh.** `BatteryStatsManager.getBatteryUsageStats` is `api=blocked`;
  I measured it as `NoSuchMethodException` on API 36. `dumpsys batterystats` needs `DUMP` plus
  `BATTERY_STATS`. Per-uid CPU time is refused at every privilege by compile-time sepolicy
  (`neverallow appdomain proc_uid_*`), and `/proc/stat` has been revoked for `untrusted_app` since
  Android 8.0 — re-verified as `EACCES` from an installed app on API 36.
- **sipper cannot limit anything.** No API, intent, appop or reflection path changes another
  package's standby bucket, background restriction, data restriction, doze allowlist membership or
  enabled state on API 26–36. `CHANGE_APP_IDLE_STATE`, `SUSPEND_APPS` and
  `CHANGE_COMPONENT_ENABLED_STATE` are `signature|privileged`; `MANAGE_APP_OPS_MODES` is
  `signature|installer|verifier`; `MANAGE_NETWORK_POLICY` is bare `signature`. None carries the
  `development` protection flag, so `adb pm grant` cannot reach any of them either. What the app
  does instead is in [Route, then observe](#route-then-observe).
- **I prove the model's inputs are unpopulated. I never prove any app's displayed mAh is wrong by
  any amount, and I do not claim it.** The per-uid figure needed to check that is the blocked one
  above.
- **I never watch the consequence end to end, and on my one real device I cannot.** The ANE-LX2's
  battery screen is EMUI's own `systemmanager/.power.ui.HwPowerManagerActivity` pipeline, not
  AOSP's calculators. An emulator does not meaningfully drain. The causal chain is verified in
  AOSP source, key by key, and never observed happening.
- **On a device with EnergyConsumers behind the PowerStats HAL — in practice largely Pixel —
  measured energy replaces the profile for subsystem totals, and no public API tells an app which
  power model is in use.** So on the phone a reviewer most likely owns, I cannot establish that
  the profile is in the loop at all. The AUDIT screen has a designed full-screen state that says
  exactly that instead of manufacturing a verdict.
- Even where measured subsystem energy exists, per-uid apportionment is still activity-duration
  based. Fixing the profile problem would not fix the attribution problem.
- Instance 4 below is n=1 on hardware: a 2018 Huawei. The emulators are a reproducibility harness,
  not a second instance, and I say so before anyone says it to me.
- **Three of the four paired demonstrations on PROBES are API 36 emulator captures.** The
  silent-false mechanisms they show begin at API 30 and API 31; my one physical device is API 28,
  so it cannot produce them. Pair 1 — `getAveragePower` — is the pair that runs there, with no
  permission and no manifest entry, and it is the capture at the top of this file.
- On Android 9 the usage event stream carries `MOVE_TO_FOREGROUND` and `MOVE_TO_BACKGROUND` and
  nothing else: `ACTIVITY_STOPPED` and `ACTIVITY_DESTROYED` arrive at API 29, `DEVICE_SHUTDOWN`
  and `DEVICE_STARTUP` at API 30. So on my phone the `visible` and `fgs` columns render
  `Absent(29)` for every row, and a shutdown gap is inferred from a stretch of stream with no
  records rather than read from an event. `:usage` publishes which columns are structurally
  unavailable at the running API level, and the APPS header says so instead of showing an empty
  column with no explanation.
- `getTotalTimeInForeground` cannot be reconstructed perfectly. The activity `instanceId` is not
  exposed, so multi-instance apps carry a residual. It is measured, published as its own column,
  and pinned by fixtures rather than smoothed away.
- Retention caps everything. Usage events are pruned at about 10 days, per-uid network rotates at
  15 days with a 2-hour bucket floor, and `queryUsageStats` returns null rather than empty while
  the user is locked from API 30. n is in single digits everywhere and no significance is claimed.
- Without `QUERY_ALL_PACKAGES` — which I do not request, and which Play would refuse, since the
  `<queries>` MAIN/LAUNCHER filter I do declare is the less intrusive method that policy demands —
  packages with no launcher activity arrive with no label. Those cells are marked
  `no launcher activity` rather than quietly rendering a package name as a label.
- Two unrepresentative devices: a rooted 2018 Huawei pinned at API 28 with a vendor power stack
  that every API here is blind to, and Google's emulator images. No real hardware between API 29
  and 33, and no non-Huawei OEM device.
- Not on Play for v1. A read-only monitor is publishable and the data safety card would read "no
  data collected", but the repo is the deliverable.

## Prior art

**Tencent matrix `battery-canary`** is the closest work and the reason to be careful here. Its
`PowerProfile.smoke()` throws on zero CPU clusters, on a cluster with zero speed steps, and on a
core count that disagrees with `/sys`. Somebody reached this premise independently and shipped it.
It is CPU-cluster-only, binary pass/fail, invisible to any user, lives inside an APM SDK, and its
own `getAveragePower` wrapper still returns 0 for a missing key. I name it here, ahead of my own
claims, rather than in a footnote after them.

**AccuBattery** argues publicly that the profile method is untrustworthy and replaces it with
coulomb counting plus foreground attribution — it sidesteps the model rather than inspecting it,
and is the most candid app in the category about its own error bars. **GSam Battery Monitor**
ships an in-app adb flow and is still listed, which is useful evidence that displaying a shell
command is not itself a policy violation; it reads `SystemHealthManager`, not
`batterystats --checkin`. **BetterBatteryStats** reflected into `BatteryStatsImpl`; its listing
404s and the repo was archived 2026-03-15 — the privileged design I was about to rebuild is the
one that already left the store. **PowerTutor / PowerBooter** (Michigan, CASES 2010) is the
canonical academic distrust of vendor power models, but it generates a replacement model rather
than auditing the shipped one. **Carat** routed around the model entirely with population
statistics, which needed a server and a crowd — a useful negative result for a solo project.
**E-MANAFA** reads `power_profile.xml` directly rather than treating it as an implementation
detail, but it is host-side Python over adb and consumes the profile as truth.

**cpu-info, Inure, DeviceInsight and plain-app** all reflect into `PowerProfile`, call
`getAveragePower("battery.capacity")`, and render "Unknown" when it fails. None of the four
enumerates keys, reports coverage, or separates a zero value from an absent key. They are also the
reason not to lead with `battery.capacity`: it is the key nearly every OEM populates.

**Digital Wellbeing, ActionDash, StayFree and YourHour** are the UsageStats tier — honest by
abstention, and proof that the one-toggle onboarding works at consumer scale.
**Greenify, SuperFreezZ, Naptime and Ice Box** are outside confirmation that no installed app
reaches a limit mechanism through an API: each is AccessibilityService automation, an adb-granted
secure setting, or an adb-set Device Owner. **dontkillmyapp** and **AutoStarter** are cited with
the correction that AutoStarter probes using `getInstalledApplications(0)`, which package
visibility filtering empties on API 30+. **AOSP itself** is cited by file, line and branch below.

I make no claim of primacy for: auditing a power profile, distrusting vendor power models,
per-app usage reconstruction, deep-linking to Settings, or offline operation. All of it ships
today, some of it since 2010.

## The claim

Android's read APIs return confident wrong answers with no exception, no log line and no counter.
sipper knows when an answer was manufactured and says so instead of printing it.

Four instances. Three are live at API 36 on every device, including a Pixel. Every line reference
below is `path:line @ branch` against `platform/frameworks/base`; I read the files rather than
citing from memory. `core/java/com/android/internal/os/PowerProfile.java` is byte-identical
between `android-15.0.0_r1` and `android-16.0.0_r1` (same md5), so one citation covers both.

### 1. `getAveragePower(String)` falls through to the default with no signal at all

`PowerProfile.java:892-899` — `getAveragePowerOrDefault(String, double)` checks `sPowerItemMap`,
then `sPowerArrayMap`, and its final `else` at `:898` returns the caller's default. There is no
throw and no `Slog` on that branch. `getAveragePower(String)` at `:909-911` supplies `0` as that
default, and the levelled overload at `:960-975` reaches its own `return 0` at `:973`. A key your
device never defined and a key your device defines as zero come back identical, from a method
whose return type is `double`.

Measured from a zero-permission app on API 36: a nonsense key returns `0.0`, and **24 of 47**
probed real keys return `0.0`.

### 2. `queryUsageStats` does not clip interval buckets to the window you asked for

`UsageStatsDatabase.java:830` — `queryUsageStats(int, long, long, StatCombiner<T>, boolean)` finds
its start with `lastIndexOnOrBefore(beginTime)` at `:868`, and when that lands before the range it
clamps `startIndex` to `0` at `:869-873`. The loop at `:875-894` then reads each whole
`IntervalStats` file, guarded by `beginTime < stats.endTime`, and hands it to the combiner — a
containment test, never an intersection.

The combiner is `UserUsageStatsService.java:394-410`; `sUsageStatsCombiner` calls
`accResult.addAll(stats.packageStats.values())` at `:400`, adding whole `UsageStats` objects.
Callers merge those with `UsageStats.add(UsageStats)` at `UsageStats.java:350`, whose
`mTotalTimeInForeground += right.mTotalTimeInForeground` at `:371` is a sum with no clipping term
anywhere in it.

Ask for 24 hours of `INTERVAL_DAILY` and you can be handed up to 48 hours of foreground time.
Every screen-time app sums those objects anyway. Reconstruction from `queryEvents` is exact to the
window, so sipper shows both numbers and the signed delta between them as its own column. This is
the instance a user can feel.

### 3. The silent-false family

Three separate places where "no" and "you were not allowed to ask" are the same `false`. The level
a call was added at and the level its behaviour starts at are different numbers, and both are
carried on the screen:

| what | source | added | behaviour from |
| --- | --- | ---: | --- |
| `isAppInactive` without usage access | `UsageStatsService.java:2584-2586` returns `false` when `hasQueryPermission(callingPackage)` fails | 23 | 30 — `android-11.0.0_r1:1663-1665`, where the helper was still named `hasPermission` |
| `isIgnoringBatteryOptimizations` for a package outside your `<queries>` | `DeviceIdleController.java:2253-2259` returns `false` at `:2257` when `mPackageManagerInternal.filterAppAccess(...)` is true, before consulting the allowlist at `:3160` | 23 | 31 — `android-12.0.0_r1:1943-1948`; the guard is absent from that file at `android-11.0.0_r1` |
| a filtered appop check | `AppOpsService.java:3002-3003` returns `AppOpsManager.opToDefaultMode(code)` when `isIncomingPackageValid` (defined at `:4739`) is false | long-standing | long-standing |

The appop case is the sharpest of the three because of what the default is:
`AppOpsManager.java:3032-3034` builds `OP_RUN_ANY_IN_BACKGROUND` with
`.setDefaultMode(AppOpsManager.MODE_ALLOWED)`. So a package you cannot see reports itself as
allowed to run in the background, in the same value a genuinely unrestricted package returns.

`hasQueryPermission` at `UsageStatsService.java:2253-2267` is also why sipper checks the toggle
with `unsafeCheckOpNoThrow(OPSTR_GET_USAGE_STATS, …)` — `checkOpNoThrow` below API 29, since
`minSdk` is 28 — and treats `MODE_DEFAULT` as denied: `checkSelfPermission(PACKAGE_USAGE_STATS)`
reports denied forever even when the call works.

PROBES renders the first two as paired rows one manifest entry apart, both returning `false`, with
the cause written on each. Below the level where a mechanism exists, its pair keeps its title row
and prints one line naming the level that introduced it, so the reader knows what they are not
seeing.

### 4. Two power profiles on one device, and the framework reads the wrong-schema one

This is the n=1 instance. My ANE-LX2 carries two.

| | `framework-res.apk` `res/xml/power_profile.xml` | `/product/etc/xml/power_profile.xml` |
| --- | --- | --- |
| schema | complete and modern: `cpu.clusters.cores`, `cpu.speeds.cluster0`, `audio`, `video`, `camera.avg`, `camera.flashlight`, `memory.bandwidths`, `ambient.on`, every `wifi.controller.*` and `modem.controller.*` | pre-Lollipop: 20 keys, `cpu.active` / `cpu.awake` / `cpu.idle` / `cpu.speeds`, `dsp.audio`, `dsp.video`, `radio.active` |
| values | AOSP placeholder: 18 keys at `0.1`, 16 at `0`, `cpu.clusters.cores=1` on an octa-core, `cpu.speeds.cluster0=[400000]` | measured by somebody: `screen.on 143`, `screen.full 414`, `dsp.audio 43` with a `k3v5` comment, three `TBD` comments |
| `battery.capacity` | `1000` | `3000` |

**The framework reads the loose file.** `dumpsys batterystats --checkin` reports
`9,0,l,pws,3000,0.0157,0,0`, and the leading field of a `pws` row is
`PowerProfile.getBatteryCapacity()`. Not an RRO: I pulled
`/product/overlay/frameworkResOverlay.apk` and decoded it — tether regexes, wifi and VoLTE
booleans, display cutout, no `power_profile`. Huawei patched `PowerProfile` to prefer the file.

Consequence, measured twice: the live `pwi` component set on this phone is **five** —
`cell idle scrn uid wifi`. On API 36 it is **seventeen**. `cpu`, `audio`, `video`, `camera`,
`sensors`, `gnss`, `memory`, `ambi`, `flashlight`, `blue` and `phone` produce no row at all.

So the finding is not "a config file has defaults in it". It is that a complete-schema profile is
sitting unused in the same ROM while the framework prices five components from a schema three
releases obsolete, and nothing anywhere reports it. One phone, and it stays labelled as one phone.

## Four verdicts, because naming the wrong mechanism is caught in a minute

Zero has more than one cause, and an audit that says "missing" about a key the vendor deliberately
wrote as `0` is wrong in a way a reviewer spots immediately. So `ProfileAudit` assigns one of four:

| verdict | meaning | examples on my hardware |
| --- | --- | --- |
| `PRESENT` | the key is in the resolved profile with a non-zero value | `screen.on 143`, `screen.full 414` |
| `ZERO-BY-EXPLICIT-VALUE` | the key is there, written as `0` | `wifi.controller.*`, `modem.controller.*`, `gps.voltage`, `gps.signalqualitybased` |
| `ZERO-BY-ABSENCE` | the key is not in the file; `getAveragePower` manufactured the `0` | `cpu.cluster_power.*`, `cpu.core_power.*`, `cpu.suspend`, `bluetooth.controller.*`, `audio`, `video`, `camera.*` |
| `BACK-FILLED` | the framework synthesised this key at init from a deprecated one | `screen.on.display0`, `ambient.on.display0`, every `ModemPowerProfile` `MODEM_DRAIN_TYPE_*` constant on API 34+ |

`BACK-FILLED` exists because of `PowerProfile.java:786-826` (`initDisplays`) and `:836-859`
(`initModem`). `initDisplays` walks display ordinals until `getAveragePowerForOrdinal` returns
`NaN`, and if no per-display key was found it copies `ambient.on`, `screen.on` and `screen.full`
into ordinal-0 keys, logging a deprecation warning that names the replacement. Measured on the
API 36 emulator: its XML contains `screen.on` and no per-display key, yet
`getAveragePower("screen.on.display0")` returns `0.1`. An audit that does not model the chain
reports a false positive on display.

Because the platform logs that synthesis, a back-filled key is a `Value` carrying a `BACK_FILL`
route: it renders as a real number with a `back-filled from screen.on` provenance chip, and it is
never hatched. `SilentDefault` is for the case where the platform said nothing at all.

`initModem` is worse, and it is the part of the audit worth reading.
`handleDeprecatedModemConstant` at `:861-867` returns early when the modem constant is already
set, and otherwise writes `getAveragePower(deprecatedKey, level)` into it — which, by instance 1,
returns `0` when the deprecated key is also absent. So on API 34+ every `MODEM_DRAIN_TYPE_*`
constant is populated after init on every device, whether or not either key ever existed. There is
a committed fixture whose whole purpose is that case, and it fails without the chain model.

## The five screens

One dense operator surface. The reference points are Bull Board and a Sentry issue list:
monospace for every machine value, right-aligned numerics, sticky headers, hairline rules, colour
reserved for state. Material defaults are left as Material defaults. Tokens, type, row heights and
every column width are in `docs/DESIGN.md`; screen behaviour, states and copy are in
`docs/SCREENS.md`. Neither is restated here.

A status strip sits at the bottom of every screen and is real rather than decoration: device and
API level, the route agreement count, whether usage access is on, the `<queries>` scope actually
declared, how many profile keys read zero, and battery level, voltage and temperature. Each
segment is tappable and navigates to the screen that owns it.

### AUDIT

The headline screen, and it needs no permission at all. A provenance block comes before any key:
three independent read routes, the resource id each one resolved, and an agreement badge — with a
byte-level diff rendered when they disagree, which on my phone they do.

The three routes are the demonstration rather than redundancy.
`Resources.getSystem().getIdentifier("power_profile", "xml", "android")` then `getXml()` reads
through the zygote AssetManager and folds in immutable RROs but can miss mutable ones;
`getResourcesForApplication("android")` builds from the android package's own `ApplicationInfo`
including overlay directories, and is the primary; `PowerProfile` reflection reads through the
app's own AssetManager and is a cross-check that never carries a claim, because
`getAveragePower(String)` is on the warn-only unsupported list and Google's stated policy is that
such members may be restricted later. A disagreement between routes is a better finding than
anything a single route can produce.

Because the primary route is the resource one, the verdicts describe `framework-res.apk` — which
on my phone is not the file the framework prices from. So wherever the routes disagree, a
persistent line sits under the capacity row: verdicts below are computed from `framework-res.apk`,
`dumpsys pws` reports 3000 on this device, so the framework is pricing from `/product/etc/xml` and
these verdicts describe a file it does not read. Instance 4 belongs on the screen and not in a
README, and it is the first question a reviewer asks.

Below that, one row per key, grouped under a sticky header per `PowerCalculator` that reads it at
this API level, each row carrying its verdict, the literal token from the XML, and what the key
does or fails to do to a priced component. The capacity row is pinned above the first group with
its sources side by side — `1000` from `framework-res.apk`, `3000` from the loose file, the
reflection cross-check, and a `409000 µAh` charge counter from `BATTERY_PROPERTY_CHARGE_COUNTER` —
and no ratio computed between them.

Hidden-API denial surfaces as `NoSuchMethodException`, not `SecurityException`
(`getAveragePowerOrDefault` and the cluster helpers are `max-target-o`;
`getAveragePowerForCpuScalingPolicy` and `BatteryStatsManager.getBatteryUsageStats` are
`api=blocked`), so a tool catching the wrong exception reports a denial as "method removed". This
one does not.

### APPS

The dense grid, and where instance 2 becomes visible. One row per visible package: the framework's
foreground total, the same window reconstructed from `queryEvents`, the signed delta between them
as its own column, visible and FGS time, per-uid rx/tx split foreground and background, the
standby bucket as a raw integer then its name (5, 15, 45 and 50 occur and are not public
constants), and a modelled mAh total. At scroll offset 0 on a 360dp screen both foreground numbers
and the delta are already there, so instance 2 is in the first screenshot and the horizontal
scroll still has to prove itself.

There is no bar beside the mAh number. The finding a bar would carry — a component priced from an
absent or explicitly zero key — is in the AUDIT verdict table and in the expanded row's segment
table with its reason lines, `wifi 0 · wifi.controller.rx = 0`, which sort, paste into a bug
report unedited, and do not change length when someone types in the filter box.

Sort and filter are a pure projection over the row list, tested on the JVM; the query object is
`SavedStateHandle`-backed so it survives process death. The table scrolls horizontally under one
gesture node above the header, with the package column frozen outside the shifted region and no
second scroll state to keep in sync. A row expands into a Canvas lane chart, unsmoothed, with
shutdown gaps drawn as gaps rather than interpolated across.

### SELF

sipper as its own subject, and the one place in the app where every read-back is first-party. Own
bucket history, own pending-job reason codes (`_APP_STANDBY`, `_QUOTA`,
`_BACKGROUND_RESTRICTION`), own `ApplicationExitInfo` with reason and RSS at death
(`REASON_FREEZER`, `REASON_EXCESSIVE_RESOURCE_USAGE`), own modelled mAh from the same profile, own
wake-lock and FGS totals against the Android vitals threshold, own sampler history with the
observed interval between runs, and a control that restricts sipper itself so the loop closes on
the reviewer's phone. Every API level this repo asserts was read off `android.jar` and committed
to a gate rather than quoted from memory.

### PROBES

Built before anything else worked, and still the screen I would look at as a stranger. One row per
read with its sealed result, a re-run button, and verbatim runtime denial lines rather than
paraphrases. Its level column reports both the level a call was added at and the level its
behaviour begins at, because those differ for two of the pairs and rendering one as the other
would put a manufactured availability claim on the screen.

The paired demonstrations live here: `getAveragePower` on a real absent key beside a nonsense key;
`isAppInactive` before and after the toggle; `isIgnoringBatteryOptimizations` on an in-`<queries>`
package beside one outside it, both `false`, for different reasons, with the reasons on the rows.

### DEVICE

Battery, thermal, memory, clocks. The thermal panel earns its place: 13 zones report through one
`temp` file in three different units on this device — millidegrees (`soc_thermal 58570`), plain
degrees (`cluster0 56`), and `-40` as an absent sentinel — and zone 2 (`Battery 41000`)
cross-checks `dumpsys battery`'s `temperature 410`, so unit detection is validated against a
public API rather than a guess. RAM is shown beside swap, because "free RAM" means nothing on a
device with 2.29 GB of zram behind 3.87 GB of RAM.

### Route, then observe

The screen says this in one line, because the alternative is implying something untrue:

> sipper cannot limit an app. It finds the switch, takes you there, and afterwards measures
> whether behaviour changed.

**Route.** Candidates are resolved at runtime with `resolveActivity` and every candidate's probe
result is shown, so the screen reports which routes exist on this device rather than assuming.
`android.settings.VIEW_ADVANCED_POWER_USAGE_DETAIL` with a `package:` URI lands on the per-app
battery page that hosts the Restricted/Optimized/Unrestricted radio. It is `@hide`, but the action
is a plain string so no hidden-API enforcement applies, and AOSP Settings declares it
`exported=true` with no permission; present from `android-12.0.0_r34`, verified resolving on API 36
and verified **not** resolving on my API 28 phone. Fallbacks are
`ACTION_APPLICATION_DETAILS_SETTINGS` (API 9, universal, and the route to that radio below 31) and
`ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS` (API 24). On EMUI, declared in `<queries>`:
`huawei.intent.action.HSM_STARTUPAPP_MANAGER`, verified by pulling `HwSystemManager.apk` off my
own device — `exported=true` with an implicit filter, so it opens by action rather than by the
hardcoded ComponentName the autostart libraries guess at. The contrast that shows I checked:
`DetailOfSoftConsumptionActivity` in that same APK is guarded by
`huawei.android.permission.HW_SIGNATURE_OR_SYSTEM` and is not reachable. Whether each action needs
a `package:` URI comes from a table, since four of them require one and six fail to resolve with
one.

The disclosure that precedes any of this is a full navigation destination, not a dialog: a dialog
closes on an outside tap, and something dismissible by accident is not a disclosure.

**Observe, with the level named on the cell.** Level A is first-party read-back and exists for
sipper's own package. Level B is indirect, with the silent default named on the cell. Level C is
behavioural: did this package's foreground time, FGS time and background bytes move afterwards.
There is no Level A for any package but mine, and the screen says so rather than letting a green
checkmark imply otherwise.

There is no waiting phase in this design. The system restricts apps continuously without anyone
asking it to, and `STANDBY_BUCKET_CHANGED` delivers every transition for every app behind the same
toggle, so the lane chart fills up on its own instead of needing the phone left alone for a week.

## How it is put together

```
:audit    pure Kotlin JVM, zero production dependencies. Both schema eras plus the API-34
          per-display schema, the back-fill chain model, and a per-release table with one
          cited row per (key, api-range, calculator, effect) carrying file, line and branch.
          Reading<T> lives here, and ProfileAudit -> the four verdicts. Every audit test
          lives here too.
:usage    pure Kotlin JVM, zero dependencies. An event-stream state machine reconstructing
          foreground / visible / FGS intervals exactly to a window, with a degraded mode
          declared per SDK level, publishing unclosedTail as a column rather than
          swallowing it.
:collect  Android library. Every platform read returns Reading<T>.
:data     SQLDelight, migrations from the initial commit.
:app      Compose. minSdk 28, compileSdk 36, targetSdk 36.
tools/    corpus CLI, host-side, never in the APK.
```

A purity gate on `:audit` and `:usage` fails the build if anything Android reaches their runtime
classpath. It keys on artifact type (`aar`, `AgpVersionAttr`) rather than on group prefixes,
because a group-prefix check passes for a repackaged dependency. The build also enforces that
`:audit` cannot see `:collect`, so provenance is checkable from one Gradle file.

`Reading<T>` is the rule the whole repo hangs on. It is written once, in `:audit`, and
`docs/ARCHITECTURE.md` carries the type, its `DefaultCause` values and the render contract.
`SilentDefault` is a distinct constructor from `Denied` and from a genuine negative because at the
call site all three are the same `false` and they mean different things. Its manufactured value is
readable only behind an opt-in annotation, so putting one on screen takes two reviewable acts;
`ReadingCell` is the composable every `Reading` renders through, and a fifth constructor breaks the
build at every render site. Colour tokens and their measured contrast are in `docs/DESIGN.md`,
where a `SilentDefault` deliberately gets no hue: a coloured cell reads as a value.

The mono face is bundled Roboto Mono at two weights rather than `FontFamily.Monospace`, which maps
to `Typeface.MONOSPACE` and is vendor-substituted freely. Every column width in `docs/DESIGN.md` is
derived from a measured digit advance, and a face whose advance is unknown makes that width table
unreproducible across devices. The subset is pinned and a JVM test fails the build on any codepoint
outside it. That is a reproducibility decision rather than a styling one.

Two deliberate omissions worth naming. Standby buckets come from `STANDBY_BUCKET_CHANGED` events
rather than the reflected `@SystemApi getAppStandbyBuckets()`, which is reachable and which I am
not using: it harvests a per-package behavioural signal for every installed package including ones
outside my declared `<queries>` scope, which is the read in this design shaped like a
circumvention argument. The cost is that a package with no transition inside the retention window
reads `unknown`, and the cell says `unknown` rather than guessing. And the sampler is one periodic
WorkManager request at a 6-hour interval with an hour of flex, never a wake-locked foreground
service: usage events survive about ten days and `NetworkStatsManager` buckets are two hours wide,
so a faster sample writes rows no column can consume. The Android vitals argument for that shape is
in `docs/ARCHITECTURE.md` §9.

## Building and running

Needs JDK 17 and the Android SDK, with `local.properties` pointing `sdk.dir` at it. The Gradle
wrapper is committed.

```bash
./gradlew :audit:test :usage:test
./gradlew :app:assembleDebug
```

`app/build/outputs/apk/debug/app-debug.apk` is signed with the standard debug keystore and
installable as-is. There is no release signing config in this repository, so `assembleRelease`
produces an unsigned APK that installs nowhere; the signed build is attached to the GitHub
release instead. Neither APK is committed here.

```bash
./gradlew check
```

runs the unit tests plus four gates:

- **`checkJvmPurity`** — fails if any Android artifact reaches the runtime classpath of `:audit` or
  `:usage`.
- **`defaultProfileControl`** — a positive control, and it is inverted from an earlier draft of
  mine. The obvious shape — "each release's AOSP default profile must satisfy its own required-key
  set" — would define the headline out of existence, because the AOSP default *is* the file the
  claim is about. So this gate asserts that each release's default **fails** its own required-key
  set on a committed list of expected-absent keys, and breaks the build if it ever starts passing.
- **`backfillCheck`** — the modelled fall-back chains must reproduce `getAveragePower` results
  captured live from the API 34 and API 36 emulators across the full 47-key probe set.
- **`apiSurfaceCheck`** — three committed lists: the reflection surface `:collect` touches, so a new
  reflective call cannot appear without showing up in a diff; every API-level constant this repo
  asserts, each read off `android.jar` rather than recalled; and the public shape of `Reading`,
  which gains no value accessor and where `SilentDefault` has no `copy` and no `componentN`.

`check` also runs lint with `NewApi` promoted to an error in `:app` and `:collect`. `minSdk` is 28,
several of these calls arrived at 29 — `unsafeCheckOpNoThrow` among them — and the device that
would have crashed on one is the device I own, so that class of defect fails the build instead of
the phone.

`./gradlew :app:contrastDoc` and `./gradlew :app:columnTableDoc` regenerate the contrast and column
tables in `docs/DESIGN.md` from the committed token and column lists, so no number in them is typed
by hand.

Two of these gates rejected drafts of my own: `defaultProfileControl` when the required-key
assertion was still written the obvious way round, and `backfillCheck` when the audit modelled
`initDisplays` but not `initModem`. Both transcripts are in `NOTES.md`, with the date I watched
each one fail.

On the Huawei, install once and then set Settings → Battery → App launch → Manage manually for
sipper, or PowerGenie freezes the collector and the app becomes a study of itself.

## Where the numbers come from

One physical device and two emulators.

```
Huawei ANE-LX2    Android 9 / API 28, permanent ceiling   EMUI 9.1.0   Kirin 659, octa-core 4+4
                  3.87 GB RAM + 2.29 GB zram              GMS present  patch 2020-07-01
                  rooted with Magisk, deliberately unused: no number in this repo came from root
Emulators         API 36 google_apis x86_64, API 34 google_apis arm64
```

An emulator has no battery and nothing to measure, so its placeholder profile shows that the audit
runs, not that a vendor failed. adb is used freely against the emulators, because a test rig is not
a user; nothing in the shipped app needs it.

`minSdk 28` because 28 is the oldest level I own hardware for. `targetSdk 36` because
`targetSdkVersion` selects the SELinux domain at install and gates package-visibility filtering, so
testing lower would hand the app capabilities the shipping build will not have.

Device facts were read over adb on 2026-09-06, 09-07 and 09-08 from `dumpsys battery`,
`dumpsys batterystats`, `dumpsys deviceidle`, `dumpsys usagestats`, `dumpsys package`,
`dumpsys power`, `pm list features`, `cmd overlay list`, `aapt2 dump xmltree` on
`framework-res.apk` and `frameworkResOverlay.apk`, `/proc/meminfo`, and the thermal and cpufreq
sysfs trees.

## Redaction

Raw usage output from my daily phone is a record of which apps I use, when, and for how long,
published under my name next to a CV. Any committed `app_day` fixture is synthetic or from an
emulator, never from the handset, and each fixture file says which in its header. `tools/redact`
is what produced them and it is in the repo too.

## Licence and attribution

Apache-2.0. `NOTICE` records that `fixtures/vendor/` contains a decoded `power_profile.xml` and
material extracted from `framework-res.apk` on a retail Huawei device — both carry the Apache-2.0
header they shipped under — and the bundled Roboto Mono, also Apache-2.0. `NOTES.md` is the working
log, written the evening each piece of work happened, including every gate observed failing and
every claim I had to withdraw, three of which came from drafts of my own.

The name came from AOSP's `BatterySipper`, which has since been deleted from the framework and
appears nowhere in this design. I kept it anyway, and it costs this line.
