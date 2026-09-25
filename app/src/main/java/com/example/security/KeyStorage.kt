package com.example.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object KeyStorage {
    private var activeMasterKey: ByteArray? = null
    private var keyCreatedTime: Long = 0
    private const val SESSION_TIMEOUT_MS = 5 * 60 * 1000L // 5 minutes

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _activeCardLabel = MutableStateFlow<String?>("e-KTP")
    val activeCardLabel: StateFlow<String?> = _activeCardLabel.asStateFlow()

    private val _activeUidHash = MutableStateFlow<String?>(null)
    val activeUidHash: StateFlow<String?> = _activeUidHash.asStateFlow()

    fun storeMasterKey(key: ByteArray, cardLabel: String, uidHash: String) {
        // If an old key existed, wipe it first
        clear()
        activeMasterKey = key.clone()
        keyCreatedTime = System.currentTimeMillis()
        _activeCardLabel.value = cardLabel
        _activeUidHash.value = uidHash
        _isUnlocked.value = true
    }

    fun getMasterKey(): ByteArray? {
        if (activeMasterKey == null) return null
        if (System.currentTimeMillis() - keyCreatedTime > SESSION_TIMEOUT_MS) {
            clear()
            return null
        }
        return activeMasterKey
    }

    // Plan A: Single-Use Protection for High-Risk accounts
    fun wipeImmediateHighRisk() {
        clear()
    }

    fun clear() {
        activeMasterKey?.let { CryptoManager.wipeBytes(it) }
        activeMasterKey = null
        keyCreatedTime = 0
        _isUnlocked.value = false
    }

    fun updateTouch() {
        if (activeMasterKey != null) {
            keyCreatedTime = System.currentTimeMillis()
        }
    }

    val isHardwareProtected: Boolean
        get() = KeystoreManager.isKeyMaterialProtected() || KeystoreManager.isHardwareBacked()
}
