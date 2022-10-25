package co.touchlab.skie.configuration

import co.touchlab.skie.configuration.util.throwIfNull
import co.touchlab.skie.configuration.values.ValidationSeverity
import co.touchlab.skie.configuration.DefaultArgumentInterop as DefaultArgumentInteropAnnotations
import co.touchlab.skie.configuration.EnumInterop as EnumInteropAnnotations
import co.touchlab.skie.configuration.ExperimentalFeatures as ExperimentalFeaturesAnnotations
import co.touchlab.skie.configuration.SealedInterop as SealedInteropAnnotations

object ConfigurationKeys {

    object ExperimentalFeatures {

        /**
         * If true, enables experimental features which might not be fully implemented yet which may cause compilation problems.
         */
        object Enabled : ConfigurationKey.Boolean {

            override val name: String = "ExperimentalFeatures.Enabled"

            override val defaultValue: Boolean = false

            override fun getAnnotationValue(configurationTarget: ConfigurationTarget): Boolean? =
                when {
                    configurationTarget.hasAnnotation<ExperimentalFeaturesAnnotations.Enabled>() -> true
                    configurationTarget.hasAnnotation<ExperimentalFeaturesAnnotations.Disabled>() -> false
                    else -> null
                }
        }
    }

    object Validation {

        /**
         * The severity of messages thrown by the compiler plugin as a result of validation error.
         */
        object Severity : ConfigurationKey<ValidationSeverity> {

            override val name: String = "Validation.Severity"

            override val defaultValue: ValidationSeverity = ValidationSeverity.Error

            override fun getAnnotationValue(configurationTarget: ConfigurationTarget): ValidationSeverity? =
                null

            override fun deserialize(value: String?): ValidationSeverity =
                ValidationSeverity.valueOf(value.throwIfNull())
        }
    }

    object DefaultArgumentInterop {

        object Enabled : ConfigurationKey.Boolean {

            override val name: String = "DefaultArgumentInterop.Enabled"

            override val defaultValue: Boolean = true

            override fun getAnnotationValue(configurationTarget: ConfigurationTarget): Boolean? =
                when {
                    configurationTarget.hasAnnotation<DefaultArgumentInteropAnnotations.Enabled>() -> true
                    configurationTarget.hasAnnotation<DefaultArgumentInteropAnnotations.Disabled>() -> false
                    else -> null
                }
        }

        object MaximumDefaultArgumentCount : ConfigurationKey.Int {

            override val name: String = "DefaultArgumentInterop.MaximumDefaultArgumentCount"

            override val defaultValue: Int = 5

            override fun getAnnotationValue(configurationTarget: ConfigurationTarget): Int? =
                configurationTarget.findAnnotation<DefaultArgumentInteropAnnotations.MaximumDefaultArgumentCount>()?.count
        }
    }

    object SealedInterop {

        /**
         * If true, the interop code is generated for the given sealed class/interface.
         */
        object Enabled : ConfigurationKey.Boolean {

            override val name: String = "SealedInterop.Enabled"

            override val defaultValue: Boolean = true

            override fun getAnnotationValue(configurationTarget: ConfigurationTarget): Boolean? =
                when {
                    configurationTarget.hasAnnotation<SealedInteropAnnotations.Enabled>() -> true
                    configurationTarget.hasAnnotation<SealedInteropAnnotations.Disabled>() -> false
                    else -> null
                }
        }

        object Function {

            /**
             * The name for the function used inside `switch`.
             */
            object Name : ConfigurationKey.String {

                override val name: String = "SealedInterop.Function.Name"

                override val defaultValue: String = "onEnum"

                override fun getAnnotationValue(configurationTarget: ConfigurationTarget): String? =
                    configurationTarget.findAnnotation<SealedInteropAnnotations.Function.Name>()?.name
            }

            /**
             * The argument label for the function used inside `switch`.
             * Disable the argument label by passing "_".
             * No argumentLabel is generated if the name is empty.
             */
            object ArgumentLabel : ConfigurationKey.String {

                override val name: String = "SealedInterop.Function.ArgumentLabel"

                override val defaultValue: String = "of"

                override fun getAnnotationValue(configurationTarget: ConfigurationTarget): String? =
                    configurationTarget.findAnnotation<SealedInteropAnnotations.Function.ArgumentLabel>()?.argumentLabel
            }

            /**
             * The parameter name for the function used inside `switch`.
             */
            object ParameterName : ConfigurationKey.String {

                override val name: String = "SealedInterop.Function.ParameterName"

                override val defaultValue: String = "sealed"

                override fun getAnnotationValue(configurationTarget: ConfigurationTarget): String? =
                    configurationTarget.findAnnotation<SealedInteropAnnotations.Function.ParameterName>()?.parameterName
            }
        }

        /**
         * The name for the custom `else` case that is generated if some children are hidden / not accessible from Swift.
         */
        object ElseName : ConfigurationKey.String {

            override val name: String = "SealedInterop.ElseName"

            override val defaultValue: String = "Else"

            override fun getAnnotationValue(configurationTarget: ConfigurationTarget): String? =
                configurationTarget.findAnnotation<SealedInteropAnnotations.ElseName>()?.elseName
        }

        object Case {

            /**
             * If false, given subclass will be hidden from the generated code, which means no dedicated enum case will be generated for it.
             */
            object Visible : ConfigurationKey.Boolean {

                override val name: String = "SealedInterop.Case.Visible"

                override val defaultValue: Boolean = true

                override fun getAnnotationValue(configurationTarget: ConfigurationTarget): Boolean? =
                    when {
                        configurationTarget.hasAnnotation<SealedInteropAnnotations.Case.Visible>() -> true
                        configurationTarget.hasAnnotation<SealedInteropAnnotations.Case.Hidden>() -> false
                        else -> null
                    }
            }

            /**
             * Overrides the name of the enum case generated for given subclass.
             */
            object Name : ConfigurationKey.OptionalString {

                override val name: String = "SealedInterop.Case.Name"

                override val defaultValue: String? = null

                override fun getAnnotationValue(configurationTarget: ConfigurationTarget): String? =
                    configurationTarget.findAnnotation<SealedInteropAnnotations.Case.Name>()?.name
            }
        }
    }

    object EnumInterop {

        /**
         * If true, the interop code is generated for the given enum.
         */
        object Enabled : ConfigurationKey.Boolean {

            override val name: String = "EnumInterop.Enabled"

            override val defaultValue: Boolean = true

            override fun getAnnotationValue(configurationTarget: ConfigurationTarget): Boolean? =
                when {
                    configurationTarget.hasAnnotation<EnumInteropAnnotations.Enabled>() -> true
                    configurationTarget.hasAnnotation<EnumInteropAnnotations.Disabled>() -> false
                    else -> null
                }
        }
    }
}