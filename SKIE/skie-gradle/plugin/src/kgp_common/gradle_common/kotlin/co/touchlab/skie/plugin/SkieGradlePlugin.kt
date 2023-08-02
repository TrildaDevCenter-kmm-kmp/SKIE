package co.touchlab.skie.plugin

import co.touchlab.skie.plugin.analytics.GradleAnalyticsManager
import co.touchlab.skie.plugin.configuration.CreateSkieConfigurationTask
import co.touchlab.skie.plugin.configuration.skieExtension
import co.touchlab.skie.plugin.dependencies.SkieCompilerPluginDependencyProvider
import co.touchlab.skie.plugin.dependencies.addDependencyOnSkieRuntime
import co.touchlab.skie.plugin.directory.SkieDirectoriesManager
import co.touchlab.skie.plugin.directory.skieDirectories
import co.touchlab.skie.plugin.fatframework.FatFrameworkConfigurator
import co.touchlab.skie.plugin.license.GradleSkieLicenseManager
import co.touchlab.skie.plugin.subplugin.SkieSubPluginManager
import co.touchlab.skie.plugin.switflink.SwiftLinkingConfigurator
import co.touchlab.skie.plugin.util.appleTargets
import co.touchlab.skie.plugin.util.doFirstOptimized
import co.touchlab.skie.plugin.util.frameworks
import co.touchlab.skie.plugin.util.subpluginOption
import co.touchlab.skie.util.plugin.SkiePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.konan.target.presetName

abstract class SkieGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.configureSkieGradlePlugin()

        project.afterEvaluate {
            project.configureSkieCompilerPlugin()
        }
    }

    private fun Project.configureSkieGradlePlugin() {
        GradleSkieLicenseManager(project).initializeLicensing()

        SkieSubPluginManager.configureDependenciesForSubPlugins(project)
    }

    private fun Project.configureSkieCompilerPlugin() {
        if (!isSkieEnabled) {
            return
        }

        FatFrameworkConfigurator.configureSkieForFatFrameworks(project)

        configureEachKotlinFrameworkLinkTask {
            configureSkieForLinkTask()
        }
    }

    private fun KotlinNativeLink.configureSkieForLinkTask() {
        SkieDirectoriesManager.configureCreateSkieBuildDirectoryTask(this)

        GradleAnalyticsManager(project).configureAnalytics(this)

        disableCaching()
        binary.target.addDependencyOnSkieRuntime()

        CreateSkieConfigurationTask.registerTask(this)

        SwiftLinkingConfigurator.configureCustomSwiftLinking(this)

        SkieSubPluginManager.registerSubPlugins(this)

        configureKotlinCompiler()
    }

    private fun KotlinNativeLink.configureKotlinCompiler() {
        compilerPluginClasspath = listOfNotNull(
            compilerPluginClasspath,
            SkieCompilerPluginDependencyProvider.getOrCreateDependencyConfiguration(project),
        ).reduce(FileCollection::plus)

        compilerPluginOptions.addPluginArgument(
            SkiePlugin.id,
            SkiePlugin.Options.skieDirectories.subpluginOption(skieDirectories),
        )
    }
}

internal fun Project.configureEachKotlinFrameworkLinkTask(
    configure: KotlinNativeLink.() -> Unit,
) {
    configureEachKotlinAppleTarget {
        frameworks.forEach { framework ->
            // Cannot use configure on linkTaskProvider because it's not possible to register new tasks in configure block of another task
            configure(framework.linkTask)
        }
    }
}

internal fun Project.configureEachKotlinAppleTarget(
    configure: KotlinNativeTarget.() -> Unit,
) {
    val kotlinExtension = extensions.findByType(KotlinMultiplatformExtension::class.java) ?: return

    kotlinExtension.appleTargets.forEach {
        configure(it)
    }
}

private fun KotlinNativeLink.disableCaching() {
    doFirstOptimized {
        project.logger.warn(
            "w: SKIE does not support Kotlin Native caching yet. Compilation time in debug mode might be increased as a result."
        )
    }

    project.extensions.extraProperties.set("kotlin.native.cacheKind.${binary.target.konanTarget.presetName}", "none")
}

private val Project.isSkieEnabled: Boolean
    get() = project.skieExtension.isEnabled.get()