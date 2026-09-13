# NOTES

The working log. Every gate observed failing, every claim withdrawn, every assumption that turned
out to be memory rather than a line someone opened.

---

## 2026-09-09 — `:audit` milestone 1 complete

`Reading<T>`, `Verdict` + `verdictFor`, `Provenance`, the StAX profile parser, the key-table types
and loader. 20 tests green. `./gradlew :audit:check` green including `checkAuditPurity` and
`checkAuditIsLeaf`. `audit/api/audit.api` committed — it confirms the negative guarantee: no `copy`
and no `componentN` on `SilentDefault`, and no accessor added to `Reading`.

The package root was renamed to `io.github.tahmid1999.sipper` across code and docs.

### Parser rewritten from DOM to StAX

`Provenance.Declared(key, value, fileLine)` needs a line number, and `DocumentBuilder` cannot report
one — the information is gone after parsing. Moved `parseProfile` to `javax.xml.stream`, which exposes
`Location.lineNumber`. Return type became `ParsedProfile(declared, lines)`. Still JDK-only, so the
purity gate is unaffected. Verified the reported line numbers against a fixture rather than assuming:
the JDK StAX implementation returns 1-based lines 2/3/4 for the three declarations in the test
document.

### A parser bug the test caught

`parseKeyTable` used `tsv.trim()`, and tab is whitespace. Every row's `citeAnchor` is empty, so the
last line ends in a tab, which `trim()` removed, leaving 8 columns and an index-out-of-bounds on the
anchor. Fixed to `trim('\n', '\r', ' ')` so the delimiter is never stripped, plus
`c.getOrElse(8) { "" }` so a row missing its trailing empty field parses rather than throwing. The
failing test is what surfaced it; the fixture was not adjusted to hide it.

---

## 2026-09-09 — AOSP source verification, and a claim withdrawn

All source below was fetched from `android.googlesource.com` with
`curl -sL "<url>?format=TEXT" | base64 -d`, and cross-checked against the `aosp-mirror` GitHub copy.
`PowerProfile.java` at `android-16.0.0_r1` is **1214 lines, 47571 bytes,
sha256 `2c6986e0f6ed7dee49f76e1bf0299052a0544a6f9d89e670ab9682f8003dbc71`** — identical on both hosts,
so the mirror is faithful and any disagreement with this repository's citations is this repository's.

`WebFetch`-style retrieval mangled line numbers repeatedly; only `curl` + `base64 -d` gave byte-exact
reads. Any future citation work uses that method.

### The README's instance-1 citations verified exactly

Every line reference the README makes for the headline claim landed on the stated line:

- `getAveragePowerOrDefault(String, double)` at `892-899`, with `898: return defaultValue;` — and no
  throw and no `Slog` on that branch.
- `getAveragePower(String)` at `909-911`; `910: return getAveragePowerOrDefault(type, 0);`
- the levelled overload at `960-975`, with its own `return 0` at `973`.
- `initDisplays` at `786-827`, `initModem` at `836-859`,
  `handleDeprecatedModemConstant` at `861-867`.

Instance 1 stands, from source.

### The §5 key table's line numbers do not

Twelve rows were spot-checked against the canonical host. **All twelve line numbers are wrong**, by
60 to 750 lines, landing in javadoc, closing braces and unrelated method signatures:

```
PowerProfile.java:254            is "* Power consumption when a screen is on, ..." (a comment).
                                 battery.capacity: declared 246, read only at 1011.
PowerProfile.java:512            is "// Default to single." inside initCpuClusters.
                                 radio.active: declared 195. See the withdrawal below - the
                                 "only a proto dump" reading of this was wrong.
stats/ScreenPowerCalculator:141  is "default:".            Real read sites: 59 and 61-62.
12r34 ScreenPowerCalculator:118  is a method signature.    Real: 54 (screen.on) and 56 (screen.full).
9r61 BatteryStatsHelper:521      is a binarySearch call.   Real: 631; addScreenUsage starts at 628.
```

