package com.steipete.codexbar.data

import android.content.SharedPreferences
import com.steipete.codexbar.data.local.SettingsRepositoryImpl
import com.steipete.codexbar.domain.model.ThemeMode
import com.steipete.codexbar.domain.model.UsageProvider
import com.steipete.codexbar.domain.model.UserSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryImplTest {

    private lateinit var testPrefs: FakeSharedPreferences
    private lateinit var repository: SettingsRepositoryImpl
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        testPrefs = FakeSharedPreferences()
        repository = SettingsRepositoryImpl(testPrefs, testDispatcher)
    }

    @Test
    fun testDefaultSettings() = runTest(testDispatcher) {
        val settings = repository.getSettings()
        assertEquals(UserSettings.DEFAULT_REFRESH_INTERVAL_MINUTES, settings.refreshIntervalMinutes)
        assertTrue(settings.showPaceIndicator)
        assertEquals(ThemeMode.DARK, settings.themeMode)
    }

    @Test
    fun testSetRefreshInterval() = runTest(testDispatcher) {
        repository.setRefreshInterval(30)
        assertEquals(30, repository.getSettings().refreshIntervalMinutes)
        assertEquals(30, repository.getSettingsFlow().first().refreshIntervalMinutes)

        // Test clamping bounds
        repository.setRefreshInterval(0)
        assertEquals(UserSettings.MIN_REFRESH_INTERVAL_MINUTES, repository.getSettings().refreshIntervalMinutes)

        repository.setRefreshInterval(5000)
        assertEquals(UserSettings.MAX_REFRESH_INTERVAL_MINUTES, repository.getSettings().refreshIntervalMinutes)
    }

    @Test
    fun testUpdateThemeMode() = runTest(testDispatcher) {
        repository.updateThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, repository.getSettings().themeMode)
        assertEquals(ThemeMode.LIGHT, repository.getSettingsFlow().first().themeMode)
    }

    @Test
    fun testSetShowPaceIndicator() = runTest(testDispatcher) {
        repository.setShowPaceIndicator(false)
        assertFalse(repository.getSettings().showPaceIndicator)
        assertFalse(repository.getSettingsFlow().first().showPaceIndicator)
    }

    @Test
    fun testUpdateProviderCustomBaseUrl() = runTest(testDispatcher) {
        val provider = UsageProvider.values().firstOrNull() ?: return@runTest
        repository.updateProviderCustomBaseUrl(provider, "https://api.custom-proxy.com/v1")

        val providerSettings = repository.getProviderSettings(provider)
        assertEquals("https://api.custom-proxy.com/v1", providerSettings.customBaseUrl)

        repository.updateProviderCustomBaseUrl(provider, null)
        val clearedSettings = repository.getProviderSettings(provider)
        assertNull(clearedSettings.customBaseUrl)
    }

    @Test
    fun testResetToDefaults() = runTest(testDispatcher) {
        repository.setRefreshInterval(45)
        repository.updateThemeMode(ThemeMode.LIGHT)
        repository.setShowPaceIndicator(false)

        repository.resetToDefaults()

        val settings = repository.getSettings()
        assertEquals(UserSettings.DEFAULT_REFRESH_INTERVAL_MINUTES, settings.refreshIntervalMinutes)
        assertTrue(settings.showPaceIndicator)
        assertEquals(ThemeMode.DARK, settings.themeMode)
    }
}

/**
 * Lightweight in-memory test double for Android [SharedPreferences] enabling pure JVM tests.
 */
class FakeSharedPreferences : SharedPreferences {
    private val data = ConcurrentHashMap<String, Any>()
    private val listeners = mutableListOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun getAll(): Map<String, *> = HashMap(data)

    override fun getString(key: String?, defValue: String?): String? {
        val value = if (key != null) data[key] else null
        return (value as? String) ?: defValue
    }

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: Set<String>?): Set<String>? {
        val value = if (key != null) data[key] else null
        return (value as? Set<String>) ?: defValues
    }

    override fun getInt(key: String?, defValue: Int): Int {
        val value = if (key != null) data[key] else null
        return (value as? Int) ?: defValue
    }

    override fun getLong(key: String?, defValue: Long): Long {
        val value = if (key != null) data[key] else null
        return (value as? Long) ?: defValue
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        val value = if (key != null) data[key] else null
        return (value as? Float) ?: defValue
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        val value = if (key != null) data[key] else null
        return (value as? Boolean) ?: defValue
    }

    override fun contains(key: String?): Boolean = key != null && data.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor(this)

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        if (listener != null) listeners.add(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        if (listener != null) listeners.remove(listener)
    }

    private fun notifyListeners(key: String?) {
        listeners.forEach { it.onSharedPreferenceChanged(this, key) }
    }

    private class FakeEditor(private val prefs: FakeSharedPreferences) : SharedPreferences.Editor {
        private val tempMap = mutableMapOf<String, Any?>()
        private var clear = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) tempMap[key] = value
            return this
        }

        override fun putStringSet(key: String?, values: Set<String>?): SharedPreferences.Editor {
            if (key != null) tempMap[key] = values
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) tempMap[key] = value
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) tempMap[key] = value
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) tempMap[key] = value
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) tempMap[key] = value
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) tempMap[key] = null
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clear = true
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clear) {
                prefs.data.clear()
            }
            tempMap.forEach { (k, v) ->
                if (v == null) {
                    prefs.data.remove(k)
                } else {
                    prefs.data[k] = v
                }
                prefs.notifyListeners(k)
            }
        }
    }
}
