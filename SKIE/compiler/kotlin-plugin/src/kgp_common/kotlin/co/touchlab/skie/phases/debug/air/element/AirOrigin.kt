package co.touchlab.skie.phases.debug.air.element

import kotlinx.serialization.Serializable

@Serializable
data class AirOrigin(val name: String, val isSynthetic: Boolean)
