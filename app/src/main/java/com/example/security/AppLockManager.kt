package com.example.security

import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import android.provider.Settings
import com.example.data.VaultDatabase
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppLockManager {
    // Stores packageName -> unlocked timestamp
    private val unlockedSessionMap = ConcurrentHashMap<String, Long>()
    const val GRACE_PERIOD_MS = 60 * 1000L // 60 seconds grace period after unlock

    // Actively locked package being verified currently (prevents re-triggering while AppLockActivity is shown)
    @Volatile
    var activeVerifyingPackage: String? = null

    suspend fun isAppLocked(context: Context, packageName: String): Boolean = withContext(Dispatchers.IO) {
        if (packageName == context.packageName) return@withContext false
        if (packageName == "com.android.systemui" || packageName == "com.google.android.inputmethod.latin") return@withContext false
        if (packageName == activeVerifyingPackage) return@withContext false

        val unlockTime = unlockedSessionMap[packageName]
        if (unlockTime != null) {
            val elapsed = System.currentTimeMillis() - unlockTime
            if (elapsed < GRACE_PERIOD_MS) {
                return@withContext false
            } else {
                unlockedSessionMap.remove(packageName)
            }
        }

        try {
            val db = VaultDatabase.getDatabase(context)
            val lockedApp = db.lockedAppDao().getLockedApp(packageName)
            lockedApp != null && lockedApp.isEnabled
        } catch (_: Exception) {
            false
        }
    }

    fun unlockAppForSession(packageName: String) {
        unlockedSessionMap[packageName] = System.currentTimeMillis()
        if (activeVerifyingPackage == packageName) {
            activeVerifyingPackage = null
        }
    }

    fun lockAppNow(packageName: String) {
        unlockedSessionMap.remove(packageName)
    }

    fun lockAllApps() {
        unlockedSessionMap.clear()
        activeVerifyingPackage = null
    }

    fun isAccessibilityEnabled(context: Context): Boolean {
        return try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            val expectedService = "${context.packageName}/com.example.service.KtpAppLockAccessibilityService"
            enabledServices.contains(expectedService) || enabledServices.contains("KtpAppLockAccessibilityService")
        } catch (_: Exception) {
            false
        }
    }

    fun isUsageAccessGranted(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }
}
