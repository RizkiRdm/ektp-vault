package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.domain.model.NfcTagData
import com.example.ui.AuthSheetAction
import com.example.ui.AuthSheetState
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CarbonGray
import com.example.ui.theme.CoolHairline
import com.example.ui.theme.MistSurface
import com.example.ui.theme.SignalWhite
import com.example.ui.theme.SovereignInk
import com.example.ui.theme.SovereignViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthBottomSheet(
    authSheetState: AuthSheetState,
    lockoutSeconds: Int,
    lastDiscoveredTag: NfcTagData? = null,
    onDismiss: () -> Unit,
    onRequestBiometricAuth: (rawUid: String) -> Unit
) {
    if (!authSheetState.isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var customUid by remember(lastDiscoveredTag) {
        mutableStateOf(lastDiscoveredTag?.hexUid ?: "04:A2:3B:5F:7E:89")
    }
    var showCustomUidInput by remember { mutableStateOf(false) }

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
                            text = "Cooldown: $lockoutSeconds detik tersisa",
                            color = SovereignInk,
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Eyebrow
            Text(
                text = "SOVEREIGN HARDWARE ANCHOR",
                color = SovereignViolet,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Header Title
            Text(
                text = "Hold e-KTP to Device",
                color = SovereignInk,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (authSheetState.action) {
                    AuthSheetAction.SETUP_VAULT -> "Inisialisasi kunci fisik e-KTP perdana"
                    AuthSheetAction.UNLOCK_VAULT -> "Dekripsi database hardware-anchored"
                    AuthSheetAction.REVEAL_PASSWORD -> "Otorisasi pembacaan password sensitif"
                    AuthSheetAction.ADD_PHYSICAL_KEY -> "Registrasi kunci NFC baru (BYOK)"
                    AuthSheetAction.EXPORT_VAULT -> "Otorisasi ekspor backup .loker"
                },
                color = CarbonGray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // NFC Radar Graphic (NEVERHACK marble & violet accent)
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
                        contentDescription = "NFC Scanner",
                        tint = SovereignViolet,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "NFC ACTIVE",
                        color = SovereignInk,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Biometric Readiness Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MistSurface, RoundedCornerShape(10.dp))
                    .border(1.dp, CoolHairline, RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Biometric Sensor",
                    tint = SovereignViolet,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FINGERPRINT VERIFICATION",
                        color = SovereignInk,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = "● TEE Hardware Key Ready",
                        color = SovereignViolet,
                        fontSize = 11.sp
                    )
                }
            }

            // Real-time Tag Telemetry if detected
            if (lastDiscoveredTag != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MistSurface, RoundedCornerShape(10.dp))
                        .border(1.dp, CoolHairline, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "HARDWARE DISCOVERY DETECTED",
                            color = SovereignViolet,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "UID: ${lastDiscoveredTag.hexUid} • Tech: ${lastDiscoveredTag.techList.joinToString(", ")}",
                            color = SovereignInk,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // High-Risk Single-Use Warning
            if (authSheetState.isHighRiskAction) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFDE8E8), RoundedCornerShape(10.dp))
                        .border(1.dp, AlertCrimson, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "High Risk Warning",
                            tint = AlertCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "CRITICAL / HIGH-RISK SINGLE-USE",
                                color = AlertCrimson,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.6.sp
                            )
                            Text(
                                text = "Data dan kunci dekripsi akan seketika di-wipe dari RAM begitu modal tertutup atau 15 detik berakhir.",
                                color = SovereignInk,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Action Button (Sovereign Ink 9999px pill triggering BiometricPrompt)
            Button(
                onClick = {
                    onRequestBiometricAuth(customUid)
                },
                enabled = lockoutSeconds == 0,
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "OTORISASI BIOMETRIK & BUKA",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Ghost Outlined Button (NEVERHACK style)
            OutlinedButton(
                onClick = { showCustomUidInput = !showCustomUidInput },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("toggle_custom_uid_button"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = SovereignInk
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, CoolHairline),
                shape = RoundedCornerShape(9999.dp)
            ) {
                Text(
                    text = if (showCustomUidInput) "SEMBUNYIKAN SIMULASI UID" else "SIMULASI UID KTP MANUAL",
                    fontSize = 11.sp,
                    letterSpacing = 0.6.sp
                )
            }

            if (showCustomUidInput) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = customUid,
                    onValueChange = { customUid = it },
                    label = { Text("NFC Static UID (ISO 14443-4)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_uid_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SovereignViolet,
                        unfocusedBorderColor = CoolHairline,
                        focusedTextColor = SovereignInk,
                        unfocusedTextColor = SovereignInk,
                        focusedLabelColor = SovereignViolet
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
