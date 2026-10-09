package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.NfcScanState
import com.example.domain.model.NfcTagData
import com.example.ui.AuthSheetAction
import com.example.ui.AuthSheetState
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CarbonGray
import com.example.ui.theme.CoolHairline
import com.example.ui.theme.GoldenRatio
import com.example.ui.theme.MistSurface
import com.example.ui.theme.SignalWhite
import com.example.ui.theme.SovereignInk
import com.example.ui.theme.SovereignViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthBottomSheet(
    authSheetState: AuthSheetState,
    lockoutSeconds: Int,
    nfcScanState: NfcScanState = NfcScanState.WAITING,
    failureCount: Int = 0,
    lastDiscoveredTag: NfcTagData? = null,
    onDismiss: () -> Unit,
    onResetNfcScan: () -> Unit = {},
    onRequestBiometricAuth: (rawUid: String) -> Unit
) {
    if (!authSheetState.isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val infiniteTransition = rememberInfiniteTransition(label = "nfc_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SignalWhite,
        scrimColor = Color(0x660A0F1F),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .background(CoolHairline, RoundedCornerShape(9999.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Lockout Indicator
            if (lockoutSeconds > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFDE8E8), RoundedCornerShape(10.dp))
                        .border(1.dp, AlertCrimson, RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "SYSTEM LOCKOUT ACTIVE",
                            color = AlertCrimson,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Cooldown: $lockoutSeconds seconds remaining",
                            color = SovereignInk,
                            fontSize = GoldenRatio.TextSmall
                        )
                    }
                }
                Spacer(modifier = Modifier.height(GoldenRatio.SpaceBase))
            }

            // Header Title
            Text(
                text = "Tap Physical Security Key",
                color = SovereignInk,
                fontSize = GoldenRatio.TextMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (authSheetState.action) {
                    AuthSheetAction.SETUP_VAULT -> "Register primary security card to initialize vault"
                    AuthSheetAction.UNLOCK_VAULT -> "Verify registered security card to unlock vault"
                    AuthSheetAction.REVEAL_PASSWORD -> "Authorize sensitive credential decryption"
                    AuthSheetAction.ADD_PHYSICAL_KEY -> "Register supplementary hardware key card"
                    AuthSheetAction.EXPORT_VAULT -> "Authorize encrypted backup export"
                },
                color = CarbonGray,
                fontSize = GoldenRatio.TextSmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(GoldenRatio.SpaceLarge))

            // NFC State Visual Indicator
            when (nfcScanState) {
                NfcScanState.WAITING -> {
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .scale(pulseScale)
                            .background(Color(0x0F6B2BEA), CircleShape)
                            .border(1.5.dp, SovereignViolet, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Nfc,
                                contentDescription = "Waiting for Security Key",
                                tint = SovereignViolet,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "AWAITING",
                                color = SovereignInk,
                                fontSize = GoldenRatio.TextMicro,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(GoldenRatio.SpaceSmall))
                    Text(
                        text = "Hold physical security card against the back of your phone",
                        color = CarbonGray,
                        fontSize = GoldenRatio.TextSmall,
                        textAlign = TextAlign.Center
                    )
                }

                NfcScanState.DETECTED, NfcScanState.READING, NfcScanState.VERIFYING -> {
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .background(Color(0x1A6B2BEA), CircleShape)
                            .border(1.5.dp, SovereignViolet, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = SovereignViolet,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (nfcScanState == NfcScanState.DETECTED) "DETECTED"
                                else if (nfcScanState == NfcScanState.READING) "READING"
                                else "VERIFYING",
                                color = SovereignInk,
                                fontSize = GoldenRatio.TextMicro,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(GoldenRatio.SpaceSmall))
                    Text(
                        text = "Authenticating ISO 14443-4 hardware key token...",
                        color = SovereignViolet,
                        fontSize = GoldenRatio.TextSmall,
                        textAlign = TextAlign.Center
                    )
                }

                NfcScanState.SUCCESS -> {
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .background(Color(0xFFE8F5E9), CircleShape)
                            .border(2.dp, Color(0xFF2E7D32), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "VERIFIED",
                                color = Color(0xFF2E7D32),
                                fontSize = GoldenRatio.TextMicro,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(GoldenRatio.SpaceSmall))
                    Text(
                        text = "Security card verified. Proceed to biometric authorization.",
                        color = Color(0xFF2E7D32),
                        fontSize = GoldenRatio.TextSmall,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                NfcScanState.FAILED -> {
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .background(Color(0xFFFDE8E8), CircleShape)
                            .border(2.dp, AlertCrimson, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Failed",
                                tint = AlertCrimson,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "FAILED",
                                color = AlertCrimson,
                                fontSize = GoldenRatio.TextMicro,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(GoldenRatio.SpaceSmall))
                    Text(
                        text = if (failureCount > 0) "Attempts failed: $failureCount of 3. Please hold card firmly."
                        else "Card reading failed. Please tap again.",
                        color = AlertCrimson,
                        fontSize = GoldenRatio.TextSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Real-time Tag Telemetry
            if (lastDiscoveredTag != null) {
                Spacer(modifier = Modifier.height(GoldenRatio.SpaceBase))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MistSurface, RoundedCornerShape(10.dp))
                        .border(1.dp, CoolHairline, RoundedCornerShape(10.dp))
                        .padding(GoldenRatio.SpaceSmall)
                ) {
                    Column {
                        Text(
                            text = "DETECTED HARDWARE KEY IDENTITY",
                            color = SovereignViolet,
                            fontSize = GoldenRatio.TextMicro,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "UID: ${lastDiscoveredTag.hexUid} • ${if (lastDiscoveredTag.isIsoDep) "ISO 14443-4" else "Contactless Tag"}",
                            color = SovereignInk,
                            fontSize = GoldenRatio.TextMicro,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // High-Risk Single-Use Warning
            if (authSheetState.isHighRiskAction) {
                Spacer(modifier = Modifier.height(GoldenRatio.SpaceBase))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFDE8E8), RoundedCornerShape(10.dp))
                        .border(1.dp, AlertCrimson, RoundedCornerShape(10.dp))
                        .padding(GoldenRatio.SpaceSmall)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "High-Risk Alert",
                            tint = AlertCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(GoldenRatio.SpaceSmall))
                        Column {
                            Text(
                                text = "HIGH-RISK / EPHEMERAL POLICY",
                                color = AlertCrimson,
                                fontSize = GoldenRatio.TextMicro,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.6.sp
                            )
                            Text(
                                text = "Decryption keys and plaintext passwords will be wiped permanently from memory after 15 seconds.",
                                color = SovereignInk,
                                fontSize = GoldenRatio.TextMicro,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(GoldenRatio.SpaceLarge))

            // Primary Action Button (Triggers BiometricPrompt)
            val canProceed = lockoutSeconds == 0 && lastDiscoveredTag != null && nfcScanState != NfcScanState.FAILED

            Button(
                onClick = {
                    lastDiscoveredTag?.let {
                        onRequestBiometricAuth(it.hexUid)
                    }
                },
                enabled = canProceed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("auth_confirm_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (authSheetState.isHighRiskAction) AlertCrimson else SovereignInk,
                    contentColor = SignalWhite
                ),
                shape = RoundedCornerShape(9999.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(GoldenRatio.SpaceSmall))
                    Text(
                        text = if (lastDiscoveredTag == null) "TAP SECURITY KEY TO CONTINUE"
                        else "AUTHORIZE BIOMETRIC & UNLOCK",
                        fontWeight = FontWeight.Medium,
                        fontSize = GoldenRatio.TextSmall,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Retry Button when scan failed or card moved
            if (nfcScanState == NfcScanState.FAILED || (lastDiscoveredTag == null && nfcScanState != NfcScanState.WAITING)) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onResetNfcScan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("retry_nfc_button"),
                    shape = RoundedCornerShape(9999.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CoolHairline)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        modifier = Modifier.size(16.dp),
                        tint = SovereignInk
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RETRY CARD SCAN",
                        fontSize = GoldenRatio.TextMicro,
                        fontWeight = FontWeight.Medium,
                        color = SovereignInk
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
