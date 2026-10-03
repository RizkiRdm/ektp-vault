package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.AuditLogEntity
import com.example.data.entity.CredentialEntity
import com.example.data.entity.PhysicalKeyEntity
import com.example.data.entity.SecurityMetaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CredentialDao {
    @Query("SELECT * FROM credentials ORDER BY lastUsedTimestamp DESC")
    fun getAllCredentials(): Flow<List<CredentialEntity>>

    @Query("SELECT * FROM credentials WHERE targetId = :targetId")
    suspend fun getCredentialsForTarget(targetId: String): List<CredentialEntity>

    @Query("SELECT * FROM credentials WHERE id = :id LIMIT 1")
    suspend fun getCredentialById(id: String): CredentialEntity?

    @Query("""
        SELECT * FROM credentials 
        WHERE serviceName LIKE '%' || :query || '%' 
           OR username LIKE '%' || :query || '%' 
           OR notes LIKE '%' || :query || '%'
        ORDER BY lastUsedTimestamp DESC
    """)
    fun searchCredentials(query: String): Flow<List<CredentialEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredential(credential: CredentialEntity)

    @Update
    suspend fun updateCredential(credential: CredentialEntity)

    @Query("UPDATE credentials SET securityCategory = :category WHERE id = :id")
    suspend fun updateSecurityCategory(id: String, category: String)

    @Query("UPDATE credentials SET lastUsedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: String, timestamp: Long)

    @Delete
    suspend fun deleteCredential(credential: CredentialEntity)

    @Query("DELETE FROM credentials WHERE id = :id")
    suspend fun deleteCredentialById(id: String)

    @Query("SELECT COUNT(*) FROM credentials")
    suspend fun getCredentialsCount(): Int
}

@Dao
interface PhysicalKeyDao {
    @Query("SELECT * FROM physical_keys ORDER BY isPrimary DESC, registeredAt ASC")
    fun getAllPhysicalKeys(): Flow<List<PhysicalKeyEntity>>

    @Query("SELECT * FROM physical_keys WHERE uidHash = :uidHash LIMIT 1")
    suspend fun getPhysicalKeyByHash(uidHash: String): PhysicalKeyEntity?

    @Query("SELECT * FROM physical_keys WHERE isPrimary = 1 LIMIT 1")
    suspend fun getPrimaryKey(): PhysicalKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhysicalKey(key: PhysicalKeyEntity)

    @Delete
    suspend fun deletePhysicalKey(key: PhysicalKeyEntity)

    @Query("DELETE FROM physical_keys WHERE uidHash = :uidHash")
    suspend fun deletePhysicalKeyByHash(uidHash: String)

    @Query("UPDATE physical_keys SET label = :newLabel WHERE uidHash = :uidHash")
    suspend fun renameKey(uidHash: String, newLabel: String)

    @Query("UPDATE physical_keys SET isPrimary = (uidHash = :primaryHash)")
    suspend fun setOnlyOnePrimary(primaryHash: String)

    @Query("SELECT COUNT(*) FROM physical_keys")
    suspend fun getPhysicalKeysCount(): Int
}

@Dao
interface SecurityMetaDao {
    @Query("SELECT * FROM security_meta WHERE id = 1 LIMIT 1")
    suspend fun getSecurityMeta(): SecurityMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSecurityMeta(meta: SecurityMetaEntity)

    @Query("UPDATE security_meta SET lastAuditTimestamp = :timestamp WHERE id = 1")
    suspend fun updateLastAudit(timestamp: Long)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 300")
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE eventType LIKE '%BIOMETRIC%' ORDER BY timestamp DESC")
    fun getBiometricAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert
    suspend fun insertAuditLog(log: AuditLogEntity)
}
