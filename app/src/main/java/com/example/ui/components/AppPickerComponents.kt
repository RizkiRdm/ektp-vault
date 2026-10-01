package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.CarbonGray
import com.example.ui.theme.CoolHairline
import com.example.ui.theme.MistSurface
import com.example.ui.theme.SignalWhite
import com.example.ui.theme.SovereignInk
import com.example.ui.theme.SovereignViolet

data class InstalledAppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable? = null,
    val isPreset: Boolean = false
)

object AppPickerHelper {
    // Curated Indonesian banking, fintech, and popular applications
    val POPULAR_APPS = listOf(
        InstalledAppInfo("BCA Mobile", "com.bca", isPreset = true),
        InstalledAppInfo("Mandiri Livin'", "id.bmri.livin", isPreset = true),
        InstalledAppInfo("BRImo BRI", "id.co.bri.brimo", isPreset = true),
        InstalledAppInfo("BNI Mobile Banking", "src.mobi.bni", isPreset = true),
        InstalledAppInfo("GoPay / Gojek", "com.gojek.app", isPreset = true),
        InstalledAppInfo("OVO Indonesia", "owo.id", isPreset = true),
        InstalledAppInfo("DANA Dompet Digital", "id.dana", isPreset = true),
        InstalledAppInfo("Tokopedia", "com.tokopedia.tkpd", isPreset = true),
        InstalledAppInfo("Shopee Indonesia", "com.shopee.id", isPreset = true),
        InstalledAppInfo("WhatsApp Messenger", "com.whatsapp", isPreset = true),
        InstalledAppInfo("Telegram", "org.telegram.messenger", isPreset = true),
        InstalledAppInfo("GitHub Enterprise", "com.github.android", isPreset = true),
        InstalledAppInfo("Google Chrome", "com.android.chrome", isPreset = true)
    )

    fun getInstalledApps(context: Context): List<InstalledAppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = try {
            pm.queryIntentActivities(intent, 0)
        } catch (_: Exception) {
            emptyList()
        }

        val installed = resolveInfos.mapNotNull { ri ->
            val pkg = ri.activityInfo.packageName
            if (pkg == context.packageName) return@mapNotNull null
            val name = ri.loadLabel(pm).toString()
            val icon = try { ri.loadIcon(pm) } catch (_: Exception) { null }
            InstalledAppInfo(name, pkg, icon, isPreset = false)
        }.distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }

        val existingPackages = installed.map { it.packageName }.toSet()
        val missingPresets = POPULAR_APPS.filter { it.packageName !in existingPackages }

        return installed + missingPresets
    }
}

// Renders the Android app icon or a default fallback
@Composable
fun AppIconView(icon: Drawable?, modifier: Modifier = Modifier) {
    if (icon != null) {
        AndroidView(
            factory = { ctx ->
                ImageView(ctx).apply {
                    setImageDrawable(icon)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }
            },
            update = { it.setImageDrawable(icon) },
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(Color(0xFFEFEAFD), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = null,
                tint = SovereignViolet,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Interactive bottom sheet dialog allowing the user to visually pick an installed app
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerBottomSheet(
    isOpen: Boolean,
    apps: List<InstalledAppInfo>,
    onDismiss: () -> Unit,
    onAppSelected: (InstalledAppInfo) -> Unit
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) {
            apps
        } else {
            apps.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SignalWhite,
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
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "PILIH APLIKASI TARGET",
                        color = SovereignInk,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Pilih aplikasi yang ingin dilindungi dengan Autofill e-KTP",
                        color = CarbonGray,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = CarbonGray)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_search_input"),
                placeholder = { Text("Cari nama aplikasi atau package...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = CarbonGray)
                },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SovereignViolet,
                    unfocusedBorderColor = CoolHairline,
                    focusedTextColor = SovereignInk,
                    unfocusedTextColor = SovereignInk
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // List of applications
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onAppSelected(app)
                                onDismiss()
                            }
                            .background(MistSurface, RoundedCornerShape(10.dp))
                            .border(1.dp, CoolHairline, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIconView(
                            icon = app.icon,
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = app.appName,
                                    color = SovereignInk,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (app.isPreset) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFEDE9FE), RoundedCornerShape(9999.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "POPULAR",
                                            color = SovereignViolet,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = app.packageName,
                                color = CarbonGray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
