package co.touchlab.skie.compilerinject.reflection.reflectors

import co.touchlab.skie.compilerinject.reflection.Reflector
import co.touchlab.skie.compilerinject.reflection.reflectedBy
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.IdSignatureComposer
import org.jetbrains.kotlin.ir.util.SymbolTable

class ObjCExportReflector(
    override val instance: Any,
) : Reflector(fqName) {

    var exportedInterface by declaredField<Any>()

    companion object {

        const val fqName: String = "org.jetbrains.kotlin.backend.konan.objcexport.ObjCExport"

        fun new(context: CommonBackendContext): ObjCExportReflector {
            val aClass = this::class.java.classLoader.loadClass(fqName)

            val constructor = aClass.constructors.first()

            return constructor.newInstance(context, createSymbolTable()).reflectedBy()
        }

        private fun createSymbolTable(): SymbolTable =
            SymbolTable(DummySignaturer(), IrFactoryImpl)

        class DummySignaturer : IdSignatureComposer {

            override fun composeAnonInitSignature(descriptor: ClassDescriptor): IdSignature? = null

            override fun composeEnumEntrySignature(descriptor: ClassDescriptor): IdSignature? = null

            override fun composeFieldSignature(descriptor: PropertyDescriptor): IdSignature? = null

            override fun composeSignature(descriptor: DeclarationDescriptor): IdSignature? = null

            override fun withFileSignature(fileSignature: IdSignature.FileSignature, body: () -> Unit) {
                body()
            }
        }
    }
}
