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

    private val _activeCardLabel = MutableStateFlow<String?>(null)
    val activeCardLabel: StateFlow<String?> = _activeCardLabel.asStateFlow()

    private val _activeUidHash = MutableStateFlow<String?>(null)
    val activeUidHash: StateFlow<String?> = _activeUidHash.asStateFlow()

    private val _isSessionPrimary = MutableStateFlow(false)
    val isSessionPrimary: StateFlow<Boolean> = _isSessionPrimary.asStateFlow()

    fun storeMasterKey(key: ByteArray, cardLabel: String, uidHash: String, isPrimary: Boolean = false) {
        // Wipe old key before storing new key
        clear()
        activeMasterKey = key.clone()
        keyCreatedTime = System.currentTimeMillis()
        _activeCardLabel.value = cardLabel
        _activeUidHash.value = uidHash
        _isSessionPrimary.value = isPrimary
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

    // High-Risk single-use auto wipe
    fun wipeImmediateHighRisk() {
        clear()
    }

    fun clear() {
        activeMasterKey?.let { CryptoManager.wipeBytes(it) }
        activeMasterKey = null
        keyCreatedTime = 0
        _activeCardLabel.value = null
        _activeUidHash.value = null
        _isSessionPrimary.value = false
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
