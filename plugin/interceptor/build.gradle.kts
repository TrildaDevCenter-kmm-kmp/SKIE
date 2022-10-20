import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader

plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    compileOnly(strippedKotlinNativeCompilerEmbeddable())

    implementation(projects.reflector)
    implementation(projects.spi)
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf("-Xcontext-receivers")
    }
}



fun strippedKotlinNativeCompilerEmbeddable(): FileCollection {
    val targetFile = layout.buildDirectory.file("tmp/kotlin-native-stripped").map {
        val file = it.asFile
        if (!file.exists()) {
            val tree = zipTree(
                NativeCompilerDownloader(project).also {
                    it.downloadIfNeeded()
                }.compilerDirectory.resolve("konan/lib/kotlin-native-compiler-embeddable.jar")
            )

            copy {
                from(tree)
                into(file)
            }
        }

        it
    }

    return files(targetFile)
}
