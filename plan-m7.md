# Milestone 7 — the on-device probe

ONE STEP PER PROMPT.

**Why this exists.** Three questions cannot be answered off-device, and all three need code running on
a real Android runtime:

1. Does `XmlResourceParser.getLineNumber()` return anything usable on AAPT2-compiled XML? If not,
   `Provenance.fileLine` is meaningless on the two routes that carry the verdicts.
2. What does `getAveragePower` actually return, per key, on a real device? Gate 3 (ARCHITECTURE §10)
   replays `ProfileModel.averagePower` against exactly this and requires exact `Double` equality.
3. Does the emulator's own `power_profile.xml` declare `screen.on` or `screen.on.display0`? The
   README's back-fill demonstration rests on the former, and the AOSP default at that tag has the
   latter. Recorded UNRESOLVED in `NOTES.md`.

**Two targets, answering different questions. Neither substitutes for the other.**

| target | answers | why it is not the other |
| --- | --- | --- |
| ANE-LX2, API 28 | 1, 2 at the level where **no back-fill applies** | the phone instance 4 is about; its profile is the vendor's, not AOSP's |
| emulator, API 34 and 36 | 2, 3, at the levels where **back-fill does** apply | what gate 3 is specified against |

**On the "47 keys".** ARCHITECTURE §10 gate 3 says 47 rows, but no 47-key list exists anywhere in this
repository. Do not invent one. The probe defines its set as the **union** of every key in
`keytable.tsv` and every key the device's own resolved profile declares, and records that this is how
the set was chosen. If that lands on a number other than 47, the number in §10 is what changes.

---

## Step M7-1 — instrumented test infrastructure in `:collect`

Add to `collect/build.gradle.kts`:

- inside `android { defaultConfig { ... } }`:
  `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"`
- a `dependencies` addition:
  ```kotlin
  androidTestImplementation("androidx.test.ext:junit:1.2.1")
  androidTestImplementation("androidx.test:runner:1.6.2")
  ```

**These two version numbers are unverified.** They are from memory, and memory has been wrong three
times already in this module. If either fails to resolve, report the exact Gradle error and STOP —
do not substitute a version you guessed at. `google()` is already in the repository list.

Note this touches only the `androidTest` source set, so it cannot reach the release AAR, and the
purity and dependency gates are unaffected. Confirm that by running the gates.

Verify: `.\gradlew.bat :collect:assembleRelease` and `.\gradlew.bat :collect:check`.
Commit: `collect: instrumented test infrastructure`.

## Step M7-2 — the probe

Create `collect/src/androidTest/kotlin/io/github/tahmid1999/sipper/collect/ProfileProbe.kt`.

It is a JUnit4 instrumented test that **records rather than asserts**. It must not fail on a value it
dislikes; its job is to report what the platform said. Every line goes to logcat with the tag
`SIPPERPROBE`, one field per line, tab-separated, so it can be pulled with
`adb logcat -d -s SIPPERPROBE:I` and turned into a fixture.

Emit, in this order:

```
device    <Build.FINGERPRINT>
api       <Build.VERSION.SDK_INT>
route     system     <Value|SilentDefault|Denied|Absent>   <declared key count>   <lines map size>
route     package    <Value|SilentDefault|Denied|Absent>   <declared key count>   <lines map size>
line      <key>      <lineNumber>          -- for the first five declared keys of the package route
declared  <key>      <first value>         -- every key the package route declared
reflect   <key>      <Value:number | Denied:permission | other>   -- for every key in the probe set
```

The probe set is `keyTable().map { it.key }.toSet()` unioned with the declared keys from whichever
resource route returned a `Reading.Value`. Sort it before probing so two runs are diffable.

The `line` rows are the point of question 1: if `getLineNumber()` returns `-1` or `0` for compiled
XML, that is the answer, and it must be reported rather than smoothed over.

Use `InstrumentationRegistry.getInstrumentation().targetContext` for the `Context`.
Wrap the whole body so an unexpected exception is logged as `error <class> <message>` and the test
still completes — a probe that dies halfway tells you less than one that reports where it stopped.

Verify: it compiles — `.\gradlew.bat :collect:assembleDebugAndroidTest`. Do not run it yet.
Commit: `collect: the on-device profile probe`.

## Step M7-3 — run it on the ANE-LX2

The phone is already connected over adb. Run:

```
.\gradlew.bat :collect:connectedDebugAndroidTest
```

Then capture the output:

```
adb logcat -d -s SIPPERPROBE:I
```

Save it verbatim to `bench/ane-lx2-api28.tsv`, stripping only the logcat prefix so each line is the
tab-separated payload. Do not edit a single value.

**This is somebody's daily phone.** The probe reads the power profile and nothing else — no usage
stats, no package list, no personal data. Keep it that way.

Commit: `bench: probe capture from the ANE-LX2 at API 28`.

## Step M7-4 — run it on emulators

Four AVDs are defined and API 34 and 36 system images are installed. Start one headless, wait for it,
run the same probe, capture to `bench/<avd>-api<level>.tsv`.

Record the emulator image build id — `adb shell getprop ro.build.fingerprint` — because ARCHITECTURE
§10 requires each capture to have a provenance rather than an origin story.

If an emulator will not boot on this machine, say so and stop. Do not fabricate a capture.

Commit: `bench: probe captures from the API 34 and 36 emulators`.

## Step M7-5 — `bench/probe-capture.md`

ARCHITECTURE §10 gate 3 requires it: the adb invocation and the image build id behind every capture,
so the numbers have a provenance. Record for each fixture: the exact gradle and adb commands, the
device fingerprint, the API level, the date, how many keys were probed, and how the probe set was
chosen (the union rule above, not a memorised 47).

Also answer, from the captures, the three questions this milestone exists for — including the
`getLineNumber()` result and whether the emulator declares `screen.on` or `screen.on.display0` — and
update the two UNRESOLVED entries in `NOTES.md` accordingly.

Commit: `bench: capture provenance, and three questions answered`.

## Not in this milestone

- **Gate 3 itself.** The replay task comes after there are captures to replay against.
- **`:app`.** Nothing renders yet; that is the next milestone and the largest.
