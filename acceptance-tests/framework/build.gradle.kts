import co.touchlab.skie.gradle.util.kotlinNativeCompilerHome

plugins {
    id("skie-jvm")
    id("skie-buildconfig")
}

buildConfig {
    buildConfigField(
        type = "String",
        name = "KONAN_HOME",
        value = "\"${kotlinNativeCompilerHome.path}\"",
    )
    buildConfigField(
        type = "String",
        name = "RESOURCES",
        value = "\"${layout.projectDirectory.dir("src/main/resources").asFile.absolutePath}\"",
    )
}

skieJvm {
    areContextReceiversEnabled.set(true)
}

dependencies {
    api(libs.bundles.testing.jvm)

    implementation(files(kotlinNativeCompilerHome.resolve("konan/lib/trove4j.jar")))
    implementation(libs.kotlin.native.compiler.embeddable)
    implementation("co.touchlab.skie:configuration-api")
    implementation("co.touchlab.skie:generator")
    implementation("co.touchlab.skie:api")
    implementation("co.touchlab.skie:kotlin-plugin")
}
