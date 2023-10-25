package co.touchlab.skie.phases.header

import co.touchlab.skie.oir.element.OirClass
import co.touchlab.skie.oir.element.OirModule
import co.touchlab.skie.oir.element.renderForwardDeclaration
import co.touchlab.skie.phases.SirPhase
import co.touchlab.skie.util.cache.writeTextIfDifferent

object GenerateFakeObjCDependenciesPhase : SirPhase {

    context(SirPhase.Context)
    override fun execute() {
        oirProvider.allExternalClassesAndProtocols
            .filter { it.module.name != "Foundation" }
            .groupBy { it.module }
            .forEach { (module, types) ->
                generateFakeFramework(module, types)
            }
    }

    context(SirPhase.Context)
    private fun generateFakeFramework(module: OirModule, classes: List<OirClass>) {
        generateModuleMap(module)
        generateHeader(module, classes)
    }

    context(SirPhase.Context)
    private fun generateModuleMap(module: OirModule) {
        val moduleMapContent =
            """
            framework module ${module.name} {
                umbrella header "${module.name}.h"
            }
        """.trimIndent()

        skieBuildDirectory.swiftCompiler.fakeObjCFrameworks.moduleMap(module.name).writeTextIfDifferent(moduleMapContent)
    }

    context(SirPhase.Context)
    private fun generateHeader(module: OirModule, classes: List<OirClass>) {
        val foundationImport = "#import <Foundation/NSObject.h>"

        val declarations = classes
            .sortedBy { it.name }
            .joinToString("\n") { it.getHeaderEntry() }

        val headerContent = "$foundationImport\n\n$declarations"

        skieBuildDirectory.swiftCompiler.fakeObjCFrameworks.header(module.name).writeTextIfDifferent(headerContent)
    }
}

private fun OirClass.getHeaderEntry(): String =
    when (kind) {
        OirClass.Kind.Class -> getClassHeaderEntry()
        OirClass.Kind.Protocol -> getProtocolHeaderEntry()
    }

private fun OirClass.getClassHeaderEntry(): String =
    "@interface ${renderForwardDeclaration()} : NSObject @end"

private fun OirClass.getProtocolHeaderEntry(): String =
    "@protocol ${renderForwardDeclaration()} @end"
