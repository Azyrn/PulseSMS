plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.skeler.pulse.core.design"
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

    buildFeatures {
        compose = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    // Material 3 Expressive — pinned outside BOM
    implementation(libs.androidx.compose.material3.expressive)
    // DataStore for theme preferences
    implementation(libs.androidx.datastore.preferences)
    // ViewModel Compose integration
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // Animation graphics (animated vector drawables)
    implementation(libs.androidx.compose.animation.graphics)
    // Graphics shapes (RoundedPolygon, Morph)
    implementation(libs.androidx.graphics.shapes)
    // MaterialKolor — dynamic ColorScheme from seed color
    implementation(libs.material.kolor)
    // Coil Compose — async image loading for avatars
    implementation(libs.coil.compose)
    testImplementation(libs.junit)
}
