package co.touchlab.skie.buildsetup.plugins

import com.github.gmazzo.gradle.plugins.BuildConfigExtension
import com.github.gmazzo.gradle.plugins.BuildConfigPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

abstract class DevBuildconfig : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        apply<BuildConfigPlugin>()

        extensions.configure<BuildConfigExtension> {
            packageName(("${project.group}.${project.name}").replace("-", "_"))
        }
    }
}
