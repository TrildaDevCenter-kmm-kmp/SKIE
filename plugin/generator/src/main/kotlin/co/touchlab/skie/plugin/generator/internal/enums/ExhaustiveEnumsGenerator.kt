package co.touchlab.skie.plugin.generator.internal.enums

import co.touchlab.skie.configuration.Configuration
import co.touchlab.skie.configuration.gradle.EnumInterop
import co.touchlab.skie.plugin.api.SkieContext
import co.touchlab.skie.plugin.api.model.SwiftModelVisibility
import co.touchlab.skie.plugin.api.model.callable.KotlinDirectlyCallableMemberSwiftModelVisitor
import co.touchlab.skie.plugin.api.model.callable.function.KotlinFunctionSwiftModel
import co.touchlab.skie.plugin.api.model.callable.parameter.KotlinValueParameterSwiftModel
import co.touchlab.skie.plugin.api.model.callable.property.regular.KotlinRegularPropertySwiftModel
import co.touchlab.skie.plugin.api.model.type.KotlinClassSwiftModel
import co.touchlab.skie.plugin.api.model.type.MutableKotlinClassSwiftModel
import co.touchlab.skie.plugin.api.model.type.SwiftTypeSwiftModel
import co.touchlab.skie.plugin.api.model.type.bridgedOrStableSpec
import co.touchlab.skie.plugin.api.model.type.packageName
import co.touchlab.skie.plugin.api.model.type.stableSpec
import co.touchlab.skie.plugin.api.util.qualifiedLocalTypeName
import co.touchlab.skie.plugin.generator.internal.enums.ObjectiveCBridgeable.addObjcBridgeableImplementation
import co.touchlab.skie.plugin.generator.internal.runtime.belongsToSkieRuntime
import co.touchlab.skie.plugin.generator.internal.util.BaseGenerator
import co.touchlab.skie.plugin.generator.internal.util.NamespaceProvider
import co.touchlab.skie.plugin.generator.internal.util.Reporter
import io.outfoxx.swiftpoet.CodeBlock
import io.outfoxx.swiftpoet.DeclaredTypeName
import io.outfoxx.swiftpoet.ExtensionSpec
import io.outfoxx.swiftpoet.FileSpec
import io.outfoxx.swiftpoet.FunctionSpec
import io.outfoxx.swiftpoet.Modifier
import io.outfoxx.swiftpoet.ParameterSpec
import io.outfoxx.swiftpoet.PropertySpec
import io.outfoxx.swiftpoet.STRING
import io.outfoxx.swiftpoet.TypeAliasSpec
import io.outfoxx.swiftpoet.TypeSpec
import io.outfoxx.swiftpoet.joinToCode
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.isEnumClass

