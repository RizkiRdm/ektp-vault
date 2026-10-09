package com.example.ui.screens.autofill

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.service.autofill.Dataset
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.VaultApplication
import com.example.data.VaultDatabase
import com.example.domain.model.Credential
import com.example.domain.model.NfcScanState
import com.example.security.BiometricHelper
import com.example.security.CryptoManager
import com.example.security.KeyStorage
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CarbonGray
import com.example.ui.theme.CoolHairline
import com.example.ui.theme.GoldenRatio
import com.example.ui.theme.MistSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SignalWhite
import com.example.ui.theme.SovereignInk
import com.example.ui.theme.SovereignViolet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AutofillAuthActivity : FragmentActivity() {

    companion object {
        const val EXTRA_TARGET_PACKAGE = "extra_target_package"
        const val EXTRA_TARGET_DOMAIN = "extra_target_domain"
        const val EXTRA_USERNAME_ID = "extra_username_id"
        const val EXTRA_PASSWORD_ID = "extra_password_id"
    }

    private var targetPackage: String = ""
    private var targetDomain: String = ""
    private var usernameAutofillId: AutofillId? = null
    private var passwordAutofillId: AutofillId? = null

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE) ?: ""
        targetDomain = intent.getStringExtra(EXTRA_TARGET_DOMAIN) ?: ""
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            usernameAutofillId = intent.getParcelableExtra(EXTRA_USERNAME_ID)
            passwordAutofillId = intent.getParcelableExtra(EXTRA_PASSWORD_ID)
        }

        initNfc()

        setContent {
            MyApplicationTheme {
                AutofillAuthModalScreen(
                    targetPackage = targetPackage,
                    targetDomain = targetDomain,
                    onCredentialSelectedAndVerified = { selectedCred, rawUid ->
                        finishWithAutofillResult(selectedCred, rawUid)
                    },
                    onDismiss = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }

    private fun initNfc() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
    }

    override fun onResume() {
        super.onResume()
        val nfcService = (application as? VaultApplication)?.container?.nfcReaderService
        nfcService?.enableReaderMode(this)
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        val nfcService = (application as? VaultApplication)?.container?.nfcReaderService
        nfcService?.disableReaderMode(this)
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val nfcService = (application as? VaultApplication)?.container?.nfcReaderService
        nfcService?.processIntent(intent)
    }

    private fun finishWithAutofillResult(credential: Credential, rawUid: String) {
        val masterKey = KeyStorage.getMasterKey()
        if (masterKey == null) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        val decryptedPassword = try {
            CryptoManager.decryptPassword(credential.encryptedBlob, masterKey)
        } catch (_: Exception) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val presentation = RemoteViews(applicationContext.packageName, android.R.layout.simple_list_item_2).apply {
                setTextViewText(android.R.id.text1, credential.serviceName)
                setTextViewText(android.R.id.text2, "LOKER: ${credential.username}")
            }

            val datasetBuilder = Dataset.Builder(presentation)

            usernameAutofillId?.let { id ->
                datasetBuilder.setValue(id, AutofillValue.forText(credential.username))
            }
            passwordAutofillId?.let { id ->
                datasetBuilder.setValue(id, AutofillValue.forText(decryptedPassword))
            }

            val dataset = datasetBuilder.build()
            val replyIntent = Intent().apply {
                putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, dataset)
            }
            setResult(Activity.RESULT_OK, replyIntent)
        } else {
            setResult(Activity.RESULT_OK)
        }
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutofillAuthModalScreen(
    targetPackage: String,
    targetDomain: String,
    onCredentialSelectedAndVerified: (Credential, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val nfcService = (context.applicationContext as? VaultApplication)?.container?.nfcReaderService
    val vaultRepository = (context.applicationContext as? VaultApplication)?.container?.vaultRepository

    var credentials by remember { mutableStateOf<List<Credential>>(emptyList()) }
    var selectedCredential by remember { mutableStateOf<Credential?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val nfcState by (nfcService?.nfcScanState?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(NfcScanState.WAITING) })
    val discoveredTag by (nfcService?.tagDiscoveryFlow?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(null) })

    var statusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(targetPackage, targetDomain) {
        withContext(Dispatchers.IO) {
            val db = VaultDatabase.getDatabase(context)
            val list = mutableListOf<Credential>()
            if (targetPackage.isNotBlank()) {
                list.addAll(db.credentialDao().getCredentialsForTarget(targetPackage).map {
                    Credential(
                        id = it.id,
                        serviceName = it.serviceName,
                        username = it.username,
                        encryptedBlob = it.encryptedBlob,
                        targetType = com.example.domain.model.TargetType.APP,
                        targetId = it.targetId,
                        securityCategory = com.example.domain.model.SecurityCategory.STANDARD,
                        lastUsedTimestamp = it.lastUsedTimestamp
                    )
                })
            }
            if (targetDomain.isNotBlank()) {
                list.addAll(db.credentialDao().getCredentialsForTarget(targetDomain).map {
                    Credential(
                        id = it.id,
                        serviceName = it.serviceName,
                        username = it.username,
                        encryptedBlob = it.encryptedBlob,
                        targetType = com.example.domain.model.TargetType.WEB,
                        targetId = it.targetId,
                        securityCategory = com.example.domain.model.SecurityCategory.STANDARD,
                        lastUsedTimestamp = it.lastUsedTimestamp
                    )
                })
            }
            credentials = list.distinctBy { it.id }
            if (credentials.isNotEmpty()) {
                selectedCredential = credentials.first()
            }
            isLoading = false
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SignalWhite,
        scrimColor = Color(0x660A0F1F),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(SovereignInk, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "L",
                            color = SignalWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = GoldenRatio.TextSmall
                        )
                    }
                    Spacer(modifier = Modifier.width(GoldenRatio.SpaceSmall))
                    Column {
                        Text(
                            text = "Use LOKER to Autofill Login",
                            color = SovereignInk,
                            fontSize = GoldenRatio.TextSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (targetDomain.isNotBlank()) "Domain: $targetDomain" else "App: $targetPackage",
                            color = CarbonGray,
                            fontSize = GoldenRatio.TextMicro,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(9999.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CoolHairline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CarbonGray),
                    modifier = Modifier.size(32.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(14.dp))
                }
            }

            Spacer(modifier = Modifier.height(GoldenRatio.SpaceBase))

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp), color = SovereignViolet)
                Spacer(modifier = Modifier.height(GoldenRatio.SpaceBase))
            } else if (credentials.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MistSurface, RoundedCornerShape(12.dp))
                        .border(1.dp, CoolHairline, RoundedCornerShape(12.dp))
                        .padding(GoldenRatio.SpaceBase),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = CarbonGray, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No saved credentials found for this target.",
                            color = SovereignInk,
                            fontSize = GoldenRatio.TextSmall,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Open LOKER to register credentials for $targetPackage.",
                            color = CarbonGray,
                            fontSize = GoldenRatio.TextMicro,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(GoldenRatio.SpaceBase))
            } else {
                Text(
                    text = "SELECT LOGIN ACCOUNT (${credentials.size})",
                    color = SovereignViolet,
                    fontSize = GoldenRatio.TextMicro,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (credentials.size > 2) 140.dp else 90.dp)
                ) {
                    items(credentials) { cred ->
                        val isSelected = cred.id == selectedCredential?.id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) Color(0xFFF5F3FF) else SignalWhite,
                                    RoundedCornerShape(10.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) SovereignViolet else CoolHairline,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedCredential = cred }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = if (isSelected) SovereignViolet else CarbonGray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = cred.username,
                                            color = SovereignInk,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = cred.serviceName,
                                            color = CarbonGray,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Dipilih",
                                        tint = SovereignViolet,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // NFC Tap Prompt
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MistSurface, RoundedCornerShape(12.dp))
                        .border(1.dp, CoolHairline, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (nfcState == NfcScanState.SUCCESS) Color(0xFFE8F5E9) else Color(0xFFEDE9FE),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (nfcState == NfcScanState.SUCCESS) Icons.Default.Check else Icons.Default.Nfc,
                                contentDescription = null,
                                tint = if (nfcState == NfcScanState.SUCCESS) Color(0xFF2E7D32) else SovereignViolet,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (discoveredTag != null) "Security Key Detected" else "Tap Physical Security Key to Back of Phone",
                                color = SovereignInk,
                                fontWeight = FontWeight.Bold,
                                fontSize = GoldenRatio.TextSmall
                            )
                            Text(
                                text = if (discoveredTag != null) "UID: ${discoveredTag?.hexUid} • Ready to fill"
                                else "Required to unlock encrypted credentials",
                                color = CarbonGray,
                                fontSize = GoldenRatio.TextMicro
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(GoldenRatio.SpaceBase))

                val canFill = selectedCredential != null && discoveredTag != null

                Button(
                    onClick = {
                        val chosen = selectedCredential ?: return@Button
                        val tag = discoveredTag ?: return@Button
                        activity?.let { act ->
                            BiometricHelper.promptBiometric(
                                activity = act,
                                title = "Fill Login: ${chosen.serviceName}",
                                subtitle = "Verify biometrics to release username & password",
                                onSuccess = { secret ->
                                    CoroutineScope(Dispatchers.Main).launch {
                                        val authRes = vaultRepository?.authenticateWithCard(tag.hexUid, secret)
                                        if (authRes?.isSuccess == true) {
                                            onCredentialSelectedAndVerified(chosen, tag.hexUid)
                                        } else {
                                            statusMessage = "Security key does not match enrolled master key."
                                        }
                                    }
                                },
                                onError = { _, err ->
                                    statusMessage = "Biometric authorization failed: $err"
                                }
                            )
                        }
                    },
                    enabled = canFill,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_autofill_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SovereignInk,
                        contentColor = SignalWhite
                    ),
                    shape = RoundedCornerShape(9999.dp)
                ) {
                    Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(GoldenRatio.SpaceSmall))
                    Text(
                        text = if (discoveredTag == null) "TAP SECURITY KEY TO FILL"
                        else "AUTHORIZE & AUTOFILL",
                        fontWeight = FontWeight.Medium,
                        fontSize = GoldenRatio.TextSmall
                    )
                }

                if (statusMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusMessage ?: "",
                        color = AlertCrimson,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