The README's references are exact while the table's are not, which points at the table's line column
having been produced differently from the README's — a mislabelled tag, or a column never opened.
The transcripts this file is supposed to hold for each row were not written at the time, so the cause
is not recoverable from the record. Rebuilding the citation columns from source rather than patching
rows by hand.

### Four defects in the table that are worse than citations

1. **`dsp.audio` and `dsp.video` are not keys.** `POWER_AUDIO = "audio"` at `PowerProfile.java:211`
   and `POWER_VIDEO = "video"` at `:217`. `grep dsp` over that file returns nothing; "DSP" survives
   only in the javadoc above each constant. An audit built on the table would look up two keys that
   do not exist and report `ZERO_BY_ABSENCE` on every device.
2. **`cpu.suspend` and `cpu.idle` are read by `IdlePowerCalculator`**, at lines 40 and 43, not by
   `CpuPowerCalculator`. `grep POWER_CPU_SUSPEND` over `CpuPowerCalculator.java` at this tag returns
   nothing.
3. **`cpu.cluster_power.*` and `cpu.core_power.*` are read by nothing at `android-16.0.0_r1`.**
   `CpuPowerCalculator`'s only profile reads are `POWER_CPU_ACTIVE` (71),
   `getAveragePowerForCpuScalingPolicy` (76) and `getAveragePowerForCpuScalingStep` (90). The legacy
   cluster keys were replaced by the scaling-policy API, so the correct effect at the top of that
   range is `NOT_READ`.
4. **`modem.controller.sleep` is not in `ModemPowerProfile.java`.** It is handled at
   `PowerProfile.java:838`, inside `initModem`.

Additionally, the four `ZERO_DIVIDES` voltage rows — `wifi.controller.voltage`,
`modem.controller.voltage`, `gps.voltage`, `bluetooth.controller.voltage` — have no read site in the
files they cite. Absence was verified in the cited file and a few named neighbours, **not repo-wide**,
so the honest status is unsupported-at-the-cited-location rather than proven-absent.

### The `anchor` design cannot work as specified for one family of keys

`stats/ScreenPowerCalculator.java` at `android-16.0.0_r1` contains no literal key strings at all:
`grep '"screen'` and `grep display0` both return nothing. The per-display values are read through
`getAveragePowerForOrdinal(POWER_GROUP_DISPLAY_SCREEN_ON, display)` at line 59 and the screen-full
equivalent at 61-62, and the string `screen.on.display0` is composed at runtime inside
`PowerProfile.getOrdinalPowerType`. Same shape for `cpu.cluster_power.*`, whose constants are private
to `PowerProfile.java`. So for these keys no anchor line can ever contain the key. `Cite.anchor` has
to be the read-site expression, not a line containing the key literal.

### An assumption confirmed, and a new finding beside it

`ProfileModel.averagePower` takes element 0 for an `<array>` key. That was believed from memory and is
now source-backed:

```
895        } else if (sPowerArrayMap.containsKey(type)) {
896            return sPowerArrayMap.get(type)[0];
```

Element 0 unconditionally — not a sum, not the last element, remaining elements discarded.

Line 896 has **no length guard**, while the levelled overload handles `values.length == 0` explicitly
at 967, and `getNumElements` reads `.length` at 879, so the length was available and unused. A
zero-length `<array>` under a profile key throws `ArrayIndexOutOfBoundsException` out of
`getAveragePower(String)`. New, in the same method family as instance 1.

### A back-fill model corrected before it was committed

A first draft of `BackFill.kt` gated the display back-fill on `ordinal < displayCount` and computed
the count as the longest per-prefix run. Both are wrong. Verbatim from `initDisplays`:

```
788        mNumDisplays = 0;
789        while (!Double.isNaN(
790                getAveragePowerForOrdinal(POWER_GROUP_DISPLAY_AMBIENT, mNumDisplays, Double.NaN))
...
796            mNumDisplays++;
...
810        if (deprecatedScreenOn != null && mNumDisplays == 0) {
...
824        if (legacy) {
825            mNumDisplays = 1;
826        }
```

