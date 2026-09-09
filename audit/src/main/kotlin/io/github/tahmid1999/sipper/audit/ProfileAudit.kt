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
