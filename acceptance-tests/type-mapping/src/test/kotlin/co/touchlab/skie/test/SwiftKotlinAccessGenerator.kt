@file:Suppress("invisible_reference", "invisible_member")

package co.touchlab.skie.test

import co.touchlab.skie.plugin.api.descriptorProvider
import co.touchlab.skie.plugin.api.kotlin.getAllExposedMembers
import co.touchlab.skie.plugin.api.model.callable.function.KotlinFunctionSwiftModel
import co.touchlab.skie.plugin.api.model.callable.property.regular.KotlinRegularPropertySwiftModel
import co.touchlab.skie.plugin.api.mutableDescriptorProvider
import co.touchlab.skie.plugin.api.skieContext
import co.touchlab.skie.plugin.intercept.PhaseInterceptor
import io.outfoxx.swiftpoet.ANY_OBJECT
import io.outfoxx.swiftpoet.AttributeSpec
import io.outfoxx.swiftpoet.CodeBlock
import io.outfoxx.swiftpoet.DeclaredTypeName
import io.outfoxx.swiftpoet.FunctionSpec
import io.outfoxx.swiftpoet.FunctionTypeName
import io.outfoxx.swiftpoet.ParameterSpec
import io.outfoxx.swiftpoet.PropertySpec
import io.outfoxx.swiftpoet.TypeSpec
import io.outfoxx.swiftpoet.TypeVariableName
import io.outfoxx.swiftpoet.joinToCode
import io.outfoxx.swiftpoet.parameterizedBy
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.PhaserState
import org.jetbrains.kotlin.backend.konan.driver.phases.PsiToIrContext
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportCodeSpec
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportedInterface
import org.jetbrains.kotlin.resolve.DescriptorUtils

internal class SwiftKotlinAccessGenerator: PhaseInterceptor<PsiToIrContext, ObjCExportedInterface, ObjCExportCodeSpec> {
    override val phase = PhaseInterceptor.Phase.CreateObjCExportCodeSpec

