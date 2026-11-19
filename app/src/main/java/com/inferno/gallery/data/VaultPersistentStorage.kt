package com.inferno.gallery.data

import android.content.Context
import android.os.Environment
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.util.Base64
import com.inferno.gallery.data.db.VaultMediaEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Manages persistent Private Space storage on shared external storage (`/storage/emulated/0/.PhotonPrivateSpace`)
 * so encrypted files, auth metadata, and the encrypted manifest survive app Clear Data and Uninstall/Reinstall.
 */
object VaultPersistentStorage {

    private const val PERSISTENT_DIR_NAME = ".PhotonPrivateSpace"
    private const val AUTH_FILE_NAME = "vault_auth.json"
    private const val MANIFEST_FILE_NAME = "vault_manifest.enc"
    private val MANIFEST_MAGIC = "PVMNF1".toByteArray(Charsets.US_ASCII)
    private const val PBKDF2_ITERATIONS = 10_000
    private const val GCM_IV_LEN = 12
    private const val GCM_TAG_BITS = 128
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val BIO_WRAP_KEY_ALIAS = "photon_vault_bio_dek_wrap"

    @Volatile
    var activeDek: SecretKey? = null

    data class WrappedDek(
        val saltBase64: String,
        val ivBase64: String,
        val cipherBase64: String
    )

    data class BioWrappedDek(
        val ivBase64: String,
        val cipherBase64: String
    )

    data class PersistentAuthRecord(
        val pinHash: String? = null,
        val pinSalt: String? = null,
        val pinWrappedDek: WrappedDek? = null,
        val recoveryHash: String? = null,
        val recoverySalt: String? = null,
        val recoveryWrappedDek: WrappedDek? = null
    )

    fun clearActiveDek() {
        activeDek = null
    }

    /**
     * Returns `/storage/emulated/0/.PhotonPrivateSpace` if external storage is writable,
     * ensuring a `.nomedia` file exists inside it. Returns null in non-writable environments (e.g., JVM tests).
     */
    fun getPersistentVaultDir(): File? {
        return try {
            val extRoot = Environment.getExternalStorageDirectory() ?: return null
            if (!extRoot.exists() || !extRoot.canWrite()) return null
            val dir = File(extRoot, PERSISTENT_DIR_NAME)
            if (!dir.exists() && !dir.mkdirs()) return null
            if (!dir.canWrite()) return null
            val nomedia = File(dir, ".nomedia")
            if (!nomedia.exists()) {
                try {
                    nomedia.createNewFile()
                } catch (_: Exception) {}
            }
            dir
        } catch (_: Throwable) {
            null
        }
    }

    fun getOrCreateVaultDir(context: Context): File {
        return getPersistentVaultDir() ?: File(context.filesDir, "vault").also { it.mkdirs() }
    }

    fun hasPersistentAuth(): Boolean {
        val dir = getPersistentVaultDir() ?: return false
        val authFile = File(dir, AUTH_FILE_NAME)
        if (!authFile.exists() || !authFile.canRead()) return false
        val record = readAuthRecord() ?: return false
        return !record.pinHash.isNullOrBlank() && !record.pinSalt.isNullOrBlank()
    }

    fun hasPersistentRecoveryCode(): Boolean {
        val record = readAuthRecord() ?: return false
        return !record.recoveryHash.isNullOrBlank() && !record.recoverySalt.isNullOrBlank()
    }

    fun generateDek(): SecretKey {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return SecretKeySpec(bytes, "AES")
    }

