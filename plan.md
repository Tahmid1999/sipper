# plan.md — Milestone 1: `:audit` foundation

**Goal of this milestone:** a green `./gradlew :audit:test`, with `Reading<T>`, the
verdict types, and their tests, plus the build scaffold and the two `:audit` purity gates.
Everything here is **pure Kotlin JVM — no Android, no Android SDK**. Buildable on JDK 17 alone.

**Authoritative sources (already loaded read-only):**
- `sipper-docs/docs/CONTRACT.md` §2 (`Reading<T>` — paste verbatim), §3 (back-fill is a Value)
- `sipper-docs/docs/ARCHITECTURE.md` §1 (modules), §2 (build gates), §3 (`Reading<T>` + `Verdict`)

Where anything here disagrees with CONTRACT.md, CONTRACT.md wins — stop and flag it.

**Scope discipline — do NOT do in this milestone:** no `ProfileModel`, no XML parsing, no
back-fill chain, no key table, no other module. Those are milestone 2+. Build only what is
listed below.

**Pinned versions (ARCHITECTURE.md §intro):** Kotlin `2.4.10`, JDK `17`. (AGP/SQLDelight/Compose
are NOT needed in `:audit` — do not add them here.)

---

## Step 0 — Gradle wrapper bootstrap (one-time; ask the human before running — 130MB download)

No system gradle exists, so bootstrap the wrapper once, then it self-installs forever after.
Run in PowerShell from `G:\sipper`:

```powershell
Invoke-WebRequest https://services.gradle.org/distributions/gradle-8.13-bin.zip -OutFile gradle-dist.zip
Expand-Archive gradle-dist.zip -DestinationPath .gradle-dist -Force
.\.gradle-dist\gradle-8.13\bin\gradle wrapper --gradle-version 8.13
Remove-Item gradle-dist.zip; Remove-Item -Recurse -Force .gradle-dist
```

Verify: `./gradlew --version` prints Gradle 8.13, JVM 17. **Stop for review.**

---

## Step 1 — root scaffold

Create, then stop for review:

- `settings.gradle.kts` — `rootProject.name = "sipper"`; `include(":audit")`;
  `pluginManagement { repositories { gradlePluginPortal(); mavenCentral() } }`;
  `dependencyResolutionManagement { repositories { mavenCentral() } }`.
- `gradle/libs.versions.toml` — `[versions]` kotlin = "2.4.10"; `[plugins]`
  `kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }`,
  `binaryCompat = { id = "org.jetbrains.kotlinx.binary-compatibility-validator", version = "0.16.3" }`.
- root `build.gradle.kts` — `plugins { alias(libs.plugins.kotlin.jvm) apply false; alias(libs.plugins.binaryCompat) apply false }`.
- `gradle.properties` — `org.gradle.caching=true`, `org.gradle.parallel=true`, `kotlin.code.style=official`.
- `.gitignore` — `.gradle/`, `build/`, `*.iml`, `.idea/` (keep `!gradle-wrapper.jar`).

Verify: `./gradlew projects` lists `:audit`. **Stop for review.**

## Step 2 — `:audit/build.gradle.kts`

- `plugins { alias(libs.plugins.kotlin.jvm); alias(libs.plugins.binaryCompat) }`
- `kotlin { explicitApi() }`
- `compilerOptions { allWarningsAsErrors.set(true) }`, `jvmToolchain(17)`
- `dependencies { testImplementation(kotlin("test")) }` — **no other production deps** (this is the claim the purity gate defends)
- `tasks.test { useJUnitPlatform() }`
- Wire the two gate tasks from ARCHITECTURE.md §2 verbatim: `checkAuditPurity` (allowlist =
  kotlin-stdlib + jetbrains annotations; assert no unlisted + no `aar`/`AgpVersionAttr` artifact)
  and `checkAuditIsLeaf` (testRuntimeClasspath project set == `[":audit"]`). Both `dependsOn`ed by `check`.

Verify: `./gradlew :audit:compileKotlin` succeeds. **Stop for review.**

## Step 3 — `Reading.kt`  (TDD: write Step 4 test FIRST, watch it fail to compile, then this)

Path: `audit/src/main/kotlin/io/github/tahmid1999/sipper/audit/Reading.kt`.
Paste **verbatim** from CONTRACT.md §2 (lines 148–204): the `Reading<T>` sealed interface with
`Value` / `SilentDefault` / `Denied` / `Absent`, the `@AuditOnly` annotation, `Route`,
`RouteKind`, `DefaultCause` (exactly 5 members), `Grant`. Do not rename a field, do not add
`getOrNull`/`map`/`fold`, do not add a 5th constructor. `SilentDefault` is NOT a data class —
keep the hand-written `equals`/`hashCode`/`toString` exactly as given.

## Step 4 — `ReadingTest.kt`

