package co.touchlab.skie.oir.type

import org.jetbrains.kotlin.backend.konan.objcexport.objcNullableAttribute
import org.jetbrains.kotlin.backend.konan.objcexport.objcNullableResultAttribute

data class NullableReferenceOirType(
    val nonNullType: NonNullReferenceOirType,
    val isNullableResult: Boolean = false,
) : ReferenceOirType() {

    override fun render(attrsAndName: String, needsNonnullAttribute: Boolean): String {
        val attribute = if (isNullableResult) objcNullableResultAttribute else objcNullableAttribute

        return nonNullType.render(attribute.withAttrsAndName(attrsAndName), false)
    }
}
