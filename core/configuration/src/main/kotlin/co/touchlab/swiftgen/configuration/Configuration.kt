package co.touchlab.swiftgen.configuration

import co.touchlab.swiftgen.BuildConfig
import co.touchlab.swiftgen.configuration.builder.ConfigurationBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class Configuration(
    private val groups: List<Group>,
) {

    operator fun <T> get(target: ConfigurationTarget, key: ConfigurationKey<T>): T {
        val group = findGroup(target)

        val configurationValue = group?.findValue(key) ?: key.defaultValue

        return if (group?.overridesAnnotations == true) {
            configurationValue
        } else {
            key.getAnnotationValue(target) ?: configurationValue
        }
    }

    private fun findGroup(target: ConfigurationTarget): Group? =
        groups.lastOrNull { target.fqName.startsWith(it.targetFqNamePrefix) }

    private fun <T> Group.findValue(key: ConfigurationKey<T>): T? =
        this.items[key.name]?.let { key.deserialize(it) }

    fun serialize(): String {
        val json = Json { prettyPrint = true }

        return json.encodeToString(groups)
    }

    operator fun plus(other: Configuration): Configuration =
        Configuration(groups + other.groups)

    @Serializable
    data class Group(
        @SerialName("target")
        val targetFqNamePrefix: String,
        val overridesAnnotations: Boolean,
        val items: Map<String, String?>,
    )

    companion object {

        const val CliPluginId: String = BuildConfig.PLUGIN_ID
        const val CliOptionKey: String = "config"

        operator fun invoke(builder: ConfigurationBuilder.() -> Unit): Configuration =
            ConfigurationBuilder().also(builder).build()

        operator fun invoke(builder: ConfigurationBuilder): Configuration =
            builder.build()

        fun deserialize(string: String): Configuration {
            val groups = Json.decodeFromString<List<Group>>(string)

            return Configuration(groups)
        }
    }
}