internal class ExhaustiveEnumsGenerator(
    skieContext: SkieContext,
    namespaceProvider: NamespaceProvider,
    configuration: Configuration,
    private val reporter: Reporter,
) : BaseGenerator(skieContext, namespaceProvider, configuration) {

    override val isActive: Boolean = true

    override fun execute() {
        module.configure {
            exposedClasses
                .filter { it.isSupported }
                .forEach {
                    generate(it)
                }
        }
    }

    private val KotlinClassSwiftModel.isSupported: Boolean
        get() = this.classDescriptor.kind.isEnumClass &&
            this.classDescriptor.isEnumInteropEnabled &&
            !this.classDescriptor.belongsToSkieRuntime

    private val ClassDescriptor.isEnumInteropEnabled: Boolean
        get() = getConfiguration(EnumInterop.Enabled)

    private fun generate(classSwiftModel: MutableKotlinClassSwiftModel) {
        if (classSwiftModel.enumEntries.isEmpty()) {
            reporter.report(
                Reporter.Severity.Warning,
                "Enum ${classSwiftModel.identifier} has no entries, no Swift enum will be generated. " +
                    "To silence this warning, add @EnumInterop.Disabled above the enum declaration.",
                classSwiftModel.classDescriptor,
            )
            return
        }

        classSwiftModel.configureBridging()
        classSwiftModel.generateBridge()
    }

    private fun MutableKotlinClassSwiftModel.configureBridging() {
        this.visibility = SwiftModelVisibility.Replaced
        this.bridge = SwiftTypeSwiftModel(
            containingType = this.original.containingType,
            identifier = this.original.identifier,
            isHashable = true,
        )
    }

    private fun KotlinClassSwiftModel.generateBridge() {
        val classSwiftModel = this

        module.generateCode(this) {
            if (classSwiftModel.bridge!!.packageName.isNotBlank()) {
                addNestedBridge(classSwiftModel)
            } else {
                addTopLevelBridge(classSwiftModel)
            }
        }
    }

    private fun FileSpec.Builder.addTopLevelBridge(classSwiftModel: KotlinClassSwiftModel) {
        addType(classSwiftModel.generateBridgingEnum())
    }

    private fun FileSpec.Builder.addNestedBridge(classSwiftModel: KotlinClassSwiftModel) {
        addExtension(
            ExtensionSpec.builder(DeclaredTypeName.qualifiedLocalTypeName(classSwiftModel.bridge!!.packageName))
                .addType(classSwiftModel.generateBridgingEnum())
                .build()
        )
    }

    private fun KotlinClassSwiftModel.generateBridgingEnum(): TypeSpec =
        TypeSpec.enumBuilder(bridge!!.identifier)
            .addAttribute("frozen")
            .addModifiers(Modifier.PUBLIC)
            .addSuperType(STRING)
            .addSuperType(DeclaredTypeName.typeName("Swift.Hashable"))
            .addEnumCases(this)
            .addPassthroughForMembers(this)
            .addNestedClassTypeAliases(this)
            .addCompanionObjectPropertyIfNeeded(this)
            .addObjcBridgeableImplementation(this)
            .build()

    private fun TypeSpec.Builder.addEnumCases(classSwiftModel: KotlinClassSwiftModel): TypeSpec.Builder =
        this.apply {
            classSwiftModel.enumEntries.forEach {
                addEnumCase(it.identifier)
            }
        }

    private fun TypeSpec.Builder.addPassthroughForMembers(classSwiftModel: KotlinClassSwiftModel): TypeSpec.Builder =
        this.apply {
            classSwiftModel.allAccessibleDirectlyCallableMembers
                .forEach {
                    it.accept(MemberPassthroughGeneratorVisitor(this))
                }
        }

    private fun TypeSpec.Builder.addNestedClassTypeAliases(classSwiftModel: KotlinClassSwiftModel): TypeSpec.Builder =
        this.apply {
            classSwiftModel.nestedClasses.forEach {
                addType(
                    TypeAliasSpec.builder(it.identifier, it.stableSpec)
                        .addModifiers(Modifier.PUBLIC)
                        .build()
                )
            }
        }

    private class MemberPassthroughGeneratorVisitor(
        private val builder: TypeSpec.Builder,
    ) : KotlinDirectlyCallableMemberSwiftModelVisitor.Unit {

        override fun visit(function: KotlinFunctionSwiftModel) {
            if (!function.isSupported) return

            builder.addFunction(
                FunctionSpec.builder(function.identifier)
                    .addModifiers(Modifier.PUBLIC)
                    .addFunctionValueParameters(function)
                    .throws(function.isThrowing)
                    .returns(function.returnType.stableSpec)
                    .addFunctionBody(function)
                    .build()
            )
        }

        // TODO When implementing interfaces: Implement remaining methods if needed (at least compareTo(other:) and toString/description should be supportable)
        private val unsupportedFunctionNames = listOf("compareTo(other:)", "hash()", "description()", "isEqual(_:)")

        private val KotlinFunctionSwiftModel.isSupported: Boolean
            get() = !this.isSuspend && this.name !in unsupportedFunctionNames

        private fun FunctionSpec.Builder.addFunctionValueParameters(function: KotlinFunctionSwiftModel): FunctionSpec.Builder =
            this.apply {
                function.valueParameters.forEach {
                    this.addFunctionValueParameter(it)
                }
            }

        private fun FunctionSpec.Builder.addFunctionValueParameter(valueParameter: KotlinValueParameterSwiftModel): FunctionSpec.Builder =
            this.addParameter(
                ParameterSpec.builder(
                    valueParameter.argumentLabel,
                    valueParameter.parameterName,
                    valueParameter.type.stableSpec,
                ).build()
            )

        private fun FunctionSpec.Builder.addFunctionBody(function: KotlinFunctionSwiftModel): FunctionSpec.Builder =
            this.addStatement(
                "return %L(self as _ObjectiveCType).%N(%L)",
                if (function.isThrowing) "try " else "",
                function.reference,
                function.valueParameters.map { CodeBlock.of("%N", it.parameterName) }.joinToCode(", "),
            )

        override fun visit(regularProperty: KotlinRegularPropertySwiftModel) {
            builder.addProperty(
                PropertySpec.builder(regularProperty.identifier, regularProperty.type.stableSpec)
                    .addModifiers(Modifier.PUBLIC)
                    .addGetter(regularProperty)
                    .addSetterIfPresent(regularProperty)
                    .build()
            )
        }

        private fun PropertySpec.Builder.addGetter(regularProperty: KotlinRegularPropertySwiftModel): PropertySpec.Builder =
            this.getter(
                FunctionSpec.getterBuilder()
                    .addStatement(
                        "return %L(self as _ObjectiveCType).%N",
                        if (regularProperty.getter.isThrowing) "try " else "",
                        regularProperty.reference,
                    )
                    .build()
            )

        private fun PropertySpec.Builder.addSetterIfPresent(regularProperty: KotlinRegularPropertySwiftModel): PropertySpec.Builder =
            this.apply {
                val setter = regularProperty.setter ?: return@apply

                setter(
                    FunctionSpec.setterBuilder()
                        .addModifiers(Modifier.NONMUTATING)
                        .addParameter("value", regularProperty.type.stableSpec)
                        .addStatement(
                            "%L(self as _ObjectiveCType).%N = value",
                            if (setter.isThrowing) "try " else "",
                            regularProperty.reference,
                        )
                        .build()
                )
            }
    }

    private fun TypeSpec.Builder.addCompanionObjectPropertyIfNeeded(classSwiftModel: KotlinClassSwiftModel): TypeSpec.Builder =
        this.apply {
            val companion = classSwiftModel.companionObject ?: return@apply

            addProperty(
                PropertySpec.builder("companion", companion.bridgedOrStableSpec)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .getter(
                        FunctionSpec.getterBuilder()
                            .addStatement("return _ObjectiveCType.companion")
                            .build()
                    )
                    .build()
            )
        }
}
