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
                            text = "Cooldown: $lockoutSeconds detik tersisa",
                            color = SovereignInk,
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Header Title
            Text(
                text = "Tempelkan Kunci Fisik e-KTP",
                color = SovereignInk,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (authSheetState.action) {
                    AuthSheetAction.SETUP_VAULT -> "Daftarkan e-KTP perdana untuk membuat brankas"
                    AuthSheetAction.UNLOCK_VAULT -> "Verifikasi e-KTP terdaftar untuk membuka brankas"
                    AuthSheetAction.REVEAL_PASSWORD -> "Otorisasi pembacaan password sensitif"
                    AuthSheetAction.ADD_PHYSICAL_KEY -> "Registrasi kunci NFC fisik tambahan"
                    AuthSheetAction.EXPORT_VAULT -> "Otorisasi enkripsi file backup .loker"
                },
                color = CarbonGray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

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
                                contentDescription = "Menunggu NFC",
                                tint = SovereignViolet,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "MENUNGGU",
                                color = SovereignInk,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Tempelkan kartu e-KTP ke belakang bodi ponsel",
                        color = CarbonGray,
                        fontSize = 12.sp,
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
                                text = if (nfcScanState == NfcScanState.DETECTED) "TERDETEKSI"
                                else if (nfcScanState == NfcScanState.READING) "MEMBACA"
                                else "MEMVERIFIKASI",
                                color = SovereignInk,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Sedang memproses chip ISO 14443-4 e-KTP...",
                        color = SovereignViolet,
                        fontSize = 12.sp,
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
                                contentDescription = "Sukses",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "TERVERIFIKASI",
                                color = Color(0xFF2E7D32),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Kartu e-KTP terverifikasi. Lanjutkan otorisasi biometrik.",
                        color = Color(0xFF2E7D32),
                        fontSize = 12.sp,
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
                                contentDescription = "Gagal",
                                tint = AlertCrimson,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "GAGAL",
                                color = AlertCrimson,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (failureCount > 0) "Percobaan gagal: $failureCount dari 3. Tempelkan kembali kartu dengan benar."
                        else "Pembacaan kartu gagal. Coba tempelkan kembali.",
                        color = AlertCrimson,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Real-time Tag Telemetry
            if (lastDiscoveredTag != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MistSurface, RoundedCornerShape(10.dp))
                        .border(1.dp, CoolHairline, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "IDENTITAS KARTU TERDETEKSI",
                            color = SovereignViolet,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "UID: ${lastDiscoveredTag.hexUid} • ${if (lastDiscoveredTag.isIsoDep) "ISO 14443-4" else "Contactless Tag"}",
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
                            contentDescription = "Peringatan Risiko Tinggi",
                            tint = AlertCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "KEBIJAKAN HIGH-RISK / SEKALI PAKAI",
                                color = AlertCrimson,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.6.sp
                            )
                            Text(
                                text = "Kunci dekripsi dan password akan segera dihapus permanen dari memori RAM setelah 15 detik.",
                                color = SovereignInk,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

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
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (lastDiscoveredTag == null) "TEMPELKAN KARTU UNTUK MELANJUTKAN"
                        else "OTORISASI BIOMETRIK & BUKA",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
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
                        contentDescription = "Coba Lagi",
                        modifier = Modifier.size(16.dp),
                        tint = SovereignInk
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "COBA TEMPELKAN KEMBALI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = SovereignInk
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
