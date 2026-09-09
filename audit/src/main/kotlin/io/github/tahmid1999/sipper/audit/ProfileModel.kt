package io.github.tahmid1999.sipper.audit

public class ProfileModel private constructor(
    public val declared: Map<String, List<Double>>,
    public val apiLevel: Int,
    private val lines: Map<String, Int>,
) {
    public constructor(declared: Map<String, List<Double>>, apiLevel: Int) :
        this(declared, apiLevel, emptyMap())

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
            .also { rows ->
                val duplicates = rows.groupingBy { it.key }.eachCount().filterValues { it > 1 }
                require(duplicates.isEmpty()) {
                    "key table has overlapping api ranges at api $apiLevel: ${duplicates.keys}"
                }
            }
            .associate { it.key to verdictOf(provenance(it.key)) },
    )
}
