# Architecture

> `docs/CONTRACT.md` fixes tokens, widths, types and the API levels below. This file owns
> modules, `Reading<T>`, the key table, the schema, the CI gates and scheduling. Where they
> disagree, the contract wins.

What each module owes the others, what the build refuses to let me do, and the handful of
decisions I expect to be argued with. The README says what sipper claims. This says how the claim
is kept true after I stop paying attention.

One thing up front, because it shapes everything below. sipper's claim is that it can tell a real
answer from a manufactured one and never prints the second as if it were the first. That is a
narrower claim than "these numbers are right", and it is the one the structure here keeps true:
the sealed `Reading<T>`, the four CI gates and the fact that `:audit` cannot see an Android class
all exist to make the property mechanical rather than a habit I might drop on evening nine.

Colour, type, row heights, the table primitive and every column width live in `docs/DESIGN.md`.
Screen layout, states, flows and copy strings live in `docs/SCREENS.md`. Nothing below restates
them.

Versions are pinned in `gradle/libs.versions.toml`: Kotlin 2.4.10, AGP 9.1.0, SQLDelight 2.3.2,
Compose BOM 2026.06.01, JDK 17. `minSdk 28`, `compileSdk 36`, `targetSdk 36`.

---

## 1. Modules

```
:audit ──────────────┐            pure Kotlin JVM, zero production deps
:usage ──────────────┤            pure Kotlin JVM, zero production deps
                     │
                     ├──> :collect        Android library
                     ├──> :data           SQLDelight, plain JVM
                     │        │
                     └────────┴──> :app   Compose

tools/corpus                              host-only CLI, post-STOP-B, never on :app's classpath
```

| module | what it owns | depends on | may not see |
| --- | --- | --- | --- |
| `:audit` | the profile schemas, the per-release key table, the back-fill chains, `Reading<T>`, `Verdict` | nothing | every other module, and `android.*` |
| `:usage` | the event-stream state machine and its residuals | nothing | every other module, and `android.*` |
| `:collect` | every read of the platform, each returning a `Reading<T>` | `:audit`, `:usage` | `:data`, `:app` |
| `:data` | SQLDelight schema, migrations, repositories | `:audit`, `:usage` | `:collect`, `:app` |
| `:app` | Compose, view models, deep links, the scheduler | all four | — |

Package roots are `io.github.tahmid1999.sipper.<module>`; the `applicationId` is `io.github.tahmid1999.sipper`.

### `:audit`

Parses a `power_profile.xml` — both schema eras, plus the API-34 per-display schema — into a
`ProfileModel`, then answers one question per key: is this value real, and if not, why not.

```kotlin
class ProfileModel(
    val declared: Map<String, List<Double>>,   // key -> value or <array> items, in file order
    val apiLevel: Int,
) {
    fun averagePower(key: String): Double      // reproduces getAveragePower, back-fill included
    fun provenance(key: String): Provenance
    fun audit(): ProfileAudit
}
```

`averagePower` exists so the model can be replayed against values captured from a real device
(gate 3, §10). It is a reimplementation of framework behaviour rather than a wrapper, and it is
the one place in the repo where a `0.0` may be produced without a `SilentDefault` around it: at
that layer it is deliberately imitating the framework's own fall-through so the gate can prove the
imitation is exact.

`:audit` also holds `Reading<T>`. That looks misplaced — the type describes collection, and
collection lives in `:collect` — but `:collect` is an Android library, so defining it there would
make the type the whole repo hangs on untestable off-device and unpersistable by `:data`.

### `:usage`

Consumes `List<RawEvent>` — a package name, a class name, an event type as a raw `Int`, and a
timestamp — and reconstructs foreground, visible and foreground-service intervals clipped exactly
to a window. It never touches `UsageEvents`; `:collect` drains the cursor into `RawEvent`s and
hands them over. §7.

### `:collect`

Every method on every collector returns `Reading<T>`. There is no method here that returns a bare
`Double`, `Boolean` or `Long` sourced from the platform, and a source-level gate in
`collect/build.gradle.kts`, `checkCollectReturnsReading`, is what keeps that true. It fails the
build if a public function in `:collect` declares a bare `Double`, `Boolean` or `Long` return type.
Collectors are ordinary classes taking a `Context`; there are no interfaces, because each has
exactly one implementation and a second one would be a fixture, which is what `:audit`'s and
`:usage`'s own test source sets are for.

The three power-profile read routes live here as three separate functions rather than one function
with a strategy parameter, because their disagreement is the finding and a strategy parameter
invites a caller to pick one and move on:

| route | mechanism | why it is in the set |
| --- | --- | --- |
| `SYSTEM_RESOURCES` | `Resources.getSystem().getIdentifier("power_profile", "xml", "android")` then `getXml()` | zygote AssetManager, folds in immutable RROs, may miss mutable ones |
| `ANDROID_PACKAGE_RESOURCES` | `getResourcesForApplication("android")` | built from the android package's own `ApplicationInfo` including overlay dirs; the primary, and the route that fills the value column on AUDIT |
| `POWER_PROFILE_REFLECTION` | `com.android.internal.os.PowerProfile` | cross-check, never carries a claim |

Reflection never carries a claim because `getAveragePower(String)` is on the warn-only
`unsupported` list today with no `maxTargetSdk`, and Google's stated policy is that unsupported
members may be restricted later. Related, and measured: hidden-API denial arrives as
`NoSuchMethodException`, not `SecurityException`. A tool that catches the wrong one reports a
denial as "method removed", which loses the distinction between a member that is blocked and a
member that is gone.

The primary-route choice has a cost on my own phone, and AUDIT states it on the screen rather than
in a footnote: on the ANE-LX2 route B resolves `framework-res.apk`, capacity 1000, while
`dumpsys pws` reports 3000, so the framework is pricing from `/product/etc/xml` and the verdicts
describe a file it does not read. The wording of that line is in `docs/SCREENS.md`.

**One `unsafeCheckOpNoThrow`, and it is branched.** That method is API 29 and `minSdk` is 28, so
unbranched it is a `NoSuchMethodError` on my only physical device, in the path that runs on every
resume. There is one implementation, here, called from everywhere:

```kotlin
val mode = if (Build.VERSION.SDK_INT >= 29) {
    appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, packageName)
} else {
    @Suppress("DEPRECATION")
    appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, packageName)
}
val granted = mode == AppOpsManager.MODE_ALLOWED   // MODE_DEFAULT is denied for this op
```

`lint { error += "NewApi" }` in `:app` and `:collect`, and the SDK CI job runs lint, so this class
of defect fails the build rather than the phone.

Every `minApi` constant this module hands to `Reading.Absent` is read off `android.jar` at E0 —
`javap -classpath $ANDROID_HOME/platforms/android-36/android.jar android.app.job.JobScheduler`,
and `api/current.txt` for the 34/35/36 additions — and committed to the `apiSurfaceCheck` list.
`getPendingJobReasons` in particular is not asserted from memory. Added-at and behaves-this-way-from
are different numbers and both are stored; see §7 and `docs/SCREENS.md` for what PROBES renders.

### `:data`

A plain Kotlin JVM module rather than an Android library, for the same reason splitline's `:data`
is: its tests then run against a real embedded sqlite file under plain JUnit with no emulator and
no Robolectric. `:app` supplies `app.cash.sqldelight:android-driver`; `:data` itself depends on the
JDBC driver alone, and that dependency is `implementation`, not `api`, so a driver choice cannot
leak into a repository signature.

