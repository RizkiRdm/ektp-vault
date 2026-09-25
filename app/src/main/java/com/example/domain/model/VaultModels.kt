package com.example.domain.model

enum class SecurityCategory {
    STANDARD,
    HIGH_RISK
}

enum class TargetType {
    APP,
    WEB
}

data class Credential(
    val id: String,
    val serviceName: String,
    val username: String,
    val passwordPlain: String? = null,
    val encryptedBlob: String,
    val targetType: TargetType,
    val targetId: String,
    val securityCategory: SecurityCategory,
    val lastUsedTimestamp: Long,
    val notes: String = ""
)

data class PhysicalKey(
    val uidHash: String,
    val label: String,
    val techList: List<String> = emptyList(),
    val registeredAt: Long,
    val isPrimary: Boolean,
    val cardType: String = "ISO 14443-4 e-KTP"
)

data class SecurityMeta(
    val deviceSalt: String,
    val kdfType: String,
    val iterations: Int,
    val recoveryPhraseHash: String,
    val vaultInitialized: Boolean
)

data class NdefParsedRecord(
    val tnf: Short,
    val type: String,
    val id: String?,
    val payloadText: String?,
    val payloadUri: String?,
    val mimeType: String?
)

data class NfcTagData(
    val rawUid: ByteArray,
    val hexUid: String,
    val techList: List<String>,
    val isIsoDep: Boolean,
    val historicalBytesHex: String? = null,
    val ndefRecords: List<NdefParsedRecord> = emptyList(),
    val isEktpCompatible: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NfcTagData) return false
        return rawUid.contentEquals(other.rawUid) && hexUid == other.hexUid
    }

    override fun hashCode(): Int = rawUid.contentHashCode()
}

data class AuditLog(
    val id: Long = 0,
    val eventType: String,
    val timestamp: Long = System.currentTimeMillis(),
    val targetService: String,
    val status: String,
    val details: String
)
