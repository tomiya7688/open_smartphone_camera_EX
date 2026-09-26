plugins {
    id("com.android.application")
}

android {
    namespace = "io.github.tomiya7688.opensmartphonecamera"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.tomiya7688.opensmartphonecamera"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":capture-api"))
    implementation(project(":processing-api"))
    implementation(project(":platform:android"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
}
