# Milestone 4 — `:usage`, the event-stream reconstruction

Run these IN ORDER under the autonomous rules in `.clinerules`. One file per step, gradle green,
commit, next step. Stop only on a red build or a genuine ambiguity.

**Source:** ARCHITECTURE §7 owns this module. CONTRACT §8.3b fixes the degraded modes.

**What this module is for.** `UsageStatsDatabase.queryUsageStats` does not clip interval buckets to
the window asked for — ask for 24 h of `INTERVAL_DAILY` and you can be handed up to 48 h. `:usage`
exists so the app has a number that *does* clip, and so the difference can be a column rather than an
assertion. That is instance 2 of the thesis, and step M4-7 is where it gets proved.

**Same constraints as `:audit`:** pure Kotlin JVM, zero production dependencies, `explicitApi()`,
`allWarningsAsErrors`, and it may not see any other project. The JDK is allowed; `android.*` is not.

---

## Step M4-1 — the module

Add `include(":usage")` to `settings.gradle.kts`, then create `usage/build.gradle.kts` as a copy of
`audit/build.gradle.kts` with every `audit` replaced by `usage`:

- same plugins, `explicitApi()`, `jvmToolchain(17)`, `allWarningsAsErrors`
- `testImplementation(kotlin("test"))` and nothing else
- `checkUsagePurity`, `checkUsageIsLeaf`, `checkUsageApiTypes` — the same three gate bodies, with
  `":usage"` in the leaf check and `api/usage.api` in the type-scope check
- all three wired into `check`

Verify: `.\gradlew.bat projects` lists `:usage`, and `.\gradlew.bat :usage:compileKotlin` succeeds
(no sources yet is fine). Commit: `usage: module scaffold with the three gates`.

## Step M4-2 — `RawEvent.kt`

`usage/src/main/kotlin/io/github/tahmid1999/sipper/usage/RawEvent.kt`:

```kotlin
package io.github.tahmid1999.sipper.usage

/**
 * One row of the platform's usage event stream, already drained from the cursor by :collect.
 * The event type stays a raw Int: several of these have no public constant, and at minSdk 28 half
 * of them have no constant to compile against at all.
 */
public data class RawEvent(
    val packageName: String,
    val className: String,
    val eventType: Int,
    val timestampMs: Long,
)
```

Verify: `.\gradlew.bat :usage:compileKotlin`. Commit: `usage: RawEvent`.

## Step M4-3 — `Reconstruction.kt` result types

`usage/src/main/kotlin/io/github/tahmid1999/sipper/usage/Reconstruction.kt`:

```kotlin
package io.github.tahmid1999.sipper.usage

/**
 * One package's reconstructed window. The residuals are columns, not corrections: swallowing
 * unclosedTailMs into foregroundMs would make the number look better and the claim unfalsifiable.
 *
 *     foregroundMs <= true foreground in window <= foregroundMs + unclosedTailMs
 */
public data class PackageWindow(
    val foregroundMs: Long,
    val visibleMs: Long,
    val fgsMs: Long,
    val unclosedTailMs: Long,
    val openAtStart: Int,
    val dupResumes: Int,
    val closedByShutdown: Int,
    val lostTails: Int,
)

/**
 * The whole reconstruction. degradedBelowApi names the columns that are structurally unavailable at
 * the running API level, so the APPS header can say which rather than showing an empty column with
 * no explanation. unknownEventTypes is published rather than logged: a new event integer in a future
 * release is a fact about this device, and a count in a returned map stays recoverable.
 */
public data class Reconstruction(
    val windows: Map<String, PackageWindow>,
    val degradedBelowApi: Set<String>,
    val unknownEventTypes: Map<Int, Int>,
)
```

Verify: `.\gradlew.bat :usage:compileKotlin`. Commit: `usage: PackageWindow and Reconstruction`.

## Step M4-4 — the state machine

`usage/src/main/kotlin/io/github/tahmid1999/sipper/usage/EventMachine.kt`. Public entry point:

```kotlin
public fun reconstruct(
    events: List<RawEvent>,
    windowStartMs: Long,
    windowEndMs: Long,
    apiLevel: Int,
): Reconstruction
```

**Event codes.** Declare them `private const val`, read as literal ints, with a comment saying why —
21, 22, 24 and 25 are `@hide` with no public field; 23 is public only from API 29 and 26/27 from 30,
so at `minSdk 28` half have no constant to compile against. Dropping 24 is not conservative: an
activity destroyed without a preceding STOPPED leaves its key resumed forever, and the leak lands in
`unclosedTail` where it reads as real usage.

