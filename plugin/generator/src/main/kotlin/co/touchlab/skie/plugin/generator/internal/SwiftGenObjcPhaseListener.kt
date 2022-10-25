package co.touchlab.skie.plugin.generator.internal

import co.touchlab.skie.configuration.Configuration
import co.touchlab.skie.plugin.api.skieContext
import co.touchlab.skie.plugin.generator.ConfigurationKeys
import co.touchlab.skie.plugin.generator.internal.util.DescriptorProvider
import co.touchlab.skie.plugin.generator.internal.util.NamespaceProvider
import co.touchlab.skie.plugin.generator.internal.util.Reporter
import co.touchlab.skie.plugin.generator.internal.util.irbuilder.DeclarationBuilder
import co.touchlab.skie.plugin.generator.internal.util.irbuilder.impl.DeclarationBuilderImpl
import co.touchlab.skie.plugin.intercept.PhaseListener
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.PhaserState

internal class SwiftGenObjcPhaseListener : PhaseListener {

    override val phase: PhaseListener.Phase = PhaseListener.Phase.OBJC_EXPORT

    override fun beforePhase(phaseConfig: PhaseConfig, phaserState: PhaserState<Unit>, context: CommonBackendContext) {
        super.beforePhase(phaseConfig, phaserState, context)

        buildIr(context) { declarationBuilder ->
            val swiftGenScheduler = SwiftGenScheduler(
                skieContext = context.skieContext,
                declarationBuilder = declarationBuilder,
                namespaceProvider = NamespaceProvider(context.skieContext.module),
                configuration = context.pluginConfiguration,
                reporter = Reporter(context.configuration),
            )

            val descriptorProvider = DescriptorProvider(context)

            swiftGenScheduler.process(descriptorProvider)
        }
    }

    private val CommonBackendContext.pluginConfiguration: Configuration
        get() = configuration.get(ConfigurationKeys.swiftGenConfiguration, Configuration {})

    private fun buildIr(context: CommonBackendContext, action: (DeclarationBuilder) -> Unit) {
        val declarationBuilder = DeclarationBuilderImpl(context)

        SwiftGenCompilerConfigurationKey.DeclarationBuilder.put(declarationBuilder, context.configuration)

        action(declarationBuilder)
    }
}