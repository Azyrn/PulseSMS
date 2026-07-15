plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
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

val releaseStoreFile = providers.gradleProperty("pulseReleaseStoreFile").orElse("").get().trim()
val releaseStorePassword = providers.gradleProperty("pulseReleaseStorePassword").orElse("").get().trim()
val releaseKeyAlias = providers.gradleProperty("pulseReleaseKeyAlias").orElse("").get().trim()
val releaseKeyPassword = providers.gradleProperty("pulseReleaseKeyPassword").orElse("").get().trim()
val hasReleaseKeystoreConfig = releaseStoreFile.isNotEmpty() &&
    releaseStorePassword.isNotEmpty() &&
    releaseKeyAlias.isNotEmpty() &&
    releaseKeyPassword.isNotEmpty()

android {
    namespace = "com.skeler.pulse"
    // IMPORTANT NOTE: Updated to API 37 to clear lint's newer SDK warning; verify target API 37 behavior before release.
    compileSdk {
        version = release(37)
    }

    signingConfigs {
        if (hasReleaseKeystoreConfig) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    defaultConfig {
        applicationId = "com.skeler.pulse"
        // IMPORTANT NOTE: Lifecycle 2.10.0 requires minSdk 23; this app stays at its existing 24 rather than lowering to 21.
        minSdk = 24
        targetSdk = 37
        versionCode = 10300
        versionName = "1.3.0"
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
        // Only register the release variant when keystore config is present.
        // Without it, assembleRelease fails at configuration time with a clear message.
        if (hasReleaseKeystoreConfig) {
            release {
                require(pulseSyncEnvironment == "prod") {
                    "Release build requires pulseSyncEnvironment=prod (got: $pulseSyncEnvironment). " +
                    "Set -PpulseSyncEnvironment=prod"
                }
                require(pulseSyncProdBaseUrl.startsWith("https://")) {
                    "Release build requires HTTPS base URL. Set pulseSyncProdBaseUrl to an https:// URL."
                }
                require(pulseSyncProdApiKey.isNotEmpty()) {
                    "Release build requires a non-empty pulseSyncProdApiKey. " +
                    "Set -PpulseSyncProdApiKey=<key>"
                }
                signingConfig = signingConfigs.getByName("release")
                isMinifyEnabled = false
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            }
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
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // IMPORTANT NOTE: Stable Material 3 Adaptive is 1.2.0; 1.3.0-beta02 is intentionally not used.
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    implementation(project(":core:database"))
    implementation(project(":core:design"))
    implementation(project(":core:observability"))
    implementation(project(":core:security"))
    implementation(project(":feature:messaging"))
    implementation(project(":feature:sync"))
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