The walk increments while **any** of the three per-display keys exists at the current ordinal, and
each legacy copy is gated on `mNumDisplays == 0` **globally** — so a single declared per-display key
anywhere suppresses every legacy back-fill. The draft would have reported `BACK_FILLED` where the
framework reports nothing. Caught before the file was written; the corrected version carries the line
numbers above in its comments, and `BackFillTest` assertion 9 pins the case that would have failed.

---

## 2026-09-09 — CONTRACT §3 amended: modem back-fill splits in two

See `docs/CONTRACT.md` §3.1. Short version: `initDisplays` announces its back-fill with a `Slog.w`
naming source and destination key, and `handleDeprecatedModemConstant` announces nothing on its write
path. §3's justification for treating a back-fill as a `Value` — "the platform says so" — therefore
holds for displays and fails for modem. A modem constant materialised from a **declared** deprecated
key stays `BACK_FILLED`; one materialised from an **absent** deprecated key is
`SilentDefault(0.0, KEY_ABSENT_FROM_PROFILE, …)` and so `ZERO_BY_ABSENCE`. No new `Reading`
constructor and no new `DefaultCause` member.

---

## 2026-09-09 — claim withdrawn: `radio.active` is read at API 34+

Earlier the same day, the entry above recorded `radio.active` as appearing "only at 1146 (proto
dump)" at `android-16.0.0_r1`, treating §5's `NOT_READ` as confirmed. **That was wrong, and the error
was mine, not the table's.** The grep behind it covered `PowerProfile.java` alone. With the
calculators under `services/core/java/com/android/server/power/stats/` grepped, `radio.active` is
genuinely consulted at that tag:

```
MobileRadioPowerCalculator.java:88-89   double powerRadioActiveMa =
                                          profile.getAveragePowerOrDefault(PowerProfile.POWER_RADIO_ACTIVE, Double.NaN);
PhonePowerCalculator.java:33-34         mPowerEstimator = new UsageBasedPowerEstimator(
                                          powerProfile.getAveragePower(PowerProfile.POWER_RADIO_ACTIVE));
```

`MobileRadioPowerCalculator` passes `Double.NaN` as the default rather than `0`, and on NaN averages
`POWER_MODEM_CONTROLLER_RX` with the five `POWER_MODEM_CONTROLLER_TX` levels instead (`:98`). So a
missing `radio.active` does not zero the estimate here — it selects a different model. That is
`ZERO_SELECTS_FALLBACK` territory, not `NOT_READ`.

**The lesson is a rule, not an apology: a negative claim ("nothing reads this key") requires a
directory-wide grep, and absence in one file is not evidence.** Every `NOT_READ` row in the table
inherits that standard, and the two `screen.on` rows that still carry it rest on 70 of 79 files in
`core/java/com/android/internal/os/` — 9 would not decode and were not grepped. They are Zygote and
Binder classes, so the gap is very likely harmless, but the coverage statement gets scoped rather
than rounded up.

## 2026-09-09 — the operating-voltage keys mostly do not divide

Four rows were labelled `ZERO_DIVIDES`: `wifi.controller.voltage`, `modem.controller.voltage`,
`gps.voltage`, `bluetooth.controller.voltage`. Read at `android-16.0.0_r1`, three of the four are
**gates, not divisors**, and every one of them is read in `BatteryStatsImpl`, not in the calculator
the table names:

