package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.security.BiometricAuthHelper
import com.example.data.security.BiometricHardwareStatus
import com.example.ui.theme.VaultEmeraldAccent
import com.example.ui.theme.VaultEmeraldLight
import com.example.ui.theme.VaultNavyDark
import com.example.ui.theme.VaultTealLight

@Composable
fun BiometricLockScreen(
    hasConfiguredPin: Boolean,
    lockScreenMessage: String?,
    onBiometricSuccess: () -> Unit,
    onBiometricError: (String) -> Unit,
    onClearMessage: () -> Unit,
    onSubmitPin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hardwareStatus by remember {
        mutableStateOf(BiometricAuthHelper.checkBiometricStatus(context))
    }
    var enteredPin by remember { mutableStateOf("") }
    var showPinPadExpanded by remember {
        mutableStateOf(!hardwareStatus.canPromptDirectly)
    }

    // Refresh biometric sensor status when returning from Android Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hardwareStatus = BiometricAuthHelper.checkBiometricStatus(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Automatically trigger system Fingerprint / Face prompt when ready
    LaunchedEffect(hardwareStatus) {
        if (hardwareStatus == BiometricHardwareStatus.READY) {
            BiometricAuthHelper.showBiometricPrompt(
                context = context,
                onSuccess = onBiometricSuccess,
                onUsePinFallback = { showPinPadExpanded = true },
                onErrorMessage = onBiometricError
            )
        } else {
            showPinPadExpanded = true
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "biometric_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VaultNavyDark)
            .testTag("biometric_lock_screen"),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_vault_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            VaultNavyDark.copy(alpha = 0.88f),
                            VaultNavyDark.copy(alpha = 0.97f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Security Shield Pill
            Surface(
                shape = CircleShape,
                color = VaultEmeraldAccent.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, VaultEmeraldLight.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = VaultEmeraldLight,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "BIOMETRIC VAULT PROTECTION ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = VaultEmeraldLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Pulsing Fingerprint & Face Scanner Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(116.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                VaultTealLight.copy(alpha = 0.28f),
                                VaultNavyDark.copy(alpha = 0.2f)
                            )
                        )
                    )
                    .clickable {
                        onClearMessage()
                        BiometricAuthHelper.showBiometricPrompt(
                            context = context,
                            onSuccess = onBiometricSuccess,
                            onUsePinFallback = { showPinPadExpanded = true },
                            onErrorMessage = onBiometricError
                        )
                    }
                    .testTag("biometric_scanner_button")
            ) {
                Surface(
                    shape = CircleShape,
                    color = VaultNavyDark.copy(alpha = 0.9f),
                    border = BorderStroke(2.dp, VaultTealLight),
                    modifier = Modifier.size(92.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Unlock with Fingerprint",
                            tint = VaultTealLight,
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "Unlock with Face",
                            tint = VaultEmeraldLight,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "DocuVault is Locked",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Verify with Fingerprint, Face Unlock, or your 4-digit Vault PIN to access your personal documents.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.78f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Biometric Prompt Button
            Button(
                onClick = {
                    onClearMessage()
                    BiometricAuthHelper.showBiometricPrompt(
                        context = context,
                        onSuccess = onBiometricSuccess,
                        onUsePinFallback = { showPinPadExpanded = true },
                        onErrorMessage = onBiometricError
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = VaultTealLight,
                    contentColor = VaultNavyDark
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("authenticate_biometric_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Unlock with Fingerprint / Face",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Hardware Status & Enroll Option Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.07f)
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (hardwareStatus.canPromptDirectly) {
                                Icons.Default.CheckCircle
                            } else {
                                Icons.Default.Lock
                            },
                            contentDescription = null,
                            tint = if (hardwareStatus.canPromptDirectly) {
                                VaultEmeraldLight
                            } else {
                                VaultTealLight
                            },
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = hardwareStatus.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                    }

                    Text(
                        text = hardwareStatus.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.72f)
                    )

                    if (hardwareStatus == BiometricHardwareStatus.NOT_ENROLLED) {
                        OutlinedButton(
                            onClick = {
                                BiometricAuthHelper.openBiometricEnrollmentSettings(context)
                            },
                            border = BorderStroke(1.dp, VaultTealLight.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VaultTealLight),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("enroll_biometrics_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Enroll Fingerprint / Face in Settings")
                        }
                    }
                }
            }

            // Error / Status Banner on Lock Screen
            AnimatedVisibility(visible = !lockScreenMessage.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .testTag("lock_screen_error_banner"),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = lockScreenMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Toggle or Display 4-Digit Backup PIN Pad
            if (!showPinPadExpanded) {
                TextButton(
                    onClick = { showPinPadExpanded = true },
                    modifier = Modifier.testTag("show_pin_pad_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = VaultTealLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasConfiguredPin) {
                            "Use 4-Digit Backup Vault PIN"
                        } else {
                            "Set Up 4-Digit Backup Vault PIN"
                        },
                        color = VaultTealLight
                    )
                }
            } else {
                VaultPinKeypadCard(
                    hasConfiguredPin = hasConfiguredPin,
                    enteredPin = enteredPin,
                    onDigitClick = { digit ->
                        onClearMessage()
                        if (enteredPin.length < 4) {
                            val next = enteredPin + digit
                            enteredPin = next
                            if (next.length == 4) {
                                onSubmitPin(next)
                                enteredPin = ""
                            }
                        }
                    },
                    onBackspaceClick = {
                        if (enteredPin.isNotEmpty()) {
                            enteredPin = enteredPin.dropLast(1)
                        }
                    },
                    onClearClick = {
                        enteredPin = ""
                        onClearMessage()
                    }
                )
            }
        }
    }
}

@Composable
private fun VaultPinKeypadCard(
    hasConfiguredPin: Boolean,
    enteredPin: String,
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vault_pin_keypad_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (hasConfiguredPin) {
                    "Enter 4-Digit Backup Vault PIN"
                } else {
                    "Create 4-Digit Backup Vault PIN"
                },
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )

            Text(
                text = if (hasConfiguredPin) {
                    "Unlocks DocuVault when fingerprint or face is not used"
                } else {
                    "Set a 4-digit PIN (SHA-256 hashed) as your backup security key"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4-Dot Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    val filled = index < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (filled) VaultEmeraldLight else Color.White.copy(alpha = 0.24f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9")
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEach { rowDigits ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowDigits.forEach { digit ->
                            PinDigitButton(
                                label = digit,
                                onClick = { onDigitClick(digit) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pin_key_$digit")
                            )
                        }
                    }
                }

                // Bottom Row: Clear, 0, Backspace
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        onClick = onClearClick,
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("pin_key_clear")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "CLR",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    PinDigitButton(
                        label = "0",
                        onClick = { onDigitClick("0") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pin_key_0")
                    )

                    Surface(
                        onClick = onBackspaceClick,
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("pin_key_backspace")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Backspace",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinDigitButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.13f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
        modifier = modifier.height(50.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
