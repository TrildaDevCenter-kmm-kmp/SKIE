package co.touchlab.skie.plugin.shim

import co.touchlab.skie.util.directory.FrameworkLayout
import org.gradle.api.Task

interface FatFrameworkTaskShim {

    val task: Task

    var baseName: String

    val targetFrameworkLayout: FrameworkLayout

    val frameworks: List<FrameworkShim>
}