| key | real read site | what a zero actually does |
| --- | --- | --- |
| `gps.voltage` | `BatteryStatsImpl.getGpsBatteryDrainMaMs` :7650-7651 | `if (opVolt == 0) { return 0; }` at :7652-7654. No divisor anywhere; the loop at :7658-7662 never touches it. |
| `modem.controller.voltage` | `BatteryStatsImpl` :12833-12834 | gate only: `if (opVolt != 0) {` at :12835 guards the whole modem energy computation. No divisor cited. |
| `bluetooth.controller.voltage` | `BatteryStatsImpl.updateBluetoothStateLocked` :13524-13525 | divisor at :13528-13529 but inside `if (opVolt != 0)` at :13527, with `controllerMaMs` pre-initialised to 0 at :13526. |
| `wifi.controller.voltage` | `BatteryStatsImpl` :12687-12688 | **the one genuine divide.** :12692 is guarded by `if (opVolt != 0)` at :12690, but :12698 divides outside the guard, and the Inf/NaN flows on to :12700 and :12702-12703. |

This needs no new `Effect` members. A zero that forces the component to 0 is exactly
`ZERO_ZEROES_COMPONENT`, which already exists, so the three gate rows take that label and only
`wifi.controller.voltage` keeps `ZERO_DIVIDES`. The enum stays at five members.

The corrected finding is also stronger than the one it replaces: **the framework guards three of its
four operating-voltage divisions and misses one.** A blanket "voltage keys divide by zero" would have
been refuted by anyone who opened the file.

## 2026-09-09 — two more structural corrections

**`cpu.cluster_power.*` and `cpu.core_power.*` are read by no calculator.** They are consumed inside
`PowerProfile.initCpuScalingPolicies` (:575, :577/:581) and reach `CpuPowerCalculator` only indirectly
through `getAveragePowerForCpuScalingPolicy` / `getAveragePowerForCpuScalingStep`. A grep for
`cluster_power` across 50 calculator and 26 processor files returns nothing.

**Five key names in the table exist only as runtime concatenations.** `getOrdinalPowerType` ends
`return group + ordinal;` at PowerProfile.java:1211-1212, so the literals in source are the *prefixes*
— `screen.on.display` (:256), `screen.full.display` (:261), `ambient.on.display` (:251),
`cpu.cluster_power.cluster` (:496), `cpu.core_power.cluster` (:498). The suffixed forms are valid XML
attribute names and valid map keys; they are simply never string literals in the framework. Any
citation model that expects to anchor on the key text fails for these five by construction.

Also missing co-readers, now recorded: `cpu.idle` is read by `WakelockPowerCalculator.java:42` and
`processor/WakelockPowerStatsProcessor.java:29` besides `IdlePowerCalculator.java:43`;
`gps.signalqualitybased` has a second read at `BatteryStatsImpl.java:7660`; the three display keys are
also read by `ScreenPowerStatsProcessor` (:63, :65-66, :79).

## 2026-09-09 — gate 2 findings and one README claim in doubt

1. Gate 2's derived absent set was verified independently against the fixtures by grep, not taken from
   the test's own output. The four keys it reports absent — `bluetooth.controller.rx`,
   `bluetooth.controller.voltage`, `cpu.suspend`, `modem.controller.sleep` — have zero occurrences in
   both `aosp-defaults/34/power_profile.xml` and `aosp-defaults/36/power_profile.xml`. Ten spot-checked
   keys the gate does not report absent are each declared once. `requiredKeys(34)` and
   `requiredKeys(36)` are the same 25 keys, which is why both fixtures produce an identical absent set.

2. Only 4 of 25 required keys are absent from the AOSP default. The rest are declared, many as a literal
   zero, so the dominant verdict against the AOSP default is `ZERO_BY_EXPLICIT_VALUE` and not
   `ZERO_BY_ABSENCE`. The README's "24 of 47 probed real keys return 0.0" does not separate those two
   cases, which is the distinction the four verdicts exist for. Gate 2 now measures the split.

3. A README and ARCHITECTURE §6 claim is in doubt. Both state that the API 36 emulator's
   `power_profile.xml` contains only `screen.on` and no per-display key, and that
   `getAveragePower("screen.on.display0")` nonetheless returns 0.1 — the back-fill demonstration. The
   AOSP default at `android-16.0.0_r1`, `core/res/res/xml/power_profile.xml`, is the opposite:
   `screen.on.display0` is declared and `screen.on` is absent. The emulator resolves `framework-res.apk`,
   which a device overlay can replace, so the two files need not be the same — but the documents do not
   distinguish them, and if the emulator's own profile declares `screen.on.display0` then that 0.1 is a
   declared read rather than a back-fill. **UNRESOLVED** until the profile is pulled off the emulator
   with adb. Do not repair the claim by editing the sentence; the measurement has to be redone.

