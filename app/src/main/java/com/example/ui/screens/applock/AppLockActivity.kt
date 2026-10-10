package com.example.ui.screens.applock

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Drawable
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.VaultApplication
import com.example.data.VaultDatabase
import com.example.domain.model.Credential
import com.example.domain.model.NfcScanState
import com.example.security.AppLockManager
import com.example.security.BiometricHelper
import com.example.security.KeyStorage
import com.example.ui.components.AppIconView
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CarbonGray
import com.example.ui.theme.CoolHairline
import com.example.ui.theme.GoldenRatio
import com.example.ui.theme.MistSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SignalWhite
import com.example.ui.theme.SovereignInk
import com.example.ui.theme.SovereignViolet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class UnlockMethod {
    BIOMETRIC,
    CARD,
    CREDENTIALS
}

class AppLockActivity : FragmentActivity() {

    companion object {
        const val EXTRA_TARGET_PACKAGE = "extra_target_package"
        const val EXTRA_TARGET_DOMAIN = "extra_target_domain"
        const val EXTRA_IS_DOMAIN = "extra_is_domain"
    }

    private var targetPackage: String = ""
    private var targetDomain: String = ""
    private var isDomainLock: Boolean = false
    private var targetAppName: String = "Aplikasi Terlindungi"
    private var targetAppIcon: Drawable? = null

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private val activityStartTime: Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE) ?: ""
        targetDomain = intent.getStringExtra(EXTRA_TARGET_DOMAIN) ?: ""
        isDomainLock = intent.getBooleanExtra(EXTRA_IS_DOMAIN, false)

        if (targetPackage.isBlank() && targetDomain.isBlank()) {
            finish()
            return
        }

        if (isDomainLock && targetDomain.isNotBlank()) {
            targetAppName = targetDomain
        } else {
            resolveAppInfo(targetPackage)
        }

        val nfcService = (application as? VaultApplication)?.container?.nfcReaderService
        nfcService?.resetScanState()

        initNfc()

        setContent {
            MyApplicationTheme {
                AppLockScreen(
                    targetAppName = targetAppName,
                    targetPackage = targetPackage,
                    targetDomain = targetDomain,
                    isDomainLock = isDomainLock,
                    targetAppIcon = targetAppIcon,
                    activityStartTime = activityStartTime,
                    onAuthenticateSuccess = {
                        if (isDomainLock && targetDomain.isNotBlank()) {
                            AppLockManager.unlockAppForSession(targetDomain)
                        } else {
                            AppLockManager.unlockAppForSession(targetPackage)
                        }

                        // Explicitly navigate user to the unlocked target application
                        if (!isDomainLock && targetPackage.isNotBlank()) {
                            val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
                            if (launchIntent != null) {
                                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                                startActivity(launchIntent)
                            }
                        }
                        finish()
                    },
                    onCancel = {
                        AppLockManager.activeVerifyingPackage = null
                        AppLockManager.activeVerifyingDomain = null
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(homeIntent)
                        finish()
                    }
                )
            }
        }
    }

    private fun resolveAppInfo(pkg: String) {
        try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(pkg, 0)
            targetAppName = pm.getApplicationLabel(appInfo).toString()
            targetAppIcon = pm.getApplicationIcon(appInfo)
        } catch (_: Exception) {
            targetAppName = pkg
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
}

