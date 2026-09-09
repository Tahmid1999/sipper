pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "sipper"

include(":audit")
include(":usage")
include(":data")