    override fun intercept(
        context: PsiToIrContext,
        input: ObjCExportedInterface,
        next: (PsiToIrContext, ObjCExportedInterface) -> ObjCExportCodeSpec
    ): ObjCExportCodeSpec {
        val descriptorProvider = context.config.configuration.descriptorProvider

        val kotlinClass = descriptorProvider.exposedClasses.first {
            it.name.identifier == "KotlinFile"
        }

        context.config.skieContext.module.file(
            "TypeVerification",
            """
                @resultBuilder
                struct VerifyReturnType<ReturnType> {
                    let value: ReturnType

                    static func buildBlock<T>(_ components: T...) -> VerifyReturnType<T> {
                        return VerifyReturnType<T>(value: components[0])
                    }

                    static func buildFinalResult(_ component: VerifyReturnType<ReturnType>) -> ReturnType {
                        return component.value
                    }
                }

                @propertyWrapper
                struct VerifyParamType<Final> {
                    let wrappedValue: Final

                    var projectedValue: VerifyParamType<Final> {
                        self
                    }

                    init(wrappedValue: Final) {
                        self.wrappedValue = wrappedValue
                    }

                    init(projectedValue: VerifyParamType<Final>) {
                        self.wrappedValue = projectedValue.wrappedValue
                    }
                }
            """.trimIndent())

        context.config.skieContext.module.file("TypeVerification+verify") {
            val parameterCounts = descriptorProvider.getAllExposedMembers(kotlinClass)
                .filter {
                    !DescriptorUtils.isMethodOfAny(it)
                }
                .flatMap {
                    it.swiftModel.directlyCallableMembers
                }
                .filterIsInstance<KotlinFunctionSwiftModel>()
                .map { it.valueParameters.size }
                .toSet()

            /**
             * func verify<P1, T>(function: @escaping (P1) -> T) -> (VerifyParamType<P1>) -> T {
             *     return { $p1 in
             *         function(p1)
             *     }
             * }
             */
            parameterCounts.filter { it >= 1 }.forEach { paramCount ->
                val paramTypeVariables = (1..paramCount).map {
                    TypeVariableName.typeVariable("P$it")
                }
                val returnTypeVariable = TypeVariableName.typeVariable("T")

                addFunction(
                    FunctionSpec.builder("verify")
                        .addTypeVariables(paramTypeVariables)
                        .addTypeVariable(returnTypeVariable)
                        .addParameter(
                            "function", FunctionTypeName.get(
                                parameters = paramTypeVariables.map {
                                    ParameterSpec.unnamed(it)
                                },
                                returnType = returnTypeVariable,
                                attributes = listOf(AttributeSpec.ESCAPING),
                            ),
                        )
                        .returns(
                            FunctionTypeName.get(
                                parameters = paramTypeVariables.map {
                                    ParameterSpec.unnamed(DeclaredTypeName.qualifiedTypeName(".VerifyParamType").parameterizedBy(it))
                                },
                                returnType = returnTypeVariable,
                            )
                        )
                        .addCode(
                            """
                                return { %L in
                                    function(%L)
                                }
                            """.trimIndent(),
                            paramTypeVariables.indices.map { CodeBlock.of("${"$"}p$it") }.joinToCode(),
                            paramTypeVariables.indices.map { CodeBlock.of("p$it") }.joinToCode(),
                        )
                        .build()
                )
            }
        }

        context.config.skieContext.module.file("KotlinFile_access") {
            addType(
                TypeSpec.classBuilder("KotlinFileWrapper")
                    .apply {
                        val typeVariables = kotlinClass.declaredTypeParameters.map { typeParameter ->
                            TypeVariableName(typeParameter.name.identifier, TypeVariableName.bound(ANY_OBJECT))
                        }
                        val parametrizedKotlinClass = if (typeVariables.isNotEmpty()) {
                            kotlinClass.swiftModel.nonBridgedDeclaration.internalName.toSwiftPoetName().parameterizedBy(typeVariables)
                        } else {
                            kotlinClass.swiftModel.nonBridgedDeclaration.internalName.toSwiftPoetName()
                        }

                        addProperty(
                            PropertySpec.builder("kotlinClass", parametrizedKotlinClass)
                                .initializer("%T()", parametrizedKotlinClass)
                                .build()
                        )

                        typeVariables.forEach { typeVariable ->
                            addTypeVariable(typeVariable)
                        }

                        descriptorProvider.getAllExposedMembers(kotlinClass)
                            .filter {
                                !DescriptorUtils.isMethodOfAny(it)
                            }
                            .flatMap {
                                it.swiftModel.directlyCallableMembers
                            }
                            .forEach { swiftModel ->
                                when (swiftModel) {
                                    is KotlinRegularPropertySwiftModel -> {
                                        addProperty(
                                            PropertySpec.builder(swiftModel.identifier, swiftModel.type.toSwiftPoetUsage())
                                                .addAttribute(CodeBlock.of("VerifyReturnType<%T>", swiftModel.type.toSwiftPoetUsage()).toString())
                                                .getter(
                                                    FunctionSpec.getterBuilder()
                                                        .addStatement("kotlinClass.%L", swiftModel.reference)
                                                        .build()
                                                )
                                                .build()
                                        )
                                    }
                                    is KotlinFunctionSwiftModel -> {
                                        if (swiftModel.role == KotlinFunctionSwiftModel.Role.Constructor) {
                                            return@forEach
                                        }
                                        addFunction(
                                            FunctionSpec.builder(swiftModel.identifier)
                                                .addAttribute(CodeBlock.of("VerifyReturnType<%T>", swiftModel.returnType.toSwiftPoetUsage()).toString())
                                                .addParameters(
                                                    swiftModel.valueParameters.map { parameter ->
                                                        ParameterSpec
                                                            .builder(
                                                                parameter.argumentLabel,
                                                                parameter.parameterName,
                                                                parameter.type.toSwiftPoetUsage(),
                                                            )
                                                            .addAttribute("VerifyParamType")
                                                            .build()
                                                    }
                                                )
                                                .returns(swiftModel.returnType.toSwiftPoetUsage())
                                                .addCode(
                                                    """
                                                        let functionVerification = verify(function: kotlinClass.%N)
                                                        functionVerification(%L)
                                                    """.trimIndent(),
                                                    swiftModel.reference,
                                                    swiftModel.valueParameters.map { parameter ->
                                                        CodeBlock.of("_%N", parameter.parameterName)
                                                    }.joinToCode(),
                                                )
                                                .build()
                                        )
                                    }
                                }
                            }
                    }
                    .build()
            )
        }

        return next(context, input)
    }
}
