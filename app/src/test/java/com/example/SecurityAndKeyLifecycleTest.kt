package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.VaultDatabase
import com.example.data.repository.VaultRepository
import com.example.domain.model.SecurityCategory
import com.example.domain.model.TargetType
import com.example.security.CryptoManager
import com.example.security.KeyStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecurityAndKeyLifecycleTest {

    private lateinit var db: VaultDatabase
    private lateinit var repository: VaultRepository

    @Before
    fun setup() {
        KeyStorage.clear()
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, VaultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = VaultRepository(
            credentialDao = db.credentialDao(),
            physicalKeyDao = db.physicalKeyDao(),
            securityMetaDao = db.securityMetaDao(),
            auditLogDao = db.auditLogDao(),
            securityEngine = CryptoManager
        )
    }

    @After
    fun tearDown() {
        KeyStorage.clear()
        db.close()
    }

    @Test
    fun `fresh install has 0 credentials and 0 physical keys`() = runBlocking {
        val initialCreds = repository.getAllCredentials().first()
        val initialKeys = repository.getAllPhysicalKeys().first()

        assertEquals(0, initialCreds.size)
        assertEquals(0, initialKeys.size)
        assertFalse(repository.isVaultInitialized())
    }

    @Test
    fun `key storage starts in locked state`() {
        assertFalse(KeyStorage.isUnlocked.value)
        assertNull(KeyStorage.getMasterKey())
        assertNull(KeyStorage.activeCardLabel.value)
    }

    @Test
    fun `setupInitialVault creates 1 primary key and recovery wrapped key`() = runBlocking {
        val rawUid = "04:12:34:56:78:90"
        val bioSecret = "BIO_SECRET_TEST_999"
        val phrase = repository.setupInitialVault(rawUid, bioSecret, "KTP Utama")

        assertEquals(12, phrase.size)
        assertTrue(repository.isVaultInitialized())

        val keys = repository.getAllPhysicalKeys().first()
        assertEquals(1, keys.size)
        assertTrue(keys[0].isPrimary)
        assertEquals("KTP Utama", keys[0].label)

        // Master key is stored in session
        assertTrue(KeyStorage.isUnlocked.value)
        assertNotNull(KeyStorage.getMasterKey())
    }

    @Test
    fun `credentials encrypted with master key and can be decrypted`() = runBlocking {
        val rawUid = "04:12:34:56:78:90"
        val bioSecret = "BIO_SECRET_TEST_999"
        repository.setupInitialVault(rawUid, bioSecret, "KTP Utama")

        val plainPass = "SuperSecret#2026"
        val addRes = repository.addCredential(
            serviceName = "BCA Mobile",
            username = "user_bca",
            passwordPlain = plainPass,
            targetType = TargetType.APP,
            targetId = "com.bca",
            securityCategory = SecurityCategory.STANDARD,
            notes = "Test notes"
        )
        assertTrue(addRes.isSuccess)

        val creds = repository.getAllCredentials().first()
        assertEquals(1, creds.size)
        val storedCred = creds[0]
        assertTrue(storedCred.encryptedBlob.isNotBlank())
        assertTrue(storedCred.encryptedBlob != plainPass)

        // Decrypt password
        val decResult = repository.decryptPassword(storedCred.encryptedBlob)
        assertTrue(decResult.isSuccess)
        assertEquals(plainPass, decResult.getOrNull())
    }

    @Test
    fun `cannot delete sole physical key`() = runBlocking {
        val rawUid = "04:12:34:56:78:90"
        val bioSecret = "BIO_SECRET_TEST_999"
        repository.setupInitialVault(rawUid, bioSecret, "KTP Utama")

        val keys = repository.getAllPhysicalKeys().first()
        assertEquals(1, keys.size)

        val removeRes = repository.removePhysicalKey(keys[0].uidHash)
        assertFalse(removeRes.isSuccess)
        assertTrue(removeRes.exceptionOrNull()?.message?.contains("Minimal") == true)
    }

    @Test
    fun `multiple keys lifecycle rename make primary and delete secondary`() = runBlocking {
        val rawUid1 = "04:11:11:11:11:11"
        val rawUid2 = "04:22:22:22:22:22"
        val bioSecret = "BIO_SECRET_TEST_999"

        repository.setupInitialVault(rawUid1, bioSecret, "KTP 1")

        // Register second key
        val regRes = repository.registerNewPhysicalKey(rawUid2, "KTP 2", listOf("IsoDep", "NfcA"), bioSecret)
        assertTrue(regRes.isSuccess)

        var keys = repository.getAllPhysicalKeys().first()
        assertEquals(2, keys.size)

        val key2Hash = CryptoManager.hashNfcUid(rawUid2)
        val key1Hash = CryptoManager.hashNfcUid(rawUid1)

        // Rename key 2
        val renameRes = repository.renamePhysicalKey(key2Hash, "KTP Cadangan Baru")
        assertTrue(renameRes.isSuccess)
        keys = repository.getAllPhysicalKeys().first()
        val renamed = keys.find { it.uidHash == key2Hash }
        assertEquals("KTP Cadangan Baru", renamed?.label)

        // Cannot delete primary key directly
        val delPrimaryRes = repository.removePhysicalKey(key1Hash)
        assertFalse(delPrimaryRes.isSuccess)

        // Make key 2 primary
        val makePrimaryRes = repository.makePhysicalKeyPrimary(key2Hash)
        assertTrue(makePrimaryRes.isSuccess)

        keys = repository.getAllPhysicalKeys().first()
        val updatedKey2 = keys.find { it.uidHash == key2Hash }
        val updatedKey1 = keys.find { it.uidHash == key1Hash }
        assertTrue(updatedKey2?.isPrimary == true)
        assertFalse(updatedKey1?.isPrimary == true)

        // Now key 1 is secondary and can be deleted
        val delKey1Res = repository.removePhysicalKey(key1Hash)
        assertTrue(delKey1Res.isSuccess)

        keys = repository.getAllPhysicalKeys().first()
        assertEquals(1, keys.size)
        assertEquals(key2Hash, keys[0].uidHash)
    }

    @Test
    fun `recovery phrase restores master key and re-anchors to new card`() = runBlocking {
        val rawUid1 = "04:AA:AA:AA:AA:AA"
        val bioSecret = "BIO_SECRET_TEST_999"
        val phrase = repository.setupInitialVault(rawUid1, bioSecret, "KTP Hilang")

        // Add a secret
        val secretPass = "PasswordToRecover123"
        repository.addCredential("Mandiri Livin", "nasabah1", secretPass, TargetType.APP, "id.bmri.livin", SecurityCategory.STANDARD, "")

        val credsBefore = repository.getAllCredentials().first()
        val blob = credsBefore[0].encryptedBlob

        // Simulate lost card and locked session
        KeyStorage.clear()
        assertFalse(KeyStorage.isUnlocked.value)

        // Recover with phrase using a brand new card
        val newRawUid = "04:BB:BB:BB:BB:BB"
        val recoverRes = repository.recoverVaultWithPhrase(phrase, newRawUid, bioSecret, "KTP Pengganti")
        assertTrue(recoverRes.isSuccess)

        assertTrue(KeyStorage.isUnlocked.value)

        // Master key was preserved - can decrypt previous password!
        val decResult = repository.decryptPassword(blob)
        assertTrue(decResult.isSuccess)
        assertEquals(secretPass, decResult.getOrNull())

        // New card is primary
        val keys = repository.getAllPhysicalKeys().first()
        val newKeyHash = CryptoManager.hashNfcUid(newRawUid)
        val primary = keys.find { it.isPrimary }
        assertEquals(newKeyHash, primary?.uidHash)
    }
}
