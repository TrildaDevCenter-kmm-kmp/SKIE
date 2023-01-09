package co.touchlab.skie.api.apinotes.builder

import co.touchlab.skie.plugin.api.module.SkieModule

class ApiNotes(
    private val moduleName: String,
    private val classes: List<ApiNotesType>,
    private val protocols: List<ApiNotesType>,
) {

    fun withoutBridging(): ApiNotes =
        ApiNotes(
            moduleName = moduleName,
            classes = classes.map { it.withoutBridging() },
            protocols = protocols.map { it.withoutBridging() },
        )

    fun createApiNotesFileContent(): String = SmartStringBuilder {
        +"Name: \"$moduleName\""

        if (classes.isNotEmpty()) {
            +"Classes:"
            classes.forEach {
                it.appendApiNote()
            }
        }

        if (protocols.isNotEmpty()) {
            +"Protocols:"
            protocols.forEach {
                it.appendApiNote()
            }
        }
    }

    fun createTypeAliasesForBridgingFile(skieModule: SkieModule) {
        skieModule.file("SkieTypeAliasesForBridging") {
            (classes + protocols).forEach {
                it.appendTypeAliasForBridgingIfNeeded()
            }
        }
    }
}