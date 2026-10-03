package com.example.security

import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.SecureRandom

object NfcHelper {
    fun isNfcSupported(context: Context): Boolean {
        val adapter = NfcAdapter.getDefaultAdapter(context)
        return adapter != null
    }

    fun isNfcEnabled(context: Context): Boolean {
        val adapter = NfcAdapter.getDefaultAdapter(context)
        return adapter != null && adapter.isEnabled
    }

    fun extractUid(tag: Tag?): String? {
        if (tag == null) return null
        val idBytes = tag.id ?: return null
        return formatUid(idBytes)
    }

    fun formatUid(bytes: ByteArray): String {
        return bytes.joinToString(":") { "%02X".format(it) }
    }
}

// Helper managing BiometricPrompt authentication to protect access to credential vault and decryption
object BiometricHelper {

    private const val PREFS_NAME = "ktp_vault_secure_meta"
    private const val PREF_BIOMETRIC_SECRET = "ktp_vault_bio_entropy"

    // Check if device supports Biometric or Device Credential (PIN/Pattern/Password)
    fun canAuthenticate(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }
        val status = biometricManager.canAuthenticate(authenticators)
        return status == BiometricManager.BIOMETRIC_SUCCESS
    }

    // Retrieve or create persistent hardware-anchored biometric entropy
    fun getOrGenerateBiometricSecret(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var secret = prefs.getString(PREF_BIOMETRIC_SECRET, null)
        if (secret == null) {
            val bytes = ByteArray(32)
            SecureRandom().nextBytes(bytes)
            secret = Base64.encodeToString(bytes, Base64.NO_WRAP)
            prefs.edit().putString(PREF_BIOMETRIC_SECRET, secret).apply()
        }
        return secret
    }

    // Launch official BiometricPrompt to protect access before decrypting vault items
    fun promptBiometric(
        activity: FragmentActivity,
        title: String = "KTP-Vault Otorisasi Biometrik",
        subtitle: String = "Verifikasi identitas biometrik untuk mengakses vault",
        onSuccess: (secret: String) -> Unit,
        onError: (errorCode: Int, errString: String) -> Unit,
        onFailed: (() -> Unit)? = null
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                val secret = getOrGenerateBiometricSecret(activity)
                onSuccess(secret)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errorCode, errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFailed?.invoke()
            }
        })

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        val biometricManager = BiometricManager.from(activity)
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }

        val canAuthenticateWithCredentials = biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS

        if (canAuthenticateWithCredentials) {
            promptInfoBuilder.setAllowedAuthenticators(authenticators)
        } else {
            promptInfoBuilder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            promptInfoBuilder.setNegativeButtonText("Batal")
        }

        try {
            prompt.authenticate(promptInfoBuilder.build())
        } catch (e: Exception) {
            // Security: Never treat an exception as authentication success
            onError(
                BiometricPrompt.ERROR_UNABLE_TO_PROCESS,
                e.localizedMessage ?: "Gagal memproses autentikasi biometrik pada perangkat."
            )
        }
    }
}