```
1 ACTIVITY_RESUMED   2 ACTIVITY_PAUSED   3 END_OF_DAY   4 CONTINUE_PREVIOUS_DAY
5 CONFIGURATION_CHANGE   11 STANDBY_BUCKET_CHANGED   19 FOREGROUND_SERVICE_START
20 FOREGROUND_SERVICE_STOP   21 CONTINUING_FOREGROUND_SERVICE
22 ROLLOVER_FOREGROUND_SERVICE   23 ACTIVITY_STOPPED   24 ACTIVITY_DESTROYED
25 FLUSH_TO_DISK   26 DEVICE_SHUTDOWN   27 DEVICE_STARTUP
```

**Activity machine** — keyed on `(packageName, className)`, because the activity `instanceId` is not
exposed to apps and the class name is the finest key available. That gap is where `dupResumes` comes
from. Three states: `Idle`, `Resumed(since)`, `Visible(since)`.

| event | transition |
| --- | --- |
| `1` from `Idle` or `Visible` | open a foreground interval; open a visible interval if none is open |
| `1` from `Resumed` | no clock restart; `dupResumes++` |
| `2` | close the foreground interval, stay `Visible` |
| `23` | close foreground if open, close visible, go `Idle` |
| `24` | close everything and drop the key entirely |
| `25` | no state change |
| `26` | close all open intervals at its timestamp; `closedByShutdown++` |
| `27` | clear all state; anything still open is a lost tail, `lostTails++` |
| `11`, `3`, `4` | never touch this machine |
| `5` | ignored deliberately — a rotation already emits its own PAUSED/RESUMED pair, so counting the config change double-counts it |
| anything else | `unknownEventTypes[type] = count + 1`, never dropped silently |

**FGS machine** — separate, keyed on package alone; services have no class in this stream worth
trusting. `19` opens, `20` closes. `21` means an FGS was open across a rollover: open it at
`windowStartMs`. `22` closes and reopens at the rollover instant.

**Degraded modes** — declared from `apiLevel`, never discovered:

| level | behaviour | `degradedBelowApi` |
| --- | --- | --- |
| `< 29` | no `Visible` state at all; event `2` closes the foreground interval and returns the key to `Idle`. `visibleMs` and `fgsMs` are always 0 | `{"visible", "fgs"}` |
| `< 30` | shutdown detection unavailable; events 26/27 do not arrive | add `"shutdown"` |
| `>= 30` | the full machine | empty set |

So API 28 yields `{"visible", "fgs", "shutdown"}`, API 29 yields `{"shutdown"}`, API 30+ empty.

**The two boundaries.**

*Open at the start.* If the first event for a key inside the window is a `2` or `23` with no matching
`1`, the session began before `windowStartMs`. Assume it started at `windowStartMs` and count the
clipped remainder — the window is the unit of the claim, and dropping it would reintroduce an
undercount with the opposite sign to the framework's overcount. Count it in `openAtStart`.

*Open at the end.* An interval still open at `windowEndMs` is neither clipped-and-added nor dropped.
Sum `windowEndMs - openSince` over every still-open key into `unclosedTailMs`, separately from
`foregroundMs`.

**Clipping.** Every interval contributes only its intersection with `[windowStartMs, windowEndMs]`.
Events outside the window are still processed for state, but contribute no time.

**Ordering.** Sort events by `timestampMs` before processing; equal timestamps keep input order.

Verify: `.\gradlew.bat :usage:compileKotlin`. Commit: `usage: the event-stream state machine`.

## Step M4-5 — `EventMachineTest.kt`

`usage/src/test/kotlin/io/github/tahmid1999/sipper/usage/EventMachineTest.kt`, kotlin.test. Build
events with a small helper. Window `0..10_000` and `apiLevel = 30` unless a test says otherwise.

1. A `1` at 1000 and a `2` at 3000 give `foregroundMs` 2000.
2. A `1` at 1000, `2` at 3000, `23` at 4000 give `foregroundMs` 2000 and `visibleMs` 3000 —
   visible spans resume to stop, foreground only resume to pause.
3. A second `1` at 2000 while already `Resumed` does not restart the clock: `1` at 1000, `1` at 2000,
   `2` at 3000 give `foregroundMs` 2000 and `dupResumes` 1.
4. `24` closes and drops: `1` at 1000, `24` at 2000, then `2` at 3000 gives `foregroundMs` 1000, and
   the stray `2` does not produce an `openAtStart` for that key.
5. `5` is ignored: inserting a `5` at 2000 between `1` at 1000 and `2` at 3000 leaves
   `foregroundMs` 2000 and `dupResumes` 0.
6. An unknown type is counted, not dropped: an event with type 99 leaves `unknownEventTypes[99] == 1`.
7. `11`, `3` and `4` change nothing and do not appear in `unknownEventTypes`.
8. FGS is keyed on package, not class: `19` at 1000 on class A and `20` at 3000 on class B still give
   `fgsMs` 2000.
