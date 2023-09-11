package co.touchlab.skie.phases.features.sealed

import co.touchlab.skie.configuration.SealedInterop
import co.touchlab.skie.phases.SkieContext
import co.touchlab.skie.swiftmodel.MutableSwiftModelScope
import co.touchlab.skie.swiftmodel.type.KotlinClassSwiftModel
import co.touchlab.skie.phases.BaseGenerator

internal class SealedInteropGenerator(
    skieContext: SkieContext,
) : BaseGenerator(skieContext), SealedGeneratorExtensionContainer {

    override val isActive: Boolean = true

    private val sealedEnumGeneratorDelegate = SealedEnumGeneratorDelegate(skieContext)
    private val sealedFunctionGeneratorDelegate = SealedFunctionGeneratorDelegate(skieContext)

    override fun runObjcPhase() {
        module.configure {
            exposedClasses
                .filter { it.isSupported }
                .forEach {
                    generate(it)
                }
        }
    }

    private val KotlinClassSwiftModel.isSupported: Boolean
        get() = this.isSealed && this.isSealedInteropEnabled

    private val KotlinClassSwiftModel.isSealedInteropEnabled: Boolean
        get() = this.getConfiguration(SealedInterop.Enabled)

    context(MutableSwiftModelScope)
    private fun generate(swiftModel: KotlinClassSwiftModel) {
        val enum = sealedEnumGeneratorDelegate.generate(swiftModel)

        sealedFunctionGeneratorDelegate.generate(swiftModel, enum)
    }
}