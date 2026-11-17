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
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Manages Master PIN and biometric authentication for Private Space.
 * Session-based: call [lock] to re-lock.
 */
class VaultAuthManager {

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    companion object {
        private const val PREFS_NAME = "photon_vault_prefs"
        private const val KEY_PIN_HASH = "vault_pin_hash"
        private const val KEY_PIN_SALT = "vault_pin_salt"
        private const val KEY_RECOVERY_HASH = "vault_recovery_hash"
        private const val KEY_RECOVERY_SALT = "vault_recovery_salt"
        private const val KEY_BIO_DEK_IV = "vault_bio_dek_iv"
        private const val KEY_BIO_DEK_CIPHER = "vault_bio_dek_cipher"
        private const val KEY_FAILED_ATTEMPTS = "vault_failed_attempts"
        private const val KEY_LOCKOUT_UNTIL_MS = "vault_lockout_until_ms"
        private const val MAX_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 30_000L
        private const val PBKDF2_ITERATIONS = 10_000
        private const val RECOVERY_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    }

    private fun hashPinLegacy(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
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
        return prefs.contains(KEY_PIN_HASH) || VaultPersistentStorage.hasPersistentAuth()
    }

    private fun storeBioWrappedDek(prefs: android.content.SharedPreferences, dek: javax.crypto.SecretKey) {
        val bioWrapped = VaultPersistentStorage.wrapDekForBiometrics(dek) ?: return
        prefs.edit()
            .putString(KEY_BIO_DEK_IV, bioWrapped.ivBase64)
            .putString(KEY_BIO_DEK_CIPHER, bioWrapped.cipherBase64)
            .apply()
    }

