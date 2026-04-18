package com.skeler.pulse.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SyncRuntimeConfigProviderTest {
    @Test
    fun `resolve selects staging profile when requested`() {
        val config = SyncRuntimeConfigProvider.resolve(
            environmentName = "staging",
            profiles = profiles(),
        )

        assertEquals("staging", config.environmentName)
        assertEquals("https://staging.example/api/v1", config.endpointConfig.baseUrl)
        assertEquals("staging-key", config.endpointConfig.apiKey)
        assertEquals(7_000, config.endpointConfig.connectTimeoutMillis)
    }

    @Test
    fun `resolve falls back to dev for unknown environment`() {
        val config = SyncRuntimeConfigProvider.resolve(
            environmentName = "qa",
            profiles = profiles(),
        )

        assertEquals("dev", config.environmentName)
        assertEquals("http://10.0.2.2:8080/api/v1", config.endpointConfig.baseUrl)
        assertNull(config.endpointConfig.apiKey)
    }

    @Test
    fun `resolve normalizes production aliases`() {
        val config = SyncRuntimeConfigProvider.resolve(
            environmentName = "production",
            profiles = profiles(),
        )

        assertEquals("prod", config.environmentName)
        assertEquals("https://api.example/api/v1", config.endpointConfig.baseUrl)
    }

    private fun profiles(): SyncEnvironmentProfiles = SyncEnvironmentProfiles(
        dev = SyncEnvironmentProfile(
            environmentName = "dev",
            baseUrl = "http://10.0.2.2:8080/api/v1",
            apiKey = null,
        ),
        staging = SyncEnvironmentProfile(
            environmentName = "staging",
            baseUrl = "https://staging.example/api/v1",
            apiKey = "staging-key",
        ),
        prod = SyncEnvironmentProfile(
            environmentName = "prod",
            baseUrl = "https://api.example/api/v1",
            apiKey = "prod-key",
        ),
        connectTimeoutMillis = 7_000,
        readTimeoutMillis = 8_000,
    )
}
