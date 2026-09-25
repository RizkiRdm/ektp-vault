package com.example

import com.example.security.CryptoManager
import com.example.security.KeyStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CryptoManagerTest {

    @Test
    fun testMasterKeyDerivationConsistency() {
        val nfcUid = "04:A2:3B:5F:7E:89"
        val biometricSecret = "HARDWARE_TEE_SECRET_123"
        val salt = CryptoManager.generateDeviceSalt()

        val key1 = CryptoManager.deriveMasterKey(nfcUid, biometricSecret, salt)
        val key2 = CryptoManager.deriveMasterKey(nfcUid, biometricSecret, salt)
        val keyDifferentUid = CryptoManager.deriveMasterKey("04:00:00:00:00:00", biometricSecret, salt)

        assertEquals(key1.toList(), key2.toList())
        assertNotEquals(key1.toList(), keyDifferentUid.toList())

        CryptoManager.wipeBytes(key1)
        CryptoManager.wipeBytes(key2)
        CryptoManager.wipeBytes(keyDifferentUid)
    }

    @Test
    fun testAesGcmEncryptAndDecrypt() {
        val nfcUid = "04:A2:3B:5F:7E:89"
        val biometricSecret = "HARDWARE_TEE_SECRET_123"
        val salt = CryptoManager.generateDeviceSalt()
        val masterKey = CryptoManager.deriveMasterKey(nfcUid, biometricSecret, salt)

        val secretPassword = "VerySecurePassword#2026!"
        val encryptedBlob = CryptoManager.encryptPassword(secretPassword, masterKey)

        assertNotNull(encryptedBlob)
        assertNotEquals(secretPassword, encryptedBlob)

        val decrypted = CryptoManager.decryptPassword(encryptedBlob, masterKey)
        assertEquals(secretPassword, decrypted)

        CryptoManager.wipeBytes(masterKey)
    }

    @Test
    fun testRecoveryPhrase() {
        val phrase = CryptoManager.generateRecoveryPhrase()
        assertEquals(12, phrase.size)

        val hash1 = CryptoManager.hashPhrase(phrase)
        val hash2 = CryptoManager.hashPhrase(phrase)
        assertEquals(hash1, hash2)
    }

    @Test
    fun testKeyStorageLifecycle() {
        val dummyKey = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        KeyStorage.storeMasterKey(dummyKey, "e-KTP", "hash123")

        assertTrue(KeyStorage.isUnlocked.value)
        assertNotNull(KeyStorage.getMasterKey())

        KeyStorage.wipeImmediateHighRisk()
        assertFalse(KeyStorage.isUnlocked.value)
        assertNull(KeyStorage.getMasterKey())
    }
}