4. Because both AOSP default fixtures declare per-display keys, `declaredDisplayCount` is non-zero for
   them and `provenanceOf` never takes the back-fill branch. The `initDisplays` chain is currently
   exercised only by synthetic fixtures in `BackFillTest` and `ProfileModelTest`. No real-world profile
   has run through it. This is the concrete argument for gate 3.

---

## 2026-09-09 — the committed 1.db does not exist, and the guarantee behind it does

ARCHITECTURE §8 claimed a generated `1.db` was committed and that `verifyMigrations` rested on it.
SQLDelight 2.3.2 emits no `.db` under `deriveSchemaFromMigrations = true`, so the claim was wrong and
the artifact was never producible. The guarantee it was supposed to provide was then observed directly
rather than assumed — a temporary query selecting a nonexistent column failed the build at generation
time:

    G:/sipper/data/src/main/sqldelight/io/github/tahmid1999/sipper/data/Temp.sq:2:7 No column found with name nonexistent_column

The temporary query was deleted and never committed. The schema is derived from `1.sqm`, so the check
runs against the migrations themselves.

---

## 2026-09-10 — the Gradle wrapper moved for AGP 9

AGP 9.1.0 required a newer Gradle than the pinned 8.13, so the wrapper was bumped to 9.3.1. Gradle is
not among the versions ARCHITECTURE pins in libs.versions.toml, so this is not a spec change. All four
modules were re-checked on 9.3.1 and stayed green. Also, AGP 9.0+ has built-in Kotlin support and
rejects the org.jetbrains.kotlin.android plugin outright, so that plugin was removed; `kotlin {
explicitApi(); jvmToolchain(17) }` still works without it.

## 2026-09-10 — line numbers may not survive binary XML

Milestone 2 rewrote the `:audit` parser from DOM to StAX specifically so `Provenance.Declared` could
carry a real fileLine. The `:collect` resource routes read AAPT2-compiled binary XML through
`XmlResourceParser`, and compiled resources generally do not preserve source line numbers, so
`getLineNumber()` may return -1 or nothing useful there. If so, fileLine is meaningless for exactly
the routes that carry the verdicts. **UNRESOLVED** — it cannot be settled off-device, and must be
checked in the same session that pulls the emulator's power_profile.xml. Do not change any code over
this; it is recorded, not fixed.

---

## 2026-09-10 — gate 4 never covered :collect

ARCHITECTURE §3 asserted that the API-surface gate kept `:collect`'s return-type rule true, but §10
gate 4 only ever named the audit and usage `.api` files, and binary-compatibility-validator 0.16.3
registers no apiDump or apiCheck task for an Android library module, so no `.api` file for `:collect`
can exist. The claim was unsupported from the start. It is replaced by a source-level gate in
`collect/build.gradle.kts`, `checkCollectReturnsReading`, which was observed failing on a
deliberately planted `public fun temporaryBareDouble(): Double` before being trusted. Gate 4 remains
unchanged and continues to guard `:audit` and `:usage`. The source-level gate's stated limitation
is that it matches single-line signatures, which every function in `:collect` currently is.

---

## 2026-09-10 — instance 4 verified on the ANE-LX2

Everything below was read off the physical ANE-LX2 over adb. Device fingerprint: HUAWEI/ANE-LX2/HWANE:9/HUAWEIANE-L22/9.1.0.353C636:user/release-keys, API 28. Nothing personal was read — ROM files and two dumpsys rows only.