### `:app`

Compose, five screens, one scheduler. Nothing else. The rule that a `SilentDefault` never renders
as a number is enforced here by a signature rather than a convention — §4.

---

## 2. What the build refuses to let me do

Three checks, all wired into `check` so they run on every CI job rather than on a task somebody has
to remember.

### Purity, keyed on the artifact, never on its name

```kotlin
// audit/build.gradle.kts (usage/build.gradle.kts is the same file with one word changed)
val agpVersionAttr = Attribute.of("com.android.build.api.attributes.AgpVersionAttr", String::class.java)
val artifactType = Attribute.of("artifactType", String::class.java)

val runtimeArtifacts: Provider<Set<ResolvedArtifactResult>> =
    configurations.named("runtimeClasspath").flatMap { it.incoming.artifacts.resolvedArtifacts }

val checkAuditPurity by tasks.registering {
    val artifacts = runtimeArtifacts
    val allowed = setOf("org.jetbrains.kotlin:kotlin-stdlib", "org.jetbrains:annotations")
    doLast {
        val extra = artifacts.get().filter {
            it.id.componentIdentifier.displayName.substringBeforeLast(':') !in allowed
        }
        // Two assertions, not one. The allowlist enforces the claim I make today
        // ("zero production dependencies"); the attribute check is the one that
        // survives the day I widen the allowlist for something innocent.
        //
        // Group prefixes are not evidence and I will not key on them: com.squareup.*
        // publishes both JVM and Android artifacts under a single group, and an
        // androidx artifact republished under a vanity group walks straight past
        // startsWith("androidx."). artifactType and the AgpVersionAttr that AGP
        // stamps onto every variant it publishes are properties of the artifact
        // itself, so they cannot be spoofed by naming.
        val android = artifacts.get().filter { art ->
            art.variant.attributes.getAttribute(artifactType) == "aar" ||
                art.variant.attributes.getAttribute(agpVersionAttr) != null
        }
        require(extra.isEmpty() && android.isEmpty()) {
            buildString {
                append("audit must depend on nothing but the Kotlin standard library")
                if (extra.isNotEmpty()) {
                    append("; unlisted: ").append(extra.joinToString { it.id.displayName })
                }
                if (android.isNotEmpty()) {
                    append("; android artifact: ").append(android.joinToString { it.id.displayName })
                }
            }
        }
    }
}
tasks.named("check") { dependsOn(checkAuditPurity) }
```

### `:audit` is a leaf, and one file says so

```kotlin
val checkAuditIsLeaf by tasks.registering {
    // testRuntimeClasspath, not runtimeClasspath: the stronger claim is that
    // :audit's own tests cannot reach :collect either, so no golden fixture in
    // this module can have been produced by a device call at test time. Every
    // fixture is a committed file with a recorded capture, and this is what
    // stops that from quietly stopping being true.
    val projects = configurations.named("testRuntimeClasspath").map { cfg ->
        cfg.incoming.resolutionResult.allComponents
            .map { it.id }.filterIsInstance<ProjectComponentIdentifier>()
            .map { it.projectPath }.toSet()
    }
    doLast {
        require(projects.get() == setOf(":audit")) {
            "audit must not depend on another project, found: ${projects.get() - ":audit"}"
        }
    }
}
```

That is the point of the arrangement: the provenance question — *could an audit verdict have been
produced by something that read the device?* — is answered by reading one build file rather than by
tracing a call graph.

### The API surface is a committed file

`:audit` and `:usage` set `explicitApi()` and `allWarningsAsErrors`, and
`org.jetbrains.kotlinx.binary-compatibility-validator` writes `audit/api/audit.api` and
`usage/api/usage.api`. A second assertion in the same gate fails if any public signature in either
module names a type outside `kotlin.*` and `io.github.tahmid1999.sipper.*`. That is not redundant with the
purity check: a `compileOnly` dependency on `android.jar` never appears on a runtime classpath, so
an `android.os.Bundle` in a public signature would pass purity and fail here.

Its second job is negative. `Reading<T>` has no `getOrNull()`, no `orElse()`, no `map()`, no
`fold()`, and `SilentDefault` has no `componentN` and no `copy`. Those absences are the enforcement
in §4, and a committed `.api` dump is how an absence gets defended.

---

## 3. `Reading<T>`

Defined in `:audit`, in `Reading.kt`. This block appears in no other document; the rest of the set
names constructors and fields and links here.

```kotlin
sealed interface Reading<out T> {

    /** The platform answered, and the answer came from the route named. */
    data class Value<out T>(val value: T, val route: Route) : Reading<T>

    /**
     * The platform answered, the answer is not an answer, and `shown` is what a caller that did
     * not know would have printed. Not a data class: `copy()` and `component1()` would hand out
     * `shown` without the opt-in.
     */
    class SilentDefault<out T>(
        @property:AuditOnly val shown: T,
        val cause: DefaultCause,
        val route: Route,
    ) : Reading<T> {
        override fun toString(): String = "SilentDefault(cause=$cause, route=$route)"
        override fun equals(other: Any?): Boolean =
            other is SilentDefault<*> &&
                @OptIn(AuditOnly::class) (shown == other.shown) &&
                cause == other.cause && route == other.route
        override fun hashCode(): Int =
            31 * (31 * @OptIn(AuditOnly::class) shown.hashCode() + cause.hashCode()) + route.hashCode()
    }

    /** A permission or appop is missing, and `howToGrant` says whether that is fixable. */
    data class Denied(val permission: String, val howToGrant: Grant) : Reading<Nothing>

    /** The API does not exist below `minApi`; this device is below it. */
    data class Absent(val minApi: Int) : Reading<Nothing>
}

@RequiresOptIn(level = RequiresOptIn.Level.ERROR, message = "Reads a manufactured value.")
@Retention(AnnotationRetention.BINARY)
annotation class AuditOnly

data class Route(val kind: RouteKind, val detail: String)

enum class RouteKind {
    SYSTEM_RESOURCES, ANDROID_PACKAGE_RESOURCES, POWER_PROFILE_REFLECTION, BACK_FILL,
    BATTERY_BROADCAST, BATTERY_PROPERTY, PROC_FILE,
    USAGE_EVENTS, USAGE_INTERVALS, NETWORK_STATS, APP_OPS, PACKAGE_MANAGER,
}

enum class DefaultCause {
    KEY_ABSENT_FROM_PROFILE,     // getAveragePower fell through to `return defaultValue`
    NO_USAGE_ACCESS,             // isAppInactive without the appop, API 30+
    PACKAGE_NOT_VISIBLE,         // isIgnoringBatteryOptimizations outside <queries>, API 31+
    APPOP_DEFAULT_MODE,          // AppOpsService returned opToDefaultMode(code)
    UNCLIPPED_INTERVAL_BUCKET,   // queryUsageStats summed buckets wider than the window
}

sealed interface Grant {
    /** One toggle the user flips themselves; `appOp` is how the grant is read back. */
    data class SettingsToggle(val action: String, val uri: String?, val appOp: String) : Grant
    /** A manifest line I could add and chose not to, with the line quoted. */
    data class ManifestQueries(val entry: String) : Grant
    /** signature|privileged with no `development` flag. Not even adb reaches it. */
    data object Unreachable : Grant
}
```

