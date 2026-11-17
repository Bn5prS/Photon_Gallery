package com.inferno.gallery.data

import android.content.ContextWrapper
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppLockAuthManagerTest {

    private lateinit var authManager: AppLockAuthManager
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
        val prefs = FakeSharedPreferences()
        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = prefs
    }

    @Before
    fun setUp() {
        authManager = AppLockAuthManager()
        fakeContext = FakeContext()
    }

    @Test
    fun pinLifecycleAndSaltedHashVerification() {
        assertFalse(authManager.isPinConfigured(fakeContext))
        assertEquals(0, authManager.getRemainingLockoutSeconds(fakeContext))

        authManager.savePin(fakeContext, "2468")
        assertTrue(authManager.isPinConfigured(fakeContext))

        val savedHash = fakeContext.prefs.getString("app_lock_pin_hash", null)
        val savedSalt = fakeContext.prefs.getString("app_lock_pin_salt", null)
        assertTrue(!savedHash.isNullOrBlank())
        assertTrue(!savedSalt.isNullOrBlank())
        assertNotEquals("2468", savedHash)

        assertTrue(authManager.verifyPin(fakeContext, "2468"))
        assertFalse(authManager.verifyPin(fakeContext, "1357"))
        assertFalse(authManager.verifyPin(fakeContext, ""))

        authManager.resetPin(fakeContext)
        assertFalse(authManager.isPinConfigured(fakeContext))
        assertFalse(authManager.verifyPin(fakeContext, "2468"))
    }

    @Test
    fun fiveFailedAttemptsTriggersThirtySecondLockout() {
        authManager.savePin(fakeContext, "9999")

        repeat(4) {
            assertFalse(authManager.verifyPin(fakeContext, "0000"))
            assertEquals(0, authManager.getRemainingLockoutSeconds(fakeContext))
        }

        // 5th failed attempt triggers lockout
        assertFalse(authManager.verifyPin(fakeContext, "0000"))
        val remaining = authManager.getRemainingLockoutSeconds(fakeContext)
        assertTrue("Expected positive lockout duration, got $remaining", remaining in 1..30)

        // Even the correct PIN must be rejected while locked out
        assertFalse(authManager.verifyPin(fakeContext, "9999"))
    }

    @Test
    fun correctPinResetsFailedAttemptCounter() {
        authManager.savePin(fakeContext, "1122")

        repeat(4) {
            assertFalse(authManager.verifyPin(fakeContext, "0000"))
        }
        assertTrue(authManager.verifyPin(fakeContext, "1122"))
        assertEquals(0, authManager.getRemainingLockoutSeconds(fakeContext))

        // After a successful unlock, it takes 5 more wrong attempts to trigger lockout
        repeat(4) {
            assertFalse(authManager.verifyPin(fakeContext, "0000"))
            assertEquals(0, authManager.getRemainingLockoutSeconds(fakeContext))
        }
    }
}
