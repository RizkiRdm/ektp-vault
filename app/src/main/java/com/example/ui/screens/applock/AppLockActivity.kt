package com.example.ui.screens.applock

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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
import com.example.domain.model.NfcScanState
import com.example.domain.model.NfcTagData
import com.example.security.AppLockManager
import com.example.security.BiometricHelper
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
import kotlinx.coroutines.launch

class AppLockActivity : FragmentActivity() {

    companion object {
        const val EXTRA_TARGET_PACKAGE = "extra_target_package"
    }

    private var targetPackage: String = ""
    private var targetAppName: String = "Aplikasi Terlindungi"
    private var targetAppIcon: Drawable? = null

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE) ?: ""
        if (targetPackage.isBlank()) {
            finish()
            return
        }

        resolveAppInfo(targetPackage)
        initNfc()

        setContent {
            MyApplicationTheme {
                AppLockScreen(
                    targetAppName = targetAppName,
                    targetPackage = targetPackage,
                    targetAppIcon = targetAppIcon,
                    onAuthenticateSuccess = {
                        AppLockManager.unlockAppForSession(targetPackage)
                        finish()
                    },
                    onCancel = {
                        AppLockManager.activeVerifyingPackage = null
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
    targetAppIcon: Drawable?,
    onAuthenticateSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val nfcService = (context.applicationContext as? VaultApplication)?.container?.nfcReaderService

    val nfcState by (nfcService?.nfcScanState?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(NfcScanState.WAITING) })
    val discoveredTag by (nfcService?.tagDiscoveryFlow?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(null) })

    var failureCount by remember { mutableIntStateOf(0) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

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
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar with Cancel button
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
                        text = "LOKER APP LOCK",
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
                    modifier = Modifier.testTag("app_lock_cancel_button")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Exit", fontSize = GoldenRatio.TextSmall)
                }
            }

            // Target App Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (targetAppIcon != null) {
                    AppIconView(
                        icon = targetAppIcon,
                        modifier = Modifier
                            .size(72.dp)
                            .background(SignalWhite, RoundedCornerShape(18.dp))
                            .border(1.dp, CoolHairline, RoundedCornerShape(18.dp))
                            .padding(8.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color(0xFFEDE9FE), RoundedCornerShape(18.dp))
                            .border(1.dp, SovereignViolet, RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = SovereignViolet, modifier = Modifier.size(36.dp))
                    }
                }

                Spacer(modifier = Modifier.height(GoldenRatio.SpaceBase))

                Text(
                    text = targetAppName,
                    color = SovereignInk,
                    fontSize = GoldenRatio.TextMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Application Locked by LOKER",
                    color = SovereignViolet,
                    fontSize = GoldenRatio.TextSmall,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = targetPackage,
                    color = CarbonGray,
                    fontSize = GoldenRatio.TextMicro,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(GoldenRatio.SpaceLarge))

                // NFC State Card
                when (nfcState) {
                    NfcScanState.WAITING -> {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .scale(pulseScale)
                                .background(Color(0x0F6B2BEA), CircleShape)
                                .border(1.5.dp, SovereignViolet, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Nfc,
                                    contentDescription = "Waiting for Hardware Key Card",
                                    tint = SovereignViolet,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "TAP KEY CARD",
                                    color = SovereignInk,
                                    fontSize = GoldenRatio.TextMicro,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(GoldenRatio.SpaceBase))
                        Text(
                            text = "Hold your registered hardware security key to back of device",
                            color = CarbonGray,
                            fontSize = GoldenRatio.TextSmall,
                            textAlign = TextAlign.Center
                        )
                    }

                    NfcScanState.DETECTED, NfcScanState.READING, NfcScanState.VERIFYING -> {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .background(Color(0x1A6B2BEA), CircleShape)
                                .border(1.5.dp, SovereignViolet, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = SovereignViolet,
                                strokeWidth = 3.dp
                            )
                        }
                        Spacer(modifier = Modifier.height(GoldenRatio.SpaceBase))
                        Text(
                            text = "Reading & verifying hardware key card...",
                            color = SovereignViolet,
                            fontSize = GoldenRatio.TextSmall,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }

                    NfcScanState.SUCCESS -> {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .background(Color(0xFFE8F5E9), CircleShape)
                                .border(2.dp, Color(0xFF2E7D32), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(42.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(GoldenRatio.SpaceBase))
                        Text(
                            text = "Security Key Card Verified!",
                            color = Color(0xFF2E7D32),
                            fontSize = GoldenRatio.TextSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    NfcScanState.FAILED -> {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .background(Color(0xFFFDE8E8), CircleShape)
                                .border(2.dp, AlertCrimson, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Failed",
                                tint = AlertCrimson,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(GoldenRatio.SpaceBase))
                        Text(
                            text = "Card unrecognized or removed prematurely. Please try again.",
                            color = AlertCrimson,
                            fontSize = GoldenRatio.TextSmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (discoveredTag != null) {
                    Spacer(modifier = Modifier.height(GoldenRatio.SpaceBase))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SignalWhite, RoundedCornerShape(10.dp))
                            .border(1.dp, CoolHairline, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "UID: ${discoveredTag?.hexUid} • Key Verified",
                            color = SovereignInk,
                            fontSize = GoldenRatio.TextSmall,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (statusMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = statusMessage ?: "",
                        color = AlertCrimson,
                        fontSize = GoldenRatio.TextSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Bottom Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val canAuthorize = discoveredTag != null && nfcState != NfcScanState.FAILED

                Button(
                    onClick = {
                        activity?.let { act ->
                            BiometricHelper.promptBiometric(
                                activity = act,
                                title = "Unlock $targetAppName",
                                subtitle = "Verify fingerprint or device PIN to proceed",
                                onSuccess = {
                                    onAuthenticateSuccess()
                                },
                                onError = { _, errStr ->
                                    failureCount++
                                    statusMessage = "Biometric authentication failed: $errStr"
                                }
                            )
                        }
                    },
                    enabled = canAuthorize,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("app_lock_unlock_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SovereignInk,
                        contentColor = SignalWhite
                    ),
                    shape = RoundedCornerShape(9999.dp)
                ) {
                    Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(GoldenRatio.SpaceSmall))
                    Text(
                        text = if (discoveredTag == null) "TAP SECURITY KEY FIRST" else "AUTHORIZE BIOMETRIC & UNLOCK",
                        fontWeight = FontWeight.Medium,
                        fontSize = GoldenRatio.TextSmall
                    )
                }

                if (nfcState == NfcScanState.FAILED) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            nfcService?.resetScanState()
                            statusMessage = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(9999.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CoolHairline)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry Card Scan", fontSize = GoldenRatio.TextSmall, color = SovereignInk)
                    }
                }
            }
        }
    }
}
