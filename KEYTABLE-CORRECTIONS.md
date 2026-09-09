# keytable corrections — proposed rebuild of ARCHITECTURE §5

Every line below was read from `android.googlesource.com` with
`curl -sL "<url>?format=TEXT" | base64 -d`, decode-verified (license header present, braces
balanced), and hashed. Nothing here is asserted from memory. Rows that could not be established to
that standard are listed in §6 as unresolved rather than filled in.

This file is a **proposal**. `docs/ARCHITECTURE.md` §5 and `audit/src/main/resources/keytable.tsv`
are unchanged until you accept it.

## 1. Files, with hashes

| file | tag | lines | sha256 (first 16) |
| --- | --- | ---: | --- |
| `core/java/com/android/internal/os/PowerProfile.java` | 16.0.0_r1 | 1214 | `2c6986e0f6ed7dee` |
| `core/java/com/android/internal/power/ModemPowerProfile.java` | 16.0.0_r1 | 563 | `0e60c6a4e6eb57b0` |
| `services/.../power/stats/BatteryStatsImpl.java` | 16.0.0_r1 | 17744 | `ca3495e80f30c776` |
| `services/.../power/stats/AudioPowerCalculator.java` | 16.0.0_r1 | 88 | `5e04017749373dda` |
| `services/.../power/stats/VideoPowerCalculator.java` | 16.0.0_r1 | 85 | `3969019f760c9406` |
| `services/.../power/stats/CameraPowerCalculator.java` | 16.0.0_r1 | 91 | `5dc1dc7a24c85c81` |
| `services/.../power/stats/FlashlightPowerCalculator.java` | 16.0.0_r1 | 71 | `f5d24d22ef85d1c2` |
| `services/.../power/stats/UsageBasedPowerEstimator.java` | 16.0.0_r1 | 52 | `dbbfc7b0d7bde3ec` |
| `core/java/com/android/internal/os/PowerProfile.java` | 12.0.0_r34 | 685 | `e9d552c5b9b598fa` |
| `core/java/com/android/internal/os/ScreenPowerCalculator.java` | 12.0.0_r34 | 294 | `30343431c3f08bad` |
| `core/java/com/android/internal/os/AmbientDisplayPowerCalculator.java` | 12.0.0_r34 | 86 | `204a7cc2c6fb9c69` |
| `core/java/com/android/internal/os/MobileRadioPowerCalculator.java` | 12.0.0_r34 | 319 | `98f4b283d554df78` |
| `core/java/com/android/internal/os/PowerProfile.java` | 9.0.0_r61 | 484 | `280b34f30a075f1d` |
| `core/java/com/android/internal/os/CpuPowerCalculator.java` | 9.0.0_r61 | 116 | `f9717c44a9e1ac54` |
| `core/java/com/android/internal/os/BatteryStatsHelper.java` | 9.0.0_r61 | 1051 | `602d7cb1d837f4c1` |

Absences confirmed by gitiles directory listing, not by 404 inference:
`BatteryStatsHelper.java` does not exist at 14.0.0_r1 (82-entry listing, no `*PowerCalculator` either);
`ScreenPowerCalculator.java` and `AmbientDisplayPowerCalculator.java` do not exist at 9.0.0_r61
(62-entry listing). `MobileRadioPowerCalculator.java` **does** exist at 9.0.0_r61.

## 2. Corrected rows — reader class and citation

`BSI` = `services/core/java/com/android/server/power/stats/BatteryStatsImpl.java` @16.0.0_r1.
`PP` = `core/java/com/android/internal/os/PowerProfile.java`.
`stats/` = `services/core/java/com/android/server/power/stats/`.

### Read in `BatteryStatsImpl`, not in the calculator §5 names

