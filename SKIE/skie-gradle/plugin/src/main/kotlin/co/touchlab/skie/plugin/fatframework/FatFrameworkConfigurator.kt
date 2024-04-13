package co.touchlab.skie.plugin.fatframework

import co.touchlab.skie.plugin.util.*
import co.touchlab.skie.plugin.util.TargetTriple
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.tasks.FatFrameworkTask
import org.jetbrains.kotlin.gradle.tasks.FrameworkLayout
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.Family
import java.io.File

internal object FatFrameworkConfigurator {

    fun configureSkieForFatFrameworks(project: Project) {
        project.fixFatFrameworkNameForCocoaPodsPlugin()
        project.configureFatFrameworkPatching()
    }

    private fun Project.fixFatFrameworkNameForCocoaPodsPlugin() {
        pluginManager.withPlugin("kotlin-native-cocoapods") {
            tasks.withType<FatFrameworkTask>().matching { it.name == "fatFramework" }.configureEach {
                // Unfortunately has to be done in `doFirst` to make sure the task is already configured by the plugin when we run our code
                doFirstOptimized {
                    val commonFrameworkName = frameworks.map { it.name }.distinct().singleOrNull() ?: return@doFirstOptimized
                    baseName = commonFrameworkName
                }
            }
        }
    }

    private fun Project.configureFatFrameworkPatching() {
        tasks.withType<FatFrameworkTask>().configureEach {
            doLastOptimized {
                // There shouldn't be any real difference between the frameworks except for architecture, so we consider the first one "primary"
                val primaryFramework = frameworks.first()
                val target = FrameworkLayout(
                    rootDir = fatFramework,
                    isMacosFramework = primaryFramework.files.isMacosFramework,
                )

                // FatFramewrokTask writes its own
                target.moduleFile.writeText(
                    primaryFramework.files.moduleFile.readText()
                )

                val frameworksByArchs = frameworks.associateBy { it.target.architecture }
                target.swiftHeader.writer().use { writer ->
                    val swiftHeaderContents = frameworksByArchs.mapValues { (_, framework) ->
                        framework.files.swiftHeader.readText()
                    }

                    if (swiftHeaderContents.values.distinct().size == 1) {
                        writer.write(swiftHeaderContents.values.first())
                    } else {
                        swiftHeaderContents.toList().forEachIndexed { i, (arch, content) ->
                            val macro = arch.clangMacro
                            if (i == 0) {
                                writer.appendLine("#if defined($macro)\n")
                            } else {
                                writer.appendLine("#elif defined($macro)\n")
                            }
                            writer.appendLine(content)
                        }
                        writer.appendLine(
                            """
                            #else
                            #error Unsupported platform
                            #endif
                            """.trimIndent(),
                        )
                    }
                }

                target.swiftModuleDir.mkdirs()

                frameworksByArchs.toList().forEach { (_, framework) ->
                    project.copy {
                        from(framework.files.apiNotes)
                        into(target.headerDir)
                    }
                    framework.files.swiftModuleFiles(framework.darwinTarget.targetTriple).forEach { swiftmoduleFile ->
                        project.copy {
                            from(swiftmoduleFile)
                            into(target.swiftModuleDir)
                        }
                    }
                }
            }
        }
    }
}

private val FrameworkLayout.frameworkName: String
    get() = rootDir.nameWithoutExtension

private val FrameworkLayout.swiftHeader: File
    get() = headerDir.resolve("$frameworkName-Swift.h")

private val FrameworkLayout.apiNotes: File
    get() = headerDir.resolve("$frameworkName.apinotes")

private val FrameworkLayout.swiftModuleDir: File
    get() = modulesDir.resolve("$frameworkName.swiftmodule")

private fun FrameworkLayout.swiftModuleFiles(triple: TargetTriple): List<File> {
    return listOf("abi.json", "swiftdoc", "swiftinterface", "swiftmodule", "swiftsourceinfo").map { ext ->
        swiftModuleDir.resolve("$triple.$ext")
    }
}

private val Architecture.clangMacro: String
    get() = when (this) {
        Architecture.X86 -> "__i386__"
        Architecture.X64 -> "__x86_64__"
        Architecture.ARM32 -> "__arm__"
        Architecture.ARM64 -> "__aarch64__"
        else -> error("Fat frameworks are not supported for architecture `$name`")
    }
