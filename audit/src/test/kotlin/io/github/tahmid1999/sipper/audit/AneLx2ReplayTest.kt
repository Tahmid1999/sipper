package io.github.tahmid1999.sipper.audit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Gate 3's second fixture, the one instance 4 is about: the ANE-LX2 at API 28, where the profile a
 * runtime resource route hands an app is NOT the profile the framework prices from.
 *
 * The two files, both committed here:
 * - `product_power_profile.xml` - /product/etc/xml/power_profile.xml, 20 keys, pre-Lollipop schema,
 *   `battery.capacity` 3000. PowerProfile on this device resolves this file, which is what the
 *   reflection route (the capture's `reflect` rows) read; the replay runs against it.
 * - `framework-res.xmltree.txt` - the aapt2 dump of framework-res.apk's res/xml/power_profile.xml,
 *   35 keys, `battery.capacity` 1000: the file the resource routes return and the capture's
 *   `declared` and `line` rows document. The two files disagree on every key they share.
 *
 * The capture is the verbatim bench/ane-lx2-api28.tsv (50 reflect rows, 2026-09-10). Provenance and
 * the adb invocations are in bench/probe-capture.md. Below API 34 no back-fill exists, so unlike
 * the API 36 fixture the replay is pure declared-vs-absent: what it pins is the silent default
 * itself, and the two findings that live on this phone (instance 1: a key name mismatch between
 * vendor file and framework constant; instance 4: two different profiles on one device).
 */
class AneLx2ReplayTest {

    private val api = 28

    private fun fixture(name: String): String {
        val raw = javaClass.getResourceAsStream("/probes/AneLx2-28/$name")
            ?.use { it.readBytes().decodeToString() }
            ?: fail("missing fixture /probes/AneLx2-28/$name")
        return raw.removePrefix("﻿") // UTF-8 BOM from the logcat redirect; stripped, not edited out
    }

    private data class Row(val key: String, val kind: String, val payload: String)

    private fun reflectRows(): List<Row> =
        fixture("capture.tsv").lineSequence()
            .filter { it.startsWith("reflect\t") }
            .map { line ->
                val f = line.split('\t')
                Row(f[1], f[2].substringBefore(':'), f[2].substringAfter(':'))
            }
            .toList()

    private val model by lazy {
        ProfileModel(parseProfile(fixture("product_power_profile.xml")), api)
    }

    /** element name -> line, for the item and array elements of the committed aapt2 dump. */
    private fun dumpElementLines(): Map<String, Int> {
        val lines = LinkedHashMap<String, Int>()
        var pending: Int? = null
        for (line in fixture("framework-res.xmltree.txt").lineSequence()) {
            Regex("""^\s*E: (?:item|array) \(line=(\d+)\)$""").find(line)?.let {
                pending = it.groupValues[1].toInt()
            }
            val attr = Regex("""^\s*A: name="([^"]+)"""").find(line)?.groupValues?.get(1)
            if (attr != null && pending != null) {
                lines[attr] = pending
                pending = null
            }
        }
        return lines
    }

    /** item name -> literal text, for the single-value elements of the committed aapt2 dump. */
    private fun dumpItemTexts(): Map<String, String> {
        val texts = LinkedHashMap<String, String>()
        var name: String? = null
        for (line in fixture("framework-res.xmltree.txt").lineSequence()) {
            if (Regex("""^\s*E: item \(line=\d+\)$""").matches(line)) name = null
            val attr = Regex("""^\s*A: name="([^"]+)"""").find(line)?.groupValues?.get(1)
            if (attr != null && name == null) name = attr
            val text = Regex("""^\s*T: '(.*)'\s*$""").find(line)?.groupValues?.get(1)
            if (text != null && name != null) {
                texts[name] = text
                name = null
            }
        }
        return texts
    }

    @Test
    fun fixtureIsComplete() {
        val rows = reflectRows()
        assertTrue(rows.isNotEmpty(), "capture.tsv has no reflect rows")
        val probeset = fixture("capture.tsv").lineSequence()
            .firstOrNull { it.startsWith("probeset\t") }
            ?.split('\t')
            ?: fail("capture.tsv has no probeset row")
        assertEquals(50, probeset[1].toInt(), "the ANE-LX2 probe set was 50 keys when captured")
        assertEquals(probeset[1].toInt(), rows.size, "reflect rows drifted from the captured probeset size")
        assertTrue(
            fixture("capture.tsv").lineSequence().any { it.startsWith("route\tsystem\tValue\t") },
            "the system route was not a Value in this capture; the fixture premise is gone",
        )
    }

    /**
     * The capture's `declared` and `line` rows document the framework-res view. Pin the committed
     * dump to them. The replay XML is NOT pinned this way because it is a different file - that is
     * the finding, and the test below makes it mechanical.
     */
    @Test
    fun frameworkResDumpMatchesTheCapture() {
        val declared = fixture("capture.tsv").lineSequence()
            .filter { it.startsWith("declared\t") }
            .map { it.split('\t')[1] }
            .toSet()
        assertEquals(declared, dumpElementLines().keys.toSet(), "dump key set drifted from the capture")
        assertEquals(35, declared.size, "the framework-res view declared 35 keys when captured")

        val lineRows = fixture("capture.tsv").lineSequence()
            .filter { it.startsWith("line\t") }
            .map { it.split('\t').let { f -> f[1] to f[2].toInt() } }
        for ((key, line) in lineRows) {
            assertEquals(line, dumpElementLines()[key], "dump line for $key drifted from the capture")
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
                // wifi.controller.tx_levels returned 0.0 on this phone - the silent default, not
                // the throw it produced on the API 36 emulator, because /product does not declare
                // the key at all and the framework's own fall-through answers. The model's
                // NotDeclared branch replays that 0.0 exactly.
                "Threw" -> fail("no key threw on this device; the capture has a Threw row: $row")
                else -> fail("unexpected reflect kind in the capture: $row")
            }
            replayed++
        }
        assertEquals(50, replayed)
    }

    /**
     * Instance 4, made mechanical: of the 13 keys both files declare, not one agrees in value. The
     * framework prices from /product (capacity 3000, screen.on 143) while the resource routes hand
     * an app framework-res (capacity 1000, screen.on 0.1). A verdict computed from the route file
     * describes a file nothing prices from.
     */
    @Test
    fun theTwoProfilesDisagreeOnEverySharedKey() {
        val route = dumpItemTexts()
        val priced = parseProfile(fixture("product_power_profile.xml")).declared
        val shared = route.keys.filter { it in priced }
        assertEquals(13, shared.size, "the fixture pair declared 13 shared keys at build time")
        val agreeing = shared.filter { route[it]!!.toDouble() == priced[it]!!.first() }
        assertEquals(
            emptyList(), agreeing,
            "keys agreeing between the route view and the priced file: $agreeing",
        )
    }

    /**
     * Instance 1 on live vendor data, made mechanical: the vendor measured and declared dsp.audio
     * 43 and dsp.video 176, but no AOSP PowerProfile has ever read those keys - the framework asks
     * for `audio` and `video` (keytable rows 26-36, AudioPowerCalculator/VideoPowerCalculator),
     * which this /product file does not declare. Both real measurements render as ZERO_BY_ABSENCE,
     * silently, on the phone that measured them.
     */
    @Test
    fun theVendorMeasuredKeysTheFrameworkNeverReads() {
        assertEquals(43.0, model.averagePower("dsp.audio"))
        assertEquals(176.0, model.averagePower("dsp.video"))
        // The keys the framework actually reads, absent from the vendor's file:
        assertEquals(Verdict.ZERO_BY_ABSENCE, model.audit().verdicts["audio"])
        assertEquals(Verdict.ZERO_BY_ABSENCE, model.audit().verdicts["video"])
        assertEquals(0.0, model.averagePower("audio"))
    }

    /** No back-fill at API 28: the deprecated singular key is declared, its .display0 child is not. */
    @Test
    fun noBackFillBelowApi34OnRealData() {
        assertEquals(143.0, model.averagePower("screen.on"))
        assertEquals(Verdict.PRESENT, verdictOf(model.provenance("screen.on")))
        assertEquals(0.0, model.averagePower("screen.on.display0"))
        assertEquals(Verdict.ZERO_BY_ABSENCE, verdictOf(model.provenance("screen.on.display0")))
    }
}

