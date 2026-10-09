package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.LockedApp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockManagerScreen(
    lockedApps: List<LockedApp>,
    onBack: () -> Unit,
    onLockNewApp: (packageName: String, appName: String) -> Unit,
    onToggleLockStatus: (packageName: String, isEnabled: Boolean) -> Unit,
    onRemoveLockedApp: (packageName: String) -> Unit
) {
    val context = LocalContext.current
    var showAppPicker by remember { mutableStateOf(false) }
    var appToDelete by remember { mutableStateOf<LockedApp?>(null) }
    val installedApps = remember {
        AppPickerHelper.getInstalledApps(context)
    }

    val isAccessibilityActive = remember(lockedApps) {
        AppLockManager.isAccessibilityEnabled(context)
    }

    AppPickerBottomSheet(
        isOpen = showAppPicker,
        apps = installedApps,
        onDismiss = { showAppPicker = false },
        onAppSelected = { selected ->
            showAppPicker = false
            onLockNewApp(selected.packageName, selected.appName)
        }
    )

    if (appToDelete != null) {
        val target = appToDelete!!
        AlertDialog(
            onDismissRequest = { appToDelete = null },
            title = {
                Text(
                    text = "REMOVE APP LOCK",
                    color = AlertCrimson,
                    fontWeight = FontWeight.Bold,
                    fontSize = GoldenRatio.TextBase
                )
            },
            text = {
                Text(
                    text = "App '${target.appName}' will no longer be protected by your security key. Anyone will be able to launch it freely.",
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
                    Text("REMOVE LOCK", color = SignalWhite, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { appToDelete = null },
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
                    Column {
                        Text(
                            text = "APPLICATION LOCK",
                            color = SovereignInk,
                            fontSize = GoldenRatio.TextBase,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Lock sensitive apps with physical security key",
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
                        text = "+ LOCK ANOTHER APPLICATION",
                        fontWeight = FontWeight.Medium,
                        fontSize = GoldenRatio.TextSmall,
                        letterSpacing = 0.5.sp
                    )
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
            // Explanation Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SignalWhite, RoundedCornerShape(12.dp))
                    .border(1.dp, CoolHairline, RoundedCornerShape(12.dp))
                    .padding(GoldenRatio.SpaceBase)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFFEDE9FE), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = SovereignViolet, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Protected Applications",
                                color = SovereignInk,
                                fontSize = GoldenRatio.TextSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Locked apps below can only be opened by tapping your registered hardware key and biometric verification.",
                                color = CarbonGray,
                                fontSize = GoldenRatio.TextMicro,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Test Lock Button
                    Button(
                        onClick = {
                            val testTarget = lockedApps.firstOrNull()?.packageName ?: "com.example.sample"
                            val testIntent = Intent(context, AppLockActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                putExtra(AppLockActivity.EXTRA_TARGET_PACKAGE, testTarget)
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
                            .height(36.dp)
                            .testTag("test_app_lock_button")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test App Lock Shield Now", fontSize = GoldenRatio.TextMicro, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(GoldenRatio.SpaceBase))

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
                                text = "Accessibility Service Required",
                                color = Color(0xFFB45309),
                                fontSize = GoldenRatio.TextSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Enable 'LOKER App Lock' in Android Accessibility settings so the system can intercept locked applications immediately on launch.",
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
                            Text("Open Accessibility Settings", fontSize = GoldenRatio.TextMicro, color = Color(0xFFB45309), fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(GoldenRatio.SpaceBase))
            }

            // Locked Apps List
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LOCKED APPLICATIONS (${lockedApps.size})",
                    color = SovereignViolet,
                    fontSize = GoldenRatio.TextMicro,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(GoldenRatio.SpaceSmall))

            if (lockedApps.isEmpty()) {
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
                            Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, tint = SovereignViolet, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.height(GoldenRatio.SpaceBase))
                        Text(
                            text = "No Applications Locked",
                            color = SovereignInk,
                            fontSize = GoldenRatio.TextSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select sensitive applications (such as banking, messaging, or gallery) to safeguard them with your hardware security key.",
                            color = CarbonGray,
                            fontSize = GoldenRatio.TextMicro,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(lockedApps, key = { it.packageName }) { app ->
                        val pm = context.packageManager
                        val appIcon = remember(app.packageName) {
                            try {
                                val info = pm.getApplicationInfo(app.packageName, 0)
                                pm.getApplicationIcon(info)
                            } catch (_: Exception) {
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
                                .padding(GoldenRatio.SpaceBase)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    AppIconView(icon = appIcon, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.width(GoldenRatio.SpaceBase))
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
                                        Text(
                                            text = if (app.isEnabled) "Status: Protected (Hardware Key)" else "Status: Lock Disabled",
                                            color = if (app.isEnabled) Color(0xFF2E7D32) else CarbonGray,
                                            fontSize = GoldenRatio.TextMicro,
                                            fontWeight = FontWeight.Medium
                                        )
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

                                    Spacer(modifier = Modifier.width(6.dp))

                                    IconButton(
                                        onClick = { appToDelete = app },
                                        modifier = Modifier.testTag("delete_lock_${app.packageName}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Lock",
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
