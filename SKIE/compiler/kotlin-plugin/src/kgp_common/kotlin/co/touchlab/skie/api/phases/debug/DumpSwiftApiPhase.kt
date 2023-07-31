package co.touchlab.skie.api.phases.debug

import co.touchlab.skie.api.phases.SkieLinkingPhase
import co.touchlab.skie.configuration.SkieFeature
import co.touchlab.skie.plugin.api.SkieContext
import co.touchlab.skie.plugin.api.configuration.SkieConfiguration
import co.touchlab.skie.plugin.api.skieBuildDirectory
import co.touchlab.skie.plugin.api.skieContext
import co.touchlab.skie.plugin.api.util.FrameworkLayout
import co.touchlab.skie.util.Command
import org.jetbrains.kotlin.backend.common.CommonBackendContext

sealed class DumpSwiftApiPhase(
    private val skieContext: SkieContext,
    private val framework: FrameworkLayout,
) : SkieLinkingPhase {

    class BeforeApiNotes(
        skieConfiguration: SkieConfiguration,
        skieContext: SkieContext,
        framework: FrameworkLayout,
    ) : DumpSwiftApiPhase(skieContext, framework) {

        override val isActive: Boolean = SkieFeature.Debug_DumpSwiftApiBeforeApiNotes in skieConfiguration.enabledFeatures
    }

    class AfterApiNotes(
        skieConfiguration: SkieConfiguration,
        skieContext: SkieContext,
        framework: FrameworkLayout,
    ) : DumpSwiftApiPhase(skieContext, framework) {

        override val isActive: Boolean = SkieFeature.Debug_DumpSwiftApiAfterApiNotes in skieConfiguration.enabledFeatures
    }

    override fun execute() {
        val moduleName = framework.moduleName
        val apiFileBaseName = "${moduleName}_${this::class.simpleName}"
        val apiFile = skieContext.skieBuildDirectory.debug.dumps.apiFile(apiFileBaseName)
        val logFile = skieContext.skieBuildDirectory.debug.logs.apiFile(apiFileBaseName)

        Command(
            "zsh",
            "-c",
            """echo "import Kotlin\n:type lookup $moduleName" | swift repl -F "${framework.framework.parentFile.absolutePath}" > "${apiFile.absolutePath}"""",
        ).execute(handleError = false, logFile = logFile)
    }
}
