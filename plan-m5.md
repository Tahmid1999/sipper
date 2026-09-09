# Milestone 5 — `:data`, the SQLDelight schema

Run these ONE STEP PER PROMPT. Milestone 4 hit DeepSeek's 8k output ceiling twice on multi-step runs;
do not batch these.

**Source:** ARCHITECTURE §8 owns this module, and gives the migration verbatim.

**Why it is a plain JVM module, not an Android library.** Its tests then run against a real embedded
sqlite file under plain JUnit — no emulator, no Robolectric. `:app` supplies
`app.cash.sqldelight:android-driver`; `:data` depends on the JDBC driver alone, and that dependency is
`implementation` and not `api`, so a driver choice cannot leak into a repository signature.

**Scope discipline.** This milestone builds the schema, proves the migration applies from empty, and
adds queries for the one thing `:audit` already produces — a `ProfileAudit` and its per-key verdicts.
It does **not** invent queries or repositories for data no module produces yet. `app_day`,
`app_event`, `bucket_change`, `intervention` and `probe_result` get their tables and nothing else,
because `:collect` does not exist and any value written into them today would be invented.

---

## Step M5-1 — the module

**a)** `gradle/libs.versions.toml` — add:

```toml
sqldelight = "2.3.2"
```
under `[versions]`,
```toml
sqldelight = { id = "app.cash.sqldelight", version.ref = "sqldelight" }
```
under `[plugins]`, and a `[libraries]` section (create it if absent) containing:
```toml
sqldelight-sqlite-driver = { module = "app.cash.sqldelight:sqlite-driver", version.ref = "sqldelight" }
```

**b)** Root `build.gradle.kts` — add `alias(libs.plugins.sqldelight) apply false` to the plugins block.

**c)** `settings.gradle.kts` — add `include(":data")`.

**d)** Create `data/build.gradle.kts`:

```kotlin
import org.gradle.api.artifacts.component.ProjectComponentIdentifier

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.sqldelight)
}

kotlin {
    jvmToolchain(17)
}

sqldelight {
    databases {
        create("SipperDatabase") {
            packageName.set("io.github.tahmid1999.sipper.data")
            schemaOutputDirectory.set(file("src/main/sqldelight/databases"))
            deriveSchemaFromMigrations.set(true)
            verifyMigrations.set(true)
        }
    }
}

dependencies {
    implementation(project(":audit"))
    implementation(project(":usage"))
    // implementation, never api: the driver choice must not leak into a repository signature.
    implementation(libs.sqldelight.sqlite.driver)
    testImplementation(kotlin("test"))
}

// --- Gate: :data may see :audit and :usage, and nothing else (ARCHITECTURE.md §1) ---
val checkDataDependencies by tasks.registering {
    val projects = configurations.named("runtimeClasspath").map { cfg ->
        cfg.incoming.resolutionResult.allComponents
            .map { it.id }.filterIsInstance<ProjectComponentIdentifier>()
            .map { it.projectPath }.toSet()
    }
    doLast {
        val allowed = setOf(":data", ":audit", ":usage")
        require(projects.get() == allowed) {
            "data may depend only on :audit and :usage, found: ${projects.get() - allowed}"
        }
    }
}

tasks.named("check") {
    dependsOn(checkDataDependencies)
}
```

Note `:data` does **not** set `explicitApi()` or the binary-compatibility validator — ARCHITECTURE §2
applies those to `:audit` and `:usage` only.

Verify: `.\gradlew.bat projects` lists `:data`. `.\gradlew.bat :data:compileKotlin` may fail until
M5-2 exists — if it does, say so and move on rather than inventing a fix.
Commit: `data: module scaffold with the dependency gate`.

## Step M5-2 — `1.sqm`

Create `data/src/main/sqldelight/io/github/tahmid1999/sipper/data/1.sqm` with exactly the SQL below.
It is the initial schema as a **migration**, not a `.sq` file, so there is no version zero whose
upgrade path has to be invented later. Copy it verbatim, comments included.