9. `21` opens the FGS at `windowStartMs`: a lone `21` at 4000 with `20` at 6000 gives `fgsMs` 6000.
10. `26` closes an open interval at its timestamp and counts it: `1` at 1000, `26` at 5000 give
    `foregroundMs` 4000, `closedByShutdown` 1, `unclosedTailMs` 0.
11. `27` clears state and counts a lost tail: `1` at 1000, `27` at 5000 give `lostTails` 1.

Verify: `.\gradlew.bat :usage:test`. Commit: `usage: state machine tests`.

## Step M4-6 — `BoundaryTest.kt`

Same package, `usage/src/test/kotlin/.../BoundaryTest.kt`. Window `1000..5000` throughout.

1. **Open at start.** A lone `2` at 3000 with no preceding `1` gives `foregroundMs` 2000 (clipped from
   `windowStartMs`) and `openAtStart` 1.
2. **Unclosed tail.** A `1` at 2000 and no close gives `foregroundMs` 0 and `unclosedTailMs` 3000 —
   the tail is never folded into the foreground total.
3. **The bound holds.** For the same stream,
   `foregroundMs <= foregroundMs + unclosedTailMs` and `unclosedTailMs > 0`.
4. **Clipping before the window.** A `1` at 0 and a `2` at 2000 give `foregroundMs` 1000, not 2000.
5. **Clipping after the window.** A `1` at 4000 and a `2` at 9000 give `foregroundMs` 1000 and
   `unclosedTailMs` 0 — the interval closed, just outside the window.
6. **Degraded at API 28.** With `apiLevel = 28`, a `1` at 2000, `2` at 3000, `23` at 4000 give
   `visibleMs` 0 and `fgsMs` 0, and `degradedBelowApi` equals `setOf("visible", "fgs", "shutdown")`.
7. **Degraded at API 29** gives `degradedBelowApi` equal to `setOf("shutdown")`.
8. **Not degraded at API 30** gives an empty `degradedBelowApi`.

Verify: `.\gradlew.bat :usage:test`. Commit: `usage: boundary and degraded-mode tests`.

## Step M4-7 — the instance-2 proof

`usage/src/test/kotlin/.../UnclippedBucketTest.kt`. This is the test ARCHITECTURE §7 calls "a test
before it is a screen", and it must be written before any device produces the number.

Add a private helper in the test file that reimplements the framework's summing — `UsageStats.add()`
does `mTotalTimeInForeground += right.mTotalTimeInForeground` with no clipping term anywhere, and
`queryUsageStats` hands over whole `IntervalStats` buckets on a containment test rather than an
intersection:

```kotlin
/** What the framework returns: whole buckets that overlap the window, summed with no clipping. */
private fun frameworkSum(buckets: List<LongRange>, window: LongRange): Long =
    buckets.filter { it.first <= window.last && it.last >= window.first }
        .sumOf { it.last - it.first }
```

Construct a stream whose true in-window foreground is known by construction: a window of
`[0, 10_000]`, and foreground intervals at `-5_000..2_000`, `4_000..6_000` and `9_000..14_000`.
True in-window total is `2_000 + 2_000 + 1_000 = 5_000`.

Assert, in this order:

1. `reconstruct(...)` returns `foregroundMs + unclosedTailMs == 5_000` for that package — the clipped
   reconstruction is exact to the window.
2. `frameworkSum` over the same three buckets returns `7_000 + 2_000 + 5_000 = 14_000`.
3. `frameworkSum` **exceeds** the reconstruction by exactly `9_000`, which is the overhang of the two
   buckets that straddle the window edges. Assert the exact difference, not merely that it is larger.

Add a comment naming the mechanism: `UsageStatsDatabase.queryUsageStats` clamps `startIndex` to 0 and
guards its loop with `beginTime < stats.endTime`, which is a containment test and never an
intersection, and `sUsageStatsCombiner` adds whole `UsageStats` objects.

Verify: `.\gradlew.bat :usage:test`. Commit: `usage: the unclipped-bucket proof, instance 2`.

## Step M4-8 — API surface and full check

- `.\gradlew.bat :usage:apiDump` → commit `usage/api/usage.api`
- `.\gradlew.bat check` green at the root: both modules, all six gates, every test.

Commit: `usage: commit API surface`.

## Report at the end

The step list, every commit, the final test counts per suite for both modules, and the exact numbers
from step M4-7 assertion 3.

## Not in this milestone

- **`:collect`.** `:usage` never touches `UsageEvents`; `:collect` drains the cursor into `RawEvent`s
  and hands them over. That module is Android and comes later.
- **The lane chart and the `no data (inferred)` band.** That is `:app`.
- **Gate 3** — still blocked on emulator captures.
