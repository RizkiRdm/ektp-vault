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
    var newKeyLabel by remember { mutableStateOf("Kunci NFC Cadangan") }

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
                    text = "DAFTARKAN KUNCI FISIK BARU",
                    color = SovereignInk,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Tambahkan kartu pintar nirsentuh ISO 14443-4 (seperti e-KTP cadangan atau smart card NFC) sebagai kunci pembuka brankas tambahan.",
                        color = CarbonGray,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newKeyLabel,
                        onValueChange = { newKeyLabel = it },
                        label = { Text("Label Kunci") },
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
                        "LANJUT KE TEMPEL KTP",
                        color = SignalWhite,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
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
                    Text("BATAL", color = SovereignInk)
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
                    text = "GANTI NAMA KUNCI",
                    color = SovereignInk,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Beri label yang mudah dikenali untuk kartu fisik ini.",
                        color = CarbonGray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editKeyLabel,
                        onValueChange = { editKeyLabel = it },
                        label = { Text("Nama Baru") },
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
                    Text("SIMPAN", color = SignalWhite, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { editingKey = null },
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

    // Dialog: Confirm Make Primary
    if (keyToMakePrimary != null) {
        val targetKey = keyToMakePrimary!!
        AlertDialog(
            onDismissRequest = { keyToMakePrimary = null },
            title = {
                Text(
                    text = "JADIKAN KUNCI UTAMA (PRIMARY)",
                    color = SovereignInk,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
            },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin menetapkan '${targetKey.label}' sebagai Kunci Utama? Hanya boleh ada satu Kunci Utama dalam brankas.",
                    color = CarbonGray,
                    fontSize = 13.sp,
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
                    Text("JADIKAN UTAMA", color = SignalWhite, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { keyToMakePrimary = null },
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

    // Dialog: Delete Key Confirmation
    if (keyToDelete != null) {
        val targetKey = keyToDelete!!
        AlertDialog(
            onDismissRequest = { keyToDelete = null },
            title = {
                Text(
                    text = "CABUT / HAPUS KUNCI FISIK",
                    color = AlertCrimson,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
            },
            text = {
                Text(
                    text = "Kartu '${targetKey.label}' tidak akan dapat lagi digunakan untuk mendeskripsi brankas. Tindakan ini tidak dapat dibatalkan.",
                    color = SovereignInk,
                    fontSize = 13.sp,
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
                    Text("HAPUS KUNCI", color = SignalWhite, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { keyToDelete = null },
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
                    Text(
                        text = "MANAJEMEN KUNCI FISIK",
                        color = SovereignInk,
                        fontSize = 15.sp,
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
                            contentDescription = "Kembali",
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
                    .padding(16.dp)
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+ DAFTARKAN KUNCI BARU",
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
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
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "KUNCI TERDAFTAR (${physicalKeys.size})",
                color = SovereignViolet,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Setiap kunci terdaftar memegang salinan terenkripsi dari Master Key brankas Anda.",
                color = CarbonGray,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                            .padding(16.dp)
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
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = key.label,
                                            color = SovereignInk,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Hash UID: ${key.uidHash.take(8)}...${key.uidHash.takeLast(4)}",
                                            color = CarbonGray,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "Terdaftar: $regDate",
                                            color = CarbonGray,
                                            fontSize = 10.sp
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
                                                fontSize = 9.sp,
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
                                                fontSize = 9.sp,
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
                                                text = "AKTIF SESI INI",
                                                color = Color(0xFF2E7D32),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

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
                                        contentDescription = "Ganti Nama",
                                        modifier = Modifier.size(13.dp),
                                        tint = SovereignInk
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ganti Nama", fontSize = 11.sp, color = SovereignInk)
                                }

                                if (!key.isPrimary) {
                                    Spacer(modifier = Modifier.width(8.dp))
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
                                            contentDescription = "Jadikan Utama",
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Jadikan Utama", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    }

                                    if (physicalKeys.size > 1) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        // Delete button
                                        IconButton(
                                            onClick = { keyToDelete = key },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .testTag("delete_key_${key.uidHash.take(6)}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus Kunci",
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