`route` is on `SilentDefault` as well as on `Value` because "which of the three routes produced this
manufactured answer" is a question AUDIT asks constantly, and losing it there makes a route
disagreement unattributable.

### Why the three-way split is the whole repo

`SilentDefault` is the case that has no error.

| call | returns | reading | what collapsing loses |
| --- | --- | --- | --- |
| `isAppInactive("com.foo")` with usage access | `false` | `Value(false, …)` | — |
| `isAppInactive("com.foo")` without it, API 30+ | `false` | `SilentDefault(false, NO_USAGE_ACCESS, …)` | identical bytes to the row above |
| `isIgnoringBatteryOptimizations` on a visible package | `false` | `Value(false, …)` | — |
| the same call on a package outside `<queries>`, API 31+ | `false` | `SilentDefault(false, PACKAGE_NOT_VISIBLE, …)` | identical bytes to the row above |
| `getAveragePower("wifi.controller.rx")` where the file says `0` | `0.0` | `Value(0.0, …)` | — |
| `getAveragePower("cpu.suspend")` where the file has no such key | `0.0` | `SilentDefault(0.0, KEY_ABSENT_FROM_PROFILE, …)` | identical bytes to the row above |
| `queryUsageStats` for a package with no usage access | throws or empties | `Denied(PACKAGE_USAGE_STATS, SettingsToggle(…))` | the user can fix this one; nothing above is fixable by them |

Three distinctions, each of which a two-case type destroys:

**Against `Denied`.** `Denied` is recoverable and carries the recovery. `SilentDefault` mostly is
not: `KEY_ABSENT_FROM_PROFILE` is a fact about the ROM, and no toggle changes it. Merging them puts
a "grant this" affordance under a row where granting nothing helps.

**Against a genuine negative.** Rows 3 and 4 return the same `false` for unrelated reasons. One
means the app is not allowlisted; the other means I cannot see the app at all. Every tool I have
read renders both as "not allowlisted".

**Against `null`, `Result`, or an exception.** All three permit `?: 0.0`, `.getOrDefault(0.0)`, or
an empty catch, and the value that came back is genuinely useful — it is exactly what the platform
would have shown the user, which is the thing the audit compares against. So `SilentDefault` carries
the value and refuses to let it be a number. A nullable would have made the bug a one-character fix
in the wrong direction.

### `Verdict` is a function of `Reading`, not a parallel taxonomy

```kotlin
enum class Verdict { PRESENT, ZERO_BY_EXPLICIT_VALUE, ZERO_BY_ABSENCE, BACK_FILLED }
```

| reading for a profile key | verdict |
| --- | --- |
| `Value(v, res route)`, `v != 0.0` | `PRESENT` |
| `Value(0.0, res route)` — the key is in the file and the file says zero | `ZERO_BY_EXPLICIT_VALUE` |
| `SilentDefault(0.0, KEY_ABSENT_FROM_PROFILE, …)` | `ZERO_BY_ABSENCE` |
| `Value(v, Route(BACK_FILL, fromKey))` | `BACK_FILLED` |

The two zeroes are separate verdicts because they have different arithmetic downstream, not because
the distinction reads well. An explicit `0` in a divisor key gives `Infinity`, or `NaN` where the
numerator is also zero; an absent one gives `0.0` and prices the component at nothing. §5 lists
which keys are divisors.

`wifi.controller.*`, `modem.controller.*`, `gps.voltage` and `gps.signalqualitybased` are present
with a literal `0` in the AOSP placeholder. `cpu.cluster_power.*`, `cpu.core_power.*`, `cpu.suspend`,
`bluetooth.controller.*` and the audio/video/camera keys are absent on the ANE-LX2's live profile.

### Back-fill is a `Value`

On API 34+ `PowerProfile.initDisplays` and `initModem` synthesise new-schema keys from deprecated
ones and log the deprecation, naming the replacement key. The platform says so, and "the platform
said nothing" is the definition of `SilentDefault`. So a back-filled key is
`Reading.Value(v, Route(RouteKind.BACK_FILL, fromKey))`, it renders as a real number with a
provenance chip, and it is never hatched. `DefaultCause` has no back-fill member and `Reading` has
no fifth constructor. §6 models the chains.

---

## 4. The render contract

`shown` is unreadable without `@OptIn(AuditOnly::class)`, and `ReadingCell` is the composable that
takes a `Reading`, with `format` reachable from the `Value` branch and nowhere else, over a `when`
that is exhaustive with no `else`:

```kotlin
@Composable
fun <T> ReadingCell(
    reading: Reading<T>,
    column: TableColumn,
    format: (T) -> String,
    speak: (T) -> String = format,
    modifier: Modifier = Modifier,
) {
    when (reading) {
        is Reading.Value -> ValueText(format(reading.value), reading.route, column, speak)
        is Reading.SilentDefault -> HatchCell(reading.cause, column)   // format is not in scope here
        is Reading.Denied -> ChipCell(Chip.Denied(reading.permission, reading.howToGrant), column)
        is Reading.Absent -> ChipCell(Chip.Absent(reading.minApi), column)
    }
}
```

Getting a number onto the screen from a manufactured answer requires writing a different composable
*and* an explicit `@OptIn`, which is two reviewable acts. A fifth `Reading` constructor breaks the
build at every render site, which is the intended cost of adding one. What each branch draws — the
hatch, the chips, the cause label — is in `docs/DESIGN.md`; the behavioural pin is the androidTest
there, which asserts against the unmerged tree that no manufactured number is in it.

`:app` carries one file-level `@OptIn(AuditOnly::class)`,
`app/src/main/kotlin/.../ui/probes/SilentDefaultDemo.kt`, where the manufactured `0.0` beside its
cause is the demo, and one function-level opt-in on the expanded detail row's `platform returned:`
line. Those two are the audit surface.

**Two mechanisms I dropped, with reasons.** A CI grep asserting that one file may mention
`SilentDefault` exits 1 in both directions as written, so it can never pass; it is defeated by a
file rename; and it rejects `SilentDefaultDemo.kt` by filename. A `fold(onValue, onSilent, onDenied,
onAbsent)`-only API costs every call site a four-lambda allocation to buy what the exhaustive `when`
already gives at compile time. Gate 4 (§10) backs the remaining mechanism by asserting against
`audit/api/audit.api` that `Reading` gains no accessor and that `SilentDefault` grows no `componentN`
or `copy`.

---

## 5. The per-release key table

A key's meaning is a function of the release, so the table is keyed on both. The same `screen.full`
is read by an inline helper at API 28, by a `PowerCalculator` at API 31, and by nothing at all at
API 36 except as a back-fill source.

```kotlin
data class KeyFact(
    val key: String,
    val apis: IntRange,
    val calculator: String?,     // null when nothing reads it at this level
    val effect: Effect,
    val cite: Cite,
)

data class Cite(
    val tag: String,      // an AOSP release tag, never a branch and never "main"
    val file: String,     // path from the frameworks/base root
    val line: Int,
    val anchor: String,   // the exact source text at that line
)

enum class Effect {
    ZERO_ZEROES_COMPONENT,  // multiplied into the component total; a 0 prices the whole component at 0
    ZERO_ZEROES_TERM,       // one addend of a sum; the rest of the component survives
    ZERO_DIVIDES,           // used as a divisor - a declared 0 gives Infinity or NaN, not 0
    ZERO_SELECTS_FALLBACK,  // the calculator branches on this being non-zero and silently uses another model
    NOT_READ,               // in the schema, read by nothing at this level
}
```

