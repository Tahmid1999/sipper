# Milestone 8 — `:app`, the Compose front end

Run these IN ORDER under the autonomous rules in `.clinerules`. One file per step, gradle green,
commit, next step. Stop only on a red build or genuine ambiguity.

**Goal:** the shipping APK — five tabs (AUDIT APPS SELF PROBES DEVICE), the status strip, the
disclosure destination, and the WorkManager sampler. `:app` depends on all four modules
(ARCHITECTURE §1) and owns the UI.

**Sources, already loaded:** CONTRACT §1 (colour), §4 (AUDIT columns), §5 (APPS columns), §6 (font +
subset), §7 (zebra banned), §8 (start dest, sampler, API corrections), §9 (hatch, chrome,
ScrollTable, a11y, contrast); DESIGN (type roles, row heights, chips, ReadingCell, loading,
failure); FLOWS §1–§10 (first run, states, navigation, saved state, refresh, a11y, status strip);
SCREENS-AUDIT-PROBES; SCREENS-APPS-SELF-DEVICE.

**Discrepancies found while planning, logged rather than propagated:**
- DESIGN §13 / CONTRACT §6 name `dev/tahmid/sipper` paths. ARCHITECTURE §1 pins the package root
  `io.github.tahmid1999.sipper.<module>`; the app module's root is therefore
  `io.github.tahmid1999.sipper.app` and the stale doc paths are not used.
- SCREENS-AUDIT-PROBES §3 says the screen state types (`RouteReading`, `Agreement`, `KeyDiff`,
  `AuditRow`, `CalculatorGroup`, `AuditUiState`, `ProbeRow`, `ProbePair`, `ProbeSide`) are declared
  in `docs/ARCHITECTURE.md`. They are not there. They are declared in `:app` from the behaviour the
  screen docs specify, and the doc gap is reported, not silently fixed.
- The `no-ai-looking-code-and-ui` doc referenced by SCREENS-AUDIT-PROBES §0 does not exist in this
  repository. Nothing in the build can follow it; noted in NOTES.
- The two Roboto Mono TTFs do not exist in the repo. They are downloaded once, verified
  Apache-2.0, recorded in NOTICE. Without them the font gate in step M8-5 cannot pass.
- DEVICE is milestone E9 and optional (FLOWS §4.6). This milestone builds the four-tab shell with
  DEVICE rendering its `Reading`-per-zone content per its screen doc; if any DEVICE detail proves
  ambiguous, the step stops and asks rather than inventing.

**Pinned versions:** as libs.versions.toml — AGP 9.1.0, Kotlin 2.4.10. Compose BOM, navigation,
lifecycle, WorkManager, icons-core are added with versions resolved from Google's Maven when the
step runs; if any fails to resolve, the exact Gradle error is reported, not guessed past.
`applicationId io.github.tahmid1999.sipper`.

**Gradle command per step:** `.\gradlew.bat :app:compileDebugKotlin` for UI files,
`:app:test` for pure-logic files, root `check` at milestone end.

---

## Phase A — module and theme

### Step M8-1 — the module
- `gradle/libs.versions.toml`: add `composeBom`, `activityCompose`, `navigationCompose`,
  `lifecycleRuntimeCompose`, `workRuntime`, `materialIconsCore`.
- root `build.gradle.kts` and `settings.gradle.kts`: the application plugin alias and
  `include(":app")`; `google()` is already in both repository blocks.
- `app/build.gradle.kts`: `applicationId io.github.tahmid1999.sipper`, `namespace
  io.github.tahmid1999.sipper.app`, `minSdk 28`, `compileSdk 36`, `targetSdk 36`. Kotlin
  `explicitApi()` is **not** applied to `:app` (ARCHITECTURE §2 applies it to `:audit` and
  `:usage` only). Compose enabled, deps on all four projects, `testImplementation(kotlin("test"))`.
- `app/src/main/AndroidManifest.xml`: no permissions, `<queries>` MAIN/LAUNCHER +
  `com.android.settings`, one exported activity.
- Verify: `.\gradlew.bat :app:assembleDebug` produces an APK. Commit: `app: module scaffold`.

### Step M8-2 — colour tokens (`ui/theme/SipperColor.kt`)
Every hex from CONTRACT §1.1/§1.2/§1.3 verbatim — 4 surfaces, 2 rules, 3 text, 1 accent, 6 state
hues, hatch ground/line — as an immutable `SipperColors` class + `LocalSipperColors`, switched by
`isSystemInDarkTheme()`, no dynamic colour anywhere (CONTRACT §1).
Verify: `:app:compileDebugKotlin`. Commit: `app: colour tokens`.

