package com.example.domain.usecase

import com.example.domain.model.AuditLog
import com.example.domain.model.Credential
import com.example.domain.model.NfcTagData
import com.example.domain.model.PhysicalKey
import com.example.domain.model.SecurityCategory
import com.example.domain.model.TargetType
import com.example.domain.repository.INfcReaderService
import com.example.domain.repository.IVaultRepository
import kotlinx.coroutines.flow.Flow

class GetCredentialsUseCase(private val repository: IVaultRepository) {
    operator fun invoke(): Flow<List<Credential>> = repository.getAllCredentials()
}

class SearchCredentialsUseCase(private val repository: IVaultRepository) {
    operator fun invoke(query: String): Flow<List<Credential>> = repository.searchCredentials(query)
}

class SaveCredentialUseCase(private val repository: IVaultRepository) {
    suspend operator fun invoke(
        id: String?,
        serviceName: String,
        username: String,
        passwordPlain: String,
        targetType: TargetType,
        targetId: String,
        securityCategory: SecurityCategory,
        notes: String
    ): Result<Unit> {
        return if (id == null) {
            repository.addCredential(serviceName, username, passwordPlain, targetType, targetId, securityCategory, notes)
        } else {
            repository.updateCredential(id, serviceName, username, passwordPlain.ifBlank { null }, targetType, targetId, securityCategory, notes)
        }
    }
}

class DeleteCredentialUseCase(private val repository: IVaultRepository) {
    suspend operator fun invoke(id: String): Result<Unit> = repository.deleteCredential(id)
}

class DecryptCredentialUseCase(private val repository: IVaultRepository) {
    suspend operator fun invoke(encryptedBlob: String): Result<String> = repository.decryptPassword(encryptedBlob)
}

class AuthenticateWithNfcUseCase(private val repository: IVaultRepository) {
    suspend operator fun invoke(rawUid: String, biometricSecret: String): Result<PhysicalKey> {
        return repository.authenticateWithCard(rawUid, biometricSecret)
    }
}

class RegisterPhysicalKeyUseCase(private val repository: IVaultRepository) {
    suspend operator fun invoke(rawUid: String, label: String, techList: List<String>): Result<Unit> {
        return repository.registerNewPhysicalKey(rawUid, label, techList)
    }
}

class VerifyNfcTagUseCase(private val nfcService: INfcReaderService) {
    operator fun invoke(tagData: NfcTagData): Boolean = nfcService.verifyEktpSignature(tagData)
}

class ExportVaultUseCase(private val repository: IVaultRepository) {
    suspend operator fun invoke(backupPassword: String): Result<String> = repository.exportVaultToLoker(backupPassword)
}

class ImportVaultUseCase(private val repository: IVaultRepository) {
    suspend operator fun invoke(content: String, backupPassword: String, currentUid: String): Result<Int> {
        return repository.importVaultFromLoker(content, backupPassword, currentUid)
    }
}

class GetAuditLogsUseCase(private val repository: IVaultRepository) {
    operator fun invoke(): Flow<List<AuditLog>> = repository.getAuditLogs()
}
