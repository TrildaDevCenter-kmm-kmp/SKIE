package co.touchlab.skie.test.annotation.type

import co.touchlab.skie.test.runner.TestType

@Target(AnnotationTarget.CLASS)
@SkieTestType(TestType.Gradle)
annotation class GradleTests
