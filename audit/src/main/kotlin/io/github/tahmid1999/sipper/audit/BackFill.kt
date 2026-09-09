package io.github.tahmid1999.sipper.audit

/** PowerProfile.initDisplays and initModem begin synthesising keys at this level. */
public const val BACK_FILL_MIN_API: Int = 34

/** Per-display key prefix -> the deprecated singular key initDisplays copies from, in framework order. */
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
