package com.example.domain.repository

import android.app.Activity
import android.content.Intent
import android.nfc.Tag
import com.example.domain.model.AuditLog
import com.example.domain.model.Credential
import com.example.domain.model.NfcTagData
import com.example.domain.model.PhysicalKey
import com.example.domain.model.SecurityCategory
import com.example.domain.model.SecurityMeta
import com.example.domain.model.TargetType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface IVaultRepository {
    fun getAllCredentials(): Flow<List<Credential>>
    fun searchCredentials(query: String): Flow<List<Credential>>
    fun getAllPhysicalKeys(): Flow<List<PhysicalKey>>
    fun getAuditLogs(): Flow<List<AuditLog>>
    suspend fun isVaultInitialized(): Boolean
    suspend fun getSecurityMeta(): SecurityMeta?
    suspend fun setupInitialVault(rawUid: String, biometricSecret: String, label: String): List<String>
    suspend fun authenticateWithCard(rawUid: String, biometricSecret: String): Result<PhysicalKey>
    suspend fun registerNewPhysicalKey(rawUid: String, label: String, techList: List<String>): Result<Unit>
    suspend fun removePhysicalKey(uidHash: String): Result<Unit>
    suspend fun recoverVaultWithPhrase(phraseWords: List<String>, newRawUid: String, biometricSecret: String): Result<Unit>
    suspend fun addCredential(serviceName: String, username: String, passwordPlain: String, targetType: TargetType, targetId: String, securityCategory: SecurityCategory, notes: String): Result<Unit>
    suspend fun updateCredential(id: String, serviceName: String, username: String, newPasswordPlain: String?, targetType: TargetType, targetId: String, securityCategory: SecurityCategory, notes: String): Result<Unit>
    suspend fun updateSecurityCategory(id: String, category: SecurityCategory): Result<Unit>
    suspend fun deleteCredential(id: String): Result<Unit>
    suspend fun decryptPassword(encryptedBlob: String): Result<String>
    suspend fun updateLastUsed(id: String)
    suspend fun exportVaultToLoker(backupPassword: String): Result<String>
    suspend fun importVaultFromLoker(lokerContent: String, backupPassword: String, currentNfcUid: String): Result<Int>
    suspend fun logAuditEvent(eventType: String, targetService: String, status: String, details: String)
}

interface INfcReaderService {
    val tagDiscoveryFlow: StateFlow<NfcTagData?>
    val isNfcAvailable: Boolean
    val isNfcEnabled: Boolean
    fun enableReaderMode(activity: Activity)
    fun disableReaderMode(activity: Activity)
    fun processIntent(intent: Intent): NfcTagData?
    fun parseTag(tag: Tag): NfcTagData
    fun verifyEktpSignature(tagData: NfcTagData): Boolean
}

interface ISecurityEngine {
    fun generateDeviceSalt(): String
    fun deriveMasterKey(nfcUid: String, biometricSecret: String, deviceSaltBase64: String): ByteArray
    fun hashNfcUid(rawUid: String): String
    fun encryptPassword(plaintext: String, masterKeyBytes: ByteArray): String
    fun decryptPassword(encryptedBlobBase64: String, masterKeyBytes: ByteArray): String
    fun generateRecoveryPhrase(): List<String>
    fun hashPhrase(phrase: List<String>): String
    fun createLokerBackup(credentialsJson: String, backupPassword: String, nfcUidHash: String): String
    fun restoreLokerBackup(backupContent: String, backupPassword: String, currentNfcUidHash: String): String
    fun wipeBytes(bytes: ByteArray?)
}