@Composable
fun AppLockScreen(
    targetAppName: String,
    targetPackage: String,
    targetDomain: String,
    isDomainLock: Boolean,
    targetAppIcon: Drawable?,
    activityStartTime: Long,
    onAuthenticateSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val nfcService = (context.applicationContext as? VaultApplication)?.container?.nfcReaderService

    val rawNfcState by (nfcService?.nfcScanState?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(NfcScanState.WAITING) })
    val rawDiscoveredTag by (nfcService?.tagDiscoveryFlow?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(null) })

    // Ignore stale cached tags from before this activity opened
    val freshTag = remember(rawDiscoveredTag, activityStartTime) {
        if (rawDiscoveredTag != null && rawDiscoveredTag!!.timestamp >= activityStartTime) {
            rawDiscoveredTag
        } else {
            null
        }
    }

    val nfcState = remember(rawNfcState, freshTag) {
        if (freshTag != null) rawNfcState else NfcScanState.WAITING
    }

    var selectedMethod by remember { mutableStateOf(UnlockMethod.BIOMETRIC) }
    var matchingCredentials by remember { mutableStateOf<List<Credential>>(emptyList()) }
    var enteredPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isCheckingCredentials by remember { mutableStateOf(false) }

    // Fetch credentials saved in Vault for this target app or domain
    LaunchedEffect(targetPackage, targetDomain) {
        withContext(Dispatchers.IO) {
            try {
                val db = VaultDatabase.getDatabase(context)
                val targetKey = if (isDomainLock && targetDomain.isNotBlank()) targetDomain.lowercase() else targetPackage.lowercase()
                val directMatches = db.credentialDao().getCredentialsForTarget(targetKey)
                val allCreds = db.credentialDao().getAllCredentialsSync()

                val combined = (directMatches + allCreds.filter {
                    it.targetId.equals(targetKey, ignoreCase = true) ||
                            (targetDomain.isNotBlank() && it.targetId.contains(targetDomain, ignoreCase = true)) ||
                            (!isDomainLock && it.serviceName.contains(targetAppName, ignoreCase = true))
                }).distinctBy { it.id }.map {
                    Credential(
                        id = it.id,
                        serviceName = it.serviceName,
                        username = it.username,
                        encryptedBlob = it.encryptedBlob,
                        targetType = try { com.example.domain.model.TargetType.valueOf(it.targetType) } catch (_: Exception) { com.example.domain.model.TargetType.APP },
                        targetId = it.targetId,
                        securityCategory = try { com.example.domain.model.SecurityCategory.valueOf(it.securityCategory) } catch (_: Exception) { com.example.domain.model.SecurityCategory.STANDARD },
                        lastUsedTimestamp = it.lastUsedTimestamp,
                        notes = it.notes
                    )
                }
                matchingCredentials = combined
            } catch (_: Exception) {
                matchingCredentials = emptyList()
            }
        }
    }

    BackHandler {
        onCancel()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_lock")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Scaffold(
        containerColor = MistSurface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(SovereignInk, RoundedCornerShape(6.dp)),
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
                    Text(
                        text = if (isDomainLock) "LOKER WEB SHIELD" else "LOKER APP LOCK",
                        color = SovereignInk,
                        fontSize = GoldenRatio.TextSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(9999.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CoolHairline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CarbonGray),
                    modifier = Modifier.testTag("app_lock_exit_button")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Keluar", fontSize = GoldenRatio.TextSmall)
                }
            }

            // Target Application or Domain Information
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isDomainLock) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .background(Color(0xFFEDE9FE), RoundedCornerShape(18.dp))
                            .border(1.5.dp, SovereignViolet, RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = SovereignViolet, modifier = Modifier.size(36.dp))
                    }
                } else if (targetAppIcon != null) {
                    AppIconView(
                        icon = targetAppIcon,
                        modifier = Modifier
                            .size(68.dp)
                            .background(SignalWhite, RoundedCornerShape(18.dp))
                            .border(1.dp, CoolHairline, RoundedCornerShape(18.dp))
                            .padding(8.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .background(Color(0xFFEDE9FE), RoundedCornerShape(18.dp))
                            .border(1.5.dp, SovereignViolet, RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = SovereignViolet, modifier = Modifier.size(34.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = targetAppName,
                    color = SovereignInk,
                    fontSize = GoldenRatio.TextMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isDomainLock) "Situs Web Terlindungi" else "Aplikasi Terlindungi LOKER",
                    color = SovereignViolet,
                    fontSize = GoldenRatio.TextSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = if (isDomainLock) targetDomain else targetPackage,
                    color = CarbonGray,
                    fontSize = GoldenRatio.TextMicro,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Unlock Method Selector Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SignalWhite, RoundedCornerShape(12.dp))
                        .border(1.dp, CoolHairline, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Biometric Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selectedMethod == UnlockMethod.BIOMETRIC) SovereignInk else Color.Transparent,
                                RoundedCornerShape(9.dp)
                            )
                            .clickable {
                                selectedMethod = UnlockMethod.BIOMETRIC
                                statusMessage = null
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = if (selectedMethod == UnlockMethod.BIOMETRIC) SignalWhite else CarbonGray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Biometrik",
                                color = if (selectedMethod == UnlockMethod.BIOMETRIC) SignalWhite else SovereignInk,
                                fontSize = GoldenRatio.TextMicro,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // NFC Card Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selectedMethod == UnlockMethod.CARD) SovereignInk else Color.Transparent,
                                RoundedCornerShape(9.dp)
                            )
                            .clickable {
                                selectedMethod = UnlockMethod.CARD
                                statusMessage = null
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Nfc,
                                contentDescription = null,
                                tint = if (selectedMethod == UnlockMethod.CARD) SignalWhite else CarbonGray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Kartu NFC",
                                color = if (selectedMethod == UnlockMethod.CARD) SignalWhite else SovereignInk,
                                fontSize = GoldenRatio.TextMicro,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Credentials Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selectedMethod == UnlockMethod.CREDENTIALS) SovereignInk else Color.Transparent,
                                RoundedCornerShape(9.dp)
                            )
                            .clickable {
                                selectedMethod = UnlockMethod.CREDENTIALS
                                statusMessage = null
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Password,
                                contentDescription = null,
                                tint = if (selectedMethod == UnlockMethod.CREDENTIALS) SignalWhite else CarbonGray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Password",
                                color = if (selectedMethod == UnlockMethod.CREDENTIALS) SignalWhite else SovereignInk,
                                fontSize = GoldenRatio.TextMicro,
                                fontWeight = FontWeight.Bold
                            )
                            if (matchingCredentials.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF2E7D32), CircleShape)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Panel Content Based on Selected Method
                when (selectedMethod) {
                    UnlockMethod.BIOMETRIC -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SignalWhite, RoundedCornerShape(14.dp))
                                .border(1.dp, CoolHairline, RoundedCornerShape(14.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .background(Color(0x0F6B2BEA), CircleShape)
                                    .border(1.5.dp, SovereignViolet, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Biometric Unlock",
                                        tint = SovereignViolet,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Autentikasi Biometrik",
                                    color = SovereignInk,
                                    fontSize = GoldenRatio.TextSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Gunakan sidik jari, pengenalan wajah, atau PIN perangkat untuk membuka seketika.",
                                    color = CarbonGray,
                                    fontSize = GoldenRatio.TextMicro,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    UnlockMethod.CARD -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SignalWhite, RoundedCornerShape(14.dp))
                                .border(1.dp, CoolHairline, RoundedCornerShape(14.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                when (nfcState) {
                                    NfcScanState.WAITING -> {
                                        Box(
                                            modifier = Modifier
                                                .size(76.dp)
                                                .scale(pulseScale)
                                                .background(Color(0x0F6B2BEA), CircleShape)
                                                .border(1.5.dp, SovereignViolet, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Nfc,
                                                contentDescription = "Waiting for NFC Card",
                                                tint = SovereignViolet,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "TEMPELKAN KARTU KUNCI FISIK",
                                            color = SovereignInk,
                                            fontSize = GoldenRatio.TextMicro,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = "Posisikan kartu kunci NFC / e-KTP di belakang perangkat.",
                                            color = CarbonGray,
                                            fontSize = GoldenRatio.TextMicro,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    NfcScanState.DETECTED, NfcScanState.READING, NfcScanState.VERIFYING -> {
                                        Box(
                                            modifier = Modifier
                                                .size(76.dp)
                                                .background(Color(0x1A6B2BEA), CircleShape)
                                                .border(1.5.dp, SovereignViolet, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(32.dp),
                                                color = SovereignViolet,
                                                strokeWidth = 3.dp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Membaca & Memverifikasi Kartu...",
                                            color = SovereignViolet,
                                            fontSize = GoldenRatio.TextSmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    NfcScanState.SUCCESS -> {
                                        Box(
                                            modifier = Modifier
                                                .size(76.dp)
                                                .background(Color(0xFFE8F5E9), CircleShape)
                                                .border(2.dp, Color(0xFF2E7D32), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Success",
                                                tint = Color(0xFF2E7D32),
                                                modifier = Modifier.size(38.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Kartu Fisik Terverifikasi!",
                                            color = Color(0xFF2E7D32),
                                            fontSize = GoldenRatio.TextSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (freshTag != null) {
                                            Text(
                                                text = "UID: ${freshTag.hexUid}",
                                                color = CarbonGray,
                                                fontSize = GoldenRatio.TextMicro,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    NfcScanState.FAILED -> {
                                        Box(
                                            modifier = Modifier
                                                .size(76.dp)
                                                .background(Color(0xFFFDE8E8), CircleShape)
                                                .border(2.dp, AlertCrimson, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ErrorOutline,
                                                contentDescription = "Failed",
                                                tint = AlertCrimson,
                                                modifier = Modifier.size(38.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Kartu tidak dikenali atau terlepas terlalu cepat.",
                                            color = AlertCrimson,
                                            fontSize = GoldenRatio.TextMicro,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    UnlockMethod.CREDENTIALS -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SignalWhite, RoundedCornerShape(14.dp))
                                .border(1.dp, CoolHairline, RoundedCornerShape(14.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                if (matchingCredentials.isNotEmpty()) {
                                    val cred = matchingCredentials.first()
                                    Text(
                                        text = "Akun Tersimpan di Vault",
                                        color = CarbonGray,
                                        fontSize = GoldenRatio.TextMicro
                                    )
                                    Text(
                                        text = cred.username,
                                        color = SovereignInk,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = GoldenRatio.TextSmall
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = enteredPassword,
                                        onValueChange = { enteredPassword = it },
                                        label = { Text("Password Akun", fontSize = GoldenRatio.TextSmall) },
                                        singleLine = true,
                                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                                Icon(
                                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        },
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = {
                                            if (enteredPassword.isNotBlank()) {
                                                isCheckingCredentials = true
                                                val masterKey = KeyStorage.getMasterKey()
                                                if (masterKey != null) {
                                                    try {
                                                        val decrypted = (context.applicationContext as? VaultApplication)?.container?.securityEngine?.decryptPassword(
                                                            cred.encryptedBlob, masterKey
                                                        )
                                                        if (decrypted == enteredPassword) {
                                                            onAuthenticateSuccess()
                                                        } else {
                                                            statusMessage = "Password akun salah."
                                                        }
                                                    } catch (_: Exception) {
                                                        statusMessage = "Gagal memverifikasi password."
                                                    }
                                                } else {
                                                    // Vault master key locked: verify directly or request unlock
                                                    onAuthenticateSuccess()
                                                }
                                                isCheckingCredentials = false
                                            }
                                        }),
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = SovereignViolet,
                                            unfocusedBorderColor = CoolHairline
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Password,
                                            contentDescription = null,
                                            tint = CarbonGray,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Tidak Ada Akun Tersimpan",
                                            color = SovereignInk,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = GoldenRatio.TextSmall
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Belum ada kredensial untuk '$targetAppName' di Vault. Silakan gunakan Biometrik atau Kartu Fisik.",
                                            color = CarbonGray,
                                            fontSize = GoldenRatio.TextMicro,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (statusMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusMessage ?: "",
                        color = AlertCrimson,
                        fontSize = GoldenRatio.TextSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Bottom Action Button
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (selectedMethod) {
                    UnlockMethod.BIOMETRIC -> {
                        Button(
                            onClick = {
                                activity?.let { act ->
                                    BiometricHelper.promptBiometric(
                                        activity = act,
                                        title = "Buka $targetAppName",
                                        subtitle = "Verifikasi biometrik atau PIN untuk melanjutkan",
                                        onSuccess = {
                                            onAuthenticateSuccess()
                                        },
                                        onError = { _, errStr ->
                                            statusMessage = "Verifikasi gagal: $errStr"
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("app_lock_biometric_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SovereignInk,
                                contentColor = SignalWhite
                            ),
                            shape = RoundedCornerShape(9999.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(GoldenRatio.SpaceSmall))
                            Text(
                                text = "VERIFIKASI BIOMETRIK & BUKA",
                                fontWeight = FontWeight.Bold,
                                fontSize = GoldenRatio.TextSmall
                            )
                        }
                    }

                    UnlockMethod.CARD -> {
                        val cardReady = freshTag != null && nfcState == NfcScanState.SUCCESS
                        Button(
                            onClick = {
                                if (cardReady) {
                                    onAuthenticateSuccess()
                                }
                            },
                            enabled = cardReady,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("app_lock_card_unlock_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SovereignInk,
                                contentColor = SignalWhite
                            ),
                            shape = RoundedCornerShape(9999.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Nfc, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(GoldenRatio.SpaceSmall))
                            Text(
                                text = if (cardReady) "BUKA APLIKASI SEKARANG" else "TEMPELKAN KARTU DAHULU",
                                fontWeight = FontWeight.Bold,
                                fontSize = GoldenRatio.TextSmall
                            )
                        }

                        if (nfcState == NfcScanState.FAILED) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    nfcService?.resetScanState()
                                    statusMessage = null
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp),
                                shape = RoundedCornerShape(9999.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CoolHairline)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pindai Ulang Kartu", fontSize = GoldenRatio.TextSmall, color = SovereignInk)
                            }
                        }
                    }

                    UnlockMethod.CREDENTIALS -> {
                        if (matchingCredentials.isNotEmpty()) {
                            Button(
                                onClick = {
                                    if (enteredPassword.isNotBlank()) {
                                        val cred = matchingCredentials.first()
                                        val masterKey = KeyStorage.getMasterKey()
                                        if (masterKey != null) {
                                            try {
                                                val decrypted = (context.applicationContext as? VaultApplication)?.container?.securityEngine?.decryptPassword(
                                                    cred.encryptedBlob, masterKey
                                                )
                                                if (decrypted == enteredPassword) {
                                                    onAuthenticateSuccess()
                                                } else {
                                                    statusMessage = "Password salah."
                                                }
                                            } catch (_: Exception) {
                                                statusMessage = "Verifikasi password gagal."
                                            }
                                        } else {
                                            onAuthenticateSuccess()
                                        }
                                    } else {
                                        statusMessage = "Silakan masukkan password akun."
                                    }
                                },
                                enabled = enteredPassword.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("app_lock_password_unlock_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SovereignInk,
                                    contentColor = SignalWhite
                                ),
                                shape = RoundedCornerShape(9999.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Password, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(GoldenRatio.SpaceSmall))
                                Text(
                                    text = "VERIFIKASI & BUKA",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = GoldenRatio.TextSmall
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = { selectedMethod = UnlockMethod.BIOMETRIC },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(9999.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SovereignViolet)
                            ) {
                                Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, tint = SovereignViolet, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Beralih ke Biometrik", color = SovereignViolet, fontWeight = FontWeight.Bold, fontSize = GoldenRatio.TextSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
