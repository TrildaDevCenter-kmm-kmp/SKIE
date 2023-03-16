package co.touchlab.skie.plugin.api.sir.type

import co.touchlab.skie.plugin.api.sir.declaration.BuiltinDeclarations

object SwiftAnyObjectSirType: SwiftNonNullReferenceSirType {
    override val declaration = BuiltinDeclarations.Swift.AnyObject

    override fun toSwiftPoetUsage() = declaration.internalName.toSwiftPoetName()

    override fun toString(): String = asString()
}
