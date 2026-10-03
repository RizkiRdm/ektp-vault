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

        // 1. Generate true 256-bit Vault Master Key
        val vmk = securityEngine.generateVaultMasterKey()

        // 2. Wrap VMK for primary physical key
        val cardKek = securityEngine.deriveKeyEncryptionKey(rawUid, biometricSecret, salt)
        val wrappedMasterKey = securityEngine.wrapKey(vmk, cardKek)

        // 3. Wrap VMK with Recovery Phrase
        val recoveryKek = securityEngine.deriveRecoveryKey(recoveryPhrase, salt)
        val recoveryWrappedKey = securityEngine.wrapKey(vmk, recoveryKek)

        val meta = SecurityMetaEntity(
            id = 1,
            deviceSalt = salt,
            kdfType = "PBKDF2WithHmacSHA256",
            iterations = 65536,
            recoveryPhraseHash = phraseHash,
            recoveryWrappedKey = recoveryWrappedKey,
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
            cardType = "ISO 14443-4 e-KTP",
            wrappedMasterKey = wrappedMasterKey
        )
        physicalKeyDao.insertPhysicalKey(primaryKey)

        // 4. Store active VMK in memory for the initial session
        KeyStorage.storeMasterKey(vmk, primaryKey.label, uidHash, isPrimary = true)

        // 5. Securely wipe ephemeral keys
        securityEngine.wipeBytes(vmk)
        securityEngine.wipeBytes(cardKek)
        securityEngine.wipeBytes(recoveryKek)

        logAuditEvent("VAULT_INIT", "System", "SUCCESS", "Vault initialized with primary physical key and recovery phrase")

        recoveryPhrase
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

            val cardKek = securityEngine.deriveKeyEncryptionKey(rawUid, biometricSecret, meta.deviceSalt)
            val masterKey = if (physicalKey.wrappedMasterKey.isNotBlank()) {
                securityEngine.unwrapKey(physicalKey.wrappedMasterKey, cardKek)
            } else {
                securityEngine.deriveMasterKey(rawUid, biometricSecret, meta.deviceSalt)
            }

            KeyStorage.storeMasterKey(masterKey, physicalKey.label, uidHash, isPrimary = physicalKey.isPrimary)
            securityEngine.wipeBytes(masterKey)
            securityEngine.wipeBytes(cardKek)

            logAuditEvent("AUTH_ATTEMPT", physicalKey.label, "SUCCESS", "Hardware verification passed (Primary: ${physicalKey.isPrimary})")
            Result.success(physicalKey.toDomain())
        } catch (e: Exception) {
            logAuditEvent("AUTH_ATTEMPT", "Hardware_NFC", "ERROR", e.message ?: "Authentication error")
            Result.failure(e)
        }
    }

    override suspend fun registerNewPhysicalKey(
        rawUid: String,
        label: String,
        techList: List<String>,
        biometricSecret: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentVmk = KeyStorage.getMasterKey()
                ?: return@withContext Result.failure(IllegalStateException("Vault harus dalam keadaan terbuka untuk mendaftarkan kunci baru."))

            val uidHash = securityEngine.hashNfcUid(rawUid)
            val existing = physicalKeyDao.getPhysicalKeyByHash(uidHash)
            if (existing != null) {
                return@withContext Result.failure(IllegalArgumentException("Kartu ini sudah terdaftar sebelumnya."))
            }

            val meta = securityMetaDao.getSecurityMeta()
                ?: return@withContext Result.failure(IllegalStateException("Metadata keamanan tidak ditemukan."))

            // Wrap VMK with the new card's KEK
            val newCardKek = securityEngine.deriveKeyEncryptionKey(rawUid, biometricSecret, meta.deviceSalt)
            val wrapped = securityEngine.wrapKey(currentVmk, newCardKek)
            securityEngine.wipeBytes(newCardKek)

            val newKey = PhysicalKeyEntity(
                uidHash = uidHash,
                label = label.ifBlank { "Kartu NFC Tambahan" },
                techListRaw = techList.joinToString(",").ifBlank { "IsoDep,NfcA" },
                registeredAt = System.currentTimeMillis(),
                isPrimary = false,
                cardType = if (techList.contains("IsoDep")) "ISO 14443-4 Smart Card" else "NFC Contactless Tag",
                wrappedMasterKey = wrapped
            )
            physicalKeyDao.insertPhysicalKey(newKey)
            logAuditEvent("KEY_REGISTER", newKey.label, "SUCCESS", "Registered secondary hardware key")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun renamePhysicalKey(uidHash: String, newLabel: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val clean = newLabel.trim()
            if (clean.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Label kunci tidak boleh kosong."))
            }
            physicalKeyDao.renameKey(uidHash, clean)
            logAuditEvent("KEY_RENAME", clean, "SUCCESS", "Physical key renamed")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun makePhysicalKeyPrimary(uidHash: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val targetKey = physicalKeyDao.getPhysicalKeyByHash(uidHash)
                ?: return@withContext Result.failure(IllegalArgumentException("Kunci tidak ditemukan."))
            physicalKeyDao.setOnlyOnePrimary(uidHash)
            logAuditEvent("KEY_PRIMARY", targetKey.label, "SUCCESS", "Assigned as new Primary Key")
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
                ?: return@withContext Result.failure(IllegalArgumentException("Kunci tidak ditemukan."))
            if (entity.isPrimary) {
                return@withContext Result.failure(IllegalStateException("Kunci utama tidak dapat dihapus. Silakan tetapkan kunci lain sebagai utama terlebih dahulu."))
            }
            physicalKeyDao.deletePhysicalKey(entity)
            logAuditEvent("KEY_REMOVE", entity.label, "SUCCESS", "Removed physical key")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recoverVaultWithPhrase(
        phraseWords: List<String>,
        newRawUid: String,
        biometricSecret: String,
        newLabel: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val meta = securityMetaDao.getSecurityMeta()
                ?: return@withContext Result.failure(IllegalStateException("Vault tidak ditemukan."))

            val enteredHash = securityEngine.hashPhrase(phraseWords)
            if (enteredHash != meta.recoveryPhraseHash) {
                logAuditEvent("RECOVERY", "Mnemonic_BIP39", "FAILED", "Invalid recovery phrase")
                return@withContext Result.failure(SecurityException("Recovery phrase 12 kata tidak valid."))
            }

            val recoveryKek = securityEngine.deriveRecoveryKey(phraseWords, meta.deviceSalt)
            val trueVmk = if (meta.recoveryWrappedKey.isNotBlank()) {
                securityEngine.unwrapKey(meta.recoveryWrappedKey, recoveryKek)
            } else {
                securityEngine.deriveMasterKey(newRawUid, biometricSecret, meta.deviceSalt)
            }
            securityEngine.wipeBytes(recoveryKek)

            val uidHash = securityEngine.hashNfcUid(newRawUid)
            val newCardKek = securityEngine.deriveKeyEncryptionKey(newRawUid, biometricSecret, meta.deviceSalt)
            val newWrapped = securityEngine.wrapKey(trueVmk, newCardKek)
            securityEngine.wipeBytes(newCardKek)

            val newPrimaryKey = PhysicalKeyEntity(
                uidHash = uidHash,
                label = newLabel.ifBlank { "Primary e-KTP (Recovered)" },
                techListRaw = "IsoDep,NfcA,ISO 14443-4",
                registeredAt = System.currentTimeMillis(),
                isPrimary = true,
                cardType = "ISO 14443-4 e-KTP",
                wrappedMasterKey = newWrapped
            )
            physicalKeyDao.insertPhysicalKey(newPrimaryKey)
            physicalKeyDao.setOnlyOnePrimary(uidHash)

            KeyStorage.storeMasterKey(trueVmk, newPrimaryKey.label, uidHash, isPrimary = true)
            securityEngine.wipeBytes(trueVmk)

            logAuditEvent("RECOVERY", newPrimaryKey.label, "SUCCESS", "Vault recovered and primary key reassigned")
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
