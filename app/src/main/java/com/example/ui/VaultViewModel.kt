package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.VaultApplication
import com.example.domain.model.AuditLog
import com.example.domain.model.Credential
import com.example.domain.model.NfcTagData
import com.example.domain.model.PhysicalKey
import com.example.domain.model.SecurityCategory
import com.example.domain.model.TargetType
import com.example.security.KeyStorage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ScreenState {
    data object Onboarding : ScreenState()
    data object Dashboard : ScreenState()
    data class AddEdit(val editingCredentialId: String? = null) : ScreenState()
    data class Detail(val credentialId: String) : ScreenState()
    data object PhysicalKeys : ScreenState()
    data object RecoveryPhrase : ScreenState()
    data object ExportImport : ScreenState()
    data object AuditLogs : ScreenState()
    data object InitialSetup : ScreenState()
}

enum class AuthSheetAction {
    UNLOCK_VAULT,
    REVEAL_PASSWORD,
    ADD_PHYSICAL_KEY,
    EXPORT_VAULT,
    SETUP_VAULT
}

data class AuthSheetState(
    val isOpen: Boolean = false,
    val action: AuthSheetAction = AuthSheetAction.UNLOCK_VAULT,
    val targetCredentialId: String? = null,
    val pendingLabel: String = "Kartu Tambahan",
    val isHighRiskAction: Boolean = false
)

