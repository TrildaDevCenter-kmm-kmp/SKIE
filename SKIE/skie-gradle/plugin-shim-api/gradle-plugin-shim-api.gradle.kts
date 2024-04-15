plugins {
    id("skie.gradle")
    id("skie.publishable")
}

skiePublishing {
    name = "SKIE Gradle Plugin Shim API"
    description = "API that's implemented by the SKIE Gradle Plugin Shim Impl, used by the main plugin module to interact with Kotlin Gradle Plugin."
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.common.configuration.configurationDeclaration)
                implementation(projects.common.configuration.configurationApi)

                compileOnly(libs.plugin.kotlin.gradle)
                compileOnly(libs.plugin.kotlin.gradle.api)
            }
        }
    }
}