package io.github.tahmid1999.sipper.audit

public data class BackFill(
    val synthesised: String,
    val from: String,
    val via: String,
    val apis: IntRange,
    val transform: Transform,
)

public sealed interface Transform {
    public data object Identity : Transform
    public data class PerDisplay(val ordinal: Int) : Transform
    public data class ModemDrain(val drainType: Int, val rat: Int, val freqRange: Int) : Transform
}

public sealed interface Provenance {
    public data class Declared(val key: String, val value: Double, val fileLine: Int) : Provenance
    public data class BackFilled(val key: String, val from: String, val value: Double, val via: String) : Provenance
    public data class NotDeclared(val key: String) : Provenance
}
