package co.touchlab.skie.plugin.api.sir.element

object SirNoParent : SirTopLevelDeclarationParent {

    override val parent: SirDeclarationParent? = null

    override val declarations: MutableList<SirDeclaration>
        get() = mutableListOf()
}