`Cite` carries a tag because a citation without one rots on the next release, and an anchor because
a citation with only a line rots on the next commit. The anchor is the match key. Path prefixes:

- `P` = `frameworks/base/core/java/com/android/internal/os/PowerProfile.java`
- `M` = `frameworks/base/core/java/com/android/internal/power/ModemPowerProfile.java`
- `I` = `frameworks/base/core/java/com/android/internal/os/`
- `S` = `frameworks/base/services/core/java/com/android/server/power/stats/`
- `B` = `frameworks/base/services/core/java/com/android/server/power/stats/BatteryStatsImpl.java`

The `I` → `S` move is itself a per-release fact: the calculators left `com.android.internal.os` for
`com.android.server.power.stats` in Android 14, so a table storing one path per calculator would be
wrong for half the rows.

| key | apis | calculator | effect | tag | file:line |
| --- | --- | --- | --- | --- | ---: |
| `battery.capacity` | 26–36 | `BatteryStatsImpl` | `ZERO_DIVIDES` | android-16.0.0_r1 | `B:11519` |
| `screen.on` | 26–29 | `BatteryStatsHelper.addScreenUsage` | `ZERO_ZEROES_COMPONENT` | android-9.0.0_r61 | `I/BatteryStatsHelper.java:631` |
| `screen.on` | 30–33 | `ScreenPowerCalculator` | `ZERO_ZEROES_COMPONENT` | android-12.0.0_r34 | `I/ScreenPowerCalculator.java:54` |
| `screen.on` | 34–36 | | `NOT_READ` | android-14.0.0_r1 | `P:175` |
| `screen.on.display0` | 34–36 | `ScreenPowerCalculator` | `ZERO_ZEROES_COMPONENT` | android-16.0.0_r1 | `S/ScreenPowerCalculator.java:59` |
| `screen.full` | 26–29 | `BatteryStatsHelper.addScreenUsage` | `ZERO_ZEROES_TERM` | android-9.0.0_r61 | `I/BatteryStatsHelper.java:633` |
| `screen.full` | 30–33 | `ScreenPowerCalculator` | `ZERO_ZEROES_TERM` | android-12.0.0_r34 | `I/ScreenPowerCalculator.java:56` |
| `screen.full.display0` | 34–36 | `ScreenPowerCalculator` | `ZERO_ZEROES_TERM` | android-16.0.0_r1 | `S/ScreenPowerCalculator.java:61` |
| `ambient.on` | 28–33 | `AmbientDisplayPowerCalculator` | `ZERO_ZEROES_COMPONENT` | android-12.0.0_r34 | `I/AmbientDisplayPowerCalculator.java:36` |
| `ambient.on.display0` | 34–36 | `AmbientDisplayPowerCalculator` | `ZERO_ZEROES_COMPONENT` | android-16.0.0_r1 | `S/AmbientDisplayPowerCalculator.java:44` |
| `cpu.suspend` | 28–36 | `IdlePowerCalculator` | `ZERO_ZEROES_TERM` | android-16.0.0_r1 | `S/IdlePowerCalculator.java:40` |
| `cpu.idle` | 28–36 | `IdlePowerCalculator` | `ZERO_ZEROES_TERM` | android-16.0.0_r1 | `S/IdlePowerCalculator.java:43` |
| `cpu.active` | 26–29 | `CpuPowerCalculator` | `ZERO_ZEROES_COMPONENT` | android-9.0.0_r61 | `I/CpuPowerCalculator.java:54` |
| `cpu.cluster_power.cluster0` | 30–36 | | `NOT_READ` | android-16.0.0_r1 | `P:575` |
| `cpu.core_power.cluster0` | 30–36 | | `NOT_READ` | android-16.0.0_r1 | `P:581` |
| `wifi.controller.idle` | 26–36 | `WifiPowerCalculator` | `ZERO_SELECTS_FALLBACK` | android-16.0.0_r1 | `S/WifiPowerCalculator.java:70` |
| `wifi.controller.rx` | 26–36 | `WifiPowerCalculator` | `ZERO_SELECTS_FALLBACK` | android-16.0.0_r1 | `S/WifiPowerCalculator.java:74` |
| `wifi.controller.tx` | 26–36 | `WifiPowerCalculator` | `ZERO_SELECTS_FALLBACK` | android-16.0.0_r1 | `S/WifiPowerCalculator.java:72` |
| `wifi.controller.voltage` | 26–36 | `BatteryStatsImpl` | `ZERO_DIVIDES` | android-16.0.0_r1 | `B:12688` |
| `wifi.on` | 26–36 | `WifiPowerCalculator` | `ZERO_ZEROES_COMPONENT` | android-16.0.0_r1 | `S/WifiPowerCalculator.java:64` |
| `wifi.active` | 26–36 | `WifiPowerCalculator` | `ZERO_ZEROES_COMPONENT` | android-16.0.0_r1 | `S/WifiPowerCalculator.java:335` |
| `radio.active` | 26–33 | `MobileRadioPowerCalculator` | `ZERO_SELECTS_FALLBACK` | android-12.0.0_r34 | `I/MobileRadioPowerCalculator.java:54` |
| `radio.active` | 34–36 | `MobileRadioPowerCalculator` | `ZERO_SELECTS_FALLBACK` | android-16.0.0_r1 | `S/MobileRadioPowerCalculator.java:89` |
| `modem.controller.sleep` | 34–36 | `BatteryStatsImpl` | `ZERO_ZEROES_TERM` | android-16.0.0_r1 | `B:12838` |
| `modem.controller.voltage` | 31–36 | `BatteryStatsImpl` | `ZERO_ZEROES_COMPONENT` | android-16.0.0_r1 | `B:12834` |
| `audio` | 26–36 | `AudioPowerCalculator` | `ZERO_ZEROES_COMPONENT` | android-16.0.0_r1 | `S/AudioPowerCalculator.java:45` |
| `video` | 26–36 | `VideoPowerCalculator` | `ZERO_ZEROES_COMPONENT` | android-16.0.0_r1 | `S/VideoPowerCalculator.java:42` |
| `camera.avg` | 26–36 | `CameraPowerCalculator` | `ZERO_ZEROES_COMPONENT` | android-16.0.0_r1 | `S/CameraPowerCalculator.java:38` |
| `camera.flashlight` | 26–36 | `FlashlightPowerCalculator` | `ZERO_ZEROES_COMPONENT` | android-16.0.0_r1 | `S/FlashlightPowerCalculator.java:36` |
| `gps.on` | 26–36 | `GnssPowerCalculator` | `ZERO_ZEROES_COMPONENT` | android-16.0.0_r1 | `S/GnssPowerCalculator.java:36` |
| `gps.signalqualitybased` | 30–36 | `BatteryStatsImpl` | `ZERO_ZEROES_TERM` | android-16.0.0_r1 | `B:7660` |
| `gps.voltage` | 30–36 | `BatteryStatsImpl` | `ZERO_ZEROES_COMPONENT` | android-16.0.0_r1 | `B:7651` |
| `memory.bandwidths` | 28–36 | `MemoryPowerCalculator` | `ZERO_ZEROES_COMPONENT` | android-16.0.0_r1 | `S/MemoryPowerCalculator.java:36` |
| `bluetooth.controller.rx` | 26–36 | `BluetoothPowerCalculator` | `ZERO_ZEROES_TERM` | android-16.0.0_r1 | `S/BluetoothPowerCalculator.java:60` |
| `bluetooth.controller.voltage` | 26–36 | `BatteryStatsImpl` | `ZERO_ZEROES_COMPONENT` | android-16.0.0_r1 | `B:13525` |

