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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CarbonGray
import com.example.ui.theme.CoolHairline
import com.example.ui.theme.GoldenRatio
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
                .padding(horizontal = GoldenRatio.SpaceLarge, vertical = GoldenRatio.SpaceBase)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(GoldenRatio.SpaceLarge))

                // Emblem
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(SignalWhite, CircleShape)
                        .border(1.5.dp, SovereignViolet, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "LOKER Shield",
                        tint = SovereignViolet,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(GoldenRatio.SpaceBase))

                Text(
                    text = "SOVEREIGN DATA SECURITY",
                    color = SovereignViolet,
                    fontSize = GoldenRatio.TextMicro,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp
                )

                Spacer(modifier = Modifier.height(GoldenRatio.SpaceMicro))

                Text(
                    text = "LOKER",
                    color = SovereignInk,
                    fontSize = GoldenRatio.TextDisplay,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(GoldenRatio.SpaceSmall))

                Text(
                    text = "Hardware-anchored sovereign credential vault and application security manager anchored to ISO 14443-4 contactless smart cards & Android Keystore TEE.",
                    color = CarbonGray,
                    fontSize = GoldenRatio.TextSmall,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(GoldenRatio.SpaceLarge))

                // Pillar Cards
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(GoldenRatio.SpaceBase)
                ) {
                    SetupPillarCard(
                        icon = Icons.Default.CreditCard,
                        title = "PHYSICAL PRESENCE FIRST",
                        description = "Master cryptographic keys are bound to the microchip of your contactless security card. Remote extraction is mathematically impossible."
                    )
                    SetupPillarCard(
                        icon = Icons.Default.Lock,
                        title = "ZERO-VISIBILITY LOCAL ENCRYPTION",
                        description = "AES-256-GCM zero-trust local database with 65,536-iteration PBKDF2 device salt. No credentials ever leave your physical device."
                    )
                    SetupPillarCard(
                        icon = Icons.Default.Fingerprint,
                        title = "SINGLE-USE REVEAL PROTECTION",
                        description = "Critical financial and personal credentials trigger an immediate, irreversible RAM wipe upon single use or 15-second reveal expiration."
                    )
                }
            }

            Spacer(modifier = Modifier.height(GoldenRatio.SpaceLarge))

            // Primary CTA Button
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
                    Spacer(modifier = Modifier.width(GoldenRatio.SpaceSmall))
                    Text(
                        text = "INITIALIZE VAULT WITH SECURITY CARD",
                        fontWeight = FontWeight.Medium,
                        fontSize = GoldenRatio.TextSmall,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(GoldenRatio.SpaceBase))
        }
    }
}

@Composable
private fun SetupPillarCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SignalWhite, RoundedCornerShape(12.dp))
            .border(1.dp, CoolHairline, RoundedCornerShape(12.dp))
            .padding(GoldenRatio.SpaceBase)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFEDE9FE), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SovereignViolet,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(GoldenRatio.SpaceBase))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = SovereignInk,
                    fontSize = GoldenRatio.TextSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(GoldenRatio.SpaceMicro))
                Text(
                    text = description,
                    color = CarbonGray,
                    fontSize = GoldenRatio.TextSmall,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