    private fun restoreBioWrappedDek(context: Context): javax.crypto.SecretKey? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val iv = prefs.getString(KEY_BIO_DEK_IV, null) ?: return null
        val cipher = prefs.getString(KEY_BIO_DEK_CIPHER, null) ?: return null
        return VaultPersistentStorage.unwrapDekFromBiometrics(iv, cipher)
    }

    fun savePin(context: Context, pin: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saltHex = generateSaltHex()
        val hash = hashPinPbkdf2(pin, saltHex)
        val dek = VaultPersistentStorage.activeDek ?: VaultPersistentStorage.generateDek().also {
            VaultPersistentStorage.activeDek = it
        }
        val wrappedForPin = VaultPersistentStorage.wrapDekWithSecret(pin, dek)
        val existingPersistent = VaultPersistentStorage.readAuthRecord()
        VaultPersistentStorage.writeAuthRecord(
            (existingPersistent ?: VaultPersistentStorage.PersistentAuthRecord()).copy(
                pinHash = hash,
                pinSalt = saltHex,
                pinWrappedDek = wrappedForPin
            )
        )
        storeBioWrappedDek(prefs, dek)
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

    private fun recordAuthOutcome(prefs: android.content.SharedPreferences, matched: Boolean): Boolean {
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

    fun verifyPin(context: Context, pin: String): Boolean {
        if (getRemainingLockoutSeconds(context) > 0) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val localHash = prefs.getString(KEY_PIN_HASH, null)
        val localSalt = prefs.getString(KEY_PIN_SALT, null)
        val persistent = VaultPersistentStorage.readAuthRecord()

        val storedHash = localHash ?: persistent?.pinHash ?: return false
        val saltHex = localSalt ?: persistent?.pinSalt

        val matched = if (saltHex != null) {
            hashPinPbkdf2(pin, saltHex) == storedHash
        } else {
            val legacyMatch = hashPinLegacy(pin) == storedHash
            if (legacyMatch) {
                // Transparently upgrade legacy unsalted SHA-256 PIN to salted PBKDF2
                savePin(context, pin)
            }
            legacyMatch
        }

        if (matched) {
            // Restore DEK into memory from persistent auth or bio wrapper, or create if upgrading
            val unwrapped = persistent?.pinWrappedDek?.let {
                VaultPersistentStorage.unwrapDekWithSecret(pin, it)
            } ?: restoreBioWrappedDek(context)

            if (unwrapped != null) {
                VaultPersistentStorage.activeDek = unwrapped
                storeBioWrappedDek(prefs, unwrapped)
                if (localHash == null) {
                    // Sync persistent auth back into local prefs after Clear Data / Reinstall
                    prefs.edit()
                        .putString(KEY_PIN_HASH, storedHash)
                        .apply {
                            if (saltHex != null) putString(KEY_PIN_SALT, saltHex)
                            if (!persistent?.recoveryHash.isNullOrBlank() && !persistent?.recoverySalt.isNullOrBlank()) {
                                putString(KEY_RECOVERY_HASH, persistent?.recoveryHash)
                                putString(KEY_RECOVERY_SALT, persistent?.recoverySalt)
                            }
                        }
                        .apply()
                }
            } else if (saltHex != null && persistent?.pinWrappedDek == null) {
                // First upgrade from pre-persistent v4.0.0: generate DEK and wrap for persistent storage
                val freshDek = VaultPersistentStorage.generateDek()
                VaultPersistentStorage.activeDek = freshDek
                storeBioWrappedDek(prefs, freshDek)
                VaultPersistentStorage.writeAuthRecord(
                    (persistent ?: VaultPersistentStorage.PersistentAuthRecord()).copy(
                        pinHash = storedHash,
                        pinSalt = saltHex,
                        pinWrappedDek = VaultPersistentStorage.wrapDekWithSecret(pin, freshDek),
                        recoveryHash = prefs.getString(KEY_RECOVERY_HASH, null),
                        recoverySalt = prefs.getString(KEY_RECOVERY_SALT, null)
                    )
                )
            }
        }

        return recordAuthOutcome(prefs, matched)
    }

    fun hasRecoveryCode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return (prefs.contains(KEY_RECOVERY_HASH) && prefs.contains(KEY_RECOVERY_SALT)) ||
            VaultPersistentStorage.hasPersistentRecoveryCode()
    }

    /**
     * Generates a 16-character recovery code formatted as XXXX-XXXX-XXXX-XXXX,
     * stores its salted PBKDF2 hash, and returns the plaintext code once for the user to save.
     */
    fun generateAndSaveRecoveryCode(context: Context): String {
        val rng = SecureRandom()
        val raw = StringBuilder(16)
        repeat(16) {
            raw.append(RECOVERY_ALPHABET[rng.nextInt(RECOVERY_ALPHABET.length)])
        }
        val normalized = raw.toString()
        val formatted = normalized.chunked(4).joinToString("-")

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saltHex = generateSaltHex()
        val hash = hashPinPbkdf2(normalized, saltHex)
        val dek = VaultPersistentStorage.activeDek ?: restoreBioWrappedDek(context)
        val wrappedForRecovery = dek?.let { VaultPersistentStorage.wrapDekWithSecret(normalized, it) }
        val existingPersistent = VaultPersistentStorage.readAuthRecord()
        VaultPersistentStorage.writeAuthRecord(
            (existingPersistent ?: VaultPersistentStorage.PersistentAuthRecord()).copy(
                recoveryHash = hash,
                recoverySalt = saltHex,
                recoveryWrappedDek = wrappedForRecovery ?: existingPersistent?.recoveryWrappedDek
            )
        )
        prefs.edit()
            .putString(KEY_RECOVERY_SALT, saltHex)
            .putString(KEY_RECOVERY_HASH, hash)
            .apply()

        return formatted
    }

    fun verifyRecoveryCode(context: Context, input: String): Boolean {
        if (getRemainingLockoutSeconds(context) > 0) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val persistent = VaultPersistentStorage.readAuthRecord()
        val storedHash = prefs.getString(KEY_RECOVERY_HASH, null) ?: persistent?.recoveryHash ?: return false
        val saltHex = prefs.getString(KEY_RECOVERY_SALT, null) ?: persistent?.recoverySalt ?: return false

        val normalized = input.uppercase().replace("-", "").replace(" ", "").trim()
        if (normalized.isEmpty()) return false

        val matched = hashPinPbkdf2(normalized, saltHex) == storedHash
        if (matched) {
            val unwrapped = persistent?.recoveryWrappedDek?.let {
                VaultPersistentStorage.unwrapDekWithSecret(normalized, it)
            } ?: restoreBioWrappedDek(context)
            if (unwrapped != null) {
                VaultPersistentStorage.activeDek = unwrapped
                storeBioWrappedDek(prefs, unwrapped)
            }
        }
        return recordAuthOutcome(prefs, matched)
    }

    fun resetPin(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .remove(KEY_BIO_DEK_IV)
            .remove(KEY_BIO_DEK_CIPHER)
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LOCKOUT_UNTIL_MS)
            .apply()
    }

    fun unlock() {
        _isAuthenticated.value = true
    }

    /** Lock the vault manually. */
    fun lock() {
        _isAuthenticated.value = false
        VaultPersistentStorage.clearActiveDek()
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
                    if (VaultPersistentStorage.activeDek == null) {
                        VaultPersistentStorage.activeDek = restoreBioWrappedDek(activity)
                    }
                    if (hasCustomPin && VaultPersistentStorage.activeDek == null) {
                        // DEK cannot be unwrapped with biometrics alone (e.g. after Clear Data or biometric re-enrollment)
                        onNegativeButton()
                        return
                    }
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

    /**
     * Requires a configured Private Space PIN before hiding media, then authenticates with
     * biometrics or delegates to the PIN dialog. Never unlocks the full UI session.
     */
    fun authenticateForHide(
        activity: FragmentActivity,
        itemCount: Int,
        onAuthenticated: () -> Unit,
        onRequirePinDialog: (() -> Unit) -> Unit
    ) {
        if (!isPinConfigured(activity)) {
            android.widget.Toast.makeText(
                activity,
                "Set up your Private Space PIN first in Collections → Private Space",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }
        val countLabel = "$itemCount item${if (itemCount != 1) "s" else ""}"
        authenticateWithBiometrics(
            activity = activity,
            title = "Move to Private Space",
            subtitle = "Authenticate to move $countLabel to Private Space",
            negativeButtonText = "Use PIN",
            unlockSession = false,
            onSuccess = onAuthenticated,
            onNegativeButton = { onRequirePinDialog(onAuthenticated) },
            onFailure = { err ->
                if (err.isNotBlank()) {
                    android.widget.Toast.makeText(activity, err, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}
