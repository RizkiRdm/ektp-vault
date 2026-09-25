package com.example

import com.example.security.KeystoreManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KeystoreManagerTest {

    @Before
    fun setup() {
        KeystoreManager.deleteMasterKey()
    }

    @Test
    fun `master key is generated and stored inside Android Keystore System`() {
        val masterKey = KeystoreManager.getOrCreateMasterKey()
        assertNotNull(masterKey)
        assertEquals("AES", masterKey.algorithm)
        assertTrue(KeystoreManager.hasMasterKey())
    }

    @Test
    fun `master key material never leaves hardware security module`() {
        // In Android Keystore, key.encoded must be null to guarantee hardware isolation
        assertTrue(KeystoreManager.isKeyMaterialProtected())
    }

    @Test
    fun `symmetric encryption and decryption of credentials using Keystore Master Key`() {
        val plaintext = "SuperSecretBankPassword#2026!"
        val encryptedBlob = KeystoreManager.encrypt(plaintext)

        assertNotNull(encryptedBlob)
        assertNotEquals(plaintext, encryptedBlob)

        val decrypted = KeystoreManager.decrypt(encryptedBlob)
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `multiple encryptions produce unique ciphertexts with random IVs`() {
        val plaintext = "ConsistentCredentialInput"
        val cipher1 = KeystoreManager.encrypt(plaintext)
        val cipher2 = KeystoreManager.encrypt(plaintext)

        assertNotEquals(cipher1, cipher2)
        assertEquals(plaintext, KeystoreManager.decrypt(cipher1))
        assertEquals(plaintext, KeystoreManager.decrypt(cipher2))
    }
}
