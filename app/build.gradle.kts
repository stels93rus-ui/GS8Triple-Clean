plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ru.gs8triple"
    compileSdk = 35
buildFeatures {
    aidl = true
}
    defaultConfig {
        applicationId = "ru.gs8triple"
        minSdk = 28
        targetSdk = 28
        versionCode = 8
        versionName = "0.7.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
}
