@file:Suppress("invisible_reference", "invisible_member")

package co.touchlab.skie.compilerinject.interceptor

import org.jetbrains.kotlin.config.CompilerConfiguration

internal actual fun phaseInterceptorConfigurers(configuration: CompilerConfiguration): List<ErasedPhaseInterceptorConfigurer> {
    return listOf(
        SameTypeNamedPhaseInterceptorConfigurer(),
        SimpleNamedPhaseInterceptorConfigurer(),
    )
}
