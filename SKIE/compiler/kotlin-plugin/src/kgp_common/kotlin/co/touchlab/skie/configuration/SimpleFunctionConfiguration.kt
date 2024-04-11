package co.touchlab.skie.configuration

import co.touchlab.skie.phases.SkiePhase

class SimpleFunctionConfiguration(
    parent: FileOrClassConfiguration,
) : FunctionConfiguration(parent) {

    operator fun <KEY, VALUE> get(configurationKey: KEY): VALUE where KEY : ConfigurationKey<VALUE>, KEY : ConfigurationScope.SimpleFunction =
        getUnsafe(configurationKey)

    operator fun <KEY, VALUE> set(configurationKey: KEY, value: VALUE) where KEY : ConfigurationKey<VALUE>, KEY : ConfigurationScope.SimpleFunction {
        setUnsafe(configurationKey, value)
    }

    fun <KEY, VALUE> reset(configurationKey: KEY) where KEY : ConfigurationKey<VALUE>, KEY : ConfigurationScope.SimpleFunction {
        resetUnsafe(configurationKey)
    }
}

context(SkiePhase.Context)
val SimpleFunctionConfiguration.isSuspendInteropEnabled: Boolean
    get() = SkieConfigurationFlag.Feature_CoroutinesInterop.isEnabled && this[SuspendInterop.Enabled]
