# Probe Capture Provenance

Two benchmark fixtures, `ane-lx2-api28.tsv` and `Pixel_9_Pro_XL-api36.tsv`, record the results of running `ProfileProbe` on two Android devices. This document records the provenance of each capture: device fingerprint, API level, probe-set composition, and the adb invocations that produced them. The API 36 fixture additionally feeds gate 3 (`BackfillCheckTest`), whose committed copy lives at `audit/src/test/resources/probes/Pixel_9_Pro_XL-36/` (the capture byte-identical, SHA-256 verified at commit time), together with the profile XML and the aapt2 dump it was rebuilt from.

## ANE-LX2 at API 28

- **File:** `bench/ane-lx2-api28.tsv`
- **Device fingerprint:** `HUAWEI/ANE-LX2/HWANE:9/HUAWEIANE-L22/9.1.0.353C636:user/release-keys`
- **API level:** 28
- **Date:** 2026-09-10
- **Probe set:** 50 keys; 50 reflect rows
- **Commands:**
  ```
  adb logcat -c
  .\gradlew.bat :collect:connectedDebugAndroidTest
  adb logcat -d -s SIPPERPROBE:I
  ```

## Pixel 9 Pro XL AVD at API 36

- **File:** `bench/Pixel_9_Pro_XL-api36.tsv`
- **Device fingerprint:** `google/sdk_gphone64_x86_64/emu64xa:16/BP22.250325.006/13344233:user/release-keys`
- **API level:** 36
- **Date:** 2026-09-10
- **Probe set:** 48 keys; 48 reflect rows
- **Commands:**
  ```
  adb logcat -c
  .\gradlew.bat :collect:connectedDebugAndroidTest
  adb logcat -d -s SIPPERPROBE:I
  ```
  The emulator was pinned with the `ANDROID_SERIAL` environment variable so the physically-connected phone was never targeted.

## One key throws rather than returning

On the API 36 emulator, `wifi.controller.tx_levels` produced, verbatim:

```
Threw:java.lang.ArrayIndexOutOfBoundsException:length=0; index=0
```

The probe records per-key throws rather than aborting, which is why the rest of the capture survived.

## How the probe set was chosen

ARCHITECTURE §10 says gate 3 replays 47 rows, but no 47-key list exists anywhere in this repository. The probe set is instead the union of every key in `keytable.tsv`, every key the resource route declared on that device, and a short `VENDOR_ONLY_KEYS` list in `ProfileProbe.kt`. That gives 50 keys on the ANE-LX2 and 48 on the emulator, not 47. The number in §10 has no derivation in the repo and these captures do not reconstruct it.

## Not captured

No API 34 capture exists. The only API 34 AVD on this machine, `miggo_arm64`, is arm64 and the host is x86_64, so it cannot boot. The `android-34` google_apis system image IS installed, so an x86_64 API 34 AVD could be created later.

## Gate 3 fixture provenance (2026-09-13)

The gate replays against the XML the runtime *actually resolved*, which on this emulator is not the file inside `framework-res.apk`. Established the same day, same image as the capture (booted headless, fingerprint re-read and matching `BP22.250325.006/13344233`):

1. `framework-res.apk` (pulled from `/system/framework/`, 36,585,209 bytes, ZIP magic `50 4B 03 04` verified) declares the **per-display** schema — `screen.on.display0` at line 45 — matching the AOSP default at `android-16.0.0_r1`.
2. The capture's package route, however, declared the **deprecated singular** keys (`screen.on` at line 31). `cmd overlay list android` showed two enabled auto-generated framework-res RROs; the product partition one carries a `power_profile.xml`, and its aapt2 dump matches the capture exactly — all 35 keys, every first value, and the five `line` rows (30–34).
3. So the emulator's effective profile — and the back-fill demonstration gate 3 exists to prove — is supplied by `/product/overlay/framework-res__sdk_gphone64_x86_64__auto_generated_rro_product.apk`, re-declaring the legacy keys over the base APK's per-display ones.