| key | corrected citation | anchor (verbatim) |
| --- | --- | --- |
| `battery.capacity` | `BSI:11519` (also `:12060`) | `mEstimatedBatteryCapacityMah = (int) mPowerProfile.getBatteryCapacity();` |
| `wifi.controller.voltage` | `BSI:12688` | `PowerProfile.POWER_WIFI_CONTROLLER_OPERATING_VOLTAGE) / 1000.0;` |
| `modem.controller.voltage` | `BSI:12834` | `PowerProfile.POWER_MODEM_CONTROLLER_OPERATING_VOLTAGE) / 1000.0;` |
| `modem.controller.sleep` | `BSI:12838` (back-fill at `PP:838`) | `mPowerProfile.getAveragePower(PowerProfile.POWER_MODEM_CONTROLLER_SLEEP)` |
| `gps.voltage` | `BSI:7651` | `PowerProfile.POWER_GPS_OPERATING_VOLTAGE) / 1000.0;` |
| `gps.signalqualitybased` | `BSI:7660` | `+= mPowerProfile.getAveragePower(PowerProfile.POWER_GPS_SIGNAL_QUALITY_BASED, i)` |
| `bluetooth.controller.voltage` | `BSI:13525` | `PowerProfile.POWER_BLUETOOTH_CONTROLLER_OPERATING_VOLTAGE) / 1000.0;` |

`battery.capacity` reaches `BSI` through the accessor `PowerProfile.getBatteryCapacity()` at `PP:1011`.
`BatteryChargeCalculator.java:41` reads it back off `BatteryStats` with no PowerProfile key literal, so
it is not the citation site.

### Wrong reader class

| key | §5 claimed | corrected |
| --- | --- | --- |
| `cpu.suspend` | `CpuPowerCalculator` | `stats/IdlePowerCalculator.java:40` |
| `cpu.idle` | `CpuPowerCalculator` | `stats/IdlePowerCalculator.java:43`, plus co-readers `WakelockPowerCalculator.java:42` and `processor/WakelockPowerStatsProcessor.java:29` |
| `modem.controller.sleep` | `ModemPowerProfile` | `PowerProfile.initModem` `:837-838`, plus `BSI:12838`. The string never appears in `ModemPowerProfile.java` |
| `radio.active` @34–36 | `NOT_READ` | `stats/MobileRadioPowerCalculator.java:89` **and** `stats/PhonePowerCalculator.java:34` — see §5 |

`grep POWER_CPU_SUSPEND` over `stats/CpuPowerCalculator.java` @16.0.0_r1 returns nothing.

### Read by no calculator at all

| key | corrected |
| --- | --- |
| `cpu.cluster_power.cluster0` | `PP:575` — `double clusterPower = getAveragePower(cpuCluster.clusterPowerKey);` inside `initCpuScalingPolicies`, the legacy-XML branch |
| `cpu.core_power.cluster0` | `PP:581` — `stepPower[step] = getAveragePower(cpuCluster.corePowerKey, step);` |

`grep cluster_power` across 50 calculator and 26 processor files: zero hits. These reach
`CpuPowerCalculator` only indirectly via `getAveragePowerForCpuScalingPolicy` /
`getAveragePowerForCpuScalingStep`, so under "a calculator reads it" they are `NOT_READ`.

### Wrong key string

| §5 key | real key | declaration |
| --- | --- | --- |
| `dsp.audio` | **`audio`** | `PP:211` — `public static final String POWER_AUDIO = "audio";` |
| `dsp.video` | **`video`** | `PP:217` — `public static final String POWER_VIDEO = "video";` |

Lowercase `dsp` has **zero** occurrences in `PP` @16.0.0_r1. The only DSP references are the comments
above those two constants (`PP:209`, `:215`). Read sites: `stats/AudioPowerCalculator.java:45`,
`stats/VideoPowerCalculator.java:42`.

### Correct as cited except for the line number

| key | corrected citation |
| --- | --- |
| `wifi.on` | `stats/WifiPowerCalculator.java:64` |
| `wifi.active` | `stats/WifiPowerCalculator.java:335` |
| `wifi.controller.idle` | `stats/WifiPowerCalculator.java:70` |
| `wifi.controller.tx` | `stats/WifiPowerCalculator.java:72` |
| `wifi.controller.rx` | `stats/WifiPowerCalculator.java:74` |
| `gps.on` | `stats/GnssPowerCalculator.java:36` |
| `memory.bandwidths` | `stats/MemoryPowerCalculator.java:36` |
| `bluetooth.controller.rx` | `stats/BluetoothPowerCalculator.java:60` |
| `camera.avg` | `stats/CameraPowerCalculator.java:38` |
| `camera.flashlight` | `stats/FlashlightPowerCalculator.java:36` |
| `screen.on.display0` | `stats/ScreenPowerCalculator.java:59` |
| `screen.full.display0` | `stats/ScreenPowerCalculator.java:61` (statement wraps to 62) |
| `ambient.on.display0` | `stats/AmbientDisplayPowerCalculator.java:44` |
| `screen.on` @30–33 | `12r34 ScreenPowerCalculator.java:54` |
| `screen.full` @26–33 | `12r34 ScreenPowerCalculator.java:56` — but see §4, the range needs splitting |
| `ambient.on` @28–33 | `12r34 AmbientDisplayPowerCalculator.java:36` |
| `radio.active` @26–33 | `12r34 MobileRadioPowerCalculator.java:54` |
| `screen.on` @26–29 | `9r61 BatteryStatsHelper.java:631` |
| `cpu.active` @26–29 | `9r61 CpuPowerCalculator.java:54` (statement 53–54) |
| `screen.on` @34–36 | `NOT_READ`; declaration `PP:177`, back-fill only at `:809-815` |
| `screen.on` @34–36 (14r1) | `NOT_READ`; declaration `14r1 PP:175`, back-fill at `:700` |

