@file:Suppress("invisible_reference", "invisible_member")

package co.touchlab.skie.plugin.generator.internal.coroutines.suspend

import co.touchlab.skie.configuration.Configuration
import co.touchlab.skie.configuration.features.SkieFeature
import co.touchlab.skie.configuration.gradle.SuspendInterop
import co.touchlab.skie.plugin.api.SkieContext
import co.touchlab.skie.plugin.api.kotlin.allExposedMembers
import co.touchlab.skie.plugin.api.model.SwiftModelVisibility
import co.touchlab.skie.plugin.generator.internal.runtime.belongsToSkieRuntime
import co.touchlab.skie.plugin.generator.internal.util.BaseGenerator
import co.touchlab.skie.plugin.generator.internal.util.NamespaceProvider
import co.touchlab.skie.plugin.generator.internal.util.NativeDescriptorProvider
import co.touchlab.skie.plugin.generator.internal.util.irbuilder.DeclarationBuilder
import org.jetbrains.kotlin.backend.konan.objcexport.isBaseMethod
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor

internal class SuspendGenerator(
    skieContext: SkieContext,
    namespaceProvider: NamespaceProvider,
    configuration: Configuration,
    private val descriptorProvider: NativeDescriptorProvider,
    private val declarationBuilder: DeclarationBuilder,
) : BaseGenerator(skieContext, namespaceProvider, configuration) {

    override val isActive: Boolean = SkieFeature.SuspendInterop in configuration.enabledFeatures &&
        SkieFeature.SwiftRuntime in configuration.enabledFeatures

    override fun execute() {
        val kotlinDelegate = KotlinSuspendGeneratorDelegate(module, declarationBuilder, descriptorProvider)
        val swiftDelegate = SwiftSuspendGeneratorDelegate(module)

        descriptorProvider.allSupportedFunctions.forEach { function ->
            val kotlinBridgingFunction = kotlinDelegate.generateKotlinBridgingFunction(function)

            swiftDelegate.generateSwiftBridgingFunction(function, kotlinBridgingFunction)

            markOriginalFunctionAsReplaced(function)
        }
    }

    private val NativeDescriptorProvider.allSupportedFunctions: List<SimpleFunctionDescriptor>
        get() = this.allExposedMembers.filterIsInstance<SimpleFunctionDescriptor>()
            .filter { this.mapper.isBaseMethod(it) }
            .filter { it.isSupported }
            .filter { it.isInteropEnabled }

    private val FunctionDescriptor.isSupported: Boolean
        get() = this.isSuspend && !this.belongsToSkieRuntime

    private val FunctionDescriptor.isInteropEnabled: Boolean
        get() = this.getConfiguration(SuspendInterop.Enabled)

    private fun markOriginalFunctionAsReplaced(originalFunctionDescriptor: SimpleFunctionDescriptor) {
        module.configure {
            originalFunctionDescriptor.swiftModel.visibility = SwiftModelVisibility.Replaced
        }
    }
}

