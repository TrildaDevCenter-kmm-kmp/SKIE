package co.touchlab.skie.swiftmodel.callable.property.converted

import co.touchlab.skie.swiftmodel.callable.swiftModelOrigin
import co.touchlab.skie.swiftmodel.MutableSwiftModelScope
import co.touchlab.skie.swiftmodel.callable.KotlinCallableMemberSwiftModel
import co.touchlab.skie.swiftmodel.callable.KotlinCallableMemberSwiftModelVisitor
import co.touchlab.skie.swiftmodel.callable.MutableKotlinCallableMemberSwiftModel
import co.touchlab.skie.swiftmodel.callable.MutableKotlinCallableMemberSwiftModelVisitor
import co.touchlab.skie.swiftmodel.callable.function.MutableKotlinFunctionSwiftModel
import co.touchlab.skie.swiftmodel.type.KotlinTypeSwiftModel
import co.touchlab.skie.sir.type.SirType
import org.jetbrains.kotlin.descriptors.PropertyDescriptor

class ActualKotlinConvertedPropertySwiftModel(
    override val descriptor: PropertyDescriptor,
    override val allBoundedSwiftModels: List<MutableKotlinCallableMemberSwiftModel>,
    swiftModelScope: MutableSwiftModelScope,
) : MutableKotlinConvertedPropertySwiftModel {

    override val owner: KotlinTypeSwiftModel? by lazy {
        with(swiftModelScope) {
            descriptor.owner()
        }
    }

    override val receiver: SirType by lazy {
        with(swiftModelScope) {
            descriptor.receiverType()
        }
    }

    override val getter: MutableKotlinFunctionSwiftModel by lazy {
        with(swiftModelScope) {
            descriptor.getter?.swiftModel ?: error("Property does not have a getter: $descriptor")
        }
    }

    override val setter: MutableKotlinFunctionSwiftModel? by lazy {
        with(swiftModelScope) {
            descriptor.setter?.swiftModel
        }
    }

    override val origin: KotlinCallableMemberSwiftModel.Origin = descriptor.swiftModelOrigin

    override val scope: KotlinCallableMemberSwiftModel.Scope = KotlinCallableMemberSwiftModel.Scope.Static

    override fun toString(): String = descriptor.toString()

    override fun <OUT> accept(visitor: KotlinCallableMemberSwiftModelVisitor<OUT>): OUT =
        visitor.visit(this)

    override fun <OUT> accept(visitor: MutableKotlinCallableMemberSwiftModelVisitor<OUT>): OUT =
        visitor.visit(this)
}