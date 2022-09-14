package co.touchlab.swiftgen.configuration

import co.touchlab.swiftgen.configuration.util.throwIfNull
import kotlinx.serialization.Serializable

@Serializable
sealed interface ConfigurationKey<T> {

    val name: kotlin.String

    val defaultValue: T

    fun getAnnotationValue(configurationTarget: ConfigurationTarget): T?

    fun deserialize(value: kotlin.String?): T

    fun serialize(value: T): kotlin.String? =
        value?.toString()

    @Serializable
    sealed interface String : ConfigurationKey<kotlin.String> {

        override fun deserialize(value: kotlin.String?): kotlin.String =
            value.throwIfNull()
    }

    @Serializable
    sealed interface Boolean : ConfigurationKey<kotlin.Boolean> {

        override fun deserialize(value: kotlin.String?): kotlin.Boolean =
            value.throwIfNull().toBooleanStrict()
    }

    @Serializable
    sealed interface OptionalString : ConfigurationKey<kotlin.String?> {

        override fun deserialize(value: kotlin.String?): kotlin.String? = value
    }
}
