package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AuditLogDao
import com.example.data.dao.CredentialDao
import com.example.data.dao.LockedAppDao
import com.example.data.dao.PhysicalKeyDao
import com.example.data.dao.SecurityMetaDao
import com.example.data.entity.AuditLogEntity
import com.example.data.entity.CredentialEntity
import com.example.data.entity.LockedAppEntity
import com.example.data.entity.PhysicalKeyEntity
import com.example.data.entity.SecurityMetaEntity

@Database(
    entities = [
        CredentialEntity::class,
        PhysicalKeyEntity::class,
        SecurityMetaEntity::class,
        AuditLogEntity::class,
        LockedAppEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun credentialDao(): CredentialDao
    abstract fun physicalKeyDao(): PhysicalKeyDao
    abstract fun securityMetaDao(): SecurityMetaDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun lockedAppDao(): LockedAppDao

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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS locked_apps (packageName TEXT PRIMARY KEY NOT NULL, appName TEXT NOT NULL, lockedAt INTEGER NOT NULL, isEnabled INTEGER NOT NULL, unlockDurationMinutes INTEGER NOT NULL DEFAULT 0, lastUnlockedTimestamp INTEGER NOT NULL DEFAULT 0)")
            }
        }

        val MIGRATION_1_4 = object : Migration(1, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE physical_keys ADD COLUMN wrappedMasterKey TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE security_meta ADD COLUMN recoveryWrappedKey TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE TABLE IF NOT EXISTS locked_apps (packageName TEXT PRIMARY KEY NOT NULL, appName TEXT NOT NULL, lockedAt INTEGER NOT NULL, isEnabled INTEGER NOT NULL, unlockDurationMinutes INTEGER NOT NULL DEFAULT 0, lastUnlockedTimestamp INTEGER NOT NULL DEFAULT 0)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE locked_apps ADD COLUMN targetType TEXT NOT NULL DEFAULT 'APP'")
            }
        }

        fun getDatabase(context: Context): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "ktp_vault.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3, MIGRATION_3_4, MIGRATION_1_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
