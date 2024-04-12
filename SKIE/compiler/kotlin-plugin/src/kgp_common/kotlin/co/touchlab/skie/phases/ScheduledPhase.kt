package co.touchlab.skie.phases

import co.touchlab.skie.phases.util.StatefulScheduledPhase
import co.touchlab.skie.util.SwiftCompilerConfiguration

interface ScheduledPhase<C : ScheduledPhase.Context> {

    context(C)
    fun isActive(): Boolean = true

    context(C)
    suspend fun execute()

    interface Context : CommonSkieContext {

        override val context: Context

        val swiftCompilerConfiguration: SwiftCompilerConfiguration

        fun launch(action: suspend () -> Unit)

        fun <CONTEXT : Context> storeStatefulScheduledPhaseBody(phase: StatefulScheduledPhase<CONTEXT>, action: CONTEXT.() -> Unit)

        fun <CONTEXT : Context> executeStatefulScheduledPhase(phase: StatefulScheduledPhase<CONTEXT>, context: CONTEXT)
    }
}
