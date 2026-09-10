# Probe Capture Provenance

Two benchmark fixtures, `ane-lx2-api28.tsv` and `Pixel_9_Pro_XL-api36.tsv`, record the results of running `ProfileProbe` on two Android devices. This document records the provenance of each capture: device fingerprint, API level, probe-set composition, and the adb invocations that produced them.

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
