package co.touchlab.swiftgen.plugin.internal.util

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.cli.jvm.compiler.report
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi

class Reporter(private val compilerConfiguration: CompilerConfiguration) {

    fun report(severity: CompilerMessageSeverity, message: String, declaration: DeclarationDescriptor?) {
        val location = MessageUtil.psiElementToMessageLocation(declaration?.findPsi())?.let {
            CompilerMessageLocation.create(it.path, it.line, it.column, it.lineContent)
        }

        compilerConfiguration.report(severity, message, location)
    }
}