### Step M8-3 — type roles (`ui/theme/SipperType.kt`)
`SipperType` object with `mono`, `monoMedium`, `monoChip`, `monoStrip`, `monoBlock`, `monoNumeric`
per DESIGN §2. Both `FontFamily.Default` and the bundled mono family.
Verify: `:app:compileDebugKotlin`. Commit: `app: type roles`.

### Step M8-4 — `ReadingCell` + hatch + chips (`ui/ReadingCell.kt`)
CONTRACT §9.3 hatch loop verbatim in a `Modifier.hatch()`; `ReadingCell` per ARCHITECTURE §4's
exact signature — `format` reachable only from the `Value` branch, exhaustive `when`, no `else`.
`ChipCell` for `Denied` (tappable, opens the one-line grant sheet) and `Absent` (em dash).
`describeReading()` beside it. The `@OptIn(AuditOnly::class)` opt-in lives only in the demo and
the expanded rows, never here.
Verify: `:app:compileDebugKotlin`. Commit: `app: ReadingCell, the render contract`.

### Step M8-5 — the font bundle + subset gate
- Download Roboto Mono 400/500 TTFs (Apache-2.0) into `app/src/main/res/font/`.
- `ui/theme/MonoSubset.kt` — the CONTRACT §6 codepoint list as a set.
- JVM test `MonoSubsetTest` asserting the subset covers every codepoint of the committed string
  constants it can reach.
Verify: `:app:test` + `:app:assembleDebug`. Commit: `app: bundled mono face and the subset gate`.

### Step M8-6 — `ScrollTable` primitive (`ui/ScrollTable.kt`)
CONTRACT §9.5's three rules: one gesture node via `Modifier.scrollable` on the wrapping Box,
layout-only rows via `place(-offset)`, frozen column outside the shifted region,
`TableColumn(id, title, chars, align)` with `width = chars × advance + 12dp`, `rememberDigitAdvance()`
keyed on Density + mono only (never `LocalConfiguration`), the frozen edge drawn not composed,
`TableScrollState` saveable as one Int.
Verify: `:app:compileDebugKotlin`. Commit: `app: the ScrollTable primitive`.

### Step M8-7 — column tables + gates
- `ui/Columns.kt` — AUDIT's six columns (CONTRACT §4) and APPS's fourteen (CONTRACT §5).
- JVM test `ColumnsTest`: each list sums to the §4 (700dp) and §5 (1059dp) totals, computed from
  the same `advance` the contract states (7.2dp).
- `contrastDoc`/`columnTableDoc` tasks per CONTRACT §9.7/§9.5.
Verify: `:app:test`. Commit: `app: committed column sets and their gates`.

## Phase B — shared chrome

### Step M8-8 — `Screen` + nav host + `StatusStrip` (`ui/SipperApp.kt`, `ui/StatusStrip.kt`)
`Screen` sealed interface with the six destinations (FLOWS §5), `PrimaryTabRow` 48dp five
text-only tabs, start destination AUDIT always, tab taps `popUpTo(Audit) { saveState = true }` +
`restoreState` + `launchSingleTop`, `Disclosure` hides the tab row, no TopAppBar. StatusStrip per
FLOWS §10 + SCREENS-AUDIT-PROBES §0.1: 32dp, `surfaceRaised`, `ruleStrong` 1dp top, `monoStrip`,
` · ` separators, segments ≥48dp tappable, scrolls horizontally on overflow, never truncated.
Verify: `:app:compileDebugKotlin`. Commit: `app: navigation shell and status strip`.

## Phase C — the screens

### Step M8-9 — AUDIT (part 1: state and routes)
`audit/` package: `AuditUiState` (Loading/Ready/NoProfile), `RouteReading`, `Agreement`,
route digests (sha256 over normalised `key=token` sorted, no Double parse), provenance block,
capacity cross-check, counts line, grouping state. `AuditViewModel` launching the three route
reads + file probe on `Dispatchers.IO`, verdicts via `ProfileModel`/`verdictOf`.
Verify: `:app:compileDebugKotlin`. Commit: `app: AUDIT state and routes`.

### Step M8-10 — AUDIT (part 2: the table)
Grouped rows per calculator, 32dp rows, header 30dp, group header 24dp sticky, verdict chips 1dp
border no fill 3dp radius, `≠` column, literal-token values, expanded row with
`platform returned:` behind `@OptIn(AuditOnly::class)`, the primary-route caveat line, the
`MoreVert` overflow. Copy verbatim from SCREENS-AUDIT-PROBES.
Verify: `:app:compileDebugKotlin`. Commit: `app: the AUDIT table`.

