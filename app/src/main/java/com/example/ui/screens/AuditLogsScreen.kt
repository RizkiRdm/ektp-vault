package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AuditLog
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

enum class AuditFilter {
    ALL,
    BIOMETRIC_ONLY,
    SUCCESS_ONLY,
    FAILED_ONLY
}

// Local read-only audit log screen displaying Room-persisted vault and biometric interactions
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogsScreen(
    auditLogs: List<AuditLog>,
    onBack: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf(AuditFilter.ALL) }

    val filteredLogs = remember(auditLogs, selectedFilter) {
        when (selectedFilter) {
            AuditFilter.ALL -> auditLogs
            AuditFilter.BIOMETRIC_ONLY -> auditLogs.filter { it.eventType.contains("BIOMETRIC", ignoreCase = true) }
            AuditFilter.SUCCESS_ONLY -> auditLogs.filter { it.status.equals("SUCCESS", ignoreCase = true) }
            AuditFilter.FAILED_ONLY -> auditLogs.filter { !it.status.equals("SUCCESS", ignoreCase = true) }
        }
    }

    val totalCount = auditLogs.size
    val biometricCount = auditLogs.count { it.eventType.contains("BIOMETRIC", ignoreCase = true) }
    val successCount = auditLogs.count { it.status.equals("SUCCESS", ignoreCase = true) }
    val failureCount = totalCount - successCount

    Scaffold(
        containerColor = MistSurface,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SECURITY AUDIT LOGS",
                            color = SovereignInk,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "LOCAL READ-ONLY AUDIT TRAIL (ROOM)",
                            color = SovereignViolet,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.8.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("audit_back_button")) {
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Read-Only Security Policy Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF3F0FA), RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFFD6C8F6), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = SovereignViolet,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "IMMUTABLE AUDIT TRAIL • ROOM DATABASE",
                            color = SovereignViolet,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = "Catatan interaksi biometrik dan akses brankas bersifat append-only dan tidak dapat dimodifikasi.",
                            color = SovereignInk,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metric Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AuditStatCard(
                    label = "TOTAL",
                    value = totalCount.toString(),
                    color = SovereignInk,
                    modifier = Modifier.weight(1f)
                )
                AuditStatCard(
                    label = "BIOMETRIK",
                    value = biometricCount.toString(),
                    color = SovereignViolet,
                    modifier = Modifier.weight(1f)
                )
                AuditStatCard(
                    label = "BERHASIL",
                    value = successCount.toString(),
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                AuditStatCard(
                    label = "GAGAL",
                    value = failureCount.toString(),
                    color = AlertCrimson,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    AuditFilterChip(
                        label = "SEMUA ($totalCount)",
                        isSelected = selectedFilter == AuditFilter.ALL,
                        onClick = { selectedFilter = AuditFilter.ALL }
                    )
                }
                item {
                    AuditFilterChip(
                        label = "BIOMETRIK ($biometricCount)",
                        isSelected = selectedFilter == AuditFilter.BIOMETRIC_ONLY,
                        onClick = { selectedFilter = AuditFilter.BIOMETRIC_ONLY }
                    )
                }
                item {
                    AuditFilterChip(
                        label = "BERHASIL ($successCount)",
                        isSelected = selectedFilter == AuditFilter.SUCCESS_ONLY,
                        onClick = { selectedFilter = AuditFilter.SUCCESS_ONLY }
                    )
                }
                item {
                    AuditFilterChip(
                        label = "GAGAL / BLOCKED ($failureCount)",
                        isSelected = selectedFilter == AuditFilter.FAILED_ONLY,
                        onClick = { selectedFilter = AuditFilter.FAILED_ONLY }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Audit Records List
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = "TIDAK ADA LOG AUDIT UNTUK FILTER INI",
                        color = CarbonGray,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        AuditLogItemCard(log)
                    }
                }
            }
        }
    }
}

// Summary statistic indicator card
@Composable
private fun AuditStatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(SignalWhite, RoundedCornerShape(10.dp))
            .border(1.dp, CoolHairline, RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                color = color,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = label,
                color = CarbonGray,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// Filter selector chip
@Composable
private fun AuditFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                if (isSelected) SovereignInk else SignalWhite,
                RoundedCornerShape(9999.dp)
            )
            .border(
                1.dp,
                if (isSelected) SovereignInk else CoolHairline,
                RoundedCornerShape(9999.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) SignalWhite else SovereignInk,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.4.sp
        )
    }
}

// Detailed read-only audit log card
@Composable
private fun AuditLogItemCard(log: AuditLog) {
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
    val formattedTime = remember(log.timestamp) { sdf.format(Date(log.timestamp)) }
    val isSuccess = log.status.equals("SUCCESS", ignoreCase = true)
    val isBiometric = log.eventType.contains("BIOMETRIC", ignoreCase = true)

    val icon: ImageVector = when {
        isBiometric -> Icons.Default.Fingerprint
        log.eventType.contains("NFC", ignoreCase = true) -> Icons.Default.Nfc
        log.eventType.contains("DECRYPT", ignoreCase = true) -> Icons.Default.Key
        log.eventType.contains("LOCKOUT", ignoreCase = true) -> Icons.Default.Warning
        else -> Icons.Default.Lock
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(10.dp), ambientColor = Color(0x10000000))
            .background(SignalWhite, RoundedCornerShape(10.dp))
            .border(
                1.dp,
                if (!isSuccess) Color(0xFFFCA5A5) else CoolHairline,
                RoundedCornerShape(10.dp)
            )
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                if (isBiometric) Color(0xFFEDE9FE) else Color(0xFFF3F4F6),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isBiometric) SovereignViolet else SovereignInk,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = log.eventType,
                        color = SovereignInk,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            if (isSuccess) Color(0xFFD1FAE5) else Color(0xFFFEE2E2),
                            RoundedCornerShape(9999.dp)
                        )
                        .border(
                            0.5.dp,
                            if (isSuccess) Color(0xFF10B981) else AlertCrimson,
                            RoundedCornerShape(9999.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = log.status.uppercase(),
                        color = if (isSuccess) Color(0xFF065F46) else AlertCrimson,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Target service and details
            Text(
                text = "Target: ${log.targetService}",
                color = CarbonGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = log.details,
                color = if (!isSuccess) AlertCrimson else SovereignInk,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = formattedTime,
                color = CarbonGray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
