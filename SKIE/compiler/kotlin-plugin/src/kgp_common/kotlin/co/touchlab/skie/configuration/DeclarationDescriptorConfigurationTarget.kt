package co.touchlab.skie.configuration

import co.touchlab.skie.kir.util.findAnnotation
import co.touchlab.skie.kir.util.hasAnnotation
import co.touchlab.skie.phases.runtime.belongsToSkieRuntime
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import kotlin.reflect.KClass

class DeclarationDescriptorConfigurationTarget(private val declarationDescriptor: DeclarationDescriptor) : ConfigurationTarget {

    override val fqName: String = declarationDescriptor.fqNameSafe.asString()

    override val belongsToSkieRuntime: Boolean = declarationDescriptor.belongsToSkieRuntime

    override fun <T : Annotation> hasAnnotation(kClass: KClass<T>): Boolean =
        declarationDescriptor.hasAnnotation(kClass)

    override fun <T : Annotation> findAnnotation(kClass: KClass<T>): T? =
        declarationDescriptor.findAnnotation(kClass)
}
