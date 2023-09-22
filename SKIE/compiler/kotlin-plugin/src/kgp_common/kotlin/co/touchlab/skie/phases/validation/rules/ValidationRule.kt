package co.touchlab.skie.phases.validation.rules

import co.touchlab.skie.configuration.SkieConfiguration
import co.touchlab.skie.util.Reporter
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor

internal interface ValidationRule<D : DeclarationDescriptor> {

    context(Reporter, SkieConfiguration)
    fun validate(descriptor: D)
}

context(Reporter, SkieConfiguration) internal fun <D : DeclarationDescriptor> Iterable<ValidationRule<D>>.validate(descriptor: D) {
    this.forEach {
        it.validate(descriptor)
    }
}
