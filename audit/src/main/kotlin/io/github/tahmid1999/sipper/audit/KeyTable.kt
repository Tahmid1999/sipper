package io.github.tahmid1999.sipper.audit

public data class Cite(val tag: String, val file: String, val line: Int, val anchor: String)

public enum class Effect {
    ZERO_ZEROES_COMPONENT,
    ZERO_ZEROES_TERM,
    ZERO_DIVIDES,
    ZERO_SELECTS_FALLBACK,
    NOT_READ,
}

public data class KeyFact(
    val key: String,
    val apis: IntRange,
    val calculator: String?,
    val effect: Effect,
    val cite: Cite,
)

public fun parseKeyTable(tsv: String): List<KeyFact> =
    tsv.trim('\n', '\r', ' ').lineSequence()
        .drop(1)
        .filter { it.isNotBlank() }
        .map { line ->
            val c = line.split('\t')
            KeyFact(
                key = c[0],
                apis = c[1].toInt()..c[2].toInt(),
                calculator = c[3].ifBlank { null },
                effect = Effect.valueOf(c[4]),
                cite = Cite(c[5], c[6], c[7].toInt(), c.getOrElse(8) { "" }),
            )
        }
        .toList()

public fun keyTable(): List<KeyFact> {
    val stream = KeyFact::class.java.getResourceAsStream("/keytable.tsv")
        ?: error("keytable.tsv missing from :audit resources")
    return stream.use { parseKeyTable(it.readBytes().decodeToString()) }
}

/**
 * The keys some calculator actually reads at this API level, so a profile that omits one is omitting
 * something the framework will price. NOT_READ rows are excluded: a key nothing reads is not required.
 */
public fun requiredKeys(apiLevel: Int): Set<String> =
    keyTable()
        .filter { apiLevel in it.apis && it.effect != Effect.NOT_READ }
        .map { it.key }
        .toSet()