```sql
-- One row per audit run. Everything needed to reproduce the verdicts is here,
-- so a screenshot can be traced back to a row rather than to "a phone, once".
CREATE TABLE profile_audit (
    id                       INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    captured_at              INTEGER NOT NULL,   -- epoch ms
    boot_count               INTEGER NOT NULL,   -- Settings.Global.BOOT_COUNT; no permission,
                                                 -- and it is what makes elapsedRealtime spans
                                                 -- from different boots non-comparable rather
                                                 -- than silently comparable
    fingerprint              TEXT NOT NULL,      -- Build.FINGERPRINT
    api_level                INTEGER NOT NULL,
    route_system_sha         TEXT,               -- sha256 of the bytes each route resolved;
    route_package_sha        TEXT,               -- null means the route itself failed, which is
    route_reflection_ok      INTEGER NOT NULL,   -- different from resolving to nothing
    routes_agree             INTEGER NOT NULL,
    disagreement             TEXT,               -- the rendered diff; null iff routes_agree = 1
    declared_key_count       INTEGER NOT NULL,
    battery_capacity_declared REAL,
    charge_counter_uah       INTEGER             -- the independent cross-check on capacity
);

-- One row per key per audit. verdict is stored as its enum name, not an ordinal:
-- a reordered enum must not silently rewrite history, and a person running
-- sqlite3 against this file should read ZERO_BY_ABSENCE rather than 2.
CREATE TABLE key_result (
    audit_id         INTEGER NOT NULL REFERENCES profile_audit(id) ON DELETE CASCADE,
    key              TEXT NOT NULL,
    verdict          TEXT NOT NULL,   -- PRESENT | ZERO_BY_EXPLICIT_VALUE | ZERO_BY_ABSENCE | BACK_FILLED
    declared_value   REAL,            -- null when the key is not in the file at all
    effective_value  REAL NOT NULL,   -- what getAveragePower returns, back-fill included
    back_filled_from TEXT,            -- null unless verdict = BACK_FILLED
    reflection_value REAL,            -- the cross-check route; recorded, never authoritative
    calculator       TEXT,            -- null when nothing reads this key at this api_level
    PRIMARY KEY (audit_id, key)
);

-- One row per package per local day. Both foreground numbers live here side by
-- side because the delta between them is the finding, and a schema that stored
-- only the better one would make the finding unrecoverable.
CREATE TABLE app_day (
    package             TEXT NOT NULL,
    day                 TEXT NOT NULL,      -- ISO-8601 local date, yyyy-MM-dd
    fw_foreground_ms    INTEGER,            -- queryUsageStats, unclipped; null when access is off
    recon_foreground_ms INTEGER NOT NULL,
    unclosed_tail_ms    INTEGER NOT NULL,   -- the residual, published rather than absorbed
    dup_resumes         INTEGER NOT NULL,   -- the multi-instance residual
    visible_ms          INTEGER,            -- null below API 29: the events do not exist
    fgs_ms              INTEGER,            -- null below API 29
    rx_fg_bytes         INTEGER,
    tx_fg_bytes         INTEGER,
    rx_bg_bytes         INTEGER,
    tx_bg_bytes         INTEGER,
    window_start        INTEGER NOT NULL,   -- the exact ms bounds the reconstruction used, so a
    window_end          INTEGER NOT NULL,   -- row can be recomputed from app_event and checked
    closed_by_shutdown  INTEGER NOT NULL,
    lost_tails          INTEGER NOT NULL,
    PRIMARY KEY (package, day)
);

-- The raw stream. class_name is NOT NULL DEFAULT '' rather than nullable
-- because SQLite treats NULLs as distinct in a UNIQUE index, so a nullable
-- column here would let every re-poll insert a duplicate of every classless
-- event and quietly double the reconstruction.
CREATE TABLE app_event (
    id          INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    package     TEXT NOT NULL,
    class_name  TEXT NOT NULL DEFAULT '',
    event_type  INTEGER NOT NULL,   -- raw int; several of these have no public constant
    timestamp   INTEGER NOT NULL,
    ingested_at INTEGER NOT NULL    -- separate from timestamp: overlapping poll windows are
                                    -- normal and the gap between the two is how a late flush
                                    -- is told from a late poll
);
CREATE INDEX app_event_pkg_ts ON app_event(package, timestamp);
CREATE UNIQUE INDEX app_event_dedupe ON app_event(package, class_name, event_type, timestamp);

CREATE TABLE bucket_change (
    package   TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    bucket    INTEGER NOT NULL,   -- raw int; 5, 15, 45 and 50 all occur and none is a
                                  -- public constant, so an enum here would lose values
    PRIMARY KEY (package, timestamp)
);

-- Route, then observe. There is no column for "applied", because sipper never
-- applies anything and a nullable applied_at would invite one.
CREATE TABLE intervention (
    id                 INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    package            TEXT NOT NULL,
    routed_at          INTEGER NOT NULL,
    action             TEXT NOT NULL,   -- the Intent action actually launched
    resolved_component TEXT,            -- what resolveActivity returned; null when nothing did
    returned_at        INTEGER,         -- null if the user never came back
    readback_level     TEXT NOT NULL,   -- A (first-party, sipper's own package) | B | C
    readback_before    TEXT,            -- the Reading, rendered, cause included
    readback_after     TEXT,
    observed_change    TEXT             -- null until the observation window closes
);

CREATE TABLE probe_result (
    id          INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    probe       TEXT NOT NULL,   -- stable id, e.g. "power_profile.reflection"
    run_at      INTEGER NOT NULL,
    api_level   INTEGER NOT NULL,
    outcome     TEXT NOT NULL,   -- VALUE | SILENT_DEFAULT | DENIED | ABSENT
    detail      TEXT NOT NULL,   -- the verbatim runtime line, exception class and message included
    duration_us INTEGER NOT NULL
);
CREATE INDEX probe_result_probe_run ON probe_result(probe, run_at);
```