Path: `audit/src/test/kotlin/io/github/tahmid1999/sipper/audit/ReadingTest.kt`. Assert:
- two `SilentDefault` with equal `shown`/`cause`/`route` are `equals` and share `hashCode`;
- `SilentDefault.toString()` contains `cause=` and `route=` and does **not** contain the `shown` value;
- `Value` / `Denied` / `Absent` are data classes (equality by fields);
- reading `shown` in the test requires `@OptIn(AuditOnly::class)` (this documents the opt-in surface).

## Step 5 — `Verdict.kt`  (TDD: write Step 6 test first)

Path: `audit/src/main/kotlin/io/github/tahmid1999/sipper/audit/Verdict.kt`.
`enum class Verdict { PRESENT, ZERO_BY_EXPLICIT_VALUE, ZERO_BY_ABSENCE, BACK_FILLED }` (ARCHITECTURE.md §3).
Plus a pure function deriving verdict from a `Reading<Double>`, matching the table in CONTRACT.md §3
(lines 274–280) / ARCHITECTURE.md §3 exactly:
- `Value(v, res route)`, `v != 0.0` → `PRESENT`
- `Value(0.0, res route)` → `ZERO_BY_EXPLICIT_VALUE`
- `SilentDefault(0.0, KEY_ABSENT_FROM_PROFILE, …)` → `ZERO_BY_ABSENCE`
- `Value(v, Route(BACK_FILL, from))` → `BACK_FILLED`
"res route" = `RouteKind.SYSTEM_RESOURCES` or `ANDROID_PACKAGE_RESOURCES`. Decide the signature to
match how §3 frames it; if the contract is ambiguous on a case, stop and ask — do not invent one.

## Step 6 — `VerdictTest.kt`

One assertion per row of the §3 table above, using real `Reading`/`Route` values.

## Step 7 — API dump + full check

- `./gradlew :audit:apiDump` → commit `audit/api/audit.api`.
- `./gradlew :audit:check` green (tests + `checkAuditPurity` + `checkAuditIsLeaf`).

**Stop. Report the green output.** Milestone 2 (`ProfileModel`, XML parse, back-fill, key table)
is planned separately after this is reviewed.

---

## How each step is executed
1. Do ONE step. 2. Aider auto-commits. 3. Human reviews the diff (Claude review pass on hard steps).
4. Next step. Never run ahead. If a build/version conflict appears (e.g. Kotlin 2.4.10 ↔ Gradle 8.13),
report the exact error and stop — do not silently downgrade a pinned version.

---

# Milestone 2 — `:audit` profile model + verdicts

**Goal:** parse a `power_profile.xml` into a `ProfileModel`, reproduce `getAveragePower`
(back-fill included), and assign a `Verdict` per key. Still **pure Kotlin JVM** — the JDK's
`javax.xml` parser is allowed (it's the JDK, not Android, and not a dependency; the purity gate
keys on Android artifacts, not JDK classes).

**Sources:** ARCHITECTURE §1 (ProfileModel shape), §5 (key table + Effect), §6 (back-fill chain);
CONTRACT §3 (verdict derivation, already implemented in `verdictFor`).

**Steps — one file each, stop + verify + commit, same rules as milestone 1:**

- **2a. `Provenance.kt`** — `Provenance` (Declared/BackFilled/NotDeclared), `Transform`
  (Identity/PerDisplay/ModemDrain), `BackFill` data class. Verbatim from ARCHITECTURE §6, with `public`.
- **2b. `ProfileParser.kt`** — parse `power_profile.xml` text → `declared: Map<String, List<Double>>`
  (a `LinkedHashMap`, preserving file order and `<array>` shape). Use `javax.xml` `DocumentBuilder`.
  Handle `<item name="k">v</item>` (single value) and `<array name="k"><value>v</value>…</array>`.
- **2c. `KeyTable.kt` + `keytable.tsv`** — `KeyFact`, `Cite`, `Effect` (ARCHITECTURE §5), plus a loader
  reading `audit/src/main/resources/keytable.tsv`. Then the TSV rows from §5's table.
- **2d. `BackFill.kt`** — the `initDisplays` (3 steps) + `initModem` chain model from §6:
  `(declared, apiLevel) -> Provenance` for a key.
- **2e. `ProfileModel.kt`** — `averagePower(key)` (reproduces `getAveragePower`, back-fill included),
  `provenance(key)`, `audit()`; plus `ProfileAudit` (key -> Verdict). Verdicts reuse `verdictFor`.
- **2f. Tests** — parser, back-fill chain, and verdicts against a small synthetic profile.
  (AOSP-default and emulator-probe fixtures for gates 2 and 3 come in a later milestone — they need
  real XML captures.)

Same lean context: CONTRACT + ARCHITECTURE + plan. Pre-decide every case in the prompt.

---

**Milestone 2 executable steps live in `plan-m2.md`.** Run those, in order, under the autonomous rules in `.clinerules`. The 2a-2f roadmap letters above are superseded by the M2-1..M2-5 steps in that file.
