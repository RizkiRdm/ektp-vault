package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.AuditLogDao
import com.example.data.dao.CredentialDao
import com.example.data.dao.PhysicalKeyDao
import com.example.data.dao.SecurityMetaDao
import com.example.data.entity.AuditLogEntity
import com.example.data.entity.CredentialEntity
import com.example.data.entity.PhysicalKeyEntity
import com.example.data.entity.SecurityMetaEntity

@Database(
    entities = [
        CredentialEntity::class,
        PhysicalKeyEntity::class,
        SecurityMetaEntity::class,
        AuditLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun credentialDao(): CredentialDao
    abstract fun physicalKeyDao(): PhysicalKeyDao
    abstract fun securityMetaDao(): SecurityMetaDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: VaultDatabase? = null

        fun getDatabase(context: Context): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "ktp_vault.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
