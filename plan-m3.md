# Milestone 3 — the CI gates that can be built without a device

Run these IN ORDER under the autonomous rules in `.clinerules`. One file per step, gradle green,
commit, next step. Stop only on a red build or a genuine ambiguity.

**Goal:** move quality enforcement from human review into the build. Milestone 2 proved why — a stale
`audit.api` left gate 4 guarding nothing for four commits, and only a manual read caught it.

**Scope, and what is deliberately out of it:**

| gate (ARCHITECTURE §10) | this milestone |
| --- | --- |
| 1 — unit tests + purity + leaf | already done |
| 2 — `defaultProfileControl` | **built here**, steps M3-3 … M3-6 |
| 3 — `backfillCheck` | **not here.** Needs live `getAveragePower` captures off the API 34 and 36 emulators, 47 keys each. No emulator, no gate. |
| 4 — `apiSurfaceCheck` | **the type-scope half built here**, step M3-2. The committed `minApi` list is deferred: it asserts constants `:collect` hands to `Reading.Absent`, and `:collect` does not exist yet. An empty list is decoration. |

---

## Step M3-1 — `requiredKeys()` in `KeyTable.kt`

Gate 2 needs to know which keys a release is expected to define. Append to
`audit/src/main/kotlin/io/github/tahmid1999/sipper/audit/KeyTable.kt`, changing nothing else:

```kotlin
/**
 * The keys some calculator actually reads at this API level, so a profile that omits one is omitting
 * something the framework will price. NOT_READ rows are excluded: a key nothing reads is not required.
 */
public fun requiredKeys(apiLevel: Int): Set<String> =
    keyTable()
        .filter { apiLevel in it.apis && it.effect != Effect.NOT_READ }
        .map { it.key }
        .toSet()
```

Then add one test to `KeyTableTest.kt`: `requiredKeys(36)` is not empty, every element is also a key
in `keyTable()`, and `"screen.on"` is NOT in `requiredKeys(36)` — its 34–36 row is `NOT_READ`.

Verify: `.\gradlew.bat :audit:apiDump` (this adds public surface), then `.\gradlew.bat :audit:check`.
Commit: `audit: requiredKeys for the positive control`.

## Step M3-2 — gate 4, the type-scope half

ARCHITECTURE §2 requires that no public signature in `:audit` names a type outside `kotlin.*` and the
project package. A literal reading of that fails on a correct surface, because Kotlin's own builtins
render in the ABI as `java/lang/String`, `java/util/Map` and so on. So the check allows those exact
JVM representations and rejects everything else — which is what actually catches an `android.os.Bundle`
leaking through a `compileOnly` dependency, or a `javax.xml` type escaping the parser.

Append to `audit/build.gradle.kts`, and add it to the `check` dependency list alongside the two
existing gates:

```kotlin
// --- Gate: no public signature names a type outside kotlin.* and the project (ARCHITECTURE.md §2) ---
// The allowlist is the JVM rendering of Kotlin builtins, not a widening of the rule: Kotlin's String
// IS java/lang/String in the ABI. Anything else - android.*, javax.*, java.io.*, a third party - is
// a real leak, and a compileOnly dependency is exactly how one arrives without failing purity.
val checkAuditApiTypes by tasks.registering {
    val apiFile = layout.projectDirectory.file("api/audit.api").asFile
    inputs.file(apiFile)
    doLast {
        val allowedPrefixes = listOf("io/github/tahmid1999/sipper/", "kotlin/")
        val allowedExact = setOf(
            "java/lang/Object", "java/lang/String", "java/lang/Enum", "java/lang/CharSequence",
            "java/lang/Number", "java/lang/Comparable", "java/lang/Iterable", "java/lang/Throwable",
            "java/lang/annotation/Annotation",
            "java/util/List", "java/util/Map", "java/util/Set", "java/util/Collection",
        )
        val offenders = Regex("""[a-z][A-Za-z0-9]*(?:/[\w${'$'}]+)+""")
            .findAll(apiFile.readText())
            .map { it.value }
            .filterNot { type ->
                type in allowedExact || allowedPrefixes.any { type.startsWith(it) }
            }
            .toSortedSet()
        require(offenders.isEmpty()) {
            "public API of :audit names types outside kotlin.* and the project package: $offenders"
        }
    }
}
```

