package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Credential
import com.example.domain.model.SecurityCategory
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CarbonGray
import com.example.ui.theme.CoolHairline
import com.example.ui.theme.MistSurface
import com.example.ui.theme.SignalWhite
import com.example.ui.theme.SovereignInk
import com.example.ui.theme.SovereignViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    credential: Credential?,
    revealedPassword: String?,
    revealCountdown: Int,
    onBack: () -> Unit,
    onTriggerReveal: (isHighRisk: Boolean) -> Unit,
    onHideAndScrub: () -> Unit,
    onEdit: (credentialId: String) -> Unit,
    onDelete: (credentialId: String) -> Unit,
    onToggleRiskCategory: (credentialId: String, newCategory: SecurityCategory) -> Unit
) {
    if (credential == null) {
        onBack()
        return
    }

    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var copiedLabel by remember { mutableStateOf<String?>(null) }
    val isHighRisk = credential.securityCategory == SecurityCategory.HIGH_RISK

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "CONFIRM DELETION",
                    color = AlertCrimson,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete '${credential.serviceName}'? Encrypted payload will be erased from local storage.",
                    color = SovereignInk,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(credential.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertCrimson),
                    shape = RoundedCornerShape(9999.dp)
                ) {
                    Text("DELETE", fontWeight = FontWeight.Medium, color = SignalWhite)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirm = false },
                    shape = RoundedCornerShape(9999.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CoolHairline)
                ) {
                    Text("CANCEL", color = SovereignInk)
                }
            },
            containerColor = SignalWhite,
            shape = RoundedCornerShape(14.dp)
        )
    }

    Scaffold(
        containerColor = MistSurface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "CREDENTIAL RECORD",
                        color = SovereignInk,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = SovereignInk
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onEdit(credential.id) },
                        modifier = Modifier.testTag("detail_edit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = SovereignInk
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SignalWhite
                ),
                modifier = Modifier.border(0.5.dp, CoolHairline)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Service Card (NEVERHACK card style)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SignalWhite, RoundedCornerShape(14.dp))
                    .border(1.dp, CoolHairline, RoundedCornerShape(14.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = credential.serviceName,
                            color = SovereignInk,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isHighRisk) Color(0xFFFDE8E8) else Color(0xFFF0EBFD),
                                    RoundedCornerShape(9999.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isHighRisk) AlertCrimson else SovereignViolet,
                                    RoundedCornerShape(9999.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = credential.securityCategory.name,
                                color = if (isHighRisk) AlertCrimson else SovereignViolet,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Target ID: ${credential.targetId.ifBlank { "Not configured" }}",
                        color = CarbonGray,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Username Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SignalWhite, RoundedCornerShape(14.dp))
                    .border(1.dp, CoolHairline, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "USERNAME / OPERATOR",
                            color = CarbonGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = credential.username,
                            color = SovereignInk,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(credential.username))
                            copiedLabel = "Username disalin!"
                        },
                        modifier = Modifier.testTag("copy_username_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Salin Username",
                            tint = SovereignViolet
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Password Reveal Section (NEVERHACK TEE Hardware Reveal)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SignalWhite, RoundedCornerShape(14.dp))
                    .border(
                        1.dp,
                        if (revealedPassword != null) SovereignViolet else CoolHairline,
                        RoundedCornerShape(14.dp)
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "PASSWORD (ZERO-VISIBILITY PROTECTED)",
                        color = CarbonGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (revealedPassword == null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = SovereignViolet,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "•••••••••••••••• (Keystore AES-256-GCM)",
                                    color = CarbonGray,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // NEVERHACK Primary Button
                        Button(
                            onClick = { onTriggerReveal(isHighRisk) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("tap_ktp_reveal_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isHighRisk) AlertCrimson else SovereignInk,
                                contentColor = SignalWhite
                            ),
                            shape = RoundedCornerShape(9999.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Nfc,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TAP KTP TO REVEAL",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp
                            )
                        }
                    } else {
                        // Plaintext Revealed with 15s Countdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = revealedPassword,
                                color = SovereignInk,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(revealedPassword))
                                    copiedLabel = "Password disalin ke clipboard!"
                                },
                                modifier = Modifier.testTag("copy_password_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Salin Password",
                                    tint = SovereignViolet
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { revealCountdown / 15f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = if (revealCountdown <= 5) AlertCrimson else SovereignViolet,
                            trackColor = CoolHairline,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RAM wipe in: ${revealCountdown}s",
                                color = if (revealCountdown <= 5) AlertCrimson else SovereignViolet,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )

                            OutlinedButton(
                                onClick = onHideAndScrub,
                                shape = RoundedCornerShape(9999.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CoolHairline),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertCrimson)
                            ) {
                                Text(
                                    "HIDE & SCRUB NOW",
                                    fontSize = 10.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }

            if (copiedLabel != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "✓ $copiedLabel",
                    color = SovereignViolet,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Security Policy & Risk Category
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SignalWhite, RoundedCornerShape(14.dp))
                    .border(1.dp, CoolHairline, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = if (isHighRisk) AlertCrimson else SovereignViolet,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SECURITY LIFECYCLE POLICY",
                            color = SovereignInk,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isHighRisk) {
                            "• Policy: Plan A Single-Use Instant Wipe\n• Session Timeout: 0s\n• Memory: Key and plaintext wiped immediately after revelation."
                        } else {
                            "• Policy: Plan B Standard Enterprise Session\n• Session Timeout: 300s (5 minutes)\n• Memory: Wiped on background or idle expiration."
                        },
                        color = CarbonGray,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            val newCat = if (isHighRisk) SecurityCategory.STANDARD else SecurityCategory.HIGH_RISK
                            onToggleRiskCategory(credential.id, newCat)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("toggle_risk_category_button"),
                        shape = RoundedCornerShape(9999.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CoolHairline)
                    ) {
                        Text(
                            text = if (isHighRisk) "SWITCH TO STANDARD SESSION" else "SWITCH TO HIGH-RISK SINGLE-USE",
                            color = if (isHighRisk) SovereignViolet else AlertCrimson,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            if (credential.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SignalWhite, RoundedCornerShape(14.dp))
                        .border(1.dp, CoolHairline, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "NOTES",
                            color = CarbonGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = credential.notes,
                            color = SovereignInk,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Delete Button (NEVERHACK Alert emergency style)
            OutlinedButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("delete_credential_button"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AlertCrimson
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                shape = RoundedCornerShape(9999.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DELETE CREDENTIAL FROM VAULT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
