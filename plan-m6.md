# Milestone 6 — `:collect`, the Android library

ONE STEP PER PROMPT. **Source:** ARCHITECTURE §3 owns this module.

## Read this before step 1: what "green" means here

Every module so far could be fully tested off-device, so a green build meant verified. **`:collect`
cannot.** Its whole job is reading a real platform, and JVM unit tests get stubbed `android.*` classes
that throw. This milestone gets the module compiling, lint-clean and gated — it does **not** verify
that any read returns what it should. That happens in a device session, and gate 3's capture depends
on it.

So: no unit tests are asked for below. Do not invent tests that assert against stubbed Android
classes; a test that passes because a stub returned a default is worse than no test.

## Two things that may block step 1

0. **Do NOT add the `org.jetbrains.kotlin.android` plugin.** AGP 9.0+ has Kotlin support built in and rejects it outright. An earlier draft of this plan specified it; that was wrong, and the `kotlin { explicitApi(); jvmToolchain(17) }` block works without it.

1. **AGP 9.1.0 against Gradle 8.13.** AGP 9 may require a newer Gradle than the wrapper has. If the
   build says so, **you are pre-authorised to bump the wrapper** with
   `.\gradlew.bat wrapper --gradle-version <the version AGP asks for>` and to commit it. This is the
   one exception to the pinned-version rule in `.clinerules`, because it is a tooling requirement, not
   a spec change. Report the version you moved to. Change no other pinned version.
2. **The SDK location.** AGP finds it from the `ANDROID_HOME` environment variable, which is set on
   this machine. If it complains anyway, create `local.properties` at the repo root containing
   `sdk.dir=C\:\\Users\\UseR\\AppData\\Local\\Android\\sdk` and add `local.properties` to
   `.gitignore` — it is machine-specific and must never be committed.

---

## Step M6-1 — the module

**a)** `gradle/libs.versions.toml` — add under `[versions]`:
```toml
agp = "9.1.0"
```
and under `[plugins]`:
```toml
android-library = { id = "com.android.library", version.ref = "agp" }
```

**b)** `settings.gradle.kts` — AGP is not on Maven Central. Add `google()` to **both** repository
blocks, before the others:

```kotlin
pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```
and `include(":collect")`.

**c)** Root `build.gradle.kts` — add `alias(libs.plugins.android.library) apply false`.

**d)** Create `collect/build.gradle.kts`:

```kotlin
import org.gradle.api.artifacts.component.ProjectComponentIdentifier

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.binaryCompat)
}

android {
    namespace = "io.github.tahmid1999.sipper.collect"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // unsafeCheckOpNoThrow is API 29 and minSdk is 28, so this class of defect must fail the
    // build rather than the phone. ARCHITECTURE §3.
    lint {
        error += "NewApi"
    }
}

kotlin {
    explicitApi()
    jvmToolchain(17)
}

dependencies {
    implementation(project(":audit"))
    implementation(project(":usage"))
}

// --- Gate: :collect may see :audit and :usage, and nothing else (ARCHITECTURE.md §1) ---
val checkCollectDependencies by tasks.registering {
    val projects = configurations.named("releaseRuntimeClasspath").map { cfg ->
        cfg.incoming.resolutionResult.allComponents
            .map { it.id }.filterIsInstance<ProjectComponentIdentifier>()
            .map { it.projectPath }.toSet()
    }
    doLast {
        val allowed = setOf(":collect", ":audit", ":usage")
        require(projects.get() == allowed) {
            "collect may depend only on :audit and :usage, found: ${projects.get() - allowed}"
        }
    }
}

tasks.named("check") {
    dependsOn(checkCollectDependencies)
}
```

Verify: `.\gradlew.bat projects` lists `:collect`, then `.\gradlew.bat :collect:assembleRelease`.
If either fails on a version or SDK problem, apply the pre-authorised fix above and say what you did.
Commit: `collect: android library scaffold with the dependency gate`.

Step M6-1 is DONE (commit 64be5bf). The wrapper was bumped 8.13 -> 9.3.1 because AGP 9.1.0 required
it; all four modules stayed green on it.

## Step M6-2 — `ProfileXmlReader.kt`

`collect/src/main/kotlin/io/github/tahmid1999/sipper/collect/ProfileXmlReader.kt`.

The resource routes hand back **binary** XML through an `XmlResourceParser`, not text, so `:audit`'s
`parseProfile(String)` — which is StAX over a `StringReader` — cannot be used on them. This reader
walks an `XmlPullParser` and produces the same `ParsedProfile`, so everything downstream is unchanged.

```kotlin
public fun readProfile(parser: XmlPullParser): ParsedProfile
```

Rules, matching `:audit`'s parser exactly so the two agree:

- `<item name="k">v</item>` becomes one entry with a single-element list.
- `<array name="k"><value>v</value>…</array>` becomes one entry with every value, in file order.
- Keys keep declaration order — use a `LinkedHashMap`.
- Values are `text.trim().toDouble()`.
- Line numbers come from `parser.lineNumber` at the element's `START_TAG`, giving the `lines` map.
- Ignore any other element. Do not throw on one.

Import `org.xmlpull.v1.XmlPullParser` and `XmlPullParserFactory` is not needed — the caller supplies
the parser. Use `XmlPullParser.START_TAG`, `TEXT`, `END_TAG`, `END_DOCUMENT` and `next()`.

