package co.touchlab.skie.plugin.subplugin

import org.gradle.api.DomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink

internal object SkieSubPluginManager {

    private const val subPluginConfigurationName: String = "skieSubPlugin"

    fun configureDependenciesForSubPlugins(project: Project) {
        val subPluginConfiguration = project.configurations.create(subPluginConfigurationName).apply {
            isCanBeResolved = true
            isCanBeConsumed = false
            isVisible = false
            isTransitive = true

            exclude("org.jetbrains.kotlin", "kotlin-stdlib-common")

            attributes {
                it.attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.LIBRARY))
                it.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_RUNTIME))
                it.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.objects.named(LibraryElements.JAR))
            }
        }

        project.skieSubPlugins.configureEach {
            it.configureDependencies(project, subPluginConfiguration)
        }
    }

    fun registerSubPlugins(linkTask: KotlinNativeLink) {
        val framework = linkTask.binary as? Framework ?: return

        linkTask.registerSubPluginsOptions(framework)

        linkTask.registerSubPluginsToClasspath()
    }

    private fun KotlinNativeLink.registerSubPluginsOptions(framework: Framework) {
        project.skieSubPlugins.forEach { subPlugin ->
            val options = subPlugin.getOptions(project, framework)

            options.get().forEach {
                compilerPluginOptions.addPluginArgument(subPlugin.compilerPluginId, it)
            }
        }
    }

    private fun KotlinNativeLink.registerSubPluginsToClasspath() {
        compilerPluginClasspath = listOfNotNull(
            compilerPluginClasspath,
            project.configurations.getByName(subPluginConfigurationName),
        ).reduce(FileCollection::plus)
    }
}

private val Project.skieSubPlugins: DomainObjectCollection<SkieSubplugin>
    get() = plugins.withType()