### Step M8-11 — PROBES
`probes/`: `ProbeRow`/`ProbePair`/`ProbeSide` types, the four paired demonstrations (both halves
identical, cause strip the only hatch), sections A–D, per-row run + `[ re-run all ]`, denials
verbatim, `[ grant ]` → Disclosure. `SilentDefaultDemo.kt` with the file-level
`@OptIn(AuditOnly::class)`.
Verify: `:app:compileDebugKotlin`. Commit: `app: PROBES`.

### Step M8-12 — APPS (part 1: state, query, projection)
`apps/`: `AppsSort`, `AppsQuery`, `AppsRow`, the pure `projectApps` + JVM tests, `AppsViewModel`
with the `combine(rowsFlow, queryFlow)` projection, SavedStateHandle keys per FLOWS §6.1
(`apps.buckets` as IntArray, the Set rebuilt in the ViewModel).
Verify: `:app:test` + `:app:compileDebugKotlin`. Commit: `app: APPS state and projection`.

### Step M8-12b — APPS (part 2: the grid)
The 14-column ScrollTable per CONTRACT §5, 28dp rows, sticky 30dp header, filter/window chips,
summary line, `PullToRefreshBox`, expanded row with the lane chart (pure `layoutLanes` + Canvas,
shutdown gaps as hatched bands, no interpolation), the label column's three cases,
`LauncherlessMarker`, bucket int-first.
Verify: `:app:compileDebugKotlin`. Commit: `app: the APPS grid`.

### Step M8-13 — the remaining `:collect` collectors
Battery broadcast + `BATTERY_PROPERTY_*`, thermal sysfs walk (Temp detector per SCREENS-APPS §1.3),
`/proc/meminfo`, `/proc/cpuinfo`, per-cluster cpufreq, usage events → `RawEvent`, `NetworkStats`
(branched for the API 28 `TYPE_MOBILE` subscriber-id constraint), package-manager reads — every one
returning `Reading<T>` per the module gate, lint `NewApi` as error.
Verify: `:collect:assembleRelease` + `:collect:check`. Commit: `collect: the remaining collectors`.

### Step M8-14 — SELF + DEVICE
SELF: three stacked tables, the sampler panel + switch, `PullToRefreshBox`, wake-lock/FGS totals.
DEVICE: `Reading` per zone, the sysfs walk, battery/RAM/swap/cpufreq, cross-check lines with `→`.
Per their screen docs; if any DEVICE detail is ambiguous, the step stops and asks (E9, optional).
Verify: `:app:compileDebugKotlin`. Commit: `app: SELF and DEVICE`.

### Step M8-15 — the sampler
`data`: `sampler_run` table + repository. `:app`: `SamplingWorker` (6h interval, 1h flex, `KEEP`,
`RequiresBatteryNotLow`, linear 30s backoff, never a foreground service, never a
`PARTIAL_WAKE_LOCK`), enqueue on app start, SELF's run-log panel reads it.
Verify: `:data:test` + `:app:compileDebugKotlin`. Commit: `app: the six-hour sampler`.

### Step M8-16 — the disclosure destination + deep link
`Screen.Disclosure`: the prominent-disclosure copy from SCREENS-PROBES §2.4, `[ open Settings ]` →
`android.settings.USAGE_ACCESS_SETTINGS`, `[ not now ]` returns. DataStore keys
`disclosure_ack_at`, `disclosure_ack_version`, `sampler_enabled` only.
Verify: `:app:compileDebugKotlin`. Commit: `app: the disclosure destination`.

### Step M8-17 — gates, docs, full check
- `:app` gates: `checkAppDependencies` (all four projects, nothing else), `ContrastTest`
  (recomputes CONTRACT §1.4, fails below 4.5:1, three non-text exceptions allowlist),
  `contrastDoc` + `columnTableDoc`, the font-subset gate, `check` wiring.
- Compose UI test: the pinning test per CONTRACT §9.6 — on the hatched demo,
  `onNodeWithText("0.0", useUnmergedTree = true).assertDoesNotExist()` and the `manufactured`
  content description exists.
- Docs: ARCHITECTURE §2/§10 amended with the new gates; NOTES.md milestone entry; NOTICE records
  the TTFs.
- Verify: `.\gradlew.bat check` at the root — five modules, all gates.
- Commit: `app: milestone 8 complete`.

---

## Report at the end
Every commit, the final gate/test counts per module, the discrepancies logged above and their
resolution, and anything that stopped and asked.

## Not in this milestone
- DEVICE beyond its `Reading`-per-zone basics if ambiguities appear (E9, optional).
- The README screenshot and `probes.gif` captures — device session, post-build.
- Macrobenchmark numbers replacing FLOWS §1 budgets — device session, post-build.
- `tools/corpus` — post-STOP-B per ARCHITECTURE §1.


