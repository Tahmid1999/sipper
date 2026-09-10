package io.github.tahmid1999.sipper.`data`

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.tahmid1999.sipper.audit.ProfileModel
import io.github.tahmid1999.sipper.audit.Verdict
import io.github.tahmid1999.sipper.audit.parseProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuditRepositoryTest {

    private val xml = """
        <device name="Android">
            <item name="screen.on">0.1</item>
            <item name="wifi.controller.rx">0.0</item>
            <item name="battery.capacity">3000</item>
        </device>
    """.trimIndent()

    private val model = ProfileModel(parseProfile(xml), 36)

    @Test
    fun saveWritesEveryVerdictAndKeepsTheColumnDistinctions() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SipperDatabase.Schema.create(driver)
        val database = SipperDatabase(driver)
        val repository = AuditRepository(database)

        val auditId = repository.save(
            model = model,
            capturedAt = 1_000L,
            bootCount = 7L,
            fingerprint = "test-fingerprint",
            routesAgree = true,
            routeReflectionOk = false,
        )

        val verdicts = model.audit().verdicts
        val rows = database.keyResultQueries.keyResultsFor(auditId).executeAsList()
        val byKey = rows.associateBy { it.key }

        // 1. Nothing dropped, nothing added.
        assertEquals(verdicts.size, rows.size)

        // 2. All four kinds appear, and every stored name round-trips through Verdict.valueOf.
        val counts = repository.verdictCounts(auditId)
        for (verdict in Verdict.entries) {
            assertTrue((counts[verdict] ?: 0L) >= 1L, "verdict $verdict missing from counts")
        }
        for ((key, expected) in verdicts) {
            assertEquals(expected, Verdict.valueOf(byKey.getValue(key).verdict), "round-trip failed for $key")
        }

        // 3. The sum of the counts equals the row count.
        assertEquals(rows.size.toLong(), counts.values.sum())

        // 4. Column relationships, one row each - where the schema earns its shape.
        //    camera.avg and wifi.controller.rx both store effective_value 0.0, and only the
        //    declared_value column distinguishes them: instance 1 written into the database.
        val cameraAvg = byKey.getValue("camera.avg")
        assertEquals(0.0, cameraAvg.effective_value)
        assertNull(cameraAvg.declared_value)

        val wifiRx = byKey.getValue("wifi.controller.rx")
        assertEquals(0.0, wifiRx.effective_value)
        assertEquals(0.0, wifiRx.declared_value)

        val battery = byKey.getValue("battery.capacity")
        assertEquals(3000.0, battery.declared_value)
        assertEquals(3000.0, battery.effective_value)

        val display0 = byKey.getValue("screen.on.display0")
        assertEquals("screen.on", display0.back_filled_from)
        assertNull(display0.declared_value)
        assertEquals(0.1, display0.effective_value)
    }
}
