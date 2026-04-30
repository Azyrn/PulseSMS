plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.skeler.pulse.core.contracts"
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
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(platform(libs.androidx.compose.bom))
    compileOnly("androidx.compose.runtime:runtime")
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.collections.immutable)
    testImplementation(libs.junit)
}
