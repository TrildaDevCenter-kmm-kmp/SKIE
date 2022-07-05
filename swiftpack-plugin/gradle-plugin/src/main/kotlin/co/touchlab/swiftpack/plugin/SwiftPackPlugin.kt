package co.touchlab.swiftpack.plugin

import co.touchlab.swiftpack.plugin.SwiftPack.mainCompilation
import co.touchlab.swiftpack.plugin.SwiftPack.swiftTemplateDirectory
import org.gradle.api.Named
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Zip
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import javax.inject.Inject

class SwiftPackPlugin @Inject constructor(
    private val softwareComponentFactory: SoftwareComponentFactory,
): Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        apply<SpecConfigGradleSubplugin>()

        afterEvaluate {
            the<KotlinMultiplatformExtension>().apply {
                val nativeAppleTargets = targets.mapNotNull { it as? KotlinNativeTarget }.filter { it.konanTarget.family.isAppleFamily }
                nativeAppleTargets.forEach { target ->
                    target.registerOutgoing()
                }

                nativeAppleTargets
                    .flatMap { it.binaries }
                    .mapNotNull { it as? Framework }
                    .forEach { framework ->
                        framework.registerIncoming()
                    }
            }
        }
    }

    private fun KotlinNativeTarget.registerOutgoing() = with(project) {
        val capitalizedTargetName = targetName.capitalized()
        val packSwiftTemplates = tasks.create<Zip>("swiftPackModules$capitalizedTargetName") {
            group = "swiftpack"
            archiveAppendix.set("swiftTemplates$capitalizedTargetName")
            from(swiftTemplateDirectory(this@registerOutgoing))
            dependsOn(mainCompilation.compileKotlinTaskProvider)
        }

        val outgoingConfiguration = configurations.register("swiftPack$capitalizedTargetName") { configuration ->
            configuration.isCanBeConsumed = true
            configuration.isCanBeResolved = false

            configuration.attributes {
                it.attribute(Category.CATEGORY_ATTRIBUTE, namedAttribute(Category.LIBRARY))
                it.attribute(Usage.USAGE_ATTRIBUTE, namedAttribute(Usage.SWIFT_API))
                it.attribute(Bundling.BUNDLING_ATTRIBUTE, namedAttribute(Bundling.EXTERNAL))
                it.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, namedAttribute("swiftPack$capitalizedTargetName"))
            }

            configuration.outgoing.artifact(packSwiftTemplates.archiveFile) {
                it.builtBy(packSwiftTemplates)
            }
        }

        val adhocComponent = softwareComponentFactory.adhoc("swiftPack${capitalizedTargetName}Component")
        components.add(adhocComponent)

        adhocComponent.addVariantsFromConfiguration(outgoingConfiguration.get()) {
            it.mapToOptional()
        }

        plugins.withType<MavenPublishPlugin>() {
            the<PublishingExtension>().apply {
                publications {
                    it.create<MavenPublication>("swiftPack$capitalizedTargetName") {
                        from(adhocComponent)
                        artifactId += "-${targetName.lowercase()}-swiftpack"
                    }
                }
            }
        }
    }

    private fun Framework.registerIncoming() = with(project) {
        val capitalizedTargetName = target.targetName.capitalized()
        val configurationName = "exportSwiftPack$capitalizedTargetName"

        if (configurations.findByName(configurationName) != null) { return@with }
        val exportConfiguration = configurations.register(configurationName) { configuration ->
            configuration.isCanBeConsumed = false
            configuration.isCanBeResolved = true

            configuration.extendsFrom(configurations.getByName(compilation.apiConfigurationName))
            configuration.attributes {
                it.attribute(Category.CATEGORY_ATTRIBUTE, namedAttribute(Category.LIBRARY))
                it.attribute(Usage.USAGE_ATTRIBUTE, namedAttribute(Usage.SWIFT_API))
                it.attribute(Bundling.BUNDLING_ATTRIBUTE, namedAttribute(Bundling.EXTERNAL))
                it.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, namedAttribute("swiftPack$capitalizedTargetName"))
            }

            configuration.resolutionStrategy { strategy ->
                strategy.eachDependency { dependency ->
                    dependency.useTarget("${dependency.requested.module}-${target.targetName.lowercase()}-swiftpack:${dependency.requested.version}")
                }
            }

            configuration.incoming.beforeResolve {
                println("Resolving $configurationName")
            }
        }

        tasks.register<Sync>("unpackSwiftPack$capitalizedTargetName") {
            group = "swiftpack"

            val frameworkExportConfiguration = configurations.getByName(exportConfigurationName)
            val artifacts = exportConfiguration.get().resolvedConfiguration.lenientConfiguration
                .getArtifacts { dependency ->
                    frameworkExportConfiguration.allDependencies.any { it.group == dependency.group && it.name == dependency.name }
                }
            dependsOn(artifacts)
            artifacts.forEach { artifact ->
                from(zipTree(artifact.file)) { spec ->
                    spec.rename { fileName ->
                        "${artifact.name}_$fileName"
                    }
                }
            }
            into(layout.buildDirectory.dir("swiftpack/${compilation.defaultSourceSetName}"))
        }
    }

    private inline fun <reified T: Named> Project.namedAttribute(value: String) = objects.named(T::class, value)
}
