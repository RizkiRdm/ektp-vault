package com.example.data.repository

import com.example.data.dao.AuditLogDao
import com.example.data.dao.CredentialDao
import com.example.data.dao.PhysicalKeyDao
import com.example.data.dao.SecurityMetaDao
import com.example.data.entity.AuditLogEntity
import com.example.data.entity.CredentialEntity
import com.example.data.entity.PhysicalKeyEntity
import com.example.data.entity.SecurityMetaEntity
import com.example.domain.model.AuditLog
import com.example.domain.model.Credential
import com.example.domain.model.PhysicalKey
import com.example.domain.model.SecurityCategory
import com.example.domain.model.SecurityMeta
import com.example.domain.model.TargetType
import com.example.domain.repository.ISecurityEngine
import com.example.domain.repository.IVaultRepository
import com.example.security.CryptoManager
import com.example.security.KeyStorage
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class VaultRepository(
    private val credentialDao: CredentialDao,
    private val physicalKeyDao: PhysicalKeyDao,
    private val securityMetaDao: SecurityMetaDao,
    private val auditLogDao: AuditLogDao,
    private val securityEngine: ISecurityEngine = CryptoManager
) : IVaultRepository {

    override fun getAllCredentials(): Flow<List<Credential>> {
        return credentialDao.getAllCredentials().map { list -> list.map { it.toDomain() } }
    }

    override fun searchCredentials(query: String): Flow<List<Credential>> {
        return credentialDao.searchCredentials(query).map { list -> list.map { it.toDomain() } }
    }

    override fun getAllPhysicalKeys(): Flow<List<PhysicalKey>> {
        return physicalKeyDao.getAllPhysicalKeys().map { list -> list.map { it.toDomain() } }
    }

    override fun getAuditLogs(): Flow<List<AuditLog>> {
        return auditLogDao.getAllAuditLogs().map { list ->
            list.map {
                AuditLog(
                    id = it.id,
                    eventType = it.eventType,
                    timestamp = it.timestamp,
                    targetService = it.targetService,
                    status = it.status,
                    details = it.details
                )
            }
        }
    }

    override suspend fun isVaultInitialized(): Boolean = withContext(Dispatchers.IO) {
        val meta = securityMetaDao.getSecurityMeta()
        meta?.vaultInitialized == true
    }

    override suspend fun getSecurityMeta(): SecurityMeta? = withContext(Dispatchers.IO) {
        securityMetaDao.getSecurityMeta()?.let {
            SecurityMeta(
                deviceSalt = it.deviceSalt,
                kdfType = it.kdfType,
                iterations = it.iterations,
                recoveryPhraseHash = it.recoveryPhraseHash,
                vaultInitialized = it.vaultInitialized
            )
        }
    }

    override suspend fun setupInitialVault(
        rawUid: String,
        biometricSecret: String,
        label: String
    ): List<String> = withContext(Dispatchers.IO) {
        val salt = securityEngine.generateDeviceSalt()
        val recoveryPhrase = securityEngine.generateRecoveryPhrase()
        val phraseHash = securityEngine.hashPhrase(recoveryPhrase)
        val uidHash = securityEngine.hashNfcUid(rawUid)

        val meta = SecurityMetaEntity(
            id = 1,
            deviceSalt = salt,
            kdfType = "PBKDF2WithHmacSHA256",
            iterations = 65536,
            recoveryPhraseHash = phraseHash,
            vaultInitialized = true,
            lastAuditTimestamp = System.currentTimeMillis()
        )
        securityMetaDao.insertSecurityMeta(meta)

        val primaryKey = PhysicalKeyEntity(
            uidHash = uidHash,
            label = label.ifBlank { "Primary e-KTP" },
            techListRaw = "IsoDep,NfcA,ISO 14443-4",
            registeredAt = System.currentTimeMillis(),
            isPrimary = true,
            cardType = "ISO 14443-4 e-KTP"
        )
        physicalKeyDao.insertPhysicalKey(primaryKey)

        val masterKey = securityEngine.deriveMasterKey(rawUid, biometricSecret, salt)
        KeyStorage.storeMasterKey(masterKey, primaryKey.label, uidHash)
        securityEngine.wipeBytes(masterKey)

        seedInitialSampleData(KeyStorage.getMasterKey()!!)
        logAuditEvent("VAULT_INIT", "System", "SUCCESS", "Vault initialized with primary e-KTP")

        recoveryPhrase
    }

    private suspend fun seedInitialSampleData(masterKeyBytes: ByteArray) {
        val sample1 = CredentialEntity(
            id = UUID.randomUUID().toString(),
            serviceName = "Bank Central Asia (KlikBCA)",
            username = "fufufafa_99",
            encryptedBlob = securityEngine.encryptPassword("BcaSecure#2026!", masterKeyBytes),
            targetType = TargetType.APP.name,
            targetId = "com.bca",
            securityCategory = SecurityCategory.HIGH_RISK.name,
            lastUsedTimestamp = System.currentTimeMillis(),
            notes = "Rekening utama & e-banking"
        )
        val sample2 = CredentialEntity(
            id = UUID.randomUUID().toString(),
            serviceName = "NEVERHACK Sovereign Console",
            username = "sec-ops@neverhack.com",
            encryptedBlob = securityEngine.encryptPassword("NeverHack#Sovereign99!", masterKeyBytes),
            targetType = TargetType.WEB.name,
            targetId = "console.neverhack.com",
            securityCategory = SecurityCategory.HIGH_RISK.name,
            lastUsedTimestamp = System.currentTimeMillis() - 3600000L,
            notes = "Cybersecurity command terminal root"
        )
        val sample3 = CredentialEntity(
            id = UUID.randomUUID().toString(),
            serviceName = "GoTo Financial (Gojek/GoPay)",
            username = "romdhoni.vault",
            encryptedBlob = securityEngine.encryptPassword("GoPay#PIN9876", masterKeyBytes),
            targetType = TargetType.APP.name,
            targetId = "com.gojek.app",
            securityCategory = SecurityCategory.HIGH_RISK.name,
            lastUsedTimestamp = System.currentTimeMillis() - 18000000L,
            notes = "E-wallet & transaksi digital"
        )
        val sample4 = CredentialEntity(
            id = UUID.randomUUID().toString(),
            serviceName = "GitHub Enterprise",
            username = "ktp-engineer",
            encryptedBlob = securityEngine.encryptPassword("ghp_SecretTokenKtpVault2026", masterKeyBytes),
            targetType = TargetType.WEB.name,
            targetId = "github.com",
            securityCategory = SecurityCategory.STANDARD.name,
            lastUsedTimestamp = System.currentTimeMillis() - 86400000L,
            notes = "Work repositories & deploy keys"
        )
        credentialDao.insertCredential(sample1)
        credentialDao.insertCredential(sample2)
        credentialDao.insertCredential(sample3)
        credentialDao.insertCredential(sample4)
    }

    override suspend fun authenticateWithCard(
        rawUid: String,
        biometricSecret: String
    ): Result<PhysicalKey> = withContext(Dispatchers.IO) {
        try {
            val uidHash = securityEngine.hashNfcUid(rawUid)
            val physicalKey = physicalKeyDao.getPhysicalKeyByHash(uidHash)
                ?: run {
                    logAuditEvent("AUTH_ATTEMPT", "Hardware_NFC", "FAILED", "Unregistered card tap")
                    return@withContext Result.failure(SecurityException("Kartu fisik tidak terdaftar dalam Vault."))
                }

            val meta = securityMetaDao.getSecurityMeta()
                ?: return@withContext Result.failure(IllegalStateException("Vault belum terkonfigurasi."))

            val masterKey = securityEngine.deriveMasterKey(rawUid, biometricSecret, meta.deviceSalt)
            KeyStorage.storeMasterKey(masterKey, physicalKey.label, uidHash)
            securityEngine.wipeBytes(masterKey)

            logAuditEvent("AUTH_ATTEMPT", physicalKey.label, "SUCCESS", "Hardware verification passed")
            Result.success(physicalKey.toDomain())
        } catch (e: Exception) {
            logAuditEvent("AUTH_ATTEMPT", "Hardware_NFC", "ERROR", e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    override suspend fun registerNewPhysicalKey(
        rawUid: String,
        label: String,
        techList: List<String>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uidHash = securityEngine.hashNfcUid(rawUid)
            val existing = physicalKeyDao.getPhysicalKeyByHash(uidHash)
            if (existing != null) {
                return@withContext Result.failure(IllegalArgumentException("Kartu ini sudah terdaftar sebelumnya."))
            }

            val newKey = PhysicalKeyEntity(
                uidHash = uidHash,
                label = label.ifBlank { "Kartu NFC Kustom" },
                techListRaw = techList.joinToString(",").ifBlank { "IsoDep,NfcA" },
                registeredAt = System.currentTimeMillis(),
                isPrimary = false,
                cardType = if (techList.contains("IsoDep")) "ISO 14443-4 Smart Card" else "NFC Contactless Tag"
            )
            physicalKeyDao.insertPhysicalKey(newKey)
            logAuditEvent("KEY_REGISTER", newKey.label, "SUCCESS", "Registered secondary hardware key")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removePhysicalKey(uidHash: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val count = physicalKeyDao.getPhysicalKeysCount()
            if (count <= 1) {
                return@withContext Result.failure(IllegalStateException("Minimal harus menyisakan 1 kunci fisik terdaftar."))
            }
            val entity = physicalKeyDao.getPhysicalKeyByHash(uidHash)
            if (entity != null) {
                physicalKeyDao.deletePhysicalKey(entity)
                logAuditEvent("KEY_REMOVE", entity.label, "SUCCESS", "Removed physical key")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recoverVaultWithPhrase(
        phraseWords: List<String>,
        newRawUid: String,
        biometricSecret: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val meta = securityMetaDao.getSecurityMeta()
                ?: return@withContext Result.failure(IllegalStateException("Vault tidak ditemukan."))

            val enteredHash = securityEngine.hashPhrase(phraseWords)
            if (enteredHash != meta.recoveryPhraseHash) {
                logAuditEvent("RECOVERY", "Mnemonic_BIP39", "FAILED", "Invalid recovery phrase")
                return@withContext Result.failure(SecurityException("Recovery phrase 12 kata tidak valid."))
            }

            val uidHash = securityEngine.hashNfcUid(newRawUid)
            val newPrimaryKey = PhysicalKeyEntity(
                uidHash = uidHash,
                label = "KTP Pengganti (Recovered)",
                techListRaw = "IsoDep,NfcA,ISO 14443-4",
                registeredAt = System.currentTimeMillis(),
                isPrimary = true,
                cardType = "ISO 14443-4 e-KTP"
            )
            physicalKeyDao.insertPhysicalKey(newPrimaryKey)

            val masterKey = securityEngine.deriveMasterKey(newRawUid, biometricSecret, meta.deviceSalt)
            KeyStorage.storeMasterKey(masterKey, newPrimaryKey.label, uidHash)
            securityEngine.wipeBytes(masterKey)

            logAuditEvent("RECOVERY", newPrimaryKey.label, "SUCCESS", "Emergency vault recovery completed")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addCredential(
        serviceName: String,
        username: String,
        passwordPlain: String,
        targetType: TargetType,
        targetId: String,
        securityCategory: SecurityCategory,
        notes: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val masterKey = KeyStorage.getMasterKey()
                ?: return@withContext Result.failure(IllegalStateException("Sesi terkunci. Silakan tap KTP terlebih dahulu."))

            val encrypted = securityEngine.encryptPassword(passwordPlain, masterKey)
            val entity = CredentialEntity(
                id = UUID.randomUUID().toString(),
                serviceName = serviceName.trim(),
                username = username.trim(),
                encryptedBlob = encrypted,
                targetType = targetType.name,
                targetId = targetId.trim().lowercase(),
                securityCategory = securityCategory.name,
                lastUsedTimestamp = System.currentTimeMillis(),
                notes = notes.trim()
            )
            credentialDao.insertCredential(entity)
            logAuditEvent("CRED_ADD", serviceName, "SUCCESS", "Encrypted record stored")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateCredential(
        id: String,
        serviceName: String,
        username: String,
        newPasswordPlain: String?,
        targetType: TargetType,
        targetId: String,
        securityCategory: SecurityCategory,
        notes: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = credentialDao.getCredentialById(id)
                ?: return@withContext Result.failure(IllegalArgumentException("Kredensial tidak ditemukan."))

            val encryptedBlob = if (!newPasswordPlain.isNullOrBlank()) {
                val masterKey = KeyStorage.getMasterKey()
                    ?: return@withContext Result.failure(IllegalStateException("Sesi terkunci. Silakan tap KTP."))
                securityEngine.encryptPassword(newPasswordPlain, masterKey)
            } else {
                existing.encryptedBlob
            }

            val updated = existing.copy(
                serviceName = serviceName.trim(),
                username = username.trim(),
                encryptedBlob = encryptedBlob,
                targetType = targetType.name,
                targetId = targetId.trim().lowercase(),
                securityCategory = securityCategory.name,
                notes = notes.trim()
            )
            credentialDao.updateCredential(updated)
            logAuditEvent("CRED_UPDATE", serviceName, "SUCCESS", "Updated record")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSecurityCategory(id: String, category: SecurityCategory): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            credentialDao.updateSecurityCategory(id, category.name)
            logAuditEvent("RISK_UPDATE", id, "SUCCESS", "Category updated to ${category.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCredential(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            credentialDao.deleteCredentialById(id)
            logAuditEvent("CRED_DELETE", id, "SUCCESS", "Record removed")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun decryptPassword(encryptedBlob: String): Result<String> = withContext(Dispatchers.Default) {
        try {
            val masterKey = KeyStorage.getMasterKey()
                ?: return@withContext Result.failure(IllegalStateException("Sesi terkunci. Diperlukan Tap KTP & Biometrik."))
            val plaintext = securityEngine.decryptPassword(encryptedBlob, masterKey)
            logAuditEvent("DECRYPT", "Password_Field", "SUCCESS", "AES-256-GCM decryption granted")
            Result.success(plaintext)
        } catch (e: Exception) {
            logAuditEvent("DECRYPT", "Password_Field", "FAILED", e.message ?: "Decryption error")
            Result.failure(e)
        }
    }

    override suspend fun updateLastUsed(id: String) = withContext(Dispatchers.IO) {
        credentialDao.updateLastUsed(id, System.currentTimeMillis())
    }

    override suspend fun exportVaultToLoker(backupPassword: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val activeUidHash = KeyStorage.activeUidHash.value
                ?: return@withContext Result.failure(IllegalStateException("Vault belum terotentikasi dengan KTP."))

            val snapshot = credentialDao.getAllCredentials().first()
            val jsonArray = JSONArray()
            snapshot.forEach { cred ->
                val obj = JSONObject().apply {
                    put("id", cred.id)
                    put("serviceName", cred.serviceName)
                    put("username", cred.username)
                    put("encryptedBlob", cred.encryptedBlob)
                    put("targetType", cred.targetType)
                    put("targetId", cred.targetId)
                    put("securityCategory", cred.securityCategory)
                    put("lastUsedTimestamp", cred.lastUsedTimestamp)
                    put("notes", cred.notes)
                }
                jsonArray.put(obj)
            }

            val lokerContent = securityEngine.createLokerBackup(
                credentialsJson = jsonArray.toString(),
                backupPassword = backupPassword,
                nfcUidHash = activeUidHash
            )
            logAuditEvent("EXPORT", "Vault_Backup", "SUCCESS", "Generated encrypted .loker envelope")
            Result.success(lokerContent)
        } catch (e: Exception) {
            logAuditEvent("EXPORT", "Vault_Backup", "FAILED", e.message ?: "Export error")
            Result.failure(e)
        }
    }

    override suspend fun importVaultFromLoker(
        lokerContent: String,
        backupPassword: String,
        currentNfcUid: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val currentHash = securityEngine.hashNfcUid(currentNfcUid)
            val decryptedJson = securityEngine.restoreLokerBackup(
                backupContent = lokerContent,
                backupPassword = backupPassword,
                currentNfcUidHash = currentHash
            )

            val jsonArray = JSONArray(decryptedJson)
            var importedCount = 0
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val entity = CredentialEntity(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    serviceName = obj.getString("serviceName"),
                    username = obj.getString("username"),
                    encryptedBlob = obj.getString("encryptedBlob"),
                    targetType = obj.optString("targetType", TargetType.APP.name),
                    targetId = obj.optString("targetId", ""),
                    securityCategory = obj.optString("securityCategory", SecurityCategory.STANDARD.name),
                    lastUsedTimestamp = obj.optLong("lastUsedTimestamp", System.currentTimeMillis()),
                    notes = obj.optString("notes", "")
                )
                credentialDao.insertCredential(entity)
                importedCount++
            }
            logAuditEvent("IMPORT", "Vault_Backup", "SUCCESS", "Imported $importedCount credentials")
            Result.success(importedCount)
        } catch (e: Exception) {
            logAuditEvent("IMPORT", "Vault_Backup", "FAILED", e.message ?: "Import error")
            Result.failure(e)
        }
    }

    override suspend fun logAuditEvent(eventType: String, targetService: String, status: String, details: String) {
        withContext(Dispatchers.IO) {
            try {
                auditLogDao.insertAuditLog(
                    AuditLogEntity(
                        eventType = eventType,
                        timestamp = System.currentTimeMillis(),
                        targetService = targetService,
                        status = status,
                        details = details
                    )
                )
            } catch (_: Exception) {
                // Ignore audit failure
            }
        }
    }

    private fun CredentialEntity.toDomain() = Credential(
        id = id,
        serviceName = serviceName,
        username = username,
        encryptedBlob = encryptedBlob,
        targetType = try { TargetType.valueOf(targetType) } catch (_: Exception) { TargetType.APP },
        targetId = targetId,
        securityCategory = try { SecurityCategory.valueOf(securityCategory) } catch (_: Exception) { SecurityCategory.STANDARD },
        lastUsedTimestamp = lastUsedTimestamp,
        notes = notes
    )

    private fun PhysicalKeyEntity.toDomain() = PhysicalKey(
        uidHash = uidHash,
        label = label,
        techList = techListRaw.split(",").filter { it.isNotBlank() },
        registeredAt = registeredAt,
        isPrimary = isPrimary,
        cardType = cardType
    )
}
