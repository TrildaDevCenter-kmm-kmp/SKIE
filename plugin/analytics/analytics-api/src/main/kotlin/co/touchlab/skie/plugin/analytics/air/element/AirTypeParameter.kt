package co.touchlab.skie.plugin.analytics.air.element

import co.touchlab.skie.plugin.analytics.air.type.AirType
import co.touchlab.skie.plugin.analytics.air.type.AirTypeVariance
import co.touchlab.skie.plugin.analytics.air.visitor.AirElementVisitor
import kotlinx.serialization.Serializable

@Serializable
data class AirTypeParameter(
    val symbol: Symbol,
    val name: Name,
    val isReified: Boolean,
    val variance: AirTypeVariance,
    val superTypes: List<AirType>,
    val annotations: List<AirConstantObject>,
    val origin: AirOrigin,
) : AirElement {

    override fun <R, D> accept(visitor: AirElementVisitor<R, D>, data: D): R =
        visitor.visitTypeParameter(this, data)

    override fun <D> acceptChildren(visitor: AirElementVisitor<Unit, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
    }

    @Serializable
    data class Name(val name: String)

    @Serializable
    data class Symbol(val id: Int) : AirTypeDeclaration.Symbol
}
