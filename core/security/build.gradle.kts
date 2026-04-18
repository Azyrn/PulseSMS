plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.skeler.pulse.core.security"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":core:contracts"))
    implementation(project(":core:common"))
    implementation(project(":core:observability"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}
