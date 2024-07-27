// @file:Suppress("invisible_reference", "invisible_member")
// import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
// import org.jetbrains.kotlin.gradle.plugin.launchInRequiredStage
import io.github.gradlenexus.publishplugin.InitializeNexusStagingRepository
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

plugins {
//     id("skie.runtime")
    kotlin("multiplatform") version "@targetKotlinVersion@"
    `maven-publish`
    alias(libs.plugins.nexusPublish)
}

group = "co.touchlab.skie"
version = System.getenv("RELEASE_VERSION").orEmpty().ifBlank { "1.0.0-SNAPSHOT" }

nexusPublishing {
    repositoryDescription = "$group:SKIE:$version"

    this.repositories {
        sonatype()
    }
}

tasks.withType<InitializeNexusStagingRepository>().configureEach {
    isEnabled = false
}

// skiePublishing {
//     name = "SKIE Configuration Annotations"
//     description = "Annotations to configure SKIE behavior."
//     publishSources = true
// }

@smokeTestTmpRepositoryConfiguration@

val isRelease = !version.toString().endsWith("SNAPSHOT")
val isPublishing = gradle.startParameter.taskNames.contains("publishToSonatype")
val shouldSign = isRelease && isPublishing
if (shouldSign) {
    apply<SigningPlugin>()
    val signing = extensions.getByType<SigningExtension>()
    val signingKey: String? by project
    val signingPassword: String? by project
    if (!signingKey.isNullOrBlank()) {
        signing.useInMemoryPgpKeys(signingKey, signingPassword)
    }
    publishing.publications.withType<MavenPublication>().configureEach {
        signing.sign(this)
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name = "SKIE Configuration Annotations"
            description = "Annotations to configure SKIE behavior."
            url = "https://skie.touchlab.co"

            licenses {
                license {
                    name = "The Apache License, Version 2.0"
                    url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                }
            }

            developers {
                listOf(
                    "Kevin Galligan" to "kevin@touchlab.co",
                    "Filip Dolnik" to "filip@touchlab.co",
                    "Tadeas Kriz" to "tadeas@touchlab.co",
                ).forEach { (name, email) ->
                    developer {
                        this.name = name
                        this.email = email
                        organization = "Touchlab"
                        organizationUrl = "https://touchlab.co"
                    }
                }
            }

            scm {
                connection = "scm:git:git://github.com/touchlab/SKIE.git"
                developerConnection = "scm:git:ssh://github.com:touchlab/SKIE.git"
                url = "https://github.com/touchlab/SKIE"
            }
        }

        val publication = this
        val javadocJar = tasks.register<Jar>("${publication.name}JavadocJar") {
            archiveClassifier.set("javadoc")
            // Each archive name should be distinct. Mirror the format for the sources Jar tasks.
            archiveBaseName.set("${archiveBaseName.orNull ?: project.name}-${publication.name}")
        }
        artifact(javadocJar)
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll("-module-name", "co.touchlab.skie:configuration-annotations")
    }
}

tasks.withType<KotlinNativeCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll("-module-name", "co.touchlab.skie:configuration-annotations")
    }
}

kotlin {
    val jvmVersion = libs.versions.java.get().toInt()

    jvmToolchain(jvmVersion)
    targets.all {
        mavenPublication {
            attributes {
                // TODO: Use `libs.versions.java`
                attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, jvmVersion)
            }
        }
    }

    @targets@

//     jvm()
//     js {
//         browser()
//         nodejs()
//     }
//
//     androidNativeArm32()
//     androidNativeArm64()
//     androidNativeX86()
//     androidNativeX64()
//
// //     iosArm32()
//     iosArm64()
//     iosX64()
//     iosSimulatorArm64()
//
//     watchosArm32()
//     watchosArm64()
// //     watchosX86()
//     watchosX64()
//     watchosSimulatorArm64()
//     watchosDeviceArm64()
//
//     tvosArm64()
//     tvosX64()
//     tvosSimulatorArm64()
//
//     macosX64()
//     macosArm64()
//
//     linuxArm64()
// //     linuxArm32Hfp()
//     linuxX64()
//
//     mingwX64()
// //     mingwX86()
//
// //     wasm32()
}

// afterEvaluate {
//     println("Components:")
//     components.forEach {
//         println("\t${it.name}")
//     }
// }

// val p = project
// project.launchInRequiredStage(KotlinPluginLifecycle.Stage.ReadyForExecution) {
//     println("Components: $p")
//     components.forEach { component ->
//         println("\t- ${component.name} - $component")
//     }
//
//     println("Publications:")
//     publishing.publications.forEach { publication ->
//         println("\t- ${publication.name} - $publication")
//     }
//
// }
