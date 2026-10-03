package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CarbonGray
import com.example.ui.theme.CoolHairline
import com.example.ui.theme.MistSurface
import com.example.ui.theme.SignalWhite
import com.example.ui.theme.SovereignInk
import com.example.ui.theme.SovereignViolet

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecoveryPhraseScreen(
    recoveryPhrase: List<String>?,
    onBack: () -> Unit,
    onRecoverWithPhrase: (words: List<String>, newUid: String, secret: String) -> Unit
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    var isRecoveryMode by remember { mutableStateOf(recoveryPhrase == null) }
    var inputWordsText by remember { mutableStateOf("") }
    var inputNewUid by remember { mutableStateOf("") }
    var copiedNote by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MistSurface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isRecoveryMode) "EMERGENCY RECOVERY" else "RECOVERY PHRASE",
                        color = SovereignInk,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("recovery_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = SovereignInk
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SignalWhite),
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
            if (!isRecoveryMode && recoveryPhrase != null) {
                // View Mode
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF0EBFD), RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFFD4C8FA), RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = SovereignViolet,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "OFFLINE 12-WORD MNEMONIC",
                                color = SovereignViolet,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Record these 12 words offline. In the event of total hardware loss of your primary e-KTP, this phrase is the only cryptographic key to recover your database.",
                                color = SovereignInk,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Words grid (NEVERHACK nested chips)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recoveryPhrase.forEachIndexed { index, word ->
                        Box(
                            modifier = Modifier
                                .background(SignalWhite, RoundedCornerShape(10.dp))
                                .border(1.dp, CoolHairline, RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "${index + 1}. $word",
                                color = SovereignInk,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(recoveryPhrase.joinToString(" ")))
                            copiedNote = "12 Words copied to clipboard!"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SovereignInk,
                            contentColor = SignalWhite
                        ),
                        shape = RoundedCornerShape(9999.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("COPY 12 PHRASE", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { isRecoveryMode = true },
                        shape = RoundedCornerShape(9999.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AlertCrimson),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertCrimson)
                    ) {
                        Text("TEST RECOVERY", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }

                if (copiedNote != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "✓ $copiedNote",
                        color = SovereignViolet,
                        fontSize = 12.sp
                    )
                }
            } else {
                // Recovery input mode
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFDE8E8), RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = AlertCrimson,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "EMERGENCY SOVEREIGN RECOVERY",
                                color = AlertCrimson,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Input the 12-word mnemonic generated during setup along with a replacement physical contactless card.",
                                color = SovereignInk,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "ENTER 12 WORDS (SPACE-SEPARATED):",
                    color = SovereignInk,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = inputWordsText,
                    onValueChange = { inputWordsText = it },
                    placeholder = { Text("abandon ability able about above absent absorb abstract absurd abuse access accident") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("recovery_phrase_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SovereignViolet,
                        unfocusedBorderColor = CoolHairline,
                        focusedTextColor = SovereignInk,
                        unfocusedTextColor = SovereignInk,
                        focusedContainerColor = SignalWhite,
                        unfocusedContainerColor = SignalWhite
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "REPLACEMENT NFC UID (ISO 14443-4):",
                    color = CarbonGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = inputNewUid,
                    onValueChange = { inputNewUid = it },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("recovery_new_uid_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SovereignViolet,
                        unfocusedBorderColor = CoolHairline,
                        focusedTextColor = SovereignInk,
                        unfocusedTextColor = SovereignInk,
                        focusedContainerColor = SignalWhite,
                        unfocusedContainerColor = SignalWhite
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val words = inputWordsText.trim().split("\\s+".toRegex())
                        if (words.size == 12) {
                            onRecoverWithPhrase(words, inputNewUid, "RECOVERED_BIO_SECRET_2026")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("execute_recovery_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SovereignInk,
                        contentColor = SignalWhite
                    ),
                    shape = RoundedCornerShape(9999.dp)
                ) {
                    Text(
                        text = "VERIFY & RESTORE VAULT",
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                if (recoveryPhrase != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { isRecoveryMode = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(9999.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CoolHairline)
                    ) {
                        Text("BACK TO WORD VIEW", color = SovereignInk, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
