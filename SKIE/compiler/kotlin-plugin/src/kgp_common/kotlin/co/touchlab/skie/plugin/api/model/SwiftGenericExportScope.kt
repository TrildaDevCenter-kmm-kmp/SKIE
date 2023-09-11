package co.touchlab.skie.plugin.api.model

import co.touchlab.skie.plugin.api.model.type.KotlinClassSwiftModel
import co.touchlab.skie.plugin.api.sir.element.SirTypeParameter
import co.touchlab.skie.plugin.api.sir.type.TypeParameterUsageSirType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.isInterface

interface SwiftGenericExportScope {

    fun getGenericTypeUsage(typeParameterDescriptor: TypeParameterDescriptor): TypeParameterUsageSirType?

    class Class(
        classDescriptor: ClassDescriptor,
        sirTypeParameters: List<SirTypeParameter>,
    ) : SwiftGenericExportScope {

        constructor(
            kotlinClassSwiftModel: KotlinClassSwiftModel,
        ) : this(kotlinClassSwiftModel.classDescriptor, kotlinClassSwiftModel.kotlinSirClass.typeParameters)

        private val descriptorsWithTypeParameters = if (!classDescriptor.kind.isInterface) {
            val descriptors = classDescriptor.typeConstructor.parameters

            assert(descriptors.size == sirTypeParameters.size)

            descriptors.zip(sirTypeParameters)
        } else {
            emptyList()
        }

        override fun getGenericTypeUsage(typeParameterDescriptor: TypeParameterDescriptor): TypeParameterUsageSirType? {
            val typeParameter = descriptorsWithTypeParameters.firstOrNull { (descriptor, _) ->
                descriptor == typeParameterDescriptor || (descriptor.isCapturedFromOuterDeclaration && descriptor.original == typeParameterDescriptor)
            }?.second

            return typeParameter?.let { TypeParameterUsageSirType(it) }
        }
    }

    object None : SwiftGenericExportScope {

        override fun getGenericTypeUsage(typeParameterDescriptor: TypeParameterDescriptor): TypeParameterUsageSirType? = null
    }
}
