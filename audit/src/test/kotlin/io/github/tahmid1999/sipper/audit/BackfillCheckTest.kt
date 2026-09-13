package io.github.tahmid1999.sipper.audit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Gate 3, ARCHITECTURE.md §10.3: replay ProfileModel.averagePower against a capture of real
 * getAveragePower values, with exact Double equality. Not a tolerance: both sides are parses of
 * the same decimal literal, and a tolerance is what would hide a back-fill returning a plausible
 * neighbour instead of the right value.
 *
 * Fixture: probes/Pixel_9_Pro_XL-36/ — the verbatim capture.tsv (48 reflect rows taken from the
 * API 36 emulator on 2026-09-10, fingerprint BP22.250325.006/13344233), the power_profile.xml the
 * runtime actually resolves, and the aapt2 dump it was rebuilt from. Provenance is in
 * bench/probe-capture.md.
 *
 * This gate cannot regenerate itself in CI, because the runner has no emulator. The fixture is
 * committed data.
 */
class BackfillCheckTest {

    private val api = 36

    private fun fixture(name: String): String {
        val raw = javaClass.getResourceAsStream("/probes/Pixel_9_Pro_XL-36/$name")
            ?.use { it.readBytes().decodeToString() }
            ?: fail("missing fixture /probes/Pixel_9_Pro_XL-36/$name")
        return raw.removePrefix("﻿") // the capture carries a UTF-8 BOM; strip it, change nothing else
    }

    private data class Row(val key: String, val kind: String, val payload: String)

    /** reflect rows: `reflect<TAB>key<TAB>Value:<double> | Threw:<class>:<message> | ...` */
    private fun reflectRows(): List<Row> =
        fixture("capture.tsv").lineSequence()
            .filter { it.startsWith("reflect\t") }
            .map { line ->
                val f = line.split('\t')
                Row(f[1], f[2].substringBefore(':'), f[2].substringAfter(':'))
            }
            .toList()

    private val model by lazy {
        ProfileModel(parseProfile(fixture("power_profile.xml")), api)
    }

    @Test
    fun fixtureIsComplete() {
        val rows = reflectRows()
        assertTrue(rows.isNotEmpty(), "capture.tsv has no reflect rows")
        // The capture's own probeset line names the row count; the gate replays every one of them.
        val probeset = fixture("capture.tsv").lineSequence()
            .firstOrNull { it.startsWith("probeset\t") }
            ?.split('\t')
            ?: fail("capture.tsv has no probeset row")
        assertEquals(
            probeset[1].toInt(), rows.size,
            "reflect row count drifted from the captured probeset size",
        )
        // Both resource routes were Value on this device, so the replay premise holds.
        assertTrue(
            fixture("capture.tsv").lineSequence().any { it.startsWith("route\tsystem\tValue\t") },
            "the system route was not a Value in this capture; the fixture premise is gone",
        )
    }

    @Test
    fun reconstructionMatchesTheCapture() {
        // The XML beside the capture must declare exactly what the device's resource route
        // declared, on the same lines, or the replay proves nothing about the device.
        val declared = fixture("capture.tsv").lineSequence()
            .filter { it.startsWith("declared\t") }
            .map { it.split('\t')[1] }
            .toSet()
        val parsed = parseProfile(fixture("power_profile.xml"))
        assertEquals(declared, parsed.declared.keys.toSet(), "fixture XML key set drifted from the capture")

        val lineRows = fixture("capture.tsv").lineSequence()
            .filter { it.startsWith("line\t") }
            .map { it.split('\t').let { f -> f[1] to f[2].toInt() } }
        for ((key, line) in lineRows) {
            assertEquals(line, parsed.lines[key], "fixture XML line for $key drifted from the capture")
        }
    }

    @Test
    fun replaysCapturedAveragePowerExactly() {
        var replayed = 0
        for (row in reflectRows()) {
            when (row.kind) {
                "Value" -> assertEquals(
                    row.payload.toDouble(), model.averagePower(row.key),
                    "replay of ${row.key} at api $api",
                )
                // The platform threw rather than returning a value, and the model has no value
                // either: parseProfile stores the empty array, provenanceOf takes values.first()
                // on the Declared branch, and that is empty. The exception classes differ by
                // construction - the model never indexes [0], so it cannot throw
                // ArrayIndexOutOfBoundsException - and the divergence is stated here rather than
                // smoothed: what the gate proves is that neither side produces a Double.
                "Threw" -> assertFailsWith<NoSuchElementException>(
                    message = "model produced a value for ${row.key}, which threw on the device",
                ) { model.averagePower(row.key) }
                else -> fail("unexpected reflect kind in the capture: $row")
            }
            replayed++
        }
        assertTrue(replayed > 40, "replayed only $replayed rows; the fixture is stale or truncated")
    }

    @Test
    fun theLoadBearingRowIsABackFill() {
        // ARCHITECTURE §10.3: an XML declaring screen.on and nothing per-display, and a device
        // returning 0.1 for screen.on.display0. Without the chain model the replay returns 0.0
        // and this gate goes red - verified by deleting initDisplays from the model and watching it.
        val provenance = model.provenance("screen.on.display0")
        assertEquals(Provenance.BackFilled::class, provenance::class)
        assertEquals("screen.on", (provenance as Provenance.BackFilled).from)
        assertEquals(0.1, model.averagePower("screen.on.display0"))
        assertEquals(Verdict.BACK_FILLED, verdictOf(provenance))
    }
}
