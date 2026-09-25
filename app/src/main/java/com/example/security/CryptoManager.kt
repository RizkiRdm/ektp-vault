package com.example.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import com.example.domain.repository.ISecurityEngine
import org.json.JSONObject

object CryptoManager : ISecurityEngine {
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val AES_GCM_ALGORITHM = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12
    private const val KEY_LENGTH = 256
    private const val PBKDF2_ITERATIONS = 65536

    private val secureRandom = SecureRandom()

    // Standard BIP39 wordlist subset (128 words for recovery phrases)
    private val RECOVERY_WORDS = listOf(
        "abandon", "ability", "able", "about", "above", "absent", "absorb", "abstract",
        "absurd", "abuse", "access", "accident", "account", "accuse", "achieve", "acid",
        "acoustic", "acquire", "across", "act", "action", "actor", "actress", "actual",
        "adapt", "add", "addict", "address", "adjust", "admit", "adult", "advance",
        "advice", "aerobic", "affair", "afford", "afraid", "again", "age", "agent",
        "agree", "ahead", "aim", "air", "airport", "aisle", "alarm", "album",
        "alcohol", "alert", "alien", "all", "alley", "allow", "almost", "alone",
        "alpha", "already", "also", "alter", "always", "amateur", "amazing", "among",
        "amount", "amused", "analyst", "anchor", "ancient", "anger", "angle", "angry",
        "animal", "ankle", "announce", "annual", "another", "answer", "antenna", "antique",
        "anxiety", "any", "apart", "apology", "appear", "apple", "approve", "april",
        "arch", "arctic", "area", "arena", "argue", "arm", "armed", "armor",
        "army", "around", "arrange", "arrest", "arrive", "arrow", "art", "artefact",
        "artist", "artwork", "ask", "aspect", "assault", "asset", "assist", "assume",
        "asthma", "athlete", "atom", "attack", "attend", "attitude", "attract", "auction",
        "audit", "august", "aunt", "author", "auto", "autumn", "average", "avocado"
    )

    override fun generateDeviceSalt(): String {
        val salt = ByteArray(32)
        secureRandom.nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    override fun deriveMasterKey(nfcUid: String, biometricSecret: String, deviceSaltBase64: String): ByteArray {
        val combined = "$nfcUid:$biometricSecret".toCharArray()
        val salt = Base64.decode(deviceSaltBase64, Base64.NO_WRAP)
        val spec = PBEKeySpec(combined, salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        // Clean up spec password memory
        spec.clearPassword()
        return keyBytes
    }

    override fun hashNfcUid(rawUid: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(rawUid.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    override fun encryptPassword(plaintext: String, masterKeyBytes: ByteArray): String {
        return try {
            KeystoreManager.encrypt(plaintext)
        } catch (_: Exception) {
            encryptWithSoftwareKey(plaintext, masterKeyBytes)
        }
    }

    override fun decryptPassword(encryptedBlobBase64: String, masterKeyBytes: ByteArray): String {
        return try {
            KeystoreManager.decrypt(encryptedBlobBase64)
        } catch (_: Exception) {
            decryptWithSoftwareKey(encryptedBlobBase64, masterKeyBytes)
        }
    }

    private fun encryptWithSoftwareKey(plaintext: String, masterKeyBytes: ByteArray): String {
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)

        val keySpec = SecretKeySpec(masterKeyBytes, "AES")
        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val plaintextBytes = plaintext.toByteArray(Charsets.UTF_8)
        val cipherBytes = cipher.doFinal(plaintextBytes)

        val combined = ByteArray(iv.size + cipherBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decryptWithSoftwareKey(encryptedBlobBase64: String, masterKeyBytes: ByteArray): String {
        val combined = Base64.decode(encryptedBlobBase64, Base64.NO_WRAP)
        if (combined.size < GCM_IV_LENGTH) {
            throw IllegalArgumentException("Invalid encrypted payload")
        }

        val iv = ByteArray(GCM_IV_LENGTH)
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)

        val cipherBytesLength = combined.size - GCM_IV_LENGTH
        val cipherBytes = ByteArray(cipherBytesLength)
        System.arraycopy(combined, GCM_IV_LENGTH, cipherBytes, 0, cipherBytesLength)

        val keySpec = SecretKeySpec(masterKeyBytes, "AES")
        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

        val decryptedBytes = cipher.doFinal(cipherBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    override fun generateRecoveryPhrase(): List<String> {
        val words = mutableListOf<String>()
        repeat(12) {
            val index = secureRandom.nextInt(RECOVERY_WORDS.size)
            words.add(RECOVERY_WORDS[index])
        }
        return words
    }

    override fun hashPhrase(phrase: List<String>): String {
        val normalized = phrase.joinToString(" ").trim().lowercase()
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(normalized.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    override fun createLokerBackup(
        credentialsJson: String,
        backupPassword: String,
        nfcUidHash: String
    ): String {
        val salt = ByteArray(16)
        secureRandom.nextBytes(salt)

        val spec = PBEKeySpec(backupPassword.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        spec.clearPassword()

        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)

        val keySpec = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(GCM_TAG_LENGTH, iv))

        val encryptedPayload = cipher.doFinal(credentialsJson.toByteArray(Charsets.UTF_8))
        wipeBytes(keyBytes)

        val envelope = JSONObject().apply {
            put("format", "LOKER_VAULT_ENVELOPE_V1")
            put("nfc_hash", nfcUidHash)
            put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            put("ciphertext", Base64.encodeToString(encryptedPayload, Base64.NO_WRAP))
            put("timestamp", System.currentTimeMillis())
        }

        return envelope.toString(2)
    }

    override fun restoreLokerBackup(
        backupContent: String,
        backupPassword: String,
        currentNfcUidHash: String
    ): String {
        val envelope = JSONObject(backupContent)
        val format = envelope.optString("format")
        if (format != "LOKER_VAULT_ENVELOPE_V1") {
            throw IllegalArgumentException("Unsupported .loker format")
        }

        val nfcHash = envelope.getString("nfc_hash")
        if (nfcHash != currentNfcUidHash) {
            throw SecurityException("NFC card mismatch. This file can only be opened with the registered KTP.")
        }

        val salt = Base64.decode(envelope.getString("salt"), Base64.NO_WRAP)
        val iv = Base64.decode(envelope.getString("iv"), Base64.NO_WRAP)
        val ciphertext = Base64.decode(envelope.getString("ciphertext"), Base64.NO_WRAP)

        val spec = PBEKeySpec(backupPassword.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        spec.clearPassword()

        val keySpec = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(GCM_TAG_LENGTH, iv))

        val decrypted = cipher.doFinal(ciphertext)
        wipeBytes(keyBytes)
        return String(decrypted, Charsets.UTF_8)
    }

    override fun wipeBytes(bytes: ByteArray?) {
        if (bytes != null) {
            bytes.fill(0)
        }
    }
}
