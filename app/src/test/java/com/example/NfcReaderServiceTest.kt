package com.example

import android.content.Context
import android.nfc.NfcAdapter
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.NfcTagData
import com.example.security.BiometricHelper
import com.example.security.CryptoManager
import com.example.security.KeyStorage
import com.example.service.NfcReaderService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NfcReaderServiceTest {

    private lateinit var context: Context
    private lateinit var nfcReaderService: NfcReaderService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        nfcReaderService = NfcReaderService(context)
        KeyStorage.clear()
    }

    @Test
    fun `nfc reader service implements reader callback interface`() {
        val callback: Any = nfcReaderService
        assertTrue(callback is NfcAdapter.ReaderCallback)
    }

    @Test
    fun `verify e-KTP detection for iso 14443-4 tag`() {
        val rawUid = byteArrayOf(0x04, 0xA2.toByte(), 0x3B, 0x5F, 0x7E, 0x89.toByte(), 0x11)
        val tagData = NfcTagData(
            rawUid = rawUid,
            hexUid = "04:A2:3B:5F:7E:89:11",
            techList = listOf("IsoDep", "NfcA"),
            isIsoDep = true,
            isEktpCompatible = true
        )

        val isEktp = nfcReaderService.verifyEktpSignature(tagData)
        assertTrue(isEktp)
    }

    @Test
    fun `reject e-KTP signature for incompatible non-smartcard tags`() {
        val rawUid = byteArrayOf(0x01, 0x02)
        val tagData = NfcTagData(
            rawUid = rawUid,
            hexUid = "01:02",
            techList = listOf("NfcF"),
            isIsoDep = false,
            isEktpCompatible = false
        )

        val isEktp = nfcReaderService.verifyEktpSignature(tagData)
        assertFalse(isEktp)
    }

    @Test
    fun `compute sha256 uid outputs expected hash length`() {
        val rawUid = byteArrayOf(0x04, 0xA2.toByte(), 0x3B, 0x5F)
        val hash = nfcReaderService.computeSha256Uid(rawUid)
        assertEquals(64, hash.length)
    }

    @Test
    fun `biometric helper generates and persists biometric entropy`() {
        val secret1 = BiometricHelper.getOrGenerateBiometricSecret(context)
        val secret2 = BiometricHelper.getOrGenerateBiometricSecret(context)
        assertNotNull(secret1)
        assertEquals(secret1, secret2)
    }

    @Test
    fun `vault decryption is protected when session is locked`() {
        assertFalse(KeyStorage.isUnlocked.value)
        assertEquals(null, KeyStorage.getMasterKey())
    }

    @Test
    fun `vault allows decryption only after key derivation and store`() {
        val masterKey = CryptoManager.deriveMasterKey(
            nfcUid = "04:A2:3B:5F",
            biometricSecret = "TEST_BIO_SECRET",
            deviceSaltBase64 = CryptoManager.generateDeviceSalt()
        )
        KeyStorage.storeMasterKey(masterKey, "Primary e-KTP", "hash123")
        assertTrue(KeyStorage.isUnlocked.value)
        assertNotNull(KeyStorage.getMasterKey())

        val encrypted = CryptoManager.encryptPassword("SecretPassword123", KeyStorage.getMasterKey()!!)
        val decrypted = CryptoManager.decryptPassword(encrypted, KeyStorage.getMasterKey()!!)
        assertEquals("SecretPassword123", decrypted)

        KeyStorage.clear()
        assertFalse(KeyStorage.isUnlocked.value)
        assertEquals(null, KeyStorage.getMasterKey())
    }
}
