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
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CarbonGray
import com.example.ui.theme.CoolHairline
import com.example.ui.theme.MistSurface
import com.example.ui.theme.SignalWhite
import com.example.ui.theme.SovereignInk
import com.example.ui.theme.SovereignViolet

@Composable
fun InitialSetupScreen(
    onStartSetup: () -> Unit
) {
    Scaffold(
        containerColor = MistSurface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // NEVERHACK N emblem
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(SovereignInk, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "N",
                    color = SignalWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 38.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SOVEREIGN CYBERSECURITY",
                color = SovereignViolet,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.6.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "KTP-Vault",
                color = SovereignInk,
                fontSize = 32.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Physical-locked sovereign credential manager anchored to ISO 14443-4 e-KTP NFC chip & Android Keystore TEE.",
                color = CarbonGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // NEVERHACK Capability Cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SetupPillarCard(
                    icon = Icons.Default.CreditCard,
                    title = "PHYSICAL PRESENCE FIRST",
                    description = "Master cryptographic keys are bound to the hardware chip of your Indonesian e-KTP. Remote exfiltration is mathematically prevented."
                )
                SetupPillarCard(
                    icon = Icons.Default.Lock,
                    title = "ZERO-VISIBILITY LOCAL ENCRYPTION",
                    description = "AES-256-GCM zero-trust local database with 65,536-iteration PBKDF2 device salt. No credentials ever transit outside this device."
                )
                SetupPillarCard(
                    icon = Icons.Default.Fingerprint,
                    title = "PLAN A SINGLE-USE PROTECTION",
                    description = "Critical financial and enterprise accounts trigger an immediate, irreversible RAM wipe upon single use or 15-second reveal expiration."
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Primary CTA Button (Sovereign Ink 9999px pill)
            Button(
                onClick = onStartSetup,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("initialize_vault_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SovereignInk,
                    contentColor = SignalWhite
                ),
                shape = RoundedCornerShape(9999.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Nfc, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "INITIALIZE VAULT WITH E-KTP",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SetupPillarCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SignalWhite, RoundedCornerShape(14.dp))
            .border(1.dp, CoolHairline, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFF0EBFD), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SovereignViolet,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = SovereignInk,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = CarbonGray,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
