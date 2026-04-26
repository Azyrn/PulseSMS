plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

fun quoted(value: String): String = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

val pulseSyncEnvironment = providers.gradleProperty("pulseSyncEnvironment").orElse("dev").get()
val pulseSyncDevBaseUrl = providers.gradleProperty("pulseSyncDevBaseUrl").orElse("http://10.0.2.2:8080/api/v1").get()
val pulseSyncStagingBaseUrl = providers.gradleProperty("pulseSyncStagingBaseUrl").orElse("https://staging.api.pulse.example/api/v1").get()
val pulseSyncProdBaseUrl = providers.gradleProperty("pulseSyncProdBaseUrl").orElse("https://api.pulse.example/api/v1").get()
val pulseSyncDevApiKey = providers.gradleProperty("pulseSyncDevApiKey").orElse("").get()
val pulseSyncStagingApiKey = providers.gradleProperty("pulseSyncStagingApiKey").orElse("").get()
val pulseSyncProdApiKey = providers.gradleProperty("pulseSyncProdApiKey").orElse("").get()
val pulseSyncConnectTimeoutMillis = providers.gradleProperty("pulseSyncConnectTimeoutMillis").orElse("5000").get()
val pulseSyncReadTimeoutMillis = providers.gradleProperty("pulseSyncReadTimeoutMillis").orElse("5000").get()

android {
    namespace = "com.skeler.pulse"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.skeler.pulse"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "PULSE_SYNC_ENVIRONMENT", quoted(pulseSyncEnvironment))
        buildConfigField("String", "PULSE_SYNC_DEV_BASE_URL", quoted(pulseSyncDevBaseUrl))
        buildConfigField("String", "PULSE_SYNC_STAGING_BASE_URL", quoted(pulseSyncStagingBaseUrl))
        buildConfigField("String", "PULSE_SYNC_PROD_BASE_URL", quoted(pulseSyncProdBaseUrl))
        buildConfigField("String", "PULSE_SYNC_DEV_API_KEY", quoted(pulseSyncDevApiKey))
        buildConfigField("String", "PULSE_SYNC_STAGING_API_KEY", quoted(pulseSyncStagingApiKey))
        buildConfigField("String", "PULSE_SYNC_PROD_API_KEY", quoted(pulseSyncProdApiKey))
        buildConfigField("int", "PULSE_SYNC_CONNECT_TIMEOUT_MILLIS", pulseSyncConnectTimeoutMillis)
        buildConfigField("int", "PULSE_SYNC_READ_TIMEOUT_MILLIS", pulseSyncReadTimeoutMillis)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3.expressive)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(project(":core:database"))
    implementation(project(":core:design"))
    implementation(project(":core:observability"))
    implementation(project(":core:security"))
    implementation(project(":feature:messaging"))
    implementation(project(":feature:sync"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
