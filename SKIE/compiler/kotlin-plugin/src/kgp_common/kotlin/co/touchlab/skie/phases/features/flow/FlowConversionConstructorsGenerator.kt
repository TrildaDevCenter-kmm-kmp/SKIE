package co.touchlab.skie.phases.features.flow

import co.touchlab.skie.configuration.SkieConfigurationFlag
import co.touchlab.skie.phases.SkieContext
import co.touchlab.skie.swiftmodel.SwiftModelScope
import co.touchlab.skie.phases.features.enums.ObjCBridgeable
import co.touchlab.skie.phases.SkieCompilationPhase
import io.outfoxx.swiftpoet.ANY_OBJECT
import io.outfoxx.swiftpoet.ExtensionSpec
import io.outfoxx.swiftpoet.FileSpec
import io.outfoxx.swiftpoet.FunctionSpec
import io.outfoxx.swiftpoet.Modifier
import io.outfoxx.swiftpoet.Taggable
import io.outfoxx.swiftpoet.TypeName
import io.outfoxx.swiftpoet.TypeVariableName
import io.outfoxx.swiftpoet.parameterizedBy

internal class FlowConversionConstructorsGenerator(
    private val skieContext: SkieContext,
) : SkieCompilationPhase {

    override val isActive: Boolean =
        SkieConfigurationFlag.Feature_CoroutinesInterop in skieContext.skieConfiguration.enabledConfigurationFlags

    override fun runObjcPhase() {
        skieContext.module.file("FlowConversions") {
            SupportedFlow.values().forEach {
                it.generateAllConversions()
            }
        }
    }
}

context (FileSpec.Builder, SwiftModelScope)
private fun SupportedFlow.generateAllConversions() {
    requiredVariant.generateAllConversions()
    optionalVariant.generateAllConversions()
}

context (FileSpec.Builder, SwiftModelScope)
private fun SupportedFlow.Variant.generateAllConversions() {
    generateAllKotlinClassConversions(this)
    generateAllSwiftClassConversions(this)
}

context (SwiftModelScope)
private fun FileSpec.Builder.generateAllKotlinClassConversions(variant: SupportedFlow.Variant) {
    generateKotlinClassWithAnyObjectConversions(variant)
    generateKotlinClassWithBridgeableConversions(variant)
}

context (SwiftModelScope)
private fun FileSpec.Builder.generateAllSwiftClassConversions(variant: SupportedFlow.Variant) {
    generateSwiftClassWithAnyObjectConversions(variant)
    generateSwiftClassWithBridgeableConversions(variant)
}

context (SwiftModelScope)
private fun FileSpec.Builder.generateKotlinClassWithAnyObjectConversions(variant: SupportedFlow.Variant) {
    addConversions(variant) { from ->
        addSwiftToKotlinConversion(from, variant, ANY_OBJECT, TypeVariableName("T"))
    }
}

context (SwiftModelScope)
private fun FileSpec.Builder.generateKotlinClassWithBridgeableConversions(variant: SupportedFlow.Variant) {
    addConversions(variant) { from ->
        addSwiftToKotlinConversion(
            from,
            variant,
            sirBuiltins.Swift._ObjectiveCBridgeable.defaultType.toSwiftPoetUsage(),
            TypeVariableName("T.${ObjCBridgeable.bridgedObjCTypeAlias}"),
        )
    }
}

context (SwiftModelScope)
private fun FileSpec.Builder.generateSwiftClassWithAnyObjectConversions(variant: SupportedFlow.Variant) {
    generateSwiftClassConversions(variant, ANY_OBJECT, TypeVariableName("T"))
}

context (SwiftModelScope)
private fun FileSpec.Builder.generateSwiftClassWithBridgeableConversions(variant: SupportedFlow.Variant) {
    generateSwiftClassConversions(
        variant,
        sirBuiltins.Swift._ObjectiveCBridgeable.defaultType.toSwiftPoetUsage(),
        TypeVariableName("T.${ObjCBridgeable.bridgedObjCTypeAlias}"),
    )
}

context (SwiftModelScope)
private fun FileSpec.Builder.generateSwiftClassConversions(
    variant: SupportedFlow.Variant,
    typeBound: TypeName,
    flowTypeParameter: TypeName,
) {
    addExtension(
        ExtensionSpec.builder(variant.swiftFlowClass().internalName.toSwiftPoetName())
            .addConditionalConstraint(TypeVariableName.typeVariable("T", TypeVariableName.bound(typeBound)))
            .addModifiers(Modifier.PUBLIC)
            .addConversions(variant) { from -> addKotlinToSwiftConversion(from, flowTypeParameter) }
            .addConversions(variant) { from -> addSwiftToSwiftConversion(from, flowTypeParameter) }
            .build()
    )
}

context (SwiftModelScope)
private fun <T : Taggable.Builder<T>> T.addConversions(
    variant: SupportedFlow.Variant,
    conversionBuilder: context (SwiftModelScope) T.(from: SupportedFlow.Variant) -> Unit,
): T =
    apply {
        variant.forEachChildVariant {
            conversionBuilder(this@SwiftModelScope, this@addConversions, it)
        }
    }

private inline fun SupportedFlow.Variant.forEachChildVariant(action: (SupportedFlow.Variant) -> Unit) {
    SupportedFlow.values()
        .flatMap { it.variants }
        .filter { it.isCastableTo(this) }
        .forEach(action)
}

context (SwiftModelScope)
private fun FileSpec.Builder.addSwiftToKotlinConversion(
    from: SupportedFlow.Variant,
    to: SupportedFlow.Variant,
    typeBound: TypeName,
    flowTypeParameter: TypeName,
) {
    addFunction(
        FunctionSpec.builder(to.kotlinFlowModel.kotlinSirClass.simpleName)
            .addModifiers(Modifier.PUBLIC)
            .addTypeVariable(TypeVariableName.typeVariable("T", TypeVariableName.Bound(typeBound)))
            .addParameter(
                "_",
                "flow",
                from.swiftFlowClass().internalName.toSwiftPoetName().parameterizedBy(TypeVariableName.typeVariable("T"))
            )
            .returns(to.kotlinFlowModel.kotlinSirClass.fqName.toSwiftPoetName().parameterizedBy(flowTypeParameter))
            .addStatement("return %T(%L)", to.kotlinFlowModel.kotlinSirClass.fqName.toSwiftPoetName(), "flow.delegate")
            .build()
    )
}

context (SwiftModelScope)
private fun ExtensionSpec.Builder.addSwiftToSwiftConversion(from: SupportedFlow.Variant, typeParameter: TypeName) {
    addFunction(
        FunctionSpec.constructorBuilder()
            .addModifiers(Modifier.CONVENIENCE)
            .addParameter("_", "flow", from.swiftFlowClass().internalName.toSwiftPoetName().parameterizedBy(typeParameter))
            .addStatement("self.init(internal: %L)", "flow.delegate")
            .build()
    )
}

context (SwiftModelScope)
private fun ExtensionSpec.Builder.addKotlinToSwiftConversion(from: SupportedFlow.Variant, flowTypeParameter: TypeName) {
    addFunction(
        FunctionSpec.constructorBuilder()
            .addModifiers(Modifier.CONVENIENCE)
            .addParameter(
                "_",
                "flow",
                from.kotlinFlowModel.kotlinSirClass.internalName.toSwiftPoetName().parameterizedBy(flowTypeParameter)
            )
            .addStatement("self.init(internal: %L)", "flow")
            .build()
    )
}