Verify: `.\gradlew.bat :data:generateMainSipperDatabaseInterface` (or `:data:build`) succeeds and
`data/src/main/sqldelight/databases/1.db` is produced. Report the exact task name that worked.
Commit both the migration and the generated `1.db`: `data: initial schema as 1.sqm`.

## Step M5-3 — `KeyResult.sq`

Create `data/src/main/sqldelight/io/github/tahmid1999/sipper/data/KeyResult.sq` with these queries and
nothing more. Queries live in `.sq` files; the schema stays in the migration.

```sql
insertAudit:
INSERT INTO profile_audit (
    captured_at, boot_count, fingerprint, api_level,
    route_system_sha, route_package_sha, route_reflection_ok, routes_agree,
    disagreement, declared_key_count, battery_capacity_declared, charge_counter_uah
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

lastAuditId:
SELECT last_insert_rowid();

insertKeyResult:
INSERT INTO key_result (
    audit_id, key, verdict, declared_value, effective_value,
    back_filled_from, reflection_value, calculator
) VALUES (?, ?, ?, ?, ?, ?, ?, ?);

keyResultsFor:
SELECT * FROM key_result WHERE audit_id = ? ORDER BY key;

verdictCountsFor:
SELECT verdict, COUNT(*) AS n FROM key_result WHERE audit_id = ? GROUP BY verdict ORDER BY verdict;

auditById:
SELECT * FROM profile_audit WHERE id = ?;
```

Verify: the module compiles and the generated queries appear.
Commit: `data: key_result queries`.

## Step M5-4 — `SchemaTest.kt`

`data/src/test/kotlin/io/github/tahmid1999/sipper/data/SchemaTest.kt`, kotlin.test. Open a real
sqlite file with the JDBC driver, apply the schema from empty, and assert:

1. `SipperDatabase.Schema.create(driver)` succeeds on an empty database.
2. All seven tables exist. Query
   `SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'`
   and assert the set equals `profile_audit, key_result, app_day, app_event, bucket_change,
   intervention, probe_result`.
3. Both `app_event` indexes exist: query `sqlite_master` for `type='index'` and assert
   `app_event_pkg_ts` and `app_event_dedupe` are present.
4. **The dedupe index actually dedupes.** Insert the same `(package, class_name, event_type,
   timestamp)` twice by raw SQL and assert the second insert fails. This is the constraint the schema
   comment justifies, and an untested UNIQUE index is a comment.
5. **`class_name` defaults to empty, not null.** Insert an `app_event` without `class_name` and assert
   the stored value is `''`. A NULL there would make the dedupe index useless, because SQLite treats
   NULLs as distinct.

Use an in-memory JDBC url (`JdbcSqliteDriver.IN_MEMORY`) unless a file is needed. If the driver's
exact API differs from what you expect, report the compiler error rather than guessing at an API.

Verify: `.\gradlew.bat :data:test`. Commit: `data: schema and dedupe-index tests`.

## Step M5-5 — `AuditRepository.kt` + its test

`data/src/main/kotlin/io/github/tahmid1999/sipper/data/AuditRepository.kt`. One class taking a
`SipperDatabase`, with two methods:

- `save(audit: ProfileAudit, capturedAt: Long, bootCount: Long, fingerprint: String, declaredKeyCount: Long): Long`
  — inserts one `profile_audit` row and one `key_result` row per verdict, returning the new audit id.
  Store the verdict as `verdict.name`, never its ordinal.
- `verdictCounts(auditId: Long): Map<Verdict, Long>` — reads the counts back, mapping the stored name
  through `Verdict.valueOf`.

Fields the `:audit` module cannot supply — the route shas, `routes_agree`, `disagreement`,
`battery_capacity_declared`, `charge_counter_uah`, `reflection_value` — are **parameters or nulls**,
never invented defaults. `routes_agree` and `route_reflection_ok` are `NOT NULL`, so take them as
parameters with no default.

Test in `data/src/test/kotlin/.../AuditRepositoryTest.kt`: build a small `ProfileAudit` by hand with
one verdict of each of the four kinds, save it, read the counts back, and assert each is 1 and that
`Verdict.valueOf` round-trips every name.

Verify: `.\gradlew.bat :data:test`. Commit: `data: audit repository and round-trip test`.

## Step M5-6 — full check

`.\gradlew.bat check` at the root: three modules, seven gates, every test.
Commit anything outstanding: `data: milestone 5 complete`.

## Report at the end

The step list, every commit, the final test counts per suite for all three modules, and the exact name
of the gradle task that generated `1.db`.

## Not in this milestone

- Queries or repositories for `app_day`, `app_event`, `bucket_change`, `intervention`,
  `probe_result` — nothing produces that data until `:collect` exists.
- Retention pruning (`app_event` at 11 days) — it belongs with the sampler, in `:app`.
- Gate 3 — still blocked on emulator captures.
