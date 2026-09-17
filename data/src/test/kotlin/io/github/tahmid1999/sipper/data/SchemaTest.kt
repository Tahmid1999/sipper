package io.github.tahmid1999.sipper.`data`

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class SchemaTest {

    @Test
    fun schemaCreatesTablesIndexesAndDedupes() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

        // 1. The schema applies to an empty database.
        SipperDatabase.Schema.create(driver)

        // 2. All seven tables exist.
        val tables = driver.executeQuery(
            null,
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'",
            { cursor ->
                val names = mutableSetOf<String>()
                while (cursor.next().value) {
                    cursor.getString(0)?.let { names.add(it) }
                }
                QueryResult.Value(names)
            },
            0,
        ).value
        assertEquals(
            setOf(
                "profile_audit",
                "key_result",
                "app_day",
                "app_event",
                "bucket_change",
                "intervention",
                "probe_result",
                "sampler_run",
            ),
            tables,
        )

        // 3. Both app_event indexes exist.
        val indexes = driver.executeQuery(
            null,
            "SELECT name FROM sqlite_master WHERE type='index'",
            { cursor ->
                val names = mutableSetOf<String>()
                while (cursor.next().value) {
                    cursor.getString(0)?.let { names.add(it) }
                }
                QueryResult.Value(names)
            },
            0,
        ).value
        assertTrue("app_event_pkg_ts" in indexes)
        assertTrue("app_event_dedupe" in indexes)

        // 4. The dedupe index actually dedupes: the same key twice fails the second insert.
        driver.execute(
            null,
            "INSERT INTO app_event (package, class_name, event_type, timestamp, ingested_at) " +
                "VALUES ('pkg', 'cls', 1, 1000, 2000)",
            0,
        )
        assertFails {
            driver.execute(
                null,
                "INSERT INTO app_event (package, class_name, event_type, timestamp, ingested_at) " +
                    "VALUES ('pkg', 'cls', 1, 1000, 2000)",
                0,
            )
        }

        // 5. class_name defaults to '', not NULL.
        driver.execute(
            null,
            "INSERT INTO app_event (package, event_type, timestamp, ingested_at) " +
                "VALUES ('pkg2', 1, 3000, 4000)",
            0,
        )
        val storedClass = driver.executeQuery(
            null,
            "SELECT class_name FROM app_event WHERE package = 'pkg2'",
            { cursor ->
                cursor.next().value
                QueryResult.Value(cursor.getString(0))
            },
            0,
        ).value
        assertEquals("", storedClass)
    }
}