## 3. Effect re-labels — no new enum members needed

Four rows were labelled `ZERO_DIVIDES`. Only one divides. A zero that forces a component to zero is
already `ZERO_ZEROES_COMPONENT`, so `Effect` stays at five members and `KeyTable.kt` needs no change.

| key | §5 | corrected | evidence |
| --- | --- | --- | --- |
| `gps.voltage` | `ZERO_DIVIDES` | **`ZERO_ZEROES_COMPONENT`** | `if (opVolt == 0) { return 0; }` at `BSI:7652-7654`. `grep opVolt` in that method: only `:7650` and `:7652`. No divisor exists. |
| `modem.controller.voltage` | `ZERO_DIVIDES` | **`ZERO_ZEROES_COMPONENT`** | gate at `:12835` skips the whole `12835-12861` block — energy never computed, counter never added to, the rail division at `:12855` never runs. |
| `bluetooth.controller.voltage` | `ZERO_DIVIDES` | **`ZERO_ZEROES_COMPONENT`** | divisor at `:13528-13529` sits inside `if (opVolt != 0)` at `:13527`, with `controllerMaMs` pre-set to 0 at `:13526`. The divide is never reached. |
| `wifi.controller.voltage` | `ZERO_DIVIDES` | **`ZERO_DIVIDES` — stands** | see §5. The guard closes at `:12694`; the division at `:12698` is outside it. |
| `gps.signalqualitybased` | `ZERO_SELECTS_FALLBACK` | **`ZERO_ZEROES_TERM`** | per-bin multiplicand summed into `energyUsedMaMs`, loop `:7658-7662`. Never a gate, never a divisor. |
| `modem.controller.sleep` | `ZERO_ZEROES_TERM` | **unchanged — correct** | one summed term; idle/rx/tx still accumulate. |
| `radio.active` @26–33 | `ZERO_ZEROES_COMPONENT` | **`ZERO_SELECTS_FALLBACK`** | see §5. |
| `battery.capacity` | `ZERO_DIVIDES` | **needs scoping** — see §6 | at both `BSI` sites it is a *stored value*, never a divisor. A divisor elsewhere was not searched for. |

## 4. Structural recommendations

1. **Split the `screen.full` 26–33 row.** It spans two eras with two different readers:
   `9r61 BatteryStatsHelper.java:633` for 26–29 and `12r34 ScreenPowerCalculator.java:56` for 30–33.
   §5 already splits `screen.on` this way; `screen.full` should match.
2. **`Cite.anchor` cannot be a line containing the key, for five keys.** `getOrdinalPowerType` ends
   `return group + ordinal;` (`PP:1211-1212`), so the literals in source are the prefixes —
   `screen.on.display` (`:256`), `screen.full.display` (`:261`), `ambient.on.display` (`:251`),
   `cpu.cluster_power.cluster` (`:496`), `cpu.core_power.cluster` (`:498`). The suffixed forms are valid
   map keys and valid XML attribute names, but never string literals. The anchor for these must be the
   read expression, e.g.
   `powerProfile.getAveragePowerForOrdinal(POWER_GROUP_DISPLAY_SCREEN_ON, display));`
3. **Add a co-reader column, or accept that one row cites one site.** `cpu.idle`, `gps.signalqualitybased`,
   `battery.capacity`, `modem.controller.sleep` and the three display keys each have two or more real
   read sites. Citing one and staying silent about the rest is the shape of defect this table already had.
