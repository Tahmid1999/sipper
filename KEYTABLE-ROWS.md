# keytable — the corrected row set

The authoritative data for regenerating `audit/src/main/resources/keytable.tsv` and the table in
`docs/ARCHITECTURE.md` §5. Every `file`, `line` and `anchor` below was read from
`android.googlesource.com` and hashed — see `KEYTABLE-CORRECTIONS.md` §1 for the hashes.

**35 rows** (was 34; `screen.full` 26–33 is split into 26–29 and 30–33 because the two halves have
different readers — `KEYTABLE-CORRECTIONS.md` §4.1).

Path shorthand used in the `file` column below — expand to the full path when writing the TSV:

- `PP` → `frameworks/base/core/java/com/android/internal/os/PowerProfile.java`
- `BSI` → `frameworks/base/services/core/java/com/android/server/power/stats/BatteryStatsImpl.java`
- `S/` → `frameworks/base/services/core/java/com/android/server/power/stats/`
- `I/` → `frameworks/base/core/java/com/android/internal/os/`

## Effect provenance — read this before trusting the effect column

`ev` marks how the effect label was established:

- **`src`** — the zero-behaviour was read from source in this verification round.
- **`inh`** — inherited from the previous §5 table and **not** verified this round. The citation is
  now correct; the effect label is not evidence.
- **`unr`** — unresolved, see `KEYTABLE-CORRECTIONS.md` §6. Left at its previous value deliberately.

The TSV has no column for this, which is a gap: it carries `src` and `inh` labels
indistinguishably. Recommendation, not part of this change: add a tenth column `effectVerified`
before anyone quotes the effect column as a finding.

## The rows

