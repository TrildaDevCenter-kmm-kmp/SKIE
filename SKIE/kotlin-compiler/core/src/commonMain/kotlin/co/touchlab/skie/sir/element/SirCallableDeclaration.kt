package co.touchlab.skie.sir.element

import co.touchlab.skie.kir.element.DeprecationLevel
import co.touchlab.skie.sir.signature.Signature

sealed interface SirCallableDeclaration : SirDeclaration, SirElementWithModifiers, SirElementWithAttributes, SirDeclarationWithScope {

    /**
     * Used to derive other names.
     *
     * Examples:
     * foo
     * foo (isReplaced)
     */
    val identifier: String

    /**
     * Used to derive other names.
     *
     * Examples:
     * foo
     * __foo (isReplaced && !constructor)
     */
    val identifierAfterVisibilityChange: String

    /**
     * Use to obtain declaration `reference` in generated Swift code.
     *
     * Examples:
     * foo  (for properties)
     * foo (for functions without parameters)
     * foo(param1:)
     * __foo(param1:) (isReplaced && !constructor)
     */
    val reference: String

    /**
     * Use `name` in header, api notes and documentation.
     *
     * Examples:
     * foo  (for properties)
     * foo() (for functions without parameters)
     * foo(param1:)
     * __foo(param1:) (isReplaced)
     */
    val name: String

    val deprecationLevel: DeprecationLevel

    fun toReadableString(): String =
        Signature(this).toString()
}

val SirCallableDeclaration.receiverDeclaration: SirClass?
    get() = when (val parent = parent) {
        is SirClass -> parent
        is SirExtension -> parent.classDeclaration
        else -> null
    }
