package co.touchlab.skie.plugin.generator.internal.coroutines.suspend

import co.touchlab.skie.plugin.api.model.SwiftModelScope
import co.touchlab.skie.plugin.api.model.type.KotlinTypeSwiftModel
import co.touchlab.skie.plugin.api.sir.element.SirClass
import co.touchlab.skie.plugin.api.sir.element.SirTypeDeclaration
import co.touchlab.skie.plugin.api.sir.element.copyTypeParametersFrom
import co.touchlab.skie.plugin.api.sir.element.toSwiftPoetVariables
import co.touchlab.skie.plugin.api.sir.element.toTypeFromEnclosingTypeParameters
import io.outfoxx.swiftpoet.FileSpec
import io.outfoxx.swiftpoet.FunctionSpec
import io.outfoxx.swiftpoet.Modifier

class SkieClassSuspendGenerator {

    private val skieClassCache = mutableMapOf<KotlinTypeSwiftModel, SirClass>()

    context(SwiftModelScope)
    fun getOrCreateSkieClass(swiftModel: KotlinTypeSwiftModel): SirTypeDeclaration =
        skieClassCache.getOrPut(swiftModel) {
            val skieClass = createSkieClass(swiftModel)

            sirProvider.getFile(swiftModel).swiftPoetBuilderModifications.add {
                generateNamespaceProvider(swiftModel, skieClass)
            }

            skieClass
        }

    context(SwiftModelScope)
    private fun createSkieClass(swiftModel: KotlinTypeSwiftModel): SirClass {
        val skieClass = SirClass(
            simpleName = "__Suspend",
            parent = sirProvider.getNamespace(swiftModel),
        )

        skieClass.copyTypeParametersFrom(swiftModel.kotlinSirClass)

        skieClass.addBody(swiftModel)

        return skieClass
    }

    context(SwiftModelScope)
    private fun FileSpec.Builder.generateNamespaceProvider(swiftModel: KotlinTypeSwiftModel, skieClass: SirClass) {
        val kotlinClass = swiftModel.kotlinSirClass
        val typeParameters = skieClass.typeParameters
        val skieClassType = skieClass.toTypeFromEnclosingTypeParameters(typeParameters).toSwiftPoetUsage()

        addFunction(
            FunctionSpec.builder("skie")
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariables(typeParameters.toSwiftPoetVariables())
                .addParameter("_", "kotlinObject", kotlinClass.toTypeFromEnclosingTypeParameters(typeParameters).toSwiftPoetUsage())
                .addCode("return %T(kotlinObject)", skieClass.internalName.toSwiftPoetName())
                .returns(skieClassType)
                .build(),
        )
    }

    companion object {

        const val kotlinObjectVariableName: String = "__kotlinObject"
    }
}

private fun SirClass.addBody(
    swiftModel: KotlinTypeSwiftModel,
) {
    val kotlinType = swiftModel.kotlinSirClass.toTypeFromEnclosingTypeParameters(typeParameters).toSwiftPoetUsage()

    swiftPoetBuilderModifications.add {
        addProperty(SkieClassSuspendGenerator.kotlinObjectVariableName, kotlinType, Modifier.PUBLIC)

        addFunction(
            FunctionSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter("_", SkieClassSuspendGenerator.kotlinObjectVariableName, kotlinType)
                .addCode("self.${SkieClassSuspendGenerator.kotlinObjectVariableName} = ${SkieClassSuspendGenerator.kotlinObjectVariableName}")
                .build(),
        )
    }
}
