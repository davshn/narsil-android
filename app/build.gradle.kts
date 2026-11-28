plugins {
    id("com.android.application")
}

android {
    namespace = "io.github.davshn.narsil"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.davshn.narsil"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.webkit:webkit:1.18.0")
    implementation("androidx.activity:activity:1.13.0")
    testImplementation("junit:junit:4.13.2")
    // Android's org.json is a stub in JVM tests: the real one stands in.
    testImplementation("org.json:json:20260814")
}
