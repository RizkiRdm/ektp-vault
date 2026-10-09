package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.security.AppLockManager
import com.example.ui.screens.applock.AppLockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KtpAppLockAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var lastCheckedPackage: String? = null
    private var lastCheckedTimestamp: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == applicationContext.packageName) return
        if (packageName == "com.android.systemui" || packageName == "com.google.android.inputmethod.latin") return

        val now = System.currentTimeMillis()
        if (packageName == lastCheckedPackage && (now - lastCheckedTimestamp < 1000L)) {
            return
        }
        lastCheckedPackage = packageName
        lastCheckedTimestamp = now

        serviceScope.launch {
            val isLocked = AppLockManager.isAppLocked(applicationContext, packageName)
            if (isLocked) {
                AppLockManager.activeVerifyingPackage = packageName
                val lockIntent = Intent(applicationContext, AppLockActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    )
                    putExtra(AppLockActivity.EXTRA_TARGET_PACKAGE, packageName)
                }
                startActivity(lockIntent)
            }
        }
    }

    override fun onInterrupt() {
        // Accessibility service interrupted
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