`ZERO_DIVIDES` is now one key, not a family of four, and it is still the row worth reading the
table for. `wifi.controller.voltage` divides, and the division sits outside its own guard: the
`opVolt != 0` check opens at `BatteryStatsImpl:12690` and closes at `:12694`, while the division at
`:12698` is conditioned only on `mTmpRailStats != null`. `opVolt` is a `double`, so `long / 0.0`
yields `Infinity`, and `(long) Infinity` is `Long.MAX_VALUE`, which flows into `addCountLocked` at
`:12700-12701` and into `mHistory.recordWifiConsumedCharge` at `:12702-12703`. A declared
`wifi.controller.voltage` of `0` therefore writes `Long.MAX_VALUE` into the monitored-rail counter
and into battery history, silently. The other three operating-voltage keys — `gps.voltage`,
`modem.controller.voltage` and `bluetooth.controller.voltage` — are gates or guarded divisors and
are now `ZERO_ZEROES_COMPONENT`.

### Keeping the citations honest

Each row is read off the checked-out tag by hand at E1 and the terminal transcript goes into
`NOTES.md` — for every row, the `git checkout <tag>` and the grep that produced the line. A row
whose line I have not opened myself does not go in the table; a short table I have read is worth
more than a long one where one line is guessed. Verification by a host-side fetch-and-rewrite tool
was cut: it needs network access to a host CI has no business depending on, so nothing would have
enforced it, and a tool that can be wrong about a 35-row table is a second thing to check.

The table in this document is generated from `audit/src/main/resources/keytable.tsv` by
`./gradlew :audit:keyTableDoc`, so the document cannot drift from the data the model reads. The
anchor is the match key, so an upstream edit renames a line rather than silently invalidating a
claim.

---

## 6. The back-fill chain

On API 34+ `PowerProfile` synthesises new-schema keys from deprecated ones during construction and
logs a warning nobody reads:

```java
Slog.w(TAG, POWER_SCREEN_FULL + " is deprecated! Use " + key + " instead.");
```

Measured on the API 36 emulator: its `power_profile.xml` contains only `screen.on`, and
`getAveragePower("screen.on.display0")` returns `0.1`. An audit that checks "is the key in the file"
flags a false positive on every display and modem key. Modelling the chains is the part of `:audit`
worth reading.

```kotlin
data class BackFill(
    val synthesised: String,      // "screen.on.display0"
    val from: String,             // "screen.on"
    val via: String,              // "PowerProfile.initDisplays"
    val apis: IntRange,           // 34..36
    val transform: Transform,
)

sealed interface Transform {
    data object Identity : Transform
    data class PerDisplay(val ordinal: Int) : Transform
    data class ModemDrain(val drainType: Int, val rat: Int, val freqRange: Int) : Transform
}

sealed interface Provenance {
    data class Declared(val key: String, val value: Double, val fileLine: Int) : Provenance
    data class BackFilled(val key: String, val from: String, val value: Double, val via: String) : Provenance
    data class NotDeclared(val key: String) : Provenance
}
```

`Provenance.BackFilled` becomes `Reading.Value(v, Route(BACK_FILL, from))` at the boundary of
`:collect`, and `Provenance.NotDeclared` becomes `SilentDefault(0.0, KEY_ABSENT_FROM_PROFILE, …)`.

`initDisplays`, modelled as three steps, in this order:

1. Parse the XML into `declared`, preserving file order and array shape.
2. Determine the display count: the longest per-display key array present, or 1.
3. For each display ordinal `i` and each of `screen.on.display`, `screen.full.display`,
   `ambient.on.display`:
   - `"$prefix$i"` declared → `Declared`.
   - else the deprecated singular key (`screen.on`, `screen.full`, `ambient.on`) declared →
     `BackFilled(from = deprecated, via = "PowerProfile.initDisplays")`, and the `Slog.w` above
     fires on-device.
   - else → `NotDeclared`, and `getAveragePower` returns its `defaultValue`, which is `0.0`.

`initModem` and `handleDeprecatedModemConstant` are the same shape with a wider target. The new
schema parses a `<modem>` element into `ModemPowerProfile`, keyed by a packed
drain-type/RAT/frequency-range long rather than by a string, so the audit's key space here is not
the XML's key space and the model holds both. `handleDeprecatedModemConstant` inserts `radio.on`
(per signal-strength bin), `radio.scanning` and `radio.active` under the corresponding new constants
when and only when the new one is absent.

The consequence for my one real device, and the reason `Provenance` is a function of
`(file, apiLevel)` rather than of the file alone: the ANE-LX2's live profile declares `radio.active`
and no `modem.controller.*`. At API 28 there is no back-fill, so those keys are `ZERO_BY_ABSENCE`.
The identical file audited as if on API 36 reports `BACK_FILLED`. sipper reports the device's own
level and labels the counterfactual as a counterfactual on a second line.

---

## 7. `:usage` — reconstruction

The framework's own path does not clip. `UsageStatsDatabase.queryUsageStats` starts at
`lastIndexOnOrBefore(beginTime)` and adds each bucket's whole `mTotalTimeInForeground`;
`UsageStats.add()` sums with no intersection. Ask for 24 h of `INTERVAL_DAILY` and get up to 48 h.
This module exists so that the app has a number that does clip, and so the difference can be a
column rather than an assertion.

### Event codes

Read as literal ints, with a comment saying why:

```kotlin
private const val ACTIVITY_RESUMED = 1
private const val ACTIVITY_PAUSED = 2
private const val END_OF_DAY = 3
private const val CONTINUE_PREVIOUS_DAY = 4
private const val CONFIGURATION_CHANGE = 5
private const val STANDBY_BUCKET_CHANGED = 11
private const val FOREGROUND_SERVICE_START = 19
private const val FOREGROUND_SERVICE_STOP = 20
private const val CONTINUING_FOREGROUND_SERVICE = 21
private const val ROLLOVER_FOREGROUND_SERVICE = 22
private const val ACTIVITY_STOPPED = 23

// 21, 22, 24 and 25 are @hide with no public field to reference. 23 is public from API 29 and
// 26/27 from API 30, so on minSdk 28 half of these have no constant to compile against anyway;
// one literal form for all of them keeps the table readable.
//
// Dropping 24 is not conservative: an activity destroyed without a preceding STOPPED - process
// death, finish() inside onCreate - leaves its key resumed forever, and the leak lands in
// unclosedTail where it reads as real usage.
private const val ACTIVITY_DESTROYED = 24
private const val FLUSH_TO_DISK = 25
private const val DEVICE_SHUTDOWN = 26
private const val DEVICE_STARTUP = 27
```

### The machine

Keyed on `(package, className)`. The activity `instanceId` is not exposed to apps, so the class name
is the finest key available, and that gap is where the multi-instance residual comes from. Three
states per key: `Idle`, `Resumed(since)`, `Visible(since)` — where `Visible` means paused but not
stopped, matching `mTotalTimeVisible`'s own semantics from API 29.

