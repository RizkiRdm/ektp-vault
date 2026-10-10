package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.domain.model.LockedApp
import com.example.domain.model.TargetType
import com.example.security.AppLockManager
import com.example.ui.components.AppIconView
import com.example.ui.components.AppPickerBottomSheet
import com.example.ui.components.AppPickerHelper
import com.example.ui.screens.applock.AppLockActivity
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CarbonGray
import com.example.ui.theme.CoolHairline
import com.example.ui.theme.GoldenRatio
import com.example.ui.theme.MistSurface
import com.example.ui.theme.SignalWhite
import com.example.ui.theme.SovereignInk
import com.example.ui.theme.SovereignViolet

enum class LockSectionTab {
    APPS,
    DOMAINS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockManagerScreen(
    lockedApps: List<LockedApp>,
    onBack: () -> Unit,
    onLockNewApp: (packageName: String, appName: String, targetType: TargetType, durationMinutes: Int) -> Unit,
    onUpdateLockDuration: (packageName: String, durationMinutes: Int) -> Unit,
    onToggleLockStatus: (packageName: String, isEnabled: Boolean) -> Unit,
    onRemoveLockedApp: (packageName: String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(LockSectionTab.APPS) }
    var showAppPicker by remember { mutableStateOf(false) }
    var showAddDomainDialog by remember { mutableStateOf(false) }
    var domainInput by remember { mutableStateOf("") }
    var domainTimeoutChoice by remember { mutableIntStateOf(1) } // default 1 minute

    var appToDelete by remember { mutableStateOf<LockedApp?>(null) }
    var appToEditDuration by remember { mutableStateOf<LockedApp?>(null) }

    val installedApps = remember {
        AppPickerHelper.getInstalledApps(context)
    }

    val isAccessibilityActive = remember(lockedApps) {
        AppLockManager.isAccessibilityEnabled(context)
    }

    val appItems = remember(lockedApps) {
        lockedApps.filter { it.targetType == TargetType.APP }
    }

    val domainItems = remember(lockedApps) {
        lockedApps.filter { it.targetType == TargetType.WEB }
    }

    fun formatDurationLabel(minutes: Int): String {
        return when (minutes) {
            -1 -> "30 Detik"
            0 -> "Segera saat keluar"
            1 -> "1 Menit"
            5 -> "5 Menit"
            15 -> "15 Menit"
            30 -> "30 Menit"
            else -> if (minutes > 0) "$minutes Menit" else "Segera saat keluar"
        }
    }

    // App Picker Modal
    AppPickerBottomSheet(
        isOpen = showAppPicker,
        apps = installedApps,
        onDismiss = { showAppPicker = false },
        onAppSelected = { selected ->
            showAppPicker = false
            onLockNewApp(selected.packageName, selected.appName, TargetType.APP, 1)
        }
    )

    // Add Domain Dialog
    if (showAddDomainDialog) {
        AlertDialog(
            onDismissRequest = { showAddDomainDialog = false },
            title = {
                Text(
                    text = "KUNCI DOMAIN / SITUS",
                    color = SovereignInk,
                    fontWeight = FontWeight.Bold,
                    fontSize = GoldenRatio.TextBase
                )
            },
            text = {
                Column {
                    Text(
                        text = "Masukkan domain situs web yang ingin dilindungi saat dibuka di browser (Chrome, Samsung Internet, dll.).",
                        color = CarbonGray,
                        fontSize = GoldenRatio.TextMicro
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = domainInput,
                        onValueChange = { domainInput = it },
                        placeholder = { Text("contoh: instagram.com", fontSize = GoldenRatio.TextSmall) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SovereignViolet,
                            unfocusedBorderColor = CoolHairline
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Saran Cepat:",
                        fontSize = GoldenRatio.TextMicro,
                        color = CarbonGray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val suggestions = listOf("instagram.com", "tiktok.com", "facebook.com", "x.com", "reddit.com", "youtube.com")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(suggestions) { item ->
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFEDE9FE), RoundedCornerShape(8.dp))
                                    .clickable { domainInput = item }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(item, fontSize = GoldenRatio.TextMicro, color = SovereignViolet)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Waktu Kunci Ulang Setelah Keluar:",
                        fontSize = GoldenRatio.TextMicro,
                        color = CarbonGray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val timeoutOptions = listOf(0 to "Segera", 1 to "1 Menit", 5 to "5 Menit")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        timeoutOptions.forEach { (mins, label) ->
                            FilterChip(
                                selected = domainTimeoutChoice == mins,
                                onClick = { domainTimeoutChoice = mins },
                                label = { Text(label, fontSize = GoldenRatio.TextMicro) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SovereignInk,
                                    selectedLabelColor = SignalWhite
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = domainInput.trim().lowercase()
                            .removePrefix("https://").removePrefix("http://").removePrefix("www.")
                            .substringBefore("/")
                        if (clean.isNotBlank()) {
                            onLockNewApp(clean, clean, TargetType.WEB, domainTimeoutChoice)
                            showAddDomainDialog = false
                            domainInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SovereignInk),
                    shape = RoundedCornerShape(9999.dp)
                ) {
                    Text("KUNCI SITUS", color = SignalWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAddDomainDialog = false },
                    shape = RoundedCornerShape(9999.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CoolHairline)
                ) {
                    Text("BATAL", color = SovereignInk)
                }
            },
            containerColor = SignalWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Change Relock Timeout Dialog
    if (appToEditDuration != null) {
        val target = appToEditDuration!!
        var selectedMinutes by remember(target.unlockDurationMinutes) {
            mutableIntStateOf(target.unlockDurationMinutes)
        }

        AlertDialog(
            onDismissRequest = { appToEditDuration = null },
            title = {
                Text(
                    text = "ATUR WAKTU KUNCI ULANG",
                    color = SovereignInk,
                    fontWeight = FontWeight.Bold,
                    fontSize = GoldenRatio.TextBase
                )
            },
            text = {
                Column {
                    Text(
                        text = "Pilih kapan '${target.appName}' akan otomatis terkunci kembali setelah Anda keluar dari aplikasi:",
                        color = CarbonGray,
                        fontSize = GoldenRatio.TextSmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val options = listOf(
                        0 to "Segera (Langsung terkunci saat keluar)",
                        -1 to "30 Detik setelah keluar",
                        1 to "1 Menit setelah keluar (Direkomendasikan)",
                        5 to "5 Menit setelah keluar",
                        15 to "15 Menit setelah keluar"
                    )

                    options.forEach { (mins, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMinutes = mins }
                                .padding(vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = selectedMinutes == mins,
                                onClick = { selectedMinutes = mins },
                                colors = RadioButtonDefaults.colors(selectedColor = SovereignViolet)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                fontSize = GoldenRatio.TextSmall,
                                color = SovereignInk
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateLockDuration(target.packageName, selectedMinutes)
                        appToEditDuration = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SovereignInk),
                    shape = RoundedCornerShape(9999.dp)
                ) {
                    Text("SIMPAN", color = SignalWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { appToEditDuration = null },
                    shape = RoundedCornerShape(9999.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CoolHairline)
                ) {
                    Text("BATAL", color = SovereignInk)
                }
            },
            containerColor = SignalWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Delete Confirmation Dialog
    if (appToDelete != null) {
        val target = appToDelete!!
        AlertDialog(
            onDismissRequest = { appToDelete = null },
            title = {
                Text(
                    text = "HAPUS KUNCI",
                    color = AlertCrimson,
                    fontWeight = FontWeight.Bold,
                    fontSize = GoldenRatio.TextBase
                )
            },
            text = {
                Text(
                    text = "Proteksi kunci untuk '${target.appName}' akan dihapus.",
                    color = SovereignInk,
                    fontSize = GoldenRatio.TextSmall
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveLockedApp(target.packageName)
                        appToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertCrimson),
                    shape = RoundedCornerShape(9999.dp)
                ) {
                    Text("HAPUS", color = SignalWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { appToDelete = null },
                    shape = RoundedCornerShape(9999.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CoolHairline)
                ) {
                    Text("BATAL", color = SovereignInk)
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
                    Column {
                        Text(
                            text = "APP & DOMAIN LOCKER",
                            color = SovereignInk,
                            fontSize = GoldenRatio.TextBase,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Kunci aplikasi dan situs web dengan LOKER Shield",
                            color = CarbonGray,
                            fontSize = GoldenRatio.TextMicro
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("app_lock_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SovereignInk
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Accessibility Settings",
                            tint = SovereignInk
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SignalWhite),
                modifier = Modifier.border(0.5.dp, CoolHairline)
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SignalWhite)
                    .border(0.5.dp, CoolHairline)
                    .padding(GoldenRatio.SpaceBase)
            ) {
                if (selectedTab == LockSectionTab.APPS) {
                    Button(
                        onClick = { showAppPicker = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("add_locked_app_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SovereignInk,
                            contentColor = SignalWhite
                        ),
                        shape = RoundedCornerShape(9999.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(GoldenRatio.SpaceSmall))
                        Text(
                            text = "+ KUNCI APLIKASI BARU",
                            fontWeight = FontWeight.Bold,
                            fontSize = GoldenRatio.TextSmall
                        )
                    }
                } else {
                    Button(
                        onClick = { showAddDomainDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("add_locked_domain_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SovereignInk,
                            contentColor = SignalWhite
                        ),
                        shape = RoundedCornerShape(9999.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(GoldenRatio.SpaceSmall))
                        Text(
                            text = "+ KUNCI DOMAIN / SITUS BARU",
                            fontWeight = FontWeight.Bold,
                            fontSize = GoldenRatio.TextSmall
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = GoldenRatio.SpaceLarge, vertical = GoldenRatio.SpaceBase)
        ) {
            // Accessibility Service Warning if disabled
            if (!isAccessibilityActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFEF3C7), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Layanan Aksesibilitas Diperlukan",
                                color = Color(0xFFB45309),
                                fontSize = GoldenRatio.TextSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Aktifkan 'LOKER App Lock' di setelan Aksesibilitas agar sistem dapat memblokir aplikasi dan domain situs terlindungi secara instan saat dibuka.",
                            color = SovereignInk,
                            fontSize = GoldenRatio.TextMicro,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(GoldenRatio.SpaceSmall))
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(9999.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB45309)),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Buka Setelan Aksesibilitas", fontSize = GoldenRatio.TextMicro, color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(GoldenRatio.SpaceBase))
            }

            // Test Lock Shield Button
            Button(
                onClick = {
                    val testTarget = if (selectedTab == LockSectionTab.APPS) {
                        appItems.firstOrNull()?.packageName ?: "com.example.sample"
                    } else {
                        domainItems.firstOrNull()?.packageName ?: "instagram.com"
                    }
                    val testIntent = Intent(context, AppLockActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        putExtra(AppLockActivity.EXTRA_TARGET_PACKAGE, testTarget)
                        if (selectedTab == LockSectionTab.DOMAINS) {
                            putExtra(AppLockActivity.EXTRA_TARGET_DOMAIN, testTarget)
                            putExtra(AppLockActivity.EXTRA_IS_DOMAIN, true)
                        }
                    }
                    context.startActivity(testIntent)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF0EBFD),
                    contentColor = SovereignViolet
                ),
                shape = RoundedCornerShape(9999.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("test_app_lock_button")
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Uji Layar Kunci (App Lock Shield)", fontSize = GoldenRatio.TextMicro, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Bar: Aplikasi vs Domain
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SignalWhite, RoundedCornerShape(12.dp))
                    .border(1.dp, CoolHairline, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (selectedTab == LockSectionTab.APPS) SovereignInk else Color.Transparent,
                            RoundedCornerShape(9.dp)
                        )
                        .clickable { selectedTab = LockSectionTab.APPS }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "APLIKASI (${appItems.size})",
                        color = if (selectedTab == LockSectionTab.APPS) SignalWhite else SovereignInk,
                        fontSize = GoldenRatio.TextMicro,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (selectedTab == LockSectionTab.DOMAINS) SovereignInk else Color.Transparent,
                            RoundedCornerShape(9.dp)
                        )
                        .clickable { selectedTab = LockSectionTab.DOMAINS }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SITUS WEB (${domainItems.size})",
                        color = if (selectedTab == LockSectionTab.DOMAINS) SignalWhite else SovereignInk,
                        fontSize = GoldenRatio.TextMicro,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main List Area
            val activeList = if (selectedTab == LockSectionTab.APPS) appItems else domainItems

            if (activeList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(SignalWhite, RoundedCornerShape(14.dp))
                        .border(1.dp, CoolHairline, RoundedCornerShape(14.dp))
                        .padding(GoldenRatio.SpaceLarge),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFFEDE9FE), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selectedTab == LockSectionTab.APPS) Icons.Default.LockOpen else Icons.Default.Language,
                                contentDescription = null,
                                tint = SovereignViolet,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(GoldenRatio.SpaceBase))
                        Text(
                            text = if (selectedTab == LockSectionTab.APPS) "Belum Ada Aplikasi Dikunci" else "Belum Ada Domain Dikunci",
                            color = SovereignInk,
                            fontSize = GoldenRatio.TextSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (selectedTab == LockSectionTab.APPS)
                                "Tambahkan aplikasi sensitif (e.g. WhatsApp, Galeri, Bank) untuk dilindungi dengan kartu NFC, biometrik, atau password akun."
                            else
                                "Tambahkan alamat domain situs web (e.g. instagram.com, tiktok.com) untuk dikunci secara otomatis saat dibuka melalui peramban web.",
                            color = CarbonGray,
                            fontSize = GoldenRatio.TextMicro,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(activeList, key = { it.packageName }) { app ->
                        val pm = context.packageManager
                        val appIcon = remember(app.packageName, app.targetType) {
                            if (app.targetType == TargetType.APP) {
                                try {
                                    val info = pm.getApplicationInfo(app.packageName, 0)
                                    pm.getApplicationIcon(info)
                                } catch (_: Exception) {
                                    null
                                }
                            } else {
                                null
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SignalWhite, RoundedCornerShape(12.dp))
                                .border(
                                    1.dp,
                                    if (app.isEnabled) SovereignViolet else CoolHairline,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    if (app.targetType == TargetType.WEB) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(Color(0xFFEDE9FE), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = SovereignViolet, modifier = Modifier.size(22.dp))
                                        }
                                    } else {
                                        AppIconView(icon = appIcon, modifier = Modifier.size(40.dp))
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = app.appName,
                                            color = SovereignInk,
                                            fontSize = GoldenRatio.TextSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = app.packageName,
                                            color = CarbonGray,
                                            fontSize = GoldenRatio.TextMicro,
                                            fontFamily = FontFamily.Monospace
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Clickable Relock Timeout Badge
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .background(Color(0xFFF3F4F6), RoundedCornerShape(6.dp))
                                                .clickable { appToEditDuration = app }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = SovereignViolet, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Kunci ulang: ${formatDurationLabel(app.unlockDurationMinutes)}",
                                                color = SovereignInk,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = app.isEnabled,
                                        onCheckedChange = { checked ->
                                            onToggleLockStatus(app.packageName, checked)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = SignalWhite,
                                            checkedTrackColor = SovereignViolet,
                                            uncheckedThumbColor = CarbonGray,
                                            uncheckedTrackColor = MistSurface
                                        ),
                                        modifier = Modifier.testTag("toggle_lock_${app.packageName}")
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))

                                    IconButton(
                                        onClick = { appToDelete = app },
                                        modifier = Modifier.testTag("delete_lock_${app.packageName}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus Kunci",
                                            tint = AlertCrimson,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
