import org.gradle.api.artifacts.component.ProjectComponentIdentifier

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.binaryCompat)
}

kotlin {
    explicitApi()
    jvmToolchain(17)
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

// --- Gate: :audit depends on nothing but the Kotlin stdlib (ARCHITECTURE.md §2) ---
// Keyed on the artifact (aar / AgpVersionAttr), never on group prefixes.
val artifactType = Attribute.of("artifactType", String::class.java)
val agpVersionAttr =
    Attribute.of("com.android.build.api.attributes.AgpVersionAttr", String::class.java)

val checkAuditPurity by tasks.registering {
    val artifacts =
        configurations.named("runtimeClasspath").flatMap { it.incoming.artifacts.resolvedArtifacts }
    val allowed = setOf("org.jetbrains.kotlin:kotlin-stdlib", "org.jetbrains:annotations")
    doLast {
        val extra = artifacts.get().filter {
            it.id.componentIdentifier.displayName.substringBeforeLast(':') !in allowed
        }
        val android = artifacts.get().filter { art ->
            art.variant.attributes.getAttribute(artifactType) == "aar" ||
                art.variant.attributes.getAttribute(agpVersionAttr) != null
        }
        require(extra.isEmpty() && android.isEmpty()) {
            buildString {
                append("audit must depend on nothing but the Kotlin standard library")
                if (extra.isNotEmpty()) append("; unlisted: ").append(extra.joinToString { it.id.displayName })
                if (android.isNotEmpty()) append("; android artifact: ").append(android.joinToString { it.id.displayName })
            }
        }
    }
}

// --- Gate: :audit is a leaf; its test classpath sees no other project (ARCHITECTURE.md §2) ---
val checkAuditIsLeaf by tasks.registering {
    val projects = configurations.named("testRuntimeClasspath").map { cfg ->
        cfg.incoming.resolutionResult.allComponents
            .map { it.id }.filterIsInstance<ProjectComponentIdentifier>()
            .map { it.projectPath }.toSet()
    }
    doLast {
        require(projects.get() == setOf(":audit")) {
            "audit must not depend on another project, found: ${projects.get() - ":audit"}"
        }
    }
}

// --- Gate: no public signature names a type outside kotlin.* and the project (ARCHITECTURE.md §2) ---
// The allowlist is the JVM rendering of Kotlin builtins, not a widening of the rule: Kotlin's String
// IS java/lang/String in the ABI. Anything else - android.*, javax.*, java.io.*, a third party - is
// a real leak, and a compileOnly dependency is exactly how one arrives without failing purity.
val checkAuditApiTypes by tasks.registering {
    val apiFile = layout.projectDirectory.file("api/audit.api").asFile
    inputs.file(apiFile)
    doLast {
        val allowedPrefixes = listOf("io/github/tahmid1999/sipper/", "kotlin/")
        val allowedExact = setOf(
            "java/lang/Object", "java/lang/String", "java/lang/Enum", "java/lang/CharSequence",
            "java/lang/Number", "java/lang/Comparable", "java/lang/Iterable", "java/lang/Throwable",
            "java/lang/annotation/Annotation",
            "java/util/List", "java/util/Map", "java/util/Set", "java/util/Collection",
        )
        val offenders = Regex("""[a-z][A-Za-z0-9]*(?:/[\w${'$'}]+)+""")
            .findAll(apiFile.readText())
            .map { it.value }
            .filterNot { type ->
                type in allowedExact || allowedPrefixes.any { type.startsWith(it) }
            }
            .toSortedSet()
        require(offenders.isEmpty()) {
            "public API of :audit names types outside kotlin.* and the project package: $offenders"
        }
    }
}

tasks.named("check") {
    dependsOn(checkAuditPurity, checkAuditIsLeaf, checkAuditApiTypes)
}
