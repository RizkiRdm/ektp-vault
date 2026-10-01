package com.example

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.security.BiometricHelper
import com.example.security.KeyStorage
import com.example.ui.AuthSheetAction
import com.example.ui.ScreenState
import com.example.ui.VaultViewModel
import com.example.ui.components.AuthBottomSheet
import com.example.ui.screens.AccountDetailScreen
import com.example.ui.screens.AddEditCredentialScreen
import com.example.ui.screens.AuditLogsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExportImportScreen
import com.example.ui.screens.InitialSetupScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PhysicalKeyManagerScreen
import com.example.ui.screens.RecoveryPhraseScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var globalViewModel: VaultViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        initNfc()

        setContent {
            MyApplicationTheme {
                val vm: VaultViewModel = viewModel()
                globalViewModel = vm
                VaultApp(
                    viewModel = vm,
                    onLaunchSystemBiometric = { title, subtitle, onSuccess, onError ->
                        BiometricHelper.promptBiometric(
                            activity = this,
                            title = title,
                            subtitle = subtitle,
                            onSuccess = { secret ->
                                vm.logAuditEvent(
                                    eventType = "BIOMETRIC_AUTH",
                                    targetService = title,
                                    status = "SUCCESS",
                                    details = "Biometric prompt verification succeeded via Android TEE."
                                )
                                onSuccess(secret)
                            },
                            onError = { errorCode, errString ->
                                val eventType = if (errorCode == 7 || errorCode == 9) "BIOMETRIC_LOCKOUT" else "BIOMETRIC_ERROR"
                                vm.logAuditEvent(
                                    eventType = eventType,
                                    targetService = title,
                                    status = "FAILED",
                                    details = "Biometric prompt error (Code $errorCode): $errString"
                                )
                                onError(errString)
                            },
                            onFailed = {
                                vm.logAuditEvent(
                                    eventType = "BIOMETRIC_MISMATCH",
                                    targetService = title,
                                    status = "FAILED",
                                    details = "Biometric sensor rejected fingerprint/credential mismatch."
                                )
                            }
                        )
                    }
                )
            }
        }
    }

    private fun initNfc() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
    }

    override fun onResume() {
        super.onResume()
        globalViewModel?.nfcService?.enableReaderMode(this)
        if (nfcAdapter != null && pendingIntent != null) {
            nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
        }
    }

    override fun onPause() {
        super.onPause()
        globalViewModel?.nfcService?.disableReaderMode(this)
        if (nfcAdapter != null) {
            nfcAdapter?.disableForegroundDispatch(this)
        }
    }

    override fun onStop() {
        super.onStop()
        KeyStorage.updateTouch()
    }

    // Handle incoming NFC contactless intent requiring BiometricPrompt before access
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val parsedTag = globalViewModel?.nfcService?.processIntent(intent)
        if (parsedTag != null && globalViewModel != null) {
            globalViewModel?.logAuditEvent(
                eventType = "NFC_DETECTED",
                targetService = "e-KTP Reader",
                status = "SUCCESS",
                details = "Contactless tag discovered: UID ${parsedTag.hexUid}"
            )
            BiometricHelper.promptBiometric(
                activity = this,
                title = "e-KTP Terdeteksi",
                subtitle = "Verifikasi biometrik untuk membuka vault dengan kartu ${parsedTag.hexUid}",
                onSuccess = { secret ->
                    globalViewModel?.logAuditEvent(
                        eventType = "BIOMETRIC_AUTH",
                        targetService = "e-KTP Unlock",
                        status = "SUCCESS",
                        details = "Biometric prompt passed for e-KTP ${parsedTag.hexUid}"
                    )
                    globalViewModel?.performNfcAndBiometricAuth(parsedTag.hexUid, secret)
                },
                onError = { errorCode, errString ->
                    globalViewModel?.logAuditEvent(
                        eventType = if (errorCode == 7 || errorCode == 9) "BIOMETRIC_LOCKOUT" else "BIOMETRIC_ERROR",
                        targetService = "e-KTP Unlock",
                        status = "FAILED",
                        details = "Biometric prompt error (Code $errorCode): $errString"
                    )
                    globalViewModel?.handleBiometricFailure("Otorisasi biometrik kartu gagal: $errString")
                },
                onFailed = {
                    globalViewModel?.logAuditEvent(
                        eventType = "BIOMETRIC_MISMATCH",
                        targetService = "e-KTP Unlock",
                        status = "FAILED",
                        details = "Biometric prompt sensor rejected input."
                    )
                }
            )
        }
    }
}

