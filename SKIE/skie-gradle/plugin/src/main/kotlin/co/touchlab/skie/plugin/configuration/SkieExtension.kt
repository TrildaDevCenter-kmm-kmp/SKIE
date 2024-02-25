package co.touchlab.skie.plugin.configuration

import co.touchlab.skie.configuration.SkieConfigurationFlag
import co.touchlab.skie.plugin.configuration.util.GradleSkieConfiguration
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import javax.inject.Inject

open class SkieExtension @Inject constructor(objects: ObjectFactory) {

    /**
     * Disables SKIE plugin. Useful for checking if an error is a bug in SKIE, or in the Kotlin compiler.
     */
    val isEnabled: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

    val additionalConfigurationFlags: SetProperty<SkieConfigurationFlag> =
        objects.setProperty(SkieConfigurationFlag::class.java).convention(emptySet())

    val suppressedConfigurationFlags: SetProperty<SkieConfigurationFlag> =
        objects.setProperty(SkieConfigurationFlag::class.java).convention(emptySet())

    val analytics: SkieAnalyticsConfiguration = objects.newInstance(SkieAnalyticsConfiguration::class.java)

    fun analytics(action: Action<in SkieAnalyticsConfiguration>) {
        action.execute(analytics)
    }

    val build: SkieBuildConfiguration = objects.newInstance(SkieBuildConfiguration::class.java)

    fun build(action: Action<in SkieBuildConfiguration>) {
        action.execute(build)
    }

    val debug: SkieDebugConfiguration = objects.newInstance(SkieDebugConfiguration::class.java)

    fun debug(action: Action<in SkieDebugConfiguration>) {
        action.execute(debug)
    }

    val features: SkieFeatureConfiguration = objects.newInstance(SkieFeatureConfiguration::class.java)

    fun features(action: Action<SkieFeatureConfiguration>) {
        action.execute(features)
    }

    val migration: SkieMigrationConfiguration = objects.newInstance(SkieMigrationConfiguration::class.java)

    fun migration(action: Action<in SkieMigrationConfiguration>) {
        action.execute(migration)
    }

    // Putting these extensions here so that they don't pollute the extension's namespace. They can be used by wrapping in `with(SkieExtension) { ... }`
    companion object {

        fun Project.createExtension(): SkieExtension =
            project.extensions.create("skie", SkieExtension::class.java)

        fun SkieExtension.buildConfiguration(): GradleSkieConfiguration =
            GradleSkieConfiguration(
                enabledConfigurationFlags = (mergeConfigurationSetsFromConfigurations() + additionalConfigurationFlags.get()) - suppressedConfigurationFlags.get(),
                groups = features.buildGroups(),
            )

        private fun SkieExtension.mergeConfigurationSetsFromConfigurations() =
            analytics.buildConfigurationFlags() +
                    build.buildConfigurationFlags() +
                    debug.buildConfigurationFlags() +
                    features.buildConfigurationFlags() +
                    migration.buildConfigurationFlags()
    }
}
