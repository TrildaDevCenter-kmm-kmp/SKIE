package co.touchlab.skie.sir.element

import co.touchlab.skie.sir.element.util.sirDeclarationParent
import co.touchlab.skie.sir.type.SirType
import io.outfoxx.swiftpoet.Modifier

class SirSimpleFunction(
    override var identifier: String,
    parent: SirDeclarationParent,
    var returnType: SirType,
    override var visibility: SirVisibility = SirVisibility.Public,
    override var scope: SirScope = parent.coerceScope(SirScope.Member),
    attributes: List<String> = emptyList(),
    modifiers: List<Modifier> = emptyList(),
    var isAsync: Boolean = false,
    override var throws: Boolean = false,
) : SirFunction(attributes.toMutableList(), modifiers.toMutableList()), SirTypeParameterParent, SirOverridableDeclaration<SirSimpleFunction> {

    override val reference: String
        get() = if (valueParameters.isEmpty()) {
            identifierAfterVisibilityChanges
        } else {
            "$identifierAfterVisibilityChanges(${valueParameters.joinToString("") { "${it.labelOrName}:" }})"
        }

    override val name: String
        get() = if (valueParameters.isEmpty()) "$identifierAfterVisibilityChanges()" else reference

    override var parent: SirDeclarationParent by sirDeclarationParent(parent)

    override val typeParameters: MutableList<SirTypeParameter> = mutableListOf()

    override val valueParameters: MutableList<SirValueParameter> = mutableListOf()

    override val hasValidSignature: Boolean
        get() = returnType.evaluate().isValid && super.hasValidSignature

    private val overridableDeclarationDelegate = SirOverridableDeclarationDelegate(this)

    override val memberOwner: SirClass? by overridableDeclarationDelegate::memberOwner

    override val overriddenDeclarations: List<SirSimpleFunction> by overridableDeclarationDelegate::overriddenDeclarations

    override val overriddenBy: List<SirSimpleFunction> by overridableDeclarationDelegate::overriddenBy

    override fun addOverride(declaration: SirSimpleFunction) {
        overridableDeclarationDelegate.addOverride(declaration)
    }

    override fun removeOverride(declaration: SirSimpleFunction) {
        overridableDeclarationDelegate.removeOverride(declaration)
    }

    override fun addOverriddenBy(declaration: SirSimpleFunction) {
        overridableDeclarationDelegate.addOverriddenBy(declaration)
    }

    override fun removeOverriddenBy(declaration: SirSimpleFunction) {
        overridableDeclarationDelegate.removeOverriddenBy(declaration)
    }

    override fun toString(): String = "${this::class.simpleName}: $name"

    companion object {

        context(SirDeclarationParent)
        operator fun invoke(
            identifier: String,
            returnType: SirType,
            visibility: SirVisibility = SirVisibility.Public,
            scope: SirScope = coerceScope(SirScope.Member),
            attributes: List<String> = emptyList(),
            modifiers: List<Modifier> = emptyList(),
            isAsync: Boolean = false,
            throws: Boolean = false,
        ): SirSimpleFunction =
            SirSimpleFunction(
                identifier = identifier,
                parent = this@SirDeclarationParent,
                returnType = returnType,
                visibility = visibility,
                scope = scope,
                attributes = attributes,
                modifiers = modifiers,
                isAsync = isAsync,
                throws = throws,
            )
    }
}

fun SirSimpleFunction.shallowCopy(
    identifier: String = this.identifier,
    parent: SirDeclarationParent = this.parent,
    returnType: SirType = this.returnType,
    visibility: SirVisibility = this.visibility,
    scope: SirScope = parent.coerceScope(this.scope),
    attributes: List<String> = this.attributes,
    modifiers: List<Modifier> = this.modifiers,
    isAsync: Boolean = this.isAsync,
    throws: Boolean = this.throws,
): SirSimpleFunction =
    SirSimpleFunction(
        identifier = identifier,
        parent = parent,
        returnType = returnType,
        visibility = visibility,
        scope = scope,
        attributes = attributes,
        modifiers = modifiers,
        isAsync = isAsync,
        throws = throws,
    )