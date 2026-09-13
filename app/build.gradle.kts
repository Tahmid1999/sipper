import org.gradle.api.artifacts.component.ProjectComponentIdentifier

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.tahmid1999.sipper.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.tahmid1999.sipper"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false // no R8 config is specced anywhere; nothing to keep yet
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // ARCHITECTURE.md §1: :app depends on all four modules.
    implementation(project(":audit"))
    implementation(project(":usage"))
    implementation(project(":data"))
    implementation(project(":collect"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.material.icons.core)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.work.runtime)

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    debugImplementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.test.manifest)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}

// --- Gate: :app may see the four project modules and nothing else (ARCHITECTURE.md §1) ---
val checkAppDependencies by tasks.registering {
    val projects = configurations.named("releaseRuntimeClasspath").map { cfg ->
        cfg.incoming.resolutionResult.allComponents
            .map { it.id }.filterIsInstance<ProjectComponentIdentifier>()
            .map { it.projectPath }.toSet()
    }
    doLast {
        val allowed = setOf(":app", ":audit", ":usage", ":data", ":collect")
        require(projects.get() == allowed) {
            "app may depend only on the four modules, found: ${projects.get() - allowed}"
        }
    }
}

tasks.named("check") {
    dependsOn(checkAppDependencies)
}
