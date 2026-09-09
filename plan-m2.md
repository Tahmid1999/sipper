# Milestone 2 — executable steps

Run these IN ORDER under the autonomous rules in `.clinerules`. One file per step, gradle green,
commit, next step. Stop only on a red build or genuine ambiguity.

Supersedes the 2a–2f roadmap letters in `plan.md`.

## Step M2-1 — `BackFill.kt`

Create `audit/src/main/kotlin/io/github/tahmid1999/sipper/audit/BackFill.kt` with exactly this.
Every rule below was read off `PowerProfile.initDisplays` at android-16.0.0_r1 (lines 786–827),
fetched from android.googlesource.com. Do not "improve" the logic.

```kotlin
package io.github.tahmid1999.sipper.audit

/** PowerProfile.initDisplays and initModem begin synthesising keys at this level. */
public const val BACK_FILL_MIN_API: Int = 34

/** Per-display key prefix -> the deprecated singular key initDisplays copies from, framework order. */
private val DISPLAY_PREFIXES: Map<String, String> = mapOf(
    "ambient.on.display" to "ambient.on",
    "screen.on.display" to "screen.on",
    "screen.full.display" to "screen.full",
)

/**
 * The framework's mNumDisplays walk: increment while ANY of the three per-display keys is present
 * at the current ordinal. PowerProfile.initDisplays, android-16.0.0_r1 lines 788-797.
 */
public fun declaredDisplayCount(declared: Map<String, List<Double>>): Int {
    var n = 0
    while (DISPLAY_PREFIXES.keys.any { declared.containsKey("$it$n") }) n++
    return n
}

/**
 * mNumDisplays after initDisplays. The legacy copy runs only when the walk found nothing at all,
 * and then promotes the count to 1. PowerProfile.initDisplays, android-16.0.0_r1 lines 800-826.
 */
public fun displayCount(declared: Map<String, List<Double>>, apiLevel: Int): Int {
    val counted = declaredDisplayCount(declared)
    if (counted > 0) return counted
    val legacy = apiLevel >= BACK_FILL_MIN_API &&
        DISPLAY_PREFIXES.values.any { declared.containsKey(it) }
    return if (legacy) 1 else 0
}

/**
 * A declared key is Declared. A deprecated singular key is copied into ORDINAL 0 ONLY, and only
 * when no per-display key exists anywhere in the profile, and only at BACK_FILL_MIN_API and above.
 * Everything else is NotDeclared, where getAveragePower returns its 0.0 default.
 */
public fun provenanceOf(key: String, parsed: ParsedProfile, apiLevel: Int): Provenance {
    val declared = parsed.declared
    val values = declared[key]
    if (values != null) {
        return Provenance.Declared(key, values.first(), parsed.lines[key] ?: 0)
    }
    if (apiLevel >= BACK_FILL_MIN_API && declaredDisplayCount(declared) == 0) {
        for ((prefix, deprecated) in DISPLAY_PREFIXES) {
            if (key != "${prefix}0") continue
            val source = declared[deprecated] ?: continue
            return Provenance.BackFilled(key, deprecated, source.first(), "PowerProfile.initDisplays")
        }
    }
    return Provenance.NotDeclared(key)
}

/** The display back-fill chains this profile would produce at BACK_FILL_MIN_API and above. */
public fun displayBackFills(declared: Map<String, List<Double>>): List<BackFill> {
    if (declaredDisplayCount(declared) != 0) return emptyList()
    return DISPLAY_PREFIXES.entries
        .filter { declared.containsKey(it.value) }
        .map { (prefix, deprecated) ->
            BackFill(
                synthesised = "${prefix}0",
                from = deprecated,
                via = "PowerProfile.initDisplays",
                apis = BACK_FILL_MIN_API..36,
                transform = Transform.PerDisplay(0),
            )
        }
}
```

Verify: `.\gradlew.bat :audit:compileKotlin`

## Step M2-2 — `BackFillTest.kt`

Create `audit/src/test/kotlin/io/github/tahmid1999/sipper/audit/BackFillTest.kt` using kotlin.test.
Three fixtures, each built by calling `parseProfile` on a `<device name="Android">` document:

- `onlyDeprecated` — contains only `<item name="screen.on">0.1</item>`
- `perDisplay` — `screen.on` 0.1, then `screen.on.display0` 0.2, then `screen.on.display1` 0.3
- `mixed` — `ambient.on` 0.5, then `screen.on.display0` 0.2

Assertions:

1. `provenanceOf("screen.on", onlyDeprecated, 36)` equals `Provenance.Declared("screen.on", 0.1, 2)`.
2. `provenanceOf("screen.on.display0", onlyDeprecated, 36)` equals
   `Provenance.BackFilled("screen.on.display0", "screen.on", 0.1, "PowerProfile.initDisplays")`.
3. `provenanceOf("screen.on.display0", onlyDeprecated, 28)` equals
   `Provenance.NotDeclared("screen.on.display0")` — there is no back-fill below API 34.
4. `provenanceOf("screen.on.display1", onlyDeprecated, 36)` equals
   `Provenance.NotDeclared("screen.on.display1")` — only ordinal 0 is ever back-filled.
5. `provenanceOf("camera.avg", onlyDeprecated, 36)` equals `Provenance.NotDeclared("camera.avg")`.
6. `declaredDisplayCount(onlyDeprecated.declared)` is 0; `declaredDisplayCount(perDisplay.declared)` is 2;
   `displayCount(onlyDeprecated.declared, 36)` is 1; `displayCount(onlyDeprecated.declared, 28)` is 0.
7. `provenanceOf("screen.on.display0", perDisplay, 36)` is a `Provenance.Declared` whose value is 0.2 —
   a declared per-display key is never overwritten by a back-fill.
