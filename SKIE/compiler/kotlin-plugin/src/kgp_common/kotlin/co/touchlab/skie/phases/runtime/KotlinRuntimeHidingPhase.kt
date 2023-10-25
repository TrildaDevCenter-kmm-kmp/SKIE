package co.touchlab.skie.phases.runtime

import co.touchlab.skie.phases.SirPhase
import co.touchlab.skie.sir.element.SirVisibility

object KotlinRuntimeHidingPhase : SirPhase {

    context(SirPhase.Context)
    override fun execute() {
        kirProvider.allClasses
            .filter { it.belongsToSkieKotlinRuntime }
            .forEach {
                it.originalSirClass.visibility = SirVisibility.PublicButHidden
            }
    }
}
