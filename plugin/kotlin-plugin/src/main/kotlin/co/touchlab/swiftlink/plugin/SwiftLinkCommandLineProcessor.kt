package co.touchlab.swiftlink.plugin

import co.touchlab.swiftpack.spec.module.SwiftPackModule
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import java.io.File

class SwiftLinkCommandLineProcessor: CommandLineProcessor {
    companion object {
        const val pluginId = "co.touchlab.skie"
    }

    override val pluginId: String = SwiftLinkCommandLineProcessor.pluginId

    private val options = listOf(
        Options.swiftPackModule,
        Options.swiftSourceFile,
        Options.expandedSwiftDir,
        Options.linkPhaseSwiftPackOutputDir,
        Options.disableWildcardExport,
    )
    private val optionsMap = options.associateBy { it.optionName }
    override val pluginOptions: Collection<AbstractCliOption> = options.map { it.toCliOption() }

    object Options {
        val swiftPackModule = PluginOption(
            optionName = "swiftPackModule",
            valueDescription = "<namespace>${File.pathSeparator}<absolute path>",
            description = "",
            allowMultipleOccurrences = true,
            serialize = { (namespace, moduleFile) ->
                "$namespace${File.pathSeparator}${moduleFile.absolutePath}"
            },
            deserialize = { value ->
                val (namespace, absolutePath) = value.split(File.pathSeparator)
                SwiftPackModule.Reference(namespace, File(absolutePath))
            },
        )

        val linkPhaseSwiftPackOutputDir = PluginOption(
            optionName = "linkPhaseSwiftPackOutputDir",
            valueDescription = "<absolute path>",
            description = "",
            serialize = File::getAbsolutePath,
            deserialize = ::File,
        )

        val swiftSourceFile = PluginOption(
            optionName = "swiftSourceFile",
            valueDescription = "<absolute path>",
            description = "",
            allowMultipleOccurrences = true,
            serialize = File::getAbsolutePath,
            deserialize = ::File,
        )

        val expandedSwiftDir = PluginOption(
            optionName = "expandedSwiftDir",
            valueDescription = "<absolute path>",
            description = "",
            isRequired = true,
            serialize = File::getAbsolutePath,
            deserialize = ::File,
        )

        val disableWildcardExport = PluginOption(
            optionName = "disableWildcardExport",
            valueDescription = "<true|false>",
            description = "",
            serialize = Boolean::toString,
            deserialize = String::toBooleanStrict,
        )
    }

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        super.processOption(option, value, configuration)

        when (optionsMap[option.optionName]) {
            Options.swiftPackModule -> {
                configuration.add(ConfigurationKeys.swiftPackModules, Options.swiftPackModule.deserialize(value))
            }
            Options.swiftSourceFile -> {
                configuration.add(ConfigurationKeys.swiftSourceFiles, Options.swiftSourceFile.deserialize(value))
            }
            Options.expandedSwiftDir -> {
                configuration.putIfNotNull(ConfigurationKeys.expandedSwiftDir, Options.expandedSwiftDir.deserialize(value))
            }
            Options.linkPhaseSwiftPackOutputDir -> {
                configuration.putIfNotNull(ConfigurationKeys.linkPhaseSwiftPackOutputDir, Options.linkPhaseSwiftPackOutputDir.deserialize(value))
            }
            Options.disableWildcardExport -> {
                configuration.putIfNotNull(ConfigurationKeys.disableWildcardExport, Options.disableWildcardExport.deserialize(value))
            }
        }
    }
}