The two-profile claim holds. `/product/etc/xml/power_profile.xml` is 2676 bytes, 17 `<item>` plus 3 `<array>` = 20 keys, pre-Lollipop schema (`cpu.active`, `cpu.awake`, `cpu.idle`, `cpu.speeds`, `dsp.audio`, `dsp.video`, `radio.active`), `battery.capacity` 3000. `framework-res.apk`'s `res/xml/power_profile.xml` carries the complete modern schema (`audio`, `video`, `camera.avg`, `camera.flashlight`, `memory.bandwidths`, `ambient.on`, every `wifi.controller.*` and `modem.controller.*`, `cpu.clusters.cores`, `cpu.speeds.cluster0`, `gps.signalqualitybased`, `gps.voltage`) with `battery.capacity` 1000.

Every figure in the README's instance-4 table verified: `screen.on 143`, `screen.full 414`, `dsp.audio 43` with its `k3v5` comment, three `TBD` comments, `cpu.clusters.cores=1` on an octa-core, `cpu.speeds.cluster0=[400000]`.

One wording fix rather than a numeric one. The README says the AOSP placeholder has "18 keys at 0.1, 16 at 0". Counted as whole keys it is 17 and 11; counted as individual values it is exactly 18 and 16, because `radio.on` contributes one 0.1, `modem.controller.tx` five zeroes and `gps.signalqualitybased` two. The numbers are right and the word "keys" should read "values".

`dumpsys batterystats --checkin` reports `9,0,l,pws,3000,1.77,0,0`. The leading field of a `pws` row is `PowerProfile.getBatteryCapacity()`, so the framework is pricing from `/product/etc/xml` and not from `framework-res.apk`. The README quotes this row with a second field of 0.0157 where the device now reports 1.77; that field is not the capacity and varies, so only the leading 3000 is load-bearing.

The live `pwi` component set is five: `cell idle scrn uid wifi`. Exactly as claimed.

NEW FINDING, and it belongs in the README. The `/product` profile declares `dsp.audio` 43 and `dsp.video` 176, values somebody measured, one carrying a `k3v5` chip comment. But `POWER_AUDIO = "audio"` at `PowerProfile.java:171` in android-9.0.0_r61 and unchanged at android-16.0.0_r1, and `POWER_VIDEO = "video"` at `:177`. No AOSP `PowerProfile` has ever looked up `dsp.audio`. The framework asks for `audio`, the file it is actually pricing from has no such key, and `getAveragePower` returns `0.0` by absence with no log and no throw. Two real measurements discarded over a key name — instance 1 firing on live vendor data rather than on a placeholder.

Still open on this device: whether `XmlResourceParser.getLineNumber()` returns anything usable for the resource routes, and the 47-key `getAveragePower` capture for gate 3. Both need code running on the device, not adb alone.

---

## 2026-09-10 — two open questions answered on device

The on-device probe has run on two targets and both have answered the questions that motivated milestone 7.

1. `XmlResourceParser.getLineNumber()` DOES preserve line numbers on AAPT2-compiled XML. Both devices returned `ambient.on` 30, `screen.on` 31, `screen.full` 32, `bluetooth.active` 33, `bluetooth.on` 34, matching the source lines an aapt2 dump of framework-res.apk shows. The earlier worry that compiled resources would lose line numbers was wrong, and `Provenance.fileLine` is real on both resource routes. The DOM to StAX rewrite in milestone 2 was worth doing. This closes the entry titled "line numbers may not survive binary XML".

2. The README's back-fill demonstration holds, measured. On the API 36 emulator (`google/sdk_gphone64_x86_64/emu64xa:16/BP22.250325.006/13344233`) the profile declares `screen.on`, `screen.full` and `ambient.on` and declares no per-display key at all, yet `getAveragePower` returns 0.1 for `screen.on.display0`, `screen.full.display0` and `ambient.on.display0`. That is `initDisplays` back-filling into ordinal 0, exactly as the README and §6 describe. It also matches what `BackFill.kt` predicts: with no per-display key declared, `declaredDisplayCount` is 0 and the legacy copy fires at API 34 and above.