| event | transition | counter |
| --- | --- | --- |
| `1` `ACTIVITY_RESUMED` from `Idle`/`Visible` | open a foreground interval; open a visible interval if none | — |
| `1` from `Resumed` | no clock restart | `dupResumes++` |
| `2` `ACTIVITY_PAUSED` | close foreground, stay visible | — |
| `23` `ACTIVITY_STOPPED` | close foreground if open, close visible, → `Idle` | — |
| `24` `ACTIVITY_DESTROYED` | close everything, drop the key entirely | — |
| `25` `FLUSH_TO_DISK` | no state change; recorded, because a flush straddling a boundary is where the framework's own bucket seams fall | — |
| `19`/`20` FGS start/stop | separate machine keyed on package alone; services have no class in this stream worth trusting | — |
| `21` `CONTINUING_FOREGROUND_SERVICE` | an FGS was open across a rollover: open it at `windowStart` | — |
| `22` `ROLLOVER_FOREGROUND_SERVICE` | close and reopen at the rollover instant | — |
| `26` `DEVICE_SHUTDOWN` | close all open intervals at its timestamp | `closedByShutdown++` |
| `27` `DEVICE_STARTUP` | clear all state; anything still open is a lost tail | `lostTails++` |
| `11` `STANDBY_BUCKET_CHANGED` | routed to `bucket_change`, never into this machine | — |
| `3`/`4` end-of-day / continue | window hints, never state | — |
| `5` `CONFIGURATION_CHANGE` | ignored deliberately: a rotation already emits its own PAUSED/RESUMED pair, and counting the config change double-counts it | — |
| anything else | counted by code in `unknownEventTypes`, never dropped silently | — |

`unknownEventTypes: Map<Int, Int>` is published rather than logged. A new event integer in a future
release is a fact about this device, and a count in a published map keeps it recoverable from the
row rather than from a logcat buffer that is already gone.

### Degraded modes, because Android 9 does not emit half of this

`ACTIVITY_STOPPED` (23) and `ACTIVITY_DESTROYED` (24) arrive at API 29 — which is why
`mTotalTimeVisible` is API 29 — and `DEVICE_SHUTDOWN` (26) / `DEVICE_STARTUP` (27) at API 30. On the
ANE-LX2 the stream carries 1 and 2 and nothing else, so a three-state machine has no exit from
`Visible` and `visibleMs` degenerates into "time since the first pause". The machine declares its
mode from `SDK_INT` rather than discovering it:

| level | behaviour |
| --- | --- |
| < 29 | no `Visible` state; event 2 closes the foreground interval and returns the key to `Idle`. `visible` and `fgs` are `Absent(29)` for every row — a full column of em dashes, uncropped in the README screenshot |
| < 30 | shutdown detection falls back to the "no records for a stretch" heuristic; the lane-chart band is labelled `no data (inferred)` |
| ≥ 30 | the full machine above |

`:usage` publishes `degradedBelowApi: Set<String>` naming the columns that are structurally
unavailable, and the APPS header says which ones rather than showing an empty column with no
explanation.

### The two boundaries

**Open at the start.** The first event for a key inside the window can be a PAUSE or STOP with no
matching RESUME, meaning the session began before `windowStart`. I assume it started at
`windowStart` and count the clipped remainder rather than dropping it — the window is the unit of
the claim, so an interval that began earlier contributes exactly its in-window part, and dropping it
would reintroduce an undercount with the opposite sign to the framework's overcount. Counted as
`openAtStart`.

**Open at the end — `unclosedTail`.** An interval still open at `windowEnd` is neither clipped and
added nor dropped. It is summed separately and published:

```kotlin
data class PackageWindow(
    val foregroundMs: Long,
    val visibleMs: Long,
    val fgsMs: Long,
    val unclosedTailMs: Long,   // sum of (windowEnd - openSince) over keys still open
    val openAtStart: Int,
    val dupResumes: Int,
    val closedByShutdown: Int,
    val lostTails: Int,
)
```

which makes the reconstruction a bound rather than a point estimate:

```
foregroundMs <= true foreground in window <= foregroundMs + unclosedTailMs
```

That inequality is why the residual is a column on `app_day` and a column on the APPS grid.
Swallowing it into `foregroundMs` would make the number look better and the claim unfalsifiable. A
missing `DEVICE_SHUTDOWN` — a battery pull, a kernel panic, an EMUI kill that never got to write one
— shows up as a large `unclosedTail` on whichever app was open, which is the correct rendering of
"I do not know when this session ended".

`dupResumes` is the multi-instance residual from the class-name key above, and is a column for the
same reason.

The comparison against the framework is a test before it is a screen: a fixture stream whose true
in-window foreground is known by construction, run through both this machine and a reimplementation
of `UsageStats.add()`'s bucket summing, asserting the second exceeds the first by the amount the
unclipped buckets overhang. Written at E3, before any device produced it.

---

## 8. `:data`

SQLDelight 2.3.2 with `deriveSchemaFromMigrations = true` and `verifyMigrations = true`. The initial
schema is `1.sqm` rather than a `.sq` file, from the first commit, so there is no version zero whose
upgrade path has to be invented later and `.sq` files hold queries. No .db snapshot is emitted or committed. Under `deriveSchemaFromMigrations = true`, SQLDelight 2.3.2
derives the schema from `1.sqm` at generation time and produces no schema file, so there is nothing to
commit — an earlier draft of this section claimed otherwise. The protection that mattered is
unaffected, and was observed working rather than assumed: a query naming a column the migrations do
not define fails `:data:generateMainSipperDatabaseInterface`, which is the failure mode a
hand-maintained schema hides until a user's device is already broken. `NOTES.md` records the
observation.

```kotlin
sqldelight {
    databases {
        create("SipperDatabase") {
            packageName.set("io.github.tahmid1999.sipper.data")
            schemaOutputDirectory.set(file("src/main/sqldelight/databases"))
            deriveSchemaFromMigrations.set(true)
            verifyMigrations.set(true)
        }
    }
}
```

### `1.sqm`

