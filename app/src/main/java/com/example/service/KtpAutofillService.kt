package com.example.service

import android.app.assist.AssistStructure
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
import com.example.R
import com.example.data.VaultDatabase
import com.example.security.CryptoManager
import com.example.security.KeyStorage
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
            // Unverified or spoofed package
            callback.onSuccess(null)
            return
        }

        serviceScope.launch {
            try {
                val db = VaultDatabase.getDatabase(applicationContext)
                val credentials = db.credentialDao().getCredentialsForTarget(packageName)

                if (credentials.isEmpty()) {
                    callback.onSuccess(null)
                    return@launch
                }

                val usernameFields = mutableListOf<AutofillId>()
                val passwordFields = mutableListOf<AutofillId>()
                traverseStructure(structure, usernameFields, passwordFields)

                if (usernameFields.isEmpty() && passwordFields.isEmpty()) {
                    callback.onSuccess(null)
                    return@launch
                }

                val masterKey = KeyStorage.getMasterKey()

                // Security requirement: Credentials are ONLY provided after authentication
                if (masterKey == null) {
                    // Vault is locked: Do not provide credentials without hardware authentication
                    callback.onSuccess(null)
                    return@launch
                }

                val responseBuilder = FillResponse.Builder()

                for (cred in credentials) {
                    // Strictly validate target application matches credential targetId
                    if (!cred.targetId.equals(packageName, ignoreCase = true)) {
                        continue
                    }

                    val usernameId = usernameFields.firstOrNull()
                    val passwordId = passwordFields.firstOrNull()

                    val presentation = RemoteViews(applicationContext.packageName, android.R.layout.simple_list_item_2).apply {
                        setTextViewText(android.R.id.text1, cred.serviceName)
                        setTextViewText(android.R.id.text2, "KTP-Vault: ${cred.username}")
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
                            // Skip password on decryption failure
                        }
                    }

                    responseBuilder.addDataset(datasetBuilder.build())
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
        // Handled automatically via In-App additions or user confirmations
        callback.onSuccess()
    }

    private fun traverseStructure(
        structure: AssistStructure,
        usernameFields: MutableList<AutofillId>,
        passwordFields: MutableList<AutofillId>
    ) {
        val windowNodesCount = structure.windowNodeCount
        for (i in 0 until windowNodesCount) {
            val windowNode = structure.getWindowNodeAt(i)
            traverseViewNode(windowNode.rootViewNode, usernameFields, passwordFields)
        }
    }

    private fun traverseViewNode(
        viewNode: AssistStructure.ViewNode?,
        usernameFields: MutableList<AutofillId>,
        passwordFields: MutableList<AutofillId>
    ) {
        if (viewNode == null) return

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
            traverseViewNode(viewNode.getChildAt(i), usernameFields, passwordFields)
        }
    }
}
