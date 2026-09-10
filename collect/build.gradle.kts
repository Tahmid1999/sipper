import org.gradle.api.artifacts.component.ProjectComponentIdentifier

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.github.tahmid1999.sipper.collect"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
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

// --- Gate: no platform read returns a bare value (ARCHITECTURE.md §3) ---
// binary-compatibility-validator produces no .api file for an Android library, so the api-surface
// gate that covers :audit and :usage cannot cover this module. This reads the source instead.
// explicitApi() guarantees every public function declares its return type, which is what makes a
// source-level check reliable here. Limitation, stated rather than hidden: the pattern matches a
// signature written on one line, which every function in this module currently is.
val checkCollectReturnsReading by tasks.registering {
    val sources = fileTree("src/main/kotlin") { include("**/*.kt") }
    inputs.files(sources)
    doLast {
        val bare = Regex("""public\s+fun\s+\w+\s*\([^)]*\)\s*:\s*(Double|Boolean|Long)\b""")
        val offenders = sources.files.flatMap { file ->
            bare.findAll(file.readText()).map { "${file.name}: ${it.value.trim()}" }
        }
        require(offenders.isEmpty()) {
            "a platform read returns a bare Double/Boolean/Long instead of a Reading: $offenders"
        }
    }
}

tasks.named("check") {
    dependsOn(checkCollectDependencies, checkCollectReturnsReading)
}
