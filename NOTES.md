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
