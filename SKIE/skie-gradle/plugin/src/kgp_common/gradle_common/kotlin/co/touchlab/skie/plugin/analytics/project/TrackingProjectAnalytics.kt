package co.touchlab.skie.plugin.analytics.project

import co.touchlab.skie.configuration.SkieFeature
import co.touchlab.skie.plugin.analytics.AnalyticsProducer
import co.touchlab.skie.plugin.util.toPrettyJson
import co.touchlab.skie.util.hashed
import org.gradle.api.Project

internal data class TrackingProjectAnalytics(
    val rootProjectNameHash: String,
    val rootProjectPathHash: String,
    val projectFullNameHash: String,
) {

    class Producer(private val project: Project) : AnalyticsProducer {

        override val feature: SkieFeature = SkieFeature.Analytics_Tracking_Project

        override val name: String = "tracking-project"

        override fun produce(): String =
            project.getIdentifyingProjectAnalytics()
                .let {
                    TrackingProjectAnalytics(
                        rootProjectNameHash = it.rootProjectName.hashed(),
                        rootProjectPathHash = it.rootProjectPath.hashed(),
                        projectFullNameHash = it.projectFullName.hashed(),
                    )
                }
                .toPrettyJson()
    }
}