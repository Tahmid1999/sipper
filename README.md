# sipper

An Android app that shows what a normal, non-rooted app is allowed to read about power and
resource use, and won't print a number it can't back up. No root, no adb, no network, nothing
leaves the phone. Hobby project.

![sipper walkthrough](media/sipper_app_demo.gif)

## Why

Android's read APIs often hand back a confident wrong answer with no exception and no log line.
`PowerProfile.getAveragePower()` returns `0.0` for a key the device sets to zero, and also `0.0`
for a key the device never defined. You can't tell them apart from the return value. On the
API 36 emulator, 21 of the 48 keys I probed came back as `0.0`.

So every platform read in sipper returns a `Reading<T>` instead of a bare value:

- `Value(value, route)`, a real answer and where it came from
- `SilentDefault(shown, cause, route)`, the framework returned a default because it had nothing
  to return. The UI draws this as a hatched cell with the cause, never as a digit
- `Denied(permission, howToGrant)`
- `Absent(minApi)`, the call doesn't exist on this API level

## Screens

- **AUDIT** reads `power_profile.xml` three ways (system resources, the framework package's
  resources, `PowerProfile` reflection) and puts each key into one of four verdicts: present,
  zero by explicit value, zero by absence, or back-filled.
- **PROBES** runs side-by-side calls that return the same value for different reasons. Pair 1
  calls `getAveragePower` on a real key the profile doesn't define and on a made-up key. Both
  return `0.0`.
- **APPS** shows foreground time per app from UsageStats, over 6 h, 24 h or 7 days, once the
  user turns on usage access in Settings.
- **SELF** shows sipper's own process: the WorkManager job that samples every 6 hours, and
  exit reasons from `ApplicationExitInfo`.
- **DEVICE** shows battery state, thermal zones, network byte counters and build info.

## Modules

- `audit`: plain Kotlin. Profile parser, key table, verdicts, `Reading<T>`
- `usage`: plain Kotlin. Rebuilds foreground time from usage events
- `data`: plain Kotlin. SQLDelight schema for the samples
- `collect`: Android library. The platform reads
- `app`: Jetpack Compose UI
- `bench/`: raw probe captures from the two devices below

The design notes the code comments refer to are in `docs/`.

## Limitations

- Android gives a normal app no per-app battery figure. `getBatteryUsageStats` is blocked, and
  `dumpsys batterystats` needs `DUMP`. The mAh number on APPS is a rough placeholder
  (foreground time times a fixed 250 mA), not a measurement.
- sipper can't limit other apps. No API lets a normal app change another app's standby bucket,
  background restriction or battery optimisation.
- I tested on two devices: a 2018 Huawei ANE-LX2 at API 28, and the Pixel 9 Pro XL emulator at
  API 36. Some PROBES pairs need API 30 or 31, so those only show on the emulator.
- Not on the Play Store.

## Build

JDK 17 and Android SDK 36. `minSdk` is 28.

```
./gradlew check
./gradlew :app:installDebug
```

## Licence

Apache 2.0, see `LICENSE`. Third-party files are listed in `NOTICE`.
