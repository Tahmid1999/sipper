package io.github.tahmid1999.sipper.collect

import android.os.Build
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.tahmid1999.sipper.audit.Reading
import io.github.tahmid1999.sipper.audit.keyTable
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileProbe {

    /**
     * Keys observed declared in a vendor power_profile.xml that no AOSP PowerProfile constant reads.
     * Probed deliberately: the vendor measured a value and the framework never asks for it, so the
     * measurement is discarded silently. Recorded here so the probe set says why it contains them.
     */
    private val VENDOR_ONLY_KEYS = listOf("dsp.audio", "dsp.video", "cpu.awake", "cpu.active", "cpu.speeds", "wifi.scan", "bluetooth.at", "none")

    @Test
    fun probeDeviceProfile() {
        try {
            val context = InstrumentationRegistry.getInstrumentation().targetContext

            // Device and API info
            log("device", Build.FINGERPRINT)
            log("api", Build.VERSION.SDK_INT.toString())

            // Read system resources route
            val systemReading = readViaSystemResources()
            val systemKind = readingKind(systemReading)
            val (systemDeclaredCount, systemLinesCount) = when (systemReading) {
                is Reading.Value -> {
                    val profile = systemReading.value
                    Pair(profile.declared.size, profile.lines.size)
                }
                else -> Pair("-", "-")
            }
            log("route", "system", systemKind, systemDeclaredCount.toString(), systemLinesCount.toString())

            // Read package resources route
            val packageReading = readViaAndroidPackage(context)
            val packageKind = readingKind(packageReading)
            val (packageDeclaredCount, packageLinesCount) = when (packageReading) {
                is Reading.Value -> {
                    val profile = packageReading.value
                    Pair(profile.declared.size, profile.lines.size)
                }
                else -> Pair("-", "-")
            }
            log("route", "package", packageKind, packageDeclaredCount.toString(), packageLinesCount.toString())

            // Get the successful route's profile for declared keys
            val successfulProfile = when (packageReading) {
                is Reading.Value -> packageReading.value
                else -> when (systemReading) {
                    is Reading.Value -> systemReading.value
                    else -> null
                }
            }

            // Log first 5 lines from the successful route's lines map
            if (successfulProfile != null) {
                successfulProfile.lines.entries.take(5).forEach { (key, lineNumber) ->
                    log("line", key, lineNumber.toString())
                }
            } else {
                log("line", "-", "-")
            }

            // Log all declared keys with their first value
            if (successfulProfile != null) {
                successfulProfile.declared.forEach { (key, values) ->
                    val firstValue = if (values.isNotEmpty()) values[0].toString() else ""
                    log("declared", key, firstValue)
                }
            }

            // Build probe set: union of keyTable keys + declared keys from successful route + vendor-only keys
            val keyTableKeys = keyTable().map { it.key }.toSet()
            val declaredKeys = successfulProfile?.declared?.keys?.toSet() ?: emptySet()
            val probeSet = (keyTableKeys + declaredKeys + VENDOR_ONLY_KEYS).sorted()

            log("probeset", probeSet.size.toString(), "keytable+declared+vendoronly")

            // Reflect getAveragePower for every key in probe set
            probeSet.forEach { key ->
                val reflection = reflectAveragePower(context, key)
                val reflectResult = when (reflection) {
                    is Reading.Value -> "Value:${reflection.value}"
                    is Reading.Denied -> "Denied:${reflection.permission}"
                    is Reading.Absent -> "Absent:${reflection.minApi}"
                    is Reading.SilentDefault -> "SilentDefault:${reflection.cause}"
                }
                log("reflect", key, reflectResult)
            }
        } catch (e: Exception) {
            log("error", e::class.simpleName ?: "Unknown", e.message ?: "")
        }
    }

    private fun readingKind(reading: Reading<*>): String = when (reading) {
        is Reading.Value -> "Value"
        is Reading.Denied -> "Denied"
        is Reading.Absent -> "Absent"
        is Reading.SilentDefault -> "SilentDefault"
    }

    private fun log(vararg fields: String) {
        val line = fields.joinToString("\t")
        Log.i("SIPPERPROBE", line)
    }
}
