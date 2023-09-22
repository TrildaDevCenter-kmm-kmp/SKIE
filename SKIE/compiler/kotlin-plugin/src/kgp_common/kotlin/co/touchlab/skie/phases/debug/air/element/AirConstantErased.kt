package co.touchlab.skie.phases.debug.air.element

import co.touchlab.skie.phases.debug.air.visitor.AirElementTransformer
import co.touchlab.skie.phases.debug.air.visitor.AirElementVisitor
import kotlinx.serialization.Serializable

@Serializable
object AirConstantErased : AirConstant {

    override fun <D> transform(transformer: AirElementTransformer<D>, data: D): AirConstantErased =
        transformer.visitConstantErased(this, data)

    override fun <R, D> accept(visitor: AirElementVisitor<R, D>, data: D): R =
        visitor.visitConstantErased(this, data)

    override fun <D> acceptChildren(visitor: AirElementVisitor<Unit, D>, data: D) {
    }
}
