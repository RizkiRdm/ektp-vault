package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
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
    private var currentForegroundPackage: String? = null
    private var lastCheckedDomain: String? = null
    private var lastCheckedDomainTimestamp: Long = 0L

    private val browserPackages = setOf(
        "com.android.chrome",
        "com.google.android.apps.chrome",
        "com.sec.android.app.sbrowser",
        "org.mozilla.firefox",
        "com.microsoft.emmx",
        "com.brave.browser",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.duckduckgo.mobile.android",
        "com.vivaldi.browser",
        "com.kiwibrowser.browser"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }

        val packageName = event.packageName?.toString() ?: return
        if (packageName == applicationContext.packageName) return
        if (packageName == "com.android.systemui" || packageName == "com.google.android.inputmethod.latin") return

        val now = System.currentTimeMillis()

        // Track app exit and resume
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (currentForegroundPackage != null && currentForegroundPackage != packageName) {
                AppLockManager.onAppExited(currentForegroundPackage!!)
            }
            currentForegroundPackage = packageName
            AppLockManager.onAppResumed(packageName)
        }

        // Check if application package itself is locked
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (packageName != lastCheckedPackage || (now - lastCheckedTimestamp > 500L)) {
                lastCheckedPackage = packageName
                lastCheckedTimestamp = now

                serviceScope.launch {
                    val isLocked = AppLockManager.isAppLocked(applicationContext, packageName)
                    if (isLocked) {
                        AppLockManager.activeVerifyingPackage = packageName
                        val lockIntent = Intent(applicationContext, AppLockActivity::class.java).apply {
                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                            )
                            putExtra(AppLockActivity.EXTRA_TARGET_PACKAGE, packageName)
                        }
                        startActivity(lockIntent)
                        return@launch
                    }
                }
            }
        }

        // Check if running inside a browser and inspecting a locked website domain
        if (browserPackages.contains(packageName) || packageName.contains("browser") || packageName.contains("chrome")) {
            val domain = extractDomainFromBrowser(packageName)
            if (domain != null && (domain != lastCheckedDomain || (now - lastCheckedDomainTimestamp > 1200L))) {
                lastCheckedDomain = domain
                lastCheckedDomainTimestamp = now

                serviceScope.launch {
                    val isDomainLocked = AppLockManager.isDomainLocked(applicationContext, domain)
                    if (isDomainLocked) {
                        AppLockManager.activeVerifyingDomain = domain
                        val lockIntent = Intent(applicationContext, AppLockActivity::class.java).apply {
                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                            )
                            putExtra(AppLockActivity.EXTRA_TARGET_PACKAGE, packageName)
                            putExtra(AppLockActivity.EXTRA_TARGET_DOMAIN, domain)
                            putExtra(AppLockActivity.EXTRA_IS_DOMAIN, true)
                        }
                        startActivity(lockIntent)
                    }
                }
            }
        }
    }

    private fun extractDomainFromBrowser(browserPackage: String): String? {
        val rootNode = rootInActiveWindow ?: return null
        return try {
            val candidateIds = listOf(
                "$browserPackage:id/url_bar",
                "$browserPackage:id/location_bar_edit_text",
                "$browserPackage:id/mozac_browser_toolbar_url_view",
                "$browserPackage:id/omnibarTextInput",
                "$browserPackage:id/url_field",
                "$browserPackage:id/search_box"
            )
            for (id in candidateIds) {
                val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
                for (node in nodes) {
                    val text = node.text?.toString()
                    val domain = parseDomainFromUrl(text)
                    if (domain != null) return domain
                }
            }
            findDomainRecursive(rootNode, 0)
        } catch (_: Exception) {
            null
        }
    }

    private fun findDomainRecursive(node: AccessibilityNodeInfo?, depth: Int): String? {
        if (node == null || depth > 8) return null
        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        if (viewId.contains("url") || viewId.contains("location") || viewId.contains("omnibox")) {
            val text = node.text?.toString()
            val domain = parseDomainFromUrl(text)
            if (domain != null) return domain
        }
        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i)
            val res = findDomainRecursive(child, depth + 1)
            if (res != null) return res
        }
        return null
    }

    private fun parseDomainFromUrl(rawText: String?): String? {
        if (rawText.isNullOrBlank()) return null
        var cleaned = rawText.trim().lowercase()
        if (cleaned.contains("://")) {
            cleaned = cleaned.substringAfter("://")
        }
        cleaned = cleaned.substringBefore("/").substringBefore("?").substringBefore("#").substringBefore(":")
        if (cleaned.startsWith("www.")) {
            cleaned = cleaned.removePrefix("www.")
        }
        if (cleaned.contains(".") && !cleaned.contains(" ") && cleaned.length >= 4) {
            return cleaned
        }
        return null
    }

    override fun onInterrupt() {
        // Accessibility service interrupted
    }
}
