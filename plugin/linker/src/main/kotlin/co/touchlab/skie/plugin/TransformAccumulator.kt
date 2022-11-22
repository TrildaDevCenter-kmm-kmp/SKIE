@file:Suppress("invisible_reference", "invisible_member")

package co.touchlab.skie.plugin

import co.touchlab.skie.plugin.api.function.MutableSwiftFunctionName
import co.touchlab.skie.plugin.api.type.MutableSwiftTypeName
import co.touchlab.skie.plugin.api.type.SwiftBridgedName
import co.touchlab.skie.plugin.reflection.reflectors.mapper
import org.jetbrains.kotlin.backend.common.serialization.findSourceFile
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamer
import org.jetbrains.kotlin.backend.konan.objcexport.getClassIfCategory
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SourceFile

internal class TransformAccumulator(
    private val namer: ObjCExportNamer,
) {
    private val mutableTypeTransforms = mutableMapOf<TypeTransformTarget, ObjcClassTransformScope>()
    val typeTransforms: Map<TypeTransformTarget, ObjcClassTransformScope> = mutableTypeTransforms

    private val nameResolver = NameResolver(namer)

    operator fun get(descriptor: ClassDescriptor): ObjcClassTransformScope? = mutableTypeTransforms[TypeTransformTarget.Class(descriptor)]

    fun transform(descriptor: ClassDescriptor): ObjcClassTransformScope {
        val target = TypeTransformTarget.Class(descriptor)

        return mutableTypeTransforms.getOrPut(target) {
            ObjcClassTransformScope(nameResolver.resolveName(target))
        }
    }

    fun resolveName(descriptor: ClassDescriptor): MutableSwiftTypeName = nameResolver.resolveName(TypeTransformTarget.Class(descriptor))

    operator fun get(descriptor: PropertyDescriptor): ObjcPropertyTransformScope? =
        mutableTypeTransforms[descriptor.containingTarget]?.properties?.get(descriptor)

    fun transform(descriptor: PropertyDescriptor): ObjcPropertyTransformScope =
        typeTransform(descriptor.containingTarget).properties.getOrPut(descriptor) {
            ObjcPropertyTransformScope()
        }

    operator fun get(descriptor: FunctionDescriptor): ObjcMethodTransformScope? =
        mutableTypeTransforms[descriptor.containingTarget]?.methods?.get(descriptor)

    fun transform(descriptor: FunctionDescriptor): ObjcMethodTransformScope =
        typeTransform(descriptor.containingTarget).methods.getOrPut(descriptor) {
            ObjcMethodTransformScope(swiftName = nameResolver.resolveName(descriptor))
        }

    fun resolveName(descriptor: FunctionDescriptor): MutableSwiftFunctionName = nameResolver.resolveName(descriptor)

    fun close() {
        // TODO: Add check for closed state in all mutating methods.
        ensureTypesRenamedWhereNeeded()
        ensureChildClassesRenamedWhereNeeded()
        ensurePropertiesRenamedWhereNeeded()
        ensureFunctionsRenamedWhereNeeded()
    }

    private fun ensureTypesRenamedWhereNeeded() {
        nameResolver.typeNames.forEach { (target, name) ->
            if (name.isChanged) {
                typeTransform(target)
            }
        }

        typeTransforms.values.forEach { scope ->
            if (scope.isHidden && !scope.swiftName.isSimpleNameChanged) {
                scope.swiftName.simpleName = "__${scope.swiftName.simpleName}"
            }
        }
    }

    private fun ensureChildClassesRenamedWhereNeeded() {
        fun touchNestedClassTransforms(descriptor: ClassDescriptor) {
            return descriptor.unsubstitutedMemberScope.getContributedDescriptors().filterIsInstance<ClassDescriptor>()
                .filter { it.kind == ClassKind.CLASS || it.kind == ClassKind.OBJECT }
                .forEach { childDescriptor ->
                    val target = TypeTransformTarget.Class(childDescriptor)
                    val transform = typeTransform(target)
                    assert(transform.swiftName.isChanged) { "Expected to have a new name for $childDescriptor" }

                    touchNestedClassTransforms(childDescriptor)
                }
        }

        mutableTypeTransforms
            .toList()
            .forEach { (target, transform) ->
                if (target is TypeTransformTarget.Class && transform.swiftName.isChanged) {
                    touchNestedClassTransforms(target.descriptor)
                }
            }
    }

    private fun ensurePropertiesRenamedWhereNeeded() {
        typeTransforms.values.forEach { classScope ->
            classScope.properties.forEach { (property, scope) ->
                if (scope.isHidden && scope.rename == null) {
                    scope.rename = "__${property.name.asString()}"
                }
            }
        }
    }

    private fun ensureFunctionsRenamedWhereNeeded() {
        nameResolver.functionNames.forEach { (descriptor, name) ->
            if (name.isChanged) {
                transform(descriptor)
            }
        }

        typeTransforms.values.forEach { classScope ->
            classScope.methods.values.forEach { scope ->
                if (scope.isHidden && !scope.swiftName.isChanged) {
                    scope.swiftName.name = "__${scope.swiftName.name}"
                }
            }
        }
    }

    private val CallableMemberDescriptor.containingTarget: TypeTransformTarget
        get() {
            val categoryClass = namer.mapper.getClassIfCategory(this)
            if (categoryClass != null) {
                return TypeTransformTarget.Class(categoryClass)
            }
            return when (val containingDeclaration = containingDeclaration) {
                is ClassDescriptor -> TypeTransformTarget.Class(containingDeclaration)
                is PackageFragmentDescriptor -> TypeTransformTarget.File(findSourceFile())
                else -> error("Unexpected containing declaration: $containingDeclaration")
            }
        }

    private fun typeTransform(typeTransformTarget: TypeTransformTarget): ObjcClassTransformScope {
        return mutableTypeTransforms.getOrPut(typeTransformTarget) {
            ObjcClassTransformScope(nameResolver.resolveName(typeTransformTarget))
        }
    }

    class ObjcClassTransformScope(
        var swiftName: MutableSwiftTypeName,
        var isRemoved: Boolean = false,
        var isHidden: Boolean = false,
        var bridge: SwiftBridgedName? = null,
    ) {
        val newSwiftName: MutableSwiftTypeName?
            get() = swiftName.takeIf { it.isChanged }

        val properties = mutableMapOf<PropertyDescriptor, ObjcPropertyTransformScope>()
        val methods = mutableMapOf<FunctionDescriptor, ObjcMethodTransformScope>()
    }

    sealed interface ObjcCallableMemberTransformScope {
        var isRemoved: Boolean
        var isHidden: Boolean
    }

    class ObjcPropertyTransformScope(
        override var isRemoved: Boolean = false,
        override var isHidden: Boolean = false,
        var rename: String? = null,
    ): ObjcCallableMemberTransformScope

    class ObjcMethodTransformScope(
        override var isRemoved: Boolean = false,
        override var isHidden: Boolean = false,
        var swiftName: MutableSwiftFunctionName,
    ): ObjcCallableMemberTransformScope {
        val newSwiftName: MutableSwiftFunctionName?
            get() = swiftName.takeIf { it.isChanged }
    }

    sealed interface TypeTransformTarget {
        data class Class(val descriptor: ClassDescriptor) : TypeTransformTarget
        data class File(val file: SourceFile) : TypeTransformTarget
    }
}