package co.touchlab.skie.swiftmodel.callable.parameter

import co.touchlab.skie.swiftmodel.type.FlowMappingStrategy

interface MutableKotlinValueParameterSwiftModel : KotlinValueParameterSwiftModel {

    override var argumentLabel: String

    override var flowMappingStrategy: FlowMappingStrategy
}