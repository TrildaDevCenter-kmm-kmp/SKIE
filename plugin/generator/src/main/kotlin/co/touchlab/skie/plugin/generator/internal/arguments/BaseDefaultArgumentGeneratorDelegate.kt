package co.touchlab.skie.plugin.generator.internal.arguments

import co.touchlab.skie.configuration.Configuration
import co.touchlab.skie.configuration.ConfigurationKeys.DefaultArgumentInterop
import co.touchlab.skie.plugin.api.SkieContext
import co.touchlab.skie.plugin.generator.internal.configuration.ConfigurationContainer
import co.touchlab.skie.plugin.generator.internal.util.Generator
import co.touchlab.skie.plugin.generator.internal.util.irbuilder.DeclarationBuilder
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue

internal abstract class BaseDefaultArgumentGeneratorDelegate(
    protected val skieContext: SkieContext,
    protected val declarationBuilder: DeclarationBuilder,
    override val configuration: Configuration,
) : Generator, ConfigurationContainer {

    protected val FunctionDescriptor.hasDefaultArguments: Boolean
        get() = this.valueParameters.any { it.declaresDefaultValue() }

    protected val FunctionDescriptor.isInteropEnabled: Boolean
        get() = this.getConfiguration(DefaultArgumentInterop.Enabled) && this.satisfiesMaximumDefaultArgumentCount

    private val FunctionDescriptor.satisfiesMaximumDefaultArgumentCount: Boolean
        get() = this.defaultArgumentCount <= this.getConfiguration(DefaultArgumentInterop.MaximumDefaultArgumentCount)

    private val FunctionDescriptor.defaultArgumentCount: Int
        get() = this.valueParameters.count { it.declaresDefaultValue() }

    protected fun FunctionDescriptor.forEachDefaultArgumentOverload(
        action: (index: Int, overloadParameters: List<ValueParameterDescriptor>) -> Unit,
    ) {
        val parametersWithDefaultValues = this.valueParameters.filter { it.hasDefaultValue() }

        parametersWithDefaultValues.forEachSubsetIndexed { index, omittedParameters ->
            if (omittedParameters.isNotEmpty()) {
                @Suppress("ConvertArgumentToSet")
                val overloadParameters = this.valueParameters - omittedParameters

                action(index, overloadParameters)
            }
        }
    }

    private fun <T> Iterable<T>.forEachSubsetIndexed(action: (Int, List<T>) -> Unit) {
        var bitmap = 0
        do {
            val subset = this.dropIndices(bitmap)

            action(bitmap, subset)

            bitmap++
        } while (subset.isNotEmpty())
    }

    private fun <T> Iterable<T>.dropIndices(bitmap: Int): List<T> =
        this.filterIndexed { index, _ ->
            !bitmap.testBit(index)
        }

    private fun Int.testBit(n: Int): Boolean =
        (this shr n) and 1 == 1

    protected fun List<ValueParameterDescriptor>.copyWithoutDefaultValue(newOwner: CallableDescriptor): List<ValueParameterDescriptor> =
        this.mapIndexed { index, valueParameter -> valueParameter.copyWithoutDefaultValue(newOwner, index) }

    private fun ValueParameterDescriptor.copyWithoutDefaultValue(
        newOwner: CallableDescriptor,
        newIndex: Int,
    ): ValueParameterDescriptor = ValueParameterDescriptorImpl(
        containingDeclaration = newOwner,
        original = null,
        index = newIndex,
        annotations = Annotations.EMPTY,
        name = this.name,
        outType = this.type,
        declaresDefaultValue = false,
        isCrossinline = this.isCrossinline,
        isNoinline = this.isNoinline,
        varargElementType = this.varargElementType,
        source = SourceElement.NO_SOURCE,
    )

    context(IrBuilderWithScope) protected fun IrFunctionAccessExpression.passArgumentsWithMatchingNames(from: IrFunction) {
        from.valueParameters.forEach { valueParameter: IrValueParameter ->
            val indexInCalledFunction = this.symbol.owner.indexOfValueParameterByName(valueParameter.name)

            putValueArgument(indexInCalledFunction, irGet(valueParameter))
        }
    }

    private fun IrFunction.indexOfValueParameterByName(name: Name): Int =
        this.valueParameters.indexOfFirst { it.name == name }
}