3. The doubt recorded earlier was mine and it was wrong. It rested on the AOSP default at `android-16.0.0_r1`, `core/res/res/xml/power_profile.xml`, which declares `screen.on.display0`. The emulator's `framework-res.apk` is not built from that file, so both are true and the inference from one to the other did not hold. This closes the third item of the entry titled "gate 2 findings and one README claim in doubt".

4. A contrast worth keeping: `dsp.audio` reflects 0.0 on the emulator, where AOSP declares no such key, and 43.0 on the ANE-LX2, where the vendor did. The key-name defect is vendor-specific, not universal.

---

## 2026-09-13 — the ANE-LX2 fixture: instance 4 and instance 1 are now gates

The phone was connected, so gate 3 gained its second fixture — `AneLx2ReplayTest`, 6 tests, `:audit`
now at 49. Both files behind instance 4 were pulled and committed:

- `/product/etc/xml/power_profile.xml`, verbatim, 2676 bytes, 20 keys. It matches the 2026-09-10
  record figure for figure (screen.on 143, dsp.audio 43 k3v5, capacity 3000, the TBDs), so two
  independent reads agree. This is the replay XML.
- `framework-res.apk`'s profile, as an aapt2 xmltree dump: 35 keys, capacity 1000 — the file the
  resource routes return, pinned to the capture's 35 `declared` keys and all five `line` numbers.

The gate makes the two README findings mechanical, so they cannot silently rot: all 13 keys the
files share disagree in value (`theTwoProfilesDisagreeOnEverySharedKey` — instance 4), and
`dsp.audio` 43 / `dsp.video` 176 are PRESENT in a file no AOSP `PowerProfile` reads while `audio`
and `video`, which it does read, are ZERO_BY_ABSENCE (`theVendorMeasuredKeysTheFrameworkNeverReads`
— instance 1 on live vendor data). The 50-row replay pins the silent default itself: every
captured value reproduces exactly, including `wifi.controller.tx_levels → 0.0` — silent here, the
ArrayIndexOutOfBoundsException on the API 36 emulator, because `/product` never declared the key.

Observed red before trusted, same protocol as the emulator gate: array selection sabotaged to
`.last()` made the 50-row replay fail at the first array key (element 0 is the framework's own
behaviour, `PowerProfile.java:896`); model restored byte-identical (`git diff` empty).

