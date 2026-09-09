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
