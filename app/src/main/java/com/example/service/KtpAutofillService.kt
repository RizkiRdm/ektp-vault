package com.example.service

import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillContext
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveInfo
import android.service.autofill.SaveRequest
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.example.data.VaultDatabase
import com.example.security.CryptoManager
import com.example.security.KeyStorage
import com.example.ui.screens.autofill.AutofillAuthActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KtpAutofillService : AutofillService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val structure = request.fillContexts.lastOrNull()?.structure ?: run {
            callback.onSuccess(null)
            return
        }

        val packageName = structure.activityComponent?.packageName ?: run {
            callback.onSuccess(null)
            return
        }

        // Never autofill the vault app itself
        if (packageName == applicationContext.packageName) {
            callback.onSuccess(null)
            return
        }

        // Validate package name exists on the system
        try {
            packageManager.getPackageInfo(packageName, 0)
        } catch (_: Exception) {
            callback.onSuccess(null)
            return
        }

        serviceScope.launch {
            try {
                val db = VaultDatabase.getDatabase(applicationContext)

                val usernameFields = mutableListOf<AutofillId>()
                val passwordFields = mutableListOf<AutofillId>()
                val detectedWebDomains = mutableListOf<String>()

                traverseStructure(structure, usernameFields, passwordFields, detectedWebDomains)

                if (usernameFields.isEmpty() && passwordFields.isEmpty()) {
                    callback.onSuccess(null)
                    return@launch
                }

                val primaryWebDomain = detectedWebDomains.firstOrNull { it.isNotBlank() } ?: ""

                // Match credentials by package name or web domain
                val credsByPackage = db.credentialDao().getCredentialsForTarget(packageName)
                val credsByDomain = if (primaryWebDomain.isNotBlank()) {
                    db.credentialDao().getCredentialsForTarget(primaryWebDomain)
                } else emptyList()

                val credentials = (credsByPackage + credsByDomain).distinctBy { it.id }

                if (credentials.isEmpty()) {
                    callback.onSuccess(null)
                    return@launch
                }

                val usernameId = usernameFields.firstOrNull()
                val passwordId = passwordFields.firstOrNull()
                val masterKey = KeyStorage.getMasterKey()

                val responseBuilder = FillResponse.Builder()

                if (masterKey == null) {
                    // Vault is locked: Provide interactive modal authentication intent via AutofillAuthActivity
                    val authIntent = Intent(applicationContext, AutofillAuthActivity::class.java).apply {
                        putExtra(AutofillAuthActivity.EXTRA_TARGET_PACKAGE, packageName)
                        putExtra(AutofillAuthActivity.EXTRA_TARGET_DOMAIN, primaryWebDomain)
                        if (usernameId != null) putExtra(AutofillAuthActivity.EXTRA_USERNAME_ID, usernameId)
                        if (passwordId != null) putExtra(AutofillAuthActivity.EXTRA_PASSWORD_ID, passwordId)
                    }

                    val flags = PendingIntent.FLAG_CANCEL_CURRENT or
                            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
                    val pendingIntent = PendingIntent.getActivity(
                        applicationContext,
                        1001,
                        authIntent,
                        flags
                    )

                    val authPresentation = RemoteViews(applicationContext.packageName, android.R.layout.simple_list_item_2).apply {
                        setTextViewText(android.R.id.text1, "LOKER: Fill Credentials (${credentials.size} Accounts)")
                        setTextViewText(android.R.id.text2, "Tap to authenticate Hardware Key & Biometrics")
                    }

                    val targetFieldIds = (usernameFields + passwordFields).toTypedArray()
                    responseBuilder.setAuthentication(
                        targetFieldIds,
                        pendingIntent.intentSender,
                        authPresentation
                    )
                } else {
                    // Master key is unlocked in session: Provide datasets
                    for (cred in credentials) {
                        val presentation = RemoteViews(applicationContext.packageName, android.R.layout.simple_list_item_2).apply {
                            setTextViewText(android.R.id.text1, cred.serviceName)
                            setTextViewText(android.R.id.text2, "LOKER: ${cred.username}")
                        }

                        val datasetBuilder = Dataset.Builder(presentation)

                        if (usernameId != null) {
                            datasetBuilder.setValue(usernameId, AutofillValue.forText(cred.username))
                        }

                        if (passwordId != null) {
                            try {
                                val decrypted = CryptoManager.decryptPassword(cred.encryptedBlob, masterKey)
                                datasetBuilder.setValue(passwordId, AutofillValue.forText(decrypted))
                                if (cred.securityCategory == "HIGH_RISK") {
                                    KeyStorage.wipeImmediateHighRisk()
                                }
                            } catch (_: Exception) {
                                // Skip on decryption failure
                            }
                        }

                        responseBuilder.addDataset(datasetBuilder.build())
                    }
                }

                val saveFields = (usernameFields + passwordFields).toTypedArray()
                if (saveFields.isNotEmpty()) {
                    val saveInfo = SaveInfo.Builder(
                        SaveInfo.SAVE_DATA_TYPE_USERNAME or SaveInfo.SAVE_DATA_TYPE_PASSWORD,
                        saveFields
                    ).build()
                    responseBuilder.setSaveInfo(saveInfo)
                }

                callback.onSuccess(responseBuilder.build())
            } catch (e: Exception) {
                callback.onFailure(e.message)
            }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        callback.onSuccess()
    }

    private fun traverseStructure(
        structure: AssistStructure,
        usernameFields: MutableList<AutofillId>,
        passwordFields: MutableList<AutofillId>,
        webDomains: MutableList<String>
    ) {
        val windowNodesCount = structure.windowNodeCount
        for (i in 0 until windowNodesCount) {
            val windowNode = structure.getWindowNodeAt(i)
            traverseViewNode(windowNode.rootViewNode, usernameFields, passwordFields, webDomains)
        }
    }

    private fun traverseViewNode(
        viewNode: AssistStructure.ViewNode?,
        usernameFields: MutableList<AutofillId>,
        passwordFields: MutableList<AutofillId>,
        webDomains: MutableList<String>
    ) {
        if (viewNode == null) return

        val webDomain = viewNode.webDomain
        if (!webDomain.isNullOrBlank()) {
            webDomains.add(webDomain)
        }

        val hints = viewNode.autofillHints
        val id = viewNode.autofillId

        if (id != null) {
            if (hints != null) {
                for (hint in hints) {
                    if (hint.contains("username", ignoreCase = true) || hint.contains("email", ignoreCase = true)) {
                        usernameFields.add(id)
                    } else if (hint.contains("password", ignoreCase = true)) {
                        passwordFields.add(id)
                    }
                }
            } else {
                val hintText = viewNode.hint ?: ""
                val text = viewNode.text?.toString() ?: ""
                val className = viewNode.className ?: ""

                if (hintText.contains("password", ignoreCase = true) || className.contains("password", ignoreCase = true)) {
                    passwordFields.add(id)
                } else if (hintText.contains("username", ignoreCase = true) || hintText.contains("user", ignoreCase = true) || hintText.contains("email", ignoreCase = true)) {
                    usernameFields.add(id)
                }
            }
        }

        for (i in 0 until viewNode.childCount) {
            traverseViewNode(viewNode.getChildAt(i), usernameFields, passwordFields, webDomains)
        }
    }
}
