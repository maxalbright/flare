pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.namespace == "com.android") {
                useModule("com.android.tools.build:gradle:7.0.3")
            }
        }
    }
}
include(":flare", ":sampleShared", ":sampleIosApp", ":sampleAndroidApp")
rootProject.name = "flare"