Tooling finding on the way: build-tools 36.0.0's aapt2 cannot open this EMUI-era framework-res.apk
(`failed opening zip: Invalid file`, though the ZIP magic and 7862 entries are intact and
PowerShell's ZipFile reads it); 35.0.1's aapt2 reads it fine. The older tool was used for the ANE
dump. The same failure had been seen from 36.1.0-rc1 on the emulator's APK; it is not an rc-only
regression.

Two deliberate asymmetries with the emulator gate, both because instance 4 makes the files differ:
the replay XML carries no `line`-number check (the capture's `line` rows document framework-res, a
different file), and the ANE fixture commits the vendor's own bytes rather than a reconstruction
(there is no compiled-XML gap to bridge — `/product/etc/xml` is plain text). NOTICE now records both
committed vendor files.

One clarification rather than a correction: the emulator's `framework-res.apk` being built from the
AOSP default (per-display keys) was already true; what the 2026-09-10 entry left unstated is that
the base file never reaches the runtime at all — the RRO replaces it. That is now in §10.3.

## 2026-09-13 — gate 3 built, and the emulator's profile comes from an RRO

Gate 3 (`BackfillCheckTest`, ARCHITECTURE §10.3) now exists and is green: 4 tests, 43 total in
`:audit`. Built against the committed API 36 capture (48 reflect rows), replayed with exact
`Double` equality through `ProfileModel.averagePower`. The gate was observed red before being
trusted: with `initDisplays` disabled in the model, 2 of its 4 tests failed (the replay row and the
load-bearing provenance row) exactly as §10.3 predicts, then the model was restored byte-identical
(`git diff` empty, test cache hit).

The fixture XML is the file the runtime actually resolves, and finding it produced the day's real
finding. `framework-res.apk` on this emulator declares the **per-display** schema
(`screen.on.display0` at line 45, matching the AOSP default) — but the capture's package route
declared the deprecated singular keys (`screen.on` at line 31). The resolved file comes from an
enabled auto-generated product overlay,
`/product/overlay/framework-res__sdk_gphone64_x86_64__auto_generated_rro_product.apk`, whose
`power_profile.xml` matches the capture exactly: all 35 keys, every first value, the five `line`
rows 30–34. So the emulator's back-fill demonstration rests on a legacy-schema profile supplied by
an emulator-specific RRO, re-declaring keys the base APK had migrated away from. The vendor
partition's framework-res RRO could not be pulled (permission denied); not a gap, since the
product RRO matches and outranks it.

This corrects an inference in the 2026-09-10 entry above ("The emulator's `framework-res.apk` is
not built from that file"): it IS built from that file — the AOSP default, per-display keys and
all — and an RRO replaces it at resolution time. The capture was right, the capture's XML source
was mislabelled, and the mislabelling is now fixed by pointing the fixture at the overlay's file
with the pull and dump recorded in `bench/probe-capture.md`.

The committed fixture XML is a reconstruction from the committed aapt2 dump (same elements, text
and line numbers; comments absent because aapt2 drops them), and the gate's
`reconstructionMatchesTheCapture` test pins it to the capture's `declared` key set and `line`
numbers so the replay cannot drift from what the device saw. During that reconstruction an
off-by-one in the blank-line gap (35 keys each one line late) was caught by a mechanical check
against the dump before the test was written — the check, not the eye, is what found it.

Also fixed in passing: the fixture TSVs carry a UTF-8 BOM (the logcat redirect wrote one), which
the test strips rather than editing the committed capture.

Two numbers in the old §10.3 text were never real: "47 rows" (no 47-key list exists anywhere in
the repo — the probe set is a union rule, and it produced 48 here) and the API 34 fixture (that
AVD is arm64 on an x86_64 host and cannot boot). Both corrected in §10.3 rather than
reconstructed; `bench/probe-capture.md` already said neither was attainable.

---

## 2026-09-10 — getAveragePower throws on an empty array, observed

Predicted from source earlier the same day and now observed on a device. `PowerProfile.java:896` at android-16.0.0_r1 is `return sPowerArrayMap.get(type)[0];` with no length check, while the levelled overload guards `values.length == 0` at `:967` and `getNumElements` reads `.length` at `:879`, so the length was available and unused.

On the API 36 emulator, `getAveragePower("wifi.controller.tx_levels")` through the reflection route produced `java.lang.ArrayIndexOutOfBoundsException: length=0; index=0`. The AOSP profile declares that key as an array with no values.

On the ANE-LX2 the same key returns 0.0 instead. The reason is instance 4, not a different framework: both devices' resource routes read `framework-res.apk`, which declares the empty array on both, but `PowerProfile` on the Huawei resolves `/product/etc/xml`, which does not declare the key at all, so the call falls through to the silent 0.0 default. The same empty array is a crash on one device and a silent zero on the other, decided only by which file the framework resolved.

Scope, stated rather than implied: this is reachable through the single-argument `getAveragePower(String)` on an array key, which is what sipper's reflection route calls. Whether the framework itself ever calls the single-argument form on `wifi.controller.tx_levels` has NOT been checked; it plausibly uses the levelled overload, which is guarded. So this is a latent unguarded path, not proof that the framework crashes.

The probe originally hid this. A single try/catch around the whole body turned the throw into one `error` line and silently dropped the last four keys, while the test still reported green. It now catches per key and unwraps `InvocationTargetException` to its cause, so a throw is recorded as a value in the fixture rather than ending the capture. Both fixtures were re-taken after the fix and each now has as many reflect rows as its probe set.
