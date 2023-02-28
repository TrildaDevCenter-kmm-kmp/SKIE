package co.touchlab.skie.plugin

import co.touchlab.skie.api.DefaultSkieModule
import co.touchlab.skie.api.phases.ApiNotesGenerationPhase
import co.touchlab.skie.api.phases.FixClassesConflictsPhase
import co.touchlab.skie.api.phases.FixHeaderFilePropertyOrderingPhase
import co.touchlab.skie.api.phases.FixNestedBridgedTypesPhase
import co.touchlab.skie.api.phases.SkieModuleConfigurationPhase
import co.touchlab.skie.api.phases.memberconflicts.FixCallableMembersConflictsPhase
import co.touchlab.skie.api.phases.memberconflicts.RemoveKonanManglingPhase
import co.touchlab.skie.api.phases.typeconflicts.AddForwardDeclarationsPhase
import co.touchlab.skie.api.phases.typeconflicts.AddTypeDefPhase
import co.touchlab.skie.api.phases.typeconflicts.ObjCTypeRenderer
import co.touchlab.skie.plugin.api.descriptorProvider
import co.touchlab.skie.plugin.api.model.MutableSwiftModelScope
import co.touchlab.skie.plugin.api.util.FrameworkLayout
import org.jetbrains.kotlin.backend.common.CommonBackendContext

class SkieLinkingPhaseScheduler(
    skieModule: DefaultSkieModule,
    context: CommonBackendContext,
    framework: FrameworkLayout,
    swiftModelScope: MutableSwiftModelScope,
) {

    private val objCTypeRenderer = ObjCTypeRenderer()

    private val linkingPhases = listOf(
        RemoveKonanManglingPhase(skieModule, context.descriptorProvider),
        FixCallableMembersConflictsPhase(skieModule, context.descriptorProvider),
        FixClassesConflictsPhase(skieModule, context.descriptorProvider),
        FixNestedBridgedTypesPhase(skieModule, context.descriptorProvider),
        FixHeaderFilePropertyOrderingPhase(framework.kotlinHeader),
        SkieModuleConfigurationPhase(skieModule, swiftModelScope),
        ApiNotesGenerationPhase(swiftModelScope, objCTypeRenderer, context, framework),
        AddForwardDeclarationsPhase(framework.kotlinHeader),
        AddTypeDefPhase(framework.kotlinHeader, objCTypeRenderer),
    )

    fun runLinkingPhases() {
        linkingPhases.forEach {
            it.execute()
        }
    }
}