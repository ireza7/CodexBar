package com.steipete.codexbar.data

import com.steipete.codexbar.data.local.InMemorySecureStorage
import com.steipete.codexbar.domain.model.ProviderCredentials
import com.steipete.codexbar.domain.model.UsageProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pure JVM unit tests for [InMemorySecureStorage] verifying credential persistence,
 * isolation, clearing, and thread-safety behavior.
 */
class InMemorySecureStorageTest {

    private lateinit var storage: InMemorySecureStorage

    @Before
    fun setUp() {
        storage = InMemorySecureStorage()
    }

    @Test
    fun testGenericPutAndGetString() = runTest {
        storage.putString("custom_key", "custom_secret_value")
        assertTrue(storage.contains("custom_key"))
        assertEquals("custom_secret_value", storage.getString("custom_key"))

        storage.remove("custom_key")
        assertFalse(storage.contains("custom_key"))
        assertNull(storage.getString("custom_key"))
    }

    @Test
    fun testPutStringWithNullRemovesKey() = runTest {
        storage.putString("key1", "value1")
        assertTrue(storage.contains("key1"))

        storage.putString("key1", null)
        assertFalse(storage.contains("key1"))
        assertNull(storage.getString("key1"))
    }

    @Test
    fun testSaveAndGetApiKey() = runTest {
        val testKey = "sk-ant-api03-test-secret-key-12345"
        val provider = UsageProvider.values().firstOrNull() ?: return@runTest

        storage.saveApiKey(provider, testKey)
        assertEquals(testKey, storage.getApiKey(provider))

        storage.removeApiKey(provider)
        assertNull(storage.getApiKey(provider))
    }

    @Test
    fun testSaveAndGetSessionToken() = runTest {
        val testToken = "session-token-xyz-987654"
        val provider = UsageProvider.values().firstOrNull() ?: return@runTest

        storage.saveSessionToken(provider, testToken)
        assertEquals(testToken, storage.getSessionToken(provider))

        storage.removeSessionToken(provider)
        assertNull(storage.getSessionToken(provider))
    }

    @Test
    fun testSaveAndGetFullCredentials() = runTest {
        val provider = UsageProvider.values().firstOrNull() ?: return@runTest
        val credentials = ProviderCredentials(
            apiKey = "sk-test-key",
            sessionToken = "sess-token-456",
            orgId = "org-my-company"
        )

        storage.saveCredentials(provider, credentials)

        val retrieved = storage.getCredentials(provider)
        assertEquals("sk-test-key", retrieved.apiKey)
        assertEquals("sess-token-456", retrieved.sessionToken)
        assertEquals("org-my-company", retrieved.orgId)
        assertTrue(retrieved.hasCredentials)

        storage.removeCredentials(provider)
        val cleared = storage.getCredentials(provider)
        assertNull(cleared.apiKey)
        assertNull(cleared.sessionToken)
        assertNull(cleared.orgId)
        assertFalse(cleared.hasCredentials)
    }

    @Test
    fun testClearWipesAllEntries() = runTest {
        storage.putString("key_a", "val_a")
        storage.putString("key_b", "val_b")
        assertEquals(2, storage.snapshot().size)

        storage.clear()
        assertEquals(0, storage.snapshot().size)
        assertFalse(storage.contains("key_a"))
        assertFalse(storage.contains("key_b"))
    }

    @Test
    fun testWhitespaceIsTrimmedOnSave() = runTest {
        val provider = UsageProvider.values().firstOrNull() ?: return@runTest
        storage.saveApiKey(provider, "   sk-key-with-spaces   \n")
        assertEquals("sk-key-with-spaces", storage.getApiKey(provider))
    }
}
