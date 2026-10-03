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
import androidx.compose.material.icons.filled.CreditCard
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
        eyebrow = "LANGKAH 1 DARI 7 • CARA KERJA",
        title = "Brankas Mandiri Berbasis e-KTP",
        subtitle = "Zero-Cloud & Kontrol Penuh di Tangan Anda",
        description = "KTP-Vault mengamankan data rahasia Anda menggunakan chip kartu e-KTP sebagai kunci fisik (Physical Token). Tidak ada server eksternal, dan data hanya dapat dibuka jika Anda menempelkan kartu fisik dan lolos verifikasi biometrik.",
        buttonText = "Lanjut: Pendaftaran Kunci",
        icon = Icons.Default.Shield,
        accentColor = SovereignViolet
    ),
    OnboardingStep(
        stepNumber = 2,
        eyebrow = "LANGKAH 2 DARI 7 • KUNCI FISIK",
        title = "Daftarkan e-KTP Sebagai Kunci Utama",
        subtitle = "Proteksi Hardware ISO 14443-4",
        description = "Setiap e-KTP memiliki chip pintar nirsentuh dengan ID unik. Brankas membuat Vault Master Key 256-bit dan membungkusnya secara kriptografis menggunakan kunci kartu Anda, sehingga brankas tidak dapat dibuka tanpa kartu tersebut.",
        buttonText = "Lanjut: Atur Autentikasi",
        icon = Icons.Default.Nfc,
        accentColor = SovereignViolet
    ),
    OnboardingStep(
        stepNumber = 3,
        eyebrow = "LANGKAH 3 DARI 7 • AUTENTIKASI",
        title = "Otorisasi Biometrik / PIN Perangkat",
        subtitle = "Android Keystore System & TEE",
        description = "Selain kartu fisik, setiap akses brankas dan dekripsi password dilindungi sidik jari atau PIN perangkat Anda. Jika orang lain menemukan kartu Anda, mereka tetap tidak dapat membuka brankas tanpa biometrik Anda.",
        buttonText = "Lanjut: Recovery Cadangan",
        icon = Icons.Default.Fingerprint,
        accentColor = Color(0xFF10B981)
    ),
    OnboardingStep(
        stepNumber = 4,
        eyebrow = "LANGKAH 4 DARI 7 • PEMULIHAN",
        title = "Recovery Phrase 12 Kata Darurat",
        subtitle = "Pemulihan Tanpa Kehilangan Data",
        description = "Saat membuat brankas, Anda akan mendapatkan 12 kata rahasia (Recovery Phrase). Jika e-KTP Anda rusak atau hilang, Anda dapat memulihkan seluruh akun dan mendaftarkan e-KTP baru menggunakan 12 kata ini.",
        buttonText = "Lanjut: Pengaturan Autofill",
        icon = Icons.Default.VpnKey,
        accentColor = Color(0xFFD97706)
    ),
    OnboardingStep(
        stepNumber = 5,
        eyebrow = "LANGKAH 5 DARI 7 • AUTOFILL",
        title = "Pengisian Otomatis Cepat & Aman",
        subtitle = "Integrasi Layanan Autofill Android",
        description = "KTP-Vault dapat mengisi otomatis username dan password langsung di layar login aplikasi perbankan atau web favorit Anda, hanya setelah otorisasi kartu berhasil.",
        buttonText = "Lanjut: Buat Kredensial",
        icon = Icons.Default.Key,
        accentColor = SovereignViolet
    ),
    OnboardingStep(
        stepNumber = 6,
        eyebrow = "LANGKAH 6 DARI 7 • KREDENSIAL PERTAMA",
        title = "Simpan Password Akun Pertama",
        subtitle = "Pilih Aplikasi Langsung dari HP",
        description = "Tidak perlu mengetik nama package aplikasi secara manual. Anda dapat memilih aplikasi perbankan atau sosial media langsung dari daftar aplikasi terpasang di perangkat Anda.",
        buttonText = "Lanjut: Ringkasan Pengaturan",
        icon = Icons.Default.Apps,
        accentColor = SovereignInk
    ),
    OnboardingStep(
        stepNumber = 7,
        eyebrow = "LANGKAH 7 DARI 7 • SELESAI",
        title = "Brankas Anda Siap Digunakan",
        subtitle = "Semua Perlindungan Beroperasi Penuh",
        description = "Seluruh konsep keamanan kini telah siap. Silakan klik tombol di bawah untuk menempelkan e-KTP Anda dan memulai inisialisasi brankas pertama kali.",
        buttonText = "Mulai Inisialisasi Brankas",
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
                .padding(horizontal = 24.dp, vertical = 16.dp)
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
                    text = "PANDUAN PENYIAPAN",
                    color = SovereignViolet,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
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
                        text = "Lewati",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step Progress Indicator (Dots 1 to 7)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                SETUP_FLOW_STEPS.forEachIndexed { index, _ ->
                    val isSelected = index == currentPage
                    val isCompleted = index < currentPage
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (isSelected) 24.dp else 8.dp)
                            .background(
                                color = if (isSelected) SovereignViolet else if (isCompleted) Color(0xFF10B981) else CoolHairline,
                                shape = RoundedCornerShape(9999.dp)
                            )
                            .clickable { currentPage = index }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                label = "step_animation"
            ) { targetStep ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Hero Icon Box
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .scale(pulseScale)
                            .background(Color(0xFFF0EBFD), CircleShape)
                            .border(2.dp, targetStep.accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = targetStep.icon,
                            contentDescription = targetStep.title,
                            tint = targetStep.accentColor,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = targetStep.eyebrow,
                        color = SovereignViolet,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = targetStep.title,
                        color = SovereignInk,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = targetStep.subtitle,
                        color = CarbonGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SignalWhite, RoundedCornerShape(14.dp))
                            .border(1.dp, CoolHairline, RoundedCornerShape(14.dp))
                            .padding(18.dp)
                    ) {
                        Text(
                            text = targetStep.description,
                            color = SovereignInk,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation Buttons Row
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
                        .height(50.dp)
                        .testTag("onboarding_primary_button"),
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
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (currentPage > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
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
                            text = "Kembali ke Langkah Sebelumnya",
                            color = CarbonGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
