pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "open_smartphone_camera_EX"

include(":capture-api")
include(":processing-api")
include(":platform:android")
include(":app-android")
