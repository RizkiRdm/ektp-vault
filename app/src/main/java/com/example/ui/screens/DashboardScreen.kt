package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
import com.example.ui.AuthSheetAction
import com.example.ui.ScreenState
import com.example.ui.VaultUiState
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CarbonGray
import com.example.ui.theme.CoolHairline
import com.example.ui.theme.MistSurface
import com.example.ui.theme.SignalWhite
import com.example.ui.theme.SovereignInk
import com.example.ui.theme.SovereignViolet
import com.example.ui.theme.VioletWash
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: VaultUiState,
    credentials: List<Credential>,
    onSearchChange: (String) -> Unit,
    onCategoryFilterChange: (SecurityCategory?) -> Unit,
    onNavigate: (ScreenState) -> Unit,
    onOpenAuthSheet: (AuthSheetAction, String?, Boolean) -> Unit,
    onLockVault: () -> Unit,
    onUpdateRiskCategory: (String, SecurityCategory) -> Unit,
    onDeleteCredential: (String) -> Unit,
    onClearStatusMessage: () -> Unit
) {
    var showSearchBar by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearStatusMessage()
        }
    }

    Scaffold(
        containerColor = MistSurface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(SovereignInk, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "N",
                                color = SignalWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "NEVERHACK",
                                    color = SovereignInk,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "KTP-VAULT",
                                    color = SovereignViolet,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "SOVEREIGN CYBERSECURITY & HARDWARE TEE",
                                color = CarbonGray,
                                fontSize = 9.sp,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSearchBar = !showSearchBar },
                        modifier = Modifier.testTag("search_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Cari Kredensial",
                            tint = if (showSearchBar) SovereignViolet else SovereignInk
                        )
                    }

                    IconButton(
                        onClick = { onLockVault() },
                        modifier = Modifier.testTag("lock_vault_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = "Kunci Vault",
                            tint = if (uiState.isUnlocked) SovereignViolet else AlertCrimson
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showOptionsMenu = true },
                            modifier = Modifier.testTag("settings_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Pengaturan",
                                tint = SovereignInk
                            )
                        }

                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false },
                            modifier = Modifier.background(SignalWhite)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "MANAGE PHYSICAL KEYS (BYOK)",
                                        color = SovereignInk,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    onNavigate(ScreenState.PhysicalKeys)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "EXPORT / IMPORT .LOKER",
                                        color = SovereignInk,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    onNavigate(ScreenState.ExportImport)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "RECOVERY PHRASE (12 WORDS)",
                                        color = SovereignInk,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    onNavigate(ScreenState.RecoveryPhrase)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "SECURITY AUDIT LOGS",
                                        color = SovereignViolet,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    onNavigate(ScreenState.AuditLogs)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SignalWhite
                ),
                modifier = Modifier.border(width = 0.5.dp, color = CoolHairline)
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SignalWhite)
                    .border(width = 0.5.dp, color = CoolHairline)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { onNavigate(ScreenState.AddEdit(null)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("add_credential_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SovereignInk,
                        contentColor = SignalWhite
                    ),
                    shape = RoundedCornerShape(9999.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+ ADD NEW CREDENTIAL",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // NEVERHACK Ambient Eyebrow Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "HARDWARE-ANCHORED CREDENTIAL TERMINAL",
                    color = SovereignViolet,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.2.sp
                )
                Box(
                    modifier = Modifier
                        .background(Color(0xFFEFEAFD), RoundedCornerShape(9999.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "ISO 14443-4",
                        color = SovereignViolet,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Active Hardware Key Card (NEVERHACK Capability Card style)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(14.dp), ambientColor = Color(0x1A281E5D))
                    .background(SignalWhite, RoundedCornerShape(14.dp))
                    .border(1.dp, CoolHairline, RoundedCornerShape(14.dp))
                    .padding(16.dp)
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
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    if (uiState.isUnlocked) SovereignViolet else AlertCrimson,
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ACTIVE KEY: ${uiState.activeCardLabel}",
                                color = SovereignInk,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (uiState.isUnlocked) "Android Keystore TEE Active • Hardware Module Isolated" else "Vault Locked • Tap e-KTP to Decrypt",
                                color = if (uiState.isUnlocked) SovereignViolet else AlertCrimson,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (!uiState.isUnlocked) {
                        Button(
                            onClick = {
                                onOpenAuthSheet(AuthSheetAction.UNLOCK_VAULT, null, false)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SovereignInk,
                                contentColor = SignalWhite
                            ),
                            shape = RoundedCornerShape(9999.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "UNLOCK",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search input if toggled
            if (showSearchBar) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input"),
                    shape = RoundedCornerShape(9999.dp),
                    placeholder = {
                        Text(
                            "Search credentials, usernames, notes...",
                            fontSize = 13.sp,
                            color = CarbonGray
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = SovereignViolet
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Hapus",
                                    tint = CarbonGray
                                )
                            }
                        }
                    },
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
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Filter Chips (ALL, HIGH-RISK, STANDARD) in 9999px pill shapes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.categoryFilter == null,
                    onClick = { onCategoryFilterChange(null) },
                    shape = RoundedCornerShape(9999.dp),
                    label = {
                        Text(
                            "ALL (${credentials.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SovereignInk,
                        selectedLabelColor = SignalWhite,
                        containerColor = SignalWhite,
                        labelColor = SovereignInk
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = uiState.categoryFilter == null,
                        borderColor = if (uiState.categoryFilter == null) SovereignInk else CoolHairline
                    )
                )

                FilterChip(
                    selected = uiState.categoryFilter == SecurityCategory.HIGH_RISK,
                    onClick = {
                        onCategoryFilterChange(
                            if (uiState.categoryFilter == SecurityCategory.HIGH_RISK) null
                            else SecurityCategory.HIGH_RISK
                        )
                    },
                    shape = RoundedCornerShape(9999.dp),
                    label = {
                        Text(
                            "CRITICAL / HIGH-RISK",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AlertCrimson,
                        selectedLabelColor = SignalWhite,
                        containerColor = SignalWhite,
                        labelColor = AlertCrimson
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = uiState.categoryFilter == SecurityCategory.HIGH_RISK,
                        borderColor = AlertCrimson
                    )
                )

                FilterChip(
                    selected = uiState.categoryFilter == SecurityCategory.STANDARD,
                    onClick = {
                        onCategoryFilterChange(
                            if (uiState.categoryFilter == SecurityCategory.STANDARD) null
                            else SecurityCategory.STANDARD
                        )
                    },
                    shape = RoundedCornerShape(9999.dp),
                    label = {
                        Text(
                            "STANDARD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SovereignViolet,
                        selectedLabelColor = SignalWhite,
                        containerColor = SignalWhite,
                        labelColor = SovereignViolet
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = uiState.categoryFilter == SecurityCategory.STANDARD,
                        borderColor = SovereignViolet
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Credentials List
            if (credentials.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .background(Color(0xFFF0EBFD), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = SovereignViolet,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "NO CREDENTIALS MATCHED",
                            color = SovereignInk,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Press button below to add a new sovereign record",
                            color = CarbonGray,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(credentials, key = { it.id }) { cred ->
                        CredentialCard(
                            credential = cred,
                            onCardClick = {
                                onNavigate(ScreenState.Detail(cred.id))
                            },
                            onRevealClick = {
                                onOpenAuthSheet(
                                    AuthSheetAction.REVEAL_PASSWORD,
                                    cred.id,
                                    cred.securityCategory == SecurityCategory.HIGH_RISK
                                )
                            },
                            onEditClick = {
                                onNavigate(ScreenState.AddEdit(cred.id))
                            },
                            onToggleCategory = {
                                val newCat = if (cred.securityCategory == SecurityCategory.HIGH_RISK)
                                    SecurityCategory.STANDARD else SecurityCategory.HIGH_RISK
                                onUpdateRiskCategory(cred.id, newCat)
                            },
                            onDeleteClick = {
                                onDeleteCredential(cred.id)
                            },
                            onCopyUsername = {
                                clipboardManager.setText(AnnotatedString(cred.username))
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CredentialCard(
    credential: Credential,
    onCardClick: () -> Unit,
    onRevealClick: () -> Unit,
    onEditClick: () -> Unit,
    onToggleCategory: () -> Unit,
    onDeleteClick: () -> Unit,
    onCopyUsername: () -> Unit
) {
    val isHighRisk = credential.securityCategory == SecurityCategory.HIGH_RISK
    var showContextMenu by remember { mutableStateOf(false) }

    val formattedTime = remember(credential.lastUsedTimestamp) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(credential.lastUsedTimestamp))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(14.dp), ambientColor = Color(0x1A281E5D))
            .background(SignalWhite, RoundedCornerShape(14.dp))
            .border(
                width = if (isHighRisk) 1.dp else 1.dp,
                color = if (isHighRisk) Color(0xFFFCA5A5) else CoolHairline,
                shape = RoundedCornerShape(14.dp)
            )
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = { showContextMenu = true }
            )
            .padding(16.dp)
            .testTag("credential_card_${credential.id}")
    ) {
        Column {
            // Header Row: Service Name & Category Badge & Overflow
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(
                        text = credential.serviceName,
                        color = SovereignInk,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isHighRisk) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFDE8E8), RoundedCornerShape(9999.dp))
                                .border(1.dp, AlertCrimson, RoundedCornerShape(9999.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "HIGH-RISK",
                                color = AlertCrimson,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFF0EBFD), RoundedCornerShape(9999.dp))
                                .border(1.dp, SovereignViolet, RoundedCornerShape(9999.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "STANDARD",
                                color = SovereignViolet,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box {
                        IconButton(
                            onClick = { showContextMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu Aksi",
                                tint = CarbonGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showContextMenu,
                            onDismissRequest = { showContextMenu = false },
                            modifier = Modifier.background(SignalWhite)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "TAP KTP TO REVEAL",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SovereignViolet
                                    )
                                },
                                onClick = {
                                    showContextMenu = false
                                    onRevealClick()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "COPY USERNAME",
                                        fontSize = 12.sp,
                                        color = SovereignInk
                                    )
                                },
                                onClick = {
                                    showContextMenu = false
                                    onCopyUsername()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "EDIT CREDENTIAL",
                                        fontSize = 12.sp,
                                        color = SovereignInk
                                    )
                                },
                                onClick = {
                                    showContextMenu = false
                                    onEditClick()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isHighRisk) "SET AS STANDARD" else "SET AS HIGH-RISK",
                                        fontSize = 12.sp,
                                        color = if (isHighRisk) SovereignViolet else AlertCrimson
                                    )
                                },
                                onClick = {
                                    showContextMenu = false
                                    onToggleCategory()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "DELETE",
                                        fontSize = 12.sp,
                                        color = AlertCrimson
                                    )
                                },
                                onClick = {
                                    showContextMenu = false
                                    onDeleteClick()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Username
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Username: ",
                    color = CarbonGray,
                    fontSize = 12.sp
                )
                Text(
                    text = credential.username,
                    color = SovereignInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Zero-Visibility Password Rule (Pill shaped indicator)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Password: ",
                    color = CarbonGray,
                    fontSize = 12.sp
                )
                Box(
                    modifier = Modifier
                        .background(
                            if (isHighRisk) Color(0xFFFDE8E8) else Color(0xFFF0EBFD),
                            RoundedCornerShape(9999.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "🔒 Encrypted AES-256",
                        color = if (isHighRisk) AlertCrimson else SovereignViolet,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Target ID & Last Used
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (credential.targetId.isNotBlank()) "Target: ${credential.targetId}" else "Target: -",
                    color = CarbonGray,
                    fontSize = 11.sp
                )
                Text(
                    text = "Last used: $formattedTime",
                    color = CarbonGray,
                    fontSize = 10.sp
                )
            }
        }
    }
}
