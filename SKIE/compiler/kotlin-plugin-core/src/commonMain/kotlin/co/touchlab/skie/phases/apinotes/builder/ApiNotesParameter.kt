package co.touchlab.skie.phases.apinotes.builder

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiNotesParameter(
    @SerialName("Position")
    val position: Int,
    @SerialName("Type")
    val type: String = "",
)
