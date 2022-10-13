pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://api.touchlab.dev/public")
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        maven("https://api.touchlab.dev/public")
        mavenLocal()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "SwiftLink"

include(":example")
include(":example:static")
include(":example:dynamic")
include(":example:plugin")

includeBuild("swiftlink-plugin")
