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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.PhysicalKey
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CarbonGray
import com.example.ui.theme.CoolHairline
import com.example.ui.theme.GoldenRatio
import com.example.ui.theme.MistSurface
import com.example.ui.theme.SignalWhite
import com.example.ui.theme.SovereignInk
import com.example.ui.theme.SovereignViolet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhysicalKeyManagerScreen(
    physicalKeys: List<PhysicalKey>,
    currentSessionUidHash: String? = null,
    onBack: () -> Unit,
    onRegisterNewKeyPrompt: (label: String) -> Unit,
    onRenameKey: (uidHash: String, newLabel: String) -> Unit = { _, _ -> },
    onMakeKeyPrimary: (uidHash: String) -> Unit = {},
    onRemoveKey: (uidHash: String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newKeyLabel by remember { mutableStateOf("Backup Hardware Key") }

    var editingKey by remember { mutableStateOf<PhysicalKey?>(null) }
    var editKeyLabel by remember { mutableStateOf("") }

    var keyToDelete by remember { mutableStateOf<PhysicalKey?>(null) }
    var keyToMakePrimary by remember { mutableStateOf<PhysicalKey?>(null) }

    // Dialog: Add Key
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "REGISTER NEW PHYSICAL KEY",
                    color = SovereignInk,
                    fontWeight = FontWeight.Medium,
                    fontSize = GoldenRatio.TextBase,
                    letterSpacing = 0.5.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Add an ISO 14443-4 contactless smart card or NFC security token as a supplementary unlocking key.",
                        color = CarbonGray,
                        fontSize = GoldenRatio.TextSmall,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(GoldenRatio.SpaceBase))
                    OutlinedTextField(
                        value = newKeyLabel,
                        onValueChange = { newKeyLabel = it },
                        label = { Text("Key Label") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_key_label_input"),
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
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = newKeyLabel.trim()
                        if (clean.isNotEmpty()) {
                            showAddDialog = false
                            onRegisterNewKeyPrompt(clean)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SovereignInk),
                    shape = RoundedCornerShape(9999.dp),
                    modifier = Modifier.testTag("confirm_register_key_button")
                ) {
                    Text(
                        "PROCEED TO CARD SCAN",
                        color = SignalWhite,
                        fontWeight = FontWeight.Medium,
                        fontSize = GoldenRatio.TextSmall,
                        letterSpacing = 0.5.sp
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAddDialog = false },
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

    // Dialog: Rename Key
    if (editingKey != null) {
        val targetKey = editingKey!!
        AlertDialog(
            onDismissRequest = { editingKey = null },
            title = {
                Text(
                    text = "RENAME SECURITY KEY",
                    color = SovereignInk,
                    fontWeight = FontWeight.Medium,
                    fontSize = GoldenRatio.TextBase,
                    letterSpacing = 0.5.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Set an identifiable label for this physical security card.",
                        color = CarbonGray,
                        fontSize = GoldenRatio.TextSmall
                    )
                    Spacer(modifier = Modifier.height(GoldenRatio.SpaceBase))
                    OutlinedTextField(
                        value = editKeyLabel,
                        onValueChange = { editKeyLabel = it },
                        label = { Text("New Label") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rename_key_input"),
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
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = editKeyLabel.trim()
                        if (clean.isNotEmpty()) {
                            onRenameKey(targetKey.uidHash, clean)
                            editingKey = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SovereignInk),
                    shape = RoundedCornerShape(9999.dp),
                    modifier = Modifier.testTag("save_rename_key_button")
                ) {
                    Text("SAVE", color = SignalWhite, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { editingKey = null },
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

    // Dialog: Confirm Make Primary
    if (keyToMakePrimary != null) {
        val targetKey = keyToMakePrimary!!
        AlertDialog(
            onDismissRequest = { keyToMakePrimary = null },
            title = {
                Text(
                    text = "DESIGNATE PRIMARY KEY",
                    color = SovereignInk,
                    fontWeight = FontWeight.Medium,
                    fontSize = GoldenRatio.TextBase,
                    letterSpacing = 0.5.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to designate '${targetKey.label}' as the Primary Key? There can only be one Primary Key for the vault.",
                    color = CarbonGray,
                    fontSize = GoldenRatio.TextSmall,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onMakeKeyPrimary(targetKey.uidHash)
                        keyToMakePrimary = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SovereignViolet),
                    shape = RoundedCornerShape(9999.dp),
                    modifier = Modifier.testTag("confirm_make_primary_button")
                ) {
                    Text("SET AS PRIMARY", color = SignalWhite, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { keyToMakePrimary = null },
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

    // Dialog: Delete Key Confirmation
    if (keyToDelete != null) {
        val targetKey = keyToDelete!!
        AlertDialog(
            onDismissRequest = { keyToDelete = null },
            title = {
                Text(
                    text = "REVOKE PHYSICAL KEY",
                    color = AlertCrimson,
                    fontWeight = FontWeight.Medium,
                    fontSize = GoldenRatio.TextBase,
                    letterSpacing = 0.5.sp
                )
            },
            text = {
                Text(
                    text = "Security key '${targetKey.label}' will no longer be capable of decrypting this vault. This action cannot be undone.",
                    color = SovereignInk,
                    fontSize = GoldenRatio.TextSmall,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveKey(targetKey.uidHash)
                        keyToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertCrimson),
                    shape = RoundedCornerShape(9999.dp),
                    modifier = Modifier.testTag("confirm_delete_key_button")
                ) {
                    Text("REVOKE KEY", color = SignalWhite, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { keyToDelete = null },
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
                        text = "PHYSICAL KEY MANAGER",
                        color = SovereignInk,
                        fontSize = GoldenRatio.TextBase,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("byok_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("add_physical_key_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SovereignInk,
                        contentColor = SignalWhite
                    ),
                    shape = RoundedCornerShape(9999.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(GoldenRatio.SpaceSmall))
                    Text(
                        text = "+ REGISTER NEW KEY",
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
            Text(
                text = "REGISTERED HARDWARE KEYS (${physicalKeys.size})",
                color = SovereignViolet,
                fontSize = GoldenRatio.TextMicro,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Each registered card holds an encrypted instance of your vault's master credentials.",
                color = CarbonGray,
                fontSize = GoldenRatio.TextSmall,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(GoldenRatio.SpaceBase))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(physicalKeys, key = { it.uidHash }) { key ->
                    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
                    val regDate = remember(key.registeredAt) { sdf.format(Date(key.registeredAt)) }
                    val isCurrentSession = currentSessionUidHash != null && currentSessionUidHash == key.uidHash

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SignalWhite, RoundedCornerShape(14.dp))
                            .border(
                                1.dp,
                                if (key.isPrimary) SovereignViolet else CoolHairline,
                                RoundedCornerShape(14.dp)
                            )
                            .padding(GoldenRatio.SpaceBase)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                if (key.isPrimary) Color(0xFFF0EBFD) else Color(0xFFF3F4F6),
                                                RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (key.isPrimary) Icons.Default.CreditCard else Icons.Default.Key,
                                            contentDescription = null,
                                            tint = if (key.isPrimary) SovereignViolet else CarbonGray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(GoldenRatio.SpaceBase))
                                    Column {
                                        Text(
                                            text = key.label,
                                            color = SovereignInk,
                                            fontSize = GoldenRatio.TextSmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "UID Hash: ${key.uidHash.take(8)}...${key.uidHash.takeLast(4)}",
                                            color = CarbonGray,
                                            fontSize = GoldenRatio.TextMicro,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "Enrolled: $regDate",
                                            color = CarbonGray,
                                            fontSize = GoldenRatio.TextMicro
                                        )
                                    }
                                }

                                // Badges
                                Column(horizontalAlignment = Alignment.End) {
                                    if (key.isPrimary) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFF0EBFD), RoundedCornerShape(9999.dp))
                                                .border(1.dp, SovereignViolet, RoundedCornerShape(9999.dp))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "PRIMARY KEY",
                                                color = SovereignViolet,
                                                fontSize = GoldenRatio.TextMicro,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFF3F4F6), RoundedCornerShape(9999.dp))
                                                .border(1.dp, CoolHairline, RoundedCornerShape(9999.dp))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "REGISTERED KEY",
                                                color = CarbonGray,
                                                fontSize = GoldenRatio.TextMicro,
                                                fontWeight = FontWeight.Medium,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }

                                    if (isCurrentSession) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFE8F5E9), RoundedCornerShape(9999.dp))
                                                .border(1.dp, Color(0xFF2E7D32), RoundedCornerShape(9999.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "ACTIVE SESSION",
                                                color = Color(0xFF2E7D32),
                                                fontSize = GoldenRatio.TextMicro,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(GoldenRatio.SpaceBase))

                            // Action buttons row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Rename button
                                OutlinedButton(
                                    onClick = {
                                        editKeyLabel = key.label
                                        editingKey = key
                                    },
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("rename_key_${key.uidHash.take(6)}"),
                                    shape = RoundedCornerShape(9999.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CoolHairline)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Rename",
                                        modifier = Modifier.size(13.dp),
                                        tint = SovereignInk
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Rename", fontSize = GoldenRatio.TextMicro, color = SovereignInk)
                                }

                                if (!key.isPrimary) {
                                    Spacer(modifier = Modifier.width(GoldenRatio.SpaceSmall))
                                    // Make Primary button
                                    Button(
                                        onClick = { keyToMakePrimary = key },
                                        modifier = Modifier
                                            .height(34.dp)
                                            .testTag("make_primary_${key.uidHash.take(6)}"),
                                        shape = RoundedCornerShape(9999.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFF0EBFD),
                                            contentColor = SovereignViolet
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Set Primary",
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Set Primary", fontSize = GoldenRatio.TextMicro, fontWeight = FontWeight.Medium)
                                    }

                                    if (physicalKeys.size > 1) {
                                        Spacer(modifier = Modifier.width(GoldenRatio.SpaceSmall))
                                        // Delete button
                                        IconButton(
                                            onClick = { keyToDelete = key },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .testTag("delete_key_${key.uidHash.take(6)}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Revoke Key",
                                                tint = AlertCrimson,
                                                modifier = Modifier.size(18.dp)
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
}
