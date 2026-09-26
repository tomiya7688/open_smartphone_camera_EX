plugins {
    id("com.android.library")
}

android {
    namespace = "io.github.tomiya7688.opensmartphonecamera.platform.android"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":capture-api"))
}
