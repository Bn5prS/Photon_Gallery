package com.inferno.gallery.data

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Manages the custom 4-digit PIN and strong biometric authentication for App Lock.
 * Credentials are stored separately from Private Space in [PREFS_NAME].
 */
class AppLockAuthManager {

    companion object {
        private const val PREFS_NAME = "photon_app_lock_prefs"
        private const val KEY_PIN_HASH = "app_lock_pin_hash"
        private const val KEY_PIN_SALT = "app_lock_pin_salt"
        private const val KEY_FAILED_ATTEMPTS = "app_lock_failed_attempts"
        private const val KEY_LOCKOUT_UNTIL_MS = "app_lock_lockout_until_ms"
        private const val MAX_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 30_000L
        private const val PBKDF2_ITERATIONS = 10_000
    }

    private fun hashPinPbkdf2(pin: String, saltHex: String): String {
        val salt = saltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val bytes = factory.generateSecret(spec).encoded
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun generateSaltHex(): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }

    fun isPinConfigured(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(KEY_PIN_HASH) && prefs.contains(KEY_PIN_SALT)
    }

    fun savePin(context: Context, pin: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saltHex = generateSaltHex()
        val hash = hashPinPbkdf2(pin, saltHex)
        prefs.edit()
            .putString(KEY_PIN_SALT, saltHex)
            .putString(KEY_PIN_HASH, hash)
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LOCKOUT_UNTIL_MS)
            .apply()
    }

    fun getRemainingLockoutSeconds(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val until = prefs.getLong(KEY_LOCKOUT_UNTIL_MS, 0L)
        val remainingMs = until - System.currentTimeMillis()
        return if (remainingMs > 0L) ((remainingMs + 999L) / 1000L).toInt() else 0
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        if (getRemainingLockoutSeconds(context) > 0) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val saltHex = prefs.getString(KEY_PIN_SALT, null) ?: return false

        val matched = hashPinPbkdf2(pin, saltHex) == storedHash
        if (matched) {
            prefs.edit()
                .remove(KEY_FAILED_ATTEMPTS)
                .remove(KEY_LOCKOUT_UNTIL_MS)
                .apply()
        } else {
            val attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
            val editor = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, attempts)
            if (attempts >= MAX_ATTEMPTS) {
                editor.putLong(KEY_LOCKOUT_UNTIL_MS, System.currentTimeMillis() + LOCKOUT_DURATION_MS)
                editor.putInt(KEY_FAILED_ATTEMPTS, 0)
            }
            editor.apply()
        }
        return matched
    }

    fun resetPin(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LOCKOUT_UNTIL_MS)
            .apply()
    }

    fun canAuthenticateWithBiometrics(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Triggers strong biometric authentication. Strictly uses BIOMETRIC_STRONG so the
     * system lockscreen credential cannot bypass the app's custom PIN.
     */
    fun authenticateWithBiometrics(
        activity: FragmentActivity,
        title: String = "Unlock Photon Gallery",
        subtitle: String = "Authenticate to access your photos",
        negativeButtonText: String = "Use PIN",
        onSuccess: () -> Unit,
        onNegativeButton: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        if (!canAuthenticateWithBiometrics(activity)) {
            onNegativeButton()
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        onNegativeButton()
                    } else if (errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                        onFailure(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    // Prompt stays open for retry
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText(negativeButtonText)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
