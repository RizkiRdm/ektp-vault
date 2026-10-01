package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Credential
import com.example.domain.model.SecurityCategory
import com.example.domain.model.TargetType
import com.example.ui.components.AppIconView
import com.example.ui.components.AppPickerBottomSheet
import com.example.ui.components.AppPickerHelper
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CarbonGray
import com.example.ui.theme.CoolHairline
import com.example.ui.theme.MistSurface
import com.example.ui.theme.SignalWhite
import com.example.ui.theme.SovereignInk
import com.example.ui.theme.SovereignViolet
import java.security.SecureRandom

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCredentialScreen(
    existingCredential: Credential?,
    onBack: () -> Unit,
    onSave: (
        id: String?,
        serviceName: String,
        username: String,
        passwordPlain: String,
        targetType: TargetType,
        targetId: String,
        securityCategory: SecurityCategory,
        notes: String
    ) -> Unit
) {
    var serviceName by remember { mutableStateOf(existingCredential?.serviceName ?: "") }
    var username by remember { mutableStateOf(existingCredential?.username ?: "") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var targetType by remember { mutableStateOf(existingCredential?.targetType ?: TargetType.APP) }
    var targetId by remember { mutableStateOf(existingCredential?.targetId ?: "") }
    var isHighRisk by remember {
        mutableStateOf(existingCredential?.securityCategory == SecurityCategory.HIGH_RISK)
    }
    var notes by remember { mutableStateOf(existingCredential?.notes ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val installedApps = remember { AppPickerHelper.getInstalledApps(context) }
    var isAppPickerOpen by remember { mutableStateOf(false) }
    var showManualPackageInput by remember { mutableStateOf(false) }

    val isEditing = existingCredential != null

    val presets = listOf(
        Triple("BCA Mobile", TargetType.APP, "com.bca"),
        Triple("Mandiri Livin", TargetType.APP, "id.bmri.livin"),
        Triple("GoPay / Gojek", TargetType.APP, "com.gojek.app"),
        Triple("NEVERHACK Console", TargetType.WEB, "console.neverhack.com"),
        Triple("GitHub Enterprise", TargetType.WEB, "github.com"),
        Triple("Google Workspace", TargetType.WEB, "accounts.google.com")
    )

    fun generateStrongPassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+"
        val random = SecureRandom()
        return (1..20).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }

    Scaffold(
        containerColor = MistSurface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "EDIT CREDENTIAL" else "ADD NEW CREDENTIAL",
                        color = SovereignInk,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
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
            // Quick presets
            if (!isEditing) {
                Text(
                    text = "ENTERPRISE & BANKING TEMPLATES",
                    color = SovereignViolet,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presets) { preset ->
                        Box(
                            modifier = Modifier
                                .background(SignalWhite, RoundedCornerShape(9999.dp))
                                .border(1.dp, CoolHairline, RoundedCornerShape(9999.dp))
                                .clickable {
                                    serviceName = preset.first
                                    targetType = preset.second
                                    targetId = preset.third
                                    if (preset.third.contains("bca") || preset.third.contains("livin") || preset.third.contains("gojek") || preset.third.contains("neverhack")) {
                                        isHighRisk = true
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = preset.first,
                                color = SovereignInk,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Service Name
            Text(
                text = "SERVICE NAME *",
                color = SovereignInk,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = serviceName,
                onValueChange = {
                    serviceName = it
                    validationError = null
                },
                placeholder = { Text("Contoh: Bank Central Asia") },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("service_name_input"),
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

            // Target Type (APP or WEB)
            Text(
                text = "TARGET TYPE & IDENTIFIER",
                color = SovereignInk,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = targetType == TargetType.APP,
                    onClick = { targetType = TargetType.APP },
                    shape = RoundedCornerShape(9999.dp),
                    label = { Text("APP (PACKAGE NAME)", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SovereignInk,
                        selectedLabelColor = SignalWhite,
                        containerColor = SignalWhite,
                        labelColor = SovereignInk
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = targetType == TargetType.APP,
                        borderColor = if (targetType == TargetType.APP) SovereignInk else CoolHairline
                    )
                )
                FilterChip(
                    selected = targetType == TargetType.WEB,
                    onClick = { targetType = TargetType.WEB },
                    shape = RoundedCornerShape(9999.dp),
                    label = { Text("WEB (DOMAIN)", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SovereignInk,
                        selectedLabelColor = SignalWhite,
                        containerColor = SignalWhite,
                        labelColor = SovereignInk
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = targetType == TargetType.WEB,
                        borderColor = if (targetType == TargetType.WEB) SovereignInk else CoolHairline
                    )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            AnimatedVisibility(
                visible = targetType == TargetType.APP,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val selectedApp = installedApps.find { it.packageName == targetId }
                Column {
                    if (targetId.isNotBlank()) {
                        // Visual selected app preview card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SignalWhite, RoundedCornerShape(12.dp))
                                .border(1.5.dp, SovereignViolet, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    AppIconView(icon = selectedApp?.icon, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = selectedApp?.appName ?: serviceName.ifBlank { "Aplikasi Terpilih" },
                                            color = SovereignInk,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = targetId,
                                            color = CarbonGray,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                OutlinedButton(
                                    onClick = { isAppPickerOpen = true },
                                    shape = RoundedCornerShape(9999.dp),
                                    modifier = Modifier.testTag("change_app_button")
                                ) {
                                    Text("GANTI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        // Prominent Visual Button to Pick Installed App
                        Button(
                            onClick = { isAppPickerOpen = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("pick_installed_app_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SovereignInk,
                                contentColor = SignalWhite
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Apps,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "PILIH DARI APLIKASI TERINSTAL",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "Lihat nama & ikon aplikasi tanpa mengetik package name",
                                        fontSize = 10.sp,
                                        color = SignalWhite.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Popular App Presets
                    Text(
                        text = "APLIKASI POPULER CEPAT:",
                        color = CarbonGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(AppPickerHelper.POPULAR_APPS.take(6)) { preset ->
                            val isChosen = targetId == preset.packageName
                            Box(
                                modifier = Modifier
                                    .clickable {
                                        targetId = preset.packageName
                                        if (serviceName.isBlank()) serviceName = preset.appName
                                    }
                                    .background(
                                        if (isChosen) Color(0xFFEDE9FE) else SignalWhite,
                                        RoundedCornerShape(9999.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isChosen) SovereignViolet else CoolHairline,
                                        RoundedCornerShape(9999.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = preset.appName,
                                    color = if (isChosen) SovereignViolet else SovereignInk,
                                    fontSize = 11.sp,
                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Manual package input toggle
                    Row(
                        modifier = Modifier
                            .clickable { showManualPackageInput = !showManualPackageInput }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showManualPackageInput) "▼ Sembunyikan Input Manual Package" else "▶ Atau Masukkan Package Name Manual",
                            color = SovereignViolet,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (showManualPackageInput) {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = targetId,
                            onValueChange = { targetId = it },
                            shape = RoundedCornerShape(10.dp),
                            placeholder = { Text("com.example.app") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("target_id_input"),
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
                    }
                }
            }

            AnimatedVisibility(
                visible = targetType == TargetType.WEB,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    OutlinedTextField(
                        value = targetId,
                        onValueChange = { targetId = it },
                        shape = RoundedCornerShape(10.dp),
                        placeholder = { Text("example.com / console.neverhack.com") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("target_id_input"),
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
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Username
            Text(
                text = "USERNAME / OPERATOR IDENTIFIER *",
                color = SovereignInk,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    validationError = null
                },
                placeholder = { Text("operator@neverhack.com / fufufafa_99") },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("username_input"),
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

            // Password
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "PASSWORD (LEAVE BLANK TO KEEP)" else "PASSWORD *",
                    color = SovereignInk,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp
                )
                IconButton(
                    onClick = { password = generateStrongPassword() },
                    modifier = Modifier.size(28.dp).testTag("generate_password_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = "Generate Password",
                        tint = SovereignViolet,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    validationError = null
                },
                shape = RoundedCornerShape(10.dp),
                placeholder = { Text(if (isEditing) "••••••••••••" else "Enter password or generate") },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (isPasswordVisible) "Sembunyikan" else "Tampilkan",
                            tint = CarbonGray
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("password_input"),
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

            // Notes
            Text(
                text = "NOTES (OPTIONAL)",
                color = CarbonGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                shape = RoundedCornerShape(10.dp),
                placeholder = { Text("Security notes or access parameters") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notes_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SovereignViolet,
                    unfocusedBorderColor = CoolHairline,
                    focusedTextColor = SovereignInk,
                    unfocusedTextColor = SovereignInk,
                    focusedContainerColor = SignalWhite,
                    unfocusedContainerColor = SignalWhite
                ),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(18.dp))

            // High-Risk Account Toggle Box (NEVERHACK Alert styling)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isHighRisk) Color(0xFFFDE8E8) else SignalWhite,
                        RoundedCornerShape(14.dp)
                    )
                    .border(
                        1.dp,
                        if (isHighRisk) Color(0xFFFCA5A5) else CoolHairline,
                        RoundedCornerShape(14.dp)
                    )
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isHighRisk,
                            onCheckedChange = { isHighRisk = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = AlertCrimson,
                                uncheckedColor = CarbonGray,
                                checkmarkColor = SignalWhite
                            ),
                            modifier = Modifier.testTag("high_risk_checkbox")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CRITICAL / HIGH-RISK (SINGLE-USE WIPE)",
                            color = if (isHighRisk) AlertCrimson else SovereignInk,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Row(
                        modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isHighRisk) AlertCrimson else CarbonGray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "High-risk accounts are subjected to Plan A: instantaneous RAM key scrub immediately upon injection or reveal timer expiration.",
                            color = if (isHighRisk) AlertCrimson else CarbonGray,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            if (validationError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "• $validationError",
                    color = AlertCrimson,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button (NEVERHACK Sovereign Ink 9999px pill)
            Button(
                onClick = {
                    if (serviceName.isBlank()) {
                        validationError = "Nama service tidak boleh kosong."
                        return@Button
                    }
                    if (username.isBlank()) {
                        validationError = "Username tidak boleh kosong."
                        return@Button
                    }
                    if (!isEditing && password.isBlank()) {
                        validationError = "Password wajib diisi untuk kredensial baru."
                        return@Button
                    }

                    onSave(
                        existingCredential?.id,
                        serviceName,
                        username,
                        password,
                        targetType,
                        targetId,
                        if (isHighRisk) SecurityCategory.HIGH_RISK else SecurityCategory.STANDARD,
                        notes
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_credential_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SovereignInk,
                    contentColor = SignalWhite
                ),
                shape = RoundedCornerShape(9999.dp)
            ) {
                Text(
                    text = if (isEditing) "UPDATE CREDENTIAL" else "ENCRYPT & STORE IN VAULT",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        // Visual installed application picker bottom sheet
        AppPickerBottomSheet(
            isOpen = isAppPickerOpen,
            apps = installedApps,
            onDismiss = { isAppPickerOpen = false },
            onAppSelected = { app ->
                targetId = app.packageName
                if (serviceName.isBlank()) {
                    serviceName = app.appName
                }
            }
        )
    }
}