Verify: `.\gradlew.bat :collect:assembleRelease`. Commit: `collect: binary-XML profile reader`.

## Step M6-3 — `PowerProfileRoutes.kt`

Two profile-reading functions that return `Reading<ParsedProfile>`, and one per-key reflection probe.
Never combine these into a single function with a strategy parameter — their disagreement is the
finding, and a strategy parameter invites a caller to pick one and move on (ARCHITECTURE §3).

The two resource readers each return `Reading<ParsedProfile>` and each carry their own `Route`:

| function | mechanism | RouteKind |
| --- | --- | --- |
| `readViaSystemResources()` | `Resources.getSystem().getIdentifier("power_profile", "xml", "android")` then `getXml(id)` | `SYSTEM_RESOURCES` |
| `readViaAndroidPackage(context)` | `context.packageManager.getResourcesForApplication("android")`, same identifier lookup | `ANDROID_PACKAGE_RESOURCES` |

The reflection route is a per-key cross-check, not a document reader — it probes individual power values:

```kotlin
public fun reflectAveragePower(context: Context, key: String): Reading<Double>
```

It returns `Reading<Double>` because reflection can only retrieve a single keyed value, matching `key_result.reflection_value REAL` in the `:data` schema. The earlier draft returned `Reading<ParsedProfile>` but that was wrong because reflection is a per-key probe.

Rules:

- A resolved id of `0` means the resource does not exist: return
  `Reading.SilentDefault(ParsedProfile(emptyMap(), emptyMap()), DefaultCause.KEY_ABSENT_FROM_PROFILE, route)`.
- On reflection, use `Class.forName`, get its constructor, instantiate with context, and call
  `getAveragePower(String)` with the key. Catch `NoSuchMethodException` **separately from**
  `SecurityException`. Hidden-API denial arrives as `NoSuchMethodException`, and a tool catching the
  wrong one reports a denial as "method removed", losing the distinction between blocked and gone.
- Catch `PackageManager.NameNotFoundException` and `Resources.NotFoundException` separately on both
  resource routes, and return `SilentDefault` the same way the `id==0` case does, since the resource
  genuinely is not there. Let any other exception propagate — do not catch it, as an unexpected
  exception must not be silently relabelled as a denial or a default.
- Every catch records what actually happened. No empty catch blocks anywhere in this file.

Verify: `.\gradlew.bat :collect:assembleRelease`. Commit: `collect: reflection is a per-key cross-check, not a document`.

## Step M6-4 — `UsageAccess.kt`

One implementation of the appop check, called from everywhere. CONTRACT §8.3a fixes it:

```kotlin
val mode = if (Build.VERSION.SDK_INT >= 29) {
    appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, packageName)
} else {
    @Suppress("DEPRECATION")
    appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, packageName)
}
val granted = mode == AppOpsManager.MODE_ALLOWED   // MODE_DEFAULT is denied for this op
```

Wrap it as `public fun usageAccessGranted(context: Context): Reading<Boolean>`, returning
`Reading.Value(granted, Route(RouteKind.APP_OPS, "OPSTR_GET_USAGE_STATS"))`.

`checkSelfPermission(PACKAGE_USAGE_STATS)` reports denied forever even when the call works, so it is
not used here. Say so in a comment.

Verify: `.\gradlew.bat :collect:assembleRelease` **and** `.\gradlew.bat :collect:lintRelease` — the
lint run is the point of this step, since `NewApi` is an error and the branch above is exactly what it
guards. Commit: `collect: the branched usage-access check`.

## Step M6-5 — the collector return-type gate, apiDump, full check

ARCHITECTURE §3 says no method here returns a bare `Double`, `Boolean` or `Long` sourced from the
platform, and that the API-surface gate keeps it true. Make that mechanical.

Add to `collect/build.gradle.kts`, wired into `check`:

```kotlin
// --- Gate: no platform read returns a bare value (ARCHITECTURE.md §3) ---
// Scoped to top-level functions in the read files: a data class getter returning Int is fine, a
// collector handing back a raw Double is the defect this module exists to prevent.
val checkCollectReturnsReading by tasks.registering {
    val apiFile = layout.projectDirectory.file("api/collect.api").asFile
    inputs.file(apiFile)
    doLast {
        val offenders = apiFile.readLines()
            .filter { it.contains("public static final fun") || it.contains("public final fun") }
            .filter { Regex("""\)[DZJ]${'$'}""").containsMatchIn(it.trim()) }
        require(offenders.isEmpty()) {
            "collect returns a bare Double/Boolean/Long instead of a Reading: $offenders"
        }
    }
}
```

Then `.\gradlew.bat :collect:apiDump`, commit `collect/api/collect.api`, and run
`.\gradlew.bat check` at the root — four modules, nine gates.

If the gate fires on something legitimate, **report it rather than widening the filter.** Commit:
`collect: gate that every platform read returns a Reading`.

## Report at the end

Every commit, the Gradle version if you had to bump it, whether `local.properties` was needed, and the
full root `check` output.

## Not in this milestone

- **Any test.** Verification of `:collect` happens on a device.
- **Gate 3's capture.** It needs a probe running on the emulator, which is the session after this one.
- **The remaining collectors** — battery, thermal, network stats, usage events, package manager. This
  milestone establishes the shape with the profile routes and the appop check; the rest follow it.
