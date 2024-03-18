package co.touchlab.skie.phases.swift

import co.touchlab.skie.phases.SirPhase
import co.touchlab.skie.sir.element.SirSourceFile
import co.touchlab.skie.util.cache.writeTextIfDifferent
import co.touchlab.skie.util.directory.SkieBuildDirectory
import java.io.File
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.pathString

// WIP Extra phase for generating debug files, this should convert and write to cache directory only files that already do not have a object file associated from cache
object ConvertSirSourceFilesToCompilableFilesPhase : SirPhase {

    context(SirPhase.Context)
    override fun execute() {
        val cacheAwareFileGenerator = CacheAwareFileGenerator(skieBuildDirectory.swift.generated)

        with(cacheAwareFileGenerator) {
            sirProvider.skieModuleFiles
                .filterIsInstance<SirSourceFile>()
                .forEach {
                    it.convertToCompilableFile()
                }
        }

        cacheAwareFileGenerator.deleteOldFiles()
    }

    private class CacheAwareFileGenerator(
        private val generatedSwiftDirectory: SkieBuildDirectory.Swift.Generated,
    ) {

        private val generatedFiles = mutableSetOf<File>()

        context(SirPhase.Context)
        fun SirSourceFile.convertToCompilableFile() {
            val file = generatedSwiftDirectory.directory.resolve(relativePath.pathString)

            file.parentFile.mkdirs()

            // WIP Can be written directly after adding support for proper incremental compilation
            file.writeTextIfDifferent(content)

            generatedFiles.add(file)

            sirFileProvider.createCompilableFile(this, file.toPath().toAbsolutePath())
        }

        fun deleteOldFiles() {
            deleteNonGeneratedFiles()
            deleteEmptyDirectories()
        }

        private fun deleteNonGeneratedFiles() {
            generatedSwiftDirectory.directory
                .walkTopDown()
                .filter { it.isFile }
                .filterNot { generatedFiles.contains(it) }
                .forEach { it.delete() }
        }

        private fun deleteEmptyDirectories() {
            generatedSwiftDirectory.directory
                .walkTopDown()
                .filter { it.isDirectory }
                .filter { it.toPath().listDirectoryEntries().isEmpty() }
                .forEach { it.deleteRecursively() }
        }
    }
}