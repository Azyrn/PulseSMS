package com.skeler.pulse.config

import com.skeler.pulse.BuildConfig
import com.skeler.pulse.sync.data.SyncEndpointConfig

data class AppSyncRuntimeConfig(
    val environmentName: String,
    val endpointConfig: SyncEndpointConfig,
)

data class SyncEnvironmentProfile(
    val environmentName: String,
    val baseUrl: String,
    val apiKey: String?,
)

data class SyncEnvironmentProfiles(
    val dev: SyncEnvironmentProfile,
    val staging: SyncEnvironmentProfile,
    val prod: SyncEnvironmentProfile,
    val connectTimeoutMillis: Int,
    val readTimeoutMillis: Int,
)

object SyncRuntimeConfigProvider {
    fun fromBuildConfig(): AppSyncRuntimeConfig = resolve(
        environmentName = BuildConfig.PULSE_SYNC_ENVIRONMENT,
        profiles = SyncEnvironmentProfiles(
            dev = SyncEnvironmentProfile(
                environmentName = "dev",
                baseUrl = BuildConfig.PULSE_SYNC_DEV_BASE_URL,
                apiKey = BuildConfig.PULSE_SYNC_DEV_API_KEY.ifBlank { null },
            ),
            staging = SyncEnvironmentProfile(
                environmentName = "staging",
                baseUrl = BuildConfig.PULSE_SYNC_STAGING_BASE_URL,
                apiKey = BuildConfig.PULSE_SYNC_STAGING_API_KEY.ifBlank { null },
            ),
            prod = SyncEnvironmentProfile(
                environmentName = "prod",
                baseUrl = BuildConfig.PULSE_SYNC_PROD_BASE_URL,
                apiKey = BuildConfig.PULSE_SYNC_PROD_API_KEY.ifBlank { null },
            ),
            connectTimeoutMillis = BuildConfig.PULSE_SYNC_CONNECT_TIMEOUT_MILLIS,
            readTimeoutMillis = BuildConfig.PULSE_SYNC_READ_TIMEOUT_MILLIS,
        ),
    )

    internal fun resolve(
        environmentName: String,
        profiles: SyncEnvironmentProfiles,
    ): AppSyncRuntimeConfig {
        val profile = when (environmentName.trim().lowercase()) {
            "prod", "production" -> profiles.prod
            "staging", "stage" -> profiles.staging
            else -> profiles.dev
        }
        return AppSyncRuntimeConfig(
            environmentName = profile.environmentName,
            endpointConfig = SyncEndpointConfig(
                baseUrl = profile.baseUrl,
                apiKey = profile.apiKey,
                connectTimeoutMillis = profiles.connectTimeoutMillis,
                readTimeoutMillis = profiles.readTimeoutMillis,
            ),
        )
    }
}
