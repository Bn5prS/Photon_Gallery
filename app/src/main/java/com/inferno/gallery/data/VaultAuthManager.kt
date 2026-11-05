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
     * Returns the allowed authenticators bitmask.
     * When a custom Vault PIN is configured, strictly BIOMETRIC_STRONG is returned.
     * DEVICE_CREDENTIAL must NEVER be allowed when custom PIN is configured,
     * preventing lockscreen PIN / pattern bypass of the vault.
     */
    fun getAllowedAuthenticators(hasCustomPin: Boolean): Int {
        return if (hasCustomPin) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }
    }

    /**
     * Check if strong biometrics are available and enrolled on the device.
     */
    fun canAuthenticateWithBiometrics(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Trigger biometric authentication on demand.
     * When a custom Vault PIN is configured, DEVICE_CREDENTIAL is NEVER allowed,
     * strictly requiring BIOMETRIC_STRONG to prevent lockscreen PIN bypass.
     */
    fun authenticateWithBiometrics(
        activity: FragmentActivity,
        title: String = "Private Space",
        subtitle: String = "Authenticate to access hidden photos",
        negativeButtonText: String = "Use PIN",
        unlockSession: Boolean = true,
        onSuccess: () -> Unit = {},
        onNegativeButton: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        val hasCustomPin = isPinConfigured(activity)
        val biometricManager = BiometricManager.from(activity)

        if (hasCustomPin) {
            val canBio = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            if (canBio != BiometricManager.BIOMETRIC_SUCCESS) {
                // Biometrics not available/enrolled; fallback to PIN
                onNegativeButton()
                return
            }
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (unlockSession) {
                        _isAuthenticated.value = true
                    }
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
                    // Biometric didn't match — prompt stays open for retry
                }
            }
        )

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        if (hasCustomPin) {
            // Strictly enforce BIOMETRIC_STRONG without DEVICE_CREDENTIAL.
            // BiometricPrompt requires negative button text when DEVICE_CREDENTIAL is not used.
            promptInfoBuilder
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .setNegativeButtonText(negativeButtonText)
        } else {
            // Fallback when no custom PIN has been configured
            promptInfoBuilder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        }

        biometricPrompt.authenticate(promptInfoBuilder.build())
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "Private Space",
        subtitle: String = "Authenticate to access hidden photos",
        negativeButtonText: String = "Use PIN",
        unlockSession: Boolean = true,
        onSuccess: () -> Unit = {},
        onNegativeButton: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        authenticateWithBiometrics(
            activity = activity,
            title = title,
            subtitle = subtitle,
            negativeButtonText = negativeButtonText,
            unlockSession = unlockSession,
            onSuccess = onSuccess,
            onNegativeButton = onNegativeButton,
            onFailure = onFailure
        )
    }
}
