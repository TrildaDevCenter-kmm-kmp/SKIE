package co.touchlab.skie.configuration

import co.touchlab.skie.phases.SkiePhase
import co.touchlab.skie.phases.runtime.belongsToSkieRuntime
import co.touchlab.skie.swiftmodel.type.KotlinClassSwiftModel
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import kotlin.properties.Delegates

class ConfigurationProvider(
    private val context: SkiePhase.Context,
) {

    private val declarationConfigurationMap = mutableMapOf<DeclarationDescriptor, DeclarationConfiguration>()

    fun <T> getConfiguration(descriptor: DeclarationDescriptor, key: ConfigurationKey<T>): T =
        getConfiguration(descriptor)[key]

    fun inheritConfiguration(from: DeclarationDescriptor, to: DeclarationDescriptor) {
        getConfiguration(to).inheritFrom = from
    }

    fun <T> overrideConfiguration(descriptor: DeclarationDescriptor, key: ConfigurationKey<T>, value: T) {
        getConfiguration(descriptor)[key] = value
    }

    fun getConfiguration(descriptor: DeclarationDescriptor): DeclarationConfiguration =
        declarationConfigurationMap.getOrPut(descriptor) {
            DeclarationConfiguration(
                descriptor,
                context.skieConfiguration,
                this@ConfigurationProvider
            )
        }

    // WIP Move to Kir
    class DeclarationConfiguration(
        private val descriptor: DeclarationDescriptor,
        private val skieConfiguration: SkieConfiguration,
        private val configurationProvider: ConfigurationProvider,
    ) {

        private val cachedValues = mutableMapOf<ConfigurationKey<*>, Any?>()

        var belongsToSkieRuntime: Boolean by Delegates.observable(descriptor.belongsToSkieRuntime) { _, _, _ ->
            cachedValues.clear()
        }

        var inheritFrom: DeclarationDescriptor? by Delegates.observable(null) { _, _, _ ->
            cachedValues.clear()
        }

        @Suppress("UNCHECKED_CAST")
        operator fun <T> get(key: ConfigurationKey<T>): T =
            cachedValues.getOrPut(key) { loadConfiguration(key) } as T

        operator fun <T> set(key: ConfigurationKey<T>, value: T) {
            cachedValues[key] = value
        }

        private fun loadConfiguration(key: ConfigurationKey<*>): Any? =
            inheritFrom?.let { configurationProvider.getConfiguration(it, key) }
                ?: skieConfiguration[DeclarationDescriptorConfigurationTarget(descriptor, belongsToSkieRuntime), key]
    }
}

context(SkiePhase.Context)
fun DeclarationDescriptor.inheritConfiguration(from: DeclarationDescriptor) {
    configurationProvider.inheritConfiguration(from, this)
}

context(SkiePhase.Context)
fun <T> DeclarationDescriptor.overrideConfiguration(key: ConfigurationKey<T>, value: T) {
    configurationProvider.overrideConfiguration(this, key, value)
}

context(SkiePhase.Context)
var DeclarationDescriptor.belongsToSkieRuntime: Boolean
    get() = configuration.belongsToSkieRuntime
    set(value) {
        configuration.belongsToSkieRuntime = value
    }

context(SkiePhase.Context)
val DeclarationDescriptor.configuration: ConfigurationProvider.DeclarationConfiguration
    get() = configurationProvider.getConfiguration(this)

context(SkiePhase.Context)
fun <T> DeclarationDescriptor.getConfiguration(key: ConfigurationKey<T>): T =
    configuration[key]

context(SkiePhase.Context)
val DeclarationDescriptor.canBeUsedWithExperimentalFeatures: Boolean
    get() = configurationProvider.canBeUsedWithExperimentalFeatures(this)

fun ConfigurationProvider.canBeUsedWithExperimentalFeatures(descriptor: DeclarationDescriptor): Boolean =
    getConfiguration(descriptor, ExperimentalFeatures.Enabled)

fun <T> ConfigurationProvider.getConfiguration(swiftModel: KotlinClassSwiftModel, key: ConfigurationKey<T>): T =
    getConfiguration(swiftModel.classDescriptor, key)

context(SkiePhase.Context)
fun <T> KotlinClassSwiftModel.getConfiguration(key: ConfigurationKey<T>): T =
    this.classDescriptor.getConfiguration(key)
