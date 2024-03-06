import co.touchlab.skie.gradle.publish.publishCode

plugins {
    id("skie.runtime")
    id("skie.publishable")
}

skiePublishing {
    name = "SKIE Configuration Annotations"
    description = "Annotations to configure SKIE behavior."
    publishSources = true
}

kotlin {
    jvm()
    js(BOTH) {
        browser()
        nodejs()
    }

    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()

    iosArm32()
    iosArm64()
    iosX64()
    iosSimulatorArm64()

    watchosArm32()
    watchosArm64()
    watchosX86()
    watchosX64()
    watchosSimulatorArm64()
    watchosDeviceArm64()

    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()

    macosX64()
    macosArm64()

    linuxArm64()
    linuxArm32Hfp()
    linuxX64()

    mingwX64()
    mingwX86()

    wasm32()
}
