package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 3,
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

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 1 to 2 migration
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE physical_keys ADD COLUMN wrappedMasterKey TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE security_meta ADD COLUMN recoveryWrappedKey TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE physical_keys ADD COLUMN wrappedMasterKey TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE security_meta ADD COLUMN recoveryWrappedKey TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "ktp_vault.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
