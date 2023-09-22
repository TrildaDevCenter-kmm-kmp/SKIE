package co.touchlab.skie.compilerinject.plugin

import co.touchlab.skie.util.plugin.PluginOption
import org.jetbrains.kotlin.compiler.plugin.CliOption

internal fun PluginOption<*>.toCliOption() = CliOption(
    optionName,
    valueDescription,
    description,
    isRequired,
    allowMultipleOccurrences,
)
