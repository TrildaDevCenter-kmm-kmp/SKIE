package co.touchlab.skie.plugin.generator.internal.util

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.Modality

internal val ClassDescriptor.isSealed: Boolean
    get() = this.modality == Modality.SEALED
