package com.inferno.gallery.data

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

/**
 * Manages Master PIN and biometric authentication for Private Space.
 * Session-based: call [lock] to re-lock.
 */
class VaultAuthManager {

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun isPinConfigured(context: Context): Boolean {
        val prefs = context.getSharedPreferences("photon_vault_prefs", Context.MODE_PRIVATE)
        return prefs.contains("vault_pin_hash")
    }

    fun savePin(context: Context, pin: String) {
        val prefs = context.getSharedPreferences("photon_vault_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("vault_pin_hash", hashPin(pin)).apply()
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val prefs = context.getSharedPreferences("photon_vault_prefs", Context.MODE_PRIVATE)
        val storedHash = prefs.getString("vault_pin_hash", null) ?: return false
        return hashPin(pin) == storedHash
    }

    fun resetPin(context: Context) {
        val prefs = context.getSharedPreferences("photon_vault_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("vault_pin_hash").apply()
    }

    fun unlock() {
        _isAuthenticated.value = true
    }

    /** Lock the vault manually. */
    fun lock() {
        _isAuthenticated.value = false
    }

    /**
     * Trigger biometric/device-credential authentication on demand.
     */
    fun authenticateWithBiometrics(
        activity: FragmentActivity,
        title: String = "Private Space",
        subtitle: String = "Authenticate to access hidden photos",
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    _isAuthenticated.value = true
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        onFailure(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    // Biometric didn't match — prompt stays open for retry
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "Private Space",
        subtitle: String = "Authenticate to access hidden photos",
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        authenticateWithBiometrics(activity, title, subtitle, onSuccess, onFailure)
    }
}
