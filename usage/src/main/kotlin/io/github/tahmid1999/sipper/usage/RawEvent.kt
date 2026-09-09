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