```sql
-- One row per audit run. Everything needed to reproduce the verdicts is here,
-- so a screenshot can be traced back to a row rather than to "a phone, once".
CREATE TABLE profile_audit (
    id                       INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    captured_at              INTEGER NOT NULL,   -- epoch ms
    boot_count               INTEGER NOT NULL,   -- Settings.Global.BOOT_COUNT; no permission,
                                                 -- and it is what makes elapsedRealtime spans
                                                 -- from different boots non-comparable rather
                                                 -- than silently comparable
    fingerprint              TEXT NOT NULL,      -- Build.FINGERPRINT
    api_level                INTEGER NOT NULL,
    route_system_sha         TEXT,               -- sha256 of the bytes each route resolved;
    route_package_sha        TEXT,               -- null means the route itself failed, which is
    route_reflection_ok      INTEGER NOT NULL,   -- different from resolving to nothing
    routes_agree             INTEGER NOT NULL,
    disagreement             TEXT,               -- the rendered diff; null iff routes_agree = 1
    declared_key_count       INTEGER NOT NULL,
    battery_capacity_declared REAL,
    charge_counter_uah       INTEGER             -- the independent cross-check on capacity
);

-- One row per key per audit. verdict is stored as its enum name, not an ordinal:
-- a reordered enum must not silently rewrite history, and a person running
-- sqlite3 against this file should read ZERO_BY_ABSENCE rather than 2.
CREATE TABLE key_result (
    audit_id         INTEGER NOT NULL REFERENCES profile_audit(id) ON DELETE CASCADE,
    key              TEXT NOT NULL,
    verdict          TEXT NOT NULL,   -- PRESENT | ZERO_BY_EXPLICIT_VALUE | ZERO_BY_ABSENCE | BACK_FILLED
    declared_value   REAL,            -- null when the key is not in the file at all
    effective_value  REAL NOT NULL,   -- what getAveragePower returns, back-fill included
    back_filled_from TEXT,            -- null unless verdict = BACK_FILLED
    reflection_value REAL,            -- the cross-check route; recorded, never authoritative
    calculator       TEXT,            -- null when nothing reads this key at this api_level
    PRIMARY KEY (audit_id, key)
);

-- One row per package per local day. Both foreground numbers live here side by
-- side because the delta between them is the finding, and a schema that stored
-- only the better one would make the finding unrecoverable.
CREATE TABLE app_day (
    package             TEXT NOT NULL,
    day                 TEXT NOT NULL,      -- ISO-8601 local date, yyyy-MM-dd
    fw_foreground_ms    INTEGER,            -- queryUsageStats, unclipped; null when access is off
    recon_foreground_ms INTEGER NOT NULL,
    unclosed_tail_ms    INTEGER NOT NULL,   -- the residual, published rather than absorbed
    dup_resumes         INTEGER NOT NULL,   -- the multi-instance residual
    visible_ms          INTEGER,            -- null below API 29: the events do not exist
    fgs_ms              INTEGER,            -- null below API 29
    rx_fg_bytes         INTEGER,
    tx_fg_bytes         INTEGER,
    rx_bg_bytes         INTEGER,
    tx_bg_bytes         INTEGER,
    window_start        INTEGER NOT NULL,   -- the exact ms bounds the reconstruction used, so a
    window_end          INTEGER NOT NULL,   -- row can be recomputed from app_event and checked
    closed_by_shutdown  INTEGER NOT NULL,
    lost_tails          INTEGER NOT NULL,
    PRIMARY KEY (package, day)
);

-- The raw stream. class_name is NOT NULL DEFAULT '' rather than nullable
-- because SQLite treats NULLs as distinct in a UNIQUE index, so a nullable
-- column here would let every re-poll insert a duplicate of every classless
-- event and quietly double the reconstruction.
CREATE TABLE app_event (
    id          INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    package     TEXT NOT NULL,
    class_name  TEXT NOT NULL DEFAULT '',
    event_type  INTEGER NOT NULL,   -- raw int; several of these have no public constant
    timestamp   INTEGER NOT NULL,
    ingested_at INTEGER NOT NULL    -- separate from timestamp: overlapping poll windows are
                                    -- normal and the gap between the two is how a late flush
                                    -- is told from a late poll
);
CREATE INDEX app_event_pkg_ts ON app_event(package, timestamp);
CREATE UNIQUE INDEX app_event_dedupe ON app_event(package, class_name, event_type, timestamp);

CREATE TABLE bucket_change (
    package   TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    bucket    INTEGER NOT NULL,   -- raw int; 5, 15, 45 and 50 all occur and none is a
                                  -- public constant, so an enum here would lose values
    PRIMARY KEY (package, timestamp)
);

-- Route, then observe. There is no column for "applied", because sipper never
-- applies anything and a nullable applied_at would invite one.
CREATE TABLE intervention (
    id                 INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    package            TEXT NOT NULL,
    routed_at          INTEGER NOT NULL,
    action             TEXT NOT NULL,   -- the Intent action actually launched
    resolved_component TEXT,            -- what resolveActivity returned; null when nothing did
    returned_at        INTEGER,         -- null if the user never came back
    readback_level     TEXT NOT NULL,   -- A (first-party, sipper's own package) | B | C
    readback_before    TEXT,            -- the Reading, rendered, cause included
    readback_after     TEXT,
    observed_change    TEXT             -- null until the observation window closes
);

CREATE TABLE probe_result (
    id          INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    probe       TEXT NOT NULL,   -- stable id, e.g. "power_profile.reflection"
    run_at      INTEGER NOT NULL,
    api_level   INTEGER NOT NULL,
    outcome     TEXT NOT NULL,   -- VALUE | SILENT_DEFAULT | DENIED | ABSENT
    detail      TEXT NOT NULL,   -- the verbatim runtime line, exception class and message included
    duration_us INTEGER NOT NULL
);
CREATE INDEX probe_result_probe_run ON probe_result(probe, run_at);
```

`probe_result.detail` is verbatim on purpose. `java.lang.NoSuchMethodException:
com.android.internal.os.PowerProfile.getAveragePowerForCpuScalingPolicy [int]` is the evidence; a
summarised "not available" loses the exception class, which is the part that says whether the member
was removed or blocked.

**Retention.** `app_event` is pruned at 11 days, one past the ~10-day floor at which the framework
prunes its own. Keeping more would create rows sipper cannot re-derive from any source, which is a
different kind of data than everything else here. `profile_audit` and `key_result` are never pruned
— they are small, and they are the history.

**Redaction.** Raw `app_day` and `app_event` off my daily phone is which apps I use, when, and for
how long, published under my name next to a CV. Every committed fixture is synthetic or from an
emulator; `tools/redact` produces the synthetic ones and the README says which files came from
where.

---

## 9. Scheduling

One `PeriodicWorkRequest` on WorkManager: 6-hour interval, 1-hour flex,
`ExistingPeriodicWorkPolicy.KEEP`, `setRequiresBatteryNotLow(true)`, linear 30-second backoff, plus
an opportunistic ingest when the app comes to the foreground. Never a foreground service, never a
`PARTIAL_WAKE_LOCK`.

Android vitals flags cumulative partial wake locks of 2 h or more per 24 h in over 5% of sessions,
and explicitly exempts JobScheduler jobs. A battery monitor that trips its own subject matter is not
a defensible artefact, so the sampler is structurally incapable of it.

Six hours rather than WorkManager's 15-minute minimum, for two measured reasons: usage events
survive about 10 days, so 6 h sits far inside the retention floor, and `NetworkStatsManager` has a
two-hour bucket floor, so sampling faster buys nothing on the network columns. Four wakeups a day is
what the retention floors justify.

`KEEP` rather than `REPLACE` because `REPLACE` on every process start resets the period, and a
periodic worker that never reaches its interval is a worker that never runs. SELF renders the actual
inter-run gaps, so deferral is visible rather than assumed.

---

## 10. The four CI gates

`.github/workflows/check.yml`, one job on `ubuntu-latest` with JDK 17, running
`./gradlew clean check --no-daemon` on a clean checkout. A second job with the Android SDK runs
`assembleDebug` and `lint` with `NewApi` as an error, so the Compose half cannot rot behind a green
badge — that is not one of the four, but it is required green.

Every gate below was observed failing before it was trusted, and the failure output is in `NOTES.md`.
A gate that has never rejected anything is decoration.

### 1. Unit tests, plus the structural checks

`:audit:test`, `:usage:test`, `:data:test`, `:app:testDebugUnitTest`, plus `checkAuditPurity`,
`checkUsagePurity`, `checkAuditIsLeaf`, `checkUsageIsLeaf`. `:data`'s tests run against a real
embedded sqlite file through the JDBC driver, including the migration itself applied from empty.