data class VaultUiState(
    val isInitialized: Boolean = false,
    val isUnlocked: Boolean = false,
    val currentScreen: ScreenState = ScreenState.Onboarding,
    val searchQuery: String = "",
    val categoryFilter: SecurityCategory? = null,
    val activeCardLabel: String = "e-KTP (ID: ****89)",
    val authSheet: AuthSheetState = AuthSheetState(),
    val revealedPassword: String? = null,
    val revealCountdown: Int = 0,
    val recoveryPhrase: List<String>? = null,
    val failureCount: Int = 0,
    val lockoutSeconds: Int = 0,
    val statusMessage: String? = null,
    val exportedLokerContent: String? = null,
    val lastDiscoveredTag: NfcTagData? = null
)

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as VaultApplication).container
    private val repository = container.vaultRepository
    val nfcService = container.nfcReaderService

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    private val _searchFlow = MutableStateFlow("")

    val credentials: StateFlow<List<Credential>> = combine(
        container.getCredentialsUseCase(),
        _searchFlow,
        _uiState
    ) { allCreds, query, state ->
        allCreds.filter { cred ->
            val matchesQuery = query.isBlank() ||
                    cred.serviceName.contains(query, ignoreCase = true) ||
                    cred.username.contains(query, ignoreCase = true) ||
                    cred.notes.contains(query, ignoreCase = true)

            val matchesCategory = state.categoryFilter == null || cred.securityCategory == state.categoryFilter
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val physicalKeys: StateFlow<List<PhysicalKey>> = repository.getAllPhysicalKeys()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLog>> = container.getAuditLogsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var countdownJob: Job? = null
    private var lockoutJob: Job? = null

    init {
        checkInitialization()
        observeLockState()
        observeNfcHardware()
    }

    private fun checkInitialization() {
        viewModelScope.launch {
            val prefs = getApplication<Application>().getSharedPreferences("ktp_vault_prefs", android.content.Context.MODE_PRIVATE)
            val onboardingCompleted = prefs.getBoolean("onboarding_completed", false)
            val initialized = repository.isVaultInitialized()

            val initialScreen = when {
                !onboardingCompleted -> ScreenState.Onboarding
                !initialized -> ScreenState.InitialSetup
                else -> ScreenState.Dashboard
            }

            _uiState.value = _uiState.value.copy(
                isInitialized = initialized,
                currentScreen = initialScreen
            )
        }
    }

    fun completeOnboarding() {
        val prefs = getApplication<Application>().getSharedPreferences("ktp_vault_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("onboarding_completed", true).apply()
        val nextScreen = if (_uiState.value.isInitialized) ScreenState.Dashboard else ScreenState.InitialSetup
        _uiState.value = _uiState.value.copy(currentScreen = nextScreen)
    }

    private fun observeLockState() {
        viewModelScope.launch {
            KeyStorage.isUnlocked.collect { unlocked ->
                _uiState.value = _uiState.value.copy(isUnlocked = unlocked)
                if (!unlocked) {
                    cancelRevealTimer()
                }
            }
        }
        viewModelScope.launch {
            KeyStorage.activeCardLabel.collect { label ->
                if (label != null) {
                    val uidHash = KeyStorage.activeUidHash.value ?: "89"
                    val shortId = uidHash.takeLast(4).uppercase()
                    _uiState.value = _uiState.value.copy(
                        activeCardLabel = "$label (ID: ****$shortId)"
                    )
                }
            }
        }
    }

    private fun observeNfcHardware() {
        viewModelScope.launch {
            nfcService.tagDiscoveryFlow.collect { tagData ->
                if (tagData != null) {
                    _uiState.value = _uiState.value.copy(lastDiscoveredTag = tagData)
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchFlow.value = query
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setCategoryFilter(category: SecurityCategory?) {
        _uiState.value = _uiState.value.copy(categoryFilter = category)
    }

    fun navigateTo(screen: ScreenState) {
        _uiState.value = _uiState.value.copy(currentScreen = screen)
    }

    fun openAuthSheet(action: AuthSheetAction, targetId: String? = null, isHighRisk: Boolean = false, pendingLabel: String = "Kartu Tambahan") {
        if (_uiState.value.lockoutSeconds > 0) {
            showStatus("Sistem dalam status Lockout. Tunggu ${_uiState.value.lockoutSeconds}s.")
            return
        }
        _uiState.value = _uiState.value.copy(
            authSheet = AuthSheetState(
                isOpen = true,
                action = action,
                targetCredentialId = targetId,
                pendingLabel = pendingLabel,
                isHighRiskAction = isHighRisk
            )
        )
    }

    fun closeAuthSheet() {
        _uiState.value = _uiState.value.copy(
            authSheet = _uiState.value.authSheet.copy(isOpen = false)
        )
    }

    fun logAuditEvent(eventType: String, targetService: String, status: String, details: String) {
        viewModelScope.launch {
            repository.logAuditEvent(eventType, targetService, status, details)
        }
    }

    fun performNfcAndBiometricAuth(rawUid: String, biometricSecret: String) {
        if (_uiState.value.lockoutSeconds > 0) return

        val sheet = _uiState.value.authSheet
        viewModelScope.launch {
            when (sheet.action) {
                AuthSheetAction.SETUP_VAULT -> {
                    val phrase = repository.setupInitialVault(rawUid, biometricSecret, "Primary e-KTP")
                    _uiState.value = _uiState.value.copy(
                        isInitialized = true,
                        isUnlocked = true,
                        recoveryPhrase = phrase,
                        currentScreen = ScreenState.RecoveryPhrase
                    )
                    closeAuthSheet()
                    repository.logAuditEvent("VAULT_SETUP", "Primary e-KTP", "SUCCESS", "Initial vault configured with biometric root")
                    showStatus("Vault berhasil diinisialisasi dengan e-KTP.")
                }
                AuthSheetAction.UNLOCK_VAULT -> {
                    val result = container.authenticateWithNfcUseCase(rawUid, biometricSecret)
                    result.onSuccess {
                        _uiState.value = _uiState.value.copy(failureCount = 0)
                        closeAuthSheet()
                        repository.logAuditEvent("VAULT_UNLOCK", "System", "SUCCESS", "Vault unlocked with e-KTP ($rawUid) and Biometric")
                        showStatus("Autentikasi hardware berhasil. Vault terbuka.")
                    }.onFailure { err ->
                        repository.logAuditEvent("VAULT_UNLOCK", "System", "FAILED", "Vault unlock attempt failed: ${err.message}")
                        handleAuthFailure(err.message ?: "Autentikasi gagal")
                    }
                }
                AuthSheetAction.REVEAL_PASSWORD -> {
                    val result = container.authenticateWithNfcUseCase(rawUid, biometricSecret)
                    result.onSuccess {
                        _uiState.value = _uiState.value.copy(failureCount = 0)
                        closeAuthSheet()
                        repository.logAuditEvent("BIOMETRIC_DECRYPT", sheet.targetCredentialId ?: "Unknown", "SUCCESS", "Decryption authorized via BiometricPrompt")
                        sheet.targetCredentialId?.let { credId ->
                            executeRevealPassword(credId, sheet.isHighRiskAction)
                        }
                    }.onFailure { err ->
                        repository.logAuditEvent("BIOMETRIC_DECRYPT", sheet.targetCredentialId ?: "Unknown", "FAILED", "Decryption denied: ${err.message}")
                        handleAuthFailure(err.message ?: "Autentikasi gagal")
                    }
                }
                AuthSheetAction.ADD_PHYSICAL_KEY -> {
                    val techList = _uiState.value.lastDiscoveredTag?.techList ?: listOf("IsoDep", "NfcA")
                    val regResult = container.registerPhysicalKeyUseCase(rawUid, sheet.pendingLabel, techList)
                    regResult.onSuccess {
                        closeAuthSheet()
                        repository.logAuditEvent("KEY_REGISTER", sheet.pendingLabel, "SUCCESS", "Registered physical NFC card $rawUid")
                        showStatus("Kunci fisik baru '${sheet.pendingLabel}' berhasil didaftarkan.")
                    }.onFailure { err ->
                        repository.logAuditEvent("KEY_REGISTER", sheet.pendingLabel, "FAILED", "Key registration rejected: ${err.message}")
                        handleAuthFailure(err.message ?: "Registrasi kartu gagal")
                    }
                }
                AuthSheetAction.EXPORT_VAULT -> {
                    val result = container.authenticateWithNfcUseCase(rawUid, biometricSecret)
                    result.onSuccess {
                        _uiState.value = _uiState.value.copy(failureCount = 0)
                        closeAuthSheet()
                        repository.logAuditEvent("EXPORT_VAULT", "Backup", "SUCCESS", "Export authorized via BiometricPrompt")
                        showStatus("Otorisasi hardware disetujui. Siap generate file .loker.")
                    }.onFailure { err ->
                        repository.logAuditEvent("EXPORT_VAULT", "Backup", "FAILED", "Export denied: ${err.message}")
                        handleAuthFailure(err.message ?: "Otorisasi ekspor gagal")
                    }
                }
            }
        }
    }

    private fun handleAuthFailure(message: String) {
        val newFailures = _uiState.value.failureCount + 1
        logAuditEvent("ACCESS_DENIED", "Vault", "FAILED", "Access denied (Attempt $newFailures/3): $message")
        if (newFailures >= 3) {
            startLockoutTimer(30)
            showStatus("3x Kegagalan autentikasi! Lockout sistem 30 detik diaktifkan.")
        } else {
            _uiState.value = _uiState.value.copy(failureCount = newFailures)
            showStatus("$message (Percobaan $newFailures dari 3)")
        }
    }

    private fun startLockoutTimer(seconds: Int) {
        lockoutJob?.cancel()
        _uiState.value = _uiState.value.copy(lockoutSeconds = seconds)
        logAuditEvent("SECURITY_LOCKOUT", "System", "BLOCKED", "3x failed attempts reached. System lockout for ${seconds}s enforced")
        lockoutJob = viewModelScope.launch {
            for (sec in seconds downTo 1) {
                _uiState.value = _uiState.value.copy(lockoutSeconds = sec)
                delay(1000L)
            }
            _uiState.value = _uiState.value.copy(lockoutSeconds = 0, failureCount = 0)
            logAuditEvent("LOCKOUT_LIFTED", "System", "SUCCESS", "System lockout cooldown period expired")
            showStatus("Lockout berakhir. Anda dapat mencoba kembali.")
        }
    }

    fun executeRevealPasswordWithBiometricAuth(credentialId: String, isHighRisk: Boolean) {
        executeRevealPassword(credentialId, isHighRisk)
    }

    fun handleBiometricFailure(message: String) {
        handleAuthFailure(message)
    }

    private fun executeRevealPassword(credentialId: String, isHighRisk: Boolean) {
        viewModelScope.launch {
            val cred = credentials.value.find { it.id == credentialId } ?: return@launch
            val result = container.decryptCredentialUseCase(cred.encryptedBlob)
            result.onSuccess { plain ->
                repository.updateLastUsed(cred.id)
                _uiState.value = _uiState.value.copy(
                    revealedPassword = plain,
                    revealCountdown = 15
                )

                countdownJob?.cancel()
                countdownJob = viewModelScope.launch {
                    for (sec in 15 downTo 1) {
                        _uiState.value = _uiState.value.copy(revealCountdown = sec)
                        delay(1000L)
                    }
                    cancelRevealTimer()
                    if (isHighRisk) {
                        KeyStorage.wipeImmediateHighRisk()
                        showStatus("Plan A: Kredensial High-Risk & kunci di-wipe dari RAM.")
                    }
                }
            }.onFailure { err ->
                showStatus("Gagal mendekripsi password: ${err.message}")
            }
        }
    }

    fun cancelRevealTimer() {
        countdownJob?.cancel()
        countdownJob = null
        _uiState.value = _uiState.value.copy(
            revealedPassword = null,
            revealCountdown = 0
        )
    }

    fun addOrUpdateCredential(
        id: String?,
        serviceName: String,
        username: String,
        passwordPlain: String,
        targetType: TargetType,
        targetId: String,
        securityCategory: SecurityCategory,
        notes: String
    ) {
        viewModelScope.launch {
            val res = container.saveCredentialUseCase(
                id, serviceName, username, passwordPlain, targetType, targetId, securityCategory, notes
            )
            res.onSuccess {
                showStatus(if (id == null) "Kredensial '$serviceName' berhasil dienkripsi dan disimpan." else "Kredensial '$serviceName' diperbarui.")
                navigateTo(ScreenState.Dashboard)
            }.onFailure { err ->
                showStatus("Gagal menyimpan: ${err.message}")
            }
        }
    }

    fun deleteCredential(id: String) {
        viewModelScope.launch {
            container.deleteCredentialUseCase(id)
            showStatus("Kredensial berhasil dihapus dari Vault.")
            navigateTo(ScreenState.Dashboard)
        }
    }

    fun updateRiskCategory(id: String, category: SecurityCategory) {
        viewModelScope.launch {
            repository.updateSecurityCategory(id, category)
            showStatus("Kategori keamanan diubah ke ${category.name}.")
        }
    }

    fun removePhysicalKey(uidHash: String) {
        viewModelScope.launch {
            val res = repository.removePhysicalKey(uidHash)
            res.onSuccess {
                showStatus("Kunci fisik berhasil dihapus.")
            }.onFailure { err ->
                showStatus("Gagal menghapus kunci: ${err.message}")
            }
        }
    }

    fun recoverWithPhrase(phraseWords: List<String>, newUid: String, biometricSecret: String) {
        viewModelScope.launch {
            val res = repository.recoverVaultWithPhrase(phraseWords, newUid, biometricSecret)
            res.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isUnlocked = true,
                    currentScreen = ScreenState.Dashboard
                )
                showStatus("Pemulihan Vault darurat berhasil! Kunci baru didaftarkan.")
            }.onFailure { err ->
                showStatus("Pemulihan gagal: ${err.message}")
            }
        }
    }

    fun exportLoker(backupPassword: String) {
        viewModelScope.launch {
            val res = container.exportVaultUseCase(backupPassword)
            res.onSuccess { content ->
                _uiState.value = _uiState.value.copy(exportedLokerContent = content)
                showStatus("File .loker terenkripsi AES-256-GCM berhasil di-generate!")
            }.onFailure { err ->
                showStatus("Gagal export .loker: ${err.message}")
            }
        }
    }

    fun importLoker(lokerContent: String, backupPassword: String, currentUid: String) {
        viewModelScope.launch {
            val res = container.importVaultUseCase(lokerContent, backupPassword, currentUid)
            res.onSuccess { count ->
                showStatus("Berhasil merestore $count kredensial dari file .loker!")
                navigateTo(ScreenState.Dashboard)
            }.onFailure { err ->
                showStatus("Gagal import .loker: ${err.message}")
            }
        }
    }

    fun lockVaultNow() {
        KeyStorage.clear()
        cancelRevealTimer()
        showStatus("Vault dikunci. Seluruh kunci didegradasi dari RAM.")
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }

    private fun showStatus(msg: String) {
        _uiState.value = _uiState.value.copy(statusMessage = msg)
    }
}
