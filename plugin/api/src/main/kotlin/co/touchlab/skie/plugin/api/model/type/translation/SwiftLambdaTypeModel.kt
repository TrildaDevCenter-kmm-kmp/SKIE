package co.touchlab.skie.plugin.api.model.type.translation

import co.touchlab.skie.plugin.api.sir.declaration.SwiftIrDeclaration
import io.outfoxx.swiftpoet.AttributeSpec
import io.outfoxx.swiftpoet.FunctionTypeName
import io.outfoxx.swiftpoet.ParameterSpec
import io.outfoxx.swiftpoet.TypeName

// data class SwiftLambdaTypeModel(
//     val returnType: SwiftTypeModel,
//     val parameterTypes: List<SwiftReferenceTypeModel>,
//     val isEscaping: Boolean,
// ) : SwiftNonNullReferenceTypeModel {
//
//     override val stableFqName: SwiftFqName
//         get() = SwiftFqName.Lambda(parameterTypes.map { it.stableFqName }, returnType.stableFqName, isEscaping)
// }

data class SwiftLambdaSirType(
    val returnType: SirType,
    val parameterTypes: List<SwiftReferenceSirType>,
    val isEscaping: Boolean,
) : SwiftNonNullReferenceSirType {

    override val declaration: SwiftIrDeclaration
        get() = TODO("Not yet implemented")

    override fun toSwiftPoetUsage(): TypeName = FunctionTypeName.get(
        parameters = parameterTypes.map { ParameterSpec.unnamed(it.toSwiftPoetUsage()) },
        returnType = returnType.toSwiftPoetUsage(),
        attributes = if (isEscaping) {
            listOf(AttributeSpec.ESCAPING)
        } else {
            emptyList()
        }
    )
}