@Composable
fun VaultApp(
    viewModel: VaultViewModel,
    onLaunchSystemBiometric: (title: String, subtitle: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val credentials by viewModel.credentials.collectAsStateWithLifecycle()
    val physicalKeys by viewModel.physicalKeys.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (val screen = uiState.currentScreen) {
                is ScreenState.Onboarding -> {
                    OnboardingScreen(
                        onFinish = { viewModel.completeOnboarding() },
                        onSkip = { viewModel.completeOnboarding() }
                    )
                }

                is ScreenState.InitialSetup -> {
                    InitialSetupScreen(
                        onStartSetup = {
                            viewModel.openAuthSheet(AuthSheetAction.SETUP_VAULT)
                        }
                    )
                }

                is ScreenState.Dashboard -> {
                    DashboardScreen(
                        uiState = uiState,
                        credentials = credentials,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onCategoryFilterChange = { viewModel.setCategoryFilter(it) },
                        onNavigate = { viewModel.navigateTo(it) },
                        onOpenAuthSheet = { action, targetId, isHighRisk ->
                            viewModel.openAuthSheet(action, targetId, isHighRisk)
                        },
                        onLockVault = { viewModel.lockVaultNow() },
                        onUpdateRiskCategory = { id, cat -> viewModel.updateRiskCategory(id, cat) },
                        onDeleteCredential = { viewModel.deleteCredential(it) },
                        onClearStatusMessage = { viewModel.clearStatusMessage() }
                    )
                }

                is ScreenState.AddEdit -> {
                    BackHandler {
                        viewModel.navigateTo(ScreenState.Dashboard)
                    }
                    val cred = credentials.find { it.id == screen.editingCredentialId }
                    AddEditCredentialScreen(
                        existingCredential = cred,
                        onBack = { viewModel.navigateTo(ScreenState.Dashboard) },
                        onSave = { id, name, user, pass, type, target, cat, notes ->
                            viewModel.addOrUpdateCredential(id, name, user, pass, type, target, cat, notes)
                        }
                    )
                }

                is ScreenState.Detail -> {
                    BackHandler {
                        viewModel.cancelRevealTimer()
                        viewModel.navigateTo(ScreenState.Dashboard)
                    }
                    val cred = credentials.find { it.id == screen.credentialId }
                    AccountDetailScreen(
                        credential = cred,
                        revealedPassword = uiState.revealedPassword,
                        revealCountdown = uiState.revealCountdown,
                        onBack = {
                            viewModel.cancelRevealTimer()
                            viewModel.navigateTo(ScreenState.Dashboard)
                        },
                        onTriggerReveal = { isHighRisk ->
                            if (uiState.isUnlocked && cred != null) {
                                // BiometricPrompt protects stored item decryption even during active sessions
                                onLaunchSystemBiometric(
                                    "Otorisasi Dekripsi Password",
                                    "Verifikasi biometrik untuk mendekripsi ${cred.serviceName}",
                                    { _ ->
                                        viewModel.executeRevealPasswordWithBiometricAuth(screen.credentialId, isHighRisk)
                                    },
                                    { err ->
                                        viewModel.handleBiometricFailure("Dekripsi ditolak: $err")
                                    }
                                )
                            } else {
                                viewModel.openAuthSheet(
                                    AuthSheetAction.REVEAL_PASSWORD,
                                    screen.credentialId,
                                    isHighRisk
                                )
                            }
                        },
                        onHideAndScrub = {
                            viewModel.cancelRevealTimer()
                        },
                        onEdit = {
                            viewModel.navigateTo(ScreenState.AddEdit(it))
                        },
                        onDelete = {
                            viewModel.deleteCredential(it)
                        },
                        onToggleRiskCategory = { id, newCat ->
                            viewModel.updateRiskCategory(id, newCat)
                        }
                    )
                }

                is ScreenState.PhysicalKeys -> {
                    BackHandler {
                        viewModel.navigateTo(ScreenState.Dashboard)
                    }
                    PhysicalKeyManagerScreen(
                        physicalKeys = physicalKeys,
                        onBack = { viewModel.navigateTo(ScreenState.Dashboard) },
                        onRegisterNewKeyPrompt = { label ->
                            viewModel.openAuthSheet(
                                AuthSheetAction.ADD_PHYSICAL_KEY,
                                null,
                                false,
                                label
                            )
                        },
                        onRemoveKey = { uidHash ->
                            viewModel.removePhysicalKey(uidHash)
                        }
                    )
                }

                is ScreenState.RecoveryPhrase -> {
                    BackHandler {
                        viewModel.navigateTo(ScreenState.Dashboard)
                    }
                    RecoveryPhraseScreen(
                        recoveryPhrase = uiState.recoveryPhrase,
                        onBack = { viewModel.navigateTo(ScreenState.Dashboard) },
                        onRecoverWithPhrase = { words, newUid, secret ->
                            viewModel.recoverWithPhrase(words, newUid, secret)
                        }
                    )
                }

                is ScreenState.ExportImport -> {
                    BackHandler {
                        viewModel.navigateTo(ScreenState.Dashboard)
                    }
                    ExportImportScreen(
                        exportedLokerContent = uiState.exportedLokerContent,
                        onBack = { viewModel.navigateTo(ScreenState.Dashboard) },
                        onExport = { pass ->
                            viewModel.exportLoker(pass)
                        },
                        onImport = { content, pass, uid ->
                            viewModel.importLoker(content, pass, uid)
                        }
                    )
                }

                is ScreenState.AuditLogs -> {
                    BackHandler {
                        viewModel.navigateTo(ScreenState.Dashboard)
                    }
                    AuditLogsScreen(
                        auditLogs = auditLogs,
                        onBack = { viewModel.navigateTo(ScreenState.Dashboard) }
                    )
                }
            }

            // Auth Bottom Sheet requiring BiometricPrompt before vault decryption
            AuthBottomSheet(
                authSheetState = uiState.authSheet,
                lockoutSeconds = uiState.lockoutSeconds,
                lastDiscoveredTag = uiState.lastDiscoveredTag,
                onDismiss = { viewModel.closeAuthSheet() },
                onRequestBiometricAuth = { rawUid ->
                    val title = when (uiState.authSheet.action) {
                        AuthSheetAction.SETUP_VAULT -> "Inisialisasi Keamanan Biometrik"
                        AuthSheetAction.UNLOCK_VAULT -> "Buka KTP-Vault"
                        AuthSheetAction.REVEAL_PASSWORD -> "Dekripsi Password Sensitif"
                        AuthSheetAction.ADD_PHYSICAL_KEY -> "Otorisasi Kunci Fisik Baru"
                        AuthSheetAction.EXPORT_VAULT -> "Otorisasi Ekspor Vault"
                    }
                    val subtitle = "Verifikasi sidik jari atau PIN perangkat untuk mengakses vault"
                    onLaunchSystemBiometric(
                        title,
                        subtitle,
                        { secret ->
                            viewModel.performNfcAndBiometricAuth(rawUid, secret)
                        },
                        { err ->
                            viewModel.handleBiometricFailure(err)
                        }
                    )
                }
            )
        }
    }
}
