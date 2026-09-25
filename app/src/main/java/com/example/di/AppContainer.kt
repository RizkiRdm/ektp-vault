package com.example.di

import android.content.Context
import com.example.data.VaultDatabase
import com.example.service.NfcReaderService
import com.example.data.repository.VaultRepository
import com.example.domain.repository.INfcReaderService
import com.example.domain.repository.ISecurityEngine
import com.example.domain.repository.IVaultRepository
import com.example.domain.usecase.AuthenticateWithNfcUseCase
import com.example.domain.usecase.DecryptCredentialUseCase
import com.example.domain.usecase.DeleteCredentialUseCase
import com.example.domain.usecase.ExportVaultUseCase
import com.example.domain.usecase.GetAuditLogsUseCase
import com.example.domain.usecase.GetCredentialsUseCase
import com.example.domain.usecase.ImportVaultUseCase
import com.example.domain.usecase.RegisterPhysicalKeyUseCase
import com.example.domain.usecase.SaveCredentialUseCase
import com.example.domain.usecase.SearchCredentialsUseCase
import com.example.domain.usecase.VerifyNfcTagUseCase
import com.example.security.CryptoManager

interface AppContainer {
    val database: VaultDatabase
    val securityEngine: ISecurityEngine
    val nfcReaderService: INfcReaderService
    val vaultRepository: IVaultRepository

    val getCredentialsUseCase: GetCredentialsUseCase
    val searchCredentialsUseCase: SearchCredentialsUseCase
    val saveCredentialUseCase: SaveCredentialUseCase
    val deleteCredentialUseCase: DeleteCredentialUseCase
    val decryptCredentialUseCase: DecryptCredentialUseCase
    val authenticateWithNfcUseCase: AuthenticateWithNfcUseCase
    val registerPhysicalKeyUseCase: RegisterPhysicalKeyUseCase
    val verifyNfcTagUseCase: VerifyNfcTagUseCase
    val exportVaultUseCase: ExportVaultUseCase
    val importVaultUseCase: ImportVaultUseCase
    val getAuditLogsUseCase: GetAuditLogsUseCase
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val database: VaultDatabase by lazy {
        VaultDatabase.getDatabase(context)
    }

    override val securityEngine: ISecurityEngine by lazy {
        CryptoManager
    }

    override val nfcReaderService: INfcReaderService by lazy {
        NfcReaderService(context)
    }

    override val vaultRepository: IVaultRepository by lazy {
        VaultRepository(
            credentialDao = database.credentialDao(),
            physicalKeyDao = database.physicalKeyDao(),
            securityMetaDao = database.securityMetaDao(),
            auditLogDao = database.auditLogDao(),
            securityEngine = securityEngine
        )
    }

    override val getCredentialsUseCase: GetCredentialsUseCase by lazy {
        GetCredentialsUseCase(vaultRepository)
    }

    override val searchCredentialsUseCase: SearchCredentialsUseCase by lazy {
        SearchCredentialsUseCase(vaultRepository)
    }

    override val saveCredentialUseCase: SaveCredentialUseCase by lazy {
        SaveCredentialUseCase(vaultRepository)
    }

    override val deleteCredentialUseCase: DeleteCredentialUseCase by lazy {
        DeleteCredentialUseCase(vaultRepository)
    }

    override val decryptCredentialUseCase: DecryptCredentialUseCase by lazy {
        DecryptCredentialUseCase(vaultRepository)
    }

    override val authenticateWithNfcUseCase: AuthenticateWithNfcUseCase by lazy {
        AuthenticateWithNfcUseCase(vaultRepository)
    }

    override val registerPhysicalKeyUseCase: RegisterPhysicalKeyUseCase by lazy {
        RegisterPhysicalKeyUseCase(vaultRepository)
    }

    override val verifyNfcTagUseCase: VerifyNfcTagUseCase by lazy {
        VerifyNfcTagUseCase(nfcReaderService)
    }

    override val exportVaultUseCase: ExportVaultUseCase by lazy {
        ExportVaultUseCase(vaultRepository)
    }

    override val importVaultUseCase: ImportVaultUseCase by lazy {
        ImportVaultUseCase(vaultRepository)
    }

    override val getAuditLogsUseCase: GetAuditLogsUseCase by lazy {
        GetAuditLogsUseCase(vaultRepository)
    }
}
