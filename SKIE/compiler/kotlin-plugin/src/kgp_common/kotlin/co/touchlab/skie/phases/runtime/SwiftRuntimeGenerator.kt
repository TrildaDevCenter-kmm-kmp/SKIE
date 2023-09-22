package co.touchlab.skie.phases.runtime

import co.touchlab.skie.configuration.SkieConfigurationFlag
import co.touchlab.skie.phases.SkieContext
import co.touchlab.skie.phases.SkieCompilationPhase

internal class SwiftRuntimeGenerator(
    private val skieContext: SkieContext,
) : SkieCompilationPhase {

    override val isActive: Boolean =
        SkieConfigurationFlag.Feature_CoroutinesInterop in skieContext.skieConfiguration.enabledConfigurationFlags

    override fun runObjcPhase() {
        getSwiftRuntimeFiles().forEach {
            skieContext.module.staticFile(it.swiftFileName) {
                it.readText()
            }
        }
    }

    private fun getSwiftRuntimeFiles(): List<Resource> =
        Resource("co/touchlab/skie/runtime/index.txt")
            .readText()
            .lines()
            .filter { it.isNotBlank() }
            .map { Resource(it) }

    private val Resource.swiftFileName: String
        get() = this.name.substringAfterLast("/").removeSuffix(".swift")

    private class Resource(val name: String) {

        private val resourceUri = this::class.java.classLoader.getResource(name)
            ?: throw IllegalArgumentException("Resource $name not found.")

        fun readText(): String =
            resourceUri.readText()
    }
}
