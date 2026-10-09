package com.example.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Credential
import com.example.ui.theme.CarbonGray
import com.example.ui.theme.CoolHairline
import com.example.ui.theme.GoldenRatio
import com.example.ui.theme.MistSurface
import com.example.ui.theme.SignalWhite
import com.example.ui.theme.SovereignInk
import com.example.ui.theme.SovereignViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutofillPromptModal(
    targetTitle: String,
    targetSubtitle: String,
    availableCredentials: List<Credential>,
    onDismiss: () -> Unit,
    onFillSelected: (Credential) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedCredential by remember {
        mutableStateOf(availableCredentials.firstOrNull())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SignalWhite,
        scrimColor = Color(0x660A0F1F),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(SovereignInk, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "L",
                            color = SignalWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = GoldenRatio.TextSmall
                        )
                    }
                    Spacer(modifier = Modifier.width(GoldenRatio.SpaceSmall))
                    Column {
                        Text(
                            text = "Use LOKER to Autofill Login",
                            color = SovereignInk,
                            fontSize = GoldenRatio.TextSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$targetTitle • $targetSubtitle",
                            color = CarbonGray,
                            fontSize = GoldenRatio.TextMicro,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(9999.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CoolHairline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CarbonGray),
                    modifier = Modifier.size(32.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(14.dp))
                }
            }

            Spacer(modifier = Modifier.height(GoldenRatio.SpaceBase))

            if (availableCredentials.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MistSurface, RoundedCornerShape(12.dp))
                        .border(1.dp, CoolHairline, RoundedCornerShape(12.dp))
                        .padding(GoldenRatio.SpaceBase),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = CarbonGray, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No saved credentials found for this target.",
                            color = SovereignInk,
                            fontSize = GoldenRatio.TextSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Open LOKER and save your first credential for this application.",
                            color = CarbonGray,
                            fontSize = GoldenRatio.TextMicro,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Text(
                    text = "SELECT LOGIN ACCOUNT (${availableCredentials.size})",
                    color = SovereignViolet,
                    fontSize = GoldenRatio.TextMicro,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(GoldenRatio.SpaceSmall))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(GoldenRatio.SpaceSmall),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (availableCredentials.size > 2) 140.dp else 90.dp)
                ) {
                    items(availableCredentials) { cred ->
                        val isSelected = cred.id == selectedCredential?.id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) Color(0xFFF5F3FF) else SignalWhite,
                                    RoundedCornerShape(10.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) SovereignViolet else CoolHairline,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedCredential = cred }
                                .padding(GoldenRatio.SpaceSmall)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = if (isSelected) SovereignViolet else CarbonGray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(GoldenRatio.SpaceSmall))
                                    Column {
                                        Text(
                                            text = cred.username,
                                            color = SovereignInk,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = GoldenRatio.TextSmall
                                        )
                                        Text(
                                            text = cred.serviceName,
                                            color = CarbonGray,
                                            fontSize = GoldenRatio.TextMicro
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = SovereignViolet,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(GoldenRatio.SpaceBase))

                // Action Button
                val chosen = selectedCredential
                Button(
                    onClick = {
                        if (chosen != null) {
                            onFillSelected(chosen)
                        }
                    },
                    enabled = chosen != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("fill_login_modal_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SovereignInk,
                        contentColor = SignalWhite
                    ),
                    shape = RoundedCornerShape(9999.dp)
                ) {
                    Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(GoldenRatio.SpaceSmall))
                    Text(
                        text = "AUTHORIZE KEY CARD & FILL LOGIN",
                        fontWeight = FontWeight.Medium,
                        fontSize = GoldenRatio.TextSmall,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
