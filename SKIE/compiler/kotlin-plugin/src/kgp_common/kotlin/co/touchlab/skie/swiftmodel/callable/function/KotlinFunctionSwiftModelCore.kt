@file:Suppress("invisible_reference", "invisible_member")

package co.touchlab.skie.swiftmodel.callable.function

import co.touchlab.skie.compilerinject.reflection.reflectors.mapper
import co.touchlab.skie.swiftmodel.DescriptorBridgeProvider
import co.touchlab.skie.swiftmodel.SwiftModelVisibility
import co.touchlab.skie.swiftmodel.callable.identifierAfterVisibilityChanges
import co.touchlab.skie.swiftmodel.callable.parameter.KotlinParameterSwiftModelCore
import co.touchlab.skie.swiftmodel.factory.ObjCTypeProvider
import co.touchlab.skie.swiftmodel.type.FlowMappingStrategy
import co.touchlab.skie.swiftmodel.type.bridge.MethodBridge
import co.touchlab.skie.swiftmodel.type.bridge.MethodBridgeParameter
import co.touchlab.skie.swiftmodel.type.bridge.valueParametersAssociated
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamer
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCType
import org.jetbrains.kotlin.backend.konan.objcexport.doesThrow
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor

class KotlinFunctionSwiftModelCore(
    val descriptor: FunctionDescriptor,
    private val namer: ObjCExportNamer,
    private val bridgeProvider: DescriptorBridgeProvider,
    private val objCTypeProvider: ObjCTypeProvider,
) {

    fun getMethodBridge(functionDescriptor: FunctionDescriptor): MethodBridge =
        if (functionDescriptor is ConstructorDescriptor) {
            bridgeProvider.bridgeMethod(functionDescriptor)
        } else {
            bridgeProvider.bridgeMethod(descriptor)
        }

    val swiftFunctionName: SwiftFunctionName = run {
        val swiftName = namer.getSwiftName(descriptor.original)

        val (identifier, argumentLabelsString) = swiftNameComponentsRegex.matchEntire(swiftName)?.destructured
            ?: error("Unable to parse swift name: $swiftName")

        val argumentLabels = argumentLabelsString.split(":").map { it.trim() }.filter { it.isNotEmpty() }

        SwiftFunctionName(identifier, argumentLabels)
    }

    var identifier: String = swiftFunctionName.identifier

    var visibility: SwiftModelVisibility = SwiftModelVisibility.Visible

    fun reference(swiftModel: KotlinFunctionSwiftModel): String =
        if (swiftModel.valueParameters.isEmpty()) {
            swiftModel.identifierAfterVisibilityChanges
        } else {
            "${swiftModel.identifierAfterVisibilityChanges}(${swiftModel.valueParameters.joinToString("") { "${it.argumentLabel}:" }})"
        }

    fun name(swiftModel: KotlinFunctionSwiftModel): String =
        if (swiftModel.valueParameters.isEmpty()) "${swiftModel.identifierAfterVisibilityChanges}()" else reference(swiftModel)

    fun getParameterCoresWithDescriptors(
        functionDescriptor: FunctionDescriptor,
    ): List<Pair<KotlinParameterSwiftModelCore, ParameterDescriptor?>> =
        getMethodBridge(functionDescriptor)
            .valueParametersAssociated(functionDescriptor)
            .filterNot { it.first is MethodBridgeParameter.ValueParameter.ErrorOutParameter }
            .zip(swiftFunctionName.argumentLabels)
            .map { (parameterBridgeWithDescriptor, argumentLabel) ->
                KotlinParameterSwiftModelCore(
                    argumentLabel = argumentLabel,
                    parameterBridge = parameterBridgeWithDescriptor.first,
                    baseParameterDescriptor = parameterBridgeWithDescriptor.second,
                    allArgumentLabels = swiftFunctionName.argumentLabels,
                    getObjCType = { functionDescriptor, parameterDescriptor, flowMappingStrategy ->
                        objCTypeProvider.getFunctionParameterType(
                            function = functionDescriptor,
                            parameter = parameterDescriptor,
                            bridge = parameterBridgeWithDescriptor.first,
                            flowMappingStrategy = flowMappingStrategy,
                        )
                    },
                ) to parameterBridgeWithDescriptor.second
            }

    val objCSelector: String = namer.getSelector(descriptor.original)

    val isThrowing: Boolean = namer.mapper.doesThrow(descriptor)

    fun getObjCReturnType(functionDescriptor: FunctionDescriptor, flowMappingStrategy: FlowMappingStrategy): ObjCType? =
        if (descriptor !is ConstructorDescriptor) {
            objCTypeProvider.getFunctionReturnType(descriptor, functionDescriptor, flowMappingStrategy)
        } else {
            null
        }

    data class SwiftFunctionName(val identifier: String, val argumentLabels: List<String>)

    private companion object {

        val swiftNameComponentsRegex = "(.+?)\\((.*?)\\)".toRegex()
    }
}
