rootProject.name = "SKIE"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }

    includeBuild("build-setup")
}

dependencyResolutionManagement {
    repositories {
        maven("https://api.touchlab.dev/public") {
            content {
                includeModule("org.jetbrains.kotlin", "kotlin-native-compiler-embeddable")
            }
        }
        mavenCentral()
        google()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

includeBuild("example")
includeBuild("plugin")
includeBuild("dev-support")

include(
    ":acceptance-tests:framework",
    ":acceptance-tests:external-libraries",
    ":acceptance-tests:type-mapping",
    ":acceptance-tests:type-mapping:exported-dependency",
    ":acceptance-tests:type-mapping:nonexported-dependency",
    ":acceptance-tests",
)