| # | key | apiLo | apiHi | calculator | effect | ev | tag | file | line | anchor (verbatim) |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | `battery.capacity` | 26 | 36 | `BatteryStatsImpl` | `ZERO_DIVIDES` | unr | android-16.0.0_r1 | `BSI` | 11519 | `mEstimatedBatteryCapacityMah = (int) mPowerProfile.getBatteryCapacity();` |
| 2 | `screen.on` | 26 | 29 | `BatteryStatsHelper.addScreenUsage` | `ZERO_ZEROES_COMPONENT` | inh | android-9.0.0_r61 | `I/BatteryStatsHelper.java` | 631 | `power += screenOnTimeMs * mPowerProfile.getAveragePower(PowerProfile.POWER_SCREEN_ON);` |
| 3 | `screen.on` | 30 | 33 | `ScreenPowerCalculator` | `ZERO_ZEROES_COMPONENT` | inh | android-12.0.0_r34 | `I/ScreenPowerCalculator.java` | 54 | `powerProfile.getAveragePower(PowerProfile.POWER_SCREEN_ON));` |
| 4 | `screen.on` | 34 | 36 | | `NOT_READ` | src | android-14.0.0_r1 | `PP` | 175 | `public static final String POWER_SCREEN_ON = "screen.on";` |
| 5 | `screen.on.display0` | 34 | 36 | `ScreenPowerCalculator` | `ZERO_ZEROES_COMPONENT` | inh | android-16.0.0_r1 | `S/ScreenPowerCalculator.java` | 59 | `powerProfile.getAveragePowerForOrdinal(POWER_GROUP_DISPLAY_SCREEN_ON, display));` |
| 6 | `screen.full` | 26 | 29 | `BatteryStatsHelper.addScreenUsage` | `ZERO_ZEROES_TERM` | inh | android-9.0.0_r61 | `I/BatteryStatsHelper.java` | 633 | `mPowerProfile.getAveragePower(PowerProfile.POWER_SCREEN_FULL);` |
| 7 | `screen.full` | 30 | 33 | `ScreenPowerCalculator` | `ZERO_ZEROES_TERM` | inh | android-12.0.0_r34 | `I/ScreenPowerCalculator.java` | 56 | `powerProfile.getAveragePower(PowerProfile.POWER_SCREEN_FULL));` |
| 8 | `screen.full.display0` | 34 | 36 | `ScreenPowerCalculator` | `ZERO_ZEROES_TERM` | inh | android-16.0.0_r1 | `S/ScreenPowerCalculator.java` | 61 | `powerProfile.getAveragePowerForOrdinal(POWER_GROUP_DISPLAY_SCREEN_FULL,` |
| 9 | `ambient.on` | 28 | 33 | `AmbientDisplayPowerCalculator` | `ZERO_ZEROES_COMPONENT` | inh | android-12.0.0_r34 | `I/AmbientDisplayPowerCalculator.java` | 36 | `powerProfile.getAveragePower(PowerProfile.POWER_AMBIENT_DISPLAY));` |
| 10 | `ambient.on.display0` | 34 | 36 | `AmbientDisplayPowerCalculator` | `ZERO_ZEROES_COMPONENT` | inh | android-16.0.0_r1 | `S/AmbientDisplayPowerCalculator.java` | 44 | `powerProfile.getAveragePowerForOrdinal(POWER_GROUP_DISPLAY_AMBIENT, display));` |
| 11 | `cpu.suspend` | 28 | 36 | `IdlePowerCalculator` | `ZERO_ZEROES_TERM` | inh | android-16.0.0_r1 | `S/IdlePowerCalculator.java` | 40 | `powerProfile.getAveragePower(PowerProfile.POWER_CPU_SUSPEND)` |
| 12 | `cpu.idle` | 28 | 36 | `IdlePowerCalculator` | `ZERO_ZEROES_TERM` | inh | android-16.0.0_r1 | `S/IdlePowerCalculator.java` | 43 | `powerProfile.getAveragePower(PowerProfile.POWER_CPU_IDLE)` |
| 13 | `cpu.active` | 26 | 29 | `CpuPowerCalculator` | `ZERO_ZEROES_COMPONENT` | inh | android-9.0.0_r61 | `I/CpuPowerCalculator.java` | 54 | `PowerProfile.POWER_CPU_ACTIVE);` |
| 14 | `cpu.cluster_power.cluster0` | 30 | 36 | | `NOT_READ` | src | android-16.0.0_r1 | `PP` | 575 | `double clusterPower = getAveragePower(cpuCluster.clusterPowerKey);` |
| 15 | `cpu.core_power.cluster0` | 30 | 36 | | `NOT_READ` | src | android-16.0.0_r1 | `PP` | 581 | `stepPower[step] = getAveragePower(cpuCluster.corePowerKey, step);` |
| 16 | `wifi.controller.idle` | 26 | 36 | `WifiPowerCalculator` | `ZERO_SELECTS_FALLBACK` | inh | android-16.0.0_r1 | `S/WifiPowerCalculator.java` | 70 | `profile.getAveragePower(PowerProfile.POWER_WIFI_CONTROLLER_IDLE));` |
| 17 | `wifi.controller.rx` | 26 | 36 | `WifiPowerCalculator` | `ZERO_SELECTS_FALLBACK` | inh | android-16.0.0_r1 | `S/WifiPowerCalculator.java` | 74 | `profile.getAveragePower(PowerProfile.POWER_WIFI_CONTROLLER_RX));` |
| 18 | `wifi.controller.tx` | 26 | 36 | `WifiPowerCalculator` | `ZERO_SELECTS_FALLBACK` | inh | android-16.0.0_r1 | `S/WifiPowerCalculator.java` | 72 | `profile.getAveragePower(PowerProfile.POWER_WIFI_CONTROLLER_TX));` |
| 19 | `wifi.controller.voltage` | 26 | 36 | `BatteryStatsImpl` | `ZERO_DIVIDES` | src | android-16.0.0_r1 | `BSI` | 12688 | `PowerProfile.POWER_WIFI_CONTROLLER_OPERATING_VOLTAGE) / 1000.0;` |
| 20 | `wifi.on` | 26 | 36 | `WifiPowerCalculator` | `ZERO_ZEROES_COMPONENT` | inh | android-16.0.0_r1 | `S/WifiPowerCalculator.java` | 64 | `profile.getAveragePower(PowerProfile.POWER_WIFI_ON));` |
| 21 | `wifi.active` | 26 | 36 | `WifiPowerCalculator` | `ZERO_ZEROES_COMPONENT` | inh | android-16.0.0_r1 | `S/WifiPowerCalculator.java` | 335 | `profile.getAveragePower(PowerProfile.POWER_WIFI_ACTIVE) / 3600;` |
| 22 | `radio.active` | 26 | 33 | `MobileRadioPowerCalculator` | `ZERO_SELECTS_FALLBACK` | src | android-12.0.0_r34 | `I/MobileRadioPowerCalculator.java` | 54 | `profile.getAveragePowerOrDefault(PowerProfile.POWER_RADIO_ACTIVE, -1);` |
| 23 | `radio.active` | 34 | 36 | `MobileRadioPowerCalculator` | `ZERO_SELECTS_FALLBACK` | src | android-16.0.0_r1 | `S/MobileRadioPowerCalculator.java` | 89 | `profile.getAveragePowerOrDefault(PowerProfile.POWER_RADIO_ACTIVE, Double.NaN);` |
| 24 | `modem.controller.sleep` | 34 | 36 | `BatteryStatsImpl` | `ZERO_ZEROES_TERM` | src | android-16.0.0_r1 | `BSI` | 12838 | `mPowerProfile.getAveragePower(PowerProfile.POWER_MODEM_CONTROLLER_SLEEP)` |
| 25 | `modem.controller.voltage` | 31 | 36 | `BatteryStatsImpl` | `ZERO_ZEROES_COMPONENT` | src | android-16.0.0_r1 | `BSI` | 12834 | `PowerProfile.POWER_MODEM_CONTROLLER_OPERATING_VOLTAGE) / 1000.0;` |
| 26 | `audio` | 26 | 36 | `AudioPowerCalculator` | `ZERO_ZEROES_COMPONENT` | src | android-16.0.0_r1 | `S/AudioPowerCalculator.java` | 45 | `powerProfile.getAveragePower(PowerProfile.POWER_AUDIO));` |
| 27 | `video` | 26 | 36 | `VideoPowerCalculator` | `ZERO_ZEROES_COMPONENT` | src | android-16.0.0_r1 | `S/VideoPowerCalculator.java` | 42 | `powerProfile.getAveragePower(PowerProfile.POWER_VIDEO));` |
| 28 | `camera.avg` | 26 | 36 | `CameraPowerCalculator` | `ZERO_ZEROES_COMPONENT` | src | android-16.0.0_r1 | `S/CameraPowerCalculator.java` | 38 | `profile.getAveragePower(PowerProfile.POWER_CAMERA));` |
| 29 | `camera.flashlight` | 26 | 36 | `FlashlightPowerCalculator` | `ZERO_ZEROES_COMPONENT` | src | android-16.0.0_r1 | `S/FlashlightPowerCalculator.java` | 36 | `profile.getAveragePower(PowerProfile.POWER_FLASHLIGHT));` |
| 30 | `gps.on` | 26 | 36 | `GnssPowerCalculator` | `ZERO_ZEROES_COMPONENT` | unr | android-16.0.0_r1 | `S/GnssPowerCalculator.java` | 36 | `mAveragePowerGnssOn = profile.getAveragePowerOrDefault(PowerProfile.POWER_GPS_ON, -1);` |
| 31 | `gps.signalqualitybased` | 30 | 36 | `BatteryStatsImpl` | `ZERO_ZEROES_TERM` | src | android-16.0.0_r1 | `BSI` | 7660 | `+= mPowerProfile.getAveragePower(PowerProfile.POWER_GPS_SIGNAL_QUALITY_BASED, i)` |
| 32 | `gps.voltage` | 30 | 36 | `BatteryStatsImpl` | `ZERO_ZEROES_COMPONENT` | src | android-16.0.0_r1 | `BSI` | 7651 | `PowerProfile.POWER_GPS_OPERATING_VOLTAGE) / 1000.0;` |
| 33 | `memory.bandwidths` | 28 | 36 | `MemoryPowerCalculator` | `ZERO_ZEROES_COMPONENT` | inh | android-16.0.0_r1 | `S/MemoryPowerCalculator.java` | 36 | `profile.getAveragePower(PowerProfile.POWER_MEMORY, i));` |
| 34 | `bluetooth.controller.rx` | 26 | 36 | `BluetoothPowerCalculator` | `ZERO_ZEROES_TERM` | inh | android-16.0.0_r1 | `S/BluetoothPowerCalculator.java` | 60 | `mRxMa = profile.getAveragePower(PowerProfile.POWER_BLUETOOTH_CONTROLLER_RX);` |
| 35 | `bluetooth.controller.voltage` | 26 | 36 | `BatteryStatsImpl` | `ZERO_ZEROES_COMPONENT` | src | android-16.0.0_r1 | `BSI` | 13525 | `PowerProfile.POWER_BLUETOOTH_CONTROLLER_OPERATING_VOLTAGE) / 1000.0;` |

## Notes on individual rows

- **Row 1** `battery.capacity` reaches `BSI` through `PowerProfile.getBatteryCapacity()` at `PP:1011`;
  a second store site is `BSI:12060`. The `ZERO_DIVIDES` label is unresolved — at both `BSI` sites the
  value is stored, never divided by.
- **Rows 4, 14, 15** are `NOT_READ`, so their citation is the declaration or the config-parse site, not
  a calculator read.
- **Row 13** `cpu.active`'s statement wraps across 53–54; the anchor is line 54 because that is the line
  carrying the key constant.
- **Row 8** `screen.full.display0`'s statement wraps to line 62.
- **Rows 5, 8, 10, 14, 15** have keys that exist only as runtime concatenations — the literals in source
  are the prefixes. Their anchors are read expressions, as `KEYTABLE-CORRECTIONS.md` §4.2 requires.
- **Row 30** `gps.on`'s `-1` sentinel means absence probably selects a fallback rather than zeroing the
  component; the fallback branch was not read, so the effect stays `unr`.
- **Rows 12, 24, 31, 1, 5, 8, 10** have additional co-readers recorded in
  `KEYTABLE-CORRECTIONS.md` §2 and §4.3, not represented in this single-citation schema.