Commands (build-tools 36.0.0; the 36.1.0-rc1 aapt2 fails on this APK):

```
adb -s emulator-5554 pull /system/framework/framework-res.apk
adb -s emulator-5554 pull /product/overlay/framework-res__sdk_gphone64_x86_64__auto_generated_rro_product.apk
aapt2 dump xmltree --file res/xml/power_profile.xml <apk>
```

The vendor-partition RRO (`framework-res__sdk_gphone64_x86_64__auto_generated_rro_vendor.apk`) could not be pulled (`adb: failed to stat remote object ... Permission denied`). That is not a gap in the fixture: the product RRO matches the capture, and product outranks vendor in overlay priority, so the vendor RRO's `power_profile.xml` (if any) is not what the runtime resolved.

The committed `power_profile.xml` in the fixture directory is a **reconstruction**, built mechanically from the committed aapt2 dump: same elements, same text, same line numbers (so `Provenance.fileLine` matches what `XmlResourceParser` reported on the device); aapt2 does not preserve comments, so the original's comments and the blank lines where they stood are blank. The gate's `reconstructionMatchesTheCapture` test pins the reconstruction to the capture — declared key set and line numbers — so the replay cannot silently drift from what the device saw.

## Gate 3's ANE-LX2 fixture provenance (2026-09-13)

The API 28 fixture is the one instance 4 is about, and it is built from two different files, both pulled from the phone over adb on 2026-09-13 (device `WCY4C18518007154`, the same physical ANE-LX2 the 2026-09-10 capture ran on):

1. `/product/etc/xml/power_profile.xml` — **the file the framework prices from** (per the 2026-09-10 NOTES record: `dumpsys batterystats --checkin` reports `pws,3000,...`, and the leading field of a `pws` row is `PowerProfile.getBatteryCapacity()`). Pulled verbatim: 2676 bytes, 20 keys (17 `<item>` + 3 `<array>`), pre-Lollipop schema, Apache-2.0 header intact. Every figure matches that independent 2026-09-10 record: `screen.on` 143, `screen.full` 414, `dsp.audio` 43 `k3v5`, `dsp.video` 176, three `TBD` comments, `battery.capacity` 3000, `radio.on` array 13.0 first, `cpu.speeds` 480000 first, `cpu.active` 107 first. Committed verbatim as `product_power_profile.xml` — this is the replay XML.
2. `framework-res.apk`'s `res/xml/power_profile.xml` — **the file the resource routes return**, 35 keys, `battery.capacity` 1000. The 51 MB APK was pulled and dumped with aapt2; the committed `framework-res.xmltree.txt` is that dump, and it pins the capture: all 35 declared keys present, and the five `line` rows (ambient.on 30, screen.on 31, screen.full 32, bluetooth.active 33, bluetooth.on 34) exact.

Tooling note: build-tools **36.0.0**'s aapt2 reads the API 36 google_apis emulator's APKs but fails with `error: failed opening zip: Invalid file` on this EMUI-era APK; **35.0.1**'s aapt2 reads it (both tried on the same pulled file, ZIP magic `50 4B 03 04` verified, 7862 zip entries). The older tool was used for the ANE dump.

The two files disagree on **all 13 keys they share** — `screen.on` 143 vs 0.1, `battery.capacity` 3000 vs 1000 — which is instance 4 made mechanical: the capture's `declared` rows document one file while the framework prices from the other. `AneLx2ReplayTest.theTwoProfilesDisagreeOnEverySharedKey` asserts it, so the finding cannot silently rot. Unlike the emulator fixture there is no reconstruction: the replay XML is the vendor's own bytes, comments and `TBD`s included, and no `line`-number check runs against it — the capture's `line` rows describe the framework-res file, and pinning them to `/product` would compare two different files. The gate was observed red before being trusted: array selection sabotaged to `.last()` made the 50-row replay fail (asserting element-0 selection, `PowerProfile.java:896`), and the model was restored byte-identical afterwards (`git diff` empty).
