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
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CarbonGray
import com.example.ui.theme.CoolHairline
import com.example.ui.theme.MistSurface
import com.example.ui.theme.SignalWhite
import com.example.ui.theme.SovereignInk
import com.example.ui.theme.SovereignViolet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportImportScreen(
    exportedLokerContent: String?,
    onBack: () -> Unit,
    onExport: (password: String) -> Unit,
    onImport: (content: String, password: String, currentUid: String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    var exportPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var exportError by remember { mutableStateOf<String?>(null) }
    var copiedExportNote by remember { mutableStateOf<String?>(null) }

    val defaultFilename = remember {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        "backup_${sdf.format(Date())}.loker"
    }

    var importContent by remember { mutableStateOf("") }
    var importPassword by remember { mutableStateOf("") }
    var importUid by remember { mutableStateOf("04:A2:3B:5F:7E:89") }
    var importError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MistSurface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectedTab == 0) "EXPORT .LOKER VAULT" else "IMPORT .LOKER VAULT",
                        color = SovereignInk,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("export_back_button")) {
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
            // Tabs in 9999px pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    shape = RoundedCornerShape(9999.dp),
                    label = { Text("EXPORT .LOKER", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SovereignInk,
                        selectedLabelColor = SignalWhite,
                        containerColor = SignalWhite,
                        labelColor = SovereignInk
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedTab == 0,
                        borderColor = if (selectedTab == 0) SovereignInk else CoolHairline
                    )
                )
                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = RoundedCornerShape(9999.dp),
                    label = { Text("IMPORT .LOKER", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SovereignInk,
                        selectedLabelColor = SignalWhite,
                        containerColor = SignalWhite,
                        labelColor = SovereignInk
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedTab == 1,
                        borderColor = if (selectedTab == 1) SovereignInk else CoolHairline
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // Export Tab
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
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = SovereignViolet,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CRYPTOGRAPHIC BACKUP METADATA",
                                color = SovereignViolet,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Target: $defaultFilename\n• Payload: AES-256-GCM Envelope (PBKDF2 Hardware Salt)\n• Extension: .loker (application/x-loker)",
                            color = SovereignInk,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "BACKUP PASSPHRASE:",
                    color = SovereignInk,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = exportPassword,
                    onValueChange = {
                        exportPassword = it
                        exportError = null
                    },
                    shape = RoundedCornerShape(10.dp),
                    placeholder = { Text("Enter backup password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("export_password_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SovereignViolet,
                        unfocusedBorderColor = CoolHairline,
                        focusedTextColor = SovereignInk,
                        unfocusedTextColor = SovereignInk,
                        focusedContainerColor = SignalWhite,
                        unfocusedContainerColor = SignalWhite
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "CONFIRM PASSPHRASE:",
                    color = SovereignInk,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        exportError = null
                    },
                    shape = RoundedCornerShape(10.dp),
                    placeholder = { Text("Re-enter backup password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("confirm_export_password_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SovereignViolet,
                        unfocusedBorderColor = CoolHairline,
                        focusedTextColor = SovereignInk,
                        unfocusedTextColor = SovereignInk,
                        focusedContainerColor = SignalWhite,
                        unfocusedContainerColor = SignalWhite
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF0EBFD), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = SovereignViolet,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "This file can only be decrypted and opened with the registered hardware e-KTP card.",
                            color = SovereignInk,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                if (exportError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• $exportError",
                        color = AlertCrimson,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (exportPassword.isBlank() || exportPassword.length < 6) {
                            exportError = "Password minimal 6 karakter."
                            return@Button
                        }
                        if (exportPassword != confirmPassword) {
                            exportError = "Konfirmasi password tidak cocok."
                            return@Button
                        }
                        onExport(exportPassword)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("generate_loker_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SovereignInk,
                        contentColor = SignalWhite
                    ),
                    shape = RoundedCornerShape(9999.dp)
                ) {
                    Text(
                        text = "GENERATE .LOKER ENVELOPE",
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                if (exportedLokerContent != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "ENCRYPTED .LOKER PAYLOAD (AES-256-GCM):",
                        color = SovereignViolet,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(SignalWhite, RoundedCornerShape(10.dp))
                            .border(1.dp, CoolHairline, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = exportedLokerContent,
                            color = CarbonGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(exportedLokerContent))
                            copiedExportNote = "Isi file .loker berhasil disalin!"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SovereignInk),
                        shape = RoundedCornerShape(9999.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("COPY .LOKER CONTENT", fontSize = 11.sp, color = SignalWhite)
                    }
                    if (copiedExportNote != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "✓ $copiedExportNote", color = SovereignViolet, fontSize = 11.sp)
                    }
                }
            } else {
                // Import Tab
                Text(
                    text = "PASTE .LOKER ENVELOPE CONTENT:",
                    color = SovereignInk,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = importContent,
                    onValueChange = {
                        importContent = it
                        importError = null
                    },
                    shape = RoundedCornerShape(10.dp),
                    placeholder = { Text("Paste payload envelope JSON here") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .testTag("import_content_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SovereignViolet,
                        unfocusedBorderColor = CoolHairline,
                        focusedTextColor = SovereignInk,
                        unfocusedTextColor = SovereignInk,
                        focusedContainerColor = SignalWhite,
                        unfocusedContainerColor = SignalWhite
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "PASSPHRASE:",
                    color = SovereignInk,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = importPassword,
                    onValueChange = {
                        importPassword = it
                        importError = null
                    },
                    shape = RoundedCornerShape(10.dp),
                    placeholder = { Text("Backup password used during export") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("import_password_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SovereignViolet,
                        unfocusedBorderColor = CoolHairline,
                        focusedTextColor = SovereignInk,
                        unfocusedTextColor = SovereignInk,
                        focusedContainerColor = SignalWhite,
                        unfocusedContainerColor = SignalWhite
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "RECIPIENT e-KTP UID (ISO 14443-4):",
                    color = CarbonGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = importUid,
                    onValueChange = { importUid = it },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("import_uid_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SovereignViolet,
                        unfocusedBorderColor = CoolHairline,
                        focusedTextColor = SovereignInk,
                        unfocusedTextColor = SovereignInk,
                        focusedContainerColor = SignalWhite,
                        unfocusedContainerColor = SignalWhite
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                    singleLine = true
                )

                if (importError != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "• $importError",
                        color = AlertCrimson,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (importContent.isBlank()) {
                            importError = "Konten .loker wajib diisi."
                            return@Button
                        }
                        if (importPassword.isBlank()) {
                            importError = "Password wajib diisi."
                            return@Button
                        }
                        onImport(importContent, importPassword, importUid)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("execute_import_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SovereignInk,
                        contentColor = SignalWhite
                    ),
                    shape = RoundedCornerShape(9999.dp)
                ) {
                    Icon(imageVector = Icons.Default.FileUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DECRYPT & RESTORE VAULT",
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
