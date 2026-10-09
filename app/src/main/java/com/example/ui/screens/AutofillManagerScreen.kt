package com.example.ui.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.autofill.AutofillManager
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Credential
import com.example.ui.components.AutofillPromptModal
import com.example.ui.theme.CarbonGray
import com.example.ui.theme.CoolHairline
import com.example.ui.theme.GoldenRatio
import com.example.ui.theme.MistSurface
import com.example.ui.theme.SignalWhite
import com.example.ui.theme.SovereignInk
import com.example.ui.theme.SovereignViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutofillManagerScreen(
    credentials: List<Credential>,
    onBack: () -> Unit,
    onRequestAuthAndReveal: (String) -> Unit
) {
    val context = LocalContext.current
    var showSimulatorModal by remember { mutableStateOf(false) }

    val isAutofillEnabled = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val afm = context.getSystemService(AutofillManager::class.java)
            afm?.hasEnabledAutofillServices() == true
        } else {
            false
        }
    }

    if (showSimulatorModal) {
        AutofillPromptModal(
            targetTitle = "BCA Mobile (Simulasi Login)",
            targetSubtitle = "com.bca • Halaman Login",
            availableCredentials = credentials,
            onDismiss = { showSimulatorModal = false },
            onFillSelected = { selectedCred ->
                showSimulatorModal = false
                onRequestAuthAndReveal(selectedCred.id)
            }
        )
    }

    Scaffold(
        containerColor = MistSurface,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AUTOFILL & LOGIN MODAL",
                            color = SovereignInk,
                            fontSize = GoldenRatio.TextBase,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Automated username & password completion in apps / browsers",
                            color = CarbonGray,
                            fontSize = GoldenRatio.TextMicro
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("autofill_back_button")
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = GoldenRatio.SpaceLarge, vertical = GoldenRatio.SpaceBase)
                .verticalScroll(rememberScrollState())
        ) {
            // Autofill Service Status Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SignalWhite, RoundedCornerShape(14.dp))
                    .border(
                        1.dp,
                        if (isAutofillEnabled) Color(0xFF2E7D32) else SovereignViolet,
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (isAutofillEnabled) Color(0xFFE8F5E9) else Color(0xFFEDE9FE),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isAutofillEnabled) Icons.Default.CheckCircle else Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (isAutofillEnabled) Color(0xFF2E7D32) else SovereignViolet,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(GoldenRatio.SpaceBase))
                            Column {
                                Text(
                                    text = if (isAutofillEnabled) "LOKER Autofill Active" else "Enable Autofill Service",
                                    color = SovereignInk,
                                    fontSize = GoldenRatio.TextSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isAutofillEnabled) "System ready to autofill logins in apps & web" else "Requires selection as Android Autofill provider",
                                    color = CarbonGray,
                                    fontSize = GoldenRatio.TextMicro
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(GoldenRatio.SpaceBase))

                    if (!isAutofillEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
                                    data = android.net.Uri.parse("package:${context.packageName}")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    val fallback = Intent(Settings.ACTION_SETTINGS)
                                    context.startActivity(fallback)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SovereignInk),
                            shape = RoundedCornerShape(9999.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("enable_autofill_button")
                        ) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(GoldenRatio.SpaceSmall))
                            Text("SET LOKER AS DEFAULT AUTOFILL PROVIDER", fontSize = GoldenRatio.TextMicro, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Simulator Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F3FF), RoundedCornerShape(14.dp))
                    .border(1.dp, SovereignViolet, RoundedCornerShape(14.dp))
                    .padding(GoldenRatio.SpaceBase)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(SovereignViolet, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = SignalWhite, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Login Modal Simulator",
                                color = SovereignInk,
                                fontSize = GoldenRatio.TextSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Preview the interactive username & password prompt triggered when tapping a login field.",
                                color = CarbonGray,
                                fontSize = GoldenRatio.TextMicro,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(GoldenRatio.SpaceBase))

                    Button(
                        onClick = { showSimulatorModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SovereignViolet),
                        shape = RoundedCornerShape(9999.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("test_autofill_modal_button")
                    ) {
                        Text(
                            text = "OPEN LOGIN MODAL PREVIEW NOW",
                            color = SignalWhite,
                            fontSize = GoldenRatio.TextMicro,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(GoldenRatio.SpaceLarge))

            // How It Works Guide Section
            Text(
                text = "HOW LOKER AUTOFILL WORKS",
                color = SovereignViolet,
                fontSize = GoldenRatio.TextMicro,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(GoldenRatio.SpaceSmall))

            // Step 1: In Android Apps
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SignalWhite, RoundedCornerShape(12.dp))
                    .border(1.dp, CoolHairline, RoundedCornerShape(12.dp))
                    .padding(GoldenRatio.SpaceBase)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFEDE9FE), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.PhoneAndroid, contentDescription = null, tint = SovereignViolet, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(GoldenRatio.SpaceBase))
                    Column {
                        Text(
                            text = "1. In Android Applications",
                            color = SovereignInk,
                            fontSize = GoldenRatio.TextSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Open apps such as banking or social media. On any login field (Username/Password), Android prompts 'LOKER'. Tap to reveal saved accounts and authenticate with your security key card.",
                            color = CarbonGray,
                            fontSize = GoldenRatio.TextSmall,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(GoldenRatio.SpaceSmall))

            // Step 2: In Web Domains
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SignalWhite, RoundedCornerShape(12.dp))
                    .border(1.dp, CoolHairline, RoundedCornerShape(12.dp))
                    .padding(GoldenRatio.SpaceBase)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFEDE9FE), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = SovereignViolet, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(GoldenRatio.SpaceBase))
                    Column {
                        Text(
                            text = "2. On Web Domains / Mobile Browsers",
                            color = SovereignInk,
                            fontSize = GoldenRatio.TextSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "When navigating to web login pages (such as google.com or github.com), LOKER inspects the domain and matches your saved credentials. Tap Autofill to fill credentials instantly after tapping your security card.",
                            color = CarbonGray,
                            fontSize = GoldenRatio.TextSmall,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(GoldenRatio.SpaceSmall))

            // Step 3: Security & Single-use Scrub
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SignalWhite, RoundedCornerShape(12.dp))
                    .border(1.dp, CoolHairline, RoundedCornerShape(12.dp))
                    .padding(GoldenRatio.SpaceBase)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFEDE9FE), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = SovereignViolet, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(GoldenRatio.SpaceBase))
                    Column {
                        Text(
                            text = "3. Zero-Leak Memory Scrubbing",
                            color = SovereignInk,
                            fontSize = GoldenRatio.TextSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Passwords are never retained unencrypted in memory. Every filled entry is cryptographically handled and erased from device RAM upon completion.",
                            color = CarbonGray,
                            fontSize = GoldenRatio.TextSmall,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
