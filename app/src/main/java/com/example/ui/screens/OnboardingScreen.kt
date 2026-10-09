package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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

data class OnboardingStep(
    val stepNumber: Int,
    val eyebrow: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val buttonText: String,
    val icon: ImageVector,
    val accentColor: Color
)

val SETUP_FLOW_STEPS = listOf(
    OnboardingStep(
        stepNumber = 1,
        eyebrow = "STEP 1 OF 7 • SYSTEM ARCHITECTURE",
        title = "Hardware-Anchored Security",
        subtitle = "Zero-Cloud & Absolute Physical Sovereignty",
        description = "LOKER safeguards confidential credentials and sensitive apps by anchoring master keys directly to a physical contactless smart card. No remote cloud servers exist—data access strictly requires physical card proximity plus biometric verification.",
        buttonText = "Continue: Hardware Key Pairing",
        icon = Icons.Default.Shield,
        accentColor = SovereignViolet
    ),
    OnboardingStep(
        stepNumber = 2,
        eyebrow = "STEP 2 OF 7 • PHYSICAL TOKEN",
        title = "Pair Your Security Card",
        subtitle = "ISO 14443-4 Contactless Hardware Token",
        description = "Every contactless security card possesses a cryptographic microchip with an immutable hardware identifier. LOKER generates an isolated 256-bit AES master key and cryptographically encapsulates it using the physical chip signature.",
        buttonText = "Continue: Biometric Gate",
        icon = Icons.Default.Nfc,
        accentColor = SovereignViolet
    ),
    OnboardingStep(
        stepNumber = 3,
        eyebrow = "STEP 3 OF 7 • AUTHENTICATION",
        title = "Biometric & Hardware TEE Gate",
        subtitle = "Android Keystore Isolated Subsystem",
        description = "Beyond card proximity, every vault read, password decryption, and app access is gated behind biometric authorization (fingerprint/PIN). Even in the event of a misplaced physical card, unauthorized access remains mathematically impossible.",
        buttonText = "Continue: Emergency Recovery",
        icon = Icons.Default.Fingerprint,
        accentColor = Color(0xFF10B981)
    ),
    OnboardingStep(
        stepNumber = 4,
        eyebrow = "STEP 4 OF 7 • DISASTER RECOVERY",
        title = "12-Word Cold Recovery Phrase",
        subtitle = "Deterministic Zero-Loss Backup",
        description = "During initial setup, you are issued an offline 12-word recovery phrase. If your physical card is lost or damaged, you can seamlessly restore all encrypted credentials and bind a replacement security card.",
        buttonText = "Continue: Secure Autofill",
        icon = Icons.Default.VpnKey,
        accentColor = Color(0xFFD97706)
    ),
    OnboardingStep(
        stepNumber = 5,
        eyebrow = "STEP 5 OF 7 • AUTOFILL ENGINE",
        title = "Instant & Verified Autofill",
        subtitle = "Single-Use Memory Scrubber",
        description = "LOKER integrates into Android's native Autofill framework to inject usernames and passwords directly on app login screens and browser domains—only after verified physical card and biometric authorization.",
        buttonText = "Continue: Application Locker",
        icon = Icons.Default.Key,
        accentColor = SovereignViolet
    ),
    OnboardingStep(
        stepNumber = 6,
        eyebrow = "STEP 6 OF 7 • APPLICATION LOCKER",
        title = "Lock Selected Applications",
        subtitle = "Hardware Interception Protection",
        description = "Protect high-risk apps (banking, messaging, galleries) on your device. Selected apps will be locked and intercepted until you tap your physical security card and pass biometric authorization.",
        buttonText = "Continue: Readiness Overview",
        icon = Icons.Default.Apps,
        accentColor = SovereignInk
    ),
    OnboardingStep(
        stepNumber = 7,
        eyebrow = "STEP 7 OF 7 • ACTIVATION",
        title = "Your Vault is Ready",
        subtitle = "Hardware Cryptographic Defense Active",
        description = "All cryptographic layers are prepared. Tap the button below to present your physical security card and initialize your personal LOKER vault.",
        buttonText = "Initialize LOKER Vault",
        icon = Icons.Default.Check,
        accentColor = Color(0xFF10B981)
    )
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onSkip: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(0) }
    val step = SETUP_FLOW_STEPS[currentPage]

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_trans")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

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
            // Top Bar with Skip Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LOKER ONBOARDING",
                    color = SovereignViolet,
                    fontSize = GoldenRatio.TextMicro,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                OutlinedButton(
                    onClick = onSkip,
                    shape = RoundedCornerShape(9999.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CoolHairline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CarbonGray),
                    modifier = Modifier.testTag("skip_onboarding_button")
                ) {
                    Text(
                        text = "Skip",
                        fontSize = GoldenRatio.TextMicro,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(GoldenRatio.SpaceBase))

            // Step Progress Indicator (Dots 1 to 7)
            Row(
                horizontalArrangement = Arrangement.spacedBy(GoldenRatio.SpaceMicro),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = GoldenRatio.SpaceSmall)
            ) {
                SETUP_FLOW_STEPS.forEachIndexed { index, _ ->
                    val isSelected = index == currentPage
                    val isCompleted = index < currentPage
                    Box(
                        modifier = Modifier
                            .height(GoldenRatio.SpaceSmall)
                            .width(if (isSelected) GoldenRatio.SpaceLarge else GoldenRatio.SpaceSmall)
                            .background(
                                color = if (isSelected) SovereignViolet else if (isCompleted) Color(0xFF10B981) else CoolHairline,
                                shape = RoundedCornerShape(9999.dp)
                            )
                            .clickable { currentPage = index }
                    )
                }
            }

            Spacer(modifier = Modifier.height(GoldenRatio.SpaceBase))

            // Animated Content Section
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (targetState.stepNumber > initialState.stepNumber) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }
                },
                label = "onboarding_step_anim"
            ) { targetStep ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Eyebrow
                    Text(
                        text = targetStep.eyebrow,
                        color = SovereignViolet,
                        fontSize = GoldenRatio.TextMicro,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )

                    Spacer(modifier = Modifier.height(GoldenRatio.SpaceLarge))

                    // Center Animated Icon Emblem
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .scale(if (targetStep.stepNumber == 2) pulseScale else 1f)
                            .background(SignalWhite, CircleShape)
                            .border(1.5.dp, targetStep.accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = targetStep.icon,
                            contentDescription = targetStep.title,
                            tint = targetStep.accentColor,
                            modifier = Modifier.size(52.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(GoldenRatio.SpaceLarge))

                    // Title
                    Text(
                        text = targetStep.title,
                        color = SovereignInk,
                        fontSize = GoldenRatio.TextLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )

                    Spacer(modifier = Modifier.height(GoldenRatio.SpaceMicro))

                    // Subtitle
                    Text(
                        text = targetStep.subtitle,
                        color = SovereignViolet,
                        fontSize = GoldenRatio.TextSmall,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(GoldenRatio.SpaceBase))

                    // Description Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SignalWhite, RoundedCornerShape(12.dp))
                            .border(1.dp, CoolHairline, RoundedCornerShape(12.dp))
                            .padding(GoldenRatio.SpaceBase)
                    ) {
                        Text(
                            text = targetStep.description,
                            color = CarbonGray,
                            fontSize = GoldenRatio.TextSmall,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(GoldenRatio.SpaceLarge))

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        if (currentPage < SETUP_FLOW_STEPS.size - 1) {
                            currentPage++
                        } else {
                            onFinish()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("onboarding_next_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SovereignInk,
                        contentColor = SignalWhite
                    ),
                    shape = RoundedCornerShape(9999.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = step.buttonText,
                            fontWeight = FontWeight.Medium,
                            fontSize = GoldenRatio.TextSmall
                        )
                        Spacer(modifier = Modifier.width(GoldenRatio.SpaceSmall))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (currentPage > 0) {
                    Spacer(modifier = Modifier.height(GoldenRatio.SpaceSmall))
                    OutlinedButton(
                        onClick = { currentPage-- },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("onboarding_back_button"),
                        shape = RoundedCornerShape(9999.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CoolHairline)
                    ) {
                        Text(
                            text = "Back to Previous Step",
                            color = CarbonGray,
                            fontSize = GoldenRatio.TextSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(GoldenRatio.SpaceSmall))
        }
    }
}
