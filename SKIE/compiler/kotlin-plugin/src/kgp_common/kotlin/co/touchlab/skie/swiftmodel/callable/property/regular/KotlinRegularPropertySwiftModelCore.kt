package co.touchlab.skie.swiftmodel.callable.property.regular

import co.touchlab.skie.swiftmodel.SwiftModelVisibility
import co.touchlab.skie.swiftmodel.factory.ObjCTypeProvider
import co.touchlab.skie.swiftmodel.type.FlowMappingStrategy
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamer
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCType
import org.jetbrains.kotlin.descriptors.PropertyDescriptor

class KotlinRegularPropertySwiftModelCore(
    val descriptor: PropertyDescriptor,
    namer: ObjCExportNamer,
    private val objCTypeProvider: ObjCTypeProvider,
) {

    var identifier: String = namer.getPropertyName(descriptor.original).swiftName

    var visibility: SwiftModelVisibility = SwiftModelVisibility.Visible

    val getter: KotlinRegularPropertyGetterSwiftModel = DefaultKotlinRegularPropertyGetterSwiftModel(
        descriptor.getter ?: error("$descriptor does not have a getter."),
        namer,
    )

    val setter: KotlinRegularPropertySetterSwiftModel? = descriptor.setter?.let { DefaultKotlinRegularPropertySetterSwiftModel(it, namer) }

    val objCName: String = namer.getPropertyName(descriptor.original).objCName

    fun getObjCType(propertyDescriptor: PropertyDescriptor, flowMappingStrategy: FlowMappingStrategy): ObjCType =
        objCTypeProvider.getPropertyType(descriptor, propertyDescriptor, flowMappingStrategy)
}
