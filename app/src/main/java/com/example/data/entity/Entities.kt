package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "credentials",
    indices = [
        Index(value = ["targetId", "username"], unique = true),
        Index(value = ["securityCategory"]),
        Index(value = ["lastUsedTimestamp"])
    ]
)
data class CredentialEntity(
    @PrimaryKey
    val id: String,
    val serviceName: String,
    val username: String,
    val encryptedBlob: String,
    val targetType: String,
    val targetId: String,
    val securityCategory: String,
    val lastUsedTimestamp: Long,
    val notes: String = ""
)

@Entity(tableName = "physical_keys")
data class PhysicalKeyEntity(
    @PrimaryKey
    val uidHash: String,
    val label: String,
    val techListRaw: String = "IsoDep,NfcA",
    val registeredAt: Long,
    val isPrimary: Boolean = false,
    val cardType: String = "ISO 14443-4 e-KTP",
    val wrappedMasterKey: String = ""
)

@Entity(tableName = "security_meta")
data class SecurityMetaEntity(
    @PrimaryKey
    val id: Int = 1,
    val deviceSalt: String,
    val kdfType: String = "PBKDF2WithHmacSHA256",
    val iterations: Int = 65536,
    val recoveryPhraseHash: String,
    val recoveryWrappedKey: String = "",
    val vaultInitialized: Boolean = false,
    val lastAuditTimestamp: Long = 0L
)

@Entity(
    tableName = "audit_logs",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["eventType"])
    ]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventType: String,
    val timestamp: Long,
    val targetService: String,
    val status: String,
    val details: String
)
