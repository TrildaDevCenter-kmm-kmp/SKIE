package co.touchlab.skie.configuration.annotations

@Target
annotation class EnumInterop {

    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.BINARY)
    annotation class Enabled

    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.BINARY)
    annotation class Disabled

    /**
     * See [co.touchlab.skie.configuration.EnumInterop.LegacyCaseName]
     */
    @Target
    annotation class LegacyCaseName {

        @Target(AnnotationTarget.CLASS)
        @Retention(AnnotationRetention.BINARY)
        annotation class Enabled

        @Target(AnnotationTarget.CLASS)
        @Retention(AnnotationRetention.BINARY)
        annotation class Disabled
    }
}