Verify: `.\gradlew.bat :audit:check` green, and `checkAuditApiTypes` appears in the task list as
executed. If it reports offenders, STOP and report them verbatim — do not widen the allowlist to make
the build pass. A genuine offender is a finding, not a configuration problem.

Commit: `audit: gate 4 type-scope check on the public API`.

## Step M3-3 — fetch the AOSP default profiles

These are the fixtures gate 2 runs against. Fetch with curl, decode, and do not edit them.

```
BASE=https://android.googlesource.com/platform/frameworks/base/+/refs/tags
PATH_IN_REPO=core/res/res/xml/power_profile.xml
```

- API 36 → tag `android-16.0.0_r1` → `audit/src/test/resources/aosp-defaults/36/power_profile.xml`
- API 34 → tag `android-14.0.0_r1` → `audit/src/test/resources/aosp-defaults/34/power_profile.xml`

```bash
curl -sL "$BASE/<TAG>/$PATH_IN_REPO?format=TEXT" | base64 -d > <DEST>
```

For each file report the line count and sha256, and confirm the decode is sound (it should begin with
an XML declaration or an Apache-2.0 comment block and contain `<device name="Android">`). Leave the
Apache-2.0 header intact — it is why step M3-4 exists.

If either URL 404s, do not substitute another path or another tag. List the directory and report what
is actually there:
`curl -sL "$BASE/<TAG>/core/res/res/xml/?format=TEXT" | base64 -d`

Verify: `.\gradlew.bat :audit:test` still green (the files are inert until M3-5).
Commit: `audit: AOSP default power profiles for api 34 and 36`.

## Step M3-4 — `NOTICE`

Create `NOTICE` at the repository root:

```
sipper
Copyright 2026 Tahmid Alavi Ishmam

This product includes software developed by The Android Open Source Project.

audit/src/test/resources/aosp-defaults/34/power_profile.xml
audit/src/test/resources/aosp-defaults/36/power_profile.xml
    Default power profiles from AOSP platform/frameworks/base, at tags android-14.0.0_r1 and
    android-16.0.0_r1 respectively, path core/res/res/xml/power_profile.xml. Committed verbatim with
    their Apache-2.0 headers intact. Licensed under the Apache License, Version 2.0.
```

No gradle run needed. Commit: `docs: NOTICE for the committed AOSP fixtures`.

## Step M3-5 — gate 2, the inverted positive control

Create `audit/src/test/kotlin/io/github/tahmid1999/sipper/audit/DefaultProfileControlTest.kt`.

**Read this before writing it.** The obvious form of this gate — "each release's default profile must
satisfy its own required-key set" — would define the project's headline out of existence, because the
AOSP default *is* the file the claim is about. So the gate asserts the default **fails** its own
required-key set, and breaks the build the day that stops being true.

The test is self-bootstrapping: on the first run there is no `expected-absent.txt`, so it fails with
the exact content to write. Step M3-6 writes it.