`:app`'s JVM test source set carries three checks that need no device, because their inputs are all
build inputs: `ContrastTest` recomputes every ratio in the contract's palette from the committed
token values and fails below 4.5:1 for a text pairing; the column-width test sums the committed
`TableColumn` list and asserts the AUDIT and APPS totals; the font test walks every string constant
in the `ui` package and asserts each codepoint is in the committed Roboto Mono subset. The
corresponding `contrastDoc` and `columnTableDoc` tasks regenerate the tables in `docs/DESIGN.md`, so
no contrast number and no width is typed into a document by hand.

### 2. Positive control — each release's AOSP default must fail its own required-key set

Fixtures: `audit/src/test/resources/aosp-defaults/<api>/power_profile.xml`, committed with the
Apache-2.0 header intact and listed in `NOTICE`, beside
`audit/src/test/resources/aosp-defaults/<api>/expected-absent.txt`.

The gate asserts four things, and the build breaks if any stops holding:

1. every key in `expected-absent.txt` is in `requiredKeys(api)` — otherwise the list is stale;
2. no key in it is declared in that release's default file;
3. no key in it resolves to a value through the back-fill chains either;
4. the audit's verdict for each is exactly `ZERO_BY_ABSENCE`, and the file as a whole **fails**
   `requiredKeys(api)`.

The failure message when it goes green:

```
positive control passed, which means the claim is gone: the android-16.0.0_r1 default
power_profile.xml now satisfies requiredKeys(36). Either the file changed upstream or
requiredKeys(36) is wrong. Do not delete this gate to make the build green.
```

**Why it is inverted.** My first draft of this gate asserted that each release's default *satisfies*
its own required-key set, treating a failure as evidence the table was wrong. That defines the
headline out of existence: the AOSP default is the file the claim is about, so a gate demanding it be
complete is a gate demanding my finding be false. Two reviewers caught it independently. The inverted
form pins the finding as a fact that must keep being true, and tells me the day it stops.

### 3. Back-fill correctness, replayed against captured device results

Fixtures: `audit/src/test/resources/probes/<avd>-<api>.tsv`, 47 rows of `key<TAB>returnedValue`
captured live from `getAveragePower` on the API 34 and API 36 emulators, beside each emulator's own
`power_profile.xml` pulled from its `framework-res.apk`.

The gate replays `ProfileModel(xml, api).averagePower(key)` against every captured row and requires
exact `Double` equality. Not a tolerance: both sides are parses of the same decimal literal, and a
tolerance is what would hide a back-fill returning a plausible neighbour instead of the right value.

The load-bearing row is `screen.on.display0 → 0.1` against an XML declaring `screen.on` and nothing
else. Without the chain model the replay returns `0.0` and the gate goes red — verified by deleting
`initDisplays` from the model and watching it.

The honest limitation: this gate cannot regenerate itself in CI, because the runner has no emulator.
The TSVs are committed data. `bench/probe-capture.md` records the adb invocation and the emulator
image build id for each, so the numbers have a provenance rather than an origin story — splitline
taught me that when a hand-edited CSV passed a chart check.

### 4. API surface

`binary-compatibility-validator` against committed `audit/api/audit.api` and `usage/api/usage.api`,
plus the assertion that no public signature in either module names a type outside `kotlin.*` and
`io.github.tahmid1999.sipper.*`.

Its real job is the negative one. It fails if anyone adds `Reading<T>.getOrNull()`, `orElse(default)`,
`map()` or `fold()`, or if `SilentDefault` acquires a `copy` or a `componentN` — each of which would
turn §4's guarantee into a convention. A diff that adds one is invisible in review; a red build is
not.

The same gate holds the committed `minApi` list: every API-level constant `:collect` hands to
`Reading.Absent`, read off `android.jar` at E0 and asserted here, so a number that was correct from
memory cannot drift into a manufactured availability claim on PROBES.

A fifth gate — corpus integrity, every CSV row's source hash matching a committed fetch manifest —
exists if the corpus lands, which is post-STOP-B and may not happen.

---

## 11. What this architecture does not do

- It verifies that the model's inputs are unpopulated, and stops there. Whether an app's displayed
  mAh is wrong by any amount needs the per-uid number, which is `api=blocked` on API 31+ and
  unreachable at every privilege, so no module here has a path to it and none is planned.
- It never observes the consequence end to end. My one physical device runs EMUI's own power
  pipeline rather than AOSP's calculators, so the causal chain in §5 is verified in source key by key
  and never watched happening. The emulators do not meaningfully drain.
- On a device with EnergyConsumers through the PowerStats HAL — in practice largely Pixel — measured
  energy replaces the profile for subsystem totals, and no public API tells an app which power model
  is in use. AUDIT has a designed full-screen state that says exactly that and manufactures no
  finding. That state is the correct answer for a healthy device, and it was designed before the
  interesting one.
- `:collect` has no path that limits another package. `Grant.Unreachable` is where that fact is
  stored rather than a comment: on API 26–36 `CHANGE_APP_IDLE_STATE`, `SUSPEND_APPS` and
  `CHANGE_COMPONENT_ENABLED_STATE` are `signature|privileged`, `MANAGE_APP_OPS_MODES` is
  `signature|installer|verifier`, `MANAGE_NETWORK_POLICY` is bare `signature`, and none carries the
  `development` flag, so `pm grant` cannot reach any of them even with a computer attached.
- Read-back has three levels and Level A exists for sipper's own package. Everything on the
  intervention screen for a third-party package is Level B or C.
- Below API 29 the `visible` and `fgs` columns are structurally unavailable (§7), and below API 30
  shutdown detection is a heuristic. Both are declared, not discovered.
- Two unrepresentative devices, no real hardware between API 29 and 33, one OEM. Instance 4 of the
  thesis is n=1, and the schema records `fingerprint` per audit so it stays countable.

---

## 12. Decisions I expect to be argued with

**`Reading<T>` in `:audit` rather than `:collect`.** It reads like a layering mistake. Putting it in
`:collect` makes the repo's central type Android-only, which costs its off-device tests and `:data`'s
ability to persist it, and buys nothing.

**Five modules for a solo app.** `:audit` and `:usage` are separate from each other because they
share no types and a single `:core` would have let one grow a dependency the other's gate then had to
tolerate. `:data` is separate from `:collect` because the JVM/Android split is what keeps the schema
testable without an emulator.

**`:data` as a plain JVM module.** The driver is injected by `:app`. The cost is one extra dependency
line in `:app`; the return is that every migration test runs in the same second as the unit tests.

**Storing enums as names and event types as raw ints in the same schema.** They are different kinds
of value. `Verdict` is mine and its names are stable; event type 24 is the platform's and its meaning
is true at a given release, so storing anything but the integer would invent a stability the platform
does not offer.

**Six hours between samples on a monitor.** Justified by the retention floors in §9 rather than by
politeness. Anything faster adds rows that no downstream column can use.

**No `getAppStandbyBuckets()`, even though it is reachable.** That reflected `@SystemApi` harvests a
per-package behavioural signal for every installed package including ones outside my declared
`<queries>` scope, which is the read in this design shaped like a circumvention argument. The cost is
real: packages with no transition inside the ~10-day event window read `unknown`. One exception
inside a refusal list reads worse than no refusal list.

**Verifying the key table by hand instead of with a tool.** A fetch-and-rewrite checker would have to
run outside CI, so nothing enforces it, and it is one more thing that can be wrong about the table it
is checking. A dated transcript in `NOTES.md` is weaker automation and stronger evidence.

**Committing an OEM's decoded `power_profile.xml`.** It is redistribution of vendor material. Both
files carry the Apache-2.0 header and `NOTICE` says so in one line.
