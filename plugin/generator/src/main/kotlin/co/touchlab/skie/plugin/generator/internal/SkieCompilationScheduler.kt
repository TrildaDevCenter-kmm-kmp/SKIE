package co.touchlab.skie.plugin.generator.internal

import co.touchlab.skie.plugin.api.SkieContext
import co.touchlab.skie.plugin.generator.internal.analytics.AnalyticsPhase
import co.touchlab.skie.plugin.generator.internal.arguments.DefaultArgumentGenerator
import co.touchlab.skie.plugin.generator.internal.coroutines.flow.FlowBridgingConfigurator
import co.touchlab.skie.plugin.generator.internal.coroutines.flow.FlowConversionConstructorsGenerator
import co.touchlab.skie.plugin.generator.internal.export.ExtraClassExportPhase
import co.touchlab.skie.plugin.generator.internal.coroutines.flow.FlowMappingConfigurator
import co.touchlab.skie.plugin.generator.internal.coroutines.suspend.SuspendGenerator
import co.touchlab.skie.plugin.generator.internal.enums.ExhaustiveEnumsGenerator
import co.touchlab.skie.plugin.generator.internal.runtime.KotlinRuntimeHidingPhase
import co.touchlab.skie.plugin.generator.internal.runtime.SwiftRuntimeGenerator
import co.touchlab.skie.plugin.generator.internal.sealed.SealedInteropGenerator
import co.touchlab.skie.plugin.generator.internal.`typealias`.TypeAliasGenerator
import co.touchlab.skie.plugin.generator.internal.util.NamespaceProvider
import co.touchlab.skie.plugin.generator.internal.util.NativeMutableDescriptorProvider
import co.touchlab.skie.plugin.generator.internal.util.Reporter
import co.touchlab.skie.plugin.generator.internal.util.irbuilder.DeclarationBuilder
import co.touchlab.skie.plugin.generator.internal.util.irbuilder.impl.DeclarationBuilderImpl
import co.touchlab.skie.plugin.generator.internal.util.irbuilder.impl.GenerateIrPhase
import co.touchlab.skie.plugin.generator.internal.validation.IrValidator
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

internal class SkieCompilationScheduler(
    context: CommonBackendContext,
    private val skieContext: SkieContext,
    descriptorProvider: NativeMutableDescriptorProvider,
    declarationBuilder: DeclarationBuilderImpl,
    namespaceProvider: NamespaceProvider,
    reporter: Reporter,
) {

    private val compilationPhases = listOf(
        AnalyticsPhase(
            context = context,
            skieContext = skieContext,
            descriptorProvider = descriptorProvider,
        ),
        ExtraClassExportPhase(
            skieContext = skieContext,
            descriptorProvider = descriptorProvider,
            declarationBuilder = declarationBuilder,
        ),
        GenerateIrPhase(
            declarationBuilder = declarationBuilder,
        ),
        IrValidator(
            skieContext = skieContext,
            reporter = reporter,
            descriptorProvider = descriptorProvider,
        ),
        KotlinRuntimeHidingPhase(
            skieContext = skieContext,
            descriptorProvider = descriptorProvider,
        ),
        FlowBridgingConfigurator(
            skieContext = skieContext,
        ),
        FlowConversionConstructorsGenerator(
            skieContext = skieContext,
        ),
        FlowMappingConfigurator(
            skieContext = skieContext,
        ),
        SwiftRuntimeGenerator(
            skieContext = skieContext,
        ),
        SealedInteropGenerator(
            skieContext = skieContext,
            namespaceProvider = namespaceProvider,
        ),
        DefaultArgumentGenerator(
            skieContext = skieContext,
            descriptorProvider = descriptorProvider,
            declarationBuilder = declarationBuilder,
        ),
        ExhaustiveEnumsGenerator(
            skieContext = skieContext,
            namespaceProvider = namespaceProvider,
            reporter = reporter,
        ),
        SuspendGenerator(
            skieContext = skieContext,
            namespaceProvider = namespaceProvider,
            descriptorProvider = descriptorProvider,
            declarationBuilder = declarationBuilder,
        ),
        TypeAliasGenerator(
            skieContext = skieContext,
            descriptorProvider = descriptorProvider,
        ),
    )

    fun runClassExportingPhases() {
        compilationPhases
            .filter { it.isActive }
            .forEach {
                skieContext.skiePerformanceAnalyticsProducer.log("Class Exporting Phase: ${it::class.simpleName}") {
                    it.runClassExportingPhase()
                }
            }
    }

    fun runObjcPhases() {
        compilationPhases
            .filter { it.isActive }
            .forEach {
                skieContext.skiePerformanceAnalyticsProducer.log("ObjC Phase: ${it::class.simpleName}") {
                    it.runObjcPhase()
                }
            }
    }

    fun runIrPhases(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext, allModules: Map<String, IrModuleFragment>) {
        compilationPhases
            .filter { it.isActive }
            .forEach {
                skieContext.skiePerformanceAnalyticsProducer.log("IR Phase: ${it::class.simpleName}") {
                    it.runIrPhase(moduleFragment, pluginContext, allModules)
                }
            }
    }
}