4. **Scope every `NOT_READ`.** The two surviving ones rest on 70 of 79 files in
   `core/java/com/android/internal/os/` — 9 would not decode and were not grepped (Zygote/Binder
   classes, very likely irrelevant). State 70/79 rather than rounding to "nothing reads it".

## 5. Three findings worth putting in the README

### a. A zero profile key really does corrupt recorded history — one line, unguarded

`BSI:12686-12703`. The guard opens at `:12690` and **closes at `:12694`**. The division at `:12698`
is outside it, conditioned only on `mTmpRailStats != null`:

```java
12690                if (opVolt != 0) {
12692                    controllerMaMs = info.getControllerEnergyUsedMicroJoules() / opVolt;
12694                }
...
12698                        ? (long) (mTmpRailStats.getWifiTotalEnergyUseduWs() / opVolt)
```

`opVolt` is a `double`, so `long / 0.0` does not throw — it yields `Infinity`, and `(long) Infinity`
is `Long.MAX_VALUE`. That value flows into `addCountLocked` at `:12700-12701` and into
`mHistory.recordWifiConsumedCharge` at `:12702-12703`. A `wifi.controller.voltage` of `0` therefore
writes `Long.MAX_VALUE` into the monitored-rail counter and into battery history, silently. The
framework guards three of its four operating-voltage divisions and misses this one.

### b. Explicit zero and absent are arithmetically different, with a citation

`12r34 MobileRadioPowerCalculator.java:54` reads
`profile.getAveragePowerOrDefault(PowerProfile.POWER_RADIO_ACTIVE, -1)`, and `:55-62` substitute a
computed fallback when the result is `-1` — the sum of `POWER_MODEM_CONTROLLER_RX` and the per-level
`POWER_MODEM_CONTROLLER_TX` values divided by `NUM_SIGNAL_STRENGTH_LEVELS + 1`.

**An explicit `0` is not `-1`, so it is taken literally.** Absent selects a different model; declared
zero prices the component at nothing. That is `ZERO_BY_EXPLICIT_VALUE` versus `ZERO_BY_ABSENCE` doing
measurably different arithmetic, from source, at a named line — exactly the distinction CONTRACT §3
asserts, and currently asserted without a citation.

The sentinel is also inconsistent across releases and subsystems: `radio.active` uses `-1` at 12.0.0_r34
and `Double.NaN` at 16.0.0_r1; `gps.on` uses `-1` (`stats/GnssPowerCalculator.java:36`); everything else
goes through `getAveragePower`, whose default is a plain `0` and which therefore **cannot** distinguish
absent from declared-zero at all. Three absence conventions in one file family.

### c. The audio, video, camera and flashlight calculators never ask whether the value exists

All four are built as `new UsageBasedPowerEstimator(profile.getAveragePower(KEY))`, and `grep isSupported`
returns nothing in any of them. So a missing or zero value produces `0.0 mAh` for any duration, reported
as a real estimate rather than as unsupported — the same silent-default shape as instance 1, one layer up.

## 6. Unresolved — do not write these yet

- **`battery.capacity` effect.** Verified as a stored value at `BSI:11519` and `:12060`, never divided
  there. §5 claims `ZERO_DIVIDES`, which is plausible elsewhere (a capacity is a natural divisor for
  percentage maths) but was not searched for outside `BatteryStatsImpl`. Either find the divisor and cite
  it, or relabel. Do not keep an unsupported `ZERO_DIVIDES`.
- **`gps.on` effect.** Reads with a `-1` sentinel, so absent probably selects a fallback rather than
  zeroing the component. §5 says `ZERO_ZEROES_COMPONENT`. The fallback branch was not read. Re-check.
- **`dsp` absence, case sensitivity.** Lowercase `dsp` is zero occurrences; the agent correctly declined
  to claim the same for `DSP`, which appears in comments. The key-string correction stands regardless.
- **`ZERO_ZEROES_COMPONENT` vs a new label for gates.** Three rows are now `ZERO_ZEROES_COMPONENT` on the
  grounds that a gate returning zero and a multiplicand of zero both price the component at nothing. If
  you would rather distinguish "the calculator refused to run" from "the arithmetic produced zero", that
  is a sixth `Effect` member and a contract change. My recommendation is not to add it: the observable
  outcome is identical, and the enum's job is the arithmetic consequence.
