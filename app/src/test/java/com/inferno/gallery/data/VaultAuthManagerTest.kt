package com.inferno.gallery.data

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VaultAuthManagerTest {

    private lateinit var authManager: VaultAuthManager
    private lateinit var fakeContext: FakeContext

    private class FakeSharedPreferences : SharedPreferences {
        private val map = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = HashMap(map)
        override fun getString(key: String?, defValue: String?): String? = map[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            (map[key] as? Set<*>)?.map { it.toString() }?.toMutableSet() ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = map[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = map[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = map[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = map.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor(map)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private class FakeEditor(private val target: MutableMap<String, Any?>) : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private val removes = mutableSetOf<String>()
            private var clearAll = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null) pending[key] = value
                return this
            }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
                if (key != null) pending[key] = values
                return this
            }
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                if (key != null) pending[key] = value
                return this
            }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                if (key != null) pending[key] = value
                return this
            }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
                if (key != null) pending[key] = value
                return this
            }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                if (key != null) pending[key] = value
                return this
            }
            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) removes.add(key)
                return this
            }
            override fun clear(): SharedPreferences.Editor {
                clearAll = true
                return this
            }
            override fun commit(): Boolean {
                apply()
                return true
            }
            override fun apply() {
                if (clearAll) target.clear()
                removes.forEach { target.remove(it) }
                target.putAll(pending)
            }
        }
    }

    private class FakeContext : ContextWrapper(null) {
        private val prefs = FakeSharedPreferences()
        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = prefs
    }

    @Before
    fun setUp() {
        authManager = VaultAuthManager()
        fakeContext = FakeContext()
    }

    @Test
    fun testInitialLockStateIsLocked() {
        assertFalse("Vault should be locked initially", authManager.isAuthenticated.value)
    }

    @Test
    fun testUnlockAndLockStateTransitions() {
        assertFalse(authManager.isAuthenticated.value)
        authManager.unlock()
        assertTrue("Vault should be authenticated after unlock", authManager.isAuthenticated.value)
        authManager.lock()
        assertFalse("Vault should be locked after lock()", authManager.isAuthenticated.value)
    }

    @Test
    fun testPinLifecycleAndVerification() {
        assertFalse("PIN should not be configured initially", authManager.isPinConfigured(fakeContext))

        authManager.savePin(fakeContext, "1234")
        assertTrue("PIN should be configured after save", authManager.isPinConfigured(fakeContext))

        assertTrue("verifyPin with correct PIN should return true", authManager.verifyPin(fakeContext, "1234"))
        assertFalse("verifyPin with wrong PIN should return false", authManager.verifyPin(fakeContext, "0000"))
        assertFalse("verifyPin with empty PIN should return false", authManager.verifyPin(fakeContext, ""))

        authManager.resetPin(fakeContext)
        assertFalse("PIN should not be configured after reset", authManager.isPinConfigured(fakeContext))
        assertFalse("verifyPin after reset should return false", authManager.verifyPin(fakeContext, "1234"))
    }

    @Test
    fun testAllowedAuthenticatorsStrictlyExcludesDeviceCredentialWhenCustomPinConfigured() {
        // When custom PIN is configured: DEVICE_CREDENTIAL must NEVER be allowed!
        val authenticatorsWithCustomPin = authManager.getAllowedAuthenticators(hasCustomPin = true)

        assertEquals(
            "Allowed authenticators must strictly be BIOMETRIC_STRONG when custom PIN is configured",
            BiometricManager.Authenticators.BIOMETRIC_STRONG,
            authenticatorsWithCustomPin
        )

        val deviceCredentialAllowed = (authenticatorsWithCustomPin and BiometricManager.Authenticators.DEVICE_CREDENTIAL) != 0
        assertFalse(
            "DEVICE_CREDENTIAL must NEVER be allowed when custom PIN is configured",
            deviceCredentialAllowed
        )
    }

    @Test
    fun testAllowedAuthenticatorsAllowsDeviceCredentialOnlyWhenNoCustomPinConfigured() {
        // When no custom PIN is configured: device credentials can be used as fallback
        val authenticatorsWithoutCustomPin = authManager.getAllowedAuthenticators(hasCustomPin = false)

        val expected = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        assertEquals(
            "Allowed authenticators should include DEVICE_CREDENTIAL when no custom PIN is configured",
            expected,
            authenticatorsWithoutCustomPin
        )

        val deviceCredentialAllowed = (authenticatorsWithoutCustomPin and BiometricManager.Authenticators.DEVICE_CREDENTIAL) != 0
        assertTrue("DEVICE_CREDENTIAL should be present when no custom PIN is configured", deviceCredentialAllowed)
    }

    @Test
    fun testSessionInvalidationEnsuresLocked() {
        authManager.unlock()
        assertTrue(authManager.isAuthenticated.value)

        // Simulate session invalidation (e.g. exit, onStop, onPause, back)
        authManager.lock()
        assertFalse(authManager.isAuthenticated.value)
    }
}
