package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.VaultDatabase
import com.example.data.entity.AuditLogEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuditLogTest {

    private lateinit var db: VaultDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, VaultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `insert and retrieve successful vault access audit log`() = runBlocking {
        val log = AuditLogEntity(
            eventType = "VAULT_UNLOCK",
            timestamp = System.currentTimeMillis(),
            targetService = "System",
            status = "SUCCESS",
            details = "Vault unlocked with e-KTP and Biometric"
        )
        db.auditLogDao().insertAuditLog(log)

        val logs = db.auditLogDao().getAllAuditLogs().first()
        assertEquals(1, logs.size)
        assertEquals("VAULT_UNLOCK", logs[0].eventType)
        assertEquals("SUCCESS", logs[0].status)
    }

    @Test
    fun `insert and retrieve failed vault access attempt audit log`() = runBlocking {
        val log = AuditLogEntity(
            eventType = "ACCESS_DENIED",
            timestamp = System.currentTimeMillis(),
            targetService = "Vault",
            status = "FAILED",
            details = "Access denied: Unauthorized card UID"
        )
        db.auditLogDao().insertAuditLog(log)

        val logs = db.auditLogDao().getAllAuditLogs().first()
        assertEquals(1, logs.size)
        assertEquals("FAILED", logs[0].status)
    }

    @Test
    fun `track biometric prompt interactions specifically`() = runBlocking {
        val bioSuccess = AuditLogEntity(
            eventType = "BIOMETRIC_AUTH",
            timestamp = System.currentTimeMillis() - 2000,
            targetService = "Password Decryption",
            status = "SUCCESS",
            details = "Biometric prompt verification succeeded via Android TEE"
        )
        val bioError = AuditLogEntity(
            eventType = "BIOMETRIC_ERROR",
            timestamp = System.currentTimeMillis() - 1000,
            targetService = "Password Decryption",
            status = "FAILED",
            details = "Biometric prompt error (Code 10): User canceled"
        )
        val nonBioLog = AuditLogEntity(
            eventType = "NFC_DETECTED",
            timestamp = System.currentTimeMillis(),
            targetService = "e-KTP Reader",
            status = "SUCCESS",
            details = "Contactless tag discovered"
        )

        db.auditLogDao().insertAuditLog(bioSuccess)
        db.auditLogDao().insertAuditLog(bioError)
        db.auditLogDao().insertAuditLog(nonBioLog)

        val allLogs = db.auditLogDao().getAllAuditLogs().first()
        assertEquals(3, allLogs.size)

        val bioLogs = db.auditLogDao().getBiometricAuditLogs().first()
        assertEquals(2, bioLogs.size)
        assertTrue(bioLogs.all { it.eventType.contains("BIOMETRIC") })
    }
}