    private fun deriveWrappingKey(secret: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(secret.toCharArray(), salt, PBKDF2_ITERATIONS, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val bytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(bytes, "AES")
    }

    fun wrapDekWithSecret(secret: String, dek: SecretKey): WrappedDek {
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val key = deriveWrappingKey(secret, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal(dek.encoded)
        val encoder = Base64.getEncoder()
        return WrappedDek(
            saltBase64 = encoder.encodeToString(salt),
            ivBase64 = encoder.encodeToString(cipher.iv),
            cipherBase64 = encoder.encodeToString(encrypted)
        )
    }

    fun unwrapDekWithSecret(secret: String, wrapped: WrappedDek): SecretKey? {
        return try {
            val decoder = Base64.getDecoder()
            val salt = decoder.decode(wrapped.saltBase64.trim())
            val iv = decoder.decode(wrapped.ivBase64.trim())
            val ciphertext = decoder.decode(wrapped.cipherBase64.trim())
            val key = deriveWrappingKey(secret, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            val dekBytes = cipher.doFinal(ciphertext)
            SecretKeySpec(dekBytes, "AES")
        } catch (_: Throwable) {
            null
        }
    }

    private fun getOrCreateBioWrapKey(): SecretKey? {
        return try {
            val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            (ks.getKey(BIO_WRAP_KEY_ALIAS, null) as? SecretKey)?.let { return it }
            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            val spec = KeyGenParameterSpec.Builder(
                BIO_WRAP_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGen.init(spec)
            keyGen.generateKey()
        } catch (_: Throwable) {
            null
        }
    }

    fun wrapDekForBiometrics(dek: SecretKey): BioWrappedDek? {
        return try {
            val wrapKey = getOrCreateBioWrapKey() ?: return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, wrapKey)
            val encrypted = cipher.doFinal(dek.encoded)
            val encoder = Base64.getEncoder()
            BioWrappedDek(
                ivBase64 = encoder.encodeToString(cipher.iv),
                cipherBase64 = encoder.encodeToString(encrypted)
            )
        } catch (_: Throwable) {
            null
        }
    }

    fun unwrapDekFromBiometrics(ivBase64: String, cipherBase64: String): SecretKey? {
        return try {
            val wrapKey = getOrCreateBioWrapKey() ?: return null
            val decoder = Base64.getDecoder()
            val iv = decoder.decode(ivBase64.trim())
            val ciphertext = decoder.decode(cipherBase64.trim())
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, wrapKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            val dekBytes = cipher.doFinal(ciphertext)
            SecretKeySpec(dekBytes, "AES")
        } catch (_: Throwable) {
            null
        }
    }

    private fun WrappedDek.toJson(): JSONObject = JSONObject().apply {
        put("salt", saltBase64)
        put("iv", ivBase64)
        put("cipher", cipherBase64)
    }

    private fun JSONObject?.toWrappedDek(): WrappedDek? {
        if (this == null) return null
        val salt = optString("salt", "")
        val iv = optString("iv", "")
        val cipher = optString("cipher", "")
        if (salt.isBlank() || iv.isBlank() || cipher.isBlank()) return null
        return WrappedDek(salt, iv, cipher)
    }

    fun readAuthRecord(): PersistentAuthRecord? {
        return try {
            val dir = getPersistentVaultDir() ?: return null
            val file = File(dir, AUTH_FILE_NAME)
            if (!file.exists() || !file.canRead()) return null
            val obj = JSONObject(file.readText(Charsets.UTF_8))
            PersistentAuthRecord(
                pinHash = obj.optString("pinHash", "").ifBlank { null },
                pinSalt = obj.optString("pinSalt", "").ifBlank { null },
                pinWrappedDek = obj.optJSONObject("pinWrappedDek").toWrappedDek(),
                recoveryHash = obj.optString("recoveryHash", "").ifBlank { null },
                recoverySalt = obj.optString("recoverySalt", "").ifBlank { null },
                recoveryWrappedDek = obj.optJSONObject("recoveryWrappedDek").toWrappedDek()
            )
        } catch (_: Throwable) {
            null
        }
    }

    fun writeAuthRecord(record: PersistentAuthRecord) {
        try {
            val dir = getPersistentVaultDir() ?: return
            val file = File(dir, AUTH_FILE_NAME)
            val obj = JSONObject().apply {
                record.pinHash?.let { put("pinHash", it) }
                record.pinSalt?.let { put("pinSalt", it) }
                record.pinWrappedDek?.let { put("pinWrappedDek", it.toJson()) }
                record.recoveryHash?.let { put("recoveryHash", it) }
                record.recoverySalt?.let { put("recoverySalt", it) }
                record.recoveryWrappedDek?.let { put("recoveryWrappedDek", it.toJson()) }
            }
            file.writeText(obj.toString(), Charsets.UTF_8)
        } catch (_: Throwable) {}
    }

    fun saveEncryptedManifest(items: List<VaultMediaEntity>, dek: SecretKey) {
        try {
            val dir = getPersistentVaultDir() ?: return
            val array = JSONArray()
            for (item in items) {
                array.put(JSONObject().apply {
                    put("originalUri", item.originalUri)
                    put("originalPath", item.originalPath)
                    put("originalBucket", item.originalBucket)
                    put("originalRelativePath", item.originalRelativePath)
                    put("fileName", item.fileName)
                    put("vaultFileName", item.vaultFileName)
                    put("mimeType", item.mimeType ?: JSONObject.NULL)
                    put("isVideo", item.isVideo)
                    put("durationMs", item.durationMs ?: JSONObject.NULL)
                    put("size", item.size)
                    put("dateAdded", item.dateAdded)
                    put("dateModified", item.dateModified)
                    put("dateHidden", item.dateHidden)
                })
            }
            val plain = array.toString().toByteArray(Charsets.UTF_8)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, dek)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(plain)

            val outFile = File(dir, MANIFEST_FILE_NAME)
            outFile.outputStream().use { out ->
                out.write(MANIFEST_MAGIC)
                out.write(iv)
                out.write(encrypted)
            }
        } catch (_: Throwable) {}
    }

    fun loadEncryptedManifest(dek: SecretKey): List<VaultMediaEntity> {
        return try {
            val dir = getPersistentVaultDir() ?: return emptyList()
            val file = File(dir, MANIFEST_FILE_NAME)
            if (!file.exists() || !file.canRead()) return emptyList()
            val allBytes = file.readBytes()
            val headerLen = MANIFEST_MAGIC.size + GCM_IV_LEN
            if (allBytes.size <= headerLen) return emptyList()
            if (!allBytes.copyOfRange(0, MANIFEST_MAGIC.size).contentEquals(MANIFEST_MAGIC)) return emptyList()

            val iv = allBytes.copyOfRange(MANIFEST_MAGIC.size, headerLen)
            val ciphertext = allBytes.copyOfRange(headerLen, allBytes.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, dek, GCMParameterSpec(GCM_TAG_BITS, iv))
            val plain = cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
            val array = JSONArray(plain)
            val result = mutableListOf<VaultMediaEntity>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val vaultFileName = obj.getString("vaultFileName")
                if (!File(dir, vaultFileName).exists()) continue
                val mimeType = if (obj.isNull("mimeType")) null else obj.getString("mimeType")
                val durationMs = if (obj.isNull("durationMs")) null else obj.getLong("durationMs")
                result.add(
                    VaultMediaEntity(
                        originalUri = obj.optString("originalUri", ""),
                        originalPath = obj.optString("originalPath", ""),
                        originalBucket = obj.optString("originalBucket", "Gallery"),
                        originalRelativePath = obj.optString("originalRelativePath", ""),
                        fileName = obj.getString("fileName"),
                        vaultFileName = vaultFileName,
                        mimeType = mimeType,
                        isVideo = obj.optBoolean("isVideo", false),
                        durationMs = durationMs,
                        size = obj.optLong("size", 0L),
                        dateAdded = obj.optLong("dateAdded", 0L),
                        dateModified = obj.optLong("dateModified", 0L),
                        dateHidden = obj.optLong("dateHidden", System.currentTimeMillis() / 1000L)
                    )
                )
            }
            result
        } catch (_: Throwable) {
            emptyList()
        }
    }
}
