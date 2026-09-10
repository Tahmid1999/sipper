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
