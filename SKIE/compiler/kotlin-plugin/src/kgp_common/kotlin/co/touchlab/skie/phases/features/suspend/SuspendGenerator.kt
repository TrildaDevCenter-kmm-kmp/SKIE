// WIP 3 Try to remove all unnecessary
// @file:Suppress("invisible_reference", "invisible_member")

package co.touchlab.skie.phases.features.suspend

import co.touchlab.skie.configuration.SkieConfigurationFlag
import co.touchlab.skie.configuration.SuspendInterop
import co.touchlab.skie.phases.SkieContext
import co.touchlab.skie.kir.MutableDescriptorProvider
import co.touchlab.skie.kir.allExposedMembers
import co.touchlab.skie.phases.BaseGenerator
import co.touchlab.skie.kir.irbuilder.DeclarationBuilder
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor

internal class SuspendGenerator(
    skieContext: SkieContext,
    private val descriptorProvider: MutableDescriptorProvider,
    private val declarationBuilder: DeclarationBuilder,
) : BaseGenerator(skieContext) {

    override val isActive: Boolean = SkieConfigurationFlag.Feature_CoroutinesInterop in skieConfiguration.enabledConfigurationFlags

    override fun runObjcPhase() {
        val kotlinDelegate = KotlinSuspendGeneratorDelegate(module, declarationBuilder, descriptorProvider)
        val swiftDelegate = SwiftSuspendGeneratorDelegate(module)

        descriptorProvider.allSupportedFunctions.forEach { function ->
            val kotlinBridgingFunction = kotlinDelegate.generateKotlinBridgingFunction(function)

            swiftDelegate.generateSwiftBridgingFunction(function, kotlinBridgingFunction)

            markOriginalFunctionAsReplaced(function)
        }
    }

    private val MutableDescriptorProvider.allSupportedFunctions: List<SimpleFunctionDescriptor>
        get() = this.allExposedMembers.filterIsInstance<SimpleFunctionDescriptor>()
            .filter { this.isBaseMethod(it) }
            .filter { it.isSupported }
            .filter { it.isInteropEnabled }

    private val FunctionDescriptor.isSupported: Boolean
        get() = this.isSuspend

    private val FunctionDescriptor.isInteropEnabled: Boolean
        get() = this.getConfiguration(SuspendInterop.Enabled)

    private fun markOriginalFunctionAsReplaced(originalFunctionDescriptor: SimpleFunctionDescriptor) {
        module.configure {
            originalFunctionDescriptor.swiftModel.visibility = co.touchlab.skie.swiftmodel.SwiftModelVisibility.Replaced
        }
    }
}