8. `displayBackFills(onlyDeprecated.declared)` has size 1, and its single element has synthesised
   `"screen.on.display0"`, from `"screen.on"`, transform `Transform.PerDisplay(0)`.
   `displayBackFills(perDisplay.declared)` is empty.
9. `provenanceOf("ambient.on.display0", mixed, 36)` equals `Provenance.NotDeclared("ambient.on.display0")`.

Assertion 2 is the measured API-36 emulator case. Assertion 3 is the API-28 physical device case.
Assertion 9 pins that a single declared per-display key suppresses every legacy back-fill.

Verify: `.\gradlew.bat :audit:test`

## Step M2-3 — `ProfileAudit.kt`

Create `audit/src/main/kotlin/io/github/tahmid1999/sipper/audit/ProfileAudit.kt`:

```kotlin
package io.github.tahmid1999.sipper.audit

/**
 * Verdict straight from Provenance. The Reading-based verdictFor stays for the :collect path, where
 * a real read route exists; :audit has no device, so it must not invent a RouteKind.
 * Rows follow CONTRACT.md section 3.
 */
public fun verdictOf(provenance: Provenance): Verdict = when (provenance) {
    is Provenance.Declared ->
        if (provenance.value != 0.0) Verdict.PRESENT else Verdict.ZERO_BY_EXPLICIT_VALUE
    is Provenance.BackFilled -> Verdict.BACK_FILLED
    is Provenance.NotDeclared -> Verdict.ZERO_BY_ABSENCE
}

public data class ProfileAudit(
    val apiLevel: Int,
    val verdicts: Map<String, Verdict>,
) {
    public fun count(verdict: Verdict): Int = verdicts.values.count { it == verdict }
}
```

Verify: `.\gradlew.bat :audit:compileKotlin`

## Step M2-4 — `ProfileModel.kt`

Create `audit/src/main/kotlin/io/github/tahmid1999/sipper/audit/ProfileModel.kt`. The primary
constructor keeps the shape ARCHITECTURE section 1 declares; the secondary one carries the line
numbers the parser captured.

```kotlin
package io.github.tahmid1999.sipper.audit

public class ProfileModel(
    public val declared: Map<String, List<Double>>,
    public val apiLevel: Int,
    private val lines: Map<String, Int> = emptyMap(),
) {
    public constructor(parsed: ParsedProfile, apiLevel: Int) :
        this(parsed.declared, apiLevel, parsed.lines)

    private val parsed: ParsedProfile = ParsedProfile(declared, lines)

    /**
     * Reproduces getAveragePower(String), back-fill included. This is the one place in the repo
     * where a 0.0 may be produced without a SilentDefault around it: it deliberately imitates the
     * framework's own fall-through so the replay gate can prove the imitation is exact.
     * The declared branch takes element 0 because getAveragePowerOrDefault returns
     * sPowerArrayMap.get(type)[0] - android-16.0.0_r1 PowerProfile.java:896.
     */
    public fun averagePower(key: String): Double = when (val p = provenance(key)) {
        is Provenance.Declared -> p.value
        is Provenance.BackFilled -> p.value
        is Provenance.NotDeclared -> 0.0
    }

    public fun provenance(key: String): Provenance = provenanceOf(key, parsed, apiLevel)

    /** One verdict per key the committed key table says is live at this API level. */
    public fun audit(): ProfileAudit = ProfileAudit(
        apiLevel = apiLevel,
        verdicts = keyTable()
            .filter { apiLevel in it.apis }
            .associate { it.key to verdictOf(provenance(it.key)) },
    )
}
```

Verify: `.\gradlew.bat :audit:compileKotlin`

## Step M2-5 — `ProfileModelTest.kt`

Create `audit/src/test/kotlin/io/github/tahmid1999/sipper/audit/ProfileModelTest.kt` using kotlin.test.
Fixture: a `<device name="Android">` declaring `screen.on` 0.1, `wifi.controller.rx` 0.0,
`battery.capacity` 3000. Build models with `ProfileModel(parseProfile(xml), apiLevel)`.

1. At apiLevel 36, `averagePower("battery.capacity")` is 3000.0 and its verdict is `PRESENT`.
2. At apiLevel 36, `averagePower("wifi.controller.rx")` is 0.0 and `verdictOf(provenance(...))` is
   `ZERO_BY_EXPLICIT_VALUE`.
3. At apiLevel 36, `averagePower("camera.avg")` is 0.0 and its verdict is `ZERO_BY_ABSENCE`.
   Assertions 2 and 3 return numerically identical zeroes and must carry different verdicts — that
   distinction is the whole point of the module.
4. At apiLevel 36, `averagePower("screen.on.display0")` is 0.1 and its verdict is `BACK_FILLED`.
5. At apiLevel 28, `averagePower("screen.on.display0")` is 0.0 and its verdict is `ZERO_BY_ABSENCE`.
6. At apiLevel 36, `audit().verdicts` is not empty and `audit().count(Verdict.ZERO_BY_ABSENCE)` is
   greater than 0.

Verify: `.\gradlew.bat :audit:test`, then report the step list, every commit, and final test counts.

## Known open items — do NOT attempt these

- **`initModem` / `handleDeprecatedModemConstant`.** The verdict question is now RESOLVED in
  CONTRACT.md §3.1 (declared source → `BACK_FILLED`; absent source → `SilentDefault` →
  `ZERO_BY_ABSENCE`). Implementation is still deferred to a later milestone because the modem key
  space is a packed drain-type/RAT/frequency-range long rather than XML key strings, which needs its
  own design. **Leave modem keys out of the model in milestone 2.**
- **`keytable.tsv` citation columns** are known wrong and are being rebuilt separately. The loader and
  its tests are correct; do not edit the TSV, and do not "fix" a key name in it.
