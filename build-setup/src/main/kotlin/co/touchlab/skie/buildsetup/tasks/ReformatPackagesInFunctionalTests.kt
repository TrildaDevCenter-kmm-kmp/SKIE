package co.touchlab.skie.buildsetup.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class ReformatPackagesInFunctionalTests : DefaultTask() {

    @InputDirectory
    lateinit var resourcesDirectory: File

    @TaskAction
    fun execute() {
        resourcesDirectory.resolve("tests")
            .walkTopDown()
            .filter { it.extension == "kt" }
            .forEach {
                reformatPackage(it)
            }
    }

    private fun reformatPackage(file: File) {
        val lines = file.readLines()

        val indexOfLineWithPackageDeclaration = lines.indexOfFirst { it.trimStart().startsWith("package ") }

        val modifiedLines = if (indexOfLineWithPackageDeclaration != -1) {
            lines.mapIndexed { index, line ->
                if (index == indexOfLineWithPackageDeclaration) {
                    file.correctPackageDeclaration()
                } else {
                    line
                }
            }
        } else {
            listOf(file.correctPackageDeclaration(), "") + lines
        }

        val modifiedText = modifiedLines.joinToString(System.lineSeparator(), postfix = System.lineSeparator())

        file.writeText(modifiedText)
    }

    private fun File.correctPackageDeclaration(): String {
        val pathComponents = this.getRelativePathComponents(resourcesDirectory)

        val packageName = pathComponents.joinToString(".") { "`$it`" }

        return "package $packageName"
    }

    private fun File.getRelativePathComponents(base: File): List<String> {
        require(this.toPath().startsWith(base.toPath()) && this != base) {
            "File $this must be located inside ${base}."
        }

        val pathComponents = mutableListOf<String>()

        var currentDirectory = this.parentFile
        while (currentDirectory != base) {
            pathComponents.add(currentDirectory.name)

            currentDirectory = currentDirectory.parentFile
        }

        return pathComponents.reversed()
    }
}