```kotlin
package io.github.tahmid1999.sipper.audit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class DefaultProfileControlTest {

    @Test
    fun api34DefaultFailsItsOwnRequiredKeys() = control(34)

    @Test
    fun api36DefaultFailsItsOwnRequiredKeys() = control(36)

    private fun resource(path: String): String? =
        javaClass.getResourceAsStream(path)?.use { it.readBytes().decodeToString() }

    private fun control(api: Int) {
        val xml = resource("/aosp-defaults/$api/power_profile.xml")
            ?: fail("missing fixture /aosp-defaults/$api/power_profile.xml")
        val model = ProfileModel(parseProfile(xml), api)
        val required = requiredKeys(api)
        val absent = required
            .filter { verdictOf(model.provenance(it)) == Verdict.ZERO_BY_ABSENCE }
            .toSortedSet()

        val committed = resource("/aosp-defaults/$api/expected-absent.txt")
            ?.lineSequence()
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() && !it.startsWith("#") }
            ?.toSet()
            ?: fail(
                "no committed expected-absent.txt for api $api. Write " +
                    "audit/src/test/resources/aosp-defaults/$api/expected-absent.txt " +
                    "containing exactly these ${absent.size} lines:\n" + absent.joinToString("\n"),
            )

        // 1 - the committed list is not stale.
        assertEquals(
            emptySet(), committed - required,
            "expected-absent.txt for api $api lists keys that are not required at that level",
        )

        // 2 - none of them is declared in the release's own default file.
        assertEquals(
            emptySet(), committed.filter { it in model.declared }.toSet(),
            "keys declared in the api $api default but listed as expected-absent",
        )

        // 3 - none of them resolves through a back-fill chain either.
        assertEquals(
            emptySet(), committed.filter { model.provenance(it) is Provenance.BackFilled }.toSet(),
            "keys back-filled at api $api but listed as expected-absent",
        )

        // 4 - each one audits to exactly ZERO_BY_ABSENCE, and the file as a whole still fails.
        committed.forEach {
            assertEquals(Verdict.ZERO_BY_ABSENCE, verdictOf(model.provenance(it)), it)
        }
        assertTrue(
            absent.isNotEmpty(),
            "positive control passed, which means the claim is gone: the api $api default " +
                "power_profile.xml now satisfies requiredKeys($api). Either the file changed " +
                "upstream or requiredKeys($api) is wrong. Do not delete this gate to make the " +
                "build green.",
        )
        assertEquals(
            committed, absent.toSet(),
            "the set of absent required keys drifted from the committed list for api $api",
        )
    }
}
```

Verify: `.\gradlew.bat :audit:test` — it is EXPECTED TO FAIL here, twice, each failure naming the
lines to write. Report both failure messages verbatim. **Do not commit a red build**; go straight to
M3-6 and commit once together.

## Step M3-6 — write the two `expected-absent.txt` files

From the two failure messages in M3-5, create:

- `audit/src/test/resources/aosp-defaults/34/expected-absent.txt`
- `audit/src/test/resources/aosp-defaults/36/expected-absent.txt`

One key per line, exactly the lines the failure message listed, in that order, no extra text and no
trailing blank lines. Above them, one comment line:

```
# Keys required at this API level that the AOSP default profile does not define. See ARCHITECTURE §10 gate 2.
```

Then `.\gradlew.bat :audit:check` must be green. Commit M3-5 and M3-6 together as
`audit: gate 2, the inverted positive control`.

**If a fixture turns out to define every required key**, the gate is telling you something real: stop
and report it, because it means either the AOSP default changed or `requiredKeys()` is wrong. Do not
write an empty expected-absent.txt to make it pass.

## Step M3-7 — CI

Create `.github/workflows/check.yml`:

```yaml
name: check
on:
  push:
  pull_request:
jobs:
  jvm:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew clean check --no-daemon
```

No second job yet — the SDK job that runs `assembleDebug` and `lint` with `NewApi` as an error belongs
with `:app`, which does not exist.

Note for the human, not for you: the wrapper scripts need the executable bit on Linux. If CI fails with
`Permission denied`, the fix is `git update-index --chmod=+x gradlew` committed once.

Verify: nothing to run locally. Commit: `ci: run gradlew check on every push`.

---

## Report at the end

The step list, every commit, the final test counts, and — for M3-3 — the line count and sha256 of both
fetched fixtures.

## Not in this milestone

- **Gate 3** (`backfillCheck`) — blocked on emulator captures.
- **The committed `minApi` list** in gate 4 — blocked on `:collect` existing.
- **`initModem`** — the verdict question is settled in CONTRACT §3.1, but the packed drain-type/RAT/
  frequency-range key space needs its own design.
- **`keytable.tsv` effect provenance** — the table carries source-verified and inherited-unverified
  effect labels indistinguishably. `KEYTABLE-ROWS.md` marks which is which; a tenth `effectVerified`
  column would make it mechanical. Not attempted here.
