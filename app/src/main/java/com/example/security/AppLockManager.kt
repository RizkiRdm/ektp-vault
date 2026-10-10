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
    // Stores target (packageName or domain) -> unlocked timestamp
    private val unlockedSessionMap = ConcurrentHashMap<String, Long>()
    // Stores target -> timestamp when user exited the target
    private val appExitTimestampMap = ConcurrentHashMap<String, Long>()

    @Volatile
    var activeVerifyingPackage: String? = null

    @Volatile
    var activeVerifyingDomain: String? = null

    fun getRelockDurationMs(minutes: Int): Long {
        return when (minutes) {
            -1 -> 30 * 1000L
            0 -> 0L
            1 -> 60 * 1000L
            5 -> 5 * 60 * 1000L
            15 -> 15 * 60 * 1000L
            30 -> 30 * 60 * 1000L
            else -> if (minutes > 0) minutes * 60 * 1000L else 0L
        }
    }

    fun onAppExited(target: String) {
        val now = System.currentTimeMillis()
        appExitTimestampMap[target] = now
    }

    fun onAppResumed(target: String) {
        appExitTimestampMap.remove(target)
    }

    suspend fun isAppLocked(context: Context, packageName: String): Boolean = withContext(Dispatchers.IO) {
        if (packageName == context.packageName) return@withContext false
        if (packageName == "com.android.systemui" || packageName == "com.google.android.inputmethod.latin") return@withContext false
        if (packageName == activeVerifyingPackage) return@withContext false

        try {
            val db = VaultDatabase.getDatabase(context)
            val lockedApp = db.lockedAppDao().getLockedApp(packageName) ?: return@withContext false
            if (!lockedApp.isEnabled) return@withContext false

            val unlockTime = unlockedSessionMap[packageName]
            if (unlockTime != null) {
                val exitTime = appExitTimestampMap[packageName]
                val durationMs = getRelockDurationMs(lockedApp.unlockDurationMinutes)

                if (durationMs <= 0L) {
                    if (exitTime != null) {
                        unlockedSessionMap.remove(packageName)
                        appExitTimestampMap.remove(packageName)
                        return@withContext true
                    }
                    return@withContext false
                } else {
                    if (exitTime != null) {
                        val elapsed = System.currentTimeMillis() - exitTime
                        if (elapsed >= durationMs) {
                            unlockedSessionMap.remove(packageName)
                            appExitTimestampMap.remove(packageName)
                            return@withContext true
                        }
                    }
                    return@withContext false
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun isDomainLocked(context: Context, domain: String): Boolean = withContext(Dispatchers.IO) {
        val cleanDomain = domain.trim().lowercase()
        if (cleanDomain.isBlank()) return@withContext false
        if (cleanDomain == activeVerifyingDomain) return@withContext false

        try {
            val db = VaultDatabase.getDatabase(context)
            val lockedDomain = db.lockedAppDao().getLockedApp(cleanDomain) ?: return@withContext false
            if (!lockedDomain.isEnabled) return@withContext false

            val unlockTime = unlockedSessionMap[cleanDomain]
            if (unlockTime != null) {
                val exitTime = appExitTimestampMap[cleanDomain]
                val durationMs = getRelockDurationMs(lockedDomain.unlockDurationMinutes)

                if (durationMs <= 0L) {
                    if (exitTime != null) {
                        unlockedSessionMap.remove(cleanDomain)
                        appExitTimestampMap.remove(cleanDomain)
                        return@withContext true
                    }
                    return@withContext false
                } else {
                    if (exitTime != null) {
                        val elapsed = System.currentTimeMillis() - exitTime
                        if (elapsed >= durationMs) {
                            unlockedSessionMap.remove(cleanDomain)
                            appExitTimestampMap.remove(cleanDomain)
                            return@withContext true
                        }
                    }
                    return@withContext false
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun unlockAppForSession(target: String) {
        val cleanTarget = target.trim().lowercase()
        unlockedSessionMap[target] = System.currentTimeMillis()
        unlockedSessionMap[cleanTarget] = System.currentTimeMillis()
        appExitTimestampMap.remove(target)
        appExitTimestampMap.remove(cleanTarget)
        if (activeVerifyingPackage == target) {
            activeVerifyingPackage = null
        }
        if (activeVerifyingDomain == cleanTarget || activeVerifyingDomain == target) {
            activeVerifyingDomain = null
        }
    }

    fun lockAppNow(target: String) {
        unlockedSessionMap.remove(target)
        unlockedSessionMap.remove(target.trim().lowercase())
        appExitTimestampMap.remove(target)
    }

    fun lockAllApps() {
        unlockedSessionMap.clear()
        appExitTimestampMap.clear()
        activeVerifyingPackage = null
        activeVerifyingDomain = null
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
